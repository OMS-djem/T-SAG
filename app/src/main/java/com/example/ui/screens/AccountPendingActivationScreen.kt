package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.auth.UserSessionManager
import com.example.ui.components.getRoleBadgeColor
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary

@Composable
fun AccountPendingActivationScreen(
    sessionManager: UserSessionManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by sessionManager.currentUser.collectAsState()
    val currentProfile by sessionManager.currentProfile.collectAsState()
    val currentRole by sessionManager.currentRole.collectAsState()
    val statusMessage by sessionManager.statusMessage.collectAsState()

    var isChecking by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pending Clock / Shield Header Icon
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = Color(0xFFD97706).copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD97706))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Awaiting Activation",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "ACCOUNT PENDING ACTIVATION",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Mobiserve Holding • Telecom Field Gate",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Status Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TelecomNavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Registered Profile",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TelecomNavyPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Text(
                                text = "PENDING APPROVAL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFE5E7EB))
                    Spacer(modifier = Modifier.height(14.dp))

                    // User Details
                    Text(
                        text = "FULL NAME",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = currentUser?.displayName?.ifBlank { "Telecom Engineer" } ?: "Telecom Engineer",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CORPORATE EMAIL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = currentUser?.email ?: "N/A",
                        fontSize = 14.sp,
                        color = Color(0xFF374151)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "REQUESTED ROLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = getRoleBadgeColor(currentRole.code).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${currentRole.code} — ${currentRole.label}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = getRoleBadgeColor(currentRole.code),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notice Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF3F4F6)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Your account is recorded in the Supabase database. A Mobiserve Administrator must activate your profile and authorize your permissions before site audits and Zoho email reports can be filed.",
                                fontSize = 12.sp,
                                color = Color(0xFF4B5563),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AccessAccordedGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = statusMessage!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccessAccordedGreen,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Refresh Button
                    Button(
                        onClick = {
                            isChecking = true
                            sessionManager.refreshActivationStatus { active ->
                                isChecking = false
                                if (active) {
                                    Toast.makeText(context, "Account activated! Welcome to T-SAG.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Still awaiting administrator activation.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isChecking,
                        colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("check_activation_button")
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Checking status...", fontSize = 14.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Activation Status", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sign Out Option
                    OutlinedButton(
                        onClick = { sessionManager.signOut() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("pending_sign_out_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out / Switch Account", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
