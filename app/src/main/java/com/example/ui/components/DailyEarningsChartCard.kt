package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CycleSummary
import com.example.domain.DayEarnings
import com.example.domain.IndiaDate
import com.example.ui.theme.AmberOvertime
import com.example.ui.theme.CharcoalOutline
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.JadeBrand
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.MintSoftContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.max

@Composable
fun DailyEarningsChartCard(
    summary: CycleSummary,
    selectedDate: IndiaDate,
    onSelectDate: (IndiaDate) -> Unit,
    onEditDay: (IndiaDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDay = summary.days.firstOrNull { it.date == selectedDate }
        ?: summary.days.lastOrNull { it.isElapsed }
        ?: summary.days.first()

    val peakDateSet = summary.peakElapsedDays.map { it.date }.toSet()
    val maxChartRupees = max(
        1000.0,
        summary.days.maxOfOrNull { if (it.isElapsed) it.totalPay.toDoubleApprox() else 500.0 } ?: 1000.0
    )

    val listState = rememberLazyListState()
    LaunchedEffect(summary.cycle.id, selectedDay.date) {
        val index = summary.days.indexOfFirst { it.date == selectedDay.date }
        if (index >= 0) {
            val target = (index - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, CharcoalOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_earnings_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header + compact stacked legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.daily_chart_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap any day for breakdown & exit time",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Text(
                    text = "${summary.elapsedDaysCount}/${summary.cycle.totalDays}d",
                    style = MaterialTheme.typography.labelSmall,
                    color = MintHighlight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact inline legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChartLegendChip(color = JadeBrand, label = "Regular")
                ChartLegendChip(color = MintHighlight, label = "Sun/Hol Work")
                ChartLegendChip(color = AmberOvertime, label = "Overtime")
            }

            // Highlight highest elapsed day(s) with date(s) & amount
            if (summary.peakElapsedDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                val peakAmount = summary.peakElapsedDays.first().totalPay.formatInr()
                val peakDatesText = if (summary.peakElapsedDays.size <= 3) {
                    summary.peakElapsedDays.joinToString(", ") { it.date.shortDisplay() }
                } else {
                    "${summary.peakElapsedDays.first().date.shortDisplay()} +${summary.peakElapsedDays.size - 1} tied"
                }
                val peakLabel = if (summary.peakElapsedDays.size == 1) "Highest elapsed day" else "Highest elapsed days"
                val jumpTarget = summary.peakElapsedDays.last().date

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MintSoftContainer,
                    border = BorderStroke(1.dp, MintHighlight.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectDate(jumpTarget) }
                        .testTag("peak_day_highlight_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = MintHighlight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = peakLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MintHighlight
                                )
                                Text(
                                    text = peakDatesText,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = peakAmount,
                            style = MaterialTheme.typography.titleLarge,
                            color = MintHighlight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stacked, tappable daily bar chart
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .testTag("daily_bars_row")
            ) {
                items(items = summary.days, key = { it.date.toIsoString() }) { day ->
                    val isSelected = day.date == selectedDay.date
                    val isPeak = day.date in peakDateSet
                    DailyBarColumn(
                        day = day,
                        maxChartRupees = maxChartRupees,
                        isSelected = isSelected,
                        isPeak = isPeak,
                        onClick = { onSelectDate(day.date) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CharcoalOutline)
            Spacer(modifier = Modifier.height(14.dp))

            // Selected Day Breakdown Panel
            SelectedDayBreakdownPanel(
                day = selectedDay,
                isPeak = selectedDay.date in peakDateSet,
                onEditClick = { onEditDay(selectedDay.date) }
            )
        }
    }
}

@Composable
private fun DailyBarColumn(
    day: DayEarnings,
    maxChartRupees: Double,
    isSelected: Boolean,
    isPeak: Boolean,
    onClick: () -> Unit
) {
    val barDesc = buildString {
        append(day.date.fullDisplay())
        append(", ")
        append(if (day.isElapsed) day.totalPay.formatInr() else "future date")
        if (isPeak) append(", highest elapsed day")
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> CharcoalSurfaceElevated
                    isPeak -> MintSoftContainer.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = barDesc }
            .padding(vertical = 6.dp, horizontal = 3.dp)
            .testTag("daily_bar_${day.date.toIsoString()}")
    ) {
        // Peak indicator dot above bar
        Box(
            modifier = Modifier.height(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPeak) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MintHighlight)
                )
            } else if (day.isToday) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(TextPrimary)
                )
            }
        }

        // Stacked vertical bar canvas
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(122.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barW = size.width
                val maxH = size.height
                val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())

                if (!day.isElapsed) {
                    // Future day: subtle dashed outline representing ₹500 normal forecast baseline
                    val futureH = ((500.0 / maxChartRupees).toFloat() * maxH).coerceAtLeast(10.dp.toPx())
                    drawRoundRect(
                        color = CharcoalOutline,
                        topLeft = Offset(0f, maxH - futureH),
                        size = Size(barW, futureH),
                        cornerRadius = corner,
                        style = Stroke(
                            width = 1.4.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                        )
                    )
                } else {
                    val reg = day.regularPay.toDoubleApprox()
                    val hol = day.holidayPremium.toDoubleApprox()
                    val ot = day.overtimePay.toDoubleApprox()
                    val total = reg + hol + ot

                    if (total <= 0.0) {
                        // Leave (₹0): draw subtle baseline tick
                        drawRoundRect(
                            color = CharcoalOutline,
                            topLeft = Offset(0f, maxH - 4.dp.toPx()),
                            size = Size(barW, 4.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    } else {
                        val regH = ((reg / maxChartRupees).toFloat() * maxH)
                        val holH = ((hol / maxChartRupees).toFloat() * maxH)
                        val otH = ((ot / maxChartRupees).toFloat() * maxH)
                        val gap = 2.dp.toPx()

                        var currentBottom = maxH
                        if (reg > 0.0) {
                            val h = regH.coerceAtLeast(4.dp.toPx())
                            drawRoundRect(
                                color = JadeBrand,
                                topLeft = Offset(0f, currentBottom - h),
                                size = Size(barW, h),
                                cornerRadius = corner
                            )
                            currentBottom -= (h + if (hol > 0.0 || ot > 0.0) gap else 0f)
                        }
                        if (hol > 0.0) {
                            val h = holH.coerceAtLeast(4.dp.toPx())
                            drawRoundRect(
                                color = MintHighlight,
                                topLeft = Offset(0f, (currentBottom - h).coerceAtLeast(0f)),
                                size = Size(barW, h),
                                cornerRadius = corner
                            )
                            currentBottom -= (h + if (ot > 0.0) gap else 0f)
                        }
                        if (ot > 0.0) {
                            val h = otH.coerceAtLeast(5.dp.toPx())
                            drawRoundRect(
                                color = AmberOvertime,
                                topLeft = Offset(0f, (currentBottom - h).coerceAtLeast(0f)),
                                size = Size(barW, h),
                                cornerRadius = corner
                            )
                        }

                        if (isPeak || isSelected) {
                            val totalH = ((total / maxChartRupees).toFloat() * maxH).coerceAtLeast(8.dp.toPx())
                            drawRoundRect(
                                color = if (isPeak) MintHighlight else TextPrimary.copy(alpha = 0.7f),
                                topLeft = Offset(-1.dp.toPx(), (maxH - totalH - 1.dp.toPx()).coerceAtLeast(0f)),
                                size = Size(barW + 2.dp.toPx(), totalH + 2.dp.toPx()),
                                cornerRadius = corner,
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = day.date.day.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = when {
                isSelected -> MintHighlight
                day.isToday -> TextPrimary
                day.isElapsed -> TextSecondary
                else -> TextMuted
            },
            fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal
        )

        Text(
            text = day.date.dayOfWeek.shortLabel.take(2),
            style = MaterialTheme.typography.bodySmall,
            color = if (day.date.isSunday()) MintHighlight.copy(alpha = 0.85f) else TextMuted
        )
    }
}

@Composable
private fun SelectedDayBreakdownPanel(
    day: DayEarnings,
    isPeak: Boolean,
    onEditClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_day_breakdown")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = day.date.fullDisplay(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (day.isToday) {
                        SmallPillTag(text = "Today", tint = MintHighlight)
                    } else if (!day.isElapsed) {
                        SmallPillTag(text = "Future · not earned", tint = TextMuted)
                    } else if (isPeak) {
                        SmallPillTag(text = "Peak", tint = MintHighlight)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${day.dayType.displayLabel} · ${day.workStatus.displayLabel}" +
                        if (day.hasOverride) " · Edited" else " · Auto default",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Text(
                text = if (day.isElapsed) day.totalPay.formatInr() else "—",
                style = MaterialTheme.typography.headlineSmall,
                color = if (day.isElapsed) TextPrimary else TextMuted,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actual exit time row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = if (day.exitTime != null) AmberOvertime else TextMuted,
                modifier = Modifier.size(15.dp)
            )
            val exitDescription = when {
                day.exitTime != null && day.overtimeEnabled && day.overtimeBlocks > 0 ->
                    "Actual exit ${day.exitTime.display24h()} IST (${day.exitTime.display12h()}) · OT ON (${day.overtimeBlocks} × 30m block${if (day.overtimeBlocks > 1) "s" else ""})"
                day.exitTime != null && day.overtimeEnabled ->
                    "Actual exit ${day.exitTime.display24h()} IST (${day.exitTime.display12h()}) · OT ON (0 blocks after 18:30)"
                day.exitTime != null ->
                    "Actual exit ${day.exitTime.display24h()} IST (${day.exitTime.display12h()}) · OT OFF"
                else ->
                    "Actual exit time not recorded"
            }
            Text(
                text = exitDescription,
                style = MaterialTheme.typography.bodySmall,
                color = if (day.exitTime != null) TextPrimary.copy(alpha = 0.9f) else TextMuted
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3-bucket exact rupee breakdown
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BreakdownMiniCell(
                label = "Regular",
                amount = if (day.isElapsed) day.regularPay.formatInr() else "₹500 est.",
                accent = JadeBrand,
                modifier = Modifier.weight(1f)
            )
            BreakdownMiniCell(
                label = "Sun/Hol Work",
                amount = if (day.isElapsed) day.holidayPremium.formatInr() else "₹0",
                accent = MintHighlight,
                modifier = Modifier.weight(1f)
            )
            BreakdownMiniCell(
                label = "Overtime",
                amount = if (day.isElapsed) day.overtimePay.formatInr() else "₹0",
                accent = AmberOvertime,
                modifier = Modifier.weight(1f)
            )
        }

        if (!day.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Note: ${day.note}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        FilledTonalButton(
            onClick = onEditClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = CharcoalSurfaceElevated,
                contentColor = MintHighlight
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("edit_selected_day_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${stringResource(id = R.string.edit_entry)} · ${day.date.shortDisplay()}",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun BreakdownMiniCell(
    label: String,
    amount: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CharcoalSurfaceElevated,
        border = BorderStroke(1.dp, CharcoalOutline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ChartLegendChip(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun SmallPillTag(
    text: String,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
