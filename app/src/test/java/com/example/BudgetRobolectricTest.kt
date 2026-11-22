package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budget.BudgetAiService
import com.example.data.FinGuardRepository
import com.example.data.local.FinGuardDatabase
import com.example.data.local.SavingsGoalEntity
import com.example.data.model.AnalysisEngine
import com.example.data.model.BudgetAdviceResult
import com.example.data.model.BudgetAlertLevel
import com.example.data.model.BudgetCalculator
import com.example.data.model.BudgetSnapshotMetrics
import com.example.data.model.ExpenseCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BudgetRobolectricTest {

    private lateinit var db: FinGuardDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FinGuardDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `sample budget data seeds 77 percent alert across all 7 categories and 2 savings goals`() = runTest {
        val repo = FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            budgetDao = db.budgetDao()
        )
        repo.initializeSandbox()

        val profile = repo.budgetProfile.first()!!
        val expenses = repo.budgetExpenses.first()
        val goals = repo.savingsGoals.first()

        assertEquals(4_500_000L, profile.monthlyIncomeInPaise)
        assertEquals(2_500_000L, profile.monthlyBudgetInPaise)
        assertTrue(profile.isSampleData)
        assertEquals(8, expenses.size)
        assertEquals(2, goals.size)

        val metrics = BudgetCalculator.computeMetrics(profile, expenses, goals)
        assertEquals(1_925_000L, metrics.totalExpensesInPaise)
        assertEquals(575_000L, metrics.remainingBudgetInPaise)
        assertEquals(2_575_000L, metrics.netSavingsInPaise)
        assertEquals(77, metrics.budgetUsedPercent)
        assertEquals(BudgetAlertLevel.WARNING_75, metrics.alertLevel)
        assertEquals(7, metrics.categoryBreakdown.size)
    }

    @Test
    fun `adding editing and deleting expenses updates budget totals and triggers 90 and 100 percent alerts`() = runTest {
        val repo = FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            budgetDao = db.budgetDao()
        )
        repo.initializeSandbox()

        val now = System.currentTimeMillis()
        // Current sample spend is ₹19,250 / ₹25,000 (77%). Add ₹3,750 -> ₹23,000 (92% -> HIGH_90)
        repo.saveExpense(
            title = "Flight Ticket",
            amountInPaise = 375_000L,
            category = ExpenseCategory.TRAVEL,
            note = "Weekend trip",
            expenseDateMillis = now
        )
        var expenses = repo.budgetExpenses.first()
        var metrics = BudgetCalculator.computeMetrics(repo.budgetProfile.first(), expenses, repo.savingsGoals.first())
        assertEquals(2_300_000L, metrics.totalExpensesInPaise)
        assertEquals(92, metrics.budgetUsedPercent)
        assertEquals(BudgetAlertLevel.HIGH_90, metrics.alertLevel)

        // Edit the added expense from ₹3,750 to ₹6,750 -> total ₹26,000 (104% -> EXCEEDED_100)
        val added = expenses.first { it.title == "Flight Ticket" }
        repo.saveExpense(
            expenseId = added.id,
            title = "Flight Ticket Upgraded",
            amountInPaise = 675_000L,
            category = ExpenseCategory.TRAVEL,
            note = "Updated amount",
            expenseDateMillis = now
        )
        expenses = repo.budgetExpenses.first()
        metrics = BudgetCalculator.computeMetrics(repo.budgetProfile.first(), expenses, repo.savingsGoals.first())
        assertEquals(2_600_000L, metrics.totalExpensesInPaise)
        assertEquals(-100_000L, metrics.remainingBudgetInPaise)
        assertEquals(104, metrics.budgetUsedPercent)
        assertEquals(BudgetAlertLevel.EXCEEDED_100, metrics.alertLevel)

        // Delete the added expense -> returns cleanly to 77% WARNING_75
        repo.deleteExpense(added.id)
        expenses = repo.budgetExpenses.first()
        metrics = BudgetCalculator.computeMetrics(repo.budgetProfile.first(), expenses, repo.savingsGoals.first())
        assertEquals(1_925_000L, metrics.totalExpensesInPaise)
        assertEquals(77, metrics.budgetUsedPercent)
        assertEquals(BudgetAlertLevel.WARNING_75, metrics.alertLevel)
    }

    @Test
    fun `savings goals creation contribution deletion and reset modes work accurately`() = runTest {
        val repo = FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            budgetDao = db.budgetDao()
        )
        repo.initializeSandbox()

        repo.createSavingsGoal(
            title = "New Laptop",
            targetInPaise = 5_000_000L,
            initialSavedInPaise = 1_000_000L,
            deadlineEpochMillis = System.currentTimeMillis() + 60L * 86_400_000L
        )
        val created = repo.savingsGoals.first().first { it.title == "New Laptop" }
        assertEquals(1_000_000L, created.savedAmountInPaise)

        repo.contributeToGoal(created.id, 500_000L)
        val updated = repo.savingsGoals.first().first { it.id == created.id }
        assertEquals(1_500_000L, updated.savedAmountInPaise)

        repo.deleteSavingsGoal(created.id)
        assertTrue(repo.savingsGoals.first().none { it.id == created.id })

        // Reset to blank clears expenses, goals, and sets income/budget to 0
        repo.resetBudgetToBlank()
        assertEquals(0L, repo.budgetProfile.first()!!.monthlyIncomeInPaise)
        assertEquals(0L, repo.budgetProfile.first()!!.monthlyBudgetInPaise)
        assertTrue(repo.budgetExpenses.first().isEmpty())
        assertTrue(repo.savingsGoals.first().isEmpty())

        // Restore sample demo restores all 8 expenses and 2 goals
        repo.restoreSampleBudgetData()
        assertEquals(4_500_000L, repo.budgetProfile.first()!!.monthlyIncomeInPaise)
        assertEquals(8, repo.budgetExpenses.first().size)
        assertEquals(2, repo.savingsGoals.first().size)
    }

    @Test
    fun `budget profile, expenses, and savings goals persist across cold database restarts`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "budget_restart_persistence_test.db"
        context.deleteDatabase(dbName)

        val db1 = Room.databaseBuilder(context, FinGuardDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repo1 = FinGuardRepository(db1.walletDao(), db1.scanHistoryDao(), budgetDao = db1.budgetDao())
        repo1.initializeSandbox()
        repo1.resetBudgetToBlank()
        repo1.saveBudgetProfile(incomeInPaise = 6_000_000L, budgetInPaise = 3_000_000L)
        repo1.saveExpense(
            title = "University Books",
            amountInPaise = 180_000L,
            category = ExpenseCategory.EDUCATION,
            note = "Semester texts",
            expenseDateMillis = System.currentTimeMillis()
        )
        repo1.createSavingsGoal(
            title = "Safety Buffer",
            targetInPaise = 2_000_000L,
            initialSavedInPaise = 500_000L,
            deadlineEpochMillis = System.currentTimeMillis() + 90L * 86_400_000L
        )
        db1.close()

        val db2 = Room.databaseBuilder(context, FinGuardDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        val repo2 = FinGuardRepository(db2.walletDao(), db2.scanHistoryDao(), budgetDao = db2.budgetDao())
        repo2.initializeSandbox() // Must NOT overwrite user's saved profile/expenses!

        val profile = repo2.budgetProfile.first()!!
        val expenses = repo2.budgetExpenses.first()
        val goals = repo2.savingsGoals.first()

        assertEquals(6_000_000L, profile.monthlyIncomeInPaise)
        assertEquals(3_000_000L, profile.monthlyBudgetInPaise)
        assertEquals(1, expenses.size)
        assertEquals("University Books", expenses.first().title)
        assertEquals(1, goals.size)
        assertEquals("Safety Buffer", goals.first().title)

        db2.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `gemini budget coach uses AI when online and automatically falls back offline on failure or no network`() = runTest {
        val successAdvisor = object : BudgetAiService {
            override suspend fun advise(
                metrics: BudgetSnapshotMetrics,
                goals: List<SavingsGoalEntity>
            ): BudgetAdviceResult = BudgetAdviceResult(
                summary = "AI customized insight for your budget.",
                suggestions = listOf("Trim Food by 10%", "Boost Emergency Fund"),
                topFocusArea = "Food",
                engineUsed = AnalysisEngine.GEMINI_AI
            )
        }
        val failingAdvisor = object : BudgetAiService {
            override suspend fun advise(
                metrics: BudgetSnapshotMetrics,
                goals: List<SavingsGoalEntity>
            ): BudgetAdviceResult {
                throw IllegalStateException("AI endpoint unreachable")
            }
        }

        val repoOnline = FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            budgetDao = db.budgetDao(),
            isNetworkAvailable = { true },
            budgetAiService = successAdvisor
        )
        repoOnline.initializeSandbox()
        val metrics = BudgetCalculator.computeMetrics(
            repoOnline.budgetProfile.first(),
            repoOnline.budgetExpenses.first(),
            repoOnline.savingsGoals.first()
        )
        val goals = repoOnline.savingsGoals.first()

        val aiRes = repoOnline.generateBudgetAdvice(metrics, goals, useGeminiAi = true)
        assertEquals(AnalysisEngine.GEMINI_AI, aiRes.engineUsed)
        assertNull(aiRes.fallbackNotice)

        val repoError = FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            budgetDao = db.budgetDao(),
            isNetworkAvailable = { true },
            budgetAiService = failingAdvisor
        )
        val errFallback = repoError.generateBudgetAdvice(metrics, goals, useGeminiAi = true)
        assertEquals(AnalysisEngine.OFFLINE_HEURISTIC, errFallback.engineUsed)
        assertTrue(errFallback.fallbackNotice?.contains("Automatically generated rule-based offline budgeting advice") == true)

        val repoOffline = FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            budgetDao = db.budgetDao(),
            isNetworkAvailable = { false },
            budgetAiService = failingAdvisor
        )
        val offlineRes = repoOffline.generateBudgetAdvice(metrics, goals, useGeminiAi = true)
        assertEquals(AnalysisEngine.OFFLINE_HEURISTIC, offlineRes.engineUsed)
        assertTrue(offlineRes.fallbackNotice?.contains("No internet connection detected") == true)
    }
}
