package com.example.scrollstop

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

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
    private lateinit var powerManager: PowerManager

    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var warningBannerView: View? = null
    private var foregroundPackage: String? = null
    private var lastLockLaunchAt = 0L
    private var lastLockedApp: TargetApp? = null
    private var launcherPackages: Set<String> = emptySet()
    private val activityCache = HashMap<String, Boolean>()

    private var isTrackingSession = false
    private var lastSessionTickElapsed = 0L
    private var screenReceiverRegistered = false

    private val evaluateRunnable = Runnable { evaluate() }
    private val hideWarningRunnable = Runnable { hideWarningBanner() }

    // Kept as a field: SharedPreferences only holds listeners weakly.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in SettingsManager.SESSION_RUNTIME_KEYS) return@OnSharedPreferenceChangeListener
        evaluate()
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    flushSessionUsage(System.currentTimeMillis())
                    stopSessionTracking()
                    handler.removeCallbacks(evaluateRunnable)
                }
                Intent.ACTION_SCREEN_ON -> evaluate()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        settings = SettingsManager.getInstance(applicationContext)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        launcherPackages = resolveLauncherPackages()
        settings.registerListener(prefsListener)
        registerScreenReceiver()
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
        flushSessionUsage(System.currentTimeMillis())
        stopSessionTracking()
        handler.removeCallbacksAndMessages(null)
        hideOverlay()
        hideWarningBanner()
        unregisterScreenReceiver()
        if (::settings.isInitialized) settings.unregisterListener(prefsListener)
        super.onDestroy()
    }

    // region Rule enforcement

    private fun evaluate() {
        handler.removeCallbacks(evaluateRunnable)
        if (!::settings.isInitialized) return

        val nowMillis = System.currentTimeMillis()
        val targetApp = TargetApp.fromPackage(foregroundPackage)
        if (targetApp == null || !settings.isAppSelected(targetApp)) {
            flushSessionUsage(nowMillis)
            stopSessionTracking()
            hideOverlay()
            return
        }

        if (settings.isEnabled && settings.scheduleMode == ScheduleMode.SESSION_LIMIT) {
            evaluateSessionLimit(targetApp, nowMillis)
            return
        }

        stopSessionTracking()
        hideWarningBanner()

        val state = settings.evaluate(nowMillis.toLocalDateTime(), nowMillis)

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

    private fun evaluateSessionLimit(targetApp: TargetApp, nowMillis: Long) {
        // Session Limit mode never uses the gray screen overlay.
        hideOverlay()

        if (::powerManager.isInitialized && !powerManager.isInteractive) {
            flushSessionUsage(nowMillis)
            stopSessionTracking()
            return
        }

        if (settings.isSessionCooldownActive(nowMillis)) {
            stopSessionTracking()
            hideWarningBanner()
            launchLockScreen(targetApp)
            scheduleEvaluation(PERIODIC_CHECK_MS)
            return
        }

        val elapsedNow = SystemClock.elapsedRealtime()
        val deltaMs = if (isTrackingSession) {
            (elapsedNow - lastSessionTickElapsed).coerceAtLeast(0L)
        } else {
            isTrackingSession = true
            0L
        }
        lastSessionTickElapsed = elapsedNow

        when (settings.recordSessionUsage(deltaMs, nowMillis)) {
            SessionTickResult.COOLDOWN_STARTED,
            SessionTickResult.COOLDOWN_ACTIVE -> {
                stopSessionTracking()
                hideWarningBanner()
                launchLockScreen(targetApp)
                scheduleEvaluation(PERIODIC_CHECK_MS)
            }
            SessionTickResult.SHOW_WARNING -> {
                showWarningBanner()
                scheduleEvaluation(SESSION_TICK_MS)
            }
            SessionTickResult.ACTIVE -> {
                scheduleEvaluation(SESSION_TICK_MS)
            }
        }
    }

    private fun flushSessionUsage(nowMillis: Long) {
        if (!isTrackingSession || !::settings.isInitialized) return
        if (!settings.isEnabled || settings.scheduleMode != ScheduleMode.SESSION_LIMIT) return
        val elapsedNow = SystemClock.elapsedRealtime()
        val deltaMs = (elapsedNow - lastSessionTickElapsed).coerceAtLeast(0L)
        lastSessionTickElapsed = elapsedNow
        if (deltaMs > 0L) {
            settings.recordSessionUsage(deltaMs, nowMillis)
        }
    }

    private fun stopSessionTracking() {
        isTrackingSession = false
        lastSessionTickElapsed = 0L
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(screenReceiver, filter)
            }
            screenReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register screen receiver", e)
        }
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        screenReceiverRegistered = false
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to unregister screen receiver", e)
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

    private fun showWarningBanner() {
        hideWarningBanner()
        val s = stringsFor(settings.appLanguage)
        val density = resources.displayMetrics.density
        val padH = (18 * density).toInt()
        val padV = (10 * density).toInt()
        val radius = 24 * density
        val topMargin = (52 * density).toInt()

        val pillBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.parseColor(WARNING_BANNER_BG_COLOR))
            setStroke((1 * density).toInt().coerceAtLeast(1), Color.parseColor(WARNING_BANNER_BORDER_COLOR))
        }

        val banner = TextView(this).apply {
            text = s.sessionWarningBanner(SettingsManager.SESSION_WARNING_MINUTES)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = pillBackground
            setPadding(padH, padV, padH, padV)
            elevation = 8 * density
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = topMargin
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            windowManager.addView(banner, params)
            warningBannerView = banner
            handler.removeCallbacks(hideWarningRunnable)
            handler.postDelayed(hideWarningRunnable, WARNING_BANNER_DURATION_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to show session warning banner", e)
        }
    }

    private fun hideWarningBanner() {
        handler.removeCallbacks(hideWarningRunnable)
        val view = warningBannerView ?: return
        warningBannerView = null
        try {
            windowManager.removeViewImmediate(view)
        } catch (e: Exception) {
            try {
                windowManager.removeView(view)
            } catch (ignored: Exception) {
                Log.w(TAG, "Warning banner already removed", ignored)
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
        private const val WARNING_BANNER_BG_COLOR = "#EB1E1F26"
        private const val WARNING_BANNER_BORDER_COLOR = "#40FFFFFF"
        private const val DEFAULT_OVERLAY_ALPHA = 0.8f
        private const val PERIODIC_CHECK_MS = 3_000L
        private const val SESSION_TICK_MS = 1_000L
        private const val WARNING_BANNER_DURATION_MS = 5_000L
        private const val EXPIRY_GRACE_MS = 100L
        private const val LOCK_RELAUNCH_DEBOUNCE_MS = 1_000L
        private const val ACTIVITY_CACHE_LIMIT = 500
    }
}
