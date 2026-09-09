package com.aistudio.micrhema

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

fun isBuiltinCosmetic(id: String): Boolean =
    id == XpRewardManager.PROMISE_FRAME || id == XpRewardManager.READER_BADGE

fun cosmeticRewardId(item: AdminProfileCosmetic): String =
    if (isBuiltinCosmetic(item.id)) item.id else "cosmetic:${item.id}"

fun builtinProfileCosmetics(): List<AdminProfileCosmetic> = listOf(
    AdminProfileCosmetic(XpRewardManager.PROMISE_FRAME, "moldura", "Moldura Luz da Promessa",
        "Aro dourado com contorno lilás.", "Adquira na Loja XP.", "", true, true, 300),
    AdminProfileCosmetic(XpRewardManager.READER_BADGE, "distintivo", "Distintivo Leitor da Palavra",
        "Estrela dourada para o perfil.", "Adquira na Loja XP.", "", true, true, 750)
)

@Composable
fun BuiltinCosmeticImage(item: AdminProfileCosmetic, modifier: Modifier = Modifier) {
    Canvas(modifier.semantics { contentDescription = item.name }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        if (item.id == XpRewardManager.PROMISE_FRAME) {
            val radius = size.minDimension * .485f
            drawCircle(Color(0xFF6D4CFF).copy(alpha = .28f), radius, center, style = Stroke(size.minDimension * .075f))
            drawCircle(Color(0xFFFFD76A), radius, center, style = Stroke(size.minDimension * .025f))
            drawCircle(Color.White.copy(alpha = .75f), radius * .94f, center, style = Stroke(size.minDimension * .008f))
        } else {
            drawCrest(center, Color(0xFFFFC107), Color.White, size.minDimension * .45f, 6)
        }
    }
}

@Composable
fun PrimaryDistinctiveOverlay(item: AdminProfileCosmetic, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val diameter = minOf(maxWidth, maxHeight) * .252f
        Box(
            modifier = Modifier
                .offset(x = maxWidth * .82f - diameter / 2, y = maxHeight * .80f - diameter / 2)
                .size(diameter)
                .background(Color(0xFF1B1B1F).copy(alpha = .82f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            DistinctiveImage(item, Modifier.fillMaxSize(.9f))
        }
    }
}
