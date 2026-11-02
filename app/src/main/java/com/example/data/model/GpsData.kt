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

    fun getGoogleMapsStaticUrl(
        zoom: Int = 16,
        width: Int = 600,
        height: Int = 320,
        mapType: String = "roadmap",
        apiKey: String = ""
    ): String {
        val latStr = String.format(Locale.US, "%.6f", latitude)
        val lngStr = String.format(Locale.US, "%.6f", longitude)
        val params = StringBuilder()
        params.append("center=$latStr,$lngStr")
        params.append("&zoom=$zoom")
        params.append("&size=${width}x${height}")
        params.append("&scale=2")
        params.append("&maptype=$mapType")
        params.append("&markers=color:red%7Clabel:S%7C$latStr,$lngStr")
        if (apiKey.isNotBlank()) {
            params.append("&key=$apiKey")
        }
        return "https://maps.googleapis.com/maps/api/staticmap?$params"
    }
}
