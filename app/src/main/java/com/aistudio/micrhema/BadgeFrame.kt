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
    val isProfileEmblem = badge.frameStyle == BadgeFrameStyle.PROFILE_EMBLEM
    Box(modifier = clickableModifier, contentAlignment = Alignment.Center) {
        if (isProfileEmblem) {
            BiblicalAvatarImage(
                avatar = avatar,
                modifier = Modifier.fillMaxSize(0.57f).clip(CircleShape),
                contentDescription = contentDescription
            )
            Canvas(modifier = Modifier.fillMaxSize()) { drawProfileEmblemFrame(badge) }
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

    drawMedallion(pointOnCircle(center, outerRadius * 1.02f, 90f), accent, highlight, shadow, radius * (0.075f + level * 0.006f), level)
    when {
        level >= 7 -> drawShield(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, shadow, radius * 0.14f)
        level >= 6 -> drawCrest(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.13f, 8)
        level >= 5 -> drawBook(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.13f)
        level >= 3 -> drawCrest(pointOnCircle(center, outerRadius * 1.04f, 270f), accent, highlight, radius * 0.105f, 6)
    }
}

/**
 * Arcos exclusivos dos níveis 8–22, inspirados diretamente na prancha aprovada:
 * folhagem, pedra dourada, escudo, água, videira, luz, armadura, leão,
 * chama, coroa, asas, tabernáculo, arca, cidade celestial e glória.
 */
private fun DrawScope.drawProfileEmblemFrame(badge: BiblicalBadge) {
    val level = (badge.level ?: 8).coerceIn(8, 22)
    val accent = Color(badge.accentColorHex)
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.minDimension * 0.355f
    val outer = size.minDimension * 0.425f
    val unit = size.minDimension
    val white = Color.White
    val gold = Color(0xFFFFD36A)
    val deepGold = Color(0xFF8A5A12)
    val shadow = Color.Black.copy(alpha = 0.55f)

    drawCircle(shadow, outer * 1.035f, center, style = Stroke(unit * 0.052f))
    drawCircle(accent.copy(alpha = 0.92f), radius * 1.03f, center, style = Stroke(unit * 0.028f))
    drawCircle(white.copy(alpha = 0.55f), radius * 0.985f, center, style = Stroke(unit * 0.007f))

    when (level) {
        8 -> drawLeafWreath(center, outer, accent, gold, unit)
        9 -> drawPromisePath(center, outer, gold, deepGold, unit)
        10 -> drawFaithShield(center, outer, accent, white, unit)
        11 -> drawLivingWaters(center, outer, accent, white, unit)
        12 -> drawTrueVine(center, outer, accent, Color(0xFF7E57C2), unit)
        13 -> drawWorldLight(center, outer, gold, white, unit)
        14 -> drawArmor(center, outer, accent, gold, unit)
        15 -> drawLionFrame(center, outer, gold, Color(0xFF8B1E2D), unit)
        16 -> drawSpiritFlame(center, outer, Color(0xFFFF6D00), gold, unit)
        17 -> drawLifeCrown(center, outer, gold, Color(0xFF7E57C2), unit)
        18 -> drawPromiseWings(center, outer, Color(0xFFE7F4FF), accent, unit)
        19 -> drawTabernacle(center, outer, gold, Color(0xFF5C3A91), unit)
        20 -> drawArk(center, outer, gold, accent, unit)
        21 -> drawNewJerusalem(center, outer, Color(0xFFFFE7A8), Color(0xFFB9E6FF), unit)
        22 -> drawEternalGlory(center, outer, gold, white, unit)
    }
}

private fun DrawScope.drawLeafWreath(c: Offset, r: Float, green: Color, gold: Color, u: Float) {
    repeat(8) { i ->
        val a1 = 135f + i * 17f
        val a2 = 45f - i * 17f
        drawMetalLeaf(pointOnCircle(c, r, a1), green, gold, a1 - 90f, u * 0.035f)
        drawMetalLeaf(pointOnCircle(c, r, a2), green, gold, a2 + 90f, u * 0.035f)
    }
    drawCrest(pointOnCircle(c, r * 1.01f, 90f), green, gold, u * 0.052f, 4)
}

private fun DrawScope.drawPromisePath(c: Offset, r: Float, gold: Color, stone: Color, u: Float) {
    repeat(12) { i ->
        val angle = i * 30f
        val p = pointOnCircle(c, r * 1.02f, angle)
        drawCircle(if (i % 2 == 0) gold else Color(0xFFC8C1AE), u * 0.022f, p)
    }
    val bottom = pointOnCircle(c, r, 90f)
    repeat(5) { i ->
        val y = bottom.y + i * u * 0.016f
        val half = u * (0.10f - i * 0.012f)
        drawLine(stone, Offset(c.x - half, y), Offset(c.x + half, y), u * 0.012f)
        drawLine(gold.copy(alpha = 0.8f), Offset(c.x - half, y - u * 0.004f), Offset(c.x + half, y - u * 0.004f), u * 0.004f)
    }
    drawStar(pointOnCircle(c, r * 1.08f, 270f), u * 0.040f, Color.White)
}

private fun DrawScope.drawFaithShield(c: Offset, r: Float, blue: Color, white: Color, u: Float) {
    repeat(6) { i ->
        val y = c.y - r * 0.60f + i * r * 0.25f
        drawBlade(Offset(c.x - r * 0.98f, y), u * 0.050f, -30f, blue, white)
        drawBlade(Offset(c.x + r * 0.98f, y), u * 0.050f, 30f, blue, white)
    }
    drawShield(pointOnCircle(c, r * 1.02f, 90f), blue, white, Color.Black.copy(alpha = 0.55f), u * 0.070f)
    drawCrest(pointOnCircle(c, r * 1.03f, 270f), blue, white, u * 0.052f, 4)
}

private fun DrawScope.drawLivingWaters(c: Offset, r: Float, blue: Color, white: Color, u: Float) {
    repeat(7) { i ->
        val spread = (i - 3) * u * 0.045f
        val base = Offset(c.x + spread, c.y + r * 0.78f)
        val path = Path().apply {
            moveTo(base.x, base.y)
            quadraticBezierTo(base.x + spread * 0.35f, base.y - u * (0.10f + i % 3 * 0.018f), base.x + spread * 0.18f, base.y - u * 0.15f)
        }
        drawPath(path, if (i % 2 == 0) blue else Color(0xFF39C6FF), style = Stroke(u * 0.018f))
    }
    drawDrop(pointOnCircle(c, r * 1.02f, 90f), u * 0.050f, blue, white)
    drawArc(white.copy(alpha = 0.55f), 205f, 130f, false, Offset(c.x-r,c.y-r), Size(r*2,r*2), style = Stroke(u*0.007f))
}

private fun DrawScope.drawTrueVine(c: Offset, r: Float, green: Color, purple: Color, u: Float) {
    repeat(10) { i ->
        val angle = 130f + i * 28f
        val p = pointOnCircle(c, r * 1.04f, angle)
        drawMetalLeaf(p, if (i % 3 == 0) Color(0xFF5C8F37) else green, Color(0xFFD7E8A7), angle - 90f, u * 0.030f)
    }
    val grape = pointOnCircle(c, r * 1.04f, 135f)
    repeat(6) { i ->
        val row = i / 3
        val col = i % 3
        drawCircle(purple, u * 0.012f, Offset(grape.x + (col-1)*u*0.020f, grape.y + row*u*0.020f))
    }
}

private fun DrawScope.drawWorldLight(c: Offset, r: Float, gold: Color, white: Color, u: Float) {
    repeat(20) { i ->
        val angle = i * 18f
        val start = pointOnCircle(c, r * 1.03f, angle)
        val end = pointOnCircle(c, r * (1.16f + (i % 3)*0.045f), angle)
        drawLine(if (i % 2 == 0) gold else white, start, end, u * 0.006f)
    }
    drawStar(pointOnCircle(c, r * 1.12f, 270f), u * 0.062f, white)
    drawMedallion(pointOnCircle(c, r * 1.01f, 90f), gold, white, Color.Black.copy(alpha=.4f), u*.060f, 8)
}

private fun DrawScope.drawArmor(c: Offset, r: Float, blue: Color, gold: Color, u: Float) {
    repeat(5) { i ->
        val y = c.y - r*0.55f + i*r*0.28f
        drawBlade(Offset(c.x-r*1.02f,y),u*0.070f,-18f,Color(0xFFC8D2E1),blue)
        drawBlade(Offset(c.x+r*1.02f,y),u*0.070f,18f,Color(0xFFC8D2E1),blue)
    }
    drawShield(pointOnCircle(c,r*1.03f,90f),blue,gold,Color.Black.copy(alpha=.55f),u*.075f)
    drawCrest(pointOnCircle(c,r*1.04f,270f),blue,gold,u*.055f,5)
}

private fun DrawScope.drawLionFrame(c: Offset, r: Float, gold: Color, red: Color, u: Float) {
    drawArc(red, 25f, 130f, false, Offset(c.x-r*1.02f,c.y-r*1.02f), Size(r*2.04f,r*2.04f), style=Stroke(u*.026f))
    drawArc(red, 205f, 130f, false, Offset(c.x-r*1.02f,c.y-r*1.02f), Size(r*2.04f,r*2.04f), style=Stroke(u*.026f))
    drawLionMedallion(pointOnCircle(c,r*1.02f,270f),u*.060f,gold)
    drawLionMedallion(pointOnCircle(c,r*1.02f,90f),u*.060f,gold)
}

private fun DrawScope.drawSpiritFlame(c: Offset, r: Float, orange: Color, gold: Color, u: Float) {
    repeat(8) { i ->
        val angle = 145f + i * 36f
        val p = pointOnCircle(c,r*1.02f,angle)
        drawFlame(p,u*(0.045f + (i%3)*0.009f),orange,gold,angle-90f)
    }
    drawFlame(pointOnCircle(c,r*1.12f,270f),u*.085f,orange,gold,0f)
    drawWingPair(pointOnCircle(c,r*.98f,90f),u*.060f,Color.White,gold)
}

private fun DrawScope.drawLifeCrown(c: Offset, r: Float, gold: Color, purple: Color, u: Float) {
    repeat(8) { i ->
        val angle = 130f + i*40f
        drawBlade(pointOnCircle(c,r*1.03f,angle),u*.045f,angle-90f,gold,purple)
    }
    drawCrown(pointOnCircle(c,r*1.09f,270f),u*.095f,gold,purple)
    drawCrest(pointOnCircle(c,r*1.02f,90f),purple,gold,u*.055f,6)
}

private fun DrawScope.drawPromiseWings(c: Offset, r: Float, white: Color, blue: Color, u: Float) {
    repeat(7) { i ->
        val y=c.y-r*.45f+i*u*.040f
        drawBlade(Offset(c.x-r*1.02f-i*u*.010f,y),u*.070f,-55f,white,blue)
        drawBlade(Offset(c.x+r*1.02f+i*u*.010f,y),u*.070f,55f,white,blue)
    }
    drawMedallion(pointOnCircle(c,r*1.02f,90f),blue,white,Color.Black.copy(alpha=.4f),u*.065f,8)
}

private fun DrawScope.drawTabernacle(c: Offset, r: Float, gold: Color, purple: Color, u: Float) {
    val left=c.x-r*.84f; val right=c.x+r*.84f; val top=c.y-r*.84f; val bottom=c.y+r*.80f
    drawLine(gold,Offset(left,bottom),Offset(left,top),u*.026f)
    drawLine(gold,Offset(right,bottom),Offset(right,top),u*.026f)
    drawLine(gold,Offset(left,top),Offset(c.x,top-u*.065f),u*.026f)
    drawLine(gold,Offset(c.x,top-u*.065f),Offset(right,top),u*.026f)
    drawArc(purple,180f,180f,false,Offset(c.x-r*.52f,c.y-r*.82f),Size(r*1.04f,r*1.30f),style=Stroke(u*.030f))
    drawBook(Offset(c.x,top-u*.03f),gold,Color.White,u*.060f)
    drawCrest(pointOnCircle(c,r*1.02f,90f),purple,gold,u*.060f,6)
}

private fun DrawScope.drawArk(c: Offset, r: Float, gold: Color, blue: Color, u: Float) {
    val bottom=c.y+r*.82f
    drawLine(gold,Offset(c.x-r*.88f,bottom),Offset(c.x+r*.88f,bottom),u*.034f)
    drawLine(gold,Offset(c.x-r*.72f,bottom),Offset(c.x-r*.72f,c.y-r*.72f),u*.026f)
    drawLine(gold,Offset(c.x+r*.72f,bottom),Offset(c.x+r*.72f,c.y-r*.72f),u*.026f)
    drawWingPair(pointOnCircle(c,r*.98f,270f),u*.075f,gold,Color.White)
    drawCrest(pointOnCircle(c,r*1.01f,90f),blue,gold,u*.060f,4)
}

private fun DrawScope.drawNewJerusalem(c: Offset, r: Float, gold: Color, ice: Color, u: Float) {
    val bottom=c.y+r*.84f
    repeat(7) { i ->
        val x=c.x+(i-3)*u*.050f
        val h=u*(.10f+(.05f*(3-kotlin.math.abs(i-3))))
        drawLine(gold,Offset(x,bottom),Offset(x,bottom-h),u*.020f)
        drawCrest(Offset(x,bottom-h),ice,gold,u*.020f,4)
    }
    repeat(5) { i ->
        val a=135f+i*45f
        drawCrest(pointOnCircle(c,r*1.04f,a),ice,gold,u*.030f,5)
    }
}

private fun DrawScope.drawEternalGlory(c: Offset, r: Float, gold: Color, white: Color, u: Float) {
    repeat(28) { i ->
        val a=i*(360f/28f)
        drawLine(if(i%2==0)gold else white.copy(alpha=.8f),pointOnCircle(c,r*.98f,a),pointOnCircle(c,r*(1.14f+(i%4)*.025f),a),u*.005f)
    }
    drawPromiseWings(c,r,Color(0xFFFFF8E1),gold,u)
    drawStar(pointOnCircle(c,r*1.10f,270f),u*.075f,white)
    drawCrest(pointOnCircle(c,r*1.03f,90f),gold,white,u*.060f,6)
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
        drawPath(path, color.copy(alpha = 0.92f), style = Fill)
        drawLine(highlight.copy(alpha = 0.62f), Offset(center.x, center.y - length * 0.72f), Offset(center.x, center.y + length * 0.68f), length * 0.13f)
    }
}

private fun DrawScope.drawMedallion(center: Offset, color: Color, highlight: Color, shadow: Color, radius: Float, level: Int) {
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

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) = drawCrest(center, color, Color.White.copy(alpha = 0.7f), radius, 5)

private fun DrawScope.drawBook(center: Offset, color: Color, highlight: Color, size: Float) {
    val left = Path().apply {
        moveTo(center.x, center.y - size * .62f); quadraticBezierTo(center.x-size*.78f,center.y-size*.78f,center.x-size,center.y-size*.25f)
        lineTo(center.x-size,center.y+size*.62f); quadraticBezierTo(center.x-size*.46f,center.y+size*.42f,center.x,center.y+size*.76f); close()
    }
    val right = Path().apply {
        moveTo(center.x, center.y - size * .62f); quadraticBezierTo(center.x+size*.78f,center.y-size*.78f,center.x+size,center.y-size*.25f)
        lineTo(center.x+size,center.y+size*.62f); quadraticBezierTo(center.x+size*.46f,center.y+size*.42f,center.x,center.y+size*.76f); close()
    }
    drawPath(left,color,style=Fill); drawPath(right,color,style=Fill)
    drawLine(highlight,Offset(center.x,center.y-size*.58f),Offset(center.x,center.y+size*.62f),size*.10f)
}

private fun DrawScope.drawShield(center: Offset, color: Color, highlight: Color, shadow: Color, size: Float) {
    val p=Path().apply { moveTo(center.x,center.y-size); lineTo(center.x+size*.82f,center.y-size*.46f); lineTo(center.x+size*.64f,center.y+size*.62f); lineTo(center.x,center.y+size); lineTo(center.x-size*.64f,center.y+size*.62f); lineTo(center.x-size*.82f,center.y-size*.46f); close() }
    drawPath(p,shadow,style=Fill); drawPath(p,color,style=Stroke(size*.20f))
    drawLine(highlight,Offset(center.x,center.y-size*.48f),Offset(center.x,center.y+size*.48f),size*.13f)
    drawLine(highlight,Offset(center.x-size*.36f,center.y-size*.02f),Offset(center.x+size*.36f,center.y-size*.02f),size*.13f)
}

private fun DrawScope.drawBlade(center: Offset, length: Float, degrees: Float, color: Color, edge: Color) {
    rotate(degrees,center) {
        val p=Path().apply { moveTo(center.x,center.y-length); lineTo(center.x+length*.28f,center.y+length*.55f); lineTo(center.x,center.y+length); lineTo(center.x-length*.28f,center.y+length*.55f); close() }
        drawPath(p,color,style=Fill); drawPath(p,edge.copy(alpha=.75f),style=Stroke(length*.10f))
    }
}

private fun DrawScope.drawDrop(center: Offset, size: Float, color: Color, highlight: Color) {
    val p=Path().apply { moveTo(center.x,center.y-size); quadraticBezierTo(center.x+size,center.y,center.x,center.y+size); quadraticBezierTo(center.x-size,center.y,center.x,center.y-size); close() }
    drawPath(p,color,style=Fill); drawPath(p,highlight.copy(alpha=.7f),style=Stroke(size*.12f))
}

private fun DrawScope.drawFlame(center: Offset, size: Float, color: Color, inner: Color, degrees: Float) {
    rotate(degrees,center) {
        val p=Path().apply { moveTo(center.x,center.y-size); quadraticBezierTo(center.x+size*.85f,center.y-size*.10f,center.x,center.y+size); quadraticBezierTo(center.x-size*.70f,center.y,center.x,center.y-size); close() }
        drawPath(p,color,style=Fill)
        val q=Path().apply { moveTo(center.x,center.y-size*.45f); quadraticBezierTo(center.x+size*.35f,center.y,center.x,center.y+size*.45f); quadraticBezierTo(center.x-size*.25f,center.y,center.x,center.y-size*.45f); close() }
        drawPath(q,inner,style=Fill)
    }
}

private fun DrawScope.drawCrown(center: Offset, size: Float, gold: Color, jewel: Color) {
    val p=Path().apply { moveTo(center.x-size,center.y+size*.45f); lineTo(center.x-size*.78f,center.y-size*.55f); lineTo(center.x-size*.32f,center.y); lineTo(center.x,center.y-size); lineTo(center.x+size*.32f,center.y); lineTo(center.x+size*.78f,center.y-size*.55f); lineTo(center.x+size,center.y+size*.45f); close() }
    drawPath(p,gold,style=Fill)
    drawCircle(jewel,size*.16f,center); drawCircle(Color.White.copy(alpha=.75f),size*.05f,Offset(center.x-size*.04f,center.y-size*.04f))
}

private fun DrawScope.drawLionMedallion(center: Offset, size: Float, gold: Color) {
    repeat(10) { i -> drawCrest(center,gold.copy(alpha=.55f),Color.White.copy(alpha=.3f),size*(1.12f-i*.025f),5) }
    drawCircle(gold,size*.55f,center)
    drawCircle(Color.Black.copy(alpha=.58f),size*.08f,Offset(center.x-size*.18f,center.y-size*.08f))
    drawCircle(Color.Black.copy(alpha=.58f),size*.08f,Offset(center.x+size*.18f,center.y-size*.08f))
}

private fun DrawScope.drawWingPair(center: Offset, size: Float, primary: Color, edge: Color) {
    repeat(5) { i ->
        drawBlade(Offset(center.x-size*(.35f+i*.16f),center.y-i*size*.06f),size*(.52f-i*.045f),-62f,primary,edge)
        drawBlade(Offset(center.x+size*(.35f+i*.16f),center.y-i*size*.06f),size*(.52f-i*.045f),62f,primary,edge)
    }
}
