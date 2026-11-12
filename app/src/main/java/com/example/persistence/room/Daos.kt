package com.example.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplianceDao {
    @Query("SELECT * FROM appliances")
    fun getAllAppliances(): Flow<List<ApplianceEntity>>

    @Query("SELECT * FROM appliances")
    suspend fun getAllAppliancesSync(): List<ApplianceEntity>

    @Query("UPDATE appliances SET isOn = :isOn WHERE id = :id")
    suspend fun updateApplianceState(id: String, isOn: Boolean)

    @Query("SELECT * FROM appliances WHERE id = :id LIMIT 1")
    suspend fun getApplianceById(id: String): ApplianceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppliances(appliances: List<ApplianceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppliance(appliance: ApplianceEntity)

    @Update
    suspend fun updateAppliance(appliance: ApplianceEntity)

    @Query("UPDATE appliances SET isOn = :isOn, currentWatts = :currentWatts WHERE id = :id")
    suspend fun updatePowerState(id: String, isOn: Boolean, currentWatts: Int)

    @Query("UPDATE appliances SET dailyLimitKwh = :limitKwh, limitAction = :limitAction WHERE id = :id")
    suspend fun updateLimit(id: String, limitKwh: Double, limitAction: String)
}

@Dao
interface HourlyReadingDao {
    @Query("SELECT * FROM hourly_readings WHERE applianceId = :applianceId ORDER BY hourTimestamp DESC LIMIT :limit")
    suspend fun getRecentHourlyReadings(applianceId: String, limit: Int = 24): List<HourlyReadingEntity>

    @Query("SELECT * FROM hourly_readings WHERE hourTimestamp >= :sinceEpoch ORDER BY hourTimestamp ASC")
    suspend fun getReadingsSince(sinceEpoch: Long): List<HourlyReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: HourlyReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<HourlyReadingEntity>)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<AlertEntity>)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAsRead(alertId: String)

    @Query("UPDATE alerts SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface TariffConfigDao {
    @Query("SELECT * FROM tariff_config WHERE id = 1 LIMIT 1")
    fun getTariffConfigFlow(): Flow<TariffConfigEntity?>

    @Query("SELECT * FROM tariff_config WHERE id = 1 LIMIT 1")
    suspend fun getTariffConfig(): TariffConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveTariffConfig(config: TariffConfigEntity)
}

@Dao
interface AlertTrackingDao {
    @Query("SELECT COUNT(*) FROM alert_tracking WHERE trackingKey = :key")
    suspend fun countTrackingKey(key: String): Int

    suspend fun hasAlertBeenEmitted(key: String): Boolean = countTrackingKey(key) > 0

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAlertEmitted(entry: AlertTrackingEntity)

    @Query("DELETE FROM alert_tracking WHERE emittedAt < :beforeTimestamp")
    suspend fun pruneOldAlerts(beforeTimestamp: Long)
}
