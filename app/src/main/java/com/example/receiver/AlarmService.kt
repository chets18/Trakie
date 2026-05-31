package com.example.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var serviceWakeLock: android.os.PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "ALARM_SERVICE_CHANNEL"
        const val ACTION_DISMISS = "ACTION_DISMISS_ALARM"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            Log.d("AlarmService", "Dismiss action clicked. Stopping alarm service.")
            stopSelf()
            return START_NOT_STICKY
        }

        // Acquire service-level WakeLock to prevent the CPU from sleeping while alarm is triggered
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            serviceWakeLock = powerManager?.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK,
                "trakie:AlarmServiceWakeLock"
            )?.apply {
                acquire(10 * 60 * 1000L) // Sound alarm for max 10 minutes, after which release automatically
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to acquire WakeLock for AlarmService", e)
        }

        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val audioUri = intent?.getStringExtra("AUDIO_URI")
        val label = intent?.getStringExtra("LABEL") ?: "Wake Up!"

        Log.d("AlarmService", "Starting alarm sound: ID=$alarmId, label='$label', URI='$audioUri'")

        // Play Sound
        playAlarmSound(audioUri)

        // Trigger vibration
        triggerVibration()

        // Build notification
        val notification = buildForegroundNotification(label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(9999, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(9999, notification)
        }

        return START_STICKY
    }

    private fun playAlarmSound(audioUriStr: String?) {
        try {
            mediaPlayer?.release()
            
            mediaPlayer = MediaPlayer().apply {
                if (!audioUriStr.isNullOrEmpty()) {
                    setDataSource(this@AlarmService, Uri.parse(audioUriStr))
                } else {
                    val defaultAlarmUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                        ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
                    setDataSource(this@AlarmService, defaultAlarmUri)
                }
                
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to play customized alarm sound", e)
            // Ultimate fallback to default alarm/ringtone
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    val fallbackUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    setDataSource(this@AlarmService, fallbackUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (ex: Exception) {
                Log.e("AlarmService", "Double failure on fallback playback", ex)
            }
        }
    }

    private fun triggerVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 800, 800, 800)
                    val amplitudes = intArrayOf(0, 255, 0, 255)
                    it.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, 0))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(longArrayOf(0, 800, 800, 800), 0)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to initiate vibration", e)
        }
    }

    private fun buildForegroundNotification(label: String): Notification {
        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java)
        val appPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RealTime Alarm Active")
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(appPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Alarm RealTime Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for sounding alarms"
                setSound(null, null) // Avoid double sound from notification system and MediaPlayer
                enableVibration(false) // Let custom Vibrator handle patterns
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (serviceWakeLock?.isHeld == true) {
                serviceWakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Error releasing WakeLock on destroy", e)
        }
        serviceWakeLock = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e("AlarmService", "Error releasing MediaPlayer on destroy", e)
        }
        mediaPlayer = null
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e("AlarmService", "Error stopping Vibrator on destroy", e)
        }
        vibrator = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
