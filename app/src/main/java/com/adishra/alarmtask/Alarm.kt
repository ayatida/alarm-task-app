package com.adishra.alarmtask

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: String,
    val taskName: String
)