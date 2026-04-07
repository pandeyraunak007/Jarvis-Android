package com.voxn.ai.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BudgetManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("voxn_budget", Context.MODE_PRIVATE)

    private val _monthlyBudget = MutableStateFlow(prefs.getFloat("monthly_budget", 0f).toDouble())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget

    private val _savingsGoal = MutableStateFlow(prefs.getFloat("savings_goal", 0f).toDouble())
    val savingsGoal: StateFlow<Double> = _savingsGoal

    fun setMonthlyBudget(amount: Double) {
        _monthlyBudget.value = amount
        prefs.edit().putFloat("monthly_budget", amount.toFloat()).apply()
    }

    fun setSavingsGoal(amount: Double) {
        _savingsGoal.value = amount
        prefs.edit().putFloat("savings_goal", amount.toFloat()).apply()
    }

    fun budgetProgress(monthlySpend: Double): Double {
        val budget = _monthlyBudget.value
        if (budget <= 0) return 0.0
        return (monthlySpend / budget).coerceIn(0.0, 1.5)
    }

    fun savingsProgress(monthlySpend: Double, monthlyIncome: Double): Double {
        val goal = _savingsGoal.value
        if (goal <= 0) return 0.0
        val saved = (monthlyIncome - monthlySpend).coerceAtLeast(0.0)
        return (saved / goal).coerceIn(0.0, 1.5)
    }

    fun isBudgetExceeded(monthlySpend: Double): Boolean {
        val budget = _monthlyBudget.value
        return budget > 0 && monthlySpend > budget
    }

    fun budgetRemaining(monthlySpend: Double): Double {
        val budget = _monthlyBudget.value
        return (budget - monthlySpend).coerceAtLeast(0.0)
    }
}
