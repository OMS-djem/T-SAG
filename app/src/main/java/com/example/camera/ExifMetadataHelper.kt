package com.example.camera

import android.location.Location
import android.media.ExifInterface
import android.util.Log
import androidx.camera.core.ImageCapture
import com.example.data.model.GpsData
import com.example.data.model.GsmData
import com.example.data.model.SiteAccessStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility helper to handle CameraX Metadata Injection and EXIF GPS tagging
 * for telecom site audit photo evidence using Android's native ExifInterface.
 */
object ExifMetadataHelper {

    private const val TAG = "ExifMetadataHelper"

    data class ExifSummary(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val hasGps: Boolean,
        val dateTime: String?,
        val description: String?,
        val software: String?
    ) {
        val coordinatesString: String
            get() = String.format(
                Locale.US, "%.5f° %s, %.5f° %s",
                kotlin.math.abs(latitude), if (latitude >= 0) "N" else "S",
                kotlin.math.abs(longitude), if (longitude >= 0) "E" else "W"
            )
    }

    /**
     * Builds CameraX ImageCapture.Metadata with GPS coordinates injected
     * into the output file's EXIF data.
     */
    fun createCameraXMetadata(
        gps: GpsData,
        isReversedHorizontal: Boolean = false
    ): ImageCapture.Metadata {
        val location = Location("gps").apply {
            latitude = gps.latitude
            longitude = gps.longitude
            altitude = gps.altitude
            accuracy = gps.accuracy
            time = System.currentTimeMillis()
        }

        return ImageCapture.Metadata().apply {
            this.location = location
            this.isReversedHorizontal = isReversedHorizontal
        }
    }

    /**
     * Injects complete GPS coordinates and site audit metadata directly into
     * the JPEG image file's EXIF header using android.media.ExifInterface.
     */
    fun writeExifGpsMetadata(
        file: File,
        gps: GpsData,
        siteId: String,
        operator: String,
        status: SiteAccessStatus,
        gsm: GsmData? = null
    ): Boolean {
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "Cannot write EXIF to non-existent or empty file: ${file.absolutePath}")
            return false
        }

        return try {
            val exif = ExifInterface(file.absolutePath)

            // 1. Convert Coordinates to Standard EXIF Rational Format (deg/1,min/1,sec*1000/1000)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToExifDms(gps.latitude))
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (gps.latitude >= 0) "N" else "S")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToExifDms(gps.longitude))
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (gps.longitude >= 0) "E" else "W")

            val altInt = (kotlin.math.abs(gps.altitude) * 1000).toInt()
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "$altInt/1000")
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, if (gps.altitude >= 0) "0" else "1")

            val utcFormat = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val utcDateFormat = SimpleDateFormat("yyyy:MM:dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val now = Date()
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, utcFormat.format(now))
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, utcDateFormat.format(now))

            // 2. Inject Telecom Audit Metadata & Descriptions
            val localFormat = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val nowStr = localFormat.format(now)
            exif.setAttribute(ExifInterface.TAG_DATETIME, nowStr)

            val statusText = if (status == SiteAccessStatus.ACCORDED) "ACCORDED" else "REFUSED"
            val siteDesc = "Mobiserve Site Audit: $siteId ($operator) - Status: $statusText"
            exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, siteDesc)

            val gsmInfo = gsm?.let { " | GSM: ${it.networkType} CID: ${it.cellId} LAC: ${it.lacTac} RSRP: ${it.rsrpDisplay}" } ?: ""
            val comment = "Coordinates: ${gps.latitude}, ${gps.longitude} (±${gps.accuracy}m) | Alt: ${gps.altitude}m$gsmInfo"
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, comment)

            exif.setAttribute(ExifInterface.TAG_MAKE, "Mobiserve Holding")
            exif.setAttribute(ExifInterface.TAG_MODEL, "Telecom Site Access Inspector")
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Mobiserve Android Field Operations")

            exif.saveAttributes()
            Log.d(TAG, "Successfully injected EXIF GPS coordinates into ${file.name}: ${gps.coordinatesString}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing EXIF GPS metadata: ${e.message}", e)
            false
        }
    }

    /**
     * Converts a decimal coordinate into standard EXIF degrees/minutes/seconds string.
     */
    fun decimalToExifDms(coordinate: Double): String {
        val absCoord = kotlin.math.abs(coordinate)
        val degrees = absCoord.toInt()
        val minutesDouble = (absCoord - degrees) * 60.0
        val minutes = minutesDouble.toInt()
        val seconds = ((minutesDouble - minutes) * 60.0 * 1000.0).toInt()
        return "$degrees/1,$minutes/1,$seconds/1000"
    }

    /**
     * Reads back the EXIF metadata stored inside a captured photo file.
     */
    fun readExifMetadata(file: File): ExifSummary {
        if (!file.exists()) {
            return ExifSummary(0.0, 0.0, 0.0, false, null, null, null)
        }

        return try {
            val exif = ExifInterface(file.absolutePath)
            val latLong = FloatArray(2)
            val hasGps = exif.getLatLong(latLong)
            val altitude = exif.getAltitude(0.0)
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?: exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP)
            val description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
            val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE)

            ExifSummary(
                latitude = if (hasGps) latLong[0].toDouble() else 0.0,
                longitude = if (hasGps) latLong[1].toDouble() else 0.0,
                altitude = altitude,
                hasGps = hasGps,
                dateTime = dateTime,
                description = description,
                software = software
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EXIF metadata: ${e.message}", e)
            ExifSummary(0.0, 0.0, 0.0, false, null, null, null)
        }
    }
}
