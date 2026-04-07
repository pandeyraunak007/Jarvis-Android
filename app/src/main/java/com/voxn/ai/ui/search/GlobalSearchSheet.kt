package com.voxn.ai.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.database.entity.HabitWithCompletions
import com.voxn.ai.data.database.entity.NoteEntity
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import java.text.SimpleDateFormat
import java.util.*

sealed class SearchResult(val type: String, val icon: ImageVector, val color: Color) {
    data class ExpenseResult(val expense: ExpenseEntity) : SearchResult("Expense", Icons.Default.CurrencyRupee, VoxnColors.warningOrange)
    data class HabitResult(val habit: HabitWithCompletions) : SearchResult("Habit", Icons.Default.CheckCircle, VoxnColors.neonGreen)
    data class NoteResult(val note: NoteEntity) : SearchResult("Note", Icons.Default.Description, VoxnColors.cyan)
}

@Composable
fun GlobalSearchSheet(
    expenses: List<ExpenseEntity>,
    habits: List<HabitWithCompletions>,
    notes: List<NoteEntity>,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val results = remember(query, expenses, habits, notes) {
        if (query.length < 2) return@remember emptyList()
        val q = query.lowercase()
        val list = mutableListOf<SearchResult>()

        // Search expenses
        expenses.filter {
            it.merchant.lowercase().contains(q) ||
            it.categoryRaw.lowercase().contains(q) ||
            it.note?.lowercase()?.contains(q) == true
        }.take(5).forEach { list.add(SearchResult.ExpenseResult(it)) }

        // Search habits
        habits.filter {
            it.habit.name.lowercase().contains(q)
        }.take(5).forEach { list.add(SearchResult.HabitResult(it)) }

        // Search notes
        notes.filter {
            it.title.lowercase().contains(q) ||
            it.body.lowercase().contains(q)
        }.take(5).forEach { list.add(SearchResult.NoteResult(it)) }

        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoxnColors.backgroundDark)
            .padding(horizontal = 16.dp)
            .statusBarsPadding(),
    ) {
        Spacer(Modifier.height(8.dp))

        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            placeholder = { Text("Search expenses, habits, notes...", color = VoxnColors.textTertiary) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = VoxnColors.electricBlue) },
            trailingIcon = {
                IconButton(onClick = {
                    if (query.isNotEmpty()) query = "" else onDismiss()
                }) {
                    Icon(Icons.Default.Close, null, tint = VoxnColors.textTertiary)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VoxnColors.electricBlue,
                unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
                focusedTextColor = VoxnColors.textPrimary,
                unfocusedTextColor = VoxnColors.textPrimary,
                cursorColor = VoxnColors.electricBlue,
            ),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )

        Spacer(Modifier.height(16.dp))

        if (query.length < 2) {
            // Hint
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.Search, null, tint = VoxnColors.textTertiary.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Type at least 2 characters to search", style = VoxnFont.caption, color = VoxnColors.textTertiary)
            }
        } else if (results.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Default.SearchOff, null, tint = VoxnColors.textTertiary.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("No results for \"$query\"", style = VoxnFont.caption, color = VoxnColors.textTertiary)
            }
        } else {
            Text("${results.size} result${if (results.size > 1) "s" else ""}", style = VoxnFont.mono(11, FontWeight.Medium), color = VoxnColors.textTertiary)
            Spacer(Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(VoxnColors.cardBackground)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(result.color.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(result.icon, null, tint = result.color, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            when (result) {
                                is SearchResult.ExpenseResult -> {
                                    Text(result.expense.merchant, style = VoxnFont.cardTitle, color = VoxnColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${result.expense.formattedAmount} · ${result.expense.categoryRaw} · ${dateFormat.format(Date(result.expense.date))}", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                                }
                                is SearchResult.HabitResult -> {
                                    Text(result.habit.habit.name, style = VoxnFont.cardTitle, color = VoxnColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${result.habit.habit.frequencyLabel} · Streak: ${result.habit.currentStreak()}", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                                }
                                is SearchResult.NoteResult -> {
                                    Text(result.note.title, style = VoxnFont.cardTitle, color = VoxnColors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${result.note.categoryRaw} · ${result.note.priorityRaw}${if (result.note.isCompleted) " · Done" else ""}", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                                }
                            }
                        }
                        Text(result.type, style = VoxnFont.mono(9, FontWeight.Medium), color = result.color)
                    }
                }
            }
        }
    }
}
