package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsTransit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.BudgetExpenseEntity
import com.example.data.local.BudgetProfileEntity
import com.example.data.local.SavingsGoalEntity
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

enum class ExpenseCategory(
    val label: String,
    val icon: ImageVector,
    val chartColor: Color
) {
    FOOD("Food", Icons.Filled.Restaurant, Color(0xFF48BDB5)),          // Tropical Aqua
    TRAVEL("Travel", Icons.Filled.DirectionsTransit, Color(0xFF146D91)), // Ocean Blue
    SHOPPING("Shopping", Icons.Filled.ShoppingBag, Color(0xFF65C8D8)), // Light Cyan
    EDUCATION("Education", Icons.Filled.School, Color(0xFF78D9C0)),    // Aurora Mint
    BILLS("Bills", Icons.Filled.ReceiptLong, Color(0xFF16845B)),       // Emerald Green
    HEALTHCARE("Healthcare", Icons.Filled.MedicalServices, Color(0xFF83D6A9)), // Luxury Mint Green
    OTHERS("Others", Icons.Filled.Category, Color(0xFFE5C77A));        // Champagne Gold

    companion object {
        fun fromRaw(raw: String): ExpenseCategory =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: OTHERS
    }
}

enum class BudgetStatusTier(
    val label: String,
    val shortBadge: String,
    val primaryColor: Color,
    val accentColor: Color,
    val containerColor: Color
) {
    HEALTHY(
        label = "Healthy Spending",
        shortBadge = "HEALTHY",
        primaryColor = Color(0xFF16845B),
        accentColor = Color(0xFF78D9C0),
        containerColor = Color(0xFF0D2922)
    ),
    MODERATE(
        label = "Moderate Spending",
        shortBadge = "MODERATE",
        primaryColor = Color(0xFF84CC5C),
        accentColor = Color(0xFF84CC5C),
        containerColor = Color(0xFF1A2B18)
    ),
    CAUTION(
        label = "Caution Spending",
        shortBadge = "CAUTION",
        primaryColor = Color(0xFFE8C55A),
        accentColor = Color(0xFFE8C55A),
        containerColor = Color(0xFF2D2512)
    ),
    HIGH(
        label = "High Spending",
        shortBadge = "HIGH SPEND",
        primaryColor = Color(0xFFF58D42),
        accentColor = Color(0xFFF58D42),
        containerColor = Color(0xFF341E11)
    ),
    EXCEEDED(
        label = "Budget Exceeded",
        shortBadge = "EXCEEDED",
        primaryColor = Color(0xFFF06464),
        accentColor = Color(0xFFF06464),
        containerColor = Color(0xFF36161B)
    );

    companion object {
        fun fromPercentage(percentUsed: Int): BudgetStatusTier = when {
            percentUsed >= 100 -> EXCEEDED
            percentUsed >= 90 -> HIGH
            percentUsed >= 70 -> CAUTION
            percentUsed >= 50 -> MODERATE
            else -> HEALTHY
        }
    }
}

enum class BudgetAlertLevel(
    val thresholdPercent: Int,
    val badgeText: String,
    val title: String
) {
    NORMAL(0, "ON TRACK", "Spending Within Safe Budget"),
    WARNING_75(75, "75% ALERT", "75% of Monthly Budget Used"),
    HIGH_90(90, "90% CRITICAL", "90% of Monthly Budget Reached"),
    EXCEEDED_100(100, "100%+ EXCEEDED", "Monthly Budget Exceeded");

    companion object {
        fun fromPercentage(percentUsed: Int): BudgetAlertLevel = when {
            percentUsed >= 100 -> EXCEEDED_100
            percentUsed >= 90 -> HIGH_90
            percentUsed >= 75 -> WARNING_75
            else -> NORMAL
        }
    }
}

enum class BudgetReportPeriod(val label: String) {
    WEEKLY("Weekly (7D)"),
    MONTHLY("Monthly (30D)")
}

data class CategorySpendSummary(
    val category: ExpenseCategory,
    val totalInPaise: Long,
    val sharePercent: Float,
    val itemCount: Int
)

data class PeriodBarPoint(
    val label: String,
    val amountInPaise: Long,
    val itemCount: Int
)

data class FinancialReportSummary(
    val period: BudgetReportPeriod,
    val periodLabel: String,
    val totalSpentInPaise: Long,
    val transactionCount: Int,
    val dailyAverageInPaise: Long,
    val periodBudgetTargetInPaise: Long,
    val topCategory: ExpenseCategory?,
    val topCategoryAmountInPaise: Long,
    val barPoints: List<PeriodBarPoint>
)

data class BudgetAdviceResult(
    val summary: String,
    val suggestions: List<String>,
    val topFocusArea: String,
    val engineUsed: AnalysisEngine,
    val fallbackNotice: String? = null,
    val generatedAtMillis: Long = System.currentTimeMillis()
)

data class BudgetSnapshotMetrics(
    val monthlyIncomeInPaise: Long,
    val monthlyBudgetInPaise: Long,
    val totalExpensesInPaise: Long,
    val remainingBudgetInPaise: Long,
    val overspentInPaise: Long,
    val netSavingsInPaise: Long,
    val savingsRatePercent: Int,
    val budgetUsedPercent: Int,
    val alertLevel: BudgetAlertLevel,
    val statusTier: BudgetStatusTier,
    val categoryBreakdown: List<CategorySpendSummary>,
    val weeklyReport: FinancialReportSummary,
    val monthlyReport: FinancialReportSummary
)

object BudgetCalculator {

    private const val DAY_MS = 86_400_000L

    fun startOfDayMillis(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun computeMetrics(
        profile: BudgetProfileEntity?,
        expenses: List<BudgetExpenseEntity>,
        goals: List<SavingsGoalEntity>,
        nowMillis: Long = System.currentTimeMillis()
    ): BudgetSnapshotMetrics {
        val income = (profile?.monthlyIncomeInPaise ?: 0L).coerceAtLeast(0L)
        val budget = (profile?.monthlyBudgetInPaise ?: 0L).coerceAtLeast(0L)

        val todayStart = startOfDayMillis(nowMillis)
        val monthCutoff = todayStart - 29L * DAY_MS
        val weekCutoff = todayStart - 6L * DAY_MS

        val monthlyExpenses = expenses.filter { it.expenseDateMillis >= monthCutoff }
        val totalExpenses = monthlyExpenses.sumOf { it.amountInPaise }
        val remainingBudget = budget - totalExpenses
        val netSavings = income - totalExpenses

        val budgetUsedPercent = if (budget > 0L) {
            ((totalExpenses.toDouble() / budget.toDouble()) * 100.0).roundToInt().coerceAtLeast(0)
        } else {
            if (totalExpenses > 0L) 100 else 0
        }

        val savingsRatePercent = if (income > 0L) {
            ((netSavings.toDouble() / income.toDouble()) * 100.0).roundToInt()
        } else {
            0
        }

        val alertLevel = if (budget <= 0L && totalExpenses == 0L) {
            BudgetAlertLevel.NORMAL
        } else {
            BudgetAlertLevel.fromPercentage(budgetUsedPercent)
        }

        val statusTier = if (budget <= 0L && totalExpenses == 0L) {
            BudgetStatusTier.HEALTHY
        } else {
            BudgetStatusTier.fromPercentage(budgetUsedPercent)
        }

        val overspentInPaise = if (budget <= 0L) {
            totalExpenses
        } else {
            (totalExpenses - budget).coerceAtLeast(0L)
        }

        val grouped = monthlyExpenses.groupBy { ExpenseCategory.fromRaw(it.category) }
        val categoryBreakdown = grouped.map { (cat, list) ->
            val catTotal = list.sumOf { it.amountInPaise }
            val share = if (totalExpenses > 0L) (catTotal.toFloat() / totalExpenses.toFloat()) * 100f else 0f
            CategorySpendSummary(
                category = cat,
                totalInPaise = catTotal,
                sharePercent = share,
                itemCount = list.size
            )
        }.sortedByDescending { it.totalInPaise }

        val weeklyReport = buildWeeklyReport(monthlyExpenses, budget, todayStart, weekCutoff)
        val monthlyReport = buildMonthlyReport(monthlyExpenses, budget, todayStart)

        return BudgetSnapshotMetrics(
            monthlyIncomeInPaise = income,
            monthlyBudgetInPaise = budget,
            totalExpensesInPaise = totalExpenses,
            remainingBudgetInPaise = remainingBudget,
            overspentInPaise = overspentInPaise,
            netSavingsInPaise = netSavings,
            savingsRatePercent = savingsRatePercent,
            budgetUsedPercent = budgetUsedPercent,
            alertLevel = alertLevel,
            statusTier = statusTier,
            categoryBreakdown = categoryBreakdown,
            weeklyReport = weeklyReport,
            monthlyReport = monthlyReport
        )
    }

    private fun buildWeeklyReport(
        expenses: List<BudgetExpenseEntity>,
        monthlyBudget: Long,
        todayStart: Long,
        weekCutoff: Long
    ): FinancialReportSummary {
        val weekExpenses = expenses.filter { it.expenseDateMillis >= weekCutoff }
        val total = weekExpenses.sumOf { it.amountInPaise }
        val dailyAvg = total / 7L
        val weeklyTarget = ((monthlyBudget.toDouble() / 30.0) * 7.0).roundToInt().toLong()

        val dayFormat = java.text.SimpleDateFormat("EEE", Locale.US)
        val bars = (6 downTo 0).map { offset ->
            val dayStart = todayStart - offset * DAY_MS
            val dayEnd = dayStart + DAY_MS
            val dayItems = weekExpenses.filter { it.expenseDateMillis in dayStart until dayEnd }
            PeriodBarPoint(
                label = dayFormat.format(java.util.Date(dayStart)).take(3),
                amountInPaise = dayItems.sumOf { it.amountInPaise },
                itemCount = dayItems.size
            )
        }

        val topGroup = weekExpenses.groupBy { ExpenseCategory.fromRaw(it.category) }
            .maxByOrNull { (_, list) -> list.sumOf { it.amountInPaise } }

        return FinancialReportSummary(
            period = BudgetReportPeriod.WEEKLY,
            periodLabel = "Last 7 Days Summary",
            totalSpentInPaise = total,
            transactionCount = weekExpenses.size,
            dailyAverageInPaise = dailyAvg,
            periodBudgetTargetInPaise = weeklyTarget,
            topCategory = topGroup?.key,
            topCategoryAmountInPaise = topGroup?.value?.sumOf { it.amountInPaise } ?: 0L,
            barPoints = bars
        )
    }

    private fun buildMonthlyReport(
        monthlyExpenses: List<BudgetExpenseEntity>,
        monthlyBudget: Long,
        todayStart: Long
    ): FinancialReportSummary {
        val total = monthlyExpenses.sumOf { it.amountInPaise }
        val dailyAvg = total / 30L

        // Bucket last 28–30 days into 4 weekly blocks (W1..W4)
        val bars = (3 downTo 0).map { weekIdx ->
            val bucketEnd = todayStart + DAY_MS - (weekIdx * 7L * DAY_MS)
            val bucketStart = if (weekIdx == 3) todayStart - 29L * DAY_MS else bucketEnd - 7L * DAY_MS
            val items = monthlyExpenses.filter { it.expenseDateMillis in bucketStart until bucketEnd }
            val label = when (weekIdx) {
                3 -> "Wk 1"
                2 -> "Wk 2"
                1 -> "Wk 3"
                else -> "This Wk"
            }
            PeriodBarPoint(
                label = label,
                amountInPaise = items.sumOf { it.amountInPaise },
                itemCount = items.size
            )
        }

        val topGroup = monthlyExpenses.groupBy { ExpenseCategory.fromRaw(it.category) }
            .maxByOrNull { (_, list) -> list.sumOf { it.amountInPaise } }

        return FinancialReportSummary(
            period = BudgetReportPeriod.MONTHLY,
            periodLabel = "Last 30 Days Summary",
            totalSpentInPaise = total,
            transactionCount = monthlyExpenses.size,
            dailyAverageInPaise = dailyAvg,
            periodBudgetTargetInPaise = monthlyBudget,
            topCategory = topGroup?.key,
            topCategoryAmountInPaise = topGroup?.value?.sumOf { it.amountInPaise } ?: 0L,
            barPoints = bars
        )
    }

    fun goalProgressFraction(goal: SavingsGoalEntity): Float {
        if (goal.targetAmountInPaise <= 0L) return 0f
        return (goal.savedAmountInPaise.toFloat() / goal.targetAmountInPaise.toFloat()).coerceIn(0f, 1f)
    }

    fun goalDaysRemaining(goal: SavingsGoalEntity, nowMillis: Long = System.currentTimeMillis()): Long {
        val diff = goal.deadlineEpochMillis - startOfDayMillis(nowMillis)
        return (diff / DAY_MS).coerceAtLeast(0L)
    }
}

object OfflineBudgetAdvisor {

    fun generateAdvice(
        metrics: BudgetSnapshotMetrics,
        goals: List<SavingsGoalEntity>,
        fallbackNotice: String? = null
    ): BudgetAdviceResult {
        val suggestions = mutableListOf<String>()
        val topCategory = metrics.categoryBreakdown.firstOrNull()
        val secondCategory = metrics.categoryBreakdown.getOrNull(1)

        if (metrics.monthlyIncomeInPaise <= 0L && metrics.monthlyBudgetInPaise <= 0L) {
            return BudgetAdviceResult(
                summary = "Set your fictional or personal monthly income and spending limit above to unlock tailored budget pacing insights.",
                suggestions = listOf(
                    "Start with the 50/30/20 rule: allocate ~50% to needs (Bills, Food, Healthcare), 30% to lifestyle, and 20% to savings goals.",
                    "Log daily expenses consistently across the 7 categories to spot small recurring leaks.",
                    "Add at least one emergency savings goal with a realistic target deadline."
                ),
                topFocusArea = "Initial Budget Setup",
                engineUsed = AnalysisEngine.OFFLINE_HEURISTIC,
                fallbackNotice = fallbackNotice
            )
        }

        when (metrics.alertLevel) {
            BudgetAlertLevel.EXCEEDED_100 -> {
                val over = FinGuardFormatters.formatPaise(-metrics.remainingBudgetInPaise)
                suggestions.add(
                    "Budget exceeded by $over (${metrics.budgetUsedPercent}% used). Pause non-essential Shopping and Travel spending for the rest of this cycle."
                )
            }
            BudgetAlertLevel.HIGH_90 -> {
                val left = FinGuardFormatters.formatPaise(metrics.remainingBudgetInPaise.coerceAtLeast(0L))
                suggestions.add(
                    "Critical threshold (${metrics.budgetUsedPercent}% used): only $left remains. Cap discretionary daily spend until month-end."
                )
            }
            BudgetAlertLevel.WARNING_75 -> {
                val left = FinGuardFormatters.formatPaise(metrics.remainingBudgetInPaise.coerceAtLeast(0L))
                suggestions.add(
                    "75% alert active (${metrics.budgetUsedPercent}% used). Keep remaining daily expenses within $left to finish the month under budget."
                )
            }
            BudgetAlertLevel.NORMAL -> {
                suggestions.add(
                    "Healthy pace at ${metrics.budgetUsedPercent}% budget utilization. You have ${FinGuardFormatters.formatPaise(metrics.remainingBudgetInPaise.coerceAtLeast(0L))} buffer left."
                )
            }
        }

        if (topCategory != null) {
            val topShare = topCategory.sharePercent.roundToInt()
            val formattedTop = FinGuardFormatters.formatPaise(topCategory.totalInPaise)
            val categoryTip = when (topCategory.category) {
                ExpenseCategory.FOOD -> "Food leads your spending ($formattedTop, $topShare%). Trimming 15% from dining/delivery can free up ${FinGuardFormatters.formatPaise((topCategory.totalInPaise * 0.15).toLong())} monthly."
                ExpenseCategory.SHOPPING -> "Shopping is your largest outflow ($formattedTop, $topShare%). Apply a 24-hour cooling rule before impulse purchases."
                ExpenseCategory.TRAVEL -> "Travel accounts for $topShare% ($formattedTop). Bundling commutes or passes could curb daily transit spikes."
                ExpenseCategory.BILLS -> "Bills represent $topShare% ($formattedTop). Audit recurring subscriptions you haven't used in 30 days."
                ExpenseCategory.EDUCATION -> "Education takes $topShare% ($formattedTop). Plan course/book outlays against a dedicated learning goal."
                ExpenseCategory.HEALTHCARE -> "Healthcare is $topShare% ($formattedTop). Maintain a dedicated medical buffer inside your savings goals."
                ExpenseCategory.OTHERS -> "Uncategorized 'Others' spend is $topShare% ($formattedTop). Tagging expenses accurately helps pinpoint leaks."
            }
            suggestions.add(categoryTip)
        }

        if (secondCategory != null && secondCategory.sharePercent >= 20f) {
            suggestions.add(
                "${secondCategory.category.label} is your #2 expense (${FinGuardFormatters.formatPaise(secondCategory.totalInPaise)}, ${secondCategory.sharePercent.roundToInt()}%). Set a weekly sub-limit for this category."
            )
        }

        if (metrics.monthlyIncomeInPaise > 0L) {
            if (metrics.savingsRatePercent < 20) {
                val targetSave = (metrics.monthlyIncomeInPaise * 0.20).toLong()
                suggestions.add(
                    "Your current net savings rate is ${metrics.savingsRatePercent}%. Aim toward saving at least 20% (${FinGuardFormatters.formatPaise(targetSave)}) before discretionary spending."
                )
            } else {
                suggestions.add(
                    "Strong ${metrics.savingsRatePercent}% savings rate (${FinGuardFormatters.formatPaise(metrics.netSavingsInPaise.coerceAtLeast(0L))} retained). Channel surplus straight into your active savings goals."
                )
            }
        }

        val activeGoal = goals.firstOrNull { it.savedAmountInPaise < it.targetAmountInPaise }
        if (activeGoal != null) {
            val remaining = (activeGoal.targetAmountInPaise - activeGoal.savedAmountInPaise).coerceAtLeast(0L)
            suggestions.add(
                "Goal '${activeGoal.title}' needs ${FinGuardFormatters.formatPaise(remaining)} more. Auto-allocating small weekly transfers keeps it on schedule."
            )
        }

        val summary = buildString {
            append("You spent ${FinGuardFormatters.formatPaise(metrics.totalExpensesInPaise)}")
            if (metrics.monthlyBudgetInPaise > 0L) {
                append(" (${metrics.budgetUsedPercent}% of your ${FinGuardFormatters.formatPaise(metrics.monthlyBudgetInPaise)} monthly budget)")
            }
            if (metrics.monthlyIncomeInPaise > 0L) {
                append(", leaving ${FinGuardFormatters.formatPaise(metrics.netSavingsInPaise)} net savings.")
            } else {
                append(".")
            }
        }

        return BudgetAdviceResult(
            summary = summary,
            suggestions = suggestions.take(4),
            topFocusArea = topCategory?.category?.label ?: "Daily Expense Tracking",
            engineUsed = AnalysisEngine.OFFLINE_HEURISTIC,
            fallbackNotice = fallbackNotice
        )
    }
}
