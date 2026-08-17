package com.aistudio.micrhema

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private data class ConfettiPiece(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val drift: Float,
    val fallSpeed: Float,
    val rotation: Float,
    val color: Color
)

@Composable
fun ConfettiBurst(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    val colors = remember {
        listOf(
            Color(0xFFFFD54F),
            Color(0xFFFFB300),
            Color(0xFF66BB6A),
            Color(0xFF42A5F5),
            Color(0xFFEF5350),
            Color(0xFFAB47BC)
        )
    }
    val pieces = remember {
        List(42) { index ->
            ConfettiPiece(
                startX = ((index * 37) % 100) / 100f,
                startY = -0.12f - ((index * 17) % 35) / 100f,
                size = 4f + (index % 4) * 2f,
                drift = -24f + ((index * 29) % 49),
                fallSpeed = 0.82f + (index % 5) * 0.07f,
                rotation = (index * 31) % 180 - 90f,
                color = colors[index % colors.size]
            )
        }
    }

    LaunchedEffect(Unit) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1900, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier) {
        val animationProgress = progress.value
        pieces.forEach { piece ->
            val x = size.width * piece.startX + sin(animationProgress * 5f + piece.startX * 12f) * piece.drift
            val y = size.height * (piece.startY + animationProgress * piece.fallSpeed * 1.35f)
            if (y in -size.height * 0.2f..size.height * 1.15f) {
                val alpha = when {
                    animationProgress < 0.12f -> animationProgress / 0.12f
                    animationProgress > 0.82f -> (1f - animationProgress) / 0.18f
                    else -> 1f
                }.coerceIn(0f, 1f)
                rotate(piece.rotation + animationProgress * 540f, pivot = Offset(x, y)) {
                    drawRect(
                        color = piece.color.copy(alpha = alpha),
                        topLeft = Offset(x - piece.size, y - piece.size / 2f),
                        size = androidx.compose.ui.geometry.Size(piece.size * 2f, piece.size)
                    )
                }
            }
        }
    }
}
