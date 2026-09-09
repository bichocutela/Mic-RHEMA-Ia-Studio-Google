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
        infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "lightPhase")
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
        val opacity = (alpha * strength).coerceIn(0f, 1f)
        drawCircle(Brush.radialGradient(
            0f to Color.White.copy(alpha = opacity),
            .12f to lerp(tint, Color.White, .75f).copy(alpha = opacity),
            .32f to tint.copy(alpha = opacity * .9f),
            .65f to tint.copy(alpha = opacity * .25f),
            1f to tint.copy(alpha = 0f),
            center = p, radius = radius * 5f), radius * 5f, p)
        drawCircle(Color.White.copy(alpha = opacity), radius * .65f, p)
        if (star) {
            val flare = Path().apply {
                moveTo(p.x - radius * 4f, p.y)
                lineTo(p.x - radius * .35f, p.y - radius * .35f)
                lineTo(p.x, p.y - radius * 4f)
                lineTo(p.x + radius * .35f, p.y - radius * .35f)
                lineTo(p.x + radius * 4f, p.y)
                lineTo(p.x + radius * .35f, p.y + radius * .35f)
                lineTo(p.x, p.y + radius * 4f)
                lineTo(p.x - radius * .35f, p.y + radius * .35f)
                close()
            }
            drawPath(flare, Brush.radialGradient(listOf(Color.White.copy(alpha = opacity),
                tint.copy(alpha = 0f)), p, radius * 4f))
        }
    }
    fun luminousPath(path: Path, tint: Color = color, width: Float = unit * .004f, alpha: Float = 1f) {
        // Saturated outer glow stays visible on pale surfaces; white core supplies the flare.
        for (layer in 4 downTo 1) drawPath(path,
            tint.copy(alpha = (.075f + (4 - layer) * .025f) * strength * alpha),
            style = Stroke(width * (2f + layer * 2.4f), cap = StrokeCap.Round))
        drawPath(path, tint.copy(alpha = strength * alpha), style = Stroke(width * 2.8f, cap = StrokeCap.Round))
        drawPath(path, lerp(tint, Color.White, .9f).copy(alpha = strength * alpha), style = Stroke(width, cap = StrokeCap.Round))
    }
    val breath = .75f + .25f * sin(time * 2)
    // A continuous, soft annular light field; the middle remains fully transparent.
    drawCircle(Brush.radialGradient(
        0f to color.copy(alpha = 0f),
        .65f to color.copy(alpha = 0f),
        .73f to color.copy(alpha = .10f * strength),
        .80f to color.copy(alpha = .44f * strength * breath),
        .84f to lerp(color, Color.White, .55f).copy(alpha = .48f * strength),
        .89f to color.copy(alpha = .30f * strength),
        1f to color.copy(alpha = 0f),
        center = center, radius = unit * .45f), unit * .45f)
    val ring = Path().apply { addOval(androidx.compose.ui.geometry.Rect(center - Offset(r, r), center + Offset(r, r))) }
    luminousPath(ring, width = unit * .0035f, alpha = .85f)

    fun ribbon(turns: Int, offset: Float, amplitude: Float, tint: Color = color, width: Float = .003f) {
        val path = Path()
        repeat(181) { i ->
            val a = i * (2 * PI / 180).toFloat()
            val radius = r + unit * (offset + amplitude * sin(a * turns + time * 2))
            val p = point(a, radius)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        luminousPath(path, tint, unit * width)
    }
    fun comet(angle: Float, offset: Float, tint: Color = color, length: Float = 1.8f) {
        // A fading tail follows a bright moving lens flare all the way around the portrait.
        repeat(20) { i ->
            val a = angle - i * length / 20
            val b = angle - (i + 1) * length / 20
            val p = point(a, r + (offset + .012f * sin(a * 2)) * unit)
            val q = point(b, r + (offset + .012f * sin(b * 2)) * unit)
            luminousPath(Path().apply { moveTo(p.x, p.y); lineTo(q.x, q.y) }, tint,
                unit * .0028f, (1 - i / 20f) * .9f)
        }
        spark(point(angle, r + (offset + .012f * sin(angle * 2)) * unit), unit * .014f, tint, star = true)
    }
    when (type) {
        "stars" -> {
            ribbon(2, .004f, .006f, lerp(color, Color.White, .35f), .0015f)
            repeat(8) { i ->
                val a = time + i * (2 * PI / 8).toFloat()
                spark(point(a, r + unit * (.015f + .012f * sin(time * 2 + i))),
                    unit * (.006f + .004f * (.5f + .5f * sin(time * 4 + i))), star = true)
            }
            comet(-time, -.008f, length = .8f)
        }
        "particles" -> {
            // Vida Abundante: braided organic energy and glowing leaf-shaped motes.
            ribbon(3, .014f, .014f)
            ribbon(3, .014f, -.014f, lerp(color, Color.White, .4f), .0015f)
            repeat(10) { i ->
                val a = time + i * (2 * PI / 10).toFloat()
                val p = point(a, r + unit * .04f)
                val leaf = Path().apply {
                    moveTo(p.x, p.y)
                    val tip = point(a + .06f, r + unit * .063f)
                    quadraticBezierTo(p.x + unit * .022f, p.y, tip.x, tip.y)
                    quadraticBezierTo(p.x - unit * .012f, p.y, p.x, p.y)
                }
                drawPath(leaf, color.copy(alpha = .32f * strength))
                luminousPath(leaf, width = unit * .0015f)
            }
        }
        "dust" -> {
            comet(time, 0f, length = 2.8f)
            repeat(32) { i ->
                val progress = (phase * 2 + i * .618034f) % 1f
                val a = i * 2.39996f
                val p = point(a, r + unit * .02f) - Offset(0f, unit * .06f * progress)
                // Fade at both ends so particles are born and disappear without jumping.
                spark(p, unit * .0025f, alpha = sin(progress * PI).toFloat().coerceIn(0f, 1f))
            }
        }
        "flame", "fire_ring", "violet_flame" -> {
                val count = when (type) { "fire_ring" -> 42; "violet_flame" -> 24; else -> 30 }
            if (type == "violet_flame") ribbon(4, .025f, .013f, lerp(color, Color.White, .3f))
            if (type == "fire_ring") comet(time * 3, .025f, Color(0xFFFFD166), .9f)
            repeat(count) { i ->
                val a = i * (2 * PI / count).toFloat() + time * (if (type == "violet_flame") -1f else 1f)
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
                val flameFill = Path().apply { addPath(flame); close() }
                drawPath(flameFill, Brush.radialGradient(
                    listOf(lerp(color, Color.White, .55f).copy(alpha = .5f * strength), color.copy(alpha = 0f)),
                    start, length.coerceAtLeast(unit * .02f)))
                luminousPath(flame, width = unit * .003f)
                if (i % 3 == 0) spark(tip, unit * .0035f)
            }
        }
        "electric" -> repeat(3) { band ->
            val path = Path()
            repeat(97) { i ->
                val a = i * (2 * PI / 96).toFloat() + time
                val jitter = sin(a * 29 + time * 8 + band) * sin(a * 11 - time * 4) * unit * .02f
                val p = point(a, r + jitter + band * unit * .009f)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            luminousPath(path, width = unit * .002f)
        }
        "rays" -> repeat(24) { i ->
            val a = i * (2 * PI / 24).toFloat() + time
            val start = point(a)
            val end = point(a, r + unit * (.035f + .065f * (.5f + .5f * sin(time * 2 + i))))
            luminousPath(Path().apply { moveTo(start.x, start.y); lineTo(end.x, end.y) }, width = unit * .002f)
        }
        "pulse" -> {
            ribbon(2, .01f, .018f * breath)
            ribbon(2, .035f, -.012f * breath, lerp(color, Color.White, .3f))
            comet(time * 2, .012f)
            repeat(3) { i ->
                val progress = (phase * 2 + i / 3f) % 1f
                drawCircle(color.copy(alpha = sin(progress * PI).toFloat().coerceIn(0f, 1f) * .25f * strength),
                    r + progress * unit * .085f, style = Stroke(unit * .005f))
            }
        }
        "halo" -> {
            ribbon(1, -.006f, .003f, lerp(color, Color.White, .65f), .004f)
            ribbon(1, .017f, .003f, width = .002f)
            spark(point(-time), unit * .014f, lerp(color, Color.White, .7f), star = true)
            spark(point(-time + PI.toFloat()), unit * .009f, star = true)
        }
        "aura" -> {
            repeat(3) { i -> ribbon(4 + i, i * .014f, .011f * breath, width = .002f) }
            comet(-time * 2, .025f, lerp(color, Color.White, .4f), 1.1f)
        }
        "spirit_light" -> {
            ribbon(2, .008f, .006f * breath, width = .0015f)
            repeat(5) { i ->
                val a = time + i * (2 * PI / 5).toFloat()
                spark(point(a, r + unit * .017f), unit * .009f,
                    alpha = .55f + .45f * sin(time * 2 + i).let { it * it }, star = true)
            }
            comet(-time, -.007f, length = .7f)
        }
        "orbit" -> {
            ribbon(2, .008f, .018f)
            ribbon(2, .008f, -.018f, lerp(color, Color.White, .3f), .0015f)
            repeat(3) { comet(time * 2 + it * (2 * PI / 3).toFloat(), it * .009f, length = 2.2f) }
        }
        "halo_orbit" -> {
            ribbon(1, .008f, .003f, width = .0015f)
            comet(time, .008f, length = 3.1f)
            comet(-time + PI.toFloat(), -.01f, lerp(color, Color.White, .5f), 1.1f)
        }
        "rainbow_orbit" -> {
            repeat(48) { i ->
                val a = time + i * (2 * PI / 48).toFloat()
                val p = point(a)
                val q = point(a + (2 * PI / 48).toFloat())
                luminousPath(Path().apply { moveTo(p.x, p.y); lineTo(q.x, q.y) },
                    Color.hsv(i * 360f / 48, .85f, 1f), unit * .0035f)
            }
            repeat(3) { comet(-time + it * (2 * PI / 3).toFloat(), .022f,
                Color.hsv((phase * 360 + it * 120) % 360, .75f, 1f), .9f) }
        }
        else -> comet(time, 0f)

    }
    when (type) {
        "flame" -> comet(time * 2, -.012f, Color(0xFFFFE5A0), .8f)
        "fire_ring" -> comet(-time * 3, -.008f, Color(0xFFFFF1C9), .6f)
        "violet_flame" -> comet(-time * 2, -.006f, lerp(color, Color.White, .5f), 1.4f)
        "electric" -> repeat(3) { i ->
            val a = -time * 3 + i * (2 * PI / 3).toFloat()
            spark(point(a, r + unit * .016f), unit * .013f, star = true)
        }
        "rays" -> spark(point(time), unit * .018f, star = true)
        "particles" -> comet(-time, -.004f, lerp(color, Color.White, .4f), 1.2f)
        "dust" -> spark(point(time + PI.toFloat()), unit * .012f, star = true)
        "aura" -> comet(time, -.012f, length = 2.1f)
        "spirit_light" -> ribbon(3, -.008f, .006f, width = .003f)
    }
    val count = when (type) { "stars" -> 28; "dust" -> 48; "particles", "aura" -> 36; else -> 18 }
    repeat(count) { i ->
        val progress = (phase * (if (type == "dust") 1 else 2) + i * .618034f) % 1f
        val a = i * 2.39996f + time * (if (type == "stars") 1f else 2f)
        val radius = r + unit * (.018f + .070f * progress)
        val p = point(a, radius)
        val alpha = sin(progress * PI).toFloat().coerceIn(0f, 1f)
        val tint = if (type == "rainbow_orbit") Color.hsv((i * 31f + phase * 360) % 360, .7f, 1f) else color
        spark(p, unit * (if (type == "stars" && i % 4 == 0) .009f else .0035f), tint, alpha, type == "stars" || i % 7 == 0)
    }
}
