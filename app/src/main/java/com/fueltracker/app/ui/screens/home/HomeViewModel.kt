package com.fueltracker.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fueltracker.app.data.local.preferences.FuelTypeCatalog
import com.fueltracker.app.data.local.preferences.UserPreferences
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.FuelTypeConfig
import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.GeoLocation
import com.fueltracker.app.domain.model.HomeListMode
import com.fueltracker.app.domain.model.StationDisplayItem
import com.fueltracker.app.domain.model.StationRecommendation
import com.fueltracker.app.domain.model.StationSortMode
import com.fueltracker.app.domain.repository.GasStationRepository
import com.fueltracker.app.domain.repository.LocationRepository
import com.fueltracker.app.domain.repository.PriceRepository
import com.fueltracker.app.domain.usecase.GetStationRecommendationsUseCase
import com.fueltracker.app.domain.util.GeoUtils
import com.fueltracker.app.domain.util.ScheduleParser
import com.fueltracker.app.domain.util.StationOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val listMode: HomeListMode = HomeListMode.FAVORITES_NEAR_HOME,
    val displayStations: List<StationDisplayItem> = emptyList(),
    val visibleFuelTypes: List<FuelTypeConfig> = emptyList(),
    val selectedFuelType: FuelType = FuelType.GASOLINA_95,
    val cheapestStation: GasStation? = null,
    val cheapestPrice: Double? = null,
    val stationRecommendations: List<StationRecommendation> = emptyList(),
    val sortMode: StationSortMode = StationSortMode.BY_PRICE,
    val isReorderMode: Boolean = false,
    val homeConfigured: Boolean = false,
    val travelRadiusKm: Int = UserPreferences.DEFAULT_RADIUS_KM,
    val isLoadingTravel: Boolean = false,
    val hasTravelCache: Boolean = false,
    val locationMessage: String? = null,
    val needsLocationPermission: Boolean = false,
    val emptyDueToFuelFilter: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val gasStationRepository: GasStationRepository,
    private val priceRepository: PriceRepository,
    private val fuelTypeCatalog: FuelTypeCatalog,
    private val getStationRecommendationsUseCase: GetStationRecommendationsUseCase,
    private val userPreferences: UserPreferences,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val selectedFuelType = MutableStateFlow(FuelType.GASOLINA_95)

    private var favoritesCache = ListModeCache()
    private var travelCache: ListModeCache? = null
    private var cachedTravelRadiusKm: Int? = null

    init {
        observeFuelTypes()
        observeFavoriteStations()
        observeRecommendations()
        observeInitialListMode()
        observeTravelRadius()
    }

    private fun observeFuelTypes() {
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
    }

    private fun observeInitialListMode() {
        var initialized = false
        userPreferences.homeListMode
            .onEach { mode ->
                if (!initialized) {
                    initialized = true
                    applyListMode(mode, fromUser = false)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeTravelRadius() {
        userPreferences.travelRadiusKm
            .onEach { radius ->
                val radiusChanged = cachedTravelRadiusKm != null && cachedTravelRadiusKm != radius
                _uiState.update { it.copy(travelRadiusKm = radius) }
                if (radiusChanged) {
                    travelCache = null
                    cachedTravelRadiusKm = null
                    _uiState.update { it.copy(hasTravelCache = false) }
                    if (_uiState.value.listMode == HomeListMode.TRAVEL) {
                        loadTravelStations()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeFavoriteStations() {
        combine(
            gasStationRepository.getTrackedStations(),
            selectedFuelType,
            userPreferences.stationSortMode,
            userPreferences.customStationOrder,
            userPreferences.homeLocation
        ) { stations, fuelType, sortMode, customOrder, homeLocation ->
            FavoritesSnapshot(stations, fuelType, sortMode, customOrder, homeLocation)
        }.onEach { snapshot ->
            val stationsWithPrices = snapshot.stations.map { station ->
                val prices = priceRepository.getLatestPrices(station.id)
                station.copy(currentPrices = prices)
            }

            val withSelectedFuel = stationsWithPrices.filter { station ->
                station.currentPrices[snapshot.fuelType] != null
            }

            val sorted = StationOrder.sort(
                withSelectedFuel,
                snapshot.sortMode,
                snapshot.customOrder,
                snapshot.fuelType
            )

            val display = sorted.map { station ->
                toDisplayItem(station, snapshot.homeLocation)
            }

            val cheapestEntry = sorted
                .mapNotNull { s -> s.currentPrices[snapshot.fuelType]?.let { s to it } }
                .minByOrNull { it.second }

            favoritesCache = ListModeCache(
                displayStations = display,
                cheapestStation = cheapestEntry?.first,
                cheapestPrice = cheapestEntry?.second,
                sortMode = snapshot.sortMode,
                hadSourceStations = snapshot.stations.isNotEmpty()
            )

            val emptyDueToFuel = snapshot.stations.isNotEmpty() && display.isEmpty()

            if (_uiState.value.listMode == HomeListMode.FAVORITES_NEAR_HOME) {
                _uiState.update {
                    it.copy(
                        displayStations = display,
                        cheapestStation = cheapestEntry?.first,
                        cheapestPrice = cheapestEntry?.second,
                        sortMode = snapshot.sortMode,
                        homeConfigured = snapshot.homeLocation != null,
                        isLoadingTravel = false,
                        locationMessage = null,
                        emptyDueToFuelFilter = emptyDueToFuel
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun observeRecommendations() {
        combine(
            gasStationRepository.getTrackedStations(),
            selectedFuelType,
            _uiState
        ) { stations, fuelType, state ->
            val source = if (state.listMode == HomeListMode.TRAVEL) {
                state.displayStations.map { it.station }.filter { it.isTracked }
            } else {
                stations
            }
            source to fuelType
        }
            .flatMapLatest { (stations, fuelType) ->
                getStationRecommendationsUseCase(fuelType, stations)
            }
            .onEach { recommendations ->
                _uiState.update { it.copy(stationRecommendations = recommendations) }
            }
            .launchIn(viewModelScope)
    }

    fun setListMode(mode: HomeListMode) {
        if (mode == _uiState.value.listMode) return
        applyListMode(mode, fromUser = true)
    }

    fun refreshTravelStations() {
        if (_uiState.value.listMode != HomeListMode.TRAVEL) return
        loadTravelStations()
    }

    private fun applyListMode(mode: HomeListMode, fromUser: Boolean) {
        when (mode) {
            HomeListMode.FAVORITES_NEAR_HOME -> {
                _uiState.update {
                    it.copy(
                        listMode = mode,
                        displayStations = favoritesCache.displayStations,
                        cheapestStation = favoritesCache.cheapestStation,
                        cheapestPrice = favoritesCache.cheapestPrice,
                        sortMode = favoritesCache.sortMode,
                        isLoadingTravel = false,
                        isReorderMode = false,
                        locationMessage = null,
                        needsLocationPermission = false,
                        emptyDueToFuelFilter = favoritesCache.displayStations.isEmpty() &&
                            favoritesCache.hadSourceStations
                    )
                }
            }
            HomeListMode.TRAVEL -> {
                val cached = travelCache
                if (cached != null) {
                    val filtered = filterByFuelType(cached.displayStations, _uiState.value.selectedFuelType)
                    _uiState.update {
                        it.copy(
                            listMode = mode,
                            displayStations = filtered.displayStations,
                            cheapestStation = filtered.cheapestStation,
                            cheapestPrice = filtered.cheapestPrice,
                            isLoadingTravel = false,
                            isReorderMode = false,
                            hasTravelCache = true,
                            locationMessage = null,
                            needsLocationPermission = false,
                            emptyDueToFuelFilter = cached.displayStations.isNotEmpty() &&
                                filtered.displayStations.isEmpty()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            listMode = mode,
                            isReorderMode = false,
                            hasTravelCache = false
                        )
                    }
                    loadTravelStations()
                }
            }
        }

        if (fromUser) {
            viewModelScope.launch {
                userPreferences.setHomeListMode(mode)
            }
        }
    }

    fun loadTravelStations() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingTravel = true,
                    locationMessage = null,
                    needsLocationPermission = false
                )
            }

            if (!locationRepository.hasLocationPermission()) {
                _uiState.update {
                    it.copy(
                        isLoadingTravel = false,
                        needsLocationPermission = true,
                        locationMessage = "Activa la ubicación para buscar gasolineras cercanas."
                    )
                }
                return@launch
            }

            val locationResult = locationRepository.getCurrentLocation()
            locationResult.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingTravel = false,
                        locationMessage = error.message ?: "No se pudo obtener la ubicación."
                    )
                }
                return@launch
            }

            val location = locationResult.getOrThrow()
            val radius = userPreferences.getTravelRadiusKm().toDouble()
            val stationsResult = gasStationRepository.findStationsNear(
                location.latitude,
                location.longitude,
                radius
            )

            stationsResult.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingTravel = false,
                        locationMessage = error.message ?: "Error buscando gasolineras cercanas."
                    )
                }
                return@launch
            }

            val fuelType = _uiState.value.selectedFuelType
            val stationsWithPrices = stationsResult.getOrThrow().map { station ->
                val prices = priceRepository.getLatestPrices(station.id)
                station.copy(currentPrices = prices)
            }

            val allDisplay = stationsWithPrices.map { station ->
                toDisplayItem(station, location)
            }
            val filtered = filterByFuelType(allDisplay, fuelType)

            val cache = ListModeCache(
                displayStations = allDisplay,
                cheapestStation = filtered.cheapestStation,
                cheapestPrice = filtered.cheapestPrice
            )
            travelCache = cache
            cachedTravelRadiusKm = radius.toInt()

            _uiState.update {
                it.copy(
                    listMode = HomeListMode.TRAVEL,
                    displayStations = filtered.displayStations,
                    cheapestStation = filtered.cheapestStation,
                    cheapestPrice = filtered.cheapestPrice,
                    isLoadingTravel = false,
                    hasTravelCache = true,
                    emptyDueToFuelFilter = allDisplay.isNotEmpty() && filtered.displayStations.isEmpty(),
                    locationMessage = if (allDisplay.isEmpty()) {
                        "No hay gasolineras en un radio de ${radius.toInt()} km."
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(needsLocationPermission = false) }
        if (granted) {
            loadTravelStations()
        } else {
            _uiState.update {
                it.copy(locationMessage = "Sin permiso de ubicación no se puede usar el modo viaje.")
            }
        }
    }

    fun clearLocationMessage() {
        _uiState.update { it.copy(locationMessage = null) }
    }

    fun selectFuelType(fuelType: FuelType) {
        selectedFuelType.value = fuelType
        _uiState.update { it.copy(selectedFuelType = fuelType) }
        if (_uiState.value.listMode == HomeListMode.TRAVEL) {
            refreshTravelFuelPrices(fuelType)
        }
    }

    private fun refreshTravelFuelPrices(fuelType: FuelType) {
        val cached = travelCache ?: return
        viewModelScope.launch {
            val allDisplay = cached.displayStations.map { item ->
                val prices = priceRepository.getLatestPrices(item.station.id)
                item.copy(station = item.station.copy(currentPrices = prices))
            }
            val filtered = filterByFuelType(allDisplay, fuelType)
            travelCache = cached.copy(
                displayStations = allDisplay,
                cheapestStation = filtered.cheapestStation,
                cheapestPrice = filtered.cheapestPrice
            )

            if (_uiState.value.listMode == HomeListMode.TRAVEL) {
                _uiState.update {
                    it.copy(
                        displayStations = filtered.displayStations,
                        cheapestStation = filtered.cheapestStation,
                        cheapestPrice = filtered.cheapestPrice,
                        emptyDueToFuelFilter = allDisplay.isNotEmpty() && filtered.displayStations.isEmpty(),
                        locationMessage = null
                    )
                }
            }
        }
    }

    fun setSortMode(mode: StationSortMode) {
        if (mode == StationSortMode.BY_PRICE) {
            _uiState.update { it.copy(isReorderMode = false) }
        }
        viewModelScope.launch {
            userPreferences.setStationSortMode(mode)
        }
    }

    fun enterReorderMode() {
        if (_uiState.value.listMode != HomeListMode.FAVORITES_NEAR_HOME) return
        val current = _uiState.value
        _uiState.update { it.copy(isReorderMode = true) }
        viewModelScope.launch {
            val order = current.displayStations.map { it.station.id }
            userPreferences.setCustomStationOrder(order)
            userPreferences.setStationSortMode(StationSortMode.CUSTOM)
        }
    }

    fun exitReorderMode() {
        _uiState.update { it.copy(isReorderMode = false) }
    }

    fun moveStation(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.displayStations.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        val order = list.map { it.station.id }
        _uiState.update { it.copy(displayStations = list, sortMode = StationSortMode.CUSTOM) }
        favoritesCache = favoritesCache.copy(displayStations = list, sortMode = StationSortMode.CUSTOM)
        viewModelScope.launch {
            userPreferences.setCustomStationOrder(order)
            userPreferences.setStationSortMode(StationSortMode.CUSTOM)
        }
    }

    private fun toDisplayItem(station: GasStation, reference: GeoLocation?): StationDisplayItem {
        val distanceKm = if (reference != null && GeoUtils.isValidCoordinate(station.latitude, station.longitude)) {
            GeoUtils.distanceKm(
                reference.latitude,
                reference.longitude,
                station.latitude,
                station.longitude
            )
        } else {
            null
        }
        return StationDisplayItem(
            station = station,
            distanceKm = distanceKm,
            openStatus = ScheduleParser.openStatus(station.schedule)
        )
    }

    private fun filterByFuelType(
        items: List<StationDisplayItem>,
        fuelType: FuelType
    ): FilteredStations {
        val filtered = items.filter { item -> item.station.currentPrices[fuelType] != null }
        val cheapest = filtered
            .mapNotNull { item ->
                item.station.currentPrices[fuelType]?.let { price -> item.station to price }
            }
            .minByOrNull { it.second }
        return FilteredStations(
            displayStations = filtered,
            cheapestStation = cheapest?.first,
            cheapestPrice = cheapest?.second
        )
    }

    private fun resolveSelectedFuelType(
        current: FuelType,
        visible: List<FuelTypeConfig>
    ): FuelType {
        if (visible.isEmpty()) return current
        if (visible.any { it.type == current }) return current
        return visible.first().type
    }

    private data class FilteredStations(
        val displayStations: List<StationDisplayItem>,
        val cheapestStation: GasStation?,
        val cheapestPrice: Double?
    )

    private data class ListModeCache(
        val displayStations: List<StationDisplayItem> = emptyList(),
        val cheapestStation: GasStation? = null,
        val cheapestPrice: Double? = null,
        val sortMode: StationSortMode = StationSortMode.BY_PRICE,
        val hadSourceStations: Boolean = false
    )

    private data class FavoritesSnapshot(
        val stations: List<GasStation>,
        val fuelType: FuelType,
        val sortMode: StationSortMode,
        val customOrder: List<String>,
        val homeLocation: GeoLocation?
    )
}
