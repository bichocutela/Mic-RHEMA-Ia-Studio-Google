package com.aistudio.micrhema

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
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

        if (remoteMessage.notification != null || remoteMessage.data.isNotEmpty()) {
            NotificationHelper.showNotification(
                context = this,
                title = title,
                message = message,
                category = category,
                respectPreferences = true,
                destinationRoute = destinationRoute
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Token FCM renovado; reconciliando tópicos")
        reconcileTopics()
    }

    private fun reconcileTopics() {
        val messaging = FirebaseMessaging.getInstance()

        messaging.subscribeToTopic("all_users")
            .addOnSuccessListener { Log.d(TAG, "Inscrição all_users confirmada") }
            .addOnFailureListener { Log.e(TAG, "Falha ao inscrever em all_users", it) }

        messaging.subscribeToTopic("devocionais")
            .addOnSuccessListener { Log.d(TAG, "Inscrição devocionais confirmada") }
            .addOnFailureListener { Log.e(TAG, "Falha ao inscrever em devocionais", it) }

        if (NotificationHelper.isIbrMember(this)) {
            messaging.subscribeToTopic("ibr_users")
                .addOnSuccessListener { Log.d(TAG, "Inscrição ibr_users confirmada") }
                .addOnFailureListener { Log.e(TAG, "Falha ao inscrever em ibr_users", it) }
        } else {
            messaging.unsubscribeFromTopic("ibr_users")
                .addOnSuccessListener { Log.d(TAG, "Usuário fora do tópico ibr_users") }
                .addOnFailureListener { Log.e(TAG, "Falha ao remover de ibr_users", it) }
        }
    }

    companion object {
        private const val TAG = "FCMService"
    }
}
