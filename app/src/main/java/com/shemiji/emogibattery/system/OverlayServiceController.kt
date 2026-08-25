package com.shemiji.emogibattery.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContextCompat
import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.service.BatteryToolbarOverlayService
import com.shemiji.emogibattery.service.OverlayAccessibilityService
import com.shemiji.emogibattery.service.ShimejiService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private const val PREFS = "overlay_prefs"
        private const val KEY_REQUESTED_BATTERY_OPT_OUT = "requested_battery_opt_out"
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = android.content.ComponentName(context, OverlayAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName.flattenToString(), ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun startBatteryToolbar(
        batteryEmoji: BatteryEmoji,
        toolbarStyle: ToolbarStyle,
        height: Int = 34,
        leftMargin: Int = 16,
        rightMargin: Int = 16,
        iconColor: String? = null,
        backgroundColor: String? = null,
    ): Result<Unit> = runCatching {
        requireOverlayPermission()
        ensureBatteryOptimizationExemption()

        val intent = Intent(context, BatteryToolbarOverlayService::class.java).apply {
            putExtra(BatteryToolbarOverlayService.EXTRA_DRAWABLE_RES, batteryEmoji.drawableRes ?: 0)
            putExtra(BatteryToolbarOverlayService.EXTRA_IMAGE_URL, batteryEmoji.imageUrl)
            putExtra(BatteryToolbarOverlayService.EXTRA_STYLE_NAME, toolbarStyle.name)
            putExtra(BatteryToolbarOverlayService.EXTRA_BACKGROUND_COLOR, backgroundColor ?: toolbarStyle.backgroundColor)
            putExtra(BatteryToolbarOverlayService.EXTRA_CONTENT_COLOR, iconColor ?: toolbarStyle.contentColor)
            putExtra(BatteryToolbarOverlayService.EXTRA_ACCENT_COLOR, toolbarStyle.accentColor)
            putExtra(BatteryToolbarOverlayService.EXTRA_HEIGHT, height)
            putExtra(BatteryToolbarOverlayService.EXTRA_LEFT_MARGIN, leftMargin)
            putExtra(BatteryToolbarOverlayService.EXTRA_RIGHT_MARGIN, rightMargin)
        }
        ContextCompat.startForegroundService(context, intent)
        Unit
    }

    fun stopBatteryToolbar(): Result<Unit> = runCatching {
        val intent = Intent(context, BatteryToolbarOverlayService::class.java).apply {
            action = BatteryToolbarOverlayService.ACTION_STOP
        }
        context.startService(intent)
        Unit
    }

    fun startShimeji(character: ShimejiCharacter): Result<Unit> = runCatching {
        requireOverlayPermission()
        ensureBatteryOptimizationExemption()

        val intent = Intent(context, ShimejiService::class.java).apply {
            putExtra(ShimejiService.EXTRA_DRAWABLE_RES, character.drawableRes ?: 0)
            putExtra(
                ShimejiService.EXTRA_IMAGE_URL,
                character.animationUrl?.takeIf(String::isNotBlank) ?: character.imageUrl,
            )
            putExtra(ShimejiService.EXTRA_CHARACTER_NAME, character.name)
            putExtra(ShimejiService.EXTRA_MOVEMENT_SPEED, character.movementSpeed)
        }
        ContextCompat.startForegroundService(context, intent)
        Unit
    }

    fun stopShimeji(): Result<Unit> = runCatching {
        val intent = Intent(context, ShimejiService::class.java).apply {
            action = ShimejiService.ACTION_STOP
        }
        context.startService(intent)
        Unit
    }

    private fun requireOverlayPermission() {
        check(Settings.canDrawOverlays(context)) {
            "Display over other apps permission is required"
        }
    }

    private fun ensureBatteryOptimizationExemption() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val packageName = context.packageName
                val isIgnoring = pm.isIgnoringBatteryOptimizations(packageName)
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val alreadyRequested = prefs.getBoolean(KEY_REQUESTED_BATTERY_OPT_OUT, false)

                if (!isIgnoring && !alreadyRequested) {
                    Log.d("OverlayServiceController", "Requesting battery optimizations exemption for package=$packageName")
                    // Launch settings intent to ask the user to whitelist the app
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    prefs.edit().putBoolean(KEY_REQUESTED_BATTERY_OPT_OUT, true).apply()
                } else {
                    Log.d("OverlayServiceController", "Battery optimization exemption already granted or requested (isIgnoring=$isIgnoring, alreadyRequested=$alreadyRequested)")
                }
            }
        } catch (t: Throwable) {
            Log.w("OverlayServiceController", "Failed to request battery optimization exemption", t)
        }
    }
}
