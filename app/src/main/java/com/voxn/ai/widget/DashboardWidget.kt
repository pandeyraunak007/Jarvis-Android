package com.voxn.ai.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.color.ColorProvider
import androidx.glance.text.*
import com.voxn.ai.MainActivity

class DashboardWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readWidgetData(context)

        provideContent {
            DashboardWidgetContent(data)
        }
    }

    @Composable
    private fun DashboardWidgetContent(data: WidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            // Greeting
            Text(
                data.greeting,
                style = TextStyle(color = ColorProvider(day = Color(0xFF00D4FF), night = Color(0xFF00D4FF)), fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(12.dp))

            // Spend + Habits row
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                // Spend
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("TODAY", style = TextStyle(color = ColorProvider(day = Color.White.copy(alpha = 0.5f), night = Color.White.copy(alpha = 0.5f)), fontSize = 9.sp))
                    Text(
                        data.todayFormatted,
                        style = TextStyle(color = ColorProvider(day = Color(0xFFFF6B35), night = Color(0xFFFF6B35)), fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                }
                // Habits
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text("HABITS", style = TextStyle(color = ColorProvider(day = Color.White.copy(alpha = 0.5f), night = Color.White.copy(alpha = 0.5f)), fontSize = 9.sp))
                    Text(
                        "${data.habitsCompleted}/${data.habitsDueToday}",
                        style = TextStyle(color = ColorProvider(day = Color(0xFF39FF14), night = Color(0xFF39FF14)), fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }

            Spacer(GlanceModifier.height(12.dp))

            // Budget progress
            if (data.monthlyBudget > 0) {
                Text("BUDGET", style = TextStyle(color = ColorProvider(day = Color.White.copy(alpha = 0.5f), night = Color.White.copy(alpha = 0.5f)), fontSize = 9.sp))
                Spacer(GlanceModifier.height(4.dp))
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                        .cornerRadius(2.dp),
                ) {
                    Box(
                        modifier = GlanceModifier.height(4.dp).fillMaxWidth()
                            .background(if (data.budgetExceeded) Color(0xFFFF3B30) else Color(0xFF39FF14))
                            .cornerRadius(2.dp),
                    ) {}
                }
                Spacer(GlanceModifier.height(4.dp))
                Text(
                    "${data.monthlyFormatted} / ${data.budgetFormatted}",
                    style = TextStyle(color = ColorProvider(day = Color.White.copy(alpha = 0.5f), night = Color.White.copy(alpha = 0.5f)), fontSize = 10.sp),
                )
            }

            // Streak
            if (data.longestStreak > 0) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    "🔥 ${data.longestStreak} day streak",
                    style = TextStyle(color = ColorProvider(day = Color(0xFFFF6B35), night = Color(0xFFFF6B35)), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

class DashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DashboardWidget()
}
