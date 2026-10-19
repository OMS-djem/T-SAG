package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmailDispatchSection
import com.example.ui.components.EmailPreviewDialog
import com.example.ui.components.GpsTelemetryCard
import com.example.ui.components.GsmTelemetryCard
import com.example.ui.components.PhotoEvidenceSection
import com.example.ui.components.SendResultDialog
import com.example.ui.components.SiteDetailsForm
import com.example.ui.components.StatusSelectorCard
import com.example.ui.theme.TelecomNavyPrimary
import com.example.ui.viewmodel.SiteAccessViewModel

@Composable
fun SiteAuditScreen(
    viewModel: SiteAccessViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val siteId by viewModel.siteId.collectAsState()
    val siteName by viewModel.siteName.collectAsState()
    val operator by viewModel.operator.collectAsState()
    val status by viewModel.status.collectAsState()
    val refusalReason by viewModel.refusalReason.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val technicianName by viewModel.technicianName.collectAsState()
    val technicianPhone by viewModel.technicianPhone.collectAsState()
    val recipientEmail by viewModel.recipientEmail.collectAsState()
    val ccRecipientsEmail by viewModel.ccRecipientsEmail.collectAsState()

    val zohoSenderEmail by viewModel.zohoSenderEmail.collectAsState()
    val zohoPassword by viewModel.zohoPassword.collectAsState()
    val isSendingEmail by viewModel.isSendingEmail.collectAsState()
    val showSendResultDialog by viewModel.showSendResultDialog.collectAsState()
    val smtpStatusMessage by viewModel.smtpStatusMessage.collectAsState()
    val smtpDetailedError by viewModel.smtpDetailedError.collectAsState()

    val gpsData by viewModel.gpsData.collectAsState()
    val isRefreshingGps by viewModel.isRefreshingGps.collectAsState()
    val gsmData by viewModel.gsmData.collectAsState()
    val isRefreshingGsm by viewModel.isRefreshingGsm.collectAsState()

    val photos by viewModel.photos.collectAsState()
    val isProcessingPhoto by viewModel.isProcessingPhoto.collectAsState()
    val showPreview by viewModel.showEmailPreview.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    // Permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.refreshLocation()
        }
        if (permissions[Manifest.permission.READ_PHONE_STATE] == true || permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.refreshGsm()
        }
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE
        )
        permissionsLauncher.launch(perms.toTypedArray())
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUserMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .testTag("site_audit_screen")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero Brand Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = TelecomNavyPrimary
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "MOBISERVE HOLDING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Site Access Audit & Telemetry Dispatch",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Status Decision Card (Accorded vs No Access)
        StatusSelectorCard(
            currentStatus = status,
            onStatusSelected = { viewModel.setStatus(it) },
            refusalReason = refusalReason,
            onRefusalReasonChanged = { viewModel.setRefusalReason(it) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Site Identification Form
        SiteDetailsForm(
            siteId = siteId,
            onSiteIdChanged = { viewModel.setSiteId(it) },
            siteName = siteName,
            onSiteNameChanged = { viewModel.setSiteName(it) },
            operator = operator,
            onOperatorChanged = { viewModel.setOperator(it) },
            technicianName = technicianName,
            onTechnicianNameChanged = { viewModel.setTechnicianName(it) },
            technicianPhone = technicianPhone,
            onTechnicianPhoneChanged = { viewModel.setTechnicianPhone(it) },
            notes = notes,
            onNotesChanged = { viewModel.setNotes(it) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Telemetry: GPS Location Card
        GpsTelemetryCard(
            gps = gpsData,
            isRefreshing = isRefreshingGps,
            onRefresh = { viewModel.refreshLocation() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Telemetry: GSM Cellular Info Card
        GsmTelemetryCard(
            gsm = gsmData,
            isRefreshing = isRefreshingGsm,
            onRefresh = { viewModel.refreshGsm() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Photo Evidence & Stamping Section
        PhotoEvidenceSection(
            photos = photos,
            isProcessing = isProcessingPhoto,
            onOpenCamera = { tag -> viewModel.openCamera(tag) },
            onPhotoCaptured = { bitmap, tag -> viewModel.addPhotoBitmap(bitmap, tag) },
            onSimulatePhoto = { tag -> viewModel.addSimulatedPhoto(tag) },
            onDeletePhoto = { photo -> viewModel.removePhoto(photo) }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 6. Email Dispatch & Reporting Section (Direct Zoho SMTP + App Fallback)
        EmailDispatchSection(
            senderEmail = zohoSenderEmail,
            recipientEmail = recipientEmail,
            onRecipientChanged = { viewModel.setRecipientEmail(it) },
            ccEmail = ccRecipientsEmail,
            onCcChanged = { viewModel.setCcRecipientsEmail(it) },
            status = status,
            subject = viewModel.getPreparedSubject(),
            body = viewModel.getPreparedBody(),
            attachedPhotoFiles = viewModel.getAttachedPhotoFiles(),
            isSending = isSendingEmail,
            onSendDirectZoho = { viewModel.sendDirectZohoEmail() },
            onPreviewEmail = { viewModel.setShowEmailPreview(true) },
            onSaveToHistory = { viewModel.saveReportToDb() }
        )

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Email Preview Dialog
    if (showPreview) {
        EmailPreviewDialog(
            recipient = recipientEmail,
            cc = ccRecipientsEmail,
            subject = viewModel.getPreparedSubject(),
            body = viewModel.getPreparedBody(),
            status = status,
            photoFiles = viewModel.getAttachedPhotoFiles(),
            onDismiss = { viewModel.setShowEmailPreview(false) }
        )
    }

    // Send Result / Status Dialog
    if (showSendResultDialog) {
        SendResultDialog(
            statusMessage = smtpStatusMessage,
            detailedError = smtpDetailedError,
            onDismiss = { viewModel.setShowSendResultDialog(false) }
        )
    }
}
