package com.example.util

import com.example.data.model.AppCurrency
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

object FormatUtils {

    private val numberFormat = NumberFormat.getIntegerInstance(Locale.US)

    /**
     * Converts a USD amount to the target currency using the given rate (units per 1 USD)
     */
    fun convertFromUsd(amountUsd: Double, ratePerUsd: Double): Double {
        return amountUsd * ratePerUsd
    }

    /**
     * Converts an amount in currency with ratePerUsd back to USD
     */
    fun convertToUsd(amount: Double, ratePerUsd: Double): Double {
        if (ratePerUsd <= 0.0) return amount
        return amount / ratePerUsd
    }

    /**
     * Formats currency amount with zero decimals using the appropriate currency symbol.
     * e.g.:
     * USD: $10,000
     * INR: ₹835,000
     * AED: AED 36,725
     */
    fun formatCurrencyZeroDecimal(amount: Double?, currency: AppCurrency): String {
        if (amount == null) return if (currency == AppCurrency.AED) "AED 0" else "${currency.symbol}0"
        val value = round(amount).toLong()
        val formattedNum = numberFormat.format(value)
        return if (currency == AppCurrency.AED) {
            "AED $formattedNum"
        } else {
            "${currency.symbol}$formattedNum"
        }
    }

    /**
     * Formats currency amount with sign (+ / -) and zero decimals.
     * e.g. +$10,000 or -₹5,000
     */
    fun formatSignedCurrencyZeroDecimal(amount: Double?, currency: AppCurrency): String {
        if (amount == null) return if (currency == AppCurrency.AED) "AED 0" else "${currency.symbol}0"
        val value = round(amount).toLong()
        val absNum = numberFormat.format(abs(value))
        val sign = if (value >= 0) "+" else "-"
        return if (currency == AppCurrency.AED) {
            "$sign AED $absNum"
        } else {
            "$sign${currency.symbol}$absNum"
        }
    }

    /**
     * Formats fiat numbers with zero decimals (defaults to USD for compatibility)
     */
    fun formatFiatZeroDecimal(amount: Double?): String {
        return formatCurrencyZeroDecimal(amount, AppCurrency.USD)
    }

    /**
     * Formats fiat numbers with sign and zero decimals (defaults to USD for compatibility)
     */
    fun formatSignedFiatZeroDecimal(amount: Double?): String {
        return formatSignedCurrencyZeroDecimal(amount, AppCurrency.USD)
    }

    /**
     * Formats USDT numbers with zero decimals, e.g. "22,950 USDT"
     */
    fun formatUsdtZeroDecimal(amount: Double?): String {
        if (amount == null) return "0 USDT"
        val value = round(amount).toLong()
        return numberFormat.format(value) + " USDT"
    }

    /**
     * Formats plain integers with comma, e.g. "24,950"
     */
    fun formatInteger(amount: Number?): String {
        if (amount == null) return "0"
        val rounded = when (amount) {
            is Double -> round(amount).toLong()
            is Float -> round(amount).toLong()
            else -> amount.toLong()
        }
        return numberFormat.format(rounded)
    }

    /**
     * Formats rate with up to 4 decimals, e.g. "83.50", "3.6725", "1.0020"
     */
    fun formatRate(rate: Double): String {
        return if (rate == rate.toLong().toDouble()) {
            rate.toLong().toString()
        } else {
            val formatted = String.format(Locale.US, "%.4f", rate)
            formatted.trimEnd('0').trimEnd('.')
        }
    }
}
