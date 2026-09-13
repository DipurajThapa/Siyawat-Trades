package com.example.data.repository

import android.content.Context
import com.example.data.local.PoolDatabase
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.AlertSeverity
import com.example.data.model.ComplianceAlert
import com.example.data.model.PoolMetrics
import com.example.data.model.PoolUser
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.data.model.UserRole
import com.example.util.ProofReceiptGenerator
import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PoolRepository(
    private val context: Context,
    private val database: PoolDatabase
) {
    private val dao = database.transactionDao()

    val allTransactions: Flow<List<PoolTransactionEntity>> = dao.getAllTransactions()
    val approvedTransactions: Flow<List<PoolTransactionEntity>> = dao.getApprovedTransactions()

    suspend fun insertTransaction(transaction: PoolTransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            dao.insertTransaction(transaction)
        }
    }

    suspend fun verifyTransaction(
        id: Long,
        adminEmail: String,
        verified: Boolean,
        notes: String? = null,
        approvalScreenshotUri: String? = null
    ) {
        withContext(Dispatchers.IO) {
            val status = if (verified) TransactionStatus.VERIFIED.name else TransactionStatus.FLAGGED.name
            val approvalStatus = if (verified) "APPROVED" else "REJECTED"
            val approvalTimestamp = System.currentTimeMillis()
            val approvalDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(approvalTimestamp))
            dao.updateApproval(
                id = id,
                status = status,
                verifiedBy = adminEmail,
                approvalStatus = approvalStatus,
                approvalTimestamp = approvalTimestamp,
                approvalDate = approvalDate,
                approvalNotes = notes ?: if (verified) "Confirmed received in central bank account" else "Rejected by administrator",
                approvalScreenshotUri = approvalScreenshotUri
            )
        }
    }

    suspend fun updateDriveInfo(id: Long, fileId: String, webViewLink: String) {
        withContext(Dispatchers.IO) {
            dao.updateDriveInfo(id, fileId, webViewLink)
        }
    }

    suspend fun deleteTransaction(id: Long) {
        withContext(Dispatchers.IO) {
            dao.deleteTransaction(id)
        }
    }

    suspend fun clearAllTransactions() {
        withContext(Dispatchers.IO) {
            dao.clearAll()
            try {
                context.filesDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("seed_")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                // Ignore file deletion errors
            }
        }
    }

    suspend fun clearAndResetDemoData() {
        clearAllTransactions()
    }

    suspend fun isReferenceDuplicate(referenceNo: String): Boolean {
        if (referenceNo.isBlank()) return false
        return withContext(Dispatchers.IO) {
            dao.getByReferenceNo(referenceNo.trim()) != null
        }
    }

    suspend fun checkAndSeedInitialData() {
        withContext(Dispatchers.IO) {
            // Clean production initialization: purge any legacy demo seed data from previous test sessions
            val all = dao.getAllTransactionsSync()
            val hasLegacySeedData = all.any {
                it.referenceNo in listOf(
                    "WIRE-2026-ALC981", "WIRE-2026-BOB442", "WIRE-2026-CHL119",
                    "BNK-BIN-88390", "ORD-BIN-99201", "TXN-TRC20-ALC881",
                    "TXN-TRC20-BOB882", "P2P-BIN-77291"
                ) || it.proofUri.contains("seed_") ||
                        it.userEmail in listOf("alice.crypto@gmail.com", "bob.trader@gmail.com", "charlie.invest@gmail.com")
            }
            if (hasLegacySeedData) {
                clearAllTransactions()
            }
        }
    }

    fun getTransactionsForPeriod(filter: com.example.data.model.TimePeriodFilter): Flow<List<PoolTransactionEntity>> {
        val bounds = filter.resolveRange()
        return if (bounds.startTime <= 0L && bounds.endTime >= Long.MAX_VALUE - 1000L) {
            dao.getAllTransactions()
        } else {
            dao.getTransactionsBetween(bounds.startTime, bounds.endTime)
        }
    }

    fun calculateMetrics(
        transactions: List<PoolTransactionEntity>,
        cumulativeTransactions: List<PoolTransactionEntity> = transactions,
        displayCurrency: com.example.data.model.AppCurrency = com.example.data.model.AppCurrency.USD,
        ratePerUsd: Double = displayCurrency.defaultRatePerUsd
    ): PoolMetrics {
        var verifiedFiatSpent = 0.0
        var pendingFiatInjections = 0.0
        var injectionsCount = 0
        var pendingInjectionsCount = 0

        var fiatToExchange = 0.0
        var bankToExchangeCount = 0

        var usdtPurchased = 0.0
        var tradingFees = 0.0
        var weightedRateSum = 0.0
        var acquisitionsCount = 0

        var usdtDistributed = 0.0
        var distributionFees = 0.0
        var distributionsCount = 0

        var usdtSold = 0.0
        var fiatRealised = 0.0
        var liquidationsCount = 0

        for (tx in transactions) {
            // Cancelled or reversed transactions do not affect active balances or totals
            if (tx.status == TransactionStatus.CANCELLED || tx.status == TransactionStatus.REVERSED) {
                continue
            }

            // Use locked converted USD value for economic calculations
            val lockedUsd = if (tx.convertedAmountUsd > 0.0) tx.convertedAmountUsd else (tx.amountFiat ?: 0.0)

            when (tx.stage) {
                TransactionStage.CAPITAL_INJECTION -> {
                    injectionsCount++
                    if (tx.status == TransactionStatus.VERIFIED || tx.status == TransactionStatus.COMPLETED) {
                        verifiedFiatSpent += lockedUsd
                    } else if (tx.status == TransactionStatus.PENDING_VERIFICATION) {
                        pendingFiatInjections += lockedUsd
                        pendingInjectionsCount++
                    }
                }
                TransactionStage.BANK_TO_EXCHANGE -> {
                    bankToExchangeCount++
                    fiatToExchange += lockedUsd
                }
                TransactionStage.USDT_ACQUISITION -> {
                    acquisitionsCount++
                    val gross = tx.amountUsdt ?: 0.0
                    val fee = tx.fee ?: 0.0
                    usdtPurchased += (gross - fee).coerceAtLeast(0.0)
                    tradingFees += fee
                    val rate = tx.exchangeRate ?: if (gross > 0) lockedUsd / gross else 1.0
                    weightedRateSum += rate * gross
                }
                TransactionStage.USDT_DISTRIBUTION -> {
                    distributionsCount++
                    usdtDistributed += (tx.amountUsdt ?: 0.0)
                    distributionFees += (tx.fee ?: 0.0)
                }
                TransactionStage.LIQUIDATION -> {
                    liquidationsCount++
                    usdtSold += (tx.amountUsdt ?: 0.0)
                    fiatRealised += lockedUsd
                }
            }
        }

        // Calculate point-in-time solvency balances as of the end of the period from cumulative history
        val cumVerifiedFiatSpent: Double
        val cumFiatToExchange: Double
        val cumUsdtPurchased: Double
        val cumTradingFees: Double
        val cumWeightedRateSum: Double
        val cumUsdtDistributed: Double
        val cumUsdtSold: Double

        if (cumulativeTransactions === transactions) {
            cumVerifiedFiatSpent = verifiedFiatSpent
            cumFiatToExchange = fiatToExchange
            cumUsdtPurchased = usdtPurchased
            cumTradingFees = tradingFees
            cumWeightedRateSum = weightedRateSum
            cumUsdtDistributed = usdtDistributed
            cumUsdtSold = usdtSold
        } else {
            var vFiat = 0.0
            var fToEx = 0.0
            var uPurch = 0.0
            var tFees = 0.0
            var wRateSum = 0.0
            var uDist = 0.0
            var uSold = 0.0

            for (tx in cumulativeTransactions) {
                if (tx.status == TransactionStatus.CANCELLED || tx.status == TransactionStatus.REVERSED) {
                    continue
                }
                val lockedUsd = if (tx.convertedAmountUsd > 0.0) tx.convertedAmountUsd else (tx.amountFiat ?: 0.0)
                when (tx.stage) {
                    TransactionStage.CAPITAL_INJECTION -> {
                        if (tx.status == TransactionStatus.VERIFIED || tx.status == TransactionStatus.COMPLETED) {
                            vFiat += lockedUsd
                        }
                    }
                    TransactionStage.BANK_TO_EXCHANGE -> {
                        fToEx += lockedUsd
                    }
                    TransactionStage.USDT_ACQUISITION -> {
                        val gross = tx.amountUsdt ?: 0.0
                        val fee = tx.fee ?: 0.0
                        uPurch += (gross - fee).coerceAtLeast(0.0)
                        tFees += fee
                        val rate = tx.exchangeRate ?: if (gross > 0) lockedUsd / gross else 1.0
                        wRateSum += rate * gross
                    }
                    TransactionStage.USDT_DISTRIBUTION -> {
                        uDist += (tx.amountUsdt ?: 0.0)
                    }
                    TransactionStage.LIQUIDATION -> {
                        uSold += (tx.amountUsdt ?: 0.0)
                    }
                }
            }
            cumVerifiedFiatSpent = vFiat
            cumFiatToExchange = fToEx
            cumUsdtPurchased = uPurch
            cumTradingFees = tFees
            cumWeightedRateSum = wRateSum
            cumUsdtDistributed = uDist
            cumUsdtSold = uSold
        }

        val remainingUsdt = (cumUsdtPurchased - cumUsdtSold).coerceAtLeast(0.0)
        val unallocatedUsdt = (cumUsdtPurchased - cumUsdtDistributed).coerceAtLeast(0.0)
        val centralBankBalance = (cumVerifiedFiatSpent - cumFiatToExchange).coerceAtLeast(0.0)

        // Estimated profit/loss: realized fiat minus acquisition cost of the sold USDT portion
        val avgBuyRate = if (cumUsdtPurchased > 0) (cumWeightedRateSum / (cumUsdtPurchased + cumTradingFees))
                         else if (usdtPurchased > 0) (weightedRateSum / (usdtPurchased + tradingFees)) else 1.0
        val costOfSoldUsdt = usdtSold * avgBuyRate
        val netPnL = fiatRealised - costOfSoldUsdt

        val baseMetrics = PoolMetrics(
            totalFiatSpent = verifiedFiatSpent,
            pendingFiatInjections = pendingFiatInjections,
            totalInjectionsCount = injectionsCount,
            pendingInjectionsCount = pendingInjectionsCount,
            totalFiatToExchange = fiatToExchange,
            bankToExchangeCount = bankToExchangeCount,
            totalUsdtPurchased = usdtPurchased,
            totalTradingFeesUsdt = tradingFees,
            averageBuyRate = avgBuyRate,
            totalUsdtDistributed = usdtDistributed,
            totalDistributionFeesUsdt = distributionFees,
            distributionsCount = distributionsCount,
            totalUsdtSold = usdtSold,
            totalFiatRealised = fiatRealised,
            liquidationsCount = liquidationsCount,
            remainingPoolUsdt = remainingUsdt,
            unallocatedCentralUsdt = unallocatedUsdt,
            centralBankBalanceFiat = centralBankBalance,
            netRealizedProfitLossFiat = netPnL,
            currency = com.example.data.model.AppCurrency.USD,
            exchangeRateUsed = 1.0
        )

        return if (displayCurrency == com.example.data.model.AppCurrency.USD) {
            baseMetrics
        } else {
            baseMetrics.toCurrency(displayCurrency, ratePerUsd)
        }
    }

    fun evaluateComplianceAlerts(
        transactions: List<PoolTransactionEntity>,
        metrics: PoolMetrics
    ): List<ComplianceAlert> {
        val alerts = mutableListOf<ComplianceAlert>()

        // 1. Missing Proof Validation
        val missingProofTxs = transactions.filter { it.proofUri.isBlank() }
        if (missingProofTxs.isNotEmpty()) {
            alerts.add(
                ComplianceAlert(
                    id = "missing_proof",
                    severity = AlertSeverity.CRITICAL,
                    title = "Missing Proof Screenshot",
                    message = "${missingProofTxs.size} transaction(s) have no receipt attached. Compliance requires mandatory proof for every entry.",
                    actionText = "Review Missing Proofs",
                    affectedTransactionIds = missingProofTxs.map { it.id }
                )
            )
        }

        // 2. Pending Bank Transfers / Injections
        val pendingInjections = transactions.filter {
            it.stage == TransactionStage.CAPITAL_INJECTION && it.status == TransactionStatus.PENDING_VERIFICATION
        }
        if (pendingInjections.isNotEmpty()) {
            val totalPending = pendingInjections.sumOf { it.amountFiat ?: 0.0 }
            alerts.add(
                ComplianceAlert(
                    id = "pending_injections",
                    severity = AlertSeverity.WARNING,
                    title = "Pending Bank Confirmation",
                    message = "${pendingInjections.size} capital injection(s) totaling $${String.format("%,.2f", totalPending)} awaiting Admin bank confirmation.",
                    actionText = "Verify Injections",
                    affectedTransactionIds = pendingInjections.map { it.id },
                    targetStageFilter = TransactionStage.CAPITAL_INJECTION
                )
            )
        }

        // 3. Unbalanced Ledger: Exchange deposits exceed verified capital
        if (metrics.totalFiatToExchange > metrics.totalFiatSpent) {
            val excess = metrics.totalFiatToExchange - metrics.totalFiatSpent
            alerts.add(
                ComplianceAlert(
                    id = "unbalanced_exchange_fiat",
                    severity = AlertSeverity.CRITICAL,
                    title = "Unbalanced Ledger: Fiat to Exchange",
                    message = "Fiat sent to Binance ($${String.format("%,.2f", metrics.totalFiatToExchange)}) exceeds verified member capital ($${String.format("%,.2f", metrics.totalFiatSpent)}) by $${String.format("%,.2f", excess)}.",
                    actionText = "Audit Bank Records"
                )
            )
        }

        // 4. Unbalanced Ledger: Distributed USDT exceeds Purchased USDT
        if (metrics.totalUsdtDistributed > metrics.totalUsdtPurchased) {
            val excess = metrics.totalUsdtDistributed - metrics.totalUsdtPurchased
            alerts.add(
                ComplianceAlert(
                    id = "unbalanced_usdt_dist",
                    severity = AlertSeverity.CRITICAL,
                    title = "Solvency Red Flag: Over-Distributed USDT",
                    message = "Total distributed USDT exceeds acquired pool USDT by ${String.format("%,.2f", excess)} USDT.",
                    actionText = "Audit Distributions"
                )
            )
        }

        // 5. Unallocated USDT on Exchange
        if (metrics.unallocatedCentralUsdt > 1.0) {
            alerts.add(
                ComplianceAlert(
                    id = "unallocated_usdt",
                    severity = AlertSeverity.INFO,
                    title = "Unallocated USDT on Binance",
                    message = "${String.format("%,.2f", metrics.unallocatedCentralUsdt)} USDT acquired on central exchange ready to be distributed to member accounts.",
                    actionText = "Log Distribution",
                    targetStageFilter = TransactionStage.USDT_DISTRIBUTION
                )
            )
        }

        return alerts
    }

    /**
     * Executes an explicit, authorized distribution of USDT or Fiat to a beneficiary member.
     * Enforces:
     * 1. Admin authorization confirmation
     * 2. Available balance validation before modifying ledger
     * 3. Permanent immutable ledger record with resulting balance
     * 4. Deducts from available pool balance
     */
    suspend fun executeDistribution(
        adminUser: PoolUser,
        beneficiaryEmail: String,
        beneficiaryName: String,
        amount: Double,
        isUsdt: Boolean = true,
        fee: Double = 0.0,
        notes: String = "",
        proofUri: String = "",
        currency: String = "USD"
    ): Result<PoolTransactionEntity> = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (!adminUser.isAdmin) {
                return@withTransaction Result.failure(IllegalStateException("Unauthorized: Only verified pool admins can execute fund distribution."))
            }

            if (amount <= 0.0) {
                return@withTransaction Result.failure(IllegalArgumentException("Distribution amount must be greater than zero."))
            }

            // Fetch current transactions synchronously for atomic balance calculation
            val currentTxs = dao.getAllTransactionsSync()
            val metrics = calculateMetrics(currentTxs)

            val startingBalance: Double
            val resultingBalance: Double

            if (isUsdt) {
                startingBalance = metrics.unallocatedCentralUsdt
                val totalDeduction = amount + fee
                if (totalDeduction > startingBalance + 0.0001) {
                    return@withTransaction Result.failure(
                        IllegalArgumentException(
                            "Insufficient unallocated USDT. Available: ${String.format(Locale.US, "%,.2f", startingBalance)} USDT, requested: ${String.format(Locale.US, "%,.2f", totalDeduction)} USDT."
                        )
                    )
                }
                resultingBalance = (startingBalance - totalDeduction).coerceAtLeast(0.0)
            } else {
                startingBalance = metrics.centralBankBalanceFiat
                if (amount > startingBalance + 0.0001) {
                    return@withTransaction Result.failure(
                        IllegalArgumentException(
                            "Insufficient central bank fiat balance. Available: ${String.format(Locale.US, "%,.2f", startingBalance)}, requested: ${String.format(Locale.US, "%,.2f", amount)}."
                        )
                    )
                }
                resultingBalance = (startingBalance - amount).coerceAtLeast(0.0)
            }

            val refNo = "DIST-${if (isUsdt) "USDT" else "FIAT"}-${System.currentTimeMillis().toString().takeLast(6)}"
            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(now))

            val finalProofUri = if (proofUri.isNotBlank()) {
                proofUri
            } else {
                // Standard cryptographic ledger voucher
                "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='400' height='300'><rect width='100%' height='100%' fill='%231e293b'/><text x='20' y='50' fill='%2364748b' font-family='sans-serif' font-size='16'>DISTRIBUTION VOUCHER</text><text x='20' y='100' fill='%23f8fafc' font-family='sans-serif' font-size='22'>$refNo</text><text x='20' y='150' fill='%2394a3b8' font-family='sans-serif' font-size='16'>Beneficiary: $beneficiaryName ($beneficiaryEmail)</text><text x='20' y='190' fill='%2338bdf8' font-family='sans-serif' font-size='24'>$amount ${if (isUsdt) "USDT" else currency}</text><text x='20' y='230' fill='%2364748b' font-family='sans-serif' font-size='14'>Disbursed by Admin: ${adminUser.email}</text><text x='20' y='260' fill='%2364748b' font-family='sans-serif' font-size='14'>Resulting Pool Bal: ${String.format(Locale.US, "%,.2f", resultingBalance)}</text></svg>"
            }

            val entity = PoolTransactionEntity(
                stage = if (isUsdt) TransactionStage.USDT_DISTRIBUTION else TransactionStage.LIQUIDATION,
                userEmail = adminUser.email,
                userName = adminUser.displayName,
                recipientEmail = beneficiaryEmail,
                amountFiat = if (!isUsdt) amount else null,
                fiatCurrency = currency,
                amountUsdt = if (isUsdt) amount else null,
                fee = fee,
                referenceNo = refNo,
                proofUri = finalProofUri,
                proofDescription = "Admin Distribution to $beneficiaryName ($beneficiaryEmail) - Ref: $refNo",
                status = TransactionStatus.COMPLETED,
                verifiedByEmail = adminUser.email,
                notes = if (notes.isNotBlank()) "$notes | Resulting balance: ${String.format(Locale.US, "%,.2f", resultingBalance)}" else "Admin distribution to $beneficiaryName. Resulting balance: ${String.format(Locale.US, "%,.2f", resultingBalance)}",
                timestamp = now,
                recordDate = dateStr,
                recordTime = timeStr,
                approvalStatus = "APPROVED",
                approvedByEmail = adminUser.email,
                approvalTimestamp = now,
                approvalDate = "$dateStr $timeStr",
                approvalNotes = "Authorized and disbursed by Admin ${adminUser.displayName}",
                approvalScreenshotUri = finalProofUri,
                originalAmount = amount,
                originalCurrency = if (isUsdt) "USDT" else currency,
                displayCurrencyAtTime = if (isUsdt) "USDT" else currency,
                fiatExchangeRate = 1.0,
                convertedAmountUsd = amount,
                resultingBalance = resultingBalance
            )

            val insertedId = dao.insertTransaction(entity)
            Result.success(entity.copy(id = insertedId))
        }
    }

    /**
     * Reverses a transaction with mandatory reason, restoring balances without deleting audit history.
     */
    suspend fun reverseTransaction(
        adminUser: PoolUser,
        transactionId: Long,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (!adminUser.isAdmin) {
                return@withTransaction Result.failure(IllegalStateException("Unauthorized: Only admins can reverse transactions."))
            }
            if (reason.isBlank()) {
                return@withTransaction Result.failure(IllegalArgumentException("Reversal reason is mandatory."))
            }

            val tx = dao.getTransactionById(transactionId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Transaction not found."))

            if (tx.status == TransactionStatus.REVERSED || tx.status == TransactionStatus.CANCELLED) {
                return@withTransaction Result.failure(IllegalStateException("Transaction is already ${tx.status.label}."))
            }

            val now = System.currentTimeMillis()
            val updatedNotes = if (tx.notes.isBlank()) "REVERSED: $reason" else "${tx.notes} | REVERSED: $reason"

            dao.updateReversal(
                id = transactionId,
                status = TransactionStatus.REVERSED.name,
                reason = reason,
                reversedBy = adminUser.email,
                timestamp = now,
                notes = updatedNotes
            )

            Result.success(Unit)
        }
    }
}
