package com.aistudio.micrhema

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
        val category = NotificationHelper.categoryFrom(remoteMessage.data["category"])
        val destinationRoute = remoteMessage.data["destination"] ?: remoteMessage.data["route"]
        val destinationDocumentId = remoteMessage.data["documentId"] ?: remoteMessage.data["document_id"]

        if (remoteMessage.notification != null || remoteMessage.data.isNotEmpty()) {
            NotificationHelper.showNotification(
                context = this,
                title = title,
                message = message,
                category = category,
                respectPreferences = true,
                destinationRoute = destinationRoute,
                destinationDocumentId = destinationDocumentId
            )
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
