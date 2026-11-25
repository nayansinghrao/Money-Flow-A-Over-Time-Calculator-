package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.DayOverride
import com.example.domain.DayType
import com.example.domain.ExitTime
import com.example.domain.IndiaDate
import com.example.domain.PayrollEngine
import com.example.domain.WorkStatus
import com.example.ui.theme.AmberOvertime
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalOutline
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.JadeBrand
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.MintSoftContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayEditorSheet(
    date: IndiaDate,
    today: IndiaDate,
    existingOverride: DayOverride?,
    isSaving: Boolean,
    onStepDate: (IndiaDate) -> Unit,
    onDismiss: () -> Unit,
    onSave: (DayOverride) -> Unit,
    onResetToDefault: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val autoDayType = remember(date) { PayrollEngine.defaultDayType(date) }

    var dayType by remember(date, existingOverride) {
        mutableStateOf(existingOverride?.dayType ?: autoDayType)
    }
    var workStatus by remember(date, existingOverride) {
        mutableStateOf(existingOverride?.workStatus ?: WorkStatus.defaultStatusFor(dayType))
    }
    var exitTime by remember(date, existingOverride) {
        mutableStateOf(existingOverride?.exitTime)
    }
    var overtimeEnabled by remember(date, existingOverride) {
        mutableStateOf(existingOverride?.overtimeEnabled ?: false)
    }
    var note by remember(date, existingOverride) {
        mutableStateOf(existingOverride?.note ?: "")
    }
    var showTimePicker by remember { mutableStateOf(false) }

    val draftOverride = DayOverride(
        date = date,
        dayType = dayType,
        workStatus = workStatus,
        exitTime = if (workStatus.isWorked()) exitTime else null,
        overtimeEnabled = if (workStatus.isWorked()) overtimeEnabled else false,
        note = note.trim().take(200).takeIf { it.isNotEmpty() }
    )
    val livePreview = PayrollEngine.calculateDay(
        date = date,
        override = draftOverride,
        today = today
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CharcoalSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Date header with prev/next step buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onStepDate(date.minusDays(1)) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_prev_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Previous day",
                        tint = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.fullDisplay(),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = when {
                            date == today -> "Today · IST"
                            date > today -> "Future date · excluded from earned"
                            else -> "Auto default: ${autoDayType.displayLabel} (₹500)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (date == today) MintHighlight else TextMuted
                    )
                }

                IconButton(
                    onClick = { onStepDate(date.plusDays(1)) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_next_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Next day",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. DAY TYPE — FIRST SELECTOR
            Text(
                text = "1. Day Type",
                style = MaterialTheme.typography.labelLarge,
                color = MintHighlight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayType.entries.forEach { option ->
                    val selected = dayType == option
                    SelectorOptionCard(
                        title = option.displayLabel,
                        subtitle = when (option) {
                            DayType.OFFICE_DAY -> "Mon–Sat default"
                            DayType.SUNDAY -> "Weekly off"
                            DayType.HOLIDAY -> "Company holiday"
                        },
                        selected = selected,
                        onClick = {
                            if (dayType != option) {
                                dayType = option
                                val valid = WorkStatus.validStatusesFor(option)
                                if (workStatus !in valid) {
                                    workStatus = WorkStatus.defaultStatusFor(option)
                                }
                                if (!workStatus.isWorked()) {
                                    overtimeEnabled = false
                                }
                            }
                        },
                        tag = "day_type_${option.name.lowercase()}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. ATTENDANCE / WORK STATUS
            Text(
                text = "2. Work Status",
                style = MaterialTheme.typography.labelLarge,
                color = MintHighlight
            )
            Spacer(modifier = Modifier.height(8.dp))

            val statusOptions = WorkStatus.validStatusesFor(dayType)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { option ->
                    val selected = workStatus == option
                    val amountHint = when (dayType) {
                        DayType.OFFICE_DAY -> when (option) {
                            WorkStatus.FULL -> "₹500"
                            WorkStatus.HALF -> "₹250"
                            WorkStatus.LEAVE -> "₹0"
                            WorkStatus.PAID_OFF -> "₹500"
                        }
                        DayType.SUNDAY, DayType.HOLIDAY -> when (option) {
                            WorkStatus.PAID_OFF -> "₹500"
                            WorkStatus.HALF -> "+₹250 = ₹750"
                            WorkStatus.FULL -> "+₹500 = ₹1,000"
                            WorkStatus.LEAVE -> "₹0"
                        }
                    }
                    SelectorOptionCard(
                        title = option.displayLabel,
                        subtitle = amountHint,
                        selected = selected,
                        onClick = {
                            workStatus = option
                            if (!option.isWorked()) {
                                overtimeEnabled = false
                            }
                        },
                        tag = "work_status_${option.name.lowercase()}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. EXIT TIME & EXPLICIT OVERTIME SWITCH
            Text(
                text = "3. Actual Exit Time & Overtime",
                style = MaterialTheme.typography.labelLarge,
                color = MintHighlight
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!workStatus.isWorked()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CharcoalSurfaceElevated,
                    border = BorderStroke(1.dp, CharcoalOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Overtime is disabled for Leave and unworked Sundays or holidays (OT = ₹0).",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CharcoalSurfaceElevated,
                    border = BorderStroke(1.dp, CharcoalOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Explicit OT Switch row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Count Overtime (OT)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "After 18:30 · ₹500/18 per started 30m block",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = overtimeEnabled,
                                onCheckedChange = { enabled ->
                                    overtimeEnabled = enabled
                                    if (enabled && exitTime == null) {
                                        exitTime = ExitTime(19, 0)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CharcoalBackground,
                                    checkedTrackColor = AmberOvertime
                                ),
                                modifier = Modifier.testTag("overtime_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = CharcoalOutline)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Actual exit time display + exact time picker trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Actual exit time (saved exact to minute)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = exitTime?.let { "${it.display24h()} IST (${it.display12h()})" }
                                        ?: "Not set",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (exitTime != null) TextPrimary else TextMuted,
                                    modifier = Modifier.testTag("exit_time_display")
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (exitTime != null) {
                                    IconButton(
                                        onClick = { exitTime = null },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear exit time",
                                            tint = TextMuted
                                        )
                                    }
                                }
                                OutlinedButton(
                                    onClick = { showTimePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MintHighlight.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("pick_exact_exit_time_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccessTime,
                                        contentDescription = null,
                                        tint = MintHighlight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Set", color = MintHighlight)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick exit time presets
                        val presets = listOf(
                            ExitTime(18, 30),
                            ExitTime(19, 0),
                            ExitTime(19, 30),
                            ExitTime(20, 0)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEach { preset ->
                                FilterChip(
                                    selected = exitTime == preset,
                                    onClick = { exitTime = preset },
                                    label = {
                                        Text(
                                            text = preset.display24h(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MintSoftContainer,
                                        selectedLabelColor = MintHighlight
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Fine minute adjustment buttons so exact times like 18:42 or 19:01 are effortless
                        if (exitTime != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(-15 to "-15m", -1 to "-1m", 1 to "+1m", 15 to "+15m").forEach { (delta, label) ->
                                    OutlinedButton(
                                        onClick = { exitTime = exitTime!!.plusMinutes(delta) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, CharcoalOutline),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live exact calculation preview card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MintSoftContainer,
                border = BorderStroke(1.dp, MintHighlight.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculated Day Pay",
                            style = MaterialTheme.typography.titleSmall,
                            color = MintHighlight
                        )
                        Text(
                            text = livePreview.totalPay.formatInr(alwaysShowPaise = livePreview.overtimePay.isPositive),
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("editor_live_total")
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Regular ${livePreview.regularPay.formatInr()} + Sun/Hol ${livePreview.holidayPremium.formatInr()} + OT ${livePreview.overtimePay.formatInr(alwaysShowPaise = livePreview.overtimePay.isPositive)}" +
                            if (livePreview.overtimeBlocks > 0) " (${livePreview.overtimeBlocks} × 30m)" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Optional note
            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                label = { Text("Note (optional)") },
                placeholder = { Text("e.g. Company holiday, late release") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MintHighlight,
                    unfocusedBorderColor = CharcoalOutline,
                    focusedLabelColor = MintHighlight,
                    unfocusedLabelColor = TextMuted
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_note_input")
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Save & Reset actions
            Button(
                onClick = { onSave(draftOverride) },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JadeBrand,
                    contentColor = Color(0xFF041C13)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_day_entry_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF041C13),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Saving to Cloud…", fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = stringResource(id = R.string.save_entry),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (existingOverride != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onResetToDefault,
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, CharcoalOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reset_day_entry_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.reset_to_default),
                        color = TextSecondary
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        val initial = exitTime ?: ExitTime(19, 0)
        val pickerState = rememberTimePickerState(
            initialHour = initial.hour,
            initialMinute = initial.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = CharcoalSurface,
            title = {
                Text(
                    text = "Select exact exit time (IST)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            text = {
                TimePicker(state = pickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        exitTime = ExitTime(pickerState.hour, pickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("Apply", color = MintHighlight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SelectorOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MintSoftContainer else CharcoalSurfaceElevated,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MintHighlight else CharcoalOutline
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MintHighlight else TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) TextPrimary.copy(alpha = 0.85f) else TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
