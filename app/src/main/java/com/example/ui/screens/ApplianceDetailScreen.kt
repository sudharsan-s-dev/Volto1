package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import com.example.types.Appliance
import com.example.types.DeviceCommandStatus
import com.example.types.HistoryPeriod
import com.example.ui.components.ApplianceIconHelper
import com.example.ui.components.UsageBarChart
import com.example.ui.theme.VoltoGreenContainer
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoOnGreenContainer
import com.example.ui.theme.VoltoRed
import com.example.ui.theme.VoltoYellow
import com.example.ui.theme.VoltoYellowContainer
import com.example.utils.Formatters
import androidx.compose.material3.TabRow
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.viewmodel.VoltoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceDetailScreen(
    appliance: Appliance,
    viewModel: VoltoViewModel,
    modifier: Modifier = Modifier
) {
    // Hardware back press handler
    BackHandler {
        viewModel.closeApplianceDetails()
    }

    val selectedPeriod by viewModel.detailsHistoryPeriod.collectAsState()
    val controlStates by viewModel.deviceControlStates.collectAsState()
    val controlState = controlStates[appliance.deviceId]
    val isSending = controlState?.status == DeviceCommandStatus.SENDING

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val iconVector = ApplianceIconHelper.getIconForAppliance(appliance.iconName, appliance.type)

    // Chart data based on selected tab
    val (chartValues, chartLabels) = when (selectedPeriod) {
        HistoryPeriod.DAY -> {
            val hourly = viewModel.getApplianceHourlyData(appliance.id)
            hourly.map { it.kwh } to hourly.map { it.label }
        }
        HistoryPeriod.WEEK -> {
            val daily = viewModel.getApplianceDailyData(appliance.id)
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            daily to days
        }
        HistoryPeriod.MONTH -> {
            val weekly = viewModel.getApplianceWeeklyData(appliance.id)
            val weeks = listOf("Week 1", "Week 2", "Week 3", "Week 4")
            weekly to weeks
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("appliance_details_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top App Bar with Back Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.closeApplianceDetails() },
                    modifier = Modifier.testTag("appliance_details_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = appliance.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "Device ID: ${appliance.deviceId}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Hero Power Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon & Type
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(VoltoGreenPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = appliance.name,
                                    tint = VoltoGreenPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = appliance.type,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Rated ~${appliance.baseWatts} W",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Status Badge & Critical Tag
                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (appliance.isOn && appliance.isOnline) VoltoGreenContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (appliance.isOn && appliance.isOnline) "RUNNING" else "STANDBY / OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (appliance.isOn && appliance.isOnline) VoltoOnGreenContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (appliance.isCritical) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = VoltoYellow,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Critical Device",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VoltoYellow,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Big Live Watts Meter
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (appliance.isOn && appliance.isOnline)
                                Formatters.formatRawWatts(appliance.currentWatts)
                            else "0",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (appliance.isOn && appliance.isOnline) VoltoGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = " W Live Draw",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }

                    // Remote feedback message if any
                    controlState?.message?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (controlState.status == DeviceCommandStatus.FAILED) VoltoRed else VoltoGreenPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons Row: Control & Set Limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.requestToggleAppliance(appliance) },
                            enabled = !isSending && appliance.isOnline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appliance.isOn) VoltoRed else VoltoGreenPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("control_appliance_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (appliance.isOn) "Switch OFF" else "Switch ON",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.openLimitConfigDialog(appliance) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("set_limit_appliance_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set Limit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Consumption Metrics Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Consumption & Cost Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailMetricCard(
                        title = "Today",
                        kwh = appliance.dailyKwh,
                        costInr = appliance.dailyKwh * 6.50,
                        modifier = Modifier.weight(1f)
                    )
                    DetailMetricCard(
                        title = "This Week",
                        kwh = appliance.weeklyKwh,
                        costInr = appliance.weeklyKwh * 6.50,
                        modifier = Modifier.weight(1f)
                    )
                    DetailMetricCard(
                        title = "This Month",
                        kwh = appliance.monthlyKwh,
                        costInr = appliance.monthlyKwh * 6.50,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Daily Limit & Action Policy Pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Limit: ${appliance.dailyLimitKwh} kWh",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Action: ${appliance.limitAction.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoltoGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Historical Usage Chart with Day / Week / Month Tabs
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Historical Usage Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TabRow(
                    selectedTabIndex = selectedPeriod.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = VoltoGreenPrimary
                ) {
                    HistoryPeriod.values().forEach { period ->
                        Tab(
                            selected = selectedPeriod == period,
                            onClick = {
                                viewModel.setDetailsHistoryPeriod(period)
                                selectedBarIndex = null
                            },
                            text = {
                                Text(
                                    text = period.name,
                                    fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                UsageBarChart(
                    period = selectedPeriod,
                    values = chartValues,
                    labels = chartLabels,
                    selectedIndex = selectedBarIndex,
                    onBarSelect = { idx -> selectedBarIndex = idx }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailMetricCard(
    title: String,
    kwh: Double,
    costInr: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Formatters.formatKwh(kwh),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = Formatters.formatInr(costInr, includeDecimals = true),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = VoltoGreenPrimary
            )
        }
    }
}
