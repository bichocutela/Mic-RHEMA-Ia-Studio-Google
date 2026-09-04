package com.aistudio.micrhema

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

private data class AchievementMission(
    val title: String,
    val current: Int,
    val target: Int,
    val unit: String
) {
    val completed: Boolean get() = current >= target
    val progress: Float get() = if (target <= 0) 1f else (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val remaining: Int get() = (target - current).coerceAtLeast(0)
}

private fun BadgeProgressSummary.levelCount(key: String): Int = levelActivityCounts[key] ?: 0

private fun missionsForNextBadge(summary: BadgeProgressSummary): List<AchievementMission> {
    return when (summary.nextLevel?.id) {
        "semeador" -> listOf(
            AchievementMission("Concluir devocionais", summary.levelCount(BadgeActivityKeys.DEVOTIONALS), 3, "devocionais"),
            AchievementMission("Explorar temas de planos", summary.levelCount(BadgeActivityKeys.PLAN_THEMES), 1, "tema")
        )
        "discipulo" -> listOf(
            AchievementMission("Concluir um plano", summary.levelCount(BadgeActivityKeys.PLANS), 1, "plano"),
            AchievementMission("Explorar temas de planos", summary.levelCount(BadgeActivityKeys.PLAN_THEMES), 3, "temas"),
            AchievementMission("Ler capítulos da Bíblia", summary.levelCount(BadgeActivityKeys.BIBLE_CHAPTERS), 3, "capítulos")
        )
        "perseverante" -> listOf(
            AchievementMission("Permanecer ativo no MIC Rhema", summary.levelActiveMinutes, 60, "minutos"),
            AchievementMission("Registrar atividades válidas", summary.levelActivityCounts.values.sum(), 10, "atividades")
        )
        "estudante_rhema" -> listOf(
            AchievementMission("Ler livros", summary.levelCount(BadgeActivityKeys.BOOKS), 3, "livros"),
            AchievementMission("Assistir vídeos", summary.levelCount(BadgeActivityKeys.VIDEOS), 3, "vídeos"),
            AchievementMission("Ouvir áudios", summary.levelCount(BadgeActivityKeys.AUDIOS), 2, "áudios")
        )
        "mestre_da_palavra" -> listOf(
            AchievementMission("Concluir curso IBR", summary.levelCompletedIbrCourses, 1, "curso"),
            AchievementMission("Ler notícias bíblicas", summary.levelCount(BadgeActivityKeys.BIBLE_NEWS), 3, "notícias"),
            AchievementMission("Ler capítulos da Bíblia", summary.levelCount(BadgeActivityKeys.BIBLE_CHAPTERS), 10, "capítulos")
        )
        "guardiao_da_fe" -> listOf(
            AchievementMission(
                "Usar todas as áreas de atividade",
                summary.levelActivityCounts.values.count { it >= 1 },
                summary.levelActivityCounts.size.coerceAtLeast(1),
                "áreas"
            ),
            AchievementMission("Permanecer ativo no MIC Rhema", summary.levelActiveMinutes, 180, "minutos")
        )
        else -> emptyList()
    }
}

@Composable
fun AchievementProgressDialog(
    progress: BadgeProgressSummary,
    onDismiss: () -> Unit
) {
    val nextBadge = progress.nextLevel
    val missions = missionsForNextBadge(progress)
    val completed = missions.filter { it.completed }
    val pending = missions.filterNot { it.completed }
    val percent = (progress.progressToNextLevel * 100f).roundToInt().coerceIn(0, 100)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Progresso das conquistas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            nextBadge?.let { "Próximo desbloqueio: ${it.name}" } ?: "Todos os níveis principais concluídos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (nextBadge == null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            "Você concluiu todas as missões dos níveis principais disponíveis.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${completed.size} de ${missions.size} missões concluídas", fontWeight = FontWeight.SemiBold)
                                Text("$percent%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { progress.progressToNextLevel },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                nextBadge.requirement,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (completed.isNotEmpty()) {
                        Text("✓ O que você já fez neste nível", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        completed.forEach { mission -> MissionProgressCard(mission) }
                    }

                    if (pending.isNotEmpty()) {
                        Text("Ainda falta para desbloquear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        pending.forEach { mission -> MissionProgressCard(mission) }
                    }

                    Text(
                        "Cada nível começa do zero. O histórico anterior continua salvo no perfil, mas só as atividades feitas depois do desbloqueio do nível atual contam para estas missões. Repetir o mesmo conteúdo também não aumenta a contagem novamente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun MissionProgressCard(mission: AchievementMission) {
    val statusText = if (mission.completed) {
        "Concluído • ${mission.current}/${mission.target} ${mission.unit}"
    } else {
        "${mission.current}/${mission.target} ${mission.unit} • faltam ${mission.remaining}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (mission.completed) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (mission.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (mission.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(mission.title, fontWeight = FontWeight.SemiBold)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LinearProgressIndicator(
                progress = { mission.progress },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
