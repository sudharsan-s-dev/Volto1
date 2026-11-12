package com.example.services

import com.example.types.AlertSeverity
import com.example.types.Appliance
import com.example.types.LimitAction
import com.example.types.VoltoAlert
import kotlin.random.Random

data class LimitCheckResult(
    val applianceId: String,
    val applianceName: String,
    val currentKwh: Double,
    val limitKwh: Double,
    val ratio: Double,
    val shouldAutoCut: Boolean,
    val alertToEmit: VoltoAlert?
)

object LimitMonitor {

    /**
     * Evaluates an appliance's consumption against its daily limit quota.
     * Enforces the user-configured limit action:
     * - NOTIFY: Emits informational alert at 80% and 100%.
     * - WARN: Emits warning alert.
     * - CONFIRM: Requires confirmation to continue.
     * - AUTO_OFF: Triggers auto-cut IF AND ONLY IF isCritical == false!
     */
    fun evaluateApplianceLimit(appliance: Appliance): LimitCheckResult {
        val ratio = appliance.dailyKwh / appliance.dailyLimitKwh.coerceAtLeast(0.1)
        var shouldAutoCut = false
        var alert: VoltoAlert? = null

        if (ratio >= 1.0) {
            val title = "${appliance.name} Daily Limit Exceeded!"
            val msg = "${appliance.name} consumed ${appliance.dailyKwh} kWh against daily limit of ${appliance.dailyLimitKwh} kWh."

            when (appliance.limitAction) {
                LimitAction.AUTO_OFF -> {
                    if (appliance.isCritical) {
                        // CRITICAL SAFETY RULE: Never auto-turn off critical appliances!
                        alert = VoltoAlert(
                            id = "alt-limit-crit-${appliance.id}",
                            title = "$title (Critical Device)",
                            message = "$msg Auto-cutoff was suppressed because this is marked as a critical appliance.",
                            timestamp = System.currentTimeMillis(),
                            severity = AlertSeverity.WARNING,
                            relatedApplianceId = appliance.id,
                            actionText = "Review Limit"
                        )
                    } else {
                        shouldAutoCut = true
                        alert = VoltoAlert(
                            id = "alt-limit-cut-${appliance.id}",
                            title = "$title (Auto Cut-Off Triggered)",
                            message = "$msg Device was safely switched off to protect your monthly bill.",
                            timestamp = System.currentTimeMillis(),
                            severity = AlertSeverity.WARNING,
                            relatedApplianceId = appliance.id,
                            actionText = "Turn Back ON"
                        )
                    }
                }
                LimitAction.WARN -> {
                    alert = VoltoAlert(
                        id = "alt-limit-warn-${appliance.id}",
                        title = title,
                        message = "$msg Running further will increase higher-tier tariff surcharge.",
                        timestamp = System.currentTimeMillis(),
                        severity = AlertSeverity.WARNING,
                        relatedApplianceId = appliance.id,
                        actionText = "Inspect Appliance"
                    )
                }
                LimitAction.CONFIRM -> {
                    alert = VoltoAlert(
                        id = "alt-limit-conf-${appliance.id}",
                        title = "$title (Confirmation Required)",
                        message = "$msg Tap to acknowledge and allow extra run time.",
                        timestamp = System.currentTimeMillis(),
                        severity = AlertSeverity.WARNING,
                        relatedApplianceId = appliance.id,
                        actionText = "Allow Overrun"
                    )
                }
                LimitAction.NOTIFY -> {
                    alert = VoltoAlert(
                        id = "alt-limit-notif-${appliance.id}",
                        title = title,
                        message = msg,
                        timestamp = System.currentTimeMillis(),
                        severity = AlertSeverity.INFO,
                        relatedApplianceId = appliance.id
                    )
                }
            }
        } else if (ratio >= 0.8) {
            alert = VoltoAlert(
                id = "alt-limit-near-${appliance.id}",
                title = "${appliance.name} Near Limit (80%)",
                message = "${appliance.name} is at ${(ratio * 100).toInt()}% of its daily quota (${appliance.dailyKwh}/${appliance.dailyLimitKwh} kWh).",
                timestamp = System.currentTimeMillis(),
                severity = AlertSeverity.INFO,
                relatedApplianceId = appliance.id
            )
        }

        return LimitCheckResult(
            applianceId = appliance.id,
            applianceName = appliance.name,
            currentKwh = appliance.dailyKwh,
            limitKwh = appliance.dailyLimitKwh,
            ratio = ratio,
            shouldAutoCut = shouldAutoCut,
            alertToEmit = alert
        )
    }
}

object BudgetMonitor {

    /**
     * Checks monthly budget thresholds: 50%, 75%, 90%, and 100%.
     */
    fun checkBudgetMilestones(currentSpendInr: Double, budgetInr: Double): VoltoAlert? {
        val percent = (currentSpendInr / budgetInr.coerceAtLeast(1.0)) * 100.0
        val now = System.currentTimeMillis()

        return when {
            percent >= 100.0 -> VoltoAlert(
                id = "budget-alert-100",
                title = "🚨 Monthly Budget Exceeded (100%)",
                message = "Your electricity bill has reached ₹${currentSpendInr.toInt()}, crossing your ₹${budgetInr.toInt()} monthly budget cap. Peak tier slab rates apply.",
                timestamp = now,
                severity = AlertSeverity.CRITICAL,
                actionText = "Review Tariff"
            )
            percent >= 90.0 -> VoltoAlert(
                id = "budget-alert-90",
                title = "⚠️ High Budget Warning (90%)",
                message = "You have consumed 90% of your monthly electricity budget (₹${currentSpendInr.toInt()} of ₹${budgetInr.toInt()}). Consider trimming AC and geyser usage.",
                timestamp = now,
                severity = AlertSeverity.WARNING,
                actionText = "View Tips"
            )
            percent >= 75.0 -> VoltoAlert(
                id = "budget-alert-75",
                title = "⚡ Budget Milestone: 75% Consumed",
                message = "Current bill estimate is ₹${currentSpendInr.toInt()} (${percent.toInt()}% of budget). On track if 75% of billing cycle has passed.",
                timestamp = now,
                severity = AlertSeverity.INFO,
                actionText = "Check Reports"
            )
            percent >= 50.0 -> VoltoAlert(
                id = "budget-alert-50",
                title = "📊 Halfway Budget Marker: 50%",
                message = "You have used 50% of your ₹${budgetInr.toInt()} budget (₹${currentSpendInr.toInt()}).",
                timestamp = now,
                severity = AlertSeverity.INFO
            )
            else -> null
        }
    }
}
