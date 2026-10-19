package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.UserSessionManager
import com.example.camera.WatermarkHelper
import com.example.data.db.AppDatabase
import com.example.data.model.FirestoreUser
import com.example.data.model.GpsData
import com.example.data.model.GsmData
import com.example.data.model.SiteAccessStatus
import com.example.data.model.SitePhoto
import com.example.data.model.SiteReportEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.FirestoreRepository
import com.example.email.EmailHelper
import com.example.email.ZohoSmtpClient
import com.example.location.GpsManager
import com.example.network.NetworkMonitor
import com.example.security.SecurityHelper
import com.example.telephony.GsmManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SiteAccessViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.siteReportDao()
    private val userDao = db.userDao()
    private val gpsManager = GpsManager(application)
    private val gsmManager = GsmManager(application)
    val networkMonitor = NetworkMonitor(application)

    val firestoreRepository = FirestoreRepository(application)
    val sessionManager = UserSessionManager(application, firestoreRepository, viewModelScope)

    val currentUser = sessionManager.currentUser
    val currentProfile = sessionManager.currentProfile
    val currentRole = sessionManager.currentRole

    private val prefs = application.getSharedPreferences("mobiserve_settings", Context.MODE_PRIVATE)

    // Form State
    val siteId = MutableStateFlow("MOB-DZ-1044")
    val siteName = MutableStateFlow("Algiers Hydra Relay")
    val operator = MutableStateFlow("Mobiserve / Djezzy")
    val status = MutableStateFlow(SiteAccessStatus.ACCORDED)
    val refusalReason = MutableStateFlow("Site security guard requested regional work permit copy")
    val notes = MutableStateFlow("Routine scheduled site audit and telecom cabinet access inspection.")
    val technicianName = MutableStateFlow(prefs.getString("tech_name", "M. Djemil") ?: "M. Djemil")
    val technicianPhone = MutableStateFlow(prefs.getString("tech_phone", "+213 550 12 34 56") ?: "+213 550 12 34 56")
    val recipientEmail = MutableStateFlow(prefs.getString("recipient_email", EmailHelper.DEFAULT_ZOHO_RECIPIENT) ?: EmailHelper.DEFAULT_ZOHO_RECIPIENT)

    // Admin Config: Mail Session & Group Mail State
    val zohoSenderEmail = MutableStateFlow(prefs.getString("zoho_sender", ZohoSmtpClient.SENDER_EMAIL) ?: ZohoSmtpClient.SENDER_EMAIL)
    val sessionSenderName = MutableStateFlow(prefs.getString("session_sender_name", "Mobiserve Field Operations NOC") ?: "Mobiserve Field Operations NOC")
    val ccRecipientsEmail = MutableStateFlow(prefs.getString("cc_group_emails", "supervision@mobiserveholding.com, om@mobiserveholding.com") ?: "supervision@mobiserveholding.com, om@mobiserveholding.com")
    val zohoPassword = MutableStateFlow(
        prefs.getString("zoho_password", null)?.takeIf { it.isNotBlank() }
            ?: SecurityHelper.getDefaultSenderPassword()
    )
    val zohoHost = MutableStateFlow(prefs.getString("zoho_host", ZohoSmtpClient.DEFAULT_ZOHO_HOST) ?: ZohoSmtpClient.DEFAULT_ZOHO_HOST)
    val zohoPort = MutableStateFlow(prefs.getInt("zoho_port", ZohoSmtpClient.DEFAULT_ZOHO_PORT))

    // Administrator Security Lock
    val isAdminUnlocked = MutableStateFlow(false)

    // Direct Sending State
    private val _isSendingEmail = MutableStateFlow(false)
    val isSendingEmail: StateFlow<Boolean> = _isSendingEmail.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _smtpStatusMessage = MutableStateFlow<String?>(null)
    val smtpStatusMessage: StateFlow<String?> = _smtpStatusMessage.asStateFlow()

    private val _smtpDetailedError = MutableStateFlow<String?>(null)
    val smtpDetailedError: StateFlow<String?> = _smtpDetailedError.asStateFlow()

    // Sensors
    private val _gpsData = MutableStateFlow(GpsData())
    val gpsData: StateFlow<GpsData> = _gpsData.asStateFlow()

    private val _gsmData = MutableStateFlow(GsmData())
    val gsmData: StateFlow<GsmData> = _gsmData.asStateFlow()

    private val _isRefreshingGps = MutableStateFlow(false)
    val isRefreshingGps: StateFlow<Boolean> = _isRefreshingGps.asStateFlow()

    private val _isRefreshingGsm = MutableStateFlow(false)
    val isRefreshingGsm: StateFlow<Boolean> = _isRefreshingGsm.asStateFlow()

    // Photos
    private val _photos = MutableStateFlow<List<SitePhoto>>(emptyList())
    val photos: StateFlow<List<SitePhoto>> = _photos.asStateFlow()

    private val _isProcessingPhoto = MutableStateFlow(false)
    val isProcessingPhoto: StateFlow<Boolean> = _isProcessingPhoto.asStateFlow()

    // CameraX Dedicated Screen State
    private val _isCameraOpen = MutableStateFlow(false)
    val isCameraOpen: StateFlow<Boolean> = _isCameraOpen.asStateFlow()

    private val _activeCameraTag = MutableStateFlow("Site Entrance")
    val activeCameraTag: StateFlow<String> = _activeCameraTag.asStateFlow()

    private val _lastCapturedPhoto = MutableStateFlow<SitePhoto?>(null)
    val lastCapturedPhoto: StateFlow<SitePhoto?> = _lastCapturedPhoto.asStateFlow()

    // Status Message / Toast
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Email Preview Dialog
    private val _showEmailPreview = MutableStateFlow(false)
    val showEmailPreview: StateFlow<Boolean> = _showEmailPreview.asStateFlow()

    // Direct Send Result Dialog
    private val _showSendResultDialog = MutableStateFlow(false)
    val showSendResultDialog: StateFlow<Boolean> = _showSendResultDialog.asStateFlow()

    // Network Connectivity Flow
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, networkMonitor.isCurrentlyConnected())

    // Queue & Sync States
    private val _isSyncingQueue = MutableStateFlow(false)
    val isSyncingQueue: StateFlow<Boolean> = _isSyncingQueue.asStateFlow()

    val pendingQueue: StateFlow<List<SiteReportEntity>> = dao.getPendingQueue()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingCount: StateFlow<Int> = dao.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // User Management Flow
    val allUsers: StateFlow<List<UserEntity>> = userDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // History from Room
    val historyReports: StateFlow<List<SiteReportEntity>> = dao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refreshLocation()
        refreshGsm()

        // Seed default users across all requested roles if database is empty
        viewModelScope.launch {
            if (userDao.getUserCount() == 0) {
                userDao.insertUsers(
                    listOf(
                        UserEntity(
                            name = "M. Djemil",
                            email = "mdjemil@mobiserveholding.com",
                            phone = "+213 550 12 34 56",
                            role = "ADMIN",
                            department = "Management / NOC",
                            region = "National"
                        ),
                        UserEntity(
                            name = "Karim Benali",
                            email = "kbenali@mobiserveholding.com",
                            phone = "+213 551 22 33 44",
                            role = "TECHNICIAN",
                            department = "Field Services",
                            region = "Algiers North"
                        ),
                        UserEntity(
                            name = "Samir Hadj",
                            email = "shadj@mobiserveholding.com",
                            phone = "+213 552 44 55 66",
                            role = "OM",
                            department = "Operations & Maintenance",
                            region = "Central"
                        ),
                        UserEntity(
                            name = "Amine Mansour",
                            email = "amansour@mobiserveholding.com",
                            phone = "+213 553 66 77 88",
                            role = "SUPERVISION",
                            department = "Regional Supervision",
                            region = "Oran & West"
                        ),
                        UserEntity(
                            name = "Yacine Cherif",
                            email = "ycherif@mobiserveholding.com",
                            phone = "+213 554 88 99 00",
                            role = "DISPATCHER",
                            department = "NOC Work Orders",
                            region = "National"
                        ),
                        UserEntity(
                            name = "Nadia Belkacem",
                            email = "nbelkacem@mobiserveholding.com",
                            phone = "+213 555 11 22 33",
                            role = "ACQUISITION",
                            department = "Site Acquisition & Legal",
                            region = "National"
                        )
                    )
                )
            }
        }

        // Automatic synchronization when cellular or Wi-Fi connectivity is restored
        viewModelScope.launch {
            var wasOffline = !networkMonitor.isCurrentlyConnected()
            isOnline.collect { online ->
                if (online && wasOffline) {
                    syncPendingReports(isAutomatic = true)
                }
                wasOffline = !online
            }
        }

        // Synchronize admin permissions with signed-in RBAC role
        viewModelScope.launch {
            currentRole.collectLatest { role ->
                if (role == UserRole.ADMIN) {
                    isAdminUnlocked.value = true
                }
            }
        }

        // Observe cloud mail configuration and sync to session state
        viewModelScope.launch {
            currentUser.collectLatest { user ->
                if (user != null) {
                    try {
                        firestoreRepository.observeMailConfig().collectLatest { config ->
                            if (config != null) {
                                if (config.senderEmail.isNotBlank()) zohoSenderEmail.value = config.senderEmail
                                if (config.senderName.isNotBlank()) sessionSenderName.value = config.senderName
                                if (config.toGroup.isNotBlank()) recipientEmail.value = config.toGroup
                                if (config.ccGroup.isNotBlank()) ccRecipientsEmail.value = config.ccGroup
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore unauthenticated initial flow
                    }
                }
            }
        }
    }

    fun setSiteId(value: String) { siteId.value = value }
    fun setSiteName(value: String) { siteName.value = value }
    fun setOperator(value: String) { operator.value = value }
    fun setStatus(value: SiteAccessStatus) { status.value = value }
    fun setRefusalReason(value: String) { refusalReason.value = value }
    fun setNotes(value: String) { notes.value = value }

    fun setTechnicianName(value: String) {
        technicianName.value = value
        prefs.edit().putString("tech_name", value).apply()
    }

    fun setTechnicianPhone(value: String) {
        technicianPhone.value = value
        prefs.edit().putString("tech_phone", value).apply()
    }

    fun setRecipientEmail(value: String) {
        recipientEmail.value = value
        prefs.edit().putString("recipient_email", value).apply()
    }

    fun setCcRecipientsEmail(value: String) {
        ccRecipientsEmail.value = value
        prefs.edit().putString("cc_group_emails", value).apply()
    }

    fun setSessionSenderName(value: String) {
        sessionSenderName.value = value
        prefs.edit().putString("session_sender_name", value).apply()
    }

    fun setZohoSenderEmail(value: String) {
        zohoSenderEmail.value = value
        prefs.edit().putString("zoho_sender", value).apply()
    }

    fun setZohoPassword(value: String) {
        zohoPassword.value = value
        prefs.edit().putString("zoho_password", value).apply()
    }

    fun setZohoHost(value: String) {
        zohoHost.value = value
        prefs.edit().putString("zoho_host", value).apply()
    }

    fun setZohoPort(value: Int) {
        zohoPort.value = value
        prefs.edit().putInt("zoho_port", value).apply()
    }

    fun saveMailSettingsToCloud() {
        viewModelScope.launch {
            try {
                firestoreRepository.saveMailConfig(
                    senderEmail = zohoSenderEmail.value,
                    senderName = sessionSenderName.value,
                    toGroup = recipientEmail.value,
                    ccGroup = ccRecipientsEmail.value
                )
                _userMessage.value = "Mail routing & sender configuration saved to cloud database!"
            } catch (e: Exception) {
                _userMessage.value = "Settings saved locally (${e.localizedMessage})"
            }
        }
    }

    fun unlockAdmin(pin: String): Boolean {
        val ok = SecurityHelper.verifyAdminPin(pin)
        if (ok) {
            isAdminUnlocked.value = true
        }
        return ok
    }

    fun lockAdmin() {
        isAdminUnlocked.value = false
    }

    fun addEmailToRecipientGroup(email: String) {
        val current = recipientEmail.value.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        if (!current.contains(email.trim())) {
            current.add(email.trim())
            setRecipientEmail(current.joinToString(", "))
            _userMessage.value = "Added $email to TO Mail Group"
        } else {
            _userMessage.value = "$email is already in TO Mail Group"
        }
    }

    fun addEmailToCcGroup(email: String) {
        val current = ccRecipientsEmail.value.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        if (!current.contains(email.trim())) {
            current.add(email.trim())
            setCcRecipientsEmail(current.joinToString(", "))
            _userMessage.value = "Added $email to CC Mail Group"
        } else {
            _userMessage.value = "$email is already in CC Mail Group"
        }
    }

    fun addRoleEmailsToCcGroup(roleCode: String) {
        val users = allUsers.value
        val roleEmails = users.filter { it.role.equals(roleCode, ignoreCase = true) }.map { it.email.trim() }
        val current = ccRecipientsEmail.value.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        var addedCount = 0
        roleEmails.forEach { email ->
            if (email.isNotBlank() && !current.contains(email)) {
                current.add(email)
                addedCount++
            }
        }
        if (addedCount > 0) {
            setCcRecipientsEmail(current.joinToString(", "))
            _userMessage.value = "Added $addedCount $roleCode user(s) to CC Group"
        } else {
            _userMessage.value = "All active $roleCode users are already in CC Group"
        }
    }

    fun addRoleEmailsToToGroup(roleCode: String) {
        val users = allUsers.value
        val roleEmails = users.filter { it.role.equals(roleCode, ignoreCase = true) }.map { it.email.trim() }
        val current = recipientEmail.value.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        var addedCount = 0
        roleEmails.forEach { email ->
            if (email.isNotBlank() && !current.contains(email)) {
                current.add(email)
                addedCount++
            }
        }
        if (addedCount > 0) {
            setRecipientEmail(current.joinToString(", "))
            _userMessage.value = "Added $addedCount $roleCode user(s) to TO Group"
        } else {
            _userMessage.value = "All active $roleCode users are already in TO Group"
        }
    }

    // User CRUD Operations
    fun createUser(name: String, email: String, phone: String, role: String, department: String, region: String) {
        viewModelScope.launch {
            val user = UserEntity(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                role = role,
                department = department.ifBlank { "Field Services" },
                region = region.ifBlank { "National" }
            )
            val id = userDao.insertUser(user)
            try {
                firestoreRepository.saveUser(
                    FirestoreUser(
                        uid = "user_${id}_${System.currentTimeMillis()}",
                        name = user.name,
                        email = user.email,
                        phone = user.phone,
                        role = user.role,
                        department = user.department,
                        region = user.region,
                        isActive = true
                    )
                )
            } catch (e: Exception) {
                // Saved in local database
            }
            _userMessage.value = "User '${user.name}' created with role ${user.role}."
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            userDao.updateUser(user)
            try {
                firestoreRepository.saveUser(
                    FirestoreUser(
                        uid = "user_${user.id}",
                        name = user.name,
                        email = user.email,
                        phone = user.phone,
                        role = user.role,
                        department = user.department,
                        region = user.region,
                        isActive = user.isActive
                    )
                )
            } catch (e: Exception) {
                // Saved in local database
            }
            _userMessage.value = "User '${user.name}' updated."
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            userDao.deleteUser(user)
            try {
                firestoreRepository.deleteUser("user_${user.id}")
            } catch (e: Exception) {
                // Removed from local database
            }
            _userMessage.value = "User '${user.name}' removed."
        }
    }

    fun selectTechnicianUser(user: UserEntity) {
        setTechnicianName(user.name)
        setTechnicianPhone(user.phone)
        _userMessage.value = "Active technician set to ${user.name} (${user.role})"
    }

    fun dismissUserMessage() { _userMessage.value = null }
    fun setShowEmailPreview(show: Boolean) { _showEmailPreview.value = show }
    fun setShowSendResultDialog(show: Boolean) { _showSendResultDialog.value = show }

    fun openCamera(tag: String = "Site Entrance") {
        _activeCameraTag.value = tag
        _isCameraOpen.value = true
    }

    fun closeCamera() {
        _isCameraOpen.value = false
    }

    fun setActiveCameraTag(tag: String) {
        _activeCameraTag.value = tag
    }

    fun refreshLocation() {
        viewModelScope.launch {
            _isRefreshingGps.value = true
            try {
                val loc = gpsManager.getCurrentLocation()
                _gpsData.value = loc
            } catch (e: Exception) {
                // Keep existing or fallback
            } finally {
                _isRefreshingGps.value = false
            }
        }
    }

    fun refreshGsm() {
        viewModelScope.launch {
            _isRefreshingGps.value = true
            try {
                val cell = gsmManager.readCurrentCellInfo()
                _gsmData.value = cell
            } catch (e: Exception) {
                // Keep fallback
            } finally {
                _isRefreshingGps.value = false
            }
        }
    }

    fun addPhotoBitmap(bitmap: Bitmap, tag: String = "Site Photo") {
        viewModelScope.launch {
            _isProcessingPhoto.value = true
            try {
                val stampedFile = WatermarkHelper.createStampedPhoto(
                    context = getApplication(),
                    sourceBitmap = bitmap,
                    siteId = siteId.value,
                    operator = operator.value,
                    status = status.value,
                    gps = _gpsData.value,
                    gsm = _gsmData.value,
                    tag = tag
                )
                val newPhoto = SitePhoto(
                    id = stampedFile.name,
                    filePath = stampedFile.absolutePath,
                    tag = tag,
                    timestamp = System.currentTimeMillis(),
                    latitude = _gpsData.value.latitude,
                    longitude = _gpsData.value.longitude,
                    cellId = _gsmData.value.cellId
                )
                _photos.value = _photos.value + newPhoto
                _lastCapturedPhoto.value = newPhoto
                _userMessage.value = "Photo captured & geo-stamped successfully!"
            } catch (e: Exception) {
                _userMessage.value = "Error stamping photo: ${e.localizedMessage}"
            } finally {
                _isProcessingPhoto.value = false
            }
        }
    }

    fun addSimulatedPhoto(tag: String) {
        viewModelScope.launch {
            _isProcessingPhoto.value = true
            try {
                val stampedFile = WatermarkHelper.createSyntheticSitePhoto(
                    context = getApplication(),
                    siteId = siteId.value,
                    operator = operator.value,
                    status = status.value,
                    gps = _gpsData.value,
                    gsm = _gsmData.value,
                    tag = tag
                )
                val newPhoto = SitePhoto(
                    id = stampedFile.name,
                    filePath = stampedFile.absolutePath,
                    tag = tag,
                    timestamp = System.currentTimeMillis(),
                    latitude = _gpsData.value.latitude,
                    longitude = _gpsData.value.longitude,
                    cellId = _gsmData.value.cellId
                )
                _photos.value = _photos.value + newPhoto
                _lastCapturedPhoto.value = newPhoto
                _userMessage.value = "Field inspection photo stamped ($tag)"
            } catch (e: Exception) {
                _userMessage.value = "Error adding photo: ${e.localizedMessage}"
            } finally {
                _isProcessingPhoto.value = false
            }
        }
    }

    fun removePhoto(photo: SitePhoto) {
        val file = File(photo.filePath)
        if (file.exists()) {
            file.delete()
        }
        _photos.value = _photos.value.filter { it.id != photo.id }
    }

    fun saveReportToDb(syncStatus: String = "PENDING", errorMsg: String? = null) {
        viewModelScope.launch {
            insertReportEntity(syncStatus, errorMsg)
            _userMessage.value = "Site report saved to local audit history!"
        }
    }

    suspend fun insertReportEntity(syncStatus: String = "PENDING", errorMsg: String? = null): Long {
        val entity = SiteReportEntity(
            siteId = siteId.value.ifBlank { "SITE-UNKNOWN" },
            siteName = siteName.value.ifBlank { "Site" },
            operator = operator.value.ifBlank { "Mobiserve" },
            status = status.value.code,
            refusalReason = if (status.value == SiteAccessStatus.REFUSED) refusalReason.value else null,
            technicianName = technicianName.value,
            technicianPhone = technicianPhone.value,
            recipientEmail = recipientEmail.value,
            notes = notes.value,
            latitude = _gpsData.value.latitude,
            longitude = _gpsData.value.longitude,
            altitude = _gpsData.value.altitude,
            accuracy = _gpsData.value.accuracy,
            address = _gpsData.value.address,
            gsmOperator = _gsmData.value.operatorName,
            networkType = _gsmData.value.networkType,
            mcc = _gsmData.value.mcc,
            mnc = _gsmData.value.mnc,
            lac = _gsmData.value.lacTac,
            cellId = _gsmData.value.cellId,
            signalDbm = _gsmData.value.signalDbm,
            photoPaths = _photos.value.joinToString("|") { it.filePath },
            syncStatus = syncStatus,
            syncErrorMessage = errorMsg,
            syncedAt = if (syncStatus == "SYNCED") System.currentTimeMillis() else null
        )
        return dao.insertReport(entity)
    }

    fun deleteReport(entity: SiteReportEntity) {
        viewModelScope.launch {
            dao.deleteReport(entity)
            _userMessage.value = "Report deleted from history"
        }
    }

    fun loadFromEntity(entity: SiteReportEntity) {
        siteId.value = entity.siteId
        siteName.value = entity.siteName
        operator.value = entity.operator
        status.value = if (entity.status == "ACCORDED") SiteAccessStatus.ACCORDED else SiteAccessStatus.REFUSED
        refusalReason.value = entity.refusalReason ?: ""
        notes.value = entity.notes
        technicianName.value = entity.technicianName
        technicianPhone.value = entity.technicianPhone
        recipientEmail.value = entity.recipientEmail

        val paths = entity.photoPaths.split("|").filter { it.isNotBlank() }
        val loadedPhotos = paths.mapNotNull { path ->
            val f = File(path)
            if (f.exists()) {
                SitePhoto(
                    id = f.name,
                    filePath = f.absolutePath,
                    tag = "Archived Photo",
                    latitude = entity.latitude,
                    longitude = entity.longitude,
                    cellId = entity.cellId
                )
            } else null
        }
        _photos.value = loadedPhotos
        _userMessage.value = "Loaded site report #${entity.id} (${entity.siteId})"
    }

    fun getPreparedSubject(): String {
        return EmailHelper.generateSubject(
            siteId = siteId.value,
            operator = operator.value,
            status = status.value
        )
    }

    fun getPreparedBody(): String {
        return EmailHelper.generateBody(
            siteId = siteId.value,
            siteName = siteName.value,
            operator = operator.value,
            status = status.value,
            refusalReason = refusalReason.value,
            notes = notes.value,
            technicianName = technicianName.value,
            technicianPhone = technicianPhone.value,
            gps = _gpsData.value,
            gsm = _gsmData.value,
            photoCount = _photos.value.size
        )
    }

    fun getAttachedPhotoFiles(): List<File> {
        return _photos.value.map { File(it.filePath) }.filter { it.exists() }
    }

    fun getCcList(): List<String> {
        return ccRecipientsEmail.value.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
    }

    // Direct Zoho SMTP Email Sending with Offline Queue Fallback & Group Mail
    fun sendDirectZohoEmail() {
        viewModelScope.launch {
            _isSendingEmail.value = true

            val isConnected = networkMonitor.isCurrentlyConnected()
            if (!isConnected) {
                // Cellular/network data is unavailable: Cache immediately in Room queue
                val reportId = insertReportEntity(syncStatus = "PENDING", errorMsg = "Cached offline (no cellular connectivity)")
                _isSendingEmail.value = false
                _smtpStatusMessage.value = "Cellular connection unavailable. Site audit log #$reportId cached locally in Room storage queue.\n\nIt will automatically synchronize and send to Zoho Mail once connectivity is restored."
                _smtpDetailedError.value = "Offline Queue active. Photos and telemetry safely saved to local database."
                _showSendResultDialog.value = true
                _userMessage.value = "Site audit queued locally in Room (Offline Mode) ✓"
                return@launch
            }

            // Cellular/Internet available: Attempt transmission
            _smtpStatusMessage.value = "Connecting to Zoho SMTP as ${zohoSenderEmail.value}..."
            _smtpDetailedError.value = null

            val recipientsList = recipientEmail.value.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
            val ccList = getCcList()
            val subject = getPreparedSubject()
            val body = getPreparedBody()
            val attachments = getAttachedPhotoFiles()

            // Save report first with SYNCING status
            val reportId = insertReportEntity(syncStatus = "SYNCING")

            val result = ZohoSmtpClient.sendEmail(
                host = zohoHost.value,
                port = zohoPort.value,
                senderEmail = zohoSenderEmail.value,
                sessionSenderName = sessionSenderName.value,
                password = zohoPassword.value,
                recipients = recipientsList,
                ccRecipients = ccList,
                subject = subject,
                bodyText = body,
                attachments = attachments
            )

            _isSendingEmail.value = false
            when (result) {
                is ZohoSmtpClient.SmtpResult.Success -> {
                    dao.updateSyncStatus(reportId, "SYNCED", null, System.currentTimeMillis())
                    _smtpStatusMessage.value = result.message
                    _smtpDetailedError.value = null
                    _showSendResultDialog.value = true
                    _userMessage.value = "Email successfully dispatched from ${zohoSenderEmail.value}!"
                }
                is ZohoSmtpClient.SmtpResult.Error -> {
                    dao.updateSyncStatus(reportId, "FAILED", result.message, null)
                    _smtpStatusMessage.value = result.message
                    _smtpDetailedError.value = "Report stored in local queue (#$reportId) for automatic retry when network stabilizes."
                    _showSendResultDialog.value = true
                    _userMessage.value = "Failed to send email. Audit retained in offline queue."
                }
            }
        }
    }

    // Automatic or manual synchronization of all queued offline reports
    fun syncPendingReports(isAutomatic: Boolean = false) {
        viewModelScope.launch {
            if (_isSyncingQueue.value) return@launch
            if (!networkMonitor.isCurrentlyConnected()) {
                if (!isAutomatic) {
                    _userMessage.value = "Cannot sync: Cellular / network data is currently offline."
                }
                return@launch
            }

            val pendingList = dao.getPendingReportsList()
            if (pendingList.isEmpty()) {
                if (!isAutomatic) {
                    _userMessage.value = "All site access audits are already synced to Zoho Mail!"
                }
                return@launch
            }

            _isSyncingQueue.value = true
            var syncedCount = 0
            val ccList = getCcList()

            for (report in pendingList) {
                dao.updateSyncStatus(report.id, "SYNCING", null, null)

                val recipients = report.recipientEmail.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
                val accessStatus = if (report.status == "ACCORDED") SiteAccessStatus.ACCORDED else SiteAccessStatus.REFUSED
                val subject = EmailHelper.generateSubject(report.siteId, report.operator, accessStatus)

                val photoFiles = report.photoPaths.split("|")
                    .filter { it.isNotBlank() }
                    .map { File(it) }
                    .filter { it.exists() }

                val body = EmailHelper.generateBody(
                    siteId = report.siteId,
                    siteName = report.siteName,
                    operator = report.operator,
                    status = accessStatus,
                    refusalReason = report.refusalReason,
                    notes = report.notes + "\n[Offline Queue Synced at: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}]",
                    technicianName = report.technicianName,
                    technicianPhone = report.technicianPhone,
                    gps = GpsData(
                        latitude = report.latitude,
                        longitude = report.longitude,
                        altitude = report.altitude,
                        accuracy = report.accuracy,
                        address = report.address
                    ),
                    gsm = GsmData(
                        operatorName = report.gsmOperator,
                        networkType = report.networkType,
                        mcc = report.mcc,
                        mnc = report.mnc,
                        lacTac = report.lac,
                        cellId = report.cellId,
                        signalDbm = report.signalDbm
                    ),
                    photoCount = photoFiles.size
                )

                val result = ZohoSmtpClient.sendEmail(
                    host = zohoHost.value,
                    port = zohoPort.value,
                    senderEmail = zohoSenderEmail.value,
                    sessionSenderName = sessionSenderName.value,
                    password = zohoPassword.value,
                    recipients = recipients,
                    ccRecipients = ccList,
                    subject = subject,
                    bodyText = body,
                    attachments = photoFiles
                )

                if (result is ZohoSmtpClient.SmtpResult.Success) {
                    dao.updateSyncStatus(report.id, "SYNCED", null, System.currentTimeMillis())
                    syncedCount++
                } else if (result is ZohoSmtpClient.SmtpResult.Error) {
                    dao.updateSyncStatus(report.id, "FAILED", result.message, null)
                }
            }

            _isSyncingQueue.value = false
            if (syncedCount > 0) {
                _userMessage.value = if (isAutomatic)
                    "Auto-sync complete: $syncedCount queued audit(s) synced to Zoho Mail!"
                else
                    "Successfully synced $syncedCount audit report(s) to Zoho Mail!"
            }
        }
    }

    // Sync a single report from the history list
    fun syncSingleReport(report: SiteReportEntity) {
        viewModelScope.launch {
            if (!networkMonitor.isCurrentlyConnected()) {
                _userMessage.value = "Cannot sync: Cellular data is offline. Will auto-sync when restored."
                return@launch
            }

            dao.updateSyncStatus(report.id, "SYNCING", null, null)

            val recipients = report.recipientEmail.split(",", ";").map { it.trim() }.filter { it.isNotBlank() }
            val ccList = getCcList()
            val accessStatus = if (report.status == "ACCORDED") SiteAccessStatus.ACCORDED else SiteAccessStatus.REFUSED
            val subject = EmailHelper.generateSubject(report.siteId, report.operator, accessStatus)

            val photoFiles = report.photoPaths.split("|")
                .filter { it.isNotBlank() }
                .map { File(it) }
                .filter { it.exists() }

            val body = EmailHelper.generateBody(
                siteId = report.siteId,
                siteName = report.siteName,
                operator = report.operator,
                status = accessStatus,
                refusalReason = report.refusalReason,
                notes = report.notes,
                technicianName = report.technicianName,
                technicianPhone = report.technicianPhone,
                gps = GpsData(
                    latitude = report.latitude,
                    longitude = report.longitude,
                    altitude = report.altitude,
                    accuracy = report.accuracy,
                    address = report.address
                ),
                gsm = GsmData(
                    operatorName = report.gsmOperator,
                    networkType = report.networkType,
                    mcc = report.mcc,
                    mnc = report.mnc,
                    lacTac = report.lac,
                    cellId = report.cellId,
                    signalDbm = report.signalDbm
                ),
                photoCount = photoFiles.size
            )

            val result = ZohoSmtpClient.sendEmail(
                host = zohoHost.value,
                port = zohoPort.value,
                senderEmail = zohoSenderEmail.value,
                sessionSenderName = sessionSenderName.value,
                password = zohoPassword.value,
                recipients = recipients,
                ccRecipients = ccList,
                subject = subject,
                bodyText = body,
                attachments = photoFiles
            )

            when (result) {
                is ZohoSmtpClient.SmtpResult.Success -> {
                    dao.updateSyncStatus(report.id, "SYNCED", null, System.currentTimeMillis())
                    _userMessage.value = "Report #${report.id} (${report.siteId}) successfully synced to Zoho Mail!"
                }
                is ZohoSmtpClient.SmtpResult.Error -> {
                    dao.updateSyncStatus(report.id, "FAILED", result.message, null)
                    _userMessage.value = "Sync failed: ${result.message}"
                }
            }
        }
    }

    fun testZohoConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _smtpStatusMessage.value = "Testing authentication with Zoho SMTP..."
            _smtpDetailedError.value = null

            val result = ZohoSmtpClient.testConnection(
                host = zohoHost.value,
                port = zohoPort.value,
                senderEmail = zohoSenderEmail.value,
                password = zohoPassword.value
            )

            _isTestingConnection.value = false
            when (result) {
                is ZohoSmtpClient.SmtpResult.Success -> {
                    _smtpStatusMessage.value = result.message
                    _smtpDetailedError.value = null
                    _showSendResultDialog.value = true
                    _userMessage.value = "Zoho SMTP Connection verified successfully!"
                }
                is ZohoSmtpClient.SmtpResult.Error -> {
                    _smtpStatusMessage.value = result.message
                    _smtpDetailedError.value = result.details
                    _showSendResultDialog.value = true
                    _userMessage.value = "Zoho Authentication test failed."
                }
            }
        }
    }
}
