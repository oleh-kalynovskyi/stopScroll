package com.example.scrollstop.i18n

import com.example.scrollstop.AppLanguage
import com.example.scrollstop.AppStrings

val GermanStrings =
    AppStrings(
        language = AppLanguage.DE,
        appSubtitle = "Instagram-, TikTok-, X-, Snapchat- & Threads-Blocker",
        languageMenuTitle = "App-Sprache",
        languageSystem = "Systemsprache",
        permissionsTitle = "Berechtigungsübersicht",
        permissionsAllActive = "Alle erforderlichen Systemberechtigungen sind aktiv.",
        permissionsGrantBoth =
            "Erteile beide Berechtigungen, damit FocusGuard Instagram, TikTok, X, Snapchat und Threads erkennen und abdunkeln kann.",
        permOverlayTitle = "Über anderen Apps einblenden",
        permOverlaySubtitle =
            "Legt während einer temporären Entsperrung einen grauen Filter über den Bildschirm",
        permAccessibilityTitle = "Bedienungshilfen-Dienst",
        permAccessibilitySubtitle =
            "Erkennt, wenn Instagram, TikTok, X, Snapchat oder Threads im Vordergrund geöffnet wird",
        actionSettings = "Einstellungen",
        actionGrant = "Erteilen",
        masterBlockerTitle = "Hauptschalter",
        blockedAppsTitle = "Geschützte Apps",
        blockedAppsHint = "Wähle die zu blockierenden Apps aus (mindestens eine)",
        minOneAppRequiredHint = "Mindestens eine App muss ausgewählt bleiben.",
        appsConjunction = " & ",
        statusPaused = "Blocker ist pausiert",
        statusWaitingAccessibility = "Warte auf Berechtigung für Bedienungshilfen",
        statusUnlockActive = { mm, ss ->
            "Temporäre Entsperrung aktiv · %d:%02d übrig (Graufilter an)".format(mm, ss)
        },
        statusHardBlockingNow = { appsLabel, untilPart ->
            "Strikte Sperre für $appsLabel aktiv$untilPart"
        },
        statusBlockingNow = { appsLabel, untilPart ->
            "Blockiere $appsLabel$untilPart · 15 Min./Std. Entsperrung erlaubt"
        },
        statusArmedOutsideHours = "Aktiviert · außerhalb der geplanten Sperrzeiten",
        untilTime = { time -> " bis $time" },
        allDaySuffix = " (24/7)",
        endUnlockNowButton = "Entsperrung beenden & Apps jetzt sperren",
        scheduleTitle = "Sperrzeitplan",
        modeAllDay = "Ganztägig (24/7)",
        modeCustomPeriods = "Eigene Zeiträume",
        ruleHardBlockOn =
            "Strikte Sperre ist AN: In keinem Modus sind temporäre Entsperrungen möglich. " +
                "Ausgewählte Apps bleiben gesperrt, bis du den Blocker hier ausschaltets.",
        ruleAllDay = { quotaMin, frictionSec ->
            "Ganztägig dauert > 2 Stunden, daher kannst du ausgewählte Apps für insgesamt bis zu $quotaMin Minuten pro Stunde entsperren " +
                "(nach einer ${frictionSec}s-Pause, mit grauem Bildschirm)."
        },
        ruleCustomEmpty = { quotaMin ->
            "Füge unten einen oder mehrere Zeiträume hinzu. Zeiträume über 2 Stunden erlauben bis zu $quotaMin Min./Std. " +
                "temporäre Entsperrung; Zeiträume bis 2 Stunden sind immer strikt gesperrt."
        },
        ruleCustomWithIntervals = { quotaMin ->
            "Zeiträume über 2 Stunden (> 120 Min.) erlauben bis zu $quotaMin Min./Std. temporäre Entsperrung. " +
                "Zeiträume von 2 Stunden oder weniger (<= 120 Min.) wirken automatisch als strikte Sperre."
        },
        hardBlockCheckboxTitle = "Strikte Sperre (alle temporären Entsperrungen deaktivieren)",
        hardBlockCheckboxSubtitle = "Gilt sowohl für Ganztägig als auch für eigene Zeiträume.",
        customPeriodsHeader = { count -> "Eigene Zeiträume ($count)" },
        overnightSupportedSubtitle = "Zeiträume über Mitternacht (z. B. 22:00 – 02:00) werden unterstützt",
        addPeriodChip = "Zeitraum hinzufügen",
        noPeriodsTitle = "Noch keine Zeiträume",
        noPeriodsSubtitle =
            "Ausgewählte Apps werden im Modus „Eigene Zeiträume“ erst blockiert, wenn du mindestens ein Intervall hinzufügst.",
        addTimePeriodButton = "Zeitraum hinzufügen",
        activeBadge = "Aktiv",
        intervalHardBlockTag = "Strikte Sperre aktiv",
        intervalUnlockEligibleTag = "> 2 Std. · 15 Min./Std. Entsperrung möglich",
        intervalShortHardBlockTag = "<= 2 Std. · Strikte Sperre (keine Entsperrung)",
        overnightTag = " · über Nacht",
        deletePeriodContentDesc = "Zeitraum löschen",
        quotaTitle = "Entsperrkontingent dieser Stunde",
        quotaLeftLabel = { remaining, total -> "$remaining / $total Min. übrig" },
        quotaUsedSubtitle = { used, resetTime ->
            "$used Min. in der aktuellen Stunde genutzt · Zurücksetzung um $resetTime."
        },
        pickerStartTimeTitle = "Startzeit",
        pickerEndTimeTitle = { start -> "Endzeit (ab $start)" },
        pickerNextButton = "Weiter",
        pickerAddButton = "Hinzufügen",
        pickerSaveButton = "Speichern",
        pickerCancelButton = "Abbrechen",
        pickerToggleInputDesc = "Zeiteingabemodus wechseln",
        errSameStartEnd =
            "Start- und Endzeit sind identisch. Wähle zwei verschiedene Zeiten (nutze „Ganztägig“ für 24/7).",
        errDuplicateInterval = "Dieser Zeitraum ist bereits in deiner Liste.",
        lockTitle = { appName -> "$appName ist gesperrt" },
        lockHardBlockEnabledReason =
            "Strikte Sperre ist aktiv. Eine Entsperrung ist von hier aus nicht möglich.",
        lockShortPeriodReason = { duration ->
            "Dieser Fokuszeitraum dauert $duration. Zeiträume von 2 Stunden oder weniger können nicht entsperrt werden."
        },
        lockLiftsAt = { time -> "Die Sperre endet um $time." },
        lockRunsAllDay = "Diese Sperre gilt den ganzen Tag.",
        lockChangeInAppHint =
            "Um sie früher aufzuheben, öffne FocusGuard und ändere deine Einstellungen.",
        lockFrictionPrompt = { appName ->
            "Atme tief durch. Brauchst du $appName jetzt wirklich?"
        },
        lockUnlockForLabel = "Entsperren für",
        lockMinutesValue = { min -> "$min Min." },
        lockQuotaRemainingSub = { remaining, total, resetsAt ->
            "$remaining von $total Min. in dieser Stunde übrig · Zurücksetzung um $resetsAt"
        },
        lockUnlockButton = { appName -> "$appName entsperren" },
        lockGrayScreenHint = "Der Bildschirm wird während der Entsperrung grau gefärbt.",
        lockQuotaExhaustedTitle =
            "15-Minuten-Stundenlimit erreicht. Versuche es in der nächsten Stunde erneut.",
        lockNextWindowOpensAt = { resetsAt ->
            "Dein nächstes Entsperrfenster öffnet um $resetsAt."
        },
        lockExitHomeButton = "Zum Startbildschirm",
        formatDuration = { minutes ->
            val hours = minutes / 60
            val mins = minutes % 60
            when {
                hours == 0 -> "$mins Min."
                mins == 0 -> "$hours Std."
                else -> "$hours Std. $mins Min."
            }
        },
    )
