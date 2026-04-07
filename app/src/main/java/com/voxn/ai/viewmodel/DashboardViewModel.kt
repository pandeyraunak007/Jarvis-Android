package com.voxn.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.database.entity.HabitWithCompletions
import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.data.model.HealthData
import com.voxn.ai.manager.BudgetManager
import com.voxn.ai.manager.ExpenseParser
import com.voxn.ai.manager.HabitManager
import com.voxn.ai.manager.HealthConnectManager
import com.voxn.ai.manager.NoteManager
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
    val budgetManager = BudgetManager(app)

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

    fun todaySpending(expenses: List<ExpenseEntity>): String {
        val today = ExpenseParser.todayStart()
        val amount = expenses.filter { it.date >= today }.sumOf { it.amount }
        return "₹${DecimalFormat("#,###").format(amount.toLong())}"
    }

    fun weeklySpending(expenses: List<ExpenseEntity>): String {
        val week = ExpenseParser.weekStart()
        val amount = expenses.filter { it.date >= week }.sumOf { it.amount }
        return "₹${DecimalFormat("#,###").format(amount.toLong())}"
    }

    fun monthlySpending(expenses: List<ExpenseEntity>): String {
        val month = ExpenseParser.monthStart()
        val amount = expenses.filter { it.date >= month }.sumOf { it.amount }
        return "₹${DecimalFormat("#,###").format(amount.toLong())}"
    }

    fun categoryBreakdown(expenses: List<ExpenseEntity>): List<Pair<ExpenseCategory, Double>> {
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
