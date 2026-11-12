package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.types.HourlyUsage
import com.example.ui.theme.VoltoGreenLight
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoYellow
import com.example.utils.Formatters

@Composable
fun PowerLineChart(
    hourlyData: List<HourlyUsage>,
    modifier: Modifier = Modifier
) {
    if (hourlyData.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val maxWatts = (hourlyData.maxOfOrNull { it.watts } ?: 3000).coerceAtLeast(1000)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("power_line_chart"),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row with Title and Selected Stat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "24-Hour Power Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap on curve to inspect hourly draw",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val currentSelected = selectedIndex?.let { hourlyData.getOrNull(it) } ?: hourlyData.lastOrNull()
                if (currentSelected != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${Formatters.formatRawWatts(currentSelected.watts)} W",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = VoltoGreenPrimary
                        )
                        Text(
                            text = "${currentSelected.label} (${Formatters.formatKwh(currentSelected.kwh)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .pointerInput(hourlyData) {
                            detectTapGestures { offset ->
                                val stepX = size.width / (hourlyData.size - 1)
                                val idx = ((offset.x + stepX / 2) / stepX).toInt().coerceIn(0, hourlyData.size - 1)
                                selectedIndex = idx
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 24.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val chartHeight = height - bottomPadding - topPadding

                    // Horizontal Grid Lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = topPadding + (chartHeight / gridLines) * i
                        drawLine(
                            color = surfaceVariant.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Compute points
                    val points = hourlyData.mapIndexed { index, data ->
                        val x = index * (width / (hourlyData.size - 1))
                        val ratio = data.watts.toFloat() / maxWatts.toFloat()
                        val y = topPadding + (chartHeight * (1f - ratio.coerceIn(0f, 1f)))
                        Offset(x, y)
                    }

                    // Build smooth bezier path
                    val strokePath = Path()
                    val fillPath = Path()

                    if (points.isNotEmpty()) {
                        strokePath.moveTo(points.first().x, points.first().y)
                        fillPath.moveTo(points.first().x, height - bottomPadding)
                        fillPath.lineTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                            strokePath.cubicTo(
                                controlPoint1.x, controlPoint1.y,
                                controlPoint2.x, controlPoint2.y,
                                p1.x, p1.y
                            )
                            fillPath.cubicTo(
                                controlPoint1.x, controlPoint1.y,
                                controlPoint2.x, controlPoint2.y,
                                p1.x, p1.y
                            )
                        }

                        fillPath.lineTo(points.last().x, height - bottomPadding)
                        fillPath.close()

                        // Draw gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    VoltoGreenPrimary.copy(alpha = 0.35f),
                                    VoltoYellow.copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                startY = topPadding,
                                endY = height - bottomPadding
                            )
                        )

                        // Draw line stroke
                        drawPath(
                            path = strokePath,
                            color = VoltoGreenPrimary,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Highlight selected point if any
                    selectedIndex?.let { idx ->
                        if (idx in points.indices) {
                            val pt = points[idx]
                            // Vertical guide
                            drawLine(
                                color = VoltoYellow,
                                start = Offset(pt.x, topPadding),
                                end = Offset(pt.x, height - bottomPadding),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            // Outer ring
                            drawCircle(
                                color = VoltoYellow,
                                radius = 7.dp.toPx(),
                                center = pt
                            )
                            // Inner circle
                            drawCircle(
                                color = Color.White,
                                radius = 4.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }
            }

            // Time axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("12 AM", "6 AM", "12 PM", "6 PM", "11 PM").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
