package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.foundation.shape.CircleShape
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun BiblicalAvatarWithBadge(
    avatar: BiblicalAvatar,
    badge: BiblicalBadge,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = avatar.displayName
) {
    val clickableModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Box(
        modifier = clickableModifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawBadgeFrame(badge)
        }
        BiblicalAvatarImage(
            avatar = avatar,
            modifier = Modifier.fillMaxSize(0.72f).clip(CircleShape),
            contentDescription = contentDescription
        )
    }
}

private fun DrawScope.drawBadgeFrame(badge: BiblicalBadge) {
    val accent = Color(badge.accentColorHex)
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.43f
    val strokeWidth = size.minDimension * 0.035f
    val softAccent = accent.copy(alpha = 0.35f)

    drawCircle(color = softAccent, radius = radius * 1.08f, center = center, style = Stroke(strokeWidth * 0.75f))
    drawCircle(color = accent.copy(alpha = 0.22f), radius = radius * 0.96f, center = center, style = Stroke(strokeWidth * 2.2f))

    when (badge.frameStyle) {
        BadgeFrameStyle.SIMPLE -> {
            drawArc(accent, 205f, 130f, false, style = Stroke(strokeWidth * 1.2f))
            drawArc(accent, 25f, 130f, false, style = Stroke(strokeWidth * 1.2f))
        }
        BadgeFrameStyle.SEEDLING -> {
            drawArc(accent, 205f, 130f, false, style = Stroke(strokeWidth * 1.5f))
            drawArc(accent, 25f, 130f, false, style = Stroke(strokeWidth * 1.5f))
            drawLeaf(center + Offset(-radius * 0.72f, radius * 0.18f), accent, -35f, radius * 0.14f)
            drawLeaf(center + Offset(radius * 0.72f, radius * 0.18f), accent, 35f, radius * 0.14f)
        }
        BadgeFrameStyle.STAR -> {
            drawArc(accent, 205f, 130f, false, style = Stroke(strokeWidth * 1.6f))
            drawArc(accent, 25f, 130f, false, style = Stroke(strokeWidth * 1.6f))
            drawStar(center + Offset(0f, -radius * 0.98f), radius * 0.13f, accent)
        }
        BadgeFrameStyle.OLIVE_BRANCH -> {
            drawArc(accent, 195f, 150f, false, style = Stroke(strokeWidth * 1.8f))
            drawArc(accent, 15f, 150f, false, style = Stroke(strokeWidth * 1.8f))
            repeat(4) { index ->
                val angle = 205f + index * 32f
                val point = pointOnCircle(center, radius * 0.98f, angle)
                drawLeaf(point, accent, angle + 90f, radius * 0.11f)
                val otherPoint = pointOnCircle(center, radius * 0.98f, 335f - index * 32f)
                drawLeaf(otherPoint, accent, angle - 90f, radius * 0.11f)
            }
        }
        BadgeFrameStyle.GOLDEN_BOOK -> {
            drawArc(accent, 200f, 140f, false, style = Stroke(strokeWidth * 2f))
            drawArc(accent, 20f, 140f, false, style = Stroke(strokeWidth * 2f))
            drawBook(center + Offset(0f, -radius * 1.02f), accent, radius * 0.16f)
        }
        BadgeFrameStyle.MASTER_WORD -> {
            drawArc(accent, 190f, 160f, false, style = Stroke(strokeWidth * 2.2f))
            drawArc(accent, 10f, 160f, false, style = Stroke(strokeWidth * 2.2f))
            drawStar(center + Offset(0f, -radius * 1.02f), radius * 0.16f, accent)
            drawStar(center + Offset(-radius * 0.9f, radius * 0.05f), radius * 0.08f, accent)
            drawStar(center + Offset(radius * 0.9f, radius * 0.05f), radius * 0.08f, accent)
        }
        BadgeFrameStyle.GUARDIAN_SHIELD -> {
            drawArc(accent, 180f, 180f, false, style = Stroke(strokeWidth * 2.4f))
            drawShield(center + Offset(0f, -radius * 1.02f), accent, radius * 0.18f)
            drawLeaf(center + Offset(-radius * 0.78f, radius * 0.12f), accent, -45f, radius * 0.13f)
            drawLeaf(center + Offset(radius * 0.78f, radius * 0.12f), accent, 45f, radius * 0.13f)
        }
    }
}

private fun DrawScope.pointOnCircle(center: Offset, radius: Float, degrees: Float): Offset {
    val radians = degrees * PI.toFloat() / 180f
    return Offset(center.x + cos(radians) * radius, center.y + sin(radians) * radius)
}

private fun DrawScope.drawLeaf(center: Offset, color: Color, degrees: Float, length: Float) {
    rotate(degrees, center) {
        val path = Path().apply {
            moveTo(center.x, center.y - length)
            quadraticBezierTo(center.x + length * 1.15f, center.y - length * 0.2f, center.x, center.y + length)
            quadraticBezierTo(center.x - length * 1.15f, center.y - length * 0.2f, center.x, center.y - length)
            close()
        }
        drawPath(path, color = color, style = Fill)
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    repeat(10) { index ->
        val angle = -PI.toFloat() / 2f + index * PI.toFloat() / 5f
        val currentRadius = if (index % 2 == 0) radius else radius * 0.42f
        val point = Offset(center.x + cos(angle) * currentRadius, center.y + sin(angle) * currentRadius)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(path, color = color, style = Fill)
}

private fun DrawScope.drawBook(center: Offset, color: Color, size: Float) {
    drawLine(color, center, center + Offset(0f, size), strokeWidth = size * 0.12f)
    drawArc(
        color = color,
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - size, center.y - size * 0.45f),
        size = Size(size, size * 1.1f),
        style = Stroke(size * 0.1f)
    )
    drawArc(
        color = color,
        startAngle = 90f,
        sweepAngle = -180f,
        useCenter = false,
        topLeft = Offset(center.x, center.y - size * 0.45f),
        size = Size(size, size * 1.1f),
        style = Stroke(size * 0.1f)
    )
}

private fun DrawScope.drawShield(center: Offset, color: Color, size: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        lineTo(center.x + size, center.y - size * 0.45f)
        lineTo(center.x + size * 0.72f, center.y + size * 0.65f)
        lineTo(center.x, center.y + size)
        lineTo(center.x - size * 0.72f, center.y + size * 0.65f)
        lineTo(center.x - size, center.y - size * 0.45f)
        close()
    }
    drawPath(path, color = color, style = Stroke(size * 0.14f))
}
