package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.types.HistoryPeriod
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoYellow
import com.example.utils.Formatters

@Composable
fun UsageBarChart(
    period: HistoryPeriod,
    values: List<Double>,
    labels: List<String>,
    selectedIndex: Int?,
    onBarSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxValue = (values.maxOrNull() ?: 5.0).coerceAtLeast(0.5)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Selected Bar Info Banner
            val selectedVal = selectedIndex?.let { values.getOrNull(it) } ?: values.lastOrNull() ?: 0.0
            val selectedLbl = selectedIndex?.let { labels.getOrNull(it) } ?: labels.lastOrNull() ?: ""
            val estimatedInr = selectedVal * 6.50

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Usage Breakdown ($selectedLbl)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${Formatters.formatKwh(selectedVal)} • ${Formatters.formatInr(estimatedInr, includeDecimals = true)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VoltoGreenPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                values.forEachIndexed { index, value ->
                    val isSelected = (selectedIndex == index) || (selectedIndex == null && index == values.lastIndex)
                    val heightRatio = (value / maxValue).toFloat().coerceIn(0.04f, 1f)

                    val animatedHeightRatio by animateFloatAsState(
                        targetValue = heightRatio,
                        animationSpec = tween(durationMillis = 500),
                        label = "BarHeight"
                    )

                    val barLabel = labels.getOrNull(index) ?: ""

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        // Bar Column
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (period == HistoryPeriod.DAY) 0.65f else 0.45f)
                                    .fillMaxHeight(animatedHeightRatio)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (isSelected) VoltoYellow else VoltoGreenPrimary.copy(alpha = 0.7f)
                                    )
                                    .clickable { onBarSelect(index) }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // X-axis label (for 24 hours only show every 3rd or 4th label to prevent clutter)
                        val showLabel = when (period) {
                            HistoryPeriod.DAY -> index % 4 == 0 || index == values.lastIndex
                            else -> true
                        }

                        Text(
                            text = if (showLabel) barLabel else "",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
