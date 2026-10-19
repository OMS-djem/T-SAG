package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // ADMIN, OM, SUPERVISION, TECHNICIAN, DISPATCHER, ACQUISITION
    val department: String = "Telecom Field Operations",
    val region: String = "National",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val roleEnum: UserRole
        get() = try {
            UserRole.valueOf(role)
        } catch (e: Exception) {
            UserRole.TECHNICIAN
        }
}
