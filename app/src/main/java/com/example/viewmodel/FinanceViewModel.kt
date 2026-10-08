package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.*
import com.example.model.*
import com.example.repository.*
import com.example.util.Currency
import com.example.util.JalaliDate
import com.example.util.PreferencesManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PooleyarUiState(
    val currentTab: String = "home",
    val userSettings: UserSettings = UserSettings(),
    val selectedJalaliYear: Int = JalaliDate.now().year,
    val selectedJalaliMonth: Int = JalaliDate.now().month,
    val isQuickAddOpen: Boolean = false,
    val quickAddDefaultType: TransactionType = TransactionType.EXPENSE,
    val transactionToEdit: TransactionDetail? = null,
    val selectedAccountIdForDetail: Long? = null,
    val selectedTransactionForDetail: Long? = null,
    val searchQuery: String = "",
    val filterType: String? = null,
    val filterCategoryId: Long? = null,
    val filterAccountId: Long? = null,
    val isAppUnlocked: Boolean = false
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = FinanceRepository(db)
    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(
        PooleyarUiState(
            userSettings = prefs.getUserSettings(),
            isAppUnlocked = !prefs.getUserSettings().isAppLockEnabled
        )
    )
    val uiState: StateFlow<PooleyarUiState> = _uiState.asStateFlow()

    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = repository.accountsWithBalance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionDetail>> = repository.transactionsWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.activeCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<DebtEntity>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChecks: StateFlow<List<CheckEntity>> = repository.allChecks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBudgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecurring: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurring
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialOverview: StateFlow<FinancialOverview> = _uiState
        .flatMapLatest { state ->
            repository.getFinancialOverview(state.selectedJalaliYear, state.selectedJalaliMonth)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            FinancialOverview(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        )

    init {
        viewModelScope.launch {
            repository.executeDueRecurringTransactions()
        }
    }

    fun selectTab(tab: String) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        prefs.saveThemeMode(themeMode)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(themeMode = themeMode)) }
    }

    fun updateLanguage(language: AppLanguage) {
        prefs.saveLanguage(language)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(language = language)) }
    }

    fun updateCalendarType(calendarType: CalendarType) {
        prefs.saveCalendarType(calendarType)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(calendarType = calendarType)) }
    }

    fun updateDigitFormat(digitFormat: DigitFormat) {
        prefs.saveDigitFormat(digitFormat)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(digitFormat = digitFormat)) }
    }

    fun updateDateFormatPattern(pattern: DateFormatPattern) {
        prefs.saveDateFormatPattern(pattern)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(dateFormatPattern = pattern)) }
    }

    fun updateFirstDayOfWeek(firstDay: FirstDayOfWeek) {
        prefs.saveFirstDayOfWeek(firstDay)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(firstDayOfWeek = firstDay)) }
    }

    fun updateTimeFormat(timeFormat: TimeFormat) {
        prefs.saveTimeFormat(timeFormat)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(timeFormat = timeFormat)) }
    }

    fun updateCurrency(currency: Currency) {
        prefs.saveCurrency(currency)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(currency = currency)) }
    }

    fun updateAppLock(enabled: Boolean, pin: String) {
        prefs.saveAppLock(enabled, pin)
        _uiState.update { it.copy(userSettings = it.userSettings.copy(isAppLockEnabled = enabled, appPin = pin)) }
    }

    fun unlockApp() {
        _uiState.update { it.copy(isAppUnlocked = true) }
    }

    fun openQuickAdd(defaultType: TransactionType = TransactionType.EXPENSE, transactionToEdit: TransactionDetail? = null) {
        _uiState.update { it.copy(isQuickAddOpen = true, quickAddDefaultType = defaultType, transactionToEdit = transactionToEdit) }
    }

    fun closeQuickAdd() {
        _uiState.update { it.copy(isQuickAddOpen = false, transactionToEdit = null) }
    }

    fun selectAccountDetail(accountId: Long?) {
        _uiState.update { it.copy(selectedAccountIdForDetail = accountId) }
    }

    fun selectTransactionDetail(txId: Long?) {
        _uiState.update { it.copy(selectedTransactionForDetail = txId) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setFilters(type: String?, categoryId: Long?, accountId: Long?) {
        _uiState.update { it.copy(filterType = type, filterCategoryId = categoryId, filterAccountId = accountId) }
    }

    fun changeMonth(year: Int, month: Int) {
        _uiState.update { it.copy(selectedJalaliYear = year, selectedJalaliMonth = month) }
    }

    // CRUD dispatchers
    fun saveTransaction(
        id: Long = 0L,
        type: TransactionType,
        amount: Long,
        accountId: Long,
        destinationAccountId: Long? = null,
        categoryId: Long? = null,
        description: String = "",
        date: JalaliDate = JalaliDate.now(),
        contactPerson: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val epoch = JalaliDate.toTimestamp(date.year, date.month, date.day)
            val tx = TransactionEntity(
                id = id,
                type = type.name,
                amount = amount,
                accountId = accountId,
                destinationAccountId = destinationAccountId,
                categoryId = categoryId,
                dateEpochMillis = epoch,
                jalaliYear = date.year,
                jalaliMonth = date.month,
                jalaliDay = date.day,
                description = description,
                contactPerson = contactPerson,
                notes = notes
            )
            repository.saveTransaction(tx)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun saveAccount(account: AccountEntity, cardNumber: String = "") {
        viewModelScope.launch {
            val accId = repository.saveAccount(account)
            if (cardNumber.isNotBlank()) {
                repository.saveCard(
                    BankCardEntity(
                        accountId = accId,
                        cardHolderName = account.name,
                        cardNumber = cardNumber,
                        bankId = account.bankId
                    )
                )
            }
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun saveDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.saveDebt(debt)
        }
    }

    fun recordDebtPayment(debt: DebtEntity, paymentAmount: Long, accountId: Long) {
        viewModelScope.launch {
            repository.recordDebtPayment(debt, paymentAmount, accountId)
        }
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun saveCheck(check: CheckEntity) {
        viewModelScope.launch {
            repository.saveCheck(check)
        }
    }

    fun clearCheck(check: CheckEntity, accountId: Long) {
        viewModelScope.launch {
            repository.clearCheck(check, accountId)
        }
    }

    fun deleteCheck(check: CheckEntity) {
        viewModelScope.launch {
            repository.deleteCheck(check)
        }
    }

    fun saveBudget(categoryId: Long, monthlyLimit: Long, year: Int, month: Int) {
        viewModelScope.launch {
            val budget = BudgetEntity(
                categoryId = categoryId,
                monthlyLimit = monthlyLimit,
                jalaliYear = year,
                jalaliMonth = month
            )
            repository.saveBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun saveRecurring(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.saveRecurring(recurring)
        }
    }

    fun deleteRecurring(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
        }
    }
}
