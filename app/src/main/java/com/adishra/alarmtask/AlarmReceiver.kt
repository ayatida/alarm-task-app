package com.adishra.alarmtask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("TASK_NAME") ?: "Complete your task"
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("TASK_NAME", taskName)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}