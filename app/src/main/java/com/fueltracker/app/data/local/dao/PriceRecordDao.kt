package com.fueltracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueltracker.app.data.local.entity.PriceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceRecordDao {
    @Query("SELECT * FROM price_records WHERE stationId = :stationId AND fuelType = :fuelType ORDER BY recordedAt ASC")
    fun getPriceHistory(stationId: String, fuelType: String): Flow<List<PriceRecordEntity>>

    @Query("SELECT * FROM price_records WHERE fuelType = :fuelType ORDER BY recordedAt ASC")
    fun getAllPriceHistory(fuelType: String): Flow<List<PriceRecordEntity>>

    @Query("SELECT * FROM price_records WHERE stationId = :stationId ORDER BY recordedAt DESC")
    suspend fun getLatestPricesForStation(stationId: String): List<PriceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecords(records: List<PriceRecordEntity>)

    @Query("DELETE FROM price_records WHERE recordedAt < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long): Int

    @Query("DELETE FROM price_records")
    suspend fun deleteAllPriceRecords(): Int

    @Query("SELECT COUNT(*) FROM price_records WHERE recordedAt < :timestamp")
    suspend fun countRecordsOlderThan(timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM price_records WHERE stationId = :stationId AND fuelType = :fuelType AND recordedAt > :since")
    suspend fun countRecentRecords(stationId: String, fuelType: String, since: Long): Int

    @Query(
        """
        SELECT DISTINCT stationId, fuelType FROM price_records
        WHERE recordedAt >= :dayStart AND recordedAt < :dayEnd
        """
    )
    suspend fun getExistingStationFuelsForDay(dayStart: Long, dayEnd: Long): List<StationFuelKey>

    @Query("SELECT DISTINCT fuelType FROM price_records")
    fun observeDistinctFuelTypes(): Flow<List<String>>
}
