package com.example

import com.example.data.model.AppCurrency
import com.example.util.TransactionScreenshotAnalyzer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionScreenshotAnalyzerTest {

    @Test
    fun testSuccessfulAedExtraction() = runBlocking {
        val slipText = """
            EMIRATES NBD TRANSFER CONFIRMATION
            Date: 2026-09-13
            Transaction ID: WIRE-2026-DXB991
            Beneficiary: Siyawat Trades Central Account
            Amount: AED 25,000.00
            Status: SUCCESS
        """.trimIndent()

        val result = TransactionScreenshotAnalyzer.analyzeText(
            fullText = slipText,
            selectedCurrency = AppCurrency.AED,
            currentlyEnteredAmount = 25000.0,
            checkDuplicateTxId = { false }
        )

        assertEquals(25000.0, result.extractedAmount)
        assertEquals(AppCurrency.AED, result.detectedCurrency)
        assertEquals("WIRE-2026-DXB991", result.extractedReferenceId)
        assertFalse(result.isAmountMissing)
        assertFalse(result.isReferenceMissing)
        assertNull(result.missingNotification)
        assertFalse(result.isSuspicious)
        assertNull(result.statusClassification)
    }

    @Test
    fun testAmountMissingNotification() = runBlocking {
        val slipText = """
            BANK TRANSFER RECEIPT
            Ref No: FT2409138472910
            Status: Executed
            Thank you for banking with us.
        """.trimIndent()

        val result = TransactionScreenshotAnalyzer.analyzeText(
            fullText = slipText,
            selectedCurrency = AppCurrency.AED,
            checkDuplicateTxId = { false }
        )

        assertNull(result.extractedAmount)
        assertEquals("FT2409138472910", result.extractedReferenceId)
        assertTrue(result.isAmountMissing)
        assertFalse(result.isReferenceMissing)
        assertEquals(
            "Transaction amount could not be found in the uploaded image.",
            result.missingNotification
        )
    }

    @Test
    fun testReferenceMissingNotification() = runBlocking {
        val slipText = """
            PAYMENT CONFIRMATION
            Total Amount: AED 50,000
            Completed successfully
        """.trimIndent()

        val result = TransactionScreenshotAnalyzer.analyzeText(
            fullText = slipText,
            selectedCurrency = AppCurrency.AED,
            checkDuplicateTxId = { false }
        )

        assertEquals(50000.0, result.extractedAmount)
        assertNull(result.extractedReferenceId)
        assertFalse(result.isAmountMissing)
        assertTrue(result.isReferenceMissing)
        assertEquals(
            "Transaction ID could not be found in the uploaded image.",
            result.missingNotification
        )
    }

    @Test
    fun testBothMissingNotification() = runBlocking {
        val slipText = """
            Welcome to mobile banking.
            No transaction records found.
        """.trimIndent()

        val result = TransactionScreenshotAnalyzer.analyzeText(
            fullText = slipText,
            selectedCurrency = AppCurrency.AED,
            checkDuplicateTxId = { false }
        )

        assertNull(result.extractedAmount)
        assertNull(result.extractedReferenceId)
        assertTrue(result.isAmountMissing)
        assertTrue(result.isReferenceMissing)
        assertEquals(
            "Both transaction amount and transaction ID could not be found in the uploaded image.",
            result.missingNotification
        )
    }

    @Test
    fun testSuspiciousAmountMismatch() = runBlocking {
        val slipText = """
            TRANSFER RECEIPT
            Transaction ID: WIRE-2026-ALC882
            Amount: AED 5,000.00
            Status: Completed
        """.trimIndent()

        val result = TransactionScreenshotAnalyzer.analyzeText(
            fullText = slipText,
            selectedCurrency = AppCurrency.AED,
            currentlyEnteredAmount = 15000.0, // Entered 15,000, slip shows 5,000
            checkDuplicateTxId = { false }
        )

        assertTrue(result.isSuspicious)
        assertEquals("Potentially suspicious — requires verification.", result.statusClassification)
        assertTrue(result.suspiciousWarnings.any { it.contains("Amount mismatch") })
    }

    @Test
    fun testSuspiciousDuplicateTransactionId() = runBlocking {
        val slipText = """
            WIRE ADVICE
            Ref No: WIRE-2026-DXB991
            Amount: AED 25,000.00
        """.trimIndent()

        val result = TransactionScreenshotAnalyzer.analyzeText(
            fullText = slipText,
            selectedCurrency = AppCurrency.AED,
            currentlyEnteredAmount = 25000.0,
            checkDuplicateTxId = { ref -> ref == "WIRE-2026-DXB991" } // Simulating duplicate found in DB
        )

        assertTrue(result.isSuspicious)
        assertEquals("Potentially suspicious — requires verification.", result.statusClassification)
        assertTrue(result.suspiciousWarnings.any { it.contains("Duplicate transaction ID") })
    }

    @Test
    fun testSuspiciousMalformedReferenceId() = runBlocking {
        val slipText = """
            CONFIRMATION
            Ref No: TEST
            Amount: AED 1,000.00
        """.trimIndent()

        val result = TransactionScreenshotAnalyzer.analyzeText(
            fullText = slipText,
            selectedCurrency = AppCurrency.AED,
            currentlyEnteredAmount = 1000.0,
            checkDuplicateTxId = { false }
        )

        assertTrue(result.isSuspicious)
        assertEquals("Potentially suspicious — requires verification.", result.statusClassification)
        assertTrue(result.suspiciousWarnings.any { it.contains("Inconsistent reference ID") || it.contains("Malformed reference ID") })
    }
}
