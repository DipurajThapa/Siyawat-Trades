package com.example.data.model

enum class AppCurrency(
    val code: String,
    val symbol: String,
    val label: String,
    val defaultRatePerUsd: Double // 1 USD = X Units of this currency
) {
    USD(code = "USD", symbol = "$", label = "USD ($)", defaultRatePerUsd = 1.0),
    INR(code = "INR", symbol = "₹", label = "INR (₹)", defaultRatePerUsd = 83.50),
    AED(code = "AED", symbol = "AED", label = "AED (د.إ)", defaultRatePerUsd = 3.6725);

    companion object {
        val ALL: List<AppCurrency> = listOf(USD, INR, AED)

        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return USD
            return values().firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
        }
    }
}
