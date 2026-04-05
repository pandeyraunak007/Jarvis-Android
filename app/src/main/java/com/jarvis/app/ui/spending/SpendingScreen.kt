package com.jarvis.app.ui.spending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.jarvis.app.data.database.entity.ExpenseEntity
import com.jarvis.app.data.model.ExpenseCategory
import com.jarvis.app.data.model.PaymentMethod
import com.jarvis.app.theme.JarvisColors
import com.jarvis.app.theme.JarvisFont
import com.jarvis.app.ui.components.GlassCard
import com.jarvis.app.viewmodel.SpendingTimeRange
import com.jarvis.app.viewmodel.SpendingViewModel
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(JarvisColors.backgroundDark, JarvisColors.backgroundMid, JarvisColors.backgroundDark)))
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
                                .background(if (isActive) JarvisColors.electricBlue else JarvisColors.electricBlue.copy(alpha = 0.1f))
                                .border(1.dp, JarvisColors.electricBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectTimeRange(range) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (range == SpendingTimeRange.Custom) {
                                    Icon(Icons.Default.CalendarMonth, null, tint = if (isActive) JarvisColors.backgroundDark else JarvisColors.electricBlue, modifier = Modifier.size(12.dp))
                                }
                                Text(
                                    range.label,
                                    style = JarvisFont.mono(11, FontWeight.SemiBold),
                                    color = if (isActive) JarvisColors.backgroundDark else JarvisColors.electricBlue,
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
                        style = JarvisFont.mono(10, FontWeight.Medium),
                        color = JarvisColors.textTertiary,
                        letterSpacing = 2.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Total spend card
                    GlassCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("TOTAL SPEND", style = JarvisFont.dataLabel, color = JarvisColors.textTertiary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                viewModel.formatAmount(viewModel.totalSpend()),
                                style = JarvisFont.heroNumber,
                                color = JarvisColors.warningOrange,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${viewModel.totalTransactionCount()} transactions",
                                style = JarvisFont.mono(11, FontWeight.Medium),
                                color = JarvisColors.textTertiary,
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
                        color = JarvisColors.electricBlue,
                        icon = Icons.Default.Schedule,
                        modifier = Modifier.weight(1f),
                    )
                    QuickStatCard(
                        label = "THIS WEEK",
                        value = viewModel.formatAmount(viewModel.weeklySpending()),
                        color = JarvisColors.cyan,
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Transaction Parser
            item {
                GlassCard {
                    Text("TRANSACTION PARSER", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.warningOrange, letterSpacing = 2.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Paste bank notification to auto-parse", style = JarvisFont.caption, color = JarvisColors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.showParse() },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisColors.warningOrange.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Default.Memory, null, tint = JarvisColors.warningOrange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PARSE", style = JarvisFont.mono(12, FontWeight.Bold), color = JarvisColors.warningOrange, letterSpacing = 2.sp)
                    }
                    parseResult?.let { result ->
                        Spacer(Modifier.height(8.dp))
                        val isSuccess = result == "Transaction logged"
                        Text(result, style = JarvisFont.caption, color = if (isSuccess) JarvisColors.neonGreen else JarvisColors.warningOrange)
                    }
                }
            }

            // Category Analysis (range-filtered)
            item {
                val breakdown = viewModel.categoryBreakdown()
                if (breakdown.isNotEmpty()) {
                    GlassCard {
                        Text("CATEGORY ANALYSIS", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.warningOrange, letterSpacing = 2.sp)
                        Spacer(Modifier.height(12.dp))
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
                                Text(cat.displayName, style = JarvisFont.caption, color = JarvisColors.textSecondary, modifier = Modifier.width(70.dp))
                                Box(Modifier.weight(1f).height(6.dp).background(JarvisColors.cardBackground, RoundedCornerShape(3.dp))) {
                                    Box(Modifier.fillMaxHeight().fillMaxWidth((amount / maxAmount).toFloat()).background(cat.color.copy(alpha = 0.7f), RoundedCornerShape(3.dp)))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(viewModel.formatAmount(amount), style = JarvisFont.mono(11, FontWeight.Medium), color = JarvisColors.textSecondary)
                            }
                        }
                    }
                }
            }

            // Statement header + filters
            item {
                Text("STATEMENT", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.warningOrange, letterSpacing = 2.sp)
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = filterSearch,
                    onValueChange = { viewModel.setFilterSearch(it) },
                    placeholder = { Text("Search transactions...", style = JarvisFont.cardBody, color = JarvisColors.textTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = JarvisColors.textTertiary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (filterSearch.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setFilterSearch("") }) {
                                Icon(Icons.Default.Clear, null, tint = JarvisColors.textTertiary, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisColors.electricBlue.copy(alpha = 0.3f),
                        unfocusedBorderColor = JarvisColors.electricBlue.copy(alpha = 0.15f),
                        focusedTextColor = JarvisColors.textPrimary,
                        unfocusedTextColor = JarvisColors.textPrimary,
                        cursorColor = JarvisColors.electricBlue,
                        focusedContainerColor = JarvisColors.cardBackground,
                        unfocusedContainerColor = JarvisColors.cardBackground,
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
                    FilterChipItem(label = "All", isActive = filterCat == null, color = JarvisColors.electricBlue) {
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
                                .background(JarvisColors.alertRed.copy(alpha = 0.1f))
                                .clickable { viewModel.clearFilters() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text("Clear", style = JarvisFont.mono(10, FontWeight.Bold), color = JarvisColors.alertRed)
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
                            Icon(Icons.Default.Inbox, null, tint = JarvisColors.textTertiary, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (viewModel.hasActiveFilters()) "No matching transactions" else "No transactions for this period",
                                style = JarvisFont.caption,
                                color = JarvisColors.textTertiary,
                            )
                            if (viewModel.hasActiveFilters()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Clear Filters", style = JarvisFont.caption, color = JarvisColors.electricBlue,
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
                        Text(group.label, style = JarvisFont.mono(11, FontWeight.Bold), color = JarvisColors.electricBlue, letterSpacing = 1.sp)
                        if (group.dailyTotal > 0) {
                            Text(viewModel.formatAmount(group.dailyTotal), style = JarvisFont.mono(11, FontWeight.SemiBold), color = JarvisColors.warningOrange)
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
                            .background(JarvisColors.electricBlue.copy(alpha = 0.08f))
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { viewModel.showAddExpense() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 100.dp).size(56.dp),
            containerColor = JarvisColors.warningOrange,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, "Add", tint = JarvisColors.backgroundDark, modifier = Modifier.size(22.dp))
        }
    }

    if (showAddDialog) { AddExpenseDialog(viewModel) }
    if (showParseSheet) { ParseDialog(viewModel) }
    if (showCustomPicker) { CustomDateRangeDialog(viewModel) }
}

@Composable
private fun QuickStatCard(label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(JarvisColors.cardBackground)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Column {
                Text(label, style = JarvisFont.mono(9, FontWeight.Medium), color = JarvisColors.textTertiary, letterSpacing = 1.sp)
                Text(value, style = JarvisFont.mono(16, FontWeight.Bold), color = color)
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
        Text(label, style = JarvisFont.caption, color = if (isActive) JarvisColors.backgroundDark else color)
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseEntity, viewModel: SpendingViewModel) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(expense.category.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CategoryIcon(expense.category)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(expense.merchant, style = JarvisFont.cardTitle, color = JarvisColors.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(expense.category.displayName, style = JarvisFont.caption, color = expense.category.color)
                    Text(timeFormat.format(Date(expense.date)), style = JarvisFont.caption, color = JarvisColors.textTertiary)
                }
            }
            Text("-${expense.formattedAmount}", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.textPrimary)
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
        ExpenseCategory.Education -> Icons.Default.MenuBook
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

    Dialog(onDismissRequest = { viewModel.dismissCustomDatePicker() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JarvisColors.backgroundMid,
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisColors.electricBlue.copy(alpha = 0.3f)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("CUSTOM RANGE", style = JarvisFont.mono(18, FontWeight.Bold), color = JarvisColors.electricBlue, letterSpacing = 3.sp)
                Spacer(Modifier.height(16.dp))

                // FROM / TO toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dfLabel = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (pickingStart) JarvisColors.electricBlue.copy(alpha = 0.2f) else JarvisColors.cardBackground)
                            .border(1.dp, if (pickingStart) JarvisColors.electricBlue else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { pickingStart = true }
                            .padding(12.dp),
                    ) {
                        Column {
                            Text("FROM", style = JarvisFont.mono(10, FontWeight.Medium), color = JarvisColors.textTertiary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(dfLabel.format(Date(customStart)), style = JarvisFont.mono(12, FontWeight.Bold), color = JarvisColors.electricBlue)
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (!pickingStart) JarvisColors.electricBlue.copy(alpha = 0.2f) else JarvisColors.cardBackground)
                            .border(1.dp, if (!pickingStart) JarvisColors.electricBlue else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { pickingStart = false }
                            .padding(12.dp),
                    ) {
                        Column {
                            Text("TO", style = JarvisFont.mono(10, FontWeight.Medium), color = JarvisColors.textTertiary, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(dfLabel.format(Date(customEnd)), style = JarvisFont.mono(12, FontWeight.Bold), color = JarvisColors.electricBlue)
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
                        titleContentColor = JarvisColors.textPrimary,
                        headlineContentColor = JarvisColors.electricBlue,
                        weekdayContentColor = JarvisColors.textTertiary,
                        dayContentColor = JarvisColors.textSecondary,
                        selectedDayContainerColor = JarvisColors.electricBlue,
                        selectedDayContentColor = JarvisColors.backgroundDark,
                        todayContentColor = JarvisColors.electricBlue,
                        todayDateBorderColor = JarvisColors.electricBlue,
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

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { viewModel.dismissCustomDatePicker() }) {
                        Text("Cancel", color = JarvisColors.textTertiary)
                    }
                    Button(
                        onClick = { viewModel.applyCustomRange() },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisColors.electricBlue),
                    ) {
                        Text("APPLY RANGE", style = JarvisFont.mono(14, FontWeight.Bold), letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(viewModel: SpendingViewModel) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.Other) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.Other) }

    Dialog(onDismissRequest = { viewModel.hideAddExpense() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JarvisColors.backgroundMid,
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisColors.warningOrange.copy(alpha = 0.3f)),
        ) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("LOG EXPENSE", style = JarvisFont.mono(18, FontWeight.Bold), color = JarvisColors.warningOrange, letterSpacing = 3.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (₹)", color = JarvisColors.textTertiary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisColors.warningOrange, unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = JarvisColors.textPrimary, unfocusedTextColor = JarvisColors.textPrimary, cursorColor = JarvisColors.warningOrange),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = merchant, onValueChange = { merchant = it },
                    label = { Text("Merchant", color = JarvisColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisColors.warningOrange, unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = JarvisColors.textPrimary, unfocusedTextColor = JarvisColors.textPrimary, cursorColor = JarvisColors.warningOrange),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                Text("CATEGORY", style = JarvisFont.mono(12, FontWeight.Medium), color = JarvisColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                val categories = ExpenseCategory.entries
                for (row in categories.chunked(4)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { cat ->
                            val selected = category == cat
                            Box(
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) cat.color.copy(alpha = 0.2f) else JarvisColors.cardBackground)
                                    .border(1.dp, if (selected) cat.color else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { category = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(cat.displayName, style = JarvisFont.mono(9, FontWeight.Medium), color = if (selected) cat.color else JarvisColors.textTertiary)
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(12.dp))
                Text("PAYMENT", style = JarvisFont.mono(12, FontWeight.Medium), color = JarvisColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PaymentMethod.entries.toList()) { pm ->
                        val selected = paymentMethod == pm
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(if (selected) JarvisColors.warningOrange.copy(alpha = 0.2f) else JarvisColors.cardBackground)
                                .border(1.dp, if (selected) JarvisColors.warningOrange else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { paymentMethod = pm }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(pm.displayName, style = JarvisFont.mono(10, FontWeight.Medium), color = if (selected) JarvisColors.warningOrange else JarvisColors.textTertiary)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { viewModel.hideAddExpense() }) { Text("Cancel", color = JarvisColors.textTertiary) }
                    Button(
                        onClick = { amount.toDoubleOrNull()?.let { viewModel.addExpense(it, merchant, category, paymentMethod, System.currentTimeMillis()) } },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisColors.warningOrange),
                        enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0,
                    ) {
                        Text("LOG EXPENSE", style = JarvisFont.mono(14, FontWeight.Bold), letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParseDialog(viewModel: SpendingViewModel) {
    var text by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { viewModel.hideParse() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JarvisColors.backgroundMid,
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisColors.warningOrange.copy(alpha = 0.3f)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("PARSE NOTIFICATION", style = JarvisFont.mono(16, FontWeight.Bold), color = JarvisColors.warningOrange, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                Text("Paste your bank SMS or notification text below", style = JarvisFont.caption, color = JarvisColors.textTertiary)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisColors.warningOrange, unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = JarvisColors.textPrimary, unfocusedTextColor = JarvisColors.textPrimary, cursorColor = JarvisColors.warningOrange),
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { viewModel.hideParse() }) { Text("Cancel", color = JarvisColors.textTertiary) }
                    Button(
                        onClick = { viewModel.parseAndAdd(text); viewModel.hideParse() },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisColors.warningOrange),
                        enabled = text.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Memory, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("PARSE", style = JarvisFont.mono(14, FontWeight.Bold), letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}
