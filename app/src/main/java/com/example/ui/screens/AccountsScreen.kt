package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.entity.AccountEntity
import com.example.model.AccountType
import com.example.model.AppLanguage
import com.example.model.IranianBanks
import com.example.model.UserSettings
import com.example.repository.AccountWithBalance
import com.example.util.AppStrings
import com.example.util.CurrencyFormatter
import com.example.util.WindowSizeInfo

@Composable
fun AccountsScreen(
    accounts: List<AccountWithBalance>,
    userSettings: UserSettings,
    windowSizeInfo: WindowSizeInfo,
    onSaveAccount: (AccountEntity, String) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountWithBalance?>(null) }
    val lang = userSettings.language
    val pDigits = userSettings.digitFormat.code == "persian"

    if (showAddDialog || accountToEdit != null) {
        AddOrEditAccountDialog(
            accountToEdit = accountToEdit,
            userSettings = userSettings,
            onDismissRequest = {
                showAddDialog = false
                accountToEdit = null
            },
            onConfirm = { account, cardNum ->
                onSaveAccount(account, cardNum)
                showAddDialog = false
                accountToEdit = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    accountToEdit = null
                    showAddDialog = true
                },
                modifier = Modifier.testTag("accounts_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.get("add_account", lang))
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
                    text = AppStrings.get("accounts", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lang == AppLanguage.PERSIAN) "مدیریت حساب‌های بانکی، کارت‌ها و کیف‌های پول نقدی" else "Manage your bank accounts, credit cards, and cash wallets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (accounts.isEmpty()) {
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
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(AppStrings.get("no_accounts_yet", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(onClick = { showAddDialog = true }) {
                                Text(AppStrings.get("add_account", lang))
                            }
                        }
                    }
                }
            } else {
                items(accounts, key = { it.account.id }) { accWithBal ->
                    AccountCardDetailed(
                        accWithBal = accWithBal,
                        userSettings = userSettings,
                        onEdit = { accountToEdit = accWithBal },
                        onDelete = { onDeleteAccount(accWithBal.account) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCardDetailed(
    accWithBal: AccountWithBalance,
    userSettings: UserSettings,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val acc = accWithBal.account
    val lang = userSettings.language
    val pDigits = userSettings.digitFormat.code == "persian"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(AppStrings.get("delete", lang), fontWeight = FontWeight.Bold) },
            text = { Text(if (lang == AppLanguage.PERSIAN) "آیا از حذف حساب «${acc.name}» اطمینان دارید؟" else "Are you sure you want to delete '${acc.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text(AppStrings.get("delete", lang), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(acc.colorHex),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(acc.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val bankObj = acc.bankId?.let { IranianBanks.findById(it) }
                        val bankName = if (lang == AppLanguage.PERSIAN) bankObj?.nameFa ?: "" else bankObj?.nameEn ?: ""
                        Text(
                            text = if (bankName.isNotBlank()) bankName else when (acc.type) {
                                AccountType.BANK.name -> AppStrings.get("bank_account", lang)
                                AccountType.CASH.name -> AppStrings.get("cash_wallet", lang)
                                else -> AppStrings.get("savings", lang)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = AppStrings.get("edit", lang), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = AppStrings.get("delete", lang), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${AppStrings.get("current_balance", lang)}:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatAmount(accWithBal.calculatedBalance, userSettings.currency, persianDigits = pDigits),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (accWithBal.cards.isNotEmpty()) {
                    val card = accWithBal.cards.first()
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${AppStrings.get("card_number", lang)}:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = card.cardNumber.chunked(4).joinToString(" - "),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (acc.shebaNumber.isNotBlank()) {
                Text(
                    text = "${AppStrings.get("sheba_number", lang)}: ${acc.shebaNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddOrEditAccountDialog(
    accountToEdit: AccountWithBalance?,
    userSettings: UserSettings,
    onDismissRequest: () -> Unit,
    onConfirm: (AccountEntity, String) -> Unit
) {
    val lang = userSettings.language
    val isEdit = accountToEdit != null
    val existing = accountToEdit?.account

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var accountType by remember {
        mutableStateOf(existing?.type?.let { AccountType.valueOf(it) } ?: AccountType.BANK)
    }
    var selectedBankId by remember { mutableStateOf(existing?.bankId ?: "mellat") }
    var initialBalanceInput by remember { mutableStateOf(existing?.initialBalance?.toString() ?: "") }
    var accountNumber by remember { mutableStateOf(existing?.accountNumber ?: "") }
    var shebaNumber by remember { mutableStateOf(existing?.shebaNumber ?: "") }
    var cardNumber by remember { mutableStateOf(accountToEdit?.cards?.firstOrNull()?.cardNumber ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dialog to add a custom bank
    var showAddCustomBankDialog by remember { mutableStateOf(false) }
    var customBankNameInput by remember { mutableStateOf("") }

    if (showAddCustomBankDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomBankDialog = false },
            title = { Text(AppStrings.get("add_custom_bank", lang), fontWeight = FontWeight.Bold) },
            confirmButton = {
                Button(onClick = {
                    if (customBankNameInput.isNotBlank()) {
                        val newBank = IranianBanks.addCustomBank(nameFa = customBankNameInput.trim(), nameEn = customBankNameInput.trim())
                        selectedBankId = newBank.id
                        showAddCustomBankDialog = false
                    }
                }) {
                    Text(AppStrings.get("confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomBankDialog = false }) {
                    Text(AppStrings.get("cancel", lang))
                }
            },
            text = {
                OutlinedTextField(
                    value = customBankNameInput,
                    onValueChange = { customBankNameInput = it },
                    label = { Text(AppStrings.get("bank_title_input", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                if (isEdit) AppStrings.get("edit_account", lang) else AppStrings.get("add_account", lang),
                fontWeight = FontWeight.Bold
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = if (lang == AppLanguage.PERSIAN) "لطفاً نام حساب را وارد کنید" else "Please enter account name"
                        return@Button
                    }
                    val initialBal = CurrencyFormatter.parseAmount(initialBalanceInput, userSettings.currency)
                    val bankObj = IranianBanks.findById(selectedBankId)
                    val acc = AccountEntity(
                        id = existing?.id ?: 0L,
                        name = name.trim(),
                        type = accountType.name,
                        bankId = if (accountType == AccountType.BANK) selectedBankId else null,
                        accountNumber = accountNumber.trim(),
                        shebaNumber = shebaNumber.trim(),
                        initialBalance = if (isEdit) existing!!.initialBalance else initialBal,
                        colorHex = bankObj?.primaryColorHex ?: existing?.colorHex ?: 0xFF0D47A1
                    )
                    onConfirm(acc, cardNumber.trim())
                }
            ) {
                Text(if (isEdit) AppStrings.get("edit", lang) else AppStrings.get("confirm", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(AppStrings.get("cancel", lang))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AccountType.entries.take(2).forEach { type ->
                        FilterChip(
                            selected = accountType == type,
                            onClick = { accountType = type },
                            label = { Text(if (lang == AppLanguage.PERSIAN) type.titleFa else type.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text(AppStrings.get("account_name", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                if (accountType == AccountType.BANK) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(AppStrings.get("select_bank", lang) + ":", style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { showAddCustomBankDialog = true }) {
                            Text("+ " + AppStrings.get("add_custom_bank", lang), fontSize = 11.sp)
                        }
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(IranianBanks.all) { bank ->
                            val bName = if (lang == AppLanguage.PERSIAN) bank.nameFa else bank.nameEn
                            FilterChip(
                                selected = selectedBankId == bank.id,
                                onClick = { selectedBankId = bank.id },
                                label = { Text(bName, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text(AppStrings.get("card_number", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = shebaNumber,
                        onValueChange = { shebaNumber = it },
                        label = { Text(AppStrings.get("sheba_number", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (!isEdit) {
                    OutlinedTextField(
                        value = initialBalanceInput,
                        onValueChange = { initialBalanceInput = it },
                        label = { Text("${AppStrings.get("initial_balance", lang)} (${userSettings.currency.titleFa})") },
                        placeholder = { Text("۰") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        }
    )
}
