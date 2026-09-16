package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.AppCurrency
import com.example.data.model.PeriodSummary
import com.example.data.model.TimePeriod
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleSheetExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun generateCsv(
        transactions: List<PoolTransactionEntity>,
        activeCurrency: AppCurrency = AppCurrency.INR,
        activeRatePerUsd: Double = activeCurrency.defaultRatePerUsd
    ): String {
        val sb = java.lang.StringBuilder()
        sb.appendLine("Tx ID,Date,Timestamp (Time),Full Timestamp,Stage / Category,Member Name,Member Email,Original Amount,Original Currency,Locked Rate (to USD),Locked USD Value,Display Currency,Display Converted Amount,Amount USD (Zero Decimal),Amount USDT (Zero Decimal),Exchange Rate,Fee Absorbed,Reference No,Transaction Status,Approval Status,Approved By,Approval Date & Time,Approval Notes,Proof Screenshot URI,Approved Screenshot URI,Google Drive Cloud Link,Notes")
        for (tx in transactions) {
            val recordDate = tx.recordDate.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp)) }
            val recordTime = tx.recordTime.ifEmpty { SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(tx.timestamp)) }
            val fullTimestamp = tx.timestamp.toString()
            val stageTitle = tx.stage.title.replace(",", " ")
            val userName = escapeCsv(tx.userName)
            val userEmail = escapeCsv(tx.userEmail)
            
            // Multi-currency audit fields
            val originalAmt = tx.originalAmount.toString()
            val origCurrency = tx.originalCurrency
            val lockedRate = tx.fiatExchangeRate.toString()
            val lockedUsd = String.format(Locale.US, "%.2f", tx.convertedAmountUsd)
            val dispCurrency = activeCurrency.code
            val dispConvertedAmt = String.format(Locale.US, "%.2f", tx.getAmountInDisplayCurrency(activeRatePerUsd))

            val fiatVal = tx.amountFiat?.toLong()?.toString() ?: ""
            val usdtVal = tx.amountUsdt?.toLong()?.toString() ?: ""
            val rateVal = tx.exchangeRate?.toString() ?: ""
            val feeVal = tx.fee?.toLong()?.toString() ?: "0"
            val refNo = escapeCsv(tx.referenceNo)
            val status = tx.status.name
            val approvalStatus = tx.approvalStatus
            val approvedBy = escapeCsv(tx.approvedByEmail ?: tx.verifiedByEmail ?: "")
            val approvalDateTime = escapeCsv(tx.approvalDate ?: "")
            val approvalNotes = escapeCsv(tx.approvalNotes ?: "")
            val proofUri = escapeCsv(tx.proofUri)
            val approvedScreenshot = escapeCsv(tx.approvalScreenshotUri ?: tx.proofUri)
            val driveLink = escapeCsv(tx.driveWebViewLink ?: "USDT_Pool_Receipts/dipuraj.thapa@gmail.com")
            val notes = escapeCsv(tx.notes)

            sb.appendLine(
                "${tx.id},$recordDate,$recordTime,$fullTimestamp,$stageTitle,$userName,$userEmail,$originalAmt,$origCurrency,$lockedRate,$lockedUsd,$dispCurrency,$dispConvertedAmt,$fiatVal,$usdtVal,$rateVal,$feeVal,$refNo,$status,$approvalStatus,$approvedBy,$approvalDateTime,$approvalNotes,$proofUri,$approvedScreenshot,$driveLink,$notes"
            )
        }
        return sb.toString()
    }

    fun exportToGoogleSheet(
        context: Context,
        transactions: List<PoolTransactionEntity>,
        timePeriod: TimePeriod = summary.period,
        summary: PeriodSummary,
        activeRatePerUsd: Double = summary.currency.defaultRatePerUsd,
        currentUser: com.example.data.model.PoolUser? = null
    ): File {
        val cleanPeriodTag = summary.periodLabel.replace(Regex("[^a-zA-Z0-9]"), "_")
        val isManager = currentUser?.canManage ?: true
        val prefix = if (isManager) "Siyawat_Trades_Consolidated_Ledger" else "Siyawat_Trades_Member_Statement"
        val fileName = "${prefix}_${summary.currency.code}_${cleanPeriodTag}_${fileDateFormat.format(Date())}.csv"
        val exportFile = File(context.cacheDir, fileName)

        FileWriter(exportFile).use { writer ->
            // Header & Period Summary block (easy to read in Google Sheets)
            if (isManager) {
                writer.appendLine("SIYAWAT TRADES - MULTI-CURRENCY TREASURY LEDGER")
                writer.appendLine("Administrative Access,Consolidated View (Admin / Sub-Admin)")
                writer.appendLine("Active Display Currency,${summary.currency.label}")
                writer.appendLine("Exchange Rate Applied,1 USD = $activeRatePerUsd ${summary.currency.code}")
                writer.appendLine("Selected Time Period,${summary.periodLabel}")
                writer.appendLine("Date Range Boundaries,${summary.dateRangeText}")
                writer.appendLine("Report Generated At,${dateFormat.format(Date())}")
                writer.appendLine("Total Capital Injected,${FormatUtils.formatCurrencyZeroDecimal(summary.moneySpent.toDouble(), summary.currency)}")
                writer.appendLine("Total Distributed / Liquidated,${FormatUtils.formatCurrencyZeroDecimal(summary.moneyEarnedBack.toDouble(), summary.currency)}")
                writer.appendLine("Pool Net Profit / Loss,${FormatUtils.formatSignedCurrencyZeroDecimal(summary.profitLoss.toDouble(), summary.currency)}")
                writer.appendLine("USDT Left in Pool,${summary.usdtRemaining} USDT")
            } else {
                writer.appendLine("SIYAWAT TRADES - MEMBER ACCOUNT STATEMENT")
                writer.appendLine("Member Name,${currentUser?.displayName ?: "Member"}")
                writer.appendLine("Member Email,${currentUser?.email ?: ""}")
                writer.appendLine("Active Display Currency,${summary.currency.label}")
                writer.appendLine("Exchange Rate Applied,1 USD = $activeRatePerUsd ${summary.currency.code}")
                writer.appendLine("Selected Time Period,${summary.periodLabel}")
                writer.appendLine("Date Range Boundaries,${summary.dateRangeText}")
                writer.appendLine("Report Generated At,${dateFormat.format(Date())}")
                writer.appendLine("My Contributed Capital,${FormatUtils.formatCurrencyZeroDecimal(summary.moneySpent.toDouble(), summary.currency)}")
                writer.appendLine("My Received Returns,${FormatUtils.formatCurrencyZeroDecimal(summary.moneyEarnedBack.toDouble(), summary.currency)}")
                writer.appendLine("My Net Return,${FormatUtils.formatSignedCurrencyZeroDecimal(summary.profitLoss.toDouble(), summary.currency)}")
            }
            writer.appendLine("") // Blank separator line

            // Column Headers
            writer.appendLine("Tx ID,Date,Timestamp (Time),Full Timestamp,Stage / Category,Member Name,Member Email,Original Amount,Original Currency,Locked Rate (to USD),Locked USD Value,Display Currency,Display Converted Amount,Amount USD (Zero Decimal),Amount USDT (Zero Decimal),Exchange Rate,Fee Absorbed,Reference No,Transaction Status,Approval Status,Approved By,Approval Date & Time,Approval Notes,Proof Screenshot URI,Approved Screenshot URI,Google Drive Cloud Link,Notes")

            // Rows
            for (tx in transactions) {
                val recordDate = tx.recordDate.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp)) }
                val recordTime = tx.recordTime.ifEmpty { SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(tx.timestamp)) }
                val fullTimestamp = tx.timestamp.toString()
                val stageTitle = tx.stage.title.replace(",", " ")
                val userName = escapeCsv(tx.userName)
                val userEmail = escapeCsv(tx.userEmail)

                // Multi-currency audit fields
                val originalAmt = tx.originalAmount.toString()
                val origCurrency = tx.originalCurrency
                val lockedRate = tx.fiatExchangeRate.toString()
                val lockedUsd = String.format(Locale.US, "%.2f", tx.convertedAmountUsd)
                val dispCurrency = summary.currency.code
                val dispConvertedAmt = String.format(Locale.US, "%.2f", tx.getAmountInDisplayCurrency(activeRatePerUsd))

                val fiatVal = tx.amountFiat?.toLong()?.toString() ?: ""
                val usdtVal = tx.amountUsdt?.toLong()?.toString() ?: ""
                val rateVal = tx.exchangeRate?.toString() ?: ""
                val feeVal = tx.fee?.toLong()?.toString() ?: "0"
                val refNo = escapeCsv(tx.referenceNo)
                val status = tx.status.name
                val approvalStatus = tx.approvalStatus
                val approvedBy = escapeCsv(tx.approvedByEmail ?: tx.verifiedByEmail ?: "")
                val approvalDateTime = escapeCsv(tx.approvalDate ?: "")
                val approvalNotes = escapeCsv(tx.approvalNotes ?: "")
                val proofUri = escapeCsv(tx.proofUri)
                val approvedScreenshot = escapeCsv(tx.approvalScreenshotUri ?: tx.proofUri)
                val driveLink = escapeCsv(tx.driveWebViewLink ?: "USDT_Pool_Receipts/dipuraj.thapa@gmail.com")
                val notes = escapeCsv(tx.notes)

                writer.appendLine(
                    "${tx.id},$recordDate,$recordTime,$fullTimestamp,$stageTitle,$userName,$userEmail,$originalAmt,$origCurrency,$lockedRate,$lockedUsd,$dispCurrency,$dispConvertedAmt,$fiatVal,$usdtVal,$rateVal,$feeVal,$refNo,$status,$approvalStatus,$approvedBy,$approvalDateTime,$approvalNotes,$proofUri,$approvedScreenshot,$driveLink,$notes"
                )
            }
        }

        return exportFile
    }

    fun shareExportedSheet(context: Context, file: File, periodLabel: String) {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "Siyawat Trades Ledger - $periodLabel (Google Sheets Export)")
            putExtra(Intent.EXTRA_TEXT, "Here is the Siyawat Trades Ledger for $periodLabel. You can open this directly in Google Sheets or Google Drive.")
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Open or Save to Google Sheets / Drive")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareExportedSheet(context: Context, file: File, timePeriod: TimePeriod) {
        shareExportedSheet(context, file, timePeriod.label)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
