package com.fueltracker.app.domain.repository

import com.fueltracker.app.domain.model.FuelType
import com.fueltracker.app.domain.model.PriceRecord
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

interface PriceRepository {
    fun getPriceHistory(stationId: String, fuelType: FuelType): Flow<List<PriceRecord>>
    fun getAllPriceHistory(fuelType: FuelType): Flow<List<PriceRecord>>
    suspend fun savePriceRecords(records: List<PriceRecord>)
    suspend fun getLatestPrices(stationId: String): Map<FuelType, Double>
    suspend fun deleteOldRecords(before: LocalDateTime): Int
    suspend fun deleteRecordsOutsideRetention(retentionDays: Int): Int
    suspend fun deleteAllPriceRecords(): Int
    fun observeAvailableFuelTypes(): Flow<Set<FuelType>>
}
