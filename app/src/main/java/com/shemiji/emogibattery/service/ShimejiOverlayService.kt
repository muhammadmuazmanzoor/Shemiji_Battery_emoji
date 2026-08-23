package com.shemiji.emogibattery.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.provider.Settings
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import coil.load
import com.shemiji.emogibattery.R
import kotlin.math.roundToInt

class ShimejiOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var movementAnimator: ValueAnimator? = null

    private var dragging = false
    private var dragStartX = 0
    private var dragStartY = 0
    private var touchStartX = 0f
    private var touchStartY = 0f

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        OverlayNotificationHelper.createChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val runtime = getSharedPreferences(PREFS, MODE_PRIVATE)
        val characterName = runtime.getString(EXTRA_CHARACTER_NAME, "Shimeji") ?: "Shimeji"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, OverlayNotificationHelper.build(this, "$characterName is active", "Drag the character anywhere on your screen"), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, OverlayNotificationHelper.build(this, "$characterName is active", "Drag the character anywhere on your screen"))
        }

        var shouldForce = false
        if (intent != null && intent.hasExtra(EXTRA_CHARACTER_NAME)) {
            if (intent.action == ACTION_STOP) {
                runtime.edit().putBoolean(EXTRA_IS_ENABLED, false).commit()
                stopSelf()
                return START_NOT_STICKY
            }

            runtime.edit()
                .putInt(EXTRA_DRAWABLE_RES, intent.getIntExtra(EXTRA_DRAWABLE_RES, 0))
                .putString(EXTRA_IMAGE_URL, intent.getStringExtra(EXTRA_IMAGE_URL))
                .putString(EXTRA_CHARACTER_NAME, intent.getStringExtra(EXTRA_CHARACTER_NAME))
                .putFloat(EXTRA_MOVEMENT_SPEED, intent.getFloatExtra(EXTRA_MOVEMENT_SPEED, 1f))
                .putBoolean(EXTRA_IS_ENABLED, true)
                .commit()
            
            shouldForce = true
        } else if (intent != null && intent.action == ACTION_STOP) {
            runtime.edit().putBoolean(EXTRA_IS_ENABLED, false).commit()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!runtime.getBoolean(EXTRA_IS_ENABLED, false)) {
            stopSelf()
            return START_NOT_STICKY
        }

        showCharacter(
            drawableRes = runtime.getInt(EXTRA_DRAWABLE_RES, R.drawable.demo_shimeji_mochi),
            imageUrl = runtime.getString(EXTRA_IMAGE_URL, null),
            movementSpeed = runtime.getFloat(EXTRA_MOVEMENT_SPEED, 1f).coerceIn(0.25f, 3f),
            force = shouldForce
        )

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isServiceRunning = false
        movementAnimator?.cancel()
        removeCharacterView()
        super.onDestroy()
    }

    private fun showCharacter(drawableRes: Int, imageUrl: String?, movementSpeed: Float, force: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        // If not forced and already showing, do nothing
        if (!force && staticCharacterView != null && staticCharacterView?.isAttachedToWindow == true) {
            return
        }

        // Clean up previous view from window manager
        removeCharacterView()

        val imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            val model: Any = imageUrl?.takeIf(String::isNotBlank) ?: drawableRes
            load(model) { crossfade(true) }
        }

        val params = WindowManager.LayoutParams(
            dp(112), dp(112),
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(24)
            y = dp(220)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        imageView.setOnTouchListener { _, event ->
            handleTouch(event = event, params = params, movementSpeed = movementSpeed)
        }

        staticCharacterView = imageView

        try {
            windowManager.addView(imageView, params)
        } catch (t: Throwable) {
            staticCharacterView = null
            stopSelf()
            return
        }

        startMovement(params = params, movementSpeed = movementSpeed)
    }

    private fun removeCharacterView() {
        movementAnimator?.cancel()
        staticCharacterView?.let { view ->
            if (view.isAttachedToWindow) {
                runCatching { windowManager.removeView(view) }
            }
        }
        staticCharacterView = null
    }

    private fun handleTouch(event: MotionEvent, params: WindowManager.LayoutParams, movementSpeed: Float): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                movementAnimator?.cancel()
                dragStartX = params.x
                dragStartY = params.y
                touchStartX = event.rawX
                touchStartY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val bounds = getMovementBounds(params)
                params.x = (dragStartX + (event.rawX - touchStartX).roundToInt()).coerceIn(0, bounds.maxX)
                params.y = (dragStartY + (event.rawY - touchStartY).roundToInt()).coerceIn(0, bounds.maxY)
                updateViewPosition(params)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                if (isOnEdge(params)) startMovement(params, movementSpeed)
                else startFalling(params, movementSpeed)
                return true
            }
        }
        return false
    }

    private fun isOnEdge(params: WindowManager.LayoutParams): Boolean {
        val bounds = getMovementBounds(params)
        val edgeThreshold = dp(8)
        return params.x <= edgeThreshold || params.x >= bounds.maxX - edgeThreshold ||
                params.y <= edgeThreshold || params.y >= bounds.maxY - edgeThreshold
    }

    private fun startFalling(params: WindowManager.LayoutParams, movementSpeed: Float) {
        movementAnimator?.cancel()
        val bounds = getMovementBounds(params)
        val startY = params.y.coerceIn(0, bounds.maxY)
        val bottomY = bounds.maxY
        if (startY >= bottomY) {
            startMovement(params, movementSpeed)
            return
        }
        val distance = (bottomY - startY).toFloat()
        val duration = (700L + (distance / resources.displayMetrics.heightPixels).coerceIn(0f, 1f) * 900L).toLong()
        movementAnimator = ValueAnimator.ofInt(startY, bottomY).apply {
            this.duration = duration
            interpolator = AccelerateInterpolator(1.5f)
            addUpdateListener { animator ->
                if (!dragging) {
                    params.y = animator.animatedValue as Int
                    updateViewPosition(params)
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (!dragging) {
                        params.y = bottomY
                        updateViewPosition(params)
                        startMovement(params, movementSpeed)
                    }
                }
            })
            start()
        }
    }

    private fun startMovement(params: WindowManager.LayoutParams, movementSpeed: Float) {
        movementAnimator?.cancel()
        val bounds = getMovementBounds(params)
        val maxX = bounds.maxX.toFloat()
        val maxY = bounds.maxY.toFloat()
        if (maxX <= 0f && maxY <= 0f) return
        val topLength = maxX
        val rightLength = maxY
        val bottomLength = maxX
        val leftLength = maxY
        val perimeter = topLength + rightLength + bottomLength + leftLength
        if (perimeter <= 0f) return
        val startDistance = getPerimeterDistance(params.x.toFloat(), params.y.toFloat(), maxX, maxY, topLength, rightLength)
        val duration = (12_000L / movementSpeed).toLong().coerceAtLeast(3_000L)
        movementAnimator = ValueAnimator.ofFloat(startDistance, startDistance + perimeter).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                if (!dragging) {
                    val distance = (animator.animatedValue as Float) % perimeter
                    updatePositionOnRectangle(distance, topLength, rightLength, bottomLength, maxX, maxY, params)
                }
            }
            start()
        }
    }

    private fun getPerimeterDistance(x: Float, y: Float, maxX: Float, maxY: Float, topL: Float, rightL: Float): Float {
        val dT = y; val dR = maxX - x; val dB = maxY - y; val dL = x
        return when (minOf(dT, dR, dB, dL)) {
            dT -> x
            dR -> topL + y
            dB -> topL + rightL + (maxX - x)
            else -> topL + rightL + maxX + (maxY - y)
        }
    }

    private fun updatePositionOnRectangle(d: Float, tL: Float, rL: Float, bL: Float, mX: Float, mY: Float, p: WindowManager.LayoutParams) {
        when {
            d <= tL -> { p.x = d.roundToInt(); p.y = 0 }
            d <= tL + rL -> { p.x = mX.roundToInt(); p.y = (d - tL).roundToInt() }
            d <= tL + rL + bL -> { p.x = (mX - (d - tL - rL)).roundToInt(); p.y = mY.roundToInt() }
            else -> { p.x = 0; p.y = (mY - (d - tL - rL - bL)).roundToInt() }
        }
        updateViewPosition(p)
    }

    private fun updateViewPosition(p: WindowManager.LayoutParams) {
        staticCharacterView?.let { view -> runCatching { windowManager.updateViewLayout(view, p) } }
    }

    private fun getMovementBounds(p: WindowManager.LayoutParams): MovementBounds {
        val m = resources.displayMetrics
        val w = staticCharacterView?.width?.takeIf { it > 0 } ?: p.width.coerceAtLeast(0)
        val h = staticCharacterView?.height?.takeIf { it > 0 } ?: p.height.coerceAtLeast(0)
        return MovementBounds((m.widthPixels - w).coerceAtLeast(0), (m.heightPixels - h).coerceAtLeast(0))
    }

    private data class MovementBounds(val maxX: Int, val maxY: Int)
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_SYSTEM_ERROR

    companion object {
        var isServiceRunning = false
            private set
        
        private var staticCharacterView: View? = null

        const val EXTRA_DRAWABLE_RES = "drawable_res"
        const val EXTRA_IMAGE_URL = "image_url"
        const val EXTRA_CHARACTER_NAME = "character_name"
        const val EXTRA_MOVEMENT_SPEED = "movement_speed"
        const val EXTRA_IS_ENABLED = "is_enabled"
        const val ACTION_STOP = "com.shemiji.ACTION_STOP"
        private const val PREFS = "shimeji_overlay_runtime"
        private const val NOTIFICATION_ID = 4102
    }
}
