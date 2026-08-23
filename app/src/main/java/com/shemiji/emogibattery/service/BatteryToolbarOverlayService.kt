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
    private var overlayView: View? = null
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

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        OverlayNotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                OverlayNotificationHelper.build(
                    this,
                    "Battery toolbar active",
                    "Tap to manage your customization",
                ),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                OverlayNotificationHelper.build(
                    this,
                    "Battery toolbar active",
                    "Tap to manage your customization",
                ),
            )
        }

        val runtime = getSharedPreferences(PREFS, MODE_PRIVATE)
        
        if (intent != null && intent.hasExtra(EXTRA_STYLE_NAME)) {
            if (intent.action == ACTION_STOP) {
                runtime.edit().putBoolean(EXTRA_IS_ENABLED, false).commit()
                stopSelf()
                return START_NOT_STICKY
            }
            
            runtime.edit()
                .putInt(EXTRA_DRAWABLE_RES, intent.getIntExtra(EXTRA_DRAWABLE_RES, 0))
                .putString(EXTRA_IMAGE_URL, intent.getStringExtra(EXTRA_IMAGE_URL))
                .putString(EXTRA_STYLE_NAME, intent.getStringExtra(EXTRA_STYLE_NAME))
                .putString(EXTRA_BACKGROUND_COLOR, intent.getStringExtra(EXTRA_BACKGROUND_COLOR))
                .putString(EXTRA_CONTENT_COLOR, intent.getStringExtra(EXTRA_CONTENT_COLOR))
                .putString(EXTRA_ACCENT_COLOR, intent.getStringExtra(EXTRA_ACCENT_COLOR))
                .putBoolean(EXTRA_IS_ENABLED, true)
                .commit()
        } else if (intent != null && intent.action == ACTION_STOP) {
            runtime.edit().putBoolean(EXTRA_IS_ENABLED, false).commit()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!runtime.getBoolean(EXTRA_IS_ENABLED, false)) {
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(
            drawableRes = runtime.getInt(EXTRA_DRAWABLE_RES, R.drawable.demo_battery_happy),
            imageUrl = runtime.getString(EXTRA_IMAGE_URL,null),
            styleName = runtime.getString(EXTRA_STYLE_NAME,null).orEmpty().ifBlank { "Custom toolbar" },
            backgroundColor = parseColor(runtime.getString(EXTRA_BACKGROUND_COLOR,null), "#16182B"),
            contentColor = parseColor(runtime.getString(EXTRA_CONTENT_COLOR,null), "#FFFFFF"),
            accentColor = parseColor(runtime.getString(EXTRA_ACCENT_COLOR,null), "#8B8FFF"),
        )
        registerBatteryReceiver()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        val restartServicePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                applicationContext,
                1,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                applicationContext,
                1,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val alarmService = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )

        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        overlayView?.let { view -> runCatching { windowManager.removeView(view) } }
        overlayView = null
        if (receiverRegistered) {
            runCatching { unregisterReceiver(batteryReceiver) }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    private fun showOverlay(
        drawableRes: Int,
        imageUrl: String?,
        styleName: String,
        backgroundColor: Int,
        contentColor: Int,
        accentColor: Int,
    ) {
        if (overlayView != null && overlayView?.isAttachedToWindow == true) {
            // Already showing, we could update if needed but for persistence check, we stay.
            return
        }

        overlayView?.let { oldView -> runCatching { windowManager.removeView(oldView) } }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(backgroundColor)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), withAlpha(accentColor, 110))
            }
            elevation = dp(8).toFloat()
        }

        val emojiView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            val model: Any = imageUrl?.takeIf(String::isNotBlank) ?: drawableRes
            load(model)
        }
        container.addView(emojiView, LinearLayout.LayoutParams(dp(36), dp(36)))

        val titleView = TextView(this).apply {
            text = styleName
            setTextColor(contentColor)
            textSize = 14f
            setPadding(dp(10), 0, dp(8), 0)
        }
        container.addView(
            titleView,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        batteryText = TextView(this).apply {
            text = "--%"
            setTextColor(accentColor)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        container.addView(
            batteryText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        overlayView = container
        windowManager.addView(container, params)
    }

    private fun registerBatteryReceiver() {
        if (receiverRegistered) return
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    private fun parseColor(value: String?, fallback: String): Int =
        runCatching { Color.parseColor(value ?: fallback) }
            .getOrElse { Color.parseColor(fallback) }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
        }

    companion object {
        const val EXTRA_DRAWABLE_RES = "drawable_res"
        const val EXTRA_IMAGE_URL = "image_url"
        const val EXTRA_STYLE_NAME = "style_name"
        const val EXTRA_BACKGROUND_COLOR = "background_color"
        const val EXTRA_CONTENT_COLOR = "content_color"
        const val EXTRA_ACCENT_COLOR = "accent_color"
        const val EXTRA_IS_ENABLED = "is_enabled"
        const val ACTION_STOP = "com.shemiji.ACTION_STOP"

        private const val PREFS = "battery_overlay_runtime"
        private const val NOTIFICATION_ID = 4101
    }
}
