package com.voxn.ai.widget

import android.content.Context
import com.voxn.ai.data.database.VoxnDatabase
import com.voxn.ai.manager.BudgetManager
import com.voxn.ai.manager.ExpenseParser
import com.voxn.ai.manager.UserProfileManager
import kotlinx.coroutines.flow.first
import java.text.DecimalFormat
import java.util.Calendar

data class WidgetData(
    val userName: String = "",
    val todaySpend: Double = 0.0,
    val monthlySpend: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val habitsCompleted: Int = 0,
    val habitsDueToday: Int = 0,
    val longestStreak: Int = 0,
) {
    val todayFormatted: String get() = "₹${DecimalFormat("#,##0").format(todaySpend.toLong())}"
    val monthlyFormatted: String get() = "₹${DecimalFormat("#,##0").format(monthlySpend.toLong())}"
    val budgetFormatted: String get() = "₹${DecimalFormat("#,##0").format(monthlyBudget.toLong())}"
    val budgetProgress: Float get() = if (monthlyBudget > 0) (monthlySpend / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
    val budgetExceeded: Boolean get() = monthlyBudget > 0 && monthlySpend > monthlyBudget

    val greeting: String
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val name = if (userName.isNotBlank()) ", $userName" else ""
            return when {
                hour in 5..11 -> "Good Morning$name"
                hour in 12..16 -> "Good Afternoon$name"
                hour in 17..20 -> "Good Evening$name"
                else -> "Good Night$name"
            }
        }
}

suspend fun readWidgetData(context: Context): WidgetData {
    val db = VoxnDatabase.getInstance(context)
    val profileManager = UserProfileManager(context)
    val budgetManager = BudgetManager(context)

    val expenses = db.expenseDao().getAll().first()
    val habits = db.habitDao().getAllWithCompletions().first()

    val todayStart = ExpenseParser.todayStart()
    val monthStart = ExpenseParser.monthStart()

    return WidgetData(
        userName = profileManager.userName.value,
        todaySpend = expenses.filter { it.date >= todayStart }.sumOf { it.amount },
        monthlySpend = expenses.filter { it.date >= monthStart }.sumOf { it.amount },
        monthlyBudget = budgetManager.monthlyBudget.value,
        habitsCompleted = habits.count { it.isCompletedToday() },
        habitsDueToday = habits.count { it.isDueToday() },
        longestStreak = habits.maxOfOrNull { it.currentStreak() } ?: 0,
    )
}
