package com.adishra.alarmtask.domain

/**
 * Domain model for the seven days of the week.
 *
 * Stored in Room via [WeekdayConverter] as a bitmask so that a single INTEGER
 * column can represent any combination of selected weekdays without relying on
 * arbitrary strings or comma-separated lists.
 */
enum class Weekday {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}