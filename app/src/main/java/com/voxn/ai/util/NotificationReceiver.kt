package com.voxn.ai.util

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import com.voxn.ai.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Voxn AI"
        val message = intent.getStringExtra("message") ?: ""
        val id = intent.getIntExtra("id", 0)
        val type = intent.getStringExtra("type") ?: "habit"
        val habitId = intent.getStringExtra("habit_id")
        val noteId = intent.getStringExtra("note_id")

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, "jarvis_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF00D4FF.toInt())
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Add action buttons based on notification type
        if (type == "habit" && habitId != null) {
            val doneIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_HABIT_DONE
                putExtra("notification_id", id)
                putExtra("habit_id", habitId)
            }
            val donePending = PendingIntent.getBroadcast(context, id * 100 + 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, "Done", donePending)
            addSnoozeActions(context, builder, id, title, message, type, habitId = habitId)
        } else if (type == "note" && noteId != null) {
            val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_NOTE_COMPLETE
                putExtra("notification_id", id)
                putExtra("note_id", noteId)
            }
            val completePending = PendingIntent.getBroadcast(context, id * 100 + 1, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, "Complete", completePending)
            addSnoozeActions(context, builder, id, title, message, type, noteId = noteId)
        }

        manager.notify(id, builder.build())
    }

    private fun addSnoozeActions(
        context: Context, builder: NotificationCompat.Builder,
        id: Int, title: String, message: String, type: String,
        habitId: String? = null, noteId: String? = null,
    ) {
        listOf(15 to "Snooze 15m", 30 to "Snooze 30m").forEach { (minutes, label) ->
            val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = if (minutes == 15) NotificationActionReceiver.ACTION_SNOOZE_15 else NotificationActionReceiver.ACTION_SNOOZE_30
                putExtra("notification_id", id)
                putExtra("title", title)
                putExtra("message", message)
                putExtra("type", type)
                habitId?.let { putExtra("habit_id", it) }
                noteId?.let { putExtra("note_id", it) }
            }
            val pending = PendingIntent.getBroadcast(context, id * 100 + minutes, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, label, pending)
        }
    }
}
