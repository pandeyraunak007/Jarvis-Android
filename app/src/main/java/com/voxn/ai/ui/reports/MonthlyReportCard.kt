package com.voxn.ai.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.database.entity.HabitWithCompletions
import com.voxn.ai.data.database.entity.NoteEntity
import com.voxn.ai.data.model.HealthData
import com.voxn.ai.manager.ExpenseParser
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import com.voxn.ai.ui.components.GlassCard
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthlyReportCard(
    expenses: List<ExpenseEntity>,
    habits: List<HabitWithCompletions>,
    notes: List<NoteEntity>,
    healthData: HealthData,
) {
    val fmt = DecimalFormat("#,##0")
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthStart = ExpenseParser.monthStart()
    val monthExpenses = expenses.filter { it.date >= monthStart }
    val totalSpend = monthExpenses.sumOf { it.amount }
    val transactionCount = monthExpenses.size
    val topCategory = monthExpenses.groupBy { it.category }
        .maxByOrNull { it.value.sumOf { e -> e.amount } }?.key
    val daysInMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val avgDaily = totalSpend / daysInMonth

    val habitsCompleted = habits.count { it.isCompletedToday() }
    val longestStreak = habits.maxOfOrNull { it.currentStreak() } ?: 0
    val completedNotes = notes.count { it.isCompleted }
    val totalNotes = notes.size

    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Assessment, null, tint = VoxnColors.purple, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("MONTHLY REPORT", style = VoxnFont.mono(14, FontWeight.Bold), color = VoxnColors.purple, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(monthFormat.format(Date()).uppercase(), style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary, letterSpacing = 2.sp)

        Spacer(Modifier.height(16.dp))

        // Spending summary
        ReportRow(Icons.Default.CurrencyRupee, "Total Spent", "₹${fmt.format(totalSpend.toLong())}", VoxnColors.warningOrange)
        ReportRow(Icons.Default.Receipt, "Transactions", "$transactionCount", VoxnColors.warningOrange)
        ReportRow(Icons.Default.TrendingUp, "Avg/Day", "₹${fmt.format(avgDaily.toLong())}", VoxnColors.electricBlue)
        topCategory?.let {
            ReportRow(Icons.Default.Category, "Top Category", it.displayName, it.color)
        }

        Spacer(Modifier.height(12.dp))
        Divider(color = VoxnColors.textTertiary.copy(alpha = 0.1f))
        Spacer(Modifier.height(12.dp))

        // Habits summary
        ReportRow(Icons.Default.CheckCircle, "Habits Today", "$habitsCompleted/${habits.size}", VoxnColors.neonGreen)
        ReportRow(Icons.Default.LocalFireDepartment, "Best Streak", "$longestStreak days", VoxnColors.warningOrange)

        Spacer(Modifier.height(12.dp))
        Divider(color = VoxnColors.textTertiary.copy(alpha = 0.1f))
        Spacer(Modifier.height(12.dp))

        // Health summary
        ReportRow(Icons.Default.FitnessCenter, "Steps Today", healthData.stepsFormatted, VoxnColors.electricBlue)
        ReportRow(Icons.Default.Bedtime, "Sleep", healthData.sleepFormatted, VoxnColors.cyan)

        Spacer(Modifier.height(12.dp))
        Divider(color = VoxnColors.textTertiary.copy(alpha = 0.1f))
        Spacer(Modifier.height(12.dp))

        // Tasks summary
        ReportRow(Icons.Default.Task, "Tasks Completed", "$completedNotes/$totalNotes", VoxnColors.cyan)
        val overdue = notes.count { it.isOverdue }
        if (overdue > 0) {
            ReportRow(Icons.Default.Warning, "Overdue", "$overdue", VoxnColors.alertRed)
        }
    }
}

@Composable
private fun ReportRow(icon: ImageVector, label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = VoxnFont.cardBody, color = VoxnColors.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = VoxnFont.mono(13, FontWeight.Bold), color = color)
    }
}
