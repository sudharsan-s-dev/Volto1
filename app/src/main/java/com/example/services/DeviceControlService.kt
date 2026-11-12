package com.example.services

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class CommandConfirmation(
    val commandId: String,
    val deviceId: String,
    val online: Boolean,
    val relayOn: Boolean,
    val result: String
)

sealed class CommandResult {
    data class Success(val newState: Boolean) : CommandResult()
    data class Failure(val reason: String) : CommandResult()
    object Timeout : CommandResult()
    object DeviceOffline : CommandResult()
}

class DeviceControlService {

    // Map of commandId -> CompletableDeferred for matching status confirmation
    private val pendingCommands = ConcurrentHashMap<String, CompletableDeferred<CommandConfirmation>>()

    /**
     * Invoked when status topic payload is received:
     * { "deviceId": "...", "online": true, "relayOn": true, "commandId": "...", "result": "SUCCESS" }
     */
    fun onStatusPacketReceived(confirmation: CommandConfirmation) {
        val deferred = pendingCommands.remove(confirmation.commandId)
        deferred?.complete(confirmation)
    }

    /**
     * Sends remote ON/OFF command to the smart switch.
     * UI stays in 'Sending' until status topic returns matching commandId.
     * Enforces a strict 5-second timeout requirement.
     */
    suspend fun sendCommand(
        deviceId: String,
        targetState: Boolean,
        isOnline: Boolean = true,
        commandDispatcher: ((commandId: String, commandPayloadJson: String) -> Unit)? = null
    ): CommandResult {
        if (!isOnline) {
            return CommandResult.DeviceOffline
        }

        val commandId = "cmd-${UUID.randomUUID().toString().take(8)}"
        val deferred = CompletableDeferred<CommandConfirmation>()
        pendingCommands[commandId] = deferred

        val commandJson = """{"commandId":"$commandId","deviceId":"$deviceId","command":"${if (targetState) "ON" else "OFF"}"}"""

        // Dispatch to hardware / MQTT broker if hooked
        commandDispatcher?.invoke(commandId, commandJson)

        return try {
            withTimeout(5000L) {
                // If no external hardware dispatcher was supplied, simulate realistic network roundtrip
                if (commandDispatcher == null) {
                    val simulatedDelay = Random.nextLong(1000, 1800)
                    delay(simulatedDelay)

                    val roll = Random.nextFloat()
                    if (roll < 0.90f) {
                        onStatusPacketReceived(
                            CommandConfirmation(
                                commandId = commandId,
                                deviceId = deviceId,
                                online = true,
                                relayOn = targetState,
                                result = "SUCCESS"
                            )
                        )
                    } else {
                        onStatusPacketReceived(
                            CommandConfirmation(
                                commandId = commandId,
                                deviceId = deviceId,
                                online = true,
                                relayOn = !targetState,
                                result = "PACKET_DROPPED_RETRY"
                            )
                        )
                    }
                }

                val confirmation = deferred.await()
                if (confirmation.result == "SUCCESS") {
                    CommandResult.Success(confirmation.relayOn)
                } else {
                    CommandResult.Failure("Device rejected: ${confirmation.result}")
                }
            }
        } catch (_: TimeoutCancellationException) {
            pendingCommands.remove(commandId)
            CommandResult.Timeout
        } catch (e: Exception) {
            pendingCommands.remove(commandId)
            CommandResult.Failure(e.message ?: "Unknown control error")
        }
    }
}
