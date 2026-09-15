package com.example.ui.components

import com.example.data.model.PeriodSummary
import com.example.data.model.TimePeriodFilter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.PoolMetrics
import com.example.data.model.PoolUser
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.MutedBlueBorder
import com.example.ui.theme.MutedBlueContainer
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBorder
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.ProfitGreenDark
import com.example.ui.theme.RestrainedRed
import com.example.ui.theme.RestrainedRedBorder
import com.example.ui.theme.RestrainedRedContainer
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Replay
import com.example.data.model.RecordState
import com.example.data.model.ReconciliationState
import com.example.data.model.SettlementState

@Composable
fun AdminOperationsPanel(
    currentUser: PoolUser,
    metrics: PoolMetrics,
    transactions: List<PoolTransactionEntity>,
    selectedFilter: com.example.data.model.TimePeriodFilter? = null,
    periodSummary: PeriodSummary? = null,
    onSelectFilter: ((com.example.data.model.TimePeriodFilter) -> Unit)? = null,
    onConfirmBankReceipt: (Long) -> Unit,
    onOpenOperation: (TransactionStage) -> Unit,
    onViewProofClick: (PoolTransactionEntity) -> Unit,
    onOpenDistributeMoney: () -> Unit = {},
    onReverseTransaction: (PoolTransactionEntity) -> Unit = {},
    onOpenFifoLotAudit: () -> Unit = {},
    onResolveDispute: (PoolTransactionEntity) -> Unit = {},
    onSecondApproval: (PoolTransactionEntity) -> Unit = {},
    onRecordSettlement: (PoolTransactionEntity) -> Unit = {}
) {
    val pendingInjections = transactions.filter {
        it.stage == TransactionStage.CAPITAL_INJECTION && 
        (it.status == TransactionStatus.PENDING_VERIFICATION || it.approvalStatus == "PENDING" || it.recordState == RecordState.SUBMITTED)
    }

    val activeDisputes = transactions.filter {
        it.reconciliationState == ReconciliationState.DISPUTED
    }

    val dualApprovalQueue = transactions.filter {
        it.recordState == RecordState.PENDING_SECOND_APPROVAL
    }

    val approvedTransactions = transactions.filter {
        it.approvalStatus == "APPROVED" || it.status == TransactionStatus.VERIFIED || it.status == TransactionStatus.COMPLETED || it.status == TransactionStatus.REVERSED
    }

    val timeFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_operations_panel"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Header Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MutedBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MutedBlueDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentUser.isAdmin) "Admin Control Center" else "Sub-Admin Operations",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MutedBlueContainer)
                                    .border(
                                        1.dp,
                                        MutedBlueBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentUser.role.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedBlueDark
                                )
                            }
                        }
                        Text(
                            text = "Manage member bank clearances, crypto acquisitions, and liquidations.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Time Period Filter
        if (selectedFilter != null && periodSummary != null && onSelectFilter != null) {
            item {
                TimePeriodSelector(
                    selectedFilter = selectedFilter,
                    periodSummary = periodSummary,
                    onSelectFilter = onSelectFilter
                )
            }
        }

        // Active Disputes Section (if any exist)
        if (activeDisputes.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_disputes_card"),
                    colors = CardDefaults.cardColors(containerColor = RestrainedRedContainer),
                    border = BorderStroke(1.dp, RestrainedRedBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ReportProblem,
                                contentDescription = null,
                                tint = RestrainedRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACTIVE DISPUTES AWAITING ADJUDICATION (${activeDisputes.size})",
                                fontWeight = FontWeight.Bold,
                                color = RestrainedRed,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Traders or members flagged payments as uncredited. Investigate bank UTR traces or issue compensating reversals.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
            items(activeDisputes, key = { "dispute_${it.id}" }) { tx ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dispute_item_${tx.id}"),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, RestrainedRedBorder.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Dispute on Ref #${tx.referenceNo}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = tx.getDisplayAmount(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = RestrainedRed
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reason: ${tx.disputeReason ?: "Non-delivery reported"}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onResolveDispute(tx) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .testTag("admin_adjudicate_btn_${tx.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = RestrainedRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Investigate & Adjudicate Dispute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Maker-Checker Dual Approval Queue (if any exist)
        if (dualApprovalQueue.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dual_approval_card"),
                    colors = CardDefaults.cardColors(containerColor = AmberContainer),
                    border = BorderStroke(1.dp, AmberTertiary.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = AmberTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MAKER-CHECKER DUAL APPROVAL QUEUE (${dualApprovalQueue.size})",
                                fontWeight = FontWeight.Bold,
                                color = AmberTertiary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "High-value transactions ($10,000+) require sign-off by a 2nd administrator.",
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
            items(dualApprovalQueue, key = { "dual_${it.id}" }) { tx ->
                val isSameAsMaker = currentUser.email.equals(tx.verifiedByEmail, ignoreCase = true)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dual_item_${tx.id}"),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, AmberTertiary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ref #${tx.referenceNo} • ${tx.stage.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = tx.getDisplayAmount(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AmberTertiary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Maker 1st approval by: ${tx.verifiedByEmail ?: "Admin"}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isSameAsMaker) {
                            Text(
                                text = "Dual control policy: A different administrator must sign as Checker.",
                                fontSize = 11.sp,
                                color = AmberTertiary
                            )
                        } else {
                            Button(
                                onClick = { onSecondApproval(tx) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .testTag("admin_checker_signoff_${tx.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = AmberTertiary, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign Off as 2nd Checker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section 1: Pending Bank Confirmations
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PENDING BANK VERIFICATIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    if (pendingInjections.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberContainer)
                                .border(1.dp, AmberTertiary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${pendingInjections.size} Awaiting Clearance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberTertiary
                            )
                        }
                    }
                }
                Text(
                    text = "Review incoming member bank deposits and verify funds once received.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        if (pendingInjections.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MutedBlueDark,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "All member transfers cleared! No pending bank deposits.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        } else {
            items(pendingInjections, key = { it.id }) { tx ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_verification_card_${tx.id}"),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = tx.userName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${tx.userEmail} · ${tx.recordDate} ${tx.recordTime}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            val txCurr = com.example.data.model.AppCurrency.fromCode(tx.originalCurrency)
                            val origAmt = if (tx.originalAmount > 0) tx.originalAmount else (tx.amountFiat ?: 0.0)
                            val origFormatted = "+${FormatUtils.formatCurrencyZeroDecimal(origAmt, txCurr)}"
                            val convertedUsd = if (txCurr != com.example.data.model.AppCurrency.USD && tx.convertedAmountUsd > 0) {
                                " (~$${FormatUtils.formatInteger(tx.convertedAmountUsd)})"
                            } else ""

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = origFormatted,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF2563EB)
                                )
                                if (convertedUsd.isNotBlank()) {
                                    Text(
                                        text = convertedUsd,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ref: ${tx.referenceNo}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            // View Slip
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateCardElevated)
                                    .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                                    .clickable { onViewProofClick(tx) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MutedBlueDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check Slip", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MutedBlueDark)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // One-Tap Bank Confirmation Button
                        Button(
                            onClick = { onConfirmBankReceipt(tx.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_bank_receipt_btn_${tx.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutedBluePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Approve & Confirm Received in Bank",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Pipeline Management Operations
        item {
            Column {
                Text(
                    text = "EXECUTE PIPELINE OPERATIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Move fiat to Binance, buy USDT with pool capital, or execute liquidation payouts.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Operation 1: Bank to Exchange
                AdminOpActionCard(
                    icon = Icons.Default.AccountBalance,
                    iconTint = MutedBlueDark,
                    iconContainer = MutedBlueContainer,
                    title = "Bank to Exchange Transfer",
                    subtitle = "Move verified member fiat from central bank to Binance corporate account",
                    badgeText = "${FormatUtils.formatCurrencyZeroDecimal(metrics.centralBankBalanceFiat, metrics.currency)} Available in Bank",
                    onClick = { onOpenOperation(TransactionStage.BANK_TO_EXCHANGE) },
                    testTag = "op_bank_to_exchange"
                )

                // Operation 2: Buy USDT
                AdminOpActionCard(
                    icon = Icons.Default.CurrencyExchange,
                    iconTint = MutedBlueDark,
                    iconContainer = MutedBlueContainer,
                    title = "Buy USDT (OTC / Spot)",
                    subtitle = "Convert fiat to USDT on Binance. Fee deducted from pool capital",
                    badgeText = "Rate ~$1.0020",
                    onClick = { onOpenOperation(TransactionStage.USDT_ACQUISITION) },
                    testTag = "op_buy_usdt"
                )

                // Operation 3: Distribute to Trading Accounts
                AdminOpActionCard(
                    icon = Icons.Default.Send,
                    iconTint = MutedBlueDark,
                    iconContainer = MutedBlueContainer,
                    title = "Distribute Funds to Traders",
                    subtitle = "Explicit, authorized payout of USDT or fiat with atomic balance verification",
                    badgeText = "${FormatUtils.formatInteger(metrics.unallocatedCentralUsdt)} USDT Unallocated",
                    onClick = { onOpenDistributeMoney() },
                    testTag = "op_distribute_usdt"
                )

                // Operation 4: Liquidation & Fiat Payout
                AdminOpActionCard(
                    icon = Icons.Default.Paid,
                    iconTint = MutedBlueDark,
                    iconContainer = MutedBlueContainer,
                    title = "Liquidation / Fiat Payout",
                    subtitle = "Sell USDT; fiat returns to central bank, recording final transaction amount",
                    badgeText = "${FormatUtils.formatCurrencyZeroDecimal(metrics.totalFiatRealised, metrics.currency)} Realized to Date",
                    onClick = { onOpenOperation(TransactionStage.LIQUIDATION) },
                    testTag = "op_liquidation"
                )

                // Operation 5: FIFO Inventory Engine & Lot Audit
                AdminOpActionCard(
                    icon = Icons.Default.Inventory2,
                    iconTint = MutedBlueDark,
                    iconContainer = MutedBlueContainer,
                    title = "FIFO Inventory Engine & Lot Audit",
                    subtitle = "Track discrete acquisition lots, remaining USDT quantities, unit cost, and realized P&L",
                    badgeText = "FIFO Cost-Basis Engine",
                    onClick = { onOpenFifoLotAudit() },
                    testTag = "op_fifo_lot_audit"
                )
            }
        }

        // Section 3: Approvals & Verified Screenshots Audit Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "APPROVALS & PROOFS AUDIT LOG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Every transaction's Date, Timestamp, Approver, and Screenshot",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MutedBlueContainer)
                        .border(1.dp, MutedBlueBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${approvedTransactions.size} Processed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MutedBlueDark
                    )
                }
            }
        }

        if (approvedTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, tint = TextMuted)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "No approved transactions yet recorded.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(approvedTransactions.take(8), key = { "audit_${it.id}" }) { tx ->
                val isReversed = tx.status == TransactionStatus.REVERSED
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audit_tx_card_${tx.id}"),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(
                        1.dp,
                        if (isReversed) RestrainedRedBorder else SlateBorder
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tx.userName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isReversed) RestrainedRedContainer else MutedBlueContainer)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isReversed) "REVERSED" else tx.status.label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isReversed) RestrainedRed else MutedBlueDark
                                        )
                                    }
                                }
                                Text(
                                    text = "${tx.stage.title} · Ref: ${tx.referenceNo}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }

                            val txCurr = com.example.data.model.AppCurrency.fromCode(tx.originalCurrency)
                            val amtText = when {
                                tx.originalAmount > 0.0 -> FormatUtils.formatCurrencyZeroDecimal(tx.originalAmount, txCurr)
                                tx.amountFiat != null -> FormatUtils.formatCurrencyZeroDecimal(tx.amountFiat, metrics.currency)
                                tx.amountUsdt != null -> "${FormatUtils.formatInteger(tx.amountUsdt)} USDT"
                                else -> if (metrics.currency == com.example.data.model.AppCurrency.AED) "AED 0" else "${metrics.currency.symbol}0"
                            }
                            Text(
                                text = amtText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isReversed) TextMuted else TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Audit Metadata Row: Date & Timestamp
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SlateCardElevated)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recorded: ${tx.recordDate} · ${tx.recordTime}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "Approved by: ${tx.approvedByEmail?.substringBefore("@") ?: "Admin"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedBlueDark
                            )
                        }

                        if (isReversed && !tx.reversalReason.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Reversal: ${tx.reversalReason}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = RestrainedRed
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tx.approvalNotes ?: "Verified bank receipt",
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SlateCardElevated)
                                        .border(1.dp, SlateBorder, RoundedCornerShape(6.dp))
                                        .clickable { onViewProofClick(tx) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MutedBlueDark,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "View Slip",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MutedBlueDark
                                    )
                                }

                                if (currentUser.isAdmin && !isReversed && tx.status != TransactionStatus.CANCELLED) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(RestrainedRedContainer)
                                            .border(1.dp, RestrainedRedBorder, RoundedCornerShape(6.dp))
                                            .clickable { onReverseTransaction(tx) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("reverse_tx_btn_${tx.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Replay,
                                            contentDescription = null,
                                            tint = RestrainedRed,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reverse",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RestrainedRed
                                        )
                                    }
                                }

                                // Record Settlement button if pending settlement
                                if (currentUser.isAdmin && (tx.settlementState == SettlementState.UNSETTLED || tx.settlementState == SettlementState.IN_TRANSIT) && !isReversed) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MutedBlueContainer)
                                            .border(1.dp, MutedBlueBorder, RoundedCornerShape(6.dp))
                                            .clickable { onRecordSettlement(tx) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("record_settle_btn_${tx.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = MutedBlueDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Record Settlement",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MutedBlueDark
                                        )
                                    }
                                }

                                // Adjudicate Dispute button if disputed
                                if (currentUser.isAdmin && tx.reconciliationState == ReconciliationState.DISPUTED) {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(RestrainedRedContainer)
                                            .border(1.dp, RestrainedRedBorder, RoundedCornerShape(6.dp))
                                            .clickable { onResolveDispute(tx) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .testTag("audit_adjudicate_btn_${tx.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gavel,
                                            contentDescription = null,
                                            tint = RestrainedRed,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Adjudicate",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RestrainedRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Central Bank & Solvency Summary
        item {
            Text(
                text = "SOLVENCY & AUDIT AUDIENCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextSecondary
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Central Bank Fiat", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = FormatUtils.formatCurrencyZeroDecimal(metrics.centralBankBalanceFiat, metrics.currency),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(text = "Awaiting wire to exchange", fontSize = 10.sp, color = TextMuted)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Exchange USDT", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "${FormatUtils.formatInteger(metrics.unallocatedCentralUsdt)} USDT",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MutedBlueDark
                        )
                        Text(text = "Undistributed inventory", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminOpActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconContainer: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlateCardElevated)
                    .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }
        }
    }
}

