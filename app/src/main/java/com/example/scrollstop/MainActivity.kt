package com.example.scrollstop

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.scrollstop.ui.theme.FocusGuardTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsManager.getInstance(newBase).appLanguage
        super.attachBaseContext(newBase.wrapWithAppLocale(lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = SettingsManager.getInstance(applicationContext)
        setContent {
            FocusGuardTheme {
                FocusGuardScreen(settings = settings)
            }
        }
    }
}

private enum class PickerStep { NONE, START, END }

private enum class IntervalError { SAME_TIME, DUPLICATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FocusGuardScreen(settings: SettingsManager) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val is24Hour = remember(configuration) { DateFormat.is24HourFormat(context) }

    var enabled by remember { mutableStateOf(settings.isEnabled) }
    var mode by remember { mutableStateOf(settings.scheduleMode) }
    var hardBlock by remember { mutableStateOf(settings.isHardBlock) }
    var intervals by remember { mutableStateOf(settings.getIntervals()) }
    var language by remember { mutableStateOf(settings.appLanguage) }

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var accessibilityGranted by remember { mutableStateOf(context.isBlockerServiceEnabled()) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun refreshFromSettings() {
        enabled = settings.isEnabled
        mode = settings.scheduleMode
        hardBlock = settings.isHardBlock
        intervals = settings.getIntervals()
        language = settings.appLanguage
        nowMillis = System.currentTimeMillis()
    }

    // Keep the UI synced with changes made from LockScreenActivity or the service.
    DisposableEffect(settings) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            refreshFromSettings()
        }
        settings.registerListener(listener)
        onDispose { settings.unregisterListener(listener) }
    }

    // Re-check permissions when the user comes back from system Settings.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = Settings.canDrawOverlays(context)
                accessibilityGranted = context.isBlockerServiceEnabled()
                refreshFromSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            nowMillis = System.currentTimeMillis()
            overlayGranted = Settings.canDrawOverlays(context)
            accessibilityGranted = context.isBlockerServiceEnabled()
        }
    }

    // Re-evaluate strings whenever the chosen language or the phone's system configuration changes.
    val strings = remember(language, configuration) { stringsFor(language) }

    val blockState = remember(nowMillis, enabled, mode, hardBlock, intervals) {
        settings.evaluate(nowMillis.toLocalDateTime())
    }
    val unlockActive = blockState.unlockEligible && settings.isUnlockActive(nowMillis)
    val remainingQuota = remember(nowMillis, enabled, mode, hardBlock) {
        settings.remainingQuotaMinutes(nowMillis)
    }

    var pickerStep by rememberSaveable { mutableStateOf(PickerStep.NONE) }
    var pendingStartMinute by rememberSaveable { mutableIntStateOf(9 * 60) }
    var intervalError by rememberSaveable { mutableStateOf<IntervalError?>(null) }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        val s = currentStrings
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("FocusGuard", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = s.appSubtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        LanguageSelector(
                            selectedLanguage = language,
                            onLanguageSelect = { newLang ->
                                settings.appLanguage = newLang
                                language = newLang
                                context.applyAppLanguage(newLang)
                            },
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item(key = "permissions") {
                        PermissionsCard(
                            overlayGranted = overlayGranted,
                            accessibilityGranted = accessibilityGranted,
                            onOpenOverlaySettings = { context.openOverlaySettings() },
                            onOpenAccessibilitySettings = { context.openAccessibilitySettings() },
                        )
                    }

                    item(key = "master") {
                        MasterToggleCard(
                            enabled = enabled,
                            accessibilityGranted = accessibilityGranted,
                            blockState = blockState,
                            unlockActive = unlockActive,
                            unlockUntilMillis = settings.unlockUntilMillis,
                            nowMillis = nowMillis,
                            is24Hour = is24Hour,
                            onToggle = {
                                settings.isEnabled = it
                                refreshFromSettings()
                            },
                            onEndUnlock = {
                                settings.clearUnlock()
                                refreshFromSettings()
                            },
                        )
                    }

                    item(key = "schedule") {
                        ScheduleCard(
                            mode = mode,
                            hardBlock = hardBlock,
                            intervalsCount = intervals.size,
                            onModeChange = {
                                settings.scheduleMode = it
                                refreshFromSettings()
                            },
                            onHardBlockChange = {
                                settings.isHardBlock = it
                                refreshFromSettings()
                            },
                        )
                    }

                    if (mode == ScheduleMode.CUSTOM) {
                        val errorText = when (intervalError) {
                            IntervalError.SAME_TIME -> s.errSameStartEnd
                            IntervalError.DUPLICATE -> s.errDuplicateInterval
                            null -> null
                        }
                        item(key = "intervals-header") {
                            IntervalsHeader(
                                count = intervals.size,
                                message = errorText,
                                onAddClick = {
                                    intervalError = null
                                    pickerStep = PickerStep.START
                                },
                            )
                        }

                        if (intervals.isEmpty()) {
                            item(key = "intervals-empty") {
                                EmptyIntervalsCard(
                                    onAddClick = {
                                        intervalError = null
                                        pickerStep = PickerStep.START
                                    },
                                )
                            }
                        } else {
                            items(intervals, key = { it.id }) { interval ->
                                IntervalRow(
                                    interval = interval,
                                    hardBlock = hardBlock,
                                    isActiveNow = enabled &&
                                        interval.contains(nowMillis.toLocalDateTime().let { it.hour * 60 + it.minute }),
                                    is24Hour = is24Hour,
                                    onDelete = {
                                        settings.removeInterval(interval.id)
                                        refreshFromSettings()
                                    },
                                )
                            }
                        }
                    }

                    item(key = "quota") {
                        HourlyQuotaCard(
                            remainingMinutes = remainingQuota,
                            nextResetTime = formatClockTime(
                                settings.nextHourStartMillis(nowMillis),
                                is24Hour,
                            ),
                        )
                    }

                    item(key = "bottom-space") { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        when (pickerStep) {
            PickerStep.START -> TimePickerDialog(
                title = s.pickerStartTimeTitle,
                confirmLabel = s.pickerNextButton,
                initialHour = 9,
                initialMinute = 0,
                is24Hour = is24Hour,
                onDismiss = { pickerStep = PickerStep.NONE },
                onConfirm = { hour, minute ->
                    pendingStartMinute = hour * 60 + minute
                    pickerStep = PickerStep.END
                },
            )

            PickerStep.END -> {
                val defaultEnd = (pendingStartMinute + 180) % MINUTES_PER_DAY
                TimePickerDialog(
                    title = s.pickerEndTimeTitle(formatMinuteOfDay(pendingStartMinute, is24Hour)),
                    confirmLabel = s.pickerAddButton,
                    initialHour = defaultEnd / 60,
                    initialMinute = defaultEnd % 60,
                    is24Hour = is24Hour,
                    onDismiss = { pickerStep = PickerStep.NONE },
                    onConfirm = { hour, minute ->
                        val endMinute = hour * 60 + minute
                        when (settings.addInterval(pendingStartMinute, endMinute)) {
                            AddIntervalResult.ADDED -> {
                                intervalError = null
                                pickerStep = PickerStep.NONE
                                refreshFromSettings()
                            }

                            AddIntervalResult.INVALID_SAME_TIME -> {
                                intervalError = IntervalError.SAME_TIME
                                pickerStep = PickerStep.NONE
                            }

                            AddIntervalResult.DUPLICATE -> {
                                intervalError = IntervalError.DUPLICATE
                                pickerStep = PickerStep.NONE
                            }
                        }
                    },
                )
            }

            PickerStep.NONE -> Unit
        }
    }
}

// region Language selector

@Composable
private fun LanguageSelector(
    selectedLanguage: AppLanguage,
    onLanguageSelect: (AppLanguage) -> Unit,
) {
    val s = currentStrings
    var expanded by remember { mutableStateOf(false) }
    val activeShortCode = if (s.isUkrainian) "UA" else "EN"
    val chipLabel = when (selectedLanguage) {
        AppLanguage.SYSTEM -> "${selectedLanguage.shortLabel} ($activeShortCode)"
        AppLanguage.EN -> AppLanguage.EN.shortLabel
        AppLanguage.UK -> AppLanguage.UK.shortLabel
    }

    Box(modifier = Modifier.padding(end = 8.dp)) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(chipLabel) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = s.languageMenuTitle,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            LanguageMenuItem(
                label = "${s.languageSystem} ($activeShortCode)",
                selected = selectedLanguage == AppLanguage.SYSTEM,
                onClick = {
                    expanded = false
                    onLanguageSelect(AppLanguage.SYSTEM)
                },
            )
            LanguageMenuItem(
                label = "${s.languageEnglish} (EN)",
                selected = selectedLanguage == AppLanguage.EN,
                onClick = {
                    expanded = false
                    onLanguageSelect(AppLanguage.EN)
                },
            )
            LanguageMenuItem(
                label = "${s.languageUkrainian} (UA)",
                selected = selectedLanguage == AppLanguage.UK,
                onClick = {
                    expanded = false
                    onLanguageSelect(AppLanguage.UK)
                },
            )
        }
    }
}

@Composable
private fun LanguageMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
        onClick = onClick,
    )
}

// endregion

// region Cards

@Composable
private fun PermissionsCard(
    overlayGranted: Boolean,
    accessibilityGranted: Boolean,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val s = currentStrings
    val allGranted = overlayGranted && accessibilityGranted
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allGranted) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = if (allGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = s.permissionsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (allGranted) {
                            s.permissionsAllActive
                        } else {
                            s.permissionsGrantBoth
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PermissionRow(
                title = s.permOverlayTitle,
                subtitle = s.permOverlaySubtitle,
                granted = overlayGranted,
                onAction = onOpenOverlaySettings,
            )

            PermissionRow(
                title = s.permAccessibilityTitle,
                subtitle = s.permAccessibilitySubtitle,
                granted = accessibilityGranted,
                onAction = onOpenAccessibilitySettings,
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onAction: () -> Unit,
) {
    val s = currentStrings
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (granted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (granted) {
            TextButton(onClick = onAction) { Text(s.actionSettings) }
        } else {
            FilledTonalButton(onClick = onAction) { Text(s.actionGrant) }
        }
    }
}

@Composable
private fun MasterToggleCard(
    enabled: Boolean,
    accessibilityGranted: Boolean,
    blockState: BlockState,
    unlockActive: Boolean,
    unlockUntilMillis: Long,
    nowMillis: Long,
    is24Hour: Boolean,
    onToggle: (Boolean) -> Unit,
    onEndUnlock: () -> Unit,
) {
    val s = currentStrings
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
            s.statusHardBlockingNow(end)
        }

        blockState.isBlocking -> {
            val end = blockState.endMinuteOfDay
                ?.let { s.untilTime(formatMinuteOfDay(it, is24Hour)) }
                ?: s.allDaySuffix
            s.statusBlockingNow(end)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleCard(
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
private fun IntervalsHeader(
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
private fun EmptyIntervalsCard(onAddClick: () -> Unit) {
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
private fun IntervalRow(
    interval: TimeInterval,
    hardBlock: Boolean,
    isActiveNow: Boolean,
    is24Hour: Boolean,
    onDelete: () -> Unit,
) {
    val s = currentStrings
    val duration = interval.durationMinutes
    val eligibleAlone = !hardBlock && duration > SettingsManager.MIN_PERIOD_FOR_UNLOCK_MINUTES

    Card(
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
private fun HourlyQuotaCard(
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

@Composable
private fun StatusBadge(
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

// endregion

// region Material 3 TimePicker dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    confirmLabel: String,
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val s = currentStrings
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = is24Hour,
    )
    var useKeyboardInput by rememberSaveable { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                )

                if (useKeyboardInput) {
                    TimeInput(state = state)
                } else {
                    TimePicker(state = state)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { useKeyboardInput = !useKeyboardInput }) {
                        Icon(
                            imageVector = if (useKeyboardInput) {
                                Icons.Outlined.AccessTime
                            } else {
                                Icons.Outlined.Keyboard
                            },
                            contentDescription = s.pickerToggleInputDesc,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(s.pickerCancelButton) }
                    Spacer(Modifier.width(4.dp))
                    Button(onClick = { onConfirm(state.hour, state.minute) }) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

// endregion

// region System permission helpers

private fun Context.isBlockerServiceEnabled(): Boolean {
    val expected = ComponentName(this, AppBlockerService::class.java)

    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val enabledInManager = am
        ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        ?.any { info ->
            val si = info.resolveInfo?.serviceInfo ?: return@any false
            ComponentName(si.packageName, si.name) == expected
        } ?: false
    if (enabledInManager) return true

    val setting = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return setting.split(':').any { ComponentName.unflattenFromString(it) == expected }
}

private fun Context.openOverlaySettings() {
    val specific = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(specific)
    } catch (e: Exception) {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun Context.openAccessibilitySettings() {
    startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

// endregion
