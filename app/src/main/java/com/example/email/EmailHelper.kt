package com.example.email

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.GpsData
import com.example.data.model.GsmData
import com.example.data.model.SiteAccessStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailHelper {

    const val DEFAULT_ZOHO_RECIPIENT = "mdjemil@mobiserveholding.com"

    fun generateSubject(siteId: String, operator: String, status: SiteAccessStatus): String {
        val cleanSite = siteId.ifBlank { "SITE-UNKNOWN" }.trim()
        val cleanOp = operator.ifBlank { "TELECOM" }.trim()
        val time = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
        return if (status == SiteAccessStatus.ACCORDED) {
            "[ACCESS ACCORDED] Site $cleanSite ($cleanOp) - $time"
        } else {
            "[URGENT: NO ACCESS ALERT] Site $cleanSite ($cleanOp) - $time"
        }
    }

    fun generateBody(
        siteId: String,
        siteName: String,
        operator: String,
        status: SiteAccessStatus,
        refusalReason: String?,
        notes: String,
        technicianName: String,
        technicianPhone: String,
        gps: GpsData,
        gsm: GsmData,
        photoCount: Int
    ): String {
        val timeString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (z)", Locale.US).format(Date())
        val cleanSite = siteId.ifBlank { "SITE-UNKNOWN" }.trim()
        val cleanName = siteName.ifBlank { cleanSite }.trim()
        val cleanOp = operator.ifBlank { "Telecom Operator" }.trim()
        val cleanTech = technicianName.ifBlank { "Field Engineer" }.trim()
        val cleanPhone = technicianPhone.ifBlank { "N/A" }.trim()

        return if (status == SiteAccessStatus.ACCORDED) {
            """
Dear Mobiserve Holding Operations Team,

Please be advised that site access has been successfully ACCORDED for the telecom site detailed below.

==================================================
           SITE ACCESS REPORT: ACCORDED
==================================================
• Status: ACCESS ACCORDED (GRANTED) ✓
• Site ID: $cleanSite
• Site Name: $cleanName
• Network Operator: $cleanOp
• Company: Mobiserve Holding
• Date & Time: $timeString
• Field Technician: $cleanTech
• Contact Number: $cleanPhone

--------------------------------------------------
1. TELECOM CELLULAR & GSM SITE TELEMETRY
--------------------------------------------------
• Connected Network: ${gsm.networkType}
• Detected Operator: ${gsm.operatorName}
• Cell ID (CID / ECI): ${gsm.cellId}
• Location / Tracking Area Code (LAC/TAC): ${gsm.lacTac}
• Mobile Country / Network Code (MCC-MNC): ${gsm.mcc}-${gsm.mnc}
• Signal Strength: ${gsm.signalDbm} dBm (${gsm.signalLevel})

--------------------------------------------------
2. GPS LOCATION STAMP & AUDIT TRAIL
--------------------------------------------------
• Latitude, Longitude: ${gps.coordinatesString}
• GPS Accuracy: ±${String.format(Locale.US, "%.1f", gps.accuracy)} meters
• Altitude: ${String.format(Locale.US, "%.1f", gps.altitude)} meters
• Approximate Address: ${gps.address}
• Google Maps Location: ${gps.mapsUrl}

--------------------------------------------------
3. FIELD WORK NOTES & ACCESS DETAILS
--------------------------------------------------
${if (notes.isNotBlank()) notes else "Technician granted entry to site premises. Normal operational conditions verified."}

--------------------------------------------------
4. PHOTO EVIDENCE
--------------------------------------------------
Total Photos Attached: $photoCount
(All photos stamped with Mobiserve security header, coordinates, and GSM cell identification)

Best regards,
$cleanTech
Mobiserve Holding Telecom Field Operations
Recipient: $DEFAULT_ZOHO_RECIPIENT
            """.trimIndent()
        } else {
            """
URGENT ATTENTION: MOBISERVE HOLDING OPERATIONS & DISPATCH

Site access has been DENIED / REFUSED at site $cleanSite. Immediate management escalation required.

==================================================
           SITE ACCESS REPORT: REFUSED
           SITE ACCESS ALERT: NO ACCESS
==================================================
• Status: NO ACCESS / ACCESS REFUSED ✕
• Site ID: $cleanSite
• Site Name: $cleanName
• Network Operator: $cleanOp
• Primary Refusal Reason: ${refusalReason?.ifBlank { "Access Blocked / Refused" } ?: "Access Denied by Site Guard / Landlord"}
• Date & Time: $timeString
• Reporting Technician: $cleanTech
• Contact Number: $cleanPhone

--------------------------------------------------
1. ESCALATION & INCIDENT SUMMARY
--------------------------------------------------
${if (notes.isNotBlank()) notes else "Technician arrived on-site but was unable to gain authorized entrance. See refusal reason above."}

--------------------------------------------------
2. FIELD PRESENCE VERIFICATION (GPS)
--------------------------------------------------
(Proves field technician physically reached site perimeter)
• Arrived Coordinates: ${gps.coordinatesString}
• Accuracy: ±${String.format(Locale.US, "%.1f", gps.accuracy)} meters
• Altitude: ${String.format(Locale.US, "%.1f", gps.altitude)} meters
• Location Area: ${gps.address}
• Google Maps Verification: ${gps.mapsUrl}

--------------------------------------------------
3. TELECOM GSM / CELL SITE TELEMETRY
--------------------------------------------------
• Cell ID (CID): ${gsm.cellId}
• LAC / TAC: ${gsm.lacTac}
• MCC-MNC: ${gsm.mcc}-${gsm.mnc}
• Signal: ${gsm.signalDbm} dBm (${gsm.networkType})

--------------------------------------------------
4. ATTACHED PHOTO PROOF
--------------------------------------------------
Total Photos Attached: $photoCount
(Stamped photos proving locked gate, padlock state, access notice, or security post)

ACTION REQUIRED: Please contact site landlord/regional security supervisor to unblock access.

Best regards,
$cleanTech
Mobiserve Holding Field Services
Escalation sent to: $DEFAULT_ZOHO_RECIPIENT
            """.trimIndent()
        }
    }

    fun generateHtmlBody(
        siteId: String,
        siteName: String,
        operator: String,
        status: SiteAccessStatus,
        refusalReason: String?,
        notes: String,
        technicianName: String,
        technicianPhone: String,
        gps: GpsData,
        gsm: GsmData,
        photoCount: Int
    ): String {
        val timeString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss (z)", Locale.US).format(Date())
        val cleanSite = siteId.ifBlank { "SITE-UNKNOWN" }.trim()
        val cleanName = siteName.ifBlank { cleanSite }.trim()
        val cleanOp = operator.ifBlank { "Telecom Operator" }.trim()
        val cleanTech = technicianName.ifBlank { "Field Engineer" }.trim()
        val cleanPhone = technicianPhone.ifBlank { "N/A" }.trim()

        val isAccorded = status == SiteAccessStatus.ACCORDED
        val bannerBg = if (isAccorded) "#16A34A" else "#DC2626"
        val bannerTitle = if (isAccorded) "SITE ACCESS REPORT: ACCORDED" else "SITE ACCESS REPORT: REFUSED"

        return """
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.5; color: #1e293b; background-color: #f8fafc; padding: 16px; margin: 0; }
    .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; padding: 20px; }
    .banner { background-color: $bannerBg; color: #ffffff; padding: 12px 16px; text-align: center; font-family: monospace; font-size: 15px; font-weight: bold; border-radius: 6px; margin: 16px 0; letter-spacing: 0.5px; }
    .section-title { font-weight: bold; font-size: 13px; color: #0f172a; margin-top: 16px; border-bottom: 1px solid #cbd5e1; padding-bottom: 4px; text-transform: uppercase; }
    .meta-item { font-size: 13px; margin: 4px 0; }
    .meta-key { font-weight: 600; color: #475569; }
    .footer { margin-top: 24px; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; padding-top: 12px; }
  </style>
</head>
<body>
  <div class="container">
    <p style="font-size: 14px; margin-top: 0;">${if (isAccorded) "Dear Mobiserve Holding Operations Team,<br><br>Please be advised that site access has been successfully ACCORDED for the telecom site detailed below." else "<strong style='color:#dc2626'>URGENT ATTENTION: MOBISERVE HOLDING OPERATIONS & DISPATCH</strong><br><br>Site access has been DENIED / REFUSED at site $cleanSite. Immediate management escalation required."}</p>

    <div class="banner">
      ==================================================<br>
      $bannerTitle<br>
      ==================================================
    </div>

    <div class="meta-item"><span class="meta-key">Status:</span> ${if (isAccorded) "<span style='color:#16a34a; font-weight:bold'>ACCESS ACCORDED (GRANTED) ✓</span>" else "<span style='color:#dc2626; font-weight:bold'>NO ACCESS / ACCESS REFUSED ✕</span>"}</div>
    <div class="meta-item"><span class="meta-key">Site ID:</span> $cleanSite</div>
    <div class="meta-item"><span class="meta-key">Site Name:</span> $cleanName</div>
    <div class="meta-item"><span class="meta-key">Network Operator:</span> $cleanOp</div>
    ${if (!isAccorded) "<div class='meta-item'><span class='meta-key' style='color:#dc2626'>Primary Refusal Reason:</span> ${refusalReason?.ifBlank { "Access Blocked / Refused" } ?: "Access Denied"}</div>" else ""}
    <div class="meta-item"><span class="meta-key">Date & Time:</span> $timeString</div>
    <div class="meta-item"><span class="meta-key">Field Technician:</span> $cleanTech ($cleanPhone)</div>

    <div class="section-title">1. Telecom Cellular & GSM Telemetry</div>
    <div class="meta-item"><span class="meta-key">Network:</span> ${gsm.networkType} (${gsm.operatorName})</div>
    <div class="meta-item"><span class="meta-key">Cell ID:</span> ${gsm.cellId} | <span class="meta-key">LAC/TAC:</span> ${gsm.lacTac}</div>
    <div class="meta-item"><span class="meta-key">MCC-MNC:</span> ${gsm.mcc}-${gsm.mnc} | <span class="meta-key">Signal:</span> ${gsm.signalDbm} dBm (${gsm.signalLevel})</div>

    <div class="section-title">2. GPS Location & Audit Trail</div>
    <div class="meta-item"><span class="meta-key">Coordinates:</span> ${gps.coordinatesString} (±${String.format(Locale.US, "%.1f", gps.accuracy)} m)</div>
    <div class="meta-item"><span class="meta-key">Altitude:</span> ${String.format(Locale.US, "%.1f", gps.altitude)} m</div>
    <div class="meta-item"><span class="meta-key">Address:</span> ${gps.address}</div>
    <div class="meta-item"><a href="${gps.mapsUrl}" style="color: #2563eb; text-decoration: none;">View Google Maps Location &rarr;</a></div>

    <div class="section-title">3. Notes</div>
    <p style="font-size: 13px; margin: 4px 0;">${if (notes.isNotBlank()) notes else (if (isAccorded) "Technician granted entry to site premises. Normal operational conditions verified." else "Technician arrived on-site but was unable to gain authorized entrance.")}</p>

    <div class="section-title">4. Photo Evidence</div>
    <div class="meta-item">Total Photos Attached: $photoCount (stamped with security telemetry)</div>

    <div class="footer">
      <strong>$cleanTech</strong><br>
      Mobiserve Holding Telecom Field Operations<br>
      Dispatched via Mobiserve Android Field App
    </div>
  </div>
</body>
</html>
        """.trimIndent()
    }

    fun createSendEmailIntent(
        context: Context,
        recipient: String,
        ccRecipients: List<String> = emptyList(),
        subject: String,
        body: String,
        photoFiles: List<File>
    ): Intent {
        val toList = recipient.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
        val targetRecipients = if (toList.isNotEmpty()) toList.toTypedArray() else arrayOf(DEFAULT_ZOHO_RECIPIENT)
        val validCc = ccRecipients.filter { it.isNotBlank() }

        val imageUris = ArrayList<Uri>()
        photoFiles.forEach { file ->
            if (file.exists()) {
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    imageUris.add(uri)
                } catch (e: Exception) {
                    // ignore invalid file
                }
            }
        }

        val intent = if (imageUris.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/jpeg"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
            }
        } else if (imageUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, imageUris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
            }
        }

        intent.putExtra(Intent.EXTRA_EMAIL, targetRecipients)
        if (validCc.isNotEmpty()) {
            intent.putExtra(Intent.EXTRA_CC, validCc.toTypedArray())
        }
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return Intent.createChooser(intent, "Send Site Report via Zoho Mail / Email")
    }
}
