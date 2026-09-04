package com.aistudio.micrhema

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Celebração global exibida quando um nível/emblema é conquistado. */
@Composable
fun BadgeUnlockCelebration(
    notification: BadgeAwardNotification,
    avatar: BiblicalAvatar,
    onOpenBadge: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val badge = notification.badges.maxByOrNull { it.level ?: 0 } ?: return
    val haptic = LocalHapticFeedback.current
    val scale = remember(badge.id) { Animatable(0.62f) }

    LaunchedEffect(badge.id) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xF51A2430),
                            Color(0xF52E271C),
                            Color(0xFA111820)
                        )
                    )
                )
                .padding(horizontal = 22.dp, vertical = 34.dp),
            contentAlignment = Alignment.Center
        ) {
            ConfettiBurst(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .zIndex(2f),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                tonalElevation = 14.dp,
                shadowElevation = 22.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (badge.level != null) "NOVO NÍVEL DESBLOQUEADO!" else "NOVO EMBLEMA!",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Parabéns!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (notification.badges.size > 1)
                            "Você conquistou ${notification.badges.size} novos emblemas de uma vez."
                        else "Sua constância fez você avançar na jornada MIC Rhema.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    BiblicalAvatarWithBadge(
                        avatar = avatar,
                        badge = badge,
                        modifier = Modifier.size(218.dp).scale(scale.value),
                        contentDescription = "Emblema ${badge.name} conquistado"
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = if (badge.level != null) "Nível ${badge.level} · ${badge.name}" else badge.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = badge.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (notification.badges.size > 1) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "+ ${notification.badges.size - 1} conquista(s) desbloqueada(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = { onOpenBadge(badge.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Ver e usar meu emblema", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Continuar depois")
                    }
                }
            }
        }
    }
}
