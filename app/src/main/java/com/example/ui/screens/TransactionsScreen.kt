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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CategoryEntity
import com.example.data.entity.TransactionEntity
import com.example.model.TransactionType
import com.example.model.UserSettings
import com.example.repository.AccountWithBalance
import com.example.repository.TransactionDetail
import com.example.ui.components.TransactionRowItem
import com.example.util.AppDateTimeFormatter
import com.example.util.AppStrings
import com.example.util.CurrencyFormatter
import com.example.util.WindowSizeInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionDetail>,
    accounts: List<AccountWithBalance>,
    categories: List<CategoryEntity>,
    userSettings: UserSettings,
    windowSizeInfo: WindowSizeInfo,
    onEditTransaction: (TransactionDetail) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onQuickAdd: () -> Unit
) {
    val lang = userSettings.language
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<String?>(null) }
    var selectedAccountFilter by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<Long?>(null) }
    var selectedDetailItem by remember { mutableStateOf<TransactionDetail?>(null) }

    val filteredTransactions = remember(
        transactions,
        searchQuery,
        selectedTypeFilter,
        selectedAccountFilter,
        selectedCategoryFilter
    ) {
        transactions.filter { detail ->
            val tx = detail.transaction
            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchesAccount = selectedAccountFilter == null || tx.accountId == selectedAccountFilter || tx.destinationAccountId == selectedAccountFilter
            val matchesCategory = selectedCategoryFilter == null || tx.categoryId == selectedCategoryFilter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim()
                tx.description.contains(q, ignoreCase = true) ||
                tx.contactPerson.contains(q, ignoreCase = true) ||
                (detail.category?.name?.contains(q, ignoreCase = true) == true) ||
                (detail.sourceAccount?.name?.contains(q, ignoreCase = true) == true)
            }
            matchesType && matchesAccount && matchesCategory && matchesSearch
        }
    }

    if (selectedDetailItem != null) {
        val detail = selectedDetailItem!!
        val tx = detail.transaction
        AlertDialog(
            onDismissRequest = { selectedDetailItem = null },
            confirmButton = {
                Button(
                    onClick = {
                        val toEdit = detail
                        selectedDetailItem = null
                        onEditTransaction(toEdit)
                    }
                ) {
                    Text(AppStrings.get("edit", lang))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(tx)
                        selectedDetailItem = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(AppStrings.get("delete", lang))
                }
            },
            title = {
                Text(AppStrings.get("transactions", lang), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${AppStrings.get("amount", lang)}: ${
                            CurrencyFormatter.formatAmount(
                                tx.amount,
                                userSettings.currency,
                                persianDigits = userSettings.digitFormat.code == "persian"
                            )
                        }",
                        fontWeight = FontWeight.Bold
                    )
                    Text("نوع: ${when (tx.type) { TransactionType.EXPENSE.name -> AppStrings.get("expense", lang); TransactionType.INCOME.name -> AppStrings.get("income", lang); else -> AppStrings.get("transfer", lang) }}")
                    detail.sourceAccount?.let { Text("${AppStrings.get("source_account", lang)}: ${it.name}") }
                    detail.destinationAccount?.let { Text("${AppStrings.get("dest_account", lang)}: ${it.name}") }
                    detail.category?.let { Text("${AppStrings.get("category", lang)}: ${it.name}") }
                    Text(
                        text = "${AppStrings.get("date", lang)}: ${
                            AppDateTimeFormatter.formatDate(
                                epochMillis = tx.dateEpochMillis,
                                calendarType = userSettings.calendarType,
                                pattern = userSettings.dateFormatPattern,
                                digitFormat = userSettings.digitFormat,
                                language = userSettings.language
                            )
                        }"
                    )
                    if (tx.description.isNotBlank()) Text("${AppStrings.get("description", lang)}: ${tx.description}")
                    if (tx.contactPerson.isNotBlank()) Text("${AppStrings.get("contact_person", lang)}: ${tx.contactPerson}")
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onQuickAdd,
                modifier = Modifier.testTag("transactions_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.get("quick_add", lang))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input"),
                placeholder = { Text(AppStrings.get("search_hint", lang)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text(if (lang.code == "fa") "همه" else "All") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.EXPENSE.name,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE.name) null else TransactionType.EXPENSE.name
                        },
                        label = { Text(AppStrings.get("expense", lang)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.INCOME.name,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME.name) null else TransactionType.INCOME.name
                        },
                        label = { Text(AppStrings.get("income", lang)) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedTypeFilter == TransactionType.TRANSFER.name,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == TransactionType.TRANSFER.name) null else TransactionType.TRANSFER.name
                        },
                        label = { Text(AppStrings.get("transfer", lang)) }
                    )
                }
            }

            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("empty_transactions_state"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = AppStrings.get("no_transactions_yet", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("transactions_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    items(filteredTransactions, key = { it.transaction.id }) { detail ->
                        TransactionRowItem(
                            detail = detail,
                            userSettings = userSettings,
                            onClick = { selectedDetailItem = detail }
                        )
                    }
                }
            }
        }
    }
}
