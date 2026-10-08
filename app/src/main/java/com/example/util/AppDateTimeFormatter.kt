package com.example.util

import com.example.model.AppLanguage
import com.example.model.CalendarType
import com.example.model.DateFormatPattern
import com.example.model.DigitFormat
import java.text.DecimalFormat
import java.util.Calendar
import java.util.TimeZone

/**
 * Universal date-time and digit formatting service.
 * Supports Jalali and Gregorian calendars, configurable digit format (Persian vs Western),
 * customizable date patterns, and time formats.
 */
object AppDateTimeFormatter {

    val GREGORIAN_MONTH_NAMES_FA = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    val GREGORIAN_MONTH_NAMES_EN = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun formatDate(
        epochMillis: Long,
        calendarType: CalendarType = CalendarType.JALALI,
        pattern: DateFormatPattern = DateFormatPattern.YEAR_MONTH_DAY,
        digitFormat: DigitFormat = DigitFormat.PERSIAN,
        language: AppLanguage = AppLanguage.PERSIAN
    ): String {
        return if (calendarType == CalendarType.JALALI) {
            val jDate = JalaliDate.fromTimestamp(epochMillis)
            val y = jDate.year.toString().padStart(4, '0')
            val m = jDate.month.toString().padStart(2, '0')
            val d = jDate.day.toString().padStart(2, '0')

            val formatted = when (pattern) {
                DateFormatPattern.YEAR_MONTH_DAY -> "$y/$m/$d"
                DateFormatPattern.DAY_MONTH_YEAR -> "$d/$m/$y"
                DateFormatPattern.PRETTY_FULL -> {
                    val mName = if (language == AppLanguage.PERSIAN) jDate.monthName() else "Month ${jDate.month}"
                    "$d $mName $y"
                }
            }
            if (digitFormat == DigitFormat.PERSIAN) formatted.toPersianDigits() else formatted.toEnglishDigits()
        } else {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran"))
            cal.timeInMillis = epochMillis
            val y = cal.get(Calendar.YEAR).toString().padStart(4, '0')
            val m = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
            val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')

            val formatted = when (pattern) {
                DateFormatPattern.YEAR_MONTH_DAY -> "$y/$m/$d"
                DateFormatPattern.DAY_MONTH_YEAR -> "$d/$m/$y"
                DateFormatPattern.PRETTY_FULL -> {
                    val mIdx = cal.get(Calendar.MONTH)
                    val mName = if (language == AppLanguage.PERSIAN) {
                        GREGORIAN_MONTH_NAMES_FA.getOrElse(mIdx) { "" }
                    } else {
                        GREGORIAN_MONTH_NAMES_EN.getOrElse(mIdx) { "" }
                    }
                    "$d $mName $y"
                }
            }
            if (digitFormat == DigitFormat.PERSIAN) formatted.toPersianDigits() else formatted.toEnglishDigits()
        }
    }

    fun getMonthName(
        monthIndex: Int, // 1..12
        calendarType: CalendarType,
        language: AppLanguage
    ): String {
        return if (calendarType == CalendarType.JALALI) {
            JalaliDate.PERSIAN_MONTH_NAMES.getOrElse(monthIndex - 1) { "" }
        } else {
            if (language == AppLanguage.PERSIAN) {
                GREGORIAN_MONTH_NAMES_FA.getOrElse(monthIndex - 1) { "" }
            } else {
                GREGORIAN_MONTH_NAMES_EN.getOrElse(monthIndex - 1) { "" }
            }
        }
    }

    fun formatNumber(
        number: Long,
        digitFormat: DigitFormat = DigitFormat.PERSIAN
    ): String {
        val df = DecimalFormat("#,###")
        val formatted = df.format(number)
        return if (digitFormat == DigitFormat.PERSIAN) formatted.toPersianDigits() else formatted.toEnglishDigits()
    }
}
