package com.shemiji.emogibattery.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.shemiji.emogibattery.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ShimejiService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val stateController = SavedStateRegistryController.create(this)
    private val serviceViewModelStore = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val physics = ShimejiPhysicsEngine()
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = serviceViewModelStore
    override val savedStateRegistry: SavedStateRegistry get() = stateController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var characterView: ComposeView? = null
    private var shelfView: ComposeView? = null
    private var characterParams: WindowManager.LayoutParams? = null
    private var ticker: Job? = null
    private var batteryRegistered = false
    private var batteryPercent by mutableIntStateOf(0)
    private var speedMultiplier = 1f
    private var downWindowX = 0
    private var downWindowY = 0
    private var downRawX = 0f
    private var downRawY = 0f

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            batteryPercent = (level * 100f / scale).roundToInt().coerceIn(0, 100)
        }
    }

    override fun onCreate() {
        super.onCreate(); isServiceRunning = true
        stateController.performAttach(); stateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        OverlayNotificationHelper.createChannel(this)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)); batteryRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (intent?.action == ACTION_STOP) {
            prefs.edit().putBoolean(EXTRA_IS_ENABLED, false).commit(); stopSelf(); return START_NOT_STICKY
        }
        if (intent?.hasExtra(EXTRA_CHARACTER_NAME) == true) {
            prefs.edit().putString(EXTRA_CHARACTER_NAME, intent.getStringExtra(EXTRA_CHARACTER_NAME))
                .putFloat(EXTRA_MOVEMENT_SPEED, intent.getFloatExtra(EXTRA_MOVEMENT_SPEED, 1f))
                .putBoolean(EXTRA_IS_ENABLED, true).commit()
        }
        if (!prefs.getBoolean(EXTRA_IS_ENABLED, false)) { stopSelf(); return START_NOT_STICKY }
        speedMultiplier = prefs.getFloat(EXTRA_MOVEMENT_SPEED, 3f)
        val name = prefs.getString(EXTRA_CHARACTER_NAME, "Shimeji") ?: "Shimeji"
        val notification = OverlayNotificationHelper.build(this, "$name is active", "Drag the pet; battery shelf is active")
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, notification)
        if (Settings.canDrawOverlays(this)) showOverlays() else stopSelf()
        // START_STICKY rebuilds both windows from persisted pose/coordinates after process death.
        return START_STICKY
    }

    private fun showOverlays() {
        if (characterView?.isAttachedToWindow == true) return
        val size = dp(112); val shelfHeight = dp(48)
        physics.configure(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels, size, shelfHeight, speedMultiplier)
        restoreState()
        val pet = ownedComposeView { SpriteCharacter(physics.motion, physics.edge) }
        val petParams = WindowManager.LayoutParams(size, size, overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = physics.x.roundToInt(); y = physics.y.roundToInt()
        }
        pet.setOnTouchListener { _, event -> handlePetTouch(event, petParams) }
        windowManager.addView(pet, petParams); characterView = pet; characterParams = petParams

        val shelf = ownedComposeView { /*SupportBar(batteryPercent)*/ }
        val shelfParams = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, shelfHeight, overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.BOTTOM or Gravity.START }
        windowManager.addView(shelf, shelfParams); shelfView = shelf
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START); startTicker()
    }

    private fun ownedComposeView(content: @Composable () -> Unit) = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@ShimejiService); setViewTreeViewModelStoreOwner(this@ShimejiService)
        setViewTreeSavedStateRegistryOwner(this@ShimejiService); setContent(content)
    }

    private fun handlePetTouch(event: MotionEvent, params: WindowManager.LayoutParams): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                physics.beginDrag(); downWindowX = params.x; downWindowY = params.y
                downRawX = event.rawX; downRawY = event.rawY; return true
            }
            MotionEvent.ACTION_MOVE -> {
                physics.dragTo(downWindowX + event.rawX - downRawX, downWindowY + event.rawY - downRawY)
                updatePetWindow(); return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                physics.endDrag(); updatePetWindow(); persistState(); return true
            }
        }
        return false
    }

    private fun startTicker() {
        ticker?.cancel(); ticker = scope.launch {
            var previous = android.os.SystemClock.uptimeMillis(); var snapshot = previous
            while (isActive) {
                val now = android.os.SystemClock.uptimeMillis(); physics.tick(now - previous); previous = now
                updatePetWindow()
                if (now - snapshot >= 1_000L) { persistState(); snapshot = now }
                delay(16L)
            }
        }
    }

    private fun updatePetWindow() {
        val view = characterView ?: return; val params = characterParams ?: return
        params.x = physics.x.roundToInt(); params.y = physics.y.roundToInt()
        if (view.isAttachedToWindow) runCatching { windowManager.updateViewLayout(view, params) }
    }

    private fun restoreState() {
        val p = getSharedPreferences(PREFS, MODE_PRIVATE)
        val motion = runCatching { ShimejiMotion.valueOf(p.getString(KEY_MOTION, ShimejiMotion.WALKING_RIGHT.name)!!) }.getOrDefault(ShimejiMotion.WALKING_RIGHT)
        val edge = runCatching { ScreenEdge.valueOf(p.getString(KEY_EDGE, ScreenEdge.BOTTOM.name)!!) }.getOrDefault(ScreenEdge.BOTTOM)
        physics.restore(p.getFloat(KEY_X, 0f), p.getFloat(KEY_Y, Float.MAX_VALUE), motion, edge)
    }

    private fun persistState() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putFloat(KEY_X, physics.x).putFloat(KEY_Y, physics.y)
            .putString(KEY_MOTION, physics.motion.name).putString(KEY_EDGE, physics.edge.name).apply()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        physics.configure(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels, dp(112), dp(48), speedMultiplier)
        updatePetWindow()
    }

    override fun onDestroy() {
        isServiceRunning = false; persistState(); ticker?.cancel()
        characterView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        shelfView?.let { runCatching { windowManager.removeViewImmediate(it) } }
        characterView = null; shelfView = null; characterParams = null
        if (batteryRegistered) unregisterReceiver(batteryReceiver)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceViewModelStore.clear(); scope.cancel(); super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) { super.onTaskRemoved(rootIntent) }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    @Suppress("DEPRECATION") private fun overlayWindowType() = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

    companion object {
        var isServiceRunning = false; private set
        const val EXTRA_DRAWABLE_RES = "drawable_res"; const val EXTRA_IMAGE_URL = "image_url"
        const val EXTRA_CHARACTER_NAME = "character_name"; const val EXTRA_MOVEMENT_SPEED = "movement_speed"
        const val EXTRA_IS_ENABLED = "is_enabled"; const val ACTION_STOP = "com.shemiji.ACTION_STOP"
        private const val PREFS = "shimeji_overlay_runtime"; private const val KEY_X = "render_x"
        private const val KEY_Y = "render_y"; private const val KEY_MOTION = "render_motion"
        private const val KEY_EDGE = "render_edge"; private const val NOTIFICATION_ID = 4102
    }
}

@Composable
private fun SpriteCharacter(motion: ShimejiMotion, edge: ScreenEdge) {
    val sheet = ImageBitmap.imageResource(R.drawable.img_1)
    var clock by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { clock = android.os.SystemClock.uptimeMillis(); delay(12L) } }
    val phase = ((clock / 150L) % 4L).toInt()
    val (row, column) = when {
        motion == ShimejiMotion.IDLE -> 0 to 0
        motion == ShimejiMotion.FALLING -> 7 to (phase % 2)
        motion == ShimejiMotion.BOUNCING -> 7 to 2
        motion == ShimejiMotion.JUMPING_LEFT_TO_RIGHT || motion == ShimejiMotion.JUMPING_RIGHT_TO_LEFT -> 3 + (phase % 3) to phase
        motion == ShimejiMotion.TOP_WALKING_LEFT || motion == ShimejiMotion.TOP_WALKING_RIGHT -> 6 to phase
        motion == ShimejiMotion.CLIMBING_LEFT || motion == ShimejiMotion.CLIMBING_RIGHT -> 5 to phase
        edge == ScreenEdge.BOTTOM -> 1 to phase
        else -> 0 to phase
    }
    val flip = motion == ShimejiMotion.WALKING_LEFT ||
        motion == ShimejiMotion.CLIMBING_RIGHT ||
        motion == ShimejiMotion.JUMPING_RIGHT_TO_LEFT ||
        motion == ShimejiMotion.TOP_WALKING_LEFT
    Canvas(Modifier.fillMaxSize().graphicsLayer { scaleX = if (flip) -1f else 1f }) {
        drawImage(sheet, IntOffset(column * sheet.width / 4, row * sheet.height / 8), IntSize(sheet.width / 4, sheet.height / 8),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
    }
}


