package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WalletAccountEntity::class,
        WalletTransactionEntity::class,
        ScanRecordEntity::class,
        BudgetProfileEntity::class,
        BudgetExpenseEntity::class,
        SavingsGoalEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FinGuardDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: FinGuardDatabase? = null

        fun getInstance(context: Context): FinGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinGuardDatabase::class.java,
                    "finguard_ai_sandbox.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
