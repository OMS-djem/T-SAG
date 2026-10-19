package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.model.GpsData
import com.example.data.model.GsmData
import com.example.data.model.SiteAccessStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object WatermarkHelper {

    suspend fun createStampedPhoto(
        context: Context,
        sourceBitmap: Bitmap,
        siteId: String,
        operator: String,
        status: SiteAccessStatus,
        gps: GpsData,
        gsm: GsmData,
        tag: String
    ): File = withContext(Dispatchers.IO) {
        val workingBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(workingBitmap)
        val width = workingBitmap.width.toFloat()
        val height = workingBitmap.height.toFloat()

        // Scaled dimensions relative to image width
        val baseTextSize = (width * 0.026f).coerceIn(24f, 44f)
        val titleTextSize = baseTextSize * 1.25f
        val padding = width * 0.035f

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timeString = dateFormat.format(Date())

        val lines = listOf(
            "MOBISERVE HOLDING • TELECOM SITE ACCESS AUDIT",
            "SITE ID: ${siteId.ifBlank { "SITE-UNKNOWN" }}  |  OPERATOR: ${operator.ifBlank { "N/A" }}  |  TAG: $tag",
            "STATUS: ${if (status == SiteAccessStatus.ACCORDED) "ACCESS ACCORDED ✓" else "NO ACCESS / REFUSED ✕"}",
            "GPS: ${gps.coordinatesString} (±${String.format(Locale.US, "%.1f", gps.accuracy)}m)  |  ALT: ${String.format(Locale.US, "%.0f", gps.altitude)}m",
            "GSM: ${gsm.networkType}  |  MCC-MNC: ${gsm.mcc}-${gsm.mnc}  |  LAC/TAC: ${gsm.lacTac}  |  CID: ${gsm.cellId}  |  ${gsm.signalDbm} dBm",
            "TIMESTAMP: $timeString"
        )

        val bannerHeight = (lines.size * (baseTextSize * 1.45f)) + (padding * 2.2f)

        // Draw translucent dark backdrop on bottom of the image
        val bgPaint = Paint().apply {
            color = Color.argb(215, 10, 20, 35)
            style = Paint.Style.FILL
        }
        val bannerRect = RectF(0f, height - bannerHeight, width, height)
        canvas.drawRect(bannerRect, bgPaint)

        // Draw top accent line on banner (Green for Accorded, Red for Refused)
        val accentPaint = Paint().apply {
            color = if (status == SiteAccessStatus.ACCORDED) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
            strokeWidth = (width * 0.008f).coerceAtLeast(6f)
            style = Paint.Style.STROKE
        }
        canvas.drawLine(0f, height - bannerHeight, width, height - bannerHeight, accentPaint)

        // Draw Text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = baseTextSize
            isAntiAlias = true
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(56, 189, 248) // Light Cyan
            textSize = titleTextSize
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val statusPaint = Paint().apply {
            color = if (status == SiteAccessStatus.ACCORDED) Color.rgb(52, 211, 153) else Color.rgb(248, 113, 113)
            textSize = baseTextSize * 1.1f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var currentY = height - bannerHeight + padding + titleTextSize

        lines.forEachIndexed { index, line ->
            when (index) {
                0 -> canvas.drawText(line, padding, currentY, headerPaint)
                2 -> canvas.drawText(line, padding, currentY, statusPaint)
                else -> canvas.drawText(line, padding, currentY, textPaint)
            }
            currentY += baseTextSize * 1.45f
        }

        // Save stamped image to disk
        val photosDir = File(context.filesDir, "site_photos").apply { mkdirs() }
        val outputFile = File(photosDir, "IMG_SITE_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
        FileOutputStream(outputFile).use { out ->
            workingBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        outputFile
    }

    suspend fun createSyntheticSitePhoto(
        context: Context,
        siteId: String,
        operator: String,
        status: SiteAccessStatus,
        gps: GpsData,
        gsm: GsmData,
        tag: String
    ): File = withContext(Dispatchers.IO) {
        // Generates an illustrative photo card for testing / field simulation
        val width = 1200
        val height = 900
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Sky / Background gradient simulation
        val skyPaint = Paint().apply {
            color = if (status == SiteAccessStatus.ACCORDED) Color.rgb(30, 41, 59) else Color.rgb(45, 25, 30)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint)

        // Draw telecom tower illustration silhouette in center
        val towerPaint = Paint().apply {
            color = Color.argb(120, 203, 213, 225)
            strokeWidth = 6f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val centerX = width / 2f
        val centerY = height * 0.35f

        // Tower legs
        canvas.drawLine(centerX - 150f, centerY + 240f, centerX - 40f, centerY - 140f, towerPaint)
        canvas.drawLine(centerX + 150f, centerY + 240f, centerX + 40f, centerY - 140f, towerPaint)
        // Tower mast
        canvas.drawLine(centerX, centerY - 240f, centerX, centerY + 240f, towerPaint)
        // Tower rungs
        for (yOffset in -100..200 step 50) {
            val spread = (yOffset + 150) * 0.35f
            canvas.drawLine(centerX - spread, centerY + yOffset, centerX + spread, centerY + yOffset, towerPaint)
        }
        // Antennas / Dishes
        val dishPaint = Paint().apply {
            color = Color.argb(180, 255, 255, 255)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(centerX - 50f, centerY - 100f, 25f, dishPaint)
        canvas.drawCircle(centerX + 50f, centerY - 60f, 22f, dishPaint)

        // Label in center
        val centerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("TELECOM TOWER PHOTO EVIDENCE [$tag]", centerX, centerY + 280f, centerTextPaint)

        // Now stamp it with the official watermark
        createStampedPhoto(context, bitmap, siteId, operator, status, gps, gsm, tag)
    }
}
