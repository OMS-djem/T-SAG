package com.example.data.model

data class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis(),
    val address: String = "Detecting location..."
) {
    val isValid: Boolean get() = latitude != 0.0 || longitude != 0.0
    val coordinatesString: String get() = String.format("%.6f, %.6f", latitude, longitude)
    val mapsUrl: String get() = "https://maps.google.com/?q=$latitude,$longitude"
}
