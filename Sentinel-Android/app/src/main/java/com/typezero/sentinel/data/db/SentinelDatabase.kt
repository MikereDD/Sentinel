package com.typezero.sentinel.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [KnownDevice::class, WatchedTarget::class, NetworkEvent::class, NetworkState::class],
    version = 5,
    exportSchema = true
)
abstract class SentinelDatabase : RoomDatabase() {
    abstract fun knownDeviceDao(): KnownDeviceDao
    abstract fun watchedTargetDao(): WatchedTargetDao
    abstract fun networkEventDao(): NetworkEventDao
    abstract fun networkStateDao(): NetworkStateDao

    companion object {
        @Volatile
        private var INSTANCE: SentinelDatabase? = null

        /** Additive migration: keeps watched targets and timeline intact. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE known_devices ADD COLUMN deviceType TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE known_devices ADD COLUMN vendor TEXT")
                db.execSQL("ALTER TABLE known_devices ADD COLUMN model TEXT")
                db.execSQL("ALTER TABLE known_devices ADD COLUMN typeConfidence INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Adds user-override columns (custom name + manual type). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE known_devices ADD COLUMN customName TEXT")
                db.execSQL("ALTER TABLE known_devices ADD COLUMN userType TEXT")
            }
        }

        /** Adds the room/location label. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE known_devices ADD COLUMN room TEXT")
            }
        }

        /** Adds discovery hysteresis so one missed sweep does not create a false outage. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE known_devices ADD COLUMN missedScans INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): SentinelDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SentinelDatabase::class.java,
                    "sentinel.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
    }
}
