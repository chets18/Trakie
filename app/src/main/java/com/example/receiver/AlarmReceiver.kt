package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarm signal received by receiver.")
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val audioUri = intent.getStringExtra("AUDIO_URI")
        val label = intent.getStringExtra("LABEL") ?: "RealTime Alarm Triggered!"

        // Acquire a temporary WakeLock to keep CPU alive during boot transition or deep sleep
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "trakie:AlarmReceiverWakeLock"
        )
        wakeLock?.acquire(15 * 1000L) // Safe max limit of 15 seconds

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("AUDIO_URI", audioUri)
            putExtra("LABEL", label)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to start AlarmService", e)
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (ex: Exception) {}
        }
    }
}
