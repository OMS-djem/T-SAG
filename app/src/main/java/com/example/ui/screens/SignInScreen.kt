package com.example.ui.screens

import android.app.Activity
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserSessionManager
import com.example.data.model.UserRole
import com.example.ui.components.getRoleBadgeColor
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    sessionManager: UserSessionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isLoading by sessionManager.isLoading.collectAsState()
    val errorMessage by sessionManager.errorMessage.collectAsState()
    val statusMessage by sessionManager.statusMessage.collectAsState()
    val scrollState = rememberScrollState()

    var selectedAuthTab by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Create Account

    // Sign In form fields
    var loginEmail by remember { mutableStateOf("mdjemil@mobiserveholding.com") }
    var loginPassword by remember { mutableStateOf("123456") }
    var showLoginPassword by remember { mutableStateOf(false) }

    // Sign Up form fields
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("+213 ") }
    var regDepartment by remember { mutableStateOf("Telecom Field Operations") }
    var regRegion by remember { mutableStateOf("Central - Algiers") }
    var regRole by remember { mutableStateOf("TECHNICIAN") }
    var showRegPassword by remember { mutableStateOf(false) }
    var isRoleExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TelecomNavyPrimary,
                        Color(0xFF0F1E36),
                        Color(0xFF14243B)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Telecom Brand Header
            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = TelecomBlueAccent.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, TelecomBlueAccent)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CellTower,
                        contentDescription = "Telecom Site Access",
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "MOBISERVE HOLDING",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color.White
            )

            Text(
                text = "Telecom Site Access & Audit Gate (T-SAG)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Auth Mode Tab Switcher
                    TabRow(
                        selectedTabIndex = selectedAuthTab,
                        containerColor = Color(0xFFF3F4F6),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedAuthTab == 0,
                            onClick = { selectedAuthTab = 0; sessionManager.dismissError() },
                            text = { Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            icon = { Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedAuthTab == 1,
                            onClick = { selectedAuthTab = 1; sessionManager.dismissError() },
                            text = { Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            icon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "Error", tint = Color(0xFFDC2626))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFF991B1B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (statusMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AccessAccordedGreen.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = AccessAccordedGreen)
                                Text(
                                    text = statusMessage!!,
                                    color = AccessAccordedGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // TAB 0: SIGN IN
                    if (selectedAuthTab == 0) {
                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it; sessionManager.dismissError() },
                            label = { Text("Email Address") },
                            placeholder = { Text("name@mobiserveholding.com") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_email_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it; sessionManager.dismissError() },
                            label = { Text("Password") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                IconButton(onClick = { showLoginPassword = !showLoginPassword }) {
                                    Icon(
                                        imageVector = if (showLoginPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showLoginPassword) "Hide password" else "Show password",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (showLoginPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                sessionManager.signInWithEmailPassword(
                                    email = loginEmail,
                                    password = loginPassword
                                )
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verifying credentials...", fontSize = 14.sp)
                            } else {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with Password", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick demo admin fill button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    loginEmail = "MaamarDjemil@gmail.com"
                                    loginPassword = "adminPassword123"
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(14.dp), tint = TelecomNavyPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fill Administrator Credentials (MaamarDjemil@gmail.com)", fontSize = 11.sp, color = TelecomNavyPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Alternate Google Sign-In
                        OutlinedButton(
                            onClick = {
                                if (activity != null) sessionManager.signInWithGoogle(activity)
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            Surface(modifier = Modifier.size(18.dp), shape = CircleShape, color = Color.White) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("G", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF4285F4))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google", fontSize = 13.sp)
                        }
                    }

                    // TAB 1: CREATE ACCOUNT (SELF-REGISTRATION)
                    if (selectedAuthTab == 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = TelecomNavyPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "New registrations require Administrator activation before gaining access.",
                                    fontSize = 11.sp,
                                    color = TelecomNavyPrimary,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it; sessionManager.dismissError() },
                            label = { Text("Full Name *") },
                            placeholder = { Text("Mohamed Cherif") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it; sessionManager.dismissError() },
                            label = { Text("Corporate Email *") },
                            placeholder = { Text("user@mobiserveholding.com") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it; sessionManager.dismissError() },
                            label = { Text("Create Password (min 6 chars) *") },
                            trailingIcon = {
                                IconButton(onClick = { showRegPassword = !showRegPassword }) {
                                    Icon(
                                        imageVector = if (showRegPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            visualTransformation = if (showRegPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regPhone,
                            onValueChange = { regPhone = it },
                            label = { Text("Phone Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Requested Role Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isRoleExpanded,
                            onExpandedChange = { isRoleExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = UserRole.entries.firstOrNull { it.code == regRole }?.label ?: regRole,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Requested Operational Role *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRoleExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = isRoleExpanded,
                                onDismissRequest = { isRoleExpanded = false }
                            ) {
                                UserRole.entries.forEach { role ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(text = role.label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(text = role.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        onClick = {
                                            regRole = role.code
                                            isRoleExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = regDepartment,
                                onValueChange = { regDepartment = it },
                                label = { Text("Department") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = regRegion,
                                onValueChange = { regRegion = it },
                                label = { Text("Region") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                sessionManager.signUpWithSupabase(
                                    name = regName,
                                    email = regEmail,
                                    password = regPassword,
                                    phone = regPhone,
                                    role = regRole,
                                    department = regDepartment,
                                    region = regRegion
                                )
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("signup_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Registering...", fontSize = 14.sp)
                            } else {
                                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Register & Submit for Activation", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Operational Roles Overview Strip
            Text(
                text = "SUPPORTED OPERATIONAL ROLES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(UserRole.entries) { role ->
                    RoleChip(role = role)
                }
            }
        }
    }
}

@Composable
private fun RoleChip(role: UserRole) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = when (role) {
                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                    UserRole.OM -> Icons.Default.Engineering
                    UserRole.SUPERVISION -> Icons.Default.SupervisorAccount
                    UserRole.TECHNICIAN -> Icons.Default.CellTower
                    UserRole.DISPATCHER -> Icons.Default.Lock
                    UserRole.ACQUISITION -> Icons.Default.Security
                },
                contentDescription = role.label,
                tint = TelecomBlueAccent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = role.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
