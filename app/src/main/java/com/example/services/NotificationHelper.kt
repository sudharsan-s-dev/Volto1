package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_SAFETY = "volto_safety_channel"
    const val CHANNEL_BUDGET = "volto_budget_channel"
    const val CHANNEL_LIMITS = "volto_limits_channel"
    const val CHANNEL_MONITOR = "volto_monitor_channel"

    const val NOTIF_ID_FOREGROUND_SERVICE = 9001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Safety Channel - HIGH importance (sound, heads-up)
            val safetyChannel = NotificationChannel(
                CHANNEL_SAFETY,
                "Safety & Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical appliance protection, surge warnings, and auto-cuts"
                enableVibration(true)
            }

            // 2. Budget Channel - DEFAULT importance
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Monthly Budget & Tariff Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Milestone alerts at 50%, 75%, 90%, and 100% of budget"
            }

            // 3. Limits Channel - DEFAULT importance
            val limitsChannel = NotificationChannel(
                CHANNEL_LIMITS,
                "Daily Appliance Quotas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily kWh appliance quota notifications and warnings"
            }

            // 4. Foreground Service Channel - LOW importance
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITOR,
                "Active Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent background monitor for Indian home energy safety"
            }

            notificationManager.createNotificationChannels(
                listOf(safetyChannel, budgetChannel, limitsChannel, monitorChannel)
            )
        }
    }

    fun buildForegroundNotification(context: Context, activeCount: Int, liveWatts: Int): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setContentTitle("VOLTO Energy Sentinel Active")
            .setContentText("Monitoring $activeCount appliances live (${liveWatts} W)")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun postNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        message: String,
        isAllowedByUser: Boolean = true
    ) {
        if (!isAllowedByUser) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val priority = if (channelId == CHANNEL_SAFETY) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(priority)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ permission not yet granted
            e.printStackTrace()
        }
    }
}
