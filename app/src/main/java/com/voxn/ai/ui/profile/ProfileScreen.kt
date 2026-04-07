package com.voxn.ai.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxn.ai.manager.BudgetManager
import com.voxn.ai.manager.UserProfileManager
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.GlassCard

@Composable
fun ProfileScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val profileManager = remember { UserProfileManager(context) }
    val budgetManager = remember { BudgetManager(context) }

    var name by remember { mutableStateOf(profileManager.userName.value) }
    var budget by remember { mutableStateOf(if (budgetManager.monthlyBudget.value > 0) budgetManager.monthlyBudget.value.toLong().toString() else "") }
    var savings by remember { mutableStateOf(if (budgetManager.savingsGoal.value > 0) budgetManager.savingsGoal.value.toLong().toString() else "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoxnColors.backgroundDark, VoxnColors.backgroundMid, VoxnColors.backgroundDark)))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, null, tint = VoxnColors.textSecondary)
            }
            Spacer(Modifier.weight(1f))
            Text("PROFILE", style = VoxnFont.mono(22, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp)) // balance
        }

        Spacer(Modifier.height(24.dp))

        // Avatar
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(VoxnColors.electricBlue.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, VoxnColors.electricBlue.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "V",
                style = VoxnFont.mono(32, FontWeight.Bold),
                color = VoxnColors.electricBlue,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Name
        GlassCard {
            SettingSection(icon = Icons.Default.Person, title = "DISPLAY NAME", color = VoxnColors.electricBlue)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
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
                shape = RoundedCornerShape(10.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // Budget & Goals
        GlassCard {
            SettingSection(icon = Icons.Default.AccountBalanceWallet, title = "FINANCIAL GOALS", color = VoxnColors.neonGreen)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = budget,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,8}$"))) budget = it },
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
                shape = RoundedCornerShape(10.dp),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = savings,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,8}$"))) savings = it },
                label = { Text("Savings Goal (₹)", color = VoxnColors.textTertiary) },
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
                shape = RoundedCornerShape(10.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // App Info
        GlassCard {
            SettingSection(icon = Icons.Default.Info, title = "ABOUT", color = VoxnColors.cyan)
            Spacer(Modifier.height(8.dp))
            InfoRow("App", "Voxn AI")
            InfoRow("Version", "1.0")
            InfoRow("Package", "com.voxn.ai")
        }

        Spacer(Modifier.height(24.dp))

        // Save button
        Button(
            onClick = {
                profileManager.setUserName(name)
                budgetManager.setMonthlyBudget(budget.toDoubleOrNull() ?: 0.0)
                budgetManager.setSavingsGoal(savings.toDoubleOrNull() ?: 0.0)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.electricBlue),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text("SAVE CHANGES", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.backgroundDark, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun SettingSection(icon: ImageVector, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = VoxnFont.mono(12, FontWeight.Bold), color = color, letterSpacing = 2.sp)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = VoxnFont.cardBody, color = VoxnColors.textTertiary)
        Text(value, style = VoxnFont.mono(13, FontWeight.Medium), color = VoxnColors.textSecondary)
    }
}
