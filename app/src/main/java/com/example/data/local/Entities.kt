package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_account")
data class WalletAccountEntity(
    @PrimaryKey val vpa: String,
    val ownerName: String,
    val bankLabel: String,
    val initials: String,
    val roleTag: String,
    val balanceInPaise: Long,
    val initialBalanceInPaise: Long,
    val isPrimary: Boolean = false,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wallet_transactions",
    indices = [Index(value = ["idempotencyKey"], unique = true)]
)
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val txRef: String,
    val idempotencyKey: String,
    val type: String,
    val title: String,
    val senderName: String,
    val senderVpa: String,
    val counterpartyName: String,
    val counterpartyVpa: String,
    val amountInPaise: Long,
    val balanceBeforeInPaise: Long,
    val balanceAfterInPaise: Long,
    val receiverBalanceBeforeInPaise: Long? = null,
    val receiverBalanceAfterInPaise: Long? = null,
    val status: String,
    val failureReason: String? = null,
    val note: String = "",
    val isSimulated: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "scan_history")
data class ScanRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scanType: String,
    val inputContent: String,
    val riskScore: Int,
    val riskLevel: String,
    val headline: String,
    val explanation: String,
    val indicatorsJoined: String,
    val recommendation: String,
    val engineUsed: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget_profile")
data class BudgetProfileEntity(
    @PrimaryKey val id: Int = 1,
    val monthlyIncomeInPaise: Long,
    val monthlyBudgetInPaise: Long,
    val isSampleData: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget_expenses")
data class BudgetExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amountInPaise: Long,
    val category: String,
    val note: String = "",
    val expenseDateMillis: Long = System.currentTimeMillis(),
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmountInPaise: Long,
    val savedAmountInPaise: Long,
    val deadlineEpochMillis: Long,
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
