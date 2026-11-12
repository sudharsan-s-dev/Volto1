package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LiveDataSource
import com.example.data.MqttTelemetryPacket
import com.example.ui.theme.VoltoGreenContainer
import com.example.ui.theme.VoltoGreenPrimary
import com.example.ui.theme.VoltoOnGreenContainer
import com.example.ui.theme.VoltoRed
import com.example.ui.theme.VoltoYellow
import com.example.viewmodel.VoltoViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun DeviceSimulatorScreen(
    viewModel: VoltoViewModel,
    modifier: Modifier = Modifier
) {
    var isSimRunning by remember { mutableStateOf(true) }
    var isConnected by remember { mutableStateOf(true) }
    var currentWatts by remember { mutableIntStateOf(1520) }
    var currentVoltage by remember { mutableDoubleStateOf(231.4) }
    var currentAmps by remember { mutableDoubleStateOf(6.9) }
    var totalKwh by remember { mutableDoubleStateOf(48.2) }
    var lastCommandReceived by remember { mutableStateOf("IDLE (Relay Closed: ON)") }

    // Every 5s generate realistic telemetry packet
    LaunchedEffect(isSimRunning, isConnected) {
        while (isSimRunning && isConnected) {
            delay(5000L)
            currentVoltage = 230.0 + Random.nextDouble(-3.5, 4.2)
            currentWatts = (1500 + Random.nextInt(-40, 50))
            currentAmps = (currentWatts / (currentVoltage * 0.95) * 10).toInt() / 10.0
            totalKwh += 0.002
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("device_simulator_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.DeveloperMode, contentDescription = null, tint = VoltoYellow)
                    Text(
                        text = "ESP32 Hardware Simulator",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Mimics physical ESP32 + PZEM-004T v3 UART telemetry node over MQTT",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Connection Status Banner
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isConnected) VoltoGreenContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isConnected) VoltoGreenPrimary else VoltoRed
                        )
                        Column {
                            Text(
                                text = if (isConnected) "ESP32 Connected via MQTT TLS" else "ESP32 Disconnected (LWT Active)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) VoltoOnGreenContainer else VoltoRed
                            )
                            Text(
                                text = "Topic: volto/home-sharma/ESP32-VOLTO-AC/telemetry",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Live JSON Telemetry Payload Viewer
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Live PZEM-004T UART Telemetry (5s Interval)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // JSON console
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Text(
                            text = """
{
  "deviceId": "ESP32-VOLTO-AC",
  "homeId": "home-sharma-blr-01",
  "watts": $currentWatts,
  "voltage": ${(currentVoltage * 10).toInt() / 10.0},
  "current": $currentAmps,
  "frequency": 50.0,
  "powerFactor": 0.95,
  "kwhTotal": ${(totalKwh * 100).toInt() / 100.0},
  "relayOn": true,
  "timestamp": ${System.currentTimeMillis() / 1000}
}
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = VoltoYellow,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        // Simulator Controls
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Fault Injection & Hardware Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isConnected = false
                                viewModel.simulateHardwareDisconnect("ESP32-VOLTO-AC")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VoltoRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("simulate_disconnect_button")
                        ) {
                            Icon(imageVector = Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Drop Wi-Fi (LWT)", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                isConnected = true
                                viewModel.simulateHardwareReconnect("ESP32-VOLTO-AC")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("simulate_reconnect_button")
                        ) {
                            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(" Restore Link", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Command Listener: volto/home-sharma/ESP32-VOLTO-AC/command",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Last Command: $lastCommandReceived",
                                style = MaterialTheme.typography.bodySmall,
                                color = VoltoGreenPrimary
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
