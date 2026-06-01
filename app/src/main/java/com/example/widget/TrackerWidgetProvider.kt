// Developer: Chetraj Jaishi
package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import com.example.R
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.ActivityLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrackerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("TrackerWidgetProvider", "Widget Action received: ${intent.action}")

        val prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)

        when (intent.action) {
            "com.example.widget.ACTION_WIDGET_START" -> {
                // If already tracking, ignore or stop first
                val current = prefs.getString("active_activity", null)
                if (current == null) {
                    prefs.edit()
                        .putString("active_activity", "Studying") // Default category from widget quickstart
                        .putLong("active_activity_start_time", System.currentTimeMillis())
                        .apply()
                }
                triggerInternalUpdate(context)
            }
            "com.example.widget.ACTION_WIDGET_STOP" -> {
                val current = prefs.getString("active_activity", null)
                val start = prefs.getLong("active_activity_start_time", 0L)
                if (current != null && start > 0L) {
                    val duration = (System.currentTimeMillis() - start) / 1000L
                    if (duration >= 1) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val db = AppDatabase.getDatabase(context)
                                db.activityLogDao.insertActivityLog(
                                    ActivityLog(
                                        activityName = current,
                                        startTime = start,
                                        durationSeconds = duration
                                    )
                                )
                                Log.d("TrackerWidget", "Successfully logged activity from widget: $current for $duration seconds")
                            } catch (e: Exception) {
                                Log.e("TrackerWidget", "Failed to log activity from widget", e)
                            }
                        }
                    }
                }
                prefs.edit()
                    .remove("active_activity")
                    .remove("active_activity_start_time")
                    .apply()
                triggerInternalUpdate(context)
            }
            "com.example.widget.UPDATE_WIDGET", Intent.ACTION_USER_PRESENT -> {
                triggerInternalUpdate(context)
            }
        }
    }

    private fun triggerInternalUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, TrackerWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.tracker_widget)

        val prefs = context.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
        val activeActivity = prefs.getString("active_activity", null)
        val startTime = prefs.getLong("active_activity_start_time", 0L)

        if (activeActivity != null) {
            views.setTextViewText(R.id.widget_status, "TRACKING: $activeActivity")

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTimeStr = timeFormat.format(Date(startTime))
            views.setTextViewText(R.id.widget_time, "Started at $startTimeStr")
        } else {
            views.setTextViewText(R.id.widget_status, "IDLE")
            views.setTextViewText(R.id.widget_time, "Ready to start")
        }

        // PendingIntent for Start Button
        val startIntent = Intent(context, TrackerWidgetProvider::class.java).apply {
            action = "com.example.widget.ACTION_WIDGET_START"
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            11001,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_start, startPendingIntent)

        // PendingIntent for Stop Button
        val stopIntent = Intent(context, TrackerWidgetProvider::class.java).apply {
            action = "com.example.widget.ACTION_WIDGET_STOP"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            11002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_stop, stopPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
