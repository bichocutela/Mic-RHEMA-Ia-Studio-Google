package com.aistudio.micrhema

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val member = loggedInMemberState.value
    var name by remember { mutableStateOf(member?.name.orEmpty()) }
    var request by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(member?.id) {
        if (name.isBlank()) name = member?.name.orEmpty()
        PrayerRepository.startUserListener(context)
    }
    DisposableEffect(Unit) {
        onDispose { PrayerRepository.stopUserListener() }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column {
                Text("Pedidos de Oração", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Envie seu pedido com tranquilidade. A equipe pastoral acompanha cada pedido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF143454))
            ) {
                Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🙏", fontSize = 34.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "\"Orai uns pelos outros, para que sejais curados. A oração do justo tem grande poder.\"",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFFFDE68A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Tiago 5:16", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Enviar novo pedido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(80) },
                        label = { Text("Seu nome") },
                        placeholder = { Text("Como podemos te chamar?") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = request,
                        onValueChange = { request = it.take(1200) },
                        label = { Text("Pedido de oração") },
                        placeholder = { Text("Descreva seu pedido de oração...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        supportingText = { Text("${request.length}/1200") }
                    )
                    Button(
                        onClick = {
                            if (name.isBlank() || request.isBlank()) {
                                Toast.makeText(context, "Preencha seu nome e o pedido de oração.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isLoading = true
                            scope.launch {
                                runCatching { PrayerRepository.submit(context, name, request) }
                                    .onSuccess {
                                        request = ""
                                        Toast.makeText(context, "Pedido enviado. A equipe pastoral foi avisada.", Toast.LENGTH_LONG).show()
                                    }
                                    .onFailure { error ->
                                        Toast.makeText(context, error.localizedMessage ?: "Não foi possível enviar o pedido.", Toast.LENGTH_LONG).show()
                                    }
                                isLoading = false
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3C344), contentColor = Color.Black)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                            Spacer(Modifier.width(10.dp))
                        }
                        Text(if (isLoading) "Enviando..." else "🙏 Enviar Pedido", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("Histórico dos meus pedidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Aqui você acompanha quando a equipe pastoral marcar a oração como respondida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (prayerUserSyncErrorState.value.isNotBlank()) {
                Text(prayerUserSyncErrorState.value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            if (userPrayerRequestsState.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Text("Nenhum pedido enviado por este aparelho ainda.", modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                userPrayerRequestsState.forEach { item ->
                    val answered = item.status == "respondida" || item.answeredAt > 0L
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (answered) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(item.date.ifBlank { "Pedido enviado" }, style = MaterialTheme.typography.labelLarge)
                                Surface(
                                    shape = RoundedCornerShape(99.dp),
                                    color = if (answered) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (answered) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(if (answered) "Oração respondida" else "Aguardando oração", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Text(item.request, style = MaterialTheme.typography.bodyMedium)
                            if (answered) {
                                HorizontalDivider()
                                Text(
                                    item.responseMessage.ifBlank {
                                        "Oração respondida — a equipe pastoral orou por este pedido${item.answeredDate.takeIf { it.isNotBlank() }?.let { " em $it" } ?: ""}."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Text(
                    "Seus pedidos são tratados com cuidado. O histórico desta tela mostra somente os pedidos associados à sessão deste aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(15.dp)
                )
            }
        }
    }
}
