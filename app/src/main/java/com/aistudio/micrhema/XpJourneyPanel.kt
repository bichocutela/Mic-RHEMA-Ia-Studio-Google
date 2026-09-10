package com.aistudio.micrhema

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class XpDailyMission(
    val title: String,
    val description: String,
    val current: Int,
    val target: Int
) {
    val completed: Boolean get() = current >= target
    val progress: Float get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
}

@Composable
fun XpJourneyPanel(member: MemberRequest) {
    val context = LocalContext.current
    val account = xpAccountState.value?.takeIf { it.memberId == member.id }
    val history = xpHistoryState.value
        ?.takeIf { it.memberId == member.id }
        ?.transactions
        .orEmpty()
    val journeyState = xpJourneyState.value?.takeIf { it.memberId == member.id }
    val xpUnlocked = isXpUnlocked(member)
    val streak = journeyState?.streak ?: 0
    val daily = journeyState?.dailyMission ?: XpDailyMissionState()
    var selectedTab by remember(member.id) { mutableStateOf(0) }

    LaunchedEffect(member.id) {
        // Antes de renderizar o saldo, corrige silenciosamente uma eventual sessão
        // Firebase antiga e consulta o ledger canônico. Assim o membro não precisa
        // responder uma pergunta para o XP aparecer.
        val syncedMember = runCatching { XpSessionSynchronizer.synchronize(context, member) }
            .getOrDefault(member)
        runCatching { XpEngineClient.loadHistoryNow(syncedMember, 100) }
        runCatching { XpEngineClient.loadJourneyStateNow(syncedMember) }
    }

    val dailyMissions = listOf(
        XpDailyMission(
            title = "Palavra do dia",
            description = "Conclua um capítulo da Bíblia",
            current = daily.chapter,
            target = daily.chapterTarget
        ),
        XpDailyMission(
            title = "Crescer e refletir",
            description = "Conclua um devocional, plano, livro ou aula IBR",
            current = daily.growth,
            target = daily.growthTarget
        ),
        XpDailyMission(
            title = "Conhecimento bíblico",
            description = "Acerte 3 perguntas diferentes do Quiz",
            current = daily.quiz,
            target = daily.quizTarget
        ),
        XpDailyMission(
            title = "Constância",
            description = "Complete 10 minutos ativos no MIC Rhema",
            current = daily.activeBlocks,
            target = daily.activeBlocksTarget
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(9.dp).size(22.dp)
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Jornada XP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (xpUnlocked) "Sua constância também se transforma em recompensas"
                        else "Quiz gera XP Extra agora · ecossistema e Loja no Nível 8",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!xpUnlocked) {
                    Icon(Icons.Default.Lock, contentDescription = "Bloqueado até o nível 8", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                XpNumberCard(
                    modifier = Modifier.weight(1f),
                    value = account?.totalEarned ?: BibleJourneyProgressTracker.totalXp(member),
                    label = "XP Total"
                )
                XpNumberCard(
                    modifier = Modifier.weight(1f),
                    value = account?.balance ?: BibleJourneyProgressTracker.totalXp(member),
                    label = if (xpUnlocked) "Saldo XP" else "Saldo futuro"
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(7.dp))
                Text("Sequência: $streak ${if (streak == 1) "dia" else "dias"}", fontWeight = FontWeight.SemiBold)
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                divider = {}
            ) {
                listOf("Missões", "Loja XP", "Histórico").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(label) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    if (xpUnlocked) {
                        Text("Missões de hoje", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        dailyMissions.forEach { mission ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (mission.completed) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.size(7.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(mission.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(mission.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("${mission.current.coerceAtMost(mission.target)}/${mission.target}", style = MaterialTheme.typography.labelMedium)
                                }
                                LinearProgressIndicator(progress = { mission.progress }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        if (daily.complete) {
                            Text(
                                if (daily.bonusGranted) "✓ Jornada de hoje concluída · bônus entregue"
                                else "Jornada de hoje concluída · sincronizando +10 XP",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text("Complete as 4 missões para receber +10 XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(
                            "As missões diárias completas serão liberadas no Nível 8. Continue avançando pela Jornada e pelo Quiz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                1 -> XpShopPanel(member = member, xpUnlocked = xpUnlocked)

                2 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(7.dp))
                        Text("Histórico XP", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    if (history.isEmpty()) {
                        Text("Ainda não há movimentações de XP registradas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        history.take(30).forEach { transaction ->
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(transaction.description.ifBlank { xpActivityLabel(transaction.activity) }, style = MaterialTheme.typography.bodyMedium)
                                    Text(transaction.dateKey, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = if (transaction.type == "spend") "-${transaction.amount} XP" else "+${transaction.amount} XP",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XpNumberCard(modifier: Modifier = Modifier, value: Int, label: String) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("$value XP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun xpActivityLabel(activity: String): String = when (activity) {
    "bible_chapter" -> "Capítulo bíblico concluído"
    "devotional" -> "Devocional concluído"
    "news_read" -> "Notícia lida"
    "plan_theme", "plan_day", "plan_complete" -> "Plano bíblico"
    "book_10", "book_complete" -> "Leitura de livro"
    "audio_10min", "audio_90" -> "Áudio"
    "video_10min", "video_90" -> "Vídeo"
    "ibr_lesson" -> "Aula IBR"
    "prayer_sent" -> "Pedido de oração"
    "quiz_easy", "quiz_medium", "quiz_hard" -> "Quiz Bíblico"
    "daily_mission" -> "Jornada diária"
    "streak_7", "streak_30" -> "Bônus de sequência"
    "shop_redeem" -> "Resgate na Loja XP"
    "shop_refund" -> "Estorno da Loja XP"
    else -> "Atividade na Jornada"
}
