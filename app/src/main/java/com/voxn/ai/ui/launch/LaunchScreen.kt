package com.voxn.ai.ui.launch

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voxn.ai.theme.VoxnColors
import com.voxn.ai.theme.VoxnFont
import kotlinx.coroutines.delay

@Composable
fun LaunchScreen(onComplete: () -> Unit) {
    // Phase tracking: 0=dark, 1=rings ignite, 2=boot messages, 3=ready flash, 4=done
    var phase by remember { mutableIntStateOf(0) }

    // Boot messages
    val bootMessages = listOf(
        "Initializing core systems...",
        "Loading health modules...",
        "Syncing financial data...",
        "Activating habit protocols...",
        "Calibrating neural interface...",
        "All systems operational.",
    )
    var visibleMessages by remember { mutableIntStateOf(0) }

    // Phase timing
    LaunchedEffect(Unit) {
        delay(300)   // Phase 0: dark
        phase = 1    // Rings ignite
        delay(1200)
        phase = 2    // Boot messages
        for (i in bootMessages.indices) {
            delay(180)
            visibleMessages = i + 1
        }
        delay(400)
        phase = 3    // Ready flash
        delay(800)
        phase = 4    // Fade out
        delay(500)
        onComplete()
    }

    // Animations
    val ringScale by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0.3f; 1 -> 1f; else -> 1.1f
        },
        animationSpec = tween(800, easing = FastOutSlowInEasing), label = "ringScale",
    )

    val ringAlpha by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0f; 1, 2 -> 1f; 3 -> 1f; else -> 0f
        },
        animationSpec = tween(if (phase == 4) 400 else 600), label = "ringAlpha",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = when (phase) {
            3 -> 1f; else -> 0.3f
        },
        animationSpec = tween(300), label = "glowAlpha",
    )

    val textAlpha by animateFloatAsState(
        targetValue = when (phase) {
            in 1..3 -> 1f; else -> 0f
        },
        animationSpec = tween(400), label = "textAlpha",
    )

    val screenAlpha by animateFloatAsState(
        targetValue = if (phase == 4) 0f else 1f,
        animationSpec = tween(400), label = "screenAlpha",
    )

    // Continuous rotation for rings
    val infiniteTransition = rememberInfiniteTransition(label = "rings")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "outerRot",
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "innerRot",
    )

    // Pulsing glow
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center,
    ) {
        // Glow background
        Canvas(modifier = Modifier.size(300.dp).alpha(glowAlpha).scale(pulseScale)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00D4FF).copy(alpha = 0.15f),
                        Color(0xFF00D4FF).copy(alpha = 0.05f),
                        Color.Transparent,
                    ),
                ),
                radius = size.minDimension / 2,
            )
        }

        // Outer ring
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .scale(ringScale)
                .alpha(ringAlpha)
                .rotate(outerRotation),
        ) {
            // Outer arc segments
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            for (i in 0..5) {
                drawArc(
                    color = Color(0xFF00D4FF).copy(alpha = 0.6f),
                    startAngle = i * 60f + 10f,
                    sweepAngle = 40f,
                    useCenter = false,
                    style = stroke,
                )
            }
        }

        // Middle ring
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .scale(ringScale)
                .alpha(ringAlpha)
                .rotate(innerRotation),
        ) {
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            for (i in 0..7) {
                drawArc(
                    color = Color(0xFF00F0FF).copy(alpha = 0.5f),
                    startAngle = i * 45f + 5f,
                    sweepAngle = 30f,
                    useCenter = false,
                    style = stroke,
                )
            }
        }

        // Inner ring
        Canvas(
            modifier = Modifier
                .size(80.dp)
                .scale(ringScale)
                .alpha(ringAlpha)
                .rotate(outerRotation * 1.5f),
        ) {
            val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            for (i in 0..3) {
                drawArc(
                    color = Color(0xFF39FF14).copy(alpha = 0.4f),
                    startAngle = i * 90f + 15f,
                    sweepAngle = 60f,
                    useCenter = false,
                    style = stroke,
                )
            }
        }

        // Center core dot
        Canvas(
            modifier = Modifier
                .size(20.dp)
                .scale(ringScale)
                .alpha(ringAlpha),
        ) {
            drawCircle(
                color = Color(0xFF00D4FF),
                radius = size.minDimension / 2 * 0.6f,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = size.minDimension / 2 * 0.2f,
            )
        }

        // Title
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 140.dp)
                .alpha(textAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "V.O.X.N.",
                style = VoxnFont.mono(24, FontWeight.Bold),
                color = VoxnColors.electricBlue,
                letterSpacing = 6.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AI",
                style = VoxnFont.mono(14, FontWeight.Medium),
                color = VoxnColors.cyan,
                letterSpacing = 4.sp,
            )
        }

        // Boot messages
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .alpha(textAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            bootMessages.take(visibleMessages).forEach { msg ->
                val isLast = msg == bootMessages.last() && visibleMessages == bootMessages.size
                Text(
                    msg,
                    style = VoxnFont.mono(10, FontWeight.Normal),
                    color = if (isLast) VoxnColors.neonGreen else VoxnColors.textTertiary,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(2.dp))
            }
        }

        // Ready flash overlay
        if (phase == 3) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(glowAlpha * 0.1f)
                    .background(Color(0xFF00D4FF)),
            )
        }
    }
}
