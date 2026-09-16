package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PeriodSummary
import com.example.data.model.TimePeriodFilter
import com.example.data.model.TimePeriodType
import com.example.ui.theme.Montserrat
import com.example.ui.theme.PaperBorder
import com.example.ui.theme.PaperCard
import com.example.ui.theme.RestrainedBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePeriodSelector(
    selectedFilter: TimePeriodFilter,
    periodSummary: PeriodSummary,
    onSelectFilter: (TimePeriodFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showDatePickerForType by remember { mutableStateOf<DatePickerTarget?>(null) }

    val resolvedBounds = remember(selectedFilter) {
        selectedFilter.resolveRange()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("time_period_selector_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PaperCard),
        border = BorderStroke(1.dp, PaperBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Label & Active Filter Range Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = RestrainedBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TIME PERIOD",
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = TextSecondary
                    )
                }

                // Range Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RestrainedBlue.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, RestrainedBlue.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = RestrainedBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = resolvedBounds.formattedDateRange(),
                            fontFamily = Montserrat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = RestrainedBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Dropdown Anchor Button
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("time_period_dropdown_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, if (expanded) RestrainedBlue else PaperBorder),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RestrainedBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getPeriodIcon(selectedFilter.type),
                                    contentDescription = null,
                                    tint = RestrainedBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = selectedFilter.type.title,
                                    fontFamily = Montserrat,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = resolvedBounds.label,
                                    fontFamily = Montserrat,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Close period menu" else "Open period menu",
                            tint = if (expanded) RestrainedBlue else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Dropdown Menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(MaterialTheme.colorScheme.surface)
                        .testTag("time_period_menu")
                ) {
                    TimePeriodType.entries.forEach { type ->
                        val isSelected = selectedFilter.type == type
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = getPeriodIcon(type),
                                            contentDescription = null,
                                            tint = if (isSelected) RestrainedBlue else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = type.title,
                                                fontFamily = Montserrat,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) RestrainedBlue else TextPrimary
                                            )
                                            Text(
                                                text = type.subtitle,
                                                fontFamily = Montserrat,
                                                fontSize = 10.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = RestrainedBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                expanded = false
                                when (type) {
                                    TimePeriodType.DAILY -> {
                                        // Set to daily with current selection or today
                                        val date = selectedFilter.specificDateMillis ?: System.currentTimeMillis()
                                        onSelectFilter(TimePeriodFilter(TimePeriodType.DAILY, specificDateMillis = date))
                                    }
                                    TimePeriodType.CUSTOM_RANGE -> {
                                        val now = System.currentTimeMillis()
                                        val start = selectedFilter.customStartMillis ?: (now - 7L * 86400000L)
                                        val end = selectedFilter.customEndMillis ?: now
                                        onSelectFilter(TimePeriodFilter(TimePeriodType.CUSTOM_RANGE, customStartMillis = start, customEndMillis = end))
                                    }
                                    else -> {
                                        onSelectFilter(TimePeriodFilter(type = type))
                                    }
                                }
                            },
                            modifier = Modifier.testTag("period_option_${type.name.lowercase()}")
                        )
                    }
                }
            }

            // Contextual Date Controls (Only shown when relevant)
            AnimatedVisibility(
                visible = selectedFilter.type == TimePeriodType.DAILY,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                DailyDatePickerControls(
                    selectedFilter = selectedFilter,
                    onDateChanged = { newMillis ->
                        onSelectFilter(TimePeriodFilter(TimePeriodType.DAILY, specificDateMillis = newMillis))
                    },
                    onRequestPicker = {
                        showDatePickerForType = DatePickerTarget.DAILY
                    }
                )
            }

            AnimatedVisibility(
                visible = selectedFilter.type == TimePeriodType.CUSTOM_RANGE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CustomDateRangeControls(
                    selectedFilter = selectedFilter,
                    onRangeChanged = { start, end ->
                        onSelectFilter(TimePeriodFilter(TimePeriodType.CUSTOM_RANGE, customStartMillis = start, customEndMillis = end))
                    },
                    onRequestStartPicker = {
                        showDatePickerForType = DatePickerTarget.CUSTOM_START
                    },
                    onRequestEndPicker = {
                        showDatePickerForType = DatePickerTarget.CUSTOM_END
                    }
                )
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePickerForType != null) {
        val target = showDatePickerForType!!
        val initialSelectedMillis = when (target) {
            DatePickerTarget.DAILY -> selectedFilter.specificDateMillis ?: System.currentTimeMillis()
            DatePickerTarget.CUSTOM_START -> selectedFilter.customStartMillis ?: (System.currentTimeMillis() - 7L * 86400000L)
            DatePickerTarget.CUSTOM_END -> selectedFilter.customEndMillis ?: System.currentTimeMillis()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerForType = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis ?: initialSelectedMillis
                        when (target) {
                            DatePickerTarget.DAILY -> {
                                onSelectFilter(TimePeriodFilter(TimePeriodType.DAILY, specificDateMillis = picked))
                            }
                            DatePickerTarget.CUSTOM_START -> {
                                val currentEnd = selectedFilter.customEndMillis ?: System.currentTimeMillis()
                                val validEnd = if (picked > currentEnd) picked else currentEnd
                                onSelectFilter(TimePeriodFilter(TimePeriodType.CUSTOM_RANGE, customStartMillis = picked, customEndMillis = validEnd))
                            }
                            DatePickerTarget.CUSTOM_END -> {
                                val currentStart = selectedFilter.customStartMillis ?: (System.currentTimeMillis() - 7L * 86400000L)
                                val validStart = if (picked < currentStart) picked else currentStart
                                onSelectFilter(TimePeriodFilter(TimePeriodType.CUSTOM_RANGE, customStartMillis = validStart, customEndMillis = picked))
                            }
                        }
                        showDatePickerForType = null
                    },
                    modifier = Modifier.testTag("date_picker_confirm")
                ) {
                    Text("Apply", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = RestrainedBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForType = null }) {
                    Text("Cancel", fontFamily = Montserrat, color = TextSecondary)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = when (target) {
                            DatePickerTarget.DAILY -> "Select Date"
                            DatePickerTarget.CUSTOM_START -> "Select Start Date"
                            DatePickerTarget.CUSTOM_END -> "Select End Date"
                        },
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Relevant controls for Daily selection: Step back, step forward, today chip, and calendar click.
 */
@Composable
private fun DailyDatePickerControls(
    selectedFilter: TimePeriodFilter,
    onDateChanged: (Long) -> Unit,
    onRequestPicker: () -> Unit
) {
    val currentDateMillis = selectedFilter.specificDateMillis ?: System.currentTimeMillis()
    val df = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.US) }
    val formattedDay = remember(currentDateMillis) { df.format(Date(currentDateMillis)) }

    val isToday = remember(currentDateMillis) {
        val cal1 = Calendar.getInstance().apply { timeInMillis = currentDateMillis }
        val cal2 = Calendar.getInstance()
        cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .testTag("daily_date_controls")
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = RestrainedBlue.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, RestrainedBlue.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Previous Day Button
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = currentDateMillis
                            add(Calendar.DAY_OF_YEAR, -1)
                        }
                        onDateChanged(cal.timeInMillis)
                    },
                    modifier = Modifier.testTag("daily_prev_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day",
                        tint = RestrainedBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Date Chip (Click to open calendar)
                Surface(
                    onClick = onRequestPicker,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, PaperBorder),
                    modifier = Modifier.testTag("daily_open_picker_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = RestrainedBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedDay,
                            fontFamily = Montserrat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                // Next Day Button
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = currentDateMillis
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                        onDateChanged(cal.timeInMillis)
                    },
                    modifier = Modifier.testTag("daily_next_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        tint = RestrainedBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick Jump Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { onDateChanged(System.currentTimeMillis()) },
                shape = RoundedCornerShape(8.dp),
                color = if (isToday) RestrainedBlue else PaperCard,
                border = BorderStroke(1.dp, if (isToday) RestrainedBlue else PaperBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("daily_jump_today")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Jump to Today",
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) Color.White else TextPrimary
                    )
                }
            }

            Surface(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -1)
                    }
                    onDateChanged(cal.timeInMillis)
                },
                shape = RoundedCornerShape(8.dp),
                color = PaperCard,
                border = BorderStroke(1.dp, PaperBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .testTag("daily_jump_yesterday")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Yesterday",
                        fontFamily = Montserrat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

/**
 * Relevant controls for Custom Date Range: Start and end pickers with preset chips.
 */
@Composable
private fun CustomDateRangeControls(
    selectedFilter: TimePeriodFilter,
    onRangeChanged: (Long, Long) -> Unit,
    onRequestStartPicker: () -> Unit,
    onRequestEndPicker: () -> Unit
) {
    val now = System.currentTimeMillis()
    val start = selectedFilter.customStartMillis ?: (now - 7L * 86400000L)
    val end = selectedFilter.customEndMillis ?: now
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .testTag("custom_range_controls")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Start Date Card
            Surface(
                onClick = onRequestStartPicker,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, PaperBorder),
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_start_date_button")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "START DATE",
                        fontFamily = Montserrat,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = RestrainedBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = df.format(Date(start)),
                            fontFamily = Montserrat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // End Date Card
            Surface(
                onClick = onRequestEndPicker,
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, PaperBorder),
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_end_date_button")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "END DATE",
                        fontFamily = Montserrat,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = RestrainedBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = df.format(Date(end)),
                            fontFamily = Montserrat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Fast Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf(
                "7 Days" to 7L,
                "14 Days" to 14L,
                "30 Days" to 30L,
                "90 Days" to 90L
            )

            presets.forEach { (label, days) ->
                Surface(
                    onClick = {
                        val newStart = now - (days * 86400000L)
                        onRangeChanged(newStart, now)
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = PaperCard,
                    border = BorderStroke(1.dp, PaperBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .testTag("custom_preset_${days}d")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            fontFamily = Montserrat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

private enum class DatePickerTarget {
    DAILY,
    CUSTOM_START,
    CUSTOM_END
}

private fun getPeriodIcon(type: TimePeriodType): ImageVector {
    return when (type) {
        TimePeriodType.TODAY -> Icons.Default.Today
        TimePeriodType.DAILY -> Icons.Default.Event
        TimePeriodType.WEEKLY -> Icons.Default.DateRange
        TimePeriodType.WEEK_TO_DATE -> Icons.Default.DateRange
        TimePeriodType.MONTHLY -> Icons.Default.CalendarMonth
        TimePeriodType.MONTH_TO_DATE -> Icons.Default.CalendarMonth
        TimePeriodType.CUSTOM_RANGE -> Icons.Default.EditCalendar
        TimePeriodType.ALL_TIME -> Icons.Default.AllInclusive
    }
}
