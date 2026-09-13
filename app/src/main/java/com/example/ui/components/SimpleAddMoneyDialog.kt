package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.AppCurrency
import com.example.data.model.PoolUser
import com.example.ui.theme.MutedBlueBorder
import com.example.ui.theme.MutedBlueContainer
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBlueLight
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import com.example.util.ProofReceiptGenerator
import com.example.util.ScreenshotAnalysisResult
import com.example.util.TransactionScreenshotAnalyzer
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimpleAddMoneyDialog(
    currentUser: PoolUser,
    activeCurrency: AppCurrency = AppCurrency.USD,
    exchangeRates: Map<AppCurrency, Double> = mapOf(
        AppCurrency.USD to 1.0,
        AppCurrency.INR to 83.50,
        AppCurrency.AED to 3.6725
    ),
    onDismiss: () -> Unit,
    onSubmit: (amountFiat: Double, referenceNo: String, proofUri: String, notes: String, currency: AppCurrency, exchangeRate: Double) -> Unit,
    checkDuplicateTxId: (suspend (String) -> Boolean)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedCurrency by remember { mutableStateOf(activeCurrency) }
    var amountInput by remember { mutableStateOf("") }
    var referenceInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }
    var proofPath by remember { mutableStateOf("") }
    var proofUriObject by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // OCR & Vision Analysis State
    var isAnalyzingScreenshot by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<ScreenshotAnalysisResult?>(null) }

    val currentRate = exchangeRates[selectedCurrency] ?: selectedCurrency.defaultRatePerUsd
    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
    val lockedUsd = if (selectedCurrency == AppCurrency.USD) parsedAmount else if (currentRate > 0) parsedAmount / currentRate else 0.0

    // Standard Android Photo Picker for Photos/Gallery/Image Library
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            proofUriObject = uri
            val destFile = File(context.cacheDir, "proof_user_${System.currentTimeMillis()}.png")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                proofPath = destFile.absolutePath
                errorMessage = null

                // Perform OCR & Vision Analysis on the selected transaction screenshot
                isAnalyzingScreenshot = true
                coroutineScope.launch {
                    val result = TransactionScreenshotAnalyzer.analyzeScreenshot(
                        context = context,
                        imageUri = uri,
                        currentlyEnteredAmount = amountInput.toDoubleOrNull(),
                        selectedCurrency = selectedCurrency,
                        checkDuplicateTxId = checkDuplicateTxId
                    )
                    isAnalyzingScreenshot = false
                    analysisResult = result

                    // Automatically populate empty fields with extracted data
                    if (result.extractedAmount != null && amountInput.isBlank()) {
                        amountInput = if (result.extractedAmount % 1.0 == 0.0) {
                            result.extractedAmount.toLong().toString()
                        } else {
                            result.extractedAmount.toString()
                        }
                    }

                    if (result.extractedReferenceId != null && referenceInput.isBlank()) {
                        referenceInput = result.extractedReferenceId
                    }

                    if (result.detectedCurrency != null && result.detectedCurrency != selectedCurrency) {
                        // User can review or switch if desired
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load image file from gallery."
                isAnalyzingScreenshot = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("simple_add_money_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Money / Transfer",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Record money sent to the pool bank account",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_money_dialog_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Currency Picker Segmented Tabs (Fixed: shows clean "AED", never "AED (AED)")
                Text(
                    text = "SELECT TRANSFER CURRENCY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppCurrency.ALL.forEach { currency ->
                        val isSelected = currency == selectedCurrency
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MutedBluePrimary else Color.Transparent)
                                .clickable { selectedCurrency = currency }
                                .padding(vertical = 10.dp)
                                .testTag("currency_tab_${currency.code}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currency.symbol.equals(currency.code, ignoreCase = true)) currency.code else "${currency.code} (${currency.symbol})",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Banner
                if (errorMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFFF87171))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 1. Transfer Amount Input
                Text(
                    text = "TRANSFER AMOUNT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { newValue ->
                        val clean = newValue.filter { it.isDigit() || it == '.' }
                        amountInput = clean
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_transfer_input"),
                    placeholder = { Text("e.g. 5000", fontSize = 18.sp) },
                    prefix = {
                        Text(
                            text = if (selectedCurrency == AppCurrency.AED) "AED " else "${selectedCurrency.symbol} ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedBlueDark
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MutedBluePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                // Quick Preset Chips (Fixed: clean AED format)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = when (selectedCurrency) {
                        AppCurrency.USD -> listOf(500, 1000, 5000, 10000, 25000)
                        AppCurrency.INR -> listOf(50000, 100000, 500000, 1000000)
                        AppCurrency.AED -> listOf(2000, 5000, 10000, 25000)
                    }
                    presets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { amountInput = preset.toString() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedCurrency == AppCurrency.AED) "AED ${FormatUtils.formatInteger(preset.toDouble())}" else "${selectedCurrency.symbol}${FormatUtils.formatInteger(preset.toDouble())}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Financial Traceability & Rate Lock Pill
                if (parsedAmount > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MutedBlueContainer),
                        border = BorderStroke(1.dp, MutedBlueBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MutedBlueDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (selectedCurrency == AppCurrency.USD) {
                                        "Economic Value: $${FormatUtils.formatInteger(parsedAmount)} USD (Base Rate 1.00)"
                                    } else {
                                        "Locked Rate: 1 USD = ${FormatUtils.formatRate(currentRate)} ${selectedCurrency.code} (≈ $${String.format(Locale.US, "%,.2f", lockedUsd)} USD)"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedBlueDark
                                )
                                Text(
                                    text = "This rate will be locked and reproducible in perpetuity for full audit compliance.",
                                    fontSize = 10.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Reference / Slip Number
                Text(
                    text = "BANK WIRE REFERENCE / SLIP NUMBER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = referenceInput,
                    onValueChange = { referenceInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reference_transfer_input"),
                    placeholder = { Text("e.g. WIRE-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-001") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MutedBluePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Screenshot Upload & Vision/OCR Section
                Text(
                    text = "TRANSACTION CONFIRMATION SCREENSHOT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // If currently analyzing
                if (isAnalyzingScreenshot) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = MutedBlueContainer),
                        border = BorderStroke(1.dp, MutedBlueBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MutedBlueDark,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Analyzing transaction screenshot...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedBlueDark
                                )
                                Text(
                                    text = "Extracting amount & reference ID with on-device OCR",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Missing Information Notice (Requirement #5)
                val analysis = analysisResult
                if (analysis != null && analysis.missingNotification != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("missing_ocr_info_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = analysis.missingNotification,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You can replace the screenshot with a clearer image or manually enter/correct the fields above.",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFD97706)),
                                    modifier = Modifier.testTag("replace_screenshot_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF92400E))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Replace Screenshot", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Suspicious Activity Warning Banner (Requirement #6)
                if (analysis != null && analysis.isSuspicious) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("suspicious_warning_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = analysis.statusClassification ?: "Potentially suspicious — requires verification.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            analysis.suspiciousWarnings.forEach { warning ->
                                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                    Text("• ", fontSize = 11.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = warning,
                                        fontSize = 11.sp,
                                        color = Color(0xFF7F1D1D)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notice: This submission will be recorded and marked for administrator verification.",
                                fontSize = 10.sp,
                                color = Color(0xFF991B1B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Extracted Value Review Summary (Requirement #4)
                if (analysis != null && (analysis.extractedAmount != null || analysis.extractedReferenceId != null)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("extracted_review_card"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Screenshot Data Extracted", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                }
                                Text("Review before submit", fontSize = 10.sp, color = Color(0xFF15803D))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (analysis.extractedAmount != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Detected Amount:", fontSize = 11.sp, color = Color(0xFF166534))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = analysis.extractedAmountFormatted ?: analysis.extractedAmount.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF14532D)
                                        )
                                        if (amountInput != analysis.extractedAmount.toString()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Apply",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFDCFCE7))
                                                    .clickable {
                                                        amountInput = if (analysis.extractedAmount % 1.0 == 0.0) {
                                                            analysis.extractedAmount.toLong().toString()
                                                        } else {
                                                            analysis.extractedAmount.toString()
                                                        }
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (analysis.extractedReferenceId != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Detected Ref ID:", fontSize = 11.sp, color = Color(0xFF166534))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = analysis.extractedReferenceId,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF14532D)
                                        )
                                        if (referenceInput != analysis.extractedReferenceId) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Apply",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16A34A),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFDCFCE7))
                                                    .clickable { referenceInput = analysis.extractedReferenceId }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Proof Screenshot Card
                if (proofPath.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MutedBlueBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MutedBlueDark)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Transfer Screenshot Attached",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MutedBlueDark
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Change Screenshot", tint = MutedBlueDark)
                                    }
                                    IconButton(onClick = {
                                        proofPath = ""
                                        proofUriObject = null
                                        analysisResult = null
                                    }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Image(
                                painter = rememberAsyncImagePainter(File(proofPath)),
                                contentDescription = "Proof Screenshot",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("pick_proof_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Screenshot", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val amt = amountInput.toDoubleOrNull() ?: 5000.0
                                val ref = if (referenceInput.isNotBlank()) referenceInput else "WIRE-${System.currentTimeMillis().toString().takeLast(6)}"
                                if (referenceInput.isBlank()) referenceInput = ref

                                val amtFormatted = if (selectedCurrency == AppCurrency.AED) {
                                    "AED ${FormatUtils.formatInteger(amt)}"
                                } else {
                                    "${selectedCurrency.symbol}${FormatUtils.formatInteger(amt)}"
                                }

                                val bitmap = ProofReceiptGenerator.generateReceiptBitmap(
                                    stage = com.example.data.model.TransactionStage.CAPITAL_INJECTION,
                                    userEmail = currentUser.email,
                                    amountText = amtFormatted,
                                    referenceNo = ref,
                                    notes = "Bank wire transfer to central pool (${selectedCurrency.code})"
                                )
                                val generatedPath = ProofReceiptGenerator.saveBitmapToFile(context, bitmap, "wire_slip")
                                proofPath = generatedPath

                                // Analyze generated receipt
                                isAnalyzingScreenshot = true
                                coroutineScope.launch {
                                    val result = TransactionScreenshotAnalyzer.analyzeBitmap(
                                        bitmap = bitmap,
                                        currentlyEnteredAmount = amt,
                                        selectedCurrency = selectedCurrency,
                                        checkDuplicateTxId = checkDuplicateTxId
                                    )
                                    isAnalyzingScreenshot = false
                                    analysisResult = result
                                }
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("generate_proof_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MutedBluePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Slip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Notes (Optional)
                Text(
                    text = "MEMO / NOTES (OPTIONAL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notes_transfer_input"),
                    placeholder = { Text("e.g. Monthly transfer from bank") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MutedBluePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Action Button
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull()
                        if (amt == null || amt <= 0.0) {
                            errorMessage = "Please enter a valid transfer amount."
                            return@Button
                        }
                        if (referenceInput.isBlank()) {
                            errorMessage = "Bank reference / wire slip number is required."
                            return@Button
                        }
                        if (proofPath.isBlank()) {
                            errorMessage = "Proof of transaction screenshot is mandatory."
                            return@Button
                        }

                        // Build final notes including audit tag if suspicious
                        val finalNotes = buildString {
                            if (notesInput.isNotBlank()) append(notesInput.trim())
                            val analysis = analysisResult
                            if (analysis != null && analysis.isSuspicious) {
                                if (isNotEmpty()) append(" | ")
                                append("[Potentially suspicious — requires verification: ")
                                append(analysis.suspiciousWarnings.joinToString("; "))
                                append("]")
                            }
                        }

                        errorMessage = null
                        onSubmit(amt, referenceInput.trim(), proofPath, finalNotes, selectedCurrency, currentRate)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_transfer_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MutedBluePrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Transfer", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
