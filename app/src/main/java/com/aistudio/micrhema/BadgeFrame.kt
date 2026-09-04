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
    Box(modifier = clickableModifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) { drawBadgeFrame(badge) }
        BiblicalAvatarImage(
            avatar = avatar,
            modifier = Modifier.fillMaxSize(0.72f).clip(CircleShape),
            contentDescription = contentDescription
        )
    }
}

/** Moldura de insígnia inspirada em coroas/lauréis de RPG, com evolução por nível. */
private fun DrawScope.drawBadgeFrame(badge: BiblicalBadge) {
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

    drawCircle(color = shadow, center = center, radius = outerRadius * 1.06f, style = Stroke(stroke * 2.4f))
    drawCircle(color = accent.copy(alpha = 0.20f + level * 0.025f), center = center, radius = outerRadius * 1.08f, style = Stroke(stroke * 1.25f))

    val leftStart = 132f - level * 1.2f
    val leftSweep = 124f + level * 2.5f
    val rightStart = 48f + level * 1.2f
    val rightSweep = -(124f + level * 2.5f)
    drawMetalArc(center, outerRadius, leftStart, leftSweep, stroke, accent, highlight, shadow)
    drawMetalArc(center, outerRadius, rightStart, rightSweep, stroke, accent, highlight, shadow)

    val leavesPerSide = (level + 1).coerceAtMost(8)
    repeat(leavesPerSide) { index ->
        val fraction = (index + 1f) / (leavesPerSide + 1f)
        val leftAngle = leftStart + leftSweep * fraction
        val rightAngle = rightStart + rightSweep * fraction
        val leafSize = radius * (0.065f + level * 0.006f)
        val leftPoint = pointOnCircle(center, outerRadius * 1.01f, leftAngle)
        val rightPoint = pointOnCircle(center, outerRadius * 1.01f, rightAngle)
        drawMetalLeaf(leftPoint, accent, highlight, leftAngle - 90f, leafSize)
        drawMetalLeaf(rightPoint, accent, highlight, rightAngle + 90f, leafSize)
    }

    val bottomPoint = pointOnCircle(center, outerRadius * 1.02f, 90f)
    drawMedallion(bottomPoint, accent, highlight, shadow, radius * (0.075f + level * 0.006f), level)

    when {
        level >= 7 -> drawShield(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, shadow, radius * 0.14f)
        level >= 6 -> {
            drawCrest(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.13f, points = 8)
            drawStar(pointOnCircle(center, outerRadius * 1.02f, 210f), radius * 0.065f, highlight)
            drawStar(pointOnCircle(center, outerRadius * 1.02f, 330f), radius * 0.065f, highlight)
        }
        level >= 5 -> drawBook(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.13f)
        level >= 3 -> drawCrest(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.105f, points = 6)
    }

    if (level >= 4) {
        drawArc(
            color = highlight.copy(alpha = 0.38f), startAngle = leftStart + 3f, sweepAngle = leftSweep - 6f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius * 0.965f, center.y - outerRadius * 0.965f),
            size = Size(outerRadius * 1.93f, outerRadius * 1.93f), style = Stroke(stroke * 0.42f)
        )
        drawArc(
            color = highlight.copy(alpha = 0.38f), startAngle = rightStart - 3f, sweepAngle = rightSweep + 6f,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius * 0.965f, center.y - outerRadius * 0.965f),
            size = Size(outerRadius * 1.93f, outerRadius * 1.93f), style = Stroke(stroke * 0.42f)
        )
    }
}

private fun DrawScope.drawMetalArc(center: Offset, radius: Float, startAngle: Float, sweepAngle: Float, stroke: Float, accent: Color, highlight: Color, shadow: Color) {
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

private fun DrawScope.drawMetalLeaf(center: Offset, color: Color, highlight: Color, degrees: Float, length: Float) {
    rotate(degrees, center) {
        val path = Path().apply {
            moveTo(center.x, center.y - length)
            quadraticBezierTo(center.x + length * 1.3f, center.y - length * 0.28f, center.x, center.y + length)
            quadraticBezierTo(center.x - length * 1.3f, center.y - length * 0.28f, center.x, center.y - length)
            close()
        }
        drawPath(path, color = color.copy(alpha = 0.92f), style = Fill)
        drawLine(color = highlight.copy(alpha = 0.62f), start = Offset(center.x, center.y - length * 0.72f), end = Offset(center.x, center.y + length * 0.68f), strokeWidth = length * 0.13f)
    }
}

private fun DrawScope.drawMedallion(center: Offset, color: Color, highlight: Color, shadow: Color, radius: Float, level: Int) {
    drawCircle(shadow, radius * 1.35f, center)
    drawCrest(center, color, highlight, radius, points = if (level >= 5) 6 else 4)
}

private fun DrawScope.drawCrest(center: Offset, color: Color, highlight: Color, radius: Float, points: Int) {
    val path = Path()
    repeat(points * 2) { index ->
        val angle = -PI.toFloat() / 2f + index * PI.toFloat() / points
        val currentRadius = if (index % 2 == 0) radius else radius * 0.48f
        val point = Offset(center.x + cos(angle) * currentRadius, center.y + sin(angle) * currentRadius)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color.copy(alpha = 0.94f), style = Fill)
    drawPath(path, highlight.copy(alpha = 0.68f), style = Stroke(radius * 0.12f))
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    drawCrest(center, color, Color.White.copy(alpha = 0.7f), radius, points = 5)
}

private fun DrawScope.drawBook(center: Offset, color: Color, highlight: Color, size: Float) {
    val left = Path().apply {
        moveTo(center.x, center.y - size * 0.62f)
        quadraticBezierTo(center.x - size * 0.78f, center.y - size * 0.78f, center.x - size, center.y - size * 0.25f)
        lineTo(center.x - size, center.y + size * 0.62f)
        quadraticBezierTo(center.x - size * 0.46f, center.y + size * 0.42f, center.x, center.y + size * 0.76f)
        close()
    }
    val right = Path().apply {
        moveTo(center.x, center.y - size * 0.62f)
        quadraticBezierTo(center.x + size * 0.78f, center.y - size * 0.78f, center.x + size, center.y - size * 0.25f)
        lineTo(center.x + size, center.y + size * 0.62f)
        quadraticBezierTo(center.x + size * 0.46f, center.y + size * 0.42f, center.x, center.y + size * 0.76f)
        close()
    }
    drawPath(left, color, style = Fill)
    drawPath(right, color, style = Fill)
    drawLine(highlight, Offset(center.x, center.y - size * 0.58f), Offset(center.x, center.y + size * 0.62f), strokeWidth = size * 0.10f)
}

private fun DrawScope.drawShield(center: Offset, color: Color, highlight: Color, shadow: Color, size: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size * 0.82f, center.y - size * 0.46f)
        lineTo(center.x + size * 0.64f, center.y + size * 0.62f)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size * 0.64f, center.y + size * 0.62f)
        lineTo(center.x - size * 0.82f, center.y - size * 0.46f)
        close()
    }
    drawPath(path, shadow, style = Fill)
    drawPath(path, color, style = Stroke(size * 0.20f))
    drawLine(highlight, Offset(center.x, center.y - size * 0.48f), Offset(center.x, center.y + size * 0.48f), strokeWidth = size * 0.13f)
    drawLine(highlight, Offset(center.x - size * 0.36f, center.y - size * 0.02f), Offset(center.x + size * 0.36f, center.y - size * 0.02f), strokeWidth = size * 0.13f)
}
