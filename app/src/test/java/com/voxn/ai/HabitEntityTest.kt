package com.voxn.ai

import com.voxn.ai.data.database.entity.HabitCompletionEntity
import com.voxn.ai.data.database.entity.HabitEntity
import com.voxn.ai.data.database.entity.HabitWithCompletions
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class HabitEntityTest {

    private fun todayStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun dayStart(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun makeHabit(completionDaysAgo: List<Int>): HabitWithCompletions {
        val habit = HabitEntity(name = "Test Habit")
        val completions = completionDaysAgo.map { daysAgo ->
            HabitCompletionEntity(habitId = habit.id, date = dayStart(daysAgo) + 3600000) // +1h into the day
        }
        return HabitWithCompletions(habit, completions)
    }

    @Test
    fun `isCompletedToday returns true when completed today`() {
        val hwc = makeHabit(listOf(0))
        assertTrue(hwc.isCompletedToday())
    }

    @Test
    fun `isCompletedToday returns false when no completions`() {
        val hwc = makeHabit(emptyList())
        assertFalse(hwc.isCompletedToday())
    }

    @Test
    fun `isCompletedToday returns false when only completed yesterday`() {
        val hwc = makeHabit(listOf(1))
        assertFalse(hwc.isCompletedToday())
    }

    @Test
    fun `currentStreak counts consecutive days from today`() {
        val hwc = makeHabit(listOf(0, 1, 2))
        assertEquals(3, hwc.currentStreak())
    }

    @Test
    fun `currentStreak is zero when no completions`() {
        val hwc = makeHabit(emptyList())
        assertEquals(0, hwc.currentStreak())
    }

    @Test
    fun `currentStreak breaks on gap`() {
        // Completed today, yesterday, and 3 days ago (gap at day 2)
        val hwc = makeHabit(listOf(0, 1, 3))
        assertEquals(2, hwc.currentStreak())
    }

    @Test
    fun `currentStreak is zero when not completed today`() {
        val hwc = makeHabit(listOf(1, 2, 3))
        assertEquals(0, hwc.currentStreak())
    }

    @Test
    fun `reminderTimeFormatted formats correctly`() {
        val habit = HabitEntity(name = "Test", reminderHour = 14, reminderMinute = 5)
        val hwc = HabitWithCompletions(habit, emptyList())
        assertEquals("2:05 PM", hwc.reminderTimeFormatted)
    }

    @Test
    fun `reminderTimeFormatted handles midnight`() {
        val habit = HabitEntity(name = "Test", reminderHour = 0, reminderMinute = 0)
        val hwc = HabitWithCompletions(habit, emptyList())
        assertEquals("12:00 AM", hwc.reminderTimeFormatted)
    }

    @Test
    fun `reminderTimeFormatted handles noon`() {
        val habit = HabitEntity(name = "Test", reminderHour = 12, reminderMinute = 30)
        val hwc = HabitWithCompletions(habit, emptyList())
        assertEquals("12:30 PM", hwc.reminderTimeFormatted)
    }
}
