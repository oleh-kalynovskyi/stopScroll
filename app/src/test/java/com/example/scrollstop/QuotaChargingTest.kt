package com.example.scrollstop

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class QuotaChargingTest {

    private val explicitZone: ZoneId = ZoneId.of("UTC")

    // region 1. Unlock fully inside one hour charges only that hour

    @Test
    fun chargesOnlyCurrentHour_whenUnlockIsFullyInsideOneHour() {
        val prefs = ScheduleEvaluationTest.MapSharedPreferences()
        val manager = createSettingsManager(prefs)
        val hour14Start = epochMillisAt(hour = 14, minute = 0)
        val nowAt1410 = epochMillisAt(hour = 14, minute = 10)

        val granted = manager.startUnlock(minutes = 5, nowMillis = nowAt1410)

        assertTrue(granted)
        val usage = readHourlyUsage(prefs)
        assertEquals(mapOf(hour14Start.toString() to 5 * MILLIS_PER_MINUTE), usage)
        assertEquals(5 * MILLIS_PER_MINUTE, manager.usedMillisInCurrentHour(nowAt1410))
    }

    // endregion

    // region 2. Unlock crossing hour boundary splits exact milliseconds per hour key

    @Test
    fun splitsExactMillisecondsPerHourKey_whenUnlockCrossesHourBoundary() {
        val prefs = ScheduleEvaluationTest.MapSharedPreferences()
        val manager = createSettingsManager(prefs)
        val hour14Start = epochMillisAt(hour = 14, minute = 0)
        val hour15Start = epochMillisAt(hour = 15, minute = 0)
        // 14:55:20 -> 4m 40s (280_000 ms) left in 14:00 hour; remaining 10m 20s (620_000 ms) in 15:00 hour
        val nowAt1455And20s = epochMillisAt(hour = 14, minute = 55, second = 20)

        val granted = manager.startUnlock(minutes = 15, nowMillis = nowAt1455And20s)

        assertTrue(granted)
        val usage = readHourlyUsage(prefs)
        assertEquals(2, usage.size)
        assertEquals(280_000L, usage[hour14Start.toString()])
        assertEquals(620_000L, usage[hour15Start.toString()])
    }

    // endregion

    // region 3. Requested minutes clamped to 1..remaining quota (0, above, equal)

    @Test
    fun clampsToOneMinute_whenRequestedMinutesIsZero() {
        val prefs = ScheduleEvaluationTest.MapSharedPreferences()
        val manager = createSettingsManager(prefs)
        val now = epochMillisAt(hour = 10, minute = 0)

        val granted = manager.startUnlock(minutes = 0, nowMillis = now)

        assertTrue(granted)
        assertEquals(1 * MILLIS_PER_MINUTE, manager.usedMillisInCurrentHour(now))
        assertEquals(now + 1 * MILLIS_PER_MINUTE, manager.unlockUntilMillis)
    }

    @Test
    fun clampsToRemainingQuota_whenRequestedMinutesExceedsRemaining() {
        val prefs = ScheduleEvaluationTest.MapSharedPreferences()
        val manager = createSettingsManager(prefs)
        val firstUnlockTime = epochMillisAt(hour = 10, minute = 0)
        val secondUnlockTime = epochMillisAt(hour = 10, minute = 10)

        assertTrue(manager.startUnlock(minutes = 6, nowMillis = firstUnlockTime))
        assertEquals(9, manager.remainingQuotaMinutes(secondUnlockTime))

        val granted = manager.startUnlock(minutes = 20, nowMillis = secondUnlockTime)

        assertTrue(granted)
        assertEquals(15 * MILLIS_PER_MINUTE, manager.usedMillisInCurrentHour(secondUnlockTime))
        assertEquals(secondUnlockTime + 9 * MILLIS_PER_MINUTE, manager.unlockUntilMillis)
        assertEquals(0, manager.remainingQuotaMinutes(secondUnlockTime))
    }

    @Test
    fun grantsExactRemainingQuota_whenRequestedMinutesEqualsRemaining() {
        val prefs = ScheduleEvaluationTest.MapSharedPreferences()
        val manager = createSettingsManager(prefs)
        val firstUnlockTime = epochMillisAt(hour = 10, minute = 0)
        val secondUnlockTime = epochMillisAt(hour = 10, minute = 10)

        assertTrue(manager.startUnlock(minutes = 5, nowMillis = firstUnlockTime))
        assertEquals(10, manager.remainingQuotaMinutes(secondUnlockTime))

        val granted = manager.startUnlock(minutes = 10, nowMillis = secondUnlockTime)

        assertTrue(granted)
        assertEquals(15 * MILLIS_PER_MINUTE, manager.usedMillisInCurrentHour(secondUnlockTime))
        assertEquals(secondUnlockTime + 10 * MILLIS_PER_MINUTE, manager.unlockUntilMillis)
        assertEquals(0, manager.remainingQuotaMinutes(secondUnlockTime))
    }

    // endregion

    // region 4. Remaining quota reduced by earlier usage in same hour, unaffected by other hours

    @Test
    fun reducesRemainingQuotaOnlyForSameHourUsage_whenUsageExistsAcrossHours() {
        val manager = createSettingsManager()
        val hour13At10 = epochMillisAt(hour = 13, minute = 10)
        val hour14At05 = epochMillisAt(hour = 14, minute = 5)
        val hour14At20 = epochMillisAt(hour = 14, minute = 20)
        val hour15At05 = epochMillisAt(hour = 15, minute = 5)

        // Use 12 minutes in the 13:00 hour (completely within 13:00)
        assertTrue(manager.startUnlock(minutes = 12, nowMillis = hour13At10))
        assertEquals(3, manager.remainingQuotaMinutes(hour13At10))

        // At 14:05 (new hour), full 15-minute quota is available
        assertEquals(15, manager.remainingQuotaMinutes(hour14At05))

        // Use 6 minutes at 14:05 -> remaining in 14:00 hour drops to 9
        assertTrue(manager.startUnlock(minutes = 6, nowMillis = hour14At05))
        assertEquals(9, manager.remainingQuotaMinutes(hour14At20))

        // At 15:05 (next hour), full 15-minute quota is available
        assertEquals(15, manager.remainingQuotaMinutes(hour15At05))
    }

    @Test
    fun truncatesRemainingQuotaMinutes_whenSubMinuteUsageExists() {
        val manager = createSettingsManager()
        // 14:55:20 for 15 min charges 4m 40s (280_000 ms) to 14:00 hour, leaving 10m 20s (620_000 ms)
        val nowAt1455And20s = epochMillisAt(hour = 14, minute = 55, second = 20)
        assertTrue(manager.startUnlock(minutes = 15, nowMillis = nowAt1455And20s))

        // Integer division: 620_000 / 60_000 = 10 (not rounded up to 11)
        assertEquals(10, manager.remainingQuotaMinutes(nowAt1455And20s))
    }

    // endregion

    // region 5. An hour with 15 minutes already used allows no unlock

    @Test
    fun deniesUnlock_when15MinutesAlreadyUsedInCurrentHour() {
        val manager = createSettingsManager()
        val hour14At00 = epochMillisAt(hour = 14, minute = 0)
        val hour14At20 = epochMillisAt(hour = 14, minute = 20)

        assertTrue(manager.startUnlock(minutes = 15, nowMillis = hour14At00))
        assertEquals(0, manager.remainingQuotaMinutes(hour14At20))
        assertFalse(manager.isUnlockActive(hour14At20))

        val secondAttempt = manager.startUnlock(minutes = 1, nowMillis = hour14At20)

        assertFalse(secondAttempt)
        assertFalse(manager.isUnlockActive(hour14At20))
        assertEquals(hour14At00 + 15 * MILLIS_PER_MINUTE, manager.unlockUntilMillis)
    }

    // endregion

    // region 6. hourly_usage keys older than current hour are pruned

    @Test
    fun prunesHourlyUsageKeys_whenOlderThanCurrentHour() {
        val prefs = ScheduleEvaluationTest.MapSharedPreferences()
        val manager = createSettingsManager(prefs)
        val hour12Start = epochMillisAt(hour = 12, minute = 0)
        val hour13Start = epochMillisAt(hour = 13, minute = 0)
        val hour14Start = epochMillisAt(hour = 14, minute = 0)
        val hour15Start = epochMillisAt(hour = 15, minute = 0)

        // Seed usage in 12:00 and 13:00 hours
        assertTrue(manager.startUnlock(minutes = 5, nowMillis = epochMillisAt(hour = 12, minute = 10)))
        assertTrue(manager.startUnlock(minutes = 4, nowMillis = epochMillisAt(hour = 13, minute = 10)))
        assertTrue(readHourlyUsage(prefs).containsKey(hour13Start.toString()))

        // Start unlock at 14:55 for 10 minutes (charges 14:00 and 15:00, prunes < 14:00)
        assertTrue(manager.startUnlock(minutes = 10, nowMillis = epochMillisAt(hour = 14, minute = 55)))

        val usageAfterHour14Unlock = readHourlyUsage(prefs)
        assertFalse(usageAfterHour14Unlock.containsKey(hour12Start.toString()))
        assertFalse(usageAfterHour14Unlock.containsKey(hour13Start.toString()))
        assertEquals(5 * MILLIS_PER_MINUTE, usageAfterHour14Unlock[hour14Start.toString()])
        assertEquals(5 * MILLIS_PER_MINUTE, usageAfterHour14Unlock[hour15Start.toString()])
    }

    // endregion

    // region 7. unlock_until equals nowMillis + durationMs

    @Test
    fun setsUnlockUntilToNowPlusDuration_whenUnlockStarts() {
        val manager = createSettingsManager()
        val now = epochMillisAt(hour = 16, minute = 12, second = 34)
        val requestedMinutes = 7
        val expectedDurationMs = requestedMinutes * MILLIS_PER_MINUTE

        val granted = manager.startUnlock(minutes = requestedMinutes, nowMillis = now)

        assertTrue(granted)
        assertEquals(now + expectedDurationMs, manager.unlockUntilMillis)
        assertTrue(manager.isUnlockActive(now + expectedDurationMs - 1))
        assertFalse(manager.isUnlockActive(now + expectedDurationMs))
    }

    // endregion

    // region 8. Enabling hard block clears unlock_until (set to 0)

    @Test
    fun clearsUnlockUntilToZero_whenHardBlockEnabled() {
        val manager = createSettingsManager()
        val now = epochMillisAt(hour = 11, minute = 0)

        assertTrue(manager.startUnlock(minutes = 10, nowMillis = now))
        assertEquals(now + 10 * MILLIS_PER_MINUTE, manager.unlockUntilMillis)
        assertTrue(manager.isUnlockActive(now + 1_000L))

        manager.isHardBlock = true

        assertEquals(0L, manager.unlockUntilMillis)
        assertFalse(manager.isUnlockActive(now + 1_000L))
    }

    // endregion

    // region Characterization tests
    // Documents current behavior. Not a guaranteed spec; changing it requires approval.

    @Test
    fun currentBehavior_preservesRemainingUnlockTimeWhenTimeZoneChangesMidUnlock() {
        val nyZone = ZoneId.of("America/New_York")
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        var activeZone: ZoneId = nyZone

        val manager = createSettingsManager(zoneProvider = { activeZone })
        val startMillis = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, nyZone).toInstant().toEpochMilli()

        assertTrue(manager.startUnlock(minutes = 10, nowMillis = startMillis))
        val expectedExpiryMillis = startMillis + 10 * MILLIS_PER_MINUTE
        assertEquals(expectedExpiryMillis, manager.unlockUntilMillis)

        // 4 minutes into the unlock, active timezone changes from New York (UTC-4) to Tokyo (UTC+9)
        val midUnlockMillis = startMillis + 4 * MILLIS_PER_MINUTE
        activeZone = tokyoZone

        assertEquals(expectedExpiryMillis, manager.unlockUntilMillis)
        assertEquals(6 * MILLIS_PER_MINUTE, manager.unlockUntilMillis - midUnlockMillis)
        assertTrue(manager.isUnlockActive(expectedExpiryMillis - 1))
        assertFalse(manager.isUnlockActive(expectedExpiryMillis))
    }

    // endregion

    // region Test helpers

    private fun epochMillisAt(hour: Int, minute: Int, second: Int = 0): Long =
        ZonedDateTime.of(2026, 6, 15, hour, minute, second, 0, explicitZone).toInstant().toEpochMilli()

    private fun readHourlyUsage(prefs: SharedPreferences): Map<String, Long> {
        val json = prefs.getString("hourly_usage", null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Long>>() {}.type
        return Gson().fromJson(json, type)
    }

    private fun createSettingsManager(
        prefs: SharedPreferences = ScheduleEvaluationTest.MapSharedPreferences(),
        zoneProvider: () -> ZoneId = { explicitZone },
    ): SettingsManager = SettingsManager(prefs = prefs, zoneProvider = zoneProvider)

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
    }

    // endregion
}
