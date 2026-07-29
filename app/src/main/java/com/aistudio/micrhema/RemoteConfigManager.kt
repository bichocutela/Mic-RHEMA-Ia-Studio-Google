package com.aistudio.micrhema

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

object RemoteConfigManager {
    val showWarningBanner = mutableStateOf(false)
    val warningBannerText = mutableStateOf("")
    val promoLinkUrl = mutableStateOf("")

    fun init() {
        if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) return
        
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        remoteConfig.setDefaultsAsync(mapOf(
            "show_warning_banner" to false,
            "warning_banner_text" to "",
            "promo_link_url" to ""
        ))
        
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Log.d("RemoteConfig", "Config params updated: $updated")
                } else {
                    Log.d("RemoteConfig", "Fetch failed")
                }
                
                showWarningBanner.value = remoteConfig.getBoolean("show_warning_banner")
                warningBannerText.value = remoteConfig.getString("warning_banner_text")
                promoLinkUrl.value = remoteConfig.getString("promo_link_url")
            }
    }
}
