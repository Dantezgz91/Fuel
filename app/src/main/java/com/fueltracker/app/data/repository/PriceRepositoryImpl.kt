package com.fueltracker.app.data.repository

import com.fueltracker.app.data.local.dao.PriceRecordDao
import com.fueltracker.app.data.local.entity.PriceRecordEntity
import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.PriceRecord
import com.fueltracker.app.domain.repository.PriceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

class PriceRepositoryImpl @Inject constructor(
    private val priceRecordDao: PriceRecordDao
) : PriceRepository {

    override fun getPriceHistory(stationId: String, fuelType: FuelType): Flow<List<PriceRecord>> {
        return priceRecordDao.getPriceHistory(stationId, fuelType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllPriceHistory(fuelType: FuelType): Flow<List<PriceRecord>> {
        return priceRecordDao.getAllPriceHistory(fuelType.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun savePriceRecords(records: List<PriceRecord>) {
        priceRecordDao.insertPriceRecords(records.map { it.toEntity() })
    }

    override suspend fun getLatestPrices(stationId: String): Map<FuelType, Double> {
        val records = priceRecordDao.getLatestPricesForStation(stationId)
        return records
            .groupBy { it.fuelType }
            .mapNotNull { (fuelTypeStr, recs) ->
                val fuelType = FuelType.entries.find { it.name == fuelTypeStr } ?: return@mapNotNull null
                val latest = recs.maxByOrNull { it.recordedAt } ?: return@mapNotNull null
                fuelType to latest.price
            }
            .toMap()
    }

    override suspend fun deleteOldRecords(before: LocalDateTime) {
        val timestamp = before.toEpochSecond(ZoneOffset.UTC)
        priceRecordDao.deleteOldRecords(timestamp)
    }

    private fun PriceRecordEntity.toDomain() = PriceRecord(
        id = id,
        stationId = stationId,
        fuelType = FuelType.entries.find { it.name == fuelType } ?: FuelType.GASOLINA_95,
        price = price,
        recordedAt = LocalDateTime.ofEpochSecond(recordedAt, 0, ZoneOffset.UTC)
    )

    private fun PriceRecord.toEntity() = PriceRecordEntity(
        id = id,
        stationId = stationId,
        fuelType = fuelType.name,
        price = price,
        recordedAt = recordedAt.toEpochSecond(ZoneOffset.UTC)
    )
}
