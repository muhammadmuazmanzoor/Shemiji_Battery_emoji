package com.shemiji.emogibattery.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import com.shemiji.emogibattery.R
import kotlin.math.roundToInt

class BatteryToolbarOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var receiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Local view update handled by windowManager.updateViewLayout if we had a persistent view.
            // But since this service now mostly serves as a persistence anchor when Accessibility is on,
            // we rely on Accessibility service to show the UI.
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        OverlayNotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("BatteryToolbarOverlayService", "onStartCommand action=${'$'}{intent?.action}")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                OverlayNotificationHelper.build(this, "Battery toolbar active", "Tap to manage your customization"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                OverlayNotificationHelper.build(this, "Battery toolbar active", "Tap to manage your customization")
            )
        }

        val runtime = getSharedPreferences(PREFS, MODE_PRIVATE)
        
        if (intent != null && intent.hasExtra(EXTRA_STYLE_NAME)) {
            if (intent.action == ACTION_STOP) {
                runtime.edit().putBoolean(EXTRA_IS_ENABLED, false).commit()
                stopSelf()
                return START_NOT_STICKY
            }

            val incomingDrawable = intent.getIntExtra(EXTRA_DRAWABLE_RES, 0)
            val incomingImageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
            val incomingStyleName = intent.getStringExtra(EXTRA_STYLE_NAME).orEmpty().ifBlank { "Custom toolbar" }
            val incomingBg = intent.getStringExtra(EXTRA_BACKGROUND_COLOR)
            val incomingContent = intent.getStringExtra(EXTRA_CONTENT_COLOR)
            val incomingAccent = intent.getStringExtra(EXTRA_ACCENT_COLOR)
            val incomingHeight = intent.getIntExtra(EXTRA_HEIGHT, 34)
            val incomingLeft = intent.getIntExtra(EXTRA_LEFT_MARGIN, 16)
            val incomingRight = intent.getIntExtra(EXTRA_RIGHT_MARGIN, 16)

            // Persist settings. This will trigger the Accessibility Service's listener to refresh the UI.
            runtime.edit()
                .putInt(EXTRA_DRAWABLE_RES, incomingDrawable)
                .putString(EXTRA_IMAGE_URL, incomingImageUrl)
                .putString(EXTRA_STYLE_NAME, incomingStyleName)
                .putString(EXTRA_BACKGROUND_COLOR, incomingBg)
                .putString(EXTRA_CONTENT_COLOR, incomingContent)
                .putString(EXTRA_ACCENT_COLOR, incomingAccent)
                .putInt(EXTRA_HEIGHT, incomingHeight)
                .putInt(EXTRA_LEFT_MARGIN, incomingLeft)
                .putInt(EXTRA_RIGHT_MARGIN, incomingRight)
                .putBoolean(EXTRA_IS_ENABLED, true)
                .commit()

            showOverlay(force = true)

        } else if (intent != null && intent.action == ACTION_STOP) {
            runtime.edit().putBoolean(EXTRA_IS_ENABLED, false).commit()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!runtime.getBoolean(EXTRA_IS_ENABLED, false)) {
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(force = false)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isServiceRunning = false
        removeOverlay()
        super.onDestroy()
    }

    private fun showOverlay(force: Boolean) {
        // PRIORITY: If accessibility service is connected, it handles the view.
        if (OverlayAccessibilityService.isServiceConnected) {
            Log.d("BatteryToolbarOverlayService", "Accessibility connected, yielding UI to AccessibilityService")
            removeOverlay()
            return
        }

        if (!force && staticOverlayView != null && staticOverlayView?.isAttachedToWindow == true) return

        removeOverlay()

        val runtime = getSharedPreferences(PREFS, MODE_PRIVATE)
        val backgroundColor = parseColor(runtime.getString(EXTRA_BACKGROUND_COLOR, "#16182B"), "#16182B")
        val contentColor = parseColor(runtime.getString(EXTRA_CONTENT_COLOR, "#FFFFFF"), "#FFFFFF")
        val accentColor = parseColor(runtime.getString(EXTRA_ACCENT_COLOR, "#8B8FFF"), "#8B8FFF")
        val height = runtime.getInt(EXTRA_HEIGHT, 34)
        val leftMargin = runtime.getInt(EXTRA_LEFT_MARGIN, 16)
        val rightMargin = runtime.getInt(EXTRA_RIGHT_MARGIN, 16)
        val drawableRes = runtime.getInt(EXTRA_DRAWABLE_RES, R.drawable.demo_battery_happy)
        val imageUrl = runtime.getString(EXTRA_IMAGE_URL, null)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(leftMargin), dp(6), dp(rightMargin), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(backgroundColor)
                setStroke(dp(1), withAlpha(accentColor, 80))
            }
        }

        // ... simplified content for fallback overlay ...
        val timeView = TextView(this).apply {
            text = "12:00"
            setTextColor(contentColor)
        }
        container.addView(timeView)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(height),
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            staticOverlayView = container
            windowManager.addView(container, params)
        } catch (e: Exception) {
            staticOverlayView = null
        }
    }

    private fun removeOverlay() {
        staticOverlayView?.let { view ->
            if (view.isAttachedToWindow) {
                runCatching { windowManager.removeView(view) }
            }
        }
        staticOverlayView = null
    }

    private fun parseColor(value: String?, fallback: String): Int =
        runCatching { Color.parseColor(normalizeHexColor(value ?: fallback)) }.getOrDefault(Color.BLACK)

    private fun normalizeHexColor(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_SYSTEM_ERROR

    companion object {
        var isServiceRunning = false
            private set
        
        private var staticOverlayView: View? = null

        const val EXTRA_DRAWABLE_RES = "drawable_res"
        const val EXTRA_IMAGE_URL = "image_url"
        const val EXTRA_STYLE_NAME = "style_name"
        const val EXTRA_BACKGROUND_COLOR = "background_color"
        const val EXTRA_CONTENT_COLOR = "content_color"
        const val EXTRA_ACCENT_COLOR = "accent_color"
        const val EXTRA_HEIGHT = "toolbar_height"
        const val EXTRA_LEFT_MARGIN = "toolbar_left_margin"
        const val EXTRA_RIGHT_MARGIN = "toolbar_right_margin"
        const val EXTRA_IS_ENABLED = "is_enabled"
        const val ACTION_STOP = "com.shemiji.ACTION_STOP"
        private const val PREFS = "battery_overlay_runtime"
        private const val NOTIFICATION_ID = 4101
    }
}
