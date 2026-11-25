package com.example.scrollstop

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleEvaluationTest {

    // region 1. 120/121 boundary (single interval)

    @Test
    fun hardBlocks_whenSingleIntervalIs119Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 10:00 (600) to 11:59 (719) = 119 minutes
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(600, 719))

        val state = manager.evaluate(fixedDateTime(hour = 10, minute = 30))

        assertTrue(state.isBlocking)
        assertEquals(119, state.periodMinutes)
        assertFalse(state.unlockEligible)
        assertEquals(719, state.endMinuteOfDay)
    }

    @Test
    fun hardBlocks_whenSingleIntervalIs120Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 10:00 (600) to 12:00 (720) = 120 minutes
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(600, 720))

        val state = manager.evaluate(fixedDateTime(hour = 10, minute = 30))

        assertTrue(state.isBlocking)
        assertEquals(120, state.periodMinutes)
        assertFalse(state.unlockEligible)
        assertEquals(720, state.endMinuteOfDay)
    }

    @Test
    fun allowsUnlock_whenSingleIntervalIs121Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 10:00 (600) to 12:01 (721) = 121 minutes
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(600, 721))

        val state = manager.evaluate(fixedDateTime(hour = 10, minute = 30))

        assertTrue(state.isBlocking)
        assertEquals(121, state.periodMinutes)
        assertTrue(state.unlockEligible)
        assertEquals(721, state.endMinuteOfDay)
    }

    // endregion

    // region 2. 120/121 boundary after merging (adjacent & overlapping)

    @Test
    fun hardBlocks_whenTwoAdjacentIntervalsTotal120Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 10:00-11:00 (600..660) + 11:00-12:00 (660..720) = 120 minutes
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(600, 660))
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(660, 720))

        val state = manager.evaluate(fixedDateTime(hour = 10, minute = 45))

        assertTrue(state.isBlocking)
        assertEquals(120, state.periodMinutes)
        assertFalse(state.unlockEligible)
        assertEquals(720, state.endMinuteOfDay)
    }

    @Test
    fun allowsUnlock_whenTwoAdjacentIntervalsTotal121Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 10:00-11:00 (600..660) + 11:00-12:01 (660..721) = 121 minutes
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(600, 660))
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(660, 721))

        val state = manager.evaluate(fixedDateTime(hour = 11, minute = 15))

        assertTrue(state.isBlocking)
        assertEquals(121, state.periodMinutes)
        assertTrue(state.unlockEligible)
        assertEquals(721, state.endMinuteOfDay)
    }

    @Test
    fun hardBlocks_whenTwoOverlappingIntervalsUnionIs120Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 10:00-11:30 (600..690) U 10:30-12:00 (630..720) = 600..720 (120 minutes)
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(600, 690))
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(630, 720))

        val state = manager.evaluate(fixedDateTime(hour = 11, minute = 0))

        assertTrue(state.isBlocking)
        assertEquals(120, state.periodMinutes)
        assertFalse(state.unlockEligible)
        assertEquals(720, state.endMinuteOfDay)
    }

    @Test
    fun allowsUnlock_whenTwoOverlappingIntervalsUnionIs121Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 10:00-11:30 (600..690) U 10:30-12:01 (630..721) = 600..721 (121 minutes)
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(600, 690))
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(630, 721))

        val state = manager.evaluate(fixedDateTime(hour = 11, minute = 0))

        assertTrue(state.isBlocking)
        assertEquals(121, state.periodMinutes)
        assertTrue(state.unlockEligible)
        assertEquals(721, state.endMinuteOfDay)
    }

    // endregion

    // region 3. 120/121 boundary on overnight interval (before & after midnight)

    @Test
    fun hardBlocksBeforeAndAfterMidnight_whenOvernightIntervalIs120Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 23:00 (1380) to 01:00 (60) = 120 minutes
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(1380, 60))

        val beforeMidnight = manager.evaluate(fixedDateTime(hour = 23, minute = 30))
        assertTrue(beforeMidnight.isBlocking)
        assertEquals(120, beforeMidnight.periodMinutes)
        assertFalse(beforeMidnight.unlockEligible)
        assertEquals(60, beforeMidnight.endMinuteOfDay)

        val afterMidnight = manager.evaluate(fixedDateTime(hour = 0, minute = 30))
        assertTrue(afterMidnight.isBlocking)
        assertEquals(120, afterMidnight.periodMinutes)
        assertFalse(afterMidnight.unlockEligible)
        assertEquals(60, afterMidnight.endMinuteOfDay)
    }

    @Test
    fun allowsUnlockBeforeAndAfterMidnight_whenOvernightIntervalIs121Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 23:00 (1380) to 01:01 (61) = 121 minutes
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(1380, 61))

        val beforeMidnight = manager.evaluate(fixedDateTime(hour = 23, minute = 30))
        assertTrue(beforeMidnight.isBlocking)
        assertEquals(121, beforeMidnight.periodMinutes)
        assertTrue(beforeMidnight.unlockEligible)
        assertEquals(61, beforeMidnight.endMinuteOfDay)

        val afterMidnight = manager.evaluate(fixedDateTime(hour = 0, minute = 30))
        assertTrue(afterMidnight.isBlocking)
        assertEquals(121, afterMidnight.periodMinutes)
        assertTrue(afterMidnight.unlockEligible)
        assertEquals(61, afterMidnight.endMinuteOfDay)
    }

    // endregion

    // region 4. Interval starting at 00:00

    @Test
    fun evaluatesWithoutCrossingMidnight_whenIntervalStartsAt0000() {
        val interval = TimeInterval(startMinute = 0, endMinute = 180)
        assertFalse(interval.crossesMidnight)
        assertEquals(180, interval.durationMinutes)
        assertTrue(interval.contains(0))
        assertTrue(interval.contains(179))
        assertFalse(interval.contains(180))
        assertFalse(interval.contains(1439))

        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(0, 180))

        val atMidnight = manager.evaluate(fixedDateTime(hour = 0, minute = 0))
        assertTrue(atMidnight.isBlocking)
        assertEquals(180, atMidnight.periodMinutes)
        assertTrue(atMidnight.unlockEligible)
        assertEquals(180, atMidnight.endMinuteOfDay)

        val beforeMidnight = manager.evaluate(fixedDateTime(hour = 23, minute = 59))
        assertFalse(beforeMidnight.isBlocking)

        val atEnd = manager.evaluate(fixedDateTime(hour = 3, minute = 0))
        assertFalse(atEnd.isBlocking)
    }

    // endregion

    // region 5. Interval ending at 00:00

    @Test
    fun coversBeforeMidnightOnly_whenIntervalEndsAt0000() {
        // 21:00 (1260) to 00:00 (0) = 180 minutes
        val interval = TimeInterval(startMinute = 1260, endMinute = 0)
        assertTrue(interval.crossesMidnight)
        assertEquals(180, interval.durationMinutes)
        assertFalse(interval.contains(1259))
        assertTrue(interval.contains(1260))
        assertTrue(interval.contains(1439))
        assertFalse(interval.contains(0))
        assertFalse(interval.contains(1))

        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(1260, 0))

        val beforeStart = manager.evaluate(fixedDateTime(hour = 20, minute = 59))
        assertFalse(beforeStart.isBlocking)

        val atStart = manager.evaluate(fixedDateTime(hour = 21, minute = 0))
        assertTrue(atStart.isBlocking)
        assertEquals(180, atStart.periodMinutes)
        assertTrue(atStart.unlockEligible)
        assertEquals(0, atStart.endMinuteOfDay)

        val lastMinuteBeforeMidnight = manager.evaluate(fixedDateTime(hour = 23, minute = 59))
        assertTrue(lastMinuteBeforeMidnight.isBlocking)
        assertEquals(180, lastMinuteBeforeMidnight.periodMinutes)
        assertTrue(lastMinuteBeforeMidnight.unlockEligible)
        assertEquals(0, lastMinuteBeforeMidnight.endMinuteOfDay)

        val atMidnight = manager.evaluate(fixedDateTime(hour = 0, minute = 0))
        assertFalse(atMidnight.isBlocking)

        val afterMidnight = manager.evaluate(fixedDateTime(hour = 0, minute = 1))
        assertFalse(afterMidnight.isBlocking)
    }

    // endregion

    // region 6. startMinute == endMinute rejected

    @Test
    fun rejectsWithInvalidSameTime_whenStartMinuteEqualsEndMinute() {
        val manager = createSettingsManager()

        assertEquals(AddIntervalResult.INVALID_SAME_TIME, manager.addInterval(0, 0))
        assertEquals(AddIntervalResult.INVALID_SAME_TIME, manager.addInterval(540, 540))
        assertTrue(manager.getIntervals().isEmpty())

        assertEquals(AddIntervalResult.ADDED, manager.addInterval(540, 600))
        val addedId = manager.getIntervals().single().id
        assertEquals(AddIntervalResult.INVALID_SAME_TIME, manager.updateInterval(addedId, 600, 600))
        assertEquals(540, manager.getIntervals().single().startMinute)
        assertEquals(600, manager.getIntervals().single().endMinute)
    }

    // endregion

    // region 7. ALL_DAY with hard block off vs. on

    @Test
    fun allowsUnlock_whenAllDayAndHardBlockOff() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.scheduleMode = ScheduleMode.ALL_DAY
        manager.isHardBlock = false

        val state = manager.evaluate(fixedDateTime(hour = 14, minute = 0))

        assertTrue(state.isBlocking)
        assertTrue(state.unlockEligible)
        assertFalse(state.hardBlockEnabled)
        assertTrue(state.coversWholeDay)
        assertEquals(MINUTES_PER_DAY, state.periodMinutes)
        assertNull(state.endMinuteOfDay)
    }

    @Test
    fun disallowsUnlock_whenAllDayAndHardBlockOn() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.scheduleMode = ScheduleMode.ALL_DAY
        manager.isHardBlock = true

        val state = manager.evaluate(fixedDateTime(hour = 14, minute = 0))

        assertTrue(state.isBlocking)
        assertFalse(state.unlockEligible)
        assertTrue(state.hardBlockEnabled)
        assertTrue(state.coversWholeDay)
        assertEquals(MINUTES_PER_DAY, state.periodMinutes)
        assertNull(state.endMinuteOfDay)
    }

    // endregion

    // region 8. Hard block overrides eligibility for >120-minute interval

    @Test
    fun disallowsUnlock_whenHardBlockOnForIntervalOver120Minutes() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.scheduleMode = ScheduleMode.CUSTOM
        manager.isHardBlock = true
        // 09:00 (540) to 13:00 (780) = 240 minutes (> 120)
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(540, 780))

        val state = manager.evaluate(fixedDateTime(hour = 10, minute = 0))

        assertTrue(state.isBlocking)
        assertEquals(240, state.periodMinutes)
        assertFalse(state.unlockEligible)
        assertTrue(state.hardBlockEnabled)
        assertEquals(780, state.endMinuteOfDay)
    }

    // endregion

    // region 9. Minutes just outside an interval (start - 1, end) are not blocked

    @Test
    fun doesNotBlock_whenAtStartMinusOneOrEndMinute() {
        val manager = createSettingsManager()
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM
        // 10:00 (600) to 12:00 (720)
        assertEquals(AddIntervalResult.ADDED, manager.addInterval(600, 720))

        // start - 1 (09:59 = 599)
        val justBeforeStart = manager.evaluate(fixedDateTime(hour = 9, minute = 59))
        assertFalse(justBeforeStart.isBlocking)
        assertEquals(BlockState.NOT_BLOCKING, justBeforeStart)

        // start (10:00 = 600)
        val atStart = manager.evaluate(fixedDateTime(hour = 10, minute = 0))
        assertTrue(atStart.isBlocking)

        // end - 1 (11:59 = 719)
        val justBeforeEnd = manager.evaluate(fixedDateTime(hour = 11, minute = 59))
        assertTrue(justBeforeEnd.isBlocking)

        // end (12:00 = 720)
        val atEnd = manager.evaluate(fixedDateTime(hour = 12, minute = 0))
        assertFalse(atEnd.isBlocking)
        assertEquals(BlockState.NOT_BLOCKING, atEnd)
    }

    // endregion

    // region 10. Invalid & malformed intervals loaded from storage

    @Test
    fun filtersOutSameTimeIntervalAndKeepsValidOnes_whenLoadedFromRawStorage() {
        val prefs = MapSharedPreferences()
        prefs.edit()
            .putString(
                "intervals",
                """[
                    {"id":"bad","startMinute":300,"endMinute":300},
                    {"id":"good1","startMinute":600,"endMinute":720},
                    {"id":"good2","startMinute":1380,"endMinute":60}
                ]""",
            )
            .apply()
        val manager = createSettingsManager(prefs)

        val intervals = manager.getIntervals()

        assertEquals(2, intervals.size)
        assertEquals(listOf("good1", "good2"), intervals.map { it.id })
        assertEquals(600, intervals[0].startMinute)
        assertEquals(720, intervals[0].endMinute)
        assertEquals(1380, intervals[1].startMinute)
        assertEquals(60, intervals[1].endMinute)
    }

    @Test
    fun ignoresLoneSameTimeInterval_whenEvaluatingFromRawStorage() {
        val prefs = MapSharedPreferences()
        prefs.edit()
            .putString("intervals", """[{"id":"bad","startMinute":300,"endMinute":300}]""")
            .apply()
        val manager = createSettingsManager(prefs)
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM

        assertTrue(manager.getIntervals().isEmpty())
        val stateAtSameMinute = manager.evaluate(fixedDateTime(hour = 5, minute = 0))
        val stateAtOtherMinute = manager.evaluate(fixedDateTime(hour = 14, minute = 0))
        assertEquals(BlockState.NOT_BLOCKING, stateAtSameMinute)
        assertEquals(BlockState.NOT_BLOCKING, stateAtOtherMinute)
    }

    @Test
    fun fallsBackSafelyToEmptyAndNotBlocking_whenIntervalsJsonIsMalformed() {
        val prefs = MapSharedPreferences()
        prefs.edit()
            .putString("intervals", "{not valid json [")
            .apply()
        val manager = createSettingsManager(prefs)
        manager.isEnabled = true
        manager.isHardBlock = false
        manager.scheduleMode = ScheduleMode.CUSTOM

        assertTrue(manager.getIntervals().isEmpty())
        assertEquals(BlockState.NOT_BLOCKING, manager.evaluate(fixedDateTime(hour = 12, minute = 0)))
    }

    // endregion

    // region Characterization tests
    // Documents current behavior. Not a guaranteed spec; changing it requires approval.

    @Test
    fun currentBehavior_usesNominalWallClockMinutesOnDstTransitions() {
        val nyZone = ZoneId.of("America/New_York")

        // DST spring-forward in America/New_York: 2026-03-08 (02:00 jumps to 03:00).
        // Wall-clock 01:00-04:00 is 180 nominal minutes, even though only 120 elapsed minutes pass.
        val springManager = createSettingsManager(zoneProvider = { nyZone })
        springManager.isEnabled = true
        springManager.isHardBlock = false
        springManager.scheduleMode = ScheduleMode.CUSTOM
        assertEquals(AddIntervalResult.ADDED, springManager.addInterval(60, 240))

        val springTime = ZonedDateTime.of(2026, 3, 8, 1, 30, 0, 0, nyZone).toLocalDateTime()
        val springState = springManager.evaluate(springTime)
        assertTrue(springState.isBlocking)
        assertEquals(180, springState.periodMinutes)
        assertTrue(springState.unlockEligible)

        // DST fall-back in America/New_York: 2026-11-01 (01:00-01:59 repeats twice).
        // Wall-clock 01:00-03:00 is 120 nominal minutes, even though 180 elapsed minutes pass.
        val fallManager = createSettingsManager(zoneProvider = { nyZone })
        fallManager.isEnabled = true
        fallManager.isHardBlock = false
        fallManager.scheduleMode = ScheduleMode.CUSTOM
        assertEquals(AddIntervalResult.ADDED, fallManager.addInterval(60, 180))

        val fallTime = ZonedDateTime.of(2026, 11, 1, 1, 30, 0, 0, nyZone).toLocalDateTime()
        val fallState = fallManager.evaluate(fallTime)
        assertTrue(fallState.isBlocking)
        assertEquals(120, fallState.periodMinutes)
        assertFalse(fallState.unlockEligible)
    }

    // endregion

    // region Test helpers

    private fun fixedDateTime(hour: Int, minute: Int): LocalDateTime =
        ZonedDateTime.of(2026, 6, 15, hour, minute, 0, 0, EXPLICIT_ZONE).toLocalDateTime()

    private fun createSettingsManager(
        prefs: SharedPreferences = MapSharedPreferences(),
        zoneProvider: () -> ZoneId = { EXPLICIT_ZONE },
    ): SettingsManager = SettingsManager(prefs = prefs, zoneProvider = zoneProvider)

    companion object {
        private val EXPLICIT_ZONE: ZoneId = ZoneId.of("UTC")
    }

    internal class MapSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()
        private val listeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

        override fun getAll(): MutableMap<String, *> = HashMap(data)
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = EditorImpl()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null) listeners.add(listener)
        }
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?,
        ) {
            if (listener != null) listeners.remove(listener)
        }

        private inner class EditorImpl : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removals = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
                pending[key] = value
                removals.remove(key)
            }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                pending[key] = values?.toSet()
                removals.remove(key)
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
                pending[key] = value
                removals.remove(key)
            }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
                pending[key] = value
                removals.remove(key)
            }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
                pending[key] = value
                removals.remove(key)
            }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
                pending[key] = value
                removals.remove(key)
            }
            override fun remove(key: String): SharedPreferences.Editor = apply {
                removals.add(key)
                pending.remove(key)
            }
            override fun clear(): SharedPreferences.Editor = apply {
                clearAll = true
                pending.clear()
                removals.clear()
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                val changedKeys = mutableListOf<String>()
                if (clearAll) {
                    changedKeys.addAll(data.keys)
                    data.clear()
                }
                for (key in removals) {
                    if (data.containsKey(key)) {
                        data.remove(key)
                        changedKeys.add(key)
                    }
                }
                for ((key, value) in pending) {
                    if (value == null) {
                        data.remove(key)
                    } else {
                        data[key] = value
                    }
                    changedKeys.add(key)
                }
                for (key in changedKeys) {
                    for (listener in listeners.toList()) {
                        listener.onSharedPreferenceChanged(this@MapSharedPreferences, key)
                    }
                }
            }
        }
    }

    // endregion
}
