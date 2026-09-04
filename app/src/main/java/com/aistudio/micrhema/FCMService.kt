package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }

        val title = remoteMessage.data["title"]
            ?: remoteMessage.notification?.title
            ?: "MIC Rhema"
        val message = remoteMessage.data["body"]
            ?: remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: "Nova mensagem"
        val rawCategory = remoteMessage.data["category"].orEmpty()
        val category = NotificationHelper.categoryFrom(rawCategory)
        val destinationRoute = remoteMessage.data["destination"] ?: remoteMessage.data["route"]
        val destinationDocumentId = remoteMessage.data["documentId"] ?: remoteMessage.data["document_id"]
        val collection = remoteMessage.data["collection"].orEmpty()
        val version = remoteMessage.data["version"].orEmpty()

        if (remoteMessage.notification == null && remoteMessage.data.isEmpty()) return

        // Release nova: o push instantâneo é reservado ao ADM que estiver logado.
        // Usuários comuns ignoram este push e deixam o WorkManager consultar a versão
        // silenciosamente a cada 12 horas, evitando avisos a cada publicação.
        val isAppUpdate = rawCategory == "app_update"
        val hasAdminSession = adminAuthenticatedState.value || loggedInMemberState.value?.isAdmin == true
        if (isAppUpdate && !hasAdminSession) {
            Log.d(TAG, "Push de atualização ignorado para usuário comum; verificação local cuidará do aviso.")
            return
        }

        // Conteúdo pode chegar pelo FCM e também pelo WorkManager. A chave abaixo garante
        // que apenas uma dessas fontes mostre a notificação daquele documento.
        val mediaCollections = setOf("conteudos_videos", "conteudos_audios", "conteudos_books", "conteudos_albums")
        val eventKey = when {
            collection in mediaCollections && !destinationDocumentId.isNullOrBlank() ->
                "content:$collection:$destinationDocumentId"
            version.isNotBlank() && isAppUpdate ->
                "app_update:$version"
            collection.isNotBlank() && !destinationDocumentId.isNullOrBlank() ->
                "fcm:$collection:$destinationDocumentId:${category.name}"
            remoteMessage.messageId?.isNotBlank() == true -> "fcm_message:${remoteMessage.messageId}"
            else -> ""
        }

        if (version.isNotBlank() && isAppUpdate) {
            val prefs = getSharedPreferences("micrhema_update_notifications", Context.MODE_PRIVATE)
            if (prefs.getString("last_notified_version", "") == version) return
        }

        if (eventKey.isNotBlank() && !NotificationHelper.claimNotificationEvent(this, eventKey)) return

        NotificationHelper.showNotification(
            context = this,
            title = title,
            message = message,
            category = category,
            respectPreferences = category != NotificationHelper.Category.PRAYER,
            destinationRoute = destinationRoute,
            destinationDocumentId = destinationDocumentId
        )

        if (collection in mediaCollections && !destinationDocumentId.isNullOrBlank()) {
            NotificationHelper.rememberMediaIds(this, listOf(destinationDocumentId))
        }
        if (version.isNotBlank() && isAppUpdate && NotificationHelper.hasNotificationPermission(this)) {
            getSharedPreferences("micrhema_update_notifications", Context.MODE_PRIVATE)
                .edit()
                .putString("last_notified_version", version)
                .putLong("last_notified_at", System.currentTimeMillis())
                .apply()
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Token FCM renovado; reconciliando tópicos")
        NotificationHelper.ensureMessagingReady(this)
    }

    companion object {
        private const val TAG = "FCMService"
    }
}
