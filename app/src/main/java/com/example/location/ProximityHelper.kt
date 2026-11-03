package com.example.location

import android.location.Location
import kotlin.math.roundToInt

data class ProximityInfo(
    val distanceMeters: Float,
    val formattedDistance: String,
    val bearingDegrees: Float,
    val cardinalDirection: String,
    val proximityLevel: ProximityLevel,
    val etaDescription: String
)

enum class ProximityLevel(val label: String) {
    IMMEDIATE("On Site (< 50m)"),
    NEARBY("Walking Distance (< 500m)"),
    COMMUTE("Short Drive (< 5km)"),
    REGIONAL("Regional (> 5km)")
}

object ProximityHelper {

    fun calculateProximity(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): ProximityInfo {
        val results = FloatArray(2)
        Location.distanceBetween(fromLat, fromLng, toLat, toLng, results)
        val distance = results[0]
        val bearing = (results[1] + 360f) % 360f

        val formattedDistance = if (distance < 1000f) {
            "${distance.roundToInt()} m"
        } else {
            "%.1f km".format(distance / 1000f)
        }

        val cardinal = when {
            bearing >= 337.5 || bearing < 22.5 -> "North (N)"
            bearing >= 22.5 && bearing < 67.5 -> "North-East (NE)"
            bearing >= 67.5 && bearing < 112.5 -> "East (E)"
            bearing >= 112.5 && bearing < 157.5 -> "South-East (SE)"
            bearing >= 157.5 && bearing < 202.5 -> "South (S)"
            bearing >= 202.5 && bearing < 247.5 -> "South-West (SW)"
            bearing >= 247.5 && bearing < 292.5 -> "West (W)"
            else -> "North-West (NW)"
        }

        val level = when {
            distance < 50f -> ProximityLevel.IMMEDIATE
            distance < 500f -> ProximityLevel.NEARBY
            distance < 5000f -> ProximityLevel.COMMUTE
            else -> ProximityLevel.REGIONAL
        }

        val eta = when (level) {
            ProximityLevel.IMMEDIATE -> "Within site perimeter"
            ProximityLevel.NEARBY -> "~${(distance / 80).roundToInt().coerceAtLeast(1)} min walk"
            ProximityLevel.COMMUTE -> "~${(distance / 500).roundToInt().coerceAtLeast(1)} min drive"
            ProximityLevel.REGIONAL -> "~${(distance / 1000).roundToInt().coerceAtLeast(1)} h drive"
        }

        return ProximityInfo(
            distanceMeters = distance,
            formattedDistance = formattedDistance,
            bearingDegrees = bearing,
            cardinalDirection = cardinal,
            proximityLevel = level,
            etaDescription = eta
        )
    }
}
