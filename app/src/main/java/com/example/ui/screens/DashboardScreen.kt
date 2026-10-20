package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SiteReportEntity
import com.example.ui.theme.AccessAccordedGreen
import com.example.ui.theme.AccessRefusedRed
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary
import com.example.ui.viewmodel.SiteAccessViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class DashboardDimension(val title: String) {
    TECHNICIAN("By Technician"),
    LOCATION("By Location")
}

enum class TimeframeFilter(val label: String, val days: Int) {
    LAST_MONTH("Last Month (30d)", 30),
    LAST_WEEK("Last 7 Days", 7),
    ALL_TIME("All Time", 3650)
}

enum class ChartTypeToggle(val label: String) {
    BAR_CHART("Bar Chart"),
    TIMELINE_TREND("Monthly Trend"),
    BREAKDOWN_LIST("Rankings")
}

data class EntityVisitStat(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val totalVisits: Int,
    val accordedCount: Int,
    val refusedCount: Int,
    val operators: Set<String>,
    val latestVisitTime: Long
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: SiteAccessViewModel,
    onNavigateToAudit: () -> Unit = {}
) {
    val reports by viewModel.historyReports.collectAsState()
    var selectedDimension by remember { mutableStateOf(DashboardDimension.TECHNICIAN) }
    var selectedTimeframe by remember { mutableStateOf(TimeframeFilter.LAST_MONTH) }
    var selectedOperator by remember { mutableStateOf("ALL") }
    var chartType by remember { mutableStateOf(ChartTypeToggle.BAR_CHART) }
    var selectedStatForTooltip by remember { mutableStateOf<EntityVisitStat?>(null) }

    // Filter reports based on timeframe and operator
    val now = remember { System.currentTimeMillis() }
    val cutoffTime = remember(selectedTimeframe) {
        now - (selectedTimeframe.days.toLong() * 86_400_000L)
    }

    val filteredReports = remember(reports, selectedTimeframe, selectedOperator) {
        reports.filter { report ->
            (report.createdAt >= cutoffTime) &&
                (selectedOperator == "ALL" || report.operator.equals(selectedOperator, ignoreCase = true))
        }
    }

    // Aggregations
    val totalVisits = filteredReports.size
    val accordedVisits = filteredReports.count { it.status.equals("ACCORDED", ignoreCase = true) }
    val refusedVisits = filteredReports.count { it.status.equals("REFUSED", ignoreCase = true) }
    val uniqueSitesCount = filteredReports.map { it.siteId }.distinct().size
    val uniqueTechniciansCount = filteredReports.map { it.technicianName }.distinct().size
    val approvalRate = if (totalVisits > 0) (accordedVisits * 100) / totalVisits else 0

    // Grouping for the selected dimension
    val statsList = remember(filteredReports, selectedDimension) {
        if (selectedDimension == DashboardDimension.TECHNICIAN) {
            filteredReports
                .groupBy { it.technicianName.ifBlank { "Unassigned" } }
                .map { (techName, list) ->
                    val accorded = list.count { it.status.equals("ACCORDED", ignoreCase = true) }
                    val refused = list.count { it.status.equals("REFUSED", ignoreCase = true) }
                    val uniqueLocs = list.map { it.siteId }.distinct().size
                    EntityVisitStat(
                        key = techName,
                        displayName = techName,
                        subtitle = "$uniqueLocs distinct site locations",
                        totalVisits = list.size,
                        accordedCount = accorded,
                        refusedCount = refused,
                        operators = list.map { it.operator }.toSet(),
                        latestVisitTime = list.maxOfOrNull { it.createdAt } ?: 0L
                    )
                }
                .sortedByDescending { it.totalVisits }
        } else {
            filteredReports
                .groupBy { it.siteId.ifBlank { "Unknown Site" } }
                .map { (siteId, list) ->
                    val accorded = list.count { it.status.equals("ACCORDED", ignoreCase = true) }
                    val refused = list.count { it.status.equals("REFUSED", ignoreCase = true) }
                    val sampleName = list.firstOrNull()?.siteName?.takeIf { it.isNotBlank() } ?: siteId
                    val address = list.firstOrNull()?.address?.takeIf { it.isNotBlank() } ?: "Algeria"
                    EntityVisitStat(
                        key = siteId,
                        displayName = "$siteId ($sampleName)",
                        subtitle = address,
                        totalVisits = list.size,
                        accordedCount = accorded,
                        refusedCount = refused,
                        operators = list.map { it.operator }.toSet(),
                        latestVisitTime = list.maxOfOrNull { it.createdAt } ?: 0L
                    )
                }
                .sortedByDescending { it.totalVisits }
        }
    }

    // Daily distribution over last 30 days for timeline chart
    val dailyTrend = remember(filteredReports, selectedTimeframe) {
        val daysCount = if (selectedTimeframe == TimeframeFilter.LAST_WEEK) 7 else 30
        val dayBuckets = mutableMapOf<String, Int>()
        val cal = Calendar.getInstance()
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("dd MMM", Locale.getDefault())

        val dateList = (daysCount - 1 downTo 0).map { offset ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = sdfKey.format(c.time)
            val label = sdfDisplay.format(c.time)
            key to label
        }

        dateList.forEach { (key, _) -> dayBuckets[key] = 0 }

        filteredReports.forEach { report ->
            cal.timeInMillis = report.createdAt
            val key = sdfKey.format(cal.time)
            if (dayBuckets.containsKey(key)) {
                dayBuckets[key] = (dayBuckets[key] ?: 0) + 1
            }
        }

        dateList.map { (key, label) ->
            DailyBucket(key = key, label = label, count = dayBuckets[key] ?: 0)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("dashboard_screen_scroll"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero / Title Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = TelecomNavyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Operations Analytics",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Site Visits Visualization & Field Audit KPI Tracking",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Re-seed demo data button if needed
                        OutlinedButton(
                            onClick = { viewModel.seedSampleMonthVisits() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("seed_demo_visits_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Seed Sample Visits",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sample 30d", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Dimension Selector Tab (Technician vs Location)
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            DashboardDimension.values().forEach { dim ->
                                val isSelected = selectedDimension == dim
                                Surface(
                                    color = if (isSelected) Color(0xFF0284C7) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedDimension = dim
                                            selectedStatForTooltip = null
                                        }
                                        .testTag("dimension_tab_${dim.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (dim == DashboardDimension.TECHNICIAN) Icons.Default.People else Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dim.title,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Filter Controls Bar (Timeframe & Operator)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dashboard Filters",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }

                    // Timeframe Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeframeFilter.values().forEach { tf ->
                            val isSelected = selectedTimeframe == tf
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTimeframe = tf },
                                label = { Text(tf.label, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                ),
                                modifier = Modifier.testTag("timeframe_chip_${tf.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Operator Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Operator:",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        listOf("ALL", "Mobilis", "Djezzy", "Ooredoo").forEach { op ->
                            val isSelected = selectedOperator == op
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedOperator = op },
                                label = { Text(op, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TelecomNavyPrimary,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("operator_chip_${op.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Top Row: 4 KPI Summary Cards
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Total Visits",
                        value = "$totalVisits",
                        subValue = "${selectedTimeframe.label}",
                        icon = Icons.Default.AutoGraph,
                        accentColor = Color(0xFF2563EB),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_card_total_visits")
                    )
                    KpiStatCard(
                        title = "Approval Rate",
                        value = "$approvalRate%",
                        subValue = "$accordedVisits OK / $refusedVisits Refused",
                        icon = Icons.Default.CheckCircle,
                        accentColor = if (approvalRate >= 80) AccessAccordedGreen else Color(0xFFEAB308),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_card_approval_rate")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        title = "Sites Audited",
                        value = "$uniqueSitesCount",
                        subValue = "Unique telecom towers",
                        icon = Icons.Default.LocationOn,
                        accentColor = Color(0xFF0D9488),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_card_unique_sites")
                    )
                    KpiStatCard(
                        title = "Active Techs",
                        value = "$uniqueTechniciansCount",
                        subValue = "Logging field audits",
                        icon = Icons.Default.People,
                        accentColor = Color(0xFF7C3AED),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kpi_card_active_techs")
                    )
                }
            }
        }

        // Chart View Toggle (Bar Chart / Monthly Trend / Rankings)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with chart mode pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (selectedDimension == DashboardDimension.TECHNICIAN) {
                                    "Visits by Technician"
                                } else {
                                    "Visits by Site Location"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Recharts-style interactive breakdown (${selectedTimeframe.label})",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Chart View Selector
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            ChartTypeToggle.values().forEach { mode ->
                                val isSelected = chartType == mode
                                Surface(
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .clickable { chartType = mode }
                                        .testTag("chart_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = mode.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recharts Legend Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF2563EB), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Accorded Visits", fontSize = 11.sp, color = Color(0xFF475569))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFFEF4444), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refused Visits", fontSize = 11.sp, color = Color(0xFF475569))
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Tap bar to inspect",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tooltip display when a bar/item is tapped
                    AnimatedVisibility(visible = selectedStatForTooltip != null) {
                        selectedStatForTooltip?.let { stat ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .testTag("recharts_tooltip_card"),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stat.displayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${stat.totalVisits} Total Audits  •  ${stat.accordedCount} Accorded  •  ${stat.refusedCount} Refused",
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp
                                        )
                                        val sharePct = if (totalVisits > 0) (stat.totalVisits * 100) / totalVisits else 0
                                        Text(
                                            text = "${stat.subtitle} (${sharePct}% of all visits)",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedStatForTooltip = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close tooltip",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Render Selected Chart Type
                    when (chartType) {
                        ChartTypeToggle.BAR_CHART -> {
                            if (statsList.isEmpty()) {
                                EmptyDataPlaceholder(onSeed = { viewModel.seedSampleMonthVisits() })
                            } else {
                                RechartsStyleBarChart(
                                    stats = statsList.take(8),
                                    selectedStat = selectedStatForTooltip,
                                    onSelectStat = { stat -> selectedStatForTooltip = stat },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .testTag("recharts_bar_chart_canvas")
                                )
                            }
                        }
                        ChartTypeToggle.TIMELINE_TREND -> {
                            Column {
                                Text(
                                    text = "30-Day Daily Site Visit Trend Line",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                RechartsStyleTimelineAreaChart(
                                    dailyData = dailyTrend,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp)
                                        .testTag("recharts_area_trend_canvas")
                                )
                            }
                        }
                        ChartTypeToggle.BREAKDOWN_LIST -> {
                            // Ranked horizontal progress bars
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val maxVisits = statsList.maxOfOrNull { it.totalVisits } ?: 1
                                statsList.take(6).forEachIndexed { idx, stat ->
                                    RankedProgressItem(
                                        rank = idx + 1,
                                        stat = stat,
                                        maxVisits = maxVisits,
                                        isSelected = selectedStatForTooltip?.key == stat.key,
                                        onClick = { selectedStatForTooltip = stat }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Secondary Section: Status Distribution Donut & Operators Breakdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Access Accordance & Operator Distribution",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Clearance approval status and cellular network split",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Chart
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .testTag("donut_distribution_chart"),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutDistributionChart(
                                accorded = accordedVisits,
                                refused = refusedVisits,
                                modifier = Modifier.size(130.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$approvalRate%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Accorded",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Operator Share Progress Bars
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val mobilisCount = filteredReports.count { it.operator.equals("Mobilis", ignoreCase = true) }
                            val djezzyCount = filteredReports.count { it.operator.equals("Djezzy", ignoreCase = true) }
                            val ooredooCount = filteredReports.count { it.operator.equals("Ooredoo", ignoreCase = true) }

                            OperatorShareRow(name = "Mobilis", count = mobilisCount, total = totalVisits, color = Color(0xFF059669))
                            OperatorShareRow(name = "Djezzy", count = djezzyCount, total = totalVisits, color = Color(0xFFDC2626))
                            OperatorShareRow(name = "Ooredoo", count = ooredooCount, total = totalVisits, color = Color(0xFFEA580C))
                        }
                    }
                }
            }
        }

        // Detailed Table / Ranked Cards of all Items
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDimension == DashboardDimension.TECHNICIAN) {
                                "Technician Performance Roster"
                            } else {
                                "Top Audited Site Locations"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${statsList.size} entities",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (statsList.isEmpty()) {
                        Text(
                            text = "No site visit records found for this filter.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        statsList.forEachIndexed { index, stat ->
                            EntityRosterRow(
                                rank = index + 1,
                                stat = stat,
                                isTechnician = selectedDimension == DashboardDimension.TECHNICIAN
                            )
                            if (index < statsList.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Color(0xFFF1F5F9))
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Spacer for Navigation Bar clearance
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ----------------------------------------------------------------------------------------
// Recharts-style Interactive Bar Chart (Canvas)
// ----------------------------------------------------------------------------------------

@Composable
fun RechartsStyleBarChart(
    stats: List<EntityVisitStat>,
    selectedStat: EntityVisitStat?,
    onSelectStat: (EntityVisitStat) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = max(stats.maxOfOrNull { it.totalVisits } ?: 1, 5)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(stats) {
                    detectTapGestures { tapOffset ->
                        val chartHeight = size.height - 40f
                        val chartWidth = size.width - 40f
                        val barSpacing = chartWidth / stats.size
                        val tappedIndex = ((tapOffset.x - 30f) / barSpacing).toInt()
                        if (tappedIndex in stats.indices) {
                            onSelectStat(stats[tappedIndex])
                        }
                    }
                }
        ) {
            val chartLeft = 32f
            val chartRight = size.width - 12f
            val chartTop = 18f
            val chartBottom = size.height - 36f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            // 1. Draw horizontal grid lines and Y-axis scale (Recharts style)
            val gridSteps = 4
            val gridColor = Color(0xFFE2E8F0)
            val textColor = android.graphics.Color.parseColor("#94A3B8")
            val textPaint = android.graphics.Paint().apply {
                color = textColor
                textSize = 24f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.RIGHT
            }

            for (i in 0..gridSteps) {
                val ratio = i.toFloat() / gridSteps.toFloat()
                val y = chartBottom - (chartHeight * ratio)
                val stepVal = ((maxVal * ratio)).toInt()

                // Grid line
                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )

                // Y-axis label
                drawContext.canvas.nativeCanvas.drawText(
                    stepVal.toString(),
                    chartLeft - 6f,
                    y + 8f,
                    textPaint
                )
            }

            // 2. Draw bars
            val barCount = stats.size
            val slotWidth = chartWidth / barCount
            val barWidth = (slotWidth * 0.55f).coerceIn(16f, 40f)

            val labelPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#475569")
                textSize = 22f
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            val valuePaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#1E293B")
                textSize = 22f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }

            stats.forEachIndexed { index, stat ->
                val xCenter = chartLeft + (index * slotWidth) + (slotWidth / 2f)
                val xLeft = xCenter - (barWidth / 2f)

                val isSelected = selectedStat?.key == stat.key

                // Heights for Accorded vs Refused
                val totalH = (stat.totalVisits.toFloat() / maxVal.toFloat()) * chartHeight
                val accordedH = (stat.accordedCount.toFloat() / maxVal.toFloat()) * chartHeight
                val refusedH = (stat.refusedCount.toFloat() / maxVal.toFloat()) * chartHeight

                val yTotalTop = chartBottom - totalH

                // If selected, draw highlight halo behind
                if (isSelected) {
                    drawRoundRect(
                        color = Color(0xFF0284C7).copy(alpha = 0.15f),
                        topLeft = Offset(xLeft - 6f, chartTop),
                        size = Size(barWidth + 12f, chartHeight + 6f),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                // Draw Accorded portion (base bar)
                val accordedColor = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF2563EB)
                drawRoundRect(
                    color = accordedColor,
                    topLeft = Offset(xLeft, chartBottom - accordedH),
                    size = Size(barWidth, accordedH),
                    cornerRadius = if (stat.refusedCount == 0) CornerRadius(6f, 6f) else CornerRadius(0f, 0f)
                )

                // Draw Refused stacked on top of Accorded if > 0
                if (stat.refusedCount > 0) {
                    val refusedColor = if (isSelected) Color(0xFFDC2626) else Color(0xFFEF4444)
                    drawRoundRect(
                        color = refusedColor,
                        topLeft = Offset(xLeft, yTotalTop),
                        size = Size(barWidth, refusedH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }

                // Value label on top of bar
                drawContext.canvas.nativeCanvas.drawText(
                    stat.totalVisits.toString(),
                    xCenter,
                    yTotalTop - 6f,
                    valuePaint
                )

                // Shortened X-axis entity label
                val shortLabel = shortenLabel(stat.displayName)
                drawContext.canvas.nativeCanvas.drawText(
                    shortLabel,
                    xCenter,
                    chartBottom + 26f,
                    labelPaint
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------------
// Recharts-style Area & Trend Chart (Timeline over 30 days)
// ----------------------------------------------------------------------------------------

data class DailyBucket(val key: String, val label: String, val count: Int)

@Composable
fun RechartsStyleTimelineAreaChart(
    dailyData: List<DailyBucket>,
    modifier: Modifier = Modifier
) {
    if (dailyData.isEmpty()) return
    val maxCount = max(dailyData.maxOfOrNull { it.count } ?: 1, 4)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val chartLeft = 28f
            val chartRight = size.width - 12f
            val chartTop = 16f
            val chartBottom = size.height - 28f
            val chartHeight = chartBottom - chartTop
            val chartWidth = chartRight - chartLeft

            // Grid lines
            val gridColor = Color(0xFFE2E8F0)
            val steps = 3
            for (i in 0..steps) {
                val y = chartBottom - (chartHeight * (i.toFloat() / steps))
                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, y),
                    end = Offset(chartRight, y),
                    strokeWidth = 1f
                )
            }

            // Build path for smooth trend line
            val stepX = chartWidth / (dailyData.size - 1).coerceAtLeast(1)
            val points = dailyData.mapIndexed { i, bucket ->
                val x = chartLeft + (i * stepX)
                val y = chartBottom - ((bucket.count.toFloat() / maxCount) * chartHeight)
                Offset(x, y)
            }

            val strokePath = Path()
            val fillPath = Path()

            if (points.isNotEmpty()) {
                strokePath.moveTo(points[0].x, points[0].y)
                fillPath.moveTo(points[0].x, chartBottom)
                fillPath.lineTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val controlX = (p0.x + p1.x) / 2f
                    strokePath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(points.last().x, chartBottom)
                fillPath.close()

                // Draw gradient area under curve
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF0284C7).copy(alpha = 0.0f)),
                        startY = chartTop,
                        endY = chartBottom
                    )
                )

                // Draw trend stroke line
                drawPath(
                    path = strokePath,
                    color = Color(0xFF0284C7),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // Draw data points on days that have visits
                points.forEachIndexed { idx, point ->
                    if (dailyData[idx].count > 0) {
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = point
                        )
                        drawCircle(
                            color = Color(0xFF0284C7),
                            radius = 4f,
                            center = point
                        )
                    }
                }
            }

            // X-axis date labels (first, middle, last)
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#94A3B8")
                textSize = 20f
                isAntiAlias = true
            }

            if (dailyData.isNotEmpty()) {
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                drawContext.canvas.nativeCanvas.drawText(
                    dailyData.first().label,
                    chartLeft,
                    chartBottom + 20f,
                    textPaint
                )

                val midIdx = dailyData.size / 2
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                drawContext.canvas.nativeCanvas.drawText(
                    dailyData[midIdx].label,
                    chartLeft + (chartWidth / 2f),
                    chartBottom + 20f,
                    textPaint
                )

                textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                drawContext.canvas.nativeCanvas.drawText(
                    dailyData.last().label,
                    chartRight,
                    chartBottom + 20f,
                    textPaint
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------------------
// Donut Distribution Chart (Accorded vs Refused)
// ----------------------------------------------------------------------------------------

@Composable
fun DonutDistributionChart(
    accorded: Int,
    refused: Int,
    modifier: Modifier = Modifier
) {
    val total = (accorded + refused).coerceAtLeast(1)
    val accordedSweep = (accorded.toFloat() / total) * 360f
    val refusedSweep = 360f - accordedSweep

    Canvas(modifier = modifier) {
        val strokeWidth = 24f
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

        // Background circle track
        drawArc(
            color = Color(0xFFF1F5F9),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokeWidth)
        )

        // Accorded arc (Emerald)
        if (accorded > 0) {
            drawArc(
                color = AccessAccordedGreen,
                startAngle = -90f,
                sweepAngle = accordedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Refused arc (Red)
        if (refused > 0) {
            drawArc(
                color = AccessRefusedRed,
                startAngle = -90f + accordedSweep,
                sweepAngle = refusedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

// ----------------------------------------------------------------------------------------
// Component Rows and Helper Cards
// ----------------------------------------------------------------------------------------

@Composable
fun KpiStatCard(
    title: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )

            Text(
                text = subValue,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OperatorShareRow(
    name: String,
    count: Int,
    total: Int,
    color: Color
) {
    val pct = if (total > 0) (count * 100) / total else 0
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )
            Text(
                text = "$count visits ($pct%)",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = if (total > 0) count.toFloat() / total else 0f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun RankedProgressItem(
    rank: Int,
    stat: EntityVisitStat,
    maxVisits: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val progress = (stat.totalVisits.toFloat() / maxVisits).coerceIn(0f, 1f)
    val rankBadgeColor = when (rank) {
        1 -> Color(0xFFF59E0B) // Gold
        2 -> Color(0xFF94A3B8) // Silver
        3 -> Color(0xFFB45309) // Bronze
        else -> Color(0xFFE2E8F0)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("ranked_stat_item_$rank"),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFFF0F9FF) else Color(0xFFF8FAFC),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)) else null
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Surface(
                color = rankBadgeColor,
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$rank",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 3) Color.White else Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stat.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stat.totalVisits} visits",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Segmented Progress Bar (Accorded vs Refused)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(8.dp)
                    ) {
                        val accordedWeight = if (stat.totalVisits > 0) stat.accordedCount.toFloat() / stat.totalVisits else 1f
                        val refusedWeight = 1f - accordedWeight

                        if (accordedWeight > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(accordedWeight)
                                    .height(8.dp)
                                    .background(AccessAccordedGreen)
                            )
                        }
                        if (refusedWeight > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(refusedWeight)
                                    .height(8.dp)
                                    .background(AccessRefusedRed)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntityRosterRow(
    rank: Int,
    stat: EntityVisitStat,
    isTechnician: Boolean
) {
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val lastVisitStr = if (stat.latestVisitTime > 0) sdf.format(Date(stat.latestVisitTime)) else "Recent"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar / Icon
        Surface(
            shape = CircleShape,
            color = if (isTechnician) Color(0xFFEFF6FF) else Color(0xFFF0FDF4),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isTechnician) Icons.Default.People else Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isTechnician) Color(0xFF2563EB) else AccessAccordedGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stat.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "${stat.subtitle} • Last: $lastVisitStr",
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Surface(
                color = Color(0xFF0284C7).copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${stat.totalVisits} visits",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "${stat.accordedCount} OK  ${stat.refusedCount} Ref",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun EmptyDataPlaceholder(onSeed: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.BarChart,
            contentDescription = null,
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No site visits in this period",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
        )
        Text(
            text = "Load realistic 30-day telecom operations data to explore charts.",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onSeed,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
        ) {
            Text("Load 30-Day Visits Dataset", fontSize = 12.sp)
        }
    }
}

private fun shortenLabel(name: String): String {
    // Shorten "Karim Benali" -> "Karim B." or "DZ-ALG-0492" -> "DZ-0492"
    val parts = name.split(" ")
    return when {
        name.startsWith("DZ-") -> {
            val tokens = name.split("-")
            if (tokens.size >= 3) "${tokens[0]}-${tokens[2].take(4)}" else name.take(7)
        }
        parts.size >= 2 -> "${parts[0]} ${parts[1].firstOrNull()?.uppercaseChar() ?: ""}."
        else -> name.take(7)
    }
}
