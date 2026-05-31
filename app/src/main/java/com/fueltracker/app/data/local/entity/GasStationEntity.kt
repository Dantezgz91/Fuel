package com.fueltracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gas_stations")
data class GasStationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val locality: String,
    val municipality: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    val municipalityId: String,
    val isTracked: Boolean = false,
    val trackedAt: Long? = null
)
