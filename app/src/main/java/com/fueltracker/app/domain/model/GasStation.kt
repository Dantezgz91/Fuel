package com.fueltracker.app.domain.model

data class GasStation(
    val id: String,
    val name: String,
    val address: String,
    val locality: String,
    val municipality: String,
    val province: String,
    val latitude: Double,
    val longitude: Double,
    val schedule: String? = null,
    val isTracked: Boolean = false,
    val currentPrices: Map<FuelType, Double> = emptyMap()
)
