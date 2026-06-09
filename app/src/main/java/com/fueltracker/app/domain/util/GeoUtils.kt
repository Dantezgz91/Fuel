package com.fueltracker.app.domain.util

import com.fueltracker.app.domain.model.GeoLocation
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Locale

object GeoUtils {

    private const val EARTH_RADIUS_KM = 6371.0

    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    fun distanceKm(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): Double {
        val dLat = Math.toRadians(toLat - fromLat)
        val dLng = Math.toRadians(toLng - fromLng)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(fromLat)) * cos(Math.toRadians(toLat)) * sin(dLng / 2).pow(2)
        return EARTH_RADIUS_KM * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun distanceKm(from: GeoLocation, toLat: Double, toLng: Double): Double {
        return distanceKm(from.latitude, from.longitude, toLat, toLng)
    }

    fun formatDistanceKm(km: Double): String {
        return if (km < 1.0) {
            "${(km * 1000).toInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", km)
        }
    }
}
