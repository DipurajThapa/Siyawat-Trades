package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PoolDatabase
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.AppCurrency
import com.example.data.model.ComplianceAlert
import com.example.data.model.FifoCalculationResult
import com.example.data.model.PeriodSummary
import com.example.data.model.PoolMetrics
import com.example.data.model.PoolUser
import com.example.data.model.RecordState
import com.example.data.model.ReconciliationState
import com.example.data.model.SettlementState
import com.example.data.model.TimePeriod
import com.example.data.model.TimePeriodFilter
import com.example.data.model.TimePeriodType
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.data.model.UserResponsibility
import com.example.data.model.UserRole
import com.example.data.remote.DriveSyncStatus
import com.example.data.remote.GoogleDriveService
import com.example.data.repository.PoolRepository
import com.example.util.FormatUtils
import com.example.util.GoogleSheetExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    DASHBOARD,
    ADMIN_OPS
}

data class PoolUiState(
    val transactions: List<PoolTransactionEntity> = emptyList(),
    val filteredTransactions: List<PoolTransactionEntity> = emptyList(),
    val metrics: PoolMetrics = PoolMetrics(),
    val periodSummary: PeriodSummary = PeriodSummary(),
    val selectedTimePeriod: TimePeriod = TimePeriod.ALL_TIME,
    val selectedTimePeriodFilter: TimePeriodFilter = TimePeriodFilter(TimePeriodType.ALL_TIME),
    val selectedTab: AppTab = AppTab.DASHBOARD,
    val complianceAlerts: List<ComplianceAlert> = emptyList(),
    val currentUser: PoolUser = PoolUser.DEFAULT_USERS.first(),
    val whitelistedUsers: List<PoolUser> = PoolUser.DEFAULT_USERS,
    val selectedStageFilter: TransactionStage? = null,
    val searchQuery: String = "",
    val selectedTransactionForProof: PoolTransactionEntity? = null,
    val showSimpleAddMoneyDialog: Boolean = false,
    val showNewTransactionDialog: Boolean = false,
    val showUserSwitcherDialog: Boolean = false,
    val showDriveDialog: Boolean = false,
    val isAccessDenied: Boolean = false,
    val driveSyncStatus: DriveSyncStatus = DriveSyncStatus(),
    val toastMessage: String? = null,
    // Multi-Currency State
    val selectedCurrency: AppCurrency = AppCurrency.INR,
    val exchangeRates: Map<AppCurrency, Double> = mapOf(
        AppCurrency.INR to 83.50,
        AppCurrency.AED to 3.6725,
        AppCurrency.USD to 1.0
    ),
    val showRateDialog: Boolean = false,
    val showMoneyDistributionDialog: Boolean = false,
    val transactionToReverse: PoolTransactionEntity? = null,
    // FIFO Engine & 3-Tier Lifecycle State
    val fifoCalculationResult: FifoCalculationResult = FifoCalculationResult(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
    val transactionToSettle: PoolTransactionEntity? = null,
    val transactionToDispute: PoolTransactionEntity? = null,
    val transactionToResolveDispute: PoolTransactionEntity? = null,
    val showFifoLotAuditDialog: Boolean = false,
    val selectedUserForRoleAssignment: PoolUser? = null
) {
    val currentRate: Double
        get() = exchangeRates[selectedCurrency] ?: selectedCurrency.defaultRatePerUsd
}

class PoolViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PoolDatabase.getDatabase(application)
    private val repository = PoolRepository(application, database)
    private val driveService = GoogleDriveService(application)

    private val _currentUser = MutableStateFlow(PoolUser.DEFAULT_USERS.first())
    private val _whitelistedUsers = MutableStateFlow(PoolUser.DEFAULT_USERS)
    private val _selectedTimePeriodFilter = MutableStateFlow(TimePeriodFilter(TimePeriodType.ALL_TIME))
    private val _selectedTab = MutableStateFlow(AppTab.DASHBOARD)
    private val _selectedStageFilter = MutableStateFlow<TransactionStage?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTransactionForProof = MutableStateFlow<PoolTransactionEntity?>(null)
    private val _showSimpleAddMoneyDialog = MutableStateFlow(false)
    private val _showNewTransactionDialog = MutableStateFlow(false)
    private val _showUserSwitcherDialog = MutableStateFlow(false)
    private val _showDriveDialog = MutableStateFlow(false)
    private val _isAccessDenied = MutableStateFlow(false)
    private val _driveSyncStatus = MutableStateFlow(DriveSyncStatus())
    private val _toastMessage = MutableStateFlow<String?>(null)

    // Multi-currency StateFlows
    private val _selectedCurrency = MutableStateFlow(AppCurrency.INR)
    private val _exchangeRates = MutableStateFlow(
        mapOf(
            AppCurrency.INR to 83.50,
            AppCurrency.AED to 3.6725,
            AppCurrency.USD to 1.0
        )
    )
    private val _showRateDialog = MutableStateFlow(false)
    private val _showMoneyDistributionDialog = MutableStateFlow(false)
    private val _transactionToReverse = MutableStateFlow<PoolTransactionEntity?>(null)
    private val _transactionToSettle = MutableStateFlow<PoolTransactionEntity?>(null)
    private val _transactionToDispute = MutableStateFlow<PoolTransactionEntity?>(null)
    private val _transactionToResolveDispute = MutableStateFlow<PoolTransactionEntity?>(null)
    private val _showFifoLotAuditDialog = MutableStateFlow(false)
    private val _selectedUserForRoleAssignment = MutableStateFlow<PoolUser?>(null)

    val uiState: StateFlow<PoolUiState> = combine(
        repository.allTransactions,
        _currentUser,
        _whitelistedUsers,
        _selectedTimePeriodFilter,
        _selectedTab,
        _selectedStageFilter,
        _searchQuery,
        _selectedTransactionForProof,
        _showSimpleAddMoneyDialog,
        _showNewTransactionDialog,
        _showUserSwitcherDialog,
        _showDriveDialog,
        _isAccessDenied,
        _driveSyncStatus,
        _toastMessage,
        _selectedCurrency,
        _exchangeRates,
        _showRateDialog,
        _showMoneyDistributionDialog,
        _transactionToReverse,
        _transactionToSettle,
        _transactionToDispute,
        _transactionToResolveDispute,
        _showFifoLotAuditDialog,
        _selectedUserForRoleAssignment
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val rawTransactions = args[0] as List<PoolTransactionEntity>
        val currentUser = args[1] as PoolUser
        @Suppress("UNCHECKED_CAST")
        val whitelist = args[2] as List<PoolUser>
        val timePeriodFilter = args[3] as TimePeriodFilter
        val selectedTab = args[4] as AppTab
        val stageFilter = args[5] as TransactionStage?
        val query = (args[6] as String).trim().lowercase()
        val proofTx = args[7] as PoolTransactionEntity?
        val showSimpleAdd = args[8] as Boolean
        val showNewTx = args[9] as Boolean
        val showUserSwitch = args[10] as Boolean
        val showDrive = args[11] as Boolean
        val accessDenied = args[12] as Boolean
        val driveStatus = args[13] as DriveSyncStatus
        val toast = args[14] as String?
        val selectedCurrency = args[15] as AppCurrency
        @Suppress("UNCHECKED_CAST")
        val exchangeRates = args[16] as Map<AppCurrency, Double>
        val showRateDialog = args[17] as Boolean
        val showMoneyDistribution = args[18] as Boolean
        val txToReverse = args[19] as PoolTransactionEntity?
        val txToSettle = args[20] as PoolTransactionEntity?
        val txToDispute = args[21] as PoolTransactionEntity?
        val txToResolveDispute = args[22] as PoolTransactionEntity?
        val showFifoAudit = args[23] as Boolean
        val selectedUserForRole = args[24] as PoolUser?

        val currentRate = exchangeRates[selectedCurrency] ?: selectedCurrency.defaultRatePerUsd
        val isManager = currentUser.canManage

        // 1. Role-based data isolation:
        // Admin, Super Admin, and Sub-admin (currentUser.canManage) can see all pool records, total collections, and USDT details.
        // Regular members (MEMBER) can ONLY see transactions they initiated or received.
        val userVisibleRawTransactions = if (isManager) {
            rawTransactions
        } else {
            rawTransactions.filter { tx ->
                tx.userEmail.equals(currentUser.email, ignoreCase = true) ||
                (tx.recipientEmail != null && tx.recipientEmail.equals(currentUser.email, ignoreCase = true))
            }
        }

        // 2. Time period filtering and range boundary resolution
        val now = System.currentTimeMillis()
        val bounds = timePeriodFilter.resolveRange(now)
        val periodTransactions = if (bounds.startTime <= 0L && bounds.endTime >= Long.MAX_VALUE - 1000L) {
            rawTransactions
        } else {
            rawTransactions.filter { it.timestamp in bounds.startTime..bounds.endTime }
        }
        val cumulativeTransactions = if (bounds.endTime >= Long.MAX_VALUE - 1000L) {
            rawTransactions
        } else {
            rawTransactions.filter { it.timestamp <= bounds.endTime }
        }

        // Transactions visible to current user in this time period
        val userVisiblePeriodTransactions = if (isManager) {
            periodTransactions
        } else {
            if (bounds.startTime <= 0L && bounds.endTime >= Long.MAX_VALUE - 1000L) {
                userVisibleRawTransactions
            } else {
                userVisibleRawTransactions.filter { it.timestamp in bounds.startTime..bounds.endTime }
            }
        }
        val userVisibleCumulativeTransactions = if (isManager) {
            cumulativeTransactions
        } else {
            if (bounds.endTime >= Long.MAX_VALUE - 1000L) {
                userVisibleRawTransactions
            } else {
                userVisibleRawTransactions.filter { it.timestamp <= bounds.endTime }
            }
        }

        // 3. Calculate live metrics:
        // For managers, calculate full pool metrics.
        // For regular members, calculate metrics strictly scoped to their own transactions with USDT inventory redacted to 0.0.
        val rawMetrics = repository.calculateMetrics(
            transactions = if (isManager) periodTransactions else userVisiblePeriodTransactions,
            cumulativeTransactions = if (isManager) cumulativeTransactions else userVisibleCumulativeTransactions,
            displayCurrency = selectedCurrency,
            ratePerUsd = currentRate
        )
        val metrics = if (isManager) {
            rawMetrics
        } else {
            rawMetrics.copy(
                remainingPoolUsdt = 0.0,
                unallocatedCentralUsdt = 0.0,
                totalUsdtPurchased = 0.0,
                totalUsdtDistributed = 0.0,
                totalDistributionFeesUsdt = 0.0,
                totalTradingFeesUsdt = 0.0,
                centralBankBalanceFiat = 0.0
            )
        }
        val alerts = if (isManager) repository.evaluateComplianceAlerts(rawTransactions, metrics) else emptyList()
        val fifoResult = repository.calculateFifoLedger(rawTransactions)

        // 4. Compute period statistics converted to active display currency
        var moneySpent = 0L
        var pendingSpent = 0L
        var moneyEarnedBack = 0L

        for (tx in userVisiblePeriodTransactions) {
            if (tx.status == TransactionStatus.CANCELLED || tx.status == TransactionStatus.REVERSED) {
                continue
            }
            val lockedUsd = if (tx.convertedAmountUsd > 0.0) tx.convertedAmountUsd else (tx.amountFiat ?: 0.0)
            val displayAmt = (lockedUsd * currentRate).toLong()

            when (tx.stage) {
                TransactionStage.CAPITAL_INJECTION -> {
                    if (tx.status == TransactionStatus.VERIFIED || tx.status == TransactionStatus.COMPLETED) {
                        moneySpent += displayAmt
                    } else if (tx.status == TransactionStatus.PENDING_VERIFICATION) {
                        pendingSpent += displayAmt
                    }
                }
                TransactionStage.LIQUIDATION -> {
                    moneyEarnedBack += displayAmt
                }
                TransactionStage.USDT_DISTRIBUTION -> {
                    if (!isManager && tx.recipientEmail != null && tx.recipientEmail.equals(currentUser.email, ignoreCase = true)) {
                        moneyEarnedBack += displayAmt
                    }
                }
                else -> Unit
            }
        }

        // For managers: net realized profit/loss and remaining USDT of the pool.
        // For regular members: their personal net return (moneyEarnedBack - moneySpent) and 0 USDT remaining (hidden in UI).
        val profitLoss = if (isManager) metrics.netRealizedProfitLossFiat.toLong() else (moneyEarnedBack - moneySpent)
        val usdtRemaining = if (isManager) metrics.remainingPoolUsdt.toLong() else 0L

        val periodSummary = PeriodSummary(
            period = timePeriodFilter.toTimePeriod(),
            filter = timePeriodFilter,
            periodLabel = bounds.label,
            dateRangeText = bounds.formattedDateRange(),
            startMillis = bounds.startTime,
            endMillis = bounds.endTime,
            moneySpent = moneySpent,
            pendingSpent = pendingSpent,
            moneyEarnedBack = moneyEarnedBack,
            usdtRemaining = usdtRemaining,
            totalTransactionsCount = userVisiblePeriodTransactions.size,
            profitLoss = profitLoss,
            currency = selectedCurrency
        )

        // 5. Filter transactions for display
        val filtered = userVisiblePeriodTransactions.filter { tx ->
            val matchesStage = stageFilter == null || tx.stage == stageFilter
            val matchesQuery = query.isEmpty() ||
                    tx.userName.lowercase().contains(query) ||
                    tx.userEmail.lowercase().contains(query) ||
                    tx.referenceNo.lowercase().contains(query) ||
                    (tx.recipientEmail?.lowercase()?.contains(query) ?: false) ||
                    tx.notes.lowercase().contains(query) ||
                    tx.originalCurrency.lowercase().contains(query)
            matchesStage && matchesQuery
        }

        PoolUiState(
            transactions = userVisibleRawTransactions,
            filteredTransactions = filtered,
            metrics = metrics,
            periodSummary = periodSummary,
            selectedTimePeriod = timePeriodFilter.toTimePeriod(),
            selectedTimePeriodFilter = timePeriodFilter,
            selectedTab = selectedTab,
            complianceAlerts = alerts,
            currentUser = currentUser,
            whitelistedUsers = whitelist,
            selectedStageFilter = stageFilter,
            searchQuery = query,
            selectedTransactionForProof = proofTx,
            showSimpleAddMoneyDialog = showSimpleAdd,
            showNewTransactionDialog = showNewTx,
            showUserSwitcherDialog = showUserSwitch,
            showDriveDialog = showDrive,
            isAccessDenied = accessDenied,
            driveSyncStatus = driveStatus,
            toastMessage = toast,
            selectedCurrency = selectedCurrency,
            exchangeRates = exchangeRates,
            showRateDialog = showRateDialog,
            showMoneyDistributionDialog = showMoneyDistribution,
            transactionToReverse = txToReverse,
            fifoCalculationResult = fifoResult,
            transactionToSettle = txToSettle,
            transactionToDispute = txToDispute,
            transactionToResolveDispute = txToResolveDispute,
            showFifoLotAuditDialog = showFifoAudit,
            selectedUserForRoleAssignment = selectedUserForRole
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PoolUiState()
    )

    init {
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            repository.autoReconcileTimeouts()
        }
    }

    // Currency Switcher Actions
    fun selectCurrency(currency: AppCurrency) {
        _selectedCurrency.value = currency
        val rate = _exchangeRates.value[currency] ?: currency.defaultRatePerUsd
        _toastMessage.value = "Switched display currency to ${currency.label}. Live rate: 1 USD = $rate ${currency.code}"
    }

    fun updateExchangeRate(currency: AppCurrency, rate: Double) {
        if (rate <= 0) return
        val updated = _exchangeRates.value.toMutableMap()
        updated[currency] = rate
        _exchangeRates.value = updated
        _toastMessage.value = "Updated rate for ${currency.code}: 1 USD = $rate ${currency.code}"
    }

    fun openRateDialog() {
        _showRateDialog.value = true
    }

    fun closeRateDialog() {
        _showRateDialog.value = false
    }

    fun selectStageFilter(stage: TransactionStage?) {
        _selectedStageFilter.value = stage
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTimePeriod(period: TimePeriod) {
        _selectedTimePeriodFilter.value = period.toFilter()
    }

    fun selectTimePeriod(filter: TimePeriodFilter) {
        _selectedTimePeriodFilter.value = filter
    }

    fun setSpecificDate(dateMillis: Long) {
        _selectedTimePeriodFilter.value = TimePeriodFilter(
            type = TimePeriodType.DAILY,
            specificDateMillis = dateMillis
        )
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        _selectedTimePeriodFilter.value = TimePeriodFilter(
            type = TimePeriodType.CUSTOM_RANGE,
            customStartMillis = startMillis,
            customEndMillis = endMillis
        )
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun openSimpleAddMoneyDialog() {
        _showSimpleAddMoneyDialog.value = true
    }

    fun closeSimpleAddMoneyDialog() {
        _showSimpleAddMoneyDialog.value = false
    }

    fun submitSimpleTransfer(
        amountFiat: Double,
        referenceNo: String,
        proofUri: String,
        notes: String,
        currency: AppCurrency = _selectedCurrency.value,
        exchangeRate: Double? = null
    ): Boolean {
        val user = _currentUser.value
        if (amountFiat <= 0) {
            _toastMessage.value = "Please enter an amount greater than 0."
            return false
        }
        if (referenceNo.isBlank()) {
            _toastMessage.value = "Bank reference / wire slip number is required."
            return false
        }
        if (proofUri.isBlank()) {
            _toastMessage.value = "Please attach or generate a verified transfer slip."
            return false
        }

        viewModelScope.launch {
            val rate = exchangeRate ?: (_exchangeRates.value[currency] ?: currency.defaultRatePerUsd)
            val lockedUsd = if (currency == AppCurrency.USD) amountFiat else (amountFiat / rate)

            val newEntity = PoolTransactionEntity(
                stage = TransactionStage.CAPITAL_INJECTION,
                userEmail = user.email,
                userName = user.name,
                amountFiat = amountFiat,
                fiatCurrency = currency.code,
                originalAmount = amountFiat,
                originalCurrency = currency.code,
                displayCurrencyAtTime = _selectedCurrency.value.code,
                fiatExchangeRate = rate,
                convertedAmountUsd = lockedUsd,
                rateTimestamp = System.currentTimeMillis(),
                referenceNo = referenceNo.trim(),
                proofUri = proofUri,
                proofDescription = "Member Bank Transfer Slip (${currency.code})",
                status = if (user.canManage) TransactionStatus.VERIFIED else TransactionStatus.PENDING_VERIFICATION,
                verifiedByEmail = if (user.canManage) user.email else null,
                notes = notes.ifBlank { "Member bank transfer submitted in ${currency.code}" },
                timestamp = System.currentTimeMillis()
            )

            val insertedId = repository.insertTransaction(newEntity)
            _showSimpleAddMoneyDialog.value = false
            _toastMessage.value = "Transfer of ${FormatUtils.formatCurrencyZeroDecimal(amountFiat, currency)} submitted! (Locked at $rate ${currency.code}/USD = $${String.format(java.util.Locale.US, "%,.2f", lockedUsd)} USD)"

            // Auto-sync proof to Google Drive folder
            try {
                val driveResult = driveService.uploadProofReceipt(
                    localFilePath = proofUri,
                    stageName = TransactionStage.CAPITAL_INJECTION.name,
                    referenceNo = referenceNo.trim(),
                    userEmail = user.email
                )
                repository.updateDriveInfo(insertedId, driveResult.fileId, driveResult.webViewLink)
                _driveSyncStatus.value = _driveSyncStatus.value.copy(
                    lastUploadedFile = "Uploaded to ${driveResult.folderName}",
                    lastUploadLink = driveResult.webViewLink
                )
            } catch (e: Exception) {
                // Keep local proof active
            }
        }
        return true
    }

    fun confirmBankReceipt(txId: Long) {
        val user = _currentUser.value
        if (!user.canManage) {
            _toastMessage.value = "Only administrators and sub-admins can confirm bank receipts."
            return
        }

        viewModelScope.launch {
            repository.verifyTransaction(txId, user.email, verified = true)
            _toastMessage.value = "Bank transfer confirmed & credited to verified pool balance!"
        }
    }

    fun downloadGoogleSheet(context: android.content.Context) {
        val state = uiState.value
        val periodTxs = state.filteredTransactions
        val activeRate = state.currentRate
        try {
            val file = GoogleSheetExporter.exportToGoogleSheet(
                context = context,
                transactions = periodTxs,
                timePeriod = state.selectedTimePeriod,
                summary = state.periodSummary,
                activeRatePerUsd = activeRate,
                currentUser = state.currentUser
            )

            viewModelScope.launch {
                try {
                    driveService.uploadGoogleSheet(file, state.periodSummary.periodLabel)
                } catch (_: Exception) {}
            }

            GoogleSheetExporter.shareExportedSheet(
                context = context,
                file = file,
                periodLabel = state.periodSummary.periodLabel
            )
            _toastMessage.value = "Generated Google Sheet for ${state.periodSummary.periodLabel} (${state.selectedCurrency.code})."
        } catch (e: Exception) {
            _toastMessage.value = "Could not export sheet: ${e.message}"
        }
    }

    fun openProof(tx: PoolTransactionEntity) {
        _selectedTransactionForProof.value = tx
    }

    fun closeProof() {
        _selectedTransactionForProof.value = null
    }

    fun openNewTransactionDialog() {
        _showNewTransactionDialog.value = true
    }

    fun closeNewTransactionDialog() {
        _showNewTransactionDialog.value = false
    }

    fun openUserSwitcher() {
        _showUserSwitcherDialog.value = true
    }

    fun closeUserSwitcher() {
        _showUserSwitcherDialog.value = false
    }

    fun openDriveDialog() {
        _showDriveDialog.value = true
    }

    fun closeDriveDialog() {
        _showDriveDialog.value = false
    }

    fun syncAllProofsToDrive() {
        viewModelScope.launch {
            _driveSyncStatus.value = _driveSyncStatus.value.copy(isUploading = true, error = null)
            try {
                val transactions = uiState.value.transactions
                var syncedCount = 0
                for (tx in transactions) {
                    if (tx.proofUri.isNotBlank()) {
                        val result = driveService.uploadProofReceipt(
                            localFilePath = tx.proofUri,
                            stageName = tx.stage.name,
                            referenceNo = tx.referenceNo,
                            userEmail = tx.userEmail
                        )
                        repository.updateDriveInfo(tx.id, result.fileId, result.webViewLink)
                        syncedCount++
                    }
                }
                _driveSyncStatus.value = _driveSyncStatus.value.copy(
                    isUploading = false,
                    lastUploadedFile = "Synced $syncedCount receipts to Google Drive",
                    lastUploadLink = driveService.getDriveFolderUrl()
                )
                _toastMessage.value = "Successfully synced $syncedCount receipts to Google Drive folder 'USDT_Pool_Receipts'."
            } catch (e: Exception) {
                _driveSyncStatus.value = _driveSyncStatus.value.copy(
                    isUploading = false,
                    error = e.message ?: "Failed to sync to Drive"
                )
                _toastMessage.value = "Drive sync notice: Local copy preserved."
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun switchUser(user: PoolUser) {
        val isAllowed = _whitelistedUsers.value.any { it.email.equals(user.email, ignoreCase = true) }
        if (!isAllowed) {
            _isAccessDenied.value = true
            _currentUser.value = user.copy(isWhitelisted = false)
            _toastMessage.value = "Access Denied: ${user.email} is not on the pool whitelist."
        } else {
            _isAccessDenied.value = false
            _currentUser.value = user
            _toastMessage.value = "Switched active account to ${user.email} (${user.role.name})"
        }
        _showUserSwitcherDialog.value = false
    }

    fun simulateCustomEmailLogin(email: String) {
        val trimmed = email.trim()
        val existing = _whitelistedUsers.value.firstOrNull { it.email.equals(trimmed, ignoreCase = true) }
        if (existing != null) {
            _isAccessDenied.value = false
            _currentUser.value = existing
            _toastMessage.value = "Welcome back, ${existing.name} (${existing.role.name})"
        } else {
            _isAccessDenied.value = true
            _currentUser.value = PoolUser(
                email = trimmed,
                name = trimmed.substringBefore("@").replace(".", " ").replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                },
                role = UserRole.MEMBER,
                isWhitelisted = false,
                avatarColorHex = 0xFFEF4444
            )
            _toastMessage.value = "Compliance Error: $trimmed not whitelisted!"
        }
        _showUserSwitcherDialog.value = false
    }

    fun returnToAdmin() {
        val admin = _whitelistedUsers.value.firstOrNull { it.email == PoolUser.ADMIN_EMAIL }
            ?: PoolUser.DEFAULT_USERS.first()
        _currentUser.value = admin
        _isAccessDenied.value = false
        _toastMessage.value = "Restored session as Designated Central Administrator."
    }

    fun openRoleAssignmentDialog(user: PoolUser) {
        _selectedUserForRoleAssignment.value = user
    }

    fun closeRoleAssignmentDialog() {
        _selectedUserForRoleAssignment.value = null
    }

    fun updateUserRoleAndResponsibilities(
        email: String,
        newRole: UserRole,
        responsibilities: Set<UserResponsibility>,
        customDesignation: String
    ) {
        val currentAdmin = _currentUser.value
        if (!currentAdmin.isAdmin && !currentAdmin.canManageMembers) {
            _toastMessage.value = "Unauthorized: Only administrators can assign roles & responsibilities."
            return
        }

        val updatedList = _whitelistedUsers.value.map { user ->
            if (user.email.equals(email, ignoreCase = true)) {
                user.copy(
                    role = newRole,
                    responsibilities = responsibilities,
                    customDesignation = customDesignation.trim()
                )
            } else {
                user
            }
        }
        _whitelistedUsers.value = updatedList

        // If currently logged-in user is this user, update active user session in real time
        if (_currentUser.value.email.equals(email, ignoreCase = true)) {
            val updatedActiveUser = updatedList.firstOrNull { it.email.equals(email, ignoreCase = true) }
            if (updatedActiveUser != null) {
                _currentUser.value = updatedActiveUser
            }
        }

        _selectedUserForRoleAssignment.value = null
        _toastMessage.value = "Assigned ${newRole.label} role with ${responsibilities.size} duties to $email."
    }

    fun addWhitelistedMember(
        email: String,
        name: String,
        role: UserRole = UserRole.MEMBER, // By default, it is the user!
        responsibilities: Set<UserResponsibility> = UserResponsibility.defaultFor(UserRole.MEMBER),
        customDesignation: String = ""
    ) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || _whitelistedUsers.value.any { it.email.equals(trimmedEmail, ignoreCase = true) }) {
            return
        }
        val newUser = PoolUser(
            email = trimmedEmail,
            name = name.ifBlank { trimmedEmail.substringBefore("@") },
            role = role, // Defaults to UserRole.MEMBER (User)
            isWhitelisted = true,
            avatarColorHex = 0xFF10B981,
            responsibilities = responsibilities,
            customDesignation = customDesignation
        )
        _whitelistedUsers.value = _whitelistedUsers.value + newUser
        _toastMessage.value = "Added $trimmedEmail to approved pool as ${role.label}."
    }

    fun removeWhitelistedMember(email: String) {
        if (email == PoolUser.ADMIN_EMAIL) {
            _toastMessage.value = "Cannot remove designated central administrator."
            return
        }
        _whitelistedUsers.value = _whitelistedUsers.value.filterNot { it.email.equals(email, ignoreCase = true) }
        if (_currentUser.value.email.equals(email, ignoreCase = true)) {
            _isAccessDenied.value = true
        }
        _toastMessage.value = "Revoked whitelist access for $email."
    }

    fun submitTransaction(
        stage: TransactionStage,
        amountFiat: Double?,
        amountUsdt: Double?,
        exchangeRate: Double?,
        fee: Double?,
        recipientEmail: String?,
        referenceNo: String,
        proofUri: String,
        proofDescription: String?,
        notes: String,
        fiatCurrency: String = _selectedCurrency.value.code,
        customFiatRate: Double? = null
    ): Boolean {
        // Validation: Proof is MANDATORY
        if (proofUri.isBlank()) {
            _toastMessage.value = "COMPLIANCE BLOCKED: Proof-of-transaction screenshot is mandatory!"
            return false
        }
        if (referenceNo.isBlank()) {
            _toastMessage.value = "Reference/Transaction ID is required."
            return false
        }

        viewModelScope.launch {
            val user = _currentUser.value
            val initialStatus = when (stage) {
                TransactionStage.CAPITAL_INJECTION -> {
                    if (user.isAdmin) TransactionStatus.VERIFIED else TransactionStatus.PENDING_VERIFICATION
                }
                else -> TransactionStatus.COMPLETED
            }

            val appCurrency = AppCurrency.fromCode(fiatCurrency)
            val lockedRate = customFiatRate ?: (_exchangeRates.value[appCurrency] ?: appCurrency.defaultRatePerUsd)
            val lockedUsd = if (amountFiat != null && amountFiat > 0) {
                if (appCurrency == AppCurrency.USD) amountFiat else (amountFiat / lockedRate)
            } else if (amountUsdt != null && amountUsdt > 0) {
                amountUsdt
            } else 0.0

            val newEntity = PoolTransactionEntity(
                stage = stage,
                userEmail = user.email,
                userName = user.name,
                recipientEmail = recipientEmail?.takeIf { it.isNotBlank() },
                amountFiat = amountFiat,
                fiatCurrency = fiatCurrency,
                amountUsdt = amountUsdt,
                exchangeRate = exchangeRate,
                fee = fee ?: 0.0,
                referenceNo = referenceNo.trim(),
                proofUri = proofUri,
                proofDescription = proofDescription ?: "Uploaded screenshot proof",
                status = initialStatus,
                verifiedByEmail = if (user.isAdmin) user.email else null,
                notes = notes.trim(),
                timestamp = System.currentTimeMillis(),
                originalAmount = amountFiat ?: amountUsdt ?: 0.0,
                originalCurrency = if (amountFiat != null) fiatCurrency else "USDT",
                displayCurrencyAtTime = _selectedCurrency.value.code,
                fiatExchangeRate = lockedRate,
                convertedAmountUsd = lockedUsd,
                rateTimestamp = System.currentTimeMillis()
            )

            val insertedId = repository.insertTransaction(newEntity)
            _showNewTransactionDialog.value = false
            _toastMessage.value = "Logged ${stage.title} successfully with verified proof."

            // Auto-sync proof to Google Drive under workflow folder
            try {
                val driveResult = driveService.uploadProofReceipt(
                    localFilePath = proofUri,
                    stageName = stage.name,
                    referenceNo = referenceNo.trim(),
                    userEmail = user.email
                )
                repository.updateDriveInfo(insertedId, driveResult.fileId, driveResult.webViewLink)
                _driveSyncStatus.value = _driveSyncStatus.value.copy(
                    lastUploadedFile = "Uploaded to ${driveResult.folderName}",
                    lastUploadLink = driveResult.webViewLink
                )
            } catch (e: Exception) {
                // Keep local proof active
            }
        }
        return true
    }

    fun verifyTransaction(txId: Long, approved: Boolean) {
        if (!_currentUser.value.isAdmin) {
            _toastMessage.value = "Only Designated Administrator (dipuraj.thapa@gmail.com) can verify bank deposits."
            return
        }
        viewModelScope.launch {
            repository.verifyTransaction(txId, _currentUser.value.email, approved)
            _toastMessage.value = if (approved) "Capital injection verified & credited to central bank!" else "Flagged for audit."
        }
    }

    fun deleteTransaction(txId: Long) {
        if (!_currentUser.value.isAdmin) {
            _toastMessage.value = "Only administrator can purge records."
            return
        }
        viewModelScope.launch {
            repository.deleteTransaction(txId)
            _toastMessage.value = "Transaction record purged."
        }
    }

    fun resetDemoData() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            _toastMessage.value = "All ledger transactions cleared. System ready for production."
        }
    }

    suspend fun checkDuplicateReference(ref: String): Boolean {
        return repository.isReferenceDuplicate(ref)
    }

    fun openMoneyDistributionDialog() {
        if (!_currentUser.value.isAdmin) {
            _toastMessage.value = "Unauthorized: Only designated pool admin can disburse funds."
            return
        }
        _showMoneyDistributionDialog.value = true
    }

    fun closeMoneyDistributionDialog() {
        _showMoneyDistributionDialog.value = false
    }

    fun openReverseDialog(tx: PoolTransactionEntity) {
        if (!_currentUser.value.isAdmin) {
            _toastMessage.value = "Unauthorized: Only designated pool admin can reverse transactions."
            return
        }
        _transactionToReverse.value = tx
    }

    fun closeReverseDialog() {
        _transactionToReverse.value = null
    }

    fun executeMoneyDistribution(
        beneficiaryEmail: String,
        beneficiaryName: String,
        amount: Double,
        isUsdt: Boolean,
        fee: Double,
        notes: String,
        proofUri: String
    ) {
        viewModelScope.launch {
            val result = repository.executeDistribution(
                adminUser = _currentUser.value,
                beneficiaryEmail = beneficiaryEmail,
                beneficiaryName = beneficiaryName,
                amount = amount,
                isUsdt = isUsdt,
                fee = fee,
                notes = notes,
                proofUri = proofUri,
                currency = _selectedCurrency.value.code
            )
            result.onSuccess { entity ->
                _showMoneyDistributionDialog.value = false
                val assetStr = if (isUsdt) "$amount USDT" else "${_selectedCurrency.value.symbol}$amount"
                _toastMessage.value = "Disbursed $assetStr to $beneficiaryName (Ref: ${entity.referenceNo}). Ledger updated!"
            }.onFailure { ex ->
                _toastMessage.value = "Distribution failed: ${ex.message}"
            }
        }
    }

    fun reverseTransaction(txId: Long, reason: String) {
        viewModelScope.launch {
            val result = repository.reverseTransaction(
                adminUser = _currentUser.value,
                transactionId = txId,
                reason = reason
            )
            result.onSuccess {
                _transactionToReverse.value = null
                _toastMessage.value = "Transaction reversed. Active balances updated."
            }.onFailure { ex ->
                _toastMessage.value = "Reversal failed: ${ex.message}"
            }
        }
    }

    // Settlement Workflow
    fun openSettleDialog(tx: PoolTransactionEntity) {
        _transactionToSettle.value = tx
    }

    fun closeSettleDialog() {
        _transactionToSettle.value = null
    }

    fun recordSettlement(txId: Long, bankUtr: String) {
        viewModelScope.launch {
            val result = repository.recordSettlement(
                id = txId,
                operatorUser = _currentUser.value,
                bankUtr = bankUtr
            )
            result.onSuccess {
                _transactionToSettle.value = null
                _toastMessage.value = "Settlement recorded with UTR: $bankUtr. 72h user confirmation window opened."
            }.onFailure { ex ->
                _toastMessage.value = "Settlement recording failed: ${ex.message}"
            }
        }
    }

    // User Confirmation & Dispute
    fun confirmUserReceipt(txId: Long) {
        viewModelScope.launch {
            val result = repository.confirmUserReceipt(
                id = txId,
                currentUser = _currentUser.value
            )
            result.onSuccess {
                _toastMessage.value = "Receipt confirmed. Transaction fully reconciled!"
            }.onFailure { ex ->
                _toastMessage.value = "Confirmation failed: ${ex.message}"
            }
        }
    }

    fun openDisputeDialog(tx: PoolTransactionEntity) {
        _transactionToDispute.value = tx
    }

    fun closeDisputeDialog() {
        _transactionToDispute.value = null
    }

    fun raiseDispute(txId: Long, reason: String) {
        viewModelScope.launch {
            val result = repository.raiseDispute(
                id = txId,
                currentUser = _currentUser.value,
                reason = reason
            )
            result.onSuccess {
                _transactionToDispute.value = null
                _toastMessage.value = "Dispute flagged. Transaction placed under central admin review."
            }.onFailure { ex ->
                _toastMessage.value = "Failed to raise dispute: ${ex.message}"
            }
        }
    }

    fun openResolveDisputeDialog(tx: PoolTransactionEntity) {
        _transactionToResolveDispute.value = tx
    }

    fun closeResolveDisputeDialog() {
        _transactionToResolveDispute.value = null
    }

    fun resolveDispute(txId: Long, notes: String, isConfirmed: Boolean) {
        viewModelScope.launch {
            val result = repository.resolveDispute(
                id = txId,
                adminUser = _currentUser.value,
                resolutionNotes = notes,
                isConfirmed = isConfirmed
            )
            result.onSuccess {
                _transactionToResolveDispute.value = null
                val action = if (isConfirmed) "confirmed as delivered" else "reversed with compensating entry"
                _toastMessage.value = "Dispute resolved: $action."
            }.onFailure { ex ->
                _toastMessage.value = "Failed to resolve dispute: ${ex.message}"
            }
        }
    }

    // Maker-Checker High-Value Dual Control Verification
    fun verifyTransactionMakerChecker(
        txId: Long,
        verified: Boolean,
        notes: String? = null,
        approvalScreenshotUri: String? = null
    ) {
        viewModelScope.launch {
            val result = repository.verifyTransactionWithMakerChecker(
                id = txId,
                adminUser = _currentUser.value,
                verified = verified,
                notes = notes,
                approvalScreenshotUri = approvalScreenshotUri
            )
            result.onSuccess { msg ->
                _toastMessage.value = msg
            }.onFailure { ex ->
                _toastMessage.value = "Verification failed: ${ex.message}"
            }
        }
    }

    // FIFO Inspection Dialog
    fun openFifoLotAudit() {
        _showFifoLotAuditDialog.value = true
    }

    fun closeFifoLotAudit() {
        _showFifoLotAuditDialog.value = false
    }

    fun getFifoLedgerResult(): com.example.data.model.FifoCalculationResult {
        return repository.calculateFifoLedger(uiState.value.transactions)
    }

    fun triggerAutoReconciliation() {
        viewModelScope.launch {
            val result = repository.autoReconcileTimeouts()
            val reconciledCount = result.getOrDefault(0)
            if (reconciledCount > 0) {
                _toastMessage.value = "$reconciledCount transactions auto-reconciled after 72h SLA."
            } else {
                _toastMessage.value = "Reconciliation up to date. No pending 72h timeouts."
            }
        }
    }
}
