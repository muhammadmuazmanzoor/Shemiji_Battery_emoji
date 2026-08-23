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
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import coil.load
import com.shemiji.emogibattery.R
import kotlin.math.roundToInt

class ShimejiOverlayService : Service() {

    private lateinit var windowManager: WindowManager

    private var characterView: ImageView? = null
    private var movementAnimator: ValueAnimator? = null

    private var dragging = false

    private var dragStartX = 0
    private var dragStartY = 0

    private var touchStartX = 0f
    private var touchStartY = 0f

    override fun onCreate() {
        super.onCreate()
        Log.d("ShimejiOverlayService", "Service Created")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        OverlayNotificationHelper.createChannel(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        val runtime = getSharedPreferences(
            PREFS,
            MODE_PRIVATE
        )

        val characterName = runtime
            .getString(EXTRA_CHARACTER_NAME, null)
            .orEmpty()
            .ifBlank {
                "Shimeji"
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                OverlayNotificationHelper.build(
                    this,
                    "$characterName is active",
                    "Drag the character anywhere on your screen"
                ),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                NOTIFICATION_ID,
                OverlayNotificationHelper.build(
                    this,
                    "$characterName is active",
                    "Drag the character anywhere on your screen"
                )
            )
        }

        if (intent != null && intent.hasExtra(EXTRA_CHARACTER_NAME)) {
            if (intent.action == ACTION_STOP) {
                runtime.edit().putBoolean(EXTRA_IS_ENABLED, false).commit()
                stopSelf()
                return START_NOT_STICKY
            }

            runtime.edit()
                .putInt(
                    EXTRA_DRAWABLE_RES,
                    intent.getIntExtra(
                        EXTRA_DRAWABLE_RES,
                        0
                    )
                )
                .putString(
                    EXTRA_IMAGE_URL,
                    intent.getStringExtra(EXTRA_IMAGE_URL)
                )
                .putString(
                    EXTRA_CHARACTER_NAME,
                    intent.getStringExtra(EXTRA_CHARACTER_NAME)
                )
                .putFloat(
                    EXTRA_MOVEMENT_SPEED,
                    intent.getFloatExtra(
                        EXTRA_MOVEMENT_SPEED,
                        1f
                    )
                )
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

        showCharacter(
            drawableRes = runtime.getInt(
                EXTRA_DRAWABLE_RES,
                R.drawable.demo_shimeji_mochi
            ),
            imageUrl = runtime.getString(
                EXTRA_IMAGE_URL,
                null
            ),
            movementSpeed = runtime.getFloat(
                EXTRA_MOVEMENT_SPEED,
                1f
            ).coerceIn(
                0.25f,
                3f
            )
        )

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
        Log.d("ShimejiOverlayService", "Service Destroyed")

        movementAnimator?.cancel()
        movementAnimator = null

        characterView?.let { view ->
            runCatching {
                windowManager.removeView(view)
            }
        }

        characterView = null

        super.onDestroy()
    }

    private fun showCharacter(
        drawableRes: Int,
        imageUrl: String?,
        movementSpeed: Float
    ) {
        if (characterView != null && characterView?.isAttachedToWindow == true) {
            return
        }

        movementAnimator?.cancel()
        movementAnimator = null

        characterView?.let { oldView ->
            runCatching {
                windowManager.removeView(oldView)
            }
        }

        val imageView = ImageView(this).apply {

            scaleType = ImageView.ScaleType.FIT_CENTER

            val model: Any =
                imageUrl?.takeIf(String::isNotBlank)
                    ?: drawableRes

            load(model) {
                crossfade(true)
            }
        }

        val params = WindowManager.LayoutParams(
            dp(112),
            dp(112),
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {

            gravity = Gravity.TOP or Gravity.START

            x = dp(24)
            y = dp(220)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        imageView.setOnTouchListener { _, event ->
            handleTouch(
                event = event,
                params = params,
                movementSpeed = movementSpeed
            )
        }

        characterView = imageView

        windowManager.addView(
            imageView,
            params
        )

        startMovement(
            params = params,
            movementSpeed = movementSpeed
        )
    }

    private fun handleTouch(
        event: MotionEvent,
        params: WindowManager.LayoutParams,
        movementSpeed: Float
    ): Boolean {

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                dragging = true

                // Stop both movement and falling.
                movementAnimator?.cancel()
                movementAnimator = null

                dragStartX = params.x
                dragStartY = params.y

                touchStartX = event.rawX
                touchStartY = event.rawY

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                val bounds = getMovementBounds(params)

                val newX =
                    dragStartX +
                            (event.rawX - touchStartX).roundToInt()

                val newY =
                    dragStartY +
                            (event.rawY - touchStartY).roundToInt()

                params.x = newX.coerceIn(
                    0,
                    bounds.maxX
                )

                params.y = newY.coerceIn(
                    0,
                    bounds.maxY
                )

                updateViewPosition(params)

                return true
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                dragging = false

                /*
                 * If the character is touching any edge,
                 * continue normal perimeter movement.
                 *
                 * Otherwise, make it fall to the bottom.
                 */
                if (isOnEdge(params)) {

                    startMovement(
                        params = params,
                        movementSpeed = movementSpeed
                    )

                } else {

                    startFalling(
                        params = params,
                        movementSpeed = movementSpeed
                    )
                }

                return true
            }
        }

        return false
    }

    /**
     * Determines whether Shimeji is currently touching
     * one of the four screen edges.
     */
    private fun isOnEdge(
        params: WindowManager.LayoutParams
    ): Boolean {

        val bounds = getMovementBounds(params)

        val edgeThreshold = dp(8)

        val touchingLeft =
            params.x <= edgeThreshold

        val touchingRight =
            params.x >= bounds.maxX - edgeThreshold

        val touchingTop =
            params.y <= edgeThreshold

        val touchingBottom =
            params.y >= bounds.maxY - edgeThreshold

        return touchingLeft ||
                touchingRight ||
                touchingTop ||
                touchingBottom
    }

    /**
     * Makes Shimeji fall vertically to the bottom
     * when it is released away from the edges.
     */
    private fun startFalling(
        params: WindowManager.LayoutParams,
        movementSpeed: Float
    ) {
        movementAnimator?.cancel()

        val bounds = getMovementBounds(params)

        val startY = params.y.coerceIn(
            0,
            bounds.maxY
        )

        val bottomY = bounds.maxY

        if (startY >= bottomY) {
            startMovement(
                params = params,
                movementSpeed = movementSpeed
            )
            return
        }

        val distance = (bottomY - startY).toFloat()

        val duration = (
                700L +
                        (
                                distance /
                                        resources.displayMetrics.heightPixels
                                ).coerceIn(0f, 1f) * 900L
                ).toLong()

        movementAnimator = ValueAnimator.ofInt(
            startY,
            bottomY
        ).apply {

            this.duration = duration

            interpolator = AccelerateInterpolator(1.5f)

            addUpdateListener { animator ->

                if (dragging) {
                    return@addUpdateListener
                }

                params.y = animator.animatedValue as Int

                updateViewPosition(params)
            }

            addListener(
                object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {

                        if (dragging) {
                            return
                        }

                        params.y = bottomY

                        updateViewPosition(params)

                        startMovement(
                            params = params,
                            movementSpeed = movementSpeed
                        )
                    }
                }
            )

            start()
        }
    }

    /**
     * Starts continuous movement around the complete
     * rectangular screen perimeter.
     *
     *        TOP
     *   ┌──────────────→┐
     *   ↑               ↓
     *   │               │
     *   │               ↓
     *   └←──────────────┘
     *
     * TOP → RIGHT → BOTTOM → LEFT → TOP
     */
    private fun startMovement(
        params: WindowManager.LayoutParams,
        movementSpeed: Float
    ) {

        movementAnimator?.cancel()

        val bounds = getMovementBounds(params)

        val maxX = bounds.maxX.toFloat()
        val maxY = bounds.maxY.toFloat()

        if (maxX <= 0f && maxY <= 0f) {
            return
        }

        val topLength = maxX
        val rightLength = maxY
        val bottomLength = maxX
        val leftLength = maxY

        val perimeter =
            topLength +
                    rightLength +
                    bottomLength +
                    leftLength

        if (perimeter <= 0f) {
            return
        }

        val currentX =
            params.x
                .coerceIn(
                    0,
                    bounds.maxX
                )
                .toFloat()

        val currentY =
            params.y
                .coerceIn(
                    0,
                    bounds.maxY
                )
                .toFloat()

        val startDistance =
            getPerimeterDistance(
                currentX = currentX,
                currentY = currentY,
                maxX = maxX,
                maxY = maxY,
                topLength = topLength,
                rightLength = rightLength
            )

        val duration =
            (12_000L / movementSpeed)
                .toLong()
                .coerceAtLeast(3_000L)

        movementAnimator =
            ValueAnimator.ofFloat(
                startDistance,
                startDistance + perimeter
            ).apply {

                this.duration = duration

                repeatCount =
                    ValueAnimator.INFINITE

                interpolator =
                    LinearInterpolator()

                addUpdateListener { animator ->

                    if (dragging) {
                        return@addUpdateListener
                    }

                    val animatedDistance =
                        animator.animatedValue as Float

                    val distance =
                        animatedDistance % perimeter

                    updatePositionOnRectangle(
                        distance = distance,
                        topLength = topLength,
                        rightLength = rightLength,
                        bottomLength = bottomLength,
                        maxX = maxX,
                        maxY = maxY,
                        params = params
                    )
                }

                start()
            }
    }

    /**
     * Finds the closest point on the perimeter and
     * converts it into a perimeter distance.
     */
    private fun getPerimeterDistance(
        currentX: Float,
        currentY: Float,
        maxX: Float,
        maxY: Float,
        topLength: Float,
        rightLength: Float
    ): Float {

        val distanceToTop = currentY
        val distanceToRight = maxX - currentX
        val distanceToBottom = maxY - currentY
        val distanceToLeft = currentX

        return when (
            minOf(
                distanceToTop,
                distanceToRight,
                distanceToBottom,
                distanceToLeft
            )
        ) {

            distanceToTop ->
                currentX

            distanceToRight ->
                topLength + currentY

            distanceToBottom ->
                topLength +
                        rightLength +
                        (maxX - currentX)

            else ->
                topLength +
                        rightLength +
                        maxX +
                        (maxY - currentY)
        }
    }

    /**
     * Converts perimeter distance into X/Y coordinates.
     */
    private fun updatePositionOnRectangle(
        distance: Float,
        topLength: Float,
        rightLength: Float,
        bottomLength: Float,
        maxX: Float,
        maxY: Float,
        params: WindowManager.LayoutParams
    ) {

        when {

            // TOP: LEFT → RIGHT
            distance <= topLength -> {

                params.x =
                    distance.roundToInt()

                params.y = 0
            }

            // RIGHT: TOP → BOTTOM
            distance <=
                    topLength + rightLength -> {

                params.x =
                    maxX.roundToInt()

                params.y =
                    (
                            distance -
                                    topLength
                            ).roundToInt()
            }

            // BOTTOM: RIGHT → LEFT
            distance <=
                    topLength +
                    rightLength +
                    bottomLength -> {

                params.x =
                    (
                            maxX -
                                    (
                                            distance -
                                                    topLength -
                                                    rightLength
                                            )
                            ).roundToInt()

                params.y =
                    maxY.roundToInt()
            }

            // LEFT: BOTTOM → TOP
            else -> {

                params.x = 0

                params.y =
                    (
                            maxY -
                                    (
                                            distance -
                                                    topLength -
                                                    rightLength -
                                                    bottomLength
                                            )
                            ).roundToInt()
            }
        }

        updateViewPosition(params)
    }

    private fun updateViewPosition(
        params: WindowManager.LayoutParams
    ) {

        characterView?.let { view ->

            runCatching {
                windowManager.updateViewLayout(
                    view,
                    params
                )
            }
        }
    }

    private fun getMovementBounds(
        params: WindowManager.LayoutParams
    ): MovementBounds {

        val displayMetrics =
            resources.displayMetrics

        val viewWidth =
            characterView?.width
                ?.takeIf { it > 0 }
                ?: params.width.coerceAtLeast(0)

        val viewHeight =
            characterView?.height
                ?.takeIf { it > 0 }
                ?: params.height.coerceAtLeast(0)

        val maxX =
            (
                    displayMetrics.widthPixels -
                            viewWidth
                    ).coerceAtLeast(0)

        val maxY =
            (
                    displayMetrics.heightPixels -
                            viewHeight
                    ).coerceAtLeast(0)

        return MovementBounds(
            maxX = maxX,
            maxY = maxY
        )
    }

    private data class MovementBounds(
        val maxX: Int,
        val maxY: Int
    )

    private fun dp(value: Int): Int =
        (
                value *
                        resources.displayMetrics.density
                ).roundToInt()

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
        }

    companion object {

        const val EXTRA_DRAWABLE_RES =
            "drawable_res"

        const val EXTRA_IMAGE_URL =
            "image_url"

        const val EXTRA_CHARACTER_NAME =
            "character_name"

        const val EXTRA_MOVEMENT_SPEED =
            "movement_speed"
        const val EXTRA_IS_ENABLED = "is_enabled"
        const val ACTION_STOP = "com.shemiji.ACTION_STOP"

        private const val PREFS =
            "shimeji_overlay_runtime"

        private const val NOTIFICATION_ID =
            4102
    }
}