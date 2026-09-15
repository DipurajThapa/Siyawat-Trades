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
    val detectedInstitution: String? = null,
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
                val bitmap = try {
                    if (imageUri.scheme == "file" || (imageUri.path != null && java.io.File(imageUri.path!!).exists())) {
                        BitmapFactory.decodeFile(imageUri.path)
                    } else {
                        context.contentResolver.openInputStream(imageUri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                } catch (e: Exception) {
                    null
                } ?: try {
                    context.contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    null
                } ?: throw IllegalArgumentException("Could not decode image from uri: $imageUri")
                InputImage.fromBitmap(bitmap, 0)
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
        var detectedInstitution: String? = null

        val upperText = fullText.uppercase(Locale.US)

        // 1. Detect Financial Institution / Exchange
        val institutions = listOf(
            "EMIRATES NBD" to "Emirates NBD",
            "ADCB" to "Abu Dhabi Commercial Bank (ADCB)",
            "DUBAI ISLAMIC BANK" to "Dubai Islamic Bank (DIB)",
            "DIB" to "Dubai Islamic Bank (DIB)",
            "MASHREQ" to "Mashreq Bank",
            "FIRST ABU DHABI BANK" to "First Abu Dhabi Bank (FAB)",
            "FAB" to "First Abu Dhabi Bank (FAB)",
            "AL ANSARI" to "Al Ansari Exchange",
            "LULU EXCHANGE" to "LuLu Exchange",
            "RAKBANK" to "RAKBANK",
            "COMMERCIAL BANK OF DUBAI" to "Commercial Bank of Dubai (CBD)",
            "CBD" to "Commercial Bank of Dubai (CBD)",
            "BINANCE" to "Binance",
            "OKX" to "OKX",
            "BYBIT" to "Bybit",
            "HDFC" to "HDFC Bank",
            "STATE BANK OF INDIA" to "State Bank of India (SBI)",
            "SBI" to "State Bank of India (SBI)",
            "ICICI" to "ICICI Bank"
        )
        for ((key, name) in institutions) {
            if (upperText.contains(key)) {
                detectedInstitution = name
                break
            }
        }

        // 2. Currency Detection
        if (upperText.contains("AED") || fullText.contains("د.إ") || upperText.contains("DIRHAM") || upperText.contains("DHS")) {
            detectedCurrency = AppCurrency.AED
        } else if (upperText.contains("INR") || fullText.contains("₹") || upperText.contains("RUPEE") || upperText.contains("RS.")) {
            detectedCurrency = AppCurrency.INR
        } else if (upperText.contains("USD") || fullText.contains("$") || upperText.contains("USDT") || upperText.contains("DOLLAR")) {
            detectedCurrency = AppCurrency.USD
        }

        // 3. Extract Reference ID / Transaction ID
        val refKeywords = listOf(
            "TRANSACTION ID", "TXN ID", "TXN NO", "TXN NUMBER", "TXN", "TXID",
            "TRANSACTION NUMBER", "TRANSACTION NO", "TRANSACTION REF", "TRANSACTION #",
            "REFERENCE NO", "REFERENCE NUMBER", "REF NO", "REF NUMBER", "REF #",
            "REFERENCE #", "REFERENCE ID", "REF ID", "REF.", "REFERENCE",
            "TRANSFER ID", "ORDER ID", "UTR NO", "UTR NUMBER", "UTR", "BANK REF", "BANK REFERENCE",
            "CONFIRMATION NO", "CONFIRMATION NUMBER", "CONFIRMATION CODE", "WIRE REF", "SLIP NO",
            "PAYMENT REF", "RECEIPT NO", "RECEIPT NUMBER", "PAYMENT ID", "FT REF", "FT NUMBER",
            "AUTH CODE", "APPROVAL CODE", "JOURNAL NO", "BATCH NO", "EXTERNAL REF"
        )

        for (i in lines.indices) {
            val line = lines[i]
            val lineUpper = line.uppercase(Locale.US)

            for (kw in refKeywords) {
                if (lineUpper.contains(kw)) {
                    // Check on same line after keyword
                    val idx = lineUpper.indexOf(kw)
                    val afterKw = line.substring(idx + kw.length)
                        .trimStart(':', '-', '#', '.', ' ', '\t')
                        .trim()

                    val cleanVal = extractCandidateRef(afterKw)
                    if (cleanVal != null) {
                        detectedReferenceId = cleanVal
                        break
                    }

                    // Check line i + 1
                    if (i + 1 < lines.size) {
                        val nextCandidate = extractCandidateRef(lines[i + 1].trim())
                        if (nextCandidate != null) {
                            detectedReferenceId = nextCandidate
                            break
                        }
                    }

                    // Check line i + 2
                    if (i + 2 < lines.size) {
                        val next2Candidate = extractCandidateRef(lines[i + 2].trim())
                        if (next2Candidate != null) {
                            detectedReferenceId = next2Candidate
                            break
                        }
                    }
                }
            }
            if (detectedReferenceId != null) break
        }

        // Regex fallback for explicit standard patterns
        if (detectedReferenceId == null) {
            val directRefRegex = Pattern.compile(
                "\\b(WIRE-[A-Z0-9\\-]+|TXN-[A-Z0-9\\-]+|FT[0-9]{8,18}|UTR[0-9]{8,18}|UPI/[0-9]{12}|REF-[A-Z0-9\\-]+)\\b",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = directRefRegex.matcher(fullText)
            if (matcher.find()) {
                detectedReferenceId = matcher.group(1)?.trim()
            }
        }

        // 4. Extract Amount
        val amountKeywords = listOf(
            "TOTAL AMOUNT", "TRANSFER AMOUNT", "AMOUNT TRANSFERRED", "NET AMOUNT",
            "AMOUNT", "TOTAL", "SUM", "SENT", "TRANSFERRED", "DEBIT", "DEBITED",
            "PAID", "VALUE", "DEPOSITED"
        )

        // Try labeled amount lines first
        for (i in lines.indices) {
            val line = lines[i]
            val lineUpper = line.uppercase(Locale.US)

            for (kw in amountKeywords) {
                if (lineUpper.contains(kw)) {
                    val idx = lineUpper.indexOf(kw)
                    val afterKw = line.substring(idx + kw.length)
                    val parsed = parseAmountFromSnippet(afterKw)
                    if (parsed != null && parsed > 0.0) {
                        detectedAmount = parsed
                        break
                    }

                    if (i + 1 < lines.size) {
                        val nextParsed = parseAmountFromSnippet(lines[i + 1])
                        if (nextParsed != null && nextParsed > 0.0) {
                            detectedAmount = nextParsed
                            break
                        }
                    }

                    if (i + 2 < lines.size) {
                        val next2Parsed = parseAmountFromSnippet(lines[i + 2])
                        if (next2Parsed != null && next2Parsed > 0.0) {
                            detectedAmount = next2Parsed
                            break
                        }
                    }
                }
            }
            if (detectedAmount != null) break
        }

        // If not found via labeled keywords, scan lines containing currency tags
        if (detectedAmount == null) {
            for (line in lines) {
                if (line.contains("AED", ignoreCase = true) ||
                    line.contains("$") ||
                    line.contains("₹") ||
                    line.contains("د.إ") ||
                    line.contains("USD", ignoreCase = true) ||
                    line.contains("INR", ignoreCase = true) ||
                    line.contains("USDT", ignoreCase = true)
                ) {
                    val parsed = parseAmountFromSnippet(line)
                    if (parsed != null && parsed > 0.0) {
                        detectedAmount = parsed
                        break
                    }
                }
            }
        }

        // 5. Missing Information Checks
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

        // 6. Suspicious Activity Validation
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
            detectedInstitution = detectedInstitution,
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
        val cleaned = text.trim().trimEnd('.', ',', ';', ':')
        val noiseWords = setOf(
            "SUCCESS", "SUCCESSFUL", "COMPLETED", "PENDING", "FAILED", "APPROVED",
            "CONFIRMED", "EXECUTED", "AED", "USD", "INR", "USDT", "AMOUNT", "PAID",
            "DEBITED", "TRANSFER", "BANK", "PAYMENT", "ACCOUNT", "DATE", "TIME"
        )

        // If entire string is a clean alphanumeric code (with possible hyphens/slashes)
        if (Pattern.compile("^[A-Za-z0-9\\-_/]{4,50}$").matcher(cleaned).matches()) {
            if (!noiseWords.contains(cleaned.uppercase(Locale.US))) {
                return cleaned
            }
        }

        // Split tokens and find valid token
        val tokens = cleaned.split("\\s+".toRegex())
        for (token in tokens) {
            val tClean = token.trim('(', ')', '[', ']', ':', '#', '-', '.', ',')
            if (tClean.length in 4..50 && !noiseWords.contains(tClean.uppercase(Locale.US))) {
                // Must contain at least digits or valid reference structure
                if (tClean.any { it.isDigit() } && (tClean.any { it.isLetter() } || tClean.contains("-") || tClean.length >= 8)) {
                    return tClean
                }
            }
        }
        return null
    }

    private fun parseAmountFromSnippet(snippet: String): Double? {
        if (snippet.isBlank()) return null

        // Ignore timestamps/dates like 2026/09/14, 14-09-2026, 18:06:21
        val datePattern = Pattern.compile("\\b(?:20[2-3][0-9][\\-/\\.][0-9]{1,2}[\\-/\\.][0-9]{1,2}|[0-9]{1,2}[\\-/\\.][0-9]{1,2}[\\-/\\.]20[2-3][0-9]|[0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)\\b")
        val cleanSnippet = datePattern.matcher(snippet).replaceAll(" ")

        // Matches both prefix and postfix currencies: "AED 25,000.00", "25,000.00 AED", "5,000.00", "$5,000", "1,50,000.00"
        val pattern = Pattern.compile(
            "(?:AED|USD|INR|USDT|[$₹د.إ])?\\s*([0-9]{1,3}(?:[,\\s][0-9]{2,3})+(?:\\.[0-9]{1,4})?|[0-9]+(?:\\.[0-9]{1,4})?)\\s*(?:AED|USD|INR|USDT|[$₹د.إ])?",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(cleanSnippet)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "")?.replace(" ", "")?.trim()
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                return parsed
            }
        }
        return null
    }
}
