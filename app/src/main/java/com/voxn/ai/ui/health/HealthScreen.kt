package com.voxn.ai.ui.health

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
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.ArcGauge
import com.voxn.ai.ui.components.GlassCard
import com.voxn.ai.viewmodel.HealthViewModel

@Composable
fun HealthScreen(viewModel: HealthViewModel = viewModel()) {
    val healthData by viewModel.healthData.collectAsStateWithLifecycle()
    val weeklyEntries by viewModel.weeklyEntries.collectAsStateWithLifecycle()
    val isAvailable by viewModel.isAvailable.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoxnColors.backgroundDark, VoxnColors.backgroundMid, VoxnColors.backgroundDark)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("HEALTH SYSTEMS", style = VoxnFont.mono(22, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 3.sp)
        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = VoxnColors.electricBlue, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(12.dp))
            Text("Fetching health data...", style = VoxnFont.caption, color = VoxnColors.textTertiary)
            Spacer(Modifier.height(32.dp))
        }

        if (!isAvailable) {
            // Authorization prompt
            GlassCard {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Favorite, null, tint = VoxnColors.alertRed, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Health Systems Offline", style = VoxnFont.sectionTitle, color = VoxnColors.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Grant Health Connect access to display steps, calories, sleep, and workout metrics.",
                        style = VoxnFont.cardBody, color = VoxnColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { /* Open Health Connect settings */ },
                        colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.electricBlue),
                    ) {
                        Text("ACTIVATE", style = VoxnFont.mono(14, FontWeight.Bold), letterSpacing = 2.sp)
                    }
                }
            }
        } else {
            // Main Gauges
            GlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ArcGauge(healthData.stepsProgress, "STEPS", healthData.stepsFormatted, VoxnColors.electricBlue, 120.dp, 8.dp)
                    ArcGauge(healthData.caloriesProgress, "KCAL", healthData.caloriesFormatted, VoxnColors.warningOrange, 120.dp, 8.dp)
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ArcGauge(healthData.sleepProgress, "SLEEP", healthData.sleepFormatted, VoxnColors.cyan, 120.dp, 8.dp)
                    ArcGauge(healthData.workoutProgress, "WORKOUT", healthData.workoutFormatted, VoxnColors.neonGreen, 120.dp, 8.dp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Detailed Readouts
            GlassCard {
                Text("DETAILED READOUTS", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
                Spacer(Modifier.height(12.dp))
                @Suppress("DEPRECATION") HealthReadout(Icons.Default.DirectionsWalk, "Steps", healthData.stepsFormatted, "Avg: ${healthData.weeklySteps.toLong()}/day", VoxnColors.electricBlue)
                HealthReadout(Icons.Default.LocalFireDepartment, "Calories", healthData.caloriesFormatted, "Avg: ${healthData.weeklyCalories.toLong()} kcal/day", VoxnColors.warningOrange)
                HealthReadout(Icons.Default.Bedtime, "Sleep", healthData.sleepFormatted, "Goal: 8h", VoxnColors.cyan)
                HealthReadout(Icons.Default.FitnessCenter, "Workout", healthData.workoutFormatted, "Goal: 60 min", VoxnColors.neonGreen)
            }

            Spacer(Modifier.height(16.dp))

            // Weekly Analysis
            if (weeklyEntries.isNotEmpty()) {
                GlassCard {
                    Text("WEEKLY ANALYSIS", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
                    Text("Steps — Last 7 Days", style = VoxnFont.caption, color = VoxnColors.textTertiary)
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
                                            Brush.verticalGradient(listOf(VoxnColors.electricBlue, VoxnColors.electricBlue.copy(alpha = 0.4f))),
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(entry.dayLabel, style = VoxnFont.mono(9, FontWeight.Medium), color = VoxnColors.textTertiary)
                            }
                        }
                    }
                }
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
    )
    } // end Box
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
            Text(label, style = VoxnFont.cardBody, color = VoxnColors.textSecondary)
            Text(subValue, style = VoxnFont.caption, color = VoxnColors.textTertiary)
        }
        Text(value, style = VoxnFont.mono(16, FontWeight.Bold), color = color)
    }
}
