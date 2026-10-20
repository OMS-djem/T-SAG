package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telecom_sites")
data class TelecomSiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siteId: String,
    val siteName: String,
    val operator: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val address: String = "",
    val region: String = "National",
    val siteType: String = "Rooftop Tower", // Rooftop Tower, Monopole, Greenfield, Mast, Indoor Microcell
    val accessStatus: String = "PENDING", // ACCORDED, REFUSED, PENDING
    val refusalReason: String? = null,
    val accessInstructions: String = "Access badge & ID required at security post",
    val contactPerson: String = "",
    val contactPhone: String = "",
    val lastAuditTimestamp: Long? = null,
    val assignedTechnicianEmail: String? = null
)
