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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.roundToInt

private data class AchievementMission(val title: String, val current: Int, val target: Int, val unit: String) {
    val completed: Boolean get() = current >= target
    val progress: Float get() = if (target <= 0) 1f else (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val remaining: Int get() = (target - current).coerceAtLeast(0)
}

private fun BadgeProgressSummary.levelCount(key: String): Int = levelActivityCounts[key] ?: 0
private fun BadgeProgressSummary.xp(target: Int) = AchievementMission("Acumular XP na Jornada Bíblica", totalXp, target, "XP")
private fun BadgeProgressSummary.chapters(target: Int) = AchievementMission("Ler novos capítulos da Bíblia", levelCount(BadgeActivityKeys.BIBLE_CHAPTERS), target, "capítulos")
private fun BadgeProgressSummary.correct(target: Int) = AchievementMission("Acertar perguntas bíblicas", levelCount(BadgeActivityKeys.QUIZ_CORRECT), target, "acertos")
private fun BadgeProgressSummary.noEasy(target: Int) = AchievementMission("Acertar sem usar Dica Fácil", levelCount(BadgeActivityKeys.QUIZ_CORRECT_NO_EASY_HINT), target, "acertos")
private fun BadgeProgressSummary.noHint(target: Int) = AchievementMission("Acertar sem usar nenhuma dica", levelCount(BadgeActivityKeys.QUIZ_CORRECT_NO_HINT), target, "acertos")
private fun BadgeProgressSummary.hardCorrect(target: Int) = AchievementMission("Acertar perguntas difíceis", levelCount(BadgeActivityKeys.QUIZ_HARD_CORRECT), target, "acertos")
private fun BadgeProgressSummary.active(target: Int) = AchievementMission("Permanecer ativo no MIC Rhema", levelActiveMinutes, target, "minutos")

private val coreKeysForDialog = listOf(
    BadgeActivityKeys.PLANS, BadgeActivityKeys.PLAN_THEMES, BadgeActivityKeys.BOOKS, BadgeActivityKeys.VIDEOS,
    BadgeActivityKeys.BIBLE_CHAPTERS, BadgeActivityKeys.BIBLE_NEWS, BadgeActivityKeys.DEVOTIONALS, BadgeActivityKeys.AUDIOS
)
private fun BadgeProgressSummary.coreAreasMission() = AchievementMission(
    "Usar todas as áreas de atividade",
    coreKeysForDialog.count { levelCount(it) >= 1 },
    coreKeysForDialog.size,
    "áreas"
)

private fun missionsForNextBadge(s: BadgeProgressSummary): List<AchievementMission> = when (s.nextLevel?.id) {
    "semeador" -> listOf(
        AchievementMission("Concluir devocionais", s.levelCount(BadgeActivityKeys.DEVOTIONALS), 3, "devocionais"),
        AchievementMission("Explorar temas de planos", s.levelCount(BadgeActivityKeys.PLAN_THEMES), 1, "tema")
    )
    "discipulo" -> listOf(
        AchievementMission("Concluir um plano", s.levelCount(BadgeActivityKeys.PLANS), 1, "plano"),
        AchievementMission("Explorar temas de planos", s.levelCount(BadgeActivityKeys.PLAN_THEMES), 3, "temas"),
        s.chapters(3)
    )
    "perseverante" -> listOf(s.active(60), AchievementMission("Registrar atividades válidas", coreKeysForDialog.sumOf { s.levelCount(it) }, 10, "atividades"))
    "estudante_rhema" -> listOf(
        AchievementMission("Ler livros", s.levelCount(BadgeActivityKeys.BOOKS), 3, "livros"),
        AchievementMission("Assistir vídeos", s.levelCount(BadgeActivityKeys.VIDEOS), 3, "vídeos"),
        AchievementMission("Ouvir áudios", s.levelCount(BadgeActivityKeys.AUDIOS), 2, "áudios")
    )
    "mestre_da_palavra" -> listOf(
        AchievementMission("Concluir curso IBR", s.levelCompletedIbrCourses, 1, "curso"),
        AchievementMission("Ler notícias bíblicas", s.levelCount(BadgeActivityKeys.BIBLE_NEWS), 3, "notícias"),
        s.chapters(10)
    )
    "guardiao_da_fe" -> listOf(s.coreAreasMission(), s.active(180))
    "semente_da_fe" -> listOf(s.chapters(2), s.correct(2))
    "caminho_da_promessa" -> listOf(s.xp(200), s.chapters(3), s.correct(3))
    "escudo_da_fe" -> listOf(s.xp(350), s.noEasy(5), s.active(10))
    "aguas_vivas" -> listOf(s.xp(500), s.chapters(5), s.correct(5))
    "videira_verdadeira" -> listOf(s.xp(650), AchievementMission("Concluir novos devocionais", s.levelCount(BadgeActivityKeys.DEVOTIONALS), 2, "devocionais"), s.correct(6))
    "luz_do_mundo" -> listOf(s.xp(850), s.noEasy(8), s.chapters(5))
    "armadura_de_deus" -> listOf(s.xp(1050), s.correct(10), s.hardCorrect(3), s.active(15))
    "leao_de_juda" -> listOf(s.xp(1250), s.chapters(10), s.correct(10), s.active(20))
    "chama_do_espirito" -> listOf(s.xp(1450), s.hardCorrect(5), s.noHint(10))
    "coroa_da_vida" -> listOf(s.xp(1650), s.noEasy(12), s.active(30))
    "asas_da_promessa" -> listOf(s.xp(1850), s.chapters(15), s.hardCorrect(8), s.active(30))
    "tabernaculo" -> listOf(s.xp(2050), s.coreAreasMission(), s.hardCorrect(10))
    "arca_da_alianca" -> listOf(s.xp(2300), s.chapters(20), s.hardCorrect(12), s.active(45))
    "nova_jerusalem" -> listOf(s.xp(2600), s.correct(20), s.hardCorrect(15), s.active(60))
    "gloria_eterna" -> listOf(s.xp(3000), s.correct(30), s.hardCorrect(20), s.active(120))
    else -> emptyList()
}

@Composable
fun AchievementProgressDialog(progress: BadgeProgressSummary, onDismiss: () -> Unit) {
    var showJourney by remember { mutableStateOf(false) }
    val member = loggedInMemberState.value
    if (showJourney) {
        if (member != null) {
            BibleJourneyDialog(member = member, onDismiss = { showJourney = false })
            return
        }
        showJourney = false
    }

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
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Progresso das conquistas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            nextBadge?.let { badge ->
                                val rarity = badge.rarity?.let { " • ${it.label}" }.orEmpty()
                                "Próximo desbloqueio: ${badge.name}$rarity"
                            } ?: "Todos os níveis principais concluídos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f)), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Jornada Bíblica", fontWeight = FontWeight.Bold)
                        Text("${progress.totalXp} XP acumulados · Quiz com 300 perguntas, dicas e missões", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (member != null) {
                    XpJourneyPanel(member)
                }

                if (nextBadge == null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)), shape = RoundedCornerShape(18.dp)) {
                        Text("Você concluiu todas as missões dos níveis principais disponíveis.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                } else {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${completed.size} de ${missions.size} missões concluídas", fontWeight = FontWeight.SemiBold)
                                Text("$percent%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(progress = { progress.progressToNextLevel }, modifier = Modifier.fillMaxWidth())
                            Text(nextBadge.requirement, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (completed.isNotEmpty()) {
                        Text("✓ O que você já fez neste nível", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        completed.forEach { MissionProgressCard(it) }
                    }
                    if (pending.isNotEmpty()) {
                        Text("Ainda falta para desbloquear", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        pending.forEach { MissionProgressCard(it) }
                    }
                    Text(
                        if (nextBadge.id == "semente_da_fe")
                            "O Quiz já gera XP Extra desde o início. Ao conquistar o Nível 8, os ganhos pelas demais ferramentas do MIC Rhema e a Loja XP são liberados."
                        else
                            "Cada nível começa do zero nas novas missões. O XP total da Jornada permanece acumulado, mas leitura, quiz, tempo e demais requisitos precisam ser cumpridos novamente depois que o nível atual começa. Repetir o mesmo conteúdo ou a mesma pergunta não gera progresso duplicado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(onClick = { showJourney = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Abrir Jornada Bíblica e Quiz")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Fechar") }
            }
        }
    }
}

@Composable
private fun MissionProgressCard(mission: AchievementMission) {
    val statusText = if (mission.completed) "Concluído • ${mission.current}/${mission.target} ${mission.unit}"
    else "${mission.current}/${mission.target} ${mission.unit} • faltam ${mission.remaining}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (mission.completed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (mission.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (mission.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(mission.title, fontWeight = FontWeight.SemiBold)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LinearProgressIndicator(progress = { mission.progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}
