package com.example.data

import com.example.types.AlertSeverity
import com.example.types.Appliance
import com.example.types.HouseholdStats
import com.example.types.HourlyUsage
import com.example.types.LimitAction
import com.example.types.VoltoAlert
import com.example.utils.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class MockDataSource(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : EnergyDataSource {

    // In-memory 30 days history per appliance: applianceId -> List of 720 hourly entries (kWh)
    private val appliance30DayHistory = mutableMapOf<String, List<Double>>()

    private val _appliances = MutableStateFlow<List<Appliance>>(emptyList())
    override val appliancesStream: StateFlow<List<Appliance>> = _appliances.asStateFlow()

    private val _householdStats = MutableStateFlow(
        HouseholdStats(
            totalWatts = 0,
            todayKwh = 12.6,
            weekKwh = 94.2,
            monthKwh = 364.5,
            monthlyBudgetInr = 3500.0,
            estimatedMonthlyBillInr = 2450.0,
            tariffRatePerKwh = 6.50
        )
    )
    override val householdStatsStream: StateFlow<HouseholdStats> = _householdStats.asStateFlow()

    private val _hourlyHistory = MutableStateFlow<List<HourlyUsage>>(emptyList())
    override val hourlyHistoryStream: StateFlow<List<HourlyUsage>> = _hourlyHistory.asStateFlow()

    private val _alerts = MutableStateFlow<List<VoltoAlert>>(emptyList())
    override val alertsStream: StateFlow<List<VoltoAlert>> = _alerts.asStateFlow()

    init {
        initializeData()
        startLiveHeartbeat()
    }

    private fun initializeData() {
        val initialAppliances = listOf(
            Appliance(
                id = "app-ac",
                name = "Living Room Split AC",
                type = "Air Conditioner",
                iconName = "ac",
                isOn = true,
                isCritical = false,
                currentWatts = 1520,
                baseWatts = 1500,
                dailyKwh = 6.8,
                weeklyKwh = 48.2,
                monthlyKwh = 185.0,
                dailyLimitKwh = 8.0,
                limitAction = LimitAction.WARN,
                isOnline = true,
                deviceId = "DEV-VOLTO-AC01"
            ),
            Appliance(
                id = "app-fridge",
                name = "Double Door Refrigerator",
                type = "Refrigerator",
                iconName = "refrigerator",
                isOn = true,
                isCritical = true,
                currentWatts = 155,
                baseWatts = 150,
                dailyKwh = 1.9,
                weeklyKwh = 13.5,
                monthlyKwh = 58.0,
                dailyLimitKwh = 2.5,
                limitAction = LimitAction.NOTIFY,
                isOnline = true,
                deviceId = "DEV-VOLTO-REF02"
            ),
            Appliance(
                id = "app-wm",
                name = "Front Load Washing Machine",
                type = "Washing Machine",
                iconName = "washing_machine",
                isOn = false,
                isCritical = false,
                currentWatts = 0,
                baseWatts = 500,
                dailyKwh = 0.8,
                weeklyKwh = 5.2,
                monthlyKwh = 21.0,
                dailyLimitKwh = 2.0,
                limitAction = LimitAction.NOTIFY,
                isOnline = true,
                deviceId = "DEV-VOLTO-WM03"
            ),
            Appliance(
                id = "app-tv",
                name = "Smart Android 55\" TV",
                type = "Television",
                iconName = "tv",
                isOn = true,
                isCritical = false,
                currentWatts = 118,
                baseWatts = 120,
                dailyKwh = 0.9,
                weeklyKwh = 6.5,
                monthlyKwh = 28.0,
                dailyLimitKwh = 1.5,
                limitAction = LimitAction.NOTIFY,
                isOnline = true,
                deviceId = "DEV-VOLTO-TV04"
            ),
            Appliance(
                id = "app-fan",
                name = "Master Bedroom BLDC Fan",
                type = "Ceiling Fan",
                iconName = "fan",
                isOn = true,
                isCritical = false,
                currentWatts = 38,
                baseWatts = 40,
                dailyKwh = 0.6,
                weeklyKwh = 4.2,
                monthlyKwh = 18.0,
                dailyLimitKwh = 1.0,
                limitAction = LimitAction.WARN,
                isOnline = true,
                deviceId = "DEV-VOLTO-FAN05"
            ),
            Appliance(
                id = "app-lights",
                name = "Living & Kitchen LED Lights",
                type = "Lights",
                iconName = "lights",
                isOn = true,
                isCritical = false,
                currentWatts = 42,
                baseWatts = 45,
                dailyKwh = 0.4,
                weeklyKwh = 2.9,
                monthlyKwh = 12.5,
                dailyLimitKwh = 1.0,
                limitAction = LimitAction.NOTIFY,
                isOnline = true,
                deviceId = "DEV-VOLTO-LGT06"
            ),
            Appliance(
                id = "app-geyser",
                name = "Bathroom Storage Geyser",
                type = "Water Heater",
                iconName = "water_heater",
                isOn = false,
                isCritical = false,
                currentWatts = 0,
                baseWatts = 2000,
                dailyKwh = 2.1,
                weeklyKwh = 14.8,
                monthlyKwh = 62.0,
                dailyLimitKwh = 3.0,
                limitAction = LimitAction.AUTO_OFF,
                isOnline = true,
                deviceId = "DEV-VOLTO-GYS07"
            )
        )

        _appliances.value = initialAppliances

        // Generate 30 days of hourly history for each appliance
        for (app in initialAppliances) {
            appliance30DayHistory[app.id] = generate30DaysHourlyKwh(app)
        }

        // Calculate initial total watts strictly from ON appliances
        recalculateTotals()

        // Generate 24-hour household usage
        _hourlyHistory.value = generate24HourProfile()

        // Initial sample alerts for Indian home
        val now = System.currentTimeMillis()
        _alerts.value = listOf(
            VoltoAlert(
                id = "alt-1",
                title = "Bathroom Geyser Warning",
                message = "Geyser has been active for 45 mins. Turn it off to save units and prevent overheating.",
                timestamp = now - (15 * 60 * 1000L),
                severity = AlertSeverity.WARNING,
                isRead = false,
                relatedApplianceId = "app-geyser",
                actionText = "Turn Off Geyser"
            ),
            VoltoAlert(
                id = "alt-2",
                title = "AC Daily Budget Limit (85%)",
                message = "Living Room Split AC consumed 6.8 kWh today against daily budget of 8.0 kWh.",
                timestamp = now - (60 * 60 * 1000L),
                severity = AlertSeverity.INFO,
                isRead = false,
                relatedApplianceId = "app-ac",
                actionText = "View AC Details"
            ),
            VoltoAlert(
                id = "alt-3",
                title = "Voltage Stabilizer Alert",
                message = "Main line voltage surged to 253V. Inverter stabilizer regulated power safely.",
                timestamp = now - (3 * 3600 * 1000L),
                severity = AlertSeverity.INFO,
                isRead = true
            ),
            VoltoAlert(
                id = "alt-4",
                title = "Critical Device Protected",
                message = "Double Door Refrigerator maintains optimal cooling cycle (155W).",
                timestamp = now - (6 * 3600 * 1000L),
                severity = AlertSeverity.INFO,
                isRead = true,
                relatedApplianceId = "app-fridge"
            )
        )
    }

    private fun startLiveHeartbeat() {
        scope.launch {
            while (isActive) {
                delay(2000L) // 2 seconds tick
                applyLiveFluctuations()
            }
        }
    }

    private fun applyLiveFluctuations() {
        val currentList = _appliances.value
        val updated = currentList.map { app ->
            if (app.isOn && app.isOnline) {
                // Realistic variation: +/- 2% to 4% fluctuation
                val deltaPercent = (Random.nextDouble(-0.035, 0.035))
                val fluctuated = (app.baseWatts * (1.0 + deltaPercent)).toInt().coerceAtLeast(10)
                app.copy(currentWatts = fluctuated)
            } else {
                app.copy(currentWatts = 0)
            }
        }
        _appliances.value = updated
        recalculateTotals()
    }

    private fun recalculateTotals() {
        val currentList = _appliances.value
        // Total watts strictly equals sum of all appliances currently ON
        val sumWatts = currentList.sumOf { if (it.isOn && it.isOnline) it.currentWatts else 0 }

        val todayKwhSum = currentList.sumOf { it.dailyKwh }
        val weekKwhSum = currentList.sumOf { it.weeklyKwh }
        val monthKwhSum = currentList.sumOf { it.monthlyKwh }

        val estimatedBill = Formatters.calculateIndianSlabBill(monthKwhSum)

        val oldStats = _householdStats.value
        _householdStats.value = oldStats.copy(
            totalWatts = sumWatts,
            todayKwh = (todayKwhSum * 10).toInt() / 10.0,
            weekKwh = (weekKwhSum * 10).toInt() / 10.0,
            monthKwh = (monthKwhSum * 10).toInt() / 10.0,
            estimatedMonthlyBillInr = estimatedBill,
            lastUpdated = System.currentTimeMillis()
        )
    }

    override fun getApplianceHourlyHistory(applianceId: String): List<HourlyUsage> {
        val app = _appliances.value.find { it.id == applianceId }
        val history30 = appliance30DayHistory[applianceId] ?: emptyList()
        // Last 24 hours
        val last24 = if (history30.size >= 24) history30.takeLast(24) else history30
        return (0..23).map { index ->
            val kwh = if (index < last24.size) last24[index] else 0.1
            val watts = (kwh * 1000).toInt()
            val label = when (index) {
                0 -> "12 AM"
                6 -> "6 AM"
                12 -> "12 PM"
                18 -> "6 PM"
                23 -> "11 PM"
                else -> "${if (index < 12) index else index - 12} ${if (index < 12) "AM" else "PM"}"
            }
            HourlyUsage(hour = index, label = label, watts = watts, kwh = kwh)
        }
    }

    override fun getApplianceDailyHistory(applianceId: String): List<Double> {
        val history30 = appliance30DayHistory[applianceId] ?: emptyList()
        // Aggregate into 7 days (Mon-Sun)
        val days = mutableListOf<Double>()
        for (d in 0 until 7) {
            val start = (history30.size - (7 - d) * 24).coerceAtLeast(0)
            val end = (start + 24).coerceAtMost(history30.size)
            val sub = if (start < end) history30.subList(start, end) else emptyList()
            val sum = sub.sum()
            days.add((sum * 10).toInt() / 10.0)
        }
        return days
    }

    override fun getApplianceWeeklyHistory(applianceId: String): List<Double> {
        val history30 = appliance30DayHistory[applianceId] ?: emptyList()
        // 4 weeks of the month
        val weeks = mutableListOf<Double>()
        for (w in 0 until 4) {
            val start = w * 7 * 24
            val end = ((w + 1) * 7 * 24).coerceAtMost(history30.size)
            val sub = if (start < end) history30.subList(start, end) else emptyList()
            val sum = sub.sum()
            weeks.add((sum * 10).toInt() / 10.0)
        }
        return weeks
    }

    override suspend fun setApplianceState(applianceId: String, newState: Boolean) {
        val currentList = _appliances.value
        val updated = currentList.map { app ->
            if (app.id == applianceId) {
                val newWatts = if (newState) app.baseWatts else 0
                app.copy(isOn = newState, currentWatts = newWatts)
            } else {
                app
            }
        }
        _appliances.value = updated
        // When an appliance turns OFF, its watts become 0 and the household total updates immediately
        recalculateTotals()
    }

    override fun setApplianceLimit(applianceId: String, dailyLimitKwh: Double, action: LimitAction) {
        _appliances.value = _appliances.value.map { app ->
            if (app.id == applianceId) {
                app.copy(dailyLimitKwh = dailyLimitKwh, limitAction = action)
            } else {
                app
            }
        }
    }

    override fun addAppliance(
        name: String,
        type: String,
        deviceId: String,
        isCritical: Boolean,
        dailyLimitKwh: Double,
        limitAction: LimitAction,
        baseWatts: Int
    ) {
        val newId = "app-${System.currentTimeMillis()}"
        val iconName = when (type.lowercase()) {
            "air conditioner", "ac" -> "ac"
            "refrigerator", "fridge" -> "refrigerator"
            "washing machine" -> "washing_machine"
            "television", "tv" -> "tv"
            "ceiling fan", "fan" -> "fan"
            "lights", "lamp" -> "lights"
            "water heater", "geyser" -> "water_heater"
            "microwave" -> "microwave"
            "water pump", "motor" -> "water_pump"
            else -> "generic"
        }

        val newApp = Appliance(
            id = newId,
            name = name,
            type = type,
            iconName = iconName,
            isOn = false,
            isCritical = isCritical,
            currentWatts = 0,
            baseWatts = baseWatts,
            dailyKwh = 0.0,
            weeklyKwh = 0.0,
            monthlyKwh = 0.0,
            dailyLimitKwh = dailyLimitKwh,
            limitAction = limitAction,
            isOnline = true,
            deviceId = deviceId
        )

        appliance30DayHistory[newId] = generate30DaysHourlyKwh(newApp)
        _appliances.value = _appliances.value + newApp
    }

    override fun markAlertAsRead(alertId: String) {
        _alerts.value = _alerts.value.map { alert ->
            if (alert.id == alertId) alert.copy(isRead = true) else alert
        }
    }

    override fun markAllAlertsAsRead() {
        _alerts.value = _alerts.value.map { it.copy(isRead = true) }
    }

    override fun refresh() {
        applyLiveFluctuations()
    }

    private fun generate24HourProfile(): List<HourlyUsage> {
        // Typical Indian household power curve:
        // Morning spike (6 AM - 9 AM) for geyser, lights, breakfast cooking
        // Mid-day moderate baseline
        // Evening peak (6 PM - 11 PM) for AC, TV, dinner lighting, cooking
        // Night baseline (fans, fridge, AC in bedroom)
        return (0..23).map { hour ->
            val baseHourWatts = when (hour) {
                in 0..5 -> Random.nextInt(320, 680)   // Night: fridge + fans + night lamp
                in 6..9 -> Random.nextInt(2100, 3100) // Morning peak: Geyser + pump + kitchen
                in 10..13 -> Random.nextInt(900, 1400) // Late morning: fridge + AC/fans
                in 14..17 -> Random.nextInt(1200, 1850) // Afternoon: Living room AC
                in 18..22 -> Random.nextInt(2200, 3400) // Evening prime: Lights + TV + AC + fans
                else -> Random.nextInt(850, 1400)      // Late night: AC winding down
            }
            val kwh = (baseHourWatts / 1000.0) * (0.85 + Random.nextDouble(0.0, 0.3))
            val label = when (hour) {
                0 -> "12 AM"
                6 -> "6 AM"
                12 -> "12 PM"
                18 -> "6 PM"
                23 -> "11 PM"
                else -> "${if (hour < 12) hour else hour - 12} ${if (hour < 12) "AM" else "PM"}"
            }
            HourlyUsage(
                hour = hour,
                label = label,
                watts = baseHourWatts,
                kwh = (kwh * 100).toInt() / 100.0
            )
        }
    }

    private fun generate30DaysHourlyKwh(appliance: Appliance): List<Double> {
        val totalHours = 30 * 24
        val result = ArrayList<Double>(totalHours)
        val nominalKw = appliance.baseWatts / 1000.0

        for (h in 0 until totalHours) {
            val hourOfDay = h % 24
            val probabilityOn = when (appliance.iconName) {
                "refrigerator" -> 0.70 // Always cycling
                "water_heater" -> if (hourOfDay in 6..8 || hourOfDay in 19..20) 0.60 else 0.05
                "ac" -> if (hourOfDay in 13..16 || hourOfDay in 21..24 || hourOfDay in 0..5) 0.80 else 0.15
                "tv" -> if (hourOfDay in 18..23 || hourOfDay in 12..14) 0.75 else 0.10
                "fan" -> if (hourOfDay in 11..17 || hourOfDay in 22..24 || hourOfDay in 0..7) 0.90 else 0.40
                "lights" -> if (hourOfDay in 18..23 || hourOfDay in 5..6) 0.85 else 0.05
                "washing_machine" -> if (hourOfDay in 8..11) 0.35 else 0.02
                else -> 0.30
            }

            val isRunning = Random.nextDouble() < probabilityOn
            val kwh = if (isRunning) {
                nominalKw * (0.8 + Random.nextDouble(0.0, 0.4))
            } else {
                0.0
            }
            result.add((kwh * 100).toInt() / 100.0)
        }
        return result
    }
}
