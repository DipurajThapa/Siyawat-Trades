package com.example.data.model

data class UsdtInventoryLot(
    val lotId: String,
    val acquisitionTxId: Long,
    val timestamp: Long,
    val originalQuantity: Double,
    val remainingQuantity: Double,
    val unitCostFiat: Double, // Effective acquisition price in USD
    val fiatCurrency: String = "USD",
    val feeUsdt: Double = 0.0,
    val exchangeRef: String = ""
)

data class FifoDepletionRecord(
    val liquidationTxId: Long,
    val lotId: String,
    val quantityDepleted: Double,
    val unitCostFiat: Double,
    val unitSaleFiat: Double,
    val grossGainLossFiat: Double,
    val timestamp: Long
)

data class FifoCalculationResult(
    val totalUsdtPurchased: Double,
    val totalUsdtSold: Double,
    val remainingUsdtInventory: Double,
    val totalRealizedGainLossFiat: Double,
    val totalCostBasisOfSoldUsdt: Double,
    val totalSaleProceedsFiat: Double,
    val totalFeesFiat: Double,
    val activeLots: List<UsdtInventoryLot> = emptyList(),
    val depletions: List<FifoDepletionRecord> = emptyList()
)
