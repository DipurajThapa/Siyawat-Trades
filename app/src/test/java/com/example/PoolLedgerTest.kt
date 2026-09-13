package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PoolDatabase
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.AlertSeverity
import com.example.data.model.PoolUser
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.data.model.UserRole
import com.example.data.repository.PoolRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PoolLedgerTest {

    private lateinit var repository: PoolRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = PoolDatabase.getDatabase(context)
        repository = PoolRepository(context, db)
    }

    @Test
    fun testPoolCalculationsAndFormulas() {
        val testTransactions = listOf(
            // 1. Capital Injections
            PoolTransactionEntity(
                stage = TransactionStage.CAPITAL_INJECTION,
                userEmail = "alice@test.com",
                userName = "Alice",
                amountFiat = 10000.0,
                referenceNo = "REF-1",
                proofUri = "proof_1.png",
                status = TransactionStatus.VERIFIED
            ),
            PoolTransactionEntity(
                stage = TransactionStage.CAPITAL_INJECTION,
                userEmail = "bob@test.com",
                userName = "Bob",
                amountFiat = 5000.0,
                referenceNo = "REF-2",
                proofUri = "proof_2.png",
                status = TransactionStatus.PENDING_VERIFICATION
            ),
            // 2. Bank to Exchange
            PoolTransactionEntity(
                stage = TransactionStage.BANK_TO_EXCHANGE,
                userEmail = "dipuraj.thapa@gmail.com",
                userName = "Dipuraj Thapa",
                amountFiat = 10000.0,
                referenceNo = "REF-3",
                proofUri = "proof_3.png",
                status = TransactionStatus.COMPLETED
            ),
            // 3. USDT Acquisition
            PoolTransactionEntity(
                stage = TransactionStage.USDT_ACQUISITION,
                userEmail = "dipuraj.thapa@gmail.com",
                userName = "Dipuraj Thapa",
                amountFiat = 10000.0,
                amountUsdt = 9980.0,
                fee = 10.0, // net: 9970
                exchangeRate = 1.002,
                referenceNo = "REF-4",
                proofUri = "proof_4.png",
                status = TransactionStatus.COMPLETED
            ),
            // 4. USDT Distribution
            PoolTransactionEntity(
                stage = TransactionStage.USDT_DISTRIBUTION,
                userEmail = "dipuraj.thapa@gmail.com",
                userName = "Dipuraj Thapa",
                recipientEmail = "alice@test.com",
                amountUsdt = 5000.0,
                fee = 1.0,
                referenceNo = "REF-5",
                proofUri = "proof_5.png",
                status = TransactionStatus.COMPLETED
            ),
            // 5. Liquidation
            PoolTransactionEntity(
                stage = TransactionStage.LIQUIDATION,
                userEmail = "alice@test.com",
                userName = "Alice",
                amountUsdt = 2000.0,
                amountFiat = 2040.0,
                exchangeRate = 1.02,
                referenceNo = "REF-6",
                proofUri = "proof_6.png",
                status = TransactionStatus.COMPLETED
            )
        )

        val metrics = repository.calculateMetrics(testTransactions)

        // Verified Fiat: 10,000.0, Pending: 5,000.0
        assertEquals(10000.0, metrics.totalFiatSpent, 0.001)
        assertEquals(5000.0, metrics.pendingFiatInjections, 0.001)

        // Fiat to Exchange: 10,000.0
        assertEquals(10000.0, metrics.totalFiatToExchange, 0.001)

        // Acquired USDT net: 9980 - 10 = 9970
        assertEquals(9970.0, metrics.totalUsdtPurchased, 0.001)

        // Distributed: 5000.0
        assertEquals(5000.0, metrics.totalUsdtDistributed, 0.001)

        // Sold USDT: 2000.0
        assertEquals(2000.0, metrics.totalUsdtSold, 0.001)

        // Realized Fiat: 2040.0
        assertEquals(2040.0, metrics.totalFiatRealised, 0.001)

        // Single Combined Remaining Pool USDT: Purchased (9970) - Sold (2000) = 7970.0
        assertEquals(7970.0, metrics.remainingPoolUsdt, 0.001)

        // Check Compliance Alerts
        val alerts = repository.evaluateComplianceAlerts(testTransactions, metrics)
        // Should flag 1 pending injection (Bob)
        val pendingAlert = alerts.find { it.id == "pending_injections" }
        assertTrue(pendingAlert != null)
        assertEquals(AlertSeverity.WARNING, pendingAlert?.severity)
    }

    @Test
    fun testSixSigmaZeroDecimalAndPeriodSummary() {
        val verifiedSpent = 15000.75
        val earnedBack = 2020.40
        val usdtRemaining = 22950.80

        assertEquals("$15,001", com.example.util.FormatUtils.formatFiatZeroDecimal(verifiedSpent))
        assertEquals("$2,020", com.example.util.FormatUtils.formatFiatZeroDecimal(earnedBack))
        assertEquals("22,951 USDT", com.example.util.FormatUtils.formatUsdtZeroDecimal(usdtRemaining))

        val summary = com.example.data.model.PeriodSummary(
            period = com.example.data.model.TimePeriod.WEEKLY,
            moneySpent = 15000,
            pendingSpent = 5000,
            moneyEarnedBack = 2020,
            usdtRemaining = 22950,
            totalTransactionsCount = 6
        )
        assertEquals(15000L, summary.moneySpent)
        assertEquals(5000L, summary.pendingSpent)
        assertEquals(2020L, summary.moneyEarnedBack)
        assertEquals(22950L, summary.usdtRemaining)
    }

    @Test
    fun testTransactionAuditAndCsvExport() {
        val tx = PoolTransactionEntity(
            stage = TransactionStage.CAPITAL_INJECTION,
            userEmail = "trader@example.com",
            userName = "Trader Bob",
            amountFiat = 12000.0,
            referenceNo = "CITI-2026-99",
            proofUri = "content://media/external/images/slip.png",
            recordDate = "2026-09-12",
            recordTime = "06:30:15",
            approvalStatus = "APPROVED",
            approvedByEmail = "admin@example.com",
            approvalTimestamp = 1789200000000L,
            approvalDate = "2026-09-12 06:35:00",
            approvalNotes = "Confirmed in Citibank account",
            approvalScreenshotUri = "content://media/external/images/slip.png",
            status = TransactionStatus.VERIFIED
        )

        assertEquals("2026-09-12", tx.recordDate)
        assertEquals("06:30:15", tx.recordTime)
        assertEquals("APPROVED", tx.approvalStatus)
        assertEquals("admin@example.com", tx.approvedByEmail)
        assertEquals("content://media/external/images/slip.png", tx.approvalScreenshotUri)

        val csv = com.example.util.GoogleSheetExporter.generateCsv(listOf(tx))
        assertTrue(csv.contains("Date"))
        assertTrue(csv.contains("Timestamp (Time)"))
        assertTrue(csv.contains("Approval Status"))
        assertTrue(csv.contains("Approved By"))
        assertTrue(csv.contains("Approved Screenshot URI"))
        assertTrue(csv.contains("2026-09-12"))
        assertTrue(csv.contains("06:30:15"))
        assertTrue(csv.contains("APPROVED"))
        assertTrue(csv.contains("admin@example.com"))
    }

    @Test
    fun testMultiCurrencyAuditAndCalculations() {
        // Test multi-currency transaction with locked rate
        // Member deposited 83,000 INR when USD/INR was 83.0 -> locked convertedAmountUsd = 1,000.0 USD
        val inrTx = PoolTransactionEntity(
            stage = TransactionStage.CAPITAL_INJECTION,
            userEmail = "raj@test.com",
            userName = "Raj Patel",
            amountFiat = 83000.0,
            originalAmount = 83000.0,
            originalCurrency = "INR",
            fiatExchangeRate = 83.0,
            convertedAmountUsd = 1000.0,
            referenceNo = "INR-REF-100",
            proofUri = "proof_inr.png",
            status = TransactionStatus.VERIFIED
        )

        // Verify locked values are preserved
        assertEquals(83000.0, inrTx.originalAmount, 0.001)
        assertEquals("INR", inrTx.originalCurrency)
        assertEquals(83.0, inrTx.fiatExchangeRate, 0.001)
        assertEquals(1000.0, inrTx.convertedAmountUsd, 0.001)

        // When display currency is USD (rate = 1.0), display amount should be 1000.0
        assertEquals(1000.0, inrTx.getAmountInDisplayCurrency(1.0), 0.001)

        // When live rate changes to 84.0 INR/USD, display amount in INR becomes 84,000 INR
        // but locked original economic USD value remains exactly 1000.0
        assertEquals(84000.0, inrTx.getAmountInDisplayCurrency(84.0), 0.001)
        assertEquals(1000.0, inrTx.convertedAmountUsd, 0.001)

        // Test Metrics in AED (rate: 3.6725)
        val metricsAed = repository.calculateMetrics(
            transactions = listOf(inrTx),
            displayCurrency = com.example.data.model.AppCurrency.AED,
            ratePerUsd = 3.6725
        )
        assertEquals(com.example.data.model.AppCurrency.AED, metricsAed.currency)
        assertEquals(3672.5, metricsAed.totalFiatSpent, 0.1)

        // Test CSV contains the multi-currency audit columns
        val csv = com.example.util.GoogleSheetExporter.generateCsv(listOf(inrTx), activeRatePerUsd = 83.0)
        assertTrue(csv.contains("Original Amount"))
        assertTrue(csv.contains("Original Currency"))
        assertTrue(csv.contains("Locked Rate (to USD)"))
        assertTrue(csv.contains("Locked USD Value"))
        assertTrue(csv.contains("83000"))
        assertTrue(csv.contains("INR"))
    }

    @Test
    fun testAdminDistributionValidationAndOverdrawPrevention() = runBlocking {
        val admin = PoolUser("dipuraj.thapa@gmail.com", "Dipuraj Thapa", UserRole.ADMIN)
        val member = PoolUser("alice@test.com", "Alice Member", UserRole.MEMBER)

        // 1. Non-admin distribution attempt MUST fail
        val nonAdminResult = repository.executeDistribution(
            adminUser = member,
            beneficiaryEmail = "bob@test.com",
            beneficiaryName = "Bob",
            amount = 100.0,
            isUsdt = true
        )
        assertTrue(nonAdminResult.isFailure)
        assertTrue(nonAdminResult.exceptionOrNull()?.message?.contains("Unauthorized") == true)

        // 2. Zero or negative amount MUST fail
        val zeroResult = repository.executeDistribution(
            adminUser = admin,
            beneficiaryEmail = member.email,
            beneficiaryName = member.displayName,
            amount = 0.0,
            isUsdt = true
        )
        assertTrue(zeroResult.isFailure)
        assertTrue(zeroResult.exceptionOrNull()?.message?.contains("greater than zero") == true)

        // 3. Overdraw attempt (amount exceeds available balance) MUST fail
        // Initially pool unallocated USDT is limited; requesting huge amount must be blocked
        val overdrawResult = repository.executeDistribution(
            adminUser = admin,
            beneficiaryEmail = member.email,
            beneficiaryName = member.displayName,
            amount = 9999999.0,
            isUsdt = true
        )
        assertTrue(overdrawResult.isFailure)
        assertTrue(overdrawResult.exceptionOrNull()?.message?.contains("Insufficient") == true)
    }

    @Test
    fun testTransactionReversalWithMandatoryReasonAndAuditTrail() = runBlocking {
        val admin = PoolUser("dipuraj.thapa@gmail.com", "Dipuraj Thapa", UserRole.ADMIN)
        val member = PoolUser("alice@test.com", "Alice Member", UserRole.MEMBER)

        // Insert a test transaction to reverse
        val testEntity = PoolTransactionEntity(
            stage = TransactionStage.CAPITAL_INJECTION,
            userEmail = member.email,
            userName = member.displayName,
            amountFiat = 500.0,
            referenceNo = "REV-TEST-001",
            notes = "Initial deposit",
            proofUri = "proof.png"
        )
        val testTxId = repository.insertTransaction(testEntity)

        // 1. Member attempting reversal MUST fail
        val memberReversal = repository.reverseTransaction(
            adminUser = member,
            transactionId = testTxId,
            reason = "Member trying to reverse"
        )
        assertTrue(memberReversal.isFailure)
        assertTrue(memberReversal.exceptionOrNull()?.message?.contains("Unauthorized") == true)

        // 2. Empty reason MUST fail
        val emptyReasonReversal = repository.reverseTransaction(
            adminUser = admin,
            transactionId = testTxId,
            reason = ""
        )
        assertTrue(emptyReasonReversal.isFailure)
        assertTrue(emptyReasonReversal.exceptionOrNull()?.message?.contains("mandatory") == true)

        // 3. Valid admin reversal MUST succeed and update audit trail
        val validReversal = repository.reverseTransaction(
            adminUser = admin,
            transactionId = testTxId,
            reason = "Duplicate deposit entry confirmed with bank"
        )
        assertTrue(validReversal.isSuccess)

        // 4. Second reversal on already reversed transaction MUST fail
        val doubleReversal = repository.reverseTransaction(
            adminUser = admin,
            transactionId = testTxId,
            reason = "Re-reversing"
        )
        assertTrue(doubleReversal.isFailure)
        assertTrue(doubleReversal.exceptionOrNull()?.message?.contains("already") == true)
    }
}

