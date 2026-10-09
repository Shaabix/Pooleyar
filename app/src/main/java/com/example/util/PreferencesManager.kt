package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pooleyar_prefs", Context.MODE_PRIVATE)

    fun getUserSettings(): UserSettings {
        val themeCode = prefs.getString("theme_mode", ThemeMode.SYSTEM.code) ?: ThemeMode.SYSTEM.code
        val theme = ThemeMode.entries.firstOrNull { it.code == themeCode } ?: ThemeMode.SYSTEM

        val langCode = prefs.getString("language", AppLanguage.PERSIAN.code) ?: AppLanguage.PERSIAN.code
        val lang = AppLanguage.entries.firstOrNull { it.code == langCode } ?: AppLanguage.PERSIAN

        val calCode = prefs.getString("calendar", CalendarType.JALALI.code) ?: CalendarType.JALALI.code
        val cal = CalendarType.entries.firstOrNull { it.code == calCode } ?: CalendarType.JALALI

        val digitCode = prefs.getString("digit_format", DigitFormat.PERSIAN.code) ?: DigitFormat.PERSIAN.code
        val digits = DigitFormat.entries.firstOrNull { it.code == digitCode } ?: DigitFormat.PERSIAN

        val datePattern = prefs.getString("date_pattern", DateFormatPattern.YEAR_MONTH_DAY.name) ?: DateFormatPattern.YEAR_MONTH_DAY.name
        val pattern = try { DateFormatPattern.valueOf(datePattern) } catch (e: Exception) { DateFormatPattern.YEAR_MONTH_DAY }

        val firstDayVal = prefs.getInt("first_day_of_week", FirstDayOfWeek.SATURDAY.dayValue)
        val firstDay = FirstDayOfWeek.entries.firstOrNull { it.dayValue == firstDayVal } ?: FirstDayOfWeek.SATURDAY

        val is24H = prefs.getBoolean("is_24_hour", true)
        val timeFormat = if (is24H) TimeFormat.FORMAT_24 else TimeFormat.FORMAT_12

        val curCode = prefs.getString("currency", Currency.TOMAN.code) ?: Currency.TOMAN.code
        val currency = Currency.fromCode(curCode)

        val isLock = prefs.getBoolean("app_lock", false)
        val pin = prefs.getString("pin", "") ?: ""

        return UserSettings(
            themeMode = theme,
            language = lang,
            calendarType = cal,
            digitFormat = digits,
            dateFormatPattern = pattern,
            firstDayOfWeek = firstDay,
            timeFormat = timeFormat,
            currency = currency,
            isAppLockEnabled = isLock,
            appPin = pin
        )
    }

    fun saveThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString("theme_mode", themeMode.code).apply()
    }

    fun saveLanguage(language: AppLanguage) {
        prefs.edit().putString("language", language.code).apply()
    }

    fun saveCalendarType(calendar: CalendarType) {
        prefs.edit().putString("calendar", calendar.code).apply()
    }

    fun saveDigitFormat(format: DigitFormat) {
        prefs.edit().putString("digit_format", format.code).apply()
    }

    fun saveDateFormatPattern(pattern: DateFormatPattern) {
        prefs.edit().putString("date_pattern", pattern.name).apply()
    }

    fun saveFirstDayOfWeek(firstDay: FirstDayOfWeek) {
        prefs.edit().putInt("first_day_of_week", firstDay.dayValue).apply()
    }

    fun saveTimeFormat(timeFormat: TimeFormat) {
        prefs.edit().putBoolean("is_24_hour", timeFormat.is24Hour).apply()
    }

    fun saveCurrency(currency: Currency) {
        prefs.edit().putString("currency", currency.code).apply()
    }

    fun saveAppLock(enabled: Boolean, pin: String) {
        prefs.edit().putBoolean("app_lock", enabled).putString("pin", pin).apply()
    }

    fun getCustomBanks(): List<IranianBank> {
        val raw = prefs.getString("custom_banks_json", null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val list = mutableListOf<IranianBank>()
        try {
            val jsonArray = org.json.JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val id = obj.optString("id", "")
                val nameFa = obj.optString("nameFa", "")
                if (id.isBlank() || nameFa.isBlank()) continue
                list.add(
                    IranianBank(
                        id = id,
                        nameFa = nameFa,
                        nameEn = obj.optString("nameEn", nameFa),
                        cardPrefix = obj.optString("cardPrefix", ""),
                        primaryColorHex = obj.optLong("primaryColorHex", 0xFF00897BL),
                        isCustom = true
                    )
                )
            }
        } catch (_: Exception) {
            // Malformed JSON should return emptyList without throwing, preserving preference integrity
            return emptyList()
        }
        return list
    }

    fun saveCustomBank(bank: IranianBank) {
        if (bank.id.isBlank() || bank.nameFa.isBlank()) return
        val current = getCustomBanks().toMutableList()
        current.removeAll { it.id == bank.id }
        current.add(bank)
        saveAllCustomBanks(current)
    }

    fun saveAllCustomBanks(banks: List<IranianBank>) {
        val jsonArray = org.json.JSONArray()
        for (b in banks) {
            if (b.id.isBlank() || b.nameFa.isBlank()) continue
            val obj = org.json.JSONObject()
            obj.put("id", b.id)
            obj.put("nameFa", b.nameFa)
            obj.put("nameEn", b.nameEn)
            obj.put("cardPrefix", b.cardPrefix)
            obj.put("primaryColorHex", b.primaryColorHex)
            obj.put("isCustom", true)
            jsonArray.put(obj)
        }
        prefs.edit().putString("custom_banks_json", jsonArray.toString()).apply()
    }
}
