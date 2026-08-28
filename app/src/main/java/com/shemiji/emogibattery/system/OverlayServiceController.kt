package com.shemiji.emogibattery.system

import android.content.Context
import android.content.Intent
import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.service.OverlayAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OverlayServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun isAccessibilityServiceEnabled(): Boolean {
        return AccessibilityPermission.isEnabled(context)
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
        requireAccessibilityPermission()

        val intent = Intent(OverlayAccessibilityService.ACTION_UPDATE_BATTERY).apply {
            putExtra(OverlayAccessibilityService.EXTRA_DRAWABLE_RES, batteryEmoji.drawableRes ?: 0)
            putExtra(OverlayAccessibilityService.EXTRA_IMAGE_URL, batteryEmoji.imageUrl)
            putExtra(OverlayAccessibilityService.EXTRA_STYLE_NAME, toolbarStyle.name)
            putExtra(OverlayAccessibilityService.EXTRA_BACKGROUND_COLOR, backgroundColor ?: toolbarStyle.backgroundColor)
            putExtra(OverlayAccessibilityService.EXTRA_CONTENT_COLOR, iconColor ?: toolbarStyle.contentColor)
            putExtra(OverlayAccessibilityService.EXTRA_ACCENT_COLOR, toolbarStyle.accentColor)
            putExtra(OverlayAccessibilityService.EXTRA_HEIGHT, height)
            putExtra(OverlayAccessibilityService.EXTRA_LEFT_MARGIN, leftMargin)
            putExtra(OverlayAccessibilityService.EXTRA_RIGHT_MARGIN, rightMargin)
        }
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
        Unit
    }

    fun stopBatteryToolbar(): Result<Unit> = runCatching {
        requireAccessibilityPermission()
        context.sendBroadcast(
            Intent(OverlayAccessibilityService.ACTION_STOP_BATTERY)
                .setPackage(context.packageName),
        )
        Unit
    }

    fun startShimeji(
        character: ShimejiCharacter,
        sizeDp: Int = 112,
        movementSpeed: Float = character.movementSpeed,
    ): Result<Unit> = runCatching {
        requireAccessibilityPermission()

        val intent = Intent(OverlayAccessibilityService.ACTION_UPDATE_SHIMEJI).apply {
            putExtra(OverlayAccessibilityService.EXTRA_DRAWABLE_RES, character.drawableRes ?: 0)
            putExtra(
                OverlayAccessibilityService.EXTRA_IMAGE_URL,
                character.animationUrl?.takeIf(String::isNotBlank) ?: character.imageUrl,
            )
            putExtra(OverlayAccessibilityService.EXTRA_CHARACTER_NAME, character.name)
            putExtra(OverlayAccessibilityService.EXTRA_MOVEMENT_SPEED, movementSpeed.coerceIn(0.5f, 3f))
            putExtra(OverlayAccessibilityService.EXTRA_CHARACTER_SIZE_DP, sizeDp.coerceIn(72, 176))
        }
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
        Unit
    }

    fun stopShimeji(): Result<Unit> = runCatching {
        requireAccessibilityPermission()
        context.sendBroadcast(
            Intent(OverlayAccessibilityService.ACTION_STOP_SHIMEJI)
                .setPackage(context.packageName),
        )
        Unit
    }

    private fun requireAccessibilityPermission() {
        check(AccessibilityPermission.isEnabled(context)) {
            "Accessibility permission is required"
        }
    }
}
