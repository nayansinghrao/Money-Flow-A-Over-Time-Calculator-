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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.MonthlySummary
import com.example.ui.SyncStatus
import com.example.ui.theme.AmberOvertime
import com.example.ui.theme.CharcoalOutline
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.JadeBrand
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.MintSoftContainer
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CurrentMonthSummaryCard(
    monthlySummary: MonthlySummary,
    syncStatus: SyncStatus,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CharcoalSurface,
        border = BorderStroke(1.dp, CharcoalOutline),
        modifier = modifier
            .fillMaxWidth()
            .testTag("current_month_summary_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header row: Month title + Calendar Month badge + Firestore live source indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MintSoftContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MintHighlight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(id = R.string.current_month_summary_title, monthlySummary.monthLabel),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${monthlySummary.trackedWindowStart.shortDisplay()} – ${monthlySummary.monthEndDate.shortDisplay()} · ${monthlySummary.trackedElapsedDaysCount}d elapsed",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                // Firestore source badge reflecting real sync state
                val (badgeIcon, badgeTint, badgeText) = when (syncStatus) {
                    is SyncStatus.Connected -> Triple(Icons.Rounded.CloudDone, MintHighlight, "To date")
                    is SyncStatus.Syncing -> Triple(Icons.Rounded.Sync, AmberOvertime, "Syncing")
                    is SyncStatus.Offline -> Triple(Icons.Rounded.CloudOff, AmberOvertime, "Cached")
                    is SyncStatus.Error -> Triple(Icons.Rounded.CloudOff, ErrorCoral, "Sync error")
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeTint.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, badgeTint.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeTint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeTint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary current-month earned amount & separate month-end estimate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.current_month_earned_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = monthlySummary.earnedTotal.formatInr(alwaysShowPaise = false),
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary,
                        modifier = Modifier.testTag("current_month_earned_amount")
                    )
                }

                if (monthlySummary.remainingDaysInMonth > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Est. through ${monthlySummary.monthEndDate.shortDisplay()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = monthlySummary.estimatedMonthTotal.formatInr(alwaysShowPaise = false),
                            style = MaterialTheme.typography.titleLarge,
                            color = MintHighlight,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("current_month_estimate_amount")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-part monthly breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonthMetricTile(
                    label = "Regular",
                    amount = monthlySummary.earnedRegularPay.formatInr(),
                    accent = JadeBrand,
                    modifier = Modifier.weight(1f)
                )
                MonthMetricTile(
                    label = "Sun/Hol Work",
                    amount = monthlySummary.earnedHolidayPremium.formatInr(),
                    accent = MintHighlight,
                    modifier = Modifier.weight(1f)
                )
                MonthMetricTile(
                    label = if (monthlySummary.earnedOvertimeBlocks > 0) {
                        "OT (${monthlySummary.earnedOvertimeBlocks}×30m)"
                    } else {
                        "Overtime"
                    },
                    amount = monthlySummary.earnedOvertimePay.formatInr(
                        alwaysShowPaise = monthlySummary.earnedOvertimePay.isPositive
                    ),
                    accent = AmberOvertime,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CharcoalOutline)
            Spacer(modifier = Modifier.height(8.dp))

            val footerText = buildString {
                append("Calendar month total from Firestore")
                if (monthlySummary.syncedOverridesCount > 0) {
                    append(" (${monthlySummary.syncedOverridesCount} edited day")
                    if (monthlySummary.syncedOverridesCount > 1) append("s")
                    append(" + auto defaults)")
                } else {
                    append(" (auto elapsed defaults)")
                }
                if (monthlySummary.trackedWindowStart > monthlySummary.monthStartDate) {
                    append(" · tracking from ${monthlySummary.trackedWindowStart.shortDisplay()}")
                }
            }
            Text(
                text = footerText,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun MonthMetricTile(
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
