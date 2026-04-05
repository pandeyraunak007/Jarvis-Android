package com.jarvis.app.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.jarvis.app.data.database.JarvisDatabase
import com.jarvis.app.data.database.entity.NoteEntity
import com.jarvis.app.data.model.NoteCategory
import com.jarvis.app.data.model.NotePriority
import com.jarvis.app.util.NotificationReceiver
import kotlinx.coroutines.flow.Flow

class NoteManager(private val context: Context) {
    private val dao = JarvisDatabase.getInstance(context).noteDao()

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

    private fun scheduleReminder(note: NoteEntity) {
        val reminderDate = note.reminderDate ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", "Voxn AI Reminder")
            putExtra("message", note.title)
            putExtra("id", note.id.hashCode())
        }
        val pending = PendingIntent.getBroadcast(
            context, note.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderDate, pending)
    }

    private fun cancelReminder(noteId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, noteId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
