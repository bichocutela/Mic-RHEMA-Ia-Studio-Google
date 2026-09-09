package com.aistudio.micrhema

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.*

/** Shared by the real avatar and the admin preview. All geometry scales with the avatar. */
@Composable
fun LightEffectVisual(item: AdminLightEffect, modifier: Modifier = Modifier, showAvatarLabel: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "avatarLight")
    val phase by transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "lightPhase")
    val color = remember(item.colorHex) {
        runCatching { Color(AndroidColor.parseColor(item.colorHex)) }.getOrDefault(Color(0xFFFFD54F))
    }
    val strength = when (item.tone) { "suave" -> .55f; "forte" -> 1f; else -> .78f }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (showAvatarLabel) Box(Modifier.fillMaxSize(.62f).aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape), Alignment.Center) {
            Text("AVATAR", fontWeight = FontWeight.Bold)
        }
        Canvas(Modifier.fillMaxSize()) {
            // Read animation in the drawing phase; it does not recompose the profile every frame.
            drawAvatarLight(item.effectType.lowercase(), color, strength, phase)
        }
    }
}

private fun DrawScope.drawAvatarLight(type: String, color: Color, strength: Float, phase: Float) {
    val unit = size.minDimension
    if (unit <= 0f) return
    val r = unit * .365f
    val time = phase * (2 * PI).toFloat()
    fun point(angle: Float, radius: Float = r) = center + Offset(cos(angle), sin(angle)) * radius
    fun spark(p: Offset, radius: Float, tint: Color = color, alpha: Float = 1f, star: Boolean = false) {
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = alpha * .8f * strength), tint.copy(alpha = 0f)), p, radius * 5f), radius * 5f, p)
        drawCircle(Color.White.copy(alpha = alpha * strength), radius * .48f, p)
        if (star) {
            drawLine(tint.copy(alpha = alpha * strength), p - Offset(radius * 3, 0f), p + Offset(radius * 3, 0f), unit * .003f)
            drawLine(Color.White.copy(alpha = alpha * strength), p - Offset(0f, radius * 3), p + Offset(0f, radius * 3), unit * .002f)
        }
    }
    fun luminousPath(path: Path, tint: Color = color, width: Float = unit * .004f, alpha: Float = 1f) {
        // Broad translucent light, saturated middle and a hot core; no hardware blur dependency.
        for (layer in 5 downTo 1) drawPath(path, tint.copy(alpha = .035f * strength * alpha), style = Stroke(width * layer * 3f, cap = StrokeCap.Round))
        drawPath(path, tint.copy(alpha = .85f * strength * alpha), style = Stroke(width * 2, cap = StrokeCap.Round))
        drawPath(path, lerp(tint, Color.White, .78f).copy(alpha = strength * alpha), style = Stroke(width * .65f, cap = StrokeCap.Round))
    }
    val breath = .75f + .25f * sin(time * 2)
    // Soft annular bloom keeps the face transparent and all light inside the allocated bounds.
    repeat(12) { i ->
        drawCircle(color.copy(alpha = (.018f + .012f * breath) * strength), r,
            style = Stroke(unit * (.018f + i * .006f)))
    }
    val ring = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center - Offset(r, r), center + Offset(r, r))) }
    luminousPath(ring, width = unit * .0025f, alpha = .55f)
    when (type) {
        "flame", "fire_ring", "violet_flame" -> {
            val count = if (type == "fire_ring") 42 else 30
            repeat(count) { i ->
                val a = i * (2 * PI / count).toFloat() + time * .5f
                val flicker = .5f + .5f * sin(time * (if (type == "fire_ring") 6 else 4) + i * 2.4f)
                val length = unit * (.025f + .065f * flicker) * strength
                val start = point(a - .065f, r - unit * .009f)
                val tip = point(a + .11f, r + length)
                val end = point(a + .065f)
                val flame = Path().apply {
                    moveTo(start.x, start.y)
                    val c1 = point(a - .12f, r + length * .7f)
                    val c2 = point(a + .19f, r + length * .55f)
                    cubicTo(c1.x, c1.y, c2.x, c2.y, tip.x, tip.y)
                    val c3 = point(a + .02f, r + length * .4f)
                    quadraticBezierTo(c3.x, c3.y, end.x, end.y)
                }
                luminousPath(flame, width = unit * .003f)
                if (i % 3 == 0) spark(tip, unit * .0035f)
            }
        }
        "electric" -> repeat(3) { band ->
            val path = Path()
            repeat(97) { i ->
                val a = i * (2 * PI / 96).toFloat() + time
                val jitter = sin(i * 2.7f + time * 8 + band) * sin(i * .7f - time * 4) * unit * .02f
                val p = point(a, r + jitter + band * unit * .009f)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            luminousPath(path, width = unit * .002f)
        }
        "rays" -> repeat(24) { i ->
            val a = i * (2 * PI / 24).toFloat() + time * .5f
            val start = point(a)
            val end = point(a, r + unit * (.035f + .065f * (.5f + .5f * sin(time * 2 + i))))
            luminousPath(Path().apply { moveTo(start.x, start.y); lineTo(end.x, end.y) }, width = unit * .002f)
        }
        "pulse", "halo", "aura", "spirit_light" -> repeat(3) { i ->
            val progress = (phase * 2 + i / 3f) % 1f
            val radius = r + progress * unit * .09f
            drawCircle(color.copy(alpha = (1 - progress) * strength * .4f), radius,
                style = Stroke(unit * (if (type == "halo") .012f else .006f)))
        }
        "orbit", "halo_orbit", "rainbow_orbit" -> repeat(if (type == "halo_orbit") 2 else 3) { orbit ->
            val tint = if (type == "rainbow_orbit") Color.hsv((phase * 360 + orbit * 120) % 360, .8f, 1f) else color
            val path = Path()
            repeat(100) { i ->
                val a = time + orbit * (2 * PI / 3).toFloat() - i * .022f
                val p = point(a, r + sin(a * 2 + orbit) * unit * .022f)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            luminousPath(path, tint, unit * .003f)
            val a = time + orbit * (2 * PI / 3).toFloat()
            spark(point(a, r + sin(a * 2 + orbit) * unit * .022f), unit * .012f, tint, star = true)
        }
    }
    val count = when (type) { "stars" -> 28; "dust" -> 48; "particles", "aura" -> 36; else -> 18 }
    repeat(count) { i ->
        val progress = (phase * (if (type == "dust") 1 else 2) + i * .618034f) % 1f
        val a = i * 2.39996f + time * (if (type == "stars") .5f else 1f)
        val radius = r + unit * (.018f + .075f * progress)
        val p = point(a, radius)
        val alpha = sin(progress * PI).toFloat().coerceIn(0f, 1f)
        val tint = if (type == "rainbow_orbit") Color.hsv((i * 31f + phase * 360) % 360, .7f, 1f) else color
        spark(p, unit * (if (type == "stars" && i % 4 == 0) .009f else .0035f), tint, alpha, type == "stars" || i % 7 == 0)
    }
}
