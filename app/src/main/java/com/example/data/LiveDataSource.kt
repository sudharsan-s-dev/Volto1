package com.example.data

import com.example.types.AlertSeverity
import com.example.types.Appliance
import com.example.types.HistoryPeriod
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

data class MqttTelemetryPacket(
    val deviceId: String,
    val homeId: String,
    val watts: Int,
    val voltage: Double,
    val current: Double,
    val frequency: Double,
    val powerFactor: Double,
    val kwhTotal: Double,
    val relayOn: Boolean,
    val timestamp: Long
)

data class MqttStatusPacket(
    val deviceId: String,
    val online: Boolean,
    val relayOn: Boolean,
    val commandId: String,
    val result: String
)

data class MqttCommandPacket(
    val commandId: String,
    val deviceId: String,
    val command: String // "ON" or "OFF"
)

enum class LiveConnectionState {
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED,
    ERROR
}

class LiveDataSource(
    private val brokerUrl: String = "ssl://mqtt.volto.in",
    private val port: Int = 8883,
    private val homeId: String = "home-sharma-blr-01",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : EnergyDataSource {

    private val _connectionState = MutableStateFlow(LiveConnectionState.CONNECTED)
    val connectionState: StateFlow<LiveConnectionState> = _connectionState.asStateFlow()

    private val _appliances = MutableStateFlow<List<Appliance>>(emptyList())
    override val appliancesStream: StateFlow<List<Appliance>> = _appliances.asStateFlow()

    private val _householdStats = MutableStateFlow(HouseholdStats(0, 0.0, 0.0, 0.0))
    override val householdStatsStream: StateFlow<HouseholdStats> = _householdStats.asStateFlow()

    private val _hourlyHistory = MutableStateFlow<List<HourlyUsage>>(emptyList())
    override val hourlyHistoryStream: StateFlow<List<HourlyUsage>> = _hourlyHistory.asStateFlow()

    private val _alerts = MutableStateFlow<List<VoltoAlert>>(emptyList())
    override val alertsStream: StateFlow<List<VoltoAlert>> = _alerts.asStateFlow()

    // 5-second telemetry loop simulating real MQTT subscription
    init {
        initializeLiveAppliances()
        startTelemetryLoop()
    }

    private fun initializeLiveAppliances() {
        _appliances.value = listOf(
            Appliance(
                id = "live-ac",
                name = "Living Room AC (ESP32 Node 1)",
                type = "Air Conditioner",
                iconName = "ac",
                isOn = true,
                isCritical = false,
                currentWatts = 1530,
                baseWatts = 1500,
                dailyKwh = 6.4,
                weeklyKwh = 44.0,
                monthlyKwh = 175.0,
                dailyLimitKwh = 8.0,
                limitAction = LimitAction.WARN,
                isOnline = true,
                deviceId = "ESP32-VOLTO-AC"
            ),
            Appliance(
                id = "live-fridge",
                name = "Smart Fridge (ESP32 Node 2)",
                type = "Refrigerator",
                iconName = "refrigerator",
                isOn = true,
                isCritical = true,
                currentWatts = 158,
                baseWatts = 150,
                dailyKwh = 1.8,
                weeklyKwh = 12.6,
                monthlyKwh = 55.0,
                dailyLimitKwh = 2.5,
                limitAction = LimitAction.NOTIFY,
                isOnline = true,
                deviceId = "ESP32-VOLTO-REF"
            ),
            Appliance(
                id = "live-geyser",
                name = "Geyser Node (PZEM-004T)",
                type = "Water Heater",
                iconName = "water_heater",
                isOn = false,
                isCritical = false,
                currentWatts = 0,
                baseWatts = 2000,
                dailyKwh = 2.2,
                weeklyKwh = 15.0,
                monthlyKwh = 60.0,
                dailyLimitKwh = 3.0,
                limitAction = LimitAction.AUTO_OFF,
                isOnline = true,
                deviceId = "ESP32-VOLTO-GYS"
            )
        )
        recomputeTotals()
    }

    private fun startTelemetryLoop() {
        scope.launch {
            while (isActive) {
                delay(5000L) // Telemetry published every 5 seconds per PZEM-004T specification
                if (_connectionState.value == LiveConnectionState.CONNECTED) {
                    processIncomingMqttTelemetry()
                }
            }
        }
    }

    private fun processIncomingMqttTelemetry() {
        val currentList = _appliances.value
        val updated = currentList.map { app ->
            if (app.isOn && app.isOnline) {
                // Real Indian mains voltage variation 226V - 238V
                val voltage = 230.0 + Random.nextDouble(-4.0, 6.0)
                val powerFactor = 0.94
                val watts = (app.baseWatts * (1.0 + Random.nextDouble(-0.02, 0.03))).toInt()
                val currentAmps = watts / (voltage * powerFactor)
                app.copy(currentWatts = watts)
            } else {
                app.copy(currentWatts = 0)
            }
        }
        _appliances.value = updated
        recomputeTotals()
    }

    fun injectTelemetryPacket(packet: MqttTelemetryPacket) {
        val currentList = _appliances.value
        val updated = currentList.map { app ->
            if (app.deviceId == packet.deviceId) {
                app.copy(
                    currentWatts = packet.watts,
                    isOn = packet.relayOn && packet.watts > 10,
                    isOnline = true
                )
            } else app
        }
        _appliances.value = updated
        recomputeTotals()
    }

    fun simulateDeviceDrop(deviceId: String) {
        _appliances.value = _appliances.value.map { app ->
            if (app.deviceId == deviceId) {
                app.copy(isOnline = false, currentWatts = 0)
            } else app
        }
        recomputeTotals()

        // Emit MQTT Last-Will alert
        val app = _appliances.value.find { it.deviceId == deviceId }
        val dropAlert = VoltoAlert(
            id = "alt-drop-${System.currentTimeMillis()}",
            title = "Hardware Disconnected (LWT)",
            message = "${app?.name ?: deviceId} disconnected from MQTT broker. Checking Wi-Fi link.",
            timestamp = System.currentTimeMillis(),
            severity = AlertSeverity.WARNING,
            relatedApplianceId = app?.id
        )
        _alerts.value = listOf(dropAlert) + _alerts.value
    }

    fun simulateDeviceReconnect(deviceId: String) {
        _appliances.value = _appliances.value.map { app ->
            if (app.deviceId == deviceId) {
                app.copy(isOnline = true)
            } else app
        }
        recomputeTotals()
    }

    private fun recomputeTotals() {
        val list = _appliances.value
        val totalWatts = list.sumOf { if (it.isOn && it.isOnline) it.currentWatts else 0 }
        val todayKwh = (list.sumOf { it.dailyKwh } * 10).toInt() / 10.0
        val weekKwh = (list.sumOf { it.weeklyKwh } * 10).toInt() / 10.0
        val monthKwh = (list.sumOf { it.monthlyKwh } * 10).toInt() / 10.0

        _householdStats.value = HouseholdStats(
            totalWatts = totalWatts,
            todayKwh = todayKwh,
            weekKwh = weekKwh,
            monthKwh = monthKwh,
            estimatedMonthlyBillInr = Formatters.calculateIndianSlabBill(monthKwh)
        )
    }

    override fun getApplianceHourlyHistory(applianceId: String): List<HourlyUsage> = emptyList()
    override fun getApplianceDailyHistory(applianceId: String): List<Double> = listOf(5.0, 6.2, 4.8, 7.1, 6.4, 5.9, 6.4)
    override fun getApplianceWeeklyHistory(applianceId: String): List<Double> = listOf(38.0, 42.0, 45.0, 44.0)

    override suspend fun setApplianceState(applianceId: String, newState: Boolean) {
        _appliances.value = _appliances.value.map { app ->
            if (app.id == applianceId) {
                app.copy(isOn = newState, currentWatts = if (newState) app.baseWatts else 0)
            } else app
        }
        recomputeTotals()
    }

    override fun setApplianceLimit(applianceId: String, dailyLimitKwh: Double, action: LimitAction) {
        _appliances.value = _appliances.value.map { app ->
            if (app.id == applianceId) app.copy(dailyLimitKwh = dailyLimitKwh, limitAction = action) else app
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
    ) {}

    override fun markAlertAsRead(alertId: String) {
        _alerts.value = _alerts.value.map { if (it.id == alertId) it.copy(isRead = true) else it }
    }

    override fun markAllAlertsAsRead() {
        _alerts.value = _alerts.value.map { it.copy(isRead = true) }
    }

    override fun refresh() {
        processIncomingMqttTelemetry()
    }
}
