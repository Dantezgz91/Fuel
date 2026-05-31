package com.fueltracker.app.data.repository

import com.fueltracker.app.data.local.dao.GasStationDao
import com.fueltracker.app.data.local.dao.PriceRecordDao
import com.fueltracker.app.data.local.entity.GasStationEntity
import com.fueltracker.app.data.local.entity.PriceRecordEntity
import com.fueltracker.app.data.remote.MitecoApi
import com.fueltracker.app.data.remote.dto.StationDto
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.GasStation
import com.fueltracker.app.domain.model.Municipality
import com.fueltracker.app.domain.repository.GasStationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class GasStationRepositoryImpl @Inject constructor(
    private val gasStationDao: GasStationDao,
    private val priceRecordDao: PriceRecordDao,
    private val mitecoApi: MitecoApi
) : GasStationRepository {

    override fun getTrackedStations(): Flow<List<GasStation>> {
        return gasStationDao.getTrackedStations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun searchStationsByMunicipality(municipalityId: String): Result<List<GasStation>> {
        return try {
            val response = mitecoApi.getStationsByMunicipality(municipalityId)
            val entities = response.stations.map { dto -> dto.toEntity(municipalityId) }
            gasStationDao.insertStations(entities)

            val now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
            val priceRecords = response.stations.flatMap { dto ->
                dto.toPriceRecords(now)
            }
            if (priceRecords.isNotEmpty()) {
                priceRecordDao.insertPriceRecords(priceRecords)
            }

            val existing = gasStationDao.getStationsByMunicipality(municipalityId)
            Result.success(existing.map { it.toDomain() })
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
            val dtos = mitecoApi.getMunicipalities()
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
                val trackedIds = trackedStations.filter { it.municipalityId == municipalityId }.map { it.id }.toSet()
                val priceRecords = response.stations
                    .filter { it.id in trackedIds }
                    .flatMap { it.toPriceRecords(now) }
                if (priceRecords.isNotEmpty()) {
                    priceRecordDao.insertPriceRecords(priceRecords)
                }
            } catch (_: Exception) { }
        }
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
        isTracked = isTracked
    )

    private fun StationDto.toEntity(municipalityId: String) = GasStationEntity(
        id = id,
        name = name.ifBlank { "Gasolinera" },
        address = address,
        locality = locality,
        municipality = municipality,
        province = province,
        latitude = latitude.replace(",", ".").toDoubleOrNull() ?: 0.0,
        longitude = longitude.replace(",", ".").toDoubleOrNull() ?: 0.0,
        municipalityId = municipalityId
    )

    private fun StationDto.toPriceRecords(timestamp: Long): List<PriceRecordEntity> {
        val records = mutableListOf<PriceRecordEntity>()
        fun addIfValid(price: String, fuelType: FuelType) {
            val p = price.replace(",", ".").toDoubleOrNull()
            if (p != null && p > 0) {
                records.add(PriceRecordEntity(stationId = id, fuelType = fuelType.name, price = p, recordedAt = timestamp))
            }
        }
        addIfValid(priceGasolina95, FuelType.GASOLINA_95)
        addIfValid(priceGasolina98, FuelType.GASOLINA_98)
        addIfValid(priceGasoilA, FuelType.GASOIL_A)
        addIfValid(priceGasoilPremium, FuelType.GASOIL_PREMIUM)
        addIfValid(priceGlp, FuelType.GLP)
        return records
    }
}
