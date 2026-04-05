package com.jarvis.app.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.app.theme.JarvisColors
import com.jarvis.app.theme.JarvisFont
import com.jarvis.app.ui.components.ArcGauge
import com.jarvis.app.ui.components.GlassCard
import com.jarvis.app.viewmodel.HealthViewModel

@Composable
fun HealthScreen(viewModel: HealthViewModel = viewModel()) {
    val healthData by viewModel.healthData.collectAsStateWithLifecycle()
    val weeklyEntries by viewModel.weeklyEntries.collectAsStateWithLifecycle()
    val isAvailable by viewModel.isAvailable.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(JarvisColors.backgroundDark, JarvisColors.backgroundMid, JarvisColors.backgroundDark)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("HEALTH SYSTEMS", style = JarvisFont.mono(22, FontWeight.Bold), color = JarvisColors.electricBlue, letterSpacing = 3.sp)
        Spacer(Modifier.height(24.dp))

        if (!isAvailable) {
            // Authorization prompt
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Favorite, null, tint = JarvisColors.alertRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Health Systems Offline", style = JarvisFont.sectionTitle, color = JarvisColors.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Grant Health Connect access to display steps, calories, sleep, and workout metrics.",
                        style = JarvisFont.cardBody, color = JarvisColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { /* Open Health Connect settings */ },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisColors.electricBlue),
                    ) {
                        Text("ACTIVATE", style = JarvisFont.mono(14, FontWeight.Bold), letterSpacing = 2.sp)
                    }
                }
            }
        } else {
            // Main Gauges
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ArcGauge(healthData.stepsProgress, "STEPS", healthData.stepsFormatted, JarvisColors.electricBlue, 120.dp, 8.dp)
                    ArcGauge(healthData.caloriesProgress, "KCAL", healthData.caloriesFormatted, JarvisColors.warningOrange, 120.dp, 8.dp)
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ArcGauge(healthData.sleepProgress, "SLEEP", healthData.sleepFormatted, JarvisColors.cyan, 120.dp, 8.dp)
                    ArcGauge(healthData.workoutProgress, "WORKOUT", healthData.workoutFormatted, JarvisColors.neonGreen, 120.dp, 8.dp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Detailed Readouts
            GlassCard {
                Text("DETAILED READOUTS", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.electricBlue, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                HealthReadout(Icons.Default.DirectionsWalk, "Steps", healthData.stepsFormatted, "Avg: ${healthData.weeklySteps.toLong()}/day", JarvisColors.electricBlue)
                HealthReadout(Icons.Default.LocalFireDepartment, "Calories", healthData.caloriesFormatted, "Avg: ${healthData.weeklyCalories.toLong()} kcal/day", JarvisColors.warningOrange)
                HealthReadout(Icons.Default.Bedtime, "Sleep", healthData.sleepFormatted, "Goal: 8h", JarvisColors.cyan)
                HealthReadout(Icons.Default.FitnessCenter, "Workout", healthData.workoutFormatted, "Goal: 60 min", JarvisColors.neonGreen)
            }

            Spacer(Modifier.height(16.dp))

            // Weekly Analysis
            if (weeklyEntries.isNotEmpty()) {
                GlassCard {
                    Text("WEEKLY ANALYSIS", style = JarvisFont.mono(14, FontWeight.Bold), color = JarvisColors.electricBlue, letterSpacing = 2.sp)
                    Text("Steps — Last 7 Days", style = JarvisFont.caption, color = JarvisColors.textTertiary)
                    Spacer(Modifier.height(12.dp))
                    val maxSteps = weeklyEntries.maxOfOrNull { it.steps } ?: 1.0
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        weeklyEntries.forEach { entry ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f),
                            ) {
                                val height = if (maxSteps > 0) (entry.steps / maxSteps * 80).dp else 0.dp
                                Box(
                                    Modifier.width(16.dp).height(height)
                                        .background(
                                            Brush.verticalGradient(listOf(JarvisColors.electricBlue, JarvisColors.electricBlue.copy(alpha = 0.4f))),
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(entry.dayLabel, style = JarvisFont.mono(9, FontWeight.Medium), color = JarvisColors.textTertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthReadout(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subValue: String,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = JarvisFont.cardBody, color = JarvisColors.textSecondary)
            Text(subValue, style = JarvisFont.caption, color = JarvisColors.textTertiary)
        }
        Text(value, style = JarvisFont.mono(16, FontWeight.Bold), color = color)
    }
}
