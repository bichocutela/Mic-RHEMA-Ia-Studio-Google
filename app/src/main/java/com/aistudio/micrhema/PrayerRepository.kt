package com.aistudio.micrhema

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
