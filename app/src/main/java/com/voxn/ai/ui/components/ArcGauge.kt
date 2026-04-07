package com.voxn.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont

@Composable
fun ArcGauge(
    value: Double,
    label: String,
    displayValue: String,
    color: Color,
    size: Dp = 75.dp,
    lineWidth: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = EaseOutCubic),
        label = "arc"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = lineWidth.toPx()
            val padding = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(padding, padding)
            val startAngle = 135f
            val totalSweep = 270f

            // Background arc
            drawArc(
                color = color.copy(alpha = 0.15f),
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Foreground arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color.copy(alpha = 0.8f), color, color.copy(alpha = 0.8f))
                ),
                startAngle = startAngle,
                sweepAngle = totalSweep * animatedValue,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = displayValue,
                style = VoxnFont.mono(if (size > 100.dp) 18 else 12, FontWeight.Bold),
                color = color,
            )
            Text(
                text = label,
                style = VoxnFont.mono(if (size > 100.dp) 10 else 8, FontWeight.Medium),
                color = VoxnColors.textTertiary,
            )
        }
    }
}
