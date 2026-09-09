package com.aistudio.micrhema

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LightEffectVisual(
    item: AdminLightEffect,
    modifier: Modifier = Modifier,
    showAvatarLabel: Boolean = true
) {
    val infinite = rememberInfiniteTransition(label = "realLightEffect")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "phase"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (item.tone == "forte") 560 else 1100), RepeatMode.Reverse),
        label = "pulse"
    )
    val shimmer by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "shimmer"
    )
    val baseColor = remember(item.colorHex) {
        runCatching { Color(AndroidColor.parseColor(item.colorHex)) }.getOrDefault(Color(0xFFFFD54F))
    }
    val intensity = when (item.tone) {
        "suave" -> 0.55f
        "forte" -> 1f
        else -> 0.78f
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val avatarR = size.minDimension * 0.31f
            val ringR = avatarR * (1.22f + (pulse - 0.82f) * 0.22f)
            val glow = baseColor.copy(alpha = 0.08f + 0.12f * intensity)

            repeat(4) { i ->
                drawCircle(
                    color = glow.copy(alpha = glow.alpha / (i + 1)),
                    radius = ringR + i * 8f,
                    center = c,
                    style = Stroke(width = 12f + i * 5f)
                )
            }

            when (item.effectType.lowercase()) {
                "flame", "fire_ring", "violet_flame" -> {
                    drawCircle(baseColor.copy(alpha = 0.6f * intensity), ringR, c, style = Stroke(width = 7f))
                    val count = if (item.tone == "forte") 28 else 20
                    repeat(count) { i ->
                        val a = Math.toRadians((i * 360f / count + phase * 0.28f).toDouble())
                        val wave = sin(Math.toRadians((phase * 2 + i * 39).toDouble())).toFloat()
                        val startR = ringR - 1f
                        val flameLen = (18f + 26f * (0.5f + 0.5f * wave)) * intensity
                        val start = Offset(c.x + cos(a).toFloat() * startR, c.y + sin(a).toFloat() * startR)
                        val end = Offset(c.x + cos(a).toFloat() * (startR + flameLen), c.y + sin(a).toFloat() * (startR + flameLen))
                        drawLine(baseColor.copy(alpha = 0.38f + 0.5f * intensity), start, end, strokeWidth = 5f)
                        drawCircle(baseColor.copy(alpha = 0.25f + 0.45f * shimmer), 4f + 3f * intensity, end)
                    }
                }
                "stars" -> {
                    repeat(18) { i ->
                        val a = Math.toRadians((i * 47 + phase * 0.35f).toDouble())
                        val r = ringR + 10f + (i % 4) * 8f
                        val p = Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r)
                        val alpha = (0.2f + 0.8f * ((shimmer + (i % 3) * 0.2f) % 1f)) * intensity
                        drawCircle(baseColor.copy(alpha = alpha.coerceIn(0.12f, 1f)), 2.5f + (i % 3) * 1.7f, p)
                        if (i % 4 == 0) {
                            drawLine(baseColor.copy(alpha = alpha), Offset(p.x - 7f, p.y), Offset(p.x + 7f, p.y), 1.8f)
                            drawLine(baseColor.copy(alpha = alpha), Offset(p.x, p.y - 7f), Offset(p.x, p.y + 7f), 1.8f)
                        }
                    }
                }
                "rays" -> {
                    repeat(18) { i ->
                        val a = Math.toRadians((i * 20 + phase * 0.08f).toDouble())
                        val inner = ringR + 3f
                        val outer = ringR + 26f + (i % 3) * 10f * pulse
                        val p1 = Offset(c.x + cos(a).toFloat() * inner, c.y + sin(a).toFloat() * inner)
                        val p2 = Offset(c.x + cos(a).toFloat() * outer, c.y + sin(a).toFloat() * outer)
                        drawLine(baseColor.copy(alpha = (0.22f + 0.55f * shimmer) * intensity), p1, p2, 3f + 3f * intensity)
                    }
                    drawCircle(baseColor.copy(alpha = 0.72f * intensity), ringR, c, style = Stroke(width = 5f))
                }
                "electric" -> {
                    val segments = 34
                    var prev: Offset? = null
                    repeat(segments + 1) { i ->
                        val angle = Math.toRadians((i * 360f / segments + phase).toDouble())
                        val jitter = sin(Math.toRadians((phase * 5 + i * 71).toDouble())).toFloat() * 8f
                        val r = ringR + jitter
                        val p = Offset(c.x + cos(angle).toFloat() * r, c.y + sin(angle).toFloat() * r)
                        prev?.let { drawLine(baseColor.copy(alpha = 0.92f * intensity), it, p, 3.2f) }
                        prev = p
                    }
                    repeat(7) { i ->
                        val a = Math.toRadians((i * 53 + phase * 1.7f).toDouble())
                        val p = Offset(c.x + cos(a).toFloat() * (ringR + 11f), c.y + sin(a).toFloat() * (ringR + 11f))
                        drawCircle(Color.White.copy(alpha = 0.7f * intensity), 3.5f, p)
                    }
                }
                "particles", "dust" -> {
                    repeat(24) { i ->
                        val a = Math.toRadians((i * 137.5 + phase * (0.15f + (i % 3) * 0.05f)).toDouble())
                        val r = ringR + 6f + (i % 6) * 7f
                        val drift = sin(Math.toRadians((phase * 1.6f + i * 29).toDouble())).toFloat() * 9f
                        val p = Offset(c.x + cos(a).toFloat() * r, c.y + sin(a).toFloat() * r - drift)
                        drawCircle(baseColor.copy(alpha = (0.22f + 0.65f * shimmer) * intensity), 2f + (i % 4) * 1.4f, p)
                    }
                }
                "pulse", "aura", "halo", "spirit_light" -> {
                    repeat(3) { i ->
                        val rr = ringR + i * 11f + (pulse - 0.82f) * 35f
                        drawCircle(baseColor.copy(alpha = (0.34f / (i + 1)) * intensity), rr, c, style = Stroke(width = 8f - i * 1.5f))
                    }
                    repeat(8) { i ->
                        val a = Math.toRadians((i * 45 + phase * 0.35f).toDouble())
                        val p = Offset(c.x + cos(a).toFloat() * (ringR + 15f), c.y + sin(a).toFloat() * (ringR + 15f))
                        drawCircle(Color.White.copy(alpha = 0.55f * shimmer * intensity), 3f, p)
                    }
                }
                "rainbow_orbit" -> {
                    val colors = listOf(Color(0xFFFF5252), Color(0xFFFFC107), Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFAB47BC))
                    colors.forEachIndexed { index, color ->
                        drawArc(
                            color = color.copy(alpha = 0.9f * intensity),
                            startAngle = phase + index * 72f,
                            sweepAngle = 54f,
                            useCenter = false,
                            topLeft = Offset(c.x - ringR, c.y - ringR),
                            size = androidx.compose.ui.geometry.Size(ringR * 2, ringR * 2),
                            style = Stroke(width = 8f)
                        )
                    }
                }
                else -> {
                    drawCircle(baseColor.copy(alpha = 0.62f * intensity), ringR, c, style = Stroke(width = 6f))
                    val a = Math.toRadians(phase.toDouble())
                    repeat(6) { i ->
                        val aa = a + i * (2 * PI / 6)
                        val p = Offset(c.x + cos(aa).toFloat() * ringR, c.y + sin(aa).toFloat() * ringR)
                        drawCircle(baseColor.copy(alpha = 0.85f * intensity), 4f + i % 2, p)
                    }
                }
            }

            if (item.effectType.lowercase() in setOf("orbit", "halo_orbit")) {
                drawCircle(baseColor.copy(alpha = 0.68f * intensity), ringR, c, style = Stroke(width = 5f))
                repeat(7) { i ->
                    val a = Math.toRadians((phase + i * 51.4f).toDouble())
                    val p = Offset(c.x + cos(a).toFloat() * ringR, c.y + sin(a).toFloat() * ringR)
                    drawCircle(if (i == 0) Color.White else baseColor, if (i == 0) 7f else 3.5f, p, alpha = intensity)
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(138.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            if (showAvatarLabel) Text("AVATAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
