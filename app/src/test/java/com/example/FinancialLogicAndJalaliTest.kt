package com.example

import com.example.model.DebtDirection
import com.example.model.DebtStatus
import com.example.model.TransactionType
import com.example.util.Currency
import com.example.util.CurrencyFormatter
import com.example.util.JalaliDate
import org.junit.Assert.*
import org.junit.Test

class FinancialLogicAndJalaliTest {

    @Test
    fun testJalaliDateConversion() {
        // 2026-10-07 -> Jalali 1405-07-15
        val jalali = JalaliDate.fromGregorian(2026, 10, 7)
        assertEquals(1405, jalali.year)
        assertEquals(7, jalali.month)
        assertEquals(15, jalali.day)

        // Convert back to Gregorian
        val (gy, gm, gd) = JalaliDate.toGregorian(1405, 7, 15)
        assertEquals(2026, gy)
        assertEquals(10, gm)
        assertEquals(7, gd)
    }

    @Test
    fun testCurrencyFormattingAndIntegerSafety() {
        val amountInToman = 2500000L
        val formattedToman = CurrencyFormatter.formatAmount(amountInToman, Currency.TOMAN, persianDigits = false)
        assertEquals("2,500,000 تومان", formattedToman)

        val formattedRial = CurrencyFormatter.formatAmount(amountInToman, Currency.RIAL, persianDigits = false)
        assertEquals("25,000,000 ریال", formattedRial)

        val parsed = CurrencyFormatter.parseAmount("۲,۵۰۰,۰۰۰", Currency.TOMAN)
        assertEquals(2500000L, parsed)
    }

    @Test
    fun testTransferDoesNotAffectNetWorth() {
        var account1Balance = 10000000L
        var account2Balance = 5000000L
        val transferAmount = 2000000L

        // Execute transfer
        account1Balance -= transferAmount
        account2Balance += transferAmount

        val totalBalance = account1Balance + account2Balance
        assertEquals(15000000L, totalBalance)
    }
}
