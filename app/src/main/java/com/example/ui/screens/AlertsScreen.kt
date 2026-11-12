package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.types.AlertSeverity
import com.example.types.VoltoAlert
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoRed
import com.example.ui.theme.VoltoRedContainer
import com.example.ui.theme.VoltoYellow
import com.example.ui.theme.VoltoYellowContainer
import com.example.viewmodel.VoltoViewModel

enum class AlertFilterCategory(val label: String) {
    ALL("All Alerts"),
    UNREAD("Unread"),
    SAFETY("Safety & Hardware"),
    BUDGET("Budget & Limits")
}

@Composable
fun AlertsScreen(
    viewModel: VoltoViewModel,
    modifier: Modifier = Modifier
) {
    val alerts by viewModel.alerts.collectAsState()
    val unreadCount by viewModel.unreadAlertsCount.collectAsState()
    var selectedFilter by remember { mutableStateOf(AlertFilterCategory.ALL) }

    val filteredAlerts = alerts.filter { alert ->
        when (selectedFilter) {
            AlertFilterCategory.ALL -> true
            AlertFilterCategory.UNREAD -> !alert.isRead
            AlertFilterCategory.SAFETY -> alert.severity == AlertSeverity.CRITICAL ||
                alert.title.contains("Voltage", ignoreCase = true) ||
                alert.title.contains("Disconnect", ignoreCase = true) ||
                alert.title.contains("Critical", ignoreCase = true)
            AlertFilterCategory.BUDGET -> alert.title.contains("Budget", ignoreCase = true) ||
                alert.title.contains("Limit", ignoreCase = true)
        }
    }

    // Grouping by category
    val safetyAlerts = filteredAlerts.filter {
        it.severity == AlertSeverity.CRITICAL ||
        it.title.contains("Voltage", ignoreCase = true) ||
        it.title.contains("Disconnect", ignoreCase = true) ||
        it.title.contains("Critical", ignoreCase = true)
    }

    val budgetAlerts = filteredAlerts.filter {
        it.title.contains("Budget", ignoreCase = true) || it.title.contains("Limit", ignoreCase = true)
    }

    val generalAlerts = filteredAlerts.filter { it !in safetyAlerts && it !in budgetAlerts }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("alerts_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Energy Alerts",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(VoltoYellow)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$unreadCount NEW",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.Black,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Real-time safety cut-offs, budget milestones & network status",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (unreadCount > 0) {
                    TextButton(
                        onClick = { viewModel.markAllAlertsAsRead() },
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = VoltoGreenPrimary
                        )
                        Text(
                            text = " Mark Read",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = VoltoGreenPrimary
                        )
                    }
                }
            }
        }

        // Filter Chips Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(AlertFilterCategory.values()) { category ->
                    val isSelected = selectedFilter == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = category },
                        label = { Text(category.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltoGreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = VoltoGreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        if (filteredAlerts.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No Alerts In This Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "All appliances and budget milestones are in normal status.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Group 1: Safety & Hardware Alerts
            if (safetyAlerts.isNotEmpty()) {
                item {
                    Text(
                        text = "🚨 Critical Safety & Hardware",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(safetyAlerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onClick = { viewModel.markAlertAsRead(alert.id) },
                        onActionClick = {
                            viewModel.markAlertAsRead(alert.id)
                            if (alert.relatedApplianceId != null) {
                                viewModel.selectTab(com.example.viewmodel.TabItem.APPLIANCES)
                                viewModel.openApplianceDetails(alert.relatedApplianceId)
                            }
                        }
                    )
                }
            }

            // Group 2: Budget & Limit Alerts
            if (budgetAlerts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚡ Budget & Energy Quotas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(budgetAlerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onClick = { viewModel.markAlertAsRead(alert.id) },
                        onActionClick = {
                            viewModel.markAlertAsRead(alert.id)
                            if (alert.relatedApplianceId != null) {
                                viewModel.selectTab(com.example.viewmodel.TabItem.APPLIANCES)
                                viewModel.openApplianceDetails(alert.relatedApplianceId)
                            }
                        }
                    )
                }
            }

            // Group 3: General System Insights
            if (generalAlerts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "💡 General & System Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(generalAlerts, key = { it.id }) { alert ->
                    AlertItemCard(
                        alert = alert,
                        onClick = { viewModel.markAlertAsRead(alert.id) },
                        onActionClick = {
                            viewModel.markAlertAsRead(alert.id)
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AlertItemCard(
    alert: VoltoAlert,
    onClick: () -> Unit,
    onActionClick: () -> Unit
) {
    val (icon, iconTint, bgTint) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Triple(Icons.Default.ErrorOutline, VoltoRed, VoltoRedContainer)
        AlertSeverity.WARNING -> Triple(Icons.Default.WarningAmber, VoltoYellow, VoltoYellowContainer)
        AlertSeverity.INFO -> Triple(Icons.Default.Info, VoltoGreenPrimary, MaterialTheme.colorScheme.surfaceVariant)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("alert_card_${alert.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (!alert.isRead) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!alert.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(VoltoYellow)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (alert.actionText != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onActionClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (alert.severity == AlertSeverity.WARNING) VoltoRed else VoltoGreenPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = alert.actionText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
