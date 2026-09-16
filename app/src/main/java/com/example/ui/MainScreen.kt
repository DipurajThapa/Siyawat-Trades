package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Surface
import com.example.ui.components.DisputeDialog
import com.example.ui.components.FifoLotAuditDialog
import com.example.ui.components.ResolveDisputeDialog
import com.example.ui.components.SettlementDialog
import com.example.ui.theme.PaperBorder
import com.example.ui.theme.PaperCardElevated
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PoolUser
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.ui.components.AccessDeniedScreen
import com.example.ui.components.AdminMoneyDistributionDialog
import com.example.ui.components.AdminOperationsPanel
import com.example.ui.components.AssignRoleAndResponsibilitiesDialog
import com.example.ui.components.ExchangeRateDialog
import com.example.ui.components.GoogleDriveStorageDialog
import com.example.ui.components.NewTransactionDialog
import com.example.ui.components.ProofLightboxDialog
import com.example.ui.components.ReverseTransactionDialog
import com.example.ui.components.SimpleAddMoneyDialog
import com.example.ui.components.SimpleMemberDashboard
import com.example.ui.components.UserRoleSwitcherDialog
import com.example.ui.theme.MutedBlueBorder
import com.example.ui.theme.MutedBlueContainer
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PoolViewModel,
    modifier: Modifier = Modifier,
    isFullScreen: Boolean = true,
    onToggleFullScreen: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingCount = uiState.transactions.count {
        it.stage == TransactionStage.CAPITAL_INJECTION && it.status == TransactionStatus.PENDING_VERIFICATION
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    if (uiState.isAccessDenied) {
        AccessDeniedScreen(
            attemptedEmail = uiState.currentUser.email,
            onReturnToAdmin = { viewModel.returnToAdmin() },
            onOpenSwitcher = { viewModel.openUserSwitcher() }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = if (isFullScreen) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    TopAppBar(
                        windowInsets = if (isFullScreen) WindowInsets(0, 0, 0, 0) else TopAppBarDefaults.windowInsets,
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MutedBluePrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SIYAWAT TRADES",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {},
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Admin & Sub-Admin Navigation Tabs
                    if (uiState.currentUser.canManage) {
                        TabRow(
                            selectedTabIndex = if (uiState.selectedTab == AppTab.DASHBOARD) 0 else 1,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MutedBluePrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(
                                        tabPositions[if (uiState.selectedTab == AppTab.DASHBOARD) 0 else 1]
                                    ),
                                    color = MutedBluePrimary
                                )
                            }
                        ) {
                            Tab(
                                selected = uiState.selectedTab == AppTab.DASHBOARD,
                                onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                                text = {
                                    Text(
                                        text = "Simple Dashboard",
                                        fontWeight = if (uiState.selectedTab == AppTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                modifier = Modifier.testTag("tab_simple_dashboard")
                            )

                            Tab(
                                selected = uiState.selectedTab == AppTab.ADMIN_OPS,
                                onClick = { viewModel.selectTab(AppTab.ADMIN_OPS) },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Admin Management",
                                            fontWeight = if (uiState.selectedTab == AppTab.ADMIN_OPS) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        if (pendingCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF59E0B))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = pendingCount.toString(),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("tab_admin_operations")
                            )
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_bottom_bar"),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, PaperBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Role / Account switcher button with user chip
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MutedBlueContainer)
                                .border(1.dp, MutedBlueBorder, RoundedCornerShape(8.dp))
                                .clickable { viewModel.openUserSwitcher() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("manage_accounts_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Manage Roles & Accounts",
                                tint = MutedBlueDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = uiState.currentUser.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedBlueDark
                                )
                                Text(
                                    text = uiState.currentUser.role.name.replace("_", " "),
                                    fontSize = 9.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Right group: Cloud Backup & Fullscreen Toggle
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.openDriveDialog() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PaperCardElevated)
                                    .border(1.dp, PaperBorder, RoundedCornerShape(8.dp))
                                    .testTag("open_gdrive_dialog_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Google Drive Receipts",
                                    tint = MutedBlueDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (onToggleFullScreen != null) {
                                IconButton(
                                    onClick = { onToggleFullScreen(!isFullScreen) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PaperCardElevated)
                                        .border(1.dp, PaperBorder, RoundedCornerShape(8.dp))
                                        .testTag("toggle_fullscreen_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isFullScreen) "Exit Full Screen" else "Enter Full Screen",
                                        tint = MutedBlueDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
                // Extended FAB only for Admin Operations tab to prevent button collision on Member Dashboard
                if (uiState.selectedTab == AppTab.ADMIN_OPS && uiState.currentUser.canManage) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.openNewTransactionDialog() },
                        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "New Operation") },
                        text = {
                            Text(
                                text = "Log Operation",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        containerColor = MutedBluePrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_log_operation")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.selectedTab == AppTab.ADMIN_OPS && uiState.currentUser.canManage) {
                    // Admin & Sub-Admin Operations Control Center
                    AdminOperationsPanel(
                        currentUser = uiState.currentUser,
                        metrics = uiState.metrics,
                        transactions = uiState.filteredTransactions,
                        selectedFilter = uiState.selectedTimePeriodFilter,
                        periodSummary = uiState.periodSummary,
                        onSelectFilter = { filter ->
                            viewModel.selectTimePeriod(filter)
                        },
                        onConfirmBankReceipt = { txId ->
                            viewModel.confirmBankReceipt(txId)
                        },
                        onOpenOperation = { stage ->
                            viewModel.selectStageFilter(stage)
                            viewModel.openNewTransactionDialog()
                        },
                        onViewProofClick = { tx ->
                            viewModel.openProof(tx)
                        },
                        onOpenDistributeMoney = {
                            viewModel.openMoneyDistributionDialog()
                        },
                        onReverseTransaction = { tx ->
                            viewModel.openReverseDialog(tx)
                        },
                        onOpenFifoLotAudit = {
                            viewModel.openFifoLotAudit()
                        },
                        onResolveDispute = { tx ->
                            viewModel.openResolveDisputeDialog(tx)
                        },
                        onSecondApproval = { tx ->
                            viewModel.verifyTransactionMakerChecker(tx.id, true)
                        },
                        onRecordSettlement = { tx ->
                            viewModel.openSettleDialog(tx)
                        },
                        whitelistedUsers = uiState.whitelistedUsers,
                        onAssignRolesAndResponsibilities = { user ->
                            viewModel.openRoleAssignmentDialog(user)
                        }
                    )
                } else {
                    // Non-Technical, General User-Friendly Dashboard (Six Sigma Lean)
                    SimpleMemberDashboard(
                        periodSummary = uiState.periodSummary,
                        selectedPeriod = uiState.selectedTimePeriod,
                        selectedFilter = uiState.selectedTimePeriodFilter,
                        transactions = uiState.filteredTransactions,
                        currentUser = uiState.currentUser,
                        activeCurrency = uiState.selectedCurrency,
                        activeRate = uiState.currentRate,
                        onSelectCurrency = { viewModel.selectCurrency(it) },
                        onOpenRateDialog = { viewModel.openRateDialog() },
                        onSelectPeriod = { period ->
                            viewModel.selectTimePeriod(period)
                        },
                        onSelectFilter = { filter ->
                            viewModel.selectTimePeriod(filter)
                        },
                        onAddMoneyClick = {
                            viewModel.openSimpleAddMoneyDialog()
                        },
                        onDownloadSheetClick = {
                            viewModel.downloadGoogleSheet(context)
                        },
                        onViewProofClick = { tx ->
                            viewModel.openProof(tx)
                        },
                        onConfirmReceipt = { txId ->
                            viewModel.confirmUserReceipt(txId)
                        },
                        onDispute = { tx ->
                            viewModel.openDisputeDialog(tx)
                        }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    // 1. Simple Non-Technical Add Money Dialog for General Users
    if (uiState.showSimpleAddMoneyDialog) {
        SimpleAddMoneyDialog(
            currentUser = uiState.currentUser,
            activeCurrency = uiState.selectedCurrency,
            exchangeRates = uiState.exchangeRates,
            onDismiss = { viewModel.closeSimpleAddMoneyDialog() },
            onSubmit = { amount, ref, proof, notes, currency, rate ->
                viewModel.submitSimpleTransfer(
                    amountFiat = amount,
                    referenceNo = ref,
                    proofUri = proof,
                    notes = notes,
                    currency = currency,
                    exchangeRate = rate
                )
            },
            checkDuplicateTxId = { ref -> viewModel.checkDuplicateReference(ref) }
        )
    }

    // 2. Admin Detailed Transaction Dialog
    if (uiState.showNewTransactionDialog) {
        NewTransactionDialog(
            currentUser = uiState.currentUser,
            whitelistedUsers = uiState.whitelistedUsers,
            onDismiss = { viewModel.closeNewTransactionDialog() },
            onSubmit = { stage, fiat, usdt, rate, fee, recipient, ref, proof, desc, notes ->
                viewModel.submitTransaction(
                    stage = stage,
                    amountFiat = fiat,
                    amountUsdt = usdt,
                    exchangeRate = rate,
                    fee = fee,
                    recipientEmail = recipient,
                    referenceNo = ref,
                    proofUri = proof,
                    proofDescription = desc,
                    notes = notes
                )
            }
        )
    }

    // 3. Proof Lightbox Dialog
    if (uiState.selectedTransactionForProof != null) {
        ProofLightboxDialog(
            transaction = uiState.selectedTransactionForProof!!,
            onDismiss = { viewModel.closeProof() }
        )
    }

    // 4. User Role Switcher (Admin / Sub-Admin / Members)
    if (uiState.showUserSwitcherDialog) {
        UserRoleSwitcherDialog(
            currentUser = uiState.currentUser,
            whitelistedUsers = uiState.whitelistedUsers,
            onDismiss = { viewModel.closeUserSwitcher() },
            onSelectUser = { viewModel.switchUser(it) },
            onSimulateCustomLogin = { viewModel.simulateCustomEmailLogin(it) },
            onAddWhitelistedMember = { email, name -> viewModel.addWhitelistedMember(email, name) },
            onResetDemoData = { viewModel.resetDemoData() },
            onAssignRolesAndResponsibilities = { user ->
                viewModel.openRoleAssignmentDialog(user)
            }
        )
    }

    // 5. Google Drive Storage Overview Dialog
    if (uiState.showDriveDialog) {
        GoogleDriveStorageDialog(
            driveStatus = uiState.driveSyncStatus,
            transactions = uiState.transactions,
            onDismiss = { viewModel.closeDriveDialog() },
            onSyncAll = { viewModel.syncAllProofsToDrive() }
        )
    }

    // 6. Live Exchange Rate & Currency Dialog
    if (uiState.showRateDialog) {
        ExchangeRateDialog(
            selectedCurrency = uiState.selectedCurrency,
            exchangeRates = uiState.exchangeRates,
            onSelectCurrency = { viewModel.selectCurrency(it) },
            onUpdateRate = { curr, rate -> viewModel.updateExchangeRate(curr, rate) },
            onDismiss = { viewModel.closeRateDialog() }
        )
    }

    // 7. Money Distribution Dialog (Authorized Admin Atomic Distribution)
    if (uiState.showMoneyDistributionDialog) {
        AdminMoneyDistributionDialog(
            currentUser = uiState.currentUser,
            metrics = uiState.metrics,
            whitelistedUsers = uiState.whitelistedUsers,
            onDismiss = { viewModel.closeMoneyDistributionDialog() },
            onSubmit = { beneficiaryEmail, beneficiaryName, amount, isUsdt, fee, notes, proofUri ->
                viewModel.executeMoneyDistribution(
                    beneficiaryEmail = beneficiaryEmail,
                    beneficiaryName = beneficiaryName,
                    amount = amount,
                    isUsdt = isUsdt,
                    fee = fee,
                    notes = notes,
                    proofUri = proofUri
                )
            }
        )
    }

    // 8. Reverse Transaction Dialog (Reversibility & Audit Trail)
    if (uiState.transactionToReverse != null) {
        val txToReverse = uiState.transactionToReverse!!
        ReverseTransactionDialog(
            transaction = txToReverse,
            onDismiss = { viewModel.closeReverseDialog() },
            onConfirmReversal = { reason ->
                viewModel.reverseTransaction(txToReverse.id, reason)
            }
        )
    }

    // 9. Record Settlement Dialog
    if (uiState.transactionToSettle != null) {
        val txToSettle = uiState.transactionToSettle!!
        SettlementDialog(
            transaction = txToSettle,
            onDismiss = { viewModel.closeSettleDialog() },
            onConfirmSettlement = { bankUtr ->
                viewModel.recordSettlement(
                    txId = txToSettle.id,
                    bankUtr = bankUtr
                )
            }
        )
    }

    // 10. User Raise Dispute Dialog
    if (uiState.transactionToDispute != null) {
        val txToDispute = uiState.transactionToDispute!!
        DisputeDialog(
            transaction = txToDispute,
            onDismiss = { viewModel.closeDisputeDialog() },
            onSubmitDispute = { reason ->
                viewModel.raiseDispute(
                    txId = txToDispute.id,
                    reason = reason
                )
            }
        )
    }

    // 11. Admin Resolve Dispute Dialog
    if (uiState.transactionToResolveDispute != null) {
        val txToResolve = uiState.transactionToResolveDispute!!
        ResolveDisputeDialog(
            transaction = txToResolve,
            onDismiss = { viewModel.closeResolveDisputeDialog() },
            onResolve = { notes, isConfirmed ->
                viewModel.resolveDispute(
                    txId = txToResolve.id,
                    notes = notes,
                    isConfirmed = isConfirmed
                )
            }
        )
    }

    // 12. FIFO Lot Audit Dialog
    if (uiState.showFifoLotAuditDialog) {
        FifoLotAuditDialog(
            fifoResult = viewModel.getFifoLedgerResult(),
            onDismiss = { viewModel.closeFifoLotAudit() }
        )
    }

    // 13. Assign Roles & Responsibilities Dialog (Admin control)
    if (uiState.selectedUserForRoleAssignment != null) {
        val targetUser = uiState.selectedUserForRoleAssignment!!
        AssignRoleAndResponsibilitiesDialog(
            user = targetUser,
            onDismiss = { viewModel.closeRoleAssignmentDialog() },
            onSave = { newRole, responsibilities, designation ->
                viewModel.updateUserRoleAndResponsibilities(
                    email = targetUser.email,
                    newRole = newRole,
                    responsibilities = responsibilities,
                    customDesignation = designation
                )
            }
        )
    }
}

