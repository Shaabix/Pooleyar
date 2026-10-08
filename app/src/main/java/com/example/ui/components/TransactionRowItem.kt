package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransactionType
import com.example.model.UserSettings
import com.example.repository.TransactionDetail
import com.example.ui.theme.PersianBlueTransfer
import com.example.ui.theme.PersianGreenIncome
import com.example.ui.theme.PersianRoseExpense
import com.example.util.AppDateTimeFormatter
import com.example.util.CurrencyFormatter

@Composable
fun TransactionRowItem(
    detail: TransactionDetail,
    userSettings: UserSettings,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tx = detail.transaction
    val isExpense = tx.type == TransactionType.EXPENSE.name
    val isIncome = tx.type == TransactionType.INCOME.name
    val isTransfer = tx.type == TransactionType.TRANSFER.name

    val amountColor = when {
        isExpense -> PersianRoseExpense
        isIncome -> PersianGreenIncome
        else -> PersianBlueTransfer
    }

    val amountPrefix = when {
        isExpense -> "- "
        isIncome -> "+ "
        else -> ""
    }

    val formattedDate = AppDateTimeFormatter.formatDate(
        epochMillis = tx.dateEpochMillis,
        calendarType = userSettings.calendarType,
        pattern = userSettings.dateFormatPattern,
        digitFormat = userSettings.digitFormat,
        language = userSettings.language
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_row_${tx.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & title/account
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = amountColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when {
                            isExpense -> Icons.Default.ArrowDownward
                            isIncome -> Icons.Default.ArrowUpward
                            else -> Icons.Default.SwapHoriz
                        }
                        Icon(icon, contentDescription = null, tint = amountColor, modifier = Modifier.size(22.dp))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    val title = when {
                        detail.category != null -> detail.category.name
                        isTransfer -> if (userSettings.language.code == "fa") "انتقال بین حساب‌ها" else "Account Transfer"
                        tx.description.isNotBlank() -> tx.description
                        else -> if (isExpense) "هزینه" else "درآمد"
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val subtitle = buildString {
                        detail.sourceAccount?.let { append(it.name) }
                        if (isTransfer && detail.destinationAccount != null) {
                            append(" ➔ ")
                            append(detail.destinationAccount.name)
                        }
                        if (tx.contactPerson.isNotBlank()) {
                            append(" • ${tx.contactPerson}")
                        }
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Amount & Date
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountPrefix + CurrencyFormatter.formatAmount(
                        amountInToman = tx.amount,
                        targetCurrency = userSettings.currency,
                        persianDigits = userSettings.digitFormat.code == "persian"
                    ),
                    color = amountColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
