from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path: str, old: str, new: str):
    p = ROOT / path
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected exactly one match, found {count}")
    p.write_text(text.replace(old, new, 1))


def append_once(path: str, marker: str, content: str):
    p = ROOT / path
    text = p.read_text()
    if marker in text:
        return
    p.write_text(text.rstrip() + "\n\n" + content.strip() + "\n")


replace_once(
    "app/src/main/java/com/aistudio/micrhema/Data.kt",
    '''data class PrayerRequest(
    var id: String = "",
    var name: String = "",
    var request: String = "",
    var date: String = ""
)''',
    '''data class PrayerRequest(
    var id: String = "",
    var name: String = "",
    var request: String = "",
    var date: String = "",
    var createdAt: Long = 0L,
    var requesterUid: String = "",
    var requesterMemberId: String = "",
    var requesterFcmToken: String = "",
    var status: String = "pendente",
    var answeredAt: Long = 0L,
    var answeredDate: String = "",
    var responseMessage: String = "",
    var answeredBy: String = ""
)'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/Data.kt",
    '''val prayerRequestsState = mutableStateListOf<PrayerRequest>(
    PrayerRequest(
        id = "1",
        name = "Maria Souza",
        request = "Pela saúde da minha família e restauração do meu casamento.",
        date = "2026-07-13"
    ),
    PrayerRequest(
        id = "2",
        name = "João Silva",
        request = "Agradecimento pela porta de emprego aberta e oração para que tudo corra bem no novo trabalho.",
        date = "2026-07-12"
    )
)''',
    '''val prayerRequestsState = mutableStateListOf<PrayerRequest>()'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/Data.kt",
    '''        db.collection("prayer_requests").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(PrayerRequest::class.java) } catch(ex: Exception) { null } }
            prayerRequestsState.clear()
                    prayerRequestsState.addAll(list)
        }
        ''',
    '''        // Pedidos de oração são carregados por PrayerRepository:
        // o usuário lê somente os próprios pedidos e o ADM lê a fila completa.
        '''
)

(ROOT / "app/src/main/java/com/aistudio/micrhema/PrayerRepository.kt").write_text(r'''package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

val userPrayerRequestsState = mutableStateListOf<PrayerRequest>()
val prayerUserSyncErrorState = mutableStateOf("")
val prayerAdminSyncErrorState = mutableStateOf("")

object PrayerRepository {
    private var userListener: ListenerRegistration? = null
    private var adminListener: ListenerRegistration? = null

    private fun formatDate(timestamp: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(timestamp))

    private suspend fun ensureFirebaseUser(): FirebaseUser {
        val auth = FirebaseAuth.getInstance()
        return auth.currentUser
            ?: auth.signInAnonymously().await().user
            ?: throw IllegalStateException("Não foi possível preparar a sessão segura para o pedido de oração.")
    }

    suspend fun submit(
        context: Context,
        name: String,
        requestText: String
    ): PrayerRequest {
        val firebaseUser = ensureFirebaseUser()
        val member = loggedInMemberState.value
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrDefault("")
        val now = System.currentTimeMillis()
        val item = PrayerRequest(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            request = requestText.trim(),
            date = formatDate(now),
            createdAt = now,
            requesterUid = firebaseUser.uid,
            requesterMemberId = member?.id.orEmpty(),
            requesterFcmToken = token,
            status = "pendente"
        )

        FirebaseFirestore.getInstance()
            .collection("prayer_requests")
            .document(item.id)
            .set(item)
            .await()

        NotificationDispatcher.enqueue(
            topic = "prayer_admins",
            title = "Novo pedido de oração",
            body = "Há um novo pedido aguardando a equipe pastoral.",
            collection = "prayer_requests",
            documentId = item.id
        )

        if (userPrayerRequestsState.none { it.id == item.id }) userPrayerRequestsState.add(0, item)
        return item
    }

    fun startUserListener(context: Context) {
        userListener?.remove()
        userListener = null
        prayerUserSyncErrorState.value = ""

        val attach: (FirebaseUser) -> Unit = { user ->
            userListener = FirebaseFirestore.getInstance()
                .collection("prayer_requests")
                .whereEqualTo("requesterUid", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        prayerUserSyncErrorState.value = error.localizedMessage ?: "Não foi possível atualizar seu histórico agora."
                        Log.w("PrayerRepository", "Falha no histórico do usuário", error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents.orEmpty()
                        .mapNotNull { document ->
                            runCatching { document.toObject(PrayerRequest::class.java) }
                                .getOrNull()
                                ?.also { if (it.id.isBlank()) it.id = document.id }
                        }
                        .sortedWith(compareByDescending<PrayerRequest> { it.createdAt }.thenByDescending { it.id })
                    userPrayerRequestsState.clear()
                    userPrayerRequestsState.addAll(list)
                }
        }

        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let(attach) ?: auth.signInAnonymously()
            .addOnSuccessListener { result ->
                result.user?.let(attach)
                    ?: run { prayerUserSyncErrorState.value = "Não foi possível identificar este aparelho." }
            }
            .addOnFailureListener { error ->
                prayerUserSyncErrorState.value = error.localizedMessage ?: "Não foi possível sincronizar seus pedidos."
                Log.w("PrayerRepository", "Falha ao preparar sessão anônima", error)
            }
    }

    fun stopUserListener() {
        userListener?.remove()
        userListener = null
    }

    fun startAdminListener() {
        adminListener?.remove()
        adminListener = null
        prayerAdminSyncErrorState.value = ""

        adminListener = FirebaseFirestore.getInstance()
            .collection("prayer_requests")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    prayerAdminSyncErrorState.value = error.localizedMessage ?: "Não foi possível carregar os pedidos."
                    Log.w("PrayerRepository", "Falha na fila pastoral", error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        runCatching { document.toObject(PrayerRequest::class.java) }
                            .getOrNull()
                            ?.also { if (it.id.isBlank()) it.id = document.id }
                    }
                    .sortedWith(compareByDescending<PrayerRequest> { it.createdAt }.thenByDescending { it.id })
                prayerRequestsState.clear()
                prayerRequestsState.addAll(list)
            }
    }

    fun stopAdminListener() {
        adminListener?.remove()
        adminListener = null
    }

    suspend fun markAsPrayed(item: PrayerRequest): PrayerRequest {
        val now = System.currentTimeMillis()
        val answeredDate = formatDate(now)
        val response = "Oração respondida — a equipe pastoral orou por este pedido em $answeredDate."
        val updates = mapOf(
            "status" to "respondida",
            "answeredAt" to now,
            "answeredDate" to answeredDate,
            "responseMessage" to response,
            "answeredBy" to "Equipe Pastoral"
        )

        FirebaseFirestore.getInstance()
            .collection("prayer_requests")
            .document(item.id)
            .update(updates)
            .await()

        val updated = item.copy(
            status = "respondida",
            answeredAt = now,
            answeredDate = answeredDate,
            responseMessage = response,
            answeredBy = "Equipe Pastoral"
        )

        prayerRequestsState.indexOfFirst { it.id == item.id }
            .takeIf { it >= 0 }
            ?.let { prayerRequestsState[it] = updated }

        if (item.requesterFcmToken.isNotBlank()) {
            NotificationDispatcher.enqueueToken(
                token = item.requesterFcmToken,
                title = "🙏 Oração respondida",
                body = "Seu pedido de oração foi atendido em $answeredDate. A equipe pastoral orou por você.",
                collection = "prayer_response",
                documentId = item.id,
                destination = Screen.Prayer.route
            )
        }
        return updated
    }
}
''')

(ROOT / "app/src/main/java/com/aistudio/micrhema/PrayerScreen.kt").write_text(r'''package com.aistudio.micrhema

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
''')

(ROOT / "app/src/main/java/com/aistudio/micrhema/AdminPrayerRequests.kt").write_text(r'''package com.aistudio.micrhema

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
''')

replace_once(
    "app/src/main/java/com/aistudio/micrhema/AdminDashboard.kt",
    '''    SERVICES, BANNERS, DONATIONS,
    MEMBERS, PROFILES, TEAM,''',
    '''    SERVICES, BANNERS, DONATIONS, PRAYERS,
    MEMBERS, PROFILES, TEAM,'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/AdminDashboard.kt",
    '''        MemberManager.syncFromFirestore(context)
    }''',
    '''        MemberManager.syncFromFirestore(context)
        PrayerRepository.startAdminListener()
    }'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/AdminDashboard.kt",
    '''    val mediaCount = contentBooksState.size + contentAudiosState.size + contentVideosState.size + contentAlbumsState.size
    var contentExpanded''',
    '''    val mediaCount = contentBooksState.size + contentAudiosState.size + contentVideosState.size + contentAlbumsState.size
    val pendingPrayerCount = prayerRequestsState.count { it.status != "respondida" && it.answeredAt <= 0L }
    var contentExpanded'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/AdminDashboard.kt",
    '''        item {
            AdminSectionHeading(
                title = "Resumo do ministério",
                subtitle = "Acompanhe o que precisa da sua atenção hoje."
            )
        }''',
    '''        if (pendingPrayerCount > 0) {
            item {
                AdminPrayerPriorityCard(count = pendingPrayerCount, onClick = { onNavigate(AdminSection.PRAYERS) })
            }
        }

        item {
            AdminSectionHeading(
                title = "Resumo do ministério",
                subtitle = "Acompanhe o que precisa da sua atenção hoje."
            )
        }'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/AdminDashboard.kt",
    '''            item { AdminMenuItem("Dízimos e Ofertas", "Contas, PIX e informações", Icons.Default.MonetizationOn, { onNavigate(AdminSection.DONATIONS) }) }
            item { AdminMenuItem("Equipe", "Líderes, pastores e ministérios", Icons.Default.Groups, { onNavigate(AdminSection.TEAM) }) }''',
    '''            item { AdminMenuItem("Dízimos e Ofertas", "Contas, PIX e informações", Icons.Default.MonetizationOn, { onNavigate(AdminSection.DONATIONS) }) }
            item { AdminMenuItem("Pedidos de Oração", "Fila pastoral e histórico de orações", Icons.Default.VolunteerActivism, { onNavigate(AdminSection.PRAYERS) }) }
            item { AdminMenuItem("Equipe", "Líderes, pastores e ministérios", Icons.Default.Groups, { onNavigate(AdminSection.TEAM) }) }'''
)
append_once(
    "app/src/main/java/com/aistudio/micrhema/AdminDashboard.kt",
    "fun AdminPrayerPriorityCard(",
    r'''@Composable
fun AdminPrayerPriorityCard(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(modifier = Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Oração Pendente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    if (count == 1) "1 novo pedido aguarda oração" else "$count pedidos aguardam oração",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.82f)
                )
            }
            Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.surface) {
                Text(count.toString(), modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir pedidos de oração", tint = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/Screens.kt",
    '''            .addOnSuccessListener { MemberManager.syncFromFirestore(context) }''',
    '''            .addOnSuccessListener {
                MemberManager.syncFromFirestore(context)
                PrayerRepository.startAdminListener()
            }'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/Screens.kt",
    '''    val adminUiPrefs = remember {
        context.getSharedPreferences("micrhema_admin_ui", android.content.Context.MODE_PRIVATE)
    }''',
    '''    LaunchedEffect(isAuthenticated) {
        runCatching {
            val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            if (isAuthenticated) messaging.subscribeToTopic("prayer_admins")
            else messaging.unsubscribeFromTopic("prayer_admins")
        }.onFailure {
            android.util.Log.w("AdminScreen", "Não foi possível atualizar o tópico de oração do ADM", it)
        }
    }

    val adminUiPrefs = remember {
        context.getSharedPreferences("micrhema_admin_ui", android.content.Context.MODE_PRIVATE)
    }'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/Screens.kt",
    '''                            IconButton(onClick = {
                                isAuthenticated = false
                            }) {''',
    '''                            IconButton(onClick = {
                                isAuthenticated = false
                                adminPrayerTargetState.value = null
                                runCatching {
                                    com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("prayer_admins")
                                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                }
                            }) {'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/Screens.kt",
    '''            var currentSection by remember { mutableStateOf(AdminSection.DASHBOARD) }

            BackHandler(enabled = currentSection != AdminSection.DASHBOARD) {
                currentSection = AdminSection.DASHBOARD
            }''',
    '''            var currentSection by remember {
                mutableStateOf(if (!adminPrayerTargetState.value.isNullOrBlank()) AdminSection.PRAYERS else AdminSection.DASHBOARD)
            }

            LaunchedEffect(adminPrayerTargetState.value) {
                if (!adminPrayerTargetState.value.isNullOrBlank()) currentSection = AdminSection.PRAYERS
            }

            BackHandler(enabled = currentSection != AdminSection.DASHBOARD) {
                if (currentSection == AdminSection.PRAYERS) adminPrayerTargetState.value = null
                currentSection = AdminSection.DASHBOARD
            }'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/Screens.kt",
    '''                    AdminSection.DONATIONS -> "Dízimos e Ofertas"
                    AdminSection.TEAM -> "Equipe"''',
    '''                    AdminSection.DONATIONS -> "Dízimos e Ofertas"
                    AdminSection.PRAYERS -> "Pedidos de Oração"
                    AdminSection.TEAM -> "Equipe"'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/Screens.kt",
    '''                            .fillMaxWidth()
                            .clickable { currentSection = AdminSection.DASHBOARD }
                            .padding(16.dp)''',
    '''                            .fillMaxWidth()
                            .clickable {
                                if (currentSection == AdminSection.PRAYERS) adminPrayerTargetState.value = null
                                currentSection = AdminSection.DASHBOARD
                            }
                            .padding(16.dp)'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/Screens.kt",
    '''                            AdminSection.DONATIONS -> EditDonationsSection()
                            AdminSection.PROFILES -> EditProfilesSection()
                            else -> {}''',
    '''                            AdminSection.DONATIONS -> EditDonationsSection()
                            AdminSection.PROFILES -> EditProfilesSection()
                            AdminSection.PRAYERS -> AdminPrayerRequestsScreen(adminPrayerTargetState.value)
                            else -> {}'''
)

(ROOT / "app/src/main/java/com/aistudio/micrhema/NotificationDispatcher.kt").write_text(r'''package com.aistudio.micrhema

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NotificationDispatcher {
    private const val FUNCTION_NAME = "notify-fcm"
    private val scheduledOnlyCollections = setOf("devocionais", "cultos_agenda", "bible_news")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    fun enqueue(topic: String, title: String, body: String, collection: String, documentId: String) {
        dispatch(topic, null, title, body, collection, documentId, null)
    }

    fun enqueueToken(token: String, title: String, body: String, collection: String, documentId: String, destination: String) {
        if (token.isBlank()) return
        dispatch(null, token, title, body, collection, documentId, destination)
    }

    private fun dispatch(
        topic: String?,
        deviceToken: String?,
        title: String,
        body: String,
        collection: String,
        documentId: String,
        requestedDestination: String?
    ) {
        if (deviceToken == null && collection in scheduledOnlyCollections) return
        val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        if (baseUrl.isBlank() || anonKey.isBlank() || baseUrl.contains("your-project")) return

        scope.launch {
            runCatching {
                var finalTitle = title
                var finalBody = body
                var destination = requestedDestination.orEmpty()
                val category = when (collection) {
                    "ibr_courses" -> {
                        finalTitle = "Novo curso no IBR"
                        destination = "ibr"
                        "courses"
                    }
                    "conteudos_videos" -> {
                        destination = "content"
                        runCatching {
                            val doc = Firebase.firestore.collection(collection).document(documentId).get().await()
                            val videoTitle = doc.getString("title").orEmpty().ifBlank { body }
                            val preacher = doc.getString("preacher").orEmpty()
                                .ifBlank { doc.getString("pregador").orEmpty() }
                                .ifBlank { doc.getString("artist").orEmpty() }
                                .ifBlank { doc.getString("description").orEmpty() }
                            finalTitle = "Nova pregação: $videoTitle"
                            finalBody = preacher.takeIf { it.isNotBlank() }?.let { "Pregador: $it" }
                                ?: "Nova pregação disponível na aba Mídia."
                        }
                        "sermons"
                    }
                    "conteudos_audios", "conteudos_books", "conteudos_albums" -> {
                        destination = "content"
                        "media"
                    }
                    "events" -> {
                        destination = "services"
                        "events"
                    }
                    "prayer_requests" -> {
                        destination = "admin_prayer/$documentId"
                        "prayer"
                    }
                    "prayer_response" -> {
                        if (destination.isBlank()) destination = Screen.Prayer.route
                        "prayer"
                    }
                    else -> "content_updates"
                }

                val payload = JSONObject()
                    .put("title", finalTitle)
                    .put("body", finalBody)
                    .put("data", JSONObject()
                        .put("collection", collection)
                        .put("documentId", documentId)
                        .put("category", category)
                        .put("destination", destination))
                topic?.takeIf { it.isNotBlank() }?.let { payload.put("topic", it) }
                deviceToken?.takeIf { it.isNotBlank() }?.let { payload.put("token", it) }

                val request = Request.Builder()
                    .url("$baseUrl/functions/v1/$FUNCTION_NAME")
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $anonKey")
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) Log.w("NotificationDispatcher", "Falha ao enviar notificação: HTTP ${response.code}")
                }
            }.onFailure { error -> Log.w("NotificationDispatcher", "Notificação automática indisponível", error) }
        }
    }
}
''')

replace_once(
    "app/src/main/java/com/aistudio/micrhema/NotificationHelper.kt",
    '''        IBR_CONTENT,
        CONTENT_UPDATES
    }''',
    '''        IBR_CONTENT,
        CONTENT_UPDATES,
        PRAYER
    }'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/NotificationHelper.kt",
    '''            "ibr", "ibr_content", "course_ibr", "aula" -> Category.IBR_CONTENT
            "content", "conteudo", "conteúdo", "content_updates", "app_update" -> Category.CONTENT_UPDATES
            else -> Category.GENERAL''',
    '''            "ibr", "ibr_content", "course_ibr", "aula" -> Category.IBR_CONTENT
            "prayer", "oracao", "oração", "prayer_response" -> Category.PRAYER
            "content", "conteudo", "conteúdo", "content_updates", "app_update" -> Category.CONTENT_UPDATES
            else -> Category.GENERAL'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/NotificationHelper.kt",
    '''        Category.IBR_CONTENT -> R.drawable.ic_notif_ibr
        Category.GENERAL, Category.CONTENT_UPDATES -> R.drawable.ic_notification''',
    '''        Category.IBR_CONTENT -> R.drawable.ic_notif_ibr
        Category.PRAYER, Category.GENERAL, Category.CONTENT_UPDATES -> R.drawable.ic_notification'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/NotificationHelper.kt",
    '''            Category.IBR_CONTENT -> settings.notifIbrContent && isIbrMember(context)
            Category.GENERAL, Category.CONTENT_UPDATES -> true''',
    '''            Category.IBR_CONTENT -> settings.notifIbrContent && isIbrMember(context)
            Category.PRAYER, Category.GENERAL, Category.CONTENT_UPDATES -> true'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/FCMService.kt",
    '''            category = category,
            respectPreferences = true,
            destinationRoute = destinationRoute,''',
    '''            category = category,
            respectPreferences = category != NotificationHelper.Category.PRAYER,
            destinationRoute = destinationRoute,'''
)

replace_once(
    "app/src/main/java/com/aistudio/micrhema/MainActivity.kt",
    '''val notificationDestinationState = mutableStateOf<String?>(null)''',
    '''val notificationDestinationState = mutableStateOf<String?>(null)
val adminPrayerTargetState = mutableStateOf<String?>(null)'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/MainActivity.kt",
    '''    private fun captureNotificationDestination(source: Intent?) {
        notificationDestinationState.value = source
            ?.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_DESTINATION)
            ?.takeIf { it == Screen.About.route }
    }''',
    '''    private fun captureNotificationDestination(source: Intent?) {
        val destination = source
            ?.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_DESTINATION)
            ?.takeIf {
                it == Screen.About.route || it == Screen.Prayer.route || it == Screen.Admin.route || it.startsWith("admin_prayer/")
            }
        notificationDestinationState.value = destination
        if (destination?.startsWith("admin_prayer/") == true) {
            adminPrayerTargetState.value = destination.substringAfter("admin_prayer/").takeIf { it.isNotBlank() }
        }
    }'''
)
replace_once(
    "app/src/main/java/com/aistudio/micrhema/MainActivity.kt",
    '''    LaunchedEffect(notificationDestinationState.value) {
        if (notificationDestinationState.value == Screen.About.route) {
            navController.navigate(Screen.About.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
            notificationDestinationState.value = null
        }
    }''',
    '''    LaunchedEffect(notificationDestinationState.value) {
        val destination = notificationDestinationState.value ?: return@LaunchedEffect
        val route = when {
            destination.startsWith("admin_prayer/") -> Screen.Admin.route
            destination == Screen.Prayer.route -> Screen.Prayer.route
            destination == Screen.Admin.route -> Screen.Admin.route
            destination == Screen.About.route -> Screen.About.route
            else -> null
        }
        route?.let {
            navController.navigate(it) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
        }
        notificationDestinationState.value = null
    }'''
)

replace_once(
    "firestore.rules",
    '''    match /prayer_requests/{document} {
      allow create: if signedIn();
      allow read, update, delete: if isAdmin();
    }''',
    '''    match /prayer_requests/{document} {
      allow create: if signedIn()
        && request.resource.data.requesterUid == request.auth.uid
        && request.resource.data.status == "pendente";
      allow read: if isAdmin()
        || (signedIn() && resource.data.requesterUid == request.auth.uid);
      allow update, delete: if isAdmin();
    }'''
)

(ROOT / "supabase/functions/notify-fcm/index.ts").write_text(r'''import { importPKCS8, SignJWT } from "npm:jose@5.10.0";

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const FCM_TOKEN_URL = "https://oauth2.googleapis.com/token";
const DEFAULT_PROJECT_ID = "mic-rhema";
const MAX_TEXT_LENGTH = 180;

type NotificationRequest = {
  topic?: string;
  token?: string;
  title?: string;
  body?: string;
  data?: Record<string, string | number | boolean>;
};

type ServiceAccount = {
  project_id?: string;
  client_email?: string;
  private_key?: string;
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json",
};

function json(body: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function cleanText(value: unknown, fallback: string): string {
  const text = String(value ?? fallback).trim();
  return text.slice(0, MAX_TEXT_LENGTH) || fallback;
}

async function accessToken(account: ServiceAccount): Promise<string> {
  if (!account.client_email || !account.private_key) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON sem client_email ou private_key.");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({ iss: account.client_email, scope: FCM_SCOPE, aud: FCM_TOKEN_URL })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(await importPKCS8(account.private_key.replace(/\\n/g, "\n"), "RS256"));

  const response = await fetch(FCM_TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }),
  });
  if (!response.ok) throw new Error(`Falha ao autenticar no FCM: ${response.status}`);
  const payload = await response.json();
  if (!payload.access_token) throw new Error("O Google não retornou um access_token.");
  return payload.access_token;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Método não permitido." }, 405);

  try {
    const configuredKey = Deno.env.get("SUPABASE_ANON_KEY");
    const providedKey = request.headers.get("apikey");
    if (configuredKey && providedKey && providedKey !== configuredKey) return json({ error: "Chave pública do Supabase inválida." }, 401);

    const input = await request.json() as NotificationRequest;
    const directToken = String(input.token ?? "").trim().slice(0, 4096);
    const topic = cleanText(input.topic, "all_users").replace(/[^a-zA-Z0-9_.-]/g, "");
    const title = cleanText(input.title, "Nova atualização disponível");
    const body = cleanText(input.body, "Confira as novidades no MIC Rhema.");
    const account = JSON.parse(Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "{}") as ServiceAccount;
    const projectId = account.project_id || Deno.env.get("FIREBASE_PROJECT_ID") || DEFAULT_PROJECT_ID;
    const token = await accessToken(account);
    const data = Object.fromEntries(Object.entries(input.data ?? {}).map(([key, value]) => [key, String(value)]));

    data.title = data.title || title;
    data.body = data.body || body;
    data.category = data.category || "content_updates";

    const target = directToken ? { token: directToken } : { topic };
    const response = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json; UTF-8" },
      body: JSON.stringify({ message: { ...target, data, android: { priority: "high", ttl: "86400s" } } }),
    });
    const responseBody = await response.text();
    if (!response.ok) {
      console.error("FCM rejected notification", response.status, responseBody);
      return json({ error: "FCM rejeitou a notificação.", details: responseBody.slice(0, 500) }, 502);
    }
    return json({ ok: true, target: directToken ? "token" : `topic:${topic}`, message: responseBody });
  } catch (error) {
    console.error("notify-fcm failed", error);
    return json({ error: error instanceof Error ? error.message : "Erro ao enviar notificação." }, 500);
  }
});
''')

print("Android prayer priority flow patch applied.")
