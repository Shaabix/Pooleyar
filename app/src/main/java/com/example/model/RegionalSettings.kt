package com.example.model

import com.example.util.Currency

enum class ThemeMode(val code: String, val titleFa: String, val titleEn: String) {
    SYSTEM("system", "پیرو سیستم", "System Default"),
    LIGHT("light", "حالت روشن (روز)", "Light Mode"),
    DARK("dark", "حالت تاریک (شب)", "Dark Mode")
}

enum class AppLanguage(val code: String, val titleFa: String, val titleEn: String) {
    PERSIAN("fa", "فارسی", "Persian"),
    ENGLISH("en", "English", "English")
}

enum class CalendarType(val code: String, val titleFa: String, val titleEn: String) {
    JALALI("jalali", "خورشیدی (جلالی)", "Solar Hijri (Jalali)"),
    GREGORIAN("gregorian", "میلادی", "Gregorian")
}

enum class DigitFormat(val code: String, val titleFa: String, val titleEn: String) {
    PERSIAN("persian", "ارقام فارسی (۱۲۳۴۵)", "Persian Digits (۱۲۳۴۵)"),
    WESTERN("western", "ارقام لاتین (12345)", "Western Digits (12345)")
}

enum class DateFormatPattern(val pattern: String, val labelFa: String, val labelEn: String) {
    YEAR_MONTH_DAY("YYYY/MM/DD", "۱۴۰۵/۰۷/۱۵ (سال/ماه/روز)", "2026/10/07 (YYYY/MM/DD)"),
    DAY_MONTH_YEAR("DD/MM/YYYY", "۱۵/۰۷/۱۴۰۵ (روز/ماه/سال)", "07/10/2026 (DD/MM/YYYY)"),
    PRETTY_FULL("D_MMMM_YYYY", "۱۵ مهر ۱۴۰۵ (نمایش کامل)", "7 October 2026 (Full Name)")
}

enum class FirstDayOfWeek(val dayValue: Int, val titleFa: String, val titleEn: String) {
    SATURDAY(6, "شنبه", "Saturday"),
    SUNDAY(7, "یکشنبه", "Sunday"),
    MONDAY(1, "دوشنبه", "Monday")
}

enum class TimeFormat(val is24Hour: Boolean, val titleFa: String, val titleEn: String) {
    FORMAT_24(true, "۲۴ ساعته (۱۴:۳۰)", "24-Hour (14:30)"),
    FORMAT_12(false, "۱۲ ساعته (۲:۳۰ ب.ظ)", "12-Hour (2:30 PM)")
}

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.PERSIAN,
    val calendarType: CalendarType = CalendarType.JALALI,
    val digitFormat: DigitFormat = DigitFormat.PERSIAN,
    val dateFormatPattern: DateFormatPattern = DateFormatPattern.YEAR_MONTH_DAY,
    val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.SATURDAY,
    val timeFormat: TimeFormat = TimeFormat.FORMAT_24,
    val currency: Currency = Currency.TOMAN,
    val isAppLockEnabled: Boolean = false,
    val appPin: String = ""
)
