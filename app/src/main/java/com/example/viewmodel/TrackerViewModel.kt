// Developer: Chetraj Jaishi
package com.example.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.ActivityLog
import com.example.data.Alarm
import com.example.data.AppDatabase
import com.example.data.Note
import com.example.data.Reminder
import com.example.data.DailyRating
import com.example.MainActivity
import com.example.data.TrackerRepository
import com.example.receiver.AlarmReceiver
import com.example.receiver.NotifierReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    private val repository = TrackerRepository(db)

    // Real-Time Activity Tracking Engine (Shared with Widget)
    private var _activeActivity = MutableStateFlow<String?>(null)
    val activeActivity: StateFlow<String?> = _activeActivity.asStateFlow()

    private var _activeActivityStartTime = MutableStateFlow<Long?>(null)
    val activeActivityStartTime: StateFlow<Long?> = _activeActivityStartTime.asStateFlow()

    private var _activeActivitySeconds = MutableStateFlow(0L)
    val activeActivitySeconds: StateFlow<Long> = _activeActivitySeconds.asStateFlow()

    private var trackerJob: Job? = null

    // Data Flows
    val allNotes: StateFlow<List<Note>> = repository.allNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAlarms: StateFlow<List<Alarm>> = repository.allAlarms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allActivityLogs: StateFlow<List<ActivityLog>> = repository.allActivityLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allReminders: StateFlow<List<Reminder>> = repository.allReminders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allDailyRatings: StateFlow<List<DailyRating>> = repository.allDailyRatings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ------------------------------------------------------------------------
    // Dark Mode & Monochrome Local Persistence
    // ------------------------------------------------------------------------
    private val prefs = application.getSharedPreferences("tracker_prefs", Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        val nextVal = !_isDarkMode.value
        _isDarkMode.value = nextVal
        prefs.edit().putBoolean("dark_mode", nextVal).apply()
    }

    private val _isMonochromeMode = MutableStateFlow(prefs.getBoolean("monochrome_mode", false))
    val isMonochromeMode: StateFlow<Boolean> = _isMonochromeMode.asStateFlow()

    fun toggleMonochromeMode() {
        val nextVal = !_isMonochromeMode.value
        _isMonochromeMode.value = nextVal
        prefs.edit().putBoolean("monochrome_mode", nextVal).apply()
    }

    // ------------------------------------------------------------------------
    // Database-backed Dynamic Reminders
    // ------------------------------------------------------------------------
    fun addReminder(title: String, intervalMinutes: Int) {
        viewModelScope.launch {
            val reminder = Reminder(title = title, intervalMinutes = intervalMinutes, isEnabled = true)
            val insertedId = repository.insertReminder(reminder)
            // Schedule using AlarmManager
            NotifierReceiver.scheduleReminder(getApplication(), reminder.copy(id = insertedId.toInt()))
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            repository.insertReminder(updated)
            if (updated.isEnabled) {
                NotifierReceiver.scheduleReminder(getApplication(), updated)
            } else {
                NotifierReceiver.cancelReminder(getApplication(), updated.id)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            NotifierReceiver.cancelReminder(getApplication(), reminder.id)
            repository.deleteReminder(reminder.id)
        }
    }

    // ------------------------------------------------------------------------
    // Daily Ratings & One-Sentence Summaries
    // ------------------------------------------------------------------------
    fun saveDailyRating(dateString: String, rating: Int, summary: String) {
        viewModelScope.launch {
            repository.insertDailyRating(DailyRating(dateString, rating, summary))
        }
    }

    suspend fun getDailyRatingForDate(dateString: String): DailyRating? {
        return repository.getDailyRating(dateString)
    }

    init {
        // Restore active tracking session if any
        val active = prefs.getString("active_activity", null)
        val startTime = if (prefs.contains("active_activity_start_time")) prefs.getLong("active_activity_start_time", 0L) else null
        if (active != null && startTime != null && startTime > 0) {
            _activeActivity = MutableStateFlow(active)
            _activeActivityStartTime = MutableStateFlow(startTime)
            _activeActivitySeconds = MutableStateFlow((System.currentTimeMillis() - startTime) / 1000)
            
            trackerJob?.cancel()
            trackerJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _activeActivitySeconds.value = (System.currentTimeMillis() - startTime) / 1000
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // Alarm Controls
    // ------------------------------------------------------------------------
    fun addAlarm(hour: Int, minute: Int, label: String, repeatDays: String, audioUri: String?, audioName: String?) {
        viewModelScope.launch {
            val alarm = Alarm(
                hour = hour,
                minute = minute,
                label = label,
                daysOfWeek = repeatDays,
                audioUri = audioUri,
                audioName = audioName,
                isEnabled = true
            )
            val insertedId = repository.insertAlarm(alarm)
            scheduleSystemAlarm(alarm.copy(id = insertedId.toInt()))
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.insertAlarm(updated)
            if (updated.isEnabled) {
                scheduleSystemAlarm(updated)
            } else {
                cancelSystemAlarm(updated)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            cancelSystemAlarm(alarm)
            repository.deleteAlarm(alarm.id)
        }
    }

    private fun scheduleSystemAlarm(alarm: Alarm) {
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(app, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("AUDIO_URI", alarm.audioUri)
            putExtra("LABEL", alarm.label)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            app,
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
            calendar.add(Calendar.DAY_OF_YEAR, 1) // next day
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val showIntent = Intent(app, MainActivity::class.java)
                val showPendingIntent = PendingIntent.getActivity(
                    app,
                    alarm.id + 100000,
                    showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(
                    calendar.timeInMillis,
                    showPendingIntent
                )
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Log.d("TrackerViewModel", "Scheduled system alarm via setAlarmClock for: ${calendar.time}")
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("TrackerViewModel", "SecurityException scheduling exact alarm, falling back", e)
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

    private fun cancelSystemAlarm(alarm: Alarm) {
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(app, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            app,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("TrackerViewModel", "Cancelled system alarm ID: ${alarm.id}")
    }

    // ------------------------------------------------------------------------
    // Note Controls (Notion style rich-text fields)
    // ------------------------------------------------------------------------
    fun saveNote(
        id: Int = 0,
        title: String,
        content: String,
        fontSize: Int,
        fontFamily: String,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderlined: Boolean,
        imageUrl: String?
    ) {
        viewModelScope.launch {
            val note = Note(
                id = id,
                title = title,
                content = content,
                fontSize = fontSize,
                fontFamily = fontFamily,
                isBold = isBold,
                isItalic = isItalic,
                isUnderlined = isUnderlined,
                imageUrl = imageUrl,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertNote(note)
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(noteId)
        }
    }

    // Helper to resolve display name from imported music Uri
    fun getFileName(context: Context, uri: Uri): String {
        var name = "Unknown Audio"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    // ------------------------------------------------------------------------
    // Real-Time Activity Tracking Engine (Shared with Widget)
    // ------------------------------------------------------------------------

    private fun updateWidget() {
        val app = getApplication<Application>()
        val intent = Intent(app, com.example.widget.TrackerWidgetProvider::class.java).apply {
            action = "com.example.widget.UPDATE_WIDGET"
        }
        app.sendBroadcast(intent)
    }

    fun startActivityTracking(activityName: String) {
        if (_activeActivity.value != null) {
            stopAndLogActivity() // stops existing if any
        }
        _activeActivity.value = activityName
        val now = System.currentTimeMillis()
        _activeActivityStartTime.value = now
        _activeActivitySeconds.value = 0L

        prefs.edit()
            .putString("active_activity", activityName)
            .putLong("active_activity_start_time", now)
            .apply()

        trackerJob?.cancel()
        trackerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeActivitySeconds.value = (System.currentTimeMillis() - now) / 1000
            }
        }
        updateWidget()
    }

    fun stopAndLogActivity() {
        val name = _activeActivity.value ?: return
        val start = _activeActivityStartTime.value ?: return
        val duration = (System.currentTimeMillis() - start) / 1000

        trackerJob?.cancel()
        trackerJob = null

        _activeActivity.value = null
        _activeActivityStartTime.value = null
        _activeActivitySeconds.value = 0L

        prefs.edit()
            .remove("active_activity")
            .remove("active_activity_start_time")
            .apply()

        if (duration >= 1) { // Only log activities that ran for at least 1 second
            viewModelScope.launch {
                repository.insertActivityLog(
                    ActivityLog(
                        activityName = name,
                        startTime = start,
                        durationSeconds = duration
                    )
                )
            }
        }
        updateWidget()
    }

    fun cancelActiveActivity() {
        trackerJob?.cancel()
        trackerJob = null
        _activeActivity.value = null
        _activeActivityStartTime.value = null
        _activeActivitySeconds.value = 0L

        prefs.edit()
            .remove("active_activity")
            .remove("active_activity_start_time")
            .apply()
        updateWidget()
    }

    fun deleteActivityLog(logId: Int) {
        viewModelScope.launch {
            repository.deleteActivityLog(logId)
        }
    }

    // ------------------------------------------------------------------------
    // Stopwatch Engine (Precise real-time ticker)
    // ------------------------------------------------------------------------
    private val _stopwatchTimeMs = MutableStateFlow(0L)
    val stopwatchTimeMs: StateFlow<Long> = _stopwatchTimeMs.asStateFlow()

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<Long>>(emptyList())
    val stopwatchLaps: StateFlow<List<Long>> = _stopwatchLaps.asStateFlow()

    private var stopwatchJob: Job? = null
    private var stopwatchStartTime = 0L
    private var stopwatchPausedTime = 0L

    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        stopwatchStartTime = System.currentTimeMillis() - stopwatchPausedTime

        stopwatchJob?.cancel()
        stopwatchJob = viewModelScope.launch {
            while (true) {
                _stopwatchTimeMs.value = System.currentTimeMillis() - stopwatchStartTime
                delay(30) // High precision (approx 33 fps)
            }
        }
    }

    fun pauseStopwatch() {
        if (!_isStopwatchRunning.value) return
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        stopwatchPausedTime = System.currentTimeMillis() - stopwatchStartTime
    }

    fun lapStopwatch() {
        val current = _stopwatchTimeMs.value
        _stopwatchLaps.value = _stopwatchLaps.value + current
    }

    fun resetStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
        stopwatchStartTime = 0L
        stopwatchPausedTime = 0L
        _stopwatchTimeMs.value = 0L
        _stopwatchLaps.value = emptyList()
    }

    // ------------------------------------------------------------------------
    // Stopwatch & Alarm Background Music Picker
    // ------------------------------------------------------------------------
    private var backgroundMediaPlayer: MediaPlayer? = null
    
    private val _importedMusicUri = MutableStateFlow<String?>(null)
    val importedMusicUri: StateFlow<String?> = _importedMusicUri.asStateFlow()

    private val _importedMusicName = MutableStateFlow<String?>(null)
    val importedMusicName: StateFlow<String?> = _importedMusicName.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    fun setImportedMusic(uriStr: String?, nameStr: String?) {
        _importedMusicUri.value = uriStr
        _importedMusicName.value = nameStr
        // Auto reset background player
        stopBackgroundMusic()
    }

    fun toggleBackgroundMusic() {
        val uriStr = _importedMusicUri.value ?: return
        if (_isMusicPlaying.value) {
            pauseBackgroundMusic()
        } else {
            playBackgroundMusic(uriStr)
        }
    }

    private fun playBackgroundMusic(uriStr: String) {
        try {
            backgroundMediaPlayer?.release()
            backgroundMediaPlayer = MediaPlayer().apply {
                setDataSource(getApplication<Application>(), Uri.parse(uriStr))
                isLooping = true
                prepare()
                start()
            }
            _isMusicPlaying.value = true
        } catch (e: Exception) {
            Log.e("TrackerViewModel", "Failed to play imported background music", e)
        }
    }

    fun pauseBackgroundMusic() {
        try {
            backgroundMediaPlayer?.pause()
            _isMusicPlaying.value = false
        } catch (e: Exception) {
            Log.e("TrackerViewModel", "Failed to pause background music", e)
        }
    }

    fun stopBackgroundMusic() {
        try {
            backgroundMediaPlayer?.stop()
            backgroundMediaPlayer?.release()
            backgroundMediaPlayer = null
            _isMusicPlaying.value = false
        } catch (e: Exception) {
            Log.e("TrackerViewModel", "Failed to stop background music", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        trackerJob?.cancel()
        stopwatchJob?.cancel()
        backgroundMediaPlayer?.release()
        backgroundMediaPlayer = null
        db.close()
    }
}
