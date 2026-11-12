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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.viewmodel.TabItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BudgetProgressBar
import com.example.ui.components.CircularWattGauge
import com.example.ui.components.PowerLineChart
import com.example.ui.components.SummaryStatRow
import com.example.ui.components.TopConsumersCard
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoYellow
import com.example.viewmodel.VoltoViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VoltoViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.householdStats.collectAsState()
    val appliances by viewModel.appliances.collectAsState()
    val hourlyHistory by viewModel.hourlyHistory.collectAsState()
    val householdName by viewModel.householdName.collectAsState()

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour in 4..11 -> "Subhodaya, $householdName"
        currentHour in 12..16 -> "Good Afternoon, $householdName"
        else -> "Namaste, $householdName"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Greeting & Refresh Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(VoltoYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "VOLTO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = VoltoGreenPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.refreshDashboard() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("refresh_dashboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Meter Data",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.selectTab(TabItem.SETTINGS) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Slogan Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = VoltoGreenPrimary.copy(alpha = 0.10f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡",
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Monitor Every Unit, Control Every Appliance, Save Every Month.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltoGreenPrimary
                    )
                }
            }
        }

        // Large Circular Watt Gauge
        item {
            CircularWattGauge(
                totalWatts = stats.totalWatts,
                maxWatts = 5000
            )
        }

        // Today / Week / Month Stat Cards
        item {
            SummaryStatRow(
                todayKwh = stats.todayKwh,
                weekKwh = stats.weekKwh,
                monthKwh = stats.monthKwh,
                tariffRate = stats.tariffRatePerKwh
            )
        }

        // Estimated Monthly Bill & Budget Progress Bar
        item {
            BudgetProgressBar(
                estimatedBillInr = stats.estimatedMonthlyBillInr,
                monthlyBudgetInr = stats.monthlyBudgetInr,
                modifier = Modifier.clickable { viewModel.selectTab(TabItem.BUDGET) }
            )
        }

        // Top 3 Power Consumers Right Now
        item {
            TopConsumersCard(
                appliances = appliances,
                totalHouseholdWatts = stats.totalWatts,
                onApplianceClick = { id ->
                    viewModel.selectTab(com.example.viewmodel.TabItem.APPLIANCES)
                    viewModel.openApplianceDetails(id)
                }
            )
        }

        // 24-Hour Power Usage Line Chart
        item {
            PowerLineChart(
                hourlyData = hourlyHistory
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
