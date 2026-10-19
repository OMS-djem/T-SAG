package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ForwardToInbox
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.security.SecurityHelper
import com.example.ui.components.SendResultDialog
import com.example.ui.components.UserManagementSection
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.AccessRefusedRed
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary
import com.example.ui.viewmodel.SiteAccessViewModel

@Composable
fun SettingsScreen(
    viewModel: SiteAccessViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var selectedSettingsTab by remember { mutableIntStateOf(0) }

    var showPlainPassword by remember { mutableStateOf(false) }
    var showAdminAuthDialog by remember { mutableStateOf(false) }
    var enteredAdminPin by remember { mutableStateOf("") }
    var adminPinError by remember { mutableStateOf<String?>(null) }

    val technicianName by viewModel.technicianName.collectAsState()
    val technicianPhone by viewModel.technicianPhone.collectAsState()
    val recipientEmail by viewModel.recipientEmail.collectAsState()
    val ccRecipientsEmail by viewModel.ccRecipientsEmail.collectAsState()

    val zohoSenderEmail by viewModel.zohoSenderEmail.collectAsState()
    val sessionSenderName by viewModel.sessionSenderName.collectAsState()
    val zohoPassword by viewModel.zohoPassword.collectAsState()
    val zohoHost by viewModel.zohoHost.collectAsState()
    val zohoPort by viewModel.zohoPort.collectAsState()
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()

    val allUsers by viewModel.allUsers.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val showSendResultDialog by viewModel.showSendResultDialog.collectAsState()
    val smtpStatusMessage by viewModel.smtpStatusMessage.collectAsState()
    val smtpDetailedError by viewModel.smtpDetailedError.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        // Admin Security Status Banner
        Surface(
            color = if (isAdminUnlocked) TelecomNavyPrimary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isAdminUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isAdminUnlocked) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isAdminUnlocked) "ADMINISTRATOR MODE ACTIVE" else "Standard Field Mode (Protected)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAdminUnlocked) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAdminUnlocked) "Full access to sender, session, PW, mail groups & users" else "Unlock admin to edit credentials and distribution lists",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isAdminUnlocked) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isAdminUnlocked) {
                    OutlinedButton(
                        onClick = { viewModel.lockAdmin() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Lock Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            enteredAdminPin = ""
                            adminPinError = null
                            showAdminAuthDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).testTag("admin_unlock_button")
                    ) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unlock Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Settings Navigation Tabs
        TabRow(
            selectedTabIndex = selectedSettingsTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TelecomNavyPrimary
        ) {
            Tab(
                selected = selectedSettingsTab == 0,
                onClick = { selectedSettingsTab = 0 },
                text = { Text("Sender & Session", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("nav_settings_smtp_tab")
            )
            Tab(
                selected = selectedSettingsTab == 1,
                onClick = { selectedSettingsTab = 1 },
                text = { Text("Mail Groups", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(imageVector = Icons.Default.ForwardToInbox, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("nav_settings_groups_tab")
            )
            Tab(
                selected = selectedSettingsTab == 2,
                onClick = { selectedSettingsTab = 2 },
                text = { Text("Users & Roles", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(imageVector = Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("nav_settings_users_tab")
            )
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            when (selectedSettingsTab) {
                0 -> {
                    // TAB 0: MAIL SENDER & SESSION CONFIGURATION
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(TelecomNavyPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(text = "Zoho Mail Sender & Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "Direct SMTP SSL relay settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(14.dp))

                                // SENDER EMAIL
                                Text(
                                    text = "SENDER EMAIL ADDRESS (AUTH LOGIN)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = zohoSenderEmail,
                                    onValueChange = { viewModel.setZohoSenderEmail(it) },
                                    enabled = isAdminUnlocked,
                                    label = { Text("Corporate Sender Email") },
                                    placeholder = { Text("mdjemil@mobiserveholding.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // SESSION DISPLAY NAME
                                Text(
                                    text = "SESSION SENDER DISPLAY NAME",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = sessionSenderName,
                                    onValueChange = { viewModel.setSessionSenderName(it) },
                                    enabled = isAdminUnlocked,
                                    label = { Text("From Display Name (Header)") },
                                    placeholder = { Text("Mobiserve Field Operations NOC") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // PASSWORD / APP PASSWORD
                                Text(
                                    text = "SENDER PASSWORD / APP PASSWORD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (!isAdminUnlocked) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = SecurityHelper.MASKED_PW,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            )
                                            Text(
                                                text = "ENCRYPTED",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = zohoPassword,
                                        onValueChange = { viewModel.setZohoPassword(it) },
                                        label = { Text("Zoho Password / App Password") },
                                        visualTransformation = if (showPlainPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        trailingIcon = {
                                            IconButton(onClick = { showPlainPassword = !showPlainPassword }) {
                                                Icon(
                                                    imageVector = if (showPlainPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showPlainPassword) "Hide password" else "Show password"
                                                )
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // HOST & PORT
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = zohoHost,
                                        onValueChange = { viewModel.setZohoHost(it) },
                                        enabled = isAdminUnlocked,
                                        label = { Text("SMTP Host") },
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = zohoPort.toString(),
                                        onValueChange = {
                                            it.toIntOrNull()?.let { p -> viewModel.setZohoPort(p) }
                                        },
                                        enabled = isAdminUnlocked,
                                        label = { Text("Port (SSL)") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // TEST SMTP CONNECTION BUTTON
                                Button(
                                    onClick = { viewModel.testZohoConnection() },
                                    enabled = !isTestingConnection,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary)
                                ) {
                                    if (isTestingConnection) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Verifying Credentials...")
                                    } else {
                                        Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Test Zoho SMTP Connection")
                                    }
                                }
                            }
                        }

                        // Field Technician Profile
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(TelecomBlueAccent.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TelecomBlueAccent, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = "Active Field Technician", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(text = "Default signatory for site audits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = technicianName,
                                    onValueChange = { viewModel.setTechnicianName(it) },
                                    label = { Text("Technician Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = technicianPhone,
                                    onValueChange = { viewModel.setTechnicianPhone(it) },
                                    label = { Text("Contact Phone") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: MAIL GROUPS CONFIGURATION (TO & CC GROUPS)
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Card 1: TO MAIL GROUP
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(TelecomNavyPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.ForwardToInbox, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = "Primary Destination Mail Group (TO)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(text = "Destination inboxes receiving the primary audit dispatch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = recipientEmail,
                                    onValueChange = { viewModel.setRecipientEmail(it) },
                                    label = { Text("TO Group Recipients (comma-separated)") },
                                    placeholder = { Text("mdjemil@mobiserveholding.com, dispatch@mobiserveholding.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    minLines = 2
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick presets for TO
                                Text(text = "Quick Presets:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = TelecomNavyPrimary.copy(alpha = 0.1f),
                                            modifier = Modifier.clickable { viewModel.addEmailToRecipientGroup("dispatch@mobiserveholding.com") }
                                        ) {
                                            Text("+ Dispatch Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelecomNavyPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = TelecomNavyPrimary.copy(alpha = 0.1f),
                                            modifier = Modifier.clickable { viewModel.addRoleEmailsToToGroup("DISPATCHER") }
                                        ) {
                                            Text("+ All Dispatchers", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelecomNavyPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = TelecomNavyPrimary.copy(alpha = 0.1f),
                                            modifier = Modifier.clickable { viewModel.addRoleEmailsToToGroup("OM") }
                                        ) {
                                            Text("+ All O&M", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelecomNavyPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Card 2: CC GROUP MAIL (CARBON COPY)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(AccessAccordedGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(text = "Carbon Copy Group Mail (CC)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(text = "Supervision, Management & Stakeholders copied automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = ccRecipientsEmail,
                                    onValueChange = { viewModel.setCcRecipientsEmail(it) },
                                    label = { Text("CC Group Recipients (comma-separated)") },
                                    placeholder = { Text("supervision@mobiserveholding.com, om@mobiserveholding.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    minLines = 2
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Quick Add Registered Users by Role into CC Group:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AccessAccordedGreen.copy(alpha = 0.12f),
                                            modifier = Modifier.clickable { viewModel.addRoleEmailsToCcGroup("SUPERVISION") }
                                        ) {
                                            Text("+ Supervision", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccessAccordedGreen, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = TelecomBlueAccent.copy(alpha = 0.12f),
                                            modifier = Modifier.clickable { viewModel.addRoleEmailsToCcGroup("OM") }
                                        ) {
                                            Text("+ O&M", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TelecomBlueAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF6B21A8).copy(alpha = 0.12f),
                                            modifier = Modifier.clickable { viewModel.addRoleEmailsToCcGroup("ADMIN") }
                                        ) {
                                            Text("+ Admins", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B21A8), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0F766E).copy(alpha = 0.12f),
                                            modifier = Modifier.clickable { viewModel.addRoleEmailsToCcGroup("ACQUISITION") }
                                        ) {
                                            Text("+ Acquisition", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F766E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                    item {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE11D48).copy(alpha = 0.12f),
                                            modifier = Modifier.clickable { viewModel.addRoleEmailsToCcGroup("DISPATCHER") }
                                        ) {
                                            Text("+ Dispatcher", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Mail Groups and CC Distribution lists saved.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Mail Groups")
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: USER & ROLES DIRECTORY (CRUD)
                    UserManagementSection(
                        users = allUsers,
                        onCreateUser = { name, email, phone, role, dept, region ->
                            viewModel.createUser(name, email, phone, role, dept, region)
                        },
                        onUpdateUser = { user ->
                            viewModel.updateUser(user)
                        },
                        onDeleteUser = { user ->
                            viewModel.deleteUser(user)
                        },
                        onSelectTechnician = { user ->
                            viewModel.selectTechnicianUser(user)
                        },
                        onAddToToGroup = { email ->
                            viewModel.addEmailToRecipientGroup(email)
                        },
                        onAddToCcGroup = { email ->
                            viewModel.addEmailToCcGroup(email)
                        }
                    )
                }
            }
        }
    }

    // Administrator Unlock Dialog
    if (showAdminAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAdminAuthDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = TelecomNavyPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(text = "Administrator Verification", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Enter the Administrator PIN to configure mail sender, session parameters, and user permissions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = enteredAdminPin,
                        onValueChange = {
                            enteredAdminPin = it
                            adminPinError = null
                        },
                        label = { Text("Admin PIN (e.g. 1976 or 1234)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = adminPinError != null,
                        supportingText = {
                            adminPinError?.let {
                                Text(it, color = AccessRefusedRed)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.unlockAdmin(enteredAdminPin)
                        if (success) {
                            showAdminAuthDialog = false
                            Toast.makeText(context, "Administrator access granted.", Toast.LENGTH_SHORT).show()
                        } else {
                            adminPinError = "Incorrect Administrator PIN."
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminAuthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSendResultDialog) {
        SendResultDialog(
            statusMessage = smtpStatusMessage,
            detailedError = smtpDetailedError,
            onDismiss = { viewModel.setShowSendResultDialog(false) }
        )
    }
}
