// Developer: Chetraj Jaishi
package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Reminder

class NotifierReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "TRAKIE_NOTIFIER_CHANNEL"

        fun scheduleReminder(context: Context, reminder: Reminder) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NotifierReceiver::class.java).apply {
                action = "com.example.receiver.TRIGGER_REMINDER"
                putExtra("REMINDER_ID", reminder.id)
                putExtra("REMINDER_TITLE", reminder.title)
                putExtra("REMINDER_INTERVAL", reminder.intervalMinutes)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id + 200000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerAtMs = System.currentTimeMillis() + (reminder.intervalMinutes * 60 * 1000L)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMs,
                            pendingIntent
                        )
                    } else {
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMs,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                }
                Log.d("NotifierReceiver", "Scheduled reminder ID:${reminder.id} [${reminder.title}] in ${reminder.intervalMinutes} mins")
            } catch (e: Exception) {
                Log.e("NotifierReceiver", "Failed to schedule reminder ID:${reminder.id}", e)
                try {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                } catch (ex: Exception) {
                    // fallthrough
                }
            }
        }

        fun cancelReminder(context: Context, reminderId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NotifierReceiver::class.java).apply {
                action = "com.example.receiver.TRIGGER_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId + 200000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("NotifierReceiver", "Cancelled reminder schedule ID:$reminderId")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra("REMINDER_ID", -1)
        val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: "Smart Reminder Check!"
        val intervalMinutes = intent.getIntExtra("REMINDER_INTERVAL", 20)

        if (reminderId == -1) return

        // 1. Show dynamic High-Priority Notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "trakie Smart Reminder",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Dynamic periodic custom notifications"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400) // Clear, short single pulse vibrate
                    setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                reminderId + 300000,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("trakie Smart Reminder")
                .setContentText(reminderTitle)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(appPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(0, 400)) // Short pulse vibration
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // Custom notification sound
                .setAutoCancel(true)
                .build()

            notificationManager.notify(reminderId + 400000, notification)
        }

        // 2. Schedule the next recursive trigger for this specific alarm
        val triggerAtMs = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val nextIntent = Intent(context, NotifierReceiver::class.java).apply {
                action = "com.example.receiver.TRIGGER_REMINDER"
                putExtra("REMINDER_ID", reminderId)
                putExtra("REMINDER_TITLE", reminderTitle)
                putExtra("REMINDER_INTERVAL", intervalMinutes)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId + 200000,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                    } else {
                        alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                }
            } catch (e: Exception) {
                try {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                } catch (ex: Exception) {
                    // Ignore
                }
            }
        }
    }
}
