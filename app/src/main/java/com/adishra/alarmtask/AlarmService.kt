package com.adishra.alarmtask

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class AlarmService : Service() {

    private var ringtone: Ringtone? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()

        val fullScreen = PendingIntent.getActivity(
            this, 3,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarm")
            .setContentText("Tap to open")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this, 1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        startRinging()

        // ★ NEW: explicitly try to launch the activity as well
        try {
            startActivity(
                Intent(this, AlarmActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
                )
            )
        } catch (_: Exception) {
            // Some OEMs will throw — full screen intent fallback handles this
        }

        return START_NOT_STICKY
    }

    private fun startRinging() {
        if (ringtone?.isPlaying == true) return

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (alarmUri == null) return // no sound available at all

        ringtone = RingtoneManager.getRingtone(this, alarmUri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            play()
        }
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    override fun onDestroy() {
        ringtone?.stop()
        ringtone = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
    }
}