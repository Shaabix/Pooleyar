package com.example.util

/**
 * High-precision algorithms for conversion between Gregorian and Persian (Jalali/Solar Hijri) calendars.
 * Based on the astronomical algorithm (Birashk / Kazimierz Borkowski algorithm).
 */
data class JalaliDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..31
) : Comparable<JalaliDate> {

    fun monthName(): String = PERSIAN_MONTH_NAMES.getOrElse(month - 1) { "" }

    fun format(persianDigits: Boolean = true): String {
        val y = year.toString().padStart(4, '0')
        val m = month.toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        val str = "$y/$m/$d"
        return if (persianDigits) str.toPersianDigits() else str
    }

    fun formatPretty(persianDigits: Boolean = true): String {
        val d = if (persianDigits) day.toString().toPersianDigits() else day.toString()
        val y = if (persianDigits) year.toString().toPersianDigits() else year.toString()
        return "$d ${monthName()} $y"
    }

    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        val PERSIAN_MONTH_NAMES = listOf(
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
        )

        fun daysInMonth(year: Int, month: Int): Int {
            return when {
                month in 1..6 -> 31
                month in 7..11 -> 30
                month == 12 -> if (isLeapYear(year)) 30 else 29
                else -> 30
            }
        }

        fun isLeapYear(year: Int): Boolean {
            val (gy1, gm1, gd1) = toGregorian(year, 1, 1)
            val (gy2, gm2, gd2) = toGregorian(year + 1, 1, 1)
            val cal1 = java.util.Calendar.getInstance().apply { set(gy1, gm1 - 1, gd1, 0, 0, 0) }
            val cal2 = java.util.Calendar.getInstance().apply { set(gy2, gm2 - 1, gd2, 0, 0, 0) }
            val diffDays = (cal2.timeInMillis - cal1.timeInMillis) / (24 * 60 * 60 * 1000L)
            return diffDays >= 366
        }

        /**
         * Converts Gregorian year, month (1..12), day to JalaliDate
         */
        fun fromGregorian(gYear: Int, gMonth: Int, gDay: Int): JalaliDate {
            val gDaysInMonths = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
            var gy = gYear - 1600
            var gm = gMonth - 1
            var gd = gDay - 1

            var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400

            gDayNo += gDaysInMonths[gm] + gd
            if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
                gDayNo++
            }

            var jDayNo = gDayNo - 79
            val jNp = jDayNo / 12053
            jDayNo %= 12053

            var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
            jDayNo %= 1461

            if (jDayNo >= 366) {
                jy += (jDayNo - 1) / 365
                jDayNo = (jDayNo - 1) % 365
            }

            val jm: Int
            val jd: Int
            if (jDayNo < 186) {
                jm = 1 + jDayNo / 31
                jd = 1 + (jDayNo % 31)
            } else {
                jm = 7 + (jDayNo - 186) / 30
                jd = 1 + ((jDayNo - 186) % 30)
            }

            return JalaliDate(jy, jm, jd)
        }

        /**
         * Converts Jalali date to Gregorian (gYear, gMonth (1..12), gDay)
         */
        fun toGregorian(jYear: Int, jMonth: Int, jDay: Int): Triple<Int, Int, Int> {
            val jy = jYear - 979
            val jm = jMonth - 1
            val jd = jDay - 1

            var jDayNo = 365 * jy + (jy / 33) * 8 + ((jy % 33 + 3) / 4)
            jDayNo += if (jm < 6) jm * 31 else 186 + (jm - 6) * 30
            jDayNo += jd

            var gDayNo = jDayNo + 79
            var gy = 1600 + 400 * (gDayNo / 146097)
            gDayNo %= 146097

            var leap = true
            if (gDayNo >= 36525) {
                gDayNo--
                gy += 100 * (gDayNo / 36524)
                gDayNo %= 36524

                if (gDayNo >= 365) {
                    gDayNo++
                } else {
                    leap = false
                }
            }

            gy += 4 * (gDayNo / 1461)
            gDayNo %= 1461

            if (gDayNo >= 366) {
                leap = false
                gDayNo--
                gy += gDayNo / 365
                gDayNo %= 365
            }

            val gDaysInMonth = intArrayOf(
                31,
                if (leap) 29 else 28,
                31, 30, 31, 30, 31, 31, 30, 31, 30, 31
            )

            var gm = 0
            while (gm < 12 && gDayNo >= gDaysInMonth[gm]) {
                gDayNo -= gDaysInMonth[gm]
                gm++
            }

            return Triple(gy, gm + 1, gDayNo + 1)
        }

        fun fromTimestamp(epochMillis: Long): JalaliDate {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tehran"))
            cal.timeInMillis = epochMillis
            return fromGregorian(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
        }

        fun toTimestamp(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long {
            val (gy, gm, gd) = toGregorian(year, month, day)
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tehran"))
            cal.set(gy, gm - 1, gd, hour, minute, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun now(): JalaliDate {
            return fromTimestamp(System.currentTimeMillis())
        }
    }
}

fun String.toPersianDigits(): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    val builder = StringBuilder(this.length)
    for (ch in this) {
        val idx = englishDigits.indexOf(ch)
        if (idx != -1) {
            builder.append(persianDigits[idx])
        } else {
            builder.append(ch)
        }
    }
    return builder.toString()
}

fun String.toEnglishDigits(): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val builder = StringBuilder(this.length)
    for (ch in this) {
        val pIdx = persianDigits.indexOf(ch)
        if (pIdx != -1) {
            builder.append('0' + pIdx)
            continue
        }
        val aIdx = arabicDigits.indexOf(ch)
        if (aIdx != -1) {
            builder.append('0' + aIdx)
            continue
        }
        builder.append(ch)
    }
    return builder.toString()
}
