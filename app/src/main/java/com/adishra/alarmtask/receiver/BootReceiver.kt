package com.adishra.alarmtask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adishra.alarmtask.receiver.BootReceiver

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Logic to reschedule exact alarms using Room database upon device reboot
        }
    }
}
