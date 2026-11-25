package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CycleSummary
import com.example.domain.RationalRupees
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
import kotlin.math.roundToInt

@Composable
fun CycleComparisonCard(
    cycles: List<CycleSummary>,
    selectedCycleId: String,
    onSelectCycle: (String) -> Unit,
    onAdjustTrackingStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maxEarned = max(
        1.0,
        cycles.maxOfOrNull { it.earnedTotal.toDoubleApprox() } ?: 1.0
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, CharcoalOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cycle_comparison_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.cycle_comparison_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Actual earned totals by cycle (estimates excluded)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                TextButton(
                    onClick = onAdjustTrackingStart,
                    modifier = Modifier.testTag("adjust_tracking_start_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = MintHighlight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Start cycle",
                        style = MaterialTheme.typography.labelMedium,
                        color = MintHighlight
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cycles.reversed().forEach { item ->
                    val isSelected = item.cycle.id == selectedCycleId
                    val ratio = (item.earnedTotal.toDoubleApprox() / maxEarned).toFloat().coerceIn(0.03f, 1f)
                    val animatedRatio by animateFloatAsState(
                        targetValue = ratio,
                        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                        label = "cycle_bar_${item.cycle.id}"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MintSoftContainer else CharcoalSurfaceElevated,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) MintHighlight.copy(alpha = 0.5f) else CharcoalOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectCycle(item.cycle.id) }
                            .testTag("cycle_row_${item.cycle.id}")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = item.cycle.rangeLabel,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (item.isOngoing) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = JadeBrand.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = "to date",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MintHighlight,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (item.isForecastProvisional) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AmberOvertime.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = "${item.cycle.totalDays}d",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AmberOvertime,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = item.earnedTotal.formatInr(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) MintHighlight else TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Proportional bar track
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(CharcoalSurface)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedRatio)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(if (isSelected) MintHighlight else JadeBrand)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.elapsedDaysCount}d elapsed · Payday ${item.cycle.paydayLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                                if (item.isOngoing) {
                                    Text(
                                        text = "Est. ${item.forecastTotal.formatInr()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CycleBreakdownRingCard(
    summary: CycleSummary,
    modifier: Modifier = Modifier
) {
    val totalVal = summary.earnedTotal.toDoubleApprox()
    val regularFrac = if (totalVal > 0.0) (summary.earnedRegularPay.toDoubleApprox() / totalVal).toFloat() else 0f
    val holidayFrac = if (totalVal > 0.0) (summary.earnedHolidayPremium.toDoubleApprox() / totalVal).toFloat() else 0f
    val overtimeFrac = if (totalVal > 0.0) (summary.earnedOvertimePay.toDoubleApprox() / totalVal).toFloat() else 0f

    val animReg by animateFloatAsState(
        targetValue = regularFrac * 360f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ring_regular"
    )
    val animHol by animateFloatAsState(
        targetValue = holidayFrac * 360f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ring_holiday"
    )
    val animOt by animateFloatAsState(
        targetValue = overtimeFrac * 360f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "ring_overtime"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, CharcoalOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("cycle_breakdown_ring_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.cycle_breakdown_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = summary.cycle.rangeLabel + if (summary.isOngoing) " · to date" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                if (summary.earnedOvertimeBlocks > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberOvertime.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "${summary.earnedOvertimeBlocks} OT blocks",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberOvertime,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated 3-part ring centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(196.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(184.dp)) {
                    val strokeWidth = 19.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(inset, inset)

                    // Base track
                    drawArc(
                        color = CharcoalSurfaceElevated,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    val activeSegments = listOf(animReg, animHol, animOt).count { it > 0.5f }
                    val gapDegrees = if (activeSegments > 1) 4f else 0f

                    var startAngle = -90f
                    if (animReg > 0.5f) {
                        val sweep = (animReg - gapDegrees).coerceAtLeast(1f)
                        drawArc(
                            color = JadeBrand,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += animReg
                    }
                    if (animHol > 0.5f) {
                        val sweep = (animHol - gapDegrees).coerceAtLeast(1f)
                        drawArc(
                            color = MintHighlight,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                        startAngle += animHol
                    }
                    if (animOt > 0.5f) {
                        val sweep = (animOt - gapDegrees).coerceAtLeast(1f)
                        drawArc(
                            color = AmberOvertime,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (summary.isOngoing) "Earned to date" else "Cycle total",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summary.earnedTotal.formatInr(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${summary.elapsedDaysCount} elapsed days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MintHighlight
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Exact amounts legend (always exact paise precision!)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RingLegendRow(
                    color = JadeBrand,
                    label = stringResource(id = R.string.legend_regular_pay),
                    subtitle = "Office full/half + paid Sunday/holiday base",
                    amount = summary.earnedRegularPay,
                    sharePercent = (regularFrac * 100f).roundToInt(),
                    tag = "legend_regular_amount"
                )
                RingLegendRow(
                    color = MintHighlight,
                    label = stringResource(id = R.string.legend_holiday_premium),
                    subtitle = "Extra ₹250 half / ₹500 full worked premium",
                    amount = summary.earnedHolidayPremium,
                    sharePercent = (holidayFrac * 100f).roundToInt(),
                    tag = "legend_holiday_amount"
                )
                RingLegendRow(
                    color = AmberOvertime,
                    label = stringResource(id = R.string.legend_overtime),
                    subtitle = if (summary.earnedOvertimeBlocks > 0) {
                        "${summary.earnedOvertimeBlocks} × 30m blocks × ₹500/18 (exact rational sum)"
                    } else {
                        "No OT blocks selected in this cycle"
                    },
                    amount = summary.earnedOvertimePay,
                    sharePercent = (overtimeFrac * 100f).roundToInt(),
                    tag = "legend_overtime_amount"
                )
            }
        }
    }
}

@Composable
private fun RingLegendRow(
    color: Color,
    label: String,
    subtitle: String,
    amount: RationalRupees,
    sharePercent: Int,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CharcoalSurfaceElevated,
        border = BorderStroke(1.dp, CharcoalOutline),
        modifier = Modifier.fillMaxWidth()
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
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amount.formatInr(alwaysShowPaise = true),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag(tag)
                )
                Text(
                    text = "$sharePercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}
