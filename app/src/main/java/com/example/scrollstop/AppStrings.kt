package com.example.scrollstop

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage(val tag: String, val shortLabel: String) {
    SYSTEM(tag = "", shortLabel = "Auto"),
    EN(tag = "en", shortLabel = "EN"),
    UK(tag = "uk", shortLabel = "UA");

    fun resolveLocale(): Locale {
        if (this == EN) return Locale.ENGLISH
        if (this == UK) return Locale("uk")
        val sysLocale = systemDeviceLocale()
        return if (sysLocale.language.equals("uk", ignoreCase = true)) {
            Locale("uk")
        } else {
            Locale.ENGLISH
        }
    }

    fun isUkrainian(): Boolean = resolveLocale().language.equals("uk", ignoreCase = true)
}

/**
 * Reads the device's system-wide language (unaffected by any app-level override).
 */
fun systemDeviceLocale(): Locale {
    val list = Resources.getSystem().configuration.locales
    return if (!list.isEmpty) list[0] else Locale.getDefault()
}

/**
 * Syncs the choice with Android 13+ per-app language settings when a specific language
 * or system default is selected, and updates the default Locale for time formatters.
 */
fun Context.applyAppLanguage(language: AppLanguage) {
    Locale.setDefault(language.resolveLocale())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = getSystemService(LocaleManager::class.java)
        val target = if (language == AppLanguage.SYSTEM) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(language.tag)
        }
        if (localeManager != null && localeManager.applicationLocales != target) {
            localeManager.applicationLocales = target
        }
    }
}

fun Context.wrapWithAppLocale(language: AppLanguage): Context {
    val locale = language.resolveLocale()
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    config.setLocales(LocaleList(locale))
    return createConfigurationContext(config)
}

data class AppStrings(
    val isUkrainian: Boolean,
    val appSubtitle: String,
    val languageMenuTitle: String,
    val languageSystem: String,
    val languageEnglish: String,
    val languageUkrainian: String,
    // Permissions card
    val permissionsTitle: String,
    val permissionsAllActive: String,
    val permissionsGrantBoth: String,
    val permOverlayTitle: String,
    val permOverlaySubtitle: String,
    val permAccessibilityTitle: String,
    val permAccessibilitySubtitle: String,
    val actionSettings: String,
    val actionGrant: String,
    // Master toggle card
    val masterBlockerTitle: String,
    val statusPaused: String,
    val statusWaitingAccessibility: String,
    val statusUnlockActive: (mm: Long, ss: Long) -> String,
    val statusHardBlockingNow: (untilPart: String) -> String,
    val statusBlockingNow: (untilPart: String) -> String,
    val statusArmedOutsideHours: String,
    val untilTime: (time: String) -> String,
    val allDaySuffix: String,
    val endUnlockNowButton: String,
    // Schedule card
    val scheduleTitle: String,
    val modeAllDay: String,
    val modeCustomPeriods: String,
    val ruleHardBlockOn: String,
    val ruleAllDay: (quotaMin: Int, frictionSec: Int) -> String,
    val ruleCustomEmpty: (quotaMin: Int) -> String,
    val ruleCustomWithIntervals: (quotaMin: Int) -> String,
    val hardBlockCheckboxTitle: String,
    val hardBlockCheckboxSubtitle: String,
    // Custom intervals
    val customPeriodsHeader: (count: Int) -> String,
    val overnightSupportedSubtitle: String,
    val addPeriodChip: String,
    val noPeriodsTitle: String,
    val noPeriodsSubtitle: String,
    val addTimePeriodButton: String,
    val activeBadge: String,
    val intervalHardBlockTag: String,
    val intervalUnlockEligibleTag: String,
    val intervalShortHardBlockTag: String,
    val overnightTag: String,
    val deletePeriodContentDesc: String,
    // Quota card
    val quotaTitle: String,
    val quotaLeftLabel: (remaining: Int, total: Int) -> String,
    val quotaUsedSubtitle: (used: Int, resetTime: String) -> String,
    // Time picker dialog
    val pickerStartTimeTitle: String,
    val pickerEndTimeTitle: (startFormatted: String) -> String,
    val pickerNextButton: String,
    val pickerAddButton: String,
    val pickerCancelButton: String,
    val pickerToggleInputDesc: String,
    val errSameStartEnd: String,
    val errDuplicateInterval: String,
    // Lock screen
    val lockTitle: String,
    val lockHardBlockEnabledReason: String,
    val lockShortPeriodReason: (duration: String) -> String,
    val lockLiftsAt: (time: String) -> String,
    val lockRunsAllDay: String,
    val lockChangeInAppHint: String,
    val lockFrictionPrompt: String,
    val lockUnlockForLabel: String,
    val lockMinutesValue: (min: Int) -> String,
    val lockQuotaRemainingSub: (remaining: Int, total: Int, resetsAt: String) -> String,
    val lockUnlockButton: String,
    val lockGrayScreenHint: String,
    val lockQuotaExhaustedTitle: String,
    val lockNextWindowOpensAt: (resetsAt: String) -> String,
    val lockExitHomeButton: String,
    // Duration formatting
    val formatDuration: (minutes: Int) -> String,
)

val EnglishStrings = AppStrings(
    isUkrainian = false,
    appSubtitle = "Instagram blocker · ${SettingsManager.TARGET_PACKAGE}",
    languageMenuTitle = "App language",
    languageSystem = "System language",
    languageEnglish = "English",
    languageUkrainian = "Українська",
    permissionsTitle = "Permissions Dashboard",
    permissionsAllActive = "All required system permissions are active.",
    permissionsGrantBoth = "Grant both permissions so FocusGuard can detect and dim Instagram.",
    permOverlayTitle = "Display Over Other Apps",
    permOverlaySubtitle = "Renders the pass-through gray tint during temporary unlocks",
    permAccessibilityTitle = "Accessibility Service",
    permAccessibilitySubtitle = "Detects when Instagram comes to the foreground",
    actionSettings = "Settings",
    actionGrant = "Grant",
    masterBlockerTitle = "Master Blocker",
    statusPaused = "Blocker is paused",
    statusWaitingAccessibility = "Waiting for Accessibility Service permission",
    statusUnlockActive = { mm, ss ->
        "Temporary unlock active · %d:%02d left (gray screen on)".format(mm, ss)
    },
    statusHardBlockingNow = { untilPart -> "Hard-blocking Instagram now$untilPart" },
    statusBlockingNow = { untilPart -> "Blocking Instagram now$untilPart · 15m/h unlock allowed" },
    statusArmedOutsideHours = "Armed · outside scheduled blocking hours",
    untilTime = { time -> " until $time" },
    allDaySuffix = " (24/7)",
    endUnlockNowButton = "End unlock & re-lock Instagram now",
    scheduleTitle = "Blocking Schedule",
    modeAllDay = "All Day (24/7)",
    modeCustomPeriods = "Custom Periods",
    ruleHardBlockOn = "Hard Block is ON: no temporary unlocks are offered in any mode. " +
        "Instagram stays locked until you turn the blocker off here.",
    ruleAllDay = { quotaMin, frictionSec ->
        "All Day is > 2 hours, so you can unlock Instagram for up to $quotaMin minutes per clock hour " +
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
    noPeriodsSubtitle = "Instagram won't be blocked in Custom mode until you add at least one interval.",
    addTimePeriodButton = "Add Time Period",
    activeBadge = "Active",
    intervalHardBlockTag = "Hard Block enabled",
    intervalUnlockEligibleTag = "> 2h · 15m/h unlock eligible",
    intervalShortHardBlockTag = "<= 2h · Hard Block (no unlock)",
    overnightTag = " · overnight",
    deletePeriodContentDesc = "Delete time period",
    quotaTitle = "This Hour's Unlock Quota",
    quotaLeftLabel = { remaining, total -> "$remaining / $total min left" },
    quotaUsedSubtitle = { used, resetTime ->
        "$used min used in the current clock hour · quota resets at $resetTime."
    },
    pickerStartTimeTitle = "Start time",
    pickerEndTimeTitle = { start -> "End time (from $start)" },
    pickerNextButton = "Next",
    pickerAddButton = "Add period",
    pickerCancelButton = "Cancel",
    pickerToggleInputDesc = "Toggle time input mode",
    errSameStartEnd = "Start and end times are the same. Pick two different times (use All Day mode for 24/7).",
    errDuplicateInterval = "That time period is already in your list.",
    lockTitle = "Instagram is blocked",
    lockHardBlockEnabledReason = "Hard Block is on. There is no way to unlock from here.",
    lockShortPeriodReason = { duration ->
        "This focus period is $duration long. Periods of 2 hours or less can't be unlocked."
    },
    lockLiftsAt = { time -> "The block lifts at $time." },
    lockRunsAllDay = "This block runs all day.",
    lockChangeInAppHint = "To lift it earlier, open FocusGuard and change your settings.",
    lockFrictionPrompt = "Take a breath. Do you really need Instagram right now?",
    lockUnlockForLabel = "Unlock for",
    lockMinutesValue = { min -> "$min min" },
    lockQuotaRemainingSub = { remaining, total, resetsAt ->
        "$remaining of $total min left this hour · resets at $resetsAt"
    },
    lockUnlockButton = "Unlock Instagram",
    lockGrayScreenHint = "The screen will be tinted gray while unlocked.",
    lockQuotaExhaustedTitle = "15-minute hourly limit reached. Try again next hour.",
    lockNextWindowOpensAt = { resetsAt -> "Your next unlock window opens at $resetsAt." },
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

val UkrainianStrings = AppStrings(
    isUkrainian = true,
    appSubtitle = "Блокувальник Instagram · ${SettingsManager.TARGET_PACKAGE}",
    languageMenuTitle = "Мова застосунку",
    languageSystem = "Системна мова",
    languageEnglish = "English",
    languageUkrainian = "Українська",
    permissionsTitle = "Панель дозволів",
    permissionsAllActive = "Усі необхідні системні дозволи надано.",
    permissionsGrantBoth = "Надайте обидва дозволи, щоб FocusGuard міг виявляти та затемнювати Instagram.",
    permOverlayTitle = "Відображення поверх інших вікон",
    permOverlaySubtitle = "Накладає прозорий сірий фільтр під час тимчасового розблокування",
    permAccessibilityTitle = "Служба спеціальних можливостей",
    permAccessibilitySubtitle = "Виявляє, коли Instagram відкривається на екрані",
    actionSettings = "Налаштування",
    actionGrant = "Надати",
    masterBlockerTitle = "Головний перемикач",
    statusPaused = "Блокувальник призупинено",
    statusWaitingAccessibility = "Очікування дозволу Служби спеціальних можливостей",
    statusUnlockActive = { mm, ss ->
        "Тимчасове розблокування · залишилось %d:%02d (сірий екран)".format(mm, ss)
    },
    statusHardBlockingNow = { untilPart -> "Жорстке блокування Instagram активне$untilPart" },
    statusBlockingNow = { untilPart ->
        "Блокування Instagram активне$untilPart · доступно 15 хв/год"
    },
    statusArmedOutsideHours = "Увімкнено · поза розкладом блокування",
    untilTime = { time -> " до $time" },
    allDaySuffix = " (24/7)",
    endUnlockNowButton = "Завершити розблокування і заблокувати зараз",
    scheduleTitle = "Розклад блокування",
    modeAllDay = "Весь день (24/7)",
    modeCustomPeriods = "Власні періоди",
    ruleHardBlockOn = "Жорстке блокування УВІМКНЕНО: тимчасові розблокування недоступні в жодному режимі. " +
        "Instagram заблоковано, доки ви не вимкнете блокувальник тут.",
    ruleAllDay = { quotaMin, frictionSec ->
        "Режим «Весь день» триває > 2 годин, тому Instagram можна розблокувати до $quotaMin хв на годину " +
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
    hardBlockCheckboxSubtitle = "Діє як для режиму «Весь день», так і для власних періодів.",
    customPeriodsHeader = { count -> "Власні часові періоди ($count)" },
    overnightSupportedSubtitle = "Підтримуються нічні інтервали (напр. 22:00 – 02:00)",
    addPeriodChip = "Додати період",
    noPeriodsTitle = "Часових періодів ще немає",
    noPeriodsSubtitle = "У режимі власних періодів Instagram не блокуватиметься, доки ви не додасте хоча б один інтервал.",
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
    pickerCancelButton = "Скасувати",
    pickerToggleInputDesc = "Змінити спосіб введення часу",
    errSameStartEnd = "Час початку та закінчення збігається. Оберіть різний час (для 24/7 використовуйте «Весь день»).",
    errDuplicateInterval = "Цей часовий період уже є у списку.",
    lockTitle = "Instagram заблоковано",
    lockHardBlockEnabledReason = "Увімкнено Жорстке блокування. Розблокування з цього екрана неможливе.",
    lockShortPeriodReason = { duration ->
        "Цей період концентрації триває $duration. Періоди тривалістю 2 години або менше не можна розблокувати."
    },
    lockLiftsAt = { time -> "Блокування завершиться о $time." },
    lockRunsAllDay = "Блокування діє весь день.",
    lockChangeInAppHint = "Щоб зняти блокування раніше, відкрийте FocusGuard і змініть налаштування.",
    lockFrictionPrompt = "Зробіть вдих. Вам справді зараз потрібен Instagram?",
    lockUnlockForLabel = "Розблокувати на",
    lockMinutesValue = { min -> "$min хв" },
    lockQuotaRemainingSub = { remaining, total, resetsAt ->
        "Залишилось $remaining із $total хв у цій годині · оновлення о $resetsAt"
    },
    lockUnlockButton = "Розблокувати Instagram",
    lockGrayScreenHint = "Під час розблокування екран буде чорно-сірим.",
    lockQuotaExhaustedTitle = "15-хвилинний ліміт на цю годину вичерпано. Спробуйте наступної години.",
    lockNextWindowOpensAt = { resetsAt -> "Наступне вікно розблокування відкриється о $resetsAt." },
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

fun stringsFor(language: AppLanguage): AppStrings =
    if (language.isUkrainian()) UkrainianStrings else EnglishStrings

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }

val currentStrings: AppStrings
    @Composable
    @ReadOnlyComposable
    get() = LocalAppStrings.current
