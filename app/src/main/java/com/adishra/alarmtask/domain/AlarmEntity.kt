package com.adishra.alarmtask.domain

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved alarm.
 *
 * The alarm is the single source of truth. All scheduling state is derived
 * from this record; no alarm state lives in memory.
 *
 * - [nextTriggerTimeMillis] is the wall-clock time (UTC millis) of the next
 *   scheduled trigger. It is recomputed after every fire and on every edit.
 *   Storing it lets the UI show "next alarm" without re-running the scheduler.
 */
@Entity(
    tableName = "alarms",
    indices = [Index(value = ["id"], unique = true)]
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Long,

    val hour: Int,          // 0-23 (24-hour)
    val minute: Int,        // 0-59
    val taskName: String,
    val repeatType: RepeatType = RepeatType.NONE,
    val selectedDays: Set<Weekday> = emptySet(),
    val everyXDays: Int = 0,

    val isEnabled: Boolean = true,
    val isSnoozed: Boolean = false,
    val snoozeUntilMillis: Long = 0L,

    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val nextTriggerTimeMillis: Long = 0L
)