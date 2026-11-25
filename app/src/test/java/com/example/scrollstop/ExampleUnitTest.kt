package com.example.scrollstop

import android.content.SharedPreferences
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Local unit tests for FocusGuard domain logic, session limits, and localization.
 */
class ExampleUnitTest {

    private fun newSettings(): SettingsManager =
        SettingsManager(InMemorySharedPreferences(), zoneProvider = { ZoneOffset.UTC })

    private fun advanceUsage(
        settings: SettingsManager,
        totalMs: Long,
        startMillis: Long,
        stepMs: Long = 5_000L,
    ): List<SessionTickResult> {
        val results = mutableListOf<SessionTickResult>()
        var remaining = totalMs
        var now = startMillis
        while (remaining > 0L) {
            val chunk = minOf(stepMs, remaining)
            now += chunk
            results += settings.recordSessionUsage(chunk, now)
            remaining -= chunk
        }
        return results
    }

    @Test
    fun targetApp_resolvesInstagramAndTikTokPackages() {
        assertEquals(TargetApp.INSTAGRAM, TargetApp.fromPackage("com.instagram.android"))
        assertEquals(TargetApp.TIKTOK, TargetApp.fromPackage("com.zhiliaoapp.musically"))
        assertEquals(TargetApp.TIKTOK, TargetApp.fromPackage("com.ss.android.ugc.trill"))
        assertEquals(TargetApp.TIKTOK, TargetApp.fromPackage("com.zhiliaoapp.musically.go"))
        assertEquals(TargetApp.X_TWITTER, TargetApp.fromPackage("com.twitter.android"))
        assertEquals(TargetApp.X_TWITTER, TargetApp.fromPackage("com.twitter.android.lite"))
        assertEquals(TargetApp.SNAPCHAT, TargetApp.fromPackage("com.snapchat.android"))
        assertEquals(TargetApp.THREADS, TargetApp.fromPackage("com.instagram.barcelona"))
        assertNull(TargetApp.fromPackage("com.android.settings"))
        assertNull(TargetApp.fromPackage(null))
    }

    @Test
    fun appLanguage_allLanguagesResolveStringsAndFormatting() {
        val explicitLanguages = AppLanguage.entries.filter { it != AppLanguage.SYSTEM }
        assertEquals(
            listOf(
                AppLanguage.EN,
                AppLanguage.UK,
                AppLanguage.DE,
                AppLanguage.ES,
                AppLanguage.FR,
                AppLanguage.PT,
                AppLanguage.IT,
                AppLanguage.PL,
            ),
            explicitLanguages,
        )

        for (lang in explicitLanguages) {
            val s = stringsFor(lang)
            assertEquals(lang, s.language)
            assertEquals(lang.tag, lang.resolveLocale().language)
            assertFalse(s.appSubtitle.isBlank())
            assertTrue(s.statusUnlockActive(12, 5).contains("12:05"))
            assertTrue(s.statusSessionActive(8, 30).contains("8:30"))
            assertFalse(s.modeSessionLimit.isBlank())
            assertFalse(s.ruleSessionLimit(15, "1h").isBlank())
            assertFalse(s.sessionLimitCardTitle.isBlank())
            assertTrue(s.sessionStatusRemaining(12, 5, 15).contains("12:05"))
            assertTrue(s.sessionStatusCooldown("17:15", 45).contains("17:15"))
            assertTrue(s.sessionResetsAtHint(15, "19:15").contains("19:15"))
            assertFalse(s.sessionCooldownHint("1h").isBlank())
            assertTrue(s.sessionWarningBanner(2).contains("2"))
            assertTrue(s.lockSessionCooldownReason(15, "1h").contains("15"))
            assertTrue(s.lockTitle("Instagram").contains("Instagram"))
            assertFalse(s.deletePeriodDialogTitle.isBlank())
            assertFalse(s.deletePeriodConfirmButton.isBlank())
            assertTrue(s.deletePeriodDialogMessage("09:00 – 12:00").contains("09:00 – 12:00"))
            assertFalse(s.formatDuration(135).isBlank())
        }
    }

    @Test
    fun sessionLimit_warnsOnceAtTwoMinutesAndBlocksUntilNextClockHour() {
        val settings = newSettings()
        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 5 // 300_000 ms

        // 2026-01-01T12:40:00Z -> next clock hour is 13:00:00Z
        val base = LocalDateTime.of(2026, 1, 1, 12, 40, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val nextHour = LocalDateTime.of(2026, 1, 1, 13, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

        // Before reaching 2 minutes left (use 2m 55s = 175_000 ms -> 2m 05s left)
        val beforeWarning = advanceUsage(settings, 175_000L, base)
        assertTrue(beforeWarning.all { it == SessionTickResult.ACTIVE })
        assertFalse(settings.evaluate(LocalDateTime.of(2026, 1, 1, 12, 42), base + 175_000L).isBlocking)

        // Crossing 2-minute threshold (use 5_000 ms -> 180_000 ms used, 120_000 ms = 2m left)
        val atWarning = settings.recordSessionUsage(5_000L, base + 180_000L)
        assertEquals(SessionTickResult.SHOW_WARNING, atWarning)

        // Subsequent ticks while <= 2 min left must NOT re-trigger warning
        val afterWarning = advanceUsage(settings, 115_000L, base + 180_000L)
        assertTrue(afterWarning.all { it == SessionTickResult.ACTIVE })

        // Final 5_000 ms exhausts the 5-min allowance -> hard block until 13:00:00Z
        val cooldownStart = base + 300_000L
        assertEquals(
            SessionTickResult.COOLDOWN_STARTED,
            settings.recordSessionUsage(5_000L, cooldownStart),
        )

        val blockState = settings.evaluate(LocalDateTime.of(2026, 1, 1, 12, 45), cooldownStart + 1_000L)
        assertTrue(blockState.isBlocking)
        assertFalse(blockState.unlockEligible)
        assertTrue(blockState.hardBlockEnabled)
        assertTrue(blockState.isSessionCooldown)
        assertEquals(nextHour, blockState.cooldownUntilMillis)

        // While still in the 12:00 hour, ticks report COOLDOWN_ACTIVE
        assertEquals(
            SessionTickResult.COOLDOWN_ACTIVE,
            settings.recordSessionUsage(5_000L, cooldownStart + 10 * 60_000L),
        )

        // At 13:00:00Z (start of next clock hour), block lifts and full 5-min allowance is restored
        assertFalse(settings.evaluate(LocalDateTime.of(2026, 1, 1, 13, 0), nextHour).isBlocking)
        val freshStatus = settings.getSessionStatus(nextHour)
        assertFalse(freshStatus.isCooldownActive)
        assertEquals(0L, freshStatus.usedMillis)
        assertEquals(5 * 60_000L, freshStatus.remainingMillis)
        assertNull(freshStatus.resetsAtMillis)
    }

    @Test
    fun sessionLimit_partialUsageResetsAtNextClockHour() {
        val settings = newSettings()
        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 15

        // 2026-01-01T20:40:00Z -> use 6 min (until 20:46:00Z), resets at 21:00:00Z
        val base = LocalDateTime.of(2026, 1, 1, 20, 40, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val nextHour = LocalDateTime.of(2026, 1, 1, 21, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        advanceUsage(settings, 6 * 60_000L, base)

        // At 20:59:00Z (still in 20:00 hour), usage is preserved and resetsAtMillis == 21:00:00Z
        val at2059 = nextHour - 60_000L
        val statusBeforeHour = settings.getSessionStatus(at2059)
        assertEquals(6 * 60_000L, statusBeforeHour.usedMillis)
        assertEquals(9 * 60_000L, statusBeforeHour.remainingMillis)
        assertEquals(nextHour, statusBeforeHour.resetsAtMillis)

        // At 21:00:00Z, partial usage resets to 0 and full 15 min is available
        val statusAtNextHour = settings.getSessionStatus(nextHour)
        assertEquals(0L, statusAtNextHour.usedMillis)
        assertEquals(15 * 60_000L, statusAtNextHour.remainingMillis)
        assertNull(statusAtNextHour.resetsAtMillis)

        // Recording a new 5s tick at 21:00:05Z starts fresh from 0
        assertEquals(
            SessionTickResult.ACTIVE,
            settings.recordSessionUsage(5_000L, nextHour + 5_000L),
        )
        assertEquals(
            5_000L,
            settings.getSessionStatus(nextHour + 5_000L).usedMillis,
        )
    }

    @Test
    fun sessionLimit_presetsAndDisableBehavior() {
        val settings = newSettings()
        assertEquals(15, settings.sessionLimitMinutes)
        settings.sessionLimitMinutes = 10
        assertEquals(10, settings.sessionLimitMinutes)
        // Invalid value falls back to default (15)
        settings.sessionLimitMinutes = 99
        assertEquals(15, settings.sessionLimitMinutes)

        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 5
        val base = LocalDateTime.of(2026, 1, 1, 14, 10, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        advanceUsage(settings, 5 * 60_000L, base)
        assertTrue(settings.isSessionCooldownActive(base + 5 * 60_000L + 1_000L))

        // Turning Master Blocker off clears active cooldown and usage
        settings.isEnabled = false
        assertFalse(settings.isSessionCooldownActive(base + 5 * 60_000L + 1_000L))
        assertEquals(0L, settings.getSessionStatus(base + 5 * 60_000L + 1_000L).usedMillis)
    }

    @Test
    fun sessionLimit_legacyCooldownEndsAtNextClockHour() {
        val prefs = InMemorySharedPreferences()
        val settings = SettingsManager(prefs, zoneProvider = { ZoneOffset.UTC })
        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 15

        val lastActivity = LocalDateTime.of(2026, 1, 1, 13, 30, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val nextHour = LocalDateTime.of(2026, 1, 1, 14, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val at1345 = LocalDateTime.of(2026, 1, 1, 13, 45, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

        // Legacy version stored session_cooldown_until = lastActivity + 60 min (14:30)
        prefs.edit()
            .putLong("session_last_activity_ms", lastActivity)
            .putLong("session_cooldown_until", lastActivity + 60 * 60_000L)
            .putLong("session_used_ms", 0L)
            .apply()

        // Before 14:00, block is active and capped to end at 14:00
        assertTrue(settings.isSessionCooldownActive(at1345))
        val blockBefore = settings.evaluate(LocalDateTime.of(2026, 1, 1, 13, 45), at1345)
        assertTrue(blockBefore.isBlocking)
        assertTrue(blockBefore.isSessionCooldown)
        assertEquals(nextHour, blockBefore.cooldownUntilMillis)
        val statusBefore = settings.getSessionStatus(at1345)
        assertTrue(statusBefore.isCooldownActive)
        assertEquals(nextHour, statusBefore.cooldownUntilMillis)

        // At 14:00, block has ended and full allowance is restored
        assertFalse(settings.isSessionCooldownActive(nextHour))
        assertFalse(settings.evaluate(LocalDateTime.of(2026, 1, 1, 14, 0), nextHour).isBlocking)
        val statusAt1400 = settings.getSessionStatus(nextHour)
        assertFalse(statusAt1400.isCooldownActive)
        assertEquals(0L, statusAt1400.usedMillis)
        assertEquals(15 * 60_000L, statusAt1400.remainingMillis)
        assertEquals(0L, statusAt1400.cooldownUntilMillis)
        assertNull(statusAt1400.resetsAtMillis)
    }

    @Test
    fun sessionLimit_limitRunsOutAt5959_resetsAtNextHour() {
        val settings = newSettings()
        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 5 // 300_000 ms

        val start = LocalDateTime.of(2026, 1, 1, 13, 54, 59).toInstant(ZoneOffset.UTC).toEpochMilli()
        val at135959 = LocalDateTime.of(2026, 1, 1, 13, 59, 59).toInstant(ZoneOffset.UTC).toEpochMilli()
        val at140000 = LocalDateTime.of(2026, 1, 1, 14, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

        // Use 4m 55s (295_000 ms) -> reaches 13:59:54
        advanceUsage(settings, 295_000L, start)

        // Final 5s exhausts limit at 13:59:59 -> COOLDOWN_STARTED until 14:00:00
        assertEquals(
            SessionTickResult.COOLDOWN_STARTED,
            settings.recordSessionUsage(5_000L, at135959),
        )
        assertTrue(settings.isSessionCooldownActive(at135959))
        assertEquals(at140000, settings.getSessionStatus(at135959).cooldownUntilMillis)

        // At 14:00:00, block is lifted and tick returns ACTIVE with a fresh allowance
        assertFalse(settings.isSessionCooldownActive(at140000))
        assertEquals(
            SessionTickResult.ACTIVE,
            settings.recordSessionUsage(1_000L, at140000),
        )
        val freshStatus = settings.getSessionStatus(at140000)
        assertFalse(freshStatus.isCooldownActive)
        assertEquals(0L, freshStatus.usedMillis)
        assertEquals(5 * 60_000L, freshStatus.remainingMillis)
    }

    @Test
    fun sessionLimit_tickCrossingHourChargesOnlyNewHourPortion() {
        val settings = newSettings()
        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 15

        val at135955 = LocalDateTime.of(2026, 1, 1, 13, 59, 55).toInstant(ZoneOffset.UTC).toEpochMilli()
        val at140005 = LocalDateTime.of(2026, 1, 1, 14, 0, 5).toInstant(ZoneOffset.UTC).toEpochMilli()
        val at150000 = LocalDateTime.of(2026, 1, 1, 15, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

        // Last tick in the 13:00 hour at 13:59:55
        assertEquals(SessionTickResult.ACTIVE, settings.recordSessionUsage(5_000L, at135955))

        // 10s tick crossing 14:00:00 -> only the 5s inside the 14:00 hour are charged
        assertEquals(SessionTickResult.ACTIVE, settings.recordSessionUsage(10_000L, at140005))

        val status = settings.getSessionStatus(at140005)
        assertEquals(5_000L, status.usedMillis)
        assertEquals(15 * 60_000L - 5_000L, status.remainingMillis)
        assertEquals(at150000, status.resetsAtMillis)
    }

    @Test
    fun sessionLimit_warningFlagResetsEachHour() {
        val prefs = InMemorySharedPreferences()
        val settings = SettingsManager(prefs, zoneProvider = { ZoneOffset.UTC })
        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 5 // warning fires at 3 min (180_000 ms) used

        val start13 = LocalDateTime.of(2026, 1, 1, 13, 55, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val at1358 = LocalDateTime.of(2026, 1, 1, 13, 58, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val at140005 = LocalDateTime.of(2026, 1, 1, 14, 0, 5).toInstant(ZoneOffset.UTC).toEpochMilli()

        // Advance 2m 55s (175_000 ms), then cross 2-min remaining threshold at 13:58:00
        assertTrue(advanceUsage(settings, 175_000L, start13).all { it == SessionTickResult.ACTIVE })
        assertEquals(SessionTickResult.SHOW_WARNING, settings.recordSessionUsage(5_000L, at1358))
        assertTrue(prefs.getBoolean("session_warned", false))

        // First tick after 14:00 resets session_warned to false
        assertEquals(SessionTickResult.ACTIVE, settings.recordSessionUsage(5_000L, at140005))
        assertFalse(prefs.getBoolean("session_warned", true))

        // Approaching the limit again in the 14:00 hour fires the warning again
        assertTrue(advanceUsage(settings, 170_000L, at140005).all { it == SessionTickResult.ACTIVE })
        assertFalse(prefs.getBoolean("session_warned", true))
        val at140300 = at140005 + 175_000L
        assertEquals(SessionTickResult.SHOW_WARNING, settings.recordSessionUsage(5_000L, at140300))
        assertTrue(prefs.getBoolean("session_warned", false))
    }

    @Test
    fun sessionLimit_zoneChangeMidBlockEndsAtOriginalMoment() {
        val utcPlus2 = ZoneOffset.ofHours(2)
        val utcPlus530 = ZoneOffset.ofHoursMinutes(5, 30)
        var activeZone: ZoneId = utcPlus2

        val settings = SettingsManager(InMemorySharedPreferences(), zoneProvider = { activeZone })
        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 5 // 300_000 ms

        // Start at 13:10+02:00 (11:10Z), exhaust limit at 13:15+02:00 (11:15Z) -> original end is 14:00+02:00 (12:00Z)
        val start = ZonedDateTime.of(2026, 1, 1, 13, 10, 0, 0, utcPlus2).toInstant().toEpochMilli()
        val cooldownStart = ZonedDateTime.of(2026, 1, 1, 13, 15, 0, 0, utcPlus2).toInstant().toEpochMilli()
        val originalEnd = ZonedDateTime.of(2026, 1, 1, 14, 0, 0, 0, utcPlus2).toInstant().toEpochMilli()

        advanceUsage(settings, 295_000L, start)
        assertEquals(SessionTickResult.COOLDOWN_STARTED, settings.recordSessionUsage(5_000L, cooldownStart))
        assertEquals(originalEnd, settings.getSessionStatus(cooldownStart).cooldownUntilMillis)

        // Switch timezone mid-block to UTC+5:30 (11:30Z is 17:00+05:30, bisecting 11:15Z..12:00Z)
        activeZone = utcPlus530

        // Before the +05:30 hour boundary (11:20Z = 16:50+05:30)
        val at1120Z = cooldownStart + 5 * 60_000L
        assertTrue(settings.isSessionCooldownActive(at1120Z))
        assertEquals(originalEnd, settings.getSessionStatus(at1120Z).cooldownUntilMillis)
        assertEquals(originalEnd, settings.evaluate(at1120Z.toLocalDateTime(activeZone), at1120Z).cooldownUntilMillis)

        // After the +05:30 hour boundary (11:45Z = 17:15+05:30), still 15 min before originalEnd
        val at1145Z = cooldownStart + 30 * 60_000L
        assertTrue(settings.isSessionCooldownActive(at1145Z))
        assertEquals(originalEnd, settings.getSessionStatus(at1145Z).cooldownUntilMillis)
        val blockState = settings.evaluate(at1145Z.toLocalDateTime(activeZone), at1145Z)
        assertTrue(blockState.isBlocking)
        assertEquals(originalEnd, blockState.cooldownUntilMillis)

        // Still blocked 1 ms before originalEnd, then lifts at exact originalEnd (12:00Z = 17:30+05:30)
        assertTrue(settings.isSessionCooldownActive(originalEnd - 1L))
        assertFalse(settings.isSessionCooldownActive(originalEnd))
        assertFalse(settings.evaluate(originalEnd.toLocalDateTime(activeZone), originalEnd).isBlocking)
        val freshStatus = settings.getSessionStatus(originalEnd)
        assertFalse(freshStatus.isCooldownActive)
        assertEquals(0L, freshStatus.usedMillis)
        assertEquals(5 * 60_000L, freshStatus.remainingMillis)
    }

    @Test
    fun sessionLimit_dstFallBackResetsOnRepeatedHour() {
        val nyZone = ZoneId.of("America/New_York")
        val settings = SettingsManager(InMemorySharedPreferences(), zoneProvider = { nyZone })
        settings.isEnabled = true
        settings.scheduleMode = ScheduleMode.SESSION_LIMIT
        settings.sessionLimitMinutes = 15

        // 2026-11-01 in America/New_York: 01:00 repeats (01:00 EDT = UTC-4, then 01:00 EST = UTC-5)
        val firstOneAm = ZonedDateTime.of(2026, 11, 1, 1, 0, 0, 0, nyZone)
            .withEarlierOffsetAtOverlap()
            .toInstant()
            .toEpochMilli()
        val secondOneAm = ZonedDateTime.of(2026, 11, 1, 1, 0, 0, 0, nyZone)
            .withLaterOffsetAtOverlap()
            .toInstant()
            .toEpochMilli()
        assertEquals(60 * 60_000L, secondOneAm - firstOneAm)

        // Use 6 minutes during the first 01:00 hour (01:30 EDT .. 01:36 EDT)
        val usageStart = firstOneAm + 30 * 60_000L
        advanceUsage(settings, 6 * 60_000L, usageStart)

        // At 01:59 EDT (still in the first 01:00 hour), usage is preserved and resets at second 01:00
        val beforeSecondOneAm = secondOneAm - 60_000L
        val statusBefore = settings.getSessionStatus(beforeSecondOneAm)
        assertEquals(6 * 60_000L, statusBefore.usedMillis)
        assertEquals(9 * 60_000L, statusBefore.remainingMillis)
        assertEquals(secondOneAm, statusBefore.resetsAtMillis)

        // At 01:00 EST (start of the second 01:00 hour), usage resets to 0
        val statusAtSecondOneAm = settings.getSessionStatus(secondOneAm)
        assertEquals(0L, statusAtSecondOneAm.usedMillis)
        assertEquals(15 * 60_000L, statusAtSecondOneAm.remainingMillis)
        assertNull(statusAtSecondOneAm.resetsAtMillis)

        // A new tick at 01:00:05 EST charges fresh from 0
        assertEquals(
            SessionTickResult.ACTIVE,
            settings.recordSessionUsage(5_000L, secondOneAm + 5_000L),
        )
        assertEquals(5_000L, settings.getSessionStatus(secondOneAm + 5_000L).usedMillis)
    }
}

private class InMemorySharedPreferences : SharedPreferences {
    private val map = LinkedHashMap<String, Any?>()

    override fun getAll(): MutableMap<String, *> = LinkedHashMap(map)
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        (map[key] as? Set<String>)?.toMutableSet() ?: defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = LinkedHashMap<String, Any?>()
        private val removals = HashSet<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?) = apply { if (key != null) pending[key] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { if (key != null) pending[key] = values?.toSet() }
        override fun putInt(key: String?, value: Int) = apply { if (key != null) pending[key] = value }
        override fun putLong(key: String?, value: Long) = apply { if (key != null) pending[key] = value }
        override fun putFloat(key: String?, value: Float) = apply { if (key != null) pending[key] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) pending[key] = value }
        override fun remove(key: String?) = apply { if (key != null) removals.add(key) }
        override fun clear() = apply { clearAll = true }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearAll) map.clear()
            removals.forEach { map.remove(it) }
            map.putAll(pending)
        }
    }
}
