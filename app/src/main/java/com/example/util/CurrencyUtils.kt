package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String
)

object CurrencyUtils {
    val supportedCurrencies = listOf(
        CurrencyInfo("SAR", "SAR", "Saudi Riyal (ر.س)"),
        CurrencyInfo("BDT", "৳", "Bangladeshi Taka (BDT)"),
        CurrencyInfo("USD", "$", "US Dollar ($)"),
        CurrencyInfo("EUR", "€", "Euro (€)"),
        CurrencyInfo("GBP", "£", "British Pound (£)"),
        CurrencyInfo("INR", "₹", "Indian Rupee (₹)"),
        CurrencyInfo("AED", "AED", "UAE Dirham (د.إ)"),
        CurrencyInfo("CAD", "CA$", "Canadian Dollar ($)"),
        CurrencyInfo("AUD", "A$", "Australian Dollar ($)"),
        CurrencyInfo("QAR", "QAR", "Qatari Riyal (ر.ق)"),
        CurrencyInfo("MYR", "RM", "Malaysian Ringgit (RM)"),
        CurrencyInfo("SGD", "S$", "Singapore Dollar (S$)")
    )

    private val numberFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun format(amount: Double, symbol: String = "$"): String {
        val formattedNumber = numberFormat.format(amount)
        return "$symbol $formattedNumber"
    }

    fun formatWithoutSymbol(amount: Double): String {
        return numberFormat.format(amount)
    }
}
