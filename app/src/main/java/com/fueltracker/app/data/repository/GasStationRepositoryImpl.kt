package com.fueltracker.app.data.repository

import com.fueltracker.app.data.local.dao.GasStationDao
import com.fueltracker.app.data.local.dao.PriceRecordDao
import com.fueltracker.app.data.local.entity.GasStationEntity
import com.fueltracker.app.data.local.entity.PriceRecordEntity
import com.fueltracker.app.data.local.preferences.UserPreferences
import com.fueltracker.app.data.remote.MitecoApi
import com.fueltracker.app.data.remote.dto.MunicipalityDto
import com.fueltracker.app.data.remote.dto.ProvinceDto
import com.fueltracker.app.data.remote.dto.StationDto
import com.fueltracker.app.data.remote.dto.extractPrice
import com.fueltracker.app.data.util.GeographicNameMatcher
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.Municipality
import com.fueltracker.app.domain.repository.GasStationRepository
import com.fueltracker.app.domain.repository.LocationRepository
import com.fueltracker.app.domain.util.GeoUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class GasStationRepositoryImpl @Inject constructor(
    private val gasStationDao: GasStationDao,
    private val priceRecordDao: PriceRecordDao,
    private val mitecoApi: MitecoApi,
    private val userPreferences: UserPreferences,
    private val locationRepository: LocationRepository
) : GasStationRepository {

    private var cachedProvinces: List<ProvinceDto>? = null
    private var cachedMunicipalities: List<MunicipalityDto>? = null

    override fun getTrackedStations(): Flow<List<GasStation>> {
        return gasStationDao.getTrackedStations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchStationsByMunicipality(municipalityId: String): Result<List<GasStation>> {
        return try {
            val response = mitecoApi.getStationsByMunicipality(municipalityId)
            cacheStationsFromResponse(response.stations, municipalityId)
            val existing = gasStationDao.getStationsByMunicipality(municipalityId)
            Result.success(existing.map { it.toDomain() }.distinctBy { it.id })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun trackStation(stationId: String) {
        gasStationDao.trackStation(stationId, System.currentTimeMillis())
    }

    override suspend fun untrackStation(stationId: String) {
        gasStationDao.untrackStation(stationId)
    }

    override suspend fun getMunicipalities(): Result<List<Municipality>> {
        return try {
            val dtos = loadMunicipalities()
            Result.success(dtos.map {
                Municipality(
                    id = it.id,
                    name = it.name,
                    province = it.province,
                    autonomousCommunity = it.autonomousCommunity
                )
            })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshPricesForTrackedStations() {
        val trackedStations = gasStationDao.getTrackedStationsList()
        val municipalityIds = trackedStations.map { it.municipalityId }.distinct()
        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

        for (municipalityId in municipalityIds) {
            try {
                val response = mitecoApi.getStationsByMunicipality(municipalityId)
                cacheStationsFromResponse(response.stations, municipalityId)
                val trackedIds = trackedStations
                    .filter { it.municipalityId == municipalityId }
                    .map { it.id }
                    .toSet()
                val priceRecords = response.stations
                    .filter { it.id in trackedIds }
                    .flatMap { it.toPriceRecords(now) }
                insertNewDailyPriceRecords(priceRecords)
            } catch (_: Exception) { }
        }
    }

    override suspend fun findStationsNear(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Result<List<GasStation>> {
        return try {
            val geocode = locationRepository.reverseGeocode(latitude, longitude).getOrElse { error ->
                return Result.failure(error)
            }

            val provinces = loadProvinces()
            if (provinces.isEmpty()) {
                return Result.failure(Exception("No se pudo cargar el listado de provincias del MITECO"))
            }

            val municipalities = loadMunicipalities()
            val resolved = GeographicNameMatcher.resolveLocation(geocode, provinces, municipalities)
                ?: return Result.failure(
                    Exception(
                        "No se pudo identificar la zona (${geocode.locality ?: geocode.province ?: geocode.featureName ?: "desconocida"})"
                    )
                )

            val provinceId = resolved.provinceId
            val municipalityId = resolved.municipalityId

            val trackedIds = gasStationDao.getTrackedStationsList().map { it.id }.toSet()
            val candidates = linkedMapOf<String, GasStationEntity>()

            if (municipalityId != null) {
                val municipalityResponse = mitecoApi.getStationsByMunicipality(municipalityId)
                cacheStationsFromResponse(municipalityResponse.stations, municipalityId)
                gasStationDao.getStationsByMunicipality(municipalityId).forEach { entity ->
                    candidates[entity.id] = entity
                }
            }

            val municipalityMatches = filterEntitiesWithinRadius(
                candidates.values.toList(),
                latitude,
                longitude,
                radiusKm
            )

            if (municipalityMatches.isEmpty()) {
                ensureProvinceStationsLoaded(provinceId).forEach { entity ->
                    candidates[entity.id] = entity
                }
            }

            val stations = filterEntitiesWithinRadius(
                candidates.values.toList(),
                latitude,
                longitude,
                radiusKm
            ).map { entity ->
                entity.toDomain().copy(isTracked = entity.id in trackedIds)
            }

            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun ensureProvinceStationsLoaded(provinceId: String): List<GasStationEntity> {
        val provinceName = loadProvinces().firstOrNull { it.id == provinceId }?.name
            ?: return emptyList()
        val normalizedProvince = GeographicNameMatcher.normalize(provinceName)

        val cachedProvinceId = userPreferences.getTravelProvinceCacheId()
        val cachedAt = userPreferences.getTravelProvinceCacheEpochMs()
        val cacheFresh = cachedProvinceId == provinceId &&
            System.currentTimeMillis() - cachedAt < PROVINCE_CACHE_MAX_AGE_MS

        if (cacheFresh) {
            val cached = gasStationDao.getAllStations()
                .filter { GeographicNameMatcher.normalize(it.province) == normalizedProvince }
            if (cached.isNotEmpty()) return cached
        }

        val response = mitecoApi.getStationsByProvince(provinceId)
        cacheStationsFromResponse(response.stations, fallbackMunicipalityId = "")
        userPreferences.setTravelProvinceCache(provinceId, System.currentTimeMillis())

        return gasStationDao.getAllStations()
            .filter { GeographicNameMatcher.normalize(it.province) == normalizedProvince }
    }

    private fun filterEntitiesWithinRadius(
        stations: List<GasStationEntity>,
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<GasStationEntity> {
        return stations
            .filter { GeoUtils.isValidCoordinate(it.latitude, it.longitude) }
            .map { entity ->
                entity to GeoUtils.distanceKm(latitude, longitude, entity.latitude, entity.longitude)
            }
            .filter { (_, distance) -> distance <= radiusKm }
            .sortedBy { it.second }
            .map { it.first }
    }

    private suspend fun loadProvinces(): List<ProvinceDto> {
        cachedProvinces?.let { return it }
        val provinces = mitecoApi.getProvinces()
        cachedProvinces = provinces
        return provinces
    }

    private suspend fun loadMunicipalities(): List<MunicipalityDto> {
        cachedMunicipalities?.let { return it }
        val municipalities = mitecoApi.getMunicipalities()
        cachedMunicipalities = municipalities
        return municipalities
    }

    private suspend fun cacheStationsFromResponse(
        stations: List<StationDto>,
        fallbackMunicipalityId: String
    ) {
        if (stations.isEmpty()) return

        val stationIds = stations.mapNotNull { it.id?.trim()?.takeIf { id -> id.isNotEmpty() } }
        val existingById = if (stationIds.isEmpty()) {
            emptyMap()
        } else {
            gasStationDao.getStationsByIds(stationIds).associateBy { it.id }
        }

        val entities = stations.mapNotNull { dto ->
            val municipalityId = dto.municipalityId?.trim()?.takeIf { it.isNotEmpty() }
                ?: fallbackMunicipalityId
            val entity = dto.toEntity(municipalityId) ?: return@mapNotNull null
            val existing = existingById[entity.id]
            if (existing != null) {
                entity.copy(
                    isTracked = existing.isTracked,
                    trackedAt = existing.trackedAt,
                    schedule = entity.schedule ?: existing.schedule
                )
            } else {
                entity
            }
        }
        gasStationDao.insertStations(entities)

        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val priceRecords = stations.flatMap { it.toPriceRecords(now) }
        insertNewDailyPriceRecords(priceRecords)
    }

    private suspend fun insertNewDailyPriceRecords(records: List<PriceRecordEntity>) {
        val newRecords = filterRecordsWithoutTodaySnapshot(records)
        if (newRecords.isNotEmpty()) {
            priceRecordDao.insertPriceRecords(newRecords)
        }
    }

    private suspend fun filterRecordsWithoutTodaySnapshot(
        records: List<PriceRecordEntity>
    ): List<PriceRecordEntity> {
        if (records.isEmpty()) return emptyList()
        val dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        val dayEnd = dayStart + 86_400
        val existing = priceRecordDao.getExistingStationFuelsForDay(dayStart, dayEnd)
            .map { it.stationId to it.fuelType }
            .toSet()
        return records.filter { (it.stationId to it.fuelType) !in existing }
    }

    private fun GasStationEntity.toDomain() = GasStation(
        id = id,
        name = name,
        address = address,
        locality = locality,
        municipality = municipality,
        province = province,
        latitude = latitude,
        longitude = longitude,
        schedule = schedule,
        isTracked = isTracked
    )

    private fun StationDto.toEntity(municipalityId: String): GasStationEntity? {
        val stationId = id?.trim().orEmpty()
        if (stationId.isEmpty()) return null
        return GasStationEntity(
            id = stationId,
            name = name.orEmpty().ifBlank { "Gasolinera" },
            address = address.orEmpty(),
            locality = locality.orEmpty(),
            municipality = municipality.orEmpty(),
            province = province.orEmpty(),
            latitude = latitude.orEmpty().replace(",", ".").toDoubleOrNull() ?: 0.0,
            longitude = longitude.orEmpty().replace(",", ".").toDoubleOrNull() ?: 0.0,
            municipalityId = municipalityId,
            schedule = schedule.orEmpty().ifBlank { null }
        )
    }

    private fun StationDto.toPriceRecords(timestamp: Long): List<PriceRecordEntity> {
        val stationId = id?.trim().orEmpty()
        if (stationId.isEmpty()) return emptyList()
        val records = mutableListOf<PriceRecordEntity>()
        fun addIfValid(price: String?, fuelType: FuelType) {
            val p = price.orEmpty().replace(",", ".").toDoubleOrNull()
            if (p != null && p > 0) {
                records.add(
                    PriceRecordEntity(
                        stationId = stationId,
                        fuelType = fuelType.name,
                        price = p,
                        recordedAt = timestamp
                    )
                )
            }
        }
        FuelType.entries.forEach { fuelType ->
            addIfValid(fuelType.extractPrice(this), fuelType)
        }
        return records
    }

    companion object {
        private const val PROVINCE_CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
