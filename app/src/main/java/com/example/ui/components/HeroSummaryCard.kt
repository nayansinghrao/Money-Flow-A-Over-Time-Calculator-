package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CycleSummary
import com.example.domain.DayEarnings
import com.example.ui.theme.AmberOvertime
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.JadeBrand
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.PineDeep
import com.example.ui.theme.PineMid
import com.example.ui.theme.TextPrimary

@Composable
fun HeroSummaryCard(
    summary: CycleSummary,
    todayEntry: DayEarnings,
    onEditTodayClick: () -> Unit,
    onPickDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val heroGradient = Brush.linearGradient(
        colors = listOf(
            PineDeep,
            PineMid,
            JadeBrand
        )
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = PineDeep,
        border = BorderStroke(1.dp, MintHighlight.copy(alpha = 0.32f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Status pill + Payday pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadgePill(
                        text = if (summary.isOngoing) {
                            stringResource(id = R.string.badge_to_date)
                        } else {
                            stringResource(id = R.string.badge_completed)
                        },
                        accent = if (summary.isOngoing) MintHighlight else TextPrimary.copy(alpha = 0.85f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Event,
                            contentDescription = null,
                            tint = MintHighlight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = stringResource(id = R.string.payday_label, summary.cycle.paydayLabel),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary.copy(alpha = 0.92f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (summary.isOngoing) {
                        stringResource(id = R.string.earned_through_today)
                    } else {
                        stringResource(id = R.string.total_earned_cycle)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary.copy(alpha = 0.82f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Big Indian Rupee earned amount
                Text(
                    text = summary.earnedTotal.formatInr(alwaysShowPaise = false),
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary,
                    modifier = Modifier.testTag("hero_earned_amount")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${summary.elapsedDaysCount} of ${summary.cycle.totalDays} days elapsed · ${summary.cycle.rangeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary.copy(alpha = 0.78f)
                )

                // Separate estimated forecast section for ongoing cycles (never mixed into actuals)
                if (summary.isOngoing) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CharcoalBackground.copy(alpha = 0.42f))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            id = R.string.estimated_through_end,
                                            summary.cycle.endDate.shortDisplay()
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextPrimary.copy(alpha = 0.86f)
                                    )
                                    if (summary.isForecastProvisional) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AmberOvertime.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, AmberOvertime.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.badge_provisional),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AmberOvertime,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = summary.forecastTotal.formatInr(alwaysShowPaise = false),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MintHighlight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("hero_forecast_amount")
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = if (summary.isForecastProvisional) AmberOvertime else MintHighlight.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (summary.isForecastProvisional) {
                                        "Provisional ${summary.cycle.totalDays}-day daily-rate projection (+${summary.remainingNormalPay.formatInr()} for ${summary.remainingDaysCount} remaining days, zero future OT). Non-30-day company policy is unconfirmed."
                                    } else {
                                        "Earned so far + ${summary.remainingNormalPay.formatInr()} normal pay for ${summary.remainingDaysCount} remaining days (zero future OT or holiday work)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary.copy(alpha = 0.76f)
                                )
                            }
                        }
                    }
                } else if (summary.isForecastProvisional) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CharcoalBackground.copy(alpha = 0.36f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Provisional ${summary.cycle.totalDays}-day cycle daily-rate total (non-30-day company policy unconfirmed).",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberOvertime
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick action buttons: 1-tap Edit Today + Edit Any Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onEditTodayClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintHighlight,
                            contentColor = Color(0xFF042016)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("edit_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit today · ${todayEntry.totalPay.formatInr()}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = onPickDateClick,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TextPrimary.copy(alpha = 0.35f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pick_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = stringResource(id = R.string.add_edit_date),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.add_edit_date),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadgePill(
    text: String,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = CharcoalBackground.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
