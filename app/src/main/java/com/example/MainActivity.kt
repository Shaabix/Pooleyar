package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppLanguage
import com.example.model.TransactionType
import com.example.ui.components.QuickAddTransactionDialog
import com.example.ui.screens.*
import com.example.ui.theme.PooleyarTheme
import com.example.util.AppStrings
import com.example.util.WindowWidthSizeClass
import com.example.util.rememberWindowSizeInfo
import com.example.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val layoutDirection = if (uiState.userSettings.language == AppLanguage.PERSIAN) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            PooleyarTheme(themeMode = uiState.userSettings.themeMode) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    if (uiState.userSettings.isAppLockEnabled && !uiState.isAppUnlocked) {
                        PinLockScreen(
                            userPin = uiState.userSettings.appPin,
                            language = uiState.userSettings.language,
                            onUnlock = { viewModel.unlockApp() }
                        )
                    } else {
                        PooleyarApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    userPin: String,
    language: AppLanguage,
    onUnlock: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (language == AppLanguage.PERSIAN) "ورود به پولیار" else "Unlock Pooleyar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (language == AppLanguage.PERSIAN) "پین‌کد امنیتی خود را وارد کنید" else "Please enter your security PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = enteredPin,
                onValueChange = {
                    if (it.length <= 6) {
                        enteredPin = it
                        isError = false
                        if (it == userPin) onUnlock()
                    }
                },
                isError = isError,
                singleLine = true,
                modifier = Modifier.width(220.dp)
            )
            if (isError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (language == AppLanguage.PERSIAN) "رمز عبور اشتباه است" else "Incorrect PIN",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (enteredPin == userPin) onUnlock() else isError = true
                }
            ) {
                Text(if (language == AppLanguage.PERSIAN) "تأیید و ورود" else "Unlock")
            }
        }
    }
}

data class NavigationDestination(
    val route: String,
    val titleKey: String,
    val icon: ImageVector
)

val NAV_ITEMS = listOf(
    NavigationDestination("home", "home", Icons.Default.Dashboard),
    NavigationDestination("transactions", "transactions", Icons.Default.ReceiptLong),
    NavigationDestination("accounts", "accounts", Icons.Default.AccountBalanceWallet),
    NavigationDestination("debts_checks", "debts_checks", Icons.Default.Payments),
    NavigationDestination("budgets", "budgets", Icons.Default.PieChart),
    NavigationDestination("recurring", "recurring", Icons.Default.Repeat),
    NavigationDestination("more", "more", Icons.Default.MoreHoriz)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PooleyarApp(viewModel: FinanceViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accounts by viewModel.accountsWithBalance.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val debts by viewModel.allDebts.collectAsStateWithLifecycle()
    val checks by viewModel.allChecks.collectAsStateWithLifecycle()
    val budgets by viewModel.allBudgets.collectAsStateWithLifecycle()
    val recurring by viewModel.allRecurring.collectAsStateWithLifecycle()
    val overview by viewModel.financialOverview.collectAsStateWithLifecycle()

    val windowSizeInfo = rememberWindowSizeInfo()
    val isTabletOrExpanded = windowSizeInfo.widthSizeClass != WindowWidthSizeClass.COMPACT
    val lang = uiState.userSettings.language

    // Quick Add or Edit Transaction Dialog
    if (uiState.isQuickAddOpen) {
        QuickAddTransactionDialog(
            initialType = uiState.quickAddDefaultType,
            transactionToEdit = uiState.transactionToEdit,
            accounts = accounts,
            categories = categories,
            userSettings = uiState.userSettings,
            onDismissRequest = { viewModel.closeQuickAdd() },
            onSaveTransaction = { id, type, amt, accId, destAccId, catId, desc, date, contact, notes ->
                viewModel.saveTransaction(
                    id = id,
                    type = type,
                    amount = amt,
                    accountId = accId,
                    destinationAccountId = destAccId,
                    categoryId = catId,
                    description = desc,
                    date = date,
                    contactPerson = contact,
                    notes = notes
                )
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isTabletOrExpanded) {
                NavigationBar(modifier = Modifier.testTag("compact_bottom_navigation")) {
                    NAV_ITEMS.take(5).forEach { item ->
                        val isSelected = uiState.currentTab == item.route
                        val title = AppStrings.get(item.titleKey, lang)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(item.route) },
                            icon = { Icon(item.icon, contentDescription = title) },
                            label = { Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Rail on Tablets & Expanded screens
            if (isTabletOrExpanded) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("tablet_navigation_rail"),
                    header = {
                        FloatingActionButton(
                            onClick = { viewModel.openQuickAdd(TransactionType.EXPENSE) },
                            modifier = Modifier
                                .padding(vertical = 12.dp)
                                .testTag("nav_rail_fab_add")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = AppStrings.get("quick_add", lang))
                        }
                    }
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    NAV_ITEMS.forEach { item ->
                        val isSelected = uiState.currentTab == item.route
                        val title = AppStrings.get(item.titleKey, lang)
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(item.route) },
                            icon = { Icon(item.icon, contentDescription = title) },
                            label = { Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                when (uiState.currentTab) {
                    "home" -> DashboardScreen(
                        overview = overview,
                        accounts = accounts,
                        recentTransactions = allTransactions,
                        userSettings = uiState.userSettings,
                        selectedJalaliYear = uiState.selectedJalaliYear,
                        selectedJalaliMonth = uiState.selectedJalaliMonth,
                        windowSizeInfo = windowSizeInfo,
                        onQuickAdd = { type -> viewModel.openQuickAdd(type) },
                        onNavigateToTransactions = { viewModel.selectTab("transactions") },
                        onNavigateToAccounts = { viewModel.selectTab("accounts") },
                        onMonthChange = { year, month -> viewModel.changeMonth(year, month) }
                    )
                    "transactions" -> TransactionsScreen(
                        transactions = allTransactions,
                        accounts = accounts,
                        categories = categories,
                        userSettings = uiState.userSettings,
                        windowSizeInfo = windowSizeInfo,
                        onEditTransaction = { txDetail -> viewModel.openQuickAdd(TransactionType.valueOf(txDetail.transaction.type), txDetail) },
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onQuickAdd = { viewModel.openQuickAdd() }
                    )
                    "accounts" -> AccountsScreen(
                        accounts = accounts,
                        userSettings = uiState.userSettings,
                        windowSizeInfo = windowSizeInfo,
                        onSaveAccount = { acc, card -> viewModel.saveAccount(acc, card) },
                        onDeleteAccount = { acc -> viewModel.deleteAccount(acc) }
                    )
                    "debts_checks" -> DebtsAndChecksScreen(
                        debts = debts,
                        checks = checks,
                        accounts = accounts,
                        userSettings = uiState.userSettings,
                        windowSizeInfo = windowSizeInfo,
                        onSaveDebt = { debt -> viewModel.saveDebt(debt) },
                        onRecordDebtPayment = { debt, amt, accId -> viewModel.recordDebtPayment(debt, amt, accId) },
                        onDeleteDebt = { debt -> viewModel.deleteDebt(debt) },
                        onSaveCheck = { chk -> viewModel.saveCheck(chk) },
                        onClearCheck = { chk, accId -> viewModel.clearCheck(chk, accId) },
                        onDeleteCheck = { chk -> viewModel.deleteCheck(chk) }
                    )
                    "budgets" -> BudgetsScreen(
                        budgets = budgets,
                        categories = categories,
                        transactions = allTransactions,
                        userSettings = uiState.userSettings,
                        selectedJalaliYear = uiState.selectedJalaliYear,
                        selectedJalaliMonth = uiState.selectedJalaliMonth,
                        windowSizeInfo = windowSizeInfo,
                        onSaveBudget = { catId, limit, year, month -> viewModel.saveBudget(catId, limit, year, month) },
                        onDeleteBudget = { b -> viewModel.deleteBudget(b) }
                    )
                    "recurring" -> RecurringTransactionsScreen(
                        recurringList = recurring,
                        accounts = accounts,
                        categories = categories,
                        userSettings = uiState.userSettings,
                        windowSizeInfo = windowSizeInfo,
                        onSaveRecurring = { rec -> viewModel.saveRecurring(rec) },
                        onDeleteRecurring = { rec -> viewModel.deleteRecurring(rec) }
                    )
                    "more" -> SettingsAndMoreScreen(
                        userSettings = uiState.userSettings,
                        transactions = allTransactions,
                        windowSizeInfo = windowSizeInfo,
                        repository = viewModel.repository,
                        onThemeModeChange = { viewModel.updateThemeMode(it) },
                        onLanguageChange = { viewModel.updateLanguage(it) },
                        onCalendarChange = { viewModel.updateCalendarType(it) },
                        onDigitChange = { viewModel.updateDigitFormat(it) },
                        onDatePatternChange = { viewModel.updateDateFormatPattern(it) },
                        onFirstDayChange = { viewModel.updateFirstDayOfWeek(it) },
                        onTimeFormatChange = { viewModel.updateTimeFormat(it) },
                        onCurrencyChange = { viewModel.updateCurrency(it) },
                        onAppLockChange = { enabled, pin -> viewModel.updateAppLock(enabled, pin) }
                    )
                }
            }
        }
    }
}
