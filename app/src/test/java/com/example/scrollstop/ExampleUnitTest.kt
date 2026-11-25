package com.example.scrollstop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun targetApp_resolvesInstagramAndTikTokPackages() {
        assertEquals(TargetApp.INSTAGRAM, TargetApp.fromPackage("com.instagram.android"))
        assertEquals(TargetApp.TIKTOK, TargetApp.fromPackage("com.zhiliaoapp.musically"))
        assertEquals(TargetApp.TIKTOK, TargetApp.fromPackage("com.ss.android.ugc.trill"))
        assertEquals(TargetApp.TIKTOK, TargetApp.fromPackage("com.zhiliaoapp.musically.go"))
        assertEquals(TargetApp.X_TWITTER, TargetApp.fromPackage("com.twitter.android"))
        assertEquals(TargetApp.X_TWITTER, TargetApp.fromPackage("com.twitter.android.lite"))
        assertEquals(TargetApp.SNAPCHAT, TargetApp.fromPackage("com.snapchat.android"))
        assertEquals(TargetApp.THREADS, TargetApp.fromPackage("com.instagram.barcelona"))
        assertNull(TargetApp.fromPackage("com.android.settings"))
        assertNull(TargetApp.fromPackage(null))
    }

    @Test
    fun appLanguage_allLanguagesResolveStringsAndFormatting() {
        val explicitLanguages = AppLanguage.entries.filter { it != AppLanguage.SYSTEM }
        assertEquals(
            listOf(
                AppLanguage.EN,
                AppLanguage.UK,
                AppLanguage.DE,
                AppLanguage.ES,
                AppLanguage.FR,
                AppLanguage.PT,
                AppLanguage.IT,
                AppLanguage.PL,
            ),
            explicitLanguages,
        )

        for (lang in explicitLanguages) {
            val s = stringsFor(lang)
            assertEquals(lang, s.language)
            assertEquals(lang.tag, lang.resolveLocale().language)
            assertFalse(s.appSubtitle.isBlank())
            assertTrue(s.statusUnlockActive(12, 5).contains("12:05"))
            assertTrue(s.lockTitle("Instagram").contains("Instagram"))
            assertFalse(s.formatDuration(135).isBlank())
        }
    }
}
