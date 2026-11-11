package com.adishra.alarmtask

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            MaterialTheme {
                var status by remember { mutableStateOf("Ready") }
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Alarm Task (test build)", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { status = schedule(15) }) { Text("Ring in 15 seconds") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { status = schedule(60) }) { Text("Ring in 1 minute") }
                    Spacer(Modifier.height(16.dp))
                    Text(status)
                }
            }
        }
    }

    private fun schedule(seconds: Long): String {
        val am = getSystemService(AlarmManager::class.java)

        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            )
            return "Allow 'Alarms & reminders', come back and press the button again."
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:$packageName"))
                )
                return "Allow full screen notifications, come back and press the button again."
            }
        }

        val triggerAt = System.currentTimeMillis() + seconds * 1000
        val alarmIntent = PendingIntent.getBroadcast(
            this, 1, Intent(this, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent), alarmIntent)
        return "Alarm set for $seconds seconds from now. Lock your phone now."
    }
}
