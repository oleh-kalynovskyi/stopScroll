package com.example.scrollstop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.scrollstop.BlockState
import com.example.scrollstop.ScheduleMode
import com.example.scrollstop.SessionStatus
import com.example.scrollstop.SettingsManager
import com.example.scrollstop.TargetApp
import com.example.scrollstop.currentStrings
import com.example.scrollstop.formatMinuteOfDay

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MasterToggleCard(
    enabled: Boolean,
    mode: ScheduleMode,
    sessionStatus: SessionStatus,
    selectedApps: Set<TargetApp>,
    showMinOneAppHint: Boolean,
    accessibilityGranted: Boolean,
    blockState: BlockState,
    unlockActive: Boolean,
    unlockUntilMillis: Long,
    nowMillis: Long,
    is24Hour: Boolean,
    onToggle: (Boolean) -> Unit,
    onAppToggle: (TargetApp, Boolean) -> Unit,
    onEndUnlock: () -> Unit,
) {
    val s = currentStrings
    val selectedNames = TargetApp.entries
        .filter { it in selectedApps }
        .map { it.displayName }
    val appsLabel = when (selectedNames.size) {
        0 -> ""
        1 -> selectedNames[0]
        2 -> "${selectedNames[0]}${s.appsConjunction}${selectedNames[1]}"
        else -> selectedNames.dropLast(1).joinToString(", ") + s.appsConjunction + selectedNames.last()
    }

    val statusText = when {
        !enabled -> s.statusPaused
        !accessibilityGranted -> s.statusWaitingAccessibility
        unlockActive -> {
            val secsLeft = ((unlockUntilMillis - nowMillis) / 1000L).coerceAtLeast(0L)
            s.statusUnlockActive(secsLeft / 60, secsLeft % 60)
        }

        blockState.isBlocking && !blockState.unlockEligible -> {
            val end = blockState.endMinuteOfDay
                ?.let { s.untilTime(formatMinuteOfDay(it, is24Hour)) }
                ?: s.allDaySuffix
            s.statusHardBlockingNow(appsLabel, end)
        }

        blockState.isBlocking -> {
            val end = blockState.endMinuteOfDay
                ?.let { s.untilTime(formatMinuteOfDay(it, is24Hour)) }
                ?: s.allDaySuffix
            s.statusBlockingNow(appsLabel, end)
        }

        mode == ScheduleMode.SESSION_LIMIT -> {
            val remSecs = ((sessionStatus.remainingMillis + 999L) / 1_000L).coerceAtLeast(0L)
            s.statusSessionActive(remSecs / 60L, remSecs % 60L)
        }

        else -> s.statusArmedOutsideHours
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = enabled,
                        role = Role.Switch,
                        onValueChange = onToggle,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.masterBlockerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = enabled, onCheckedChange = null)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = s.blockedAppsTitle,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = s.blockedAppsHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TargetApp.entries.forEach { app ->
                        val isSelected = app in selectedApps
                        FilterChip(
                            selected = isSelected,
                            onClick = { onAppToggle(app, !isSelected) },
                            label = {
                                Text(
                                    text = app.displayName,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
                if (showMinOneAppHint) {
                    Text(
                        text = s.minOneAppRequiredHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            if (unlockActive) {
                OutlinedButton(
                    onClick = onEndUnlock,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(s.endUnlockNowButton)
                }
            }
        }
    }
}

@Composable
internal fun HourlyQuotaCard(
    remainingMinutes: Int,
    nextResetTime: String,
) {
    val s = currentStrings
    val used = SettingsManager.QUOTA_MINUTES_PER_HOUR - remainingMinutes
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = s.quotaTitle,
                    modifier = Modifier.width(180.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = s.quotaLeftLabel(remainingMinutes, SettingsManager.QUOTA_MINUTES_PER_HOUR),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            LinearProgressIndicator(
                progress = { remainingMinutes / SettingsManager.QUOTA_MINUTES_PER_HOUR.toFloat() },
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(6.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Text(
                text = s.quotaUsedSubtitle(used, nextResetTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
