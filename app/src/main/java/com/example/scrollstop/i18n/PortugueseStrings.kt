package com.example.scrollstop.i18n

import com.example.scrollstop.AppLanguage
import com.example.scrollstop.AppStrings

val PortugueseStrings =
        AppStrings(
                language = AppLanguage.PT,
                appSubtitle = "Bloqueador de Instagram, TikTok e X",
                languageMenuTitle = "Idioma do aplicativo",
                languageSystem = "Idioma do sistema",
                permissionsTitle = "Painel de permissões",
                permissionsAllActive = "Todas as permissões necessárias do sistema estão ativas.",
                permissionsGrantBoth =
                        "Conceda ambas as permissões para que o FocusGuard possa detectar e escurecer o Instagram, TikTok e X.",
                permOverlayTitle = "Sobrepor a outros apps",
                permOverlaySubtitle =
                        "Aplica o filtro cinza translúcido durante desbloqueios temporários",
                permAccessibilityTitle = "Serviço de acessibilidade",
                permAccessibilitySubtitle =
                        "Detecta quando o Instagram, TikTok ou X é aberto em primeiro plano",
                actionSettings = "Configurações",
                actionGrant = "Conceder",
                masterBlockerTitle = "Bloqueador principal",
                blockedAppsTitle = "Apps monitorados",
                blockedAppsHint = "Selecione quais apps bloquear (pelo menos um)",
                minOneAppRequiredHint = "Pelo menos um app deve permanecer selecionado.",
                appsConjunction = " e ",
                statusPaused = "O bloqueador está pausado",
                statusWaitingAccessibility = "Aguardando permissão do Serviço de acessibilidade",
                statusUnlockActive = { mm, ss ->
                    "Desbloqueio temporário ativo · %d:%02d restantes (tela cinza ativa)".format(
                            mm,
                            ss
                    )
                },
                statusHardBlockingNow = { appsLabel, untilPart ->
                    "Bloqueio rígido de $appsLabel ativo$untilPart"
                },
                statusBlockingNow = { appsLabel, untilPart ->
                    "Bloqueando $appsLabel agora$untilPart · desbloqueio de 15 min/h permitido"
                },
                statusArmedOutsideHours = "Ativado · fora do horário de bloqueio programado",
                untilTime = { time -> " até $time" },
                allDaySuffix = " (24/7)",
                endUnlockNowButton = "Encerrar desbloqueio e bloquear agora",
                scheduleTitle = "Programação de bloqueio",
                modeAllDay = "O dia todo (24/7)",
                modeCustomPeriods = "Personalizado",
                ruleHardBlockOn =
                        "O Bloqueio rígido está ATIVADO: nenhum desbloqueio temporário é oferecido em nenhum modo. " +
                                "Os apps selecionados permanecem bloqueados até que você desative o bloqueador aqui.",
                ruleAllDay = { quotaMin, frictionSec ->
                    "O modo O dia todo dura > 2 horas, então você pode desbloquear os apps selecionados por até $quotaMin minutos no total por hora " +
                            "(após uma pausa de ${frictionSec}s, com tela cinza)."
                },
                ruleCustomEmpty = { quotaMin ->
                    "Adicione um ou mais períodos abaixo. Períodos maiores que 2 horas permitem até $quotaMin min/h " +
                            "de desbloqueio temporário; períodos de 2 horas ou menos têm sempre bloqueio rígido."
                },
                ruleCustomWithIntervals = { quotaMin ->
                    "Períodos maiores que 2 horas (> 120 min) permitem até $quotaMin min/h de desbloqueio temporário. " +
                            "Períodos de 2 horas ou menos (<= 120 min) funcionam como Bloqueio rígido automaticamente."
                },
                hardBlockCheckboxTitle =
                        "Bloqueio rígido (desativar todos os desbloqueios temporários)",
                hardBlockCheckboxSubtitle =
                        "Aplica-se tanto ao modo O dia todo quanto aos Períodos personalizados.",
                customPeriodsHeader = { count -> "Períodos personalizados ($count)" },
                overnightSupportedSubtitle =
                        "Intervalos noturnos (ex.: 22:00 – 02:00) são suportados",
                addPeriodChip = "Adicionar período",
                noPeriodsTitle = "Nenhum período definido",
                noPeriodsSubtitle =
                        "Os apps selecionados não serão bloqueados no modo personalizado até que você adicione pelo menos um intervalo.",
                addTimePeriodButton = "Adicionar período de tempo",
                activeBadge = "Ativo",
                intervalHardBlockTag = "Bloqueio rígido ativado",
                intervalUnlockEligibleTag = "> 2h · 15 min/h de desbloqueio disponível",
                intervalShortHardBlockTag = "<= 2h · Bloqueio rígido (sem desbloqueio)",
                overnightTag = " · noturno",
                deletePeriodContentDesc = "Excluir período de tempo",
                quotaTitle = "Cota de desbloqueio desta hora",
                quotaLeftLabel = { remaining, total -> "$remaining / $total min restantes" },
                quotaUsedSubtitle = { used, resetTime ->
                    "$used min usados na hora atual · a cota reinicia às $resetTime."
                },
                pickerStartTimeTitle = "Hora de início",
                pickerEndTimeTitle = { start -> "Hora de término (de $start)" },
                pickerNextButton = "Avançar",
                pickerAddButton = "Adicionar período",
                pickerSaveButton = "Salvar",
                pickerCancelButton = "Cancelar",
                pickerToggleInputDesc = "Alternar modo de entrada de hora",
                errSameStartEnd =
                        "Os horários de início e término são iguais. Escolha horários diferentes (use O dia todo para 24/7).",
                errDuplicateInterval = "Esse período de tempo já está na sua lista.",
                lockTitle = { appName -> "$appName está bloqueado" },
                lockHardBlockEnabledReason =
                        "O Bloqueio rígido está ativado. Não é possível desbloquear a partir daqui.",
                lockShortPeriodReason = { duration ->
                    "Este período de foco dura $duration. Períodos de 2 horas ou menos não podem ser desbloqueados."
                },
                lockLiftsAt = { time -> "O bloqueio termina às $time." },
                lockRunsAllDay = "Este bloqueio dura o dia todo.",
                lockChangeInAppHint =
                        "Para desativá-lo antes, abra o FocusGuard e altere suas configurações.",
                lockFrictionPrompt = { appName ->
                    "Respire fundo. Você realmente precisa do $appName agora?"
                },
                lockUnlockForLabel = "Desbloquear por",
                lockMinutesValue = { min -> "$min min" },
                lockQuotaRemainingSub = { remaining, total, resetsAt ->
                    "$remaining de $total min restantes nesta hora · reinicia às $resetsAt"
                },
                lockUnlockButton = { appName -> "Desbloquear $appName" },
                lockGrayScreenHint =
                        "A tela ficará em tons de cinza enquanto estiver desbloqueada.",
                lockQuotaExhaustedTitle =
                        "Limite de 15 minutos por hora atingido. Tente novamente na próxima hora.",
                lockNextWindowOpensAt = { resetsAt ->
                    "Sua próxima janela de desbloqueio abre às $resetsAt."
                },
                lockExitHomeButton = "Sair para a tela inicial",
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
