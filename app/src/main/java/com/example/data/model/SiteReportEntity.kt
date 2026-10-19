package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_reports")
data class SiteReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siteId: String,
    val siteName: String,
    val operator: String,
    val status: String, // ACCORDED or REFUSED
    val refusalReason: String?,
    val technicianName: String,
    val technicianPhone: String,
    val recipientEmail: String,
    val notes: String,
    // GPS
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val address: String,
    // GSM
    val gsmOperator: String,
    val networkType: String,
    val mcc: String,
    val mnc: String,
    val lac: String,
    val cellId: String,
    val signalDbm: Int,
    // Photos stored as pipe or JSON separated string
    val photoPaths: String,
    val createdAt: Long = System.currentTimeMillis(),

    // Offline Storage Queue & Auto-Sync fields
    val syncStatus: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val syncErrorMessage: String? = null,
    val syncedAt: Long? = null
)
