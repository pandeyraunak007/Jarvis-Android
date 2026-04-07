package com.voxn.ai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.voxn.ai.data.model.NoteCategory
import com.voxn.ai.data.model.NotePriority
import java.util.UUID

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val categoryRaw: String = "Personal",
    val priorityRaw: String = "Medium",
    val dueDate: Long? = null,
    val reminderDate: Long? = null,
    val isPinned: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val category: NoteCategory get() = NoteCategory.fromString(categoryRaw)
    val priority: NotePriority get() = NotePriority.fromString(priorityRaw)

    val isOverdue: Boolean
        get() = dueDate != null && dueDate < System.currentTimeMillis() && !isCompleted

    val hasUpcomingReminder: Boolean
        get() {
            if (reminderDate == null || isCompleted) return false
            val now = System.currentTimeMillis()
            return reminderDate > now && reminderDate - now < 24 * 60 * 60 * 1000
        }
}
