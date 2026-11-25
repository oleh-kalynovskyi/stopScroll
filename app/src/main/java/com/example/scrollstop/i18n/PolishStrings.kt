package com.example.scrollstop.i18n

import com.example.scrollstop.AppLanguage
import com.example.scrollstop.AppStrings

val PolishStrings =
    AppStrings(
        language = AppLanguage.PL,
        appSubtitle = "Blokada Instagram, TikTok, X, Snapchat i Threads",
        languageMenuTitle = "Język aplikacji",
        languageSystem = "Język systemowy",
        permissionsTitle = "Panel uprawnień",
        permissionsAllActive = "Wszystkie wymagane uprawnienia systemowe są aktywne.",
        permissionsGrantBoth =
            "Przyznaj oba uprawnienia, aby FocusGuard mógł wykrywać i przyciemniać aplikacje Instagram, TikTok, X, Snapchat oraz Threads.",
        permOverlayTitle = "Wyświetlanie nad innymi aplikacjami",
        permOverlaySubtitle =
            "Nakłada półprzezroczysty szary filtr podczas tymczasowego odblokowania",
        permAccessibilityTitle = "Usługa ułatwień dostępu",
        permAccessibilitySubtitle =
            "Wykrywa uruchomienie aplikacji Instagram, TikTok, X, Snapchat lub Threads na pierwszym planie",
        actionSettings = "Ustawienia",
        actionGrant = "Przyznaj",
        masterBlockerTitle = "Główna blokada",
        blockedAppsTitle = "Chronione aplikacje",
        blockedAppsHint = "Wybierz aplikacje do blokowania (co najmniej jedną)",
        minOneAppRequiredHint = "Co najmniej jedna aplikacja musi pozostać wybrana.",
        appsConjunction = " i ",
        statusPaused = "Blokada jest wstrzymana",
        statusWaitingAccessibility = "Oczekiwanie na uprawnienie Usługi ułatwień dostępu",
        statusUnlockActive = { mm, ss ->
            "Tymczasowe odblokowanie · pozostało %d:%02d (szary ekran)".format(mm, ss)
        },
        statusHardBlockingNow = { appsLabel, untilPart ->
            "Ścisła blokada $appsLabel jest aktywna$untilPart"
        },
        statusBlockingNow = { appsLabel, untilPart ->
            "Blokowanie $appsLabel$untilPart · dostępne odblokowanie 15 min/godz."
        },
        statusArmedOutsideHours = "Włączono · poza zaplanowanymi godzinami blokowania",
        statusSessionActive = { mm, ss ->
            "Swobodny dostęp · pozostało %d:%02d w sesji".format(mm, ss)
        },
        untilTime = { time -> " do $time" },
        allDaySuffix = " (24/7)",
        endUnlockNowButton = "Zakończ odblokowanie i zablokuj teraz",
        scheduleTitle = "Harmonogram blokowania",
        modeAllDay = "Cały dzień",
        modeCustomPeriods = "Przedziały",
        modeSessionLimit = "Sesja",
        ruleHardBlockOn =
            "Ścisła blokada jest WŁĄCZONA: tymczasowe odblokowania są niedostępne w żadnym trybie. " +
                "Wybrane aplikacje pozostają zablokowane, dopóki nie wyłączysz blokady tutaj.",
        ruleAllDay = { quotaMin, frictionSec ->
            "Tryb Cały dzień trwa > 2 godziny, więc możesz odblokować wybrane aplikacje łącznie na maks. $quotaMin minut na godzinę " +
                "(po ${frictionSec}s przerwy, z szarym ekranem)."
        },
        ruleCustomEmpty = { quotaMin ->
            "Dodaj jeden lub więcej przedziałów czasowych poniżej. Przedziały dłuższe niż 2 godziny pozwalają na tymczasowe " +
                "odblokowanie do $quotaMin min/godz.; przedziały do 2 godzin są zawsze ściśle blokowane."
        },
        ruleCustomWithIntervals = { quotaMin ->
            "Przedziały dłuższe niż 2 godziny (> 120 min) pozwalają na odblokowanie do $quotaMin min/godz. " +
                "Przedziały trwające 2 godziny lub krócej (<= 120 min) działają automatycznie jako Ścisła blokada."
        },
        ruleSessionLimit = { limitMin, cooldownDuration ->
            "Korzystaj swobodnie z wybranych aplikacji do $limitMin min bez szarego ekranu, po czym zostaną ściśle zablokowane na $cooldownDuration. " +
                "Przerwa trwająca $cooldownDuration resetuje licznik."
        },
        hardBlockCheckboxTitle = "Ścisła blokada (wyłącz wszystkie tymczasowe odblokowania)",
        hardBlockCheckboxSubtitle =
            "Dotyczy zarówno trybu Cały dzień, jak i Własnych przedziałów.",
        sessionLimitCardTitle = "Dozwolony czas sesji",
        sessionStatusRemaining = { mm, ss, limitMin ->
            "Pozostało %d:%02d z %d min · bez szarego ekranu".format(mm, ss, limitMin)
        },
        sessionStatusCooldown = { untilTime, remainingMin ->
            "Zablokowano do $untilTime (pozostało $remainingMin min)"
        },
        sessionResetsAtHint = { limitMin, resetTime ->
            "Zresetuje się do $limitMin min o $resetTime bez użycia."
        },
        sessionCooldownHint = { cooldownDuration ->
            "Wspólny limit dla wszystkich wybranych aplikacji. Po jego wyczerpaniu włącza się ścisła blokada na $cooldownDuration; " +
                "brak aktywności przez $cooldownDuration resetuje sesję."
        },
        sessionWarningBanner = { minutesLeft ->
            "FocusGuard · pozostały $minutesLeft min do blokady na 1 godz."
        },
        customPeriodsHeader = { count -> "Własne przedziały czasowe ($count)" },
        overnightSupportedSubtitle = "Przedziały nocne (np. 22:00 – 02:00) są obsługiwane",
        addPeriodChip = "Dodaj przedział",
        noPeriodsTitle = "Brak przedziałów czasowych",
        noPeriodsSubtitle =
            "Wybrane aplikacje nie będą blokowane w trybie własnym, dopóki nie dodasz co najmniej jednego przedziału.",
        addTimePeriodButton = "Dodaj przedział czasowy",
        activeBadge = "Aktywny",
        intervalHardBlockTag = "Ścisła blokada",
        intervalUnlockEligibleTag = "> 2 godz. · dostępne 15 min/godz.",
        intervalShortHardBlockTag = "<= 2 godz. · Ścisła blokada (bez odblokowania)",
        overnightTag = " · nocny",
        deletePeriodContentDesc = "Usuń przedział czasowy",
        deletePeriodDialogTitle = "Usunąć przedział czasowy?",
        deletePeriodDialogMessage = { range ->
            "Czy na pewno chcesz usunąć przedział czasowy $range?"
        },
        deletePeriodConfirmButton = "Usuń",
        quotaTitle = "Limit odblokowania w tej godzinie",
        quotaLeftLabel = { remaining, total -> "Pozostało $remaining / $total min" },
        quotaUsedSubtitle = { used, resetTime ->
            "Wykorzystano $used min w bieżącej godzinie · reset limitu o $resetTime."
        },
        pickerStartTimeTitle = "Godzina rozpoczęcia",
        pickerEndTimeTitle = { start -> "Godzina zakończenia (od $start)" },
        pickerNextButton = "Dalej",
        pickerAddButton = "Dodaj przedział",
        pickerSaveButton = "Zapisz",
        pickerCancelButton = "Anuluj",
        pickerToggleInputDesc = "Przełącz tryb wprowadzania godziny",
        errSameStartEnd =
            "Godzina rozpoczęcia i zakończenia są takie same. Wybierz różne godziny (dla 24/7 użyj trybu Cały dzień).",
        errDuplicateInterval = "Ten przedział czasowy jest już na liście.",
        lockTitle = { appName -> "Aplikacja $appName jest zablokowana" },
        lockHardBlockEnabledReason =
            "Ścisła blokada jest włączona. Odblokowanie z tego ekranu nie jest możliwe.",
        lockShortPeriodReason = { duration ->
            "Ten okres skupienia trwa $duration. Przedziałów trwających 2 godziny lub krócej nie można odblokować."
        },
        lockSessionCooldownReason = { limitMin, cooldownDuration ->
            "Limit sesji ($limitMin min) został wyczerpany. Chronione aplikacje są wstrzymane na $cooldownDuration."
        },
        lockLiftsAt = { time -> "Blokada kończy się o $time." },
        lockRunsAllDay = "Ta blokada działa przez cały dzień.",
        lockChangeInAppHint =
            "Aby zdjąć blokadę wcześniej, otwórz FocusGuard i zmień ustawienia.",
        lockFrictionPrompt = { appName ->
            "Weź głęboki oddech. Czy naprawdę potrzebujesz teraz aplikacji $appName?"
        },
        lockUnlockForLabel = "Odblokuj na",
        lockMinutesValue = { min -> "$min min" },
        lockQuotaRemainingSub = { remaining, total, resetsAt ->
            "Pozostało $remaining z $total min w tej godzinie · reset o $resetsAt"
        },
        lockUnlockButton = { appName -> "Odblokuj $appName" },
        lockGrayScreenHint = "Podczas odblokowania ekran będzie czarno-biały.",
        lockQuotaExhaustedTitle =
            "Osiągnięto 15-minutowy limit na tę godzinę. Spróbuj ponownie w następnej godzinie.",
        lockNextWindowOpensAt = { resetsAt ->
            "Następne okno odblokowania otworzy się o $resetsAt."
        },
        lockExitHomeButton = "Wróć do ekranu głównego",
        formatDuration = { minutes ->
            val hours = minutes / 60
            val mins = minutes % 60
            when {
                hours == 0 -> "$mins min"
                mins == 0 -> "$hours godz."
                else -> "$hours godz. $mins min"
            }
        },
    )
