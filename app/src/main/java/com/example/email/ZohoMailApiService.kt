package com.example.email

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.GpsData
import com.example.data.model.GsmData
import com.example.data.model.SiteAccessStatus
import com.example.data.model.SitePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Data payload bundling all gathered telecom site audit data for transmission to Zoho Mail API.
 */
data class AuditReportPayload(
    val siteId: String,
    val siteName: String,
    val operator: String,
    val status: SiteAccessStatus,
    val refusalReason: String?,
    val notes: String,
    val technicianName: String,
    val technicianPhone: String,
    val recipientEmail: String,
    val ccEmails: List<String> = emptyList(),
    val gps: GpsData,
    val gsm: GsmData,
    val photos: List<SitePhoto> = emptyList()
) {
    val photoFiles: List<File>
        get() = photos.map { File(it.filePath) }.filter { it.exists() && it.length() > 0 }
}

/**
 * Configuration options for Zoho Mail API & SMTP communications.
 */
data class ZohoMailConfig(
    val senderEmail: String = ZohoSmtpClient.SENDER_EMAIL,
    val senderName: String = "Mobiserve Field Operations NOC",
    val apiToken: String = "",
    val accountId: String = "default",
    val apiBaseUrl: String = "https://mail.zoho.com/api",
    val smtpHost: String = ZohoSmtpClient.DEFAULT_ZOHO_HOST,
    val smtpPort: Int = ZohoSmtpClient.DEFAULT_ZOHO_PORT,
    val smtpPassword: String = ""
)

/**
 * Result state of Zoho Mail report dispatch.
 */
sealed class ZohoMailResult {
    data class Success(
        val message: String,
        val messageId: String? = null,
        val dispatchMethod: String = "REST_API"
    ) : ZohoMailResult()

    data class OfflineQueued(
        val message: String,
        val localReportId: Long
    ) : ZohoMailResult()

    data class Error(
        val message: String,
        val details: String? = null
    ) : ZohoMailResult()
}

/**
 * Service layer client to send site audit data, GPS telemetry, cell metrics,
 * and equipment photo attachments to the Zoho Mail API.
 */
class ZohoMailApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "ZohoMailApiService"
    }

    /**
     * Dispatches the audit report payload directly to the Zoho Mail REST API
     * (POST /api/accounts/{accountId}/messages).
     */
    suspend fun sendAuditReport(
        payload: AuditReportPayload,
        config: ZohoMailConfig
    ): ZohoMailResult = withContext(Dispatchers.IO) {
        val token = config.apiToken.trim()
        if (token.isBlank()) {
            return@withContext ZohoMailResult.Error(
                message = "Zoho Mail API token is not configured.",
                details = "Please configure your Zoho API token in Settings or use authenticated SMTP mode."
            )
        }

        val subject = EmailHelper.generateSubject(payload.siteId, payload.operator, payload.status)
        val htmlContent = EmailHelper.generateHtmlBody(
            siteId = payload.siteId,
            siteName = payload.siteName,
            operator = payload.operator,
            status = payload.status,
            refusalReason = payload.refusalReason,
            notes = payload.notes,
            technicianName = payload.technicianName,
            technicianPhone = payload.technicianPhone,
            gps = payload.gps,
            gsm = payload.gsm,
            photoCount = payload.photos.size
        )

        val toAddress = payload.recipientEmail.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(",")
            .ifBlank { config.senderEmail }

        val ccAddress = payload.ccEmails
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(",")

        val accountId = config.accountId.ifBlank { "default" }
        val endpoint = "${config.apiBaseUrl.trimEnd('/')}/accounts/$accountId/messages"

        try {
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("fromAddress", config.senderEmail)
                .addFormDataPart("toAddress", toAddress)
                .addFormDataPart("subject", subject)
                .addFormDataPart("content", htmlContent)
                .addFormDataPart("mailFormat", "html")

            if (ccAddress.isNotBlank()) {
                multipartBuilder.addFormDataPart("ccAddress", ccAddress)
            }

            // Include explicit GPS and GSM telemetry headers for backend parsing
            val telemetryJson = JSONObject().apply {
                put("siteId", payload.siteId)
                put("status", payload.status.code)
                put("latitude", payload.gps.latitude)
                put("longitude", payload.gps.longitude)
                put("altitude", payload.gps.altitude)
                put("accuracy", payload.gps.accuracy)
                put("networkType", payload.gsm.networkType)
                put("cellId", payload.gsm.cellId)
                put("lacTac", payload.gsm.lacTac)
                put("rsrpDbm", payload.gsm.rsrpDbm)
                put("operator", payload.gsm.operatorName)
            }
            multipartBuilder.addFormDataPart("auditTelemetry", telemetryJson.toString())

            // Attach all photo files gathered with GPS and EXIF data
            payload.photoFiles.forEachIndexed { index, file ->
                val photoMetadata = payload.photos.getOrNull(index)
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestFile = file.asRequestBody(mediaType)
                val partName = "attachments"
                val filename = file.name
                multipartBuilder.addFormDataPart(partName, filename, requestFile)
            }

            val requestBody = multipartBuilder.build()
            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", if (token.startsWith("Zoho-oauthtoken", true) || token.startsWith("Bearer", true)) token else "Zoho-oauthtoken $token")
                .addHeader("Accept", "application/json")
                .post(requestBody)
                .build()

            Log.i(TAG, "Dispatching audit report to Zoho Mail API: $endpoint (Site: ${payload.siteId})")

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Zoho API response code: ${response.code}, body: $responseBody")

            if (response.isSuccessful) {
                var messageId: String? = null
                try {
                    val json = JSONObject(responseBody)
                    val dataObj = json.optJSONObject("data")
                    messageId = dataObj?.optString("messageId") ?: json.optString("messageId")
                } catch (_: Exception) {}

                ZohoMailResult.Success(
                    message = "Audit report successfully delivered via Zoho Mail API!",
                    messageId = messageId,
                    dispatchMethod = "REST_API"
                )
            } else {
                val errorMsg = try {
                    val json = JSONObject(responseBody)
                    json.optJSONObject("status")?.optString("description")
                        ?: json.optString("message", "HTTP error ${response.code}")
                } catch (_: Exception) {
                    "HTTP ${response.code}: ${response.message}"
                }

                ZohoMailResult.Error(
                    message = "Zoho Mail API rejected request: $errorMsg",
                    details = "Server code: ${response.code}. Response: $responseBody"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception contacting Zoho Mail API", e)
            ZohoMailResult.Error(
                message = "Failed to reach Zoho Mail API: ${e.localizedMessage ?: e.javaClass.simpleName}",
                details = e.message
            )
        }
    }
}

/**
 * Service repository orchestrating the complete audit transmission pipeline:
 * 1. Checks online status; if offline, caches report in local Room database.
 * 2. If online and API token available, attempts Zoho Mail REST API.
 * 3. If API token is missing or API returns authentication failure, seamlessly
 *    dispatches via secure Zoho SMTP (SSL/TLS socket).
 * 4. Ensures audit data is never lost by managing Room queue status.
 */
class ZohoMailRepository(
    private val apiService: ZohoMailApiService = ZohoMailApiService()
) {
    suspend fun sendAuditReport(
        payload: AuditReportPayload,
        config: ZohoMailConfig,
        isOnline: Boolean,
        onQueueOffline: suspend (AuditReportPayload) -> Long,
        onUpdateReportStatus: suspend (Long, String, String?, Long?) -> Unit
    ): ZohoMailResult = withContext(Dispatchers.IO) {
        // 1. Offline Mode: Immediately store in Room queue
        if (!isOnline) {
            val reportId = onQueueOffline(payload)
            return@withContext ZohoMailResult.OfflineQueued(
                message = "Network unavailable. Audit report #$reportId queued locally in Room database.\n\nIt will automatically synchronize and dispatch once cellular connectivity is restored.",
                localReportId = reportId
            )
        }

        // 2. Online Mode: Save initial entry in Room with SYNCING status
        val reportId = onQueueOffline(payload)
        onUpdateReportStatus(reportId, "SYNCING", null, null)

        // 3. Try Zoho Mail REST API if token is provided
        if (config.apiToken.isNotBlank()) {
            val apiResult = apiService.sendAuditReport(payload, config)
            if (apiResult is ZohoMailResult.Success) {
                onUpdateReportStatus(reportId, "SYNCED", null, System.currentTimeMillis())
                return@withContext apiResult
            }
            Log.w("ZohoMailRepository", "Zoho API dispatch did not succeed, trying authenticated SMTP fallback...")
        }

        // 4. Try Authenticated Zoho SMTP Socket
        val subject = EmailHelper.generateSubject(payload.siteId, payload.operator, payload.status)
        val textBody = EmailHelper.generateBody(
            siteId = payload.siteId,
            siteName = payload.siteName,
            operator = payload.operator,
            status = payload.status,
            refusalReason = payload.refusalReason,
            notes = payload.notes,
            technicianName = payload.technicianName,
            technicianPhone = payload.technicianPhone,
            gps = payload.gps,
            gsm = payload.gsm,
            photoCount = payload.photos.size
        )
        val htmlBody = EmailHelper.generateHtmlBody(
            siteId = payload.siteId,
            siteName = payload.siteName,
            operator = payload.operator,
            status = payload.status,
            refusalReason = payload.refusalReason,
            notes = payload.notes,
            technicianName = payload.technicianName,
            technicianPhone = payload.technicianPhone,
            gps = payload.gps,
            gsm = payload.gsm,
            photoCount = payload.photos.size
        )

        val recipientsList = payload.recipientEmail.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val smtpResult = ZohoSmtpClient.sendEmail(
            host = config.smtpHost,
            port = config.smtpPort,
            senderEmail = config.senderEmail,
            sessionSenderName = config.senderName,
            password = config.smtpPassword,
            recipients = recipientsList,
            ccRecipients = payload.ccEmails,
            subject = subject,
            bodyText = textBody,
            htmlBody = htmlBody,
            attachments = payload.photoFiles
        )

        when (smtpResult) {
            is ZohoSmtpClient.SmtpResult.Success -> {
                onUpdateReportStatus(reportId, "SYNCED", null, System.currentTimeMillis())
                ZohoMailResult.Success(
                    message = smtpResult.message,
                    dispatchMethod = "SMTP"
                )
            }
            is ZohoSmtpClient.SmtpResult.Error -> {
                onUpdateReportStatus(reportId, "FAILED", smtpResult.message, null)
                ZohoMailResult.Error(
                    message = smtpResult.message,
                    details = "Report stored in local queue (#$reportId) for automatic retry when network stabilizes."
                )
            }
        }
    }
}
