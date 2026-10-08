package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransactionType
import com.example.model.UserSettings
import com.example.repository.AccountWithBalance
import com.example.repository.FinancialOverview
import com.example.repository.TransactionDetail
import com.example.ui.components.ChartSlice
import com.example.ui.components.MonthlyComparisonBarChart
import com.example.ui.components.PooleyarDonutChart
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.PersianGreenIncome
import com.example.ui.theme.PersianRoseExpense
import com.example.util.*

@Composable
fun DashboardScreen(
    overview: FinancialOverview,
    accounts: List<AccountWithBalance>,
    recentTransactions: List<TransactionDetail>,
    userSettings: UserSettings,
    selectedJalaliYear: Int,
    selectedJalaliMonth: Int,
    windowSizeInfo: WindowSizeInfo,
    onQuickAdd: (TransactionType) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onMonthChange: (Int, Int) -> Unit
) {
    val lang = userSettings.language
    val monthName = AppDateTimeFormatter.getMonthName(
        monthIndex = selectedJalaliMonth,
        calendarType = userSettings.calendarType,
        language = lang
    )

    val expenseDetailsThisMonth = remember(recentTransactions, selectedJalaliYear, selectedJalaliMonth) {
        recentTransactions.filter {
            it.transaction.type == TransactionType.EXPENSE.name &&
            it.transaction.jalaliYear == selectedJalaliYear &&
            it.transaction.jalaliMonth == selectedJalaliMonth
        }
    }

    val categorySlices = remember(expenseDetailsThisMonth) {
        val grouped = expenseDetailsThisMonth.groupBy { it.category?.name ?: "سایر هزینه‌ها" }
        grouped.map { (catName, list) ->
            val total = list.sumOf { it.transaction.amount }
            val colorHex = list.firstOrNull()?.category?.colorHex ?: 0xFF9E9E9E
            ChartSlice(catName, total, Color(colorHex))
        }.sortedByDescending { it.value }
    }

    val isLargeScreenLandscape = windowSizeInfo.isTablet && windowSizeInfo.isLandscape

    if (isLargeScreenLandscape) {
        LandscapeTabletDashboard(
            overview = overview,
            accounts = accounts,
            recentTransactions = recentTransactions,
            categorySlices = categorySlices,
            userSettings = userSettings,
            monthName = monthName,
            selectedJalaliYear = selectedJalaliYear,
            selectedJalaliMonth = selectedJalaliMonth,
            onQuickAdd = onQuickAdd,
            onNavigateToTransactions = onNavigateToTransactions,
            onNavigateToAccounts = onNavigateToAccounts,
            onMonthChange = onMonthChange
        )
    } else {
        PortraitDashboard(
            overview = overview,
            accounts = accounts,
            recentTransactions = recentTransactions,
            categorySlices = categorySlices,
            userSettings = userSettings,
            monthName = monthName,
            selectedJalaliYear = selectedJalaliYear,
            selectedJalaliMonth = selectedJalaliMonth,
            onQuickAdd = onQuickAdd,
            onNavigateToTransactions = onNavigateToTransactions,
            onNavigateToAccounts = onNavigateToAccounts,
            onMonthChange = onMonthChange
        )
    }
}

@Composable
private fun PortraitDashboard(
    overview: FinancialOverview,
    accounts: List<AccountWithBalance>,
    recentTransactions: List<TransactionDetail>,
    categorySlices: List<ChartSlice>,
    userSettings: UserSettings,
    monthName: String,
    selectedJalaliYear: Int,
    selectedJalaliMonth: Int,
    onQuickAdd: (TransactionType) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onMonthChange: (Int, Int) -> Unit
) {
    val lang = userSettings.language

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("portrait_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MonthNavigatorHeader(
                selectedYear = selectedJalaliYear,
                selectedMonth = selectedJalaliMonth,
                monthName = monthName,
                userSettings = userSettings,
                onMonthChange = onMonthChange
            )
        }

        item {
            TotalBalanceHeroCard(
                totalBalance = overview.totalBalance,
                monthlyIncome = overview.monthlyIncome,
                monthlyExpense = overview.monthlyExpense,
                userSettings = userSettings
            )
        }

        item {
            QuickActionButtonsRow(userSettings = userSettings, onQuickAdd = onQuickAdd)
        }

        item {
            AccountsCarouselSection(
                accounts = accounts,
                userSettings = userSettings,
                onNavigateToAccounts = onNavigateToAccounts
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${AppStrings.get("expense_breakdown", lang)}: $monthName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PooleyarDonutChart(
                        slices = categorySlices,
                        totalAmount = overview.monthlyExpense,
                        currency = userSettings.currency
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MonthlyComparisonBarChart(
                        income = overview.monthlyIncome,
                        expense = overview.monthlyExpense,
                        currency = userSettings.currency
                    )
                }
            }
        }

        item {
            DebtsAndChecksSummaryCard(overview = overview, userSettings = userSettings)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.get("recent_transactions", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text(AppStrings.get("view_all", lang), fontWeight = FontWeight.Bold)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(AppStrings.get("no_transactions_yet", lang), fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            items(recentTransactions.take(6)) { detail ->
                TransactionRowItem(detail = detail, userSettings = userSettings)
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun LandscapeTabletDashboard(
    overview: FinancialOverview,
    accounts: List<AccountWithBalance>,
    recentTransactions: List<TransactionDetail>,
    categorySlices: List<ChartSlice>,
    userSettings: UserSettings,
    monthName: String,
    selectedJalaliYear: Int,
    selectedJalaliMonth: Int,
    onQuickAdd: (TransactionType) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onMonthChange: (Int, Int) -> Unit
) {
    val lang = userSettings.language

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("landscape_tablet_dashboard"),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column (60% width): Hero Overview, Charts, Accounts
        LazyColumn(
            modifier = Modifier.weight(1.2f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MonthNavigatorHeader(
                    selectedYear = selectedJalaliYear,
                    selectedMonth = selectedJalaliMonth,
                    monthName = monthName,
                    userSettings = userSettings,
                    onMonthChange = onMonthChange
                )
            }
            item {
                TotalBalanceHeroCard(
                    totalBalance = overview.totalBalance,
                    monthlyIncome = overview.monthlyIncome,
                    monthlyExpense = overview.monthlyExpense,
                    userSettings = userSettings
                )
            }
            item {
                QuickActionButtonsRow(userSettings = userSettings, onQuickAdd = onQuickAdd)
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${AppStrings.get("expense_breakdown", lang)}: $monthName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PooleyarDonutChart(
                            slices = categorySlices,
                            totalAmount = overview.monthlyExpense,
                            currency = userSettings.currency
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MonthlyComparisonBarChart(
                            income = overview.monthlyIncome,
                            expense = overview.monthlyExpense,
                            currency = userSettings.currency
                        )
                    }
                }
            }
            item {
                AccountsCarouselSection(
                    accounts = accounts,
                    userSettings = userSettings,
                    onNavigateToAccounts = onNavigateToAccounts
                )
            }
        }

        // Right Column (40% width): Recent Transactions & Obligations
        LazyColumn(
            modifier = Modifier.weight(0.9f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DebtsAndChecksSummaryCard(overview = overview, userSettings = userSettings)
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("recent_transactions", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text(AppStrings.get("view_all", lang), fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (recentTransactions.isEmpty()) {
                item {
                    Text(
                        AppStrings.get("no_transactions_yet", lang),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                items(recentTransactions.take(8)) { detail ->
                    TransactionRowItem(detail = detail, userSettings = userSettings)
                }
            }
        }
    }
}

@Composable
private fun MonthNavigatorHeader(
    selectedYear: Int,
    selectedMonth: Int,
    monthName: String,
    userSettings: UserSettings,
    onMonthChange: (Int, Int) -> Unit
) {
    val yearStr = if (userSettings.digitFormat.code == "persian") selectedYear.toString().toPersianDigits() else selectedYear.toString()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                var newMonth = selectedMonth - 1
                var newYear = selectedYear
                if (newMonth < 1) {
                    newMonth = 12
                    newYear--
                }
                onMonthChange(newYear, newMonth)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Previous Month")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$monthName $yearStr",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = {
                var newMonth = selectedMonth + 1
                var newYear = selectedYear
                if (newMonth > 12) {
                    newMonth = 1
                    newYear++
                }
                onMonthChange(newYear, newMonth)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Next Month")
            }
        }
    }
}

@Composable
private fun TotalBalanceHeroCard(
    totalBalance: Long,
    monthlyIncome: Long,
    monthlyExpense: Long,
    userSettings: UserSettings
) {
    val lang = userSettings.language
    val pDigits = userSettings.digitFormat.code == "persian"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = AppStrings.get("total_net_worth", lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = CurrencyFormatter.formatAmount(totalBalance, userSettings.currency, persianDigits = pDigits),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PersianGreenIncome.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = PersianGreenIncome, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(AppStrings.get("monthly_income", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyFormatter.formatAmount(monthlyIncome, userSettings.currency, persianDigits = pDigits),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PersianGreenIncome
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Expense Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PersianRoseExpense.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = PersianRoseExpense, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(AppStrings.get("monthly_expense", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                CurrencyFormatter.formatAmount(monthlyExpense, userSettings.currency, persianDigits = pDigits),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PersianRoseExpense
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButtonsRow(
    userSettings: UserSettings,
    onQuickAdd: (TransactionType) -> Unit
) {
    val lang = userSettings.language
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = { onQuickAdd(TransactionType.EXPENSE) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PersianRoseExpense),
            modifier = Modifier.weight(1f).testTag("quick_add_expense_btn")
        ) {
            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(AppStrings.get("expense", lang), fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { onQuickAdd(TransactionType.INCOME) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PersianGreenIncome),
            modifier = Modifier.weight(1f).testTag("quick_add_income_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(AppStrings.get("income", lang), fontWeight = FontWeight.Bold)
        }

        FilledTonalButton(
            onClick = { onQuickAdd(TransactionType.TRANSFER) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(0.9f).testTag("quick_add_transfer_btn")
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(AppStrings.get("transfer", lang), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AccountsCarouselSection(
    accounts: List<AccountWithBalance>,
    userSettings: UserSettings,
    onNavigateToAccounts: () -> Unit
) {
    val lang = userSettings.language
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(AppStrings.get("accounts", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onNavigateToAccounts) {
                Text(AppStrings.get("view_all", lang), fontWeight = FontWeight.Bold)
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts) { accWithBal ->
                AccountMiniCard(accWithBal = accWithBal, userSettings = userSettings)
            }
        }
    }
}

@Composable
private fun AccountMiniCard(accWithBal: AccountWithBalance, userSettings: UserSettings) {
    val pDigits = userSettings.digitFormat.code == "persian"
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(accWithBal.account.colorHex)),
        modifier = Modifier
            .width(190.dp)
            .height(105.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = accWithBal.account.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            }

            Column {
                Text(
                    text = CurrencyFormatter.formatAmount(accWithBal.calculatedBalance, userSettings.currency, persianDigits = pDigits),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DebtsAndChecksSummaryCard(overview: FinancialOverview, userSettings: UserSettings) {
    val lang = userSettings.language
    val pDigits = userSettings.digitFormat.code == "persian"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(AppStrings.get("debts_checks", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(if (lang.code == "fa") "بدهی من" else "I Owe", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            CurrencyFormatter.formatAmount(overview.totalDebtIOwe, userSettings.currency, persianDigits = pDigits),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PersianRoseExpense
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(if (lang.code == "fa") "طلب من" else "Owed to Me", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            CurrencyFormatter.formatAmount(overview.totalOwedToMe, userSettings.currency, persianDigits = pDigits),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PersianGreenIncome
                        )
                    }
                }
            }
        }
    }
}
