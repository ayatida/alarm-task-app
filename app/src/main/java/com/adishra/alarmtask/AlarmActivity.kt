package com.adishra.alarmtask

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        // Back button / swipe-back also stops the ringtone and dismisses.
        onBackPressedDispatcher.addCallback(this) {
            dismissAlarm()
        }

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("ALARM!", style = MaterialTheme.typography.displayMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("Later: the camera task check will go here.")
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { dismissAlarm() }) {
                        Text("Dismiss (test only)")
                    }
                }
            }
        }
    }

    private fun dismissAlarm() {
        stopService(Intent(this, AlarmService::class.java))
        finish()
    }
}