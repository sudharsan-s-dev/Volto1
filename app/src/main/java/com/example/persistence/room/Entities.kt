package com.example.persistence.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.types.AlertSeverity
import com.example.types.Appliance
import com.example.types.LimitAction
import com.example.types.VoltoAlert

@Entity(tableName = "appliances")
data class ApplianceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val iconName: String,
    val isOn: Boolean,
    val isCritical: Boolean,
    val currentWatts: Int,
    val baseWatts: Int,
    val dailyKwh: Double,
    val weeklyKwh: Double,
    val monthlyKwh: Double,
    val dailyLimitKwh: Double,
    val limitAction: String, // NOTIFY, WARN, CONFIRM, AUTO_OFF
    val isOnline: Boolean,
    val deviceId: String
) {
    fun toDomain(): Appliance = Appliance(
        id = id,
        name = name,
        type = type,
        iconName = iconName,
        isOn = isOn,
        isCritical = isCritical,
        currentWatts = currentWatts,
        baseWatts = baseWatts,
        dailyKwh = dailyKwh,
        weeklyKwh = weeklyKwh,
        monthlyKwh = monthlyKwh,
        dailyLimitKwh = dailyLimitKwh,
        limitAction = LimitAction.valueOf(limitAction),
        isOnline = isOnline,
        deviceId = deviceId
    )

    companion object {
        fun fromDomain(app: Appliance): ApplianceEntity = ApplianceEntity(
            id = app.id,
            name = app.name,
            type = app.type,
            iconName = app.iconName,
            isOn = app.isOn,
            isCritical = app.isCritical,
            currentWatts = app.currentWatts,
            baseWatts = app.baseWatts,
            dailyKwh = app.dailyKwh,
            weeklyKwh = app.weeklyKwh,
            monthlyKwh = app.monthlyKwh,
            dailyLimitKwh = app.dailyLimitKwh,
            limitAction = app.limitAction.name,
            isOnline = app.isOnline,
            deviceId = app.deviceId
        )
    }
}

@Entity(tableName = "hourly_readings", primaryKeys = ["applianceId", "hourTimestamp"])
data class HourlyReadingEntity(
    val applianceId: String,
    val hourTimestamp: Long, // Start of hour epoch ms
    val hourOfDay: Int,      // 0..23
    val kwh: Double,
    val avgWatts: Int,
    val peakWatts: Int
)

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val severity: String, // INFO, WARNING, CRITICAL
    val category: String, // SAFETY, BUDGET, HARDWARE
    val isRead: Boolean,
    val relatedApplianceId: String?,
    val actionText: String?
) {
    fun toDomain(): VoltoAlert = VoltoAlert(
        id = id,
        title = title,
        message = message,
        timestamp = timestamp,
        severity = AlertSeverity.valueOf(severity),
        isRead = isRead,
        relatedApplianceId = relatedApplianceId,
        actionText = actionText
    )

    companion object {
        fun fromDomain(a: VoltoAlert, category: String = "GENERAL"): AlertEntity = AlertEntity(
            id = a.id,
            title = a.title,
            message = a.message,
            timestamp = a.timestamp,
            severity = a.severity.name,
            category = category,
            isRead = a.isRead,
            relatedApplianceId = a.relatedApplianceId,
            actionText = a.actionText
        )
    }
}

@Entity(tableName = "tariff_config")
data class TariffConfigEntity(
    @PrimaryKey val id: Int = 1,
    val providerName: String = "BESCOM (Bengaluru)",
    val slab1Limit: Double = 100.0,
    val slab1Rate: Double = 4.50,
    val slab2Limit: Double = 200.0,
    val slab2Rate: Double = 6.00,
    val slab3Limit: Double = 300.0,
    val slab3Rate: Double = 7.50,
    val slab4Rate: Double = 8.50,
    val fixedCharges: Double = 120.0,
    val taxDutyPercent: Double = 9.0,
    val monthlyBudget: Double = 3500.0
)

@Entity(tableName = "alert_tracking")
data class AlertTrackingEntity(
    @PrimaryKey val trackingKey: String, // e.g. limit_app-ac_2026-11-11 or budget_90_2026-11
    val emittedAt: Long,
    val category: String
)
