package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PoolMetrics
import com.example.data.model.PoolUser
import com.example.data.model.UserRole
import com.example.ui.theme.MutedBlueBorder
import com.example.ui.theme.MutedBlueContainer
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.PaperBorder
import com.example.ui.theme.PaperCard
import com.example.ui.theme.PaperCardElevated
import com.example.ui.theme.RestrainedRed
import com.example.ui.theme.RestrainedRedBorder
import com.example.ui.theme.RestrainedRedContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DistributionAssetType(val label: String, val unit: String) {
    USDT("USDT Allocation", "USDT"),
    FIAT("Fiat Capital / Profit Payout", "USD")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMoneyDistributionDialog(
    currentUser: PoolUser,
    metrics: PoolMetrics,
    whitelistedUsers: List<PoolUser>,
    onDismiss: () -> Unit,
    onSubmit: (
        beneficiaryEmail: String,
        beneficiaryName: String,
        amount: Double,
        isUsdt: Boolean,
        fee: Double,
        notes: String,
        proofUri: String
    ) -> Unit
) {
    val memberUsers = remember(whitelistedUsers) {
        whitelistedUsers.filter { !it.isAdmin }
    }

    var selectedAssetType by remember { mutableStateOf(DistributionAssetType.USDT) }
    var isCustomBeneficiary by remember { mutableStateOf(memberUsers.isEmpty()) }
    var customBeneficiaryEmail by remember { mutableStateOf("") }
    var customBeneficiaryName by remember { mutableStateOf("") }
    var selectedUser by remember {
        mutableStateOf(memberUsers.firstOrNull() ?: PoolUser("", "Select Beneficiary", UserRole.MEMBER))
    }

    var userDropdownExpanded by remember { mutableStateOf(false) }

    var amountInput by remember { mutableStateOf("") }
    var feeInput by remember { mutableStateOf("0.0") }
    var notesInput by remember { mutableStateOf("") }
    var proofUriInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            proofUriInput = uri.toString()
        }
    }

    val availableBalance = if (selectedAssetType == DistributionAssetType.USDT) {
        metrics.unallocatedCentralUsdt
    } else {
        metrics.centralBankBalanceFiat
    }

    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
    val parsedFee = feeInput.toDoubleOrNull() ?: 0.0
    val totalDeduction = parsedAmount + (if (selectedAssetType == DistributionAssetType.USDT) parsedFee else 0.0)
    val resultingBalance = (availableBalance - totalDeduction).coerceAtLeast(0.0)

    val isOverdrawn = totalDeduction > availableBalance + 0.0001
    val isAmountValid = parsedAmount > 0.0 && !isOverdrawn

    val effectiveBeneficiaryEmail = if (isCustomBeneficiary) customBeneficiaryEmail.trim() else selectedUser.email
    val effectiveBeneficiaryName = if (isCustomBeneficiary) customBeneficiaryName.trim() else selectedUser.displayName
    val isBeneficiaryValid = effectiveBeneficiaryEmail.contains("@") && effectiveBeneficiaryEmail.contains(".")
    val isNotesValid = notesInput.trim().isNotEmpty()

    val isFormValid = currentUser.isAdmin && isAmountValid && isBeneficiaryValid && isNotesValid && !isSubmitting

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        if (!currentUser.isAdmin) {
            // Restricted Administrative Access View
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp)
                    .testTag("admin_money_distribution_restricted"),
                shape = RoundedCornerShape(20.dp),
                color = PaperCard,
                border = BorderStroke(1.dp, RestrainedRedBorder),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(RestrainedRedContainer)
                            .border(1.dp, RestrainedRedBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = RestrainedRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Restricted Administrative Action",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Treasury fund distribution and capital disbursements can only be initiated and committed by a verified pool administrator (${PoolUser.ADMIN_EMAIL}).",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaperCardElevated),
                        border = BorderStroke(1.dp, PaperBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current User", fontSize = 12.sp, color = TextSecondary)
                                Text(currentUser.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Assigned Role", fontSize = 12.sp, color = TextSecondary)
                                Text(currentUser.role.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RestrainedRed)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Authorization Status", fontSize = 12.sp, color = TextSecondary)
                                Text("ACCESS DENIED (403)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RestrainedRed)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_close_distribute_dialog"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MutedBluePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Return to Dashboard", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            // Authorized Admin Money Distribution Modal
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 20.dp)
                    .testTag("admin_money_distribution_dialog"),
                shape = RoundedCornerShape(20.dp),
                color = PaperCard,
                border = BorderStroke(1.dp, PaperBorder),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MutedBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = MutedBlueDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Distribute Funds",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Authorized Admin Distribution Ledger",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("btn_close_distribute_dialog")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Admin Authorization Clearance Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MutedBlueContainer)
                            .border(1.dp, MutedBlueBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MutedBlueDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Verified: ${currentUser.displayName} (${currentUser.email})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MutedBlueDark
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Asset Selector (USDT vs Fiat)
                    Text(
                        text = "SELECT ASSET TO DISTRIBUTE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DistributionAssetType.values().forEach { asset ->
                            val isSelected = selectedAssetType == asset
                            Surface(
                                onClick = {
                                    selectedAssetType = asset
                                    amountInput = ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("asset_selector_${asset.name.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MutedBluePrimary else PaperCardElevated,
                                border = BorderStroke(1.dp, if (isSelected) MutedBlueDark else PaperBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (asset == DistributionAssetType.USDT) Icons.Default.Token else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = asset.unit,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Available Balance & Live Resulting Balance Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PaperCardElevated),
                        border = BorderStroke(1.dp, if (isOverdrawn) RestrainedRedBorder else PaperBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Current Available Pool Balance",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = if (selectedAssetType == DistributionAssetType.USDT) {
                                        "${FormatUtils.formatInteger(availableBalance)} USDT"
                                    } else {
                                        FormatUtils.formatCurrencyZeroDecimal(availableBalance, metrics.currency)
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Outflow (Amount + Fee)",
                                    fontSize = 12.sp,
                                    color = if (isOverdrawn) RestrainedRed else TextSecondary
                                )
                                Text(
                                    text = if (selectedAssetType == DistributionAssetType.USDT) {
                                        "${String.format(Locale.US, "%,.2f", totalDeduction)} USDT"
                                    } else {
                                        "${metrics.currency.symbol}${String.format(Locale.US, "%,.2f", totalDeduction)}"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverdrawn) RestrainedRed else TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Resulting Pool Balance",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isOverdrawn) RestrainedRed else MutedBlueDark
                                )
                                Text(
                                    text = if (selectedAssetType == DistributionAssetType.USDT) {
                                        "${String.format(Locale.US, "%,.2f", resultingBalance)} USDT"
                                    } else {
                                        "${metrics.currency.symbol}${String.format(Locale.US, "%,.2f", resultingBalance)}"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOverdrawn) RestrainedRed else MutedBlueDark
                                )
                            }

                            if (isOverdrawn) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RestrainedRedContainer)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = RestrainedRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "OVERDRAW PREVENTED: Total outflow exceeds available pool balance.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RestrainedRed
                                    )
                                }
                            }
                        }
                    }

                    // Quick percentage fill chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "Max").forEach { (ratio, label) ->
                            OutlinedButton(
                                onClick = {
                                    val calculated = if (ratio == 1.0) {
                                        if (selectedAssetType == DistributionAssetType.USDT) {
                                            (availableBalance - parsedFee).coerceAtLeast(0.0)
                                        } else {
                                            availableBalance.coerceAtLeast(0.0)
                                        }
                                    } else {
                                        (availableBalance * ratio).coerceAtLeast(0.0)
                                    }
                                    amountInput = if (calculated > 0.0) String.format(Locale.US, "%.2f", calculated) else ""
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MutedBlueBorder)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedBlueDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Beneficiary Selection
                    Text(
                        text = "BENEFICIARY MEMBER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isCustomBeneficiary) {
                        ExposedDropdownMenuBox(
                            expanded = userDropdownExpanded,
                            onExpandedChange = { userDropdownExpanded = !userDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "${selectedUser.displayName} (${selectedUser.email})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Pool Member") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = userDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("dropdown_beneficiary_user"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = userDropdownExpanded,
                                onDismissRequest = { userDropdownExpanded = false }
                            ) {
                                memberUsers.forEach { user ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(user.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(user.email, fontSize = 11.sp, color = TextSecondary)
                                            }
                                        },
                                        onClick = {
                                            selectedUser = user
                                            userDropdownExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customBeneficiaryName,
                                onValueChange = { customBeneficiaryName = it },
                                label = { Text("Beneficiary Full Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_beneficiary_name"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = customBeneficiaryEmail,
                                onValueChange = { customBeneficiaryEmail = it },
                                label = { Text("Beneficiary Email Address") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_beneficiary_email"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable { isCustomBeneficiary = !isCustomBeneficiary },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCustomBeneficiary) "← Select from Whitelisted Members" else "+ Enter Custom Beneficiary",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MutedBlueDark
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Amount and Fee Inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Amount (${selectedAssetType.unit})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("input_distribution_amount"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isOverdrawn) RestrainedRed else MutedBlueDark,
                                unfocusedBorderColor = if (isOverdrawn) RestrainedRed else PaperBorder
                            )
                        )

                        if (selectedAssetType == DistributionAssetType.USDT) {
                            OutlinedTextField(
                                value = feeInput,
                                onValueChange = { feeInput = it },
                                label = { Text("Network Fee (USDT)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_distribution_fee"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notes / Reason (Mandatory for Audit Trail)
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Distribution Reason (Mandatory for Audit Trail)") },
                        placeholder = { Text("e.g., Weekly trading capital tranche allocation") },
                        supportingText = {
                            if (notesInput.isBlank()) {
                                Text("Reason is mandatory to generate cryptographic audit record", color = TextSecondary, fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_distribution_notes"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Audit Record Preview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MutedBlueContainer.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MutedBlueBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "IMMUTABLE AUDIT RECORD PREVIEW",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = MutedBlueDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Authorized Disburser", fontSize = 11.sp, color = TextSecondary)
                                Text("${currentUser.displayName} (${currentUser.email})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Beneficiary", fontSize = 11.sp, color = TextSecondary)
                                Text("$effectiveBeneficiaryName ($effectiveBeneficiaryEmail)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Resulting Balance Stamped", fontSize = 11.sp, color = TextSecondary)
                                Text(
                                    if (selectedAssetType == DistributionAssetType.USDT) {
                                        "${String.format(Locale.US, "%,.2f", resultingBalance)} USDT"
                                    } else {
                                        "${metrics.currency.symbol}${String.format(Locale.US, "%,.2f", resultingBalance)}"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedBlueDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Proof / Transfer Screenshot
                    Text(
                        text = "DISTRIBUTION PROOF / VOUCHER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (proofUriInput.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MutedBlueBorder, RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(proofUriInput)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Transfer Proof",
                                modifier = Modifier.fillMaxWidth()
                            )
                            IconButton(
                                onClick = { proofUriInput = "" },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = Color.White)
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_pick_distribution_proof"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, PaperBorder)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Attach Transfer Slip / Voucher (Optional)", fontSize = 12.sp)
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
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_cancel_distribution"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (isFormValid) {
                                    isSubmitting = true
                                    onSubmit(
                                        effectiveBeneficiaryEmail,
                                        effectiveBeneficiaryName,
                                        parsedAmount,
                                        selectedAssetType == DistributionAssetType.USDT,
                                        parsedFee,
                                        notesInput.trim(),
                                        proofUriInput
                                    )
                                }
                            },
                            enabled = isFormValid,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .testTag("btn_confirm_distribution"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutedBluePrimary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Confirm Distribution", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
