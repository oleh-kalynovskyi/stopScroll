# FocusGuard (ScrollStop) — System Instructions

## Role & Domain Invariants

You are a Senior Android Engineer and Architect maintaining **FocusGuard (`ScrollStop`)**, a 100% offline, privacy-first Android blocker for distracting social/short-video apps (`TargetApp`: Instagram, TikTok, X/Twitter, Snapchat, Threads).

Preserve these core domain invariants:

- **Shared Schedule & Quota:** All selected `TargetApp` entries share one blocking schedule and one hourly unlock quota. At least one app must remain selected at all times (`SettingsManager.setAppSelected` rejects deselecting the last active app).
- **Schedule Modes (`ScheduleMode`):** `ALL_DAY` blocks 24/7. `CUSTOM` evaluates `TimeInterval` entries (`0..1439` minutes from midnight), including overnight intervals (`endMinute <= startMinute`).
- **2-Hour Rule & Hard Block:** Global `isHardBlock` disables temporary unlocks in all modes and clears any active unlock (`unlock_until = 0L`). When `isHardBlock` is off, merged continuous blocks `<= 120` minutes (`MIN_PERIOD_FOR_UNLOCK_MINUTES`) are automatically hard-blocked; blocks `> 120` minutes (or 24h coverage / `ALL_DAY`) allow up to `15` minutes of temporary unlock per clock hour (`QUOTA_MINUTES_PER_HOUR`) after a `10`-second mindfulness countdown (`FRICTION_SECONDS`).
- A `TimeInterval` with `startMinute == endMinute` is invalid and must never reach schedule evaluation. Validate at the write boundary (`addInterval`, `updateInterval`) and the read boundary (`getIntervals`). Do not add an `init`/`require` guard to `TimeInterval`: Gson bypasses it, and with a constructor-invoking parser one corrupt entry would drop the whole list instead of being filtered out.

## Non-Obvious Architecture Decisions

- **Isolated Lock Task:** `LockScreenActivity` is configured with `launchMode="singleTask"`, `taskAffinity="${applicationId}.lock"`, and `excludeFromRecents="true"` so calling `finish()` after an unlock returns directly to the guarded app's task without dragging `MainActivity` to the foreground. Pressing Back routes to the Home launcher; `onUserLeaveHint()` finishes the activity so the 10-second friction countdown resets on the next open.
- **Strong Preference Listener Reference:** `SharedPreferences` holds `OnSharedPreferenceChangeListener` instances weakly; listeners in `AppBlockerService` (and UI observers) must be held in a strong reference for their active lifecycle to prevent silent garbage collection.
- **Pass-Through Accessibility Overlay:** `AppBlockerService` attaches the gray overlay using `WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY` with `FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE` and `IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS` so it renders directly via the accessibility service token while passing all touches, back presses, and accessibility events through to the underlying app.
- **Real Activity Switch Filtering:** `AppBlockerService.onAccessibilityEvent` ignores `com.android.systemui` and only updates `foregroundPackage` when the package matches a `TargetApp`, a resolved home launcher, or a verified `Activity` (`PackageManager.getActivityInfo` cached in `activityCache`), preventing keyboards, toasts, notification shades, and the overlay itself from hiding the overlay or re-triggering the lock screen.

## Time Edge Cases (`SettingsManager.evaluate()` & `startUnlock()`)

- **120-Minute Boundary & Interval Merging:** `evaluate()` projects all custom intervals onto a `BooleanArray(1440)` coverage map and walks backward/forward from `nowMinute` to measure the merged continuous block `length`. `length <= 120` sets `unlockEligible = false` (hard block); `length > 120` (or all 1440 minutes covered) sets `unlockEligible = !hardBlock`.
- **Intervals Starting or Ending at `00:00`:** Minutes run `0..1439` (`startMinute == endMinute` is rejected as `INVALID_SAME_TIME`). An interval starting at `00:00` (`startMinute = 0, endMinute > 0`) has `crossesMidnight = false`. An interval ending at `00:00` (`endMinute = 0`) satisfies `endMinute <= startMinute`, so `crossesMidnight` is `true` and the coverage loop wraps from `startMinute..1439` and terminates at `0`.
- **Quota Splitting Across Hour Boundaries:** `startUnlock(minutes, nowMillis)` clamps `minutes` to `1..remainingQuotaMinutes(nowMillis)` (computed via integer division `remainingMs / 60_000L` for the current clock hour), prunes `hourly_usage` keys older than `currentHourStart`, and splits `durationMs` across calendar hours (`ZonedDateTime.truncatedTo(ChronoUnit.HOURS).plusHours(1)`), charging each hour's epoch-millis string key and setting `unlock_until = nowMillis + durationMs`.
- **DST Transitions & Timezone Changes:** `evaluate(LocalDateTime)` uses nominal wall-clock minutes (`hour * 60 + minute`) and does not adjust `periodMinutes` for DST spring-forward or fall-back shifts. Active unlock expiry compares UTC epoch milliseconds (`unlockUntilMillis > nowMillis`), so a timezone change during an unlock does not alter remaining unlock time, whereas `toLocalDateTime()` and `hourStartMillis()` dynamically read `ZoneId.systemDefault()`.

## Coding, Localization & Persistence Standards

- **CRITICAL — Persistence Compatibility:** Do not change any of the following without an explicit migration path:
  1. SharedPreferences file name: `"focusguard_settings"`.
  2. Preference keys: `"enabled"`, `"schedule_mode"`, `"hard_block"`, `"intervals"`, `"hourly_usage"`, `"unlock_until"`, `"app_language"`, and key prefix `"app_selected_"` (`app_selected_<TargetApp.name>`).
  3. JSON schemas: `TimeInterval` (`id`, `startMinute`, `endMinute`, kept for Gson in `proguard-rules.pro`) and `hourly_usage` (`Map<String, Long>` mapping hour-start epoch millis string to used millis).
- **8-Language Localization Parity:** Do not hardcode user-visible strings in Compose. Add every UI string to `AppStrings` (`LocalAppStrings`) and update all 8 translations in `i18n/*Strings.kt` (`EN`, `UK`, `DE`, `ES`, `FR`, `PT`, `IT`, `PL`), plus `res/values*/strings.xml` and `locales_config.xml` for system-facing strings.
- **Compose & Platform Hygiene:** Keep `ui/components/` composables `internal` and stateless with hoisted state/callbacks. Use `java.time.*` APIs and respect `DateFormat.is24HourFormat(context)`. Wrap `WindowManager`, `PackageManager`, `startActivity`, and `Gson` calls in `try/catch` with safe fallbacks so background enforcement never crashes.

## Testing & Verification

- Verify every code change with `./gradlew assembleDebug testDebugUnitTest`.
- Any change to `evaluate()`, `startUnlock()`, or quota logic must include unit tests covering the affected time/quota edge cases.
- Recommend splitting `ExampleUnitTest.kt` into focused test classes (`ScheduleEvaluationTest`, `QuotaChargingTest`, `TargetAppTest`, `AppStringsTest`), but ask for confirmation before renaming or moving existing test files.

## Workflow & Strict Constraints

1. **CRITICAL — Clarify Ambiguity First:** Never guess when requirements affecting schedule evaluation math, quota charging, or accessibility/overlay behavior are incomplete or ambiguous. Pause and ask clarifying questions before writing code.
2. **CRITICAL — Privacy & Offline Guarantee:** Never add `android.permission.INTERNET`, network clients, analytics, telemetry, or ads. Never change `android:canRetrieveWindowContent="false"` in `accessibility_service_config.xml` or inspect screen text/node hierarchies.
3. **CRITICAL — Blocking Integrity:** Never allow the Back button on `LockScreenActivity` to reveal the blocked app underneath, never allow zero selected `TargetApp`s, and never leak `WindowManager` overlay views when leaving a guarded app or destroying `AppBlockerService`.
4. **Permissions & Build Files:** Ask for explicit approval before adding any new permission to `AndroidManifest.xml`, adding third-party dependencies to `gradle/libs.versions.toml`, or modifying Gradle, manifest, or CI (`.github/workflows/build.yml`) files.
5. **Commits, Docs & Language:** Keep commits atomic (one logical change per commit) using English Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `ci:`). Write all code, comments, and KDoc in English. Update `README.md` whenever user-visible behavior, supported apps, or supported languages change (no `CHANGELOG.md` is used).
