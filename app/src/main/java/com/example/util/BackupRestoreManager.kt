package com.example.util

import com.example.data.entity.*
import com.example.repository.FinanceRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first

data class PooleyarBackupData(
    val version: Int = 1,
    val backupTimestamp: Long = System.currentTimeMillis(),
    val accounts: List<AccountEntity>,
    val cards: List<BankCardEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val debts: List<DebtEntity>,
    val checks: List<CheckEntity>,
    val recurring: List<RecurringTransactionEntity>
)

object BackupRestoreManager {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(PooleyarBackupData::class.java)

    suspend fun createBackupJson(repository: FinanceRepository): String {
        val accounts = repository.activeAccounts.first()
        val cards = repository.allCards.first()
        val categories = repository.activeCategories.first()
        val transactions = repository.allTransactions.first()
        val budgets = repository.allBudgets.first()
        val debts = repository.allDebts.first()
        val checks = repository.allChecks.first()
        val recurring = repository.allRecurring.first()

        val backup = PooleyarBackupData(
            accounts = accounts,
            cards = cards,
            categories = categories,
            transactions = transactions,
            budgets = budgets,
            debts = debts,
            checks = checks,
            recurring = recurring
        )
        return adapter.indent("  ").toJson(backup)
    }

    suspend fun restoreBackupJson(jsonString: String, repository: FinanceRepository): Boolean {
        return try {
            val data = adapter.fromJson(jsonString) ?: return false
            repository.restoreAllData(
                accounts = data.accounts,
                cards = data.cards,
                categories = data.categories,
                transactions = data.transactions,
                budgets = data.budgets,
                debts = data.debts,
                checks = data.checks,
                recurring = data.recurring
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
