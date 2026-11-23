package com.example.scrollstop

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.scrollstop.ui.theme.FocusGuardTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Full-screen block shown on top of Instagram.
 * - Hard block (or period <= 2h): only "Exit to Home Screen".
 * - Soft block: 10-second mindful pause, then a 1..N minute unlock within the hourly quota.
 */
class LockScreenActivity : ComponentActivity() {

    private lateinit var settings: SettingsManager

    override fun attachBaseContext(newBase: Context) {
        val lang = SettingsManager.getInstance(newBase).appLanguage
        super.attachBaseContext(newBase.wrapWithAppLocale(lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settings = SettingsManager.getInstance(applicationContext)

        // Back must never reveal Instagram underneath: send the user home instead.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitToHome()
            }
        })

        setContent {
            FocusGuardTheme {
                LockScreen(
                    settings = settings,
                    onExitHome = { exitToHome() },
                    onUnlock = { minutes -> unlock(minutes) },
                    onBlockLifted = { finish() },
                )
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // User pressed Home / switched apps: start fresh (new friction pause) next time.
        if (!isFinishing) finish()
    }

    private fun unlock(minutes: Int) {
        if (settings.startUnlock(minutes)) {
            // Finishing returns to Instagram's task; the service then applies the gray overlay.
            finish()
        }
    }

    private fun exitToHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(home)
        finish()
    }
}

@Composable
private fun LockScreen(
    settings: SettingsManager,
    onExitHome: () -> Unit,
    onUnlock: (Int) -> Unit,
    onBlockLifted: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val is24Hour = remember(configuration) { DateFormat.is24HourFormat(context) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            nowMillis = System.currentTimeMillis()
        }
    }

    val language = remember(nowMillis) { settings.appLanguage }
    val strings = remember(language, configuration) { stringsFor(language) }

    val state = remember(nowMillis) { settings.evaluate(nowMillis.toLocalDateTime()) }
    val unlockActive = state.unlockEligible && settings.isUnlockActive(nowMillis)

    // Block period ended (or an unlock is already running): nothing to guard anymore.
    LaunchedEffect(state.isBlocking, unlockActive) {
        if (!state.isBlocking || unlockActive) onBlockLifted()
    }

    var secondsLeft by rememberSaveable { mutableIntStateOf(SettingsManager.FRICTION_SECONDS) }
    if (state.unlockEligible) {
        LaunchedEffect(Unit) {
            while (secondsLeft > 0) {
                delay(1_000)
                secondsLeft--
            }
        }
    }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        val s = currentStrings
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LockBadge()

                    Text(
                        text = s.lockTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )

                    when {
                        !state.unlockEligible -> HardBlockContent(state = state, is24Hour = is24Hour)
                        secondsLeft > 0 -> FrictionCountdown(secondsLeft = secondsLeft)
                        else -> {
                            val remaining = remember(nowMillis) { settings.remainingQuotaMinutes(nowMillis) }
                            val resetsAt = formatClockTime(settings.nextHourStartMillis(nowMillis), is24Hour)
                            if (remaining > 0) {
                                UnlockSelector(
                                    remainingMinutes = remaining,
                                    resetsAt = resetsAt,
                                    onUnlock = onUnlock,
                                )
                            } else {
                                QuotaExhausted(resetsAt = resetsAt)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = onExitHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(s.lockExitHomeButton)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockBadge() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp),
        )
    }
}

@Composable
private fun HardBlockContent(state: BlockState, is24Hour: Boolean) {
    val s = currentStrings
    val reason = if (state.hardBlockEnabled) {
        s.lockHardBlockEnabledReason
    } else {
        s.lockShortPeriodReason(s.formatDuration(state.periodMinutes))
    }
    val ends = state.endMinuteOfDay
        ?.let { s.lockLiftsAt(formatMinuteOfDay(it, is24Hour)) }
        ?: s.lockRunsAllDay

    Text(
        text = reason,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Text(
        text = ends,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        text = s.lockChangeInAppHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun FrictionCountdown(secondsLeft: Int) {
    val s = currentStrings
    val progress by animateFloatAsState(
        targetValue = secondsLeft / SettingsManager.FRICTION_SECONDS.toFloat(),
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "friction-progress",
    )
    Text(
        text = s.lockFrictionPrompt,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 6.dp,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Text(
            text = "$secondsLeft",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Light,
        )
    }
}

@Composable
private fun UnlockSelector(
    remainingMinutes: Int,
    resetsAt: String,
    onUnlock: (Int) -> Unit,
) {
    val s = currentStrings
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val minutes = if (selected in 1..remainingMinutes) selected else remainingMinutes

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = s.lockUnlockForLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = s.lockMinutesValue(minutes),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Medium,
        )
        if (remainingMinutes > 1) {
            Slider(
                value = minutes.toFloat(),
                onValueChange = { selected = it.roundToInt().coerceIn(1, remainingMinutes) },
                valueRange = 1f..remainingMinutes.toFloat(),
                steps = (remainingMinutes - 2).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(s.lockMinutesValue(1), style = MaterialTheme.typography.labelSmall)
                Text(s.lockMinutesValue(remainingMinutes), style = MaterialTheme.typography.labelSmall)
            }
        }
        Text(
            text = s.lockQuotaRemainingSub(
                remainingMinutes,
                SettingsManager.QUOTA_MINUTES_PER_HOUR,
                resetsAt,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedButton(
            onClick = { onUnlock(minutes) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Icon(Icons.Rounded.LockOpen, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(s.lockUnlockButton)
        }
        Text(
            text = s.lockGrayScreenHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuotaExhausted(resetsAt: String) {
    val s = currentStrings
    Icon(
        imageVector = Icons.Rounded.HourglassTop,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.size(40.dp),
    )
    Text(
        text = s.lockQuotaExhaustedTitle,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
    Text(
        text = s.lockNextWindowOpensAt(resetsAt),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}
