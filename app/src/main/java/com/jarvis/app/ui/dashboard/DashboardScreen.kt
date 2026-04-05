package com.jarvis.app.ui.dashboard

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
import com.jarvis.app.theme.JarvisColors
import com.jarvis.app.theme.JarvisFont
import com.jarvis.app.ui.components.ArcGauge
import com.jarvis.app.ui.components.GlassCard
import com.jarvis.app.ui.components.ProgressRing
import com.jarvis.app.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val healthData by viewModel.healthData.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(JarvisColors.backgroundDark, JarvisColors.backgroundMid, JarvisColors.backgroundDark)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Text(
            text = "V.O.X.N.",
            style = JarvisFont.mono(28, FontWeight.Bold),
            color = JarvisColors.electricBlue,
            letterSpacing = 4.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = viewModel.greeting,
            style = JarvisFont.mono(16, FontWeight.Medium),
            color = JarvisColors.textSecondary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = viewModel.currentDate.uppercase(),
            style = JarvisFont.mono(11, FontWeight.Medium),
            color = JarvisColors.textTertiary,
            letterSpacing = 3.sp,
        )

        Spacer(Modifier.height(24.dp))

        // Today's Calendar
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, null, tint = JarvisColors.purple, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("TODAY'S SCHEDULE", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.purple, letterSpacing = 2.sp)
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
                style = JarvisFont.mono(11, FontWeight.Medium),
                color = JarvisColors.textTertiary,
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
                            style = JarvisFont.mono(10, FontWeight.Medium),
                            color = if (isToday) JarvisColors.purple else JarvisColors.textTertiary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier.size(32.dp)
                                .then(
                                    if (isToday) Modifier.background(JarvisColors.purple.copy(alpha = 0.2f), CircleShape)
                                        .border(1.5.dp, JarvisColors.purple, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$day",
                                style = JarvisFont.mono(13, if (isToday) FontWeight.Bold else FontWeight.Normal),
                                color = if (isToday) JarvisColors.purple else JarvisColors.textSecondary,
                            )
                        }
                    }
                    weekCal.add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            Spacer(Modifier.height(16.dp))

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
                    Icon(Icons.Default.CheckCircle, null, tint = JarvisColors.neonGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("No tasks due today. You're all clear!", style = JarvisFont.caption, color = JarvisColors.textTertiary)
                }
            } else {
                if (overdueNotes.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = JarvisColors.alertRed, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${overdueNotes.size} overdue", style = JarvisFont.mono(11, FontWeight.Bold), color = JarvisColors.alertRed)
                    }
                    overdueNotes.take(3).forEach { note ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(6.dp).background(JarvisColors.alertRed, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(note.title, style = JarvisFont.caption, color = JarvisColors.textSecondary, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (dueTodayNotes.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Today, null, tint = JarvisColors.electricBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${dueTodayNotes.size} due today", style = JarvisFont.mono(11, FontWeight.Bold), color = JarvisColors.electricBlue)
                    }
                    dueTodayNotes.take(3).forEach { note ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(6.dp).background(note.priority.color, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(note.title, style = JarvisFont.caption, color = JarvisColors.textSecondary, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Health Systems HUD
        GlassCard {
            Text("HEALTH SYSTEMS", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.electricBlue, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ArcGauge(healthData.stepsProgress, "STEPS", healthData.stepsFormatted, JarvisColors.electricBlue)
                ArcGauge(healthData.caloriesProgress, "KCAL", healthData.caloriesFormatted, JarvisColors.warningOrange)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ArcGauge(healthData.sleepProgress, "SLEEP", healthData.sleepFormatted, JarvisColors.cyan)
                ArcGauge(healthData.workoutProgress, "WORKOUT", healthData.workoutFormatted, JarvisColors.neonGreen)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Habit Protocol
        GlassCard {
            Text("HABIT PROTOCOL", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.neonGreen, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val completed = habits.count { it.isCompletedToday() }
                val total = habits.size
                val rate = if (total > 0) completed.toDouble() / total else 0.0

                ProgressRing(rate, JarvisColors.neonGreen, 60.dp, 5.dp) {
                    Text(
                        "$completed/$total",
                        style = JarvisFont.mono(12, FontWeight.Bold),
                        color = JarvisColors.neonGreen,
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Habits Completed", style = JarvisFont.cardTitle, color = JarvisColors.textPrimary)
                    Text("$completed tasks done today", style = JarvisFont.caption, color = JarvisColors.textTertiary)
                    Spacer(Modifier.height(4.dp))
                    val longestStreak = habits.maxOfOrNull { it.currentStreak() } ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = JarvisColors.warningOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Longest streak: $longestStreak days", style = JarvisFont.caption, color = JarvisColors.warningOrange)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Financial Monitor
        GlassCard {
            Text("FINANCIAL MONITOR", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.warningOrange, letterSpacing = 2.sp)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SpendingStat("TODAY", viewModel.todaySpending(expenses), JarvisColors.warningOrange)
                SpendingStat("WEEK", viewModel.weeklySpending(expenses), JarvisColors.electricBlue)
                SpendingStat("MONTH", viewModel.monthlySpending(expenses), JarvisColors.cyan)
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
                        Text(cat.displayName, style = JarvisFont.caption, color = cat.color, modifier = Modifier.width(80.dp))
                        Box(
                            Modifier.weight(1f).height(6.dp)
                                .background(JarvisColors.cardBackground, MaterialTheme.shapes.small)
                        ) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth((amount / maxAmount).toFloat())
                                    .background(cat.color.copy(alpha = 0.7f), MaterialTheme.shapes.small)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("₹${amount.toLong()}", style = JarvisFont.mono(11, FontWeight.Medium), color = JarvisColors.textSecondary)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mission Notes
        GlassCard {
            Text("MISSION NOTES", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.cyan, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            val activeCount = notes.count { !it.isCompleted }
            val overdueCount = notes.count { it.isOverdue }
            val upcomingCount = notes.count { it.hasUpcomingReminder }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatBadge("Active", activeCount, JarvisColors.electricBlue)
                StatBadge("Overdue", overdueCount, JarvisColors.alertRed)
                StatBadge("Upcoming", upcomingCount, JarvisColors.cyan)
            }
        }
    }
}

@Composable
private fun SpendingStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = JarvisFont.mono(10, FontWeight.Medium), color = JarvisColors.textTertiary, letterSpacing = 1.sp)
        Text(value, style = JarvisFont.mono(18, FontWeight.Bold), color = color)
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = JarvisFont.mono(20, FontWeight.Bold), color = color)
        Text(label, style = JarvisFont.mono(10, FontWeight.Medium), color = JarvisColors.textTertiary)
    }
}
