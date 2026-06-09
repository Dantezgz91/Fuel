package com.fueltracker.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fueltracker.app.data.local.preferences.FuelTypeCatalog
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.FuelTypeConfig
import com.fueltracker.app.domain.model.PriceAnalysis
import com.fueltracker.app.domain.usecase.AnalyzePatternsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AnalyticsUiState(
    val visibleFuelTypes: List<FuelTypeConfig> = emptyList(),
    val selectedFuelType: FuelType = FuelType.GASOLINA_95,
    val analysis: PriceAnalysis? = null,
    val isLoading: Boolean = true,
    val selectedTab: AnalyticsTab = AnalyticsTab.DAY_OF_WEEK
)

enum class AnalyticsTab(val label: String) {
    DAY_OF_WEEK("Día semana"),
    DAY_OF_MONTH("Día mes"),
    WEEK_OF_MONTH("Semana mes")
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyzePatternsUseCase: AnalyzePatternsUseCase,
    private val fuelTypeCatalog: FuelTypeCatalog
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val selectedFuelType = MutableStateFlow(FuelType.GASOLINA_95)

    init {
        fuelTypeCatalog.observeVisibleConfigs()
            .onEach { configs ->
                _uiState.update { state ->
                    val selected = resolveSelectedFuelType(state.selectedFuelType, configs)
                    if (selected != state.selectedFuelType) {
                        selectedFuelType.value = selected
                    }
                    state.copy(visibleFuelTypes = configs, selectedFuelType = selected)
                }
            }
            .launchIn(viewModelScope)

        selectedFuelType
            .flatMapLatest { fuelType ->
                _uiState.update { it.copy(isLoading = true) }
                analyzePatternsUseCase(fuelType)
            }
            .onEach { analysis ->
                _uiState.update { it.copy(analysis = analysis, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun selectFuelType(fuelType: FuelType) {
        selectedFuelType.value = fuelType
        _uiState.update { it.copy(selectedFuelType = fuelType, isLoading = true) }
    }

    fun selectTab(tab: AnalyticsTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    private fun resolveSelectedFuelType(
        current: FuelType,
        visible: List<FuelTypeConfig>
    ): FuelType {
        if (visible.isEmpty()) return current
        if (visible.any { it.type == current }) return current
        return visible.first().type
    }
}
