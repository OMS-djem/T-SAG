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
import com.example.data.model.FirestoreUser
import com.example.data.model.UserRole
import com.example.data.remote.FirestoreRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserSessionManager(
    private val context: Context,
    private val firestoreRepository: FirestoreRepository,
    private val scope: CoroutineScope
) {
    private val auth: FirebaseAuth = Firebase.auth
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _currentProfile = MutableStateFlow<FirestoreUser?>(null)
    val currentProfile: StateFlow<FirestoreUser?> = _currentProfile.asStateFlow()

    private val _currentRole = MutableStateFlow(UserRole.TECHNICIAN)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var profileJob: Job? = null

    init {
        // Register Auth state listener
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                bindUserProfile(user)
            } else {
                profileJob?.cancel()
                _currentProfile.value = null
                _currentRole.value = UserRole.TECHNICIAN
            }
        }

        // Silent auto sign-in attempt
        if (auth.currentUser == null) {
            attemptAutoSignIn()
        } else {
            bindUserProfile(auth.currentUser!!)
        }
    }

    private fun bindUserProfile(user: FirebaseUser) {
        profileJob?.cancel()
        profileJob = scope.launch(Dispatchers.IO) {
            // First time sync: ensure user document exists in Firestore
            try {
                firestoreRepository.observeUserProfile(user.uid).collectLatest { profile ->
                    if (profile == null) {
                        // User not in Firestore yet, create default record
                        // If admin email bootstrap or first user, assign ADMIN
                        val assignedRole = if (user.email.equals("etb.djemil@gmail.com", ignoreCase = true)) {
                            UserRole.ADMIN.code
                        } else {
                            UserRole.TECHNICIAN.code
                        }

                        val newProfile = FirestoreUser(
                            uid = user.uid,
                            name = user.displayName ?: "Field Engineer",
                            email = user.email ?: "",
                            phone = user.phoneNumber ?: "",
                            role = assignedRole,
                            department = "Telecom Field Operations",
                            region = "National",
                            isActive = true
                        )
                        firestoreRepository.saveUser(newProfile)
                    } else {
                        _currentProfile.value = profile
                        _currentRole.value = profile.roleEnum
                    }
                }
            } catch (e: Exception) {
                Log.e("UserSessionManager", "Error observing user profile", e)
                // Fallback role
                if (user.email.equals("etb.djemil@gmail.com", ignoreCase = true)) {
                    _currentRole.value = UserRole.ADMIN
                }
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun attemptAutoSignIn() {
        if (auth.currentUser != null) return

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.w("UserSessionManager", "default_web_client_id not found")
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    auth.signInWithCredential(authCredential).await()
                }
            } catch (e: Exception) {
                Log.d("UserSessionManager", "Silent auto sign-in not available, awaiting user interaction")
            }
        }
    }

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
                    auth.signInWithCredential(authCredential).await()
                    _isLoading.value = false
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Unexpected credential returned from Google Play services."
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
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
                _currentProfile.value = null
                _currentRole.value = UserRole.TECHNICIAN
                onComplete()
            }
        }
    }
}
