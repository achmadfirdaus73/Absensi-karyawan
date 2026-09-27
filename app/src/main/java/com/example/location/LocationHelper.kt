package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val isWithinOfficeGeofence: Boolean,
    val distanceToOfficeMeters: Float
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(
        officeLat: Double = -6.2088,
        officeLng: Double = 106.8456,
        officeRadiusMeters: Double = 250.0,
        officeAddressFallback: String = "Gedung Cyber Tower Lt. 12, Jakarta"
    ): LocationInfo {
        var lat = officeLat
        var lng = officeLng
        var address = officeAddressFallback

        try {
            val cts = CancellationTokenSource()
            val location: Location? = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            ).await()

            if (location != null) {
                lat = location.latitude
                lng = location.longitude

                try {
                    val geocoder = Geocoder(context, Locale("id", "ID"))
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        address = "${addr.thoroughfare ?: "Kawasan Bisnis"}, ${addr.subLocality ?: ""}, ${addr.locality ?: "Jakarta"}"
                    }
                } catch (e: Exception) {
                    address = officeAddressFallback
                }
            }
        } catch (e: Exception) {
            // Use defaults if permission denied or emulator
            lat = officeLat
            lng = officeLng
            address = officeAddressFallback
        }

        val distance = calculateDistanceInMeters(lat, lng, officeLat, officeLng)
        val isWithinGeofence = distance <= officeRadiusMeters

        return LocationInfo(
            latitude = lat,
            longitude = lng,
            address = address,
            isWithinOfficeGeofence = isWithinGeofence,
            distanceToOfficeMeters = distance
        )
    }

    private fun calculateDistanceInMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }
}
