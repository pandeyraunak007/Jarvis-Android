package com.voxn.ai.manager

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.voxn.ai.data.database.VoxnDatabase
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.util.NotificationReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class SmartNotificationManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_smart_notifs", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val ENABLED_KEY = "smart_notifs_enabled"
        private const val BUDGET_FIRED_KEY = "budget_fired_month"
        private const val ID_MORNING = 9001
        private const val ID_EXPENSE_REMINDER = 9002
        private const val ID_EVENING_HABITS = 9003
        private const val ID_BUDGET_WARNING = 9004
        private const val ID_SPIKE_BASE = 9100
    }

    var isEnabled: Boolean
        get() = prefs.getBoolean(ENABLED_KEY, true)
        set(value) = prefs.edit().putBoolean(ENABLED_KEY, value).apply()

    suspend fun refresh() {
        if (!isEnabled) { removeAll(); return }
        try {
            scheduleMorningBrief()
            scheduleExpenseReminder()
            scheduleEveningHabitCheck()
            checkBudgetWarning()
        } catch (_: Exception) { }
    }

    fun removeAll() {
        listOf(ID_MORNING, ID_EXPENSE_REMINDER, ID_EVENING_HABITS, ID_BUDGET_WARNING).forEach { id ->
            val intent = Intent(context, NotificationReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context, id, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pending?.let { alarmManager.cancel(it) }
        }
    }

    // MARK: 1. Morning Brief (8:00 AM)
    private suspend fun scheduleMorningBrief() {
        val db = VoxnDatabase.getInstance(context)
        val habits = withContext(Dispatchers.IO) { db.habitDao().getAllWithCompletions().first() }
        val expenses = withContext(Dispatchers.IO) { db.expenseDao().getAll().first() }

        val dueToday = habits.count { it.isDueToday() }
        val budgetMgr = BudgetManager(context)
        val budget = budgetMgr.monthlyBudget.value
        val monthStart = ExpenseParser.monthStart()
        val monthlySpend = expenses.filter { it.date >= monthStart }.sumOf { it.amount }

        val lines = mutableListOf<String>()
        if (dueToday > 0) lines.add("$dueToday habit${if (dueToday > 1) "s" else ""} to complete")
        if (budget > 0) {
            val pct = ((monthlySpend / budget) * 100).toInt()
            lines.add("₹${monthlySpend.toInt()} of ₹${budget.toInt()} budget ($pct%)")
        }
        if (lines.isEmpty()) return

        scheduleAt(8, 0, ID_MORNING, "Good Morning — Voxn Brief", lines.joinToString(" · "), repeating = true)
    }

    // MARK: 2. Expense Reminder (1:00 PM)
    private suspend fun scheduleExpenseReminder() {
        val db = VoxnDatabase.getInstance(context)
        val expenses = withContext(Dispatchers.IO) { db.expenseDao().getAll().first() }
        val todayStart = ExpenseParser.todayStart()
        val todaySpend = expenses.filter { it.date >= todayStart }.sumOf { it.amount }
        if (todaySpend > 0) return

        scheduleAt(13, 0, ID_EXPENSE_REMINDER, "Voxn AI", "You haven't logged any expenses today. Tap to add one before you forget.")
    }

    // MARK: 3. Evening Habit Check (8:00 PM)
    private suspend fun scheduleEveningHabitCheck() {
        val db = VoxnDatabase.getInstance(context)
        val habits = withContext(Dispatchers.IO) { db.habitDao().getAllWithCompletions().first() }
        val pending = habits.filter { it.isDueToday() && !it.isCompletedToday() }
        if (pending.isEmpty()) return

        val now = Calendar.getInstance()
        if (now.get(Calendar.HOUR_OF_DAY) >= 20) return

        val names = pending.take(3).joinToString(", ") { it.habit.name }
        val suffix = if (pending.size > 3) " and ${pending.size - 3} more" else ""
        var body = "${pending.size} habit${if (pending.size > 1) "s" else ""} still pending: $names$suffix."

        val bestStreak = pending.filter { it.currentStreak() > 0 }.maxByOrNull { it.currentStreak() }
        if (bestStreak != null) {
            body += " Your ${bestStreak.currentStreak()}-day ${bestStreak.habit.name} streak is at risk!"
        }

        scheduleAt(20, 0, ID_EVENING_HABITS, "Voxn AI — Habit Check", body)
    }

    // MARK: 4. Budget Warning (immediate)
    private suspend fun checkBudgetWarning() {
        val budgetMgr = BudgetManager(context)
        val budget = budgetMgr.monthlyBudget.value
        if (budget <= 0) return

        val db = VoxnDatabase.getInstance(context)
        val expenses = withContext(Dispatchers.IO) { db.expenseDao().getAll().first() }
        val monthStart = ExpenseParser.monthStart()
        val spent = expenses.filter { it.date >= monthStart }.sumOf { it.amount }
        val pct = spent / budget

        if (pct < 0.8) return

        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val threshold = if (pct >= 1.0) "exceeded" else "80pct"
        val firedKey = "$monthKey-$threshold"
        if (prefs.getString(BUDGET_FIRED_KEY, "") == firedKey) return

        val title = if (pct >= 1.0) "Budget Exceeded" else "Budget Alert — ${(pct * 100).toInt()}% Used"
        val body = if (pct >= 1.0)
            "You've spent ₹${spent.toInt()} — over your ₹${budget.toInt()} monthly budget."
        else
            "₹${spent.toInt()} of ₹${budget.toInt()} spent. ₹${(budget - spent).toInt()} remaining this month."

        fireImmediate(ID_BUDGET_WARNING, title, body)
        prefs.edit().putString(BUDGET_FIRED_KEY, firedKey).apply()
    }

    // MARK: 5. Spending Spike (called from ExpenseParser)
    fun checkSpendingSpike(amount: Double, merchant: String, avgDaily: Double) {
        if (!isEnabled || avgDaily <= 0 || amount <= avgDaily * 2.5) return

        val multiplier = String.format("%.1f", amount / avgDaily)
        val body = "₹${amount.toInt()} at $merchant is ${multiplier}x your daily average of ₹${avgDaily.toInt()}."
        val spikeId = ID_SPIKE_BASE + (System.currentTimeMillis() % 100).toInt()
        fireImmediate(spikeId, "Unusual Spend Detected", body)
    }

    // MARK: Helpers

    private fun scheduleAt(hour: Int, minute: Int, id: Int, title: String, message: String, repeating: Boolean = false) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("id", id)
            putExtra("type", "smart")
        }
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (repeating) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
        }
    }

    private fun fireImmediate(id: Int, title: String, message: String) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("message", message)
            putExtra("id", id)
            putExtra("type", "smart")
        }
        val pending = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1000,
            pending
        )
    }
}
