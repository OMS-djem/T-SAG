package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SiteReportEntity
import com.example.ui.theme.TelecomNavyPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class ChartRangeFilter(val label: String, val limit: Int?) {
    ALL("All Audits", null),
    LAST_10("Last 10", 10),
    LAST_5("Last 5", 5)
}

/**
 * Native Jetpack Compose Line Chart visualizing historical Reference Signal Received Power (RSRP)
 * across saved telecom site audits. Replaces web-based D3/Recharts with a high-performance,
 * accessible native Android Canvas component adhering to Material 3 design guidelines.
 */
@Composable
fun RsrpSignalStrengthChart(
    reports: List<SiteReportEntity>,
    modifier: Modifier = Modifier
) {
    var rangeFilter by remember { mutableStateOf(ChartRangeFilter.ALL) }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    // Filter and sort chronologically (oldest to newest for left-to-right trend)
    val sortedReports = remember(reports, rangeFilter) {
        val chronological = reports.sortedBy { it.createdAt }
        val limit = rangeFilter.limit
        if (limit != null && chronological.size > limit) {
            chronological.takeLast(limit)
        } else {
            chronological
        }
    }

    // Default select latest item if none selected or out of bounds
    val activeReport = remember(sortedReports, selectedIndex) {
        if (sortedReports.isEmpty()) null
        else if (selectedIndex in sortedReports.indices) sortedReports[selectedIndex]
        else sortedReports.last()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rsrp_signal_strength_chart"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TelecomNavyPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Signal Chart",
                            tint = TelecomNavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Signal Strength History (RSRP)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RSRP (dBm) from saved telecom site audits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (sortedReports.isEmpty()) {
                // Empty state when no audits are saved yet
                EmptyChartPlaceholder()
            } else {
                // Key Metrics Bar
                val rsrpValues = sortedReports.map { it.signalDbm }
                val latestRsrp = rsrpValues.last()
                val avgRsrp = rsrpValues.average().toInt()
                val minRsrp = rsrpValues.minOrNull() ?: -120
                val maxRsrp = rsrpValues.maxOrNull() ?: -70

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricChip(
                        label = "LATEST RSRP",
                        value = "$latestRsrp dBm",
                        color = getSignalColor(latestRsrp),
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "AVG RSRP",
                        value = "$avgRsrp dBm",
                        color = getSignalColor(avgRsrp),
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        label = "MIN / MAX",
                        value = "$minRsrp / $maxRsrp",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time Range Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ChartRangeFilter.values().forEach { filter ->
                        FilterChip(
                            selected = rangeFilter == filter,
                            onClick = {
                                rangeFilter = filter
                                selectedIndex = -1
                            },
                            label = { Text(filter.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TelecomNavyPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = TelecomNavyPrimary
                            ),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Canvas Chart
                RsrpCanvas(
                    reports = sortedReports,
                    selectedIndex = if (selectedIndex in sortedReports.indices) selectedIndex else sortedReports.lastIndex,
                    onPointSelected = { index -> selectedIndex = index },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Selected Point Inspection Card
                AnimatedVisibility(
                    visible = activeReport != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    activeReport?.let { report ->
                        SelectedPointDetailCard(report = report)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quality Legend
                SignalQualityLegend()
            }
        }
    }
}

@Composable
private fun RsrpCanvas(
    reports: List<SiteReportEntity>,
    selectedIndex: Int,
    onPointSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val textPaint = remember(density) {
        Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = with(density) { 10.sp.toPx() }
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
        }
    }

    val minScaleDbm = -130f // Bottom of chart (Poor/No Signal)
    val maxScaleDbm = -60f  // Top of chart (Excellent Signal)
    val dbmRange = maxScaleDbm - minScaleDbm

    // Threshold lines: Excellent (-85), Good (-95), Fair (-105), Poor (-115)
    val guidelineThresholds = listOf(
        -85 to Color(0xFF10B981),
        -95 to Color(0xFF059669),
        -105 to Color(0xFFF59E0B),
        -115 to Color(0xFFEF4444)
    )

    var pointOffsets by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .pointerInput(reports) {
                detectTapGestures { tapOffset ->
                    if (pointOffsets.isNotEmpty()) {
                        // Find closest point by X coordinate
                        val closestIndex = pointOffsets.indices.minByOrNull { index ->
                            abs(pointOffsets[index].x - tapOffset.x)
                        } ?: -1
                        if (closestIndex != -1) {
                            onPointSelected(closestIndex)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val leftPadding = 80f
            val rightPadding = 40f
            val topPadding = 28f
            val bottomPadding = 40f

            val chartWidth = width - leftPadding - rightPadding
            val chartHeight = height - topPadding - bottomPadding

            if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

            // 1. Draw horizontal threshold guideline dashed lines & dBm labels
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

            guidelineThresholds.forEach { (thresholdDbm, color) ->
                val normY = (thresholdDbm - minScaleDbm) / dbmRange
                val y = topPadding + chartHeight * (1f - normY)

                drawLine(
                    color = color.copy(alpha = 0.4f),
                    start = Offset(leftPadding, y),
                    end = Offset(width - rightPadding, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "$thresholdDbm dBm",
                    12f,
                    y + 4f,
                    textPaint
                )
            }

            if (reports.isEmpty()) return@Canvas

            // 2. Compute point coordinates
            val count = reports.size
            val xStep = if (count > 1) chartWidth / (count - 1) else chartWidth / 2f

            val points = reports.mapIndexed { index, report ->
                val x = if (count > 1) leftPadding + index * xStep else leftPadding + chartWidth / 2f
                val clampedDbm = report.signalDbm.toFloat().coerceIn(minScaleDbm, maxScaleDbm)
                val normY = (clampedDbm - minScaleDbm) / dbmRange
                val y = topPadding + chartHeight * (1f - normY)
                Offset(x, y)
            }
            pointOffsets = points

            // 3. Draw gradient underfill
            val fillPath = Path().apply {
                moveTo(points.first().x, topPadding + chartHeight)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, topPadding + chartHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        TelecomNavyPrimary.copy(alpha = 0.35f),
                        TelecomNavyPrimary.copy(alpha = 0.05f)
                    ),
                    startY = topPadding,
                    endY = topPadding + chartHeight
                )
            )

            // 4. Draw line connecting points
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    // Smooth cubic bezier or straight lines
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }

            drawPath(
                path = linePath,
                color = TelecomNavyPrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Draw vertical guideline for selected point
            if (selectedIndex in points.indices) {
                val selectedPt = points[selectedIndex]
                drawLine(
                    color = TelecomNavyPrimary.copy(alpha = 0.6f),
                    start = Offset(selectedPt.x, topPadding),
                    end = Offset(selectedPt.x, topPadding + chartHeight),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // 6. Draw point markers
            points.forEachIndexed { index, offset ->
                val report = reports[index]
                val pointColor = getSignalColor(report.signalDbm)
                val isSelected = index == selectedIndex

                if (isSelected) {
                    // Outer pulsing highlight ring
                    drawCircle(
                        color = pointColor.copy(alpha = 0.3f),
                        radius = 12.dp.toPx(),
                        center = offset
                    )
                }

                // Outer border (white)
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                    center = offset
                )

                // Inner core
                drawCircle(
                    color = pointColor,
                    radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                    center = offset
                )
            }

            // 7. Draw X-axis timestamps or site labels
            val dateFormat = SimpleDateFormat("dd/MM", Locale.US)
            val labelIndices = if (count <= 5) {
                points.indices.toList()
            } else {
                listOf(0, count / 2, count - 1)
            }

            labelIndices.forEach { idx ->
                if (idx in points.indices && idx in reports.indices) {
                    val pt = points[idx]
                    val label = dateFormat.format(Date(reports[idx].createdAt))
                    val labelWidth = textPaint.measureText(label)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        (pt.x - labelWidth / 2f).coerceIn(leftPadding, width - rightPadding - labelWidth),
                        height - 12f,
                        textPaint
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedPointDetailCard(report: SiteReportEntity) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val formattedTime = remember(report.createdAt) { dateFormat.format(Date(report.createdAt)) }
    val signalColor = getSignalColor(report.signalDbm)
    val isAccorded = report.status == "ACCORDED"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, signalColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(signalColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Site ${report.siteId} (${report.siteName})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAccorded) Color(0xFF16A34A).copy(alpha = 0.15f) else Color(0xFFDC2626).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isAccorded) "ACCORDED ✓" else "REFUSED ✕",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAccorded) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "RSRP SIGNAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${report.signalDbm} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = signalColor
                    )
                }

                Column {
                    Text(
                        text = "NETWORK / CARRIER",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${report.networkType} • ${report.gsmOperator}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column {
                    Text(
                        text = "CELL ID / LAC",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "CID ${report.cellId} (LAC ${report.lac})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Captured: $formattedTime • GPS: ${String.format(Locale.US, "%.4f, %.4f", report.latitude, report.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}

@Composable
private fun SignalQualityLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = Color(0xFF10B981), label = "Exc (≥-85)")
        LegendItem(color = Color(0xFF059669), label = "Good (-95)")
        LegendItem(color = Color(0xFFF59E0B), label = "Fair (-105)")
        LegendItem(color = Color(0xFFEF4444), label = "Poor (<-105)")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyChartPlaceholder() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CellTower,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No saved site audits yet",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Perform an audit and save or send to view the historical RSRP line chart.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

fun getSignalColor(dbm: Int): Color {
    return when {
        dbm >= -85 -> Color(0xFF10B981) // Excellent Green
        dbm >= -95 -> Color(0xFF059669) // Good Emerald
        dbm >= -105 -> Color(0xFFF59E0B) // Fair Amber
        else -> Color(0xFFEF4444)        // Poor Red
    }
}
