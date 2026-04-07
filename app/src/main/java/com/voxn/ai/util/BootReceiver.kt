package com.voxn.ai.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voxn.ai.data.database.VoxnDatabase
import com.voxn.ai.manager.HabitManager
import com.voxn.ai.manager.NoteManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = VoxnDatabase.getInstance(context)
                val habitManager = HabitManager(context)
                val noteManager = NoteManager(context)

                // Re-schedule all habit reminders
                val habits = db.habitDao().getAllWithCompletions().first()
                for (hwc in habits) {
                    if (hwc.habit.reminderEnabled) {
                        habitManager.scheduleReminder(hwc.habit)
                    }
                }

                // Re-schedule all future note reminders
                val notes = db.noteDao().getAll().first()
                val now = System.currentTimeMillis()
                for (note in notes) {
                    if (!note.isCompleted && note.reminderDate != null && note.reminderDate > now) {
                        noteManager.scheduleReminder(note)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
