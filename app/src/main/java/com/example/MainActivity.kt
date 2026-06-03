package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DashboardScreen
import com.example.ui.LogsScreen
import com.example.ui.LoginScreen
import com.example.ui.MainViewModel
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val loggedInUser by viewModel.loggedInUser.collectAsStateWithLifecycle()

                // Request crucial Location & Notification permissions at startup
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                    
                    if (fineGranted) {
                        viewModel.updateNetworkStatus()
                    }
                }

                // Trigger permission check on Mount
                LaunchedEffect(Unit) {
                    val requiredPerms = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requiredPerms.add(Manifest.permission.POST_NOTIFICATIONS)
                    }

                    val missing = requiredPerms.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    }
                }

                // Collect and show real-time notification Toast alerts
                LaunchedEffect(Unit) {
                    viewModel.uiToastMessage.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }

                if (loggedInUser == null) {
                    // Show custom branded Login first-landing page
                    LoginScreen(viewModel = viewModel)
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            BottomNavBar(
                                currentTab = currentTab,
                                onTabSelected = { viewModel.setTab(it) },
                                showSettingsTab = loggedInUser?.isAdmin == true
                            )
                        }
                    ) { innerPadding ->
                        val contentModifier = Modifier.padding(innerPadding)
                        
                        when (currentTab) {
                            "punch" -> DashboardScreen(
                                viewModel = viewModel,
                                modifier = contentModifier
                            )
                            "logs" -> LogsScreen(
                                viewModel = viewModel,
                                modifier = contentModifier
                            )
                            "settings" -> {
                                if (loggedInUser?.isAdmin == true) {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        modifier = contentModifier
                                    )
                                } else {
                                    viewModel.setTab("punch")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    showSettingsTab: Boolean
) {
    NavigationBar(
        modifier = Modifier.testTag("app_bottom_nav_bar"),
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        // Tab 1: Punch Card
        NavigationBarItem(
            selected = currentTab == "punch",
            onClick = { onTabSelected("punch") },
            label = { Text("Punch Masuk", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.example.ui.theme.Blue600,
                selectedTextColor = com.example.ui.theme.Blue600,
                unselectedIconColor = com.example.ui.theme.Slate400,
                unselectedTextColor = com.example.ui.theme.Slate500,
                indicatorColor = com.example.ui.theme.Blue50
            ),
            icon = {
                Icon(
                    imageVector = if (currentTab == "punch") Icons.Filled.Fingerprint else Icons.Outlined.Fingerprint,
                    contentDescription = "Menu Punch Card"
                )
            },
            modifier = Modifier.testTag("nav_tab_punch")
        )

        // Tab 2: Logs
        NavigationBarItem(
            selected = currentTab == "logs",
            onClick = { onTabSelected("logs") },
            label = { Text("Log Sejarah", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = com.example.ui.theme.Blue600,
                selectedTextColor = com.example.ui.theme.Blue600,
                unselectedIconColor = com.example.ui.theme.Slate400,
                unselectedTextColor = com.example.ui.theme.Slate500,
                indicatorColor = com.example.ui.theme.Blue50
            ),
            icon = {
                Icon(
                    imageVector = if (currentTab == "logs") Icons.Filled.History else Icons.Outlined.History,
                    contentDescription = "Menu Logs Sejarah"
                )
            },
            modifier = Modifier.testTag("nav_tab_logs")
        )

        // Tab 3: Settings (Admin Only)
        if (showSettingsTab) {
            NavigationBarItem(
                selected = currentTab == "settings",
                onClick = { onTabSelected("settings") },
                label = { Text("Tetapan", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = com.example.ui.theme.Blue600,
                    selectedTextColor = com.example.ui.theme.Blue600,
                    unselectedIconColor = com.example.ui.theme.Slate400,
                    unselectedTextColor = com.example.ui.theme.Slate500,
                    indicatorColor = com.example.ui.theme.Blue50
                ),
                icon = {
                    Icon(
                        imageVector = if (currentTab == "settings") Icons.Filled.Settings else Icons.Outlined.Settings,
                        contentDescription = "Menu Tetapan Pentadbir"
                    )
                },
                modifier = Modifier.testTag("nav_tab_settings")
            )
        }
    }
}
