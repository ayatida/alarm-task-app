package com.adishra.alarmtask.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adishra.alarmtask.domain.AlarmDatabase
import com.adishra.alarmtask.domain.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    val db = AlarmDatabase.getDatabase(context)
                    val repo = AlarmRepository(context, db)
                    repo.rescheduleAllEnabled()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
