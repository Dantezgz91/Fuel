package com.fueltracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fueltracker.app.data.local.entity.GasStationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GasStationDao {
    @Query("SELECT * FROM gas_stations WHERE isTracked = 1 ORDER BY trackedAt DESC")
    fun getTrackedStations(): Flow<List<GasStationEntity>>

    @Query("SELECT * FROM gas_stations WHERE isTracked = 1")
    suspend fun getTrackedStationsList(): List<GasStationEntity>

    @Query("SELECT * FROM gas_stations WHERE municipalityId = :municipalityId")
    suspend fun getStationsByMunicipality(municipalityId: String): List<GasStationEntity>

    @Query("SELECT * FROM gas_stations")
    suspend fun getAllStations(): List<GasStationEntity>

    @Query("SELECT COUNT(*) FROM gas_stations")
    suspend fun countStations(): Int

    @Query("SELECT * FROM gas_stations WHERE id = :id")
    suspend fun getStationById(id: String): GasStationEntity?

    @Query("SELECT * FROM gas_stations WHERE id IN (:ids)")
    suspend fun getStationsByIds(ids: List<String>): List<GasStationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStations(stations: List<GasStationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station: GasStationEntity)

    @Query("UPDATE gas_stations SET isTracked = 1, trackedAt = :timestamp WHERE id = :id")
    suspend fun trackStation(id: String, timestamp: Long)

    @Query("UPDATE gas_stations SET isTracked = 0, trackedAt = NULL WHERE id = :id")
    suspend fun untrackStation(id: String)
}
