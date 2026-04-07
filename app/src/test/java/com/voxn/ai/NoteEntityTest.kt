package com.voxn.ai

import com.voxn.ai.data.database.entity.NoteEntity
import org.junit.Assert.*
import org.junit.Test

class NoteEntityTest {

    @Test
    fun `isOverdue returns true when due date is past and not completed`() {
        val note = NoteEntity(
            title = "Test",
            dueDate = System.currentTimeMillis() - 86400000, // yesterday
            isCompleted = false,
        )
        assertTrue(note.isOverdue)
    }

    @Test
    fun `isOverdue returns false when completed`() {
        val note = NoteEntity(
            title = "Test",
            dueDate = System.currentTimeMillis() - 86400000,
            isCompleted = true,
        )
        assertFalse(note.isOverdue)
    }

    @Test
    fun `isOverdue returns false when due date is in the future`() {
        val note = NoteEntity(
            title = "Test",
            dueDate = System.currentTimeMillis() + 86400000, // tomorrow
            isCompleted = false,
        )
        assertFalse(note.isOverdue)
    }

    @Test
    fun `isOverdue returns false when no due date`() {
        val note = NoteEntity(title = "Test", dueDate = null)
        assertFalse(note.isOverdue)
    }

    @Test
    fun `hasUpcomingReminder returns true when reminder is within 24 hours`() {
        val note = NoteEntity(
            title = "Test",
            reminderDate = System.currentTimeMillis() + 3600000, // 1 hour from now
            isCompleted = false,
        )
        assertTrue(note.hasUpcomingReminder)
    }

    @Test
    fun `hasUpcomingReminder returns false when reminder is more than 24 hours away`() {
        val note = NoteEntity(
            title = "Test",
            reminderDate = System.currentTimeMillis() + 2 * 86400000, // 2 days from now
            isCompleted = false,
        )
        assertFalse(note.hasUpcomingReminder)
    }

    @Test
    fun `hasUpcomingReminder returns false when completed`() {
        val note = NoteEntity(
            title = "Test",
            reminderDate = System.currentTimeMillis() + 3600000,
            isCompleted = true,
        )
        assertFalse(note.hasUpcomingReminder)
    }

    @Test
    fun `hasUpcomingReminder returns false when no reminder`() {
        val note = NoteEntity(title = "Test", reminderDate = null)
        assertFalse(note.hasUpcomingReminder)
    }

    @Test
    fun `hasUpcomingReminder returns false when reminder is in the past`() {
        val note = NoteEntity(
            title = "Test",
            reminderDate = System.currentTimeMillis() - 3600000, // 1 hour ago
            isCompleted = false,
        )
        assertFalse(note.hasUpcomingReminder)
    }
}
