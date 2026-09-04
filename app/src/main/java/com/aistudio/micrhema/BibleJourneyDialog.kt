package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun BibleJourneyDialog(
    member: MemberRequest,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var difficulty by remember { mutableStateOf(BibleQuizDifficulty.EASY) }
    var questionIndex by remember { mutableIntStateOf(0) }
    var hintUsed by remember { mutableStateOf(BibleQuizHintUsage.NONE) }
    var selectedOption by remember { mutableIntStateOf(-1) }
    var submission by remember { mutableStateOf<BibleQuizSubmission?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var showMissions by remember { mutableStateOf(true) }

    val liveMember = loggedInMemberState.value?.takeIf { it.id == member.id } ?: member
    val stats = BibleJourneyProgressTracker.stats(liveMember)
    val badgeProgress = calculateBadgeProgress(liveMember)
    val missionProgress = calculateBibleMissionProgress(liveMember)
    val questions = remember(difficulty) {
        BibleQuizCatalog.questions.filter { it.difficulty == difficulty }
    }
    val question = questions.getOrNull(questionIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0)))

    LaunchedEffect(difficulty, liveMember.id) {
        val answered = liveMember.badgeActivityIds[BadgeActivityKeys.QUIZ_ANSWERED].orEmpty().toSet()
        val firstPending = questions.indexOfFirst { it.id !in answered }
        questionIndex = if (firstPending >= 0) firstPending else 0
        selectedOption = -1
        submission = null
        hintUsed = BibleQuizHintUsage.NONE
        errorMessage = ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 760.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Jornada Bíblica", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Quiz, XP, missões e Emblemas do Perfil", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onDismiss) { Text("Fechar") }
                }

                JourneySummaryCard(stats, badgeProgress)

                Text("Dificuldade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BibleQuizDifficulty.entries.forEach { item ->
                        val selected = item == difficulty
                        if (selected) {
                            Button(onClick = { difficulty = item }, contentPadding = ButtonDefaults.ContentPadding) {
                                Text("${item.label} · ${item.baseXp} XP")
                            }
                        } else {
                            OutlinedButton(onClick = { difficulty = item }, contentPadding = ButtonDefaults.ContentPadding) {
                                Text(item.label)
                            }
                        }
                    }
                }

                if (question == null) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text("Não há perguntas disponíveis nesta dificuldade.", modifier = Modifier.padding(16.dp))
                    }
                } else {
                    QuizQuestionCard(
                        question = question,
                        hintUsed = hintUsed,
                        selectedOption = selectedOption,
                        submission = submission,
                        onHardHint = {
                            if (submission == null) hintUsed = BibleQuizHintUsage.HARD
                        },
                        onEasyHint = {
                            if (submission == null) hintUsed = BibleQuizHintUsage.EASY
                        },
                        onAnswer = { index ->
                            if (submission != null) return@QuizQuestionCard
                            selectedOption = index
                            errorMessage = ""
                            submission = runCatching {
                                BibleJourneyProgressTracker.submitQuizAnswer(context, question, index, hintUsed)
                            }.getOrElse {
                                errorMessage = it.message ?: "Não foi possível registrar esta resposta."
                                null
                            }
                        }
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    if (submission != null) {
                        Button(
                            onClick = {
                                questionIndex = if (questions.isEmpty()) 0 else (questionIndex + 1) % questions.size
                                hintUsed = BibleQuizHintUsage.NONE
                                selectedOption = -1
                                submission = null
                                errorMessage = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Próxima pergunta") }
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showMissions = !showMissions }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Missões da Jornada", fontWeight = FontWeight.Bold)
                        Text(
                            "${stats.completedMissionIds.size} concluídas · XP entregue uma única vez",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(if (showMissions) "Ocultar" else "Mostrar", color = MaterialTheme.colorScheme.primary)
                }

                if (showMissions) {
                    val ordered = missionProgress.sortedWith(compareBy<BibleMissionProgress> { it.completed }.thenBy { it.mission.difficulty.ordinal })
                    ordered.take(10).forEach { MissionJourneyCard(it) }
                    if (ordered.size > 10) {
                        Text(
                            "+ ${ordered.size - 10} missões continuam sendo acompanhadas automaticamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "Regra de progresso: a primeira tentativa de cada pergunta é a única que pode gerar XP. Repetições ficam liberadas para estudo, mas valem 0 XP. Cada nível novo também começa suas próprias missões do zero.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun JourneySummaryCard(stats: BibleJourneyStats, badgeProgress: BadgeProgressSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${stats.totalXp} XP", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Experiência acumulada", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${stats.accuracyPercent}%", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Precisão", style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${stats.correctAnswers}/${stats.answeredQuestions} acertos")
                Text("${stats.hardCorrectAnswers} difíceis")
            }
            badgeProgress.nextLevel?.let { next ->
                Text("Próximo emblema: Nível ${next.level} · ${next.name}", fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(progress = { badgeProgress.progressToNextLevel }, modifier = Modifier.fillMaxWidth())
                Text(next.requirement, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun QuizQuestionCard(
    question: BibleQuizQuestion,
    hintUsed: BibleQuizHintUsage,
    selectedOption: Int,
    submission: BibleQuizSubmission?,
    onHardHint: () -> Unit,
    onEasyHint: () -> Unit,
    onAnswer: (Int) -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text("${question.difficulty.label} · até ${question.difficulty.baseXp} XP", fontWeight = FontWeight.Bold)
            }
            Text(question.prompt, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            if (submission == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onHardHint, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Dica sutil")
                    }
                    OutlinedButton(onClick = onEasyHint, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Dica direta")
                    }
                }
                if (hintUsed != BibleQuizHintUsage.NONE) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                        Column(Modifier.padding(12.dp)) {
                            Text(hintUsed.label, fontWeight = FontWeight.Bold)
                            Text(BibleQuizEngine.hintText(question, hintUsed))
                            Text(
                                if (hintUsed == BibleQuizHintUsage.EASY) "Acerto com esta dica vale 70% do XP base." else "Acerto com esta dica vale 90% do XP base.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            question.options.forEachIndexed { index, option ->
                val answered = submission != null
                val correctIndex = submission?.result?.correctOptionIndex
                val selected = selectedOption == index
                val container = when {
                    answered && index == correctIndex -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    answered && selected && index != correctIndex -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)
                    selected -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                    else -> MaterialTheme.colorScheme.surface
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !answered) { onAnswer(index) },
                    color = container,
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 1.dp
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (answered && index == correctIndex) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (answered && index == correctIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.size(10.dp))
                        Text("${('A'.code + index).toChar()}. $option", modifier = Modifier.weight(1f))
                    }
                }
            }

            submission?.let { value ->
                val result = value.result
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isCorrect) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (result.isCorrect) "Resposta correta" else "Resposta incorreta", fontWeight = FontWeight.Bold)
                        Text(result.explanation)
                        Text("Referência: ${result.bibleReference}", fontWeight = FontWeight.SemiBold)
                        if (value.firstAttempt) {
                            Text(if (value.xpGranted > 0) "+${value.xpGranted} XP · Total ${value.totalXp} XP" else "Primeira tentativa registrada · 0 XP")
                        } else {
                            Text("Questão repetida para estudo · 0 XP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionJourneyCard(progress: BibleMissionProgress) {
    val color = if (progress.completed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    Card(colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (progress.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(progress.mission.title, fontWeight = FontWeight.SemiBold)
                    Text("${progress.mission.difficulty.label} · ${progress.mission.xpReward} XP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(progress.mission.description, style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
            progress.objectives.forEach { objective ->
                Text(
                    "${objective.objective.label}: ${objective.current}/${objective.objective.target} ${objective.objective.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (objective.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
