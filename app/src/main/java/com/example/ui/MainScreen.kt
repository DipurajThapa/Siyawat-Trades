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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PoolUser
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.ui.components.AccessDeniedScreen
import com.example.ui.components.AdminMoneyDistributionDialog
import com.example.ui.components.AdminOperationsPanel
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
                        actions = {
                            if (onToggleFullScreen != null) {
                                IconButton(
                                    onClick = { onToggleFullScreen(!isFullScreen) },
                                    modifier = Modifier.testTag("toggle_fullscreen_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isFullScreen) "Exit Full Screen" else "Enter Full Screen",
                                        tint = MutedBlueDark
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.openDriveDialog() },
                                modifier = Modifier.testTag("open_gdrive_dialog_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Google Drive Receipts",
                                    tint = MutedBlueDark
                                )
                            }

                            IconButton(
                                onClick = { viewModel.openUserSwitcher() },
                                modifier = Modifier.testTag("manage_accounts_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = "Manage Roles & Accounts",
                                    tint = TextSecondary
                                )
                            }
                        },
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
                                TabRowDefaults.Indicator(
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
            onResetDemoData = { viewModel.resetDemoData() }
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
}

