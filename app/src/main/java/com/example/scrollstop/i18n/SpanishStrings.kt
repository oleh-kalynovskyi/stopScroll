package com.example.scrollstop.i18n

import com.example.scrollstop.AppLanguage
import com.example.scrollstop.AppStrings

val SpanishStrings =
        AppStrings(
                language = AppLanguage.ES,
                appSubtitle = "Bloqueador de Instagram, TikTok, X, Snapchat y Threads",
                languageMenuTitle = "Idioma de la aplicación",
                languageSystem = "Idioma del sistema",
                permissionsTitle = "Panel de permisos",
                permissionsAllActive = "Todos los permisos del sistema requeridos están activos.",
                permissionsGrantBoth =
                        "Concede ambos permisos para que FocusGuard pueda detectar y atenuar Instagram, TikTok, X, Snapchat y Threads.",
                permOverlayTitle = "Mostrar sobre otras aplicaciones",
                permOverlaySubtitle =
                        "Aplica el filtro gris translúcido durante los desbloqueos temporales",
                permAccessibilityTitle = "Servicio de accesibilidad",
                permAccessibilitySubtitle =
                        "Detecta cuándo Instagram, TikTok, X, Snapchat o Threads pasa a primer plano",
                actionSettings = "Ajustes",
                actionGrant = "Conceder",
                masterBlockerTitle = "Bloqueador principal",
                blockedAppsTitle = "Aplicaciones protegidas",
                blockedAppsHint = "Selecciona qué aplicaciones bloquear (al menos una)",
                minOneAppRequiredHint = "Debe permanecer seleccionada al menos una aplicación.",
                appsConjunction = " y ",
                statusPaused = "El bloqueador está en pausa",
                statusWaitingAccessibility = "Esperando permiso del Servicio de accesibilidad",
                statusUnlockActive = { mm, ss ->
                    "Desbloqueo temporal activo · quedan %d:%02d (pantalla gris activa)".format(
                            mm,
                            ss
                    )
                },
                statusHardBlockingNow = { appsLabel, untilPart ->
                    "Bloqueo estricto de $appsLabel activo$untilPart"
                },
                statusBlockingNow = { appsLabel, untilPart ->
                    "Bloqueando $appsLabel ahora$untilPart · 15 min/h de desbloqueo permitido"
                },
                statusArmedOutsideHours = "Activado · fuera del horario de bloqueo programado",
                untilTime = { time -> " hasta las $time" },
                allDaySuffix = " (24/7)",
                endUnlockNowButton = "Terminar desbloqueo y bloquear ahora",
                scheduleTitle = "Horario de bloqueo",
                modeAllDay = "Todo el día (24/7)",
                modeCustomPeriods = "Personalizado",
                ruleHardBlockOn =
                        "El Bloqueo estricto está ACTIVADO: no se ofrecen desbloqueos temporales en ningún modo. " +
                                "Las aplicaciones seleccionadas permanecen bloqueadas hasta que desactives el bloqueador aquí.",
                ruleAllDay = { quotaMin, frictionSec ->
                    "Todo el día dura > 2 horas, por lo que puedes desbloquear las aplicaciones seleccionadas hasta $quotaMin minutos en total por hora " +
                            "(tras una pausa de ${frictionSec}s, con pantalla gris)."
                },
                ruleCustomEmpty = { quotaMin ->
                    "Añade uno o más periodos de tiempo abajo. Los periodos de más de 2 horas permiten hasta $quotaMin min/h " +
                            "de desbloqueo temporal; los periodos de 2 horas o menos siempre tienen bloqueo estricto."
                },
                ruleCustomWithIntervals = { quotaMin ->
                    "Los periodos de más de 2 horas (> 120 min) permiten hasta $quotaMin min/h de desbloqueo temporal. " +
                            "Los periodos de 2 horas o menos (<= 120 min) actúan como Bloqueo estricto automáticamente."
                },
                hardBlockCheckboxTitle =
                        "Bloqueo estricto (desactivar todos los desbloqueos temporales)",
                hardBlockCheckboxSubtitle =
                        "Se aplica tanto a Todo el día como a los Periodos personalizados.",
                customPeriodsHeader = { count -> "Periodos personalizados ($count)" },
                overnightSupportedSubtitle = "Se admiten horarios nocturnos (p. ej. 22:00 – 02:00)",
                addPeriodChip = "Añadir periodo",
                noPeriodsTitle = "Aún no hay periodos de tiempo",
                noPeriodsSubtitle =
                        "Las aplicaciones seleccionadas no se bloquearán en el modo personalizado hasta que añadas al menos un intervalo.",
                addTimePeriodButton = "Añadir periodo de tiempo",
                activeBadge = "Activo",
                intervalHardBlockTag = "Bloqueo estricto activado",
                intervalUnlockEligibleTag = "> 2 h · 15 min/h de desbloqueo disponible",
                intervalShortHardBlockTag = "<= 2 h · Bloqueo estricto (sin desbloqueo)",
                overnightTag = " · nocturno",
                deletePeriodContentDesc = "Eliminar periodo de tiempo",
                quotaTitle = "Cuota de desbloqueo de esta hora",
                quotaLeftLabel = { remaining, total -> "Quedan $remaining / $total min" },
                quotaUsedSubtitle = { used, resetTime ->
                    "$used min usados en la hora actual · la cuota se reinicia a las $resetTime."
                },
                pickerStartTimeTitle = "Hora de inicio",
                pickerEndTimeTitle = { start -> "Hora de fin (desde $start)" },
                pickerNextButton = "Siguiente",
                pickerAddButton = "Añadir periodo",
                pickerSaveButton = "Guardar",
                pickerCancelButton = "Cancelar",
                pickerToggleInputDesc = "Cambiar modo de entrada de hora",
                errSameStartEnd =
                        "La hora de inicio y fin son iguales. Elige dos horas diferentes (usa Todo el día para 24/7).",
                errDuplicateInterval = "Ese periodo de tiempo ya está en tu lista.",
                lockTitle = { appName -> "$appName está bloqueado" },
                lockHardBlockEnabledReason =
                        "El Bloqueo estricto está activado. No es posible desbloquear desde aquí.",
                lockShortPeriodReason = { duration ->
                    "Este periodo de concentración dura $duration. Los periodos de 2 horas o menos no se pueden desbloquear."
                },
                lockLiftsAt = { time -> "El bloqueo termina a las $time." },
                lockRunsAllDay = "Este bloqueo dura todo el día.",
                lockChangeInAppHint = "Para quitarlo antes, abre FocusGuard y cambia tus ajustes.",
                lockFrictionPrompt = { appName ->
                    "Respira hondo. ¿Realmente necesitas $appName ahora mismo?"
                },
                lockUnlockForLabel = "Desbloquear durante",
                lockMinutesValue = { min -> "$min min" },
                lockQuotaRemainingSub = { remaining, total, resetsAt ->
                    "Quedan $remaining de $total min esta hora · se reinicia a las $resetsAt"
                },
                lockUnlockButton = { appName -> "Desbloquear $appName" },
                lockGrayScreenHint =
                        "La pantalla se mostrará en tonos grises mientras esté desbloqueada.",
                lockQuotaExhaustedTitle =
                        "Límite de 15 minutos por hora alcanzado. Inténtalo de nuevo en la próxima hora.",
                lockNextWindowOpensAt = { resetsAt ->
                    "Tu próxima ventana de desbloqueo se abre a las $resetsAt."
                },
                lockExitHomeButton = "Salir a la pantalla de inicio",
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
