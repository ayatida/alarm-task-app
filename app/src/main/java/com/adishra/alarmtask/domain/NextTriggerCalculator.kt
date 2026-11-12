package com.adishra.alarmtask.domain

import java.util.Calendar

/**
 * Pure function that computes the wall-clock time of the *next* trigger for an
 * alarm, given the current time and (for [RepeatType.EVERY_X]) the previously
 * scheduled trigger.
 *
 * This is the single source of truth for "when will this alarm fire next".
 * It is intentionally side-effect free so it can be unit-tested and reused by
 * the repository (persistence) and the scheduler (AlarmManager).
 *
 * Calendar.DAY_OF_WEEK uses Sunday = 1 .. Saturday = 7 (see [Calendar.SUNDAY]).
 *
 * @param nowMillis current wall-clock time in millis.
 * @param previousTriggerMillis the time the alarm is currently/was scheduled to
 *   fire. Only consulted for [RepeatType.EVERY_X]; pass 0L when there is no
 *   previous trigger (initial schedule).
 */
object NextTriggerCalculator {

    fun computeNextTrigger(
        alarm: AlarmEntity,
        nowMillis: Long,
        previousTriggerMillis: Long = 0L
    ): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }

        // Start from today at the target time and advance until we land on a
        // day that satisfies the repeat rule.
        cal.set(Calendar.HOUR_OF_DAY, alarm.hour)
        cal.set(Calendar.MINUTE, alarm.minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // If the target time today already passed, start looking from tomorrow.
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        when (alarm.repeatType) {
            RepeatType.NONE -> {
                // One-time: the next occurrence of the target time.
                return cal.timeInMillis
            }
            RepeatType.DAILY -> {
                // Every day. cal is already tomorrow (or today if the time
                // hasn't passed yet), which is correct.
                return cal.timeInMillis
            }
            RepeatType.WEEKLY -> {
                val selected = alarm.selectedDays
                if (selected.isEmpty()) {
                    // Invalid state (UI prevents it). Fall back to tomorrow.
                    return cal.timeInMillis
                }
                // Advance up to 8 days to find the next selected weekday.
                repeat(8) {
                    if (cal.get(Calendar.DAY_OF_WEEK) in selected) {
                        return cal.timeInMillis
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                // Should never happen, but be defensive.
                return cal.timeInMillis
            }
            RepeatType.EVERY_X -> {
                val interval = alarm.everyXDays.coerceAtLeast(1)
                if (previousTriggerMillis <= 0L) {
                    // Initial schedule: fire at the next occurrence of the time.
                    return cal.timeInMillis
                }
                // Subsequent triggers: previous trigger + interval days.
                // Adding whole days preserves the target hour/minute.
                val next = previousTriggerMillis + interval * MILLIS_PER_DAY
                // Guard against clock skew / late delivery: if the computed time
                // is already in the past, advance to the next valid interval.
                var candidate = next
                while (candidate <= nowMillis) {
                    candidate += interval * MILLIS_PER_DAY
                }
                return candidate
            }
        }
    }

    /** Calendar.DAY_OF_WEEK value for a [Weekday]. */
    private fun Weekday.toCalendarDay(): Int = when (this) {
        Weekday.SUNDAY -> Calendar.SUNDAY
        Weekday.MONDAY -> Calendar.MONDAY
        Weekday.TUESDAY -> Calendar.TUESDAY
        Weekday.WEDNESDAY -> Calendar.WEDNESDAY
        Weekday.THURSDAY -> Calendar.THURSDAY
        Weekday.FRIDAY -> Calendar.FRIDAY
        Weekday.SATURDAY -> Calendar.SATURDAY
    }

    private const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
}