package com.example.scrollstop.i18n

import com.example.scrollstop.AppLanguage
import com.example.scrollstop.AppStrings

val UkrainianStrings =
    AppStrings(
        language = AppLanguage.UK,
        appSubtitle = "Блокувальник Instagram, TikTok та X",
        languageMenuTitle = "Мова застосунку",
        languageSystem = "Системна мова",
        permissionsTitle = "Панель дозволів",
        permissionsAllActive = "Усі необхідні системні дозволи надано.",
        permissionsGrantBoth =
            "Надайте обидва дозволи, щоб FocusGuard міг виявляти та затемнювати Instagram, TikTok та X.",
        permOverlayTitle = "Відображення поверх інших вікон",
        permOverlaySubtitle =
            "Накладає прозорий сірий фільтр під час тимчасового розблокування",
        permAccessibilityTitle = "Служба спеціальних можливостей",
        permAccessibilitySubtitle =
            "Виявляє, коли Instagram, TikTok або X відкривається на екрані",
        actionSettings = "Налаштування",
        actionGrant = "Надати",
        masterBlockerTitle = "Головний перемикач",
        blockedAppsTitle = "Застосунки для блокування",
        blockedAppsHint = "Оберіть застосунки для блокування (хоча б один)",
        minOneAppRequiredHint = "Має бути обраний хоча б один застосунок.",
        appsConjunction = " і ",
        statusPaused = "Блокувальник призупинено",
        statusWaitingAccessibility = "Очікування дозволу Служби спеціальних можливостей",
        statusUnlockActive = { mm, ss ->
            "Тимчасове розблокування · залишилось %d:%02d (сірий екран)".format(mm, ss)
        },
        statusHardBlockingNow = { appsLabel, untilPart ->
            "Жорстке блокування $appsLabel активне$untilPart"
        },
        statusBlockingNow = { appsLabel, untilPart ->
            "Блокування $appsLabel активне$untilPart · доступно 15 хв/год"
        },
        statusArmedOutsideHours = "Увімкнено · поза розкладом блокування",
        untilTime = { time -> " до $time" },
        allDaySuffix = " (24/7)",
        endUnlockNowButton = "Завершити розблокування і заблокувати зараз",
        scheduleTitle = "Розклад блокування",
        modeAllDay = "Весь день (24/7)",
        modeCustomPeriods = "Власні періоди",
        ruleHardBlockOn =
            "Жорстке блокування УВІМКНЕНО: тимчасові розблокування недоступні в жодному режимі. " +
                "Обрані застосунки заблоковано, доки ви не вимкнете блокувальник тут.",
        ruleAllDay = { quotaMin, frictionSec ->
            "Режим «Весь день» триває > 2 годин, тому обрані застосунки можна розблокувати загалом до $quotaMin хв на годину " +
                "(після ${frictionSec}-секундної паузи, із сірим екраном)."
        },
        ruleCustomEmpty = { quotaMin ->
            "Додайте один або кілька часових періодів нижче. Періоди понад 2 години дозволяють тимчасове " +
                "розблокування до $quotaMin хв/год; періоди до 2 годин блокуються жорстко."
        },
        ruleCustomWithIntervals = { quotaMin ->
            "Періоди понад 2 години (> 120 хв) дозволяють розблокування до $quotaMin хв/год. " +
                "Періоди 2 години або менше (<= 120 хв) працюють як Жорстке блокування автоматично."
        },
        hardBlockCheckboxTitle = "Жорстке блокування (без тимчасових розблокувань)",
        hardBlockCheckboxSubtitle =
            "Діє як для режиму «Весь день», так і для власних періодів.",
        customPeriodsHeader = { count -> "Власні часові періоди ($count)" },
        overnightSupportedSubtitle = "Підтримуються нічні інтервали (напр. 22:00 – 02:00)",
        addPeriodChip = "Додати період",
        noPeriodsTitle = "Часових періодів ще немає",
        noPeriodsSubtitle =
            "У режимі власних періодів обрані застосунки не блокуватимуться, доки ви не додасте хоча б один інтервал.",
        addTimePeriodButton = "Додати часовий період",
        activeBadge = "Активний",
        intervalHardBlockTag = "Жорстке блокування",
        intervalUnlockEligibleTag = "> 2 год · доступно 15 хв/год",
        intervalShortHardBlockTag = "<= 2 год · Жорстке блокування",
        overnightTag = " · через північ",
        deletePeriodContentDesc = "Видалити часовий період",
        quotaTitle = "Ліміт розблокування на цю годину",
        quotaLeftLabel = { remaining, total -> "Залишилось $remaining із $total хв" },
        quotaUsedSubtitle = { used, resetTime ->
            "Використано $used хв у поточній годині · оновлення ліміту о $resetTime."
        },
        pickerStartTimeTitle = "Час початку",
        pickerEndTimeTitle = { start -> "Час закінчення (від $start)" },
        pickerNextButton = "Далі",
        pickerAddButton = "Додати період",
        pickerSaveButton = "Зберегти",
        pickerCancelButton = "Скасувати",
        pickerToggleInputDesc = "Змінити спосіб введення часу",
        errSameStartEnd =
            "Час початку та закінчення збігається. Оберіть різний час (для 24/7 використовуйте «Весь день»).",
        errDuplicateInterval = "Цей часовий період уже є у списку.",
        lockTitle = { appName -> "$appName заблоковано" },
        lockHardBlockEnabledReason =
            "Увімкнено Жорстке блокування. Розблокування з цього екрана неможливе.",
        lockShortPeriodReason = { duration ->
            "Цей період концентрації триває $duration. Періоди тривалістю 2 години або менше не можна розблокувати."
        },
        lockLiftsAt = { time -> "Блокування завершиться о $time." },
        lockRunsAllDay = "Блокування діє весь день.",
        lockChangeInAppHint =
            "Щоб зняти блокування раніше, відкрийте FocusGuard і змініть налаштування.",
        lockFrictionPrompt = { appName ->
            "Зробіть вдих. Вам справді зараз потрібен $appName?"
        },
        lockUnlockForLabel = "Розблокувати на",
        lockMinutesValue = { min -> "$min хв" },
        lockQuotaRemainingSub = { remaining, total, resetsAt ->
            "Залишилось $remaining із $total хв у цій годині · оновлення о $resetsAt"
        },
        lockUnlockButton = { appName -> "Розблокувати $appName" },
        lockGrayScreenHint = "Під час розблокування екран буде чорно-сірим.",
        lockQuotaExhaustedTitle =
            "15-хвилинний ліміт на цю годину вичерпано. Спробуйте наступної години.",
        lockNextWindowOpensAt = { resetsAt ->
            "Наступне вікно розблокування відкриється о $resetsAt."
        },
        lockExitHomeButton = "Вийти на головний екран",
        formatDuration = { minutes ->
            val hours = minutes / 60
            val mins = minutes % 60
            when {
                hours == 0 -> "$mins хв"
                mins == 0 -> "$hours год"
                else -> "$hours год $mins хв"
            }
        },
    )
