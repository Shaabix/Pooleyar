package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.repository.FinanceRepository
import com.example.repository.TransactionDetail
import com.example.util.AppStrings
import com.example.util.BackupRestoreManager
import com.example.util.Currency
import com.example.util.WindowSizeInfo
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsAndMoreScreen(
    userSettings: UserSettings,
    transactions: List<TransactionDetail>,
    windowSizeInfo: WindowSizeInfo,
    repository: FinanceRepository,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onCalendarChange: (CalendarType) -> Unit,
    onDigitChange: (DigitFormat) -> Unit,
    onDatePatternChange: (DateFormatPattern) -> Unit,
    onFirstDayChange: (FirstDayOfWeek) -> Unit,
    onTimeFormatChange: (TimeFormat) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onAppLockChange: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lang = userSettings.language

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showDigitDialog by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        OptionSelectionDialog(
            title = AppStrings.get("theme_mode", lang),
            options = ThemeMode.entries.map { it to if (lang == AppLanguage.PERSIAN) it.titleFa else it.titleEn },
            selectedOption = userSettings.themeMode,
            onSelect = {
                onThemeModeChange(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        OptionSelectionDialog(
            title = AppStrings.get("app_language", lang),
            options = AppLanguage.entries.map { it to if (lang == AppLanguage.PERSIAN) it.titleFa else it.titleEn },
            selectedOption = userSettings.language,
            onSelect = {
                onLanguageChange(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showCalendarDialog) {
        OptionSelectionDialog(
            title = AppStrings.get("calendar_system", lang),
            options = CalendarType.entries.map { it to if (lang == AppLanguage.PERSIAN) it.titleFa else it.titleEn },
            selectedOption = userSettings.calendarType,
            onSelect = {
                onCalendarChange(it)
                showCalendarDialog = false
            },
            onDismiss = { showCalendarDialog = false }
        )
    }

    if (showDigitDialog) {
        OptionSelectionDialog(
            title = AppStrings.get("digit_format", lang),
            options = DigitFormat.entries.map { it to if (lang == AppLanguage.PERSIAN) it.titleFa else it.titleEn },
            selectedOption = userSettings.digitFormat,
            onSelect = {
                onDigitChange(it)
                showDigitDialog = false
            },
            onDismiss = { showDigitDialog = false }
        )
    }

    if (showPatternDialog) {
        OptionSelectionDialog(
            title = AppStrings.get("date_format", lang),
            options = DateFormatPattern.entries.map { it to if (lang == AppLanguage.PERSIAN) it.labelFa else it.labelEn },
            selectedOption = userSettings.dateFormatPattern,
            onSelect = {
                onDatePatternChange(it)
                showPatternDialog = false
            },
            onDismiss = { showPatternDialog = false }
        )
    }

    if (showFirstDayDialog) {
        OptionSelectionDialog(
            title = AppStrings.get("first_day_of_week", lang),
            options = FirstDayOfWeek.entries.map { it to if (lang == AppLanguage.PERSIAN) it.titleFa else it.titleEn },
            selectedOption = userSettings.firstDayOfWeek,
            onSelect = {
                onFirstDayChange(it)
                showFirstDayDialog = false
            },
            onDismiss = { showFirstDayDialog = false }
        )
    }

    if (showCurrencyDialog) {
        OptionSelectionDialog(
            title = AppStrings.get("currency_unit", lang),
            options = Currency.entries.map { it to it.titleFa },
            selectedOption = userSettings.currency,
            onSelect = {
                onCurrencyChange(it)
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showPinDialog) {
        var pinInput by remember { mutableStateOf(userSettings.appPin) }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(AppStrings.get("set_pin", lang), fontWeight = FontWeight.Bold) },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.length >= 4) {
                        onAppLockChange(true, pinInput)
                        showPinDialog = false
                        Toast.makeText(context, if (lang.code == "fa") "قفل برنامه فعال شد" else "App lock enabled", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text(AppStrings.get("confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onAppLockChange(false, "")
                    showPinDialog = false
                }) {
                    Text(if (lang.code == "fa") "غیرفعال‌سازی" else "Disable")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (lang.code == "fa") "یک پین‌کد حداقل ۴ رقمی وارد کنید:" else "Enter at least 4 digits PIN:")
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6) pinInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 72.dp)
    ) {
        item {
            Text(
                text = AppStrings.get("settings_title", lang),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Section: Theme (Day & Night)
        item {
            Text(AppStrings.get("theme_mode", lang), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        item {
            SettingRow(
                icon = Icons.Default.BrightnessMedium,
                title = AppStrings.get("theme_mode", lang),
                value = if (lang == AppLanguage.PERSIAN) userSettings.themeMode.titleFa else userSettings.themeMode.titleEn,
                onClick = { showThemeDialog = true }
            )
        }

        // Section: Language & Region
        item {
            Text(AppStrings.get("language_region", lang), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        item {
            SettingRow(
                icon = Icons.Default.Language,
                title = AppStrings.get("app_language", lang),
                value = if (lang == AppLanguage.PERSIAN) userSettings.language.titleFa else userSettings.language.titleEn,
                onClick = { showLanguageDialog = true }
            )
        }

        item {
            SettingRow(
                icon = Icons.Default.CalendarMonth,
                title = AppStrings.get("calendar_system", lang),
                value = if (lang == AppLanguage.PERSIAN) userSettings.calendarType.titleFa else userSettings.calendarType.titleEn,
                onClick = { showCalendarDialog = true }
            )
        }

        item {
            SettingRow(
                icon = Icons.Default.DateRange,
                title = AppStrings.get("date_format", lang),
                value = if (lang == AppLanguage.PERSIAN) userSettings.dateFormatPattern.labelFa else userSettings.dateFormatPattern.labelEn,
                onClick = { showPatternDialog = true }
            )
        }

        item {
            SettingRow(
                icon = Icons.Default.Pin,
                title = AppStrings.get("digit_format", lang),
                value = if (lang == AppLanguage.PERSIAN) userSettings.digitFormat.titleFa else userSettings.digitFormat.titleEn,
                onClick = { showDigitDialog = true }
            )
        }

        item {
            SettingRow(
                icon = Icons.Default.Today,
                title = AppStrings.get("first_day_of_week", lang),
                value = if (lang == AppLanguage.PERSIAN) userSettings.firstDayOfWeek.titleFa else userSettings.firstDayOfWeek.titleEn,
                onClick = { showFirstDayDialog = true }
            )
        }

        item {
            SettingRow(
                icon = Icons.Default.AttachMoney,
                title = AppStrings.get("currency_unit", lang),
                value = userSettings.currency.titleFa,
                onClick = { showCurrencyDialog = true }
            )
        }

        // Section: Security
        item {
            Text(AppStrings.get("security_privacy", lang), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        item {
            SettingRow(
                icon = Icons.Default.Lock,
                title = AppStrings.get("app_lock_pin", lang),
                value = if (userSettings.isAppLockEnabled) (if (lang.code == "fa") "فعال" else "Enabled") else (if (lang.code == "fa") "غیرفعال" else "Disabled"),
                onClick = { showPinDialog = true }
            )
        }

        // Section: Backup & Restore
        item {
            Text(AppStrings.get("backup_restore", lang), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        item {
            SettingRow(
                icon = Icons.Default.CloudUpload,
                title = AppStrings.get("export_backup", lang),
                value = "",
                onClick = {
                    coroutineScope.launch {
                        val json = BackupRestoreManager.createBackupJson(repository)
                        val file = File(context.filesDir, "pooleyar_backup_${System.currentTimeMillis()}.json")
                        file.writeText(json)
                        Toast.makeText(context, if (lang.code == "fa") "فایل پشتیبان در حافظه ذخیره شد: ${file.name}" else "Backup saved: ${file.name}", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        item {
            SettingRow(
                icon = Icons.Default.TableChart,
                title = AppStrings.get("export_csv", lang),
                value = "",
                onClick = {
                    exportTransactionsToCsv(context, transactions)
                }
            )
        }

        // Section: About
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(AppStrings.get("about_pooleyar", lang), fontWeight = FontWeight.Bold)
                    Text(
                        text = AppStrings.get("about_desc", lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            if (value.isNotBlank()) {
                Text(value, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            } else {
                Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun <T> OptionSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { (option, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selectedOption, onClick = { onSelect(option) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    )
}

private fun exportTransactionsToCsv(context: Context, transactions: List<TransactionDetail>) {
    try {
        val file = File(context.filesDir, "pooleyar_transactions.csv")
        file.bufferedWriter().use { writer ->
            writer.write("ID,Type,Amount(Toman),DateEpoch,Account,Category,Description,Contact\n")
            transactions.forEach { d ->
                val tx = d.transaction
                writer.write("${tx.id},${tx.type},${tx.amount},${tx.dateEpochMillis},\"${d.sourceAccount?.name ?: ""}\",\"${d.category?.name ?: ""}\",\"${tx.description}\",\"${tx.contactPerson}\"\n")
            }
        }
        Toast.makeText(context, "فایل خروجی ذخیره شد: ${file.name}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در خروجی: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
