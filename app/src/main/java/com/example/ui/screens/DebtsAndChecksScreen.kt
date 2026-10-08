package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.CheckEntity
import com.example.data.entity.DebtEntity
import com.example.model.*
import com.example.repository.AccountWithBalance
import com.example.ui.components.JalaliDatePickerDialog
import com.example.ui.theme.PersianGreenIncome
import com.example.ui.theme.PersianRoseExpense
import com.example.util.AppDateTimeFormatter
import com.example.util.AppStrings
import com.example.util.CurrencyFormatter
import com.example.util.JalaliDate
import com.example.util.WindowSizeInfo

@Composable
fun DebtsAndChecksScreen(
    debts: List<DebtEntity>,
    checks: List<CheckEntity>,
    accounts: List<AccountWithBalance>,
    userSettings: UserSettings,
    windowSizeInfo: WindowSizeInfo,
    onSaveDebt: (DebtEntity) -> Unit,
    onRecordDebtPayment: (DebtEntity, Long, Long) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit,
    onSaveCheck: (CheckEntity) -> Unit,
    onClearCheck: (CheckEntity, Long) -> Unit,
    onDeleteCheck: (CheckEntity) -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<DebtEntity?>(null) }

    var showAddCheckDialog by remember { mutableStateOf(false) }
    var checkToEdit by remember { mutableStateOf<CheckEntity?>(null) }

    var debtToPay by remember { mutableStateOf<DebtEntity?>(null) }
    var checkToClear by remember { mutableStateOf<CheckEntity?>(null) }

    val lang = userSettings.language
    val pDigits = userSettings.digitFormat.code == "persian"

    if (showAddDebtDialog || debtToEdit != null) {
        AddOrEditDebtDialog(
            debtToEdit = debtToEdit,
            userSettings = userSettings,
            onDismissRequest = {
                showAddDebtDialog = false
                debtToEdit = null
            },
            onConfirm = {
                onSaveDebt(it)
                showAddDebtDialog = false
                debtToEdit = null
            }
        )
    }

    if (showAddCheckDialog || checkToEdit != null) {
        AddOrEditCheckDialog(
            checkToEdit = checkToEdit,
            userSettings = userSettings,
            onDismissRequest = {
                showAddCheckDialog = false
                checkToEdit = null
            },
            onConfirm = {
                onSaveCheck(it)
                showAddCheckDialog = false
                checkToEdit = null
            }
        )
    }

    // Repayment Dialog
    if (debtToPay != null) {
        val debt = debtToPay!!
        val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0L)
        var paymentInput by remember { mutableStateOf(remaining.toString()) }
        var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.account?.id ?: 0L) }

        AlertDialog(
            onDismissRequest = { debtToPay = null },
            title = { Text(AppStrings.get("pay_repay", lang), fontWeight = FontWeight.Bold) },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = CurrencyFormatter.parseAmount(paymentInput, userSettings.currency)
                        if (amt > 0 && selectedAccountId != 0L) {
                            onRecordDebtPayment(debt, amt, selectedAccountId)
                            debtToPay = null
                        }
                    }
                ) {
                    Text(AppStrings.get("confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToPay = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${debt.personName}")
                    Text("${AppStrings.get("remaining_amount", lang)}: ${CurrencyFormatter.formatAmount(remaining, userSettings.currency, persianDigits = pDigits)}")
                    OutlinedTextField(
                        value = paymentInput,
                        onValueChange = { paymentInput = it },
                        label = { Text("${AppStrings.get("amount", lang)} (${userSettings.currency.titleFa})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(AppStrings.get("source_account", lang) + ":")
                    accounts.forEach { accWithBal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountId = accWithBal.account.id }
                        ) {
                            RadioButton(
                                selected = selectedAccountId == accWithBal.account.id,
                                onClick = { selectedAccountId = accWithBal.account.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(accWithBal.account.name)
                        }
                    }
                }
            }
        )
    }

    // Check Clear Dialog
    if (checkToClear != null) {
        val chk = checkToClear!!
        var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.account?.id ?: 0L) }

        AlertDialog(
            onDismissRequest = { checkToClear = null },
            title = { Text(AppStrings.get("clear_check", lang), fontWeight = FontWeight.Bold) },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedAccountId != 0L) {
                            onClearCheck(chk, selectedAccountId)
                            checkToClear = null
                        }
                    }
                ) {
                    Text(AppStrings.get("confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { checkToClear = null }) {
                    Text(AppStrings.get("cancel", lang))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${AppStrings.get("check_number", lang)}: ${chk.checkNumber}")
                    Text("${AppStrings.get("amount", lang)}: ${CurrencyFormatter.formatAmount(chk.amount, userSettings.currency, persianDigits = pDigits)}")
                    Text(AppStrings.get("source_account", lang) + ":")
                    accounts.forEach { accWithBal ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAccountId = accWithBal.account.id }
                        ) {
                            RadioButton(
                                selected = selectedAccountId == accWithBal.account.id,
                                onClick = { selectedAccountId = accWithBal.account.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(accWithBal.account.name)
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedSection == 0) {
                        debtToEdit = null
                        showAddDebtDialog = true
                    } else {
                        checkToEdit = null
                        showAddCheckDialog = true
                    }
                },
                modifier = Modifier.testTag("debts_checks_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabRow(selectedTabIndex = selectedSection) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text(if (lang == AppLanguage.PERSIAN) "طلب و بدهی اشخاص" else "Debts & Receivables", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text(if (lang == AppLanguage.PERSIAN) "مدیریت چک‌های صیادی" else "Checks", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedSection == 0) {
                if (debts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(AppStrings.get("no_debts_yet", lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(debts, key = { it.id }) { debt ->
                            val isIOwe = debt.direction == DebtDirection.I_OWE.name
                            val remaining = (debt.totalAmount - debt.paidAmount).coerceAtLeast(0L)
                            val isPaid = debt.status == DebtStatus.PAID.name

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
                                            Text(debt.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (isIOwe) AppStrings.get("i_owe", lang) else AppStrings.get("owed_to_me", lang),
                                                fontSize = 11.sp,
                                                color = if (isIOwe) PersianRoseExpense else PersianGreenIncome
                                            )
                                        }

                                        Row {
                                            IconButton(onClick = { debtToEdit = debt }) {
                                                Icon(Icons.Default.Edit, contentDescription = AppStrings.get("edit", lang), tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { onDeleteDebt(debt) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = AppStrings.get("delete", lang), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${AppStrings.get("total_amount", lang)}: ${CurrencyFormatter.formatAmount(debt.totalAmount, userSettings.currency, persianDigits = pDigits)}", fontSize = 12.sp)
                                        Text(
                                            "${AppStrings.get("remaining_amount", lang)}: ${CurrencyFormatter.formatAmount(remaining, userSettings.currency, persianDigits = pDigits)}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPaid) PersianGreenIncome else MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (!isPaid) {
                                        Button(
                                            onClick = { debtToPay = debt },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(AppStrings.get("pay_repay", lang))
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PersianGreenIncome.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PersianGreenIncome, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(AppStrings.get("fully_settled", lang), color = PersianGreenIncome, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (checks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(AppStrings.get("no_checks_yet", lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(checks, key = { it.id }) { chk ->
                            val isIncoming = chk.direction == CheckDirection.INCOMING.name
                            val isCleared = chk.status == CheckStatus.CLEARED.name

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
                                            Text("${AppStrings.get("check_number", lang)}: ${chk.checkNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (isIncoming) "${AppStrings.get("check_incoming", lang)} (${chk.issuerName})" else "${AppStrings.get("check_outgoing", lang)} (${chk.recipientName})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = { checkToEdit = chk }) {
                                                Icon(Icons.Default.Edit, contentDescription = AppStrings.get("edit", lang), tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { onDeleteCheck(chk) }) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = AppStrings.get("delete", lang), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val dueDateFormatted = AppDateTimeFormatter.formatDate(
                                            epochMillis = chk.dueDateEpochMillis,
                                            calendarType = userSettings.calendarType,
                                            pattern = userSettings.dateFormatPattern,
                                            digitFormat = userSettings.digitFormat,
                                            language = userSettings.language
                                        )
                                        Text("${AppStrings.get("due_date", lang)}: $dueDateFormatted", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            CurrencyFormatter.formatAmount(chk.amount, userSettings.currency, persianDigits = pDigits),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isIncoming) PersianGreenIncome else PersianRoseExpense
                                        )
                                    }

                                    if (!isCleared) {
                                        Button(
                                            onClick = { checkToClear = chk },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(AppStrings.get("clear_check", lang))
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PersianGreenIncome.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PersianGreenIncome, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(AppStrings.get("cleared", lang), color = PersianGreenIncome, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddOrEditDebtDialog(
    debtToEdit: DebtEntity?,
    userSettings: UserSettings,
    onDismissRequest: () -> Unit,
    onConfirm: (DebtEntity) -> Unit
) {
    val lang = userSettings.language
    val isEdit = debtToEdit != null
    var name by remember { mutableStateOf(debtToEdit?.personName ?: "") }
    var direction by remember {
        mutableStateOf(debtToEdit?.direction?.let { DebtDirection.valueOf(it) } ?: DebtDirection.I_OWE)
    }
    var amountInput by remember { mutableStateOf(debtToEdit?.totalAmount?.toString() ?: "") }
    var description by remember { mutableStateOf(debtToEdit?.description ?: "") }
    var dueDate by remember {
        mutableStateOf(debtToEdit?.dueDateEpochMillis?.let { JalaliDate.fromTimestamp(it) })
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = {
                dueDate = it
                showDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(if (isEdit) AppStrings.get("edit_debt", lang) else AppStrings.get("add_debt", lang), fontWeight = FontWeight.Bold)
        },
        confirmButton = {
            Button(onClick = {
                val amt = CurrencyFormatter.parseAmount(amountInput, userSettings.currency)
                if (name.isNotBlank() && amt > 0) {
                    val nowJ = JalaliDate.now()
                    val dueEpoch = dueDate?.let { JalaliDate.toTimestamp(it.year, it.month, it.day) }
                    val debt = DebtEntity(
                        id = debtToEdit?.id ?: 0L,
                        personName = name.trim(),
                        direction = direction.name,
                        totalAmount = amt,
                        paidAmount = debtToEdit?.paidAmount ?: 0L,
                        dateEpochMillis = debtToEdit?.dateEpochMillis ?: System.currentTimeMillis(),
                        dueDateEpochMillis = dueEpoch,
                        jalaliYear = debtToEdit?.jalaliYear ?: nowJ.year,
                        jalaliMonth = debtToEdit?.jalaliMonth ?: nowJ.month,
                        jalaliDay = debtToEdit?.jalaliDay ?: nowJ.day,
                        status = debtToEdit?.status ?: DebtStatus.ACTIVE.name,
                        description = description.trim()
                    )
                    onConfirm(debt)
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = direction == DebtDirection.I_OWE,
                        onClick = { direction = DebtDirection.I_OWE },
                        label = { Text(AppStrings.get("i_owe", lang)) }
                    )
                    FilterChip(
                        selected = direction == DebtDirection.OWED_TO_ME,
                        onClick = { direction = DebtDirection.OWED_TO_ME },
                        label = { Text(AppStrings.get("owed_to_me", lang)) }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.get("contact_person", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("${AppStrings.get("amount", lang)} (${userSettings.currency.titleFa})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(dueDate?.let { "${AppStrings.get("due_date", lang)}: ${it.formatPretty()}" } ?: AppStrings.get("due_date", lang))
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(AppStrings.get("description", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun AddOrEditCheckDialog(
    checkToEdit: CheckEntity?,
    userSettings: UserSettings,
    onDismissRequest: () -> Unit,
    onConfirm: (CheckEntity) -> Unit
) {
    val lang = userSettings.language
    val isEdit = checkToEdit != null
    var checkNumber by remember { mutableStateOf(checkToEdit?.checkNumber ?: "") }
    var direction by remember {
        mutableStateOf(checkToEdit?.direction?.let { CheckDirection.valueOf(it) } ?: CheckDirection.INCOMING)
    }
    var partyName by remember {
        mutableStateOf(
            if (checkToEdit != null) {
                if (checkToEdit.direction == CheckDirection.INCOMING.name) checkToEdit.issuerName else checkToEdit.recipientName
            } else ""
        )
    }
    var amountInput by remember { mutableStateOf(checkToEdit?.amount?.toString() ?: "") }
    var dueDate by remember {
        mutableStateOf(
            if (checkToEdit != null) JalaliDate(checkToEdit.dueJalaliYear, checkToEdit.dueJalaliMonth, checkToEdit.dueJalaliDay) else JalaliDate.now()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        JalaliDatePickerDialog(
            initialDate = dueDate,
            onDismissRequest = { showDatePicker = false },
            onDateSelected = {
                dueDate = it
                showDatePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(if (isEdit) AppStrings.get("edit_check", lang) else AppStrings.get("add_check", lang), fontWeight = FontWeight.Bold)
        },
        confirmButton = {
            Button(onClick = {
                val amt = CurrencyFormatter.parseAmount(amountInput, userSettings.currency)
                if (checkNumber.isNotBlank() && amt > 0) {
                    val dueEpoch = JalaliDate.toTimestamp(dueDate.year, dueDate.month, dueDate.day)
                    val chk = CheckEntity(
                        id = checkToEdit?.id ?: 0L,
                        checkNumber = checkNumber.trim(),
                        direction = direction.name,
                        amount = amt,
                        bankId = checkToEdit?.bankId,
                        accountId = checkToEdit?.accountId,
                        issuerName = if (direction == CheckDirection.INCOMING) partyName.trim() else "من",
                        recipientName = if (direction == CheckDirection.OUTGOING) partyName.trim() else "من",
                        issueDateEpochMillis = checkToEdit?.issueDateEpochMillis ?: System.currentTimeMillis(),
                        dueDateEpochMillis = dueEpoch,
                        dueJalaliYear = dueDate.year,
                        dueJalaliMonth = dueDate.month,
                        dueJalaliDay = dueDate.day,
                        status = checkToEdit?.status ?: CheckStatus.RECEIVED_OR_ISSUED.name,
                        description = checkToEdit?.description ?: ""
                    )
                    onConfirm(chk)
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = direction == CheckDirection.INCOMING,
                        onClick = { direction = CheckDirection.INCOMING },
                        label = { Text(AppStrings.get("check_incoming", lang)) }
                    )
                    FilterChip(
                        selected = direction == CheckDirection.OUTGOING,
                        onClick = { direction = CheckDirection.OUTGOING },
                        label = { Text(AppStrings.get("check_outgoing", lang)) }
                    )
                }

                OutlinedTextField(
                    value = checkNumber,
                    onValueChange = { checkNumber = it },
                    label = { Text(AppStrings.get("check_number", lang)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(if (direction == CheckDirection.INCOMING) "صادرکننده / Issuer" else "گیرنده / Recipient") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("${AppStrings.get("amount", lang)} (${userSettings.currency.titleFa})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${AppStrings.get("due_date", lang)}: ${dueDate.formatPretty()}")
                }
            }
        }
    )
}
