package com.example.scrollstop

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Watches foreground window changes and enforces the FocusGuard rules for guarded apps
 * (Instagram and TikTok):
 *  - launches [LockScreenActivity] when a guarded app is opened during an active block,
 *  - shows a pass-through gray overlay while a temporary unlock is running,
 *  - re-locks the app the moment the unlock expires.
 */
class AppBlockerService : AccessibilityService() {

    private lateinit var settings: SettingsManager
    private lateinit var windowManager: WindowManager

    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var foregroundPackage: String? = null
    private var lastLockLaunchAt = 0L
    private var lastLockedApp: TargetApp? = null
    private var launcherPackages: Set<String> = emptySet()
    private val activityCache = HashMap<String, Boolean>()

    private val evaluateRunnable = Runnable { evaluate() }

    // Kept as a field: SharedPreferences only holds listeners weakly.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        evaluate()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = SettingsManager.getInstance(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        launcherPackages = resolveLauncherPackages()
        settings.registerListener(prefsListener)
        Log.i(TAG, "AppBlockerService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!::settings.isInitialized) return

        val pkg = event.packageName?.toString() ?: return
        if (pkg == SYSTEM_UI_PACKAGE) return
        val cls = event.className?.toString()

        // Only real app switches count. Keyboards, the notification shade, toasts and
        // our own overlay window are not activities and must not hide/show anything.
        val isAppSwitch = TargetApp.fromPackage(pkg) != null ||
            pkg in launcherPackages ||
            isActivity(pkg, cls)
        if (!isAppSwitch) return

        foregroundPackage = pkg
        evaluate()
    }

    override fun onInterrupt() {
        // No spoken/haptic feedback to interrupt.
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        hideOverlay()
        if (::settings.isInitialized) settings.unregisterListener(prefsListener)
        super.onDestroy()
    }

    // region Rule enforcement

    private fun evaluate() {
        handler.removeCallbacks(evaluateRunnable)
        if (!::settings.isInitialized) return

        val targetApp = TargetApp.fromPackage(foregroundPackage)
        if (targetApp == null || !settings.isAppSelected(targetApp)) {
            hideOverlay()
            return
        }

        val nowMillis = System.currentTimeMillis()
        val state = settings.evaluate(nowMillis.toLocalDateTime())

        when {
            !state.isBlocking -> {
                hideOverlay()
                // Keep checking: a blocking period may start while the user is scrolling.
                scheduleEvaluation(PERIODIC_CHECK_MS)
            }

            state.unlockEligible && settings.isUnlockActive(nowMillis) -> {
                showOverlay()
                val untilExpiry = settings.unlockUntilMillis - nowMillis + EXPIRY_GRACE_MS
                scheduleEvaluation(minOf(untilExpiry, PERIODIC_CHECK_MS))
            }

            else -> {
                hideOverlay()
                launchLockScreen(targetApp)
                scheduleEvaluation(PERIODIC_CHECK_MS)
            }
        }
    }

    private fun scheduleEvaluation(delayMs: Long) {
        handler.removeCallbacks(evaluateRunnable)
        handler.postDelayed(evaluateRunnable, delayMs.coerceAtLeast(0L))
    }

    private fun launchLockScreen(targetApp: TargetApp) {
        val now = SystemClock.elapsedRealtime()
        if (targetApp == lastLockedApp && now - lastLockLaunchAt < LOCK_RELAUNCH_DEBOUNCE_MS) return
        lastLockLaunchAt = now
        lastLockedApp = targetApp

        val intent = Intent(this, LockScreenActivity::class.java)
            .putExtra(LockScreenActivity.EXTRA_TARGET_APP, targetApp.name)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to launch lock screen", e)
        }
    }

    // endregion

    // region Gray overlay

    private fun showOverlay() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = DEFAULT_OVERLAY_ALPHA
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val view = View(this).apply {
            setBackgroundColor(Color.parseColor(OVERLAY_COLOR))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }

        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            Log.e(TAG, "Unable to add gray overlay", e)
        }
    }

    private fun hideOverlay() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeViewImmediate(view)
        } catch (e: Exception) {
            try {
                windowManager.removeView(view)
            } catch (ignored: Exception) {
                Log.w(TAG, "Overlay already removed", ignored)
            }
        }
    }

    // endregion

    // region Foreground detection helpers

    @Suppress("DEPRECATION")
    private fun resolveLauncherPackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.queryIntentActivities(homeIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .filter { it != SYSTEM_UI_PACKAGE && it != SETTINGS_PACKAGE }
            .toSet()
    }

    @Suppress("DEPRECATION")
    private fun isActivity(pkg: String, cls: String?): Boolean {
        if (cls.isNullOrEmpty()) return false
        val key = "$pkg/$cls"
        activityCache[key]?.let { return it }

        val result = try {
            packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        if (activityCache.size > ACTIVITY_CACHE_LIMIT) activityCache.clear()
        activityCache[key] = result
        return result
    }

    // endregion

    companion object {
        private const val TAG = "AppBlockerService"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val SETTINGS_PACKAGE = "com.android.settings"
        private const val OVERLAY_COLOR = "#CC2C2C2C"
        private const val DEFAULT_OVERLAY_ALPHA = 0.8f
        private const val PERIODIC_CHECK_MS = 3_000L
        private const val EXPIRY_GRACE_MS = 100L
        private const val LOCK_RELAUNCH_DEBOUNCE_MS = 1_000L
        private const val ACTIVITY_CACHE_LIMIT = 500
    }
}
