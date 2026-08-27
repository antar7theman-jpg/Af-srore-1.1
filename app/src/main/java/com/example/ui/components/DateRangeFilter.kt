package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PolishPrimary
import java.text.SimpleDateFormat
import java.util.*

enum class DateFilterPreset(val displayName: String, val icon: ImageVector) {
    TODAY("اليوم", Icons.Default.FlashOn),
    LAST_7_DAYS("آخر 7 أيام", Icons.Default.DateRange),
    LAST_30_DAYS("آخر 30 يوم", Icons.Default.CalendarMonth),
    THIS_MONTH("هذا الشهر", Icons.Default.CalendarToday),
    LAST_3_MONTHS("آخر 3 أشهر", Icons.Default.QueryStats),
    THIS_YEAR("هذا العام", Icons.Default.Timeline),
    CUSTOM("نطاق مخصص", Icons.Default.Tune)
}

data class DateRangeSelection(
    val preset: DateFilterPreset,
    val startMillis: Long,
    val endMillis: Long,
    val label: String
)

object DateRangeHelper {
    private val arabicDateFormat = SimpleDateFormat("yyyy/MM/dd", Locale("ar"))

    fun getRangeForPreset(
        preset: DateFilterPreset,
        customStart: Long? = null,
        customEnd: Long? = null
    ): DateRangeSelection {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        // Set to end of today (23:59:59.999)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfToday = cal.timeInMillis

        return when (preset) {
            DateFilterPreset.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfToday = cal.timeInMillis
                DateRangeSelection(
                    preset = preset,
                    startMillis = startOfToday,
                    endMillis = endOfToday,
                    label = "اليوم (${arabicDateFormat.format(Date(now))})"
                )
            }
            DateFilterPreset.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                DateRangeSelection(
                    preset = preset,
                    startMillis = start,
                    endMillis = endOfToday,
                    label = "آخر 7 أيام (${arabicDateFormat.format(Date(start))} - ${arabicDateFormat.format(Date(endOfToday))})"
                )
            }
            DateFilterPreset.LAST_30_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -29)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                DateRangeSelection(
                    preset = preset,
                    startMillis = start,
                    endMillis = endOfToday,
                    label = "آخر 30 يوم"
                )
            }
            DateFilterPreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                DateRangeSelection(
                    preset = preset,
                    startMillis = start,
                    endMillis = endOfToday,
                    label = "هذا الشهر"
                )
            }
            DateFilterPreset.LAST_3_MONTHS -> {
                cal.add(Calendar.MONTH, -3)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                DateRangeSelection(
                    preset = preset,
                    startMillis = start,
                    endMillis = endOfToday,
                    label = "آخر 3 أشهر"
                )
            }
            DateFilterPreset.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                DateRangeSelection(
                    preset = preset,
                    startMillis = start,
                    endMillis = endOfToday,
                    label = "هذا العام (${Calendar.getInstance().get(Calendar.YEAR)})"
                )
            }
            DateFilterPreset.CUSTOM -> {
                val start = customStart ?: (endOfToday - (7L * 24 * 3600 * 1000))
                val end = customEnd ?: endOfToday
                DateRangeSelection(
                    preset = preset,
                    startMillis = start,
                    endMillis = end,
                    label = "مخصص (${arabicDateFormat.format(Date(start))} إلى ${arabicDateFormat.format(Date(end))})"
                )
            }
        }
    }
}

@Composable
fun DateRangeFilterRow(
    selectedPreset: DateFilterPreset,
    activeRangeLabel: String,
    onSelectPreset: (DateFilterPreset) -> Unit,
    onOpenCustomPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Active Date Range Badge Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "نطاق العرض:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activeRangeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (selectedPreset == DateFilterPreset.CUSTOM) {
                    IconButton(
                        onClick = onOpenCustomPicker,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "تعديل التاريخ",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Horizontal Scrollable Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateFilterPreset.values().forEach { preset ->
                val isSelected = selectedPreset == preset
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (preset == DateFilterPreset.CUSTOM) {
                            onOpenCustomPicker()
                        } else {
                            onSelectPreset(preset)
                        }
                    },
                    label = {
                        Text(
                            text = preset.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = preset.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        selectedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePickerDialog(
    initialStartMillis: Long,
    initialEndMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMillis,
        initialSelectedEndDateMillis = initialEndMillis
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis ?: initialStartMillis
                    // Ensure end of day for end date
                    val endCal = Calendar.getInstance().apply {
                        timeInMillis = dateRangePickerState.selectedEndDateMillis ?: start
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    onConfirm(start, endCal.timeInMillis)
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("تطبيق الفلترة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إلغاء")
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("تحديد نطاق التاريخ المخصص", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.fillMaxSize(),
                    title = null,
                    headline = null,
                    showModeToggle = false
                )
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
