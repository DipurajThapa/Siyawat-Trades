package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PoolTransactionEntity
import com.example.data.model.PeriodSummary
import com.example.data.model.PoolUser
import com.example.data.model.TimePeriod
import com.example.data.model.TransactionStage
import com.example.data.model.TransactionStatus
import com.example.data.model.SettlementState
import com.example.data.model.ReconciliationState
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.ThumbUp
import com.example.ui.theme.AmberBorder
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberTertiary
import com.example.ui.theme.Montserrat
import com.example.ui.theme.MutedBlueBorder
import com.example.ui.theme.MutedBlueContainer
import com.example.ui.theme.MutedBlueDark
import com.example.ui.theme.MutedBlueLight
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.PaperBackground
import com.example.ui.theme.PaperBorder
import com.example.ui.theme.PaperBorderSubtle
import com.example.ui.theme.PaperCard
import com.example.ui.theme.PaperCardElevated
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBorder
import com.example.ui.theme.ProfitGreenContainer
import com.example.ui.theme.RestrainedBlue
import com.example.ui.theme.RestrainedBlueBorder
import com.example.ui.theme.RestrainedBlueContainer
import com.example.ui.theme.RestrainedRed
import com.example.ui.theme.RestrainedRedBorder
import com.example.ui.theme.RestrainedRedContainer
import com.example.ui.theme.RobotoCondensed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import kotlin.math.abs
import com.example.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimpleMemberDashboard(
    periodSummary: PeriodSummary,
    selectedPeriod: TimePeriod = periodSummary.period,
    selectedFilter: com.example.data.model.TimePeriodFilter = periodSummary.filter,
    transactions: List<PoolTransactionEntity>,
    currentUser: PoolUser,
    activeCurrency: com.example.data.model.AppCurrency = com.example.data.model.AppCurrency.USD,
    activeRate: Double = 1.0,
    onSelectCurrency: (com.example.data.model.AppCurrency) -> Unit = {},
    onOpenRateDialog: () -> Unit = {},
    onSelectPeriod: (TimePeriod) -> Unit = {},
    onSelectFilter: (com.example.data.model.TimePeriodFilter) -> Unit = {},
    onAddMoneyClick: () -> Unit,
    onDownloadSheetClick: () -> Unit,
    onViewProofClick: (PoolTransactionEntity) -> Unit,
    onConfirmReceipt: (Long) -> Unit = {},
    onDispute: (PoolTransactionEntity) -> Unit = {}
) {
    val context = LocalContext.current
    var showApprovedOnly by remember { mutableStateOf(false) }

    val displayedTransactions = remember(transactions, showApprovedOnly) {
        if (showApprovedOnly) {
            transactions.filter { it.approvalStatus == "APPROVED" || it.status == TransactionStatus.VERIFIED || it.status == TransactionStatus.COMPLETED }
        } else {
            transactions
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperBackground)
            .testTag("simple_member_dashboard"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Multi-Currency Selection Bar
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_selector_card"),
                colors = CardDefaults.cardColors(containerColor = PaperCard),
                border = BorderStroke(1.dp, PaperBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenRateDialog() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MutedBlueContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Exchange Rates",
                                tint = MutedBlueDark,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "DISPLAY CURRENCY",
                                fontFamily = Montserrat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp,
                                color = TextSecondary
                            )
                            if (activeCurrency != com.example.data.model.AppCurrency.USD) {
                                Text(
                                    text = "1 USD = ${FormatUtils.formatRate(activeRate)} ${activeCurrency.code}",
                                    fontSize = 10.sp,
                                    color = MutedBlueDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // 3-Currency Segmented Buttons
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaperCardElevated)
                            .border(1.dp, PaperBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        com.example.data.model.AppCurrency.ALL.forEach { currency ->
                            val isSelected = currency == activeCurrency
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MutedBlueDark else Color.Transparent)
                                    .clickable { onSelectCurrency(currency) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("currency_chip_${currency.code}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (currency.symbol.equals(currency.code, ignoreCase = true)) currency.code else "${currency.symbol} ${currency.code}",
                                    fontFamily = Montserrat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Time Period Dropdown & Date Boundary Selector
        item {
            TimePeriodSelector(
                selectedFilter = selectedFilter,
                periodSummary = periodSummary,
                onSelectFilter = { newFilter ->
                    onSelectFilter(newFilter)
                    onSelectPeriod(newFilter.toTimePeriod())
                }
            )
        }

        // 2. Financial Summary Layout
        // Hierarchy Step 1: Money Spent / Added and Money Earned Back (Same Row, Equal Weight)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card A: Money Spent / Added
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_card_money_spent"),
                    colors = CardDefaults.cardColors(containerColor = PaperCard),
                    border = BorderStroke(1.dp, PaperBorder),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEBEFF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color(0xFF3B5998),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            if (periodSummary.pendingSpent > 0L) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AmberContainer)
                                        .border(1.dp, AmberBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = AmberTertiary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Pending",
                                        fontFamily = Montserrat,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AmberTertiary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Money Spent",
                            fontFamily = Montserrat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Large Roboto Condensed, Non-Bold Number
                        Text(
                            text = FormatUtils.formatCurrencyZeroDecimal(periodSummary.moneySpent.toDouble(), periodSummary.currency),
                            fontFamily = RobotoCondensed,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.testTag("value_money_spent")
                        )
                    }
                }

                // Card B: Money Earned Back
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_card_money_earned"),
                    colors = CardDefaults.cardColors(containerColor = PaperCard),
                    border = BorderStroke(1.dp, PaperBorder),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(RestrainedBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = RestrainedBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = RestrainedBlue.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Earned Back",
                            fontFamily = Montserrat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Large Roboto Condensed, Non-Bold Number
                        Text(
                            text = FormatUtils.formatCurrencyZeroDecimal(periodSummary.moneyEarnedBack.toDouble(), periodSummary.currency),
                            fontFamily = RobotoCondensed,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Normal,
                            color = RestrainedBlue,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.testTag("value_money_earned")
                        )
                    }
                }
            }
        }

        // Hierarchy Step 2: Profit & Loss (Dedicated row directly below, visually larger and prominent)
        item {
            val isProfit = periodSummary.isProfit
            val profitColor = if (isProfit) ProfitGreen else RestrainedRed
            val profitContainer = if (isProfit) ProfitGreenContainer else RestrainedRedContainer
            val profitBorder = if (isProfit) ProfitGreenBorder else RestrainedRedBorder

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("metric_card_profit_loss"),
                colors = CardDefaults.cardColors(containerColor = PaperCard),
                border = BorderStroke(1.dp, profitBorder),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(profitContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = profitColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "PROFIT & LOSS",
                                fontFamily = Montserrat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = TextSecondary
                            )
                        }

                        // Prominent Status Pill with Google Icon
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(profitContainer)
                                .border(1.dp, profitBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isProfit) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = profitColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (periodSummary.profitLoss > 0) "GAIN" else if (periodSummary.profitLoss < 0) "LOSS" else "NEUTRAL",
                                fontFamily = Montserrat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = profitColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Visually larger financial value in Roboto Condensed (Not Bold)
                    val formattedPnl = FormatUtils.formatSignedCurrencyZeroDecimal(periodSummary.profitLoss.toDouble(), periodSummary.currency)

                    Text(
                        text = formattedPnl,
                        fontFamily = RobotoCondensed,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Normal,
                        color = profitColor,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.testTag("value_profit_loss")
                    )
                }
            }
        }

        // Hierarchy Step 3: Remaining USDT (Placed directly below Profit & Loss)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("metric_card_usdt_left"),
                colors = CardDefaults.cardColors(containerColor = PaperCard),
                border = BorderStroke(1.dp, PaperBorder),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MutedBlueContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MutedBlueDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Remaining USDT",
                                fontFamily = Montserrat,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MutedBlueContainer)
                                .border(1.dp, MutedBlueBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Token,
                                contentDescription = null,
                                tint = MutedBlueDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Active Pool",
                                fontFamily = Montserrat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MutedBlueDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Large Roboto Condensed, Non-Bold Number
                    Text(
                        text = "${FormatUtils.formatInteger(periodSummary.usdtRemaining)} USDT",
                        fontFamily = RobotoCondensed,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Normal,
                        color = MutedBlueDark,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.testTag("value_usdt_left")
                    )
                }
            }
        }

        // 3. Symmetrical Action Buttons (Add Money + Download Google Sheet)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddMoneyClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_money_transfer_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MutedBluePrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Money",
                        fontFamily = Montserrat,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onDownloadSheetClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("download_google_sheet_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PaperCard,
                        contentColor = MutedBlueDark
                    ),
                    border = BorderStroke(1.2.dp, MutedBlueDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Google Sheet",
                        fontFamily = Montserrat,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 4. Activity Header & Filter
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ACTIVITY",
                        fontFamily = Montserrat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PaperCardElevated)
                            .border(0.5.dp, PaperBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${displayedTransactions.size}",
                            fontFamily = Montserrat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }

                FilterChip(
                    selected = showApprovedOnly,
                    onClick = { showApprovedOnly = !showApprovedOnly },
                    label = {
                        Text(
                            text = if (showApprovedOnly) "Approved" else "All Records",
                            fontFamily = Montserrat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showApprovedOnly) Icons.Default.CheckCircle else Icons.Default.FilterAlt,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RestrainedBlueContainer,
                        selectedLabelColor = RestrainedBlue,
                        containerColor = PaperCardElevated,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (showApprovedOnly) RestrainedBlueBorder else PaperBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // 5. Activity Transactions List (With explicit Date, Timestamp & Approvals)
        if (displayedTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PaperCard),
                    border = BorderStroke(1.dp, PaperBorder),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (showApprovedOnly) "No approved transactions in ${periodSummary.periodLabel}" else "No activity in ${periodSummary.periodLabel}",
                            fontFamily = Montserrat,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Date range: ${periodSummary.dateRangeText}. Tap 'Add Money' above or adjust your time period filter.",
                            fontFamily = Montserrat,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedTransactions, key = { it.id }) { tx ->
                SimpleTransactionRow(
                    tx = tx,
                    currentUser = currentUser,
                    activeCurrency = activeCurrency,
                    activeRate = activeRate,
                    onViewProof = { onViewProofClick(tx) },
                    onConfirmReceipt = { onConfirmReceipt(tx.id) },
                    onDispute = { onDispute(tx) }
                )
            }
        }
    }
}

@Composable
fun SimpleTransactionRow(
    tx: PoolTransactionEntity,
    currentUser: PoolUser? = null,
    activeCurrency: com.example.data.model.AppCurrency = com.example.data.model.AppCurrency.USD,
    activeRate: Double = 1.0,
    onViewProof: () -> Unit,
    onConfirmReceipt: () -> Unit = {},
    onDispute: () -> Unit = {}
) {
    val isInjection = tx.stage == TransactionStage.CAPITAL_INJECTION
    val isLiquidation = tx.stage == TransactionStage.LIQUIDATION
    val isApproved = tx.approvalStatus == "APPROVED" ||
            tx.status == TransactionStatus.VERIFIED ||
            tx.status == TransactionStatus.COMPLETED

    val displayAmt = tx.getAmountInDisplayCurrency(activeRate)
    val origCurr = com.example.data.model.AppCurrency.fromCode(tx.originalCurrency)

    val amountText = when {
        tx.amountFiat != null || tx.convertedAmountUsd > 0.0 -> {
            val prefix = if (isInjection || isLiquidation) "+" else ""
            prefix + FormatUtils.formatCurrencyZeroDecimal(displayAmt, activeCurrency)
        }
        tx.amountUsdt != null -> {
            FormatUtils.formatInteger(tx.amountUsdt) + " USDT"
        }
        else -> if (activeCurrency == com.example.data.model.AppCurrency.AED) "AED 0" else "${activeCurrency.symbol}0"
    }

    val amountColor = when {
        isLiquidation -> RestrainedBlue
        isInjection -> Color(0xFF2563EB)
        else -> MutedBlueDark
    }

    val recordDate = tx.recordDate.ifEmpty {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(tx.timestamp))
    }
    val recordTime = tx.recordTime.ifEmpty {
        SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(tx.timestamp))
    }

    val (stageIcon, stageTint, stageBg) = when (tx.stage) {
        TransactionStage.CAPITAL_INJECTION -> Triple(Icons.Default.ArrowOutward, Color(0xFF2563EB), Color(0xFFEBF2FE))
        TransactionStage.BANK_TO_EXCHANGE -> Triple(Icons.Default.AccountBalanceWallet, MutedBlueDark, MutedBlueContainer)
        TransactionStage.USDT_ACQUISITION -> Triple(Icons.Default.Token, MutedBlueDark, MutedBlueContainer)
        TransactionStage.USDT_DISTRIBUTION -> Triple(Icons.Default.SwapHoriz, MutedBlueDark, MutedBlueContainer)
        TransactionStage.LIQUIDATION -> Triple(Icons.Default.CheckCircle, RestrainedBlue, RestrainedBlueContainer)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("simple_tx_item_${tx.id}"),
        colors = CardDefaults.cardColors(containerColor = PaperCard),
        border = BorderStroke(1.dp, PaperBorder),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Member Avatar/Icon + Name, and Roboto Condensed Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(PaperCardElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tx.userName,
                        fontFamily = Montserrat,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    if (tx.driveFileId != null || tx.driveWebViewLink != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced to Drive",
                            tint = RestrainedBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Amount in Roboto Condensed with multi-currency original trace
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = amountText,
                        fontFamily = RobotoCondensed,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = amountColor,
                        letterSpacing = (-0.25).sp
                    )
                    if (tx.originalCurrency != activeCurrency.code && tx.originalAmount > 0) {
                        Text(
                            text = "Orig: ${FormatUtils.formatCurrencyZeroDecimal(tx.originalAmount, origCurr)}",
                            fontFamily = Montserrat,
                            fontSize = 10.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Stage indicator badge with Google Icon
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(stageBg)
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = stageIcon,
                    contentDescription = null,
                    tint = stageTint,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tx.stage.title,
                    fontFamily = Montserrat,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = stageTint
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Metadata bar with Google Icons replacing text-heavy labels (Date, Time, Ref)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(PaperCardElevated)
                    .border(0.5.dp, PaperBorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = recordDate,
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = recordTime,
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = "Reference",
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = tx.referenceNo,
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Approval Status with Google Icon & Slip Viewer Button with Google Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isApproved) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RestrainedBlueContainer)
                            .border(1.dp, RestrainedBlueBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = RestrainedBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Approved by ${tx.approvedByEmail?.substringBefore("@") ?: "Admin"}",
                            fontFamily = Montserrat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RestrainedBlue
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmberContainer)
                            .border(1.dp, AmberBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = AmberTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pending Clearance",
                            fontFamily = Montserrat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberTertiary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PaperCardElevated)
                        .border(1.dp, PaperBorder, RoundedCornerShape(8.dp))
                        .clickable { onViewProof() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MutedBlueDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "View Slip",
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MutedBlueDark
                    )
                }
            }

            // Settlement UTR and Reconciliation Status Badges
            if (tx.bankUtrNumber != null || tx.reconciliationState == ReconciliationState.DISPUTED || tx.reconciliationState == ReconciliationState.RECONCILED) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tx.bankUtrNumber != null) {
                        Text(
                            text = "UTR: ${tx.bankUtrNumber}",
                            fontFamily = Montserrat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MutedBlueDark
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    when (tx.reconciliationState) {
                        ReconciliationState.DISPUTED -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(RestrainedRedContainer)
                                    .border(1.dp, RestrainedRedBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, tint = RestrainedRed, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Disputed", fontFamily = Montserrat, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RestrainedRed)
                            }
                        }
                        ReconciliationState.RECONCILED -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ProfitGreenContainer)
                                    .border(1.dp, ProfitGreenBorder, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Reconciled", fontFamily = Montserrat, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Member Action: If settled and pending user confirmation, allow Confirm or Dispute
            val isUserBeneficiaryOrOwner = currentUser != null && (
                currentUser.email.equals(tx.userEmail, ignoreCase = true) ||
                currentUser.email.equals(tx.recipientEmail, ignoreCase = true) ||
                currentUser.canManage
            )
            if (tx.settlementState == SettlementState.SETTLED && tx.reconciliationState == ReconciliationState.PENDING_USER_CONFIRM && isUserBeneficiaryOrOwner) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmberContainer.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, AmberBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Funds dispatched via external bank/crypto. Have you received this transfer?",
                            fontFamily = Montserrat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onConfirmReceipt,
                                modifier = Modifier.weight(1f).height(32.dp).testTag("confirm_receipt_btn_${tx.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirm Receipt", fontFamily = Montserrat, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onDispute,
                                modifier = Modifier.weight(1f).height(32.dp).testTag("dispute_btn_${tx.id}"),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, RestrainedRed),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RestrainedRed),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(12.dp), tint = RestrainedRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dispute", fontFamily = Montserrat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RestrainedRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
