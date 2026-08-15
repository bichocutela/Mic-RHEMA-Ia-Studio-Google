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

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "MIC Rhema"
        val message = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]
            ?: "Nova mensagem"
        val category = NotificationHelper.categoryFrom(remoteMessage.data["category"])

        if (remoteMessage.notification != null || remoteMessage.data.isNotEmpty()) {
            NotificationHelper.showNotification(
                context = this,
                title = title,
                message = message,
                category = category,
                respectPreferences = true
            )
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // O token fica disponível para futura integração de envio segmentado.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    companion object {
        private const val TAG = "FCMService"
    }
}
