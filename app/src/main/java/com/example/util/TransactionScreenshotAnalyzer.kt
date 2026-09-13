package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.model.AppCurrency
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

data class ScreenshotAnalysisResult(
    val extractedAmount: Double? = null,
    val extractedAmountFormatted: String? = null,
    val detectedCurrency: AppCurrency? = null,
    val extractedReferenceId: String? = null,
    val isAmountMissing: Boolean = false,
    val isReferenceMissing: Boolean = false,
    val missingNotification: String? = null,
    val isSuspicious: Boolean = false,
    val suspiciousWarnings: List<String> = emptyList(),
    val statusClassification: String? = null, // "Potentially suspicious — requires verification."
    val rawExtractedText: String = ""
)

object TransactionScreenshotAnalyzer {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Analyzes an uploaded transaction confirmation screenshot.
     * Extracts transaction amount and reference ID using on-device ML Kit OCR.
     * Validates for missing information and suspicious activity indicators.
     */
    suspend fun analyzeScreenshot(
        context: Context,
        imageUri: Uri,
        currentlyEnteredAmount: Double? = null,
        selectedCurrency: AppCurrency = AppCurrency.AED,
        checkDuplicateTxId: (suspend (String) -> Boolean)? = null
    ): ScreenshotAnalysisResult = withContext(Dispatchers.Default) {
        try {
            val inputImage = withContext(Dispatchers.IO) {
                InputImage.fromFilePath(context, imageUri)
            }
            val visionText = recognizer.process(inputImage).await()
            val fullText = visionText.text
            val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }

            analyzeTextLines(
                lines = lines,
                fullText = fullText,
                currentlyEnteredAmount = currentlyEnteredAmount,
                selectedCurrency = selectedCurrency,
                checkDuplicateTxId = checkDuplicateTxId
            )
        } catch (e: Exception) {
            // Fallback gracefully if image cannot be processed
            ScreenshotAnalysisResult(
                isAmountMissing = true,
                isReferenceMissing = true,
                missingNotification = "Both transaction amount and transaction ID could not be found in the uploaded image.",
                rawExtractedText = "Error processing image: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Analyzes an existing Bitmap directly (e.g. for generated receipts or cached files).
     */
    suspend fun analyzeBitmap(
        bitmap: Bitmap,
        currentlyEnteredAmount: Double? = null,
        selectedCurrency: AppCurrency = AppCurrency.AED,
        checkDuplicateTxId: (suspend (String) -> Boolean)? = null
    ): ScreenshotAnalysisResult = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(inputImage).await()
            val fullText = visionText.text
            val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }

            analyzeTextLines(
                lines = lines,
                fullText = fullText,
                currentlyEnteredAmount = currentlyEnteredAmount,
                selectedCurrency = selectedCurrency,
                checkDuplicateTxId = checkDuplicateTxId
            )
        } catch (e: Exception) {
            ScreenshotAnalysisResult(
                isAmountMissing = true,
                isReferenceMissing = true,
                missingNotification = "Both transaction amount and transaction ID could not be found in the uploaded image.",
                rawExtractedText = "Error processing bitmap: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    /**
     * Analyzes raw text directly (useful for testing and OCR text processors).
     */
    suspend fun analyzeText(
        fullText: String,
        currentlyEnteredAmount: Double? = null,
        selectedCurrency: AppCurrency = AppCurrency.AED,
        checkDuplicateTxId: (suspend (String) -> Boolean)? = null
    ): ScreenshotAnalysisResult {
        val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
        return analyzeTextLines(
            lines = lines,
            fullText = fullText,
            currentlyEnteredAmount = currentlyEnteredAmount,
            selectedCurrency = selectedCurrency,
            checkDuplicateTxId = checkDuplicateTxId
        )
    }

    private suspend fun analyzeTextLines(
        lines: List<String>,
        fullText: String,
        currentlyEnteredAmount: Double?,
        selectedCurrency: AppCurrency,
        checkDuplicateTxId: (suspend (String) -> Boolean)?
    ): ScreenshotAnalysisResult {
        var detectedAmount: Double? = null
        var detectedCurrency: AppCurrency? = null
        var detectedReferenceId: String? = null

        // 1. Currency Detection
        val upperText = fullText.uppercase(Locale.US)
        if (upperText.contains("AED") || fullText.contains("د.إ")) {
            detectedCurrency = AppCurrency.AED
        } else if (upperText.contains("INR") || fullText.contains("₹")) {
            detectedCurrency = AppCurrency.INR
        } else if (upperText.contains("USD") || fullText.contains("$")) {
            detectedCurrency = AppCurrency.USD
        }

        // 2. Extract Reference ID / Transaction ID
        // Look for reference markers
        val refKeywords = listOf(
            "TRANSACTION ID", "TXN ID", "TXID", "REFERENCE NO", "REFERENCE NUMBER",
            "REF NO", "REF NUMBER", "REF #", "REFERENCE #", "REFERENCE",
            "TRANSFER ID", "ORDER ID", "UTR", "BANK REF", "CONFIRMATION NO",
            "CONFIRMATION NUMBER", "WIRE REF", "SLIP NO", "PAYMENT REF"
        )

        for (i in lines.indices) {
            val line = lines[i]
            val lineUpper = line.uppercase(Locale.US)

            for (kw in refKeywords) {
                if (lineUpper.contains(kw)) {
                    // Check if value is on the same line after keyword/colon/hash
                    val afterKw = line.substring(lineUpper.indexOf(kw) + kw.length)
                        .trimStart(':', '-', '#', ' ', '\t')
                        .trim()

                    val cleanVal = extractCandidateRef(afterKw)
                    if (cleanVal != null) {
                        detectedReferenceId = cleanVal
                        break
                    } else if (i + 1 < lines.size) {
                        // Check next line for the ID value
                        val nextCandidate = extractCandidateRef(lines[i + 1].trim())
                        if (nextCandidate != null) {
                            detectedReferenceId = nextCandidate
                            break
                        }
                    }
                }
            }
            if (detectedReferenceId != null) break
        }

        // If not found via keywords, check for explicit known patterns
        if (detectedReferenceId == null) {
            // Pattern like WIRE-2026-..., TXN-TRC20-..., FT24..., 12-digit UTR
            val directRefRegex = Pattern.compile("\\b(WIRE-[A-Z0-9\\-]+|TXN-[A-Z0-9\\-]+|FT[0-9]{8,18}|UTR[0-9]{8,16})\\b", Pattern.CASE_INSENSITIVE)
            val matcher = directRefRegex.matcher(fullText)
            if (matcher.find()) {
                detectedReferenceId = matcher.group(1)?.trim()
            }
        }

        // 3. Extract Amount
        val amountKeywords = listOf(
            "AMOUNT", "TOTAL", "SUM", "SENT", "TRANSFERRED", "DEBIT",
            "PAID", "VALUE", "NET AMOUNT", "DEPOSITED"
        )

        // Try finding labeled amount first
        for (i in lines.indices) {
            val line = lines[i]
            val lineUpper = line.uppercase(Locale.US)

            for (kw in amountKeywords) {
                if (lineUpper.contains(kw)) {
                    val afterKw = line.substring(lineUpper.indexOf(kw) + kw.length)
                    val parsed = parseAmountFromSnippet(afterKw)
                    if (parsed != null && parsed > 0.0) {
                        detectedAmount = parsed
                        break
                    } else if (i + 1 < lines.size) {
                        val nextParsed = parseAmountFromSnippet(lines[i + 1])
                        if (nextParsed != null && nextParsed > 0.0) {
                            detectedAmount = nextParsed
                            break
                        }
                    }
                }
            }
            if (detectedAmount != null) break
        }

        // If still not found, search lines containing currency prefixes like AED 10,000, $5,000, etc.
        if (detectedAmount == null) {
            for (line in lines) {
                if (line.contains("AED", ignoreCase = true) ||
                    line.contains("$") ||
                    line.contains("₹") ||
                    line.contains("د.إ") ||
                    line.contains("USD", ignoreCase = true) ||
                    line.contains("INR", ignoreCase = true)
                ) {
                    val parsed = parseAmountFromSnippet(line)
                    if (parsed != null && parsed > 0.0) {
                        detectedAmount = parsed
                        break
                    }
                }
            }
        }

        // 4. Missing Information Checks
        val isAmountMissing = detectedAmount == null
        val isReferenceMissing = detectedReferenceId == null

        val missingNotification: String? = when {
            isAmountMissing && isReferenceMissing ->
                "Both transaction amount and transaction ID could not be found in the uploaded image."
            isAmountMissing ->
                "Transaction amount could not be found in the uploaded image."
            isReferenceMissing ->
                "Transaction ID could not be found in the uploaded image."
            else -> null
        }

        // 5. Suspicious Activity Validation
        val suspiciousWarnings = mutableListOf<String>()

        // Check A: Amount mismatch against user's entered amount
        if (currentlyEnteredAmount != null && currentlyEnteredAmount > 0.0 && detectedAmount != null) {
            val diff = abs(currentlyEnteredAmount - detectedAmount)
            if (diff > 0.01) {
                suspiciousWarnings.add(
                    "Amount mismatch: The screenshot indicates ${FormatUtils.formatRate(detectedAmount)} but the entered amount is ${FormatUtils.formatRate(currentlyEnteredAmount)}."
                )
            }
        }

        // Check B: Duplicate Transaction ID check in ledger
        if (detectedReferenceId != null && checkDuplicateTxId != null) {
            val isDuplicate = checkDuplicateTxId(detectedReferenceId)
            if (isDuplicate) {
                suspiciousWarnings.add(
                    "Duplicate transaction ID: The reference ID '$detectedReferenceId' has already been submitted in the system."
                )
            }
        }

        // Check C: Malformed or suspicious reference ID pattern
        if (detectedReferenceId != null) {
            val upperRef = detectedReferenceId.uppercase(Locale.US)
            val isAllSameChar = detectedReferenceId.all { it == detectedReferenceId[0] }
            if (detectedReferenceId.length < 4) {
                suspiciousWarnings.add("Malformed reference ID: Extracted reference ID is unusually short.")
            } else if (isAllSameChar || upperRef.contains("TEST") || upperRef.contains("SAMPLE") || upperRef.contains("DEMO") || upperRef.contains("FAKE") || upperRef.contains("000000")) {
                suspiciousWarnings.add("Inconsistent reference ID: Test or dummy pattern detected in transaction ID.")
            }
        }

        // Check D: Conflicting currency details
        if (detectedCurrency != null && detectedCurrency != selectedCurrency && detectedAmount != null) {
            suspiciousWarnings.add(
                "Currency conflict: Uploaded slip indicates ${detectedCurrency.code}, which conflicts with selected transfer currency ${selectedCurrency.code}."
            )
        }

        val isSuspicious = suspiciousWarnings.isNotEmpty()
        val statusClassification = if (isSuspicious) "Potentially suspicious — requires verification." else null

        val formattedAmount = detectedAmount?.let {
            if (selectedCurrency == AppCurrency.AED) {
                "AED ${FormatUtils.formatInteger(it)}"
            } else {
                "${selectedCurrency.symbol}${FormatUtils.formatInteger(it)}"
            }
        }

        return ScreenshotAnalysisResult(
            extractedAmount = detectedAmount,
            extractedAmountFormatted = formattedAmount,
            detectedCurrency = detectedCurrency,
            extractedReferenceId = detectedReferenceId,
            isAmountMissing = isAmountMissing,
            isReferenceMissing = isReferenceMissing,
            missingNotification = missingNotification,
            isSuspicious = isSuspicious,
            suspiciousWarnings = suspiciousWarnings,
            statusClassification = statusClassification,
            rawExtractedText = fullText
        )
    }

    private fun extractCandidateRef(text: String): String? {
        if (text.isBlank()) return null
        // Strip common trailing punctuation
        val cleaned = text.trim().trimEnd('.', ',', ';')
        // Match alphanumeric reference sequences with hyphens or underscores
        val matcher = Pattern.compile("^[A-Za-z0-9\\-_/]{4,50}$").matcher(cleaned)
        if (matcher.matches()) {
            return cleaned
        }
        // If the line contains words, pick the first token that looks like an ID
        val tokens = cleaned.split("\\s+".toRegex())
        for (token in tokens) {
            val tClean = token.trim('(', ')', '[', ']', ':', '#', '-', '.')
            if (tClean.length in 4..50 && tClean.any { it.isDigit() } && tClean.any { it.isLetter() || it == '-' }) {
                return tClean
            }
        }
        return null
    }

    private fun parseAmountFromSnippet(snippet: String): Double? {
        if (snippet.isBlank()) return null
        // Match numbers like 25,000.00 or 25000 or 500.50
        val pattern = Pattern.compile("(?:AED|USD|INR|USDT|[$₹د.إ])?\\s*([0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,4})?|[0-9]+(?:\\.[0-9]{1,4})?)")
        val matcher = pattern.matcher(snippet)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "")?.trim()
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                return parsed
            }
        }
        return null
    }
}
