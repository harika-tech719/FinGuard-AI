package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FinGuardRepository
import com.example.data.local.BudgetExpenseEntity
import com.example.data.local.BudgetProfileEntity
import com.example.data.local.FinGuardDatabase
import com.example.data.local.SavingsGoalEntity
import com.example.data.local.ScanRecordEntity
import com.example.data.local.WalletAccountEntity
import com.example.data.local.WalletTransactionEntity
import com.example.data.model.BudgetAdviceResult
import com.example.data.model.BudgetAlertLevel
import com.example.data.model.BudgetCalculator
import com.example.data.model.BudgetReportPeriod
import com.example.data.model.BudgetSnapshotMetrics
import com.example.data.model.BudgetStatusTier
import com.example.data.model.DemoCatalog
import com.example.data.model.DemoProfile
import com.example.data.model.EducationCategory
import com.example.data.model.ExpenseCategory
import com.example.data.model.FinGuardFormatters
import com.example.data.model.FraudAnalysisResult
import com.example.data.model.INITIAL_DEMO_BALANCE_PAISE
import com.example.data.model.MAX_SINGLE_TOPUP_PAISE
import com.example.data.model.RiskLevel
import com.example.data.model.ScanType
import com.example.data.model.SimulationOutcome
import com.example.fraud.OfflineHeuristicAnalyzer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class WalletFilter(val label: String) {
    ALL("All"),
    CREDITS("Credits"),
    DEBITS("Debits"),
    FAILED("Failed")
}

data class HomeSummaryUiState(
    val balanceInPaise: Long = INITIAL_DEMO_BALANCE_PAISE,
    val ownerName: String = "Aarav Sharma (Primary)",
    val vpa: String = "aarav.demo@finguard",
    val transactionCount: Int = 0,
    val totalScans: Int = 0,
    val threatsFlagged: Int = 0,
    val securityScore: Int = 96,
    val recentTransactions: List<WalletTransactionEntity> = emptyList(),
    val recentScans: List<ScanRecordEntity> = emptyList(),
    val monthlyIncomeInPaise: Long = 0L,
    val monthlyBudgetInPaise: Long = 0L,
    val totalMonthlyExpensesInPaise: Long = 0L,
    val remainingBudgetInPaise: Long = 0L,
    val budgetOverspentInPaise: Long = 0L,
    val netSavingsInPaise: Long = 0L,
    val savingsRatePercent: Int = 0,
    val budgetUsedPercent: Int = 0,
    val budgetAlertLevel: BudgetAlertLevel = BudgetAlertLevel.NORMAL,
    val budgetStatusTier: BudgetStatusTier = BudgetStatusTier.HEALTHY,
    val topBudgetCategoryLabel: String? = null,
    val savingsGoalCount: Int = 0
)

data class WalletUiState(
    val accounts: List<WalletAccountEntity> = emptyList(),
    val selectedAccount: WalletAccountEntity? = null,
    val balanceInPaise: Long = INITIAL_DEMO_BALANCE_PAISE,
    val ownerName: String = "Aarav Sharma (Primary)",
    val vpa: String = "aarav.demo@finguard",
    val transactions: List<WalletTransactionEntity> = emptyList(),
    val selectedFilter: WalletFilter = WalletFilter.ALL,
    val selectedTransaction: WalletTransactionEntity? = null,
    val latestReceipt: WalletTransactionEntity? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
)

data class UpiSimUiState(
    val balanceInPaise: Long = INITIAL_DEMO_BALANCE_PAISE,
    val accountBalancesByVpa: Map<String, Long> = emptyMap(),
    val selectedSender: DemoProfile = DemoCatalog.senderProfiles.first(),
    val selectedReceiver: DemoProfile? = DemoCatalog.receiverProfiles.first(),
    val customReceiverName: String = "",
    val customReceiverVpa: String = "",
    val useCustomReceiver: Boolean = false,
    val amountInput: String = "500",
    val noteInput: String = "Lunch split",
    val outcomeMode: SimulationOutcome = SimulationOutcome.AUTO,
    val preCheckWarning: String? = null,
    val latestReceipt: WalletTransactionEntity? = null,
    val errorMessage: String? = null,
    val isSimulating: Boolean = false
)

sealed interface FraudScanUiState {
    data object Idle : FraudScanUiState
    data object Scanning : FraudScanUiState
    data class Success(val result: FraudAnalysisResult) : FraudScanUiState
    data class Error(val message: String, val allowOfflineFallback: Boolean = true) : FraudScanUiState
}

data class FraudDetectorScreenState(
    val selectedScanType: ScanType = ScanType.SMS,
    val inputText: String = "",
    val useGeminiAi: Boolean = true,
    val scanState: FraudScanUiState = FraudScanUiState.Idle
)

data class ScanHistoryScreenState(
    val allScans: List<ScanRecordEntity> = emptyList(),
    val filterType: ScanType? = null,
    val filterRisk: RiskLevel? = null,
    val selectedRecord: ScanRecordEntity? = null
)

data class EducationScreenState(
    val selectedCategory: EducationCategory? = null,
    val completedLessonIds: Set<String> = emptySet(),
    val selectedQuizAnswers: Map<String, Int> = emptyMap()
)

data class BudgetPlannerUiState(
    val profile: BudgetProfileEntity? = null,
    val expenses: List<BudgetExpenseEntity> = emptyList(),
    val filteredExpenses: List<BudgetExpenseEntity> = emptyList(),
    val goals: List<SavingsGoalEntity> = emptyList(),
    val metrics: BudgetSnapshotMetrics = BudgetCalculator.computeMetrics(null, emptyList(), emptyList()),
    val selectedCategoryFilter: ExpenseCategory? = null,
    val selectedReportPeriod: BudgetReportPeriod = BudgetReportPeriod.WEEKLY,
    val useGeminiCoach: Boolean = true,
    val isLoadingAdvice: Boolean = false,
    val advice: BudgetAdviceResult? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class FinGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinGuardRepository

    init {
        val db = FinGuardDatabase.getInstance(application)
        repository = FinGuardRepository(
            walletDao = db.walletDao(),
            scanHistoryDao = db.scanHistoryDao(),
            isNetworkAvailable = { hasInternetConnection(application) },
            budgetDao = db.budgetDao()
        )
        viewModelScope.launch {
            repository.initializeSandbox()
        }
    }

    private fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private val allAccountsFlow: StateFlow<List<WalletAccountEntity>> = repository.allWalletAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val primaryAccountFlow: StateFlow<WalletAccountEntity?> = repository.primaryWalletAccount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val transactionsFlow: StateFlow<List<WalletTransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val scansFlow: StateFlow<List<ScanRecordEntity>> = repository.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val budgetProfileFlow: StateFlow<BudgetProfileEntity?> = repository.budgetProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val budgetExpensesFlow: StateFlow<List<BudgetExpenseEntity>> = repository.budgetExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val savingsGoalsFlow: StateFlow<List<SavingsGoalEntity>> = repository.savingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeState: StateFlow<HomeSummaryUiState> = combine(
        primaryAccountFlow,
        transactionsFlow,
        scansFlow,
        combine(budgetProfileFlow, budgetExpensesFlow, savingsGoalsFlow) { p, e, g -> Triple(p, e, g) }
    ) { account, txs, scans, budgetTriple ->
        val (profile, expenses, goals) = budgetTriple
        val budgetMetrics = BudgetCalculator.computeMetrics(profile, expenses, goals)
        val threats = scans.count {
            it.riskLevel == RiskLevel.HIGH.name ||
                it.riskLevel == "HIGH_RISK" ||
                it.riskLevel == RiskLevel.CRITICAL.name
        }
        val latestThreatPenalty = if (scans.firstOrNull()?.riskScore ?: 0 >= 75) 6 else 0
        HomeSummaryUiState(
            balanceInPaise = account?.balanceInPaise ?: INITIAL_DEMO_BALANCE_PAISE,
            ownerName = account?.ownerName ?: "Aarav Sharma (Primary)",
            vpa = account?.vpa ?: "aarav.demo@finguard",
            transactionCount = txs.size,
            totalScans = scans.size,
            threatsFlagged = threats,
            securityScore = (98 - latestThreatPenalty).coerceIn(70, 99),
            recentTransactions = txs.take(3),
            recentScans = scans.take(2),
            monthlyIncomeInPaise = budgetMetrics.monthlyIncomeInPaise,
            monthlyBudgetInPaise = budgetMetrics.monthlyBudgetInPaise,
            totalMonthlyExpensesInPaise = budgetMetrics.totalExpensesInPaise,
            remainingBudgetInPaise = budgetMetrics.remainingBudgetInPaise,
            budgetOverspentInPaise = budgetMetrics.overspentInPaise,
            netSavingsInPaise = budgetMetrics.netSavingsInPaise,
            savingsRatePercent = budgetMetrics.savingsRatePercent,
            budgetUsedPercent = budgetMetrics.budgetUsedPercent,
            budgetAlertLevel = budgetMetrics.alertLevel,
            budgetStatusTier = budgetMetrics.statusTier,
            topBudgetCategoryLabel = budgetMetrics.categoryBreakdown.firstOrNull()?.category?.label,
            savingsGoalCount = goals.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeSummaryUiState())

    private val _selectedAccountVpa = MutableStateFlow("aarav.demo@finguard")
    private val _walletFilter = MutableStateFlow(WalletFilter.ALL)
    private val _selectedTx = MutableStateFlow<WalletTransactionEntity?>(null)
    private val _latestWalletReceipt = MutableStateFlow<WalletTransactionEntity?>(null)
    private val _walletFeedback = MutableStateFlow<Pair<String?, String?>>(null to null)
    private val _walletProcessing = MutableStateFlow(false)

    val walletState: StateFlow<WalletUiState> = combine(
        allAccountsFlow,
        transactionsFlow,
        _selectedAccountVpa,
        _walletFilter,
        _selectedTx,
        _latestWalletReceipt,
        _walletFeedback,
        _walletProcessing
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val accounts = values[0] as List<WalletAccountEntity>
        @Suppress("UNCHECKED_CAST")
        val txs = values[1] as List<WalletTransactionEntity>
        val selectedVpa = values[2] as String
        val filter = values[3] as WalletFilter
        val selectedTx = values[4] as WalletTransactionEntity?
        val latestReceipt = values[5] as WalletTransactionEntity?
        @Suppress("UNCHECKED_CAST")
        val feedbackPair = values[6] as Pair<String?, String?>
        val processing = values[7] as Boolean

        val activeAccount = accounts.firstOrNull { it.vpa == selectedVpa }
            ?: accounts.firstOrNull { it.isPrimary }
            ?: accounts.firstOrNull()

        val filtered = when (filter) {
            WalletFilter.ALL -> txs
            WalletFilter.CREDITS -> txs.filter { it.type == "CREDIT" }
            WalletFilter.DEBITS -> txs.filter { it.type == "DEBIT" || it.type == "UPI_SIM_SUCCESS" }
            WalletFilter.FAILED -> txs.filter { it.status == "SIMULATED_FAILED" }
        }

        WalletUiState(
            accounts = accounts,
            selectedAccount = activeAccount,
            balanceInPaise = activeAccount?.balanceInPaise ?: INITIAL_DEMO_BALANCE_PAISE,
            ownerName = activeAccount?.ownerName ?: "Aarav Sharma (Primary)",
            vpa = activeAccount?.vpa ?: "aarav.demo@finguard",
            transactions = filtered,
            selectedFilter = filter,
            selectedTransaction = selectedTx,
            latestReceipt = latestReceipt,
            feedbackMessage = feedbackPair.first,
            errorMessage = feedbackPair.second,
            isProcessing = processing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WalletUiState())

    private val _upiSimState = MutableStateFlow(UpiSimUiState())
    val upiSimState: StateFlow<UpiSimUiState> = combine(
        allAccountsFlow,
        _upiSimState
    ) { accounts, state ->
        val balanceMap = accounts.associate { it.vpa to it.balanceInPaise }
        val senderBalance = balanceMap[state.selectedSender.vpa] ?: INITIAL_DEMO_BALANCE_PAISE
        state.copy(
            balanceInPaise = senderBalance,
            accountBalancesByVpa = balanceMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UpiSimUiState())

    private val _detectorState = MutableStateFlow(FraudDetectorScreenState())
    val detectorState: StateFlow<FraudDetectorScreenState> = _detectorState.asStateFlow()

    private val _historyFilterType = MutableStateFlow<ScanType?>(null)
    private val _historyFilterRisk = MutableStateFlow<RiskLevel?>(null)
    private val _selectedHistoryRecord = MutableStateFlow<ScanRecordEntity?>(null)

    val scanHistoryState: StateFlow<ScanHistoryScreenState> = combine(
        scansFlow,
        _historyFilterType,
        _historyFilterRisk,
        _selectedHistoryRecord
    ) { scans, typeFilter, riskFilter, selected ->
        val filtered = scans.filter { item ->
            val normalizedRisk = RiskLevel.fromRaw(item.riskLevel, item.riskScore)
            (typeFilter == null || item.scanType == typeFilter.name) &&
                (riskFilter == null || normalizedRisk == riskFilter)
        }
        ScanHistoryScreenState(
            allScans = filtered,
            filterType = typeFilter,
            filterRisk = riskFilter,
            selectedRecord = selected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanHistoryScreenState())

    private val _educationState = MutableStateFlow(EducationScreenState())
    val educationState: StateFlow<EducationScreenState> = _educationState.asStateFlow()

    private val _budgetCategoryFilter = MutableStateFlow<ExpenseCategory?>(null)
    private val _budgetReportPeriod = MutableStateFlow(BudgetReportPeriod.WEEKLY)
    private val _budgetUseGemini = MutableStateFlow(true)
    private val _budgetAdviceLoading = MutableStateFlow(false)
    private val _budgetAdvice = MutableStateFlow<BudgetAdviceResult?>(null)
    private val _budgetMessages = MutableStateFlow<Pair<String?, String?>>(null to null)

    val budgetState: StateFlow<BudgetPlannerUiState> = combine(
        budgetProfileFlow,
        budgetExpensesFlow,
        savingsGoalsFlow,
        _budgetCategoryFilter,
        _budgetReportPeriod,
        _budgetUseGemini,
        _budgetAdviceLoading,
        _budgetAdvice,
        _budgetMessages
    ) { values ->
        val profile = values[0] as BudgetProfileEntity?
        @Suppress("UNCHECKED_CAST")
        val expenses = values[1] as List<BudgetExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = values[2] as List<SavingsGoalEntity>
        val categoryFilter = values[3] as ExpenseCategory?
        val reportPeriod = values[4] as BudgetReportPeriod
        val useGemini = values[5] as Boolean
        val loadingAdvice = values[6] as Boolean
        val advice = values[7] as BudgetAdviceResult?
        @Suppress("UNCHECKED_CAST")
        val messages = values[8] as Pair<String?, String?>

        val metrics = BudgetCalculator.computeMetrics(profile, expenses, goals)
        val filtered = if (categoryFilter == null) {
            expenses
        } else {
            expenses.filter { ExpenseCategory.fromRaw(it.category) == categoryFilter }
        }

        BudgetPlannerUiState(
            profile = profile,
            expenses = expenses,
            filteredExpenses = filtered,
            goals = goals,
            metrics = metrics,
            selectedCategoryFilter = categoryFilter,
            selectedReportPeriod = reportPeriod,
            useGeminiCoach = useGemini,
            isLoadingAdvice = loadingAdvice,
            advice = advice,
            feedbackMessage = messages.first,
            errorMessage = messages.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetPlannerUiState())

    // --- Wallet Actions ---

    fun selectWalletAccount(vpa: String) {
        _selectedAccountVpa.value = vpa
    }

    fun setWalletFilter(filter: WalletFilter) {
        _walletFilter.value = filter
    }

    fun selectTransaction(tx: WalletTransactionEntity?) {
        _selectedTx.value = tx
    }

    fun dismissWalletReceipt() {
        _latestWalletReceipt.value = null
    }

    fun clearWalletMessages() {
        _walletFeedback.value = null to null
    }

    fun addDemoMoney(
        targetVpa: String,
        amountRaw: String,
        note: String,
        requestKey: String = UUID.randomUUID().toString()
    ) {
        if (_walletProcessing.value) return
        val paise = FinGuardFormatters.parsePaiseInput(amountRaw)
        if (paise == null || paise <= 0L) {
            _walletFeedback.value = null to "Enter a valid positive demo amount greater than ₹0."
            return
        }
        if (paise > MAX_SINGLE_TOPUP_PAISE) {
            _walletFeedback.value = null to "Single demo top-up is capped at ₹1,00,000.00."
            return
        }
        viewModelScope.launch {
            _walletProcessing.value = true
            try {
                val tx = repository.addDemoMoney(
                    targetVpa = targetVpa,
                    amountInPaise = paise,
                    note = note,
                    idempotencyKey = requestKey
                )
                _latestWalletReceipt.value = tx
                _walletFeedback.value = "Added ${FinGuardFormatters.formatPaise(paise)} (Simulated Ref: ${tx.txRef})" to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _walletFeedback.value = null to (e.localizedMessage ?: "Could not add demo funds.")
            } finally {
                _walletProcessing.value = false
            }
        }
    }

    fun transferBetweenAccounts(
        sender: WalletAccountEntity,
        receiver: WalletAccountEntity,
        amountRaw: String,
        note: String,
        requestKey: String = UUID.randomUUID().toString()
    ) {
        if (_walletProcessing.value) return
        val paise = FinGuardFormatters.parsePaiseInput(amountRaw)
        if (paise == null || paise <= 0L) {
            _walletFeedback.value = null to "Enter a valid positive transfer amount."
            return
        }
        if (sender.vpa.equals(receiver.vpa, ignoreCase = true)) {
            _walletFeedback.value = null to "Sender and receiver fictional accounts must be different."
            return
        }
        viewModelScope.launch {
            _walletProcessing.value = true
            try {
                val tx = repository.transferBetweenAccounts(
                    senderVpa = sender.vpa,
                    receiverName = receiver.ownerName,
                    receiverVpa = receiver.vpa,
                    amountInPaise = paise,
                    note = note,
                    idempotencyKey = requestKey
                )
                _latestWalletReceipt.value = tx
                _walletFeedback.value = "Simulated transfer of ${FinGuardFormatters.formatPaise(paise)} from ${sender.ownerName} to ${receiver.ownerName} complete." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _walletFeedback.value = null to (e.localizedMessage ?: "Transfer failed.")
            } finally {
                _walletProcessing.value = false
            }
        }
    }

    fun resetDemoWallet() {
        if (_walletProcessing.value) return
        viewModelScope.launch {
            _walletProcessing.value = true
            try {
                repository.resetDemoWallet()
                _selectedAccountVpa.value = "aarav.demo@finguard"
                _selectedTx.value = null
                _latestWalletReceipt.value = null
                _walletFeedback.value = "All fictional accounts reset (Primary: ₹10,000.00 Simulated)." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _walletFeedback.value = null to (e.localizedMessage ?: "Reset failed.")
            } finally {
                _walletProcessing.value = false
            }
        }
    }

    // --- UPI Simulation Actions ---

    fun selectUpiSender(profile: DemoProfile) {
        _upiSimState.update { it.copy(selectedSender = profile, errorMessage = null) }
    }

    fun selectUpiReceiver(profile: DemoProfile) {
        _upiSimState.update {
            it.copy(
                selectedReceiver = profile,
                useCustomReceiver = false,
                errorMessage = null
            )
        }
        refreshUpiPreCheck()
    }

    fun setCustomReceiverMode(enabled: Boolean) {
        _upiSimState.update { it.copy(useCustomReceiver = enabled, errorMessage = null) }
        refreshUpiPreCheck()
    }

    fun updateCustomReceiverName(name: String) {
        _upiSimState.update { it.copy(customReceiverName = name, errorMessage = null) }
    }

    fun updateCustomReceiverVpa(vpa: String) {
        _upiSimState.update { it.copy(customReceiverVpa = vpa, errorMessage = null) }
        refreshUpiPreCheck()
    }

    fun updateUpiAmount(amount: String) {
        _upiSimState.update { it.copy(amountInput = amount, errorMessage = null) }
        refreshUpiPreCheck()
    }

    fun updateUpiNote(note: String) {
        _upiSimState.update { it.copy(noteInput = note, errorMessage = null) }
        refreshUpiPreCheck()
    }

    fun selectSimulationOutcome(mode: SimulationOutcome) {
        _upiSimState.update { it.copy(outcomeMode = mode, errorMessage = null) }
    }

    fun dismissUpiReceipt() {
        _upiSimState.update { it.copy(latestReceipt = null) }
    }

    private fun refreshUpiPreCheck() {
        val state = _upiSimState.value
        val vpa = if (state.useCustomReceiver) state.customReceiverVpa.trim() else state.selectedReceiver?.vpa.orEmpty()
        val paise = FinGuardFormatters.parsePaiseInput(state.amountInput) ?: 0L
        if (vpa.isBlank() || paise <= 0L) {
            _upiSimState.update { it.copy(preCheckWarning = null) }
            return
        }
        val (_, warning) = OfflineHeuristicAnalyzer.quickAssessUpiTransfer(vpa, paise, state.noteInput)
        _upiSimState.update { it.copy(preCheckWarning = warning) }
    }

    fun executeSimulatedUpiPayment(requestKey: String = UUID.randomUUID().toString()) {
        val state = _upiSimState.value
        if (state.isSimulating) return

        val paise = FinGuardFormatters.parsePaiseInput(state.amountInput)
        if (paise == null || paise <= 0L) {
            _upiSimState.update { it.copy(errorMessage = "Enter a valid positive amount greater than ₹0.") }
            return
        }

        val receiverName: String
        val receiverVpa: String
        if (state.useCustomReceiver) {
            receiverName = state.customReceiverName.trim().ifBlank { "Fictional Recipient" }
            receiverVpa = state.customReceiverVpa.trim()
            val vpaRegex = Regex("""^[a-zA-Z0-9.\-_]{2,}@[a-zA-Z0-9.\-]{2,}$""")
            if (!vpaRegex.matches(receiverVpa)) {
                _upiSimState.update { it.copy(errorMessage = "Enter a valid fictional VPA format (e.g., alex.demo@okdemo).") }
                return
            }
        } else {
            val profile = state.selectedReceiver
            if (profile == null) {
                _upiSimState.update { it.copy(errorMessage = "Select a fictional receiver profile.") }
                return
            }
            receiverName = profile.displayName
            receiverVpa = profile.vpa
        }

        if (receiverVpa.equals(state.selectedSender.vpa, ignoreCase = true)) {
            _upiSimState.update { it.copy(errorMessage = "Sender and receiver VPA cannot be identical.") }
            return
        }

        viewModelScope.launch {
            _upiSimState.update { it.copy(isSimulating = true, errorMessage = null) }
            try {
                val receipt = repository.simulateUpiTransfer(
                    sender = state.selectedSender,
                    receiverName = receiverName,
                    receiverVpa = receiverVpa,
                    amountInPaise = paise,
                    note = state.noteInput,
                    outcomeMode = state.outcomeMode,
                    idempotencyKey = requestKey
                )
                _upiSimState.update { it.copy(latestReceipt = receipt, isSimulating = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _upiSimState.update {
                    it.copy(
                        isSimulating = false,
                        errorMessage = e.localizedMessage ?: "Simulated UPI transfer could not be completed."
                    )
                }
            }
        }
    }

    // --- Fraud Detector Actions ---

    fun selectScanType(type: ScanType) {
        _detectorState.update {
            it.copy(selectedScanType = type, scanState = FraudScanUiState.Idle)
        }
    }

    fun updateScanInput(text: String) {
        _detectorState.update { state ->
            state.copy(
                inputText = text,
                scanState = if (state.scanState is FraudScanUiState.Error) FraudScanUiState.Idle else state.scanState
            )
        }
    }

    fun toggleGeminiEngine(enabled: Boolean) {
        _detectorState.update { it.copy(useGeminiAi = enabled) }
    }

    fun loadSamplePrompt(scanType: ScanType, payload: String) {
        _detectorState.update {
            it.copy(
                selectedScanType = scanType,
                inputText = payload,
                scanState = FraudScanUiState.Idle
            )
        }
    }

    fun runFraudScan(forceOffline: Boolean = false) {
        val current = _detectorState.value
        val trimmed = current.inputText.trim()
        if (trimmed.length < 4) {
            _detectorState.update {
                it.copy(
                    scanState = FraudScanUiState.Error(
                        message = "Please enter an SMS message, URL, or UPI request (at least 4 characters) to analyze.",
                        allowOfflineFallback = false
                    )
                )
            }
            return
        }

        val useAi = current.useGeminiAi && !forceOffline
        viewModelScope.launch {
            _detectorState.update { it.copy(scanState = FraudScanUiState.Scanning) }
            try {
                val result = repository.runFraudScan(
                    scanType = current.selectedScanType,
                    input = trimmed,
                    useGeminiAi = useAi
                )
                _detectorState.update { it.copy(scanState = FraudScanUiState.Success(result)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _detectorState.update {
                    it.copy(
                        scanState = FraudScanUiState.Error(
                            message = e.localizedMessage ?: "AI scan failed.",
                            allowOfflineFallback = useAi
                        )
                    )
                }
            }
        }
    }

    // --- Scan History Actions ---

    fun setHistoryTypeFilter(type: ScanType?) {
        _historyFilterType.value = type
    }

    fun setHistoryRiskFilter(risk: RiskLevel?) {
        _historyFilterRisk.value = risk
    }

    fun selectHistoryRecord(record: ScanRecordEntity?) {
        _selectedHistoryRecord.value = record
    }

    fun deleteScanRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteScan(id)
            if (_selectedHistoryRecord.value?.id == id) {
                _selectedHistoryRecord.value = null
            }
        }
    }

    fun clearAllScanHistory() {
        viewModelScope.launch {
            repository.clearScanHistory()
            _selectedHistoryRecord.value = null
        }
    }

    // --- Education Actions ---

    fun selectEducationCategory(category: EducationCategory?) {
        _educationState.update { it.copy(selectedCategory = category) }
    }

    fun toggleLessonCompleted(lessonId: String) {
        _educationState.update { state ->
            val updated = if (lessonId in state.completedLessonIds) {
                state.completedLessonIds - lessonId
            } else {
                state.completedLessonIds + lessonId
            }
            state.copy(completedLessonIds = updated)
        }
    }

    fun selectQuizAnswer(questionId: String, optionIndex: Int) {
        _educationState.update { state ->
            state.copy(selectedQuizAnswers = state.selectedQuizAnswers + (questionId to optionIndex))
        }
    }

    // --- Smart Personal Budget Planner Actions ---

    fun clearBudgetMessages() {
        _budgetMessages.value = null to null
    }

    fun selectBudgetCategoryFilter(category: ExpenseCategory?) {
        _budgetCategoryFilter.update { current ->
            if (current == category) null else category
        }
    }

    fun selectReportPeriod(period: BudgetReportPeriod) {
        _budgetReportPeriod.value = period
    }

    fun toggleBudgetGemini(enabled: Boolean) {
        _budgetUseGemini.value = enabled
    }

    fun saveBudgetSettings(incomeRaw: String, budgetRaw: String) {
        val incomePaise = FinGuardFormatters.parseNonNegativePaiseInput(incomeRaw)
        val budgetPaise = FinGuardFormatters.parseNonNegativePaiseInput(budgetRaw)
        if (incomePaise == null) {
            _budgetMessages.value = null to "Enter a valid monthly income amount (e.g., 45000)."
            return
        }
        if (budgetPaise == null || budgetPaise <= 0L) {
            _budgetMessages.value = null to "Enter a monthly budget limit greater than ₹0."
            return
        }
        viewModelScope.launch {
            try {
                repository.saveBudgetProfile(incomePaise, budgetPaise)
                _budgetMessages.value = "Updated monthly income & budget target." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Failed to save budget settings.")
            }
        }
    }

    fun saveExpense(
        expenseId: Long? = null,
        title: String,
        amountRaw: String,
        category: ExpenseCategory,
        note: String,
        daysAgoOffset: Int,
        preserveExistingDateMillis: Long? = null
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            _budgetMessages.value = null to "Enter an expense title."
            return
        }
        val paise = FinGuardFormatters.parsePaiseInput(amountRaw)
        if (paise == null || paise <= 0L) {
            _budgetMessages.value = null to "Enter a valid expense amount greater than ₹0."
            return
        }
        val dateMillis = if (daysAgoOffset < 0 && preserveExistingDateMillis != null) {
            preserveExistingDateMillis
        } else {
            val todayNoon = BudgetCalculator.startOfDayMillis(System.currentTimeMillis()) + 43_200_000L
            todayNoon - (daysAgoOffset.coerceAtLeast(0).toLong() * 86_400_000L)
        }
        viewModelScope.launch {
            try {
                repository.saveExpense(
                    expenseId = expenseId,
                    title = trimmedTitle,
                    amountInPaise = paise,
                    category = category,
                    note = note,
                    expenseDateMillis = dateMillis
                )
                val actionLabel = if (expenseId == null) "Added" else "Updated"
                _budgetMessages.value = "$actionLabel ${category.label} expense (${FinGuardFormatters.formatPaise(paise)})." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Could not save expense.")
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(id)
                _budgetMessages.value = "Expense removed." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Could not delete expense.")
            }
        }
    }

    fun createSavingsGoal(
        title: String,
        targetRaw: String,
        initialSavedRaw: String,
        deadlineDays: Int
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            _budgetMessages.value = null to "Enter a savings goal name."
            return
        }
        val targetPaise = FinGuardFormatters.parsePaiseInput(targetRaw)
        if (targetPaise == null || targetPaise <= 0L) {
            _budgetMessages.value = null to "Enter a target amount greater than ₹0."
            return
        }
        val savedPaise = FinGuardFormatters.parseNonNegativePaiseInput(initialSavedRaw.ifBlank { "0" })
        if (savedPaise == null) {
            _budgetMessages.value = null to "Enter a valid initial saved amount (0 or higher)."
            return
        }
        val safeDays = deadlineDays.coerceIn(7, 3650)
        val deadlineMillis = System.currentTimeMillis() + safeDays.toLong() * 86_400_000L

        viewModelScope.launch {
            try {
                repository.createSavingsGoal(
                    title = trimmedTitle,
                    targetInPaise = targetPaise,
                    initialSavedInPaise = savedPaise,
                    deadlineEpochMillis = deadlineMillis
                )
                _budgetMessages.value = "Created savings goal '$trimmedTitle'." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Could not create savings goal.")
            }
        }
    }

    fun contributeToGoal(goalId: Long, amountRaw: String) {
        val paise = FinGuardFormatters.parsePaiseInput(amountRaw)
        if (paise == null || paise <= 0L) {
            _budgetMessages.value = null to "Enter a contribution greater than ₹0."
            return
        }
        viewModelScope.launch {
            try {
                repository.contributeToGoal(goalId, paise)
                _budgetMessages.value = "Added ${FinGuardFormatters.formatPaise(paise)} to your savings goal." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Could not update savings goal.")
            }
        }
    }

    fun deleteSavingsGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteSavingsGoal(goalId)
                _budgetMessages.value = "Savings goal removed." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Could not delete savings goal.")
            }
        }
    }

    fun generateBudgetAdvice(forceOffline: Boolean = false) {
        if (_budgetAdviceLoading.value) return
        val currentState = budgetState.value
        val useAi = currentState.useGeminiCoach && !forceOffline
        viewModelScope.launch {
            _budgetAdviceLoading.value = true
            try {
                val result = repository.generateBudgetAdvice(
                    metrics = currentState.metrics,
                    goals = currentState.goals,
                    useGeminiAi = useAi
                )
                _budgetAdvice.value = result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Unable to generate budget advice.")
            } finally {
                _budgetAdviceLoading.value = false
            }
        }
    }

    fun resetBudgetToBlank() {
        viewModelScope.launch {
            try {
                repository.resetBudgetToBlank()
                _budgetCategoryFilter.value = null
                _budgetAdvice.value = null
                _budgetMessages.value = "Sample & personal budget entries cleared. Ready for your own numbers." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Failed to reset budget.")
            }
        }
    }

    fun restoreSampleBudgetData() {
        viewModelScope.launch {
            try {
                repository.restoreSampleBudgetData()
                _budgetCategoryFilter.value = null
                _budgetAdvice.value = null
                _budgetMessages.value = "Loaded demonstration budget dataset (₹45,000 income / ₹25,000 limit)." to null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _budgetMessages.value = null to (e.localizedMessage ?: "Failed to load sample budget.")
            }
        }
    }
}
