package com.example.scrollstop.i18n

import com.example.scrollstop.AppLanguage
import com.example.scrollstop.AppStrings

val FrenchStrings =
        AppStrings(
                language = AppLanguage.FR,
                appSubtitle = "Bloqueur Instagram, TikTok, X, Snapchat et Threads",
                languageMenuTitle = "Langue de l'application",
                languageSystem = "Langue du système",
                permissionsTitle = "Tableau des autorisations",
                permissionsAllActive = "Toutes les autorisations système requises sont actives.",
                permissionsGrantBoth =
                        "Accordez les deux autorisations pour que FocusGuard puisse détecter et assombrir Instagram, TikTok, X, Snapchat et Threads.",
                permOverlayTitle = "Superposer sur d'autres applications",
                permOverlaySubtitle =
                        "Affiche un filtre gris transparent lors des déverrouillages temporaires",
                permAccessibilityTitle = "Service d'accessibilité",
                permAccessibilitySubtitle =
                        "Détecte l'ouverture d'Instagram, TikTok, X, Snapchat ou Threads au premier plan",
                actionSettings = "Paramètres",
                actionGrant = "Autoriser",
                masterBlockerTitle = "Bloqueur principal",
                blockedAppsTitle = "Applications surveillées",
                blockedAppsHint = "Sélectionnez les applications à bloquer (au moins une)",
                minOneAppRequiredHint = "Au moins une application doit rester sélectionnée.",
                appsConjunction = " et ",
                statusPaused = "Le bloqueur est en pause",
                statusWaitingAccessibility =
                        "En attente de l'autorisation du Service d'accessibilité",
                statusUnlockActive = { mm, ss ->
                    "Déverrouillage temporaire actif · %d:%02d restantes (écran gris activé)".format(
                            mm,
                            ss
                    )
                },
                statusHardBlockingNow = { appsLabel, untilPart ->
                    "Blocage strict de $appsLabel actif$untilPart"
                },
                statusBlockingNow = { appsLabel, untilPart ->
                    "Blocage de $appsLabel actif$untilPart · déverrouillage 15 min/h autorisé"
                },
                statusArmedOutsideHours = "Activé · en dehors des heures de blocage programmées",
                untilTime = { time -> " jusqu'à $time" },
                allDaySuffix = " (24h/24)",
                endUnlockNowButton = "Terminer le déverrouillage et reverrouiller",
                scheduleTitle = "Planning de blocage",
                modeAllDay = "Toute la journée (24h/7)",
                modeCustomPeriods = "Plages personnalisées",
                ruleHardBlockOn =
                        "Le Blocage strict est ACTIVÉ : aucun déverrouillage temporaire n'est proposé. " +
                                "Les applications sélectionnées restent verrouillées jusqu'à ce que vous désactiviez le bloqueur ici.",
                ruleAllDay = { quotaMin, frictionSec ->
                    "Le mode Toute la journée dure > 2 heures, vous pouvez donc déverrouiller les applications sélectionnées jusqu'à $quotaMin minutes au total par heure " +
                            "(après une pause de ${frictionSec}s, avec un écran gris)."
                },
                ruleCustomEmpty = { quotaMin ->
                    "Ajoutez une ou plusieurs plages horaires ci-dessous. Les plages de plus de 2 heures permettent jusqu'à $quotaMin min/h " +
                            "de déverrouillage temporaire ; les plages de 2 heures ou moins sont toujours en blocage strict."
                },
                ruleCustomWithIntervals = { quotaMin ->
                    "Les plages de plus de 2 heures (> 120 min) permettent jusqu'à $quotaMin min/h de déverrouillage temporaire. " +
                            "Les plages de 2 heures ou moins (<= 120 min) agissent automatiquement comme un Blocage strict."
                },
                hardBlockCheckboxTitle =
                        "Blocage strict (désactiver tous les déverrouillages temporaires)",
                hardBlockCheckboxSubtitle =
                        "S'applique aux modes Toute la journée et Plages personnalisées.",
                customPeriodsHeader = { count -> "Plages horaires personnalisées ($count)" },
                overnightSupportedSubtitle =
                        "Les plages nocturnes (ex. 22:00 – 02:00) sont prises en charge",
                addPeriodChip = "Ajouter une plage",
                noPeriodsTitle = "Aucune plage horaire",
                noPeriodsSubtitle =
                        "Les applications sélectionnées ne seront pas bloquées en mode personnalisé tant que vous n'aurez pas ajouté au moins un intervalle.",
                addTimePeriodButton = "Ajouter une plage horaire",
                activeBadge = "Actif",
                intervalHardBlockTag = "Blocage strict activé",
                intervalUnlockEligibleTag = "> 2 h · déverrouillage 15 min/h possible",
                intervalShortHardBlockTag = "<= 2 h · Blocage strict (sans déverrouillage)",
                overnightTag = " · nuit",
                deletePeriodContentDesc = "Supprimer la plage horaire",
                deletePeriodDialogTitle = "Supprimer la plage horaire ?",
                deletePeriodDialogMessage = { range ->
                    "Voulez-vous vraiment supprimer la plage horaire $range ?"
                },
                deletePeriodConfirmButton = "Supprimer",
                quotaTitle = "Quota de déverrouillage pour cette heure",
                quotaLeftLabel = { remaining, total -> "$remaining / $total min restantes" },
                quotaUsedSubtitle = { used, resetTime ->
                    "$used min utilisées dans l'heure en cours · réinitialisation à $resetTime."
                },
                pickerStartTimeTitle = "Heure de début",
                pickerEndTimeTitle = { start -> "Heure de fin (à partir de $start)" },
                pickerNextButton = "Suivant",
                pickerAddButton = "Ajouter",
                pickerSaveButton = "Enregistrer",
                pickerCancelButton = "Annuler",
                pickerToggleInputDesc = "Changer le mode de saisie de l'heure",
                errSameStartEnd =
                        "Les heures de début et de fin sont identiques. Choisissez deux heures différentes (utilisez Toute la journée pour 24h/24).",
                errDuplicateInterval = "Cette plage horaire figure déjà dans votre liste.",
                lockTitle = { appName -> "$appName est bloqué" },
                lockHardBlockEnabledReason =
                        "Le Blocage strict est activé. Il est impossible de déverrouiller depuis cet écran.",
                lockShortPeriodReason = { duration ->
                    "Cette période de concentration dure $duration. Les périodes de 2 heures ou moins ne peuvent pas être déverrouillées."
                },
                lockLiftsAt = { time -> "Le blocage prend fin à $time." },
                lockRunsAllDay = "Ce blocage est actif toute la journée.",
                lockChangeInAppHint =
                        "Pour le lever plus tôt, ouvrez FocusGuard et modifiez vos paramètres.",
                lockFrictionPrompt = { appName ->
                    "Prenez une grande inspiration. Avez-vous vraiment besoin de $appName maintenant ?"
                },
                lockUnlockForLabel = "Déverrouiller pendant",
                lockMinutesValue = { min -> "$min min" },
                lockQuotaRemainingSub = { remaining, total, resetsAt ->
                    "$remaining sur $total min restantes cette heure · réinitialisation à $resetsAt"
                },
                lockUnlockButton = { appName -> "Déverrouiller $appName" },
                lockGrayScreenHint = "L'écran sera teinté en gris pendant le déverrouillage.",
                lockQuotaExhaustedTitle =
                        "Limite de 15 minutes par heure atteinte. Réessayez à l'heure suivante.",
                lockNextWindowOpensAt = { resetsAt ->
                    "Votre prochain créneau de déverrouillage s'ouvre à $resetsAt."
                },
                lockExitHomeButton = "Retour à l'écran d'accueil",
                formatDuration = { minutes ->
                    val hours = minutes / 60
                    val mins = minutes % 60
                    when {
                        hours == 0 -> "$mins min"
                        mins == 0 -> "${hours}h"
                        else -> "${hours}h ${mins}m"
                    }
                },
        )
