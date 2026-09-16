package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PoolDatabase
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.AlertSeverity
import com.example.data.model.PoolUser
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.data.model.UserRole
import com.example.data.model.UserResponsibility
import com.example.data.repository.PoolRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.data.model.AppCurrency
import com.example.data.model.ReconciliationState
import com.example.data.model.RecordState
import com.example.data.model.SettlementState

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PoolLedgerTest {

    private lateinit var context: android.content.Context
    private lateinit var repository: PoolRepository
    private lateinit var db: PoolDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = PoolDatabase.getDatabase(context)
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

    @Test
    fun testCurrencyOrderingAndDefaults() {
        // 1. AppCurrency.ALL must have INR first, AED second, USD final
        val currencies = AppCurrency.ALL
        assertEquals(3, currencies.size)
        assertEquals(AppCurrency.INR, currencies[0])
        assertEquals(AppCurrency.AED, currencies[1])
        assertEquals(AppCurrency.USD, currencies[2])

        // 2. Default fallback from empty code must be INR
        assertEquals(AppCurrency.INR, AppCurrency.fromCode(null))
        assertEquals(AppCurrency.INR, AppCurrency.fromCode(""))
        assertEquals(AppCurrency.INR, AppCurrency.fromCode("unknown"))

        // 3. Specific codes resolve properly
        assertEquals(AppCurrency.INR, AppCurrency.fromCode("INR"))
        assertEquals(AppCurrency.AED, AppCurrency.fromCode("AED"))
        assertEquals(AppCurrency.USD, AppCurrency.fromCode("USD"))
    }

    @Test
    fun testDatabaseOperationsAndIntegrity() = runBlocking {
        val dao = db.transactionDao()
        val testRef = "DB-TEST-REF-999"
        val testEntity = PoolTransactionEntity(
            stage = TransactionStage.CAPITAL_INJECTION,
            userEmail = "dipuraj.thapa@gmail.com",
            userName = "Dipuraj Thapa",
            amountFiat = 50000.0,
            originalAmount = 50000.0,
            originalCurrency = "INR",
            fiatCurrency = "INR",
            fiatExchangeRate = 83.50,
            convertedAmountUsd = 50000.0 / 83.50,
            referenceNo = testRef,
            proofUri = "file:///data/proof_test.jpg",
            notes = "Test database record integrity",
            recordState = RecordState.SUBMITTED,
            settlementState = SettlementState.UNSETTLED,
            reconciliationState = ReconciliationState.UNRECONCILED
        )

        // 1. Insert & Retrieve
        val insertedId = dao.insertTransaction(testEntity)
        assertTrue("Inserted ID should be positive", insertedId > 0)

        val retrieved = dao.getTransactionById(insertedId)
        assertNotNull("Retrieved record should not be null", retrieved)
        assertEquals(testRef, retrieved?.referenceNo)
        assertEquals("INR", retrieved?.originalCurrency)
        assertEquals(50000.0, retrieved?.originalAmount ?: 0.0, 0.001)

        // 2. Query by reference
        val byRef = dao.getByReferenceNo(testRef)
        assertNotNull("Record queried by reference number should not be null", byRef)
        assertEquals(insertedId, byRef?.id)

        // 3. Maker-Checker dual signature update
        dao.updateApproval(
            id = insertedId,
            status = TransactionStatus.VERIFIED.name,
            verifiedBy = "dipuraj.thapa@gmail.com",
            approvalStatus = "APPROVED",
            approvalTimestamp = System.currentTimeMillis(),
            approvalDate = "2026-09-16 10:00:00",
            approvalNotes = "Verified against bank statement",
            approvalScreenshotUri = null
        )

        val afterApproval = dao.getTransactionById(insertedId)
        assertEquals(TransactionStatus.VERIFIED, afterApproval?.status)
        assertEquals("APPROVED", afterApproval?.approvalStatus)
        assertEquals("dipuraj.thapa@gmail.com", afterApproval?.verifiedByEmail)

        // 4. Update settlement
        dao.updateSettlement(
            id = insertedId,
            settlementState = SettlementState.SETTLED.name,
            bankUtr = "UTR1234567890",
            timestamp = System.currentTimeMillis(),
            reconState = ReconciliationState.PENDING_USER_CONFIRM.name
        )
        val afterSettlement = dao.getTransactionById(insertedId)
        assertEquals(SettlementState.SETTLED, afterSettlement?.settlementState)
        assertEquals("UTR1234567890", afterSettlement?.bankUtrNumber)

        // 5. Cleanup / Delete
        dao.deleteTransaction(insertedId)
        val afterDelete = dao.getTransactionById(insertedId)
        assertNull("Deleted transaction should be null", afterDelete)
    }

    @Test
    fun testSecurityHardeningAdminPinVerificationAndLockout() {
        // Reset or test valid PIN
        val successRes = com.example.security.SecurityHardeningManager.verifyAdminPin("786999")
        assertTrue("Default PIN should succeed", successRes is com.example.security.PinVerificationResult.Success)

        // Test failed attempt
        val failRes1 = com.example.security.SecurityHardeningManager.verifyAdminPin("000000")
        assertTrue("Wrong PIN should fail", failRes1 is com.example.security.PinVerificationResult.Failure)
        assertEquals(2, (failRes1 as com.example.security.PinVerificationResult.Failure).remainingAttempts)

        val failRes2 = com.example.security.SecurityHardeningManager.verifyAdminPin("111111")
        assertTrue("Second wrong PIN should fail", failRes2 is com.example.security.PinVerificationResult.Failure)
        assertEquals(1, (failRes2 as com.example.security.PinVerificationResult.Failure).remainingAttempts)

        val failRes3 = com.example.security.SecurityHardeningManager.verifyAdminPin("222222")
        assertTrue("Third wrong PIN should trigger lockout", failRes3 is com.example.security.PinVerificationResult.LockedOut)
        assertTrue(com.example.security.SecurityHardeningManager.isLockedOut())
    }

    @Test
    fun testCryptographicLedgerChecksumAndTamperDetection() {
        val validTx = PoolTransactionEntity(
            id = 101,
            stage = TransactionStage.CAPITAL_INJECTION,
            status = TransactionStatus.VERIFIED,
            amountFiat = 100000.0,
            originalAmount = 100000.0,
            originalCurrency = "INR",
            fiatCurrency = "INR",
            fiatExchangeRate = 83.50,
            convertedAmountUsd = 100000.0 / 83.50,
            referenceNo = "SEC-TEST-001",
            proofUri = "",
            userEmail = "trader@example.com",
            userName = "Trader Alice"
        )

        // Compute checksum
        val checksum1 = com.example.security.SecurityHardeningManager.computeTransactionChecksum(validTx)
        assertNotNull(checksum1)
        assertEquals(64, checksum1.length) // SHA-256 hex is 64 characters

        // Changing any value must alter checksum
        val tamperedTx = validTx.copy(amountFiat = 999999.0)
        val checksum2 = com.example.security.SecurityHardeningManager.computeTransactionChecksum(tamperedTx)
        assertTrue("Checksum must change when entity data changes", checksum1 != checksum2)

        // Test ledger integrity validation
        val (intactStatus, tamperedCount0) = com.example.security.SecurityHardeningManager.verifyLedgerIntegrity(listOf(validTx))
        assertEquals(com.example.security.LedgerIntegrityStatus.VERIFIED_INTACT, intactStatus)
        assertEquals(0, tamperedCount0)

        // Injected corrupt/negative transaction
        val corruptTx = validTx.copy(amountFiat = -500.0)
        val (tamperStatus, tamperedCount1) = com.example.security.SecurityHardeningManager.verifyLedgerIntegrity(listOf(validTx, corruptTx))
        assertEquals(com.example.security.LedgerIntegrityStatus.TAMPER_DETECTED, tamperStatus)
        assertEquals(1, tamperedCount1)
    }

    @Test
    fun testSecurityAuditReportGeneration() {
        val tx = PoolTransactionEntity(
            stage = TransactionStage.CAPITAL_INJECTION,
            status = TransactionStatus.VERIFIED,
            amountFiat = 50000.0,
            originalAmount = 50000.0,
            originalCurrency = "INR",
            fiatCurrency = "INR",
            fiatExchangeRate = 83.50,
            convertedAmountUsd = 50000.0 / 83.50,
            referenceNo = "AUDIT-001",
            proofUri = "",
            userEmail = "trader@example.com",
            userName = "Trader Alice"
        )

        val report = com.example.security.SecurityHardeningManager.performSecurityAudit(context, listOf(tx))
        assertNotNull(report)
        assertTrue("Security score should be positive", report.score > 0)
        assertNotNull(report.grade)
        assertTrue(report.totalTransactionsChecked >= 1)
        assertEquals(com.example.security.LedgerIntegrityStatus.VERIFIED_INTACT, report.ledgerStatus)
    }

    @Test
    fun testUserDefaultRoleAndAdminResponsibilityAssignment() {
        // 1. New user added to pool defaults to role = MEMBER (User)
        val defaultNewMember = PoolUser(
            email = "newtrader@example.com",
            name = "New Trader",
            role = UserRole.MEMBER
        )
        assertTrue("Default role must be User/Member", defaultNewMember.isDefaultUser)
        assertEquals(UserRole.MEMBER, defaultNewMember.role)
        assertTrue("Default user can deposit funds", defaultNewMember.canDeposit)
        assertTrue("Default user can view own ledger", defaultNewMember.canViewOwnLedger)
        // Ensure regular user cannot access admin panels or manage members
        org.junit.Assert.assertFalse("Default user cannot manage pool", defaultNewMember.canManage)
        org.junit.Assert.assertFalse("Default user cannot manage roles/members", defaultNewMember.canManageMembers)
        org.junit.Assert.assertFalse("Default user cannot do maker-checker", defaultNewMember.canPerformDualApproval)

        // 2. Admin customizes role to SUB_ADMIN and assigns specific duties (e.g., P2P operations + maker checker)
        val elevatedMember = defaultNewMember.copy(
            role = UserRole.SUB_ADMIN,
            customDesignation = "Operations Specialist",
            responsibilities = setOf(
                UserResponsibility.DEPOSIT_FUNDS,
                UserResponsibility.VIEW_OWN_LEDGER,
                UserResponsibility.P2P_TRADING,
                UserResponsibility.DUAL_APPROVAL,
                UserResponsibility.EXPORT_AUDITS
            )
        )

        assertEquals("Operations Specialist", elevatedMember.customDesignation)
        assertEquals(UserRole.SUB_ADMIN, elevatedMember.role)
        assertTrue("Elevated user can manage transactions", elevatedMember.canManage)
        assertTrue("Elevated user has P2P trading responsibility", elevatedMember.canTradeP2P)
        assertTrue("Elevated user has maker-checker approval responsibility", elevatedMember.canPerformDualApproval)
        org.junit.Assert.assertFalse("Elevated member does not have member management unless explicitly granted", elevatedMember.canManageMembers)

        // 3. Admin elevates with full role and member management
        val managerMember = elevatedMember.copy(
            role = UserRole.ADMIN,
            responsibilities = elevatedMember.responsibilities + UserResponsibility.MANAGE_ROLES_AND_MEMBERS
        )
        assertTrue("Manager member can manage roles & whitelist members", managerMember.canManageMembers)
    }

    @Test
    fun testUserPartitionedStoragePreventsDataLeakage() {
        val user1Email = "alice@example.com"
        val user2Email = "bob@example.com"

        // 1. Verify partitioned paths are strictly isolated
        val path1 = repository.getUserPartitionCollectionPath(user1Email)
        val path2 = repository.getUserPartitionCollectionPath(user2Email)

        assertEquals("users/alice@example_com/transactions", path1)
        assertEquals("users/bob@example_com/transactions", path2)
        assertTrue("Partitions must never overlap across different users", path1 != path2)

        // 2. Verify mapping of transaction entity to partition document
        val testTx = PoolTransactionEntity(
            id = 101L,
            stage = TransactionStage.CAPITAL_INJECTION,
            userEmail = user1Email,
            userName = "Alice",
            amountFiat = 50000.0,
            originalAmount = 50000.0,
            originalCurrency = "INR",
            fiatCurrency = "INR",
            fiatExchangeRate = 83.5,
            convertedAmountUsd = 598.8,
            referenceNo = "UTR-ALICE-101",
            status = TransactionStatus.VERIFIED,
            timestamp = 1700000000000L,
            proofUri = "content://media/alice_utr.jpg",
            notes = "Personal capital injection"
        )

        val docMap = repository.transactionToPartitionMap(testTx)
        assertEquals(101L, docMap["id"])
        assertEquals(user1Email, docMap["userEmail"])
        assertEquals(50000.0, docMap["amountFiat"])
        assertEquals("UTR-ALICE-101", docMap["referenceNo"])
        assertEquals(TransactionStatus.VERIFIED.name, docMap["status"])
    }

    @Test
    fun testMultiCurrencyConversionsAndDefaultCurrencies() {
        // Verify default currencies order and symbols
        assertEquals(AppCurrency.INR, AppCurrency.ALL[0])
        assertEquals(AppCurrency.AED, AppCurrency.ALL[1])
        assertEquals(AppCurrency.USD, AppCurrency.ALL[2])

        assertEquals("₹", AppCurrency.INR.symbol)
        assertEquals("AED", AppCurrency.AED.symbol)
        assertEquals("$", AppCurrency.USD.symbol)

        // Lookup from code
        assertEquals(AppCurrency.INR, AppCurrency.fromCode("inr"))
        assertEquals(AppCurrency.AED, AppCurrency.fromCode("AED"))
        assertEquals(AppCurrency.USD, AppCurrency.fromCode("USD"))
        assertEquals(AppCurrency.INR, AppCurrency.fromCode(null))
        assertEquals(AppCurrency.INR, AppCurrency.fromCode("INVALID"))

        // Conversion math: 83.50 INR = 1 USD; 3.6725 AED = 1 USD
        val inrAmount = 83500.0
        val usdFromInr = inrAmount / AppCurrency.INR.defaultRatePerUsd
        assertEquals(1000.0, usdFromInr, 0.001)

        val aedAmount = 3672.50
        val usdFromAed = aedAmount / AppCurrency.AED.defaultRatePerUsd
        assertEquals(1000.0, usdFromAed, 0.001)
    }

    @Test
    fun testSettlementAndDisputeStateTransitions() = runBlocking {
        val testEntity = PoolTransactionEntity(
            stage = TransactionStage.CAPITAL_INJECTION,
            userEmail = "trader@example.com",
            userName = "Trader",
            amountFiat = 1000.0,
            referenceNo = "TX-SETTLE-001",
            proofUri = "proof.png",
            settlementState = SettlementState.UNSETTLED,
            recordState = RecordState.APPROVED,
            reconciliationState = ReconciliationState.UNRECONCILED
        )
        val txId = repository.insertTransaction(testEntity)

        // 1. Record Settlement
        val settleResult = repository.recordSettlement(
            id = txId,
            operatorUser = PoolUser("dipuraj.thapa@gmail.com", "Admin", UserRole.ADMIN),
            bankUtr = "BANK-UTR-999"
        )
        assertTrue("Settlement should succeed", settleResult.isSuccess)

        val settledTx = db.transactionDao().getTransactionById(txId)
        assertNotNull(settledTx)
        assertEquals(SettlementState.SETTLED, settledTx!!.settlementState)
        assertEquals("BANK-UTR-999", settledTx.bankUtrNumber)

        // 2. Raise Dispute
        val disputeResult = repository.raiseDispute(
            id = txId,
            currentUser = PoolUser("trader@example.com", "Trader", UserRole.MEMBER),
            reason = "Amount credited does not match transfer slip"
        )
        assertTrue("Dispute raising should succeed", disputeResult.isSuccess)

        val disputedTx = db.transactionDao().getTransactionById(txId)
        assertNotNull(disputedTx)
        assertEquals(ReconciliationState.DISPUTED, disputedTx!!.reconciliationState)
        assertEquals("Amount credited does not match transfer slip", disputedTx.disputeReason)

        // 3. Resolve Dispute
        val resolveResult = repository.resolveDispute(
            id = txId,
            adminUser = PoolUser("dipuraj.thapa@gmail.com", "Admin", UserRole.ADMIN),
            resolutionNotes = "Verified banking ledger. Transfer was correct.",
            isConfirmed = true
        )
        assertTrue("Dispute resolution should succeed", resolveResult.isSuccess)

        val resolvedTx = db.transactionDao().getTransactionById(txId)
        assertNotNull(resolvedTx)
        assertEquals(ReconciliationState.RECONCILED, resolvedTx!!.reconciliationState)
    }

    @Test
    fun testFifoAccountingWithMultipleLots() {
        val transactions = listOf(
            // Lot 1: Buy 1000 USDT at $1.00 each = $1000
            PoolTransactionEntity(
                id = 1L,
                stage = TransactionStage.USDT_ACQUISITION,
                amountFiat = 1000.0,
                fiatCurrency = "USD",
                amountUsdt = 1000.0,
                exchangeRate = 1.00,
                fee = 0.0,
                status = TransactionStatus.VERIFIED,
                timestamp = 1000L,
                userEmail = "admin@example.com",
                userName = "Admin",
                referenceNo = "LOT-1",
                proofUri = "",
                convertedAmountUsd = 1000.0
            ),
            // Lot 2: Buy 2000 USDT at $1.02 each = $2040
            PoolTransactionEntity(
                id = 2L,
                stage = TransactionStage.USDT_ACQUISITION,
                amountFiat = 2040.0,
                fiatCurrency = "USD",
                amountUsdt = 2000.0,
                exchangeRate = 1.02,
                fee = 0.0,
                status = TransactionStatus.VERIFIED,
                timestamp = 2000L,
                userEmail = "admin@example.com",
                userName = "Admin",
                referenceNo = "LOT-2",
                proofUri = "",
                convertedAmountUsd = 2040.0
            ),
            // Liquidation 1: Sell 1500 USDT at $1.05 each
            // FIFO: takes 1000 from Lot 1 (cost $1000) and 500 from Lot 2 (cost 500 * 1.02 = $510)
            // Total cost = $1510. Proceeds = 1500 * 1.05 = $1575. Realized Gain = $65.
            PoolTransactionEntity(
                id = 3L,
                stage = TransactionStage.LIQUIDATION,
                amountFiat = 1575.0,
                fiatCurrency = "USD",
                amountUsdt = 1500.0,
                exchangeRate = 1.05,
                fee = 0.0,
                status = TransactionStatus.VERIFIED,
                timestamp = 3000L,
                userEmail = "admin@example.com",
                userName = "Admin",
                referenceNo = "SELL-1",
                proofUri = "",
                convertedAmountUsd = 1575.0
            )
        )

        val fifo = repository.calculateFifoLedger(transactions)
        assertEquals(3000.0, fifo.totalUsdtPurchased, 0.001)
        assertEquals(1500.0, fifo.totalUsdtSold, 0.001)
        assertEquals(1500.0, fifo.remainingUsdtInventory, 0.001)
        assertEquals(1575.0, fifo.totalSaleProceedsFiat, 0.001)
        assertEquals(1510.0, fifo.totalCostBasisOfSoldUsdt, 0.001)
        assertEquals(65.0, fifo.totalRealizedGainLossFiat, 0.001)
    }
}

