package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("budget_savings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENCY_CODE = "currency_code"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_THEME_MODE = "theme_mode" // SYSTEM, LIGHT, DARK
        private const val KEY_PIN_LOCK_ENABLED = "pin_lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_CARRY_FORWARD_BUDGET = "carry_forward_budget"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }

    var currencyCode: String
        get() = prefs.getString(KEY_CURRENCY_CODE, "USD") ?: "USD"
        set(value) = prefs.edit().putString(KEY_CURRENCY_CODE, value).apply()

    var currencySymbol: String
        get() = prefs.getString(KEY_CURRENCY_SYMBOL, "$") ?: "$"
        set(value) = prefs.edit().putString(KEY_CURRENCY_SYMBOL, value).apply()

    var dateFormat: String
        get() = prefs.getString(KEY_DATE_FORMAT, "dd MMM yyyy") ?: "dd MMM yyyy"
        set(value) = prefs.edit().putString(KEY_DATE_FORMAT, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var isPinLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_LOCK_ENABLED, value).apply()

    var isCarryForwardBudgetEnabled: Boolean
        get() = prefs.getBoolean(KEY_CARRY_FORWARD_BUDGET, false)
        set(value) = prefs.edit().putBoolean(KEY_CARRY_FORWARD_BUDGET, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).putBoolean(KEY_PIN_LOCK_ENABLED, true).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return storedHash == hashPin(pin)
    }

    fun disablePin() {
        prefs.edit().remove(KEY_PIN_HASH).putBoolean(KEY_PIN_LOCK_ENABLED, false).apply()
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(pin.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
