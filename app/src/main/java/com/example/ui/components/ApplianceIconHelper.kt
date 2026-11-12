package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.ModeFanOff
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.Waves
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

object ApplianceIconHelper {
    fun getIconForAppliance(iconName: String, type: String = ""): ImageVector {
        return when (iconName.lowercase()) {
            "ac", "air_conditioner" -> Icons.Default.AcUnit
            "refrigerator", "fridge" -> Icons.Default.Kitchen
            "washing_machine" -> Icons.Default.LocalLaundryService
            "tv", "television" -> Icons.Default.Tv
            "fan", "ceiling_fan" -> Icons.Default.Air
            "lights", "lamp", "led" -> Icons.Default.Lightbulb
            "water_heater", "geyser" -> Icons.Default.DeviceThermostat
            "water_pump", "pump", "motor" -> Icons.Default.Waves
            "microwave", "oven" -> Icons.Default.Power
            else -> Icons.Default.Bolt
        }
    }
}
