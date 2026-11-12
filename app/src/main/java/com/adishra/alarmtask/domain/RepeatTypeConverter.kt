package com.adishra.alarmtask.domain

import androidx.room.TypeConverter

class RepeatTypeConverter {
    @TypeConverter
    fun fromRepeatType(type: RepeatType): String = type.name

    @TypeConverter
    fun toRepeatType(name: String): RepeatType = RepeatType.valueOf(name)
}