package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.example.persistence.datastore.DataSourceMode
import com.example.ui.theme.VoltoGreenContainer
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoOnGreenContainer
import com.example.ui.theme.VoltoRed
import com.example.ui.theme.VoltoYellow
import com.example.viewmodel.TabItem
import com.example.viewmodel.VoltoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VoltoViewModel,
    modifier: Modifier = Modifier
) {
    val householdName by viewModel.householdName.collectAsState()
    val homeAddress by viewModel.homeAddress.collectAsState()
    val discomProvider by viewModel.discomProvider.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentLang by viewModel.currentLanguage.collectAsState()
    val currentSource by viewModel.currentSourceMode.collectAsState()

    var editingName by remember(householdName) { mutableStateOf(householdName) }
    var editingAddress by remember(homeAddress) { mutableStateOf(homeAddress) }
    var editingProvider by remember(discomProvider) { mutableStateOf(discomProvider) }
    var editingBudget by remember(monthlyBudget) { mutableDoubleStateOf(monthlyBudget) }

    var safetyNotifs by remember { mutableStateOf(true) }
    var budgetNotifs by remember { mutableStateOf(true) }
    var limitNotifs by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Android 13+ POST_NOTIFICATIONS launcher
    var notificationPermissionGranted by remember { mutableStateOf(true) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted = isGranted
    }

    val indianProviders = listOf(
        "BESCOM (Bengaluru, Karnataka)",
        "TANGEDCO (Tamil Nadu)",
        "MSEDCL (Maharashtra)",
        "Tata Power (Delhi / Mumbai)",
        "UPPCL (Uttar Pradesh)",
        "WBSEDCL (West Bengal)",
        "APEPDCL (Andhra Pradesh)"
    )
    var providerDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Preferences & System Setup",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure your home profile, DISCOM provider, and language",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Language Selector (English, Tamil, Hindi)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = VoltoGreenPrimary)
                        Text(text = "App Language / மொழி / भाषा", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentLang == "en",
                            onClick = { viewModel.setLanguage("en") },
                            label = { Text("English") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VoltoGreenPrimary.copy(alpha = 0.2f), selectedLabelColor = VoltoGreenPrimary)
                        )
                        FilterChip(
                            selected = currentLang == "ta",
                            onClick = { viewModel.setLanguage("ta") },
                            label = { Text("தமிழ் (Tamil)") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VoltoGreenPrimary.copy(alpha = 0.2f), selectedLabelColor = VoltoGreenPrimary)
                        )
                        FilterChip(
                            selected = currentLang == "hi",
                            onClick = { viewModel.setLanguage("hi") },
                            label = { Text("हिन्दी (Hindi)") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VoltoGreenPrimary.copy(alpha = 0.2f), selectedLabelColor = VoltoGreenPrimary)
                        )
                    }
                }
            }
        }

        // Appearance Mode (Dark / Light)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null, tint = VoltoYellow, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(text = if (isDarkMode) "Dark Energy Theme" else "Light Theme", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(text = "High contrast OLED battery saving", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VoltoGreenPrimary)
                    )
                }
            }
        }

        // Home Profile & DISCOM Provider
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(text = "Household Profile & Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = editingName,
                        onValueChange = {
                            editingName = it
                            viewModel.updateProfile(it, editingAddress, editingProvider)
                        },
                        label = { Text("Household Name") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = VoltoGreenPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = editingAddress,
                        onValueChange = {
                            editingAddress = it
                            viewModel.updateProfile(editingName, it, editingProvider)
                        },
                        label = { Text("Home Address & City") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = VoltoYellow) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // DISCOM Dropdown
                    ExposedDropdownMenuBox(
                        expanded = providerDropdownExpanded,
                        onExpandedChange = { providerDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = editingProvider,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("State DISCOM Provider") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = providerDropdownExpanded,
                            onDismissRequest = { providerDropdownExpanded = false }
                        ) {
                            indianProviders.forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov) },
                                    onClick = {
                                        editingProvider = prov
                                        viewModel.updateProfile(editingName, editingAddress, prov)
                                        providerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Shortcut to Tariff Editor
                    Button(
                        onClick = { viewModel.selectTab(TabItem.BUDGET) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = VoltoGreenPrimary)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Open Tariff & Slab Editor", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Data Source Switch (Mock vs Live)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = VoltoGreenPrimary)
                            Text(text = "Telemetry Data Source", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VoltoGreenContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = currentSource.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = VoltoOnGreenContainer)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = currentSource == DataSourceMode.MOCK,
                            onClick = { viewModel.switchDataSource(DataSourceMode.MOCK) },
                            label = { Text("Simulated Smart Meter (2s)") }
                        )
                        FilterChip(
                            selected = currentSource == DataSourceMode.LIVE,
                            onClick = { viewModel.switchDataSource(DataSourceMode.LIVE) },
                            label = { Text("Live MQTT TLS Broker") }
                        )
                    }

                    // Developer Simulator Link
                    OutlinedButton(
                        onClick = { viewModel.selectTab(TabItem.SIMULATOR) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("open_simulator_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeveloperMode, contentDescription = null, tint = VoltoYellow)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Open ESP32 Hardware Simulator", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Android 13+ Notification Channels & Permission
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Notification Channels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltoGreenPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Request System Notification Permission")
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Safety & Voltage Surge Alerts", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = safetyNotifs, onCheckedChange = { safetyNotifs = it })
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Budget Milestones (50%, 75%, 90%, 100%)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = budgetNotifs, onCheckedChange = { budgetNotifs = it })
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Daily Appliance Quota Warnings", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = limitNotifs, onCheckedChange = { limitNotifs = it })
                    }
                }
            }
        }

        // Account Privacy & Deletion (Google Play Store compliance)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Account & Data Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Permanently remove your registered email, household device configurations, telemetry readings, and alert records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VoltoRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("delete_account_button")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = VoltoRed)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Delete My Account and Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sign Out Button
        item {
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = VoltoRed.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = VoltoRed)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Sign Out", color = VoltoRed, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Permanently Delete Account & Data?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("This action is irreversible. In compliance with Google Play Store data safety policies, all your energy telemetry, hourly logs, appliance configurations, and credentials will be permanently erased.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccountAndData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltoRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    modifier = Modifier.testTag("cancel_delete_account_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_account_dialog")
        )
    }
}
