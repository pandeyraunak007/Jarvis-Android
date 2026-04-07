package com.voxn.ai.ui.notes

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
import com.voxn.ai.data.database.entity.NoteEntity
import com.voxn.ai.data.model.NoteCategory
import com.voxn.ai.data.model.NotePriority
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.GlassCard
import com.voxn.ai.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(viewModel: NoteViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()
    val filterPriority by viewModel.filterPriority.collectAsStateWithLifecycle()

    val noteToDelete by viewModel.noteToDelete.collectAsStateWithLifecycle()
    val filteredNotes = viewModel.filteredNotes()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(VoxnColors.backgroundDark, VoxnColors.backgroundMid, VoxnColors.backgroundDark)))
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("MISSION NOTES", style = VoxnFont.mono(22, FontWeight.Bold), color = VoxnColors.cyan, letterSpacing = 3.sp)
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { viewModel.setSearch(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search notes...", color = VoxnColors.textTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = VoxnColors.textTertiary) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearch("") }) {
                                Icon(Icons.Default.Clear, null, tint = VoxnColors.textTertiary)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VoxnColors.cyan,
                        unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
                        focusedTextColor = VoxnColors.textPrimary,
                        unfocusedTextColor = VoxnColors.textPrimary,
                        cursorColor = VoxnColors.cyan,
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Category + priority filter chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filterCategory == null,
                            onClick = { viewModel.setFilterCategory(null) },
                            label = { Text("All", style = VoxnFont.mono(11, FontWeight.Medium)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VoxnColors.cyan.copy(alpha = 0.2f),
                                selectedLabelColor = VoxnColors.cyan,
                                labelColor = VoxnColors.textTertiary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = filterCategory == null,
                                borderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
                                selectedBorderColor = VoxnColors.cyan,
                            ),
                        )
                    }
                    items(NoteCategory.entries.toList()) { cat ->
                        FilterChip(
                            selected = filterCategory == cat,
                            onClick = { viewModel.setFilterCategory(if (filterCategory == cat) null else cat) },
                            label = { Text(cat.displayName, style = VoxnFont.mono(11, FontWeight.Medium)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color.copy(alpha = 0.2f),
                                selectedLabelColor = cat.color,
                                labelColor = VoxnColors.textTertiary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = filterCategory == cat,
                                borderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
                                selectedBorderColor = cat.color,
                            ),
                        )
                    }
                    items(NotePriority.entries.toList()) { pri ->
                        FilterChip(
                            selected = filterPriority == pri,
                            onClick = { viewModel.setFilterPriority(if (filterPriority == pri) null else pri) },
                            label = { Text(pri.displayName, style = VoxnFont.mono(11, FontWeight.Medium)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = pri.color.copy(alpha = 0.2f),
                                selectedLabelColor = pri.color,
                                labelColor = VoxnColors.textTertiary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true, selected = filterPriority == pri,
                                borderColor = VoxnColors.textTertiary.copy(alpha = 0.3f),
                                selectedBorderColor = pri.color,
                            ),
                        )
                    }
                }
            }

            // Stats bar
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatBadge("Active", viewModel.activeCount, VoxnColors.electricBlue)
                    StatBadge("Overdue", viewModel.overdueCount, VoxnColors.alertRed)
                    StatBadge("Upcoming", viewModel.upcomingCount, VoxnColors.cyan)
                }
            }

            // Notes list
            if (filteredNotes.isEmpty()) {
                item {
                    GlassCard {
                        Text("No notes found.", style = VoxnFont.cardBody, color = VoxnColors.textTertiary, modifier = Modifier.fillMaxWidth())
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
            containerColor = VoxnColors.cyan,
            shape = CircleShape,
        ) {
            Icon(Icons.Default.Add, "Add", tint = VoxnColors.backgroundDark, modifier = Modifier.size(22.dp))
        }
    }

    if (showAddDialog) { AddEditNoteDialog(viewModel) }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Note", style = VoxnFont.cardTitle, color = VoxnColors.textPrimary) },
            text = { Text("Delete \"${note.title}\"? This cannot be undone.", style = VoxnFont.cardBody, color = VoxnColors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteNote() },
                    colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.alertRed),
                ) { Text("Delete", style = VoxnFont.mono(13, FontWeight.Bold)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel", color = VoxnColors.textTertiary)
                }
            },
            containerColor = VoxnColors.backgroundMid,
        )
    }
}

@Composable
private fun StatBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", style = VoxnFont.mono(20, FontWeight.Bold), color = color)
        Text(label, style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary)
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
                        Icon(Icons.Default.PushPin, null, tint = VoxnColors.electricBlue, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        note.title,
                        style = VoxnFont.cardTitle,
                        color = if (note.isCompleted) VoxnColors.textTertiary else VoxnColors.textPrimary,
                        textDecoration = if (note.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (note.body.isNotBlank()) {
                    Text(note.body, style = VoxnFont.caption, color = VoxnColors.textTertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.background(note.category.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(note.category.displayName, style = VoxnFont.mono(9, FontWeight.Medium), color = note.category.color)
                    }
                    Box(
                        modifier = Modifier.background(note.priority.color.copy(alpha = 0.15f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(note.priority.displayName, style = VoxnFont.mono(9, FontWeight.Medium), color = note.priority.color)
                    }
                }
                if (note.dueDate != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, null, tint = if (note.isOverdue) VoxnColors.alertRed else VoxnColors.textTertiary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(dateFormat.format(Date(note.dueDate)), style = VoxnFont.mono(10, FontWeight.Normal), color = if (note.isOverdue) VoxnColors.alertRed else VoxnColors.textTertiary)
                    }
                }
                if (note.reminderDate != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reminder set", style = VoxnFont.mono(10, FontWeight.Normal), color = VoxnColors.textTertiary)
                    }
                }
            }

            // Action buttons
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { viewModel.toggleComplete(note) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (note.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        null,
                        tint = if (note.isCompleted) VoxnColors.neonGreen else VoxnColors.electricBlue,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = { viewModel.togglePin(note) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.PushPin,
                        null,
                        tint = if (note.isPinned) VoxnColors.electricBlue else VoxnColors.textTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = { viewModel.startEditing(note) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, null, tint = VoxnColors.textTertiary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.requestDeleteNote(note) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null, tint = VoxnColors.alertRed.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

    var showDueDatePicker by remember { mutableStateOf(false) }
    var showReminderDatePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    Dialog(onDismissRequest = { viewModel.hideAdd() }) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VoxnColors.backgroundMid,
            border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.cyan.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            ) {
                Text(
                    if (isEditing) "EDIT NOTE" else "NEW NOTE",
                    style = VoxnFont.mono(18, FontWeight.Bold), color = VoxnColors.cyan, letterSpacing = 3.sp,
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title", color = VoxnColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.cyan, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.cyan),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = body, onValueChange = { body = it },
                    label = { Text("Details", color = VoxnColors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VoxnColors.cyan, unfocusedBorderColor = VoxnColors.textTertiary.copy(alpha = 0.3f), focusedTextColor = VoxnColors.textPrimary, unfocusedTextColor = VoxnColors.textPrimary, cursorColor = VoxnColors.cyan),
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                )
                Spacer(Modifier.height(12.dp))

                // Category
                Text("CATEGORY", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteCategory.entries.forEach { cat ->
                        val selected = category == cat
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) cat.color.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                                .border(1.dp, if (selected) cat.color else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(cat.displayName, style = VoxnFont.mono(12, FontWeight.Medium), color = if (selected) cat.color else VoxnColors.textTertiary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Priority
                Text("PRIORITY", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotePriority.entries.forEach { pri ->
                        val selected = priority == pri
                        Box(
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                .background(if (selected) pri.color.copy(alpha = 0.2f) else VoxnColors.cardBackground)
                                .border(1.dp, if (selected) pri.color else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { priority = pri }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(pri.displayName, style = VoxnFont.mono(12, FontWeight.Medium), color = if (selected) pri.color else VoxnColors.textTertiary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Due Date toggle + picker
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarToday, null, tint = VoxnColors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("DUE DATE", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = hasDueDate, onCheckedChange = { hasDueDate = it }, colors = SwitchDefaults.colors(checkedTrackColor = VoxnColors.cyan))
                }
                if (hasDueDate) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(VoxnColors.cardBackground)
                            .border(1.dp, VoxnColors.cyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { showDueDatePicker = true }
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditCalendar, null, tint = VoxnColors.cyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(dateFormat.format(Date(dueDate)), style = VoxnFont.mono(14, FontWeight.Medium), color = VoxnColors.textPrimary)
                            Spacer(Modifier.weight(1f))
                            Text("Tap to change", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Reminder toggle + picker
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Notifications, null, tint = VoxnColors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("REMINDER", style = VoxnFont.mono(12, FontWeight.Medium), color = VoxnColors.textSecondary)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = hasReminder, onCheckedChange = { hasReminder = it }, colors = SwitchDefaults.colors(checkedTrackColor = VoxnColors.cyan))
                }
                if (hasReminder) {
                    Spacer(Modifier.height(8.dp))
                    // Date row
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(VoxnColors.cardBackground)
                            .border(1.dp, VoxnColors.cyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { showReminderDatePicker = true }
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditCalendar, null, tint = VoxnColors.cyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(dateFormat.format(Date(reminderDate)), style = VoxnFont.mono(13, FontWeight.Medium), color = VoxnColors.textPrimary)
                            Spacer(Modifier.weight(1f))
                            Text("Date", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Time row
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(VoxnColors.cardBackground)
                            .border(1.dp, VoxnColors.cyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .clickable { showReminderTimePicker = true }
                            .padding(12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, null, tint = VoxnColors.cyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(timeFormat.format(Date(reminderDate)), style = VoxnFont.mono(13, FontWeight.Medium), color = VoxnColors.textPrimary)
                            Spacer(Modifier.weight(1f))
                            Text("Time", style = VoxnFont.caption, color = VoxnColors.textTertiary)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Action buttons — full width
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.hideAdd() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxnColors.textTertiary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.textTertiary.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel", style = VoxnFont.mono(13, FontWeight.Medium), maxLines = 1)
                    }
                    Button(
                        onClick = {
                            viewModel.addOrUpdateNote(
                                title, body, category, priority,
                                if (hasDueDate) dueDate else null,
                                if (hasReminder) reminderDate else null,
                            )
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.cyan),
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            if (isEditing) "UPDATE" else "CREATE",
                            style = VoxnFont.mono(13, FontWeight.Bold), maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    // Due date picker dialog
    if (showDueDatePicker) {
        val dueDatePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        dueDatePickerState.selectedDateMillis?.let { dueDate = it }
                        showDueDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.cyan),
                ) {
                    Text("OK", style = VoxnFont.mono(14, FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) {
                    Text("Cancel", color = VoxnColors.textTertiary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = VoxnColors.backgroundMid),
        ) {
            DatePicker(
                state = dueDatePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    titleContentColor = VoxnColors.textPrimary,
                    headlineContentColor = VoxnColors.cyan,
                    weekdayContentColor = VoxnColors.textTertiary,
                    dayContentColor = VoxnColors.textSecondary,
                    selectedDayContainerColor = VoxnColors.cyan,
                    selectedDayContentColor = VoxnColors.backgroundDark,
                    todayContentColor = VoxnColors.cyan,
                    todayDateBorderColor = VoxnColors.cyan,
                ),
                showModeToggle = false,
            )
        }
    }

    // Reminder date picker dialog
    if (showReminderDatePicker) {
        val reminderDatePickerState = rememberDatePickerState(initialSelectedDateMillis = reminderDate)
        DatePickerDialog(
            onDismissRequest = { showReminderDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        reminderDatePickerState.selectedDateMillis?.let { selectedMillis ->
                            // Preserve the time portion from existing reminderDate
                            val existingCal = Calendar.getInstance().apply { timeInMillis = reminderDate }
                            val newCal = Calendar.getInstance().apply {
                                timeInMillis = selectedMillis
                                set(Calendar.HOUR_OF_DAY, existingCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, existingCal.get(Calendar.MINUTE))
                            }
                            reminderDate = newCal.timeInMillis
                        }
                        showReminderDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.cyan),
                ) {
                    Text("OK", style = VoxnFont.mono(14, FontWeight.Bold))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReminderDatePicker = false }) {
                    Text("Cancel", color = VoxnColors.textTertiary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = VoxnColors.backgroundMid),
        ) {
            DatePicker(
                state = reminderDatePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    titleContentColor = VoxnColors.textPrimary,
                    headlineContentColor = VoxnColors.cyan,
                    weekdayContentColor = VoxnColors.textTertiary,
                    dayContentColor = VoxnColors.textSecondary,
                    selectedDayContainerColor = VoxnColors.cyan,
                    selectedDayContentColor = VoxnColors.backgroundDark,
                    todayContentColor = VoxnColors.cyan,
                    todayDateBorderColor = VoxnColors.cyan,
                ),
                showModeToggle = false,
            )
        }
    }

    // Reminder time picker dialog
    if (showReminderTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = reminderDate }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false,
        )
        Dialog(onDismissRequest = { showReminderTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VoxnColors.backgroundMid,
                border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.cyan.copy(alpha = 0.3f)),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("SET REMINDER TIME", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.cyan, letterSpacing = 2.sp)
                    Spacer(Modifier.height(16.dp))
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = VoxnColors.cardBackground,
                            clockDialSelectedContentColor = VoxnColors.backgroundDark,
                            clockDialUnselectedContentColor = VoxnColors.textSecondary,
                            selectorColor = VoxnColors.cyan,
                            containerColor = Color.Transparent,
                            periodSelectorSelectedContainerColor = VoxnColors.cyan.copy(alpha = 0.2f),
                            periodSelectorUnselectedContainerColor = VoxnColors.cardBackground,
                            periodSelectorSelectedContentColor = VoxnColors.cyan,
                            periodSelectorUnselectedContentColor = VoxnColors.textTertiary,
                            timeSelectorSelectedContainerColor = VoxnColors.cyan.copy(alpha = 0.2f),
                            timeSelectorUnselectedContainerColor = VoxnColors.cardBackground,
                            timeSelectorSelectedContentColor = VoxnColors.cyan,
                            timeSelectorUnselectedContentColor = VoxnColors.textSecondary,
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showReminderTimePicker = false },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VoxnColors.textTertiary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, VoxnColors.textTertiary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Cancel", style = VoxnFont.mono(13, FontWeight.Medium), maxLines = 1)
                        }
                        Button(
                            onClick = {
                                val updatedCal = Calendar.getInstance().apply {
                                    timeInMillis = reminderDate
                                    set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                    set(Calendar.MINUTE, timePickerState.minute)
                                    set(Calendar.SECOND, 0)
                                }
                                reminderDate = updatedCal.timeInMillis
                                showReminderTimePicker = false
                            },
                            modifier = Modifier.weight(1f).height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VoxnColors.cyan),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("SET", style = VoxnFont.mono(13, FontWeight.Bold), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
