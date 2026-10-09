package com.example.util

import java.text.DecimalFormat
import java.math.BigInteger

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
            Currency.RIAL -> BigInteger.valueOf(amountInToman).multiply(BigInteger.TEN)
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
        if (parsed <= 0L) return 0L
        return when (currency) {
            Currency.TOMAN -> parsed
            Currency.RIAL -> {
                // 10 Rial = 1 Toman
                // Positive Rial amounts >= 1 are never truncated to 0, using integer math
                if (parsed in 1L..9L) 1L else parsed / 10L + if (parsed % 10L >= 5L) 1L else 0L
            }
        }
    }

    /**
     * Converts a stored Toman amount to string representation for input fields
     * in the user's selected currency.
     */
    fun amountForInput(amountInToman: Long, currency: Currency = Currency.TOMAN): String {
        val displayed = when (currency) {
            Currency.TOMAN -> amountInToman
            Currency.RIAL -> BigInteger.valueOf(amountInToman).multiply(BigInteger.TEN)
        }
        return displayed.toString()
    }

    /**
     * Converts an amount stored in Toman to the target currency.
     */
    fun convertAmount(amountInToman: Long, targetCurrency: Currency): Long {
        return when (targetCurrency) {
            Currency.TOMAN -> amountInToman
            Currency.RIAL -> Math.multiplyExact(amountInToman, 10L)
        }
    }
}
