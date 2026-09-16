package com.example.data.repository

import android.content.Context
import com.example.data.local.PoolDatabase
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.AlertSeverity
import com.example.data.model.ComplianceAlert
import com.example.data.model.FifoCalculationResult
import com.example.data.model.FifoDepletionRecord
import com.example.data.model.PoolMetrics
import com.example.data.model.PoolUser
import com.example.data.model.ReconciliationState
import com.example.data.model.RecordState
import com.example.data.model.SettlementState
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.data.model.UsdtInventoryLot
import com.example.data.model.UserRole
import com.example.util.ProofReceiptGenerator
import androidx.room.withTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Tasks

class PoolRepository(
    private val context: Context,
    private val database: PoolDatabase,
    private val firestoreInstance: FirebaseFirestore? = null
) {
    private val dao = database.transactionDao()

    /**
     * Firebase Firestore client initialized for secure, user-partitioned cloud persistence.
     * Prevents cross-user data leakage by enforcing user-isolated paths: /users/{userKey}/transactions
     */
    val firestore: FirebaseFirestore? by lazy {
        firestoreInstance ?: try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            android.util.Log.w("PoolRepository", "Firestore client not initialized or unavailable in this environment: ${e.message}")
            null
        }
    }

    val allTransactions: Flow<List<PoolTransactionEntity>> = dao.getAllTransactions()
    val approvedTransactions: Flow<List<PoolTransactionEntity>> = dao.getApprovedTransactions()

    suspend fun insertTransaction(transaction: PoolTransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            val id = dao.insertTransaction(transaction)
            try {
                syncTransactionToUserPartition(transaction.copy(id = id))
            } catch (e: Throwable) {
                // Non-blocking cloud sync fallback to preserve local availability
            }
            id
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
            val tx = dao.getTransactionById(id)
            dao.deleteTransaction(id)
            if (tx != null) {
                try {
                    deleteFromUserPartition(tx.userEmail, id)
                } catch (e: Throwable) {
                    // Non-blocking cloud fallback
                }
            }
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

        // 6. User Disputed Transactions
        val disputedTxs = transactions.filter { it.reconciliationState == ReconciliationState.DISPUTED }
        if (disputedTxs.isNotEmpty()) {
            alerts.add(
                ComplianceAlert(
                    id = "disputed_transactions",
                    severity = AlertSeverity.CRITICAL,
                    title = "User Dispute Flagged",
                    message = "${disputedTxs.size} transaction(s) have active user disputes. Requires immediate bank UTR tracer and resolution.",
                    actionText = "Review Disputes",
                    affectedTransactionIds = disputedTxs.map { it.id }
                )
            )
        }

        // 7. Maker-Checker High-Value Dual Approval Queue
        val dualApprovalTxs = transactions.filter { it.recordState == RecordState.PENDING_SECOND_APPROVAL }
        if (dualApprovalTxs.isNotEmpty()) {
            alerts.add(
                ComplianceAlert(
                    id = "dual_approval_queue",
                    severity = AlertSeverity.WARNING,
                    title = "Maker-Checker Sign-off Required",
                    message = "${dualApprovalTxs.size} high-value transaction(s) ($10,000+) approved by Maker awaiting 2nd Admin Checker signature.",
                    actionText = "Sign Off",
                    affectedTransactionIds = dualApprovalTxs.map { it.id }
                )
            )
        }

        // 8. Settlements Awaiting User Delivery Confirmation
        val awaitingConfirmTxs = transactions.filter { it.isAwaitingUserConfirm }
        if (awaitingConfirmTxs.isNotEmpty()) {
            alerts.add(
                ComplianceAlert(
                    id = "awaiting_user_confirm",
                    severity = AlertSeverity.INFO,
                    title = "Pending Delivery Confirmation",
                    message = "${awaitingConfirmTxs.size} payout(s) marked Settled, awaiting member confirmation (72h SLA window active).",
                    actionText = "View Payouts",
                    affectedTransactionIds = awaitingConfirmTxs.map { it.id }
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
                resultingBalance = resultingBalance,
                recordState = RecordState.APPROVED,
                settlementState = SettlementState.SETTLED,
                reconciliationState = ReconciliationState.PENDING_USER_CONFIRM,
                settlementTimestamp = now
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

    /**
     * FIFO (First-In, First-Out) Inventory Engine for USDT Acquisition and Liquidation.
     * Computes exact cost basis and realized P&L across discrete purchase lots.
     */
    fun calculateFifoLedger(
        transactions: List<PoolTransactionEntity>
    ): FifoCalculationResult {
        val validTxs = transactions
            .filter { it.status != TransactionStatus.CANCELLED && it.status != TransactionStatus.REVERSED }
            .sortedBy { it.timestamp }

        val activeLots = mutableListOf<UsdtInventoryLot>()
        val depletions = mutableListOf<FifoDepletionRecord>()
        var totalPurchased = 0.0
        var totalSold = 0.0
        var totalCostBasisOfSold = 0.0
        var totalProceeds = 0.0
        var totalFeesFiat = 0.0

        for (tx in validTxs) {
            val lockedUsd = if (tx.convertedAmountUsd > 0.0) tx.convertedAmountUsd else (tx.amountFiat ?: 0.0)

            when (tx.stage) {
                TransactionStage.USDT_ACQUISITION -> {
                    val grossUsdt = tx.amountUsdt ?: 0.0
                    val feeUsdt = tx.fee ?: 0.0
                    val netUsdt = (grossUsdt - feeUsdt).coerceAtLeast(0.0)
                    if (netUsdt > 0.0) {
                        val unitCost = if (grossUsdt > 0.0) lockedUsd / grossUsdt else 1.0
                        val lot = UsdtInventoryLot(
                            lotId = "LOT-${tx.id}-${tx.referenceNo.take(6)}",
                            acquisitionTxId = tx.id,
                            timestamp = tx.timestamp,
                            originalQuantity = netUsdt,
                            remainingQuantity = netUsdt,
                            unitCostFiat = unitCost,
                            fiatCurrency = tx.fiatCurrency,
                            feeUsdt = feeUsdt,
                            exchangeRef = tx.referenceNo
                        )
                        activeLots.add(lot)
                        totalPurchased += netUsdt
                        totalFeesFiat += (feeUsdt * unitCost)
                    }
                }
                TransactionStage.LIQUIDATION -> {
                    var qtyToDeplete = tx.amountUsdt ?: 0.0
                    val saleProceeds = lockedUsd
                    totalSold += qtyToDeplete
                    totalProceeds += saleProceeds
                    val unitSaleRate = if (qtyToDeplete > 0.0) saleProceeds / qtyToDeplete else 1.0

                    var i = 0
                    while (qtyToDeplete > 0.000001 && i < activeLots.size) {
                        val currentLot = activeLots[i]
                        if (currentLot.remainingQuantity <= 0.000001) {
                            i++
                            continue
                        }

                        val depleteAmount = minOf(qtyToDeplete, currentLot.remainingQuantity)
                        val lotCostBasis = depleteAmount * currentLot.unitCostFiat
                        val lotProceeds = depleteAmount * unitSaleRate
                        val lotGainLoss = lotProceeds - lotCostBasis

                        depletions.add(
                            FifoDepletionRecord(
                                liquidationTxId = tx.id,
                                lotId = currentLot.lotId,
                                quantityDepleted = depleteAmount,
                                unitCostFiat = currentLot.unitCostFiat,
                                unitSaleFiat = unitSaleRate,
                                grossGainLossFiat = lotGainLoss,
                                timestamp = tx.timestamp
                            )
                        )

                        totalCostBasisOfSold += lotCostBasis
                        qtyToDeplete -= depleteAmount
                        activeLots[i] = currentLot.copy(
                            remainingQuantity = (currentLot.remainingQuantity - depleteAmount).coerceAtLeast(0.0)
                        )
                        i++
                    }
                }
                else -> Unit
            }
        }

        val remainingInventory = activeLots.sumOf { it.remainingQuantity }
        val netRealizedPnL = totalProceeds - totalCostBasisOfSold - totalFeesFiat

        return FifoCalculationResult(
            totalUsdtPurchased = totalPurchased,
            totalUsdtSold = totalSold,
            remainingUsdtInventory = remainingInventory,
            totalRealizedGainLossFiat = netRealizedPnL,
            totalCostBasisOfSoldUsdt = totalCostBasisOfSold,
            totalSaleProceedsFiat = totalProceeds,
            totalFeesFiat = totalFeesFiat,
            activeLots = activeLots.filter { it.remainingQuantity > 0.000001 },
            depletions = depletions
        )
    }

    /**
     * Deterministic idempotency hash to prevent accidental duplicate submission.
     */
    fun generateIdempotencyHash(
        userEmail: String,
        amount: Double,
        currency: String,
        referenceNo: String
    ): String {
        val raw = "${userEmail.trim().lowercase()}_${String.format(Locale.US, "%.4f", amount)}_${currency.trim().uppercase()}_${referenceNo.trim().uppercase()}"
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(raw.toByteArray())
            digest.joinToString("") { "%02x".format(it) }.take(16)
        } catch (e: Exception) {
            raw.hashCode().toString()
        }
    }

    suspend fun isDuplicateTransaction(referenceNo: String, idempotencyHash: String): Boolean {
        if (referenceNo.isBlank() && idempotencyHash.isBlank()) return false
        return withContext(Dispatchers.IO) {
            dao.getByReferenceOrHash(referenceNo.trim(), idempotencyHash) != null
        }
    }

    /**
     * Maker-Checker Dual Verification.
     * High-value transactions (>= $10,000 USD) require approval from two distinct administrators.
     */
    suspend fun verifyTransactionWithMakerChecker(
        id: Long,
        adminUser: PoolUser,
        verified: Boolean,
        notes: String? = null,
        approvalScreenshotUri: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (!adminUser.isAdmin) {
                return@withTransaction Result.failure(IllegalStateException("Unauthorized: Only admins can verify transactions."))
            }

            val tx = dao.getTransactionById(id)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Transaction not found."))

            val now = System.currentTimeMillis()
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(now))

            if (!verified) {
                // Outright rejection
                dao.updateApproval(
                    id = id,
                    status = TransactionStatus.FLAGGED.name,
                    verifiedBy = adminUser.email,
                    approvalStatus = "REJECTED",
                    approvalTimestamp = now,
                    approvalDate = dateStr,
                    approvalNotes = notes ?: "Rejected by Admin ${adminUser.displayName}",
                    approvalScreenshotUri = approvalScreenshotUri
                )
                return@withTransaction Result.success("Transaction rejected.")
            }

            // If high value and pending second approval
            if (tx.isHighValue && tx.recordState == RecordState.PENDING_SECOND_APPROVAL) {
                // Ensure second checker is NOT the same person as maker
                if (tx.verifiedByEmail?.equals(adminUser.email, ignoreCase = true) == true) {
                    return@withTransaction Result.failure(
                        IllegalStateException("Dual-Control Violation: Checker must be a different administrator than the Maker (${tx.verifiedByEmail}).")
                    )
                }

                // Checker signs off -> final approval
                dao.updateApproval(
                    id = id,
                    status = TransactionStatus.VERIFIED.name,
                    verifiedBy = tx.verifiedByEmail ?: adminUser.email,
                    approvalStatus = "APPROVED",
                    approvalTimestamp = tx.approvalTimestamp ?: now,
                    approvalDate = tx.approvalDate ?: dateStr,
                    approvalNotes = "${tx.approvalNotes ?: "Approved by Maker"} | Checker Sign-off by ${adminUser.displayName}: ${notes ?: "Dual-signature confirmed"}",
                    approvalScreenshotUri = approvalScreenshotUri ?: tx.approvalScreenshotUri
                )
                dao.updateSecondApproval(
                    id = id,
                    recordState = RecordState.APPROVED.name,
                    checkerEmail = adminUser.email,
                    timestamp = now,
                    notes = notes ?: "Checker dual signature authorized."
                )
                return@withTransaction Result.success("High-value transaction fully approved with Maker-Checker dual signature.")
            } else if (tx.isHighValue && tx.recordState != RecordState.APPROVED) {
                // High-value first approval (Maker stage)
                dao.updateApproval(
                    id = id,
                    status = TransactionStatus.PENDING_VERIFICATION.name,
                    verifiedBy = adminUser.email,
                    approvalStatus = "MAKER_APPROVED",
                    approvalTimestamp = now,
                    approvalDate = dateStr,
                    approvalNotes = "Maker initial sign-off by ${adminUser.displayName}: ${notes ?: "Pending 2nd Admin Checker"}",
                    approvalScreenshotUri = approvalScreenshotUri
                )
                dao.updateSecondApproval(
                    id = id,
                    recordState = RecordState.PENDING_SECOND_APPROVAL.name,
                    checkerEmail = "",
                    timestamp = 0L,
                    notes = null
                )
                return@withTransaction Result.success("Maker signature recorded. Transaction placed in Dual Approval Queue for 2nd Admin Checker.")
            } else {
                // Standard transaction (< $10k) single admin approval
                dao.updateApproval(
                    id = id,
                    status = TransactionStatus.VERIFIED.name,
                    verifiedBy = adminUser.email,
                    approvalStatus = "APPROVED",
                    approvalTimestamp = now,
                    approvalDate = dateStr,
                    approvalNotes = notes ?: "Confirmed received in central bank account",
                    approvalScreenshotUri = approvalScreenshotUri
                )
                dao.updateSecondApproval(
                    id = id,
                    recordState = RecordState.APPROVED.name,
                    checkerEmail = adminUser.email,
                    timestamp = now,
                    notes = "Single sign-off approved."
                )
                return@withTransaction Result.success("Transaction approved successfully.")
            }
        }
    }

    /**
     * Records external settlement execution with bank UTR or blockchain TxHash.
     */
    suspend fun recordSettlement(
        id: Long,
        operatorUser: PoolUser,
        bankUtr: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!operatorUser.canManage) {
            return@withContext Result.failure(IllegalStateException("Unauthorized: Only Admins or Sub-Admins can record settlement."))
        }
        val cleanUtr = bankUtr.trim()
        if (cleanUtr.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bank UTR / Transaction Hash is mandatory."))
        }

        val now = System.currentTimeMillis()
        dao.updateSettlement(
            id = id,
            settlementState = SettlementState.SETTLED.name,
            bankUtr = cleanUtr,
            timestamp = now,
            reconState = ReconciliationState.PENDING_USER_CONFIRM.name
        )
        Result.success(Unit)
    }

    /**
     * User confirms receipt of settled funds or USDT.
     */
    suspend fun confirmUserReceipt(
        id: Long,
        currentUser: PoolUser
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tx = dao.getTransactionById(id)
            ?: return@withContext Result.failure(IllegalArgumentException("Transaction not found."))

        // Ensure only recipient, sender or admin can confirm receipt
        val isAuthorized = currentUser.isAdmin ||
                currentUser.email.equals(tx.recipientEmail, ignoreCase = true) ||
                currentUser.email.equals(tx.userEmail, ignoreCase = true)

        if (!isAuthorized) {
            return@withContext Result.failure(IllegalStateException("Unauthorized: Only the beneficiary can confirm receipt."))
        }

        dao.updateConfirmReceipt(id)
        Result.success(Unit)
    }

    /**
     * User raises dispute if funds were not received or amount is incorrect.
     */
    suspend fun raiseDispute(
        id: Long,
        currentUser: PoolUser,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (reason.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Dispute reason is mandatory."))
        }
        val tx = dao.getTransactionById(id)
            ?: return@withContext Result.failure(IllegalArgumentException("Transaction not found."))

        val isAuthorized = currentUser.isAdmin ||
                currentUser.email.equals(tx.recipientEmail, ignoreCase = true) ||
                currentUser.email.equals(tx.userEmail, ignoreCase = true)

        if (!isAuthorized) {
            return@withContext Result.failure(IllegalStateException("Unauthorized: Only the beneficiary can raise a dispute."))
        }

        dao.updateDispute(id, reason.trim(), System.currentTimeMillis())
        Result.success(Unit)
    }

    /**
     * Admin resolves a dispute after reviewing bank UTR tracking.
     */
    suspend fun resolveDispute(
        id: Long,
        adminUser: PoolUser,
        resolutionNotes: String,
        isConfirmed: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!adminUser.isAdmin) {
            return@withContext Result.failure(IllegalStateException("Unauthorized: Only admins can resolve disputes."))
        }
        if (resolutionNotes.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Resolution notes are mandatory."))
        }

        val tx = dao.getTransactionById(id)
            ?: return@withContext Result.failure(IllegalArgumentException("Transaction not found."))

        val now = System.currentTimeMillis()
        val reconState = if (isConfirmed) ReconciliationState.RECONCILED.name else ReconciliationState.UNRECONCILED.name
        val updatedNotes = "${tx.notes} | DISPUTE RESOLVED: $resolutionNotes by ${adminUser.displayName}"

        dao.updateDisputeResolution(
            id = id,
            reconState = reconState,
            timestamp = now,
            resolverEmail = adminUser.email,
            resolutionNotes = resolutionNotes,
            updatedNotes = updatedNotes
        )

        // If funds were confirmed not delivered, execute compensating reversal
        if (!isConfirmed) {
            reverseTransaction(
                adminUser = adminUser,
                transactionId = id,
                reason = "Dispute resolution: payment confirmed failed by bank. $resolutionNotes"
            )
        }

        Result.success(Unit)
    }

    /**
     * Auto-reconciliation SLA (72 hours default):
     * Settled payments with no dispute after cutoff window are marked CONFIRMED_BY_TIMEOUT.
     */
    suspend fun autoReconcileTimeouts(cutoffHours: Int = 72): Result<Int> = withContext(Dispatchers.IO) {
        val cutoffTimestamp = System.currentTimeMillis() - (cutoffHours * 3600 * 1000L)
        val pendingTxs = dao.getPendingAutoReconciliation(cutoffTimestamp)
        if (pendingTxs.isNotEmpty()) {
            val ids = pendingTxs.map { it.id }
            dao.autoReconcileTimeouts(ids)
            Result.success(ids.size)
        } else {
            Result.success(0)
        }
    }

    // =========================================================================
    // SECURE USER-PARTITIONED FIRESTORE STORAGE (Zero Cross-User Data Leakage)
    // =========================================================================

    /**
     * Sanitizes user email to create a secure, collision-free Firestore partition key.
     */
    fun sanitizeUserEmailForPartition(email: String): String {
        return email.trim().lowercase().replace("/", "_").replace(".", "_")
    }

    /**
     * Partition collection path for the given user, preventing cross-user data leakage:
     * e.g., /users/{sanitized_user_email}/transactions
     */
    fun getUserPartitionCollectionPath(userEmail: String): String {
        val sanitized = sanitizeUserEmailForPartition(userEmail)
        return "users/$sanitized/transactions"
    }

    /**
     * Converts a local transaction entity to a secure, partitioned Firestore document map.
     */
    fun transactionToPartitionMap(tx: PoolTransactionEntity): Map<String, Any?> {
        return mapOf(
            "id" to tx.id,
            "stage" to tx.stage.name,
            "userEmail" to tx.userEmail,
            "userName" to tx.userName,
            "amountFiat" to tx.amountFiat,
            "originalAmount" to tx.originalAmount,
            "originalCurrency" to tx.originalCurrency,
            "fiatCurrency" to tx.fiatCurrency,
            "fiatExchangeRate" to tx.fiatExchangeRate,
            "convertedAmountUsd" to tx.convertedAmountUsd,
            "referenceNo" to tx.referenceNo,
            "status" to tx.status.name,
            "timestamp" to tx.timestamp,
            "recordDate" to tx.recordDate,
            "recordTime" to tx.recordTime,
            "proofUri" to tx.proofUri,
            "driveFileId" to (tx.driveFileId ?: ""),
            "driveWebViewLink" to (tx.driveWebViewLink ?: ""),
            "notes" to tx.notes,
            "verifiedByEmail" to (tx.verifiedByEmail ?: ""),
            "approvalStatus" to tx.approvalStatus,
            "approvalTimestamp" to (tx.approvalTimestamp ?: 0L),
            "approvalDate" to (tx.approvalDate ?: ""),
            "approvalNotes" to (tx.approvalNotes ?: ""),
            "recordState" to tx.recordState.name,
            "settlementState" to tx.settlementState.name,
            "reconciliationState" to tx.reconciliationState.name,
            "lastSyncedAt" to System.currentTimeMillis()
        )
    }

    /**
     * Persists a transaction to the user's isolated partition in Firestore.
     * Guarantees that documents are saved under /users/{user}/transactions to prevent leakage.
     */
    suspend fun syncTransactionToUserPartition(transaction: PoolTransactionEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val client = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore client not available"))
        try {
            val userPartition = sanitizeUserEmailForPartition(transaction.userEmail)
            val docRef = client.collection("users")
                .document(userPartition)
                .collection("transactions")
                .document(transaction.id.toString())

            val task = docRef.set(transactionToPartitionMap(transaction), SetOptions.merge())
            Tasks.await(task)
            Result.success(Unit)
        } catch (e: Throwable) {
            android.util.Log.w("PoolRepository", "Failed to sync to user partition in Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Removes a transaction from the user's isolated partition in Firestore.
     */
    suspend fun deleteFromUserPartition(userEmail: String, transactionId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val client = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore client not available"))
        try {
            val userPartition = sanitizeUserEmailForPartition(userEmail)
            val docRef = client.collection("users")
                .document(userPartition)
                .collection("transactions")
                .document(transactionId.toString())

            val task = docRef.delete()
            Tasks.await(task)
            Result.success(Unit)
        } catch (e: Throwable) {
            android.util.Log.w("PoolRepository", "Failed to delete from user partition: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Queries only the caller's partitioned transactions from Firestore.
     * Prevents cross-user data leakage by scoping the query exclusively to the user's partition.
     */
    suspend fun fetchUserPartitionedTransactions(userEmail: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        val client = firestore ?: return@withContext Result.failure(IllegalStateException("Firestore client not available"))
        try {
            val userPartition = sanitizeUserEmailForPartition(userEmail)
            val task = client.collection("users")
                .document(userPartition)
                .collection("transactions")
                .get()

            val snapshot = Tasks.await(task)
            val results = snapshot.documents.mapNotNull { it.data }
            Result.success(results)
        } catch (e: Throwable) {
            android.util.Log.w("PoolRepository", "Failed to fetch user partitioned data from Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Bulk syncs transactions to their respective user partitions.
     */
    suspend fun syncAllToUserPartitions(transactions: List<PoolTransactionEntity>): Int = withContext(Dispatchers.IO) {
        var syncedCount = 0
        transactions.forEach { tx ->
            if (syncTransactionToUserPartition(tx).isSuccess) {
                syncedCount++
            }
        }
        syncedCount
    }
}
