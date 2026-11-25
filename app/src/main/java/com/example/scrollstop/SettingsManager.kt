package com.example.scrollstop

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

const val MINUTES_PER_DAY = 24 * 60

/**
 * A blocking interval expressed in minutes from midnight (0..1439). If [endMinute] <= [startMinute]
 * the interval crosses midnight (e.g. 22:00 - 02:00).
 */
data class TimeInterval(
        val id: String = UUID.randomUUID().toString(),
        val startMinute: Int,
        val endMinute: Int,
) {
    val crossesMidnight: Boolean
        get() = endMinute <= startMinute

    val durationMinutes: Int
        get() =
                if (endMinute > startMinute) {
                    endMinute - startMinute
                } else {
                    MINUTES_PER_DAY - startMinute + endMinute
                }

    fun contains(minuteOfDay: Int): Boolean =
            if (!crossesMidnight) {
                minuteOfDay in startMinute until endMinute
            } else {
                minuteOfDay >= startMinute || minuteOfDay < endMinute
            }
}

enum class ScheduleMode {
    ALL_DAY,
    CUSTOM,
    SESSION_LIMIT
}

enum class SessionTickResult {
    ACTIVE,
    SHOW_WARNING,
    COOLDOWN_STARTED,
    COOLDOWN_ACTIVE
}

data class SessionStatus(
        val limitMinutes: Int,
        val usedMillis: Long,
        val remainingMillis: Long,
        val cooldownUntilMillis: Long,
        val isCooldownActive: Boolean,
        val resetsAtMillis: Long? = null,
)

enum class AddIntervalResult {
    ADDED,
    INVALID_SAME_TIME,
    DUPLICATE
}

/** Supported short-video / social apps guarded by FocusGuard under a shared schedule and quota. */
enum class TargetApp(
        val displayName: String,
        val packages: Set<String>,
) {
    INSTAGRAM(
            displayName = "Instagram",
            packages = setOf("com.instagram.android"),
    ),
    TIKTOK(
            displayName = "TikTok",
            packages =
                    setOf(
                            "com.zhiliaoapp.musically",
                            "com.ss.android.ugc.trill",
                            "com.zhiliaoapp.musically.go",
                    ),
    ),
    X_TWITTER(
            displayName = "X (Twitter)",
            packages =
                    setOf(
                            "com.twitter.android",
                            "com.twitter.android.lite",
                    ),
    ),
    SNAPCHAT(
            displayName = "Snapchat",
            packages =
                    setOf(
                            "com.snapchat.android",
                    ),
    ),
    THREADS(
            displayName = "Threads",
            packages =
                    setOf(
                            "com.instagram.barcelona",
                    ),
    );

    companion object {
        fun fromPackage(pkg: String?): TargetApp? =
                if (pkg == null) null else entries.firstOrNull { pkg in it.packages }
    }
}

/**
 * Result of evaluating the schedule at a given moment.
 *
 * @param isBlocking Instagram must be blocked right now.
 * @param unlockEligible The 15-min/hour temporary unlock may be offered.
 * @param hardBlockEnabled The user enabled "Hard Block".
 * @param coversWholeDay All Day mode, or custom periods that cover all 24h.
 * @param periodMinutes Length of the continuous active block (merged adjacent/overlapping periods).
 * @param endMinuteOfDay Minute of day when the current block ends (null if it never ends).
 */
data class BlockState(
        val isBlocking: Boolean,
        val unlockEligible: Boolean,
        val hardBlockEnabled: Boolean,
        val coversWholeDay: Boolean,
        val periodMinutes: Int,
        val endMinuteOfDay: Int?,
        val isSessionCooldown: Boolean = false,
        val cooldownUntilMillis: Long? = null,
) {
    companion object {
        val NOT_BLOCKING =
                BlockState(
                        isBlocking = false,
                        unlockEligible = false,
                        hardBlockEnabled = false,
                        coversWholeDay = false,
                        periodMinutes = 0,
                        endMinuteOfDay = null,
                )
    }
}

/**
 * Single source of truth for FocusGuard settings, schedule evaluation and the hourly unlock quota.
 * Backed by SharedPreferences + Gson.
 */
class SettingsManager
internal constructor(
        private val prefs: SharedPreferences,
        private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
) {

    private constructor(
            context: Context
    ) : this(
            prefs =
                    context.applicationContext.getSharedPreferences(
                            PREFS_NAME,
                            Context.MODE_PRIVATE
                    ),
    )

    private val gson = Gson()
    private val intervalListType = object : TypeToken<List<TimeInterval>>() {}.type
    private val usageMapType = object : TypeToken<Map<String, Long>>() {}.type

    // region Basic settings

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) =
                prefs.edit {
                    putBoolean(KEY_ENABLED, value)
                    if (!value) {
                        putLong(KEY_UNLOCK_UNTIL, 0L)
                        putLong(KEY_SESSION_USED_MS, 0L)
                        putLong(KEY_SESSION_LAST_ACTIVITY_MS, 0L)
                        putLong(KEY_SESSION_COOLDOWN_UNTIL, 0L)
                        putBoolean(KEY_SESSION_WARNED, false)
                    }
                }

    var scheduleMode: ScheduleMode
        get() =
                prefs.getString(KEY_MODE, null)?.let { name ->
                    ScheduleMode.entries.firstOrNull { it.name == name }
                }
                        ?: ScheduleMode.ALL_DAY
        set(value) = prefs.edit { putString(KEY_MODE, value.name) }

    var sessionLimitMinutes: Int
        get() =
                prefs.getInt(KEY_SESSION_LIMIT_MINUTES, DEFAULT_SESSION_LIMIT_MINUTES).takeIf {
                    it in SESSION_LIMIT_OPTIONS
                }
                        ?: DEFAULT_SESSION_LIMIT_MINUTES
        set(value) {
            val sanitized =
                    value.takeIf { it in SESSION_LIMIT_OPTIONS } ?: DEFAULT_SESSION_LIMIT_MINUTES
            prefs.edit { putInt(KEY_SESSION_LIMIT_MINUTES, sanitized) }
        }

    var isHardBlock: Boolean
        get() = prefs.getBoolean(KEY_HARD_BLOCK, false)
        set(value) =
                prefs.edit {
                    putBoolean(KEY_HARD_BLOCK, value)
                    // Turning Hard Block on immediately ends any running temporary unlock.
                    if (value) putLong(KEY_UNLOCK_UNTIL, 0L)
                }

    var appLanguage: AppLanguage
        get() =
                prefs.getString(KEY_LANGUAGE, null)?.let { name ->
                    AppLanguage.entries.firstOrNull { it.name == name }
                }
                        ?: AppLanguage.SYSTEM
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value.name) }

    fun isAppSelected(app: TargetApp): Boolean = prefs.getBoolean(appSelectionKey(app), true)

    fun getSelectedApps(): Set<TargetApp> {
        val selected = TargetApp.entries.filter { isAppSelected(it) }.toSet()
        return selected.ifEmpty { setOf(TargetApp.INSTAGRAM) }
    }

    /**
     * Enables or disables blocking for [app]. At least one app must remain selected (either both
     * apps or one of them).
     *
     * @return true if the selection was updated, false if blocked because [app] is the last active
     * app.
     */
    fun setAppSelected(app: TargetApp, selected: Boolean): Boolean {
        if (!selected) {
            val othersSelected = TargetApp.entries.any { it != app && isAppSelected(it) }
            if (!othersSelected) return false
        }
        prefs.edit { putBoolean(appSelectionKey(app), selected) }
        return true
    }

    private fun appSelectionKey(app: TargetApp): String = "$KEY_APP_SELECTED_PREFIX${app.name}"

    // endregion

    // region Intervals

    fun getIntervals(): List<TimeInterval> {
        val json = prefs.getString(KEY_INTERVALS, null) ?: return emptyList()
        val parsed: List<TimeInterval> =
                try {
                    gson.fromJson<List<TimeInterval>>(json, intervalListType) ?: emptyList()
                } catch (e: RuntimeException) {
                    emptyList()
                }
        return parsed
                .filter { it.isValid() }
                .sortedWith(compareBy<TimeInterval> { it.startMinute }.thenBy { it.endMinute })
    }

    fun addInterval(startMinute: Int, endMinute: Int): AddIntervalResult {
        if (startMinute == endMinute) return AddIntervalResult.INVALID_SAME_TIME
        val current = getIntervals()
        if (current.any { it.startMinute == startMinute && it.endMinute == endMinute }) {
            return AddIntervalResult.DUPLICATE
        }
        saveIntervals(current + TimeInterval(startMinute = startMinute, endMinute = endMinute))
        return AddIntervalResult.ADDED
    }

    fun updateInterval(id: String, startMinute: Int, endMinute: Int): AddIntervalResult {
        if (startMinute == endMinute) return AddIntervalResult.INVALID_SAME_TIME
        val current = getIntervals()
        if (current.any {
                    it.id != id && it.startMinute == startMinute && it.endMinute == endMinute
                }
        ) {
            return AddIntervalResult.DUPLICATE
        }
        val updated =
                current.map {
                    if (it.id == id) it.copy(startMinute = startMinute, endMinute = endMinute)
                    else it
                }
        saveIntervals(updated)
        return AddIntervalResult.ADDED
    }

    fun removeInterval(id: String) {
        saveIntervals(getIntervals().filterNot { it.id == id })
    }

    private fun saveIntervals(intervals: List<TimeInterval>) {
        prefs.edit { putString(KEY_INTERVALS, gson.toJson(intervals, intervalListType)) }
    }

    private fun TimeInterval.isValid(): Boolean =
            startMinute in 0 until MINUTES_PER_DAY &&
                    endMinute in 0 until MINUTES_PER_DAY &&
                    startMinute != endMinute

    // endregion

    // region Schedule evaluation

    fun evaluate(
            now: LocalDateTime = LocalDateTime.now(),
            nowMillis: Long = System.currentTimeMillis(),
    ): BlockState {
        if (!isEnabled) return BlockState.NOT_BLOCKING
        val hardBlock = isHardBlock

        if (scheduleMode == ScheduleMode.SESSION_LIMIT) {
            val cooldownUntil = activeSessionCooldownUntil(nowMillis)
            if (cooldownUntil != null) {
                val endLocal = cooldownUntil.toLocalDateTime(zoneProvider())
                return BlockState(
                        isBlocking = true,
                        unlockEligible = false,
                        hardBlockEnabled = true,
                        coversWholeDay = false,
                        periodMinutes = SESSION_COOLDOWN_MINUTES,
                        endMinuteOfDay = endLocal.hour * 60 + endLocal.minute,
                        isSessionCooldown = true,
                        cooldownUntilMillis = cooldownUntil,
                )
            }
            return BlockState.NOT_BLOCKING
        }

        if (scheduleMode == ScheduleMode.ALL_DAY) {
            return wholeDayState(hardBlock)
        }

        val intervals = getIntervals()
        if (intervals.isEmpty()) return BlockState.NOT_BLOCKING

        // Minute-level coverage map; merges adjacent/overlapping periods and handles midnight.
        val covered = BooleanArray(MINUTES_PER_DAY)
        intervals.forEach { interval ->
            var minute = interval.startMinute
            while (minute != interval.endMinute) {
                covered[minute] = true
                minute = (minute + 1) % MINUTES_PER_DAY
            }
        }

        val nowMinute = now.hour * 60 + now.minute
        if (!covered[nowMinute]) return BlockState.NOT_BLOCKING
        if (covered.all { it }) return wholeDayState(hardBlock)

        // Walk backwards to the first covered minute of the current continuous block.
        var start = nowMinute
        while (covered[(start - 1 + MINUTES_PER_DAY) % MINUTES_PER_DAY]) {
            start = (start - 1 + MINUTES_PER_DAY) % MINUTES_PER_DAY
        }
        // Walk forwards to the first uncovered minute (exclusive end).
        var end = nowMinute
        while (covered[end]) {
            end = (end + 1) % MINUTES_PER_DAY
        }
        val length = (end - start + MINUTES_PER_DAY) % MINUTES_PER_DAY

        return BlockState(
                isBlocking = true,
                unlockEligible = !hardBlock && length > MIN_PERIOD_FOR_UNLOCK_MINUTES,
                hardBlockEnabled = hardBlock,
                coversWholeDay = false,
                periodMinutes = length,
                endMinuteOfDay = end,
        )
    }

    private fun wholeDayState(hardBlock: Boolean) =
            BlockState(
                    isBlocking = true,
                    unlockEligible = !hardBlock,
                    hardBlockEnabled = hardBlock,
                    coversWholeDay = true,
                    periodMinutes = MINUTES_PER_DAY,
                    endMinuteOfDay = null,
            )

    // endregion

    // region Temporary unlock + hourly quota

    val unlockUntilMillis: Long
        get() = prefs.getLong(KEY_UNLOCK_UNTIL, 0L)

    fun isUnlockActive(nowMillis: Long = System.currentTimeMillis()): Boolean =
            unlockUntilMillis > nowMillis

    fun clearUnlock() {
        prefs.edit { putLong(KEY_UNLOCK_UNTIL, 0L) }
    }

    fun usedMillisInCurrentHour(nowMillis: Long = System.currentTimeMillis()): Long =
            loadUsage()[hourStartMillis(nowMillis).toString()] ?: 0L

    fun remainingQuotaMinutes(nowMillis: Long = System.currentTimeMillis()): Int {
        val remainingMs =
                (QUOTA_MILLIS_PER_HOUR - usedMillisInCurrentHour(nowMillis)).coerceAtLeast(0L)
        return (remainingMs / MILLIS_PER_MINUTE).toInt()
    }

    fun nextHourStartMillis(nowMillis: Long = System.currentTimeMillis()): Long =
            hourStart(nowMillis).plusHours(1).toInstant().toEpochMilli()

    /**
     * Starts a temporary unlock. Usage is charged as wall-clock time and split across calendar
     * hours, so an unlock started at 14:55 for 15 min charges 5 min to the 14:00 hour and 10 min to
     * the 15:00 hour.
     *
     * @return true if the unlock was granted.
     */
    fun startUnlock(minutes: Int, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val remaining = remainingQuotaMinutes(nowMillis)
        if (remaining <= 0) return false
        val durationMs = minutes.coerceIn(1, remaining) * MILLIS_PER_MINUTE

        val currentHourStart = hourStartMillis(nowMillis)
        val usage =
                loadUsage()
                        .filterKeys { (it.toLongOrNull() ?: 0L) >= currentHourStart }
                        .toMutableMap()

        var cursor = nowMillis
        var left = durationMs
        while (left > 0) {
            val hourStart = hourStartMillis(cursor)
            val chunk = minOf(left, nextHourStartMillis(cursor) - cursor)
            val key = hourStart.toString()
            usage[key] = (usage[key] ?: 0L) + chunk
            cursor += chunk
            left -= chunk
        }

        prefs.edit {
            putString(KEY_USAGE, gson.toJson(usage, usageMapType))
            putLong(KEY_UNLOCK_UNTIL, nowMillis + durationMs)
        }
        return true
    }

    private fun loadUsage(): Map<String, Long> {
        val json = prefs.getString(KEY_USAGE, null) ?: return emptyMap()
        return try {
            gson.fromJson<Map<String, Long>>(json, usageMapType) ?: emptyMap()
        } catch (e: RuntimeException) {
            emptyMap()
        }
    }

    private fun hourStart(millis: Long): ZonedDateTime =
            Instant.ofEpochMilli(millis).atZone(zoneProvider()).truncatedTo(ChronoUnit.HOURS)

    private fun hourStartMillis(millis: Long): Long = hourStart(millis).toInstant().toEpochMilli()

    // endregion

    // region Session limit + hourly reset

    val sessionCooldownUntilMillis: Long
        get() = prefs.getLong(KEY_SESSION_COOLDOWN_UNTIL, 0L)

    private fun activeSessionCooldownUntil(nowMillis: Long): Long? {
        val cooldownUntil = sessionCooldownUntilMillis
        if (cooldownUntil <= nowMillis) return null
        val lastActivity = prefs.getLong(KEY_SESSION_LAST_ACTIVITY_MS, 0L)
        val effectiveUntil =
                if (lastActivity > 0L && cooldownUntil - lastActivity >= SESSION_COOLDOWN_MILLIS) {
                    minOf(cooldownUntil, nextHourStartMillis(lastActivity))
                } else {
                    cooldownUntil
                }
        return effectiveUntil.takeIf { it > nowMillis }
    }

    fun isSessionCooldownActive(nowMillis: Long = System.currentTimeMillis()): Boolean =
            activeSessionCooldownUntil(nowMillis) != null

    fun getSessionStatus(nowMillis: Long = System.currentTimeMillis()): SessionStatus {
        val limitMin = sessionLimitMinutes
        val limitMs = limitMin * MILLIS_PER_MINUTE
        val activeCooldownUntil = activeSessionCooldownUntil(nowMillis)

        if (activeCooldownUntil != null) {
            return SessionStatus(
                    limitMinutes = limitMin,
                    usedMillis = limitMs,
                    remainingMillis = 0L,
                    cooldownUntilMillis = activeCooldownUntil,
                    isCooldownActive = true,
            )
        }

        val currentHourStart = hourStartMillis(nowMillis)
        val lastActivity = prefs.getLong(KEY_SESSION_LAST_ACTIVITY_MS, 0L)
        if (lastActivity < currentHourStart) {
            return SessionStatus(
                    limitMinutes = limitMin,
                    usedMillis = 0L,
                    remainingMillis = limitMs,
                    cooldownUntilMillis = 0L,
                    isCooldownActive = false,
            )
        }

        val used = prefs.getLong(KEY_SESSION_USED_MS, 0L).coerceIn(0L, limitMs)
        val resetsAt = if (used > 0L) nextHourStartMillis(nowMillis) else null
        return SessionStatus(
                limitMinutes = limitMin,
                usedMillis = used,
                remainingMillis = (limitMs - used).coerceAtLeast(0L),
                cooldownUntilMillis = 0L,
                isCooldownActive = false,
                resetsAtMillis = resetsAt,
        )
    }

    /**
     * Records active foreground usage [deltaMs] in Session Limit mode. Usage and cooldowns reset at
     * the start of every calendar clock hour (:00). Once the configured allowance is reached,
     * guarded apps are hard-blocked until the next hour.
     */
    fun recordSessionUsage(
            deltaMs: Long,
            nowMillis: Long = System.currentTimeMillis(),
    ): SessionTickResult {
        val limitMs = sessionLimitMinutes * MILLIS_PER_MINUTE
        if (isSessionCooldownActive(nowMillis)) return SessionTickResult.COOLDOWN_ACTIVE

        val currentHourStart = hourStartMillis(nowMillis)
        val lastActivity = prefs.getLong(KEY_SESSION_LAST_ACTIVITY_MS, 0L)
        val shouldReset = lastActivity < currentHourStart

        val currentUsed =
                if (shouldReset) 0L else prefs.getLong(KEY_SESSION_USED_MS, 0L).coerceAtLeast(0L)
        val alreadyWarned = if (shouldReset) false else prefs.getBoolean(KEY_SESSION_WARNED, false)

        val safeDelta = deltaMs.coerceIn(0L, MAX_SESSION_TICK_MS)
        val effectiveDelta =
                if (shouldReset) {
                    minOf(safeDelta, (nowMillis - currentHourStart).coerceAtLeast(0L))
                } else {
                    safeDelta
                }
        val newUsed = currentUsed + effectiveDelta

        if (newUsed >= limitMs) {
            prefs.edit {
                putLong(KEY_SESSION_USED_MS, 0L)
                putLong(KEY_SESSION_LAST_ACTIVITY_MS, nowMillis)
                putLong(KEY_SESSION_COOLDOWN_UNTIL, nextHourStartMillis(nowMillis))
                putBoolean(KEY_SESSION_WARNED, false)
            }
            return SessionTickResult.COOLDOWN_STARTED
        }

        val remaining = limitMs - newUsed
        val shouldWarn =
                !alreadyWarned &&
                        limitMs > SESSION_WARNING_MILLIS &&
                        remaining <= SESSION_WARNING_MILLIS

        prefs.edit {
            putLong(KEY_SESSION_USED_MS, newUsed)
            putLong(KEY_SESSION_LAST_ACTIVITY_MS, nowMillis)
            putLong(KEY_SESSION_COOLDOWN_UNTIL, 0L)
            putBoolean(KEY_SESSION_WARNED, alreadyWarned || shouldWarn)
        }
        return if (shouldWarn) SessionTickResult.SHOW_WARNING else SessionTickResult.ACTIVE
    }

    // endregion

    // region Listeners

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    // endregion

    companion object {
        const val TARGET_PACKAGE = "com.instagram.android"
        const val QUOTA_MINUTES_PER_HOUR = 15
        const val MIN_PERIOD_FOR_UNLOCK_MINUTES = 120
        const val FRICTION_SECONDS = 10
        const val DEFAULT_SESSION_LIMIT_MINUTES = 15
        const val SESSION_COOLDOWN_MINUTES = 60
        const val SESSION_WARNING_MINUTES = 2
        val SESSION_LIMIT_OPTIONS = listOf(5, 10, 15)

        private const val MILLIS_PER_MINUTE = 60_000L
        private const val QUOTA_MILLIS_PER_HOUR = QUOTA_MINUTES_PER_HOUR * MILLIS_PER_MINUTE
        private const val SESSION_COOLDOWN_MILLIS = SESSION_COOLDOWN_MINUTES * MILLIS_PER_MINUTE
        private const val SESSION_WARNING_MILLIS = SESSION_WARNING_MINUTES * MILLIS_PER_MINUTE
        private const val MAX_SESSION_TICK_MS = 10_000L

        private const val PREFS_NAME = "focusguard_settings"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_MODE = "schedule_mode"
        private const val KEY_HARD_BLOCK = "hard_block"
        private const val KEY_INTERVALS = "intervals"
        private const val KEY_USAGE = "hourly_usage"
        private const val KEY_UNLOCK_UNTIL = "unlock_until"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_APP_SELECTED_PREFIX = "app_selected_"
        private const val KEY_SESSION_LIMIT_MINUTES = "session_limit_minutes"
        private const val KEY_SESSION_USED_MS = "session_used_ms"
        private const val KEY_SESSION_LAST_ACTIVITY_MS = "session_last_activity_ms"
        private const val KEY_SESSION_COOLDOWN_UNTIL = "session_cooldown_until"
        private const val KEY_SESSION_WARNED = "session_warned"

        val SESSION_RUNTIME_KEYS =
                setOf(
                        KEY_SESSION_USED_MS,
                        KEY_SESSION_LAST_ACTIVITY_MS,
                        KEY_SESSION_COOLDOWN_UNTIL,
                        KEY_SESSION_WARNED,
                )

        @Volatile private var instance: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager =
                instance
                        ?: synchronized(this) {
                            instance ?: SettingsManager(context).also { instance = it }
                        }
    }
}

// region Formatting helpers

fun Long.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(this), zoneId)

private fun timeFormatter(is24Hour: Boolean): DateTimeFormatter =
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())

fun formatMinuteOfDay(minuteOfDay: Int, is24Hour: Boolean): String {
    val normalized = ((minuteOfDay % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY
    return LocalTime.of(normalized / 60, normalized % 60).format(timeFormatter(is24Hour))
}

fun formatClockTime(
        epochMillis: Long,
        is24Hour: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault(),
): String = epochMillis.toLocalDateTime(zoneId).toLocalTime().format(timeFormatter(is24Hour))

// endregion
