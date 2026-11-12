package com.adishra.alarmtask.domain

import androidx.room Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for alarms.
 *
 * All queries are [Flow]-based so the UI observes the database and always
 * reflects the current state without manual refresh.
 */
@Dao
interface AlarmDao {

    /** Observes all alarms, most recently created first. */
    @Query("SELECT * FROM alarms ORDER BY id DESC")
    fun observeAlarms(): Flow<List<AlarmEntity>>

    /** One-shot read of all alarms (used by the boot receiver). */
    @Query("SELECT * FROM alarms ORDER BY id DESC")
    suspend fun getAllAlarms(): List<AlarmEntity>

    /** One-shot read of a single alarm by id. */
    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    /** One-shot read of all *enabled* alarms. */
    @Query("SELECT * FROM alarms WHERE isEnabled = 1 ORDER BY id DESC")
    suspend fun getEnabledAlarms(): List<AlarmEntity>

    /** The next enabled alarm whose trigger time is in the future. */
    @Query(
        "SELECT * FROM alarms WHERE isEnabled = 1 AND nextTriggerTimeMillis > 0 " +
                "AND nextTriggerTimeMillis > :nowMillis " +
                "ORDER BY nextTriggerTimeMillis ASC LIMIT 1"
    )
    suspend fun getNextAlarm(nowMillis: Long): AlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    /** Hard delete by id (used when a record is removed and its schedule cancelled). */
    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteById(id: Long)
}