package com.example.persistence.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStorageManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("volto_secure_keystore_vault", Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private const val KEY_ALIAS = "VoltoSecureKeyAlias"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        const val KEY_MQTT_USER = "sec_mqtt_user"
        const val KEY_MQTT_PASS = "sec_mqtt_pass"
        const val KEY_AUTH_TOKEN = "sec_auth_jwt_token"
    }

    init {
        createKeyIfNeeded()
    }

    private fun createKeyIfNeeded() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun saveEncryptedString(key: String, value: String) {
        try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            val encoded = Base64.encodeToString(combined, Base64.DEFAULT)
            prefs.edit().putString(key, encoded).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDecryptedString(key: String, defaultValue: String = ""): String {
        val encoded = prefs.getString(key, null) ?: return defaultValue
        return try {
            val combined = Base64.decode(encoded, Base64.DEFAULT)
            if (combined.size <= GCM_IV_LENGTH) return defaultValue

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val cipherText = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun clearAllSecureData() {
        prefs.edit().clear().apply()
    }

    fun getMqttUsername(default: String = "volto_user"): String =
        getDecryptedString(KEY_MQTT_USER, default)

    fun setMqttUsername(username: String) =
        saveEncryptedString(KEY_MQTT_USER, username)

    fun getMqttPassword(default: String = "volto_secret"): String =
        getDecryptedString(KEY_MQTT_PASS, default)

    fun setMqttPassword(password: String) =
        saveEncryptedString(KEY_MQTT_PASS, password)

    fun getAuthToken(): String =
        getDecryptedString(KEY_AUTH_TOKEN, "")

    fun setAuthToken(token: String) =
        saveEncryptedString(KEY_AUTH_TOKEN, token)
}
