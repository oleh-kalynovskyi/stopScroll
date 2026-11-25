package com.example.scrollstop.i18n

import com.example.scrollstop.AppLanguage
import com.example.scrollstop.AppStrings

val ItalianStrings =
    AppStrings(
        language = AppLanguage.IT,
        appSubtitle = "Blocco per Instagram, TikTok, X, Snapchat e Threads",
        languageMenuTitle = "Lingua dell'app",
        languageSystem = "Lingua di sistema",
        permissionsTitle = "Pannello autorizzazioni",
        permissionsAllActive = "Tutte le autorizzazioni di sistema richieste sono attive.",
        permissionsGrantBoth =
            "Concedi entrambe le autorizzazioni affinché FocusGuard possa rilevare e oscurare Instagram, TikTok, X, Snapchat e Threads.",
        permOverlayTitle = "Mostra sopra altre app",
        permOverlaySubtitle =
            "Applica il filtro grigio trasparente durante gli sblocchi temporanei",
        permAccessibilityTitle = "Servizio di accessibilità",
        permAccessibilitySubtitle =
            "Rileva quando Instagram, TikTok, X, Snapchat o Threads viene aperto in primo piano",
        actionSettings = "Impostazioni",
        actionGrant = "Concedi",
        masterBlockerTitle = "Blocco principale",
        blockedAppsTitle = "App protette",
        blockedAppsHint = "Seleziona quali app bloccare (almeno una)",
        minOneAppRequiredHint = "Almeno un'app deve rimanere selezionata.",
        appsConjunction = " e ",
        statusPaused = "Il blocco è in pausa",
        statusWaitingAccessibility = "In attesa dell'autorizzazione del Servizio di accessibilità",
        statusUnlockActive = { mm, ss ->
            "Sblocco temporaneo attivo · %d:%02d rimasti (schermo grigio attivo)".format(mm, ss)
        },
        statusHardBlockingNow = { appsLabel, untilPart ->
            "Blocco rigoroso di $appsLabel attivo$untilPart"
        },
        statusBlockingNow = { appsLabel, untilPart ->
            "Blocco di $appsLabel attivo$untilPart · sblocco di 15 min/h consentito"
        },
        statusArmedOutsideHours = "Attivo · fuori dagli orari di blocco programmati",
        untilTime = { time -> " fino alle $time" },
        allDaySuffix = " (24/7)",
        endUnlockNowButton = "Termina sblocco e riblocca ora",
        scheduleTitle = "Programmazione blocco",
        modeAllDay = "Tutto il giorno (24/7)",
        modeCustomPeriods = "Fasce personalizzate",
        ruleHardBlockOn =
            "Il Blocco rigoroso è ATTIVO: non sono consentiti sblocchi temporanei in nessuna modalità. " +
                "Le app selezionate restano bloccate finché non disattivi il blocco qui.",
        ruleAllDay = { quotaMin, frictionSec ->
            "La modalità Tutto il giorno dura > 2 ore, quindi puoi sbloccare le app selezionate fino a $quotaMin minuti totali all'ora " +
                "(dopo una pausa di ${frictionSec}s, con schermo grigio)."
        },
        ruleCustomEmpty = { quotaMin ->
            "Aggiungi una o più fasce orarie qui sotto. Le fasce superiori a 2 ore consentono fino a $quotaMin min/h " +
                "di sblocco temporaneo; le fasce di 2 ore o meno sono sempre a blocco rigoroso."
        },
        ruleCustomWithIntervals = { quotaMin ->
            "Le fasce superiori a 2 ore (> 120 min) consentono fino a $quotaMin min/h di sblocco temporaneo. " +
                "Le fasce di 2 ore o meno (<= 120 min) attivano automaticamente il Blocco rigoroso."
        },
        hardBlockCheckboxTitle = "Blocco rigoroso (disattiva tutti gli sblocchi temporanei)",
        hardBlockCheckboxSubtitle =
            "Si applica sia a Tutto il giorno sia alle Fasce personalizzate.",
        customPeriodsHeader = { count -> "Fasce orarie personalizzate ($count)" },
        overnightSupportedSubtitle = "Gli intervalli notturni (es. 22:00 – 02:00) sono supportati",
        addPeriodChip = "Aggiungi fascia",
        noPeriodsTitle = "Nessuna fascia oraria",
        noPeriodsSubtitle =
            "Le app selezionate non verranno bloccate in modalità personalizzata finché non aggiungi almeno un intervallo.",
        addTimePeriodButton = "Aggiungi fascia oraria",
        activeBadge = "Attivo",
        intervalHardBlockTag = "Blocco rigoroso attivo",
        intervalUnlockEligibleTag = "> 2h · sblocco 15 min/h disponibile",
        intervalShortHardBlockTag = "<= 2h · Blocco rigoroso (nessuno sblocco)",
        overnightTag = " · notturno",
        deletePeriodContentDesc = "Elimina fascia oraria",
        quotaTitle = "Quota di sblocco per quest'ora",
        quotaLeftLabel = { remaining, total -> "$remaining / $total min rimasti" },
        quotaUsedSubtitle = { used, resetTime ->
            "$used min usati nell'ora corrente · la quota si azzera alle $resetTime."
        },
        pickerStartTimeTitle = "Ora di inizio",
        pickerEndTimeTitle = { start -> "Ora di fine (da $start)" },
        pickerNextButton = "Avanti",
        pickerAddButton = "Aggiungi fascia",
        pickerSaveButton = "Salva",
        pickerCancelButton = "Annulla",
        pickerToggleInputDesc = "Cambia modalità di inserimento orario",
        errSameStartEnd =
            "L'ora di inizio e di fine coincidono. Scegli due orari diversi (usa Tutto il giorno per 24/7).",
        errDuplicateInterval = "Questa fascia oraria è già presente nell'elenco.",
        lockTitle = { appName -> "$appName è bloccato" },
        lockHardBlockEnabledReason =
            "Il Blocco rigoroso è attivo. Non è possibile sbloccare da qui.",
        lockShortPeriodReason = { duration ->
            "Questa fascia di concentrazione dura $duration. Le fasce di 2 ore o meno non possono essere sbloccate."
        },
        lockLiftsAt = { time -> "Il blocco termina alle $time." },
        lockRunsAllDay = "Questo blocco è attivo tutto il giorno.",
        lockChangeInAppHint =
            "Per rimuoverlo prima, apri FocusGuard e modifica le impostazioni.",
        lockFrictionPrompt = { appName ->
            "Fai un respiro profondo. Hai davvero bisogno di $appName adesso?"
        },
        lockUnlockForLabel = "Sblocca per",
        lockMinutesValue = { min -> "$min min" },
        lockQuotaRemainingSub = { remaining, total, resetsAt ->
            "$remaining di $total min rimasti in quest'ora · si azzera alle $resetsAt"
        },
        lockUnlockButton = { appName -> "Sblocca $appName" },
        lockGrayScreenHint = "Lo schermo sarà in scala di grigi durante lo sblocco.",
        lockQuotaExhaustedTitle =
            "Limite orario di 15 minuti raggiunto. Riprova nella prossima ora.",
        lockNextWindowOpensAt = { resetsAt ->
            "La prossima finestra di sblocco si apre alle $resetsAt."
        },
        lockExitHomeButton = "Torna alla schermata Home",
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
