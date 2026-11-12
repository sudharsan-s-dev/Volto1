package com.example.types

enum class LimitAction(val label: String, val description: String) {
    NOTIFY("Notify Only", "Send notification when limit is reached"),
    WARN("Warning Alert", "Highlight high power alert in dashboard"),
    CONFIRM("Require Confirmation", "Ask confirmation before continuing operation"),
    AUTO_OFF("Auto Cut-Off", "Safely turn off appliance to prevent bill surge")
}

enum class DeviceCommandStatus {
    IDLE,
    SENDING,
    CONFIRMED,
    FAILED,
    OFFLINE
}

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class Appliance(
    val id: String,
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
    val limitAction: LimitAction,
    val isOnline: Boolean,
    val deviceId: String
)

data class HouseholdStats(
    val totalWatts: Int,
    val todayKwh: Double,
    val weekKwh: Double,
    val monthKwh: Double,
    val monthlyBudgetInr: Double = 3500.0,
    val estimatedMonthlyBillInr: Double = 2369.25,
    val tariffRatePerKwh: Double = 6.50,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class HourlyUsage(
    val hour: Int,
    val label: String,
    val watts: Int,
    val kwh: Double
)

data class DailyUsage(
    val day: String,
    val kwh: Double,
    val costInr: Double
)

data class WeeklyUsage(
    val week: String,
    val kwh: Double,
    val costInr: Double
)

data class VoltoAlert(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val severity: AlertSeverity,
    val isRead: Boolean = false,
    val relatedApplianceId: String? = null,
    val actionText: String? = null
)

data class DeviceControlState(
    val deviceId: String,
    val status: DeviceCommandStatus = DeviceCommandStatus.IDLE,
    val targetState: Boolean = false,
    val message: String? = null
)

enum class HistoryPeriod {
    DAY,
    WEEK,
    MONTH
}
