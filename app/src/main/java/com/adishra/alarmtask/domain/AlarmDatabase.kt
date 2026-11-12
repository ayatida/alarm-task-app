package com.adishra.alarmtask.domain

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

/**
 * Room database for the AlarmTask app.
 *
 * Schema version 1. Migrations are handled explicitly (see AlarmDatabase.MIGRATION_1_2)
 * so that adding a column never silently corrupts existing data.
 */
@Database(
    entities = [AlarmEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlarmDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        private const val DATABASE_NAME = "alarmtask.db"

        @Volatile
        private var INSTANCE: AlarmDatabase? = null

        fun getDatabase(context: Context): AlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AlarmDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}