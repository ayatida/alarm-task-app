package com.adishra.alarmtask.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.adishra.alarmtask.AlarmReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Single entry point for all alarm persistence and AlarmManager scheduling.
 *
 * The database is the single source of truth. All scheduling state is derived
 * from this record.
 */
class AlarmRepository(
    private val context: Context,
    private val database: AlarmDatabase,
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
) {
    private val tag = "AlarmRepository"
    private val dao = database.alarmDao()

    fun observeAlarms(): Flow<List<AlarmEntity>> = dao.observeAlarms()

    fun observeNextAlarm(nowMillis: Long = System.currentTimeMillis()): Flow<AlarmEntity?> =
        dao.getNextAlarm(nowMillis)

    suspend fun getEnabledAlarms(): List<AlarmEntity> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.getEnabledAlarms()
    }

    suspend fun getAlarm(id: Long): AlarmEntity? = withContext(kotlinx.coroutines.Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun createAlarm(alarm: AlarmEntity): Long? =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val id = dao.insert(alarm)
                val saved = alarm.copy(id = id)
                scheduleAlarmInternal(saved)
                id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "createAlarm failed", e)
                null
            }
        }

    suspend fun updateAlarm(alarm: AlarmEntity): Boolean =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                cancelAlarmInternal(alarm.id)
                val updated = alarm.copy(updatedAtMillis = System.currentTimeMillis())
                dao.update(updated)
                scheduleAlarmInternal(updated)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "updateAlarm failed", e)
                false
            }
        }

    suspend fun deleteAlarm(id: Long): Boolean =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                cancelAlarmInternal(id)
                dao.deleteById(id)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "deleteAlarm failed", e)
                false
            }
        }

    suspend fun setEnabled(id: Long, enabled: Boolean): Boolean =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val alarm = dao.getById(id) ?: return@withContext false
                val now = System.currentTimeMillis()
                if (enabled) {
                    val rescheduled = alarm.copy(
                        isEnabled = true,
                        updatedAtMillis = now
                    )
                    dao.update(rescheduled)
                    scheduleAlarmInternal(rescheduled)
                } else {
                    cancelAlarmInternal(id)
                    dao.update(alarm.copy(
                        isEnabled = false,
                        nextTriggerTimeMillis = 0L,
                        updatedAtMillis = now
                    ))
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "setEnabled failed", e)
                false
            }
        }

    suspend fun onAlarmFired(alarmId: Long) =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val alarm = dao.getById(alarmId) ?: return@withContext
                val now = System.currentTimeMillis()
                val updated = if (alarm.repeatType == RepeatType.NONE) {
                    alarm.copy(
                        isEnabled = false,
                        nextTriggerTimeMillis = 0L,
                        updatedAtMillis = now
                    )
                } else {
                    val next = NextTriggerCalculator.computeNextTrigger(
                        alarm = alarm,
                        nowMillis = now,
                        previousTriggerMillis = alarm.nextTriggerTimeMillis
                    )
                    alarm.copy(
                        nextTriggerTimeMillis = next,
                        updatedAtMillis = now
                    )
                }
                dao.update(updated)
                if (updated.isEnabled) scheduleAlarmInternal(updated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(tag, "onAlarmFired failed", e)
            }
        }

    suspend fun rescheduleAllEnabled() =
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            val alarms = dao.getEnabledAlarms()
            for (alarm in alarms) {
                try {
                    scheduleAlarmInternal(alarm)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(tag, "rescheduleAllEnabled failed for ${alarm.id}", e)
                }
            }
        }

    private suspend fun scheduleAlarmInternal(alarm: AlarmEntity) {
        if (!alarm.isEnabled) return
        cancelAlarmInternal(alarm.id)

        val now = System.currentTimeMillis()
        val next = NextTriggerCalculator.computeNextTrigger(
            alarm = alarm,
            nowMillis = now,
            previousTriggerMillis = alarm.nextTriggerTimeMillis
        )

        dao.update(alarm.copy(nextTriggerTimeMillis = next, updatedAtMillis = now))

        val pi = buildPendingIntent(alarm.id, alarm.taskName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, next, pi)
            return
        }

        val info = AlarmManager.AlarmClockInfo(next, pi)
        alarmManager.setAlarmClock(info, pi)
    }

    private suspend fun cancelAlarmInternal(id: Long) {
        val pi = buildPendingIntent(id, "")
        alarmManager.cancel(pi)
    }

    private fun buildPendingIntent(id: Long, taskName: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, id)
            putExtra(EXTRA_TASK_NAME, taskName)
        }
        return PendingIntent.getBroadcast(
            context,
            id.toAlarmRequestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private companion object {
        const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"
        const val EXTRA_TASK_NAME = "EXTRA_TASK_NAME"

        fun Long.toAlarmRequestCode(): Int = (this and 0x7FFFFFFF).toInt()
    }
}
