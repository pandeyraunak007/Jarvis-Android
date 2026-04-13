package com.voxn.ai.ui.spending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.data.model.PaymentMethod
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.GlassCard
import com.voxn.ai.viewmodel.SpendingTimeRange
import com.voxn.ai.viewmodel.SpendingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingScreen(viewModel: SpendingViewModel = viewModel()) {
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val showParseSheet by viewModel.showParseSheet.collectAsStateWithLifecycle()
    val parseResult by viewModel.parseResult.collectAsStateWithLifecycle()
    val selectedRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()
    val showCustomPicker by viewModel.showCustomDatePicker.collectAsStateWithLifecycle()
    val filterSearch by viewModel.filterSearchText.collectAsStateWithLifecycle()
    val filterCat by viewModel.filterCategory.collectAsStateWithLifecycle()
    val expenseToDelete by viewModel.expenseToDelete.collectAsStateWithLifecycle()
    val monthlyBudget by viewModel.budgetManager.monthlyBudget.collectAsStateWithLifecycle()
    val savingsGoal by viewModel.budgetManager.savingsGoal.collectAsStateWithLifecycle()
    val recurringExpenses by viewModel.recurringManager.recurringExpenses.collectAsStateWithLifecycle()
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showRecurringDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(VoxnColors.backgroundDark, VoxnColors.backgroundMid, VoxnColors.backgroundDark)))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Time Range Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SpendingTimeRange.entries.forEach { range ->
                        val isActive = selectedRange == range
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isActive) VoxnColors.electricBlue else VoxnColors.electricBlue.copy(alpha = 0.1f))
                                .border(1.dp, VoxnColors.electricBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectTimeRange(range) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (range == SpendingTimeRange.Custom) {
                                    Icon(Icons.Default.CalendarMonth, null, tint = if (isActive) VoxnColors.backgroundDark else VoxnColors.electricBlue, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    range.label,
                                    style = VoxnFont.mono(11, FontWeight.SemiBold),
                                    color = if (isActive) VoxnColors.backgroundDark else VoxnColors.electricBlue,
                                )
                            }
                        }
                    }
                }
            }

            // Statement Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Period label
                    Text(
                        viewModel.dateRangeLabel().uppercase(),
                        style = VoxnFont.mono(10, FontWeight.Medium),
                        color = VoxnColors.textTertiary,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Total spend card
                    GlassCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("TOTAL SPEND", style = VoxnFont.dataLabel, color = VoxnColors.textTertiary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                viewModel.formatAmount(viewModel.totalSpend()),
                                style = VoxnFont.heroNumber,
                                color = VoxnColors.warningOrange,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${viewModel.totalTransactionCount()} transactions",
                                style = VoxnFont.mono(11, FontWeight.Medium),
                                color = VoxnColors.textTertiary,
                            )
                        }
                    }
                }
            }

            // Quick Stats — Today / This Week (always current)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickStatCard(
                        label = "TODAY",
                        value = viewModel.formatAmount(viewModel.todaySpending()),
                        color = VoxnColors.electricBlue,
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f),
                    )
                    QuickStatCard(
                        label = "THIS WEEK",
                        value = viewModel.formatAmount(viewModel.weeklySpending()),
                        color = VoxnColors.cyan,
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Budget & Savings
            item {
                val monthSpend = viewModel.monthlySpending()
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("BUDGET & GOALS", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.neonGreen, letterSpacing = 2.sp)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showBudgetDialog = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Settings, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    if (monthlyBudget > 0) {
                        // Budget progress bar
                        val budgetProgress = viewModel.budgetManager.budgetProgress(monthSpend)
                        val exceeded = viewModel.budgetManager.isBudgetExceeded(monthSpend)
                        val remaining = viewModel.budgetManager.budgetRemaining(monthSpend)
                        val barColor = if (exceeded) VoxnColors.alertRed else if (budgetProgress > 0.8) VoxnColors.warningOrange else VoxnColors.neonGreen

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = barColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Monthly Budget", style = VoxnFont.cardBody, color = VoxnColors.textSecondary)
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(8.dp).background(VoxnColors.cardBackground, RoundedCornerShape(4.dp))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(budgetProgress.coerceAtMost(1.0).toFloat()).background(barColor, RoundedCornerShape(4.dp)))
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                viewModel.formatAmount(monthSpend) + " / " + viewModel.formatAmount(monthlyBudget),
                                style = VoxnFont.mono(11, FontWeight.Medium), color = VoxnColors.textTertiary,
                            )
                            Text(
                                if (exceeded) "Over by ${viewModel.formatAmount(monthSpend - monthlyBudget)}" else "${viewModel.formatAmount(remaining)} left",
                                style = VoxnFont.mono(11, FontWeight.Bold), color = barColor,
                            )
                        }

                        if (exceeded) {
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
                                .background(VoxnColors.alertRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp)) {
                                Icon(Icons.Default.Warning, null, tint = VoxnColors.alertRed, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Budget exceeded!", style = VoxnFont.mono(11, FontWeight.Bold), color = VoxnColors.alertRed)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddCircle, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tap the gear to set a monthly budget", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                        }
                    }

                    if (savingsGoal > 0) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Savings, null, tint = VoxnColors.cyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Savings Goal: ${viewModel.formatAmount(savingsGoal)}", style = VoxnFont.cardBody, color = VoxnColors.textSecondary)
                        }
                    }
                }
            }

            // Transaction Parser
            item {
                GlassCard {
                    Text("TRANSACTION PARSER", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.warningOrange, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Paste bank notification to auto-parse", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.showParse() },
                        colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.warningOrange.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Default.Memory, null, tint = VoxnColors.warningOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PARSE", style = VoxnFont.mono(12, FontWeight.Bold), color = VoxnColors.warningOrange, letterSpacing = 2.sp)
                    }
                    parseResult?.let { result ->
                        Spacer(Modifier.height(8.dp))
                        val isSuccess = result == "Transaction logged"
                        Text(result, style = VoxnFont.caption, color = if (isSuccess) VoxnColors.neonGreen else VoxnColors.warningOrange)
                    }
                }
            }

            // Recurring Expenses
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("RECURRING BILLS", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.purple, letterSpacing = 2.sp)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showRecurringDialog = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.AddCircle, null, tint = VoxnColors.purple, modifier = Modifier.size(22.dp))
                        }
                    }
                    if (recurringExpenses.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("No recurring bills. Tap + to add.", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                    } else {
                        Spacer(Modifier.height(8.dp))
                        recurringExpenses.forEach { re ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp).background(re.category.color.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CategoryIcon(re.category)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(re.name, style = VoxnFont.cardBody, color = VoxnColors.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text("Day ${re.dayOfMonth} every month", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                                }
                                Text(viewModel.formatAmount(re.amount), style = VoxnFont.mono(12, FontWeight.Bold), color = VoxnColors.textSecondary)
                                IconButton(onClick = { viewModel.recurringManager.removeRecurringExpense(re.id) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Default.Close, null, tint = VoxnColors.alertRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        val totalMonthly = recurringExpenses.sumOf { it.amount }
                        Text("Total: ${viewModel.formatAmount(totalMonthly)}/mo", style = VoxnFont.mono(11, FontWeight.Bold), color = VoxnColors.purple)
                    }
                }
            }

            // Category Analysis (range-filtered)
            item {
                val breakdown = viewModel.categoryBreakdown()
                if (breakdown.isNotEmpty()) {
                    GlassCard {
                        Text("CATEGORY ANALYSIS", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.warningOrange, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))

                        // Donut chart
                        val totalSpend = breakdown.sumOf { it.second }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            com.voxn.ai.ui.components.DonutChart(
                                slices = breakdown.map { (cat, amount) ->
                                    com.voxn.ai.ui.components.DonutSlice(cat.displayName, amount, cat.color)
                                },
                                centerText = viewModel.formatAmount(totalSpend),
                                centerSubText = "Total",
                                size = 140.dp,
                                strokeWidth = 16.dp,
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        // Legend bars
                        val maxAmount = breakdown.maxOf { it.second }
                        breakdown.forEach { (cat, amount) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp).background(cat.color.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CategoryIcon(cat)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(cat.displayName, style = VoxnFont.caption, color = VoxnColors.textSecondary, modifier = Modifier.width(70.dp))
                                Box(Modifier.weight(1f).height(6.dp).background(VoxnColors.cardBackground, RoundedCornerShape(3.dp))) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth((amount / maxAmount).toFloat()).background(cat.color.copy(alpha = 0.7f), RoundedCornerShape(3.dp)))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(viewModel.formatAmount(amount), style = VoxnFont.mono(11, FontWeight.Medium), color = VoxnColors.textSecondary)
                            }
                        }
                    }
                }
            }

            // Spending Insights
            item {
                GlassCard {
                    Text("SPENDING INSIGHTS", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
                    Spacer(Modifier.height(12.dp))

                    // Week-over-week change
                    val wowChange = viewModel.weekOverWeekChange()
                    val avgDaily = viewModel.averageDailySpend()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("WoW Change", style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary)
                            if (wowChange != null) {
                                val isUp = wowChange > 0
                                Text(
                                    "${if (isUp) "+" else ""}${String.format("%.1f", wowChange)}%",
                                    style = VoxnFont.mono(18, FontWeight.Bold),
                                    color = if (isUp) VoxnColors.alertRed else VoxnColors.neonGreen,
                                )
                            } else {
                                Text("—", style = VoxnFont.mono(18, FontWeight.Bold), color = VoxnColors.textTertiary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Avg/Day", style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary)
                            Text(viewModel.formatAmount(avgDaily), style = VoxnFont.mono(18, FontWeight.Bold), color = VoxnColors.electricBlue)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 14-day bar chart
                    Text("LAST 14 DAYS", style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    val dailyData = viewModel.dailySpendingLast14Days()
                    val maxDaily = dailyData.maxOfOrNull { it.second } ?: 1.0

                    Row(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        dailyData.forEach { (label, amount) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f),
                            ) {
                                val height = if (maxDaily > 0 && amount > 0) (amount / maxDaily * 70).dp else 2.dp
                                Box(
                                    Modifier.width(10.dp).height(height)
                                        .background(
                                            Brush.verticalGradient(listOf(VoxnColors.electricBlue, VoxnColors.electricBlue.copy(alpha = 0.3f))),
                                            RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(label, style = VoxnFont.mono(7, FontWeight.Normal), color = VoxnColors.textTertiary)
                            }
                        }
                    }
                }
            }

            // Statement header + filters
            item {
                Text("STATEMENT", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.warningOrange, letterSpacing = 2.sp)
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = filterSearch,
                    onValueChange = { viewModel.setFilterSearch(it) },
                    placeholder = { Text("Search transactions...", style = VoxnFont.cardBody, color = VoxnColors.textTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (filterSearch.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setFilterSearch("") }) {
                                Icon(Icons.Default.Clear, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VoxnColors.electricBlue.copy(alpha = 0.3f),
                        unfocusedBorderColor = VoxnColors.electricBlue.copy(alpha = 0.15f),
                        focusedTextColor = VoxnColors.textPrimary,
                        unfocusedTextColor = VoxnColors.textPrimary,
                        cursorColor = VoxnColors.electricBlue,
                        focusedContainerColor = VoxnColors.cardBackground,
                        unfocusedContainerColor = VoxnColors.cardBackground,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            // Category filter chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // All chip
                    FilterChipItem(label = "All", isActive = filterCat == null, color = VoxnColors.electricBlue) {
                        viewModel.setFilterCategory(null)
                    }
                    ExpenseCategory.entries.forEach { cat ->
                        FilterChipItem(label = cat.displayName, isActive = filterCat == cat, color = cat.color) {
                            viewModel.setFilterCategory(if (filterCat == cat) null else cat)
                        }
                    }
                    if (viewModel.hasActiveFilters()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(VoxnColors.alertRed.copy(alpha = 0.1f))
                                .clickable { viewModel.clearFilters() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text("Clear", style = VoxnFont.mono(10, FontWeight.Bold), color = VoxnColors.alertRed)
                        }
                    }
                }
            }

            // Grouped transactions — bank statement style
            val groups = viewModel.groupedTransactions()
            if (groups.isEmpty()) {
                item {
                    GlassCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                            Icon(Icons.Default.Inbox, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (viewModel.hasActiveFilters()) "No matching transactions" else "No transactions for this period",
                                style = VoxnFont.caption,
                                color = VoxnColors.textTertiary,
                            )
                            if (viewModel.hasActiveFilters()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Clear Filters", style = VoxnFont.caption, color = VoxnColors.electricBlue,
                                    modifier = Modifier.clickable { viewModel.clearFilters() })
                            }
                        }
                    }
                }
            }

            groups.forEach { group ->
                // Date header
                item(key = "header_${group.date}") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(group.label, style = VoxnFont.mono(11, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 1.sp)
                        if (group.dailyTotal > 0) {
                            Text(viewModel.formatAmount(group.dailyTotal), style = VoxnFont.mono(11, FontWeight.SemiBold), color = VoxnColors.warningOrange)
                        }
                    }
                }

                // Transactions for this date
                items(group.transactions, key = { it.id }) { expense ->
                    ExpenseRow(expense, viewModel)
                }

                // Separator
                item(key = "sep_${group.date}") {
                    Spacer(
                        modifier = Modifier.fillMaxWidth().height(1.dp)
                            .background(VoxnColors.electricBlue.copy(alpha = 0.08f))
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { viewModel.showAddExpense() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 100.dp).size(56.dp),
            containerColor = VoxnColors.warningOrange,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, "Add", tint = VoxnColors.backgroundDark, modifier = Modifier.size(22.dp))
        }
    }

    if (showAddDialog) { AddExpenseDialog(viewModel) }
    if (showParseSheet) { ParseDialog(viewModel) }
    if (showCustomPicker) { CustomDateRangeDialog(viewModel) }

    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Transaction", style = VoxnFont.cardTitle, color = VoxnColors.textPrimary) },
            text = { Text("Delete ${expense.formattedAmount} at ${expense.merchant}?", style = VoxnFont.cardBody, color = VoxnColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteExpense() },
                    colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.alertRed),
                ) { Text("Delete", style = VoxnFont.mono(13, FontWeight.Bold)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel", color = VoxnColors.textTertiary)
                }
            },
            containerColor = VoxnColors.backgroundMid,
        )
    }

    if (showBudgetDialog) {
        BudgetSetupDialog(
            currentBudget = monthlyBudget,
            currentSavingsGoal = savingsGoal,
            onSave = { budget, savings ->
                viewModel.budgetManager.setMonthlyBudget(budget)
                viewModel.budgetManager.setSavingsGoal(savings)
                showBudgetDialog = false
            },
            onDismiss = { showBudgetDialog = false },
        )
    }

    if (showRecurringDialog) {
        AddRecurringExpenseDialog(
            onAdd = { name, amount, category, day ->
                viewModel.recurringManager.addRecurringExpense(name, amount, category, day)
                showRecurringDialog = false
            },
            onDismiss = { showRecurringDialog = false },
        )
    }
}

@Composable
private fun BudgetSetupDialog(
    currentBudget: Double,
    currentSavingsGoal: Double,
    onSave: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var budgetText by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toLong().toString() else "") }
    var savingsText by remember { mutableStateOf(if (currentSavingsGoal > 0) currentSavingsGoal.toLong().toString() else "") }

    com.voxn.ai.ui.components.VoxnDialog(
        title = "BUDGET & GOALS",
        accent = VoxnColors.neonGreen,
        onDismiss = onDismiss,
        confirmLabel = "SAVE",
        onConfirm = {
            onSave(
                budgetText.toDoubleOrNull() ?: 0.0,
                savingsText.toDoubleOrNull() ?: 0.0,
            )
        },
    ) {
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,8}$"))) budgetText = it },
                    label = { Text("Monthly Budget (₹)", color = VoxnColors.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.neonGreen, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.neonGreen),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = savingsText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,8}$"))) savingsText = it },
                    label = { Text("Monthly Savings Goal (₹)", color = VoxnColors.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.neonGreen, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.neonGreen),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Text("Set to 0 or leave empty to disable", style = VoxnFont.caption, color = VoxnColors.textTertiary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecurringExpenseDialog(
    onAdd: (String, Double, ExpenseCategory, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.Bills) }
    var dayOfMonth by remember { mutableStateOf("1") }

    com.voxn.ai.ui.components.VoxnDialog(
        title = "ADD RECURRING BILL",
        accent = VoxnColors.purple,
        onDismiss = onDismiss,
        confirmLabel = "ADD",
        confirmEnabled = name.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0 && (dayOfMonth.toIntOrNull() ?: 0) in 1..31,
        onConfirm = {
            val amt = amount.toDoubleOrNull() ?: return@VoxnDialog
            val day = dayOfMonth.toIntOrNull() ?: return@VoxnDialog
            onAdd(name, amt, category, day)
        },
    ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Bill name (e.g., Netflix)", color = VoxnColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.purple, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.purple),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                    label = { Text("Amount (₹)", color = VoxnColors.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.purple, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.purple),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = dayOfMonth,
                    onValueChange = { v -> if (v.isEmpty() || (v.toIntOrNull()?.let { it in 1..31 } == true)) dayOfMonth = v },
                    label = { Text("Day of month (1-31)", color = VoxnColors.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.purple, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.purple),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                Text("CATEGORY", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpenseCategory.entries.toList()) { cat ->
                        val selected = category == cat
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (selected) cat.color.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                                .border(1.dp, if (selected) cat.color else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(cat.displayName, style = VoxnFont.mono(10, FontWeight.Medium), color = if (selected) cat.color else VoxnColors.textTertiary)
                        }
                    }
                }
    }
}

@Composable
private fun QuickStatCard(label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(VoxnColors.cardBackground)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Column {
                Text(label, style = VoxnFont.mono(9, FontWeight.Medium), color = VoxnColors.textTertiary, letterSpacing = 1.sp)
                Text(value, style = VoxnFont.mono(16, FontWeight.Bold), color = color)
            }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, isActive: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) color else color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, style = VoxnFont.caption, color = if (isActive) VoxnColors.backgroundDark else color)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExpenseRow(expense: ExpenseEntity, viewModel: SpendingViewModel) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    GlassCard(modifier = Modifier.combinedClickable(onClick = {}, onLongClick = { viewModel.requestDeleteExpense(expense) })) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(expense.category.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CategoryIcon(expense.category)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.merchant, style = VoxnFont.cardTitle, color = VoxnColors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(expense.category.displayName, style = VoxnFont.caption, color = expense.category.color)
                    Text(timeFormat.format(Date(expense.date)), style = VoxnFont.caption, color = VoxnColors.textTertiary)
                }
            }
            Text("-${expense.formattedAmount}", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.textPrimary)
        }
    }
}

@Composable
private fun CategoryIcon(category: ExpenseCategory) {
    val icon = when (category) {
        ExpenseCategory.Food -> Icons.Default.Restaurant
        ExpenseCategory.Transport -> Icons.Default.DirectionsCar
        ExpenseCategory.Shopping -> Icons.Default.ShoppingBag
        ExpenseCategory.Bills -> Icons.Default.Description
        ExpenseCategory.Entertainment -> Icons.Default.SportsEsports
        ExpenseCategory.Health -> Icons.Default.Favorite
        ExpenseCategory.Education -> Icons.Default.School
        ExpenseCategory.Other -> Icons.Default.MoreHoriz
    }
    Icon(icon, null, tint = category.color, modifier = Modifier.size(18.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(viewModel: SpendingViewModel) {
    val customStart by viewModel.customStartDate.collectAsStateWithLifecycle()
    val customEnd by viewModel.customEndDate.collectAsStateWithLifecycle()

    var pickingStart by remember { mutableStateOf(true) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (pickingStart) customStart else customEnd
    )

    Dialog(
        onDismissRequest = { viewModel.dismissCustomDatePicker() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VoxnColors.backgroundMid,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.electricBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("CUSTOM RANGE", style = VoxnFont.mono(16, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
                Spacer(Modifier.height(16.dp))

                // FROM / TO toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dfLabel = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (pickingStart) VoxnColors.electricBlue.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                            .border(1.dp, if (pickingStart) VoxnColors.electricBlue else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { pickingStart = true }
                            .padding(12.dp),
                    ) {
                        Column {
                            Text("FROM", style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(dfLabel.format(Date(customStart)), style = VoxnFont.mono(12, FontWeight.Bold), color = VoxnColors.electricBlue)
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (!pickingStart) VoxnColors.electricBlue.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                            .border(1.dp, if (!pickingStart) VoxnColors.electricBlue else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { pickingStart = false }
                            .padding(12.dp),
                    ) {
                        Column {
                            Text("TO", style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(dfLabel.format(Date(customEnd)), style = VoxnFont.mono(12, FontWeight.Bold), color = VoxnColors.electricBlue)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Date picker
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier.fillMaxWidth(),
                    colors = DatePickerDefaults.colors(
                        containerColor = Color.Transparent,
                        titleContentColor = VoxnColors.textPrimary,
                        headlineContentColor = VoxnColors.electricBlue,
                        weekdayContentColor = VoxnColors.textTertiary,
                        dayContentColor = VoxnColors.textSecondary,
                        selectedDayContainerColor = VoxnColors.electricBlue,
                        selectedDayContentColor = VoxnColors.backgroundDark,
                        todayContentColor = VoxnColors.electricBlue,
                        todayDateBorderColor = VoxnColors.electricBlue,
                    ),
                    showModeToggle = false,
                )

                // Apply selected date
                LaunchedEffect(datePickerState.selectedDateMillis) {
                    datePickerState.selectedDateMillis?.let {
                        if (pickingStart) viewModel.setCustomStartDate(it)
                        else viewModel.setCustomEndDate(it)
                    }
                }

                Spacer(Modifier.height(8.dp))
                com.voxn.ai.ui.components.VoxnDialogActions(
                    onCancel = { viewModel.dismissCustomDatePicker() },
                    confirmLabel = "APPLY RANGE",
                    onConfirm = { viewModel.applyCustomRange() },
                    accent = VoxnColors.electricBlue,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseDialog(viewModel: SpendingViewModel) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.Other) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.Other) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    com.voxn.ai.ui.components.VoxnDialog(
        title = "LOG EXPENSE",
        accent = VoxnColors.warningOrange,
        onDismiss = { viewModel.hideAddExpense() },
        confirmLabel = "LOG",
        confirmEnabled = amount.toDoubleOrNull() != null && (amount.toDoubleOrNull() ?: 0.0) > 0,
        onConfirm = { amount.toDoubleOrNull()?.let { viewModel.addExpense(it, merchant, category, paymentMethod, selectedDate) } },
    ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = newValue
                        }
                    },
                    label = { Text("Amount (₹)", color = VoxnColors.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.warningOrange, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.warningOrange),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = merchant, onValueChange = { merchant = it },
                    label = { Text("Merchant", color = VoxnColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.warningOrange, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.warningOrange),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                // Date picker
                Text("DATE", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(VoxnColors.cardBackground)
                        .border(1.dp, VoxnColors.warningOrange.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EditCalendar, null, tint = VoxnColors.warningOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(dateFormat.format(Date(selectedDate)), style = VoxnFont.mono(14, FontWeight.Medium), color = VoxnColors.textPrimary)
                        Spacer(Modifier.weight(1f))
                        Text("Tap to change", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                    }
                }
                Spacer(Modifier.height(12.dp))

                Text("CATEGORY", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                val categories = ExpenseCategory.entries
                for (row in categories.chunked(4)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            val selected = category == cat
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) cat.color.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                                    .border(1.dp, if (selected) cat.color else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { category = cat }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(cat.displayName, style = VoxnFont.mono(10, FontWeight.Medium), color = if (selected) cat.color else VoxnColors.textTertiary)
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(12.dp))
                Text("PAYMENT", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PaymentMethod.entries.toList()) { pm ->
                        val selected = paymentMethod == pm
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (selected) VoxnColors.warningOrange.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                                .border(1.dp, if (selected) VoxnColors.warningOrange else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { paymentMethod = pm }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text(pm.displayName, style = VoxnFont.mono(11, FontWeight.Medium), color = if (selected) VoxnColors.warningOrange else VoxnColors.textTertiary)
                        }
                    }
                }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate = it }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.warningOrange),
                ) {
                    Text("OK", style = VoxnFont.mono(14, FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = VoxnColors.textTertiary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = VoxnColors.backgroundMid),
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    titleContentColor = VoxnColors.textPrimary,
                    headlineContentColor = VoxnColors.warningOrange,
                    weekdayContentColor = VoxnColors.textTertiary,
                    dayContentColor = VoxnColors.textSecondary,
                    selectedDayContainerColor = VoxnColors.warningOrange,
                    selectedDayContentColor = VoxnColors.backgroundDark,
                    todayContentColor = VoxnColors.warningOrange,
                    todayDateBorderColor = VoxnColors.warningOrange,
                ),
                showModeToggle = false,
            )
        }
    }
}

@Composable
private fun ParseDialog(viewModel: SpendingViewModel) {
    var text by remember { mutableStateOf("") }

    com.voxn.ai.ui.components.VoxnDialog(
        title = "PARSE NOTIFICATION",
        accent = VoxnColors.warningOrange,
        onDismiss = { viewModel.hideParse() },
        confirmLabel = "PARSE",
        confirmEnabled = text.isNotBlank(),
        onConfirm = { viewModel.parseAndAdd(text); viewModel.hideParse() },
    ) {
        Text("Paste your bank SMS or notification text below", style = VoxnFont.caption, color = VoxnColors.textTertiary)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 200.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.warningOrange, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.warningOrange),
            shape = RoundedCornerShape(12.dp),
        )
    }
}
