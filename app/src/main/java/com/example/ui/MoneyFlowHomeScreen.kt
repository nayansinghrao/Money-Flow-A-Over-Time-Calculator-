package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.IndiaDate
import com.example.ui.components.AccountAndSyncSheet
import com.example.ui.components.CycleBreakdownRingCard
import com.example.ui.components.CycleComparisonCard
import com.example.ui.components.DailyEarningsChartCard
import com.example.ui.components.DayEditorSheet
import com.example.ui.components.HeroSummaryCard
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalOutline
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.MintSoftContainer
import com.example.ui.theme.OnErrorContainerDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyFlowHomeScreen(
    viewModel: MoneyFlowViewModel,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val selectedIndex = state.cycleSummaries.indexOfFirst {
        it.cycle.id == state.selectedCycleSummary.cycle.id
    }
    val canGoPrev = selectedIndex > 0
    val canGoNext = selectedIndex >= 0 && selectedIndex < state.cycleSummaries.lastIndex

    Scaffold(
        containerColor = CharcoalBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
            ) {
                // Compact Home Brand Header: approved rising-M mark + separate wordmark + Sync + Account
                item(key = "header") {
                    MoneyFlowHeaderBar(
                        syncStatus = state.syncStatus,
                        onSyncBadgeClick = {
                            if (state.syncStatus is SyncStatus.Error) {
                                viewModel.retrySync()
                            } else {
                                viewModel.setShowAccountSheet(true)
                            }
                        },
                        onAccountClick = { viewModel.setShowAccountSheet(true) }
                    )
                }

                // Explicit Sync Failure Banner (never claim local-only data is synced)
                if (state.syncStatus is SyncStatus.Error) {
                    val err = state.syncStatus as SyncStatus.Error
                    item(key = "sync_error_banner") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ErrorContainerDark,
                            border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sync_error_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudOff,
                                        contentDescription = "Cloud sync error",
                                        tint = ErrorCoral,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = err.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = OnErrorContainerDark
                                    )
                                }
                                TextButton(onClick = { viewModel.retrySync() }) {
                                    Text(
                                        text = stringResource(id = R.string.retry),
                                        color = ErrorCoral,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Action feedback banner (save / revert / write error)
                if (state.bannerMessage != null) {
                    item(key = "action_banner") {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (state.isBannerError) ErrorContainerDark else MintSoftContainer,
                            border = BorderStroke(
                                1.dp,
                                if (state.isBannerError) ErrorCoral.copy(alpha = 0.5f) else MintHighlight.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("action_feedback_banner")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = state.bannerMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.isBannerError) OnErrorContainerDark else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.dismissBanner() },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss notification",
                                        tint = if (state.isBannerError) ErrorCoral else MintHighlight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Cycle Selector Bar
                item(key = "cycle_selector") {
                    CycleSelectorBar(
                        rangeLabel = state.selectedCycleSummary.cycle.rangeLabel,
                        isOngoing = state.selectedCycleSummary.isOngoing,
                        canGoPrev = canGoPrev,
                        canGoNext = canGoNext,
                        onPrev = { viewModel.selectPreviousCycle() },
                        onNext = { viewModel.selectNextCycle() }
                    )
                }

                // Hero Summary Card: Earned through today + Separate Estimated amount through 23rd + Quick Edit Today
                item(key = "hero_summary") {
                    HeroSummaryCard(
                        summary = state.selectedCycleSummary,
                        todayEntry = state.todayEntry,
                        onEditTodayClick = { viewModel.openTodayEditor() },
                        onPickDateClick = { viewModel.setShowDatePickerDialog(true) }
                    )
                }

                // FIRST CHART: Daily stacked tappable bars + peak highlight + selected breakdown & exit time
                item(key = "daily_chart") {
                    DailyEarningsChartCard(
                        summary = state.selectedCycleSummary,
                        selectedDate = state.selectedDate,
                        onSelectDate = { viewModel.selectDate(it) },
                        onEditDay = { viewModel.openEditorForDate(it) }
                    )
                }

                // BELOW DAILY CHART: Salary Cycle Comparison
                item(key = "cycle_comparison") {
                    CycleComparisonCard(
                        cycles = state.cycleSummaries,
                        selectedCycleId = state.selectedCycleSummary.cycle.id,
                        onSelectCycle = { viewModel.selectCycle(it) },
                        onAdjustTrackingStart = { viewModel.setShowAccountSheet(true) }
                    )
                }

                // BELOW DAILY CHART: Animated 3-Part Breakdown Ring + Exact Legend
                item(key = "breakdown_ring") {
                    CycleBreakdownRingCard(summary = state.selectedCycleSummary)
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Day Entry Editor Bottom Sheet
    val editingDate = state.editingDate
    if (editingDate != null) {
        DayEditorSheet(
            date = editingDate,
            today = state.today,
            existingOverride = state.overrides[editingDate],
            isSaving = state.isSaving,
            onStepDate = { nextDate -> viewModel.openEditorForDate(nextDate) },
            onDismiss = { viewModel.closeEditor() },
            onSave = { override -> viewModel.saveDayOverride(override) },
            onResetToDefault = { viewModel.resetDayToDefault(editingDate) }
        )
    }

    // Account, Tracking Start Cycle & Future Website Schema Sheet
    if (state.showAccountSheet) {
        AccountAndSyncSheet(
            userDisplayName = state.userDisplayName,
            userEmail = state.userEmail,
            trackingStartCycle = state.trackingStartCycle,
            currentCycle = state.currentCycle,
            syncStatus = state.syncStatus,
            firebaseProjectId = state.firebaseProjectId,
            firestoreDatabaseId = state.firestoreDatabaseId,
            isSaving = state.isSaving,
            onUpdateTrackingStart = { viewModel.updateTrackingStartCycle(it) },
            onRetrySync = { viewModel.retrySync() },
            onSignOut = {
                viewModel.setShowAccountSheet(false)
                onSignOut()
            },
            onDismiss = { viewModel.setShowAccountSheet(false) }
        )
    }

    // Date Picker Dialog to edit any specific date (including past real entries)
    if (state.showDatePickerDialog) {
        val initialMillis = utcMillisForDate(state.selectedDate)
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { viewModel.setShowDatePickerDialog(false) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val picked = datePickerState.selectedDateMillis?.let { dateFromUtcMillis(it) }
                        if (picked != null) {
                            viewModel.openEditorForDate(picked)
                        } else {
                            viewModel.setShowDatePickerDialog(false)
                        }
                    }
                ) {
                    Text("Edit date", color = MintHighlight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowDatePickerDialog(false) }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun MoneyFlowHeaderBar(
    syncStatus: SyncStatus,
    onSyncBadgeClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Approved rising-M logo + separate legible MoneyFlow wordmark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_moneyflow_logo),
                contentDescription = stringResource(id = R.string.brand_logo_description),
                modifier = Modifier.size(width = 30.dp, height = 24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val (icon, tint, label) = when (syncStatus) {
                is SyncStatus.Synced -> Triple(
                    Icons.Rounded.CloudDone,
                    MintHighlight,
                    stringResource(id = R.string.sync_status_live)
                )
                is SyncStatus.Syncing -> Triple(
                    Icons.Rounded.Sync,
                    TextSecondary,
                    stringResource(id = R.string.sync_status_syncing)
                )
                is SyncStatus.Error -> Triple(
                    Icons.Rounded.CloudOff,
                    ErrorCoral,
                    stringResource(id = R.string.sync_status_error)
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = CharcoalSurface,
                border = BorderStroke(1.dp, tint.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onSyncBadgeClick)
                    .testTag("sync_status_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint
                    )
                }
            }

            IconButton(
                onClick = onAccountClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(CharcoalSurface)
                    .testTag("account_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountCircle,
                    contentDescription = stringResource(id = R.string.account_settings_title),
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CycleSelectorBar(
    rangeLabel: String,
    isOngoing: Boolean,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, CharcoalOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrev,
                enabled = canGoPrev,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("prev_cycle_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Previous salary cycle",
                    tint = if (canGoPrev) TextPrimary else TextMuted.copy(alpha = 0.35f)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = rangeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (isOngoing) "Current salary cycle · to date" else "Past tracked cycle",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOngoing) MintHighlight else TextMuted
                )
            }

            IconButton(
                onClick = onNext,
                enabled = canGoNext,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("next_cycle_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Next salary cycle",
                    tint = if (canGoNext) TextPrimary else TextMuted.copy(alpha = 0.35f)
                )
            }
        }
    }
}

private fun utcMillisForDate(date: IndiaDate): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(date.year, date.month - 1, date.day, 0, 0, 0)
    return cal.timeInMillis
}

private fun dateFromUtcMillis(millis: Long): IndiaDate {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.timeInMillis = millis
    return IndiaDate(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH) + 1,
        day = cal.get(Calendar.DAY_OF_MONTH)
    )
}
