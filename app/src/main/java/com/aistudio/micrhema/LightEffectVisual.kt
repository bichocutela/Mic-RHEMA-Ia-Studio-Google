package com.aistudio.micrhema

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

/** One clock drives BOTH sides of the emblem, including in the admin preview. */
@Composable
fun rememberLightEffectPhase(): State<Float> = rememberInfiniteTransition(label = "lightDepth")
    .animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "orbit")

/** The back pass belongs below the emblem; the front pass above it. Neither can paint the photo. */
@Composable
fun AvatarLightLayer(
    items: List<AdminLightEffect>, phase: State<Float>, front: Boolean,
    portraitFraction: Float, modifier: Modifier = Modifier
) {
    val colors = remember(items) { items.associate { it.id to runCatching {
        Color(AndroidColor.parseColor(it.colorHex))
    }.getOrDefault(Color(0xFFFFD54F)) } }
    Canvas(modifier.fillMaxSize()) {
        val photoRadius = size.minDimension * portraitFraction / 2f + size.minDimension * .006f
        val photoMask = Path().apply { addOval(Rect(center - Offset(photoRadius, photoRadius), center + Offset(photoRadius, photoRadius))) }
        clipPath(photoMask, ClipOp.Difference) {
            items.forEach { item ->
                val power = when (item.tone) { "suave" -> .6f; "forte" -> 1f; else -> .82f }
                DepthLightPainter(this, phase.value, front, colors.getValue(item.id), power).render(item.effectType.lowercase())
            }
        }
    }
}

/** Project a tilted orbit from 3D to the avatar plane. z determines actual layer ordering. */
internal data class LightOrbitPoint(val x: Float, val y: Float, val z: Float)
internal fun projectLightOrbit(angle: Float, tilt: Float, rotation: Float, radius: Float): LightOrbitPoint {
    val x = cos(angle) * radius
    val y = sin(angle) * radius * cos(tilt)
    return LightOrbitPoint(x * cos(rotation) - y * sin(rotation),
        x * sin(rotation) + y * cos(rotation), sin(angle) * sin(tilt))
}

private class DepthLightPainter(
    val scope: DrawScope, val time: Float, val front: Boolean, val color: Color, val power: Float
) {
    private val u = scope.size.minDimension
    private val tau = (2 * PI).toFloat()
    private fun screen(p: LightOrbitPoint) = scope.center + Offset(p.x, p.y) * u
    private fun visible(z: Float) = (z >= 0f) == front
    private fun polar(a: Float, r: Float) = scope.center + Offset(cos(a), sin(a)) * (r * u)
    private fun glow(path: Path, tint: Color, width: Float, alpha: Float = 1f) = with(scope) {
        val a = (alpha * power).coerceIn(0f, 1f)
        drawPath(path, tint.copy(alpha = a * .09f), style = Stroke(u * width * 9, cap = StrokeCap.Round))
        drawPath(path, tint.copy(alpha = a * .20f), style = Stroke(u * width * 5, cap = StrokeCap.Round))
        drawPath(path, tint.copy(alpha = a * .85f), style = Stroke(u * width * 2, cap = StrokeCap.Round))
        drawPath(path, lerp(tint, Color.White, .85f).copy(alpha = a), style = Stroke(u * width * .65f, cap = StrokeCap.Round))
    }
    private fun flare(p: Offset, tint: Color, radius: Float, alpha: Float = 1f, star: Boolean = true) = with(scope) {
        val a = (alpha * power).coerceIn(0f, 1f)
        val r = radius * u
        drawCircle(Brush.radialGradient(0f to Color.White.copy(alpha = a),
            .16f to lerp(tint, Color.White, .65f).copy(alpha = a),
            .40f to tint.copy(alpha = a * .6f), 1f to tint.copy(alpha = 0f),
            center = p, radius = r * 4), r * 4, p)
        if (star) {
            val path = Path().apply {
                moveTo(p.x - r * 3, p.y); lineTo(p.x - r * .22f, p.y - r * .22f)
                lineTo(p.x, p.y - r * 3); lineTo(p.x + r * .22f, p.y - r * .22f)
                lineTo(p.x + r * 3, p.y); lineTo(p.x + r * .22f, p.y + r * .22f)
                lineTo(p.x, p.y + r * 3); lineTo(p.x - r * .22f, p.y + r * .22f); close()
            }
            drawPath(path, Color.White.copy(alpha = a))
        }
    }
    // Paths are batched into four brightness bands rather than drawing a glow per segment.
    private fun orbit(tilt: Float, rotation: Float, speed: Int, radius: Float = .39f,
        tint: Color = color, wave: Float = 0f, width: Float = .003f, heads: Int = 1) {
        val paths = List(4) { Path() }
        repeat(96) { i ->
            val a = i * tau / 96
            val b = (i + 1) * tau / 96
            fun at(v: Float) = projectLightOrbit(v, tilt, rotation,
                radius + wave * sin(v * 3 + time * 2))
            val mid = at((a + b) / 2)
            if (visible(mid.z)) {
                val light = .5f + .5f * cos(a * heads - time * speed)
                val path = paths[(light * 3.99f).toInt().coerceIn(0, 3)]
                val p = screen(at(a)); val q = screen(at(b))
                path.moveTo(p.x, p.y); path.lineTo(q.x, q.y)
            }
        }
        paths.forEachIndexed { i, path -> glow(path, tint, width, .25f + i * .25f) }
        repeat(heads) { i ->
            val a = time * speed / heads + i * tau / heads
            val p = projectLightOrbit(a, tilt, rotation, radius + wave * sin(a * 3 + time * 2))
            if (visible(p.z)) flare(screen(p), tint, .009f + .004f * ((p.z + 1) / 2))
        }
    }
    private fun motes(count: Int, tint: Color = color, stars: Boolean = false, rise: Boolean = false) {
        repeat(count) { i ->
            val life = (time / tau * 2 + i * .618034f) % 1f
            val a = i * 2.39996f + if (rise) 0f else time
            val z = sin(a)
            if (visible(z)) {
                val p = polar(a, .36f + life * .065f) - Offset(0f, if (rise) life * u * .035f else 0f)
                flare(p, tint, if (stars && i % 3 == 0) .007f else .0028f,
                    sin(life * PI).toFloat().coerceIn(0f, 1f), stars)
            }
        }
    }
    private fun flames(count: Int, speed: Int, curl: Float, tint: Color, height: Float) {
        repeat(count) { i ->
            val a = i * tau / count + time * speed
            if (visible(sin(a))) {
                val h = height * (.5f + .5f * sin(time * 4 + i * 2.4f)) + .018f
                val base = polar(a, .35f)
                val tip = polar(a + curl, .35f + h)
                val left = polar(a - .035f, .35f)
                val right = polar(a + .035f, .35f)
                val c1 = polar(a - curl, .35f + h * .55f)
                val c2 = polar(a + curl * 1.6f, .35f + h * .7f)
                val path = Path().apply {
                    moveTo(left.x, left.y); cubicTo(c1.x, c1.y, c2.x, c2.y, tip.x, tip.y)
                    quadraticBezierTo(base.x, base.y, right.x, right.y); close()
                }
                with(scope) { drawPath(path, Brush.linearGradient(listOf(tint.copy(alpha = .6f * power),
                    tint.copy(alpha = 0f)), base, tip)) }
                glow(path, tint, .0023f)
            }
        }
    }
    private fun petals(count: Int, tint: Color, ray: Boolean = false) {
        repeat(count) { i ->
            val a = i * tau / count + time
            if (visible(sin(a))) {
                val root = polar(a, .36f)
                val tip = polar(a + if (ray) 0f else .05f, .41f + .02f * sin(time * 2 + i))
                val path = Path().apply {
                    moveTo(root.x, root.y)
                    if (ray) lineTo(tip.x, tip.y) else {
                        val c = polar(a - .06f, .40f)
                        quadraticBezierTo(c.x, c.y, tip.x, tip.y)
                        val d = polar(a + .09f, .38f)
                        quadraticBezierTo(d.x, d.y, root.x, root.y); close()
                    }
                }
                glow(path, tint, if (ray) .002f else .0018f)
            }
        }
    }
    fun render(type: String) {
        when (type) {
            // Aura da Promessa: two inclined golden ribbons and traveling solar flares.
            "orbit" -> { orbit(.50f, -.48f, 2); orbit(.35f, .55f, -2, .415f, width = .0018f); motes(28) }
            // Céu Estrelado: a tilted constellation with independently twinkling stars.
            "stars" -> { orbit(.62f, .35f, 2, width = .0015f, heads = 2); motes(36, stars = true) }
            // Chama do Espírito: ascending amber tongues carried by a sweeping fire ribbon.
            "flame" -> { flames(28, 1, .13f, color, .07f); orbit(.48f, -.55f, 2, tint = lerp(color, Color.White, .4f)); motes(18, rise = true) }
            // Glória Divina: three swelling violet veils around a brighter equatorial ribbon.
            "pulse" -> { repeat(3) { i -> orbit(.30f + i * .13f, i * 1.1f, 2, .37f + i * .015f, wave = .012f, width = .002f) }; motes(15) }
            // Vida Abundante: leaves and green vines moving in opposite directions.
            "particles" -> { petals(12, color); orbit(.52f, -.4f, 2, wave = .012f); orbit(.52f, .4f, -2, .405f, width = .0015f); motes(20) }
            // Luz Celestial: broad ivory halo with a single white sun orbiting it.
            "halo" -> { orbit(.28f, -.3f, 1, tint = lerp(color, Color.White, .6f), width = .006f); orbit(.5f, .4f, -1, .413f, width = .0015f) }
            // Espírito de Fogo: fast double fire vortex with a hot inner ribbon.
            "fire_ring" -> { flames(38, 2, .19f, color, .085f); orbit(.6f, -.6f, -4, tint = Color(0xFFFFD278), heads = 2); motes(26, rise = true) }
            // Raios de Glória: radial golden shafts plus two crossing sunbeams.
            "rays" -> { petals(24, color, ray = true); orbit(.6f, -.65f, 2); orbit(.6f, .65f, -2, width = .002f) }
            // Poeira Dourada: a rising cloud of tiny gold motes with one long fine orbit.
            "dust" -> { motes(56, rise = true); orbit(.38f, -.55f, 1, width = .0016f) }
            // Halo Divino: two fine counter-rotating ivory hoops on distinct planes.
            "halo_orbit" -> { orbit(.65f, -.5f, 1, .40f, width = .0018f); orbit(.45f, .5f, -1, .415f, width = .0018f); motes(12, stars = true) }
            // Energia Azul: jagged arcs live on a tilted orbit, not on a flat outer circle.
            "electric" -> {
                repeat(2) { band ->
                    val path = Path()
                    repeat(96) { i ->
                        fun at(j: Int): LightOrbitPoint {
                            val a = j * tau / 96
                            val jitter = .014f * sin(a * 31 + time * 8) * cos(a * 13 - time * 4)
                            return projectLightOrbit(a, .52f, -.45f + band * .9f, .39f + jitter)
                        }
                        val p = at(i); val q = at(i + 1)
                        if (visible((p.z + q.z) / 2)) { val x = screen(p); val y = screen(q); path.moveTo(x.x, x.y); path.lineTo(y.x, y.y) }
                    }
                    glow(path, color, .0028f)
                }
                orbit(.52f, -.45f, 6, width = .001f, heads = 3); motes(18)
            }
            // Aura Esmeralda: three fluid green sheets forming a braided torus.
            "aura" -> { repeat(3) { orbit(.5f, it * tau / 3, if (it % 2 == 0) 2 else -2, .39f, wave = .015f, width = .0025f) }; motes(24) }
            // Chamas Roxas: longer violet wisps curling backwards around a lilac orbit.
            "violet_flame" -> { flames(20, -1, -.20f, color, .085f); orbit(.65f, .5f, -2, tint = lerp(color, Color.White, .35f)); motes(22, stars = true) }
            // Arco-Íris da Aliança: seven individual colored arcs occupying seven planes.
            "rainbow_orbit" -> { repeat(7) { i -> orbit(.3f + i * .055f, i * .3f, 2, .375f + i * .005f,
                Color.hsv(i * 360f / 7, .82f, 1f), width = .0012f) } }
            // Luz do Espírito: airy white wisps and soft drifting sparks.
            "spirit_light" -> { orbit(.65f, -.6f, 1, wave = .009f, width = .002f); orbit(.4f, .6f, -1, .40f, width = .0012f); motes(25, stars = true, rise = true) }
            else -> { orbit(.5f, -.4f, 2); motes(16) }
        }
    }
}
