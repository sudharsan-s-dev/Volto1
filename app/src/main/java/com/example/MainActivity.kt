package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.AuthState
import com.example.ui.components.AddApplianceDialog
import com.example.ui.components.CriticalApplianceConfirmDialog
import com.example.ui.components.SetLimitDialog
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.AppliancesScreen
import com.example.ui.screens.AskVoltoChatScreen
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.DeviceSimulatorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LimitsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoTheme
import com.example.ui.theme.VoltoYellow
import com.example.viewmodel.TabItem
import com.example.viewmodel.VoltoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Notification Channels and Background Sentinel Service
        com.example.services.NotificationHelper.createNotificationChannels(this)
        com.example.services.VoltoMonitorService.startService(this)

        setContent {
            val voltoViewModel: VoltoViewModel = viewModel()
            val isDarkMode by voltoViewModel.isDarkMode.collectAsState()

            VoltoTheme(darkTheme = isDarkMode) {
                VoltoRoot(viewModel = voltoViewModel)
            }
        }
    }
}

@Composable
fun VoltoRoot(viewModel: VoltoViewModel) {
    var isSplashActive by remember { mutableStateOf(true) }
    var isRegisterMode by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authErrorMessage.collectAsState()

    if (isSplashActive) {
        SplashScreen(onSplashFinished = { isSplashActive = false })
        return
    }

    when (val state = authState) {
        is AuthState.Unauthenticated, is AuthState.Error -> {
            if (isRegisterMode) {
                RegisterScreen(
                    isLoading = isAuthLoading,
                    errorMessage = authError,
                    onRegister = { name, email, pass -> viewModel.register(name, email, pass) },
                    onNavigateToLogin = { isRegisterMode = false }
                )
            } else {
                LoginScreen(
                    isLoading = isAuthLoading,
                    errorMessage = authError,
                    onLogin = { email, pass -> viewModel.login(email, pass) },
                    onGoogleSignIn = { viewModel.googleSignIn() },
                    onNavigateToRegister = { isRegisterMode = true },
                    onForgotPassword = { email -> viewModel.forgotPassword(email) }
                )
            }
        }
        else -> {
            VoltoApp(viewModel = viewModel)
        }
    }
}

@Composable
fun VoltoApp(viewModel: VoltoViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val unreadAlerts by viewModel.unreadAlertsCount.collectAsState()
    val criticalApplianceToConfirm by viewModel.criticalConfirmationAppliance.collectAsState()
    val showAddAppliance by viewModel.showAddApplianceDialog.collectAsState()
    val limitAppliance by viewModel.limitConfigAppliance.collectAsState()

    // Back handler for sub-tabs to return to Home
    if (activeTab != TabItem.HOME) {
        BackHandler {
            viewModel.selectTab(TabItem.HOME)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                floatingActionButton = {
                    if (activeTab != TabItem.ASK_VOLTO) {
                        FloatingActionButton(
                            onClick = { viewModel.selectTab(TabItem.ASK_VOLTO) },
                            containerColor = VoltoYellow,
                            contentColor = Color.Black,
                            shape = CircleShape,
                            modifier = Modifier.testTag("fab_ask_volto")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Ask VOLTO", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask AI", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bottom_nav_bar"),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        // 5 Core navigation tabs
                        listOf(
                            TabItem.HOME to Icons.Default.Home,
                            TabItem.APPLIANCES to Icons.Default.Devices,
                            TabItem.LIMITS to Icons.Default.Tune,
                            TabItem.REPORTS to Icons.Default.BarChart,
                            TabItem.ALERTS to Icons.Default.Notifications
                        ).forEach { (tab, icon) ->
                            val isSelected = activeTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    if (tab == TabItem.ALERTS && unreadAlerts > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = VoltoYellow,
                                                    contentColor = Color.Black
                                                ) {
                                                    Text(text = "$unreadAlerts", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                                }
                                            }
                                        ) {
                                            Icon(imageVector = icon, contentDescription = tab.title)
                                        }
                                    } else {
                                        Icon(imageVector = icon, contentDescription = tab.title)
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = VoltoGreenPrimary,
                                    selectedTextColor = VoltoGreenPrimary,
                                    indicatorColor = VoltoGreenPrimary.copy(alpha = 0.15f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (activeTab) {
                        TabItem.HOME -> HomeScreen(viewModel = viewModel)
                        TabItem.APPLIANCES -> AppliancesScreen(viewModel = viewModel)
                        TabItem.LIMITS -> LimitsScreen(viewModel = viewModel)
                        TabItem.BUDGET -> BudgetScreen(viewModel = viewModel)
                        TabItem.REPORTS -> ReportsScreen(viewModel = viewModel)
                        TabItem.ALERTS -> AlertsScreen(viewModel = viewModel)
                        TabItem.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        TabItem.ASK_VOLTO -> AskVoltoChatScreen(viewModel = viewModel)
                        TabItem.SIMULATOR -> DeviceSimulatorScreen(viewModel = viewModel)
                    }
                }
            }

            // Global Dialogs
            criticalApplianceToConfirm?.let { appliance ->
                CriticalApplianceConfirmDialog(
                    appliance = appliance,
                    onConfirm = { viewModel.confirmCriticalTurnOff() },
                    onDismiss = { viewModel.dismissCriticalConfirmation() }
                )
            }

            if (showAddAppliance) {
                AddApplianceDialog(
                    onAdd = { name, type, deviceId, isCritical, limitKwh, limitAction, baseWatts ->
                        viewModel.addNewAppliance(
                            name = name,
                            type = type,
                            deviceId = deviceId,
                            isCritical = isCritical,
                            dailyLimitKwh = limitKwh,
                            limitAction = limitAction,
                            baseWatts = baseWatts
                        )
                    },
                    onDismiss = { viewModel.closeAddApplianceDialog() }
                )
            }

            limitAppliance?.let { appliance ->
                SetLimitDialog(
                    appliance = appliance,
                    onSave = { limitKwh, action ->
                        viewModel.saveApplianceLimit(appliance.id, limitKwh, action)
                    },
                    onDismiss = { viewModel.closeLimitConfigDialog() }
                )
            }
        }
    }
}
