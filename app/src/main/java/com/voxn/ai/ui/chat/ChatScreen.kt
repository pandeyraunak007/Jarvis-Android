package com.voxn.ai.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voxn.ai.data.model.ChatMessage
import com.voxn.ai.manager.KeystoreManager
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.GlassCard
import com.voxn.ai.viewmodel.ChatViewModel

@Composable
fun ChatScreen(onBack: () -> Unit, vm: ChatViewModel = viewModel()) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val isStreaming by vm.isStreaming.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (!KeystoreManager.hasApiKey(context)) showSettings = true
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    DisposableEffect(Unit) {
        onDispose { vm.onDismiss() }
    }

    if (showSettings) {
        SettingsSheet(onDismiss = { showSettings = false })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(VoxnColors.backgroundDark).statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = VoxnColors.textSecondary)
            }
            Box(Modifier.size(10.dp).clip(CircleShape).background(VoxnColors.electricBlue))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("JARVIS", style = VoxnFont.mono(16, FontWeight.Bold), color = VoxnColors.textPrimary, letterSpacing = 3.sp)
                Text(
                    if (isStreaming) "THINKING…" else "ONLINE",
                    style = VoxnFont.mono(9, FontWeight.Medium),
                    color = VoxnColors.electricBlue.copy(alpha = 0.7f),
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            if (messages.isNotEmpty()) {
                IconButton(onClick = { vm.clear() }) {
                    Icon(Icons.Default.Delete, "Clear", tint = VoxnColors.textTertiary, modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, "Settings", tint = VoxnColors.textSecondary, modifier = Modifier.size(20.dp))
            }
        }
        Divider(color = VoxnColors.electricBlue.copy(alpha = 0.3f), thickness = 0.5.dp)

        // Messages or empty state
        if (messages.isEmpty()) {
            EmptyState(onSuggestion = { vm.send(it) }, isStreaming = isStreaming, modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(messages, key = { it.id }) { msg -> MessageRow(msg) }
            }
        }

        // Error
        errorMessage?.let {
            Text(it, style = VoxnFont.caption, color = VoxnColors.alertRed, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }

        // Input bar
        Divider(color = VoxnColors.electricBlue.copy(alpha = 0.2f), thickness = 0.5.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Message JARVIS…", style = VoxnFont.cardBody, color = VoxnColors.textTertiary) },
                textStyle = VoxnFont.cardBody.copy(color = VoxnColors.textPrimary),
                modifier = Modifier.weight(1f),
                maxLines = 5,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VoxnColors.electricBlue.copy(alpha = 0.5f),
                    unfocusedBorderColor = VoxnColors.electricBlue.copy(alpha = 0.2f),
                    cursorColor = VoxnColors.electricBlue,
                    focusedContainerColor = VoxnColors.cardBackground,
                    unfocusedContainerColor = VoxnColors.cardBackground,
                ),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (isStreaming) vm.cancel()
                    else { vm.send(input); input = "" }
                },
                enabled = isStreaming || input.isNotBlank(),
                modifier = Modifier.size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(VoxnColors.electricBlue, VoxnColors.cyan)),
                        CircleShape,
                    ),
            ) {
                Icon(
                    if (isStreaming) Icons.Default.Stop else Icons.Default.ArrowUpward,
                    "Send",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onSuggestion: (String) -> Unit, isStreaming: Boolean, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(100.dp).clip(CircleShape).background(VoxnColors.electricBlue.copy(alpha = 0.1f)))
            Box(Modifier.size(60.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(VoxnColors.electricBlue, VoxnColors.cyan))
            ))
            Icon(Icons.Default.GraphicEq, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("AT YOUR SERVICE", style = VoxnFont.mono(11, FontWeight.SemiBold), color = VoxnColors.electricBlue, letterSpacing = 3.sp)
        Spacer(Modifier.height(6.dp))
        Text("Ask about spending, habits, health, or calendar.\nI can also log expenses for you.",
            style = VoxnFont.cardBody, color = VoxnColors.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        listOf(
            "How am I doing today?",
            "How much did I spend this month?",
            "Which habits still need doing?",
            "Log ₹240 Swiggy food",
        ).forEach { text ->
            TextButton(
                onClick = { if (!isStreaming) onSuggestion(text) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = VoxnColors.electricBlue, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text, style = VoxnFont.cardBody, color = VoxnColors.textPrimary)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.NorthEast, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: ChatMessage) {
    when (message.role) {
        ChatMessage.Role.USER -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    message.content,
                    style = VoxnFont.cardBody,
                    color = VoxnColors.textPrimary,
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .background(VoxnColors.electricBlue.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
        ChatMessage.Role.ASSISTANT -> {
            Column(Modifier.fillMaxWidth().animateContentSize()) {
                message.toolCalls.forEach { tc -> ToolChip(tc.name) }
                if (message.content.isNotEmpty() || message.status == ChatMessage.Status.STREAMING) {
                    val text = if (message.content.isEmpty()) " " else message.content
                    val suffix = if (message.status == ChatMessage.Status.STREAMING) " ▍" else ""
                    Text(
                        text + suffix,
                        style = VoxnFont.cardBody,
                        color = if (message.status == ChatMessage.Status.ERROR) VoxnColors.alertRed else VoxnColors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VoxnColors.cardBackground, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
        }
        ChatMessage.Role.TOOL -> { /* hidden */ }
    }
}

@Composable
private fun ToolChip(name: String) {
    val label = when (name) {
        "get_today_summary" -> "SCANNING TODAY'S DATA"
        "get_spending" -> "QUERYING SPENDING"
        "get_habits" -> "CHECKING HABITS"
        "get_health" -> "READING HEALTH"
        "get_notes" -> "SEARCHING NOTES"
        "get_calendar" -> "CHECKING CALENDAR"
        "log_expense" -> "LOGGING EXPENSE"
        else -> name.uppercase()
    }
    Row(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .background(VoxnColors.electricBlue.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Hearing, null, tint = VoxnColors.electricBlue, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = VoxnFont.mono(10, FontWeight.SemiBold), color = VoxnColors.electricBlue, letterSpacing = 1.5.sp)
    }
}
