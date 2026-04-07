package com.voxn.ai.util

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voxn.ai.data.database.VoxnDatabase
import com.voxn.ai.data.database.entity.HabitCompletionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HABIT_DONE = "com.voxn.ai.HABIT_DONE"
        const val ACTION_NOTE_COMPLETE = "com.voxn.ai.NOTE_COMPLETE"
        const val ACTION_SNOOZE_15 = "com.voxn.ai.SNOOZE_15"
        const val ACTION_SNOOZE_30 = "com.voxn.ai.SNOOZE_30"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)

        when (intent.action) {
            ACTION_HABIT_DONE -> handleHabitDone(context, intent)
            ACTION_NOTE_COMPLETE -> handleNoteComplete(context, intent)
            ACTION_SNOOZE_15 -> handleSnooze(context, intent, 15)
            ACTION_SNOOZE_30 -> handleSnooze(context, intent, 30)
        }
    }

    private fun handleHabitDone(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id") ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = VoxnDatabase.getInstance(context).habitDao()
                dao.insertCompletion(HabitCompletionEntity(habitId = habitId))
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleNoteComplete(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("note_id") ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = VoxnDatabase.getInstance(context).noteDao()
                val notes = dao.getById(noteId)
                notes?.let { dao.update(it.copy(isCompleted = true)) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleSnooze(context: Context, intent: Intent, minutes: Int) {
        val title = intent.getStringExtra("title") ?: "Voxn AI"
        val message = intent.getStringExtra("message") ?: ""
        val notificationId = intent.getIntExtra("notification_id", 0)
        val habitId = intent.getStringExtra("habit_id")
        val noteId = intent.getStringExtra("note_id")
        val type = intent.getStringExtra("type") ?: "habit"

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("id", notificationId)
            putExtra("type", type)
            habitId?.let { putExtra("habit_id", it) }
            noteId?.let { putExtra("note_id", it) }
        }
        val pending = PendingIntent.getBroadcast(
            context, notificationId + 10000 + minutes,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerTime = System.currentTimeMillis() + minutes * 60 * 1000L
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
    }
}
