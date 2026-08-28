package com.shemiji.emogibattery.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import com.shemiji.emogibattery.service.OverlayAccessibilityService

object AccessibilityPermission {
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, OverlayAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected.flattenToString(), ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun openSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
