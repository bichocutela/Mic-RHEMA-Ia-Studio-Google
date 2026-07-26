package com.aistudio.micrhema

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MICRhemaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        
        
    }
}
