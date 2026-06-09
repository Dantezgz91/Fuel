package com.fueltracker.app.domain.model

data class ReverseGeocodeResult(
    val locality: String?,
    val province: String?,
    val autonomousCommunity: String?,
    val featureName: String? = null
)
