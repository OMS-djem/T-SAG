package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.FirestoreUser
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.FirestoreRepository
import com.example.supabase.SupabaseClient
import com.example.supabase.SupabaseProfile
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SessionUser(
    val uid: String,
    val email: String,
    val displayName: String = "",
    val phoneNumber: String = "",
    val authProvider: String = "SUPABASE"
)

class UserSessionManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository,
    private val scope: CoroutineScope
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(context)
    val supabaseClient = SupabaseClient(context)
    private val userDao = AppDatabase.getDatabase(context).userDao()

    private val prefs = context.getSharedPreferences("tsag_user_session", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<SessionUser?>(null)
    val currentUser: StateFlow<SessionUser?> = _currentUser.asStateFlow()

    private val _currentProfile = MutableStateFlow<SupabaseProfile?>(null)
    val currentProfile: StateFlow<SupabaseProfile?> = _currentProfile.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.TECHNICIAN)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _isPendingActivation = MutableStateFlow(false)
    val isPendingActivation: StateFlow<Boolean> = _isPendingActivation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    init {
        restoreStoredSession()
    }

    fun isInitialAdminEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        val clean = email.trim().lowercase()
        return clean == "maamardjemil@gmail.com" ||
                clean == "etb.djemil@gmail.com" ||
                clean == "mdjemil@mobiserveholding.com"
    }

    private fun restoreStoredSession() {
        val savedUid = prefs.getString("session_uid", null)
        val savedEmail = prefs.getString("session_email", null)
        val savedName = prefs.getString("session_name", "") ?: ""
        val savedRole = prefs.getString("session_role", UserRole.TECHNICIAN.code) ?: UserRole.TECHNICIAN.code
        val savedPending = prefs.getBoolean("session_pending", false)
        val savedToken = prefs.getString("session_token", null)

        if (!savedUid.isNullOrBlank() && !savedEmail.isNullOrBlank()) {
            val isRootAdmin = isInitialAdminEmail(savedEmail)
            val effectivePending = if (isRootAdmin) false else savedPending
            val effectiveRole = if (isRootAdmin) UserRole.ADMIN else {
                try { UserRole.valueOf(savedRole) } catch (_: Exception) { UserRole.TECHNICIAN }
            }

            _currentUser.value = SessionUser(
                uid = savedUid,
                email = savedEmail,
                displayName = savedName
            )
            _currentRole.value = effectiveRole
            _isPendingActivation.value = effectivePending
            _accessToken.value = savedToken

            _currentProfile.value = SupabaseProfile(
                id = savedUid,
                name = savedName,
                email = savedEmail,
                role = effectiveRole.code,
                isActive = !effectivePending
            )

            // Asynchronously refresh activation status from Supabase or Room
            scope.launch {
                refreshActivationStatus()
            }
        }
    }

    private fun persistSession(
        uid: String,
        email: String,
        name: String,
        role: String,
        isPending: Boolean,
        token: String?
    ) {
        prefs.edit()
            .putString("session_uid", uid)
            .putString("session_email", email)
            .putString("session_name", name)
            .putString("session_role", role)
            .putBoolean("session_pending", isPending)
            .putString("session_token", token)
            .apply()
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun dismissStatus() {
        _statusMessage.value = null
    }

    /**
     * Authenticates with email and password using Supabase Auth.
     * Enforces admin activation: unactivated users are routed to AccountPendingActivationScreen.
     */
    fun signInWithEmailPassword(
        email: String,
        password: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) {
            _errorMessage.value = "Please enter your corporate email address."
            onError("Please enter your corporate email address.")
            return
        }
        if (password.isBlank()) {
            _errorMessage.value = "Please enter your account password."
            onError("Please enter your account password.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        scope.launch {
            val isRootAdmin = isInitialAdminEmail(cleanEmail)

            // Try Supabase Auth API
            val result = supabaseClient.signIn(cleanEmail, password)

            if (result.isSuccess) {
                val authResponse = result.getOrNull()
                val token = authResponse?.accessToken
                val user = authResponse?.user
                val uid = user?.id ?: "usr_${cleanEmail.hashCode()}"

                _accessToken.value = token

                // Fetch or resolve user profile
                val profileResult = supabaseClient.fetchProfile(userId = uid, email = cleanEmail, accessToken = token)
                val profile = profileResult.getOrNull()

                val isActive = if (isRootAdmin) {
                    true
                } else {
                    profile?.isActive ?: false
                }

                val role = if (isRootAdmin) {
                    UserRole.ADMIN
                } else {
                    profile?.roleEnum ?: UserRole.TECHNICIAN
                }

                val name = profile?.name?.ifBlank { null }
                    ?: (user?.userMetadata?.get("name") as? String)
                    ?: cleanEmail.substringBefore("@")

                _currentUser.value = SessionUser(
                    uid = uid,
                    email = cleanEmail,
                    displayName = name
                )
                _currentRole.value = role
                _isPendingActivation.value = !isActive

                _currentProfile.value = SupabaseProfile(
                    id = uid,
                    name = name,
                    email = cleanEmail,
                    phone = profile?.phone ?: "",
                    role = role.code,
                    department = profile?.department ?: "Telecom Field Operations",
                    region = profile?.region ?: "National",
                    isActive = isActive
                )

                // Sync into local Room database
                val localUser = userDao.getUserByEmail(cleanEmail)
                if (localUser != null) {
                    userDao.updateUser(
                        localUser.copy(
                            name = name,
                            role = role.code,
                            isActive = isActive
                        )
                    )
                } else {
                    userDao.insertUser(
                        UserEntity(
                            name = name,
                            email = cleanEmail,
                            phone = profile?.phone ?: "",
                            role = role.code,
                            department = profile?.department ?: "Telecom Field Operations",
                            region = profile?.region ?: "National",
                            isActive = isActive
                        )
                    )
                }

                persistSession(uid, cleanEmail, name, role.code, !isActive, token)
                _isLoading.value = false
                onSuccess()
            } else {
                // If Supabase Auth call failed (e.g. invalid credentials or network)
                val ex = result.exceptionOrNull()
                val errorMsg = ex?.message ?: "Login failed"

                // Check local user database for offline / local-credentials fallback
                val localUser = userDao.getUserByEmail(cleanEmail)
                if (localUser != null) {
                    val isUserActive = if (isRootAdmin) true else localUser.isActive
                    val effectiveRole = if (isRootAdmin) UserRole.ADMIN else localUser.roleEnum

                    _currentUser.value = SessionUser(
                        uid = "local_${localUser.id}",
                        email = cleanEmail,
                        displayName = localUser.name
                    )
                    _currentRole.value = effectiveRole
                    _isPendingActivation.value = !isUserActive

                    _currentProfile.value = SupabaseProfile(
                        id = "local_${localUser.id}",
                        name = localUser.name,
                        email = cleanEmail,
                        phone = localUser.phone,
                        role = effectiveRole.code,
                        department = localUser.department,
                        region = localUser.region,
                        isActive = isUserActive
                    )

                    persistSession(
                        "local_${localUser.id}",
                        cleanEmail,
                        localUser.name,
                        effectiveRole.code,
                        !isUserActive,
                        null
                    )
                    _isLoading.value = false
                    onSuccess()
                } else if (isRootAdmin) {
                    // Auto-seed root admin locally if offline
                    val id = userDao.insertUser(
                        UserEntity(
                            name = "Administrator",
                            email = cleanEmail,
                            phone = "+213 550 12 34 56",
                            role = "ADMIN",
                            department = "Management / NOC",
                            region = "National",
                            isActive = true
                        )
                    )
                    _currentUser.value = SessionUser(
                        uid = "admin_$id",
                        email = cleanEmail,
                        displayName = "Administrator"
                    )
                    _currentRole.value = UserRole.ADMIN
                    _isPendingActivation.value = false
                    persistSession("admin_$id", cleanEmail, "Administrator", "ADMIN", false, null)
                    _isLoading.value = false
                    onSuccess()
                } else {
                    _isLoading.value = false
                    _errorMessage.value = errorMsg
                    onError(errorMsg)
                }
            }
        }
    }

    /**
     * Self-Registration: creates user account in Supabase with isActive = false,
     * awaiting administrator activation.
     */
    fun signUpWithSupabase(
        name: String,
        email: String,
        password: String,
        phone: String,
        role: String,
        department: String,
        region: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        if (cleanName.isBlank()) {
            _errorMessage.value = "Please enter your full name."
            onError("Please enter your full name.")
            return
        }
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _errorMessage.value = "Please enter a valid corporate email address."
            onError("Please enter a valid corporate email address.")
            return
        }
        if (password.length < 6) {
            _errorMessage.value = "Password must be at least 6 characters."
            onError("Password must be at least 6 characters.")
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        scope.launch {
            val isRootAdmin = isInitialAdminEmail(cleanEmail)
            val targetRole = if (isRootAdmin) UserRole.ADMIN.code else role
            val initialActive = isRootAdmin // Non-admins wait for admin activation

            val result = supabaseClient.signUp(
                name = cleanName,
                email = cleanEmail,
                password = password,
                phone = phone.trim(),
                role = targetRole,
                department = department.ifBlank { "Telecom Field Operations" },
                region = region.ifBlank { "National" }
            )

            val token = result.getOrNull()?.accessToken
            val uid = result.getOrNull()?.user?.id ?: "usr_${System.currentTimeMillis()}"

            // Persist to local Room database
            val localId = userDao.insertUser(
                UserEntity(
                    name = cleanName,
                    email = cleanEmail,
                    phone = phone.trim(),
                    role = targetRole,
                    department = department.ifBlank { "Telecom Field Operations" },
                    region = region.ifBlank { "National" },
                    isActive = initialActive
                )
            )

            _currentUser.value = SessionUser(
                uid = uid,
                email = cleanEmail,
                displayName = cleanName,
                phoneNumber = phone.trim()
            )
            _currentRole.value = if (isRootAdmin) UserRole.ADMIN else {
                try { UserRole.valueOf(targetRole) } catch (_: Exception) { UserRole.TECHNICIAN }
            }
            _isPendingActivation.value = !initialActive
            _accessToken.value = token

            _currentProfile.value = SupabaseProfile(
                id = uid,
                name = cleanName,
                email = cleanEmail,
                phone = phone.trim(),
                role = targetRole,
                department = department.ifBlank { "Telecom Field Operations" },
                region = region.ifBlank { "National" },
                isActive = initialActive
            )

            persistSession(uid, cleanEmail, cleanName, targetRole, !initialActive, token)

            _isLoading.value = false
            if (!initialActive) {
                _statusMessage.value = "Account created! Awaiting administrator activation."
            } else {
                _statusMessage.value = "Administrator account created and activated!"
            }
            onSuccess()
        }
    }

    /**
     * Checks if the current user has been activated by an administrator.
     */
    fun refreshActivationStatus(onChecked: (Boolean) -> Unit = {}) {
        val user = _currentUser.value ?: return
        val isRootAdmin = isInitialAdminEmail(user.email)
        if (isRootAdmin) {
            _isPendingActivation.value = false
            _currentRole.value = UserRole.ADMIN
            persistSession(user.uid, user.email, user.displayName, UserRole.ADMIN.code, false, _accessToken.value)
            onChecked(true)
            return
        }

        scope.launch {
            // Check Supabase profiles table
            val profileRes = supabaseClient.fetchProfile(user.uid, user.email, _accessToken.value)
            val profile = profileRes.getOrNull()

            // Check local Room database
            val localUser = userDao.getUserByEmail(user.email)

            val isNowActive = profile?.isActive == true || localUser?.isActive == true
            _isPendingActivation.value = !isNowActive

            if (isNowActive) {
                if (localUser != null && !localUser.isActive) {
                    userDao.updateUser(localUser.copy(isActive = true, role = profile?.role ?: localUser.role))
                }
                val newRole = profile?.roleEnum ?: localUser?.roleEnum ?: UserRole.TECHNICIAN
                _currentRole.value = newRole
                _currentProfile.value = profile ?: SupabaseProfile(
                    id = user.uid,
                    name = user.displayName,
                    email = user.email,
                    role = newRole.code,
                    isActive = true
                )
                persistSession(user.uid, user.email, user.displayName, newRole.code, false, _accessToken.value)
                _statusMessage.value = "Account activated! Welcome to Mobiserve T-SAG."
            }

            onChecked(isNowActive)
        }
    }

    /**
     * Google Sign-In support via Credential Manager as alternate authentication option.
     */
    fun signInWithGoogle(activity: Activity) {
        _isLoading.value = true
        _errorMessage.value = null

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = "Google Sign-In configuration missing: default_web_client_id not found"
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authRes = auth.signInWithCredential(authCredential).await()
                    val fbUser = authRes.user

                    if (fbUser != null) {
                        val email = fbUser.email.orEmpty().lowercase()
                        val isRootAdmin = isInitialAdminEmail(email)
                        val name = fbUser.displayName ?: email.substringBefore("@")
                        val role = if (isRootAdmin) UserRole.ADMIN else UserRole.TECHNICIAN

                        _currentUser.value = SessionUser(
                            uid = fbUser.uid,
                            email = email,
                            displayName = name,
                            authProvider = "GOOGLE"
                        )
                        _currentRole.value = role
                        _isPendingActivation.value = !isRootAdmin

                        persistSession(fbUser.uid, email, name, role.code, !isRootAdmin, null)
                    }
                    _isLoading.value = false
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Unexpected credential returned from Google Play services."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                _isLoading.value = false
                _errorMessage.value = e.localizedMessage ?: "Sign-in failed. Please try again."
            }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                auth.signOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            } finally {
                prefs.edit().clear().apply()
                _currentUser.value = null
                _currentProfile.value = null
                _currentRole.value = UserRole.TECHNICIAN
                _isPendingActivation.value = false
                _accessToken.value = null
                _errorMessage.value = null
                _statusMessage.value = null
                onComplete()
            }
        }
    }
}
