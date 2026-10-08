package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // BANK, CASH, SAVINGS, etc.
    val bankId: String? = null,
    val accountNumber: String = "",
    val shebaNumber: String = "",
    val initialBalance: Long = 0L, // in Toman
    val colorHex: Long = 0xFF0D47A1,
    val iconName: String = "account_balance",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cards")
data class BankCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val cardHolderName: String = "",
    val cardNumber: String = "",
    val bankId: String? = null,
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val notes: String = ""
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // EXPENSE, INCOME
    val iconName: String,
    val colorHex: Long,
    val parentId: Long? = null,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // EXPENSE, INCOME, TRANSFER
    val amount: Long, // in Toman
    val accountId: Long,
    val destinationAccountId: Long? = null, // for TRANSFER
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val dateEpochMillis: Long,
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val description: String = "",
    val notes: String = "",
    val tags: String = "", // comma-separated
    val contactPerson: String = "",
    val relatedDebtId: Long? = null,
    val relatedCheckId: Long? = null,
    val recurringRuleId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val monthlyLimit: Long, // in Toman
    val jalaliYear: Int,
    val jalaliMonth: Int, // 1..12 or 0 for recurring monthly
    val notes: String = ""
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val direction: String, // I_OWE, OWED_TO_ME
    val totalAmount: Long, // in Toman
    val paidAmount: Long = 0L, // in Toman
    val dateEpochMillis: Long,
    val dueDateEpochMillis: Long? = null,
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val status: String, // ACTIVE, PARTIALLY_PAID, PAID, OVERDUE
    val description: String = "",
    val notes: String = ""
)

@Entity(tableName = "checks")
data class CheckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val checkNumber: String,
    val direction: String, // INCOMING, OUTGOING
    val amount: Long, // in Toman
    val bankId: String? = null,
    val accountId: Long? = null,
    val issuerName: String = "",
    val recipientName: String = "",
    val issueDateEpochMillis: Long,
    val dueDateEpochMillis: Long,
    val dueJalaliYear: Int,
    val dueJalaliMonth: Int,
    val dueJalaliDay: Int,
    val status: String, // RECEIVED_OR_ISSUED, PRESENTED, CLEARED, BOUNCED, CANCELLED
    val description: String = "",
    val clearedTransactionId: Long? = null
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // EXPENSE, INCOME
    val amount: Long,
    val accountId: Long,
    val categoryId: Long,
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val dayOfInterval: Int = 1, // e.g., 1st day of month
    val lastExecutedEpochMillis: Long = 0L,
    val isActive: Boolean = true
)
