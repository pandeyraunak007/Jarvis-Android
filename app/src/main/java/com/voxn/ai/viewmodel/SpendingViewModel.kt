package com.voxn.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.data.model.PaymentMethod
import com.voxn.ai.manager.ExpenseParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

enum class SpendingTimeRange(val label: String) {
    ThisMonth("This Month"),
    LastMonth("Last Month"),
    ThreeMonths("3 Months"),
    SixMonths("6 Months"),
    Custom("Custom");
}

data class TransactionGroup(
    val date: Long,
    val label: String,
    val dailyTotal: Double,
    val transactions: List<ExpenseEntity>,
)

class SpendingViewModel(app: Application) : AndroidViewModel(app) {
    private val parser = ExpenseParser(app)

    val expenses: StateFlow<List<ExpenseEntity>> = parser.expensesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _showParseSheet = MutableStateFlow(false)
    val showParseSheet: StateFlow<Boolean> = _showParseSheet

    private val _parseResult = MutableStateFlow<String?>(null)
    val parseResult: StateFlow<String?> = _parseResult

    // Time range state
    private val _selectedTimeRange = MutableStateFlow(SpendingTimeRange.ThisMonth)
    val selectedTimeRange: StateFlow<SpendingTimeRange> = _selectedTimeRange

    private val _customStartDate = MutableStateFlow(ExpenseParser.monthStart())
    val customStartDate: StateFlow<Long> = _customStartDate

    private val _customEndDate = MutableStateFlow(System.currentTimeMillis())
    val customEndDate: StateFlow<Long> = _customEndDate

    private val _showCustomDatePicker = MutableStateFlow(false)
    val showCustomDatePicker: StateFlow<Boolean> = _showCustomDatePicker

    // Filters
    private val _filterSearchText = MutableStateFlow("")
    val filterSearchText: StateFlow<String> = _filterSearchText

    private val _filterCategory = MutableStateFlow<ExpenseCategory?>(null)
    val filterCategory: StateFlow<ExpenseCategory?> = _filterCategory

    fun showAddExpense() { _showAddDialog.value = true }
    fun hideAddExpense() { _showAddDialog.value = false }
    fun showParse() { _showParseSheet.value = true }
    fun hideParse() { _showParseSheet.value = false }

    fun selectTimeRange(range: SpendingTimeRange) {
        if (range == SpendingTimeRange.Custom) {
            _showCustomDatePicker.value = true
        } else {
            _selectedTimeRange.value = range
        }
    }

    fun setCustomStartDate(millis: Long) { _customStartDate.value = millis }
    fun setCustomEndDate(millis: Long) { _customEndDate.value = millis }

    fun applyCustomRange() {
        _selectedTimeRange.value = SpendingTimeRange.Custom
        _showCustomDatePicker.value = false
    }

    fun dismissCustomDatePicker() { _showCustomDatePicker.value = false }

    fun setFilterSearch(text: String) { _filterSearchText.value = text }
    fun setFilterCategory(cat: ExpenseCategory?) { _filterCategory.value = cat }
    fun clearFilters() { _filterSearchText.value = ""; _filterCategory.value = null }
    fun hasActiveFilters(): Boolean = _filterSearchText.value.isNotEmpty() || _filterCategory.value != null

    // Date range calculation
    fun dateRangeStart(): Long {
        val cal = Calendar.getInstance()
        return when (_selectedTimeRange.value) {
            SpendingTimeRange.ThisMonth -> ExpenseParser.monthStart()
            SpendingTimeRange.LastMonth -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            SpendingTimeRange.ThreeMonths -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -2)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            SpendingTimeRange.SixMonths -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -5)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            SpendingTimeRange.Custom -> _customStartDate.value
        }
    }

    fun dateRangeEnd(): Long {
        return when (_selectedTimeRange.value) {
            SpendingTimeRange.LastMonth -> ExpenseParser.monthStart() - 1
            SpendingTimeRange.Custom -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = _customEndDate.value
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59)
                cal.timeInMillis
            }
            else -> System.currentTimeMillis()
        }
    }

    fun dateRangeLabel(): String {
        val df = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val mf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return when (_selectedTimeRange.value) {
            SpendingTimeRange.ThisMonth -> mf.format(Date())
            SpendingTimeRange.LastMonth -> mf.format(Date(dateRangeStart()))
            SpendingTimeRange.ThreeMonths, SpendingTimeRange.SixMonths ->
                "${df.format(Date(dateRangeStart()))} — ${df.format(Date())}"
            SpendingTimeRange.Custom ->
                "${df.format(Date(_customStartDate.value))} — ${df.format(Date(_customEndDate.value))}"
        }
    }

    // Range-filtered expenses
    fun rangeFilteredExpenses(): List<ExpenseEntity> {
        val start = dateRangeStart()
        val end = dateRangeEnd()
        return expenses.value.filter { it.date in start..end }
    }

    // Aggregations for selected range
    fun totalSpend(): Double = rangeFilteredExpenses().sumOf { it.amount }
    fun totalTransactionCount(): Int = rangeFilteredExpenses().size

    // Always-current quick stats
    fun todaySpending(): Double {
        val today = ExpenseParser.todayStart()
        return expenses.value.filter { it.date >= today }.sumOf { it.amount }
    }

    fun weeklySpending(): Double {
        val week = ExpenseParser.weekStart()
        return expenses.value.filter { it.date >= week }.sumOf { it.amount }
    }

    fun monthlySpending(): Double {
        val month = ExpenseParser.monthStart()
        return expenses.value.filter { it.date >= month }.sumOf { it.amount }
    }

    // Category breakdown filtered by range
    fun categoryBreakdown(): List<Pair<ExpenseCategory, Double>> {
        return rangeFilteredExpenses()
            .groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }

    // Filtered + grouped transactions
    fun filteredTransactions(): List<ExpenseEntity> {
        var result = rangeFilteredExpenses()
        val cat = _filterCategory.value
        if (cat != null) {
            result = result.filter { it.category == cat }
        }
        val search = _filterSearchText.value.lowercase()
        if (search.isNotEmpty()) {
            result = result.filter {
                it.merchant.lowercase().contains(search) ||
                (it.note?.lowercase()?.contains(search) == true) ||
                it.category.displayName.lowercase().contains(search)
            }
        }
        return result.sortedByDescending { it.date }
    }

    fun groupedTransactions(): List<TransactionGroup> {
        val cal = Calendar.getInstance()
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        return filteredTransactions()
            .groupBy { expense ->
                cal.timeInMillis = expense.date
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            .entries
            .sortedByDescending { it.key }
            .map { (dayMillis, transactions) ->
                cal.timeInMillis = dayMillis
                val label = when {
                    cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) -> "TODAY"
                    cal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) -> "YESTERDAY"
                    else -> dateFormat.format(Date(dayMillis)).uppercase()
                }
                val dailyTotal = transactions.sumOf { it.amount }
                TransactionGroup(dayMillis, label, dailyTotal, transactions.sortedByDescending { it.date })
            }
    }

    fun parseAndAdd(text: String) {
        viewModelScope.launch {
            val success = parser.addFromNotification(text)
            _parseResult.value = if (success) "Transaction logged" else "Could not parse"
            kotlinx.coroutines.delay(2000)
            _parseResult.value = null
        }
    }

    fun addExpense(amount: Double, merchant: String, category: ExpenseCategory, paymentMethod: PaymentMethod, date: Long) {
        if (amount <= 0) return
        viewModelScope.launch {
            parser.addExpense(amount, merchant, category, paymentMethod, date)
            _showAddDialog.value = false
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch { parser.deleteExpense(expense) }
    }

    fun formatAmount(amount: Double): String = "₹${DecimalFormat("#,###").format(amount.toLong())}"
}
