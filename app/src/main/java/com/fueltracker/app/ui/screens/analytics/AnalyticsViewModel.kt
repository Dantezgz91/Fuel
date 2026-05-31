package com.fueltracker.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.PriceAnalysis
import com.fueltracker.app.domain.usecase.AnalyzePatternsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
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

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyzePatternsUseCase: AnalyzePatternsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        observeAnalysis()
    }

    private fun observeAnalysis() {
        viewModelScope.launch {
            analyzePatternsUseCase(_uiState.value.selectedFuelType).collect { analysis ->
                _uiState.value = _uiState.value.copy(analysis = analysis, isLoading = false)
            }
        }
    }

    fun selectFuelType(fuelType: FuelType) {
        _uiState.value = _uiState.value.copy(selectedFuelType = fuelType, isLoading = true)
        observeAnalysis()
    }

    fun selectTab(tab: AnalyticsTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
}
