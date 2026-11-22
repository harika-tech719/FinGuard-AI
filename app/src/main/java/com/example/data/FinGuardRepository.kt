package com.example.data

import android.util.Log
import com.example.budget.BudgetAiService
import com.example.budget.GeminiBudgetAdvisor
import com.example.data.local.BudgetDao
import com.example.data.local.BudgetExpenseEntity
import com.example.data.local.BudgetProfileEntity
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.ScanHistoryDao
import com.example.data.local.ScanRecordEntity
import com.example.data.local.WalletAccountEntity
import com.example.data.local.WalletDao
import com.example.data.local.WalletTransactionEntity
import com.example.data.model.BudgetAdviceResult
import com.example.data.model.BudgetSnapshotMetrics
import com.example.data.model.DemoProfile
import com.example.data.model.ExpenseCategory
import com.example.data.model.FraudAnalysisResult
import com.example.data.model.OfflineBudgetAdvisor
import com.example.data.model.ScanType
import com.example.data.model.SimulationOutcome
import com.example.data.model.TransactionType
import com.example.fraud.FraudAiService
import com.example.fraud.GeminiFraudAnalyzer
import com.example.fraud.OfflineHeuristicAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val TAG = "FinGuardRepository"

class FinGuardRepository(
    private val walletDao: WalletDao,
    private val scanHistoryDao: ScanHistoryDao,
    private val aiService: FraudAiService = GeminiFraudAnalyzer(),
    private val isNetworkAvailable: () -> Boolean = { true },
    private val budgetDao: BudgetDao? = null,
    private val budgetAiService: BudgetAiService = GeminiBudgetAdvisor()
) {
    val allWalletAccounts: Flow<List<WalletAccountEntity>> = walletDao.observeAllAccounts()
    val primaryWalletAccount: Flow<WalletAccountEntity?> = walletDao.observePrimaryAccount()
    val transactions: Flow<List<WalletTransactionEntity>> = walletDao.observeTransactions()
    val scanHistory: Flow<List<ScanRecordEntity>> = scanHistoryDao.observeAllScans()

    val budgetProfile: Flow<BudgetProfileEntity?> = budgetDao?.observeBudgetProfile() ?: emptyFlow()
    val budgetExpenses: Flow<List<BudgetExpenseEntity>> = budgetDao?.observeExpenses() ?: emptyFlow()
    val savingsGoals: Flow<List<SavingsGoalEntity>> = budgetDao?.observeSavingsGoals() ?: emptyFlow()

    suspend fun initializeSandbox() {
        walletDao.ensureSeeded()
        budgetDao?.ensureBudgetSeeded()
    }

    suspend fun addDemoMoney(
        targetVpa: String,
        amountInPaise: Long,
        note: String,
        idempotencyKey: String
    ): WalletTransactionEntity {
        return walletDao.addDemoMoney(
            targetVpa = targetVpa,
            amountInPaise = amountInPaise,
            note = note,
            idempotencyKey = idempotencyKey
        )
    }

    suspend fun transferBetweenAccounts(
        senderVpa: String,
        receiverName: String,
        receiverVpa: String,
        amountInPaise: Long,
        note: String,
        idempotencyKey: String
    ): WalletTransactionEntity {
        return walletDao.executeTransfer(
            type = TransactionType.DEBIT,
            txPrefix = "FG-WAL",
            title = "Demo Transfer to $receiverName",
            senderVpa = senderVpa,
            counterpartyName = receiverName,
            counterpartyVpa = receiverVpa,
            amountInPaise = amountInPaise,
            note = note.ifBlank { "Fictional account-to-account transfer" },
            idempotencyKey = idempotencyKey
        )
    }

    suspend fun simulateUpiTransfer(
        sender: DemoProfile,
        receiverName: String,
        receiverVpa: String,
        amountInPaise: Long,
        note: String,
        outcomeMode: SimulationOutcome,
        idempotencyKey: String
    ): WalletTransactionEntity {
        val (riskScore, riskWarning) = OfflineHeuristicAnalyzer.quickAssessUpiTransfer(
            vpa = receiverVpa,
            amountInPaise = amountInPaise,
            note = note
        )

        val failureReason = when (outcomeMode) {
            SimulationOutcome.FORCE_NETWORK_FAILURE ->
                "SIMULATED NETWORK TIMEOUT: Demo UPI switch did not respond in time. No demo funds deducted."
            SimulationOutcome.FORCE_BANK_DECLINE ->
                "SIMULATED BANK DECLINE (ERR-SIM-U30): Fictional issuer bank declined this sandbox request."
            SimulationOutcome.FORCE_SUCCESS -> null
            SimulationOutcome.AUTO -> {
                if (riskScore >= 75) {
                    "BLOCKED BY FINGUARD SHIELD: ${riskWarning ?: "High-risk VPA or scam pattern detected."}"
                } else {
                    null
                }
            }
        }

        return walletDao.executeTransfer(
            type = if (failureReason == null) TransactionType.UPI_SIM_SUCCESS else TransactionType.UPI_SIM_FAILED,
            txPrefix = "UPI-SIM",
            title = "Simulated UPI to $receiverName",
            senderVpa = sender.vpa,
            counterpartyName = receiverName,
            counterpartyVpa = receiverVpa,
            amountInPaise = amountInPaise,
            note = note.ifBlank { "Simulated UPI Sandbox Payment" },
            idempotencyKey = idempotencyKey,
            forceFailureReason = failureReason
        )
    }

    suspend fun resetDemoWallet() {
        walletDao.resetDemoWallet()
    }

    suspend fun runFraudScan(
        scanType: ScanType,
        input: String,
        useGeminiAi: Boolean
    ): FraudAnalysisResult {
        val result = if (!useGeminiAi) {
            OfflineHeuristicAnalyzer.analyze(scanType, input)
        } else if (!isNetworkAvailable()) {
            OfflineHeuristicAnalyzer.analyze(scanType, input).copy(
                fallbackNotice = "No internet connection detected. Automatically analyzed using the Offline Local Rules Shield."
            )
        } else {
            try {
                aiService.analyze(scanType, input)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini AI unavailable; falling back to OfflineHeuristicAnalyzer", e)
                val reason = e.localizedMessage?.take(140) ?: "Service unavailable"
                OfflineHeuristicAnalyzer.analyze(scanType, input).copy(
                    fallbackNotice = "Gemini AI unavailable ($reason). Automatically analyzed using the Offline Local Rules Shield."
                )
            }
        }
        persistScanResult(result)
        return result
    }

    private suspend fun persistScanResult(result: FraudAnalysisResult) {
        scanHistoryDao.insertScan(
            ScanRecordEntity(
                scanType = result.scanType.name,
                inputContent = result.inputContent,
                riskScore = result.riskScore,
                riskLevel = result.riskLevel.name,
                headline = result.headline,
                explanation = result.explanation,
                indicatorsJoined = result.indicators.joinToString("\n"),
                recommendation = result.recommendation,
                engineUsed = result.engineUsed.name
            )
        )
    }

    suspend fun deleteScan(id: Long) {
        scanHistoryDao.deleteScanById(id)
    }

    suspend fun clearScanHistory() {
        scanHistoryDao.clearAllScans()
    }

    // --- Smart Personal Budget Planner ---

    suspend fun saveBudgetProfile(incomeInPaise: Long, budgetInPaise: Long) {
        val dao = budgetDao ?: return
        require(incomeInPaise >= 0L) { "Monthly income cannot be negative." }
        require(budgetInPaise >= 0L) { "Monthly budget cannot be negative." }
        dao.upsertBudgetProfile(
            BudgetProfileEntity(
                id = 1,
                monthlyIncomeInPaise = incomeInPaise,
                monthlyBudgetInPaise = budgetInPaise,
                isSampleData = false,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveExpense(
        expenseId: Long? = null,
        title: String,
        amountInPaise: Long,
        category: ExpenseCategory,
        note: String,
        expenseDateMillis: Long
    ) {
        val dao = budgetDao ?: return
        require(title.trim().isNotEmpty()) { "Expense title is required." }
        require(amountInPaise > 0L) { "Expense amount must be greater than ₹0." }
        val (cleanNote, _) = OfflineHeuristicAnalyzer.sanitizeSensitiveData(note.trim())
        dao.insertExpense(
            BudgetExpenseEntity(
                id = expenseId ?: 0L,
                title = title.trim(),
                amountInPaise = amountInPaise,
                category = category.name,
                note = cleanNote,
                expenseDateMillis = expenseDateMillis,
                isSampleData = false,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteExpense(id: Long) {
        budgetDao?.deleteExpenseById(id)
    }

    suspend fun createSavingsGoal(
        title: String,
        targetInPaise: Long,
        initialSavedInPaise: Long,
        deadlineEpochMillis: Long
    ) {
        val dao = budgetDao ?: return
        require(title.trim().isNotEmpty()) { "Goal name is required." }
        require(targetInPaise > 0L) { "Target amount must be greater than ₹0." }
        require(initialSavedInPaise >= 0L) { "Saved amount cannot be negative." }
        dao.upsertSavingsGoal(
            SavingsGoalEntity(
                title = title.trim(),
                targetAmountInPaise = targetInPaise,
                savedAmountInPaise = initialSavedInPaise,
                deadlineEpochMillis = deadlineEpochMillis,
                isSampleData = false,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun contributeToGoal(goalId: Long, amountInPaise: Long) {
        budgetDao?.contributeToSavingsGoal(goalId, amountInPaise)
    }

    suspend fun deleteSavingsGoal(id: Long) {
        budgetDao?.deleteSavingsGoalById(id)
    }

    suspend fun resetBudgetToBlank() {
        budgetDao?.clearAllBudgetData()
    }

    suspend fun restoreSampleBudgetData() {
        budgetDao?.restoreSampleBudgetData()
    }

    suspend fun generateBudgetAdvice(
        metrics: BudgetSnapshotMetrics,
        goals: List<SavingsGoalEntity>,
        useGeminiAi: Boolean
    ): BudgetAdviceResult {
        if (!useGeminiAi) {
            return OfflineBudgetAdvisor.generateAdvice(metrics, goals)
        }
        if (!isNetworkAvailable()) {
            return OfflineBudgetAdvisor.generateAdvice(
                metrics = metrics,
                goals = goals,
                fallbackNotice = "No internet connection detected. Automatically generated rule-based offline budgeting advice."
            )
        }
        return try {
            budgetAiService.advise(metrics, goals)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini budget advice unavailable; falling back to offline advisor", e)
            val reason = e.localizedMessage?.take(120) ?: "Service unavailable"
            OfflineBudgetAdvisor.generateAdvice(
                metrics = metrics,
                goals = goals,
                fallbackNotice = "Gemini AI unavailable ($reason). Automatically generated rule-based offline budgeting advice."
            )
        }
    }
}
