package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.util.FormatUtils
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.PoolTransactionEntity
import com.example.ui.theme.MutedBlueBorder
import com.example.ui.theme.MutedBlueContainer
import com.example.ui.theme.MutedBlueLight
import com.example.ui.theme.MutedBluePrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProofLightboxDialog(
    transaction: PoolTransactionEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US).format(Date(transaction.timestamp))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("proof_lightbox_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MutedBlueLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = MutedBlueLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PROOF OF TRANSACTION",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mandatory Compliance Ledger Receipt",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_proof_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High-Res Image Preview Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val imageModel = if (transaction.proofUri.startsWith("/")) {
                            File(transaction.proofUri)
                        } else {
                            transaction.proofUri
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Proof Screenshot Full Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        )

                        // Verified Watermark
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xDD1B4266))
                                .border(1.dp, MutedBlueLight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "✓ AUDIT COMPLIANT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Details
                Text(
                    text = "TRANSACTION AUDIT LOG",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                AuditField(label = "Workflow Stage", value = transaction.stage.title)
                AuditField(label = "Logged By", value = "${transaction.userName} (${transaction.userEmail})")
                if (transaction.recipientEmail != null) {
                    AuditField(label = "Recipient Account", value = transaction.recipientEmail)
                }
                
                // Multi-Currency Audit Section
                if (transaction.originalAmount > 0.0) {
                    val origCurr = com.example.data.model.AppCurrency.fromCode(transaction.originalCurrency)
                    AuditField(
                        label = "Original Amount",
                        value = "${FormatUtils.formatCurrencyZeroDecimal(transaction.originalAmount, origCurr)} ${transaction.originalCurrency}"
                    )
                    AuditField(
                        label = "Locked FX Rate",
                        value = "1 USD = ${FormatUtils.formatRate(transaction.fiatExchangeRate)} ${transaction.originalCurrency}"
                    )
                    AuditField(
                        label = "Base USD Value",
                        value = "$${String.format(java.util.Locale.US, "%,.2f", transaction.convertedAmountUsd)} USD"
                    )
                } else if (transaction.amountFiat != null) {
                    AuditField(label = "Amount (Fiat)", value = "$${FormatUtils.formatInteger(transaction.amountFiat)} ${transaction.fiatCurrency}")
                }
                if (transaction.amountUsdt != null) {
                    AuditField(label = "Amount (USDT)", value = "${FormatUtils.formatInteger(transaction.amountUsdt)} USDT")
                }

                AuditField(label = "Reference / TXID", value = transaction.referenceNo, isMono = true)
                AuditField(label = "Timestamp", value = dateStr)
                AuditField(label = "Compliance Status", value = transaction.status.label)
                if (transaction.verifiedByEmail != null) {
                    AuditField(label = "Verified By Admin", value = transaction.verifiedByEmail)
                }
                if (transaction.notes.isNotBlank()) {
                    AuditField(label = "Ledger Notes", value = transaction.notes)
                }
                AuditField(
                    label = "Google Drive Cloud Backup",
                    value = if (transaction.driveFileId != null) "Synced to USDT_Pool_Receipts" else "Connected to dipuraj.thapa@gmail.com"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Button to open proof in Google Drive
                Button(
                    onClick = {
                        val targetUrl = transaction.driveWebViewLink ?: "https://drive.google.com/drive/u/0/my-drive"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_proof_in_drive_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedBlueLight.copy(alpha = 0.2f),
                        contentColor = MutedBluePrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Receipt in Google Drive",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Transparency Notice: All registered members have read access to this immutable receipt to ensure proof-of-transaction integrity across the entire pool lifecycle.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AuditField(label: String, value: String, isMono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}
