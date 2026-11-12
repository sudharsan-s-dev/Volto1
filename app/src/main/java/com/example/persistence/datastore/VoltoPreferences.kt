package com.example.persistence.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "volto_settings")

enum class DataSourceMode {
    MOCK,
    LIVE
}

class VoltoPreferences(private val context: Context) {

    companion object {
        val KEY_DARK_MODE = booleanPreferencesKey("key_dark_mode")
        val KEY_LANGUAGE = stringPreferencesKey("key_language")
        val KEY_DATA_SOURCE = stringPreferencesKey("key_data_source")
        val KEY_HOUSEHOLD_NAME = stringPreferencesKey("key_household_name")
        val KEY_HOME_ADDRESS = stringPreferencesKey("key_home_address")
        val KEY_DISCOM_PROVIDER = stringPreferencesKey("key_discom_provider")
        val KEY_MONTHLY_BUDGET = doublePreferencesKey("key_monthly_budget")
        val KEY_MQTT_BROKER_URL = stringPreferencesKey("key_mqtt_broker_url")
        val KEY_MQTT_PORT = stringPreferencesKey("key_mqtt_port")
        val KEY_HOME_ID = stringPreferencesKey("key_home_id")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: true
    }

    val selectedLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "en" // "en", "ta", "hi"
    }

    val dataSourceMode: Flow<DataSourceMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_DATA_SOURCE] ?: DataSourceMode.MOCK.name
        try { DataSourceMode.valueOf(raw) } catch (_: Exception) { DataSourceMode.MOCK }
    }

    val householdName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOUSEHOLD_NAME] ?: "Sharma Family"
    }

    val homeAddress: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOME_ADDRESS] ?: "Flat 402, Green Glen Layout, Bellandur, Bengaluru"
    }

    val discomProvider: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DISCOM_PROVIDER] ?: "BESCOM (Karnataka)"
    }

    val monthlyBudget: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_MONTHLY_BUDGET] ?: 3500.0
    }

    val mqttBrokerUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MQTT_BROKER_URL] ?: "ssl://mqtt.volto.in"
    }

    val mqttPort: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MQTT_PORT] ?: "8883"
    }

    val homeId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOME_ID] ?: "home-sharma-blr-01"
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setLanguage(langCode: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = langCode }
    }

    suspend fun setDataSourceMode(mode: DataSourceMode) {
        context.dataStore.edit { it[KEY_DATA_SOURCE] = mode.name }
    }

    suspend fun updateHouseholdProfile(name: String, address: String, provider: String, budget: Double) {
        context.dataStore.edit {
            it[KEY_HOUSEHOLD_NAME] = name
            it[KEY_HOME_ADDRESS] = address
            it[KEY_DISCOM_PROVIDER] = provider
            it[KEY_MONTHLY_BUDGET] = budget
        }
    }

    suspend fun updateMqttConfig(url: String, port: String, homeId: String) {
        context.dataStore.edit {
            it[KEY_MQTT_BROKER_URL] = url
            it[KEY_MQTT_PORT] = port
            it[KEY_HOME_ID] = homeId
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
