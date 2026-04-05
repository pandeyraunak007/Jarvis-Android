package com.jarvis.app.ui.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.app.data.database.entity.HabitWithCompletions
import com.jarvis.app.theme.JarvisColors
import com.jarvis.app.theme.JarvisFont
import com.jarvis.app.ui.components.GlassCard
import com.jarvis.app.ui.components.ProgressRing
import com.jarvis.app.viewmodel.HabitViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HabitsScreen(viewModel: HabitViewModel = viewModel()) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()

    val completed = habits.count { it.isCompletedToday() }
    val total = habits.size
    val rate = if (total > 0) completed.toDouble() / total else 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(JarvisColors.backgroundDark, JarvisColors.backgroundMid, JarvisColors.backgroundDark)))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("HABIT PROTOCOL", style = JarvisFont.mono(22, FontWeight.Bold), color = JarvisColors.neonGreen, letterSpacing = 3.sp)
                }
            }

            // Progress
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        ProgressRing(rate, JarvisColors.neonGreen, 80.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$completed/$total", style = JarvisFont.mono(16, FontWeight.Bold), color = JarvisColors.neonGreen)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Today's Progress", style = JarvisFont.cardTitle, color = JarvisColors.textPrimary)
                            Text("${(rate * 100).toInt()}% complete", style = JarvisFont.caption, color = JarvisColors.textTertiary)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, null, tint = JarvisColors.warningOrange, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Best streak: ${viewModel.longestStreak} days", style = JarvisFont.caption, color = JarvisColors.warningOrange)
                            }
                        }
                    }
                }
            }

            // Daily Objectives
            item {
                Text("DAILY OBJECTIVES", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.neonGreen, letterSpacing = 2.sp)
            }

            if (habits.isEmpty()) {
                item {
                    GlassCard {
                        Text("No habits yet. Tap + to add one.", style = JarvisFont.cardBody, color = JarvisColors.textTertiary, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            items(habits, key = { it.habit.id }) { habitWithCompletions ->
                HabitRow(habitWithCompletions, viewModel)
            }

            // Calendar
            item {
                Text("COMPLETION GRID", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.neonGreen, letterSpacing = 2.sp)
            }

            item {
                GlassCard {
                    CompletionCalendar(habits, selectedMonth, viewModel)
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { viewModel.showAddHabit() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 100.dp).size(56.dp),
            containerColor = JarvisColors.electricBlue,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, "Add", tint = JarvisColors.backgroundDark, modifier = Modifier.size(22.dp))
        }
    }

    if (showAddDialog) {
        AddHabitDialog(viewModel)
    }
}

@Composable
private fun HabitRow(habitWithCompletions: HabitWithCompletions, viewModel: HabitViewModel) {
    val isCompleted = habitWithCompletions.isCompletedToday()
    val streak = habitWithCompletions.currentStreak()

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Checkbox
            IconButton(onClick = { viewModel.toggleCompletion(habitWithCompletions) }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    null,
                    tint = if (isCompleted) JarvisColors.neonGreen else JarvisColors.textTertiary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    habitWithCompletions.habit.name,
                    style = JarvisFont.cardTitle,
                    color = if (isCompleted) JarvisColors.textTertiary else JarvisColors.textPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                )
                if (habitWithCompletions.habit.reminderEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = JarvisColors.textTertiary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(habitWithCompletions.reminderTimeFormatted, style = JarvisFont.caption, color = JarvisColors.textTertiary)
                    }
                }
            }

            if (streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = JarvisColors.warningOrange, modifier = Modifier.size(14.dp))
                    Text("$streak", style = JarvisFont.mono(12, FontWeight.Bold), color = JarvisColors.warningOrange)
                }
                Spacer(Modifier.width(8.dp))
            }

            IconButton(onClick = { viewModel.deleteHabit(habitWithCompletions.habit) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = JarvisColors.alertRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CompletionCalendar(habits: List<HabitWithCompletions>, selectedMonth: Calendar, viewModel: HabitViewModel) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val cal = selectedMonth.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthStart = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    val monthEnd = cal.timeInMillis

    // Collect all completion dates
    val completionDates = habits.flatMap { it.completionDatesForMonth(monthStart, monthEnd) }.toSet()

    val todayCal = Calendar.getInstance()
    todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0)
    val todayMs = todayCal.timeInMillis

    // Month navigation
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = { viewModel.previousMonth() }) {
            Icon(Icons.Default.ChevronLeft, null, tint = JarvisColors.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        Text(monthFormat.format(selectedMonth.time), style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.textPrimary)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { viewModel.nextMonth() }) {
            Icon(Icons.Default.ChevronRight, null, tint = JarvisColors.textSecondary)
        }
    }

    // Day headers
    val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(modifier = Modifier.fillMaxWidth()) {
        dayHeaders.forEach { d ->
            Text(d, style = JarvisFont.mono(10, FontWeight.Medium), color = JarvisColors.textTertiary,
                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }

    Spacer(Modifier.height(8.dp))

    // Calendar grid
    val gridCal = selectedMonth.clone() as Calendar
    gridCal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = gridCal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = gridCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val cells = (0 until 42).map { index ->
        val dayIndex = index - firstDayOfWeek
        if (dayIndex in 0 until daysInMonth) dayIndex + 1 else null
    }

    Column {
        for (week in 0 until 6) {
            val weekCells = cells.subList(week * 7, (week + 1) * 7)
            if (weekCells.all { it == null }) continue
            Row(modifier = Modifier.fillMaxWidth()) {
                weekCells.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val dayCal = (selectedMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
                            val isToday = dayCal.timeInMillis == todayMs
                            val isCompleted = completionDates.contains(dayCal.timeInMillis)

                            Box(
                                modifier = Modifier.size(28.dp)
                                    .then(
                                        if (isCompleted) Modifier.background(JarvisColors.neonGreen.copy(alpha = 0.2f), CircleShape).border(1.dp, JarvisColors.neonGreen.copy(alpha = 0.5f), CircleShape)
                                        else if (isToday) Modifier.border(1.dp, JarvisColors.electricBlue, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = JarvisFont.mono(10, if (isCompleted) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isCompleted) JarvisColors.neonGreen else if (isToday) JarvisColors.electricBlue else JarvisColors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddHabitDialog(viewModel: HabitViewModel) {
    var name by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableIntStateOf(9) }
    var reminderMinute by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = { viewModel.hideAddHabit() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JarvisColors.backgroundMid,
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisColors.electricBlue.copy(alpha = 0.3f)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("NEW HABIT", style = JarvisFont.mono(18, FontWeight.Bold), color = JarvisColors.electricBlue, letterSpacing = 3.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name", color = JarvisColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisColors.electricBlue,
                        unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f),
                        focusedTextColor = JarvisColors.textPrimary,
                        unfocusedTextColor = JarvisColors.textPrimary,
                        cursorColor = JarvisColors.electricBlue,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = JarvisColors.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DAILY REMINDER", style = JarvisFont.mono(12, FontWeight.Medium), color = JarvisColors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = JarvisColors.electricBlue),
                    )
                }

                if (reminderEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Time: ", style = JarvisFont.caption, color = JarvisColors.textSecondary)
                        // Simple hour/minute selector
                        var hourText by remember { mutableStateOf("$reminderHour") }
                        var minuteText by remember { mutableStateOf(reminderMinute.toString().padStart(2, '0')) }
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { v -> hourText = v; v.toIntOrNull()?.let { reminderHour = it.coerceIn(0, 23) } },
                            modifier = Modifier.width(60.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisColors.electricBlue, unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = JarvisColors.textPrimary, unfocusedTextColor = JarvisColors.textPrimary, cursorColor = JarvisColors.electricBlue),
                        )
                        Text(" : ", style = JarvisFont.mono(16, FontWeight.Bold), color = JarvisColors.textPrimary)
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { v -> minuteText = v; v.toIntOrNull()?.let { reminderMinute = it.coerceIn(0, 59) } },
                            modifier = Modifier.width(60.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisColors.electricBlue, unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = JarvisColors.textPrimary, unfocusedTextColor = JarvisColors.textPrimary, cursorColor = JarvisColors.electricBlue),
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.hideAddHabit() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JarvisColors.textTertiary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, JarvisColors.textTertiary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel", style = JarvisFont.mono(14, FontWeight.Medium))
                    }
                    Button(
                        onClick = { viewModel.addHabit(name, reminderEnabled, reminderHour, reminderMinute) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisColors.electricBlue),
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("ACTIVATE", style = JarvisFont.mono(14, FontWeight.Bold), letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}
