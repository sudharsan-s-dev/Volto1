package com.example.persistence.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ApplianceEntity::class,
        HourlyReadingEntity::class,
        AlertEntity::class,
        TariffConfigEntity::class,
        AlertTrackingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VoltoDatabase : RoomDatabase() {
    abstract fun applianceDao(): ApplianceDao
    abstract fun hourlyReadingDao(): HourlyReadingDao
    abstract fun alertDao(): AlertDao
    abstract fun tariffConfigDao(): TariffConfigDao
    abstract fun alertTrackingDao(): AlertTrackingDao

    companion object {
        @Volatile
        private var INSTANCE: VoltoDatabase? = null

        // Example migration definition for future schema evolution v1 -> v2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE appliances ADD COLUMN powerFactor REAL NOT NULL DEFAULT 0.95")
            }
        }

        fun getInstance(context: Context): VoltoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoltoDatabase::class.java,
                    "volto_database.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
