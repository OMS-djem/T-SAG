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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
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
                val snackbarHostState = remember { SnackbarHostState() }
                var currentTab by remember { mutableStateOf(AppTab.AUDIT) }
                val historyReports by viewModel.historyReports.collectAsState()
                val pendingCount by viewModel.pendingCount.collectAsState()
                val isCameraOpen by viewModel.isCameraOpen.collectAsState()

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
                                Text(
                                    text = when (currentTab) {
                                        AppTab.AUDIT -> "Telecom Site Access Audit"
                                        AppTab.HISTORY -> "Site Audit Logs & History"
                                        AppTab.SETTINGS -> "Mobiserve Settings"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
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
