package com.example.data.model

enum class SiteAccessStatus(val label: String, val code: String) {
    ACCORDED("Access Accorded", "ACCORDED"),
    REFUSED("No Access", "REFUSED")
}

data class SitePhoto(
    val id: String,
    val filePath: String,
    val tag: String = "Site Entrance",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cellId: String = ""
)
