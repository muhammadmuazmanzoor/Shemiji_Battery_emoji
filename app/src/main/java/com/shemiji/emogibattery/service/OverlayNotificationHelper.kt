package com.shemiji.emogibattery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.shemiji.emogibattery.MainActivity
import com.shemiji.emogibattery.R

internal object OverlayNotificationHelper {
    const val CHANNEL_ID = "active_customizations"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Active customizations",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps your battery toolbar and Shimeji character active"
                enableLights(false)
                enableVibration(false)
            },
        )
    }

    fun build(context: Context, title: String, text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(Color.rgb(97, 103, 243))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }
}
