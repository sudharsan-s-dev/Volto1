package com.example.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.persistence.security.SecureStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Handles incoming push notifications and token registration.
 * Listens for "com.google.firebase.MESSAGING_EVENT" declared in AndroidManifest.xml.
 */
open class VoltoFirebaseMessagingService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient()

    override fun onBind(intent: Intent?): IBinder? = null

    open fun onNewToken(token: String) {
        Log.d("VoltoFCM", "New FCM registration token received: $token")
        val secureStorage = SecureStorageManager(applicationContext)
        secureStorage.saveEncryptedString("fcm_device_token", token)

        scope.launch {
            try {
                val json = """{"token": "$token", "homeId": "home-sharma-blr-01"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://api.volto.in/api/fcm/register-token")
                    .post(body)
                    .build()
                httpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w("VoltoFCM", "Backend token sync deferred: ${e.message}")
            }
        }
    }

    open fun onMessageReceived(title: String, message: String, type: String) {
        val channelId = when (type.uppercase()) {
            "SAFETY", "SURGE", "CRITICAL" -> NotificationHelper.CHANNEL_SAFETY
            "BUDGET", "SLAB" -> NotificationHelper.CHANNEL_BUDGET
            else -> NotificationHelper.CHANNEL_LIMITS
        }

        NotificationHelper.postNotification(
            context = applicationContext,
            notificationId = System.currentTimeMillis().toInt(),
            channelId = channelId,
            title = title,
            message = message,
            isAllowedByUser = true
        )
    }
}
