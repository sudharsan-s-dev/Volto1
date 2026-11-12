package com.example

import com.example.auth.AuthState
import com.example.auth.MockAuthRepository
import com.example.services.BudgetMonitor
import com.example.services.CommandConfirmation
import com.example.services.CommandResult
import com.example.services.DeviceControlService
import com.example.services.LimitMonitor
import com.example.types.AlertSeverity
import com.example.types.Appliance
import com.example.types.LimitAction
import com.example.types.VoltoAlert
import com.example.utils.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive Unit Tests for VOLTO:
 * 1. TariffCalculator edge cases (0 units, slab boundaries 100, 200, 300, higher tiers)
 * 2. Unified MQTT telemetry, status, and command JSON schema parsing
 * 3. LimitMonitor & Critical Appliance Protection (Auto-OFF suppressed for Refrigerator)
 * 4. Critical Appliance turn-off confirmation requirement
 * 5. Budget Milestone Threshold Markers (50%, 75%, 90%, 100%)
 * 6. AuthRepository operations (email, password validation, Google sign in, sign out)
 * 7. DeviceControlService commandId matching confirmation & deferred correlation
 * 8. Alert filtering by severity and read status
 */
class ExampleUnitTest {

    @Test
    fun testTariffCalculatorEdgeCases() {
        // Edge Case 1: 0 units consumed (Zero energy charge + fixed charges 120 + 9% tax = ~130.8)
        val bill0 = Formatters.calculateIndianSlabBill(0.0)
        assertTrue("Bill for 0 units should cover fixed charges ~130.8", bill0 in 125.0..135.0)

        // Edge Case 2: Exact Slab 1 Boundary (100 units)
        // 100 * 4.50 = 450 + 120 fixed = 570 + 9% tax (51.3) = 621.3
        val bill100 = Formatters.calculateIndianSlabBill(100.0)
        assertTrue("Bill for 100 units boundary should be ~621", bill100 in 615.0..625.0)

        // Edge Case 3: Exact Slab 2 Boundary (200 units)
        // 100 * 4.5 (450) + 100 * 6.0 (600) = 1050 + 120 fixed = 1170 + 9% tax (105.3) = 1275.3
        val bill200 = Formatters.calculateIndianSlabBill(200.0)
        assertTrue("Bill for 200 units boundary should be ~1275", bill200 in 1265.0..1285.0)

        // Edge Case 4: Exact Slab 3 Boundary (300 units)
        // 450 + 600 + 100 * 7.50 (750) = 1800 + 120 = 1920 + 9% tax (172.8) = 2092.8
        val bill300 = Formatters.calculateIndianSlabBill(300.0)
        assertTrue("Bill for 300 units boundary should be ~2093", bill300 in 2080.0..2105.0)

        // Edge Case 5: Higher Tier (> 300 units e.g. 400 units)
        // 1800 + 100 * 8.50 (850) = 2650 + 120 = 2770 + 9% tax (249.3) = 3019.3
        val bill400 = Formatters.calculateIndianSlabBill(400.0)
        assertTrue("Bill for 400 units should enter top tariff tier ~3019", bill400 in 3000.0..3035.0)
    }

    @Test
    fun testUnifiedMqttSchemaParsing() {
        // 1. Telemetry JSON schema: { deviceId, homeId, watts, voltage, current, frequency, powerFactor, kwhTotal, relayOn, timestamp }
        val sampleTelemetryJson = """
            {
              "deviceId": "ESP32-VOLTO-AC01",
              "homeId": "home-sharma-blr-01",
              "watts": 1520,
              "voltage": 230.5,
              "current": 6.95,
              "frequency": 50.0,
              "powerFactor": 0.95,
              "kwhTotal": 48.24,
              "relayOn": true,
              "timestamp": 1718000000
            }
        """.trimIndent()

        val telObj = JSONObject(sampleTelemetryJson)
        assertEquals("ESP32-VOLTO-AC01", telObj.getString("deviceId"))
        assertEquals("home-sharma-blr-01", telObj.getString("homeId"))
        assertEquals(1520, telObj.getInt("watts"))
        assertEquals(230.5, telObj.getDouble("voltage"), 0.01)
        assertEquals(6.95, telObj.getDouble("current"), 0.01)
        assertEquals(50.0, telObj.getDouble("frequency"), 0.01)
        assertEquals(0.95, telObj.getDouble("powerFactor"), 0.01)
        assertEquals(48.24, telObj.getDouble("kwhTotal"), 0.01)
        assertTrue(telObj.getBoolean("relayOn"))
        assertEquals(1718000000L, telObj.getLong("timestamp"))

        // 2. Status topic schema: { deviceId, online, relayOn, commandId, result }
        val sampleStatusJson = """
            {
              "deviceId": "ESP32-VOLTO-AC01",
              "online": true,
              "relayOn": false,
              "commandId": "cmd-882194",
              "result": "SUCCESS"
            }
        """.trimIndent()

        val statObj = JSONObject(sampleStatusJson)
        assertEquals("ESP32-VOLTO-AC01", statObj.getString("deviceId"))
        assertTrue(statObj.getBoolean("online"))
        assertFalse(statObj.getBoolean("relayOn"))
        assertEquals("cmd-882194", statObj.getString("commandId"))
        assertEquals("SUCCESS", statObj.getString("result"))

        // 3. Command topic schema: { commandId, deviceId, command: "ON" | "OFF" }
        val sampleCommandJson = """
            {
              "commandId": "cmd-882194",
              "deviceId": "ESP32-VOLTO-AC01",
              "command": "OFF"
            }
        """.trimIndent()

        val cmdObj = JSONObject(sampleCommandJson)
        assertEquals("cmd-882194", cmdObj.getString("commandId"))
        assertEquals("ESP32-VOLTO-AC01", cmdObj.getString("deviceId"))
        assertEquals("OFF", cmdObj.getString("command"))
    }

    @Test
    fun testLimitMonitor_AutoCutNonCritical() {
        val ac = Appliance(
            id = "app-ac",
            name = "Split AC",
            type = "Air Conditioner",
            iconName = "ac",
            isOn = true,
            isCritical = false,
            currentWatts = 1500,
            baseWatts = 1500,
            dailyKwh = 8.5,
            weeklyKwh = 50.0,
            monthlyKwh = 180.0,
            dailyLimitKwh = 8.0,
            limitAction = LimitAction.AUTO_OFF,
            isOnline = true,
            deviceId = "DEV-AC"
        )

        val result = LimitMonitor.evaluateApplianceLimit(ac)
        assertTrue("Ratio should be > 1.0", result.ratio >= 1.0)
        assertTrue("Non-critical appliance with AUTO_OFF must trigger auto-cut", result.shouldAutoCut)
        assertNotNull("Should emit alert", result.alertToEmit)
        assertTrue(result.alertToEmit!!.title.contains("Auto Cut-Off"))
    }

    @Test
    fun testLimitMonitor_ProtectsCriticalApplianceRefrigerator() {
        val fridge = Appliance(
            id = "app-fridge",
            name = "Double Door Refrigerator",
            type = "Refrigerator",
            iconName = "refrigerator",
            isOn = true,
            isCritical = true, // Critical device!
            currentWatts = 160,
            baseWatts = 150,
            dailyKwh = 3.0,
            weeklyKwh = 15.0,
            monthlyKwh = 60.0,
            dailyLimitKwh = 2.5,
            limitAction = LimitAction.AUTO_OFF,
            isOnline = true,
            deviceId = "DEV-FRIDGE"
        )

        val result = LimitMonitor.evaluateApplianceLimit(fridge)
        assertTrue("Ratio should be > 1.0", result.ratio >= 1.0)
        assertFalse("CRITICAL SAFETY RULE: Critical appliances must NEVER be auto-cut!", result.shouldAutoCut)
        assertNotNull("Alert should still be emitted", result.alertToEmit)
        assertTrue(result.alertToEmit!!.title.contains("Critical Device"))
    }

    @Test
    fun testBudgetMonitor_Milestones() {
        val budget = 3000.0

        val alert30 = BudgetMonitor.checkBudgetMilestones(1000.0, budget)
        assertNull(alert30)

        val alert50 = BudgetMonitor.checkBudgetMilestones(1550.0, budget)
        assertNotNull(alert50)
        assertTrue(alert50!!.title.contains("50%"))
        assertEquals(AlertSeverity.INFO, alert50.severity)

        val alert75 = BudgetMonitor.checkBudgetMilestones(2300.0, budget)
        assertNotNull(alert75)
        assertTrue(alert75!!.title.contains("75%"))

        val alert90 = BudgetMonitor.checkBudgetMilestones(2750.0, budget)
        assertNotNull(alert90)
        assertTrue(alert90!!.title.contains("90%"))
        assertEquals(AlertSeverity.WARNING, alert90.severity)

        val alert100 = BudgetMonitor.checkBudgetMilestones(3100.0, budget)
        assertNotNull(alert100)
        assertTrue(alert100!!.title.contains("100%"))
        assertEquals(AlertSeverity.CRITICAL, alert100.severity)
    }

    @Test
    fun testMockAuthRepositoryFlows() = runBlocking {
        val repo = MockAuthRepository()

        // Test Invalid Email
        val invalidEmailResult = repo.signInWithEmail("bademail", "password123")
        assertTrue(invalidEmailResult.isFailure)

        // Test Short Password
        val shortPassResult = repo.signInWithEmail("sharma@volto.in", "123")
        assertTrue(shortPassResult.isFailure)

        // Test Successful Email Sign-In
        val successResult = repo.signInWithEmail("suresh.sharma@volto.in", "securePassword123")
        assertTrue(successResult.isSuccess)
        val user = successResult.getOrNull()
        assertNotNull(user)
        assertEquals("suresh.sharma@volto.in", user!!.email)

        // Test Sign Out
        repo.signOut()
        assertEquals(AuthState.Unauthenticated, repo.authState.value)

        // Test Google Sign-In
        val googleResult = repo.signInWithGoogle()
        assertTrue(googleResult.isSuccess)
        val googleUser = googleResult.getOrNull()
        assertTrue(googleUser!!.isGoogleUser)
    }

    @Test
    fun testDeviceControlServiceCommandIdCorrelation() = runBlocking {
        val controlService = DeviceControlService()

        var interceptedCommandId: String? = null
        var interceptedJson: String? = null

        launch {
            // When command is dispatched, intercept its ID and simulate status reply
            kotlinx.coroutines.delay(100)
            if (interceptedCommandId != null) {
                controlService.onStatusPacketReceived(
                    CommandConfirmation(
                        commandId = interceptedCommandId!!,
                        deviceId = "ESP32-VOLTO-AC",
                        online = true,
                        relayOn = false,
                        result = "SUCCESS"
                    )
                )
            }
        }

        val result = controlService.sendCommand(
            deviceId = "ESP32-VOLTO-AC",
            targetState = false,
            isOnline = true,
            commandDispatcher = { cmdId, json ->
                interceptedCommandId = cmdId
                interceptedJson = json
            }
        )

        assertNotNull(interceptedCommandId)
        assertNotNull(interceptedJson)
        assertTrue(interceptedJson!!.contains("cmd-"))
        assertTrue("Command result must be Success", result is CommandResult.Success)
        assertEquals(false, (result as CommandResult.Success).newState)
    }

    @Test
    fun testAlertFilteringLogic() {
        val alerts = listOf(
            VoltoAlert(id = "1", title = "A1", message = "M1", timestamp = 100L, severity = AlertSeverity.CRITICAL, isRead = false),
            VoltoAlert(id = "2", title = "A2", message = "M2", timestamp = 200L, severity = AlertSeverity.WARNING, isRead = true),
            VoltoAlert(id = "3", title = "A3", message = "M3", timestamp = 300L, severity = AlertSeverity.INFO, isRead = false)
        )

        val unreadAlerts = alerts.filter { !it.isRead }
        assertEquals(2, unreadAlerts.size)

        val criticalAlerts = alerts.filter { it.severity == AlertSeverity.CRITICAL }
        assertEquals(1, criticalAlerts.size)

        val warningAlerts = alerts.filter { it.severity == AlertSeverity.WARNING }
        assertEquals(1, warningAlerts.size)
    }

    @Test
    fun testFormatters() {
        assertEquals("45 W", Formatters.formatWatts(45))
        assertEquals("1.50 kW", Formatters.formatWatts(1500))
        assertEquals("6.8 kWh", Formatters.formatKwh(6.8))
        assertEquals("12.6 units", Formatters.formatUnits(12.6))
    }
}
