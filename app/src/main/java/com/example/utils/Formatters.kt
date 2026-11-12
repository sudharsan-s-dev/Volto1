package com.example.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object Formatters {
    private val inrFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    private val inrFormatDecimal: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    private val decimalFormatOne = DecimalFormat("#,##0.0")
    private val decimalFormatTwo = DecimalFormat("#,##0.00")
    private val integerFormat = DecimalFormat("#,###")

    fun formatInr(amount: Double, includeDecimals: Boolean = false): String {
        return try {
            if (includeDecimals) {
                inrFormatDecimal.format(amount)
            } else {
                inrFormat.format(amount)
            }
        } catch (_: Exception) {
            "₹" + (if (includeDecimals) decimalFormatTwo.format(amount) else integerFormat.format(amount))
        }
    }

    fun formatWatts(watts: Int): String {
        return if (watts >= 1000) {
            "${decimalFormatTwo.format(watts / 1000.0)} kW"
        } else {
            "${integerFormat.format(watts)} W"
        }
    }

    fun formatRawWatts(watts: Int): String {
        return integerFormat.format(watts)
    }

    fun formatKwh(kwh: Double): String {
        return "${decimalFormatOne.format(kwh)} kWh"
    }

    fun formatUnits(kwh: Double): String {
        return "${decimalFormatOne.format(kwh)} units"
    }

    /**
     * Calculates slab tariff typical for Indian state electricity boards (e.g. BESCOM/MSEDCL/TANGEDCO)
     */
    fun calculateIndianSlabBill(units: Double): Double {
        val fixedCharges = 120.0
        var remainingUnits = units
        var energyCharges = 0.0

        // Slab 1: 0 - 100 units @ ₹4.50
        val slab1 = minOf(remainingUnits, 100.0)
        energyCharges += slab1 * 4.50
        remainingUnits -= slab1

        if (remainingUnits > 0) {
            // Slab 2: 101 - 200 units @ ₹6.00
            val slab2 = minOf(remainingUnits, 100.0)
            energyCharges += slab2 * 6.00
            remainingUnits -= slab2
        }

        if (remainingUnits > 0) {
            // Slab 3: 201 - 300 units @ ₹7.50
            val slab3 = minOf(remainingUnits, 100.0)
            energyCharges += slab3 * 7.50
            remainingUnits -= slab3
        }

        if (remainingUnits > 0) {
            // Slab 4: > 300 units @ ₹8.50
            energyCharges += remainingUnits * 8.50
        }

        val electricityDuty = energyCharges * 0.09 // 9% electricity tax
        return energyCharges + fixedCharges + electricityDuty
    }
}
