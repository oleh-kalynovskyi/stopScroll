package com.example.scrollstop

import org.junit.Test

import org.junit.Assert.*

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
        assertNull(TargetApp.fromPackage("com.android.settings"))
        assertNull(TargetApp.fromPackage(null))
    }
}