package com.voxn.ai.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun GlowText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    glowRadius: Float = 16f,
) {
    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.drawBehind {
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                this.color = color.copy(alpha = 0.6f).toArgb()
                maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
                textSize = style.fontSize.toPx()
            }
            drawContext.canvas.nativeCanvas.drawText(
                text,
                0f,
                size.height * 0.75f,
                paint,
            )
        },
    )
}
