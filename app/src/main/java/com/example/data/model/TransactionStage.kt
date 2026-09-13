package com.example.data.model

enum class TransactionStage(
    val title: String,
    val description: String,
    val stepIndex: Int
) {
    CAPITAL_INJECTION(
        title = "Capital Injection",
        description = "User logs initial fiat currency spent",
        stepIndex = 1
    ),
    BANK_TO_EXCHANGE(
        title = "Bank to Exchange",
        description = "Admin logs fiat transfer from central bank to Binance",
        stepIndex = 2
    ),
    USDT_ACQUISITION(
        title = "USDT Acquisition",
        description = "Admin logs USDT purchased on Binance",
        stepIndex = 3
    ),
    USDT_DISTRIBUTION(
        title = "USDT Distribution",
        description = "USDT transferred to individual trading accounts",
        stepIndex = 4
    ),
    LIQUIDATION(
        title = "Liquidation",
        description = "User logs USDT sold back to fiat",
        stepIndex = 5
    )
}
