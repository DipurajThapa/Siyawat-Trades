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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PoolMetrics
import com.example.ui.theme.MutedBlueLight
import com.example.ui.theme.MutedBluePrimary
import com.example.ui.theme.ProfitGreen
import java.util.Locale

@Composable
fun KpiOverviewCard(
    metrics: PoolMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("kpi_overview_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Hero: Single Combined Remaining USDT Balance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF0F2B48),
                                Color(0xFF183D60),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        MutedBlueLight.copy(alpha = 0.4f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MutedBlueLight.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "₮",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedBlueLight
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "REMAINING POOL BALANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "Combined Net USDT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        // Live Pool Status Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E456B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "● LIVE POOL",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MutedBlueLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${formatNumber(metrics.remainingPoolUsdt)} USDT",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Calculated: Total Purchased (${formatNumber(metrics.totalUsdtPurchased)}) - Total Sold (${formatNumber(metrics.totalUsdtSold)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBAE6FD)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Grid KPI metrics: Fiat Spent, USDT Purchased, USDT Sold, Fiat Realized
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniTile(
                    modifier = Modifier.weight(1f),
                    title = "Total Fiat Spent",
                    value = "$${formatNumber(metrics.totalFiatSpent)}",
                    subtitle = if (metrics.pendingFiatInjections > 0) "+$${formatNumber(metrics.pendingFiatInjections)} pending" else "100% verified",
                    icon = Icons.Default.Savings,
                    accentColor = MutedBlueLight
                )
                MetricMiniTile(
                    modifier = Modifier.weight(1f),
                    title = "Total USDT Purchased",
                    value = "${formatNumber(metrics.totalUsdtPurchased)} ₮",
                    subtitle = "Fee: ${formatNumber(metrics.totalTradingFeesUsdt)} ₮",
                    icon = Icons.Default.CurrencyExchange,
                    accentColor = MutedBluePrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniTile(
                    modifier = Modifier.weight(1f),
                    title = "Total USDT Sold",
                    value = "${formatNumber(metrics.totalUsdtSold)} ₮",
                    subtitle = "${metrics.liquidationsCount} liquidation(s)",
                    icon = Icons.Default.Payments,
                    accentColor = Color(0xFFF59E0B)
                )
                MetricMiniTile(
                    modifier = Modifier.weight(1f),
                    title = "Total Fiat Realised",
                    value = "$${formatNumber(metrics.totalFiatRealised)}",
                    subtitle = "PnL: ${if (metrics.netRealizedProfitLossFiat >= 0) "+$" else "-$"}${formatNumber(kotlin.math.abs(metrics.netRealizedProfitLossFiat))}",
                    icon = Icons.Default.TrendingUp,
                    accentColor = if (metrics.netRealizedProfitLossFiat >= 0) ProfitGreen else Color(0xFFEF4444)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Pipeline Solvency Stats: Bank Cash & Central Exchange Pool
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Central Bank Reserve: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${formatNumber(metrics.centralBankBalanceFiat)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Undistributed on Binance: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatNumber(metrics.unallocatedCentralUsdt)} ₮",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (metrics.unallocatedCentralUsdt > 0) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricMiniTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (subtitle.contains("pending")) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

private fun formatNumber(value: Double): String {
    return String.format(Locale.US, "%,d", value.toLong())
}
