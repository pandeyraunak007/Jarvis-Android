package com.voxn.ai.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.voxn.ai.data.database.VoxnDatabase
import com.voxn.ai.data.database.entity.HabitCompletionEntity
import com.voxn.ai.data.database.entity.HabitEntity
import com.voxn.ai.data.database.entity.HabitWithCompletions
import com.voxn.ai.util.NotificationReceiver
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class HabitManager(private val context: Context) {
    private val dao = VoxnDatabase.getInstance(context).habitDao()

    val habitsFlow: Flow<List<HabitWithCompletions>> = dao.getAllWithCompletions()

    suspend fun addHabit(
        name: String, reminderEnabled: Boolean, reminderHour: Int, reminderMinute: Int,
        frequencyRaw: String = "Daily", targetCount: Int = 1, weeklyDaysRaw: String = "",
    ) {
        val habit = HabitEntity(
            name = name,
            reminderEnabled = reminderEnabled,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            frequencyRaw = frequencyRaw,
            targetCount = targetCount,
            weeklyDaysRaw = weeklyDaysRaw,
        )
        dao.insertHabit(habit)
        if (reminderEnabled) scheduleReminder(habit)
    }

    suspend fun deleteHabit(habit: HabitEntity) {
        cancelReminder(habit.id)
        dao.deleteHabit(habit)
    }

    suspend fun updateReminder(habit: HabitEntity, enabled: Boolean, hour: Int, minute: Int) {
        val updated = habit.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute)
        dao.updateHabit(updated)
        cancelReminder(habit.id)
        if (enabled) scheduleReminder(updated)
    }

    suspend fun toggleCompletion(habitWithCompletions: HabitWithCompletions) {
        val todayStart = HabitWithCompletions.todayStart()
        if (habitWithCompletions.isCompletedToday()) {
            dao.deleteCompletionForToday(habitWithCompletions.habit.id, todayStart)
        } else {
            dao.insertCompletion(HabitCompletionEntity(habitId = habitWithCompletions.habit.id, date = System.currentTimeMillis()))
        }
    }

    fun scheduleReminder(habit: HabitEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Fall back to inexact alarm — still fires, just not at the exact second
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", "Voxn AI Habit Reminder")
                putExtra("message", "Don't forget: ${habit.name}")
                putExtra("id", habit.id.hashCode())
                putExtra("type", "habit")
                putExtra("habit_id", habit.id)
            }
            val pending = PendingIntent.getBroadcast(
                context, habit.id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, habit.reminderHour)
                set(Calendar.MINUTE, habit.reminderMinute)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
            return
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", "Voxn AI Habit Reminder")
            putExtra("message", "Don't forget: ${habit.name}")
            putExtra("id", habit.id.hashCode())
        }
        val pending = PendingIntent.getBroadcast(
            context, habit.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, habit.reminderHour)
            set(Calendar.MINUTE, habit.reminderMinute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
    }

    fun cancelReminder(habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, habitId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
