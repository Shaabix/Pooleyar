package com.example.util

import java.text.DecimalFormat

enum class Currency(val code: String, val titleFa: String, val symbolFa: String) {
    TOMAN("TOMAN", "تومان", "تومان"),
    RIAL("RIAL", "ریال", "ریال");

    companion object {
        fun fromCode(code: String): Currency = entries.firstOrNull { it.code == code } ?: TOMAN
    }
}

/**
 * Monetary utility ensuring 64-bit integer arithmetic (zero floating-point errors).
 * In Pooleyar, the standard base unit stored in database is always TOMAN.
 * Rial values are exactly 10x Toman.
 */
object CurrencyFormatter {
    private val decimalFormat = DecimalFormat("#,###")

    fun formatAmount(
        amountInToman: Long,
        targetCurrency: Currency = Currency.TOMAN,
        includeUnit: Boolean = true,
        persianDigits: Boolean = true
    ): String {
        val displayedAmount = when (targetCurrency) {
            Currency.TOMAN -> amountInToman
            Currency.RIAL -> amountInToman * 10L
        }

        val formattedNum = decimalFormat.format(displayedAmount)
        val withDigits = if (persianDigits) formattedNum.toPersianDigits() else formattedNum

        return if (includeUnit) {
            "$withDigits ${targetCurrency.symbolFa}"
        } else {
            withDigits
        }
    }

    fun parseAmount(input: String, currency: Currency = Currency.TOMAN): Long {
        val cleaned = input.toEnglishDigits().replace(Regex("[^0-9]"), "")
        if (cleaned.isBlank()) return 0L
        val parsed = cleaned.toLongOrNull() ?: 0L
        return when (currency) {
            Currency.TOMAN -> parsed
            Currency.RIAL -> parsed / 10L
        }
    }
}
