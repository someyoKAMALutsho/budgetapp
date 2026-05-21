package com.gorib.app.ui.screens.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gorib.app.ui.navigation.Screen
import com.gorib.app.domain.model.analytics.DailySpending
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    navController: NavController,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val displayMonth by viewModel.displayMonth.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val comparison by viewModel.comparison.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val comparisonExpanded by viewModel.comparisonExpanded.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)  // space for bottom nav
    ) {

        // ── SECTION 1: Month Selector ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.changeMonth(-1) }) {
                Icon(Icons.Default.KeyboardArrowLeft, "Previous month")
            }
            Text(
                displayMonth, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { viewModel.changeMonth(1) },
                enabled = canGoForward
            ) {
                Icon(
                    Icons.Default.KeyboardArrowRight, "Next month",
                    tint = if (canGoForward) LocalContentColor.current
                    else LocalContentColor.current.copy(alpha = 0.3f)
                )
            }
        }

        if (isLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // ── SECTION 2: Summary Card ────────────────────────────────────────
            summary?.let { s ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Total Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        "RM ${"%.2f".format(s.totalSpent)}",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (s.totalBudget > 0) {
                        Spacer(Modifier.height(8.dp))
                        val progress = (s.totalSpent / s.totalBudget).coerceIn(0.0, 1.0)
                        val overBudget = s.totalSpent > s.totalBudget
                        LinearProgressIndicator(
                            progress = progress.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (overBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "RM ${"%.2f".format(s.totalSpent)} of RM ${"%.2f".format(s.totalBudget)} budget",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overBudget) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── SECTION 3: Month Comparison ───────────────────────────────────
            comparison?.let { c ->
                val isIncrease = c.changePercent >= 0
                val chipColor = if (isIncrease)
                    MaterialTheme.colorScheme.errorContainer
                else Color(0xFFDCEDC8)  // light green

                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { viewModel.toggleComparisonExpanded() },
                    shape = RoundedCornerShape(50),
                    colors = CardDefaults.cardColors(containerColor = chipColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (isIncrease) "▲" else "▼",
                            color = if (isIncrease) MaterialTheme.colorScheme.error
                            else Color(0xFF2E7D32)
                        )
                        Text(
                            "${"%.1f".format(abs(c.changePercent))}% vs last month",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isIncrease) MaterialTheme.colorScheme.error
                            else Color(0xFF2E7D32)
                        )
                        Icon(
                            if (comparisonExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Expandable category breakdown
                AnimatedVisibility(visible = comparisonExpanded) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header row
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "Category", Modifier.weight(2f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                )
                                Text(
                                    "This Month", Modifier.weight(1.5f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    "Change", Modifier.weight(1f),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.5f),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            c.categoryChanges.forEach { cc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${cc.iconEmoji} ${cc.categoryName}",
                                        Modifier.weight(2f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "RM ${"%.2f".format(cc.currentSpent)}",
                                        Modifier.weight(1.5f),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.End
                                    )
                                    val pctColor = if (cc.changePercent >= 0)
                                        MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                                    Text(
                                        "${if (cc.changePercent >= 0) "+" else ""}${"%.1f".format(cc.changePercent)}%",
                                        Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = pctColor,
                                        textAlign = TextAlign.End,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SECTION 4: By Category ────────────────────────────────────────
            if (s.byCategory.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "By Category",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                s.byCategory.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate(
                                    Screen.CategoryDetail.createRoute(cat.categoryId)
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cat.iconEmoji, fontSize = 24.sp,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    cat.categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "RM ${"%.2f".format(cat.totalSpent)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            val barProgress = (cat.percentage / 100.0).coerceIn(0.0, 1.0)
                            val overBudget = cat.budgetLimit != null &&
                                    cat.totalSpent > cat.budgetLimit
                            LinearProgressIndicator(
                                progress = barProgress.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (overBudget) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            cat.budgetLimit?.let { budget ->
                                Text(
                                    "RM ${"%.2f".format(cat.totalSpent)} / RM ${"%.2f".format(budget)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (overBudget) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                )
                            }
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight, null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(0.3f)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(0.5f)
                    )
                }
            }

            // ── SECTION 5: Daily Spending Canvas Chart ────────────────────────
            Spacer(Modifier.height(12.dp))
            Text(
                "Daily Spending",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            if (s.dailyTotals.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No spending recorded this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.4f)
                    )
                }
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
                var tappedDay by remember { mutableStateOf<DailySpending?>(null) }
                val maxVal = s.dailyTotals.maxOf { it.total }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        tappedDay?.let { td ->
                            Text(
                                "Day ${td.day} · RM ${"%.2f".format(td.total)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = primaryColor,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .pointerInput(s.dailyTotals) {
                                    detectTapGestures { offset ->
                                        val barCount = s.dailyTotals.size
                                        val barWidth = size.width / (barCount * 1.5f)
                                        val gap = barWidth * 0.5f
                                        s.dailyTotals.forEachIndexed { i, d ->
                                            val x = i * (barWidth + gap)
                                            if (offset.x >= x && offset.x <= x + barWidth) {
                                                tappedDay = d
                                            }
                                        }
                                    }
                                }
                        ) {
                            val barCount = s.dailyTotals.size
                            val barWidth = size.width / (barCount * 1.5f)
                            val gap = barWidth * 0.5f
                            val chartHeight = size.height - 20f

                            // Draw baseline
                            drawLine(
                                color = surfaceVariant,
                                start = Offset(0f, chartHeight),
                                end = Offset(size.width, chartHeight),
                                strokeWidth = 1f
                            )

                            s.dailyTotals.forEachIndexed { i, d ->
                                val barH = if (maxVal > 0) ((d.total / maxVal) * chartHeight).toFloat() else 0f
                                val x = i * (barWidth + gap)
                                val isSelected = tappedDay?.day == d.day
                                drawRect(
                                    color = if (isSelected) primaryColor.copy(alpha = 1f)
                                    else primaryColor.copy(alpha = 0.7f),
                                    topLeft = Offset(x, chartHeight - barH),
                                    size = Size(barWidth, barH)
                                )
                            }
                        }
                    }
                }
            }

            // ── SECTION 6: Top Merchants ──────────────────────────────────────
            if (s.topMerchants.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Top Spending",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        s.topMerchants.forEachIndexed { i, m ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${i + 1}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    m.merchantName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "RM ${"%.2f".format(m.totalSpent)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "${m.transactionCount} transaction${if (m.transactionCount > 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                    )
                                }
                            }
                            if (i < s.topMerchants.lastIndex)
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f)
                                )
                        }
                    }
                }
            }

        } ?: run {
            // Empty state — no data
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No data for $displayMonth",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Add some expenses to see analytics",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                    )
                }
            }
        }
    }
}
}
