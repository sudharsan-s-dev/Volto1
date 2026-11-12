package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoRed
import com.example.ui.theme.VoltoYellow
import com.example.utils.Formatters

@Composable
fun BudgetProgressBar(
    estimatedBillInr: Double,
    monthlyBudgetInr: Double,
    modifier: Modifier = Modifier
) {
    val progressRatio = (estimatedBillInr / monthlyBudgetInr.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(700),
        label = "BudgetProgress"
    )

    val percentInt = (progressRatio * 100).toInt()
    val isOverBudget = estimatedBillInr > monthlyBudgetInr
    val savingsInr = (monthlyBudgetInr - estimatedBillInr).coerceAtLeast(0.0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_progress_bar"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estimated Monthly Bill",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Based on current billing cycle usage",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = Formatters.formatInr(estimatedBillInr),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isOverBudget) VoltoRed else VoltoGreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Track & Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    VoltoGreenPrimary,
                                    if (progressRatio > 0.85f) VoltoRed else VoltoYellow
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: Budget Target & Projected Savings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$percentInt% of ${Formatters.formatInr(monthlyBudgetInr)} budget",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isOverBudget) Icons.Default.WarningAmber else Icons.Default.Savings,
                        contentDescription = null,
                        tint = if (isOverBudget) VoltoRed else VoltoGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isOverBudget)
                            "Over budget by ${Formatters.formatInr(estimatedBillInr - monthlyBudgetInr)}"
                        else
                            "Saving ${Formatters.formatInr(savingsInr)} this month",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) VoltoRed else VoltoGreenPrimary
                    )
                }
            }
        }
    }
}
