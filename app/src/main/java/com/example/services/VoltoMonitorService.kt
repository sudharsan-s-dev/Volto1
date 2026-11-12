package com.example.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.persistence.datastore.VoltoPreferences
import com.example.persistence.room.AlertEntity
import com.example.persistence.room.AlertTrackingEntity
import com.example.persistence.room.VoltoDatabase
import com.example.types.AlertSeverity
import com.example.types.Appliance
import com.example.types.LimitAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoltoMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: VoltoDatabase
    private lateinit var preferences: VoltoPreferences

    companion object {
        const val ACTION_START_MONITORING = "com.example.volto.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.volto.STOP_MONITORING"

        fun startService(context: Context) {
            val intent = Intent(context, VoltoMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VoltoMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        database = VoltoDatabase.getInstance(applicationContext)
        preferences = VoltoPreferences(applicationContext)
        NotificationHelper.createNotificationChannels(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground Notification
        val initialNotif = NotificationHelper.buildForegroundNotification(this, activeCount = 3, liveWatts = 1680)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIF_ID_FOREGROUND_SERVICE,
                initialNotif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.NOTIF_ID_FOREGROUND_SERVICE, initialNotif)
        }

        startMonitoringLoop()
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)

            while (isActive) {
                try {
                    val now = Date()
                    val todayStr = dateFormat.format(now)
                    val monthStr = monthFormat.format(now)

                    val applianceDao = database.applianceDao()
                    val alertDao = database.alertDao()
                    val alertTrackingDao = database.alertTrackingDao()

                    val appliancesEntities = applianceDao.getAllAppliancesSync()
                    val budget = preferences.monthlyBudget.first()

                    var currentMonthSpend = 0.0

                    // 1. Evaluate Appliance Daily Limits
                    for (entity in appliancesEntities) {
                        val appModel = Appliance(
                            id = entity.id,
                            name = entity.name,
                            type = entity.type,
                            iconName = entity.iconName,
                            isOn = entity.isOn,
                            isCritical = entity.isCritical,
                            currentWatts = entity.baseWatts,
                            baseWatts = entity.baseWatts,
                            dailyKwh = entity.dailyKwh,
                            weeklyKwh = entity.weeklyKwh,
                            monthlyKwh = entity.monthlyKwh,
                            dailyLimitKwh = entity.dailyLimitKwh,
                            limitAction = try { LimitAction.valueOf(entity.limitAction) } catch (_: Exception) { LimitAction.WARN },
                            isOnline = entity.isOnline,
                            deviceId = entity.deviceId
                        )

                        currentMonthSpend += entity.monthlyKwh * 6.50

                        val checkResult = LimitMonitor.evaluateApplianceLimit(appModel)
                        val alertToEmit = checkResult.alertToEmit
                        if (alertToEmit != null) {
                            val trackingKey = "limit_${appModel.id}_$todayStr"
                            val alreadyEmitted = alertTrackingDao.hasAlertBeenEmitted(trackingKey)

                            if (!alreadyEmitted) {
                                // Persist in Room alert tracking: rule fires once per day per appliance
                                alertTrackingDao.recordAlertEmitted(
                                    AlertTrackingEntity(
                                        trackingKey = trackingKey,
                                        emittedAt = System.currentTimeMillis(),
                                        category = "LIMIT"
                                    )
                                )

                                alertDao.insertAlert(
                                    AlertEntity(
                                        id = "alt-${System.currentTimeMillis()}-${appModel.id}",
                                        title = alertToEmit.title,
                                        message = alertToEmit.message,
                                        timestamp = System.currentTimeMillis(),
                                        severity = alertToEmit.severity.name,
                                        category = if (appModel.isCritical) "SAFETY" else "LIMIT",
                                        isRead = false,
                                        relatedApplianceId = appModel.id,
                                        actionText = alertToEmit.actionText
                                    )
                                )

                                NotificationHelper.postNotification(
                                    context = applicationContext,
                                    notificationId = appModel.id.hashCode(),
                                    channelId = if (appModel.isCritical) NotificationHelper.CHANNEL_SAFETY else NotificationHelper.CHANNEL_LIMITS,
                                    title = alertToEmit.title,
                                    message = alertToEmit.message
                                )

                                if (checkResult.shouldAutoCut && !appModel.isCritical) {
                                    applianceDao.updateApplianceState(appModel.id, false)
                                }
                            }
                        }
                    }

                    // 2. Evaluate Monthly Budget Milestones (50%, 75%, 90%, 100%)
                    val budgetAlert = BudgetMonitor.checkBudgetMilestones(currentSpendInr = currentMonthSpend, budgetInr = budget)
                    if (budgetAlert != null) {
                        val milestoneTag = when (budgetAlert.severity) {
                            AlertSeverity.CRITICAL -> "100"
                            AlertSeverity.WARNING -> "90"
                            else -> if (budgetAlert.title.contains("75%")) "75" else "50"
                        }
                        val trackingKey = "budget_${milestoneTag}_$monthStr"
                        val alreadyEmitted = alertTrackingDao.hasAlertBeenEmitted(trackingKey)

                        if (!alreadyEmitted) {
                            // Persist in Room alert tracking: milestone fires once per threshold per month
                            alertTrackingDao.recordAlertEmitted(
                                AlertTrackingEntity(
                                    trackingKey = trackingKey,
                                    emittedAt = System.currentTimeMillis(),
                                    category = "BUDGET"
                                )
                            )

                            alertDao.insertAlert(
                                AlertEntity(
                                    id = "alt-bgt-${System.currentTimeMillis()}-$milestoneTag",
                                    title = budgetAlert.title,
                                    message = budgetAlert.message,
                                    timestamp = System.currentTimeMillis(),
                                    severity = budgetAlert.severity.name,
                                    category = "BUDGET",
                                    isRead = false,
                                    relatedApplianceId = null,
                                    actionText = budgetAlert.actionText
                                )
                            )

                            NotificationHelper.postNotification(
                                context = applicationContext,
                                notificationId = 8000 + milestoneTag.toInt(),
                                channelId = NotificationHelper.CHANNEL_BUDGET,
                                title = budgetAlert.title,
                                message = budgetAlert.message
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Check interval: every 30 seconds
                delay(30_000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
