package com.aistudio.micrhema

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private fun formatXpNumber(value: Int): String =
    NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(value)

private fun formatXpHistoryDate(value: String): String {
    if (value.isBlank()) return ""
    return value.replace('T', ' ').substringBefore('.').removeSuffix("Z").take(16)
}

@Composable
fun MemberXpAdminSection(member: MemberRequest) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember(member.id) { mutableStateOf<AdminMemberXpSnapshot?>(null) }
    var isLoading by remember(member.id) { mutableStateOf(false) }
    var errorMessage by remember(member.id) { mutableStateOf("") }
    var showDialog by remember(member.id) { mutableStateOf(false) }

    fun refresh() {
        if (isLoading) return
        isLoading = true
        errorMessage = ""
        scope.launch {
            runCatching { MemberXpAdminClient.load(member.id) }
                .onSuccess { snapshot = it }
                .onFailure { errorMessage = it.message ?: "Não foi possível carregar o XP." }
            isLoading = false
        }
    }

    LaunchedEffect(member.id) { refresh() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                showDialog = true
                refresh()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "XP do usuário",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                when {
                    snapshot != null -> {
                        Text(
                            "${formatXpNumber(snapshot!!.totalEarned)} XP",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Saldo: ${formatXpNumber(snapshot!!.balance)} XP · Toque para ver histórico",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    isLoading -> Text("Carregando XP…", style = MaterialTheme.typography.bodyMedium)
                    else -> Text(
                        errorMessage.ifBlank { "Toque para consultar" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Default.History, contentDescription = "Abrir histórico de XP")
            }
        }
    }

    if (showDialog) {
        MemberXpAdminDialog(
            member = member,
            snapshot = snapshot,
            isLoading = isLoading,
            errorMessage = errorMessage,
            onRefresh = ::refresh,
            onSnapshotChanged = { snapshot = it },
            onDismiss = { showDialog = false },
            onMessage = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
        )
    }
}

@Composable
private fun MemberXpAdminDialog(
    member: MemberRequest,
    snapshot: AdminMemberXpSnapshot?,
    isLoading: Boolean,
    errorMessage: String,
    onRefresh: () -> Unit,
    onSnapshotChanged: (AdminMemberXpSnapshot) -> Unit,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var showExtraInput by remember(member.id) { mutableStateOf(false) }
    var extraXp by remember(member.id) { mutableStateOf("") }
    var isCrediting by remember(member.id) { mutableStateOf(false) }
    var creditError by remember(member.id) { mutableStateOf("") }
    var showBadgePicker by remember(member.id) { mutableStateOf(false) }
    var isUnlockingBadge by remember(member.id) { mutableStateOf(false) }
    var unlockedBadgeIds by remember(member.id) { mutableStateOf(member.unlockedBadgeIds.toSet()) }

    val isBusy = isCrediting || isUnlockingBadge

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("XP de ${member.name.ifBlank { "usuário" }}") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (snapshot != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("XP Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${formatXpNumber(snapshot.totalEarned)} XP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Saldo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${formatXpNumber(snapshot.balance)} XP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Histórico de XP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(
                        enabled = !isBusy,
                        onClick = {
                            showExtraInput = !showExtraInput
                            creditError = ""
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("XP Extra")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        enabled = !isBusy,
                        onClick = { showBadgePicker = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Liberar emblema")
                    }
                }

                if (showExtraInput) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("Adicionar XP Extra", fontWeight = FontWeight.SemiBold)
                            Text(
                                "O valor é creditado diretamente no saldo central do usuário, sem limite diário.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = extraXp,
                                onValueChange = { value -> extraXp = value.filter(Char::isDigit).take(10) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Quantidade de XP") },
                                placeholder = { Text("Ex.: 500") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                enabled = !isBusy
                            )
                            if (creditError.isNotBlank()) {
                                Text(
                                    creditError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isBusy && (extraXp.toLongOrNull() ?: 0L) in 1L..Int.MAX_VALUE.toLong(),
                                onClick = {
                                    val amount = extraXp.toIntOrNull() ?: return@Button
                                    isCrediting = true
                                    creditError = ""
                                    scope.launch {
                                        runCatching { MemberXpAdminClient.addExtra(member.id, amount) }
                                            .onSuccess {
                                                onSnapshotChanged(it)
                                                extraXp = ""
                                                showExtraInput = false
                                                onMessage("+$amount XP creditados para ${member.name}.")
                                            }
                                            .onFailure { creditError = it.message ?: "Não foi possível adicionar XP." }
                                        isCrediting = false
                                    }
                                }
                            ) {
                                if (isCrediting) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (isCrediting) "Creditando…" else "Creditar XP")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                when {
                    isLoading && snapshot == null -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                    snapshot == null -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                            Text(errorMessage.ifBlank { "Não foi possível carregar o histórico." }, textAlign = TextAlign.Center)
                            TextButton(onClick = onRefresh) { Text("Tentar novamente") }
                        }
                    }
                    snapshot.transactions.isEmpty() -> {
                        Text(
                            "Este usuário ainda não possui movimentações de XP.",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(snapshot.transactions, key = { it.id }) { item ->
                                val isSpend = item.type == "spend"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.description.ifBlank {
                                                if (item.activity == "admin_extra") "XP Extra concedido pelo ADM" else item.activity.replace('_', ' ')
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            formatXpHistoryDate(item.createdAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "${if (isSpend) "−" else "+"}${formatXpNumber(item.amount)} XP",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSpend) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !isBusy, onClick = onDismiss) { Text("Fechar") }
        }
    )

    if (showBadgePicker) {
        AdminBadgeUnlockDialog(
            member = member,
            unlockedBadgeIds = unlockedBadgeIds,
            isSaving = isUnlockingBadge,
            onDismiss = { if (!isUnlockingBadge) showBadgePicker = false },
            onConfirm = { selected ->
                if (selected.isEmpty()) return@AdminBadgeUnlockDialog
                isUnlockingBadge = true
                scope.launch {
                    runCatching { MemberXpAdminClient.unlockBadges(member, selected) }
                        .onSuccess { updated ->
                            unlockedBadgeIds = updated.toSet()
                            showBadgePicker = false
                            val names = profileEmblemBadges
                                .filter { it.id in selected }
                                .joinToString { it.name }
                            onMessage(
                                if (selected.size == 1) "Emblema $names liberado para ${member.name}."
                                else "${selected.size} emblemas liberados para ${member.name}."
                            )
                        }
                        .onFailure { onMessage(it.message ?: "Não foi possível liberar o emblema.") }
                    isUnlockingBadge = false
                }
            }
        )
    }
}

@Composable
private fun AdminBadgeUnlockDialog(
    member: MemberRequest,
    unlockedBadgeIds: Set<String>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selectedIds by remember(member.id, unlockedBadgeIds) { mutableStateOf(emptySet<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Liberar emblema") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Escolha um ou mais emblemas para desbloquear manualmente para ${member.name.ifBlank { "este usuário" }}.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(profileEmblemBadges, key = { it.id }) { badge ->
                        val alreadyUnlocked = badge.id in unlockedBadgeIds
                        val selected = badge.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !alreadyUnlocked && !isSaving) {
                                    selectedIds = if (selected) selectedIds - badge.id else selectedIds + badge.id
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = alreadyUnlocked || selected,
                                onCheckedChange = if (alreadyUnlocked || isSaving) null else { checked ->
                                    selectedIds = if (checked) selectedIds + badge.id else selectedIds - badge.id
                                },
                                enabled = !alreadyUnlocked && !isSaving
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Nível ${badge.level} · ${badge.name}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (alreadyUnlocked) "Já desbloqueado" else badge.rarity?.label.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (alreadyUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancelar") }
        },
        confirmButton = {
            Button(
                enabled = selectedIds.isNotEmpty() && !isSaving,
                onClick = { onConfirm(selectedIds) }
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isSaving) "Liberando…" else "Liberar selecionados")
            }
        }
    )
}
