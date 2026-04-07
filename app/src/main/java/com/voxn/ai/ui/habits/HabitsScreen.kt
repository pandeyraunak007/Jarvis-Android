package com.voxn.ai.ui.habits

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.voxn.ai.data.database.entity.HabitFrequency
import com.voxn.ai.util.HapticFeedback
import com.voxn.ai.data.database.entity.HabitWithCompletions
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.GlassCard
import com.voxn.ai.ui.components.ProgressRing
import com.voxn.ai.viewmodel.HabitViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HabitsScreen(viewModel: HabitViewModel = viewModel()) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val habitToDelete by viewModel.habitToDelete.collectAsStateWithLifecycle()

    val completed = habits.count { it.isCompletedToday() }
    val total = habits.size
    val rate = if (total > 0) completed.toDouble() / total else 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(VoxnColors.backgroundDark, VoxnColors.backgroundMid, VoxnColors.backgroundDark)))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("HABIT PROTOCOL", style = VoxnFont.mono(22, FontWeight.Bold), color = VoxnColors.neonGreen, letterSpacing = 3.sp)
                }
            }

            // Progress
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        ProgressRing(rate, VoxnColors.neonGreen, 80.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$completed/$total", style = VoxnFont.mono(16, FontWeight.Bold), color = VoxnColors.neonGreen)
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Today's Progress", style = VoxnFont.cardTitle, color = VoxnColors.textPrimary)
                            Text("${(rate * 100).toInt()}% complete", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, null, tint = VoxnColors.warningOrange, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Best streak: ${viewModel.longestStreak} days", style = VoxnFont.caption, color = VoxnColors.warningOrange)
                            }
                        }
                    }
                }
            }

            // Daily Objectives
            item {
                Text("DAILY OBJECTIVES", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.neonGreen, letterSpacing = 2.sp)
            }

            if (habits.isEmpty()) {
                item {
                    GlassCard {
                        Text("No habits yet. Tap + to add one.", style = VoxnFont.cardBody, color = VoxnColors.textTertiary, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            items(habits, key = { it.habit.id }) { habitWithCompletions ->
                HabitRow(habitWithCompletions, viewModel)
            }

            // Calendar
            item {
                Text("COMPLETION GRID", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.neonGreen, letterSpacing = 2.sp)
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
            containerColor = VoxnColors.electricBlue,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, "Add", tint = VoxnColors.backgroundDark, modifier = Modifier.size(22.dp))
        }
    }

    if (showAddDialog) {
        AddHabitDialog(viewModel)
    }

    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Habit", style = VoxnFont.cardTitle, color = VoxnColors.textPrimary) },
            text = { Text("Delete \"${habit.name}\"? This will remove all completion history and cannot be undone.", style = VoxnFont.cardBody, color = VoxnColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteHabit() },
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
}

@Composable
private fun HabitRow(habitWithCompletions: HabitWithCompletions, viewModel: HabitViewModel) {
    val context = LocalContext.current
    val isCompleted = habitWithCompletions.isCompletedToday()
    val isDue = habitWithCompletions.isDueToday()
    val streak = habitWithCompletions.currentStreak()
    val isMulti = habitWithCompletions.habit.frequency == HabitFrequency.MultiplePerDay

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isMulti) {
                // Progress ring for multi-per-day
                ProgressRing(habitWithCompletions.progressToday(), VoxnColors.neonGreen, 36.dp, 3.dp) {
                    Text(
                        "${habitWithCompletions.todayCompletionCount()}",
                        style = VoxnFont.mono(10, FontWeight.Bold),
                        color = VoxnColors.neonGreen,
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { HapticFeedback.click(context); viewModel.toggleCompletion(habitWithCompletions) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, null, tint = VoxnColors.neonGreen, modifier = Modifier.size(18.dp))
                }
            } else {
                // Checkbox for daily/weekly
                IconButton(onClick = { HapticFeedback.click(context); viewModel.toggleCompletion(habitWithCompletions) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (isCompleted) VoxnColors.neonGreen else VoxnColors.textTertiary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    habitWithCompletions.habit.name,
                    style = VoxnFont.cardTitle,
                    color = if (isCompleted) VoxnColors.textTertiary else VoxnColors.textPrimary,
                    textDecoration = if (isCompleted && !isMulti) TextDecoration.LineThrough else null,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        habitWithCompletions.habit.frequencyLabel,
                        style = VoxnFont.mono(9, FontWeight.Medium),
                        color = VoxnColors.textTertiary,
                    )
                    if (!isDue && habitWithCompletions.habit.frequency == HabitFrequency.Weekly) {
                        Text("(not today)", style = VoxnFont.mono(9, FontWeight.Normal), color = VoxnColors.textTertiary)
                    }
                }
                if (habitWithCompletions.habit.reminderEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(habitWithCompletions.reminderTimeFormatted, style = VoxnFont.caption, color = VoxnColors.textTertiary)
                    }
                }
            }

            if (streak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, null, tint = VoxnColors.warningOrange, modifier = Modifier.size(14.dp))
                    Text("$streak", style = VoxnFont.mono(12, FontWeight.Bold), color = VoxnColors.warningOrange)
                }
                Spacer(Modifier.width(8.dp))
            }

            IconButton(onClick = { viewModel.requestDeleteHabit(habitWithCompletions.habit) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, null, tint = VoxnColors.alertRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
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
            Icon(Icons.Default.ChevronLeft, null, tint = VoxnColors.textSecondary)
        }
        Spacer(Modifier.weight(1f))
        Text(monthFormat.format(selectedMonth.time), style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.textPrimary)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { viewModel.nextMonth() }) {
            Icon(Icons.Default.ChevronRight, null, tint = VoxnColors.textSecondary)
        }
    }

    // Day headers
    val dayHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(modifier = Modifier.fillMaxWidth()) {
        dayHeaders.forEach { d ->
            Text(d, style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary,
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
                                        if (isCompleted) Modifier.background(VoxnColors.neonGreen.copy(alpha = 0.2f), CircleShape).border(1.dp, VoxnColors.neonGreen.copy(alpha = 0.5f), CircleShape)
                                        else if (isToday) Modifier.border(1.dp, VoxnColors.electricBlue, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = VoxnFont.mono(10, if (isCompleted) FontWeight.Bold else FontWeight.Normal),
                                    color = if (isCompleted) VoxnColors.neonGreen else if (isToday) VoxnColors.electricBlue else VoxnColors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddHabitDialog(viewModel: HabitViewModel) {
    var name by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableIntStateOf(9) }
    var reminderMinute by remember { mutableIntStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(HabitFrequency.Daily) }
    var targetCount by remember { mutableIntStateOf(3) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }

    Dialog(onDismissRequest = { viewModel.hideAddHabit() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VoxnColors.backgroundMid,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.electricBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("NEW HABIT", style = VoxnFont.mono(18, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 3.sp)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name", color = VoxnColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VoxnColors.electricBlue,
                        unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
                        focusedTextColor = VoxnColors.textPrimary,
                        unfocusedTextColor = VoxnColors.textPrimary,
                        cursorColor = VoxnColors.electricBlue,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Spacer(Modifier.height(16.dp))

                // Frequency selector
                Text("FREQUENCY", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    HabitFrequency.entries.forEach { freq ->
                        val selected = selectedFrequency == freq
                        Box(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) VoxnColors.neonGreen.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                                .border(1.dp, if (selected) VoxnColors.neonGreen else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedFrequency = freq }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(freq.label, style = VoxnFont.mono(10, FontWeight.Medium), color = if (selected) VoxnColors.neonGreen else VoxnColors.textTertiary)
                        }
                    }
                }

                // Weekly day picker
                if (selectedFrequency == HabitFrequency.Weekly) {
                    Spacer(Modifier.height(12.dp))
                    Text("ACTIVE DAYS", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                    Spacer(Modifier.height(8.dp))
                    val dayNames = listOf("S" to 1, "M" to 2, "T" to 3, "W" to 4, "T" to 5, "F" to 6, "S" to 7)
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        dayNames.forEach { (label, dayNum) ->
                            val isActive = selectedDays.contains(dayNum)
                            Box(
                                modifier = Modifier.size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) VoxnColors.neonGreen.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                                    .border(1.dp, if (isActive) VoxnColors.neonGreen else VoxnColors.textTertiary.copy(alpha = 0.3f), CircleShape)
                                    .clickable { selectedDays = if (isActive) selectedDays - dayNum else selectedDays + dayNum },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(label, style = VoxnFont.mono(12, FontWeight.Bold), color = if (isActive) VoxnColors.neonGreen else VoxnColors.textTertiary)
                            }
                        }
                    }
                }

                // Target count for multi-per-day
                if (selectedFrequency == HabitFrequency.MultiplePerDay) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("TARGET PER DAY", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { if (targetCount > 2) targetCount-- }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, null, tint = VoxnColors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                        Text("$targetCount", style = VoxnFont.mono(18, FontWeight.Bold), color = VoxnColors.neonGreen)
                        IconButton(onClick = { if (targetCount < 20) targetCount++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, null, tint = VoxnColors.textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Notifications, null, tint = VoxnColors.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DAILY REMINDER", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = VoxnColors.electricBlue),
                    )
                }

                if (reminderEnabled) {
                    Spacer(Modifier.height(8.dp))
                    val displayTime = String.format("%02d:%02d %s",
                        if (reminderHour % 12 == 0) 12 else reminderHour % 12,
                        reminderMinute,
                        if (reminderHour < 12) "AM" else "PM"
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(VoxnColors.cardBackground)
                            .border(1.dp, VoxnColors.electricBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { showTimePicker = true }
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = VoxnColors.electricBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(displayTime, style = VoxnFont.mono(16, FontWeight.Bold), color = VoxnColors.textPrimary)
                            Spacer(Modifier.weight(1f))
                            Text("Tap to change", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.hideAddHabit() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxnColors.textTertiary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.textTertiary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel", style = VoxnFont.mono(13, FontWeight.Medium), maxLines = 1)
                    }
                    Button(
                        onClick = {
                            viewModel.addHabit(
                                name, reminderEnabled, reminderHour, reminderMinute,
                                selectedFrequency.name, targetCount,
                                selectedDays.sorted().joinToString(","),
                            )
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.electricBlue),
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("ADD HABIT", style = VoxnFont.mono(13, FontWeight.Bold), maxLines = 1)
                    }
                }
            }
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderHour,
            initialMinute = reminderMinute,
            is24Hour = false,
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VoxnColors.backgroundMid,
                border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.electricBlue.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("SET REMINDER TIME", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = VoxnColors.cardBackground,
                            clockDialSelectedContentColor = VoxnColors.backgroundDark,
                            clockDialUnselectedContentColor = VoxnColors.textSecondary,
                            selectorColor = VoxnColors.electricBlue,
                            containerColor = Color.Transparent,
                            periodSelectorSelectedContainerColor = VoxnColors.electricBlue.copy(alpha = 0.2f),
                            periodSelectorUnselectedContainerColor = VoxnColors.cardBackground,
                            periodSelectorSelectedContentColor = VoxnColors.electricBlue,
                            periodSelectorUnselectedContentColor = VoxnColors.textTertiary,
                            timeSelectorSelectedContainerColor = VoxnColors.electricBlue.copy(alpha = 0.2f),
                            timeSelectorUnselectedContainerColor = VoxnColors.cardBackground,
                            timeSelectorSelectedContentColor = VoxnColors.electricBlue,
                            timeSelectorUnselectedContentColor = VoxnColors.textSecondary,
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showTimePicker = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxnColors.textTertiary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.textTertiary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Cancel", style = VoxnFont.mono(13, FontWeight.Medium), maxLines = 1)
                        }
                        Button(
                            onClick = {
                                reminderHour = timePickerState.hour
                                reminderMinute = timePickerState.minute
                                showTimePicker = false
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.electricBlue),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("SET", style = VoxnFont.mono(13, FontWeight.Bold), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
