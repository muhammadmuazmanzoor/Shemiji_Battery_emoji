package com.shemiji.emogibattery.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.MotionEvent
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
        const val ACTION_UPDATE_BATTERY = "com.shemiji.emogibattery.UPDATE_BATTERY_OVERLAY"
        const val ACTION_STOP_BATTERY = "com.shemiji.emogibattery.STOP_BATTERY_OVERLAY"
        const val ACTION_UPDATE_SHIMEJI = "com.shemiji.emogibattery.UPDATE_SHIMEJI_OVERLAY"
        const val ACTION_STOP_SHIMEJI = "com.shemiji.emogibattery.STOP_SHIMEJI_OVERLAY"
        const val EXTRA_DRAWABLE_RES = "drawable_res"
        const val EXTRA_IMAGE_URL = "image_url"
        const val EXTRA_STYLE_NAME = "style_name"
        const val EXTRA_BACKGROUND_COLOR = "background_color"
        const val EXTRA_CONTENT_COLOR = "content_color"
        const val EXTRA_ACCENT_COLOR = "accent_color"
        const val EXTRA_HEIGHT = "toolbar_height"
        const val EXTRA_LEFT_MARGIN = "toolbar_left_margin"
        const val EXTRA_RIGHT_MARGIN = "toolbar_right_margin"
        const val EXTRA_CHARACTER_NAME = "character_name"
        const val EXTRA_MOVEMENT_SPEED = "movement_speed"
        const val EXTRA_CHARACTER_SIZE_DP = "character_size_dp"
        var isServiceConnected = false
            private set
        var hasActiveOverlay = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private var batteryOverlayView: View? = null
    private var batteryText: TextView? = null
    private var receiverRegistered = false
    private var commandReceiverRegistered = false
    private var shimejiView: SpriteView? = null
    private var shimejiParams: WindowManager.LayoutParams? = null
    private var physics: ShimejiPhysicsEngine? = null
    private val animationHandler = Handler(Looper.getMainLooper())
    private var lastFrameAt = 0L
    private var touchOffsetX = 0f
    private var touchOffsetY = 0f

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_UPDATE_BATTERY -> {
                    getSharedPreferences("battery_overlay_runtime", MODE_PRIVATE).edit()
                        .putBoolean("is_enabled", true)
                        .putInt(EXTRA_DRAWABLE_RES, intent.getIntExtra(EXTRA_DRAWABLE_RES, 0))
                        .putString(EXTRA_IMAGE_URL, intent.getStringExtra(EXTRA_IMAGE_URL))
                        .putString(EXTRA_STYLE_NAME, intent.getStringExtra(EXTRA_STYLE_NAME))
                        .putString(EXTRA_BACKGROUND_COLOR, intent.getStringExtra(EXTRA_BACKGROUND_COLOR))
                        .putString(EXTRA_CONTENT_COLOR, intent.getStringExtra(EXTRA_CONTENT_COLOR))
                        .putString(EXTRA_ACCENT_COLOR, intent.getStringExtra(EXTRA_ACCENT_COLOR))
                        .putInt(EXTRA_HEIGHT, intent.getIntExtra(EXTRA_HEIGHT, 34))
                        .putInt(EXTRA_LEFT_MARGIN, intent.getIntExtra(EXTRA_LEFT_MARGIN, 16))
                        .putInt(EXTRA_RIGHT_MARGIN, intent.getIntExtra(EXTRA_RIGHT_MARGIN, 16)).apply()
                    checkAndRestartServices(true)
                }
                ACTION_STOP_BATTERY -> {
                    getSharedPreferences("battery_overlay_runtime", MODE_PRIVATE).edit().putBoolean("is_enabled", false).apply()
                    removeBatteryAccessibilityOverlay()
                }
                ACTION_UPDATE_SHIMEJI -> {
                    getSharedPreferences("shimeji_overlay_runtime", MODE_PRIVATE).edit()
                        .putBoolean("is_enabled", true)
                        .putInt(EXTRA_DRAWABLE_RES, intent.getIntExtra(EXTRA_DRAWABLE_RES, R.drawable.img_1))
                        .putString(EXTRA_IMAGE_URL, intent.getStringExtra(EXTRA_IMAGE_URL))
                        .putString(EXTRA_CHARACTER_NAME, intent.getStringExtra(EXTRA_CHARACTER_NAME))
                        .putFloat(EXTRA_MOVEMENT_SPEED, intent.getFloatExtra(EXTRA_MOVEMENT_SPEED, 1f))
                        .putInt(EXTRA_CHARACTER_SIZE_DP, intent.getIntExtra(EXTRA_CHARACTER_SIZE_DP, 112)).apply()
                    checkAndRestartServices(true)
                }
                ACTION_STOP_SHIMEJI -> {
                    getSharedPreferences("shimeji_overlay_runtime", MODE_PRIVATE).edit().putBoolean("is_enabled", false).apply()
                    removeShimejiAccessibilityOverlay()
                }
            }
        }
    }

    private val animationTick = object : Runnable {
        override fun run() {
            val engine = physics ?: return
            val now = SystemClock.uptimeMillis()
            engine.tick(if (lastFrameAt == 0L) 16 else now - lastFrameAt)
            lastFrameAt = now
            val params = shimejiParams
            val view = shimejiView
            if (params != null && view != null) {
                params.x = engine.x.roundToInt(); params.y = engine.y.roundToInt()
                runCatching { windowManager.updateViewLayout(view, params) }
                view.motion = engine.motion; view.invalidate()
            }
            animationHandler.postDelayed(this, 16)
        }
    }

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
        removeShimejiAccessibilityOverlay()
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
        registerCommandReceiver()
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
        removeShimejiAccessibilityOverlay()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(batteryReceiver) }
            receiverRegistered = false
        }
        if (commandReceiverRegistered) runCatching { unregisterReceiver(commandReceiver) }
    }

    private fun checkAndRestartServices(forceRefresh: Boolean = false) {
        val batteryPrefs = getSharedPreferences("battery_overlay_runtime", MODE_PRIVATE)
        if (batteryPrefs.getBoolean("is_enabled", false)) {
            ensureBatteryAccessibilityOverlayShown(batteryPrefs, forceRefresh)
            
        } else {
            removeBatteryAccessibilityOverlay()
        }

        val shimejiPrefs = getSharedPreferences("shimeji_overlay_runtime", MODE_PRIVATE)
        if (shimejiPrefs.getBoolean("is_enabled", false)) ensureShimejiAccessibilityOverlayShown(shimejiPrefs, forceRefresh)
        else removeShimejiAccessibilityOverlay()
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

    private fun registerCommandReceiver() {
        if (commandReceiverRegistered) return
        ContextCompat.registerReceiver(this, commandReceiver, IntentFilter().apply {
            addAction(ACTION_UPDATE_BATTERY); addAction(ACTION_STOP_BATTERY)
            addAction(ACTION_UPDATE_SHIMEJI); addAction(ACTION_STOP_SHIMEJI)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        commandReceiverRegistered = true
    }

    private fun ensureShimejiAccessibilityOverlayShown(prefs: SharedPreferences, force: Boolean) {
        if (!force && shimejiView?.isAttachedToWindow == true) return
        if (force) removeShimejiAccessibilityOverlay()
        val size = dp(prefs.getInt("character_size_dp", 112).coerceIn(72, 176))
        val metrics = resources.displayMetrics
        val engine = ShimejiPhysicsEngine().apply {
            configure(metrics.widthPixels, metrics.heightPixels, size, dp(48), prefs.getFloat("movement_speed", 1f))
        }
        val drawable = prefs.getInt("drawable_res", R.drawable.img_1).takeIf { it != 0 } ?: R.drawable.img_1
        val view = SpriteView(this, drawable).apply {
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { engine.beginDrag(); touchOffsetX = event.rawX - engine.x; touchOffsetY = event.rawY - engine.y; true }
                    MotionEvent.ACTION_MOVE -> { engine.dragTo(event.rawX - touchOffsetX, event.rawY - touchOffsetY); true }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { engine.endDrag(); true }
                    else -> false
                }
            }
        }
        val params = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        runCatching { windowManager.addView(view, params) }.onSuccess {
            shimejiView = view; shimejiParams = params; physics = engine; lastFrameAt = 0L
            animationHandler.post(animationTick); hasActiveOverlay = true
        }.onFailure { Log.w("OverlayAccessibility", "Failed to add Shimeji accessibility overlay", it) }
    }

    private fun removeShimejiAccessibilityOverlay() {
        animationHandler.removeCallbacks(animationTick)
        shimejiView?.let { runCatching { if (it.isAttachedToWindow) windowManager.removeView(it) } }
        shimejiView = null; shimejiParams = null; physics = null
        hasActiveOverlay = batteryOverlayView != null
    }

    private class SpriteView(context: Context, drawableRes: Int) : View(context) {
        private val bitmap: Bitmap = BitmapFactory.decodeResource(resources, drawableRes, BitmapFactory.Options().apply { inScaled = false })
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        var motion: ShimejiMotion = ShimejiMotion.IDLE
        override fun onDraw(canvas: Canvas) {
            val phase = ((SystemClock.uptimeMillis() / 150L) % 4).toInt()
            val row = when (motion) {
                ShimejiMotion.WALKING_LEFT, ShimejiMotion.WALKING_RIGHT -> 1
                ShimejiMotion.CLIMBING_LEFT, ShimejiMotion.CLIMBING_RIGHT -> 5
                ShimejiMotion.JUMPING_LEFT_TO_RIGHT, ShimejiMotion.JUMPING_RIGHT_TO_LEFT -> 3
                ShimejiMotion.TOP_WALKING_LEFT, ShimejiMotion.TOP_WALKING_RIGHT -> 6
                ShimejiMotion.FALLING, ShimejiMotion.BOUNCING -> 7
                else -> 0
            }
            val col = if (motion == ShimejiMotion.BOUNCING) 2 else phase
            val src = Rect(col * bitmap.width / 4, row * bitmap.height / 8, (col + 1) * bitmap.width / 4, (row + 1) * bitmap.height / 8)
            val flip = motion in setOf(ShimejiMotion.WALKING_LEFT, ShimejiMotion.CLIMBING_RIGHT, ShimejiMotion.JUMPING_RIGHT_TO_LEFT, ShimejiMotion.TOP_WALKING_LEFT)
            if (flip) { canvas.save(); canvas.scale(-1f, 1f, width / 2f, height / 2f) }
            canvas.drawBitmap(bitmap, src, Rect(0, 0, width, height), paint)
            if (flip) canvas.restore()
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
