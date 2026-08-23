package com.shemiji.emogibattery.system

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.service.BatteryToolbarOverlayService
import com.shemiji.emogibattery.service.OverlayAccessibilityService
import com.shemiji.emogibattery.service.ShimejiOverlayService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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
    ): Result<Unit> = runCatching {
        requireOverlayPermission()
        val intent = Intent(context, BatteryToolbarOverlayService::class.java).apply {
            putExtra(BatteryToolbarOverlayService.EXTRA_DRAWABLE_RES, batteryEmoji.drawableRes ?: 0)
            putExtra(BatteryToolbarOverlayService.EXTRA_IMAGE_URL, batteryEmoji.imageUrl)
            putExtra(BatteryToolbarOverlayService.EXTRA_STYLE_NAME, toolbarStyle.name)
            putExtra(BatteryToolbarOverlayService.EXTRA_BACKGROUND_COLOR, toolbarStyle.backgroundColor)
            putExtra(BatteryToolbarOverlayService.EXTRA_CONTENT_COLOR, toolbarStyle.contentColor)
            putExtra(BatteryToolbarOverlayService.EXTRA_ACCENT_COLOR, toolbarStyle.accentColor)
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
        val intent = Intent(context, ShimejiOverlayService::class.java).apply {
            putExtra(ShimejiOverlayService.EXTRA_DRAWABLE_RES, character.drawableRes ?: 0)
            putExtra(
                ShimejiOverlayService.EXTRA_IMAGE_URL,
                character.animationUrl?.takeIf(String::isNotBlank) ?: character.imageUrl,
            )
            putExtra(ShimejiOverlayService.EXTRA_CHARACTER_NAME, character.name)
            putExtra(ShimejiOverlayService.EXTRA_MOVEMENT_SPEED, character.movementSpeed)
        }
        ContextCompat.startForegroundService(context, intent)
        Unit
    }

    fun stopShimeji(): Result<Unit> = runCatching {
        val intent = Intent(context, ShimejiOverlayService::class.java).apply {
            action = ShimejiOverlayService.ACTION_STOP
        }
        context.startService(intent)
        Unit
    }

    private fun requireOverlayPermission() {
        check(Settings.canDrawOverlays(context)) {
            "Display over other apps permission is required"
        }
    }
}
