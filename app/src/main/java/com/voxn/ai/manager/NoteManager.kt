package com.voxn.ai.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.voxn.ai.data.database.VoxnDatabase
import com.voxn.ai.data.database.entity.NoteEntity
import com.voxn.ai.data.model.NoteCategory
import com.voxn.ai.data.model.NotePriority
import com.voxn.ai.util.NotificationReceiver
import kotlinx.coroutines.flow.Flow

class NoteManager(private val context: Context) {
    private val dao = VoxnDatabase.getInstance(context).noteDao()

    val notesFlow: Flow<List<NoteEntity>> = dao.getAll()

    suspend fun addNote(
        title: String, body: String, category: NoteCategory, priority: NotePriority,
        dueDate: Long? = null, reminderDate: Long? = null,
    ) {
        val note = NoteEntity(
            title = title, body = body, categoryRaw = category.name, priorityRaw = priority.name,
            dueDate = dueDate, reminderDate = reminderDate,
        )
        dao.insert(note)
        if (reminderDate != null) scheduleReminder(note)
    }

    suspend fun updateNote(
        note: NoteEntity, title: String, body: String, category: NoteCategory,
        priority: NotePriority, dueDate: Long?, reminderDate: Long?,
    ) {
        val updated = note.copy(
            title = title, body = body, categoryRaw = category.name, priorityRaw = priority.name,
            dueDate = dueDate, reminderDate = reminderDate,
        )
        dao.update(updated)
        cancelReminder(note.id)
        if (reminderDate != null) scheduleReminder(updated)
    }

    suspend fun deleteNote(note: NoteEntity) {
        cancelReminder(note.id)
        dao.delete(note)
    }

    suspend fun togglePin(note: NoteEntity) = dao.update(note.copy(isPinned = !note.isPinned))
    suspend fun toggleComplete(note: NoteEntity) = dao.update(note.copy(isCompleted = !note.isCompleted))

    fun scheduleReminder(note: NoteEntity) {
        val reminderDate = note.reminderDate ?: return
        if (reminderDate <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", "Voxn AI Reminder")
            putExtra("message", note.title)
            putExtra("id", note.id.hashCode())
            putExtra("type", "note")
            putExtra("note_id", note.id)
        }
        val pending = PendingIntent.getBroadcast(
            context, note.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderDate, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderDate, pending)
        }
    }

    fun cancelReminder(noteId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, noteId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
