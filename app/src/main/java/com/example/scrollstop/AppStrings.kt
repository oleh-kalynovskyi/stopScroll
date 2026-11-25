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
import com.example.scrollstop.i18n.EnglishStrings
import com.example.scrollstop.i18n.FrenchStrings
import com.example.scrollstop.i18n.GermanStrings
import com.example.scrollstop.i18n.ItalianStrings
import com.example.scrollstop.i18n.PolishStrings
import com.example.scrollstop.i18n.PortugueseStrings
import com.example.scrollstop.i18n.SpanishStrings
import com.example.scrollstop.i18n.UkrainianStrings
import java.util.Locale

enum class AppLanguage(
    val tag: String,
    val shortLabel: String,
    val nativeName: String,
) {
    SYSTEM(tag = "", shortLabel = "Auto", nativeName = ""),
    EN(tag = "en", shortLabel = "EN", nativeName = "English"),
    UK(tag = "uk", shortLabel = "UA", nativeName = "Українська"),
    DE(tag = "de", shortLabel = "DE", nativeName = "Deutsch"),
    ES(tag = "es", shortLabel = "ES", nativeName = "Español"),
    FR(tag = "fr", shortLabel = "FR", nativeName = "Français"),
    PT(tag = "pt", shortLabel = "PT", nativeName = "Português"),
    IT(tag = "it", shortLabel = "IT", nativeName = "Italiano"),
    PL(tag = "pl", shortLabel = "PL", nativeName = "Polski");

    fun resolveLanguage(): AppLanguage {
        if (this != SYSTEM) return this
        val sysLang = systemDeviceLocale().language
        return entries.firstOrNull { it != SYSTEM && it.tag.equals(sysLang, ignoreCase = true) }
            ?: EN
    }

    fun resolveLocale(): Locale = Locale.forLanguageTag(resolveLanguage().tag)
}

/** Reads the device's system-wide language (unaffected by any app-level override). */
fun systemDeviceLocale(): Locale {
    val list = Resources.getSystem().configuration.locales
    return if (!list.isEmpty) list[0] else Locale.getDefault()
}

/**
 * Syncs the choice with Android 13+ per-app language settings when a specific language or system
 * default is selected, and updates the default Locale for time formatters.
 */
fun Context.applyAppLanguage(language: AppLanguage) {
    Locale.setDefault(language.resolveLocale())
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = getSystemService(LocaleManager::class.java)
        val target =
            if (language == AppLanguage.SYSTEM) {
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
    val language: AppLanguage,
    val appSubtitle: String,
    val languageMenuTitle: String,
    val languageSystem: String,
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
    val blockedAppsTitle: String,
    val blockedAppsHint: String,
    val minOneAppRequiredHint: String,
    val appsConjunction: String,
    val statusPaused: String,
    val statusWaitingAccessibility: String,
    val statusUnlockActive: (mm: Long, ss: Long) -> String,
    val statusHardBlockingNow: (appsLabel: String, untilPart: String) -> String,
    val statusBlockingNow: (appsLabel: String, untilPart: String) -> String,
    val statusArmedOutsideHours: String,
    val statusSessionActive: (mm: Long, ss: Long) -> String,
    val untilTime: (time: String) -> String,
    val allDaySuffix: String,
    val endUnlockNowButton: String,
    // Schedule card
    val scheduleTitle: String,
    val modeAllDay: String,
    val modeCustomPeriods: String,
    val modeSessionLimit: String,
    val ruleHardBlockOn: String,
    val ruleAllDay: (quotaMin: Int, frictionSec: Int) -> String,
    val ruleCustomEmpty: (quotaMin: Int) -> String,
    val ruleCustomWithIntervals: (quotaMin: Int) -> String,
    val ruleSessionLimit: (limitMin: Int, cooldownDuration: String) -> String,
    val hardBlockCheckboxTitle: String,
    val hardBlockCheckboxSubtitle: String,
    // Session limit card & warning banner
    val sessionLimitCardTitle: String,
    val sessionStatusRemaining: (mm: Long, ss: Long, limitMin: Int) -> String,
    val sessionStatusCooldown: (untilTime: String, remainingMin: Long) -> String,
    val sessionResetsAtHint: (limitMin: Int, resetTime: String) -> String,
    val sessionCooldownHint: (cooldownDuration: String) -> String,
    val sessionWarningBanner: (minutesLeft: Int) -> String,
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
    val deletePeriodDialogTitle: String,
    val deletePeriodDialogMessage: (range: String) -> String,
    val deletePeriodConfirmButton: String,
    // Quota card
    val quotaTitle: String,
    val quotaLeftLabel: (remaining: Int, total: Int) -> String,
    val quotaUsedSubtitle: (used: Int, resetTime: String) -> String,
    // Time picker dialog
    val pickerStartTimeTitle: String,
    val pickerEndTimeTitle: (startFormatted: String) -> String,
    val pickerNextButton: String,
    val pickerAddButton: String,
    val pickerSaveButton: String,
    val pickerCancelButton: String,
    val pickerToggleInputDesc: String,
    val errSameStartEnd: String,
    val errDuplicateInterval: String,
    // Lock screen
    val lockTitle: (appName: String) -> String,
    val lockHardBlockEnabledReason: String,
    val lockShortPeriodReason: (duration: String) -> String,
    val lockSessionCooldownReason: (limitMin: Int, cooldownDuration: String) -> String,
    val lockLiftsAt: (time: String) -> String,
    val lockRunsAllDay: String,
    val lockChangeInAppHint: String,
    val lockFrictionPrompt: (appName: String) -> String,
    val lockUnlockForLabel: String,
    val lockMinutesValue: (min: Int) -> String,
    val lockQuotaRemainingSub: (remaining: Int, total: Int, resetsAt: String) -> String,
    val lockUnlockButton: (appName: String) -> String,
    val lockGrayScreenHint: String,
    val lockQuotaExhaustedTitle: String,
    val lockNextWindowOpensAt: (resetsAt: String) -> String,
    val lockExitHomeButton: String,
    // Duration formatting
    val formatDuration: (minutes: Int) -> String,
)

fun stringsFor(language: AppLanguage): AppStrings =
    when (language.resolveLanguage()) {
        AppLanguage.EN, AppLanguage.SYSTEM -> EnglishStrings
        AppLanguage.UK -> UkrainianStrings
        AppLanguage.DE -> GermanStrings
        AppLanguage.ES -> SpanishStrings
        AppLanguage.FR -> FrenchStrings
        AppLanguage.PT -> PortugueseStrings
        AppLanguage.IT -> ItalianStrings
        AppLanguage.PL -> PolishStrings
    }

val LocalAppStrings = staticCompositionLocalOf { EnglishStrings }

val currentStrings: AppStrings
    @Composable @ReadOnlyComposable get() = LocalAppStrings.current
