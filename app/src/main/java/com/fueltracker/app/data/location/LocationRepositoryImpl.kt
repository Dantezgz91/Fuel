package com.fueltracker.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.location.Geocoder
import android.os.Build
import com.fueltracker.app.domain.model.GeoLocation
import com.fueltracker.app.domain.model.ReverseGeocodeResult
import com.fueltracker.app.domain.repository.LocationRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : LocationRepository {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    override fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<GeoLocation> {
        if (!hasLocationPermission()) {
            return Result.failure(SecurityException("Sin permiso de ubicación"))
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationToken = CancellationTokenSource()
            continuation.invokeOnCancellation { cancellationToken.cancel() }

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(
                        Result.success(GeoLocation(location.latitude, location.longitude))
                    )
                } else {
                    fusedClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            continuation.resume(
                                Result.success(GeoLocation(lastLocation.latitude, lastLocation.longitude))
                            )
                        } else {
                            continuation.resume(
                                Result.failure(Exception("No se pudo obtener la ubicación"))
                            )
                        }
                    }.addOnFailureListener { error ->
                        continuation.resume(Result.failure(error))
                    }
                }
            }.addOnFailureListener { error ->
                continuation.resume(Result.failure(error))
            }
        }
    }

    override suspend fun reverseGeocode(
        latitude: Double,
        longitude: Double
    ): Result<ReverseGeocodeResult> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext Result.failure(Exception("Geocodificación no disponible en este dispositivo"))
        }

        try {
            val geocoder = Geocoder(context, Locale.forLanguageTag("es-ES"))
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                        continuation.resume(addresses.firstOrNull())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }

            if (address == null) {
                Result.failure(Exception("No se pudo determinar el municipio o provincia"))
            } else {
                val subAdmin = address.subAdminArea
                val admin = address.adminArea
                Result.success(
                    ReverseGeocodeResult(
                        locality = address.locality ?: address.subLocality,
                        province = subAdmin ?: admin,
                        autonomousCommunity = if (subAdmin != null) admin else null,
                        featureName = address.featureName
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
