package com.voxn.ai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont

data class DonutSlice(
    val label: String,
    val value: Double,
    val color: Color,
)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerText: String,
    centerSubText: String = "",
    size: Dp = 160.dp,
    strokeWidth: Dp = 20.dp,
) {
    val total = slices.sumOf { it.value }
    if (total == 0.0) return

    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }
    val animProgress by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(1000),
        label = "donut",
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            var startAngle = -90f

            // Background ring
            drawArc(
                color = VoxnColors.cardBackground,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )

            // Slices
            slices.forEach { slice ->
                val sweep = (slice.value / total * 360f * animProgress).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke,
                )
                startAngle += sweep
            }
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, style = VoxnFont.mono(18, FontWeight.Bold), color = VoxnColors.textPrimary)
            if (centerSubText.isNotEmpty()) {
                Text(centerSubText, style = VoxnFont.mono(10, FontWeight.Medium), color = VoxnColors.textTertiary)
            }
        }
    }
}
