package com.voxn.ai.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.database.entity.HabitWithCompletions
import com.voxn.ai.data.database.entity.NoteEntity
import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.data.model.HealthData
import com.voxn.ai.manager.BudgetManager
import com.voxn.ai.manager.ExpenseParser
import com.voxn.ai.manager.HabitManager
import com.voxn.ai.manager.HealthConnectManager
import com.voxn.ai.manager.NoteManager
import com.voxn.ai.theme.VoxnColors
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class BriefLine(val icon: String, val text: String, val color: Color)
data class SmartAlert(val icon: String, val text: String, val color: Color)

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

    // --- Daily Brief ---
    fun generateDailyBrief(
        expenses: List<ExpenseEntity>,
        habits: List<HabitWithCompletions>,
        notes: List<NoteEntity>,
        healthData: HealthData,
    ): List<BriefLine> {
        val lines = mutableListOf<BriefLine>()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val fmt = DecimalFormat("#,###")

        // Tasks
        val activeNotes = notes.filter { !it.isCompleted }
        val overdue = activeNotes.count { it.isOverdue }
        val highPriority = activeNotes.count { it.priorityRaw == "High" }
        if (overdue > 0) {
            lines.add(BriefLine("⚠️", "$overdue overdue task${if (overdue > 1) "s" else ""} — take action", VoxnColors.alertRed))
        }
        if (highPriority > 0) {
            lines.add(BriefLine("🎯", "$highPriority high priority task${if (highPriority > 1) "s" else ""} pending", VoxnColors.warningOrange))
        }
        if (activeNotes.isEmpty()) {
            lines.add(BriefLine("✅", "All tasks complete — nice work!", VoxnColors.neonGreen))
        }

        // Spending
        val todayStart = ExpenseParser.todayStart()
        val yesterdayStart = todayStart - 86400000L
        val yesterdaySpend = expenses.filter { it.date in yesterdayStart until todayStart }.sumOf { it.amount }
        val todaySpend = expenses.filter { it.date >= todayStart }.sumOf { it.amount }
        if (yesterdaySpend > 0) {
            val topCat = expenses.filter { it.date in yesterdayStart until todayStart }
                .groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }?.key
            lines.add(BriefLine("💰", "Yesterday: ₹${fmt.format(yesterdaySpend.toLong())}${topCat?.let { " (mostly ${it.displayName})" } ?: ""}", VoxnColors.warningOrange))
        }
        if (todaySpend > 0) {
            lines.add(BriefLine("💳", "Spent ₹${fmt.format(todaySpend.toLong())} today so far", VoxnColors.electricBlue))
        }

        // Budget
        val monthSpend = expenses.filter { it.date >= ExpenseParser.monthStart() }.sumOf { it.amount }
        val budget = budgetManager.monthlyBudget.value
        if (budget > 0) {
            val pct = (monthSpend / budget * 100).toInt()
            if (pct >= 100) {
                lines.add(BriefLine("🚨", "Budget exceeded — ₹${fmt.format((monthSpend - budget).toLong())} over", VoxnColors.alertRed))
            } else if (pct >= 80) {
                lines.add(BriefLine("⚡", "Budget at $pct% — ₹${fmt.format((budget - monthSpend).toLong())} remaining", VoxnColors.warningOrange))
            }
        }

        // Health
        if (healthData.stepsProgress >= 1.0) {
            lines.add(BriefLine("🏆", "Step goal achieved! ${healthData.stepsFormatted} steps", VoxnColors.neonGreen))
        } else if (healthData.steps > 0) {
            lines.add(BriefLine("👟", "${healthData.stepsFormatted} steps — ${(healthData.stepsProgress * 100).toInt()}% of goal", VoxnColors.electricBlue))
        }
        if (healthData.sleepHours >= 7.0) {
            lines.add(BriefLine("😴", "Good rest: ${healthData.sleepFormatted} sleep", VoxnColors.cyan))
        } else if (healthData.sleepHours > 0) {
            lines.add(BriefLine("😴", "Only ${healthData.sleepFormatted} sleep — aim for 8h", VoxnColors.warningOrange))
        }

        // Habits
        val completedHabits = habits.count { it.isCompletedToday() }
        val totalHabits = habits.size
        if (totalHabits > 0) {
            if (completedHabits == totalHabits) {
                lines.add(BriefLine("🔥", "All $totalHabits habits done today!", VoxnColors.neonGreen))
            } else {
                val pending = totalHabits - completedHabits
                lines.add(BriefLine("📋", "$pending habit${if (pending > 1) "s" else ""} remaining today", VoxnColors.textSecondary))
            }
            val topStreak = habits.maxOfOrNull { it.currentStreak() } ?: 0
            if (topStreak >= 7) {
                lines.add(BriefLine("🔥", "$topStreak-day streak going strong!", VoxnColors.warningOrange))
            }
        }

        // Time-based recommendation
        when {
            hour in 5..9 -> lines.add(BriefLine("🌅", "Morning focus: tackle your high-priority tasks first", VoxnColors.electricBlue))
            hour in 10..13 -> lines.add(BriefLine("☀️", "Stay on track — review your spending and habits", VoxnColors.warningOrange))
            hour in 14..17 -> lines.add(BriefLine("🚶", "Afternoon check: how's your step count?", VoxnColors.neonGreen))
            hour in 18..21 -> lines.add(BriefLine("🌙", "Wind down — log any remaining expenses", VoxnColors.cyan))
            else -> lines.add(BriefLine("💤", "Rest well — tomorrow is a fresh start", VoxnColors.purple))
        }

        return lines
    }

    // --- Smart Alerts ---
    fun generateSmartAlerts(
        expenses: List<ExpenseEntity>,
        notes: List<NoteEntity>,
    ): List<SmartAlert> {
        val alerts = mutableListOf<SmartAlert>()
        val fmt = DecimalFormat("#,###")

        // Budget exceeded
        val monthSpend = expenses.filter { it.date >= ExpenseParser.monthStart() }.sumOf { it.amount }
        if (budgetManager.isBudgetExceeded(monthSpend)) {
            alerts.add(SmartAlert("🚨", "Monthly budget exceeded by ₹${fmt.format((monthSpend - budgetManager.monthlyBudget.value).toLong())}", VoxnColors.alertRed))
        }

        // Unusual spending (today > 2.5x daily average)
        val todaySpend = expenses.filter { it.date >= ExpenseParser.todayStart() }.sumOf { it.amount }
        val daysInMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val avgDaily = if (daysInMonth > 1) monthSpend / (daysInMonth - 1).coerceAtLeast(1) else monthSpend
        if (todaySpend > 0 && avgDaily > 0 && todaySpend > avgDaily * 2.5) {
            alerts.add(SmartAlert("⚡", "Unusual spending today: ₹${fmt.format(todaySpend.toLong())} (avg ₹${fmt.format(avgDaily.toLong())}/day)", VoxnColors.warningOrange))
        }

        // Inactivity (no expenses in 3+ days)
        val threeDaysAgo = System.currentTimeMillis() - 3 * 86400000L
        val recentExpenses = expenses.filter { it.date >= threeDaysAgo }
        if (recentExpenses.isEmpty() && expenses.isNotEmpty()) {
            alerts.add(SmartAlert("📝", "No expenses logged in 3+ days — are you tracking?", VoxnColors.electricBlue))
        }

        // Overdue tasks
        val overdue = notes.count { it.isOverdue }
        if (overdue > 0) {
            alerts.add(SmartAlert("⏰", "$overdue overdue task${if (overdue > 1) "s" else ""} need attention", VoxnColors.alertRed))
        }

        return alerts
    }

    fun refreshAll() {
        viewModelScope.launch { healthManager.fetchAllData() }
    }

    init { refreshAll() }
}
