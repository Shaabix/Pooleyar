package com.example.repository

import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.entity.*
import com.example.model.*
import com.example.util.JalaliDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class AccountWithBalance(
    val account: AccountEntity,
    val calculatedBalance: Long,
    val cards: List<BankCardEntity> = emptyList()
)

data class TransactionDetail(
    val transaction: TransactionEntity,
    val sourceAccount: AccountEntity?,
    val destinationAccount: AccountEntity?,
    val category: CategoryEntity?
)

data class FinancialOverview(
    val totalBalance: Long,
    val bankBalance: Long,
    val cashBalance: Long,
    val monthlyIncome: Long,
    val monthlyExpense: Long,
    val monthlyNetSavings: Long,
    val totalDebtIOwe: Long,
    val totalOwedToMe: Long,
    val pendingIncomingChecks: Long,
    val pendingOutgoingChecks: Long
)

class FinanceRepository(private val db: AppDatabase) {

    val activeAccounts: Flow<List<AccountEntity>> = db.accountDao().getAllActiveAccounts()
    val allCards: Flow<List<BankCardEntity>> = db.accountDao().getAllCards()
    val activeCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllActiveCategories()
    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val allDebts: Flow<List<DebtEntity>> = db.debtDao().getAllDebts()
    val allChecks: Flow<List<CheckEntity>> = db.checkDao().getAllChecks()
    val allBudgets: Flow<List<BudgetEntity>> = db.budgetDao().getAllBudgets()
    val allRecurring: Flow<List<RecurringTransactionEntity>> = db.recurringTransactionDao().getAllRecurring()

    /**
     * Calculates live balances for all accounts based on initialBalance + sum of transactions
     */
    val accountsWithBalance: Flow<List<AccountWithBalance>> = combine(
        activeAccounts,
        allCards,
        allTransactions
    ) { accounts, cards, txList ->
        accounts.map { acc ->
            var currentBalance = acc.initialBalance
            for (tx in txList) {
                if (tx.accountId == acc.id) {
                    when (tx.type) {
                        TransactionType.EXPENSE.name -> currentBalance -= tx.amount
                        TransactionType.INCOME.name -> currentBalance += tx.amount
                        TransactionType.TRANSFER.name -> currentBalance -= tx.amount
                    }
                }
                if (tx.destinationAccountId == acc.id && tx.type == TransactionType.TRANSFER.name) {
                    currentBalance += tx.amount
                }
            }
            val accCards = cards.filter { it.accountId == acc.id }
            AccountWithBalance(acc, currentBalance, accCards)
        }
    }

    /**
     * Rich transactions with resolved Category and Account references
     */
    val transactionsWithDetails: Flow<List<TransactionDetail>> = combine(
        allTransactions,
        activeAccounts,
        activeCategories
    ) { txs, accounts, categories ->
        val accMap = accounts.associateBy { it.id }
        val catMap = categories.associateBy { it.id }
        txs.map { tx ->
            TransactionDetail(
                transaction = tx,
                sourceAccount = accMap[tx.accountId],
                destinationAccount = tx.destinationAccountId?.let { accMap[it] },
                category = tx.categoryId?.let { catMap[it] }
            )
        }
    }

    /**
     * Live financial overview for current Jalali month
     */
    fun getFinancialOverview(currentJalaliYear: Int, currentJalaliMonth: Int): Flow<FinancialOverview> = combine(
        accountsWithBalance,
        allTransactions,
        allDebts,
        allChecks
    ) { accList, txList, debtList, checkList ->
        var totalBal = 0L
        var bankBal = 0L
        var cashBal = 0L

        accList.forEach { acc ->
            totalBal += acc.calculatedBalance
            if (acc.account.type == AccountType.BANK.name) {
                bankBal += acc.calculatedBalance
            } else if (acc.account.type == AccountType.CASH.name) {
                cashBal += acc.calculatedBalance
            }
        }

        var mIncome = 0L
        var mExpense = 0L
        txList.filter { it.jalaliYear == currentJalaliYear && it.jalaliMonth == currentJalaliMonth }
            .forEach { tx ->
                when (tx.type) {
                    TransactionType.INCOME.name -> mIncome += tx.amount
                    TransactionType.EXPENSE.name -> mExpense += tx.amount
                }
            }

        var iOwe = 0L
        var owedToMe = 0L
        debtList.filter { it.status == DebtStatus.ACTIVE.name || it.status == DebtStatus.PARTIALLY_PAID.name || it.status == DebtStatus.OVERDUE.name }
            .forEach { d ->
                val remaining = (d.totalAmount - d.paidAmount).coerceAtLeast(0L)
                if (d.direction == DebtDirection.I_OWE.name) {
                    iOwe += remaining
                } else {
                    owedToMe += remaining
                }
            }

        var incomingChecks = 0L
        var outgoingChecks = 0L
        checkList.filter { it.status == CheckStatus.RECEIVED_OR_ISSUED.name || it.status == CheckStatus.PRESENTED.name }
            .forEach { chk ->
                if (chk.direction == CheckDirection.INCOMING.name) {
                    incomingChecks += chk.amount
                } else {
                    outgoingChecks += chk.amount
                }
            }

        FinancialOverview(
            totalBalance = totalBal,
            bankBalance = bankBal,
            cashBalance = cashBal,
            monthlyIncome = mIncome,
            monthlyExpense = mExpense,
            monthlyNetSavings = mIncome - mExpense,
            totalDebtIOwe = iOwe,
            totalOwedToMe = owedToMe,
            pendingIncomingChecks = incomingChecks,
            pendingOutgoingChecks = outgoingChecks
        )
    }

    // CRUD operations
    suspend fun saveAccount(account: AccountEntity): Long {
        return if (account.id == 0L) db.accountDao().insertAccount(account)
        else {
            db.accountDao().updateAccount(account)
            account.id
        }
    }

    suspend fun saveAccountWithCard(account: AccountEntity, cardNumber: String = "", existingCardId: Long? = null): Long {
        return db.withTransaction {
            val accId = if (account.id == 0L) db.accountDao().insertAccount(account)
            else {
                db.accountDao().updateAccount(account)
                account.id
            }
            if (cardNumber.isNotBlank()) {
                val existingCards = db.accountDao().getCardsForAccountSync(accId)
                // If existingCardId is provided, validate it belongs to this account; otherwise fallback to first existing card
                val targetCard = if (existingCardId != null && existingCardId != 0L) {
                    val found = db.accountDao().getCardById(existingCardId)
                    require(found != null && found.accountId == accId) {
                        "Card does not exist or does not belong to this account"
                    }
                    found
                } else {
                    existingCards.firstOrNull()
                }

                if (targetCard != null) {
                    // Update existing card while strictly preserving expiryMonth, expiryYear, notes
                    val updatedCard = targetCard.copy(
                        cardHolderName = account.name,
                        cardNumber = cardNumber,
                        bankId = account.bankId
                    )
                    db.accountDao().updateCard(updatedCard)
                } else {
                    val newCard = BankCardEntity(
                        accountId = accId,
                        cardHolderName = account.name,
                        cardNumber = cardNumber,
                        bankId = account.bankId
                    )
                    db.accountDao().insertCard(newCard)
                }
            }
            accId
        }
    }

    suspend fun deleteAccount(account: AccountEntity) = db.accountDao().deleteAccount(account)

    suspend fun saveCard(card: BankCardEntity): Long = db.accountDao().insertCard(card)

    suspend fun deleteCard(card: BankCardEntity) = db.accountDao().deleteCard(card)

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        return if (transaction.id == 0L) db.transactionDao().insertTransaction(transaction)
        else {
            db.transactionDao().updateTransaction(transaction)
            transaction.id
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) = db.transactionDao().deleteTransaction(transaction)

    suspend fun saveCategory(category: CategoryEntity): Long {
        return if (category.id == 0L) db.categoryDao().insertCategory(category)
        else {
            db.categoryDao().updateCategory(category)
            category.id
        }
    }

    suspend fun deleteCategory(category: CategoryEntity) = db.categoryDao().deleteCategory(category)

    suspend fun saveDebt(debt: DebtEntity): Long {
        return if (debt.id == 0L) db.debtDao().insertDebt(debt)
        else {
            db.debtDao().updateDebt(debt)
            debt.id
        }
    }

    suspend fun recordDebtPayment(debt: DebtEntity, paymentAmount: Long, accountId: Long) {
        val newPaid = (debt.paidAmount + paymentAmount).coerceAtMost(debt.totalAmount)
        val newStatus = if (newPaid >= debt.totalAmount) DebtStatus.PAID.name else DebtStatus.PARTIALLY_PAID.name
        val updatedDebt = debt.copy(paidAmount = newPaid, status = newStatus)
        db.debtDao().updateDebt(updatedDebt)

        val nowJalali = JalaliDate.now()
        val txType = if (debt.direction == DebtDirection.I_OWE.name) TransactionType.EXPENSE.name else TransactionType.INCOME.name
        val desc = if (debt.direction == DebtDirection.I_OWE.name) "پرداخت بدهی به ${debt.personName}" else "دریافت طلب از ${debt.personName}"

        val tx = TransactionEntity(
            type = txType,
            amount = paymentAmount,
            accountId = accountId,
            dateEpochMillis = System.currentTimeMillis(),
            jalaliYear = nowJalali.year,
            jalaliMonth = nowJalali.month,
            jalaliDay = nowJalali.day,
            description = desc,
            contactPerson = debt.personName,
            relatedDebtId = debt.id
        )
        db.transactionDao().insertTransaction(tx)
    }

    suspend fun deleteDebt(debt: DebtEntity) = db.debtDao().deleteDebt(debt)

    suspend fun saveCheck(check: CheckEntity): Long {
        return if (check.id == 0L) db.checkDao().insertCheck(check)
        else {
            db.checkDao().updateCheck(check)
            check.id
        }
    }

    suspend fun clearCheck(check: CheckEntity, accountId: Long) {
        val nowJalali = JalaliDate.now()
        val txType = if (check.direction == CheckDirection.INCOMING.name) TransactionType.INCOME.name else TransactionType.EXPENSE.name
        val desc = if (check.direction == CheckDirection.INCOMING.name) "وصول چک دریافتی شماره ${check.checkNumber}" else "پاس شدن چک صادره شماره ${check.checkNumber}"

        val tx = TransactionEntity(
            type = txType,
            amount = check.amount,
            accountId = accountId,
            dateEpochMillis = System.currentTimeMillis(),
            jalaliYear = nowJalali.year,
            jalaliMonth = nowJalali.month,
            jalaliDay = nowJalali.day,
            description = desc,
            relatedCheckId = check.id
        )
        val txId = db.transactionDao().insertTransaction(tx)
        val updatedCheck = check.copy(
            status = CheckStatus.CLEARED.name,
            accountId = accountId,
            clearedTransactionId = txId
        )
        db.checkDao().updateCheck(updatedCheck)
    }

    suspend fun deleteCheck(check: CheckEntity) = db.checkDao().deleteCheck(check)

    suspend fun saveBudget(budget: BudgetEntity): Long {
        return if (budget.id == 0L) db.budgetDao().insertBudget(budget)
        else {
            db.budgetDao().updateBudget(budget)
            budget.id
        }
    }

    suspend fun deleteBudget(budget: BudgetEntity) = db.budgetDao().deleteBudget(budget)

    // Recurring transactions engine
    suspend fun saveRecurring(recurring: RecurringTransactionEntity): Long {
        return if (recurring.id == 0L) db.recurringTransactionDao().insertRecurring(recurring)
        else {
            db.recurringTransactionDao().updateRecurring(recurring)
            recurring.id
        }
    }

    suspend fun deleteRecurring(recurring: RecurringTransactionEntity) = db.recurringTransactionDao().deleteRecurring(recurring)

    /**
     * Executes pending recurring transactions deterministically (idempotent, prevents duplicates)
     */
    suspend fun executeDueRecurringTransactions() {
        val activeRecurring = db.recurringTransactionDao().getActiveRecurringTransactions().first()
        val now = System.currentTimeMillis()
        val nowJalali = JalaliDate.now()

        for (rec in activeRecurring) {
            val oneDayMillis = 24 * 60 * 60 * 1000L
            val isDue = when (rec.frequency) {
                RecurringFrequency.DAILY.name -> now - rec.lastExecutedEpochMillis >= oneDayMillis
                RecurringFrequency.WEEKLY.name -> now - rec.lastExecutedEpochMillis >= 7 * oneDayMillis
                RecurringFrequency.MONTHLY.name -> {
                    // Check if run this month and reached day of month
                    val lastRunJalali = if (rec.lastExecutedEpochMillis > 0) JalaliDate.fromTimestamp(rec.lastExecutedEpochMillis) else null
                    val alreadyRunThisMonth = lastRunJalali != null && lastRunJalali.year == nowJalali.year && lastRunJalali.month == nowJalali.month
                    !alreadyRunThisMonth && nowJalali.day >= rec.dayOfInterval
                }
                RecurringFrequency.YEARLY.name -> {
                    val lastRunJalali = if (rec.lastExecutedEpochMillis > 0) JalaliDate.fromTimestamp(rec.lastExecutedEpochMillis) else null
                    lastRunJalali == null || (nowJalali.year > lastRunJalali.year && nowJalali.month >= 1)
                }
                else -> false
            }

            if (isDue) {
                val tx = TransactionEntity(
                    type = rec.type,
                    amount = rec.amount,
                    accountId = rec.accountId,
                    categoryId = rec.categoryId,
                    dateEpochMillis = now,
                    jalaliYear = nowJalali.year,
                    jalaliMonth = nowJalali.month,
                    jalaliDay = nowJalali.day,
                    description = "[دوره] ${rec.title}",
                    recurringRuleId = rec.id
                )
                db.transactionDao().insertTransaction(tx)
                db.recurringTransactionDao().updateRecurring(rec.copy(lastExecutedEpochMillis = now))
            }
        }
    }

    suspend fun restoreAllData(
        accounts: List<AccountEntity>,
        cards: List<BankCardEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        debts: List<DebtEntity>,
        checks: List<CheckEntity>,
        recurring: List<RecurringTransactionEntity>
    ) {
        db.accountDao().insertAllAccounts(accounts)
        db.accountDao().insertAllCards(cards)
        db.categoryDao().insertAll(categories)
        db.transactionDao().insertAllTransactions(transactions)
        db.budgetDao().insertAllBudgets(budgets)
        db.debtDao().insertAllDebts(debts)
        db.checkDao().insertAllChecks(checks)
        db.recurringTransactionDao().insertAllRecurring(recurring)
    }
}
