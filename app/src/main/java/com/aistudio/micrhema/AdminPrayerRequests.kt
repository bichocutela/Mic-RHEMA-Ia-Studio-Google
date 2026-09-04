package com.aistudio.micrhema

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdminPrayerRequestsScreen(focusedRequestId: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf<PrayerRequest?>(null) }
    var processingId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { PrayerRepository.startAdminListener() }

    val pending = prayerRequestsState
        .filter { it.status != "respondida" && it.answeredAt <= 0L }
        .sortedWith(compareByDescending<PrayerRequest> { it.id == focusedRequestId }.thenByDescending { it.createdAt })
    val answered = prayerRequestsState
        .filter { it.status == "respondida" || it.answeredAt > 0L }
        .sortedByDescending { it.answeredAt }
        .take(30)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Oração Pendente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(if (pending.size == 1) "1 pedido aguarda oração" else "${pending.size} pedidos aguardam oração")
                    }
                    Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.surface) {
                        Text(pending.size.toString(), modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (prayerAdminSyncErrorState.value.isNotBlank()) {
            item { Text(prayerAdminSyncErrorState.value, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }

        if (pending.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Nenhuma oração pendente", fontWeight = FontWeight.Bold)
                            Text("Todos os pedidos recebidos foram marcados como oração feita.")
                        }
                    }
                }
            }
        } else {
            item { Text("Pedidos aguardando oração", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(pending, key = { it.id }) { item ->
                val focused = item.id == focusedRequestId
                Card(
                    modifier = Modifier.fillMaxWidth().then(
                        if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)) else Modifier
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        if (focused) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ABERTO PELA NOTIFICAÇÃO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name.ifBlank { "Pedido sem nome" }, fontWeight = FontWeight.Bold)
                                Text(item.date.ifBlank { "Data não informada" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)) {
                                Row(modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pendente", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(item.request, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { confirming = item }, enabled = processingId != item.id, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                            if (processingId == item.id) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (processingId == item.id) "Confirmando..." else "Oração Feita", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (answered.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Histórico de orações respondidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(answered, key = { "answered_${it.id}" }) { item ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.name.ifBlank { "Pedido sem nome" }, fontWeight = FontWeight.Bold)
                            Text(item.answeredDate.ifBlank { item.date }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(item.request, style = MaterialTheme.typography.bodySmall)
                        Text("✓ Oração respondida", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    confirming?.let { item ->
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text("Confirmar oração feita?") },
            text = { Text("Ao confirmar, ${item.name.ifBlank { "a pessoa" }} verá “Oração respondida” no histórico e receberá uma notificação neste aparelho.") },
            confirmButton = {
                Button(onClick = {
                    confirming = null
                    processingId = item.id
                    scope.launch {
                        runCatching { PrayerRepository.markAsPrayed(item) }
                            .onSuccess { Toast.makeText(context, "Oração marcada como respondida.", Toast.LENGTH_SHORT).show() }
                            .onFailure { error -> Toast.makeText(context, error.localizedMessage ?: "Não foi possível confirmar a oração.", Toast.LENGTH_LONG).show() }
                        processingId = null
                    }
                }) { Text("Sim, oração feita") }
            },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Cancelar") } }
        )
    }
}
