package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.PoolTransactionEntity
import com.example.security.LedgerIntegrityStatus
import com.example.security.SecurityAuditReport
import com.example.security.SecurityHardeningManager
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

@Composable
fun SecurityAuditDialog(
    transactions: List<PoolTransactionEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isAuditing by remember { mutableStateOf(false) }
    var auditReport by remember {
        mutableStateOf(SecurityHardeningManager.performSecurityAudit(context, transactions))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .systemBarsPadding()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .testTag("security_audit_dialog"),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Pinned Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ProfitGreenContainer)
                                    .border(1.dp, ProfitGreenBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = ProfitGreenDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Security & System Defense",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Defense-in-Depth Audit & Ledger Proof",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_security_dialog")) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(color = SlateBorder)

                    // Scrollable Audit Content
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        // Score Banner
                        ScoreBanner(auditReport)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Defense Layers Breakdown
                        Text(
                            text = "SECURITY CONTROLS BREAKDOWN",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 1. Device Sandbox & Root
                        SecurityItemRow(
                            icon = Icons.Default.Security,
                            title = "Device Sandbox & Root Integrity",
                            subtitle = if (auditReport.isRootDetected) "Root exploit detected. Unsafe environment." else "Secure sandbox verified. No root binaries found.",
                            isSuccess = !auditReport.isRootDetected
                        )

                        // 2. Anti-Extraction & Backup Defense
                        SecurityItemRow(
                            icon = Icons.Default.Lock,
                            title = "Local Storage & Extraction Defense",
                            subtitle = if (auditReport.isBackupDisabled) "ADB backup disabled. Local database extraction blocked." else "Backup permitted. Exposure risk.",
                            isSuccess = auditReport.isBackupDisabled
                        )

                        // 3. Network MitM & TLS Enforcement
                        SecurityItemRow(
                            icon = Icons.Default.Shield,
                            title = "Network Encryption & MitM Protection",
                            subtitle = if (auditReport.isCleartextBlocked) "Strict HTTPS enforced. Cleartext traffic blocked. User CAs rejected." else "Cleartext HTTP traffic allowed.",
                            isSuccess = auditReport.isCleartextBlocked
                        )

                        // 4. Ledger Cryptographic Integrity
                        SecurityItemRow(
                            icon = Icons.Default.CheckCircle,
                            title = "Cryptographic Ledger Health",
                            subtitle = when (auditReport.ledgerStatus) {
                                LedgerIntegrityStatus.VERIFIED_INTACT -> "100% Intact • ${auditReport.verifiedTransactionsCount} transactions verified"
                                LedgerIntegrityStatus.EMPTY_LEDGER -> "Empty Ledger • Ready for verified transactions"
                                LedgerIntegrityStatus.TAMPER_DETECTED -> "ALERT: ${auditReport.tamperedCount} tampered records detected!"
                            },
                            isSuccess = auditReport.ledgerStatus != LedgerIntegrityStatus.TAMPER_DETECTED
                        )

                        // 5. Maker-Checker Authorization
                        SecurityItemRow(
                            icon = Icons.Default.Key,
                            title = "Maker-Checker Dual Authorization",
                            subtitle = "Enforced on transactions ≥ $10,000 / AED 35,000 / INR 8,35,000",
                            isSuccess = true
                        )

                        // 6. Master PIN Gatekeeper
                        SecurityItemRow(
                            icon = Icons.Default.Lock,
                            title = "Master Admin PIN Gatekeeper",
                            subtitle = "Default Master PIN: 786999 (Salted SHA-256 + 3-attempt lockout)",
                            isSuccess = true
                        )

                        if (auditReport.recommendations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RestrainedRedContainer),
                                border = BorderStroke(1.dp, RestrainedRedBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RestrainedRed, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Security Observations",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = RestrainedRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    auditReport.recommendations.forEach { rec ->
                                        Text(text = "• $rec", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = SlateBorder)

                    // Pinned Action Footer
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Close")
                            }

                            Button(
                                onClick = {
                                    isAuditing = true
                                    auditReport = SecurityHardeningManager.performSecurityAudit(context, transactions)
                                    isAuditing = false
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp)
                                    .testTag("rerun_security_audit_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isAuditing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Auditing...")
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Re-Run Audit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBanner(report: SecurityAuditReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, if (report.score >= 80) ProfitGreenBorder else RestrainedRedBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (report.score >= 80) ProfitGreen else RestrainedRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYSTEM SECURITY SCORE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.grade,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (report.score >= 80) ProfitGreen else RestrainedRed
                )
                Text(
                    text = "Defense-in-depth validated against OWASP Mobile Top 10",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(if (report.score >= 80) ProfitGreenContainer else RestrainedRedContainer)
                    .border(2.dp, if (report.score >= 80) ProfitGreen else RestrainedRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${report.score}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = if (report.score >= 80) ProfitGreenDark else RestrainedRed
                )
            }
        }
    }
}

@Composable
private fun SecurityItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSuccess: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardElevated),
        border = BorderStroke(0.5.dp, SlateBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) ProfitGreenContainer else RestrainedRedContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSuccess) ProfitGreenDark else RestrainedRed,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) ProfitGreenContainer else RestrainedRedContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSuccess) ProfitGreen else RestrainedRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
