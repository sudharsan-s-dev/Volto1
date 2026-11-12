package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.types.DeviceControlState
import com.example.ui.theme.VoltoGreenContainer
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoOnGreenContainer
import com.example.ui.theme.VoltoRed
import com.example.ui.theme.VoltoRedContainer
import com.example.ui.theme.VoltoYellow
import com.example.ui.theme.VoltoYellowContainer
import com.example.utils.Formatters

@Composable
fun ApplianceCard(
    appliance: Appliance,
    controlState: DeviceControlState?,
    onCardClick: () -> Unit,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSending = controlState?.status == DeviceCommandStatus.SENDING
    val isConfirmed = controlState?.status == DeviceCommandStatus.CONFIRMED
    val isFailed = controlState?.status == DeviceCommandStatus.FAILED

    val statusBg = when {
        !appliance.isOnline -> MaterialTheme.colorScheme.surfaceVariant
        appliance.isOn -> VoltoGreenContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val statusColor = when {
        !appliance.isOnline -> MaterialTheme.colorScheme.onSurfaceVariant
        appliance.isOn -> VoltoOnGreenContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when {
        !appliance.isOnline -> "OFFLINE"
        appliance.isOn -> "RUNNING"
        else -> "STANDBY"
    }

    val iconVector = ApplianceIconHelper.getIconForAppliance(appliance.iconName, appliance.type)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCardClick)
            .testTag("appliance_card_${appliance.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Appliance Icon with Status Glow
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (appliance.isOn && appliance.isOnline)
                                VoltoGreenPrimary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = appliance.name,
                        tint = if (appliance.isOn && appliance.isOnline) VoltoGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Status
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = appliance.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        if (appliance.isCritical) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VoltoYellowContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Critical",
                                        tint = Color(0xFF854D0E),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "CRITICAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF854D0E)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        // Today's estimated cost
                        val todayCost = appliance.dailyKwh * 6.50
                        Text(
                            text = "• Today: ${Formatters.formatInr(todayCost, includeDecimals = true)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Control Switch / Remote Progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(width = 56.dp, height = 48.dp)
                ) {
                    when {
                        isSending -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = VoltoGreenPrimary
                            )
                        }
                        isConfirmed -> {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(VoltoGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirmed",
                                    tint = VoltoGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        isFailed -> {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(VoltoRedContainer)
                                    .clickable { onToggleClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Failed",
                                    tint = VoltoRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        !appliance.isOnline -> {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        else -> {
                            Switch(
                                checked = appliance.isOn,
                                onCheckedChange = { onToggleClick() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VoltoGreenPrimary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Power Metrics & State Banner
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Watts
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = if (appliance.isOn && appliance.isOnline)
                            Formatters.formatRawWatts(appliance.currentWatts)
                        else "0",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (appliance.isOn && appliance.isOnline) VoltoGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " W Live",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 1.dp, start = 2.dp)
                    )
                }

                // Daily kWh & Limit Progress
                val limitRatio = (appliance.dailyKwh / appliance.dailyLimitKwh.coerceAtLeast(0.1)).toFloat()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${Formatters.formatKwh(appliance.dailyKwh)} / ${appliance.dailyLimitKwh.toInt()} kWh",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (limitRatio > 0.9f) VoltoRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (limitRatio > 0.9f) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // Command feedback banner if any
            AnimatedVisibility(
                visible = controlState?.message != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                controlState?.message?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFailed) VoltoRed else VoltoGreenPrimary,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
                }
            }
        }
    }
}
