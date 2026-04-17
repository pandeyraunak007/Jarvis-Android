package com.voxn.ai.service

import android.content.Context
import com.voxn.ai.data.database.VoxnDatabase
import com.voxn.ai.data.database.entity.HabitWithCompletions
import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.data.model.PaymentMethod
import com.voxn.ai.manager.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object ChatToolCatalog {
    fun tools(): JSONArray = JSONArray().apply {
        put(tool("get_today_summary", "Snapshot of today: spending, health, habits, events.", obj()))
        put(tool("get_spending", "Spending totals and category breakdown. INR (₹).",
            obj("range" to prop("string", "Time range", listOf("today", "week", "month", "all"))), listOf("range")))
        put(tool("get_habits", "All habits with streak, completion status, frequency.", obj()))
        put(tool("get_health", "Today's HealthKit metrics: steps, sleep, calories, workout.", obj()))
        put(tool("get_notes", "Recent notes and reminders.",
            obj("limit" to prop("integer", "Max notes, default 10"))))
        put(tool("get_calendar", "Calendar events from device calendars.",
            obj("day" to prop("string", "today, tomorrow, day_after_tomorrow, or ISO date"))))
        put(tool("log_expense", "Log a new expense. Confirm in reply.",
            obj(
                "amount" to prop("number", "Amount in INR"),
                "merchant" to prop("string", "Merchant or description"),
                "category" to prop("string", "Category", listOf("Grocery", "Food", "Entertainment", "Investment", "Travel", "Bills", "Others")),
            ), listOf("amount", "merchant", "category")))
    }

    private fun tool(name: String, desc: String, params: JSONObject, required: List<String>? = null): JSONObject {
        val schema = JSONObject().put("type", "object").put("properties", params)
        if (required != null) schema.put("required", JSONArray(required))
        return JSONObject()
            .put("type", "function")
            .put("function", JSONObject().put("name", name).put("description", desc).put("parameters", schema))
    }

    private fun obj(vararg pairs: Pair<String, JSONObject>): JSONObject = JSONObject().apply {
        pairs.forEach { put(it.first, it.second) }
    }

    private fun prop(type: String, desc: String, enum_: List<String>? = null): JSONObject =
        JSONObject().put("type", type).put("description", desc).apply {
            if (enum_ != null) put("enum", JSONArray(enum_))
        }
}

class ChatToolDispatcher(private val context: Context) {

    private val db by lazy { VoxnDatabase.getInstance(context) }
    private val expenseParser by lazy { ExpenseParser(context) }
    private val calendarManager by lazy { CalendarManager(context) }
    private val healthManager by lazy { HealthConnectManager(context) }

    suspend fun execute(name: String, argsJson: String): String {
        val args = try { JSONObject(argsJson) } catch (_: Exception) { JSONObject() }
        return when (name) {
            "get_today_summary" -> todaySummary()
            "get_spending" -> spending(args.optString("range", "today"))
            "get_habits" -> habits()
            "get_health" -> health()
            "get_notes" -> notes(args.optInt("limit", 10))
            "get_calendar" -> calendar(args.optString("day", "today"))
            "log_expense" -> logExpense(args)
            else -> JSONObject().put("error", "Unknown tool: $name").toString()
        }
    }

    private suspend fun todaySummary(): String {
        val habits = db.habitDao().getAllWithCompletions().first()
        val expenses = db.expenseDao().getAll().first()
        val dueToday = habits.filter { it.isDueToday() }
        val completed = dueToday.count { it.isCompletedToday() }
        val remaining = dueToday.filter { !it.isCompletedToday() }.map { it.habit.name }

        val todayStart = ExpenseParser.todayStart()
        val monthStart = ExpenseParser.monthStart()
        val todaySpend = expenses.filter { it.date >= todayStart  }.sumOf { it.amount }
        val monthSpend = expenses.filter { it.date >= monthStart  }.sumOf { it.amount }

        val hd = healthManager.healthData.value
        val events = calendarManager.fetchEventsForDate(System.currentTimeMillis())

        return JSONObject().apply {
            put("date", isoDate(System.currentTimeMillis()))
            put("user_name", UserProfileManager(context).userName.value)
            put("today_spending_inr", todaySpend.toInt())
            put("month_spending_inr", monthSpend.toInt())
            put("steps", hd.steps)
            put("sleep_hours", String.format("%.1f", hd.sleepHours).toDouble())
            put("calories_burned", hd.caloriesBurned.toInt())
            put("workout_minutes", hd.workoutMinutes.toInt())
            put("habits_completed_today", completed)
            put("habits_due_today", dueToday.size)
            put("habits_remaining_today", JSONArray(remaining))
            put("events_today_count", events.size)
        }.toString()
    }

    private suspend fun spending(range: String): String {
        val expenses = db.expenseDao().getAll().first()
        val now = System.currentTimeMillis()
        val (start, label) = when (range.lowercase()) {
            "today" -> ExpenseParser.todayStart() to "today"
            "week" -> ExpenseParser.weekStart() to "last 7 days"
            "month" -> ExpenseParser.monthStart() to "this month"
            "all" -> 0L to "all time"
            else -> ExpenseParser.todayStart() to "today"
        }
        val inRange = expenses.filter { it.date >= start  }
        val total = inRange.sumOf { it.amount }
        val byCategory = JSONObject()
        inRange.groupBy { it.categoryRaw }.forEach { (cat, list) ->
            byCategory.put(cat, list.sumOf { it.amount }.toInt())
        }
        return JSONObject().apply {
            put("range", label)
            put("total_inr", total.toInt())
            put("count", inRange.size)
            put("by_category", byCategory)
        }.toString()
    }

    private suspend fun habits(): String {
        val list = db.habitDao().getAllWithCompletions().first()
        val arr = JSONArray()
        list.forEach { h ->
            arr.put(JSONObject().apply {
                put("name", h.habit.name)
                put("frequency", h.habit.frequencyRaw)
                put("due_today", h.isDueToday())
                put("completed_today", h.isCompletedToday())
                put("streak", h.currentStreak())
            })
        }
        return JSONObject().put("count", list.size).put("habits", arr).toString()
    }

    private fun health(): String {
        val d = healthManager.healthData.value
        return JSONObject().apply {
            put("steps", d.steps)
            put("sleep_hours", String.format("%.1f", d.sleepHours).toDouble())
            put("calories_burned", d.caloriesBurned.toInt())
            put("workout_minutes", d.workoutMinutes.toInt())
        }.toString()
    }

    private suspend fun notes(limit: Int): String {
        val notes = db.noteDao().getAll().first()
        val arr = JSONArray()
        notes.take(limit.coerceIn(1, 50)).forEach { n ->
            arr.put(JSONObject().apply {
                put("title", n.title)
                put("body", n.body.take(200))
                put("priority", n.priorityRaw)
                put("category", n.categoryRaw)
                put("is_completed", n.isCompleted)
            })
        }
        return JSONObject().put("count", arr.length()).put("notes", arr).toString()
    }

    private suspend fun calendar(day: String): String {
        val cal = Calendar.getInstance()
        when (day.lowercase()) {
            "tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 1)
            "day_after_tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 2)
            "today", "" -> { }
            else -> {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.parse(day)?.let { cal.time = it }
                } catch (_: Exception) { }
            }
        }
        val dateLabel = SimpleDateFormat("EEEE, MMM d yyyy", Locale.getDefault()).format(cal.time)
        val events = calendarManager.fetchEventsForDate(cal.timeInMillis)
        val arr = JSONArray()
        events.forEach { e ->
            arr.put(JSONObject().apply {
                put("title", e.title)
                put("time", e.timeRange)
                put("all_day", e.isAllDay)
                put("location", e.location ?: JSONObject.NULL)
                put("is_ongoing", e.isOngoing)
            })
        }
        return JSONObject().apply {
            put("authorized", calendarManager.hasPermission.value)
            put("day", dateLabel)
            put("count", arr.length())
            put("events", arr)
        }.toString()
    }

    private suspend fun logExpense(args: JSONObject): String {
        val amount = args.optDouble("amount", -1.0)
        val merchant = args.optString("merchant", "")
        val categoryStr = args.optString("category", "Others")
        if (amount <= 0 || merchant.isEmpty()) {
            return JSONObject().put("error", "Missing amount or merchant").toString()
        }
        val category = try { ExpenseCategory.valueOf(categoryStr) } catch (_: Exception) { ExpenseCategory.Other }
        expenseParser.addExpense(amount, merchant, category, PaymentMethod.Other)
        return JSONObject().apply {
            put("success", true)
            put("logged", JSONObject().apply {
                put("amount_inr", amount.toInt())
                put("merchant", merchant)
                put("category", category.name)
            })
        }.toString()
    }

    private fun isoDate(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault()).format(Date(millis))
}
