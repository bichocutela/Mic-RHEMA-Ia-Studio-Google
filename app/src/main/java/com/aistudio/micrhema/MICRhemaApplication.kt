package com.aistudio.micrhema

import android.app.Application

class MICRhemaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        BackgroundNotificationCoordinator.initialize(this)
    }
}
