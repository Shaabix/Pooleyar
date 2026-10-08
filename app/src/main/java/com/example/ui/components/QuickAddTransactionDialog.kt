package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.model.TransactionType
import com.example.model.UserSettings
import com.example.repository.AccountWithBalance
import com.example.repository.TransactionDetail
import com.example.ui.theme.PersianBlueTransfer
import com.example.ui.theme.PersianGreenIncome
import com.example.ui.theme.PersianRoseExpense
import com.example.util.AppDateTimeFormatter
import com.example.util.AppStrings
import com.example.util.Currency
import com.example.util.CurrencyFormatter
import com.example.util.JalaliDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTransactionDialog(
    initialType: TransactionType = TransactionType.EXPENSE,
    transactionToEdit: TransactionDetail? = null,
    accounts: List<AccountWithBalance>,
    categories: List<CategoryEntity>,
    userSettings: UserSettings,
    onDismissRequest: () -> Unit,
    onSaveTransaction: (
        id: Long,
        type: TransactionType,
        amount: Long,
        accountId: Long,
        destinationAccountId: Long?,
        categoryId: Long?,
        description: String,
        date: JalaliDate,
        contactPerson: String,
        notes: String
    ) -> Unit
) {
    val lang = userSettings.language
    val existingTx = transactionToEdit?.transaction

    var selectedType by remember {
        mutableStateOf(
            existingTx?.let { TransactionType.valueOf(it.type) } ?: initialType
        )
    }
    var amountInput by remember {
        mutableStateOf(existingTx?.let { it.amount.toString() } ?: "")
    }
    var selectedAccountId by remember {
        mutableStateOf(existingTx?.accountId ?: accounts.firstOrNull()?.account?.id ?: 0L)
    }
    var destinationAccountId by remember {
        mutableStateOf(existingTx?.destinationAccountId ?: accounts.getOrNull(1)?.account?.id)
    }
    var selectedCategoryId by remember {
        mutableStateOf<Long?>(existingTx?.categoryId)
    }
    var descriptionInput by remember {
        mutableStateOf(existingTx?.description ?: "")
    }
    var contactPersonInput by remember {
        mutableStateOf(existingTx?.contactPerson ?: "")
    }
    var selectedDate by remember {
        mutableStateOf(
            existingTx?.let { JalaliDate(it.jalaliYear, it.jalaliMonth, it.jalaliDay) } ?: JalaliDate.now()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredCategories = remember(selectedType, categories) {
        categories.filter { it.type == selectedType.name }
    }

    LaunchedEffect(filteredCategories) {
        if (selectedCategoryId == null || filteredCategories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = filteredCategories.firstOrNull()?.id
        }
    }

    val parsedAmount = remember(amountInput, userSettings.currency) {
        CurrencyFormatter.parseAmount(amountInput, userSettings.currency)
    }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialDate = selectedDate,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = {
                selectedDate = it
                showDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .testTag("quick_add_transaction_dialog"),
        confirmButton = {
            Button(
                onClick = {
                    if (parsedAmount <= 0L) {
                        errorMessage = if (lang.code == "fa") "لطفاً مبلغ معتبری وارد کنید" else "Please enter a valid amount"
                        return@Button
                    }
                    if (selectedAccountId == 0L) {
                        errorMessage = if (lang.code == "fa") "لطفاً حساب مبدا را انتخاب کنید" else "Please select a source account"
                        return@Button
                    }
                    if (selectedType == TransactionType.TRANSFER && destinationAccountId == selectedAccountId) {
                        errorMessage = if (lang.code == "fa") "حساب مبدا و مقصد نمی‌تواند یکسان باشد" else "Source and destination accounts must be different"
                        return@Button
                    }

                    onSaveTransaction(
                        existingTx?.id ?: 0L,
                        selectedType,
                        parsedAmount,
                        selectedAccountId,
                        if (selectedType == TransactionType.TRANSFER) destinationAccountId else null,
                        if (selectedType != TransactionType.TRANSFER) selectedCategoryId else null,
                        descriptionInput.trim(),
                        selectedDate,
                        contactPersonInput.trim(),
                        ""
                    )
                    onDismissRequest()
                },
                modifier = Modifier.testTag("quick_add_save_button")
            ) {
                Text(
                    text = if (existingTx != null) AppStrings.get("edit", lang) else AppStrings.get("save_transaction", lang),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(AppStrings.get("cancel", lang))
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTx != null) AppStrings.get("edit", lang) + " " + AppStrings.get("transactions", lang) else AppStrings.get("quick_add", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val formattedDate = AppDateTimeFormatter.formatDate(
                            epochMillis = JalaliDate.toTimestamp(selectedDate.year, selectedDate.month, selectedDate.day),
                            calendarType = userSettings.calendarType,
                            pattern = userSettings.dateFormatPattern,
                            digitFormat = userSettings.digitFormat,
                            language = userSettings.language
                        )
                        Text(formattedDate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Transaction Type Selector (Tabs: Expense / Income / Transfer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        TransactionType.EXPENSE to (AppStrings.get("expense", lang) to PersianRoseExpense),
                        TransactionType.INCOME to (AppStrings.get("income", lang) to PersianGreenIncome),
                        TransactionType.TRANSFER to (AppStrings.get("transfer", lang) to PersianBlueTransfer)
                    ).forEach { (type, pair) ->
                        val (label, color) = pair
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type }
                                .testTag("type_tab_${type.name}"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) color else Color.Transparent
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Amount Field with live formatted helper
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input"),
                    label = { Text("${AppStrings.get("amount", lang)} (${userSettings.currency.titleFa})") },
                    placeholder = { Text("۲,۵۰۰,۰۰۰") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = {
                        if (parsedAmount > 0L) {
                            Text(
                                text = CurrencyFormatter.formatAmount(
                                    amountInToman = parsedAmount,
                                    targetCurrency = userSettings.currency,
                                    persianDigits = userSettings.digitFormat.code == "persian"
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Account Selection
                Text(AppStrings.get("source_account", lang) + ":", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(accounts) { accWithBal ->
                        val isSelected = selectedAccountId == accWithBal.account.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccountId = accWithBal.account.id },
                            label = { Text(accWithBal.account.name) },
                            leadingIcon = {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                // Destination Account if Transfer
                AnimatedVisibility(visible = selectedType == TransactionType.TRANSFER) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(AppStrings.get("dest_account", lang) + ":", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(accounts) { accWithBal ->
                                val isSelected = destinationAccountId == accWithBal.account.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { destinationAccountId = accWithBal.account.id },
                                    label = { Text(accWithBal.account.name) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }

                // Category Selection if not Transfer
                AnimatedVisibility(visible = selectedType != TransactionType.TRANSFER) {
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(AppStrings.get("category", lang) + ":", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCategories) { cat ->
                                val isSelected = selectedCategoryId == cat.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategoryId = cat.id },
                                    label = { Text(cat.name) },
                                    leadingIcon = {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(cat.colorHex),
                                            modifier = Modifier.size(12.dp)
                                        ) {}
                                    }
                                )
                            }
                        }
                    }
                }

                // Description Field
                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("description_input"),
                    label = { Text(AppStrings.get("description", lang)) },
                    singleLine = true
                )

                // Contact Person / Store
                OutlinedTextField(
                    value = contactPersonInput,
                    onValueChange = { contactPersonInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_person_input"),
                    label = { Text(AppStrings.get("contact_person", lang)) },
                    singleLine = true
                )
            }
        }
    )
}
