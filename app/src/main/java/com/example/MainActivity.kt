package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.getRoleBadgeColor
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SiteAuditScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary
import com.example.ui.viewmodel.SiteAccessViewModel

enum class AppTab(val label: String) {
    AUDIT("Site Audit"),
    HISTORY("History"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: SiteAccessViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentUser by viewModel.currentUser.collectAsState()
                val currentRole by viewModel.currentRole.collectAsState()
                val currentProfile by viewModel.currentProfile.collectAsState()

                if (currentUser == null) {
                    // Auth Gate: Interactive Sign-In Screen
                    SignInScreen(sessionManager = viewModel.sessionManager)
                } else {
                    val snackbarHostState = remember { SnackbarHostState() }
                    var currentTab by remember { mutableStateOf(AppTab.AUDIT) }
                    var showSignOutConfirm by remember { mutableStateOf(false) }

                    val historyReports by viewModel.historyReports.collectAsState()
                    val pendingCount by viewModel.pendingCount.collectAsState()
                    val isCameraOpen by viewModel.isCameraOpen.collectAsState()

                    if (showSignOutConfirm) {
                        AlertDialog(
                            onDismissRequest = { showSignOutConfirm = false },
                            title = { Text("Sign Out Session", fontWeight = FontWeight.Bold) },
                            text = {
                                Text("Are you sure you want to sign out of Mobiserve T-SAG as ${currentUser?.email}?")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showSignOutConfirm = false
                                        viewModel.sessionManager.signOut()
                                    }
                                ) {
                                    Text("Sign Out")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSignOutConfirm = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (isCameraOpen) {
                        CameraScreen(
                            viewModel = viewModel,
                            onClose = { viewModel.closeCamera() }
                        )
                    } else {
                        BackHandler(enabled = currentTab != AppTab.AUDIT) {
                            currentTab = AppTab.AUDIT
                        }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = when (currentTab) {
                                                    AppTab.AUDIT -> "Site Audit"
                                                    AppTab.HISTORY -> "Audit Logs"
                                                    AppTab.SETTINGS -> "Settings"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 18.sp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = getRoleBadgeColor(currentRole.code),
                                                modifier = Modifier.testTag("current_user_role_badge")
                                            ) {
                                                Text(
                                                    text = currentRole.code,
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = { showSignOutConfirm = true },
                                            modifier = Modifier.testTag("action_sign_out")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                                contentDescription = "Sign Out",
                                                tint = Color.White
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = TelecomNavyPrimary
                                    )
                                )
                            },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.AUDIT,
                                onClick = { currentTab = AppTab.AUDIT },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.CellTower,
                                        contentDescription = "Site Audit"
                                    )
                                },
                                label = { Text("Audit") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TelecomNavyPrimary,
                                    indicatorColor = TelecomBlueAccent.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_audit_tab")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.HISTORY,
                                onClick = { currentTab = AppTab.HISTORY },
                                icon = {
                                    BadgedBox(badge = {
                                        if (pendingCount > 0) {
                                            Badge(containerColor = Color(0xFFD97706)) { Text("$pendingCount") }
                                        } else if (historyReports.isNotEmpty()) {
                                            Badge { Text("${historyReports.size}") }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = "History"
                                        )
                                    }
                                },
                                label = { Text("History") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TelecomNavyPrimary,
                                    indicatorColor = TelecomBlueAccent.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_history_tab")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.SETTINGS,
                                onClick = { currentTab = AppTab.SETTINGS },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings"
                                    )
                                },
                                label = { Text("Settings") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TelecomNavyPrimary,
                                    indicatorColor = TelecomBlueAccent.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.testTag("nav_settings_tab")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            AppTab.AUDIT -> {
                                SiteAuditScreen(
                                    viewModel = viewModel,
                                    snackbarHostState = snackbarHostState
                                )
                            }
                            AppTab.HISTORY -> {
                                HistoryScreen(
                                    viewModel = viewModel,
                                    onSelectReport = { report ->
                                        viewModel.loadFromEntity(report)
                                        currentTab = AppTab.AUDIT
                                    }
                                )
                            }
                            AppTab.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
