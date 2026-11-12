package com.example.services

import com.example.types.Appliance
import com.example.types.HouseholdStats
import com.example.utils.Formatters
import kotlinx.coroutines.delay

data class BillPrediction(
    val currentMonthKwh: Double,
    val projectedMonthKwh: Double,
    val projectedBillInr: Double,
    val budgetInr: Double,
    val varianceInr: Double,
    val isOverBudget: Boolean,
    val savingsPotentialInr: Double
)

data class AiRecommendation(
    val id: String,
    val title: String,
    val explanation: String,
    val estimatedMonthlySavingsInr: Double,
    val applianceId: String? = null,
    val actionLabel: String? = null
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class RecommendationService {

    /**
     * Calculates bill prediction based on current burn rate and seasonal demand.
     */
    fun predictMonthlyBill(stats: HouseholdStats, dayOfMonth: Int = 18): BillPrediction {
        val daysInMonth = 30
        val effectiveDay = dayOfMonth.coerceIn(1, daysInMonth)
        val dailyAverageKwh = stats.monthKwh / effectiveDay.toDouble()
        val projectedKwh = (dailyAverageKwh * daysInMonth * 10).toInt() / 10.0
        val projectedBill = Formatters.calculateIndianSlabBill(projectedKwh)
        val variance = projectedBill - stats.monthlyBudgetInr
        val savings = if (projectedBill > stats.monthlyBudgetInr) (projectedBill - stats.monthlyBudgetInr) * 0.75 else 450.0

        return BillPrediction(
            currentMonthKwh = stats.monthKwh,
            projectedMonthKwh = projectedKwh,
            projectedBillInr = projectedBill,
            budgetInr = stats.monthlyBudgetInr,
            varianceInr = variance,
            isOverBudget = variance > 0,
            savingsPotentialInr = savings
        )
    }

    /**
     * Generates structured energy-saving insights.
     */
    fun generateRecommendations(appliances: List<Appliance>, stats: HouseholdStats): List<AiRecommendation> {
        val list = mutableListOf<AiRecommendation>()

        val ac = appliances.find { it.type.contains("Air", ignoreCase = true) || it.iconName == "ac" }
        if (ac != null) {
            list.add(
                AiRecommendation(
                    id = "rec-ac-temp",
                    title = "Set AC to 24°C Recommendation",
                    explanation = "Increasing your Living Room AC from 20°C to 24°C saves up to 24% of AC electricity (approx 1.6 kWh/day).",
                    estimatedMonthlySavingsInr = 340.0,
                    applianceId = ac.id,
                    actionLabel = "Adjust AC Limit"
                )
            )
        }

        val geyser = appliances.find { it.type.contains("Heater", ignoreCase = true) || it.iconName == "water_heater" }
        if (geyser != null) {
            list.add(
                AiRecommendation(
                    id = "rec-geyser-timer",
                    title = "Install 20-Minute Geyser Timer",
                    explanation = "Storage water heaters heat up in 15 minutes. Leaving them switched on wastes ~1.8 units daily on thermal dissipation.",
                    estimatedMonthlySavingsInr = 280.0,
                    applianceId = geyser.id,
                    actionLabel = "Set Geyser Limit"
                )
            )
        }

        list.add(
            AiRecommendation(
                id = "rec-vampire-loads",
                title = "Cut Phantom Standby Power",
                explanation = "Appliances left plugged in on wall switches (TV set-top box, microwave clock, chargers) continuously draw 35-50W phantom power.",
                estimatedMonthlySavingsInr = 145.0,
                actionLabel = "Review Standby Appliances"
            )
        )

        list.add(
            AiRecommendation(
                id = "rec-bldc-fans",
                title = "Upgrade to 5-Star BLDC Fan",
                explanation = "BLDC motors use 28W instead of 75W on standard ceiling fans, cutting fan electricity by 60%.",
                estimatedMonthlySavingsInr = 190.0
            )
        )

        return list
    }

    /**
     * Interactive conversational assistant queries ("Ask VOLTO").
     */
    suspend fun answerEnergyQuery(query: String, stats: HouseholdStats, appliances: List<Appliance>): String {
        delay(900) // Simulated AI thinking
        val q = query.lowercase()

        return when {
            q.contains("ac") || q.contains("air condition") || q.contains("cool") -> {
                "Air conditioning accounts for roughly 45–55% of summer electricity in Indian homes. For your '${appliances.find { it.iconName == "ac" }?.name ?: "Split AC"}', running at 24°C instead of 18°C saves ~₹350 each month! Also, clean the indoor filters every 2 weeks to maintain optimal airflow."
            }
            q.contains("bill") || q.contains("save") || q.contains("reduce") -> {
                "Your current estimated bill is ${Formatters.formatInr(stats.estimatedMonthlyBillInr)} against your ${Formatters.formatInr(stats.monthlyBudgetInr)} budget. To stay safely in the lower tariff slab, set daily limits on your AC and geyser, and switch off appliances from the wall switch to prevent vampire standby draw."
            }
            q.contains("geyser") || q.contains("water heater") || q.contains("bath") -> {
                "Storage geysers are high-power devices drawing ~2,000 Watts! A 15–20 minute heating cycle is more than enough for a hot bath. Never keep the geyser switch ON throughout the morning."
            }
            q.contains("fridge") || q.contains("refrigerator") -> {
                "Your Double Door Refrigerator runs 24x7 with ~150W cycling and is marked as a Critical Device. Keep the condenser coils clean, check that the magnetic door gasket seals tightly, and avoid putting hot food directly into the fridge."
            }
            q.contains("tariff") || q.contains("slab") || q.contains("rate") -> {
                "State DISCOMs (like BESCOM and TANGEDCO) use telescoping slab rates. Units up to 100 are charged around ₹4.50, units from 101–200 at ₹6.00, and units over 200 at ₹7.50+. Keeping your home's total monthly units under 250 prevents entering the highest surcharge bracket."
            }
            else -> {
                "Namaste! I'm your VOLTO Energy Advisor. I analyze your home's live telemetry and tariff slabs. You can ask me how to reduce your electricity bill, optimize your AC or geyser, or set smart auto cut-off limits."
            }
        }
    }
}
