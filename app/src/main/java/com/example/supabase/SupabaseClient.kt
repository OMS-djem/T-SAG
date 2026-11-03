package com.example.supabase

import android.content.Context
import android.util.Log
import com.example.data.model.SiteReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseClient(private val context: Context) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val authAdapter = moshi.adapter(SupabaseAuthResponse::class.java)
    private val profileAdapter = moshi.adapter(SupabaseProfile::class.java)
    private val profileListAdapter = moshi.adapter<List<SupabaseProfile>>(
        Types.newParameterizedType(List::class.java, SupabaseProfile::class.java)
    )
    private val siteReportAdapter = moshi.adapter(SupabaseSiteReport::class.java)
    private val siteReportListAdapter = moshi.adapter<List<SupabaseSiteReport>>(
        Types.newParameterizedType(List::class.java, SupabaseSiteReport::class.java)
    )

    private fun baseUrl(): String = SupabaseConfig.getUrl(context)
    private fun anonKey(): String = SupabaseConfig.getAnonKey(context)

    private fun buildHeaders(accessToken: String? = null): Map<String, String> {
        val key = anonKey()
        val headers = mutableMapOf(
            "apikey" to key,
            "Authorization" to "Bearer ${accessToken?.ifBlank { null } ?: key}",
            "Content-Type" to "application/json"
        )
        return headers
    }

    /**
     * Signs up a new user via Supabase Auth and registers their profile with isActive = false.
     * Root admin emails are pre-activated.
     */
    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        department: String,
        region: String
    ): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val isRootAdmin = isRootAdminEmail(cleanEmail)
        val assignedRole = if (isRootAdmin) UserRole.ADMIN.code else role
        val initialActive = isRootAdmin // Non-admins require admin activation

        val payload = JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
            put("data", JSONObject().apply {
                put("name", name.trim())
                put("phone", phone.trim())
                put("role", assignedRole)
                put("department", department.ifBlank { "Telecom Field Operations" })
                put("region", region.ifBlank { "National" })
                put("is_active", initialActive)
            })
        }

        val request = Request.Builder()
            .url("${baseUrl()}/auth/v1/signup")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .apply {
                buildHeaders().forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errorJson = JSONObject(body)
                    errorJson.optString("msg", errorJson.optString("error_description", errorJson.optString("message", "Sign up failed: ${response.code}")))
                } catch (_: Exception) {
                    "Sign up error: ${response.code} - $body"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val parsed = authAdapter.fromJson(body)
            val userId = parsed?.user?.id.orEmpty()
            val token = parsed?.accessToken

            // Create profile entry in profiles table
            if (userId.isNotBlank()) {
                val profile = SupabaseProfile(
                    id = userId,
                    name = name.trim(),
                    email = cleanEmail,
                    phone = phone.trim(),
                    role = assignedRole,
                    department = department.ifBlank { "Telecom Field Operations" },
                    region = region.ifBlank { "National" },
                    isActive = initialActive
                )
                upsertProfile(profile, token)
            }

            Result.success(parsed ?: SupabaseAuthResponse(user = SupabaseUser(id = userId, email = cleanEmail)))
        } catch (e: Exception) {
            Log.w("SupabaseClient", "Sign up failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user with email and password via Supabase Auth.
     */
    suspend fun signIn(email: String, password: String): Result<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured(context)) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }
        val cleanEmail = email.trim().lowercase()
        val payload = JSONObject().apply {
            put("email", cleanEmail)
            put("password", password)
        }

        val request = Request.Builder()
            .url("${baseUrl()}/auth/v1/token?grant_type=password")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .apply {
                buildHeaders().forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorMsg = try {
                    val errorJson = JSONObject(body)
                    errorJson.optString("error_description", errorJson.optString("msg", errorJson.optString("message", "Invalid login credentials")))
                } catch (_: Exception) {
                    "Authentication error (${response.code})"
                }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val parsed = authAdapter.fromJson(body)
                ?: return@withContext Result.failure(Exception("Unable to parse session token from Supabase Auth"))

            Result.success(parsed)
        } catch (e: Exception) {
            Log.w("SupabaseClient", "Sign in failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Fetches user profile by user auth ID or email.
     */
    suspend fun fetchProfile(userId: String, email: String, accessToken: String? = null): Result<SupabaseProfile?> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured(context)) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }
        val queryUrl = if (userId.isNotBlank()) {
            "${baseUrl()}/rest/v1/profiles?id=eq.$userId&select=*"
        } else {
            "${baseUrl()}/rest/v1/profiles?email=eq.${email.trim().lowercase()}&select=*"
        }

        val request = Request.Builder()
            .url(queryUrl)
            .get()
            .apply {
                buildHeaders(accessToken).forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch profile: ${response.code}"))
            }

            val list = profileListAdapter.fromJson(body).orEmpty()
            Result.success(list.firstOrNull())
        } catch (e: Exception) {
            Log.w("SupabaseClient", "fetchProfile warning: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Fetches all registered user profiles from Supabase profiles table.
     */
    suspend fun fetchAllProfiles(accessToken: String? = null): Result<List<SupabaseProfile>> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured(context)) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }

        val request = Request.Builder()
            .url("${baseUrl()}/rest/v1/profiles?select=*&order=name.asc")
            .get()
            .apply {
                buildHeaders(accessToken).forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                Log.w("SupabaseClient", "fetchAllProfiles HTTP error: ${response.code}")
                return@withContext Result.failure(Exception("Failed to fetch user directory: ${response.code}"))
            }

            val list = try {
                profileListAdapter.fromJson(body).orEmpty()
            } catch (e: Exception) {
                Log.w("SupabaseClient", "fetchAllProfiles JSON parse warning: ${e.message}")
                emptyList()
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.w("SupabaseClient", "fetchAllProfiles network warning: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Upserts a profile record in the Supabase profiles table.
     */
    suspend fun upsertProfile(profile: SupabaseProfile, accessToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            if (profile.id.isNotBlank()) put("id", profile.id)
            put("name", profile.name)
            put("email", profile.email)
            put("phone", profile.phone)
            put("role", profile.role)
            put("department", profile.department)
            put("region", profile.region)
            put("is_active", profile.isActive)
        }

        val request = Request.Builder()
            .url("${baseUrl()}/rest/v1/profiles")
            .post(json.toString().toRequestBody(jsonMediaType))
            .apply {
                buildHeaders(accessToken).forEach { (k, v) -> addHeader(k, v) }
                addHeader("Prefer", "resolution=merge-duplicates")
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Upsert profile error: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Activates or deactivates a user account (Admin operation).
     */
    suspend fun setUserActive(profileId: String, email: String, isActive: Boolean, accessToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val queryUrl = if (profileId.isNotBlank()) {
            "${baseUrl()}/rest/v1/profiles?id=eq.$profileId"
        } else {
            "${baseUrl()}/rest/v1/profiles?email=eq.${email.trim().lowercase()}"
        }

        val payload = JSONObject().apply {
            put("is_active", isActive)
        }

        val request = Request.Builder()
            .url(queryUrl)
            .patch(payload.toString().toRequestBody(jsonMediaType))
            .apply {
                buildHeaders(accessToken).forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update user active status: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates an existing profile's role, contact, or operational details.
     */
    suspend fun updateProfile(profile: SupabaseProfile, accessToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val queryUrl = if (profile.id.isNotBlank()) {
            "${baseUrl()}/rest/v1/profiles?id=eq.${profile.id}"
        } else {
            "${baseUrl()}/rest/v1/profiles?email=eq.${profile.email.trim().lowercase()}"
        }

        val json = JSONObject().apply {
            put("name", profile.name)
            put("phone", profile.phone)
            put("role", profile.role)
            put("department", profile.department)
            put("region", profile.region)
            put("is_active", profile.isActive)
        }

        val request = Request.Builder()
            .url(queryUrl)
            .patch(json.toString().toRequestBody(jsonMediaType))
            .apply {
                buildHeaders(accessToken).forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update profile: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a user profile from Supabase profiles table.
     */
    suspend fun deleteProfile(profileId: String, email: String, accessToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val queryUrl = if (profileId.isNotBlank()) {
            "${baseUrl()}/rest/v1/profiles?id=eq.$profileId"
        } else {
            "${baseUrl()}/rest/v1/profiles?email=eq.${email.trim().lowercase()}"
        }

        val request = Request.Builder()
            .url(queryUrl)
            .delete()
            .apply {
                buildHeaders(accessToken).forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete profile: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Inserts or syncs a site audit report to the Supabase site_reports table.
     */
    suspend fun insertSiteReport(report: SiteReportEntity, accessToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured(context)) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }
        val payload = JSONObject().apply {
            put("site_id", report.siteId)
            put("site_name", report.siteName)
            put("operator", report.operator)
            put("status", report.status)
            put("refusal_reason", report.refusalReason)
            put("notes", report.notes)
            put("technician_name", report.technicianName)
            put("technician_phone", report.technicianPhone)
            put("technician_email", report.recipientEmail)
            put("latitude", report.latitude)
            put("longitude", report.longitude)
            put("altitude", report.altitude)
            put("accuracy", report.accuracy)
            put("address", report.address)
            put("gsm_operator", report.gsmOperator)
            put("network_type", report.networkType)
            put("cell_id", report.cellId)
            put("lac", report.lac)
        }

        val request = Request.Builder()
            .url("${baseUrl()}/rest/v1/site_reports")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .apply {
                buildHeaders(accessToken).forEach { (k, v) -> addHeader(k, v) }
                addHeader("Prefer", "return=minimal")
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Supabase site report upload failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.w("SupabaseClient", "insertSiteReport warning: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Fetches site reports from Supabase site_reports table.
     */
    suspend fun fetchSiteReports(accessToken: String? = null): Result<List<SupabaseSiteReport>> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured(context)) {
            return@withContext Result.failure(IllegalStateException("Supabase is not configured"))
        }
        val request = Request.Builder()
            .url("${baseUrl()}/rest/v1/site_reports?select=*&order=created_at.desc")
            .get()
            .apply {
                buildHeaders(accessToken).forEach { (k, v) -> addHeader(k, v) }
            }
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch site reports: ${response.code}"))
            }

            val list = siteReportListAdapter.fromJson(body).orEmpty()
            Result.success(list)
        } catch (e: Exception) {
            Log.w("SupabaseClient", "fetchSiteReports warning: ${e.localizedMessage}")
            Result.failure(e)
        }
    }
    /**
     * Tests connectivity to the Supabase project endpoint and anon key.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val url = baseUrl()
        val request = Request.Builder()
            .url("$url/rest/v1/")
            .get()
            .apply {
                buildHeaders().forEach { (k, v) -> addHeader(k, v) }
            }
            .build()
        try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code == 200 || response.code == 404) {
                Result.success("Connection to Supabase established successfully (HTTP ${response.code})")
            } else if (response.code == 401 || response.code == 403) {
                Result.failure(Exception("Supabase rejected API key (HTTP ${response.code}). Check your anon key."))
            } else {
                Result.failure(Exception("Supabase reachable with status code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Could not connect to Supabase: ${e.localizedMessage}"))
        }
    }

    companion object {
        fun isRootAdminEmail(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            val clean = email.trim().lowercase()
            return clean == "maamardjemil@gmail.com" ||
                    clean == "etb.djemil@gmail.com" ||
                    clean == "mdjemil@mobiserveholding.com"
        }
    }
}
