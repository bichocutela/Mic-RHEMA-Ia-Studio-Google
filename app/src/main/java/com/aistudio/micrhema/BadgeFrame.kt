package com.aistudio.micrhema

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BiblicalAvatarWithBadge(
    avatar: BiblicalAvatar,
    badge: BiblicalBadge,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = avatar.displayName
) {
    val clickableModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    val isProfileEmblem = badge.frameStyle == BadgeFrameStyle.PROFILE_EMBLEM

    Box(modifier = clickableModifier, contentAlignment = Alignment.Center) {
        if (isProfileEmblem) {
            BiblicalAvatarImage(
                avatar = avatar,
                modifier = Modifier.fillMaxSize(0.58f).clip(CircleShape),
                contentDescription = contentDescription
            )
            coil.compose.AsyncImage(
                model = profileEmblemDrawable(badge.level ?: 8),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) { drawClassicBadgeFrame(badge) }
            BiblicalAvatarImage(
                avatar = avatar,
                modifier = Modifier.fillMaxSize(0.72f).clip(CircleShape),
                contentDescription = contentDescription
            )
        }
    }
}

private fun profileEmblemDrawable(level: Int): Int = when (level.coerceIn(8, 22)) {
    8 -> R.drawable.profile_emblem_level_08
    9 -> R.drawable.profile_emblem_level_09
    10 -> R.drawable.profile_emblem_level_10
    11 -> R.drawable.profile_emblem_level_11
    12 -> R.drawable.profile_emblem_level_12
    13 -> R.drawable.profile_emblem_level_13
    14 -> R.drawable.profile_emblem_level_14
    15 -> R.drawable.profile_emblem_level_15
    16 -> R.drawable.profile_emblem_level_16
    17 -> R.drawable.profile_emblem_level_17
    18 -> R.drawable.profile_emblem_level_18
    19 -> R.drawable.profile_emblem_level_19
    20 -> R.drawable.profile_emblem_level_20
    21 -> R.drawable.profile_emblem_level_21
    22 -> R.drawable.profile_emblem_level_22
    else -> R.drawable.profile_emblem_level_08
}

private fun DrawScope.drawClassicBadgeFrame(badge: BiblicalBadge) {
    val accent = Color(badge.accentColorHex)
    val highlight = Color.White.copy(alpha = 0.58f)
    val shadow = Color.Black.copy(alpha = 0.46f)
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.43f
    val level = badge.level ?: when (badge.frameStyle) {
        BadgeFrameStyle.SIMPLE -> 1
        BadgeFrameStyle.SEEDLING -> 2
        BadgeFrameStyle.STAR -> 3
        BadgeFrameStyle.OLIVE_BRANCH -> 4
        BadgeFrameStyle.GOLDEN_BOOK -> 5
        BadgeFrameStyle.MASTER_WORD -> 6
        BadgeFrameStyle.GUARDIAN_SHIELD -> 7
        BadgeFrameStyle.PROFILE_EMBLEM -> 8
    }
    val stroke = size.minDimension * (0.018f + level * 0.0035f)
    val outerRadius = radius * (1.03f + level * 0.012f)

    drawCircle(shadow, outerRadius * 1.06f, center, style = Stroke(stroke * 2.4f))
    drawCircle(accent.copy(alpha = 0.20f + level * 0.025f), outerRadius * 1.08f, center, style = Stroke(stroke * 1.25f))

    val leftStart = 132f - level * 1.2f
    val leftSweep = 124f + level * 2.5f
    val rightStart = 48f + level * 1.2f
    val rightSweep = -(124f + level * 2.5f)
    drawMetalArc(center, outerRadius, leftStart, leftSweep, stroke, accent, highlight, shadow)
    drawMetalArc(center, outerRadius, rightStart, rightSweep, stroke, accent, highlight, shadow)

    val leavesPerSide = (level + 1).coerceAtMost(8)
    repeat(leavesPerSide) { index ->
        val fraction = (index + 1f) / (leavesPerSide + 1f)
        val leafSize = radius * (0.065f + level * 0.006f)
        val leftAngle = leftStart + leftSweep * fraction
        val rightAngle = rightStart + rightSweep * fraction
        drawMetalLeaf(pointOnCircle(center, outerRadius * 1.01f, leftAngle), accent, highlight, leftAngle - 90f, leafSize)
        drawMetalLeaf(pointOnCircle(center, outerRadius * 1.01f, rightAngle), accent, highlight, rightAngle + 90f, leafSize)
    }

    drawMedallion(
        pointOnCircle(center, outerRadius * 1.02f, 90f),
        accent,
        highlight,
        shadow,
        radius * (0.075f + level * 0.006f),
        level
    )
    when {
        level >= 7 -> drawShield(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, shadow, radius * 0.14f)
        level >= 6 -> drawCrest(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.13f, 8)
        level >= 5 -> drawBook(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.13f)
        level >= 3 -> drawCrest(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.105f, 6)
    }
}

private fun DrawScope.drawMetalArc(
    center: Offset,
    radius: Float,
    startAngle: Float,
    sweepAngle: Float,
    stroke: Float,
    accent: Color,
    highlight: Color,
    shadow: Color
) {
    val bounds = Size(radius * 2f, radius * 2f)
    val topLeft = Offset(center.x - radius, center.y - radius)
    drawArc(shadow, startAngle + 2f, sweepAngle, false, topLeft, bounds, style = Stroke(stroke * 2.1f))
    drawArc(accent, startAngle, sweepAngle, false, topLeft, bounds, style = Stroke(stroke * 1.45f))
    drawArc(highlight, startAngle - 1.5f, sweepAngle * 0.76f, false, topLeft, bounds, style = Stroke(stroke * 0.34f))
}

private fun DrawScope.pointOnCircle(center: Offset, radius: Float, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(center.x + cos(radians) * radius, center.y + sin(radians) * radius)
}

private fun DrawScope.drawMetalLeaf(
    center: Offset,
    color: Color,
    highlight: Color,
    degrees: Float,
    length: Float
) {
    rotate(degrees, center) {
        val path = Path().apply {
            moveTo(center.x, center.y - length)
            quadraticBezierTo(center.x + length * 1.3f, center.y - length * 0.28f, center.x, center.y + length)
            quadraticBezierTo(center.x - length * 1.3f, center.y - length * 0.28f, center.x, center.y - length)
            close()
        }
        drawPath(path, color.copy(alpha = 0.92f), style = Fill)
        drawLine(
            highlight.copy(alpha = 0.62f),
            Offset(center.x, center.y - length * 0.72f),
            Offset(center.x, center.y + length * 0.68f),
            length * 0.13f
        )
    }
}

private fun DrawScope.drawMedallion(
    center: Offset,
    color: Color,
    highlight: Color,
    shadow: Color,
    radius: Float,
    level: Int
) {
    drawCircle(shadow, radius * 1.35f, center)
    drawCrest(center, color, highlight, radius, if (level >= 5) 6 else 4)
}

private fun DrawScope.drawCrest(center: Offset, color: Color, highlight: Color, radius: Float, points: Int) {
    val path = Path()
    repeat(points * 2) { index ->
        val angle = -PI.toFloat() / 2f + index * PI.toFloat() / points
        val rr = if (index % 2 == 0) radius else radius * 0.48f
        val p = Offset(center.x + cos(angle) * rr, center.y + sin(angle) * rr)
        if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    drawPath(path, color.copy(alpha = 0.94f), style = Fill)
    drawPath(path, highlight.copy(alpha = 0.68f), style = Stroke(radius * 0.12f))
}

private fun DrawScope.drawBook(center: Offset, color: Color, highlight: Color, size: Float) {
    val left = Path().apply {
        moveTo(center.x, center.y - size * .62f)
        quadraticBezierTo(center.x - size * .78f, center.y - size * .78f, center.x - size, center.y - size * .25f)
        lineTo(center.x - size, center.y + size * .62f)
        quadraticBezierTo(center.x - size * .46f, center.y + size * .42f, center.x, center.y + size * .76f)
        close()
    }
    val right = Path().apply {
        moveTo(center.x, center.y - size * .62f)
        quadraticBezierTo(center.x + size * .78f, center.y - size * .78f, center.x + size, center.y - size * .25f)
        lineTo(center.x + size, center.y + size * .62f)
        quadraticBezierTo(center.x + size * .46f, center.y + size * .42f, center.x, center.y + size * .76f)
        close()
    }
    drawPath(left, color, style = Fill)
    drawPath(right, color, style = Fill)
    drawLine(
        highlight,
        Offset(center.x, center.y - size * .58f),
        Offset(center.x, center.y + size * .62f),
        size * .10f
    )
}

private fun DrawScope.drawShield(center: Offset, color: Color, highlight: Color, shadow: Color, size: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size * .82f, center.y - size * .46f)
        lineTo(center.x + size * .64f, center.y + size * .62f)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size * .64f, center.y + size * .62f)
        lineTo(center.x - size * .82f, center.y - size * .46f)
        close()
    }
    drawPath(path, shadow, style = Fill)
    drawPath(path, color, style = Stroke(size * .20f))
    drawLine(highlight, Offset(center.x, center.y - size * .48f), Offset(center.x, center.y + size * .48f), size * .13f)
    drawLine(highlight, Offset(center.x - size * .36f, center.y - size * .02f), Offset(center.x + size * .36f, center.y - size * .02f), size * .13f)
}
