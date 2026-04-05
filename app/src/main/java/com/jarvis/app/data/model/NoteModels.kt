package com.jarvis.app.data.model

import androidx.compose.ui.graphics.Color
import com.jarvis.app.theme.JarvisColors

enum class NoteCategory(val displayName: String, val icon: String, val color: Color) {
    Personal("Personal", "person", JarvisColors.electricBlue),
    Office("Office", "work", JarvisColors.warningOrange);

    companion object {
        fun fromString(raw: String): NoteCategory =
            entries.find { it.name.equals(raw, ignoreCase = true) } ?: Personal
    }
}

enum class NotePriority(val displayName: String, val icon: String, val color: Color, val sortOrder: Int) {
    High("High", "warning", JarvisColors.alertRed, 0),
    Medium("Medium", "remove_circle", JarvisColors.warningOrange, 1),
    Low("Low", "arrow_downward", JarvisColors.neonGreen, 2);

    companion object {
        fun fromString(raw: String): NotePriority =
            entries.find { it.name.equals(raw, ignoreCase = true) } ?: Medium
    }
}
