package com.voxn.ai.manager

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.voxn.ai.data.database.VoxnDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class DataExportManager(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val fileDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    suspend fun exportExpensesCsv(): File = withContext(Dispatchers.IO) {
        val db = VoxnDatabase.getInstance(context)
        val expenses = db.expenseDao().getAll().firstOrNull().orEmpty()

        val sb = StringBuilder()
        sb.appendLine("Date,Amount,Merchant,Category,Payment Method,Note")
        expenses.forEach { e ->
            sb.appendLine("${dateFormat.format(Date(e.date))},${e.amount},\"${e.merchant}\",${e.categoryRaw},${e.paymentMethodRaw},\"${e.note ?: ""}\"")
        }

        val file = File(context.cacheDir, "voxn_expenses_${fileDate.format(Date())}.csv")
        runCatching { file.writeText(sb.toString()) }.getOrThrow()
        file
    }

    suspend fun exportHabitsCsv(): File = withContext(Dispatchers.IO) {
        val db = VoxnDatabase.getInstance(context)
        val habits = db.habitDao().getAllWithCompletions().firstOrNull().orEmpty()

        val sb = StringBuilder()
        sb.appendLine("Habit Name,Frequency,Target Count,Created Date,Current Streak,Total Completions")
        habits.forEach { hwc ->
            sb.appendLine("\"${hwc.habit.name}\",${hwc.habit.frequencyRaw},${hwc.habit.targetCount},${dateFormat.format(Date(hwc.habit.createdAt))},${hwc.currentStreak()},${hwc.completions.size}")
        }

        val file = File(context.cacheDir, "voxn_habits_${fileDate.format(Date())}.csv")
        runCatching { file.writeText(sb.toString()) }.getOrThrow()
        file
    }

    suspend fun exportNotesCsv(): File = withContext(Dispatchers.IO) {
        val db = VoxnDatabase.getInstance(context)
        val notes = db.noteDao().getAll().firstOrNull().orEmpty()

        val sb = StringBuilder()
        sb.appendLine("Title,Body,Category,Priority,Due Date,Is Pinned,Is Completed,Created Date")
        notes.forEach { n ->
            sb.appendLine("\"${n.title}\",\"${n.body}\",${n.categoryRaw},${n.priorityRaw},${n.dueDate?.let { dateFormat.format(Date(it)) } ?: ""},${n.isPinned},${n.isCompleted},${dateFormat.format(Date(n.createdAt))}")
        }

        val file = File(context.cacheDir, "voxn_notes_${fileDate.format(Date())}.csv")
        runCatching { file.writeText(sb.toString()) }.getOrThrow()
        file
    }

    suspend fun exportAllAndShare(): Intent = withContext(Dispatchers.IO) {
        val expenseFile = exportExpensesCsv()
        val habitFile = exportHabitsCsv()
        val noteFile = exportNotesCsv()

        val authority = "${context.packageName}.fileprovider"
        val uris = listOf(expenseFile, habitFile, noteFile).map {
            FileProvider.getUriForFile(context, authority, it)
        }

        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            putExtra(Intent.EXTRA_SUBJECT, "Voxn AI Data Export — ${fileDate.format(Date())}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
