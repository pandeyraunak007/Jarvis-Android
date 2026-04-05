package com.jarvis.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jarvis.app.data.database.entity.NoteEntity
import com.jarvis.app.data.model.NoteCategory
import com.jarvis.app.data.model.NotePriority
import com.jarvis.app.theme.JarvisColors
import com.jarvis.app.theme.JarvisFont
import com.jarvis.app.ui.components.GlassCard
import com.jarvis.app.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(viewModel: NoteViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
    val filterPriority by viewModel.filterPriority.collectAsStateWithLifecycle()

    val filteredNotes = viewModel.filteredNotes()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(JarvisColors.backgroundDark, JarvisColors.backgroundMid, JarvisColors.backgroundDark)))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("MISSION NOTES", style = JarvisFont.mono(22, FontWeight.Bold), color = JarvisColors.cyan, letterSpacing = 3.sp)
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { viewModel.setSearch(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search notes...", color = JarvisColors.textTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = JarvisColors.textTertiary) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearch("") }) {
                                Icon(Icons.Default.Clear, null, tint = JarvisColors.textTertiary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisColors.cyan,
                        unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f),
                        focusedTextColor = JarvisColors.textPrimary,
                        unfocusedTextColor = JarvisColors.textPrimary,
                        cursorColor = JarvisColors.cyan,
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Category filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filterCategory == null,
                            onClick = { viewModel.setFilterCategory(null) },
                            label = { Text("All", style = JarvisFont.mono(11, FontWeight.Medium)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = JarvisColors.cyan.copy(alpha = 0.2f),
                                selectedLabelColor = JarvisColors.cyan,
                                labelColor = JarvisColors.textTertiary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = filterCategory == null,
                                borderColor = JarvisColors.textTertiary.copy(alpha = 0.3f),
                                selectedBorderColor = JarvisColors.cyan,
                            ),
                        )
                    }
                    items(NoteCategory.entries.toList()) { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { viewModel.setFilterCategory(if (filterCategory == cat) null else cat) },
                            label = { Text(cat.displayName, style = JarvisFont.mono(11, FontWeight.Medium)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color.copy(alpha = 0.2f),
                                selectedLabelColor = cat.color,
                                labelColor = JarvisColors.textTertiary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = filterCategory == cat,
                                borderColor = JarvisColors.textTertiary.copy(alpha = 0.3f),
                                selectedBorderColor = cat.color,
                            ),
                        )
                    }
                    // Priority filter
                    items(NotePriority.entries.toList()) { pri ->
                        FilterChip(
                            selected = filterPriority == pri,
                            onClick = { viewModel.setFilterPriority(if (filterPriority == pri) null else pri) },
                            label = { Text(pri.displayName, style = JarvisFont.mono(11, FontWeight.Medium)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pri.color.copy(alpha = 0.2f),
                                selectedLabelColor = pri.color,
                                labelColor = JarvisColors.textTertiary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = filterPriority == pri,
                                borderColor = JarvisColors.textTertiary.copy(alpha = 0.3f),
                                selectedBorderColor = pri.color,
                            ),
                        )
                    }
                }
            }

            // Stats bar
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBadge("Active", viewModel.activeCount, JarvisColors.electricBlue)
                    StatBadge("Overdue", viewModel.overdueCount, JarvisColors.alertRed)
                    StatBadge("Upcoming", viewModel.upcomingCount, JarvisColors.cyan)
                }
            }

            // Notes list
            if (filteredNotes.isEmpty()) {
                item {
                    GlassCard {
                        Text("No notes found.", style = JarvisFont.cardBody, color = JarvisColors.textTertiary, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            items(filteredNotes, key = { it.id }) { note ->
                NoteRow(note, viewModel)
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { viewModel.showAdd() },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 100.dp).size(56.dp),
            containerColor = JarvisColors.cyan,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, "Add", tint = JarvisColors.backgroundDark, modifier = Modifier.size(22.dp))
        }
    }

    if (showAddDialog) { AddEditNoteDialog(viewModel) }
}

@Composable
private fun StatBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = JarvisFont.mono(20, FontWeight.Bold), color = color)
        Text(label, style = JarvisFont.mono(10, FontWeight.Medium), color = JarvisColors.textTertiary)
    }
}

@Composable
private fun NoteRow(note: NoteEntity, viewModel: NoteViewModel) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    GlassCard(
        modifier = Modifier.then(
            Modifier.border(
                width = 2.dp,
                color = note.priority.color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            )
        )
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(Icons.Default.PushPin, null, tint = JarvisColors.electricBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        note.title,
                        style = JarvisFont.cardTitle,
                        color = if (note.isCompleted) JarvisColors.textTertiary else JarvisColors.textPrimary,
                        textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (note.body.isNotBlank()) {
                    Text(note.body, style = JarvisFont.caption, color = JarvisColors.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Category badge
                    Box(
                        modifier = Modifier.background(note.category.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(note.category.displayName, style = JarvisFont.mono(9, FontWeight.Medium), color = note.category.color)
                    }
                    // Priority badge
                    Box(
                        modifier = Modifier.background(note.priority.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(note.priority.displayName, style = JarvisFont.mono(9, FontWeight.Medium), color = note.priority.color)
                    }
                }
                if (note.dueDate != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = if (note.isOverdue) JarvisColors.alertRed else JarvisColors.textTertiary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(dateFormat.format(Date(note.dueDate)), style = JarvisFont.mono(10, FontWeight.Normal), color = if (note.isOverdue) JarvisColors.alertRed else JarvisColors.textTertiary)
                    }
                }
                if (note.reminderDate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = JarvisColors.textTertiary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reminder set", style = JarvisFont.mono(10, FontWeight.Normal), color = JarvisColors.textTertiary)
                    }
                }
            }

            // Action buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { viewModel.toggleComplete(note) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (note.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (note.isCompleted) JarvisColors.neonGreen else JarvisColors.electricBlue,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { viewModel.togglePin(note) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (note.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                        null,
                        tint = if (note.isPinned) JarvisColors.electricBlue else JarvisColors.textTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                IconButton(onClick = { viewModel.startEditing(note) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, null, tint = JarvisColors.textTertiary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { viewModel.deleteNote(note) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = JarvisColors.alertRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AddEditNoteDialog(viewModel: NoteViewModel) {
    val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()
    val isEditing = editingNote != null

    var title by remember(editingNote) { mutableStateOf(editingNote?.title ?: "") }
    var body by remember(editingNote) { mutableStateOf(editingNote?.body ?: "") }
    var category by remember(editingNote) { mutableStateOf(editingNote?.category ?: NoteCategory.Personal) }
    var priority by remember(editingNote) { mutableStateOf(editingNote?.priority ?: NotePriority.Medium) }
    var hasDueDate by remember(editingNote) { mutableStateOf(editingNote?.dueDate != null) }
    var dueDate by remember(editingNote) { mutableStateOf(editingNote?.dueDate ?: System.currentTimeMillis()) }
    var hasReminder by remember(editingNote) { mutableStateOf(editingNote?.reminderDate != null) }
    var reminderDate by remember(editingNote) { mutableStateOf(editingNote?.reminderDate ?: System.currentTimeMillis()) }

    Dialog(onDismissRequest = { viewModel.hideAdd() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = JarvisColors.backgroundMid,
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisColors.cyan.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    if (isEditing) "EDIT NOTE" else "NEW NOTE",
                    style = JarvisFont.mono(18, FontWeight.Bold), color = JarvisColors.cyan, letterSpacing = 3.sp,
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title", color = JarvisColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisColors.cyan, unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = JarvisColors.textPrimary, unfocusedTextColor = JarvisColors.textPrimary, cursorColor = JarvisColors.cyan),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    label = { Text("Details", color = JarvisColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = JarvisColors.cyan, unfocusedBorderColor = JarvisColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = JarvisColors.textPrimary, unfocusedTextColor = JarvisColors.textPrimary, cursorColor = JarvisColors.cyan),
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                )
                Spacer(Modifier.height(12.dp))

                // Category
                Text("CATEGORY", style = JarvisFont.mono(12, FontWeight.Medium), color = JarvisColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteCategory.entries.forEach { cat ->
                        val selected = category == cat
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) cat.color.copy(alpha = 0.2f) else JarvisColors.cardBackground)
                                .border(1.dp, if (selected) cat.color else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(cat.displayName, style = JarvisFont.mono(11, FontWeight.Medium), color = if (selected) cat.color else JarvisColors.textTertiary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Priority
                Text("PRIORITY", style = JarvisFont.mono(12, FontWeight.Medium), color = JarvisColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotePriority.entries.forEach { pri ->
                        val selected = priority == pri
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) pri.color.copy(alpha = 0.2f) else JarvisColors.cardBackground)
                                .border(1.dp, if (selected) pri.color else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { priority = pri }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(pri.displayName, style = JarvisFont.mono(11, FontWeight.Medium), color = if (selected) pri.color else JarvisColors.textTertiary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Due Date toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = JarvisColors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DUE DATE", style = JarvisFont.mono(12, FontWeight.Medium), color = JarvisColors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = hasDueDate, onCheckedChange = { hasDueDate = it }, colors = SwitchDefaults.colors(checkedTrackColor = JarvisColors.cyan))
                }

                Spacer(Modifier.height(8.dp))

                // Reminder toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, null, tint = JarvisColors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("REMINDER", style = JarvisFont.mono(12, FontWeight.Medium), color = JarvisColors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = hasReminder, onCheckedChange = { hasReminder = it }, colors = SwitchDefaults.colors(checkedTrackColor = JarvisColors.cyan))
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { viewModel.hideAdd() }) { Text("Cancel", color = JarvisColors.textTertiary) }
                    Button(
                        onClick = {
                            viewModel.addOrUpdateNote(
                                title, body, category, priority,
                                if (hasDueDate) dueDate else null,
                                if (hasReminder) reminderDate else null,
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = JarvisColors.cyan),
                        enabled = title.isNotBlank(),
                    ) {
                        Text(
                            if (isEditing) "UPDATE NOTE" else "CREATE NOTE",
                            style = JarvisFont.mono(14, FontWeight.Bold), letterSpacing = 2.sp,
                        )
                    }
                }
            }
        }
    }
}
