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

class HabitWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readWidgetData(context)

        provideContent {
            HabitWidgetContent(data)
        }
    }

    @Composable
    private fun HabitWidgetContent(data: WidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                "HABIT TRACKER",
                style = TextStyle(color = ColorProvider(day = Color(0xFF39FF14), night = Color(0xFF39FF14)), fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(8.dp))

            Text(
                "${data.habitsCompleted}/${data.habitsDueToday}",
                style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), fontSize = 28.sp, fontWeight = FontWeight.Bold),
            )
            Text("Completed today", style = TextStyle(color = ColorProvider(day = Color.White.copy(alpha = 0.5f), night = Color.White.copy(alpha = 0.5f)), fontSize = 11.sp))

            Spacer(GlanceModifier.height(12.dp))

            if (data.longestStreak > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🔥 ",
                        style = TextStyle(fontSize = 14.sp),
                    )
                    Text(
                        "${data.longestStreak} day streak",
                        style = TextStyle(color = ColorProvider(day = Color(0xFFFF6B35), night = Color(0xFFFF6B35)), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = HabitWidget()
}
