package com.adishra.alarmtask.domain

/**
 * How an alarm repeats.
 *
 * NONE       -> fires once at the scheduled time and then never again.
 * DAILY      -> fires every day at the scheduled time.
 * WEEKLY     -> fires on the weekdays selected in [Weekday] (at least one required).
 * EVERY_X    -> fires every X days starting from the first trigger.
 */
enum class RepeatType {
    NONE,
    DAILY,
    WEEKLY,
    EVERY_X
}