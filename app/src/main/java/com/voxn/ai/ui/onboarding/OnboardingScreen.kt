package com.voxn.ai.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxn.ai.manager.BudgetManager
import com.voxn.ai.manager.UserProfileManager
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont

@Composable
fun OnboardingScreen(
    profileManager: UserProfileManager,
    budgetManager: BudgetManager,
    onComplete: () -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(0) }

    // Step 1 state
    var name by remember { mutableStateOf("") }

    // Step 2 state
    var budget by remember { mutableStateOf("") }
    var savings by remember { mutableStateOf("") }

    // Step 3 state
    var notifGranted by remember { mutableStateOf(false) }
    var calendarGranted by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
    }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        calendarGranted = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoxnColors.backgroundDark, VoxnColors.backgroundMid, VoxnColors.backgroundDark)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))

        // Logo
        Text("V.O.X.N.", style = VoxnFont.mono(32, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 6.sp)
        Spacer(Modifier.height(4.dp))
        Text("AI", style = VoxnFont.mono(18, FontWeight.Medium), color = VoxnColors.cyan, letterSpacing = 4.sp)

        Spacer(Modifier.height(40.dp))

        // Step indicators
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0..2) {
                Box(
                    modifier = Modifier
                        .width(if (i == currentStep) 32.dp else 12.dp)
                        .height(4.dp)
                        .background(
                            if (i <= currentStep) VoxnColors.electricBlue else VoxnColors.textTertiary.copy(alpha = 0.3f),
                            RoundedCornerShape(2.dp),
                        )
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // Step content
        AnimatedContent(targetState = currentStep, label = "step") { step ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (step) {
                    0 -> StepWelcome(name = name, onNameChange = { name = it })
                    1 -> StepBudget(budget = budget, savings = savings, onBudgetChange = { budget = it }, onSavingsChange = { savings = it })
                    2 -> StepPermissions(
                        notifGranted = notifGranted,
                        calendarGranted = calendarGranted,
                        onRequestNotif = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else notifGranted = true
                        },
                        onRequestCalendar = { calendarLauncher.launch(Manifest.permission.READ_CALENDAR) },
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Navigation buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (currentStep > 0) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxnColors.textTertiary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.textTertiary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Back", style = VoxnFont.mono(14, FontWeight.Medium))
                }
            }
            Button(
                onClick = {
                    if (currentStep < 2) {
                        currentStep++
                    } else {
                        // Save and complete
                        if (name.isNotBlank()) profileManager.setUserName(name)
                        budget.toDoubleOrNull()?.let { budgetManager.setMonthlyBudget(it) }
                        savings.toDoubleOrNull()?.let { budgetManager.setSavingsGoal(it) }
                        profileManager.completeOnboarding()
                        onComplete()
                    }
                },
                modifier = Modifier.weight(if (currentStep > 0) 1f else 2f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.electricBlue),
                shape = RoundedCornerShape(14.dp),
                enabled = when (currentStep) {
                    0 -> name.isNotBlank()
                    else -> true
                },
            ) {
                Text(
                    if (currentStep < 2) "Continue" else "Get Started",
                    style = VoxnFont.mono(14, FontWeight.Bold),
                    color = VoxnColors.backgroundDark,
                )
                if (currentStep < 2) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null, tint = VoxnColors.backgroundDark, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (currentStep < 2) {
            Text(
                "Skip for now",
                style = VoxnFont.caption,
                color = VoxnColors.textTertiary,
                modifier = Modifier.clickable {
                    profileManager.completeOnboarding()
                    onComplete()
                },
            )
        }
    }
}

@Composable
private fun StepWelcome(name: String, onNameChange: (String) -> Unit) {
    Icon(Icons.Default.Person, null, tint = VoxnColors.electricBlue, modifier = Modifier.size(56.dp))
    Spacer(Modifier.height(24.dp))
    Text("What should we call you?", style = VoxnFont.sectionTitle, color = VoxnColors.textPrimary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text("This personalizes your dashboard experience", style = VoxnFont.cardBody, color = VoxnColors.textTertiary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        placeholder = { Text("Your name", color = VoxnColors.textTertiary) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VoxnColors.electricBlue,
            unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
            focusedTextColor = VoxnColors.textPrimary,
            unfocusedTextColor = VoxnColors.textPrimary,
            cursorColor = VoxnColors.electricBlue,
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun StepBudget(budget: String, savings: String, onBudgetChange: (String) -> Unit, onSavingsChange: (String) -> Unit) {
    Icon(Icons.Default.AccountBalanceWallet, null, tint = VoxnColors.neonGreen, modifier = Modifier.size(56.dp))
    Spacer(Modifier.height(24.dp))
    Text("Set your financial goals", style = VoxnFont.sectionTitle, color = VoxnColors.textPrimary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text("You can always change these later in settings", style = VoxnFont.cardBody, color = VoxnColors.textTertiary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))
    OutlinedTextField(
        value = budget,
        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,8}$"))) onBudgetChange(it) },
        label = { Text("Monthly Budget (₹)", color = VoxnColors.textTertiary) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VoxnColors.neonGreen,
            unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
            focusedTextColor = VoxnColors.textPrimary,
            unfocusedTextColor = VoxnColors.textPrimary,
            cursorColor = VoxnColors.neonGreen,
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = savings,
        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,8}$"))) onSavingsChange(it) },
        label = { Text("Monthly Savings Goal (₹)", color = VoxnColors.textTertiary) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VoxnColors.neonGreen,
            unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
            focusedTextColor = VoxnColors.textPrimary,
            unfocusedTextColor = VoxnColors.textPrimary,
            cursorColor = VoxnColors.neonGreen,
        ),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(Modifier.height(8.dp))
    Text("Leave empty to skip", style = VoxnFont.caption, color = VoxnColors.textTertiary)
}

@Composable
private fun StepPermissions(
    notifGranted: Boolean,
    calendarGranted: Boolean,
    onRequestNotif: () -> Unit,
    onRequestCalendar: () -> Unit,
) {
    Icon(Icons.Default.Security, null, tint = VoxnColors.cyan, modifier = Modifier.size(56.dp))
    Spacer(Modifier.height(24.dp))
    Text("Enable permissions", style = VoxnFont.sectionTitle, color = VoxnColors.textPrimary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text("These help Voxn AI send reminders and show your calendar", style = VoxnFont.cardBody, color = VoxnColors.textTertiary, textAlign = TextAlign.Center)
    Spacer(Modifier.height(32.dp))

    PermissionRow(
        icon = Icons.Default.Notifications,
        title = "Notifications",
        description = "Habit reminders & note alerts",
        granted = notifGranted,
        onRequest = onRequestNotif,
        color = VoxnColors.warningOrange,
    )
    Spacer(Modifier.height(12.dp))
    PermissionRow(
        icon = Icons.Default.CalendarMonth,
        title = "Calendar",
        description = "Show today's events on dashboard",
        granted = calendarGranted,
        onRequest = onRequestCalendar,
        color = VoxnColors.purple,
    )
    Spacer(Modifier.height(12.dp))
    PermissionRow(
        icon = Icons.Default.Favorite,
        title = "Health Connect",
        description = "Steps, calories, sleep, workout",
        granted = false,
        onRequest = { /* Health Connect uses its own permission flow */ },
        color = VoxnColors.alertRed,
        note = "Requested when you open Health tab",
    )
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    onRequest: () -> Unit,
    color: Color,
    note: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(VoxnColors.cardBackground)
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(enabled = !granted) { onRequest() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = VoxnFont.cardTitle, color = VoxnColors.textPrimary)
            Text(note ?: description, style = VoxnFont.caption, color = VoxnColors.textTertiary)
        }
        if (granted) {
            Icon(Icons.Default.CheckCircle, null, tint = VoxnColors.neonGreen, modifier = Modifier.size(24.dp))
        } else if (note == null) {
            Text("GRANT", style = VoxnFont.mono(11, FontWeight.Bold), color = color)
        }
    }
}
