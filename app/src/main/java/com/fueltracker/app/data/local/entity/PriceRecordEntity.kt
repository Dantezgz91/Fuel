package com.fueltracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_records",
    foreignKeys = [
        ForeignKey(
            entity = GasStationEntity::class,
            parentColumns = ["id"],
            childColumns = ["stationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("stationId"), Index("fuelType"), Index("recordedAt")]
)
data class PriceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stationId: String,
    val fuelType: String,
    val price: Double,
    val recordedAt: Long
)
