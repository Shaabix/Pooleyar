package com.example

import com.example.model.*
import com.example.util.AppDateTimeFormatter
import com.example.util.Currency
import com.example.util.CurrencyFormatter
import com.example.util.JalaliDate
import org.junit.Assert.*
import org.junit.Test

class RegionalAndCalendarValidationTest {

    @Test
    fun testLeapYearAndNowruzBoundaries() {
        // 1403 is a leap year (366 days, Esfand has 30 days)
        assertTrue(JalaliDate.isLeapYear(1403))
        assertEquals(30, JalaliDate.daysInMonth(1403, 12))

        // 1404 is not a leap year (Esfand has 29 days)
        assertFalse(JalaliDate.isLeapYear(1404))
        assertEquals(29, JalaliDate.daysInMonth(1404, 12))

        // Reversible Nowruz boundary test: 2024-03-20 -> 1403-01-01
        val jNowruz = JalaliDate.fromGregorian(2024, 3, 20)
        assertEquals(1403, jNowruz.year)
        assertEquals(1, jNowruz.month)
        assertEquals(1, jNowruz.day)

        val (gy, gm, gd) = JalaliDate.toGregorian(1403, 1, 1)
        assertEquals(2024, gy)
        assertEquals(3, gm)
        assertEquals(20, gd)
    }

    @Test
    fun testReversibleRoundTripConversionAcrossCentury() {
        // Test 100 historical and future dates
        val testYears = listOf(1350, 1370, 1390, 1400, 1403, 1405, 1420, 1450)
        for (jy in testYears) {
            for (jm in listOf(1, 6, 7, 12)) {
                val maxDay = JalaliDate.daysInMonth(jy, jm)
                for (jd in listOf(1, 15, maxDay)) {
                    val (gYear, gMonth, gDay) = JalaliDate.toGregorian(jy, jm, jd)
                    val backJalali = JalaliDate.fromGregorian(gYear, gMonth, gDay)
                    assertEquals("Failed roundtrip for $jy/$jm/$jd", jy, backJalali.year)
                    assertEquals("Failed roundtrip for $jy/$jm/$jd", jm, backJalali.month)
                    assertEquals("Failed roundtrip for $jy/$jm/$jd", jd, backJalali.day)
                }
            }
        }
    }

    @Test
    fun testConfigurableDateFormatAndDigits() {
        val epoch = JalaliDate.toTimestamp(1405, 7, 15)

        val formattedJalaliPersianDigits = AppDateTimeFormatter.formatDate(
            epochMillis = epoch,
            calendarType = CalendarType.JALALI,
            pattern = DateFormatPattern.YEAR_MONTH_DAY,
            digitFormat = DigitFormat.PERSIAN,
            language = AppLanguage.PERSIAN
        )
        assertEquals("۱۴۰۵/۰۷/۱۵", formattedJalaliPersianDigits)

        val formattedJalaliWesternDigits = AppDateTimeFormatter.formatDate(
            epochMillis = epoch,
            calendarType = CalendarType.JALALI,
            pattern = DateFormatPattern.YEAR_MONTH_DAY,
            digitFormat = DigitFormat.WESTERN,
            language = AppLanguage.PERSIAN
        )
        assertEquals("1405/07/15", formattedJalaliWesternDigits)

        val formattedGregorian = AppDateTimeFormatter.formatDate(
            epochMillis = epoch,
            calendarType = CalendarType.GREGORIAN,
            pattern = DateFormatPattern.YEAR_MONTH_DAY,
            digitFormat = DigitFormat.WESTERN,
            language = AppLanguage.ENGLISH
        )
        // 1405/07/15 is 2026/10/07
        assertEquals("2026/10/07", formattedGregorian)
    }

    @Test
    fun testCurrencyFormatting() {
        val amount = 50000000L // 50M Toman
        val tomanStr = CurrencyFormatter.formatAmount(amount, Currency.TOMAN, persianDigits = false)
        assertEquals("50,000,000 تومان", tomanStr)

        val rialStr = CurrencyFormatter.formatAmount(amount, Currency.RIAL, persianDigits = false)
        assertEquals("500,000,000 ریال", rialStr)
    }
}
