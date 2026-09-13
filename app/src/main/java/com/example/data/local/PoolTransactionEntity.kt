package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "pool_transactions")
data class PoolTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val stage: TransactionStage,
    val userEmail: String,
    val userName: String,
    val recipientEmail: String? = null,
    val amountFiat: Double? = null,
    val fiatCurrency: String = "USD",
    val amountUsdt: Double? = null,
    val exchangeRate: Double? = null,
    val fee: Double? = null,
    val referenceNo: String,
    val proofUri: String, // Mandatory proof screenshot path / data URI
    val proofDescription: String? = null,
    val driveFileId: String? = null,
    val driveWebViewLink: String? = null,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val verifiedByEmail: String? = null,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    
    // Explicit Date and Timestamp tracking for audit and ledger
    val recordDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp)),
    val recordTime: String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestamp)),
    
    // Approvals auditing
    val approvalStatus: String = if (status == TransactionStatus.VERIFIED || status == TransactionStatus.COMPLETED) "APPROVED" else "PENDING",
    val approvedByEmail: String? = verifiedByEmail,
    val approvalTimestamp: Long? = if (status == TransactionStatus.VERIFIED || status == TransactionStatus.COMPLETED) timestamp else null,
    val approvalDate: String? = if (status == TransactionStatus.VERIFIED || status == TransactionStatus.COMPLETED) SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp)) else null,
    val approvalNotes: String? = if (status == TransactionStatus.VERIFIED) "Confirmed received in central bank account" else null,
    
    // Screenshot of the approved transaction
    val approvalScreenshotUri: String? = if (status == TransactionStatus.VERIFIED || status == TransactionStatus.COMPLETED) proofUri else null,

    // Multi-currency locking & financial traceability fields
    val originalAmount: Double = amountFiat ?: amountUsdt ?: 0.0,
    val originalCurrency: String = fiatCurrency,
    val displayCurrencyAtTime: String = fiatCurrency,
    val fiatExchangeRate: Double = 1.0,
    val convertedAmountUsd: Double = if (originalCurrency.equals("USD", ignoreCase = true)) {
        originalAmount
    } else if (fiatExchangeRate > 0.0) {
        originalAmount / fiatExchangeRate
    } else {
        originalAmount
    },
    val rateTimestamp: Long = timestamp,
    val resultingBalance: Double? = null,
    val reversalReason: String? = null,
    val reversedByEmail: String? = null,
    val reversalTimestamp: Long? = null
) {
    /**
     * The locked USD value of this transaction.
     * Guaranteed to remain immutable once written to the database.
     */
    val lockedAmountUsd: Double
        get() = convertedAmountUsd

    /**
     * Converts the locked USD value into the user's current live display currency
     * using the current exchange rate.
     */
    fun getAmountInDisplayCurrency(currentRatePerUsd: Double): Double {
        return lockedAmountUsd * currentRatePerUsd
    }
}
