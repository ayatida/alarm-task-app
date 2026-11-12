package com.adishra.alarmtask.domain

import androidx.room.TypeConverter

/**
 * Converts a [Set] of [Weekday] to and from a single INTEGER bitmask.
 *
 * Bit layout (1-based):
 *   MONDAY    = 1
 *   TUESDAY   = 2
 *   WEDNESDAY = 4
 *   THURSDAY  = 8
 *   FRIDAY    = 16
 *   SATURDAY  = 32
 *   SUNDAY    = 64
 *
 * A value of 0 means no days selected.
 */
class WeekdayConverter {

    @TypeConverter
    fun toBitmask(days: Set<Weekday>?): Int {
        if (days == null) return 0
        var mask = 0
        for (day in days) {
            mask = mask or (1 shl day.ordinal)
        }
        return mask
    }

    @TypeConverter
    fun fromBitmask(mask: Int?): Set<Weekday> {
        if (mask == null || mask == 0) return emptySet()
        val result = mutableSetOf<Weekday>()
        for (day in Weekday.values()) {
            if (mask and (1 shl day.ordinal) != 0) {
                result.add(day)
            }
        }
        return result
    }
}