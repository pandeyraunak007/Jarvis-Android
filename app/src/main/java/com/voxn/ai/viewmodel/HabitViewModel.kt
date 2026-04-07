package com.voxn.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxn.ai.data.database.entity.HabitEntity
import com.voxn.ai.data.database.entity.HabitWithCompletions
import com.voxn.ai.manager.HabitManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HabitViewModel(app: Application) : AndroidViewModel(app) {
    private val manager = HabitManager(app)

    val habits: StateFlow<List<HabitWithCompletions>> = manager.habitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())
    val selectedMonth: StateFlow<Calendar> = _selectedMonth

    fun showAddHabit() { _showAddDialog.value = true }
    fun hideAddHabit() { _showAddDialog.value = false }

    fun addHabit(name: String, reminderEnabled: Boolean, hour: Int, minute: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            manager.addHabit(name, reminderEnabled, hour, minute)
            _showAddDialog.value = false
        }
    }

    fun toggleCompletion(habit: HabitWithCompletions) {
        viewModelScope.launch { manager.toggleCompletion(habit) }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch { manager.deleteHabit(habit) }
    }

    fun updateReminder(habit: HabitEntity, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch { manager.updateReminder(habit, enabled, hour, minute) }
    }

    fun previousMonth() {
        _selectedMonth.value = (selectedMonth.value.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
    }

    fun nextMonth() {
        _selectedMonth.value = (selectedMonth.value.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    }

    val completedCount: Int get() = habits.value.count { it.isCompletedToday() }
    val totalCount: Int get() = habits.value.size
    val completionRate: Double get() = if (totalCount > 0) completedCount.toDouble() / totalCount else 0.0
    val longestStreak: Int get() = habits.value.maxOfOrNull { it.currentStreak() } ?: 0
}
