package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BudgetEntity
import com.example.data.entity.CategoryEntity
import com.example.model.AppLanguage
import com.example.model.UserSettings
import com.example.repository.TransactionDetail
import com.example.ui.theme.PersianGreenIncome
import com.example.ui.theme.PersianRoseExpense
import com.example.util.AppDateTimeFormatter
import com.example.util.AppStrings
import com.example.util.CurrencyFormatter
import com.example.util.WindowSizeInfo
import com.example.util.toPersianDigits

@Composable
fun BudgetsScreen(
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionDetail>,
    userSettings: UserSettings,
    selectedJalaliYear: Int,
    selectedJalaliMonth: Int,
    windowSizeInfo: WindowSizeInfo,
    onSaveBudget: (Long, Long, Long, Int, Int) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit
) {
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetEntity?>(null) }
    val lang = userSettings.language
    val pDigits = userSettings.digitFormat.code == "persian"

    val monthName = AppDateTimeFormatter.getMonthName(
        monthIndex = selectedJalaliMonth,
        calendarType = userSettings.calendarType,
        language = lang
    )

    val expenseCategories = remember(categories) {
        categories.filter { it.type == "EXPENSE" }
    }

    if (showAddBudgetDialog || budgetToEdit != null) {
        AddOrEditBudgetDialog(
            budgetToEdit = budgetToEdit,
            expenseCategories = expenseCategories,
            userSettings = userSettings,
            year = selectedJalaliYear,
            month = selectedJalaliMonth,
            onDismissRequest = {
                showAddBudgetDialog = false
                budgetToEdit = null
            },
            onConfirm = { catId, limit ->
                onSaveBudget(budgetToEdit?.id ?: 0L, catId, limit, selectedJalaliYear, selectedJalaliMonth)
                showAddBudgetDialog = false
                budgetToEdit = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    budgetToEdit = null
                    showAddBudgetDialog = true
                },
                modifier = Modifier.testTag("budgets_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.get("add_budget", lang))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            item {
                Text(
                    text = "${AppStrings.get("budgets", lang)}: $monthName ${if (pDigits) selectedJalaliYear.toString().toPersianDigits() else selectedJalaliYear}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "سقف بودجه برای دسته‌بندی‌ها تعیین کنید و کنترل هزینه‌ها را در دست بگیرید" else "Set spending limits for categories and prevent budget overruns",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (budgets.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(AppStrings.get("no_budgets_yet", lang), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { showAddBudgetDialog = true }) {
                                Text(AppStrings.get("add_budget", lang))
                            }
                        }
                    }
                }
            } else {
                items(budgets, key = { it.id }) { budget ->
                    val cat = categories.firstOrNull { it.id == budget.categoryId }
                    val spentInCat = transactions
                        .filter {
                            it.transaction.type == "EXPENSE" &&
                            it.transaction.categoryId == budget.categoryId &&
                            it.transaction.jalaliYear == selectedJalaliYear &&
                            it.transaction.jalaliMonth == selectedJalaliMonth
                        }
                        .sumOf { it.transaction.amount }

                    val percent = if (budget.monthlyLimit > 0) ((spentInCat.toDouble() / budget.monthlyLimit.toDouble()) * 100).toInt() else 0
                    val isExceeded = spentInCat > budget.monthlyLimit
                    val remaining = (budget.monthlyLimit - spentInCat).coerceAtLeast(0L)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat?.name ?: AppStrings.get("category", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(onClick = { budgetToEdit = budget }) {
                                        Icon(Icons.Default.Edit, contentDescription = AppStrings.get("edit", lang), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { onDeleteBudget(budget) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = AppStrings.get("delete", lang), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${AppStrings.get("budget_spent", lang)}: ${CurrencyFormatter.formatAmount(spentInCat, userSettings.currency, persianDigits = pDigits)}", fontSize = 12.sp)
                                Text("${AppStrings.get("budget_limit", lang)}: ${CurrencyFormatter.formatAmount(budget.monthlyLimit, userSettings.currency, persianDigits = pDigits)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            LinearProgressIndicator(
                                progress = { (spentInCat.toFloat() / budget.monthlyLimit.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = if (isExceeded) PersianRoseExpense else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isExceeded) "⚠️ ${AppStrings.get("budget_exceeded", lang)}" else "${AppStrings.get("budget_remaining", lang)}: ${CurrencyFormatter.formatAmount(remaining, userSettings.currency, persianDigits = pDigits)}",
                                    fontSize = 11.sp,
                                    color = if (isExceeded) PersianRoseExpense else PersianGreenIncome,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${if (pDigits) percent.toString().toPersianDigits() else percent}٪",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrEditBudgetDialog(
    budgetToEdit: BudgetEntity?,
    expenseCategories: List<CategoryEntity>,
    userSettings: UserSettings,
    year: Int,
    month: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val lang = userSettings.language
    val isEdit = budgetToEdit != null
    var selectedCategoryId by remember { mutableStateOf(budgetToEdit?.categoryId ?: expenseCategories.firstOrNull()?.id ?: 0L) }
    var limitInput by remember {
        mutableStateOf(budgetToEdit?.let { CurrencyFormatter.amountForInput(it.monthlyLimit, userSettings.currency) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(if (isEdit) AppStrings.get("edit_budget", lang) else AppStrings.get("add_budget", lang), fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(onClick = {
                val limit = CurrencyFormatter.parseAmount(limitInput, userSettings.currency)
                if (selectedCategoryId != 0L && limit > 0) {
                    onConfirm(selectedCategoryId, limit)
                }
            }) {
                Text(AppStrings.get("confirm", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(AppStrings.get("cancel", lang)) }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(AppStrings.get("category", lang) + ":")
                expenseCategories.take(8).forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategoryId = cat.id }
                    ) {
                        RadioButton(selected = selectedCategoryId == cat.id, onClick = { selectedCategoryId = cat.id })
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(cat.name)
                    }
                }

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("${AppStrings.get("budget_limit", lang)} (${userSettings.currency.titleFa})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
