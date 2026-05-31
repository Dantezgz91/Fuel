package com.fueltracker.app.domain.model

import java.time.LocalDateTime

data class PriceRecord(
    val id: Long = 0,
    val stationId: String,
    val fuelType: FuelType,
    val price: Double,
    val recordedAt: LocalDateTime
)
