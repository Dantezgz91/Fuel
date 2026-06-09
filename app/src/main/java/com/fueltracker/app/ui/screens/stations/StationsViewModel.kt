package com.fueltracker.app.ui.screens.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.Municipality
import com.fueltracker.app.domain.repository.GasStationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StationsUiState(
    val trackedStations: List<GasStation> = emptyList(),
    val searchResults: List<GasStation> = emptyList(),
    val stationQuery: String = "",
    val municipalities: List<Municipality> = emptyList(),
    val filteredMunicipalities: List<Municipality> = emptyList(),
    val municipalityQuery: String = "",
    val selectedMunicipality: Municipality? = null,
    val isSearchingMunicipalities: Boolean = false,
    val isLoadingStations: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class StationsViewModel @Inject constructor(
    private val gasStationRepository: GasStationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StationsUiState())
    val uiState: StateFlow<StationsUiState> = _uiState.asStateFlow()

    init {
        gasStationRepository.getTrackedStations().onEach { stations ->
            _uiState.value = _uiState.value.copy(trackedStations = stations)
        }.launchIn(viewModelScope)
    }

    fun loadMunicipalities() {
        if (_uiState.value.municipalities.isNotEmpty()) {
            applyMunicipalityFilter(_uiState.value.municipalityQuery)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingMunicipalities = true)
            gasStationRepository.getMunicipalities().fold(
                onSuccess = { list ->
                    _uiState.value = _uiState.value.copy(
                        municipalities = list,
                        isSearchingMunicipalities = false
                    )
                    applyMunicipalityFilter(_uiState.value.municipalityQuery)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Error cargando municipios: ${e.message}",
                        isSearchingMunicipalities = false
                    )
                }
            )
        }
    }

    fun onMunicipalityQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(municipalityQuery = query)
        applyMunicipalityFilter(query)
    }

    private fun applyMunicipalityFilter(query: String) {
        val municipalities = _uiState.value.municipalities
        val normalizedQuery = query.trim()
        val filtered = when {
            normalizedQuery.length < 3 -> {
                // Evita listas gigantes: hasta que haya 3 letras, muestra una lista útil y corta.
                municipalities.filter(::isCapital)
            }
            else -> {
                municipalities.filter { municipality ->
                    municipality.name.contains(normalizedQuery, ignoreCase = true) ||
                        municipality.province.contains(normalizedQuery, ignoreCase = true) ||
                        municipality.autonomousCommunity.contains(normalizedQuery, ignoreCase = true)
                }
            }
        }

        // Prioriza capitales (Municipio == Provincia) y luego orden alfabético.
        val sorted = filtered.sortedWith(
            compareByDescending<Municipality> { isCapital(it) }
                .thenBy { it.name.lowercase() }
        )

        _uiState.value = _uiState.value.copy(filteredMunicipalities = sorted.distinctBy { it.id })
    }

    private fun isCapital(municipality: Municipality): Boolean {
        return municipality.name.equals(municipality.province, ignoreCase = true)
    }

    fun selectMunicipality(municipality: Municipality) {
        _uiState.value = _uiState.value.copy(
            selectedMunicipality = municipality,
            municipalityQuery = municipality.name,
            isLoadingStations = true,
            searchResults = emptyList(),
            stationQuery = "",
            error = null
        )
        viewModelScope.launch {
            gasStationRepository.searchStationsByMunicipality(municipality.id).fold(
                onSuccess = { stations ->
                    _uiState.value = _uiState.value.copy(
                        searchResults = stations.distinctBy { it.id },
                        isLoadingStations = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Error buscando gasolineras: ${e.message}",
                        isLoadingStations = false
                    )
                }
            )
        }
    }

    fun trackStation(stationId: String) {
        viewModelScope.launch {
            gasStationRepository.trackStation(stationId)
        }
    }

    fun untrackStation(stationId: String) {
        viewModelScope.launch {
            gasStationRepository.untrackStation(stationId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun onStationQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(stationQuery = query)
    }
}
