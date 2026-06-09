package com.fueltracker.app.data.local.dao

import androidx.room.ColumnInfo

data class StationFuelKey(
    @ColumnInfo(name = "stationId") val stationId: String,
    @ColumnInfo(name = "fuelType") val fuelType: String
)
