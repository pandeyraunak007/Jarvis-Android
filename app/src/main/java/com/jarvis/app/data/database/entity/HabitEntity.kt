package com.jarvis.app.data.database.entity

import androidx.room.*
import java.util.UUID

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
)

@Entity(
    tableName = "habit_completions",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class HabitCompletionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val date: Long = System.currentTimeMillis(),
)

data class HabitWithCompletions(
    @Embedded val habit: HabitEntity,
    @Relation(parentColumn = "id", entityColumn = "habitId")
    val completions: List<HabitCompletionEntity>
) {
    val reminderTimeFormatted: String
        get() {
            val h = if (habit.reminderHour == 0) 12 else if (habit.reminderHour > 12) habit.reminderHour - 12 else habit.reminderHour
            val amPm = if (habit.reminderHour < 12) "AM" else "PM"
            return "$h:${habit.reminderMinute.toString().padStart(2, '0')} $amPm"
        }

    fun isCompletedToday(): Boolean {
        val today = todayStart()
        return completions.any { it.date >= today }
    }

    fun currentStreak(): Int {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        val completionDays = completions.map { c ->
            val cc = java.util.Calendar.getInstance()
            cc.timeInMillis = c.date
            cc.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cc.set(java.util.Calendar.MINUTE, 0)
            cc.set(java.util.Calendar.SECOND, 0)
            cc.set(java.util.Calendar.MILLISECOND, 0)
            cc.timeInMillis
        }.toSet()

        var streak = 0
        while (completionDays.contains(cal.timeInMillis)) {
            streak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    fun completionDatesForMonth(monthStart: Long, monthEnd: Long): Set<Long> {
        return completions.filter { it.date in monthStart..monthEnd }
            .map { c ->
                val cc = java.util.Calendar.getInstance()
                cc.timeInMillis = c.date
                cc.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cc.set(java.util.Calendar.MINUTE, 0)
                cc.set(java.util.Calendar.SECOND, 0)
                cc.set(java.util.Calendar.MILLISECOND, 0)
                cc.timeInMillis
            }.toSet()
    }

    companion object {
        fun todayStart(): Long {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
