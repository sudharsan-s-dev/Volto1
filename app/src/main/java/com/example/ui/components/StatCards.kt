package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoYellow
import com.example.utils.Formatters

@Composable
fun StatCard(
    title: String,
    kwhValue: Double,
    tariffRate: Double,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    val estimatedCost = kwhValue * tariffRate

    Surface(
        modifier = modifier
            .testTag("stat_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Formatters.formatKwh(kwhValue),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = Formatters.formatInr(estimatedCost, includeDecimals = true),
                style = MaterialTheme.typography.labelSmall,
                color = VoltoGreenPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SummaryStatRow(
    todayKwh: Double,
    weekKwh: Double,
    monthKwh: Double,
    tariffRate: Double = 6.50,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Today",
            kwhValue = todayKwh,
            tariffRate = tariffRate,
            icon = Icons.Default.Today,
            iconTint = VoltoGreenPrimary,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "This Week",
            kwhValue = weekKwh,
            tariffRate = tariffRate,
            icon = Icons.Default.DateRange,
            iconTint = VoltoYellow,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "This Month",
            kwhValue = monthKwh,
            tariffRate = tariffRate,
            icon = Icons.Default.CalendarMonth,
            iconTint = Color(0xFF38BDF8),
            modifier = Modifier.weight(1f)
        )
    }
}
