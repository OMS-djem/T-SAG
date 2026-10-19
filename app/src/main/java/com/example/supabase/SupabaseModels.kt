package com.example.supabase

import com.example.data.model.UserRole
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String = "",
    val email: String = "",
    val phone: String? = null,
    val role: String? = null,
    @Json(name = "user_metadata")
    val userMetadata: Map<String, Any?>? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token")
    val accessToken: String? = null,
    @Json(name = "token_type")
    val tokenType: String? = null,
    @Json(name = "expires_in")
    val expiresIn: Long? = null,
    @Json(name = "refresh_token")
    val refreshToken: String? = null,
    val user: SupabaseUser? = null,
    val error: String? = null,
    @Json(name = "error_description")
    val errorDescription: String? = null,
    val msg: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "TECHNICIAN",
    val department: String = "Telecom Field Operations",
    val region: String = "National",
    @Json(name = "is_active")
    val isActive: Boolean = false,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "updated_at")
    val updatedAt: String? = null
) {
    val roleEnum: UserRole
        get() = try {
            UserRole.valueOf(role)
        } catch (_: Exception) {
            UserRole.TECHNICIAN
        }
}

@JsonClass(generateAdapter = true)
data class SupabaseSiteReport(
    val id: String? = null,
    @Json(name = "site_id")
    val siteId: String = "",
    @Json(name = "site_name")
    val siteName: String = "",
    val operator: String = "",
    val status: String = "ACCORDED",
    @Json(name = "refusal_reason")
    val refusalReason: String? = null,
    val notes: String? = null,
    @Json(name = "technician_name")
    val technicianName: String = "",
    @Json(name = "technician_phone")
    val technicianPhone: String = "",
    @Json(name = "technician_email")
    val technicianEmail: String = "",
    @Json(name = "technician_uid")
    val technicianUid: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val address: String? = null,
    @Json(name = "gsm_operator")
    val gsmOperator: String? = null,
    @Json(name = "network_type")
    val networkType: String? = null,
    @Json(name = "cell_id")
    val cellId: String? = null,
    val lac: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)
