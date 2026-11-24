package com.example.scrollstop

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

internal fun Context.isBlockerServiceEnabled(): Boolean {
    val expected = ComponentName(this, AppBlockerService::class.java)

    val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val enabledInManager = am
        ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        ?.any { info ->
            val si = info.resolveInfo?.serviceInfo ?: return@any false
            ComponentName(si.packageName, si.name) == expected
        } ?: false
    if (enabledInManager) return true

    val setting = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return setting.split(':').any { ComponentName.unflattenFromString(it) == expected }
}

internal fun Context.openOverlaySettings() {
    val specific = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        startActivity(specific)
    } catch (e: Exception) {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

internal fun Context.openAccessibilitySettings() {
    startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
