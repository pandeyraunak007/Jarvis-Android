package com.voxn.ai.manager

import android.content.Context
import android.content.SharedPreferences
import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.data.model.PaymentMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class RecurringExpense(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: Double,
    val category: ExpenseCategory,
    val dayOfMonth: Int,
)

class RecurringExpenseManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("voxn_recurring", Context.MODE_PRIVATE)

    private val _recurringExpenses = MutableStateFlow<List<RecurringExpense>>(emptyList())
    val recurringExpenses: StateFlow<List<RecurringExpense>> = _recurringExpenses

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val json = prefs.getString("recurring_expenses", "[]") ?: "[]"
        val list = mutableListOf<RecurringExpense>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                RecurringExpense(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    amount = obj.getDouble("amount"),
                    category = try { ExpenseCategory.valueOf(obj.getString("category")) } catch (_: Exception) { ExpenseCategory.Other },
                    dayOfMonth = obj.getInt("dayOfMonth"),
                )
            )
        }
        _recurringExpenses.value = list
    }

    private fun saveToPrefs() {
        val arr = JSONArray()
        _recurringExpenses.value.forEach { re ->
            arr.put(JSONObject().apply {
                put("id", re.id)
                put("name", re.name)
                put("amount", re.amount)
                put("category", re.category.name)
                put("dayOfMonth", re.dayOfMonth)
            })
        }
        prefs.edit().putString("recurring_expenses", arr.toString()).apply()
    }

    fun addRecurringExpense(name: String, amount: Double, category: ExpenseCategory, dayOfMonth: Int) {
        val expense = RecurringExpense(name = name, amount = amount, category = category, dayOfMonth = dayOfMonth)
        _recurringExpenses.value = _recurringExpenses.value + expense
        saveToPrefs()
    }

    fun removeRecurringExpense(id: String) {
        _recurringExpenses.value = _recurringExpenses.value.filter { it.id != id }
        saveToPrefs()
    }

    /**
     * Auto-log recurring expenses that are due today.
     * Call this on app launch. Checks if today's day matches and hasn't been logged this month.
     */
    suspend fun autoLogIfNeeded(expenseParser: ExpenseParser) {
        val today = Calendar.getInstance()
        val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)
        val monthKey = "${today.get(Calendar.YEAR)}_${today.get(Calendar.MONTH)}"

        _recurringExpenses.value.forEach { re ->
            if (re.dayOfMonth == dayOfMonth) {
                val loggedKey = "logged_${re.id}_$monthKey"
                if (!prefs.getBoolean(loggedKey, false)) {
                    expenseParser.addExpense(
                        amount = re.amount,
                        merchant = re.name,
                        category = re.category,
                        paymentMethod = PaymentMethod.Other,
                    )
                    prefs.edit().putBoolean(loggedKey, true).apply()
                }
            }
        }
    }
}
