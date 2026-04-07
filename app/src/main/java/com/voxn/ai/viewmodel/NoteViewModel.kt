package com.voxn.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxn.ai.data.database.entity.NoteEntity
import com.voxn.ai.data.model.NoteCategory
import com.voxn.ai.data.model.NotePriority
import com.voxn.ai.manager.NoteManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    private val manager = NoteManager(app)

    val notes: StateFlow<List<NoteEntity>> = manager.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    private val _filterCategory = MutableStateFlow<NoteCategory?>(null)
    val filterCategory: StateFlow<NoteCategory?> = _filterCategory

    private val _filterPriority = MutableStateFlow<NotePriority?>(null)
    val filterPriority: StateFlow<NotePriority?> = _filterPriority

    private val _editingNote = MutableStateFlow<NoteEntity?>(null)
    val editingNote: StateFlow<NoteEntity?> = _editingNote

    private val _noteToDelete = MutableStateFlow<NoteEntity?>(null)
    val noteToDelete: StateFlow<NoteEntity?> = _noteToDelete

    fun showAdd() { _editingNote.value = null; _showAddDialog.value = true }
    fun hideAdd() { _showAddDialog.value = false; _editingNote.value = null }
    fun startEditing(note: NoteEntity) { _editingNote.value = note; _showAddDialog.value = true }

    fun setSearch(text: String) { _searchText.value = text }
    fun setFilterCategory(cat: NoteCategory?) { _filterCategory.value = cat }
    fun setFilterPriority(pri: NotePriority?) { _filterPriority.value = pri }

    fun filteredNotes(): List<NoteEntity> {
        var list = notes.value
        val search = searchText.value
        if (search.isNotBlank()) {
            list = list.filter { it.title.contains(search, true) || it.body.contains(search, true) }
        }
        filterCategory.value?.let { cat -> list = list.filter { it.category == cat } }
        filterPriority.value?.let { pri -> list = list.filter { it.priority == pri } }
        return list
    }

    fun addOrUpdateNote(
        title: String, body: String, category: NoteCategory, priority: NotePriority,
        dueDate: Long?, reminderDate: Long?,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val editing = editingNote.value
                if (editing != null) {
                    manager.updateNote(editing, title, body, category, priority, dueDate, reminderDate)
                } else {
                    manager.addNote(title, body, category, priority, dueDate, reminderDate)
                }
            } catch (_: Exception) {
                // Prevent crash on DB errors
            }
            _showAddDialog.value = false
            _editingNote.value = null
        }
    }

    fun togglePin(note: NoteEntity) { viewModelScope.launch { manager.togglePin(note) } }
    fun toggleComplete(note: NoteEntity) { viewModelScope.launch { manager.toggleComplete(note) } }
    fun requestDeleteNote(note: NoteEntity) { _noteToDelete.value = note }
    fun cancelDelete() { _noteToDelete.value = null }
    fun confirmDeleteNote() {
        val note = _noteToDelete.value ?: return
        viewModelScope.launch { manager.deleteNote(note) }
        _noteToDelete.value = null
    }

    val activeCount: Int get() = notes.value.count { !it.isCompleted }
    val overdueCount: Int get() = notes.value.count { it.isOverdue }
    val upcomingCount: Int get() = notes.value.count { it.hasUpcomingReminder }
}
