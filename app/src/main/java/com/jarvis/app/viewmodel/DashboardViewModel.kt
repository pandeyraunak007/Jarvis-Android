package com.jarvis.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.app.data.database.entity.HabitWithCompletions
import com.jarvis.app.data.model.ExpenseCategory
import com.jarvis.app.data.model.HealthData
import com.jarvis.app.manager.ExpenseParser
import com.jarvis.app.manager.HabitManager
import com.jarvis.app.manager.HealthConnectManager
import com.jarvis.app.manager.NoteManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val healthManager = HealthConnectManager(app)
    private val habitManager = HabitManager(app)
    private val expenseParser = ExpenseParser(app)
    private val noteManager = NoteManager(app)

    val healthData: StateFlow<HealthData> = healthManager.healthData

    val habits: StateFlow<List<HabitWithCompletions>> = habitManager.habitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses = expenseParser.expensesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes = noteManager.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val greeting: String
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when {
                hour in 5..11 -> "Good Morning"
                hour in 12..16 -> "Good Afternoon"
                hour in 17..20 -> "Good Evening"
                else -> "Good Night"
            }
        }

    val currentDate: String
        get() = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())

    fun todaySpending(expenses: List<com.jarvis.app.data.database.entity.ExpenseEntity>): String {
        val today = ExpenseParser.todayStart()
        val amount = expenses.filter { it.date >= today }.sumOf { it.amount }
        return "₹${DecimalFormat("#,###").format(amount.toLong())}"
    }

    fun weeklySpending(expenses: List<com.jarvis.app.data.database.entity.ExpenseEntity>): String {
        val week = ExpenseParser.weekStart()
        val amount = expenses.filter { it.date >= week }.sumOf { it.amount }
        return "₹${DecimalFormat("#,###").format(amount.toLong())}"
    }

    fun monthlySpending(expenses: List<com.jarvis.app.data.database.entity.ExpenseEntity>): String {
        val month = ExpenseParser.monthStart()
        val amount = expenses.filter { it.date >= month }.sumOf { it.amount }
        return "₹${DecimalFormat("#,###").format(amount.toLong())}"
    }

    fun categoryBreakdown(expenses: List<com.jarvis.app.data.database.entity.ExpenseEntity>): List<Pair<ExpenseCategory, Double>> {
        val month = ExpenseParser.monthStart()
        return expenses.filter { it.date >= month }
            .groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(3)
    }

    fun refreshAll() {
        viewModelScope.launch { healthManager.fetchAllData() }
    }

    init { refreshAll() }
}
