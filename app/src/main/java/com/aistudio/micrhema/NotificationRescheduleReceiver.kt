package com.aistudio.micrhema

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reconstitui os trabalhos persistentes após reinício, atualização ou mudança de relógio/fuso. */
class NotificationRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        Thread {
            try {
                BackgroundNotificationCoordinator.initialize(context.applicationContext)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
