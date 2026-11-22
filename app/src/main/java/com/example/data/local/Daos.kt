package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.model.FinGuardFormatters
import com.example.data.model.INITIAL_DEMO_BALANCE_PAISE
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DuplicateTransactionException(message: String) : IllegalStateException(message)

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallet_account ORDER BY isPrimary DESC, ownerName ASC")
    fun observeAllAccounts(): Flow<List<WalletAccountEntity>>

    @Query("SELECT * FROM wallet_account WHERE isPrimary = 1 LIMIT 1")
    fun observePrimaryAccount(): Flow<WalletAccountEntity?>

    @Query("SELECT * FROM wallet_account WHERE vpa = :vpa LIMIT 1")
    suspend fun getAccountByVpa(vpa: String): WalletAccountEntity?

    @Query("SELECT * FROM wallet_account WHERE isPrimary = 1 LIMIT 1")
    suspend fun getPrimaryAccountOnce(): WalletAccountEntity?

    @Query("SELECT * FROM wallet_account ORDER BY isPrimary DESC, ownerName ASC")
    suspend fun getAllAccountsOnce(): List<WalletAccountEntity>

    @Query("SELECT * FROM wallet_transactions ORDER BY createdAt DESC, id DESC")
    fun observeTransactions(): Flow<List<WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun findByIdempotencyKey(key: String): WalletTransactionEntity?

    @Query(
        """
        SELECT * FROM wallet_transactions
        WHERE senderVpa = :senderVpa
          AND counterpartyVpa = :counterpartyVpa
          AND amountInPaise = :amountInPaise
          AND note = :note
          AND status = 'SIMULATED_SUCCESS'
          AND createdAt >= :sinceMillis
        LIMIT 1
        """
    )
    suspend fun findRecentDuplicateTransfer(
        senderVpa: String,
        counterpartyVpa: String,
        amountInPaise: Long,
        note: String,
        sinceMillis: Long
    ): WalletTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(accounts: List<WalletAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: WalletAccountEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(tx: WalletTransactionEntity): Long

    @Query("DELETE FROM wallet_transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM wallet_account")
    suspend fun clearAllAccounts()

    private fun defaultSeedAccounts(now: Long): List<WalletAccountEntity> = listOf(
        WalletAccountEntity(
            vpa = "aarav.demo@finguard",
            ownerName = "Aarav Sharma (Primary)",
            bankLabel = "FinGuard Sandbox Vault",
            initials = "AS",
            roleTag = "Primary Demo Wallet",
            balanceInPaise = INITIAL_DEMO_BALANCE_PAISE, // ₹10,000.00
            initialBalanceInPaise = INITIAL_DEMO_BALANCE_PAISE,
            isPrimary = true,
            lastUpdatedAt = now
        ),
        WalletAccountEntity(
            vpa = "meera.sim@okdemo",
            ownerName = "Meera Kapoor (Demo)",
            bankLabel = "FinGuard Joint Sandbox",
            initials = "MK",
            roleTag = "Secondary Fictional Account",
            balanceInPaise = 500_000L, // ₹5,000.00
            initialBalanceInPaise = 500_000L,
            isPrimary = false,
            lastUpdatedAt = now
        ),
        WalletAccountEntity(
            vpa = "priya.nair@okdemo",
            ownerName = "Priya Nair (Demo)",
            bankLabel = "Demo Metro Bank",
            initials = "PN",
            roleTag = "Verified Demo Contact",
            balanceInPaise = 350_000L, // ₹3,500.00
            initialBalanceInPaise = 350_000L,
            isPrimary = false,
            lastUpdatedAt = now
        ),
        WalletAccountEntity(
            vpa = "rohan.v@finguard",
            ownerName = "Rohan Verma (Demo)",
            bankLabel = "Horizon Sandbox Bank",
            initials = "RV",
            roleTag = "Verified Demo Contact",
            balanceInPaise = 250_000L, // ₹2,500.00
            initialBalanceInPaise = 250_000L,
            isPrimary = false,
            lastUpdatedAt = now
        ),
        WalletAccountEntity(
            vpa = "metro.mart@demopay",
            ownerName = "Metro Fresh Mart",
            bankLabel = "Merchant Sandbox",
            initials = "MF",
            roleTag = "Demo Merchant",
            balanceInPaise = 1_500_000L, // ₹15,000.00
            initialBalanceInPaise = 1_500_000L,
            isPrimary = false,
            lastUpdatedAt = now
        ),
        WalletAccountEntity(
            vpa = "quickfix.bill@simupi",
            ownerName = "QuickFix Utilities",
            bankLabel = "Utility Sandbox",
            initials = "QF",
            roleTag = "Demo Biller",
            balanceInPaise = 800_000L, // ₹8,000.00
            initialBalanceInPaise = 800_000L,
            isPrimary = false,
            lastUpdatedAt = now
        )
    )

    @Transaction
    suspend fun ensureSeeded() {
        if (getPrimaryAccountOnce() == null) {
            val now = System.currentTimeMillis()
            val accounts = defaultSeedAccounts(now)
            upsertAccounts(accounts)
            val primary = accounts.first()
            insertTransaction(
                WalletTransactionEntity(
                    txRef = FinGuardFormatters.generateLocalTxRef("FG-INIT"),
                    idempotencyKey = "seed-initial-allocation-${UUID.randomUUID()}",
                    type = TransactionType.CREDIT.name,
                    title = "Initial Demo Wallet Allocation",
                    senderName = "FinGuard Sandbox",
                    senderVpa = "sandbox@finguard",
                    counterpartyName = primary.ownerName,
                    counterpartyVpa = primary.vpa,
                    amountInPaise = INITIAL_DEMO_BALANCE_PAISE,
                    balanceBeforeInPaise = 0L,
                    balanceAfterInPaise = INITIAL_DEMO_BALANCE_PAISE,
                    receiverBalanceBeforeInPaise = 0L,
                    receiverBalanceAfterInPaise = INITIAL_DEMO_BALANCE_PAISE,
                    status = "SIMULATED_SUCCESS",
                    note = "Starting fictional demo funds (₹10,000.00)",
                    isSimulated = true,
                    createdAt = now
                )
            )
        }
    }

    @Transaction
    suspend fun addDemoMoney(
        targetVpa: String,
        amountInPaise: Long,
        note: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
        duplicateWindowMillis: Long = 4_000L
    ): WalletTransactionEntity {
        require(amountInPaise > 0L) { "Demo top-up amount must be greater than zero." }
        ensureSeeded()

        if (findByIdempotencyKey(idempotencyKey) != null) {
            throw DuplicateTransactionException("Duplicate top-up blocked: this simulated request was already processed.")
        }

        val now = System.currentTimeMillis()
        val normalizedNote = note.ifBlank { "Fictional demo wallet refill" }

        if (duplicateWindowMillis > 0L &&
            findRecentDuplicateTransfer(
                senderVpa = "vault@finguard",
                counterpartyVpa = targetVpa,
                amountInPaise = amountInPaise,
                note = normalizedNote,
                sinceMillis = now - duplicateWindowMillis
            ) != null
        ) {
            throw DuplicateTransactionException("Duplicate top-up prevented: identical simulated refill just completed.")
        }

        val target = getAccountByVpa(targetVpa)
            ?: throw IllegalStateException("Fictional target account '$targetVpa' not found.")

        val newBalance = target.balanceInPaise + amountInPaise
        upsertAccount(target.copy(balanceInPaise = newBalance, lastUpdatedAt = now))

        val tx = WalletTransactionEntity(
            txRef = FinGuardFormatters.generateLocalTxRef("FG-ADD"),
            idempotencyKey = idempotencyKey,
            type = TransactionType.CREDIT.name,
            title = "Added Demo Funds to ${target.ownerName}",
            senderName = "Demo Vault Top-Up",
            senderVpa = "vault@finguard",
            counterpartyName = target.ownerName,
            counterpartyVpa = target.vpa,
            amountInPaise = amountInPaise,
            balanceBeforeInPaise = target.balanceInPaise,
            balanceAfterInPaise = newBalance,
            receiverBalanceBeforeInPaise = target.balanceInPaise,
            receiverBalanceAfterInPaise = newBalance,
            status = "SIMULATED_SUCCESS",
            note = normalizedNote,
            isSimulated = true,
            createdAt = now
        )
        val id = insertTransaction(tx)
        return tx.copy(id = id)
    }

    @Transaction
    suspend fun executeTransfer(
        type: TransactionType,
        txPrefix: String,
        title: String,
        senderVpa: String,
        counterpartyName: String,
        counterpartyVpa: String,
        amountInPaise: Long,
        note: String,
        idempotencyKey: String = UUID.randomUUID().toString(),
        forceFailureReason: String? = null,
        duplicateWindowMillis: Long = 4_000L
    ): WalletTransactionEntity {
        require(amountInPaise > 0L) { "Transfer amount must be greater than ₹0." }
        ensureSeeded()

        if (senderVpa.equals(counterpartyVpa, ignoreCase = true)) {
            throw IllegalArgumentException("Sender and receiver fictional accounts must be different.")
        }

        if (findByIdempotencyKey(idempotencyKey) != null) {
            throw DuplicateTransactionException("Duplicate transaction blocked: this request key was already processed.")
        }

        val sender = getAccountByVpa(senderVpa)
            ?: throw IllegalStateException("Fictional sender account '$senderVpa' not found.")
        val receiver = getAccountByVpa(counterpartyVpa)
        val now = System.currentTimeMillis()
        val normalizedNote = note.ifBlank { "Simulated fictional transfer" }

        if (forceFailureReason == null && duplicateWindowMillis > 0L &&
            findRecentDuplicateTransfer(
                senderVpa = sender.vpa,
                counterpartyVpa = counterpartyVpa,
                amountInPaise = amountInPaise,
                note = normalizedNote,
                sinceMillis = now - duplicateWindowMillis
            ) != null
        ) {
            throw DuplicateTransactionException(
                "Duplicate transaction prevented: identical simulated transfer to $counterpartyVpa was just processed."
            )
        }

        if (forceFailureReason != null) {
            val failedTx = WalletTransactionEntity(
                txRef = FinGuardFormatters.generateLocalTxRef(txPrefix),
                idempotencyKey = idempotencyKey,
                type = TransactionType.UPI_SIM_FAILED.name,
                title = title,
                senderName = sender.ownerName,
                senderVpa = sender.vpa,
                counterpartyName = receiver?.ownerName ?: counterpartyName,
                counterpartyVpa = counterpartyVpa,
                amountInPaise = amountInPaise,
                balanceBeforeInPaise = sender.balanceInPaise,
                balanceAfterInPaise = sender.balanceInPaise,
                receiverBalanceBeforeInPaise = receiver?.balanceInPaise,
                receiverBalanceAfterInPaise = receiver?.balanceInPaise,
                status = "SIMULATED_FAILED",
                failureReason = forceFailureReason,
                note = normalizedNote,
                isSimulated = true,
                createdAt = now
            )
            val id = insertTransaction(failedTx)
            return failedTx.copy(id = id)
        }

        if (amountInPaise > sender.balanceInPaise) {
            throw IllegalStateException(
                "Overdraft prevented: ${sender.ownerName} has ${FinGuardFormatters.formatPaise(sender.balanceInPaise)} available."
            )
        }

        val senderAfter = sender.balanceInPaise - amountInPaise
        upsertAccount(sender.copy(balanceInPaise = senderAfter, lastUpdatedAt = now))

        val receiverBefore = receiver?.balanceInPaise
        val receiverAfter = if (receiver != null) {
            val updated = receiver.balanceInPaise + amountInPaise
            upsertAccount(receiver.copy(balanceInPaise = updated, lastUpdatedAt = now))
            updated
        } else {
            null
        }

        val successTx = WalletTransactionEntity(
            txRef = FinGuardFormatters.generateLocalTxRef(txPrefix),
            idempotencyKey = idempotencyKey,
            type = type.name,
            title = title,
            senderName = sender.ownerName,
            senderVpa = sender.vpa,
            counterpartyName = receiver?.ownerName ?: counterpartyName,
            counterpartyVpa = counterpartyVpa,
            amountInPaise = amountInPaise,
            balanceBeforeInPaise = sender.balanceInPaise,
            balanceAfterInPaise = senderAfter,
            receiverBalanceBeforeInPaise = receiverBefore,
            receiverBalanceAfterInPaise = receiverAfter,
            status = "SIMULATED_SUCCESS",
            note = normalizedNote,
            isSimulated = true,
            createdAt = now
        )
        val id = insertTransaction(successTx)
        return successTx.copy(id = id)
    }

    @Transaction
    suspend fun resetDemoWallet() {
        val now = System.currentTimeMillis()
        clearAllTransactions()
        clearAllAccounts()
        val accounts = defaultSeedAccounts(now)
        upsertAccounts(accounts)
        val primary = accounts.first()
        insertTransaction(
            WalletTransactionEntity(
                txRef = FinGuardFormatters.generateLocalTxRef("FG-RST"),
                idempotencyKey = "reset-allocation-${UUID.randomUUID()}",
                type = TransactionType.CREDIT.name,
                title = "Demo Sandbox Reset Allocation",
                senderName = "FinGuard Sandbox",
                senderVpa = "sandbox@finguard",
                counterpartyName = primary.ownerName,
                counterpartyVpa = primary.vpa,
                amountInPaise = INITIAL_DEMO_BALANCE_PAISE,
                balanceBeforeInPaise = 0L,
                balanceAfterInPaise = INITIAL_DEMO_BALANCE_PAISE,
                receiverBalanceBeforeInPaise = 0L,
                receiverBalanceAfterInPaise = INITIAL_DEMO_BALANCE_PAISE,
                status = "SIMULATED_SUCCESS",
                note = "All demo accounts reset (Primary: ₹10,000.00)",
                isSimulated = true,
                createdAt = now
            )
        )
    }
}

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY createdAt DESC, id DESC")
    fun observeAllScans(): Flow<List<ScanRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(record: ScanRecordEntity): Long

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun clearAllScans()
}

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget_profile WHERE id = 1 LIMIT 1")
    fun observeBudgetProfile(): Flow<BudgetProfileEntity?>

    @Query("SELECT * FROM budget_profile WHERE id = 1 LIMIT 1")
    suspend fun getBudgetProfileOnce(): BudgetProfileEntity?

    @Query("SELECT * FROM budget_expenses ORDER BY expenseDateMillis DESC, id DESC")
    fun observeExpenses(): Flow<List<BudgetExpenseEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY deadlineEpochMillis ASC, id DESC")
    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudgetProfile(profile: BudgetProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: BudgetExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<BudgetExpenseEntity>)

    @Query("DELETE FROM budget_expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM budget_expenses")
    suspend fun clearAllExpenses()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavingsGoal(goal: SavingsGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoals(goals: List<SavingsGoalEntity>)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Long)

    @Query("DELETE FROM savings_goals")
    suspend fun clearAllSavingsGoals()

    @Transaction
    suspend fun contributeToSavingsGoal(goalId: Long, addedPaise: Long) {
        require(addedPaise > 0L) { "Contribution amount must be greater than zero." }
        val existing = getGoalById(goalId) ?: throw IllegalStateException("Savings goal not found.")
        val updated = existing.copy(
            savedAmountInPaise = (existing.savedAmountInPaise + addedPaise).coerceAtMost(existing.targetAmountInPaise * 2L),
            isSampleData = false
        )
        upsertSavingsGoal(updated)
    }

    @Transaction
    suspend fun ensureBudgetSeeded(nowMillis: Long = System.currentTimeMillis()) {
        if (getBudgetProfileOnce() == null) {
            seedSampleBudgetInternal(nowMillis)
        }
    }

    @Transaction
    suspend fun restoreSampleBudgetData(nowMillis: Long = System.currentTimeMillis()) {
        clearAllExpenses()
        clearAllSavingsGoals()
        seedSampleBudgetInternal(nowMillis)
    }

    @Transaction
    suspend fun clearAllBudgetData(nowMillis: Long = System.currentTimeMillis()) {
        clearAllExpenses()
        clearAllSavingsGoals()
        upsertBudgetProfile(
            BudgetProfileEntity(
                id = 1,
                monthlyIncomeInPaise = 0L,
                monthlyBudgetInPaise = 0L,
                isSampleData = false,
                updatedAt = nowMillis
            )
        )
    }

    private suspend fun seedSampleBudgetInternal(nowMillis: Long) {
        val dayMs = 86_400_000L
        upsertBudgetProfile(
            BudgetProfileEntity(
                id = 1,
                monthlyIncomeInPaise = 4_500_000L, // ₹45,000.00 sample income
                monthlyBudgetInPaise = 2_500_000L, // ₹25,000.00 sample monthly budget
                isSampleData = true,
                updatedAt = nowMillis
            )
        )
        // Total = ₹19,250.00 (77% of ₹25,000.00 budget -> activates 75% Budget Alert cleanly)
        insertExpenses(
            listOf(
                BudgetExpenseEntity(
                    title = "Daily Meals & Tiffin (Sample)",
                    amountInPaise = 85_000L,
                    category = "FOOD",
                    note = "Sample daily food expense",
                    expenseDateMillis = nowMillis,
                    isSampleData = true,
                    createdAt = nowMillis
                ),
                BudgetExpenseEntity(
                    title = "Notebooks & Stationery (Sample)",
                    amountInPaise = 70_000L,
                    category = "OTHERS",
                    note = "Sample miscellaneous expense",
                    expenseDateMillis = nowMillis,
                    isSampleData = true,
                    createdAt = nowMillis
                ),
                BudgetExpenseEntity(
                    title = "Metro & Cab Commute (Sample)",
                    amountInPaise = 160_000L,
                    category = "TRAVEL",
                    note = "Sample weekly transit",
                    expenseDateMillis = nowMillis - 1L * dayMs,
                    isSampleData = true,
                    createdAt = nowMillis
                ),
                BudgetExpenseEntity(
                    title = "Weekly Groceries (Sample)",
                    amountInPaise = 350_000L,
                    category = "FOOD",
                    note = "Sample supermarket basket",
                    expenseDateMillis = nowMillis - 2L * dayMs,
                    isSampleData = true,
                    createdAt = nowMillis
                ),
                BudgetExpenseEntity(
                    title = "Pharmacy & Dental Checkup (Sample)",
                    amountInPaise = 110_000L,
                    category = "HEALTHCARE",
                    note = "Sample medical outlay",
                    expenseDateMillis = nowMillis - 4L * dayMs,
                    isSampleData = true,
                    createdAt = nowMillis
                ),
                BudgetExpenseEntity(
                    title = "Footwear & Accessories (Sample)",
                    amountInPaise = 320_000L,
                    category = "SHOPPING",
                    note = "Sample retail purchase",
                    expenseDateMillis = nowMillis - 5L * dayMs,
                    isSampleData = true,
                    createdAt = nowMillis
                ),
                BudgetExpenseEntity(
                    title = "Electricity & Fiber Broadband (Sample)",
                    amountInPaise = 620_000L,
                    category = "BILLS",
                    note = "Sample monthly utilities",
                    expenseDateMillis = nowMillis - 10L * dayMs,
                    isSampleData = true,
                    createdAt = nowMillis
                ),
                BudgetExpenseEntity(
                    title = "Security Certification Prep (Sample)",
                    amountInPaise = 210_000L,
                    category = "EDUCATION",
                    note = "Sample study materials",
                    expenseDateMillis = nowMillis - 14L * dayMs,
                    isSampleData = true,
                    createdAt = nowMillis
                )
            )
        )
        insertSavingsGoals(
            listOf(
                SavingsGoalEntity(
                    title = "Emergency Safety Fund (Sample)",
                    targetAmountInPaise = 3_000_000L, // ₹30,000
                    savedAmountInPaise = 1_400_000L,  // ₹14,000
                    deadlineEpochMillis = nowMillis + 90L * dayMs,
                    isSampleData = true,
                    createdAt = nowMillis
                ),
                SavingsGoalEntity(
                    title = "Cloud Security Course (Sample)",
                    targetAmountInPaise = 1_200_000L, // ₹12,000
                    savedAmountInPaise = 650_000L,    // ₹6,500
                    deadlineEpochMillis = nowMillis + 45L * dayMs,
                    isSampleData = true,
                    createdAt = nowMillis
                )
            )
        )
    }
}
