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
import androidx.compose.material.icons.filled.Repeat
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
import com.example.data.entity.CategoryEntity
import com.example.data.entity.RecurringTransactionEntity
import com.example.model.RecurringFrequency
import com.example.model.TransactionType
import com.example.model.UserSettings
import com.example.repository.AccountWithBalance
import com.example.ui.theme.PersianGreenIncome
import com.example.ui.theme.PersianRoseExpense
import com.example.util.Currency
import com.example.util.CurrencyFormatter
import com.example.util.WindowSizeInfo
import com.example.util.toPersianDigits

@Composable
fun RecurringTransactionsScreen(
    recurringList: List<RecurringTransactionEntity>,
    accounts: List<AccountWithBalance>,
    categories: List<CategoryEntity>,
    userSettings: UserSettings,
    windowSizeInfo: WindowSizeInfo,
    onSaveRecurring: (RecurringTransactionEntity) -> Unit,
    onDeleteRecurring: (RecurringTransactionEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val lang = userSettings.language
    val pDigits = userSettings.digitFormat.code == "persian"

    if (showAddDialog) {
        AddRecurringDialog(
            accounts = accounts,
            categories = categories,
            userSettings = userSettings,
            onDismissRequest = { showAddDialog = false },
            onConfirm = {
                onSaveRecurring(it)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("recurring_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن دوره مالی جدید")
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
                    text = if (lang.code == "fa") "تراکنش‌های دوره‌ای و تکرارشونده" else "Recurring Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lang.code == "fa") "حقوق، اجاره خانه، قبوض و اشتراک‌ها را تعریف کنید تا در موعد مقرر خودکار ثبت شوند" else "Automate your salary, rent, bills and subscriptions deterministically",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (recurringList.isEmpty()) {
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
                            Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                if (lang.code == "fa") "هیچ تراکنش دوره‌ای تعریف نشده است" else "No recurring transactions yet",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text(if (lang.code == "fa") "+ افزودن تراکنش دوره‌ای" else "+ Add Recurring")
                            }
                        }
                    }
                }
            } else {
                items(recurringList, key = { it.id }) { rec ->
                    val acc = accounts.firstOrNull { it.account.id == rec.accountId }?.account
                    val cat = categories.firstOrNull { it.id == rec.categoryId }
                    val isIncome = rec.type == TransactionType.INCOME.name

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
                                Column {
                                    Text(rec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    val freqTitle = RecurringFrequency.entries.firstOrNull { it.name == rec.frequency }?.titleFa ?: rec.frequency
                                    Text(
                                        text = "$freqTitle • روز ${if (pDigits) rec.dayOfInterval.toString().toPersianDigits() else rec.dayOfInterval}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteRecurring(rec) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${cat?.name ?: ""} (${acc?.name ?: ""})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = (if (isIncome) "+ " else "- ") + CurrencyFormatter.formatAmount(rec.amount, userSettings.currency, persianDigits = pDigits),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) PersianGreenIncome else PersianRoseExpense
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
private fun AddRecurringDialog(
    accounts: List<AccountWithBalance>,
    categories: List<CategoryEntity>,
    userSettings: UserSettings,
    onDismissRequest: () -> Unit,
    onConfirm: (RecurringTransactionEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountInput by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.account?.id ?: 0L) }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0L) }
    var selectedFrequency by remember { mutableStateOf(RecurringFrequency.MONTHLY) }
    var dayIntervalInput by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("تعریف تراکنش دوره‌ای جدید", fontWeight = FontWeight.Bold) },
        confirmButton = {
            Button(onClick = {
                val amt = CurrencyFormatter.parseAmount(amountInput, userSettings.currency)
                val day = dayIntervalInput.toIntOrNull() ?: 1
                if (title.isNotBlank() && amt > 0 && selectedAccountId != 0L) {
                    val rec = RecurringTransactionEntity(
                        title = title.trim(),
                        type = selectedType.name,
                        amount = amt,
                        accountId = selectedAccountId,
                        categoryId = selectedCategoryId,
                        frequency = selectedFrequency.name,
                        dayOfInterval = day
                    )
                    onConfirm(rec)
                }
            }) {
                Text("ثبت دوره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("انصراف") }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان (مثلاً اجاره خانه، واریز حقوق)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == TransactionType.EXPENSE,
                        onClick = { selectedType = TransactionType.EXPENSE },
                        label = { Text("هزینه دوره‌ای") }
                    )
                    FilterChip(
                        selected = selectedType == TransactionType.INCOME,
                        onClick = { selectedType = TransactionType.INCOME },
                        label = { Text("درآمد دوره‌ای") }
                    )
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("مبلغ هر دوره (${userSettings.currency.titleFa})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurringFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = selectedFrequency == freq,
                            onClick = { selectedFrequency = freq },
                            label = { Text(freq.titleFa, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = dayIntervalInput,
                    onValueChange = { dayIntervalInput = it },
                    label = { Text("روز موعد در هر ماه (۱ الی ۳۰)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
