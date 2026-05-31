package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.room.Room
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.Alarm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            Log.d("BootReceiver", "Device booted! Rescheduling all active alarms and notifier.")
            
            val pendingResult = goAsync()
            val coroutineScope = CoroutineScope(Dispatchers.IO)
            
            coroutineScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    
                    val alarms = db.alarmDao.getAllAlarms().first()
                    alarms.forEach { alarm ->
                        if (alarm.isEnabled) {
                            scheduleAlarm(context, alarm)
                        }
                    }
                    
                    // Reschedule custom notifier if enabled in preferences
                    val prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
                    val notifierEnabled = prefs.getBoolean("notifier_enabled", false)
                    if (notifierEnabled) {
                        val interval = prefs.getInt("notifier_interval_minutes", 20)
                        NotifierReceiver.scheduleNotification(context, interval)
                        Log.d("BootReceiver", "Rescheduled custom notifier successfully at interval: $interval mins")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun scheduleAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("AUDIO_URI", alarm.audioUri)
            putExtra("LABEL", alarm.label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Set to next day if time already passed
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(context, MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    context,
                    alarm.id + 100000,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(
                    calendar.timeInMillis,
                    showPendingIntent
                )
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d("BootReceiver", "Rescheduled active alarm ID: ${alarm.id} via setAlarmClock for time: ${calendar.time}")
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("BootReceiver", "SecurityException scheduling alarm ID ${alarm.id} on boot, falling back", e)
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}
