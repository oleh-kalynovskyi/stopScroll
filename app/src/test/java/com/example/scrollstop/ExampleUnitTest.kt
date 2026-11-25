package com.example.scrollstop

import android.content.SharedPreferences
import java.time.LocalDateTime
import java.time.ZoneOffset
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
