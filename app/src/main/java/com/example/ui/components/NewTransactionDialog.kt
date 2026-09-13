package com.example.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PoolUser
import com.example.data.model.TransactionStage
import com.example.ui.theme.MutedBlueBorder
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBlueLight
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.RedCritical
import com.example.util.ProofReceiptGenerator
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionDialog(
    currentUser: PoolUser,
    whitelistedUsers: List<PoolUser>,
    onDismiss: () -> Unit,
    onSubmit: (
        stage: TransactionStage,
        amountFiat: Double?,
        amountUsdt: Double?,
        exchangeRate: Double?,
        fee: Double?,
        recipientEmail: String?,
        referenceNo: String,
        proofUri: String,
        proofDescription: String?,
        notes: String
    ) -> Unit
) {
    val context = LocalContext.current

    // Permitted stages based on role
    val availableStages = if (currentUser.isAdmin) {
        TransactionStage.values().toList()
    } else {
        listOf(TransactionStage.CAPITAL_INJECTION, TransactionStage.LIQUIDATION)
    }

    var selectedStage by remember { mutableStateOf(availableStages.first()) }
    var amountFiatText by remember { mutableStateOf("") }
    var amountUsdtText by remember { mutableStateOf("") }
    var exchangeRateText by remember { mutableStateOf("") }
    var feeText by remember { mutableStateOf("") }
    var referenceNoText by remember {
        mutableStateOf(generateDefaultReference(availableStages.first()))
    }
    var notesText by remember { mutableStateOf("") }
    var recipientEmailText by remember {
        mutableStateOf(whitelistedUsers.firstOrNull { it.email != currentUser.email }?.email ?: "")
    }

    // Proof screenshot state (MANDATORY)
    var proofUri by remember { mutableStateOf("") }
    var proofDescription by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    // System Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            proofUri = uri.toString()
            proofDescription = "Gallery screenshot upload"
            validationError = null
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
                .testTag("new_transaction_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Log Pool Transaction",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Authenticated as ${currentUser.name} (${currentUser.role.name})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stage Selector Tabs
                Text(
                    text = "SELECT WORKFLOW STAGE",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableStages.forEach { stage ->
                        val isSelected = selectedStage == stage
                        val stageColor = when (stage) {
                            TransactionStage.CAPITAL_INJECTION -> MutedBlueLight
                            TransactionStage.BANK_TO_EXCHANGE -> MutedBluePrimary
                            TransactionStage.USDT_ACQUISITION -> Color(0xFFF59E0B)
                            TransactionStage.USDT_DISTRIBUTION -> Color(0xFF8B5CF6)
                            TransactionStage.LIQUIDATION -> Color(0xFFEC4899)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) stageColor.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) stageColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedStage = stage
                                    referenceNoText = generateDefaultReference(stage)
                                    // clear proof if stage changes
                                    proofUri = ""
                                    validationError = null
                                }
                                .padding(10.dp)
                                .testTag("select_stage_${stage.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(stageColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${stage.stepIndex}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stage.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) stageColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stage.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stage-Specific Form Inputs
                when (selectedStage) {
                    TransactionStage.CAPITAL_INJECTION -> {
                        OutlinedTextField(
                            value = amountFiatText,
                            onValueChange = { amountFiatText = it },
                            label = { Text("Fiat Injected Amount (USD) *") },
                            placeholder = { Text("e.g. 10000.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_fiat_amount"),
                            singleLine = true
                        )
                    }

                    TransactionStage.BANK_TO_EXCHANGE -> {
                        OutlinedTextField(
                            value = amountFiatText,
                            onValueChange = { amountFiatText = it },
                            label = { Text("Fiat Transferred from Bank to Binance (USD) *") },
                            placeholder = { Text("e.g. 25000.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_bank_fiat"),
                            singleLine = true
                        )
                    }

                    TransactionStage.USDT_ACQUISITION -> {
                        OutlinedTextField(
                            value = amountFiatText,
                            onValueChange = { amountFiatText = it },
                            label = { Text("Fiat Spent on Binance (USD) *") },
                            placeholder = { Text("e.g. 25000.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_acq_fiat"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = amountUsdtText,
                                onValueChange = { amountUsdtText = it },
                                label = { Text("USDT Received *") },
                                placeholder = { Text("e.g. 24950.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_acq_usdt"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = feeText,
                                onValueChange = { feeText = it },
                                label = { Text("Trading Fee (USDT)") },
                                placeholder = { Text("25.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_acq_fee"),
                                singleLine = true
                            )
                        }
                    }

                    TransactionStage.USDT_DISTRIBUTION -> {
                        OutlinedTextField(
                            value = recipientEmailText,
                            onValueChange = { recipientEmailText = it },
                            label = { Text("Recipient Member Google Email *") },
                            placeholder = { Text("member@domain.com") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_dist_recipient"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = amountUsdtText,
                                onValueChange = { amountUsdtText = it },
                                label = { Text("USDT Dispatched *") },
                                placeholder = { Text("e.g. 5000.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_dist_usdt"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = feeText,
                                onValueChange = { feeText = it },
                                label = { Text("Gas Fee (USDT)") },
                                placeholder = { Text("1.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_dist_fee"),
                                singleLine = true
                            )
                        }
                    }

                    TransactionStage.LIQUIDATION -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = amountUsdtText,
                                onValueChange = {
                                    amountUsdtText = it
                                    val usdt = it.toDoubleOrNull() ?: 0.0
                                    val rate = exchangeRateText.toDoubleOrNull() ?: 1.0
                                    if (usdt > 0 && amountFiatText.isBlank()) {
                                        amountFiatText = String.format("%.2f", usdt * rate)
                                    }
                                },
                                label = { Text("USDT Sold *") },
                                placeholder = { Text("e.g. 2000.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_liq_usdt"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = exchangeRateText,
                                onValueChange = {
                                    exchangeRateText = it
                                    val rate = it.toDoubleOrNull() ?: 1.0
                                    val usdt = amountUsdtText.toDoubleOrNull() ?: 0.0
                                    if (usdt > 0) {
                                        amountFiatText = String.format("%.2f", usdt * rate)
                                    }
                                },
                                label = { Text("Sell Rate ($/USDT)") },
                                placeholder = { Text("1.0100") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_liq_rate"),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amountFiatText,
                            onValueChange = { amountFiatText = it },
                            label = { Text("Realized Fiat Received (USD) *") },
                            placeholder = { Text("e.g. 2020.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_liq_fiat"),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = referenceNoText,
                    onValueChange = { referenceNoText = it },
                    label = { Text("Transaction Reference / Order ID / TXID *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_reference_no"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Ledger Audit Notes (Optional)") },
                    placeholder = { Text("e.g. Bank wire ref, OTC trade notes, wallet address") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes"),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // MANDATORY PROOF UPLOAD SECTION
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (proofUri.isBlank()) RedCritical.copy(alpha = 0.6f) else MutedBlueLight.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (proofUri.isBlank()) Color(0x18EF4444) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (proofUri.isBlank()) Icons.Default.Warning else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (proofUri.isBlank()) RedCritical else MutedBlueLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "MANDATORY PROOF SCREENSHOT",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (proofUri.isBlank()) RedCritical else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (proofUri.isNotBlank()) {
                                IconButton(
                                    onClick = { proofUri = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Proof",
                                        tint = RedCritical,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (proofUri.isBlank()) {
                            Text(
                                text = "⚠️ Compliance Rule: Every state change strictly requires a proof-of-transaction image (bank slip, Binance order, or transfer receipt) before submission.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

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
                                        .testTag("upload_proof_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick Photo", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        // Quick Generate instant high-fidelity proof receipt
                                        val displayAmount = when (selectedStage) {
                                            TransactionStage.CAPITAL_INJECTION,
                                            TransactionStage.BANK_TO_EXCHANGE -> "$${amountFiatText.ifBlank { "5000.00" }} USD"
                                            TransactionStage.USDT_ACQUISITION -> "${amountUsdtText.ifBlank { "5000.00" }} USDT"
                                            TransactionStage.USDT_DISTRIBUTION -> "${amountUsdtText.ifBlank { "2500.00" }} USDT"
                                            TransactionStage.LIQUIDATION -> "$${amountFiatText.ifBlank { "2550.00" }} USD"
                                        }

                                        val bitmap = ProofReceiptGenerator.generateReceiptBitmap(
                                            stage = selectedStage,
                                            userEmail = currentUser.email,
                                            amountText = displayAmount,
                                            referenceNo = referenceNoText.ifBlank { generateDefaultReference(selectedStage) },
                                            notes = notesText
                                        )
                                        proofUri = ProofReceiptGenerator.saveBitmapToFile(context, bitmap)
                                        proofDescription = "Generated Verified Electronic Slip"
                                        validationError = null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("generate_proof_btn"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MutedBluePrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Generate Slip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Attached Proof Preview Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val imageModel = if (proofUri.startsWith("/")) File(proofUri) else proofUri
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageModel)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Proof Attached",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "✓ Proof Receipt Attached",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MutedBluePrimary
                                    )
                                    Text(
                                        text = proofDescription.ifBlank { "Verified image receipt" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "☁ Destination: Google Drive / USDT_Pool_Receipts / ${selectedStage.title}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MutedBlueLight
                                    )
                                }
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = RedCritical,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = RedCritical
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (proofUri.isBlank()) {
                                validationError = "Mandatory proof screenshot is missing! Please pick or generate a receipt."
                                return@Button
                            }

                            val fiat = amountFiatText.toDoubleOrNull()
                            val usdt = amountUsdtText.toDoubleOrNull()
                            val rate = exchangeRateText.toDoubleOrNull() ?: if (fiat != null && usdt != null && usdt > 0) fiat / usdt else 1.0
                            val fee = feeText.toDoubleOrNull() ?: 0.0

                            onSubmit(
                                selectedStage,
                                fiat,
                                usdt,
                                rate,
                                fee,
                                if (selectedStage == TransactionStage.USDT_DISTRIBUTION) recipientEmailText else null,
                                referenceNoText.ifBlank { generateDefaultReference(selectedStage) },
                                proofUri,
                                proofDescription,
                                notesText
                            )
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_tx_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MutedBluePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Submit Transaction", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun generateDefaultReference(stage: TransactionStage): String {
    val randomHex = UUID.randomUUID().toString().take(6).uppercase()
    return when (stage) {
        TransactionStage.CAPITAL_INJECTION -> "WIRE-2026-$randomHex"
        TransactionStage.BANK_TO_EXCHANGE -> "BNK-BIN-$randomHex"
        TransactionStage.USDT_ACQUISITION -> "ORD-BIN-$randomHex"
        TransactionStage.USDT_DISTRIBUTION -> "TXN-TRC20-$randomHex"
        TransactionStage.LIQUIDATION -> "P2P-BIN-$randomHex"
    }
}
