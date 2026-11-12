package com.example.data

import com.example.types.AlertSeverity
import com.example.types.Appliance
import com.example.types.HistoryPeriod
import com.example.types.HouseholdStats
import com.example.types.HourlyUsage
import com.example.types.LimitAction
import com.example.types.VoltoAlert
import kotlinx.coroutines.flow.StateFlow

interface EnergyDataSource {
    val appliancesStream: StateFlow<List<Appliance>>
    val householdStatsStream: StateFlow<HouseholdStats>
    val hourlyHistoryStream: StateFlow<List<HourlyUsage>>
    val alertsStream: StateFlow<List<VoltoAlert>>

    fun getApplianceHourlyHistory(applianceId: String): List<HourlyUsage>
    fun getApplianceDailyHistory(applianceId: String): List<Double>
    fun getApplianceWeeklyHistory(applianceId: String): List<Double>

    suspend fun setApplianceState(applianceId: String, newState: Boolean)
    fun setApplianceLimit(applianceId: String, dailyLimitKwh: Double, action: LimitAction)
    fun addAppliance(
        name: String,
        type: String,
        deviceId: String,
        isCritical: Boolean,
        dailyLimitKwh: Double,
        limitAction: LimitAction,
        baseWatts: Int
    )
    fun markAlertAsRead(alertId: String)
    fun markAllAlertsAsRead()
    fun refresh()
}
