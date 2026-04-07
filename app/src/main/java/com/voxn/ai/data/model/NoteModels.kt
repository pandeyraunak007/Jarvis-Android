package com.voxn.ai.data.model

import androidx.compose.ui.graphics.Color
import com.voxn.ai.theme.VoxnColors

enum class NoteCategory(val displayName: String, val icon: String, val color: Color) {
    Personal("Personal", "person", VoxnColors.electricBlue),
    Office("Office", "work", VoxnColors.warningOrange);

    companion object {
        fun fromString(raw: String): NoteCategory =
            entries.find { it.name.equals(raw, ignoreCase = true) } ?: Personal
    }
}

enum class NotePriority(val displayName: String, val icon: String, val color: Color, val sortOrder: Int) {
    High("High", "warning", VoxnColors.alertRed, 0),
    Medium("Medium", "remove_circle", VoxnColors.warningOrange, 1),
    Low("Low", "arrow_downward", VoxnColors.neonGreen, 2);

    companion object {
        fun fromString(raw: String): NotePriority =
            entries.find { it.name.equals(raw, ignoreCase = true) } ?: Medium
    }
}
