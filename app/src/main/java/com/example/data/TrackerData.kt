package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val fontSize: Int = 16, // Font size in SP, e.g. 14, 18, 22
    val fontFamily: String = "SansSerif", // "SansSerif", "Serif", "Monospace"
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val imageUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean = true,
    val daysOfWeek: String = "", // e.g., "Mon,Tue,Wed,Thu,Fri"
    val audioUri: String? = null,
    val audioName: String? = null,
    val isVibrate: Boolean = true
)

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityName: String,
    val startTime: Long,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)
}

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Int)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY startTime DESC")
    fun getAllActivityLogs(): Flow<List<ActivityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivityLog(log: ActivityLog)

    @Query("DELETE FROM activity_logs WHERE id = :id")
    suspend fun deleteActivityLogById(id: Int)
}

@Database(entities = [Note::class, Alarm::class, ActivityLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract val noteDao: NoteDao
    abstract val alarmDao: AlarmDao
    abstract val activityLogDao: ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TrackerRepository(private val db: AppDatabase) {
    val allNotes: Flow<List<Note>> = db.noteDao.getAllNotes()
    suspend fun insertNote(note: Note) = db.noteDao.insertNote(note)
    suspend fun deleteNoteById(id: Int) = db.noteDao.deleteNoteById(id)

    val allAlarms: Flow<List<Alarm>> = db.alarmDao.getAllAlarms()
    suspend fun insertAlarm(alarm: Alarm): Long = db.alarmDao.insertAlarm(alarm)
    suspend fun deleteAlarm(id: Int) = db.alarmDao.deleteAlarmById(id)

    val allActivityLogs: Flow<List<ActivityLog>> = db.activityLogDao.getAllActivityLogs()
    suspend fun insertActivityLog(log: ActivityLog) = db.activityLogDao.insertActivityLog(log)
    suspend fun deleteActivityLog(id: Int) = db.activityLogDao.deleteActivityLogById(id)
}
