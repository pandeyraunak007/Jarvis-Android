package com.jarvis.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Colors
object JarvisColors {
    val electricBlue = Color(0xFF00D4FF)
    val cyan = Color(0xFF00F0FF)
    val neonGreen = Color(0xFF39FF14)
    val warningOrange = Color(0xFFFF6B35)
    val alertRed = Color(0xFFFF3B30)
    val backgroundDark = Color(0xFF0A0A0F)
    val backgroundMid = Color(0xFF0D1117)
    val cardBackground = Color(0x0DFFFFFF) // 5% white
    val cardBackgroundHover = Color(0x14FFFFFF) // 8% white
    val textPrimary = Color.White
    val textSecondary = Color(0xB3FFFFFF) // 70%
    val textTertiary = Color(0x80FFFFFF) // 50%
    val pink = Color(0xFFFF3B80)
    val yellow = Color(0xFFFFD60A)
    val purple = Color(0xFFBF5AF2)
    val gray = Color(0xFF8E8E93)
}

// Typography
object JarvisFont {
    val mono = FontFamily.Monospace
    val display = FontFamily.Default

    val heroNumber = TextStyle(fontFamily = mono, fontWeight = FontWeight.Bold, fontSize = 42.sp)
    val dataReadout = TextStyle(fontFamily = mono, fontWeight = FontWeight.SemiBold, fontSize = 24.sp)
    val dataLabel = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val sectionTitle = TextStyle(fontFamily = display, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val cardTitle = TextStyle(fontFamily = display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val cardBody = TextStyle(fontFamily = display, fontWeight = FontWeight.Normal, fontSize = 14.sp)
    val caption = TextStyle(fontFamily = display, fontWeight = FontWeight.Normal, fontSize = 12.sp)

    fun mono(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = mono, fontWeight = weight, fontSize = size.sp
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = JarvisColors.electricBlue,
    secondary = JarvisColors.cyan,
    tertiary = JarvisColors.neonGreen,
    background = JarvisColors.backgroundDark,
    surface = JarvisColors.backgroundMid,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = JarvisColors.alertRed,
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
