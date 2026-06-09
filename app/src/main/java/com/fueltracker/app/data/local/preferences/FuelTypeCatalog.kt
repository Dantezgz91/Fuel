package com.fueltracker.app.data.local.preferences

import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.FuelTypeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelTypeCatalog @Inject constructor(
    private val userPreferences: UserPreferences
) {
    fun observeAllConfigs(): Flow<List<FuelTypeConfig>> {
        return combine(
            userPreferences.enabledFuelTypes,
            userPreferences.customFuelLabels
        ) { enabled, labels ->
            FuelType.entries.map { type ->
                val isEnabled = enabled.isEmpty() || type.name in enabled
                FuelTypeConfig(
                    type = type,
                    displayName = labels[type.name] ?: type.displayName,
                    isEnabled = isEnabled,
                    isAvailableFromApi = true
                )
            }
        }
    }

    fun observeVisibleConfigs(): Flow<List<FuelTypeConfig>> {
        return observeAllConfigs().map { configs ->
            configs.filter { config -> config.isEnabled }
        }
    }
}
