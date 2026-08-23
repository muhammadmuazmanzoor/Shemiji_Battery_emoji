package com.shemiji.emogibattery.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log
import android.content.Intent
import android.os.Build

class OverlayAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Check on window changes to ensure our overlays are still visible
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                checkAndRestartServices()
            }
            else -> {}
        }
    }

    override fun onInterrupt() {
        Log.d("OverlayAccessibility", "Service interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("OverlayAccessibility", "Service connected")
        checkAndRestartServices()
    }

    private fun checkAndRestartServices() {
        // Battery Toolbar
        val batteryPrefs = getSharedPreferences("battery_overlay_runtime", MODE_PRIVATE)
        if (batteryPrefs.getBoolean("is_enabled", false)) {
            startOverlayService(BatteryToolbarOverlayService::class.java)
        }

        // Shimeji
        val shimejiPrefs = getSharedPreferences("shimeji_overlay_runtime", MODE_PRIVATE)
        if (shimejiPrefs.getBoolean("is_enabled", false)) {
            startOverlayService(ShimejiOverlayService::class.java)
        }
    }

    private fun <T> startOverlayService(serviceClass: Class<T>) {
        val intent = Intent(this, serviceClass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
