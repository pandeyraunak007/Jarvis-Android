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

class SpendWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = readWidgetData(context)

        provideContent {
            SpendWidgetContent(data)
        }
    }

    @Composable
    private fun SpendWidgetContent(data: WidgetData) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F))
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Text(
                "SPEND TRACKER",
                style = TextStyle(color = ColorProvider(day = Color(0xFFFF6B35), night = Color(0xFFFF6B35)), fontSize = 11.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(8.dp))

            Text(
                data.todayFormatted,
                style = TextStyle(color = ColorProvider(day = Color.White, night = Color.White), fontSize = 28.sp, fontWeight = FontWeight.Bold),
            )
            Text("Today", style = TextStyle(color = ColorProvider(day = Color.White.copy(alpha = 0.5f), night = Color.White.copy(alpha = 0.5f)), fontSize = 11.sp))

            Spacer(GlanceModifier.height(12.dp))

            if (data.monthlyBudget > 0) {
                // Budget bar
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp)
                        .background(Color.White.copy(alpha = 0.1f))
                        .cornerRadius(2.dp),
                ) {
                    Box(
                        modifier = GlanceModifier
                            .height(4.dp)
                            .fillMaxWidth()
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
        }
    }
}

class SpendWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SpendWidget()
}
