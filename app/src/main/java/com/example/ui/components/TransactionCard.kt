package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.PoolUser
import com.example.data.model.RecordState
import com.example.data.model.ReconciliationState
import com.example.data.model.SettlementState
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.MutedBlueBorder
import com.example.ui.theme.MutedBlueContainer
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBlueLight
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.RedCritical
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCard(
    transaction: PoolTransactionEntity,
    currentUser: PoolUser,
    onViewProof: () -> Unit,
    onVerifyClick: (Boolean) -> Unit,
    onConfirmReceipt: (() -> Unit)? = null,
    onRaiseDispute: (() -> Unit)? = null,
    onRecordSettlement: (() -> Unit)? = null,
    onResolveDispute: (() -> Unit)? = null,
    onSecondApproval: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val stageColor = when (transaction.stage) {
        TransactionStage.CAPITAL_INJECTION -> MutedBlueLight
        TransactionStage.BANK_TO_EXCHANGE -> MutedBluePrimary
        TransactionStage.USDT_ACQUISITION -> Color(0xFFF59E0B)
        TransactionStage.USDT_DISTRIBUTION -> Color(0xFF8B5CF6)
        TransactionStage.LIQUIDATION -> Color(0xFFEC4899)
    }

    val context = LocalContext.current
    val dateStr = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.US).format(Date(transaction.timestamp))

    val isUserRecipientOrOwner = currentUser.email.equals(transaction.recipientEmail, ignoreCase = true) ||
            currentUser.email.equals(transaction.userEmail, ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_card_${transaction.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Stage badge & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stage chip
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(stageColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = transaction.stage.title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = stageColor
                    )
                }

                // 3-Tier State Machine Chips
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (transaction.driveFileId != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MutedBlueLight.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "☁ DRIVE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedBlueLight
                            )
                        }
                    }

                    // Settlement State Badge
                    SettlementBadge(state = transaction.settlementState)

                    // Reconciliation State Badge
                    ReconBadge(state = transaction.reconciliationState)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Financial Figures Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Primary amount display
                    when (transaction.stage) {
                        TransactionStage.CAPITAL_INJECTION -> {
                            Text(
                                text = "$${formatAmount(transaction.amountFiat ?: 0.0)} ${transaction.fiatCurrency}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Fiat Injected into Central Bank",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TransactionStage.BANK_TO_EXCHANGE -> {
                            Text(
                                text = "$${formatAmount(transaction.amountFiat ?: 0.0)} ${transaction.fiatCurrency}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sent: Bank ➔ Binance Exchange",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TransactionStage.USDT_ACQUISITION -> {
                            Text(
                                text = "${formatAmount(transaction.amountUsdt ?: 0.0)} USDT",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Bought for $${formatAmount(transaction.amountFiat ?: 0.0)} • Rate: $${String.format(Locale.US, "%.4f", transaction.exchangeRate ?: 1.0)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TransactionStage.USDT_DISTRIBUTION -> {
                            Text(
                                text = "${formatAmount(transaction.amountUsdt ?: 0.0)} USDT",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Dispatched to: ${transaction.recipientEmail ?: "Member Wallet"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TransactionStage.LIQUIDATION -> {
                            Text(
                                text = "$${formatAmount(transaction.amountFiat ?: 0.0)} ${transaction.fiatCurrency}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sold ${formatAmount(transaction.amountUsdt ?: 0.0)} USDT • Rate: $${String.format(Locale.US, "%.4f", transaction.exchangeRate ?: 1.0)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Mandatory Proof Thumbnail (Clickable)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onViewProof() }
                        .testTag("proof_thumb_${transaction.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (transaction.proofUri.isNotBlank()) {
                        val imageModel = if (transaction.proofUri.startsWith("/")) File(transaction.proofUri) else transaction.proofUri
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageModel)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Proof Screenshot Receipt",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                        // Overlay badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color(0xCC000000), RoundedCornerShape(topStart = 6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PROOF",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedBlueLight
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Missing Proof",
                                tint = RedCritical,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "MISSING",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedCritical
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Row: Author & Ref ID & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = transaction.userName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reference & Bank UTR
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "REF: ${transaction.getMaskedReference()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (transaction.bankUtrNumber?.isNotBlank() == true) {
                    Text(
                        text = "UTR: ${transaction.bankUtrNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = ProfitGreen
                    )
                } else {
                    Text(
                        text = "Tap image to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedBluePrimary,
                        modifier = Modifier.clickable { onViewProof() }
                    )
                }
            }

            // SECTION 1: Active Dispute Banner & Adjudication
            if (transaction.reconciliationState == ReconciliationState.DISPUTED) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(RedCritical.copy(alpha = 0.12f))
                        .border(1.dp, RedCritical.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "⚠ ACTIVE DISPUTE: ${transaction.disputeReason ?: "Non-delivery reported"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = RedCritical
                        )
                        if (currentUser.isAdmin && onResolveDispute != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onResolveDispute,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedCritical),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Investigate & Resolve Dispute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION 2: Member Confirmation of Receipt (72h SLA window)
            if (transaction.isAwaitingUserConfirm && (isUserRecipientOrOwner || currentUser.isAdmin)) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ProfitGreen.copy(alpha = 0.1f))
                        .border(1.dp, ProfitGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Payment marked Settled. Please confirm receipt in your account/wallet:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onConfirmReceipt != null) {
                                Button(
                                    onClick = onConfirmReceipt,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .testTag("confirm_receipt_btn_${transaction.id}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ProfitGreen,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Confirm Received", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (onRaiseDispute != null) {
                                OutlinedButton(
                                    onClick = onRaiseDispute,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("raise_dispute_btn_${transaction.id}"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Dispute", fontSize = 11.sp, color = RedCritical)
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: Maker-Checker High-Value Dual Approval Banner
            if (transaction.recordState == RecordState.PENDING_SECOND_APPROVAL && currentUser.isAdmin) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberWarning.copy(alpha = 0.12f))
                        .border(1.dp, AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Dual Control: High-Value ($10,000+) Maker approved by ${transaction.verifiedByEmail ?: "Admin"}.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AmberWarning
                        )
                        val isSameUserAsMaker = currentUser.email.equals(transaction.verifiedByEmail, ignoreCase = true)
                        if (isSameUserAsMaker) {
                            Text(
                                text = "Dual-signature policy: A different administrator must sign off as Checker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        } else if (onSecondApproval != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onSecondApproval,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .testTag("checker_signoff_btn_${transaction.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign Off as 2nd Checker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION 4: Admin Settlement Execution (For Unsettled Distributions/Liquidation)
            if (currentUser.canManage && transaction.settlementState == SettlementState.UNSETTLED &&
                (transaction.stage == TransactionStage.USDT_DISTRIBUTION || transaction.stage == TransactionStage.LIQUIDATION) &&
                onRecordSettlement != null
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRecordSettlement,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .testTag("record_settlement_btn_${transaction.id}"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Record Bank UTR / Settle", fontSize = 11.sp, color = MutedBluePrimary)
                }
            }

            // SECTION 5: Standard Admin Wire Deposit Verification for Capital Injections
            if (transaction.stage == TransactionStage.CAPITAL_INJECTION &&
                (transaction.status == TransactionStatus.PENDING_VERIFICATION || transaction.recordState == RecordState.SUBMITTED)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmberWarning.copy(alpha = 0.12f))
                        .border(1.dp, AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Awaiting Central Bank Wire Clearing",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AmberWarning
                            )
                        }

                        if (currentUser.isAdmin) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onVerifyClick(true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("verify_approve_btn_${transaction.id}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MutedBluePrimary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Verify & Credit Bank", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onVerifyClick(false) },
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("verify_reject_btn_${transaction.id}"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Flag Issue", fontSize = 12.sp, color = RedCritical)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Central Administrator (${PoolUser.ADMIN_EMAIL}) must verify wire receipt before funds are credited to pool balance.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementBadge(state: SettlementState) {
    val (color, text) = when (state) {
        SettlementState.SETTLED -> Pair(ProfitGreen, "SETTLED")
        SettlementState.IN_TRANSIT -> Pair(AmberWarning, "IN TRANSIT")
        SettlementState.UNSETTLED -> Pair(MaterialTheme.colorScheme.outline, "UNSETTLED")
        SettlementState.REVERSED -> Pair(RedCritical, "REVERSED")
        SettlementState.FAILED -> Pair(RedCritical, "FAILED")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ReconBadge(state: ReconciliationState) {
    val (color, text) = when (state) {
        ReconciliationState.CONFIRMED_BY_USER -> Pair(ProfitGreen, "CONFIRMED")
        ReconciliationState.CONFIRMED_BY_TIMEOUT -> Pair(ProfitGreen, "AUTO-CONFIRMED")
        ReconciliationState.RECONCILED -> Pair(MutedBluePrimary, "RECONCILED")
        ReconciliationState.PENDING_USER_CONFIRM -> Pair(AmberWarning, "PENDING CONFIRM")
        ReconciliationState.DISPUTED -> Pair(RedCritical, "DISPUTED")
        ReconciliationState.UNRECONCILED -> Pair(MaterialTheme.colorScheme.outline, "UNRECONCILED")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.US, "%,d", amount.toLong())
}
