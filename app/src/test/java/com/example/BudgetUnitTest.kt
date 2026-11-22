package com.example

import com.example.data.local.BudgetExpenseEntity
import com.example.data.local.BudgetProfileEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.model.AnalysisEngine
import com.example.data.model.BudgetAlertLevel
import com.example.data.model.BudgetCalculator
import com.example.data.model.BudgetReportPeriod
import com.example.data.model.BudgetStatusTier
import com.example.data.model.ExpenseCategory
import com.example.data.model.FinGuardFormatters
import com.example.data.model.OfflineBudgetAdvisor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class BudgetUnitTest {

    @Test
    fun `budget alert thresholds classify 75, 90, and 100 percent boundaries accurately`() {
        assertEquals(BudgetAlertLevel.NORMAL, BudgetAlertLevel.fromPercentage(0))
        assertEquals(BudgetAlertLevel.NORMAL, BudgetAlertLevel.fromPercentage(74))
        assertEquals(BudgetAlertLevel.WARNING_75, BudgetAlertLevel.fromPercentage(75))
        assertEquals(BudgetAlertLevel.WARNING_75, BudgetAlertLevel.fromPercentage(89))
        assertEquals(BudgetAlertLevel.HIGH_90, BudgetAlertLevel.fromPercentage(90))
        assertEquals(BudgetAlertLevel.HIGH_90, BudgetAlertLevel.fromPercentage(99))
        assertEquals(BudgetAlertLevel.EXCEEDED_100, BudgetAlertLevel.fromPercentage(100))
        assertEquals(BudgetAlertLevel.EXCEEDED_100, BudgetAlertLevel.fromPercentage(135))
    }

    @Test
    fun `dynamic 5-tier budget status colors map exact 0-49, 50-69, 70-89, 90-99, and 100+ ranges`() {
        assertEquals(BudgetStatusTier.HEALTHY, BudgetStatusTier.fromPercentage(0))
        assertEquals(BudgetStatusTier.HEALTHY, BudgetStatusTier.fromPercentage(49))
        assertEquals(BudgetStatusTier.MODERATE, BudgetStatusTier.fromPercentage(50))
        assertEquals(BudgetStatusTier.MODERATE, BudgetStatusTier.fromPercentage(69))
        assertEquals(BudgetStatusTier.CAUTION, BudgetStatusTier.fromPercentage(70))
        assertEquals(BudgetStatusTier.CAUTION, BudgetStatusTier.fromPercentage(89))
        assertEquals(BudgetStatusTier.HIGH, BudgetStatusTier.fromPercentage(90))
        assertEquals(BudgetStatusTier.HIGH, BudgetStatusTier.fromPercentage(99))
        assertEquals(BudgetStatusTier.EXCEEDED, BudgetStatusTier.fromPercentage(100))
        assertEquals(BudgetStatusTier.EXCEEDED, BudgetStatusTier.fromPercentage(142))
    }

    @Test
    fun `zero budget edge cases and overspentInPaise compute safely`() {
        val now = System.currentTimeMillis()
        // Zero budget & zero expenses -> stays HEALTHY / NORMAL, zero overspend
        val zeroEmpty = BudgetCalculator.computeMetrics(
            profile = BudgetProfileEntity(monthlyIncomeInPaise = 0L, monthlyBudgetInPaise = 0L),
            expenses = emptyList(),
            goals = emptyList(),
            nowMillis = now
        )
        assertEquals(BudgetStatusTier.HEALTHY, zeroEmpty.statusTier)
        assertEquals(BudgetAlertLevel.NORMAL, zeroEmpty.alertLevel)
        assertEquals(0L, zeroEmpty.overspentInPaise)
        assertEquals(0, zeroEmpty.budgetUsedPercent)

        // Zero budget with expenses -> EXCEEDED with full spend as overspentInPaise
        val zeroWithSpend = BudgetCalculator.computeMetrics(
            profile = BudgetProfileEntity(monthlyIncomeInPaise = 500_000L, monthlyBudgetInPaise = 0L),
            expenses = listOf(BudgetExpenseEntity(title = "Snacks", amountInPaise = 25_000L, category = "FOOD", expenseDateMillis = now)),
            goals = emptyList(),
            nowMillis = now
        )
        assertEquals(BudgetStatusTier.EXCEEDED, zeroWithSpend.statusTier)
        assertEquals(100, zeroWithSpend.budgetUsedPercent)
        assertEquals(25_000L, zeroWithSpend.overspentInPaise)
    }

    @Test
    fun `calculator computes income, expenses, remaining, savings, category breakdown, and reports accurately`() {
        val dayMs = 86_400_000L
        val fixedNow = 1_763_683_200_000L // deterministic mid-day epoch
        val todayStart = BudgetCalculator.startOfDayMillis(fixedNow)

        val profile = BudgetProfileEntity(
            monthlyIncomeInPaise = 5_000_000L, // ₹50,000
            monthlyBudgetInPaise = 2_000_000L  // ₹20,000
        )

        val expenses = listOf(
            BudgetExpenseEntity(id = 1, title = "Meals", amountInPaise = 400_000L, category = "FOOD", expenseDateMillis = todayStart + 1000L),
            BudgetExpenseEntity(id = 2, title = "Cab", amountInPaise = 200_000L, category = "TRAVEL", expenseDateMillis = todayStart - 2 * dayMs),
            BudgetExpenseEntity(id = 3, title = "Jacket", amountInPaise = 300_000L, category = "SHOPPING", expenseDateMillis = todayStart - 4 * dayMs),
            BudgetExpenseEntity(id = 4, title = "Course", amountInPaise = 250_000L, category = "EDUCATION", expenseDateMillis = todayStart - 8 * dayMs),
            BudgetExpenseEntity(id = 5, title = "Power", amountInPaise = 450_000L, category = "BILLS", expenseDateMillis = todayStart - 12 * dayMs),
            BudgetExpenseEntity(id = 6, title = "Clinic", amountInPaise = 150_000L, category = "HEALTHCARE", expenseDateMillis = todayStart - 16 * dayMs),
            BudgetExpenseEntity(id = 7, title = "Misc", amountInPaise = 50_000L, category = "OTHERS", expenseDateMillis = todayStart - 20 * dayMs),
            // Outside 30D window (35 days ago) -> must be excluded from 30D monthly budget metrics
            BudgetExpenseEntity(id = 8, title = "Old Item", amountInPaise = 999_900L, category = "OTHERS", expenseDateMillis = todayStart - 35 * dayMs)
        )

        val metrics = BudgetCalculator.computeMetrics(profile, expenses, emptyList(), nowMillis = fixedNow)

        // Monthly expenses = 4,000 + 2,000 + 3,000 + 2,500 + 4,500 + 1,500 + 500 = ₹18,000 (1,800,000 paise)
        assertEquals(5_000_000L, metrics.monthlyIncomeInPaise)
        assertEquals(2_000_000L, metrics.monthlyBudgetInPaise)
        assertEquals(1_800_000L, metrics.totalExpensesInPaise)
        assertEquals(200_000L, metrics.remainingBudgetInPaise) // ₹2,000 left
        assertEquals(3_200_000L, metrics.netSavingsInPaise)     // ₹32,000 saved
        assertEquals(64, metrics.savingsRatePercent)
        assertEquals(90, metrics.budgetUsedPercent)             // 18k / 20k = 90%
        assertEquals(BudgetAlertLevel.HIGH_90, metrics.alertLevel)

        // All 7 categories present & ordered descending by total spend
        assertEquals(7, metrics.categoryBreakdown.size)
        assertEquals(ExpenseCategory.BILLS, metrics.categoryBreakdown.first().category)
        assertEquals(ExpenseCategory.OTHERS, metrics.categoryBreakdown.last().category)
        val totalShare = metrics.categoryBreakdown.sumOf { it.sharePercent.toDouble() }
        assertEquals(100, totalShare.roundToInt())

        // Weekly report includes only items within last 7 days (id 1, 2, 3 = ₹9,000 = 900,000 paise)
        assertEquals(BudgetReportPeriod.WEEKLY, metrics.weeklyReport.period)
        assertEquals(900_000L, metrics.weeklyReport.totalSpentInPaise)
        assertEquals(3, metrics.weeklyReport.transactionCount)
        assertEquals(7, metrics.weeklyReport.barPoints.size)
        assertEquals(ExpenseCategory.FOOD, metrics.weeklyReport.topCategory)

        // Monthly report includes all 7 recent items across 4 weekly buckets
        assertEquals(BudgetReportPeriod.MONTHLY, metrics.monthlyReport.period)
        assertEquals(1_800_000L, metrics.monthlyReport.totalSpentInPaise)
        assertEquals(7, metrics.monthlyReport.transactionCount)
        assertEquals(4, metrics.monthlyReport.barPoints.size)
        assertEquals(ExpenseCategory.BILLS, metrics.monthlyReport.topCategory)
    }

    @Test
    fun `over-budget expenses yield negative remaining budget and EXCEEDED_100 alert`() {
        val now = System.currentTimeMillis()
        val profile = BudgetProfileEntity(
            monthlyIncomeInPaise = 2_000_000L,
            monthlyBudgetInPaise = 1_000_000L
        )
        val expenses = listOf(
            BudgetExpenseEntity(title = "Rent", amountInPaise = 1_250_000L, category = "BILLS", expenseDateMillis = now)
        )

        val metrics = BudgetCalculator.computeMetrics(profile, expenses, emptyList(), nowMillis = now)
        assertEquals(-250_000L, metrics.remainingBudgetInPaise)
        assertEquals(250_000L, metrics.overspentInPaise)
        assertEquals(125, metrics.budgetUsedPercent)
        assertEquals(BudgetAlertLevel.EXCEEDED_100, metrics.alertLevel)
        assertEquals(BudgetStatusTier.EXCEEDED, metrics.statusTier)
    }

    @Test
    fun `savings goal progress and days remaining clamp properly`() {
        val now = 1_763_683_200_000L
        val goal = SavingsGoalEntity(
            title = "Laptop",
            targetAmountInPaise = 4_000_000L,
            savedAmountInPaise = 1_000_000L,
            deadlineEpochMillis = BudgetCalculator.startOfDayMillis(now) + 30L * 86_400_000L
        )
        assertEquals(0.25f, BudgetCalculator.goalProgressFraction(goal), 0.001f)
        assertEquals(30L, BudgetCalculator.goalDaysRemaining(goal, now))

        val overGoal = goal.copy(savedAmountInPaise = 5_000_000L, deadlineEpochMillis = now - 86_400_000L)
        assertEquals(1.0f, BudgetCalculator.goalProgressFraction(overGoal), 0.001f)
        assertEquals(0L, BudgetCalculator.goalDaysRemaining(overGoal, now))
    }

    @Test
    fun `offline budget advisor generates contextual guidance for empty, alert, and goal states`() {
        val emptyMetrics = BudgetCalculator.computeMetrics(null, emptyList(), emptyList())
        val emptyAdvice = OfflineBudgetAdvisor.generateAdvice(emptyMetrics, emptyList())
        assertEquals(AnalysisEngine.OFFLINE_HEURISTIC, emptyAdvice.engineUsed)
        assertTrue(emptyAdvice.suggestions.isNotEmpty())

        val now = System.currentTimeMillis()
        val highMetrics = BudgetCalculator.computeMetrics(
            profile = BudgetProfileEntity(monthlyIncomeInPaise = 4_000_000L, monthlyBudgetInPaise = 2_000_000L),
            expenses = listOf(
                BudgetExpenseEntity(title = "Dining", amountInPaise = 1_850_000L, category = "FOOD", expenseDateMillis = now)
            ),
            goals = listOf(
                SavingsGoalEntity(title = "Reserve", targetAmountInPaise = 1_000_000L, savedAmountInPaise = 200_000L, deadlineEpochMillis = now + 86_400_000L)
            ),
            nowMillis = now
        )
        val highAdvice = OfflineBudgetAdvisor.generateAdvice(
            metrics = highMetrics,
            goals = listOf(
                SavingsGoalEntity(title = "Reserve", targetAmountInPaise = 1_000_000L, savedAmountInPaise = 200_000L, deadlineEpochMillis = now + 86_400_000L)
            ),
            fallbackNotice = "Offline test notice"
        )
        assertEquals("Food", highAdvice.topFocusArea)
        assertEquals("Offline test notice", highAdvice.fallbackNotice)
        assertTrue(highAdvice.suggestions.any { it.contains("Critical threshold", ignoreCase = true) })
        assertTrue(highAdvice.suggestions.any { it.contains("Reserve") })
    }

    @Test
    fun `non-negative paise parser handles zero while positive parser rejects zero`() {
        assertNull(FinGuardFormatters.parsePaiseInput("0"))
        assertEquals(0L, FinGuardFormatters.parseNonNegativePaiseInput("0"))
        assertEquals(4_500_050L, FinGuardFormatters.parseNonNegativePaiseInput("45,000.50"))
        assertNull(FinGuardFormatters.parseNonNegativePaiseInput("-100"))
    }
}
