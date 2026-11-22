package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.BudgetAlertLevel
import com.example.data.model.BudgetStatusTier
import com.example.data.model.CategorySpendSummary
import com.example.data.model.ExpenseCategory
import com.example.data.model.FinancialReportSummary
import com.example.data.model.FinGuardFormatters
import com.example.data.model.PeriodBarPoint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalFinGuardColors
import kotlin.math.roundToInt

@Composable
fun CircularBudgetProgressIndicator(
    statusTier: BudgetStatusTier,
    usedPercent: Int,
    spentInPaise: Long,
    budgetInPaise: Long,
    overspentInPaise: Long,
    modifier: Modifier = Modifier,
    ringSize: Dp = 152.dp
) {
    val targetPrimary = statusTier.primaryColor
    val targetAccent = statusTier.accentColor
    val animatedPrimary by animateColorAsState(
        targetValue = targetPrimary,
        animationSpec = tween(durationMillis = 320),
        label = "budget_ring_primary"
    )
    val animatedAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 320),
        label = "budget_ring_accent"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = usedPercent.coerceIn(0, 100) / 100f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "budget_ring_sweep"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val statusIcon = when (statusTier) {
        BudgetStatusTier.HEALTHY -> Icons.Filled.VerifiedUser
        BudgetStatusTier.MODERATE -> Icons.Filled.TrendingUp
        BudgetStatusTier.CAUTION -> Icons.Filled.NotificationsActive
        BudgetStatusTier.HIGH -> Icons.Filled.WarningAmber
        BudgetStatusTier.EXCEEDED -> Icons.Filled.ErrorOutline
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(ringSize)
                .testTag("budget_circular_indicator"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                val strokeWidth = 14.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                if (animatedProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                animatedPrimary,
                                animatedAccent,
                                animatedPrimary
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = statusTier.label,
                    tint = animatedAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$usedPercent%",
                    style = MaterialTheme.typography.displayMedium,
                    color = animatedAccent
                )
                Text(
                    text = statusTier.shortBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (budgetInPaise > 0L && overspentInPaise > 0L) {
            Text(
                text = "Overspent by ${FinGuardFormatters.formatPaise(overspentInPaise)}",
                style = MaterialTheme.typography.labelMedium,
                color = animatedAccent
            )
        } else if (budgetInPaise <= 0L && spentInPaise > 0L) {
            Text(
                text = "Spent ${FinGuardFormatters.formatPaise(spentInPaise)} without limit",
                style = MaterialTheme.typography.labelMedium,
                color = animatedAccent
            )
        }
    }
}

@Composable
fun BudgetAlertBanner(
    alertLevel: BudgetAlertLevel,
    statusTier: BudgetStatusTier,
    usedPercent: Int,
    spentInPaise: Long,
    budgetInPaise: Long,
    remainingInPaise: Long,
    overspentInPaise: Long,
    modifier: Modifier = Modifier
) {
    val animatedAccent by animateColorAsState(
        targetValue = statusTier.accentColor,
        animationSpec = tween(durationMillis = 320),
        label = "budget_alert_accent"
    )
    val animatedBg by animateColorAsState(
        targetValue = statusTier.containerColor,
        animationSpec = tween(durationMillis = 320),
        label = "budget_alert_bg"
    )
    val progress by animateFloatAsState(
        targetValue = (usedPercent.coerceIn(0, 100)) / 100f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "budget_progress"
    )

    val icon = when (statusTier) {
        BudgetStatusTier.HEALTHY -> Icons.Filled.CheckCircle
        BudgetStatusTier.MODERATE -> Icons.Filled.TrendingUp
        BudgetStatusTier.CAUTION -> Icons.Filled.NotificationsActive
        BudgetStatusTier.HIGH -> Icons.Filled.WarningAmber
        BudgetStatusTier.EXCEEDED -> Icons.Filled.ErrorOutline
    }

    val statusTitle = when {
        budgetInPaise <= 0L && spentInPaise == 0L -> "Budget Ready to Configure"
        alertLevel == BudgetAlertLevel.NORMAL -> statusTier.label
        else -> alertLevel.title
    }

    val subtitle = when {
        budgetInPaise <= 0L && spentInPaise == 0L ->
            "Set your monthly budget limit to activate live 5-tier status tracking and 75% / 90% / 100% alerts."
        budgetInPaise <= 0L && spentInPaise > 0L ->
            "No monthly budget is set yet, while ${FinGuardFormatters.formatPaise(overspentInPaise)} in expenses has been logged."
        statusTier == BudgetStatusTier.EXCEEDED ->
            "Budget exceeded by ${FinGuardFormatters.formatPaise(overspentInPaise)}. Pause non-essential purchases to recover savings."
        statusTier == BudgetStatusTier.HIGH ->
            "High spending (${usedPercent}% used). Only ${FinGuardFormatters.formatPaise(remainingInPaise.coerceAtLeast(0L))} left for this month."
        statusTier == BudgetStatusTier.CAUTION ->
            "Caution zone (${usedPercent}% used). Keep daily outflows within ${FinGuardFormatters.formatPaise(remainingInPaise.coerceAtLeast(0L))} remaining."
        statusTier == BudgetStatusTier.MODERATE ->
            "Moderate pace (${usedPercent}% used). ${FinGuardFormatters.formatPaise(remainingInPaise.coerceAtLeast(0L))} remaining before caution zone."
        else ->
            "Healthy pace (${usedPercent}% used). Strong ${FinGuardFormatters.formatPaise(remainingInPaise.coerceAtLeast(0L))} buffer remains."
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_alert_banner"),
        shape = MaterialTheme.shapes.large,
        color = animatedBg.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.58f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = statusTitle,
                        tint = animatedAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = statusTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = statusTier.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = animatedAccent
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = animatedAccent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = alertLevel.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = animatedAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Animated horizontal bar with smooth gradient fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(statusTier.primaryColor, animatedAccent)
                            )
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${FinGuardFormatters.formatPaise(spentInPaise)} spent",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (budgetInPaise > 0L) {
                        "$usedPercent% of ${FinGuardFormatters.formatPaise(budgetInPaise)}"
                    } else {
                        "No limit set"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMonoFontFamily,
                    color = animatedAccent
                )
            }

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseDonutChart(
    breakdown: List<CategorySpendSummary>,
    totalExpensesInPaise: Long,
    selectedCategory: ExpenseCategory?,
    onSelectCategory: (ExpenseCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSummary = selectedCategory?.let { sel ->
        breakdown.firstOrNull { it.category == sel }
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val sweepProgress by animateFloatAsState(
        targetValue = if (breakdown.isEmpty()) 0f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "donut_sweep"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(176.dp)) {
                val strokeBase = 24.dp.toPx()
                val diameter = size.minDimension - strokeBase
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                val arcSize = Size(diameter, diameter)

                if (breakdown.isEmpty() || totalExpensesInPaise <= 0L) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeBase, cap = StrokeCap.Round)
                    )
                } else {
                    var startAngle = -90f
                    breakdown.forEach { item ->
                        val rawSweep = (item.sharePercent / 100f) * 360f * sweepProgress
                        val gap = if (breakdown.size > 1) 3f else 0f
                        val sweep = (rawSweep - gap).coerceAtLeast(1.5f)
                        val isSelected = selectedCategory == null || selectedCategory == item.category
                        val alpha = if (isSelected) 1f else 0.28f
                        val strokeW = if (selectedCategory == item.category) strokeBase + 6.dp.toPx() else strokeBase
                        drawArc(
                            color = item.category.chartColor.copy(alpha = alpha),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                        )
                        startAngle += rawSweep
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = activeSummary?.category?.label ?: "30D Total",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeSummary?.category?.chartColor
                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FinGuardFormatters.formatPaise(
                        activeSummary?.totalInPaise ?: totalExpensesInPaise
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (activeSummary != null) {
                    Text(
                        text = "${activeSummary.sharePercent.roundToInt()}% • ${activeSummary.itemCount} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalFinGuardColors.current.mutedText
                    )
                }
            }
        }

        if (breakdown.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                breakdown.forEach { item ->
                    val isSelected = selectedCategory == item.category
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(item.category) },
                        label = {
                            Text(
                                text = "${item.category.label} ${item.sharePercent.roundToInt()}%",
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(item.category.chartColor)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = item.category.chartColor.copy(alpha = 0.22f),
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("budget_cat_slice_${item.category.name.lowercase()}")
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                breakdown.forEach { item ->
                    val highlighted = selectedCategory == item.category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onSelectCategory(item.category) }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(item.category.chartColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.category.icon,
                                contentDescription = item.category.label,
                                tint = item.category.chartColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.category.label} (${item.itemCount})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = FinGuardFormatters.formatPaise(item.totalInPaise),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (item.sharePercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = item.category.chartColor,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyMonthlyComparisonStrip(
    weeklyReport: FinancialReportSummary,
    monthlyReport: FinancialReportSummary,
    modifier: Modifier = Modifier
) {
    val ext = LocalFinGuardColors.current
    val maxTotal = maxOf(weeklyReport.totalSpentInPaise, monthlyReport.totalSpentInPaise, 1L)
    val weekFraction = (weeklyReport.totalSpentInPaise.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f)
    val monthFraction = (monthlyReport.totalSpentInPaise.toFloat() / maxTotal.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, ext.cardBorder.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Weekly vs Monthly Pace Comparison",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Last 7 Days (${weeklyReport.transactionCount} entries)",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.auroraMint
                    )
                    Text(
                        text = "${FinGuardFormatters.formatPaise(weeklyReport.totalSpentInPaise)} • ${FinGuardFormatters.formatPaise(weeklyReport.dailyAverageInPaise)}/day",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { weekFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ext.tropicalAqua,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Last 30 Days (${monthlyReport.transactionCount} entries)",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.lightCyan
                    )
                    Text(
                        text = "${FinGuardFormatters.formatPaise(monthlyReport.totalSpentInPaise)} • ${FinGuardFormatters.formatPaise(monthlyReport.dailyAverageInPaise)}/day",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { monthFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = ext.oceanBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

@Composable
fun SpendingTrendBarChart(
    bars: List<PeriodBarPoint>,
    targetLineInPaise: Long,
    accentColor: Color = LocalFinGuardColors.current.oceanBlue,
    modifier: Modifier = Modifier
) {
    val ext = LocalFinGuardColors.current
    var selectedIndex by remember(bars) {
        mutableIntStateOf(bars.indexOfLast { it.amountInPaise > 0L }.coerceAtLeast(0))
    }

    val safeIndex = selectedIndex.coerceIn(0, (bars.size - 1).coerceAtLeast(0))
    val activePoint = bars.getOrNull(safeIndex)
    val maxAmount = (bars.maxOfOrNull { it.amountInPaise } ?: 0L)
        .coerceAtLeast(targetLineInPaise)
        .coerceAtLeast(100_00L)

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectedBarColor = ext.auroraMint

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (activePoint != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activePoint.label} • ${activePoint.itemCount} expense(s)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FinGuardFormatters.formatPaise(activePoint.amountInPaise),
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = JetBrainsMonoFontFamily,
                        color = selectedBarColor
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            if (bars.isEmpty()) return@Canvas

            val chartHeight = size.height
            val slotWidth = size.width / bars.size.toFloat()
            val barWidth = (slotWidth * 0.56f).coerceIn(14.dp.toPx(), 40.dp.toPx())

            drawLine(
                color = gridColor,
                start = Offset(0f, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            if (targetLineInPaise > 0L) {
                val targetRatio = (targetLineInPaise.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
                val targetY = chartHeight - (chartHeight * targetRatio)
                drawLine(
                    color = ext.champagneGold.copy(alpha = 0.65f),
                    start = Offset(0f, targetY),
                    end = Offset(size.width, targetY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            bars.forEachIndexed { index, point ->
                val ratio = (point.amountInPaise.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
                val barHeight = (chartHeight * ratio).coerceAtLeast(if (point.amountInPaise > 0L) 6.dp.toPx() else 3.dp.toPx())
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = chartHeight - barHeight
                val isSelected = index == safeIndex
                drawRoundRect(
                    color = when {
                        isSelected -> selectedBarColor
                        point.amountInPaise > 0L -> accentColor.copy(alpha = 0.85f)
                        else -> gridColor.copy(alpha = 0.55f)
                    },
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            bars.forEachIndexed { index, point ->
                val isSelected = index == safeIndex
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("budget_bar_$index"),
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    },
                    border = if (isSelected) {
                        BorderStroke(1.dp, selectedBarColor.copy(alpha = 0.6f))
                    } else null,
                    onClick = { selectedIndex = index }
                ) {
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) selectedBarColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
