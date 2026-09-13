package com.example.data.model

data class PoolMetrics(
    // 1. Capital Injections
    val totalFiatSpent: Double = 0.0,           // Verified fiat injected by members
    val pendingFiatInjections: Double = 0.0,    // Fiat pending admin verification
    val totalInjectionsCount: Int = 0,
    val pendingInjectionsCount: Int = 0,

    // 2. Bank to Exchange
    val totalFiatToExchange: Double = 0.0,      // Fiat moved from Bank to Binance
    val bankToExchangeCount: Int = 0,

    // 3. USDT Acquisitions
    val totalUsdtPurchased: Double = 0.0,       // Gross USDT acquired on Binance
    val totalTradingFeesUsdt: Double = 0.0,     // Total fees on buys
    val averageBuyRate: Double = 0.0,           // Avg exchange rate

    // 4. USDT Distributions
    val totalUsdtDistributed: Double = 0.0,     // Distributed to member trading accounts
    val totalDistributionFeesUsdt: Double = 0.0,// Network gas fees
    val distributionsCount: Int = 0,

    // 5. Liquidations
    val totalUsdtSold: Double = 0.0,            // Sold back to fiat
    val totalFiatRealised: Double = 0.0,        // Realized fiat proceeds
    val liquidationsCount: Int = 0,

    // 6. Global Pool Reconciliation & Solvency
    val remainingPoolUsdt: Double = 0.0,        // Total USDT Purchased - Total USDT Sold
    val unallocatedCentralUsdt: Double = 0.0,   // Acquired USDT on Binance not yet distributed to members
    val centralBankBalanceFiat: Double = 0.0,   // Injected Fiat still in central bank not yet sent to Binance
    val netRealizedProfitLossFiat: Double = 0.0,// Realized Fiat - Fiat equivalent of sold volume

    // Multi-currency display metadata
    val currency: AppCurrency = AppCurrency.USD,
    val exchangeRateUsed: Double = 1.0
) {
    /**
     * Converts fiat metrics to the user's active display currency using the current exchange rate.
     * USDT amounts remain intact as cryptocurrency units.
     */
    fun toCurrency(targetCurrency: AppCurrency, ratePerUsd: Double): PoolMetrics {
        if (targetCurrency == AppCurrency.USD) {
            return this.copy(currency = AppCurrency.USD, exchangeRateUsed = 1.0)
        }
        return copy(
            currency = targetCurrency,
            exchangeRateUsed = ratePerUsd,
            totalFiatSpent = totalFiatSpent * ratePerUsd,
            pendingFiatInjections = pendingFiatInjections * ratePerUsd,
            totalFiatToExchange = totalFiatToExchange * ratePerUsd,
            totalFiatRealised = totalFiatRealised * ratePerUsd,
            centralBankBalanceFiat = centralBankBalanceFiat * ratePerUsd,
            netRealizedProfitLossFiat = netRealizedProfitLossFiat * ratePerUsd
        )
    }
}
