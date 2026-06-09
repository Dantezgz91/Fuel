package com.fueltracker.app.domain.repository

import com.fueltracker.app.domain.model.GeoLocation
import com.fueltracker.app.domain.model.ReverseGeocodeResult

interface LocationRepository {
    fun hasLocationPermission(): Boolean
    suspend fun getCurrentLocation(): Result<GeoLocation>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<ReverseGeocodeResult>
}
