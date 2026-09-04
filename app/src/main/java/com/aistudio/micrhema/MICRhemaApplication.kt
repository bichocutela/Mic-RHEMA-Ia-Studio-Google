package com.aistudio.micrhema

import android.app.Application

class MICRhemaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        BackgroundNotificationCoordinator.initialize(this)
        // Canal exclusivo para avisos de nova versão do APK. A PWA nunca é inscrita neste tópico.
        runCatching {
            com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .subscribeToTopic("android_app_updates")
        }.onFailure { error ->
            android.util.Log.w("MICRhemaApplication", "Não foi possível inscrever o Android em atualizações", error)
        }
    }
}
