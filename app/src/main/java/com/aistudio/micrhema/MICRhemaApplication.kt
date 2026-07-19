package com.aistudio.micrhema

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MICRhemaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty() && FirebaseApp.getApps(this).isEmpty()) {
            try {
                val options = FirebaseOptions.Builder()
                    .setProjectId(com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID)
                    .setApplicationId(com.aistudio.micrhema.BuildConfig.FIREBASE_APP_ID)
                    .setApiKey(com.aistudio.micrhema.BuildConfig.FIREBASE_API_KEY)
                    .build()
                FirebaseApp.initializeApp(this, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
