package com.example.scrollstop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.scrollstop.ScheduleMode
import com.example.scrollstop.SettingsManager
import com.example.scrollstop.TimeInterval
import com.example.scrollstop.currentStrings
import com.example.scrollstop.formatMinuteOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleCard(
    mode: ScheduleMode,
    hardBlock: Boolean,
    intervalsCount: Int,
    onModeChange: (ScheduleMode) -> Unit,
    onHardBlockChange: (Boolean) -> Unit,
) {
    val s = currentStrings
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = s.scheduleTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = mode == ScheduleMode.ALL_DAY,
                    onClick = { onModeChange(ScheduleMode.ALL_DAY) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text(s.modeAllDay) },
                )
                SegmentedButton(
                    selected = mode == ScheduleMode.CUSTOM,
                    onClick = { onModeChange(ScheduleMode.CUSTOM) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text(s.modeCustomPeriods) },
                )
            }

            val ruleExplanation = when {
                hardBlock -> s.ruleHardBlockOn
                mode == ScheduleMode.ALL_DAY -> s.ruleAllDay(
                    SettingsManager.QUOTA_MINUTES_PER_HOUR,
                    SettingsManager.FRICTION_SECONDS,
                )

                intervalsCount == 0 -> s.ruleCustomEmpty(SettingsManager.QUOTA_MINUTES_PER_HOUR)
                else -> s.ruleCustomWithIntervals(SettingsManager.QUOTA_MINUTES_PER_HOUR)
            }
            Text(
                text = ruleExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = hardBlock,
                        role = Role.Checkbox,
                        onValueChange = onHardBlockChange,
                    )
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = hardBlock, onCheckedChange = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = s.hardBlockCheckboxTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = s.hardBlockCheckboxSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun IntervalsHeader(
    count: Int,
    message: String?,
    onAddClick: () -> Unit,
) {
    val s = currentStrings
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.customPeriodsHeader(count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = s.overnightSupportedSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            AssistChip(
                onClick = onAddClick,
                label = { Text(s.addPeriodChip) },
                leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            )
        }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
internal fun EmptyIntervalsCard(onAddClick: () -> Unit) {
    val s = currentStrings
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = s.noPeriodsTitle,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = s.noPeriodsSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAddClick) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(s.addTimePeriodButton)
            }
        }
    }
}

@Composable
internal fun IntervalRow(
    interval: TimeInterval,
    hardBlock: Boolean,
    isActiveNow: Boolean,
    is24Hour: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val s = currentStrings
    val duration = interval.durationMinutes
    val eligibleAlone = !hardBlock && duration > SettingsManager.MIN_PERIOD_FOR_UNLOCK_MINUTES

    Card(
        onClick = onEdit,
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveNow) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (eligibleAlone) Icons.Rounded.LockOpen else Icons.Rounded.LockClock,
                contentDescription = null,
                tint = if (eligibleAlone) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.tertiary
                },
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val rangeText =
                        "${formatMinuteOfDay(interval.startMinute, is24Hour)} – " +
                            formatMinuteOfDay(interval.endMinute, is24Hour)
                    Text(
                        text = rangeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isActiveNow) {
                        StatusBadge(
                            label = s.activeBadge,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                val ruleTag = when {
                    hardBlock -> s.intervalHardBlockTag
                    eligibleAlone -> s.intervalUnlockEligibleTag
                    else -> s.intervalShortHardBlockTag
                }
                val overnightTag = if (interval.crossesMidnight) s.overnightTag else ""
                Text(
                    text = "${s.formatDuration(duration)}$overnightTag · $ruleTag",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = s.deletePeriodContentDesc,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun StatusBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
