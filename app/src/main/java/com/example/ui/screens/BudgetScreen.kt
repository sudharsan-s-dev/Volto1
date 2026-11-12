package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import com.example.ui.components.BudgetProgressBar
import com.example.ui.theme.VoltoGreenContainer
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoOnGreenContainer
import com.example.ui.theme.VoltoYellow
import com.example.utils.Formatters
import com.example.viewmodel.VoltoViewModel

@Composable
fun BudgetScreen(
    viewModel: VoltoViewModel,
    modifier: Modifier = Modifier
) {
    val stats by viewModel.householdStats.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()

    var showTariffEditor by remember { mutableStateOf(false) }

    // Tariff Editor State
    var slab1Rate by remember { mutableDoubleStateOf(4.50) }
    var slab2Rate by remember { mutableDoubleStateOf(6.00) }
    var slab3Rate by remember { mutableDoubleStateOf(7.50) }
    var slab4Rate by remember { mutableDoubleStateOf(8.50) }
    var fixedCharges by remember { mutableDoubleStateOf(120.0) }
    var providerName by remember { mutableStateOf("BESCOM (Bengaluru)") }

    // Tariff Calculator Interactive Slider
    var simulatedUnits by remember { mutableDoubleStateOf(240.0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("budget_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Budget & Tariff Manager",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Track monthly burn rate, customize DISCOM slabs, and forecast bills",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Budget Progress Component
        item {
            BudgetProgressBar(
                estimatedBillInr = stats.estimatedMonthlyBillInr,
                monthlyBudgetInr = monthlyBudget
            )
        }

        // Daily Pacing & Burn Rate Card
        item {
            val dailyBudgetPacing = monthlyBudget / 30.0
            val actualDailySpend = stats.todayKwh * 6.50
            val isSaving = actualDailySpend <= dailyBudgetPacing

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Budget Pacing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSaving) VoltoGreenContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isSaving) "ON TRACK" else "ELEVATED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSaving) VoltoOnGreenContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Daily Budget Allowance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${Formatters.formatInr(dailyBudgetPacing)} / day",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Today's Current Burn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${Formatters.formatInr(actualDailySpend, includeDecimals = true)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSaving) VoltoGreenPrimary else VoltoYellow
                            )
                        }
                    }
                }
            }
        }

        // Interactive Tariff Calculator
        item {
            val calculatedBill = Formatters.calculateIndianSlabBill(simulatedUnits)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = VoltoGreenPrimary)
                            Text(
                                text = "Tariff Calculator",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = Formatters.formatInr(calculatedBill),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = VoltoGreenPrimary
                        )
                    }

                    Text(
                        text = "Simulate your bill by sliding monthly units (kWh):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Units:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${simulatedUnits.toInt()} kWh (units)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = VoltoGreenPrimary
                        )
                    }

                    Slider(
                        value = simulatedUnits.toFloat(),
                        onValueChange = { simulatedUnits = it.toDouble() },
                        valueRange = 20f..600f,
                        steps = 58,
                        colors = SliderDefaults.colors(
                            thumbColor = VoltoGreenPrimary,
                            activeTrackColor = VoltoGreenPrimary
                        )
                    )

                    // Breakdown Pill
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "• Fixed Meter Charge: ₹${fixedCharges.toInt()}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "• Telescoping Slabs: 0-100 @ ₹$slab1Rate, 101-200 @ ₹$slab2Rate, 201-300 @ ₹$slab3Rate, 300+ @ ₹$slab4Rate",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "• Electricity Tax: 9% duty included",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        // Tariff Editor Accordion Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = VoltoYellow)
                            Text(
                                text = "Custom DISCOM Tariff Editor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { showTariffEditor = !showTariffEditor },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = if (showTariffEditor) "Hide" else "Edit Slabs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showTariffEditor) {
                        OutlinedTextField(
                            value = providerName,
                            onValueChange = { providerName = it },
                            label = { Text("Electricity Provider (DISCOM)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = slab1Rate.toString(),
                                onValueChange = { slab1Rate = it.toDoubleOrNull() ?: slab1Rate },
                                label = { Text("Slab 1 (₹)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = slab2Rate.toString(),
                                onValueChange = { slab2Rate = it.toDoubleOrNull() ?: slab2Rate },
                                label = { Text("Slab 2 (₹)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = slab3Rate.toString(),
                                onValueChange = { slab3Rate = it.toDoubleOrNull() ?: slab3Rate },
                                label = { Text("Slab 3 (₹)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = slab4Rate.toString(),
                                onValueChange = { slab4Rate = it.toDoubleOrNull() ?: slab4Rate },
                                label = { Text("Slab 4 (₹)") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        OutlinedTextField(
                            value = fixedCharges.toInt().toString(),
                            onValueChange = { fixedCharges = it.toDoubleOrNull() ?: fixedCharges },
                            label = { Text("Fixed Meter Charges (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.updateHouseholdSettings(
                                    name = providerName,
                                    budget = monthlyBudget,
                                    rate = (slab1Rate + slab2Rate + slab3Rate) / 3.0
                                )
                                showTariffEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltoGreenPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Tariff Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
