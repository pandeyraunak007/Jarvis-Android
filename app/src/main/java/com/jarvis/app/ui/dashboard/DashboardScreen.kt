package com.jarvis.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
            text = "J.A.R.V.I.S.",
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
private fun SpendingStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = JarvisFont.mono(10, FontWeight.Medium), color = JarvisColors.textTertiary, letterSpacing = 1.sp)
        Text(value, style = JarvisFont.mono(18, FontWeight.Bold), color = color)
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = JarvisFont.mono(20, FontWeight.Bold), color = color)
        Text(label, style = JarvisFont.mono(10, FontWeight.Medium), color = JarvisColors.textTertiary)
    }
}
