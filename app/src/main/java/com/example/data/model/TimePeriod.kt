package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * High-level category of supported time periods across the app.
 */
enum class TimePeriodType(
    val title: String,
    val subtitle: String
) {
    TODAY("Today", "Activity for today"),
    DAILY("Daily", "Select a specific calendar date"),
    WEEKLY("Weekly", "Last 7 days"),
    WEEK_TO_DATE("Week to Date", "Monday of current week to now"),
    MONTHLY("Monthly", "Last 30 days"),
    MONTH_TO_DATE("Month to Date", "1st of current month to now"),
    CUSTOM_RANGE("Custom Date Range", "Select start and end dates"),
    ALL_TIME("All Time", "Complete historical ledger")
}

/**
 * Resolved boundary timestamps for database querying and metric calculation.
 */
data class TimeRangeBounds(
    val startTime: Long,
    val endTime: Long,
    val label: String
) {
    fun formattedDateRange(): String {
        if (startTime <= 0L && endTime >= Long.MAX_VALUE - 1000L) {
            return "Complete History"
        }
        val df = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val startStr = df.format(Date(startTime))
        val endStr = df.format(Date(endTime))
        return if (startStr == endStr) {
            startStr
        } else {
            "$startStr – $endStr"
        }
    }
}

/**
 * Immutable filter model for UI selection, database queries, and exports.
 */
data class TimePeriodFilter(
    val type: TimePeriodType = TimePeriodType.ALL_TIME,
    val specificDateMillis: Long? = null,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null
) {
    /**
     * Resolves the exact [startMillis, endMillis] range in epoch milliseconds.
     * Starts at 00:00:00.000 of start date; ends at 23:59:59.999 of end date.
     */
    fun resolveRange(referenceTime: Long = System.currentTimeMillis()): TimeRangeBounds {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceTime }

        return when (type) {
            TimePeriodType.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                TimeRangeBounds(start, end, "Today")
            }

            TimePeriodType.DAILY -> {
                val targetMillis = specificDateMillis ?: referenceTime
                cal.timeInMillis = targetMillis
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                TimeRangeBounds(start, end, "Daily · ${dateFormat.format(Date(start))}")
            }

            TimePeriodType.WEEKLY -> {
                // Trailing 7 days up to end of today
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                cal.add(Calendar.DAY_OF_MONTH, -6)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                TimeRangeBounds(start, end, "Weekly (7 Days)")
            }

            TimePeriodType.WEEK_TO_DATE -> {
                // From Monday of the current week to end of today
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                // Sunday is 1, Monday is 2, ..., Saturday is 7
                val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
                cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                TimeRangeBounds(start, end, "Week to Date")
            }

            TimePeriodType.MONTHLY -> {
                // Trailing 30 days
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                cal.add(Calendar.DAY_OF_MONTH, -29)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                TimeRangeBounds(start, end, "Monthly (30 Days)")
            }

            TimePeriodType.MONTH_TO_DATE -> {
                // From 1st of current month to end of today
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                TimeRangeBounds(start, end, "Month to Date")
            }

            TimePeriodType.CUSTOM_RANGE -> {
                var s = customStartMillis ?: (referenceTime - 7L * 86400000L)
                var e = customEndMillis ?: referenceTime
                if (s > e) {
                    val temp = s
                    s = e
                    e = temp
                }
                cal.timeInMillis = s
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.timeInMillis = e
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                TimeRangeBounds(start, end, "Custom Date Range")
            }

            TimePeriodType.ALL_TIME -> {
                TimeRangeBounds(0L, Long.MAX_VALUE, "All Time")
            }
        }
    }

    fun toTimePeriod(): TimePeriod {
        return when (type) {
            TimePeriodType.TODAY -> TimePeriod.TODAY
            TimePeriodType.DAILY -> TimePeriod.DAILY
            TimePeriodType.WEEKLY -> TimePeriod.WEEKLY
            TimePeriodType.WEEK_TO_DATE -> TimePeriod.WEEK_TO_DATE
            TimePeriodType.MONTHLY -> TimePeriod.MONTHLY
            TimePeriodType.MONTH_TO_DATE -> TimePeriod.MONTH_TO_DATE
            TimePeriodType.CUSTOM_RANGE -> TimePeriod.CUSTOM_RANGE
            TimePeriodType.ALL_TIME -> TimePeriod.ALL_TIME
        }
    }
}

/**
 * Backward-compatible Enum with extended presets.
 */
enum class TimePeriod(
    val label: String,
    val shortLabel: String,
    val type: TimePeriodType = TimePeriodType.ALL_TIME,
    val durationMillis: Long = Long.MAX_VALUE
) {
    TODAY("Today", "Today", TimePeriodType.TODAY, 24L * 60 * 60 * 1000),
    DAILY("Daily (24h)", "Daily", TimePeriodType.DAILY, 24L * 60 * 60 * 1000),
    WEEKLY("Weekly (7d)", "Weekly", TimePeriodType.WEEKLY, 7L * 24 * 60 * 60 * 1000),
    WEEK_TO_DATE("Week to Date", "WTD", TimePeriodType.WEEK_TO_DATE, 7L * 24 * 60 * 60 * 1000),
    MONTHLY("Monthly (30d)", "Monthly", TimePeriodType.MONTHLY, 30L * 24 * 60 * 60 * 1000),
    MONTH_TO_DATE("Month to Date", "MTD", TimePeriodType.MONTH_TO_DATE, 30L * 24 * 60 * 60 * 1000),
    CUSTOM_RANGE("Custom Range", "Custom", TimePeriodType.CUSTOM_RANGE, Long.MAX_VALUE),
    ALL_TIME("All Time", "All Time", TimePeriodType.ALL_TIME, Long.MAX_VALUE);

    fun toFilter(): TimePeriodFilter = TimePeriodFilter(type = this.type)
}

data class PeriodSummary(
    val period: TimePeriod = TimePeriod.ALL_TIME,
    val filter: TimePeriodFilter = TimePeriodFilter(period.type),
    val periodLabel: String = period.label,
    val dateRangeText: String = "Complete History",
    val startMillis: Long = 0L,
    val endMillis: Long = Long.MAX_VALUE,
    val moneySpent: Long = 0L,         // Zero decimal fiat spent/injected in display currency
    val pendingSpent: Long = 0L,       // Zero decimal fiat pending admin bank confirmation in display currency
    val moneyEarnedBack: Long = 0L,    // Zero decimal fiat returned from liquidations in display currency
    val usdtRemaining: Long = 0L,      // Zero decimal USDT left in pool (always USDT)
    val totalTransactionsCount: Int = 0,
    val profitLoss: Long = 0L,         // Zero decimal net realized profit/loss in display currency
    val currency: AppCurrency = AppCurrency.INR // Active display currency
) {
    val isProfit: Boolean get() = profitLoss >= 0
}
