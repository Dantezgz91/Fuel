package com.fueltracker.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.PriceAnalysis
import com.fueltracker.app.domain.model.Recommendation
import com.fueltracker.app.domain.repository.GasStationRepository
import com.fueltracker.app.domain.repository.PriceRepository
import com.fueltracker.app.domain.usecase.AnalyzePatternsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val trackedStations: List<GasStation> = emptyList(),
    val selectedFuelType: FuelType = FuelType.GASOLINA_95,
    val cheapestStation: GasStation? = null,
    val cheapestPrice: Double? = null,
    val analysis: PriceAnalysis? = null,
    val recommendation: Recommendation? = null,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gasStationRepository: GasStationRepository,
    private val priceRepository: PriceRepository,
    private val analyzePatternsUseCase: AnalyzePatternsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeTrackedStations()
        observeAnalysis()
    }

    private fun observeTrackedStations() {
        gasStationRepository.getTrackedStations().onEach { stations ->
            val fuelType = _uiState.value.selectedFuelType
            val stationsWithPrices = stations.map { station ->
                val prices = priceRepository.getLatestPrices(station.id)
                station.copy(currentPrices = prices)
            }
            val cheapestEntry = stationsWithPrices
                .mapNotNull { s -> s.currentPrices[fuelType]?.let { s to it } }
                .minByOrNull { it.second }

            _uiState.value = _uiState.value.copy(
                trackedStations = stationsWithPrices,
                cheapestStation = cheapestEntry?.first,
                cheapestPrice = cheapestEntry?.second,
                error = null
            )
        }.launchIn(viewModelScope)
    }

    private fun observeAnalysis() {
        viewModelScope.launch {
            analyzePatternsUseCase(_uiState.value.selectedFuelType).collect { analysis ->
                val best = analysis.bestDayOfWeek
                val worst = analysis.worstDayOfWeek
                val recommendation = if (best != null) {
                    val saving = if (worst != null) worst.averagePrice - best.averagePrice else 0.0
                    val confidence = minOf(best.sampleCount / 10f, 1f)
                    val trendText = when (analysis.trend) {
                        com.fueltracker.app.domain.model.PriceTrend.RISING -> " Los precios están subiendo."
                        com.fueltracker.app.domain.model.PriceTrend.FALLING -> " Los precios están bajando."
                        com.fueltracker.app.domain.model.PriceTrend.STABLE -> " Los precios están estables."
                    }
                    Recommendation(
                        fuelType = analysis.fuelType,
                        bestDayLabel = best.dayName,
                        expectedSaving = saving,
                        confidence = confidence,
                        detail = buildString {
                            append("Históricamente, el ${best.dayName} tiene el precio más bajo ")
                            append("(${String.format("%.3f", best.averagePrice)} €/L).")
                            if (worst != null) append(" Evita el ${worst.dayName} (${String.format("%.3f", worst.averagePrice)} €/L).")
                            append(trendText)
                        }
                    )
                } else null
                _uiState.value = _uiState.value.copy(analysis = analysis, recommendation = recommendation)
            }
        }
    }

    fun selectFuelType(fuelType: FuelType) {
        _uiState.value = _uiState.value.copy(selectedFuelType = fuelType)
        observeTrackedStations()
        observeAnalysis()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                gasStationRepository.refreshPricesForTrackedStations()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Error actualizando precios: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }
}
