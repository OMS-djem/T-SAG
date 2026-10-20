package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.FirestoreMailConfig
import com.example.data.model.FirestoreUser
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    val db: FirebaseFirestore
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // Real-time observation of all users in organization (RBAC directory)
    fun observeUsers(): Flow<List<FirestoreUser>> = flow {
        val currentAuth = auth.currentUser
        if (currentAuth == null || currentAuth.isAnonymous) {
            emit(emptyList())
            return@flow
        }
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(FirestoreUser::class.java) }
                .catch { error ->
                    if (auth.currentUser != null && error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    emit(emptyList())
                }
        )
    }

    // Real-time observation of a single user profile
    fun observeUserProfile(uid: String): Flow<FirestoreUser?> = flow {
        val currentAuth = auth.currentUser
        if (currentAuth == null || currentAuth.isAnonymous) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot -> snapshot.toObject(FirestoreUser::class.java) }
                .catch { error ->
                    if (auth.currentUser != null && error is Exception) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    emit(null)
                }
        )
    }

    // Save or update user profile
    suspend fun saveUser(user: FirestoreUser) {
        val targetUid = user.uid.ifBlank { requireUserId() }
        val path = "users/$targetUid"
        val docRef = db.collection("users").document(targetUid)
        try {
            val payload = mapOf(
                "uid" to targetUid,
                "name" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "role" to user.role,
                "department" to user.department,
                "region" to user.region,
                "isActive" to user.isActive,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // Admin updates user role
    suspend fun updateUserRole(uid: String, newRole: UserRole) {
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "role" to newRole.code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // Admin updates active status
    suspend fun setUserActive(uid: String, isActive: Boolean) {
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "isActive" to isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // Admin deletes user
    suspend fun deleteUser(uid: String) {
        val path = "users/$uid"
        val docRef = db.collection("users").document(uid)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            throw e
        }
    }

    suspend fun deleteUserByEmailOrUid(uid: String, email: String) {
        try {
            deleteUser(uid)
        } catch (e: Exception) {
            try {
                val query = db.collection("users").whereEqualTo("email", email.trim()).get().await()
                for (doc in query.documents) {
                    doc.reference.delete().await()
                }
            } catch (ignored: Exception) {}
        }
    }

    suspend fun updateUserByEmailOrUid(user: FirestoreUser) {
        try {
            saveUser(user)
        } catch (e: Exception) {
            try {
                val query = db.collection("users").whereEqualTo("email", user.email.trim()).get().await()
                for (doc in query.documents) {
                    saveUser(user.copy(uid = doc.id))
                }
            } catch (ignored: Exception) {}
        }
    }

    // Observe global email configuration
    fun observeMailConfig(): Flow<FirestoreMailConfig?> = flow {
        val currentAuth = auth.currentUser
        if (currentAuth == null || currentAuth.isAnonymous) {
            emit(null)
            return@flow
        }
        val path = "mail_config/settings"
        emitAll(
            db.collection("mail_config").document("settings")
                .snapshots()
                .map { snapshot -> snapshot.toObject(FirestoreMailConfig::class.java) }
                .catch { error ->
                    if (auth.currentUser != null && error is Exception) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    emit(null)
                }
        )
    }

    // Admin saves mail configuration
    suspend fun saveMailConfig(senderEmail: String, senderName: String, toGroup: String, ccGroup: String) {
        val uid = requireUserId()
        val path = "mail_config/settings"
        val docRef = db.collection("mail_config").document("settings")
        try {
            val payload = mapOf(
                "senderEmail" to senderEmail,
                "senderName" to senderName,
                "toGroup" to toGroup,
                "ccGroup" to ccGroup,
                "updatedBy" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            throw e
        }
    }

    // Save site report to cloud Firestore
    suspend fun saveSiteReport(
        siteId: String,
        siteName: String,
        status: String,
        technicianName: String,
        technicianPhone: String,
        latitude: Double,
        longitude: Double
    ): String {
        val uid = requireUserId()
        val path = "site_reports"
        val docRef = db.collection(path).document()
        try {
            val payload = mapOf(
                "siteId" to siteId,
                "siteName" to siteName,
                "status" to status,
                "technicianName" to technicianName,
                "technicianPhone" to technicianPhone,
                "technicianUid" to uid,
                "latitude" to latitude,
                "longitude" to longitude,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            throw e
        }
    }
}
