package com.shemiji.emogibattery.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import com.shemiji.emogibattery.MainActivity
import com.shemiji.emogibattery.R
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class OverlayAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        var isServiceConnected = false
            private set
        var hasActiveOverlay = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private var batteryOverlayView: View? = null
    private var batteryText: TextView? = null
    private var receiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra("level", -1) ?: -1
            val scale = intent?.getIntExtra("scale", 100) ?: 100
            if (level >= 0 && scale > 0) {
                batteryText?.text = "${(level * 100f / scale).roundToInt()}%"
            }
        }
    }

    private var lastRestartTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = System.currentTimeMillis()
        // Window state changes happen often, throttle checks
        if (now - lastRestartTime < 2000) return 

        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                lastRestartTime = now
                checkAndRestartServices()
            }
            else -> {}
        }
    }

    override fun onInterrupt() {
        Log.d("OverlayAccessibility", "Service interrupted")
        isServiceConnected = false
        removeBatteryAccessibilityOverlay()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("OverlayAccessibility", "Service connected")
        isServiceConnected = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Listen for style updates in real-time
        getSharedPreferences("battery_overlay_runtime", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(this)
            
        checkAndRestartServices()
        registerBatteryReceiver()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // When any style or enable/disable key changes, refresh the overlays
        checkAndRestartServices(forceRefresh = true)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        getSharedPreferences("battery_overlay_runtime", MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(this)
        removeBatteryAccessibilityOverlay()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(batteryReceiver) }
            receiverRegistered = false
        }
    }

    private fun checkAndRestartServices(forceRefresh: Boolean = false) {
        val batteryPrefs = getSharedPreferences("battery_overlay_runtime", MODE_PRIVATE)
        if (batteryPrefs.getBoolean("is_enabled", false)) {
            ensureBatteryAccessibilityOverlayShown(batteryPrefs, forceRefresh)
            
            if (!BatteryToolbarOverlayService.isServiceRunning) {
                startOverlayService(BatteryToolbarOverlayService::class.java)
            }
        } else {
            removeBatteryAccessibilityOverlay()
        }

        val shimejiPrefs = getSharedPreferences("shimeji_overlay_runtime", MODE_PRIVATE)
        if (shimejiPrefs.getBoolean("is_enabled", false) && !ShimejiOverlayService.isServiceRunning) {
            startOverlayService(ShimejiOverlayService::class.java)
        }
    }

    private fun ensureBatteryAccessibilityOverlayShown(prefs: SharedPreferences, force: Boolean = false) {
        if (!force && batteryOverlayView != null && batteryOverlayView?.isAttachedToWindow == true) return

        // Remove old view if we are forcing a refresh
        if (force) {
            removeBatteryAccessibilityOverlay()
        }

        val bgColor = parseColor(prefs.getString("background_color", null), "#16182B")
        val contentColor = parseColor(prefs.getString("content_color", null), "#FFFFFF")
        val accentColor = parseColor(prefs.getString("accent_color", null), "#8B8FFF")
        
        val height = prefs.getInt("toolbar_height", 34)
        val leftMargin = prefs.getInt("toolbar_left_margin", 16)
        val rightMargin = prefs.getInt("toolbar_right_margin", 16)
        val drawableRes = prefs.getInt("drawable_res", R.drawable.demo_battery_happy)
        val imageUrl = prefs.getString("image_url", null)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(leftMargin), dp(6), dp(rightMargin), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(bgColor)
                cornerRadius = dp(0).toFloat()
                setStroke(dp(1), withAlpha(accentColor, 80))
            }
            elevation = dp(8).toFloat()
            
            setOnClickListener {
                val intent = Intent(this@OverlayAccessibilityService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        val timeView = TextView(this).apply {
            text = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            setTextColor(contentColor)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            includeFontPadding = false
            setPadding(0, 0, dp(6), 0)
        }
        container.addView(timeView)

        val emojiView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            val model: Any = imageUrl?.takeIf(String::isNotBlank) ?: drawableRes
            load(model)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
        }
        container.addView(emojiView)

        val spacer = View(this)
        container.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))

        val statusIcons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        
        val wifiView = TextView(this).apply {
            text = "◔"
            setTextColor(contentColor)
            textSize = 12f
            setPadding(dp(8), 0, dp(8), 0)
        }
        statusIcons.addView(wifiView)

        val batteryGlyph = TextView(this).apply {
            text = "▣"
            setTextColor(accentColor)
            textSize = 15f
            setPadding(0, 0, dp(4), 0)
        }
        statusIcons.addView(batteryGlyph)

        batteryText = TextView(this).apply {
            text = "--%"
            setTextColor(accentColor)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        
        // Immediate battery level update
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            registerReceiver(null, filter)
        }
        batteryStatus?.let { intent ->
            val level = intent.getIntExtra("level", -1)
            val scale = intent.getIntExtra("scale", 100)
            if (level >= 0 && scale > 0) {
                batteryText?.text = "${(level * 100f / scale).roundToInt()}%"
            }
        }
        
        statusIcons.addView(batteryText)
        container.addView(statusIcons)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            dp(height),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        try {
            batteryOverlayView = container
            windowManager.addView(container, params)
            hasActiveOverlay = true
            Log.d("OverlayAccessibility", "Added battery accessibility overlay pid=${android.os.Process.myPid()} time=${System.currentTimeMillis()}")
        } catch (t: Throwable) {
            Log.w("OverlayAccessibility", "Failed to add accessibility overlay", t)
            batteryOverlayView = null
            hasActiveOverlay = false
        }
    }

    private fun registerBatteryReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val stickyIntent = ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // Initial update from sticky intent
        stickyIntent?.let { intent ->
            val level = intent.getIntExtra("level", -1)
            val scale = intent.getIntExtra("scale", 100)
            if (level >= 0 && scale > 0) {
                batteryText?.text = "${(level * 100f / scale).roundToInt()}%"
            }
        }
        receiverRegistered = true
    }

    private fun removeBatteryAccessibilityOverlay() {
        hasActiveOverlay = false
        batteryOverlayView?.let { view ->
            try {
                if (view.isAttachedToWindow) {
                    windowManager.removeView(view)
                }
                Log.d("OverlayAccessibility", "Removed battery accessibility overlay pid=${android.os.Process.myPid()} time=${System.currentTimeMillis()}")
            } catch (t: Throwable) {
                Log.w("OverlayAccessibility", "Failed to remove accessibility overlay", t)
            }
        }
        batteryOverlayView = null
    }

    private fun <T> startOverlayService(serviceClass: Class<T>) {
        val intent = Intent(this, serviceClass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun parseColor(value: String?, fallback: String): Int =
        runCatching {
            val colorString = value ?: fallback
            val normalized = normalizeHexColor(colorString)
            Color.parseColor(normalized)
        }.getOrElse {
            runCatching { Color.parseColor(fallback) }.getOrDefault(Color.BLACK)
        }

    private fun normalizeHexColor(value: String): String {
        val trimmed = value.trim()
        return when {
            trimmed.startsWith("#") -> {
                if (trimmed.length == 7 || trimmed.length == 9) trimmed
                else if (trimmed.length > 9) trimmed.substring(0, 9)
                else trimmed
            }
            trimmed.startsWith("0x", true) -> "#${trimmed.substring(2)}"
            trimmed.length == 6 -> "#$trimmed"
            trimmed.length == 8 -> "#$trimmed"
            else -> "#$trimmed"
        }
    }
}
