package com.voxn.ai.ui.chat

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxn.ai.manager.JarvisMemoryManager
import com.voxn.ai.manager.KeystoreManager
import com.voxn.ai.manager.SmartNotificationManager
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.GlassCard

@Composable
fun SettingsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var key by remember { mutableStateOf(KeystoreManager.loadApiKey(context) ?: "") }
    var showKey by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(KeystoreManager.hasApiKey(context)) }
    val memoryManager = remember { JarvisMemoryManager(context) }
    var memories by remember { mutableStateOf(memoryManager.getMemories()) }
    val smartNotifs = remember { SmartNotificationManager(context) }
    var notifsEnabled by remember { mutableStateOf(smartNotifs.isEnabled) }

    Column(
        modifier = Modifier.fillMaxSize().background(VoxnColors.backgroundDark).statusBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SETTINGS", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.electricBlue, letterSpacing = 3.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Close", tint = VoxnColors.textSecondary)
            }
        }
        Spacer(Modifier.height(20.dp))

        // API Key
        Text("GROQ API KEY", style = VoxnFont.mono(11, FontWeight.SemiBold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = key,
            onValueChange = { key = it; isSaved = false },
            placeholder = { Text("gsk_...", style = VoxnFont.mono(13), color = VoxnColors.textTertiary) },
            textStyle = VoxnFont.mono(13).copy(color = VoxnColors.textPrimary),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = VoxnColors.textSecondary)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VoxnColors.electricBlue.copy(alpha = 0.5f),
                unfocusedBorderColor = VoxnColors.electricBlue.copy(alpha = 0.2f),
                cursorColor = VoxnColors.electricBlue,
                focusedContainerColor = VoxnColors.cardBackground,
                unfocusedContainerColor = VoxnColors.cardBackground,
            ),
            singleLine = true,
        )
        Text("Stored encrypted. Never leaves your device except for Groq API calls.", style = VoxnFont.caption, color = VoxnColors.textTertiary,
            modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (key.isNotBlank()) { KeystoreManager.saveApiKey(context, key.trim()); isSaved = true }
            },
            enabled = key.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.electricBlue),
        ) {
            Icon(if (isSaved) Icons.Default.CheckCircle else Icons.Default.Shield, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (isSaved) "SAVED" else "SAVE KEY", style = VoxnFont.mono(12, FontWeight.Bold), color = Color.Black, letterSpacing = 2.sp)
        }

        if (KeystoreManager.hasApiKey(context)) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { KeystoreManager.deleteApiKey(context); key = ""; isSaved = false },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxnColors.alertRed),
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("REMOVE KEY", style = VoxnFont.mono(11, FontWeight.SemiBold), letterSpacing = 2.sp)
            }
        }

        Spacer(Modifier.height(28.dp))

        // Memory section
        Text("JARVIS MEMORY", style = VoxnFont.mono(11, FontWeight.SemiBold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        if (memories.isEmpty()) {
            Text("No memories yet. JARVIS will learn about you as you chat.", style = VoxnFont.cardBody, color = VoxnColors.textTertiary)
        } else {
            GlassCard {
                memories.forEachIndexed { index, fact ->
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Default.Psychology, null, tint = VoxnColors.electricBlue.copy(alpha = 0.6f), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(fact, style = VoxnFont.cardBody, color = VoxnColors.textSecondary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { memoryManager.remove(index); memories = memoryManager.getMemories() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Cancel, null, tint = VoxnColors.textTertiary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                    }
                    if (index < memories.lastIndex) Divider(color = VoxnColors.electricBlue.copy(alpha = 0.1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { memoryManager.removeAll(); memories = emptyList() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxnColors.warningOrange),
            ) {
                Icon(Icons.Default.Psychology, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("CLEAR ALL MEMORIES", style = VoxnFont.mono(10, FontWeight.SemiBold), letterSpacing = 2.sp)
            }
        }
        Text("JARVIS learns facts from conversations. Stored only on your device.", style = VoxnFont.caption, color = VoxnColors.textTertiary,
            modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(28.dp))

        // Smart notifications toggle
        Text("SMART NOTIFICATIONS", style = VoxnFont.mono(11, FontWeight.SemiBold), color = VoxnColors.electricBlue, letterSpacing = 2.sp)
        Spacer(Modifier.height(8.dp))
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Proactive Alerts", style = VoxnFont.cardBody, color = VoxnColors.textPrimary)
                    Text("Morning briefs, expense reminders, habit warnings, budget alerts.", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                }
                Switch(
                    checked = notifsEnabled,
                    onCheckedChange = {
                        notifsEnabled = it; smartNotifs.isEnabled = it
                        if (!it) smartNotifs.removeAll()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = VoxnColors.electricBlue),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // How to get a key
        GlassCard {
            Text("HOW TO GET A KEY", style = VoxnFont.mono(10, FontWeight.SemiBold), color = VoxnColors.electricBlue.copy(alpha = 0.8f), letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            listOf("01" to "Go to console.groq.com and sign in", "02" to "Open API Keys, create a new key", "03" to "Paste it above, then Save").forEach { (num, text) ->
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(num, style = VoxnFont.mono(10, FontWeight.Bold), color = VoxnColors.electricBlue)
                    Spacer(Modifier.width(10.dp))
                    Text(text, style = VoxnFont.cardBody, color = VoxnColors.textSecondary)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Groq offers a generous free tier for Llama 3.3 70B.", style = VoxnFont.caption, color = VoxnColors.textTertiary)
        }
    }
}
