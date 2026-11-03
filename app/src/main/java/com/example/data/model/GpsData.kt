package com.example.data.model

import java.util.Locale

data class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val speed: Float = 0.0f,
    val provider: String = "Google Play Services Location API",
    val timestamp: Long = System.currentTimeMillis(),
    val address: String = "Detecting location..."
) {
    val isValid: Boolean get() = latitude != 0.0 || longitude != 0.0
    val coordinatesString: String get() = String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
    val formattedLatitude: String get() = String.format(Locale.US, "%.6f° %s", Math.abs(latitude), if (latitude >= 0) "N" else "S")
    val formattedLongitude: String get() = String.format(Locale.US, "%.6f° %s", Math.abs(longitude), if (longitude >= 0) "E" else "W")
    val mapsUrl: String get() = "https://maps.google.com/?q=$latitude,$longitude"
    val geoUriString: String get() = "geo:$latitude,$longitude?q=$latitude,$longitude(Site+Location)"
}
