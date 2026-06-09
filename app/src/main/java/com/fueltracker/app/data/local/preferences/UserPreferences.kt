package com.fueltracker.app.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.GeoLocation
import com.fueltracker.app.domain.model.HomeListMode
import com.fueltracker.app.domain.model.StationSortMode
import com.fueltracker.app.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_settings"
)

private val fuelLabelKeys: Map<FuelType, Preferences.Key<String>> =
    FuelType.entries.associateWith { type ->
        stringPreferencesKey("fuel_label_${type.name}")
    }

@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore

    val autoSyncEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SYNC] ?: true
    }

    val dataRetentionDays: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_DATA_RETENTION_DAYS] ?: DEFAULT_RETENTION_DAYS
    }

    /** Vacío = todos los disponibles en la API están activos. */
    val enabledFuelTypes: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_ENABLED_FUEL_TYPES] ?: emptySet()
    }

    val stationSortMode: Flow<StationSortMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_STATION_SORT_MODE]) {
            StationSortMode.CUSTOM.name -> StationSortMode.CUSTOM
            else -> StationSortMode.BY_PRICE
        }
    }

    val customStationOrder: Flow<List<String>> = dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_STATION_ORDER]
            ?.split(ORDER_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    val homeListMode: Flow<HomeListMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_HOME_LIST_MODE]) {
            HomeListMode.TRAVEL.name -> HomeListMode.TRAVEL
            else -> HomeListMode.FAVORITES_NEAR_HOME
        }
    }

    val homeLocation: Flow<GeoLocation?> = dataStore.data.map { prefs ->
        val lat = prefs[KEY_HOME_LATITUDE]?.toDoubleOrNull()
        val lng = prefs[KEY_HOME_LONGITUDE]?.toDoubleOrNull()
        if (lat != null && lng != null) GeoLocation(lat, lng) else null
    }

    val homeRadiusKm: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_HOME_RADIUS_KM] ?: DEFAULT_RADIUS_KM
    }

    val travelRadiusKm: Flow<Int> = dataStore.data.map { prefs ->
        prefs[KEY_TRAVEL_RADIUS_KM] ?: DEFAULT_RADIUS_KM
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        when (prefs[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val customFuelLabels: Flow<Map<String, String>> = dataStore.data.map { prefs ->
        fuelLabelKeys.mapNotNull { (type, key) ->
            val label = prefs[key]?.trim().orEmpty()
            if (label.isEmpty()) null else type.name to label
        }.toMap()
    }

    suspend fun getAutoSyncEnabled(): Boolean = autoSyncEnabled.first()

    suspend fun getDataRetentionDays(): Int = dataRetentionDays.first()

    suspend fun getLastSyncEpochMs(): Long = dataStore.data.map { prefs ->
        prefs[KEY_LAST_SYNC_EPOCH_MS] ?: 0L
    }.first()

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_AUTO_SYNC] = enabled }
    }

    suspend fun setDataRetentionDays(days: Int) {
        dataStore.edit { prefs -> prefs[KEY_DATA_RETENTION_DAYS] = days }
    }

    suspend fun setLastSyncEpochMs(epochMs: Long) {
        dataStore.edit { prefs -> prefs[KEY_LAST_SYNC_EPOCH_MS] = epochMs }
    }

    suspend fun getEnabledFuelTypeNames(): Set<String> = enabledFuelTypes.first()

    suspend fun setEnabledFuelTypeNames(names: Set<String>) {
        dataStore.edit { prefs ->
            if (names.isEmpty()) {
                prefs.remove(KEY_ENABLED_FUEL_TYPES)
            } else {
                prefs[KEY_ENABLED_FUEL_TYPES] = names
            }
        }
    }

    suspend fun setStationSortMode(mode: StationSortMode) {
        dataStore.edit { prefs -> prefs[KEY_STATION_SORT_MODE] = mode.name }
    }

    suspend fun setCustomStationOrder(order: List<String>) {
        dataStore.edit { prefs ->
            if (order.isEmpty()) {
                prefs.remove(KEY_CUSTOM_STATION_ORDER)
            } else {
                prefs[KEY_CUSTOM_STATION_ORDER] = order.joinToString(ORDER_SEPARATOR)
            }
        }
    }

    suspend fun getHomeListMode(): HomeListMode = homeListMode.first()

    suspend fun getHomeLocation(): GeoLocation? = homeLocation.first()

    suspend fun getHomeRadiusKm(): Int = homeRadiusKm.first()

    suspend fun getTravelRadiusKm(): Int = travelRadiusKm.first()

    suspend fun getTravelProvinceCacheId(): String? = dataStore.data.map { prefs ->
        prefs[KEY_TRAVEL_PROVINCE_CACHE_ID]
    }.first()

    suspend fun getTravelProvinceCacheEpochMs(): Long = dataStore.data.map { prefs ->
        prefs[KEY_TRAVEL_PROVINCE_CACHE_EPOCH_MS] ?: 0L
    }.first()

    suspend fun setHomeListMode(mode: HomeListMode) {
        dataStore.edit { prefs -> prefs[KEY_HOME_LIST_MODE] = mode.name }
    }

    suspend fun setHomeLocation(location: GeoLocation?) {
        dataStore.edit { prefs ->
            if (location == null) {
                prefs.remove(KEY_HOME_LATITUDE)
                prefs.remove(KEY_HOME_LONGITUDE)
            } else {
                prefs[KEY_HOME_LATITUDE] = location.latitude.toString()
                prefs[KEY_HOME_LONGITUDE] = location.longitude.toString()
            }
        }
    }

    suspend fun setHomeRadiusKm(km: Int) {
        dataStore.edit { prefs -> prefs[KEY_HOME_RADIUS_KM] = km }
    }

    suspend fun setTravelRadiusKm(km: Int) {
        dataStore.edit { prefs -> prefs[KEY_TRAVEL_RADIUS_KM] = km }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setTravelProvinceCache(provinceId: String, epochMs: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_TRAVEL_PROVINCE_CACHE_ID] = provinceId
            prefs[KEY_TRAVEL_PROVINCE_CACHE_EPOCH_MS] = epochMs
        }
    }

    suspend fun setFuelTypeLabel(fuelType: FuelType, label: String) {
        dataStore.edit { prefs ->
            val trimmed = label.trim()
            val key = fuelLabelKeys.getValue(fuelType)
            if (trimmed.isEmpty() || trimmed == fuelType.displayName) {
                prefs.remove(key)
            } else {
                prefs[key] = trimmed
            }
        }
    }

    companion object {
        private val KEY_AUTO_SYNC = booleanPreferencesKey("auto_sync_enabled")
        private val KEY_DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
        private val KEY_LAST_SYNC_EPOCH_MS = longPreferencesKey("last_sync_epoch_ms")
        private val KEY_ENABLED_FUEL_TYPES = stringSetPreferencesKey("enabled_fuel_types")
        private val KEY_STATION_SORT_MODE = stringPreferencesKey("station_sort_mode")
        private val KEY_CUSTOM_STATION_ORDER = stringPreferencesKey("custom_station_order")
        private val KEY_HOME_LIST_MODE = stringPreferencesKey("home_list_mode")
        private val KEY_HOME_LATITUDE = stringPreferencesKey("home_latitude")
        private val KEY_HOME_LONGITUDE = stringPreferencesKey("home_longitude")
        private val KEY_HOME_RADIUS_KM = intPreferencesKey("home_radius_km")
        private val KEY_TRAVEL_RADIUS_KM = intPreferencesKey("travel_radius_km")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_TRAVEL_PROVINCE_CACHE_ID = stringPreferencesKey("travel_province_cache_id")
        private val KEY_TRAVEL_PROVINCE_CACHE_EPOCH_MS = longPreferencesKey("travel_province_cache_epoch_ms")
        private const val ORDER_SEPARATOR = ","
        const val DEFAULT_RETENTION_DAYS = 365
        const val DEFAULT_RADIUS_KM = 20
    }
}
