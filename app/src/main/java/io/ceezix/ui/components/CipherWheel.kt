package io.ceezix.ui.components

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CipherWheel(
    currentShift: Int,
    onShiftChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    themeName: String = "Cyber Neon"
) {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val alphaCount = 26
    val anglePerChar = 360f / alphaCount

    // Target angle of rotation for the inner wheel
    val targetRotationAngle = -currentShift * anglePerChar
    val animatedRotationAngle by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "WheelRotation"
    )

    // Keep track of drag interaction
    var center by remember { mutableStateOf(Offset.Zero) }

    // Resolve color scheme dynamically based on selected theme
    val (outerRingColor, innerRingColor, labelColor, textColor, surfaceVariantColor) = when (themeName) {
        "Ubuntu" -> {
            // Auburn and orange vibes matching Ubuntu system
            Quintet(
                Color(0xFFDF4814), // Aubergine Orange
                Color(0xFFF99157), // Mid Salmon
                Color(0xFFE95420), // Standard Ubuntu Orange
                Color(0xFFFFFFFF), // White Text
                Color(0xFF1C0012)  // Warm aubergine core
            )
        }
        "VS Code" -> {
            // VS Code cool slate with syntax-highlighting green and teal
            Quintet(
                Color(0xFF007ACC), // Code blue
                Color(0xFF4EC9B0), // Code teal
                Color(0xFFC586C0), // Keyword purple
                Color(0xFFD4D4D4), // Gray text
                Color(0xFF1E1E1E)  // Dark slate core
            )
        }
        "GitHub" -> {
            // Clean developer white and Git green/blue accent
            Quintet(
                Color(0xFF0969DA), // Git blue
                Color(0xFF2DA44E), // Git green
                Color(0xFF8250DF), // Purple
                Color(0xFF24292F), // Dark charcoal text
                Color(0xFFF6F8FA)  // Pristine light gray
            )
        }
        "Minimalist" -> {
            // Bold minimalist architectural monochrome flat style
            Quintet(
                Color(0xFF000000), // Solid Black
                Color(0xFF555555), // Mid Gray
                Color(0xFF000000), // Solid Black
                Color(0xFF000000), // text black
                Color(0xFFFFFFFF)  // Stark active white
            )
        }
        "Antique" -> {
            // Golden aged brass & mahogany aesthetic values
            Quintet(
                Color(0xFFCBA374), // Warm Brass
                Color(0xFF82684B), // Mahogany Bronze
                Color(0xFFA67744), // Aged Gold Accent
                Color(0xFF000000), // Charcoal ink on parchment
                Color(0xFFFBF0D9)  // Aged pristine parchment paper
            )
        }
        else -> { // "Cyber Neon" default
            Quintet(
                Color(0xFF45F3FF), // Neon Cyan
                Color(0xFF66FCF1), // Neon Teal
                Color(0xFFBD5EFF), // Neon Purple
                Color(0xFFFFFFFF), // Custom White
                Color(0xFF0C0F1B)  // Core Dark Black Space
            )
        }
    }

    // Resolve customized Typeface based on theme selection
    val currentTypeface = when (themeName) {
        "Antique" -> android.graphics.Typeface.SERIF
        "GitHub", "Minimalist" -> android.graphics.Typeface.SANS_SERIF
        else -> android.graphics.Typeface.MONOSPACE
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Transparent)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                            val dx = event.x - center.x
                            val dy = event.y - center.y
                            var angleDegrees = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angleDegrees < 0) angleDegrees += 360f
                            
                            // Align rotation such that top is 0 shift
                            var relativeAngle = angleDegrees + 90f // Offset so top is index 0
                            if (relativeAngle >= 360f) relativeAngle -= 360f
                            
                            val rawShift = Math.round(relativeAngle / anglePerChar) % 26
                            val targetShift = (26 - rawShift) % 26
                            onShiftChanged(targetShift)
                            true
                        }
                        else -> false
                    }
                }
        ) {
            center = size.center
            val radius = size.minDimension / 2f
            
            val outerRadius = radius * 0.95f
            val midRadius = radius * 0.70f
            val innerRadius = radius * 0.45f

            // 1. Draw glowing background orbit
            if (themeName != "Minimalist") {
                val radialGlowColors = when (themeName) {
                    "GitHub" -> listOf(
                        innerRingColor.copy(alpha = 0.08f),
                        outerRingColor.copy(alpha = 0.03f),
                        Color.Transparent
                    )
                    else -> listOf(
                        innerRingColor.copy(alpha = 0.15f),
                        outerRingColor.copy(alpha = 0.02f),
                        Color.Transparent
                    )
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = radialGlowColors,
                        center = center,
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = center
                )
            }

            // 2. Draw outer boundary layout (dashed standard)
            drawCircle(
                color = outerRingColor.copy(alpha = if (themeName == "Minimalist") 1f else 0.3f),
                radius = outerRadius,
                center = center,
                style = Stroke(
                    width = (if (themeName == "Minimalist") 3.dp else 2.dp).toPx(),
                    pathEffect = if (themeName == "Minimalist") null else PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )

            // 3. Draw middle separator ring (dashed/solid)
            drawCircle(
                color = innerRingColor.copy(alpha = if (themeName == "Minimalist") 1f else 0.4f),
                radius = midRadius,
                center = center,
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = if (themeName == "Minimalist" || themeName == "Antique") null else PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                )
            )

            // 4. Draw inner core background disk
            drawCircle(
                color = surfaceVariantColor,
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = labelColor.copy(alpha = if (themeName == "Minimalist") 1f else 0.6f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = (if (themeName == "Minimalist") 2.dp else 3.dp).toPx())
            )

            // Paint configurations for drawing text beautifully
            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 14.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                color = textColor.toArgb()
                typeface = currentTypeface
            }

            val shiftPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 12.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                color = innerRingColor.toArgb()
                typeface = android.graphics.Typeface.create(currentTypeface, android.graphics.Typeface.BOLD)
            }

            val corePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = 24.dp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                color = labelColor.toArgb()
                typeface = android.graphics.Typeface.create(currentTypeface, android.graphics.Typeface.BOLD)
            }

            // Draw center active marker index
            drawContext.canvas.nativeCanvas.drawText(
                "KEY: $currentShift",
                center.x,
                center.y + 8.dp.toPx(),
                corePaint
            )

            // 5. Draw static Outer ring (Normal Plaintext alphabet A-Z)
            for (i in 0 until alphaCount) {
                val baseAngleDegrees = i * anglePerChar - 90f // Start directly at the top
                val angleRad = Math.toRadians(baseAngleDegrees.toDouble())
                
                val x = center.x + (outerRadius - 18.dp.toPx()) * kotlin.math.cos(angleRad).toFloat()
                val y = center.y + (outerRadius - 18.dp.toPx()) * kotlin.math.sin(angleRad).toFloat()

                // Rotate text to look beautiful around the circle
                rotate(degrees = baseAngleDegrees + 90f, pivot = Offset(x, y)) {
                    drawContext.canvas.nativeCanvas.drawText(
                        alphabet[i].toString(),
                        x,
                        y + 6.dp.toPx(),
                        textPaint
                    )
                }
            }

            // 6. Draw rotating Inner ring (Shifted cipher alphabet)
            rotate(degrees = animatedRotationAngle, pivot = center) {
                for (i in 0 until alphaCount) {
                    val baseAngleDegrees = i * anglePerChar - 90f
                    val angleRad = Math.toRadians(baseAngleDegrees.toDouble())
                    
                    val x = center.x + (midRadius - 18.dp.toPx()) * kotlin.math.cos(angleRad).toFloat()
                    val y = center.y + (midRadius - 18.dp.toPx()) * kotlin.math.sin(angleRad).toFloat()

                    rotate(degrees = baseAngleDegrees + 90f, pivot = Offset(x, y)) {
                        drawContext.canvas.nativeCanvas.drawText(
                            alphabet[i].toString(),
                            x,
                            y + 5.dp.toPx(),
                            shiftPaint
                        )
                    }
                }
            }
        }
    }
}

// Simple Helper quintet container for mapping colors compactly
private data class Quintet<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

