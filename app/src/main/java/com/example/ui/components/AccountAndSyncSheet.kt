package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.IndiaDate
import com.example.domain.PayrollEngine
import com.example.domain.SalaryCycle
import com.example.ui.SyncStatus
import com.example.ui.theme.CharcoalOutline
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CharcoalSurfaceElevated
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.MintHighlight
import com.example.ui.theme.OnErrorContainerDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountAndSyncSheet(
    userDisplayName: String?,
    userEmail: String?,
    trackingStartCycle: IndiaDate,
    currentCycle: SalaryCycle,
    syncStatus: SyncStatus,
    firebaseProjectId: String,
    firestoreDatabaseId: String,
    isSaving: Boolean,
    onUpdateTrackingStart: (IndiaDate) -> Unit,
    onRetrySync: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val startCycleObj = PayrollEngine.cycleFromStart(trackingStartCycle)
    val canMoveForward = trackingStartCycle < currentCycle.startDate

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
            Text(
                text = stringResource(id = R.string.account_settings_title),
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Account card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CharcoalSurfaceElevated,
                border = BorderStroke(1.dp, CharcoalOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = userDisplayName?.takeIf { it.isNotBlank() } ?: "Google Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (icon, tint, label) = when (syncStatus) {
                            is SyncStatus.Synced -> Triple(
                                Icons.Rounded.CloudDone,
                                MintHighlight,
                                "Live Cloud Firestore sync active"
                            )
                            is SyncStatus.Syncing -> Triple(
                                Icons.Rounded.Sync,
                                TextSecondary,
                                "Connecting to Cloud Firestore…"
                            )
                            is SyncStatus.Error -> Triple(
                                Icons.Rounded.CloudOff,
                                ErrorCoral,
                                syncStatus.message
                            )
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = tint
                        )
                    }
                    if (syncStatus is SyncStatus.Error) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onRetrySync,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = stringResource(id = R.string.retry),
                                color = ErrorCoral
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // First Tracked Cycle selector
            Text(
                text = "First Tracked Salary Cycle",
                style = MaterialTheme.typography.labelLarge,
                color = MintHighlight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Only cycles from this cycle onward auto-count elapsed daily pay. Move earlier to include real past cycles.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CharcoalSurfaceElevated,
                border = BorderStroke(1.dp, CharcoalOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = !isSaving,
                        onClick = {
                            val earlier = PayrollEngine.previousCycle(startCycleObj)
                            onUpdateTrackingStart(earlier.startDate)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tracking_start_earlier_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Include earlier cycle",
                            tint = MintHighlight
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = startCycleObj.rangeLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Starts ${startCycleObj.startDate.toIsoString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    IconButton(
                        enabled = !isSaving && canMoveForward,
                        onClick = {
                            val later = PayrollEngine.nextCycle(startCycleObj)
                            onUpdateTrackingStart(later.startDate)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("tracking_start_later_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Start from later cycle",
                            tint = if (canMoveForward) MintHighlight else TextMuted.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shared Firebase connection info for future website
            Text(
                text = "Shared Firebase Schema (Future Website)",
                style = MaterialTheme.typography.labelLarge,
                color = MintHighlight
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CharcoalSurfaceElevated,
                border = BorderStroke(1.dp, CharcoalOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SchemaInfoLine("Firebase Project ID", firebaseProjectId)
                    HorizontalDivider(color = CharcoalOutline)
                    SchemaInfoLine("Firestore Database ID", firestoreDatabaseId)
                    HorizontalDivider(color = CharcoalOutline)
                    SchemaInfoLine("Auth Provider", "Google Sign-In (owner UID isolation)")
                    HorizontalDivider(color = CharcoalOutline)
                    SchemaInfoLine("Profile Document", "users/{uid}")
                    HorizontalDivider(color = CharcoalOutline)
                    SchemaInfoLine("Per-Date Overrides", "users/{uid}/dayEntries/{YYYY-MM-DD}")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorContainerDark,
                    contentColor = OnErrorContainerDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.sign_out),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SchemaInfoLine(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
    }
}
