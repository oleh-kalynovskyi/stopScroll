package com.example.scrollstop.i18n

import com.example.scrollstop.AppLanguage
import com.example.scrollstop.AppStrings

val EnglishStrings =
    AppStrings(
        language = AppLanguage.EN,
        appSubtitle = "Instagram, TikTok, X, Snapchat & Threads blocker",
        languageMenuTitle = "App language",
        languageSystem = "System language",
        permissionsTitle = "Permissions Dashboard",
        permissionsAllActive = "All required system permissions are active.",
        permissionsGrantBoth =
            "Grant both permissions so FocusGuard can detect and dim Instagram, TikTok, X, Snapchat, and Threads.",
        permOverlayTitle = "Display Over Other Apps",
        permOverlaySubtitle = "Renders the pass-through gray tint during temporary unlocks",
        permAccessibilityTitle = "Accessibility Service",
        permAccessibilitySubtitle =
            "Detects when Instagram, TikTok, X, Snapchat, or Threads comes to the foreground",
        actionSettings = "Settings",
        actionGrant = "Grant",
        masterBlockerTitle = "Master Blocker",
        blockedAppsTitle = "Guarded apps",
        blockedAppsHint = "Select which apps to block (at least one)",
        minOneAppRequiredHint = "At least one app must remain selected.",
        appsConjunction = " & ",
        statusPaused = "Blocker is paused",
        statusWaitingAccessibility = "Waiting for Accessibility Service permission",
        statusUnlockActive = { mm, ss ->
            "Temporary unlock active · %d:%02d left (gray screen on)".format(mm, ss)
        },
        statusHardBlockingNow = { appsLabel, untilPart ->
            "Hard-blocking $appsLabel now$untilPart"
        },
        statusBlockingNow = { appsLabel, untilPart ->
            "Blocking $appsLabel now$untilPart · 15m/h unlock allowed"
        },
        statusArmedOutsideHours = "Armed · outside scheduled blocking hours",
        untilTime = { time -> " until $time" },
        allDaySuffix = " (24/7)",
        endUnlockNowButton = "End unlock & re-lock apps now",
        scheduleTitle = "Blocking Schedule",
        modeAllDay = "All Day (24/7)",
        modeCustomPeriods = "Custom Periods",
        ruleHardBlockOn =
            "Hard Block is ON: no temporary unlocks are offered in any mode. " +
                "Selected apps stay locked until you turn the blocker off here.",
        ruleAllDay = { quotaMin, frictionSec ->
            "All Day is > 2 hours, so you can unlock selected apps for up to $quotaMin minutes in total per clock hour " +
                "(after a ${frictionSec}s pause, with a gray screen)."
        },
        ruleCustomEmpty = { quotaMin ->
            "Add one or more time periods below. Periods longer than 2 hours allow up to ${quotaMin}m/h " +
                "temporary unlock; periods of 2 hours or less are always hard-blocked."
        },
        ruleCustomWithIntervals = { quotaMin ->
            "Periods longer than 2 hours (> 120 min) allow up to ${quotaMin}m/h temporary unlock. " +
                "Periods of 2 hours or less (<= 120 min) act as a Hard Block automatically."
        },
        hardBlockCheckboxTitle = "Hard Block (disable all temporary unlocks)",
        hardBlockCheckboxSubtitle = "Applies to both All Day and Custom Time Periods.",
        customPeriodsHeader = { count -> "Custom Time Periods ($count)" },
        overnightSupportedSubtitle = "Overnight ranges (e.g. 22:00 – 02:00) are supported",
        addPeriodChip = "Add period",
        noPeriodsTitle = "No time periods yet",
        noPeriodsSubtitle =
            "Selected apps won't be blocked in Custom mode until you add at least one interval.",
        addTimePeriodButton = "Add Time Period",
        activeBadge = "Active",
        intervalHardBlockTag = "Hard Block enabled",
        intervalUnlockEligibleTag = "> 2h · 15m/h unlock eligible",
        intervalShortHardBlockTag = "<= 2h · Hard Block (no unlock)",
        overnightTag = " · overnight",
        deletePeriodContentDesc = "Delete time period",
        deletePeriodDialogTitle = "Delete time period?",
        deletePeriodDialogMessage = { range ->
            "Are you sure you want to delete the time period $range?"
        },
        deletePeriodConfirmButton = "Delete",
        quotaTitle = "This Hour's Unlock Quota",
        quotaLeftLabel = { remaining, total -> "$remaining / $total min left" },
        quotaUsedSubtitle = { used, resetTime ->
            "$used min used in the current clock hour · quota resets at $resetTime."
        },
        pickerStartTimeTitle = "Start time",
        pickerEndTimeTitle = { start -> "End time (from $start)" },
        pickerNextButton = "Next",
        pickerAddButton = "Add period",
        pickerSaveButton = "Save",
        pickerCancelButton = "Cancel",
        pickerToggleInputDesc = "Toggle time input mode",
        errSameStartEnd =
            "Start and end times are the same. Pick two different times (use All Day mode for 24/7).",
        errDuplicateInterval = "That time period is already in your list.",
        lockTitle = { appName -> "$appName is blocked" },
        lockHardBlockEnabledReason =
            "Hard Block is on. There is no way to unlock from here.",
        lockShortPeriodReason = { duration ->
            "This focus period is $duration long. Periods of 2 hours or less can't be unlocked."
        },
        lockLiftsAt = { time -> "The block lifts at $time." },
        lockRunsAllDay = "This block runs all day.",
        lockChangeInAppHint =
            "To lift it earlier, open FocusGuard and change your settings.",
        lockFrictionPrompt = { appName ->
            "Take a breath. Do you really need $appName right now?"
        },
        lockUnlockForLabel = "Unlock for",
        lockMinutesValue = { min -> "$min min" },
        lockQuotaRemainingSub = { remaining, total, resetsAt ->
            "$remaining of $total min left this hour · resets at $resetsAt"
        },
        lockUnlockButton = { appName -> "Unlock $appName" },
        lockGrayScreenHint = "The screen will be tinted gray while unlocked.",
        lockQuotaExhaustedTitle = "15-minute hourly limit reached. Try again next hour.",
        lockNextWindowOpensAt = { resetsAt ->
            "Your next unlock window opens at $resetsAt."
        },
        lockExitHomeButton = "Exit to Home Screen",
        formatDuration = { minutes ->
            val hours = minutes / 60
            val mins = minutes % 60
            when {
                hours == 0 -> "${mins}m"
                mins == 0 -> "${hours}h"
                else -> "${hours}h ${mins}m"
            }
        },
    )
