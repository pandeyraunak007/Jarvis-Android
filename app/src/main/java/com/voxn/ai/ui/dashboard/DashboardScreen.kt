package com.voxn.ai.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.viewmodel.BriefLine
import com.voxn.ai.viewmodel.SmartAlert
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.ArcGauge
import com.voxn.ai.ui.components.GlassCard
import com.voxn.ai.ui.components.ProgressRing
import com.voxn.ai.ui.profile.ProfileScreen
import com.voxn.ai.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val monthlyBudget by viewModel.budgetManager.monthlyBudget.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarManager.todayEvents.collectAsStateWithLifecycle()
    val healthData by viewModel.healthData.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var showProfile by remember { mutableStateOf(false) }

    if (showProfile) {
        ProfileScreen(onDismiss = { showProfile = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoxnColors.backgroundDark, VoxnColors.backgroundMid, VoxnColors.backgroundDark)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.size(40.dp))
            Spacer(Modifier.weight(1f))
            Text("V.O.X.N.", style = VoxnFont.mono(28, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 4.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showProfile = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Person, "Profile", tint = VoxnColors.electricBlue, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = viewModel.greeting,
            style = VoxnFont.mono(16, FontWeight.Medium),
            color = VoxnColors.textSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = viewModel.currentDate.uppercase(),
            style = VoxnFont.mono(11, FontWeight.Medium),
            color = VoxnColors.textTertiary,
            letterSpacing = 3.sp,
        )

        Spacer(Modifier.height(24.dp))

        // Smart Alerts
        val alerts = viewModel.generateSmartAlerts(expenses, notes)
        if (alerts.isNotEmpty()) {
            GlassCard {
                alerts.forEach { alert ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                            .background(alert.color.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(alert.icon, style = VoxnFont.cardBody)
                        Spacer(Modifier.width(8.dp))
                        Text(alert.text, style = VoxnFont.mono(11, FontWeight.Medium), color = alert.color)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Daily Brief
        val briefLines = viewModel.generateDailyBrief(expenses, habits, notes, healthData)
        if (briefLines.isNotEmpty()) {
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = VoxnColors.electricBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DAILY BRIEF", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(12.dp))
                briefLines.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(line.icon, style = VoxnFont.caption)
                        Spacer(Modifier.width(8.dp))
                        Text(line.text, style = VoxnFont.cardBody, color = line.color)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Today's Calendar
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = VoxnColors.purple, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("TODAY'S SCHEDULE", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.purple, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(16.dp))

            // Mini calendar showing current week
            val calendar = remember { Calendar.getInstance() }
            val today = remember { calendar.get(Calendar.DAY_OF_MONTH) }
            val dayOfWeek = remember { calendar.get(Calendar.DAY_OF_WEEK) }
            val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
            val dayNames = remember { listOf("S", "M", "T", "W", "T", "F", "S") }

            Text(
                monthFormat.format(calendar.time).uppercase(),
                style = VoxnFont.mono(11, FontWeight.Medium),
                color = VoxnColors.textTertiary,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(12.dp))

            // Week strip
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                val weekCal = (calendar.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, -(dayOfWeek - 1))
                }
                for (i in 0..6) {
                    val day = weekCal.get(Calendar.DAY_OF_MONTH)
                    val isToday = day == today
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            dayNames[i],
                            style = VoxnFont.mono(10, FontWeight.Medium),
                            color = if (isToday) VoxnColors.purple else VoxnColors.textTertiary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier.size(32.dp)
                                .then(
                                    if (isToday) Modifier.background(VoxnColors.purple.copy(alpha = 0.2f), CircleShape)
                                        .border(1.5.dp, VoxnColors.purple, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$day",
                                style = VoxnFont.mono(13, if (isToday) FontWeight.Bold else FontWeight.Normal),
                                color = if (isToday) VoxnColors.purple else VoxnColors.textSecondary,
                            )
                        }
                    }
                    weekCal.add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Calendar events
            if (calendarEvents.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, tint = VoxnColors.purple, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${calendarEvents.size} event${if (calendarEvents.size > 1) "s" else ""} today", style = VoxnFont.mono(11, FontWeight.Bold), color = VoxnColors.purple)
                }
                calendarEvents.take(4).forEach { event ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(6.dp).background(
                                if (event.isOngoing) VoxnColors.neonGreen else VoxnColors.purple,
                                CircleShape,
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(event.title, style = VoxnFont.caption, color = VoxnColors.textSecondary, maxLines = 1)
                            Text(event.timeRange, style = VoxnFont.mono(9, FontWeight.Normal), color = VoxnColors.textTertiary)
                        }
                        if (event.isOngoing) {
                            Text("NOW", style = VoxnFont.mono(9, FontWeight.Bold), color = VoxnColors.neonGreen)
                        }
                    }
                }
                if (calendarEvents.size > 4) {
                    Text("+${calendarEvents.size - 4} more", style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary, modifier = Modifier.padding(start = 20.dp))
                }
                Spacer(Modifier.height(12.dp))
            }

            // Today's agenda
            val activeNotes = notes.filter { !it.isCompleted }
            val dueTodayNotes = activeNotes.filter { note ->
                note.dueDate != null && run {
                    val noteCal = Calendar.getInstance().apply { timeInMillis = note.dueDate }
                    val todayCal = Calendar.getInstance()
                    noteCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    noteCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                }
            }
            val overdueNotes = activeNotes.filter { it.isOverdue }

            if (dueTodayNotes.isEmpty() && overdueNotes.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = VoxnColors.neonGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("No tasks due today. You're all clear!", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                }
            } else {
                if (overdueNotes.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = VoxnColors.alertRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${overdueNotes.size} overdue", style = VoxnFont.mono(11, FontWeight.Bold), color = VoxnColors.alertRed)
                    }
                    overdueNotes.take(3).forEach { note ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(6.dp).background(VoxnColors.alertRed, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(note.title, style = VoxnFont.caption, color = VoxnColors.textSecondary, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (dueTodayNotes.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, null, tint = VoxnColors.electricBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${dueTodayNotes.size} due today", style = VoxnFont.mono(11, FontWeight.Bold), color = VoxnColors.electricBlue)
                    }
                    dueTodayNotes.take(3).forEach { note ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(6.dp).background(note.priority.color, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(note.title, style = VoxnFont.caption, color = VoxnColors.textSecondary, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Health Systems HUD
        GlassCard {
            Text("HEALTH SYSTEMS", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ArcGauge(healthData.stepsProgress, "STEPS", healthData.stepsFormatted, VoxnColors.electricBlue)
                ArcGauge(healthData.caloriesProgress, "KCAL", healthData.caloriesFormatted, VoxnColors.warningOrange)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ArcGauge(healthData.sleepProgress, "SLEEP", healthData.sleepFormatted, VoxnColors.cyan)
                ArcGauge(healthData.workoutProgress, "WORKOUT", healthData.workoutFormatted, VoxnColors.neonGreen)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Habit Protocol
        GlassCard {
            Text("HABIT PROTOCOL", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.neonGreen, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val completed = habits.count { it.isCompletedToday() }
                val total = habits.size
                val rate = if (total > 0) completed.toDouble() / total else 0.0

                ProgressRing(rate, VoxnColors.neonGreen, 60.dp, 5.dp) {
                    Text(
                        "$completed/$total",
                        style = VoxnFont.mono(12, FontWeight.Bold),
                        color = VoxnColors.neonGreen,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Habits Completed", style = VoxnFont.cardTitle, color = VoxnColors.textPrimary)
                    Text("$completed tasks done today", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                    Spacer(Modifier.height(4.dp))
                    val longestStreak = habits.maxOfOrNull { it.currentStreak() } ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = VoxnColors.warningOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Longest streak: $longestStreak days", style = VoxnFont.caption, color = VoxnColors.warningOrange)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Financial Monitor
        GlassCard {
            Text("FINANCIAL MONITOR", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.warningOrange, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SpendingStat("TODAY", viewModel.todaySpending(expenses), VoxnColors.warningOrange)
                SpendingStat("WEEK", viewModel.weeklySpending(expenses), VoxnColors.electricBlue)
                SpendingStat("MONTH", viewModel.monthlySpending(expenses), VoxnColors.cyan)
            }
            // Budget progress
            if (monthlyBudget > 0) {
                Spacer(Modifier.height(12.dp))
                val monthSpend = expenses.filter { it.date >= com.voxn.ai.manager.ExpenseParser.monthStart() }.sumOf { it.amount }
                val progress = viewModel.budgetManager.budgetProgress(monthSpend)
                val exceeded = viewModel.budgetManager.isBudgetExceeded(monthSpend)
                val barColor = if (exceeded) VoxnColors.alertRed else if (progress > 0.8) VoxnColors.warningOrange else VoxnColors.neonGreen

                Box(Modifier.fillMaxWidth().height(6.dp).background(VoxnColors.cardBackground, RoundedCornerShape(3.dp))) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceAtMost(1.0).toFloat()).background(barColor, RoundedCornerShape(3.dp)))
                }
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Budget", style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary)
                    Text(
                        if (exceeded) "Exceeded!" else "₹${(monthlyBudget - monthSpend).toLong()} left",
                        style = VoxnFont.mono(10, FontWeight.Bold), color = barColor,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            val breakdown = viewModel.categoryBreakdown(expenses)
            if (breakdown.isNotEmpty()) {
                val maxAmount = breakdown.maxOf { it.second }
                breakdown.forEach { (cat, amount) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(cat.displayName, style = VoxnFont.caption, color = cat.color, modifier = Modifier.width(80.dp))
                        Box(
                            Modifier.weight(1f).height(6.dp)
                                .background(VoxnColors.cardBackground, MaterialTheme.shapes.small)
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth((amount / maxAmount).toFloat())
                                    .background(cat.color.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("₹${amount.toLong()}", style = VoxnFont.mono(11, FontWeight.Medium), color = VoxnColors.textSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mission Notes
        GlassCard {
            Text("MISSION NOTES", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.cyan, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            val activeCount = notes.count { !it.isCompleted }
            val overdueCount = notes.count { it.isOverdue }
            val upcomingCount = notes.count { it.hasUpcomingReminder }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBadge("Active", activeCount, VoxnColors.electricBlue)
                StatBadge("Overdue", overdueCount, VoxnColors.alertRed)
                StatBadge("Upcoming", upcomingCount, VoxnColors.cyan)
            }
        }
    }
}

@Composable
private fun SpendingStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary, letterSpacing = 1.sp)
        Text(value, style = VoxnFont.mono(18, FontWeight.Bold), color = color)
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = VoxnFont.mono(20, FontWeight.Bold), color = color)
        Text(label, style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary)
    }
}
