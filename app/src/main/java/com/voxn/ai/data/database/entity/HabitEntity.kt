package com.voxn.ai.data.database.entity

import androidx.room.*
import java.util.UUID

enum class HabitFrequency(val label: String) {
    Daily("Daily"),
    Weekly("Weekly"),
    MultiplePerDay("Multiple/Day");
}

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val frequencyRaw: String = "Daily",
    val targetCount: Int = 1,
    val weeklyDaysRaw: String = "",
) {
    val frequency: HabitFrequency
        get() = try { HabitFrequency.valueOf(frequencyRaw) } catch (_: Exception) { HabitFrequency.Daily }

    val weeklyDays: Set<Int>
        get() = if (weeklyDaysRaw.isBlank()) emptySet()
        else weeklyDaysRaw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    val frequencyLabel: String
        get() = when (frequency) {
            HabitFrequency.Daily -> "Daily"
            HabitFrequency.Weekly -> "Weekly (${weeklyDays.sorted().joinToString(",") { dayName(it) }})"
            HabitFrequency.MultiplePerDay -> "${targetCount}x per day"
        }

    companion object {
        fun dayName(day: Int): String = when (day) {
            1 -> "Sun"; 2 -> "Mon"; 3 -> "Tue"; 4 -> "Wed"
            5 -> "Thu"; 6 -> "Fri"; 7 -> "Sat"; else -> "?"
        }
    }
}

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

    /** Count of completions today */
    fun todayCompletionCount(): Int {
        val today = todayStart()
        return completions.count { it.date >= today }
    }

    /** Whether this habit is done for today based on frequency */
    fun isCompletedToday(): Boolean {
        return when (habit.frequency) {
            HabitFrequency.Daily -> todayCompletionCount() >= 1
            HabitFrequency.Weekly -> {
                val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                if (!habit.weeklyDays.contains(dayOfWeek)) true // not due today = considered done
                else todayCompletionCount() >= 1
            }
            HabitFrequency.MultiplePerDay -> todayCompletionCount() >= habit.targetCount
        }
    }

    /** Whether this habit is due today */
    fun isDueToday(): Boolean {
        return when (habit.frequency) {
            HabitFrequency.Daily, HabitFrequency.MultiplePerDay -> true
            HabitFrequency.Weekly -> {
                val dayOfWeek = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                habit.weeklyDays.contains(dayOfWeek)
            }
        }
    }

    /** Progress 0.0 to 1.0 for multi-per-day, 0 or 1 for others */
    fun progressToday(): Double {
        return when (habit.frequency) {
            HabitFrequency.MultiplePerDay -> {
                if (habit.targetCount <= 0) 1.0
                else (todayCompletionCount().toDouble() / habit.targetCount).coerceAtMost(1.0)
            }
            else -> if (isCompletedToday()) 1.0 else 0.0
        }
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
