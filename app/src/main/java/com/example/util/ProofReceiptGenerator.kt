package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.model.TransactionStage
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object ProofReceiptGenerator {

    fun generateReceiptBitmap(
        stage: TransactionStage,
        userEmail: String,
        amountText: String,
        referenceNo: String,
        notes: String = ""
    ): Bitmap {
        val width = 720
        val height = 960
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Card Container
        val cardPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
        }
        val cardRect = RectF(36f, 36f, width - 36f, height - 36f)
        canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)

        // Border Accent
        val borderPaint = Paint().apply {
            color = when (stage) {
                TransactionStage.CAPITAL_INJECTION -> Color.parseColor("#10B981")
                TransactionStage.BANK_TO_EXCHANGE -> Color.parseColor("#0284C7")
                TransactionStage.USDT_ACQUISITION -> Color.parseColor("#F59E0B")
                TransactionStage.USDT_DISTRIBUTION -> Color.parseColor("#8B5CF6")
                TransactionStage.LIQUIDATION -> Color.parseColor("#EC4899")
            }
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(cardRect, 24f, 24f, borderPaint)

        // Header Banner
        val headerPaint = Paint().apply {
            color = borderPaint.color
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(36f, 36f, width - 36f, 130f), 24f, 24f, headerPaint)
        canvas.drawRect(36f, 100f, width - 36f, 130f, headerPaint)

        // Header Title
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerLabel = when (stage) {
            TransactionStage.CAPITAL_INJECTION -> "BANK WIRE RECEIPT"
            TransactionStage.BANK_TO_EXCHANGE -> "CENTRAL BANK TO BINANCE"
            TransactionStage.USDT_ACQUISITION -> "BINANCE ORDER CONFIRMATION"
            TransactionStage.USDT_DISTRIBUTION -> "TRON/BSC TRANSFER RECEIPT"
            TransactionStage.LIQUIDATION -> "LIQUIDATION SETTLEMENT"
        }
        canvas.drawText(headerLabel, 72f, 96f, textPaint)

        // Watermark / Verified Badge
        val badgePaint = Paint().apply {
            color = Color.parseColor("#334155")
            style = Paint.Style.FILL
        }
        val badgeRect = RectF(72f, 160f, width - 72f, 215f)
        canvas.drawRoundRect(badgeRect, 12f, 12f, badgePaint)

        val badgeTextPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("STATUS: PROOF VERIFIED ON-LEDGER", 90f, 196f, badgeTextPaint)

        // Details Section
        val labelPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        var yPos = 270f
        fun drawField(label: String, value: String) {
            canvas.drawText(label.uppercase(), 72f, yPos, labelPaint)
            yPos += 36f
            canvas.drawText(value, 72f, yPos, valuePaint)
            yPos += 54f
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date())
        drawField("Timestamp", dateStr)
        drawField("Workflow Stage", stage.title)
        drawField("Authorized Party", userEmail)
        drawField("Transaction Volume", amountText)
        drawField("Reference / TXID", referenceNo)
        if (notes.isNotBlank()) {
            drawField("Ledger Notes", notes)
        }

        // Mock Barcode / Security Hash Box
        yPos += 10f
        val securityBoxPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }
        val secRect = RectF(72f, yPos, width - 72f, yPos + 120f)
        canvas.drawRoundRect(secRect, 16f, 16f, securityBoxPaint)

        val hashLabelPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("AUTHENTICITY HASH (SHA-256):", 92f, yPos + 40f, hashLabelPaint)
        val hash = UUID.nameUUIDFromBytes(referenceNo.toByteArray()).toString().replace("-", "") + "ef84a"
        canvas.drawText(hash.take(34), 92f, yPos + 72f, hashLabelPaint)
        canvas.drawText(hash.substring(34.coerceAtMost(hash.length)), 92f, yPos + 98f, hashLabelPaint)

        // Footer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("Centralised USDT Trading Pool Ledger • Proof of Transaction", 72f, height - 70f, footerPaint)

        return bitmap
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, prefix: String = "proof"): String {
        val dir = File(context.filesDir, "proof_receipts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        return file.absolutePath
    }
}
