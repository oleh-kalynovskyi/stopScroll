package com.example.scrollstop.ui

import android.content.SharedPreferences
import android.provider.Settings
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.scrollstop.AddIntervalResult
import com.example.scrollstop.LocalAppStrings
import com.example.scrollstop.MINUTES_PER_DAY
import com.example.scrollstop.ScheduleMode
import com.example.scrollstop.SettingsManager
import com.example.scrollstop.applyAppLanguage
import com.example.scrollstop.currentStrings
import com.example.scrollstop.formatClockTime
import com.example.scrollstop.formatMinuteOfDay
import com.example.scrollstop.isBlockerServiceEnabled
import com.example.scrollstop.openAccessibilitySettings
import com.example.scrollstop.openOverlaySettings
import com.example.scrollstop.stringsFor
import com.example.scrollstop.toLocalDateTime
import com.example.scrollstop.ui.components.EmptyIntervalsCard
import com.example.scrollstop.ui.components.HourlyQuotaCard
import com.example.scrollstop.ui.components.IntervalRow
import com.example.scrollstop.ui.components.IntervalsHeader
import com.example.scrollstop.ui.components.LanguageSelector
import com.example.scrollstop.ui.components.MasterToggleCard
import com.example.scrollstop.ui.components.PermissionsCard
import com.example.scrollstop.ui.components.ScheduleCard
import com.example.scrollstop.ui.components.TimePickerDialog
import kotlinx.coroutines.delay

private enum class PickerStep { NONE, START, END }

private enum class IntervalError { SAME_TIME, DUPLICATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusGuardScreen(settings: SettingsManager) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val is24Hour = remember(configuration) { DateFormat.is24HourFormat(context) }

    var enabled by remember { mutableStateOf(settings.isEnabled) }
    var selectedApps by remember { mutableStateOf(settings.getSelectedApps()) }
    var showMinOneAppHint by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(settings.scheduleMode) }
    var hardBlock by remember { mutableStateOf(settings.isHardBlock) }
    var intervals by remember { mutableStateOf(settings.getIntervals()) }
    var language by remember { mutableStateOf(settings.appLanguage) }

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var accessibilityGranted by remember { mutableStateOf(context.isBlockerServiceEnabled()) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun refreshFromSettings() {
        enabled = settings.isEnabled
        selectedApps = settings.getSelectedApps()
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
    var editingIntervalId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingStartMinute by rememberSaveable { mutableIntStateOf(9 * 60) }
    var pendingEndMinute by rememberSaveable { mutableIntStateOf(12 * 60) }
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
                            selectedApps = selectedApps,
                            showMinOneAppHint = showMinOneAppHint,
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
                            onAppToggle = { app, selected ->
                                val updated = settings.setAppSelected(app, selected)
                                showMinOneAppHint = !updated
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
                                    editingIntervalId = null
                                    pendingStartMinute = 9 * 60
                                    pickerStep = PickerStep.START
                                },
                            )
                        }

                        if (intervals.isEmpty()) {
                            item(key = "intervals-empty") {
                                EmptyIntervalsCard(
                                    onAddClick = {
                                        intervalError = null
                                        editingIntervalId = null
                                        pendingStartMinute = 9 * 60
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
                                    onEdit = {
                                        intervalError = null
                                        editingIntervalId = interval.id
                                        pendingStartMinute = interval.startMinute
                                        pendingEndMinute = interval.endMinute
                                        pickerStep = PickerStep.START
                                    },
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
                initialHour = pendingStartMinute / 60,
                initialMinute = pendingStartMinute % 60,
                is24Hour = is24Hour,
                onDismiss = {
                    pickerStep = PickerStep.NONE
                    editingIntervalId = null
                },
                onConfirm = { hour, minute ->
                    val newStart = hour * 60 + minute
                    if (editingIntervalId == null) {
                        pendingEndMinute = (newStart + 180) % MINUTES_PER_DAY
                    }
                    pendingStartMinute = newStart
                    pickerStep = PickerStep.END
                },
            )

            PickerStep.END -> {
                TimePickerDialog(
                    title = s.pickerEndTimeTitle(formatMinuteOfDay(pendingStartMinute, is24Hour)),
                    confirmLabel = if (editingIntervalId != null) s.pickerSaveButton else s.pickerAddButton,
                    initialHour = pendingEndMinute / 60,
                    initialMinute = pendingEndMinute % 60,
                    is24Hour = is24Hour,
                    onDismiss = {
                        pickerStep = PickerStep.NONE
                        editingIntervalId = null
                    },
                    onConfirm = { hour, minute ->
                        val endMinute = hour * 60 + minute
                        val editId = editingIntervalId
                        val result = if (editId != null) {
                            settings.updateInterval(editId, pendingStartMinute, endMinute)
                        } else {
                            settings.addInterval(pendingStartMinute, endMinute)
                        }
                        when (result) {
                            AddIntervalResult.ADDED -> {
                                intervalError = null
                                pickerStep = PickerStep.NONE
                                editingIntervalId = null
                                refreshFromSettings()
                            }

                            AddIntervalResult.INVALID_SAME_TIME -> {
                                intervalError = IntervalError.SAME_TIME
                                pickerStep = PickerStep.NONE
                                editingIntervalId = null
                            }

                            AddIntervalResult.DUPLICATE -> {
                                intervalError = IntervalError.DUPLICATE
                                pickerStep = PickerStep.NONE
                                editingIntervalId = null
                            }
                        }
                    },
                )
            }

            PickerStep.NONE -> Unit
        }
    }
}
