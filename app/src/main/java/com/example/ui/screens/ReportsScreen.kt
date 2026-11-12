package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.types.HistoryPeriod
import com.example.ui.components.UsageBarChart
import com.example.ui.theme.VoltoGreenContainer
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoOnGreenContainer
import com.example.ui.theme.VoltoYellow
import com.example.ui.theme.VoltoYellowContainer
import com.example.utils.Formatters
import com.example.viewmodel.VoltoViewModel

enum class ReportDateRange(val label: String, val days: Int) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    BILLING_CYCLE("Current Billing Cycle", 18)
}

data class EnergyTip(
    val title: String,
    val description: String,
    val potentialSavings: String,
    val icon: ImageVector
)

@Composable
fun ReportsScreen(
    viewModel: VoltoViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.householdStats.collectAsState()
    var selectedRange by remember { mutableStateOf(ReportDateRange.LAST_30_DAYS) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val rangeMultiplier = when (selectedRange) {
        ReportDateRange.LAST_7_DAYS -> 7.0 / 30.0
        ReportDateRange.LAST_30_DAYS -> 1.0
        ReportDateRange.BILLING_CYCLE -> 18.0 / 30.0
    }

    val displayUnits = ((stats.monthKwh * rangeMultiplier) * 10).toInt() / 10.0
    val displayCost = Formatters.calculateIndianSlabBill(displayUnits)

    val chartValues = when (selectedRange) {
        ReportDateRange.LAST_7_DAYS -> listOf(12.4, 14.1, 11.8, 15.6, 13.2, 14.8, 12.6)
        ReportDateRange.LAST_30_DAYS -> listOf(82.0, 94.0, 98.0, 90.5)
        ReportDateRange.BILLING_CYCLE -> listOf(14.0, 15.2, 12.8, 16.0, 13.9, 14.5, 12.6)
    }

    val chartLabels = when (selectedRange) {
        ReportDateRange.LAST_7_DAYS -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        ReportDateRange.LAST_30_DAYS -> listOf("Week 1", "Week 2", "Week 3", "Week 4")
        ReportDateRange.BILLING_CYCLE -> listOf("Day 12", "Day 13", "Day 14", "Day 15", "Day 16", "Day 17", "Today")
    }

    val energyTips = listOf(
        EnergyTip(
            title = "Set AC to 24°C Recommendation",
            description = "Bureau of Energy Efficiency (BEE) India recommends 24°C. Every 1°C increase saves ~6% electricity.",
            potentialSavings = "Save ~₹350/mo",
            icon = Icons.Default.AcUnit
        ),
        EnergyTip(
            title = "Switch to 5-Star BLDC Fans",
            description = "BLDC ceiling fans consume only 28-35W compared to 75-80W on standard induction fans.",
            potentialSavings = "Save ~₹180/mo per fan",
            icon = Icons.Default.Speed
        ),
        EnergyTip(
            title = "Geyser 20-Min Shower Rule",
            description = "Turn off geyser after 15-20 mins heating instead of leaving thermostat on standby.",
            potentialSavings = "Save ~₹240/mo",
            icon = Icons.Default.Savings
        ),
        EnergyTip(
            title = "Cut Phantom Standby Loads",
            description = "Set-top boxes, microwave displays, and chargers left plugged in draw continuous vampire watts.",
            potentialSavings = "Save ~₹110/mo",
            icon = Icons.Default.Lightbulb
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Tariff & Efficiency Reports",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Historical consumption analytics with custom date range filtering",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Date Range Selector
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ReportDateRange.values()) { range ->
                    val isSelected = selectedRange == range
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedRange = range
                            selectedBarIndex = null
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        label = { Text(range.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VoltoGreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = VoltoGreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Date Range Summary Metrics
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Units Consumed (${selectedRange.label})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatUnits(displayUnits),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = VoltoGreenPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Estimated Tariff Cost",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatInr(displayCost),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Dynamic Chart for Selected Range
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Historical Energy Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    UsageBarChart(
                        period = if (selectedRange == ReportDateRange.LAST_30_DAYS) HistoryPeriod.MONTH else HistoryPeriod.WEEK,
                        values = chartValues,
                        labels = chartLabels,
                        selectedIndex = selectedBarIndex,
                        onBarSelect = { selectedBarIndex = it }
                    )
                }
            }
        }

        // Slab Tariff Breakdown Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "State DISCOM Tariff Slabs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VoltoGreenContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Current Slab: 3",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = VoltoOnGreenContainer
                            )
                        }
                    }

                    Text(
                        text = "Total Monthly Consumption: ${Formatters.formatUnits(stats.monthKwh)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltoGreenPrimary
                    )

                    SlabRow(slab = "Slab 1 (0 - 100 units)", rate = "₹4.50 / unit", status = "Completed (100 units)")
                    SlabRow(slab = "Slab 2 (101 - 200 units)", rate = "₹6.00 / unit", status = "Completed (100 units)")
                    SlabRow(slab = "Slab 3 (201 - 300 units)", rate = "₹7.50 / unit", status = "Active (${(stats.monthKwh - 200).coerceAtLeast(0.0).toInt()} units)")
                    SlabRow(slab = "Slab 4 (> 300 units)", rate = "₹8.50 / unit", status = "Future threshold")
                }
            }
        }

        // Load Category Share
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Load Category Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    CategoryBar(name = "Cooling (AC)", share = 51, color = VoltoGreenPrimary)
                    CategoryBar(name = "Heating (Geyser)", share = 17, color = VoltoYellow)
                    CategoryBar(name = "Refrigeration (24x7)", share = 16, color = Color(0xFF38BDF8))
                    CategoryBar(name = "Entertainment & Fans", share = 11, color = Color(0xFFA855F7))
                    CategoryBar(name = "Lighting & Others", share = 5, color = Color(0xFFF97316))
                }
            }
        }

        // Energy Tips
        item {
            Text(
                text = "Tips to Save Every Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(energyTips) { tip ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VoltoYellowContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tip.icon,
                            contentDescription = null,
                            tint = Color(0xFF854D0E),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tip.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tip.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = tip.potentialSavings,
                            style = MaterialTheme.typography.labelSmall,
                            color = VoltoGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SlabRow(slab: String, rate: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = slab, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(text = status, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = rate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = VoltoGreenPrimary)
    }
}

@Composable
private fun CategoryBar(name: String, share: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, style = MaterialTheme.typography.bodySmall)
            Text(text = "$share%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(share / 100f)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
