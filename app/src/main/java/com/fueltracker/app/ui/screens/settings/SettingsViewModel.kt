package com.fueltracker.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fueltracker.app.data.local.preferences.FuelTypeCatalog
import com.fueltracker.app.data.local.preferences.UserPreferences
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.FuelTypeConfig
import com.fueltracker.app.domain.model.GeoLocation
import com.fueltracker.app.domain.model.ThemeMode
import com.fueltracker.app.domain.repository.LocationRepository
import com.fueltracker.app.domain.repository.PriceRepository
import com.fueltracker.app.worker.PriceSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoSyncEnabled: Boolean = true,
    val dataRetentionDays: Int = UserPreferences.DEFAULT_RETENTION_DAYS,
    val fuelTypeConfigs: List<FuelTypeConfig> = emptyList(),
    val homeLocation: GeoLocation? = null,
    val travelRadiusKm: Int = UserPreferences.DEFAULT_RADIUS_KM,
    val isClearingData: Boolean = false,
    val isLoadingLocation: Boolean = false,
    val needsLocationPermission: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val priceRepository: PriceRepository,
    private val priceSyncScheduler: PriceSyncScheduler,
    fuelTypeCatalog: FuelTypeCatalog,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            combine(
                userPreferences.autoSyncEnabled,
                userPreferences.dataRetentionDays,
                fuelTypeCatalog.observeAllConfigs(),
                userPreferences.homeLocation
            ) { autoSync, retention, fuelConfigs, home ->
                PrefsBase(autoSync, retention, fuelConfigs, home)
            },
            userPreferences.travelRadiusKm
        ) { base, travelRadius ->
            PrefsSnapshot(
                autoSync = base.autoSync,
                retention = base.retention,
                fuelConfigs = base.fuelConfigs,
                home = base.home,
                travelRadius = travelRadius
            )
        }.onEach { snapshot ->
            _uiState.update {
                it.copy(
                    autoSyncEnabled = snapshot.autoSync,
                    dataRetentionDays = snapshot.retention,
                    fuelTypeConfigs = snapshot.fuelConfigs,
                    homeLocation = snapshot.home,
                    travelRadiusKm = snapshot.travelRadius
                )
            }
        }.launchIn(viewModelScope)

        userPreferences.themeMode
            .onEach { mode -> _uiState.update { it.copy(themeMode = mode) } }
            .launchIn(viewModelScope)
    }

    private data class PrefsBase(
        val autoSync: Boolean,
        val retention: Int,
        val fuelConfigs: List<FuelTypeConfig>,
        val home: GeoLocation?
    )

    private data class PrefsSnapshot(
        val autoSync: Boolean,
        val retention: Int,
        val fuelConfigs: List<FuelTypeConfig>,
        val home: GeoLocation?,
        val travelRadius: Int
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoSyncEnabled(enabled)
            if (enabled) {
                priceSyncScheduler.schedulePeriodicSync()
            } else {
                priceSyncScheduler.cancelPeriodicSync()
            }
        }
    }

    fun setDataRetention(days: Int) {
        if (days == _uiState.value.dataRetentionDays && !_uiState.value.isClearingData) {
            return
        }
        viewModelScope.launch {
            userPreferences.setDataRetentionDays(days)
            _uiState.update { it.copy(isClearingData = true) }
            val deleted = priceRepository.deleteRecordsOutsideRetention(days)
            _uiState.update {
                it.copy(
                    isClearingData = false,
                    dataRetentionDays = days,
                    message = if (deleted > 0) {
                        "Eliminados $deleted registros con más de $days días."
                    } else {
                        "No hay registros con más de $days días."
                    }
                )
            }
        }
    }

    fun setTravelRadiusKm(km: Int) {
        viewModelScope.launch {
            userPreferences.setTravelRadiusKm(km)
        }
    }

    fun requestHomeFromCurrentLocation() {
        if (!locationRepository.hasLocationPermission()) {
            _uiState.update { it.copy(needsLocationPermission = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true) }
            locationRepository.getCurrentLocation()
                .onSuccess { location ->
                    userPreferences.setHomeLocation(location)
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            message = "Ubicación de casa guardada."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            message = error.message ?: "No se pudo obtener la ubicación."
                        )
                    }
                }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(needsLocationPermission = false) }
        if (granted) {
            requestHomeFromCurrentLocation()
        } else {
            _uiState.update {
                it.copy(message = "Sin permiso de ubicación no se puede guardar tu casa.")
            }
        }
    }

    fun clearHomeLocation() {
        viewModelScope.launch {
            userPreferences.setHomeLocation(null)
            _uiState.update { it.copy(message = "Ubicación de casa eliminada.") }
        }
    }

    fun setFuelTypeEnabled(fuelType: FuelType, enabled: Boolean) {
        viewModelScope.launch {
            val available = FuelType.entries.map { it.name }.toSet()

            var enabledNames = userPreferences.getEnabledFuelTypeNames()
            if (enabledNames.isEmpty()) {
                enabledNames = available
            }
            enabledNames = if (enabled) {
                enabledNames + fuelType.name
            } else {
                val without = enabledNames - fuelType.name
                without.ifEmpty { enabledNames }
            }
            val finalSet = if (enabledNames.size >= available.size) emptySet() else enabledNames
            userPreferences.setEnabledFuelTypeNames(finalSet)
        }
    }

    fun setFuelTypeLabel(fuelType: FuelType, label: String) {
        viewModelScope.launch {
            userPreferences.setFuelTypeLabel(fuelType, label)
        }
    }

    fun clearAllPriceHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingData = true) }
            val deleted = priceRepository.deleteAllPriceRecords()
            _uiState.update {
                it.copy(
                    isClearingData = false,
                    message = if (deleted > 0) {
                        "Historial de precios eliminado ($deleted registros)."
                    } else {
                        "No había historial de precios que borrar."
                    }
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
