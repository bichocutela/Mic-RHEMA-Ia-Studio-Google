package com.aistudio.micrhema

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig

object VersionManager {
    val showForceUpdateDialog = mutableStateOf(false)
    val updateUrl = mutableStateOf("")

    fun checkVersion(context: Context) {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val minVersionCode = remoteConfig.getLong("min_version_code")
                val url = remoteConfig.getString("update_url")
                
                val currentVersionCode = try {
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
                } catch (e: Exception) {
                    // Fallback to BuildConfig if PackageInfo fails, though it's technically an Int in BuildConfig
                    BuildConfig.VERSION_CODE.toLong()
                }

                if (minVersionCode > 0 && currentVersionCode < minVersionCode) {
                    updateUrl.value = if (url.isNotBlank()) url else "https://play.google.com/store/apps/details?id=${context.packageName}"
                    showForceUpdateDialog.value = true
                }
            } else {
                Log.d("VersionManager", "Failed to fetch remote config for version check")
            }
        }
    }

    fun openUpdateLink(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl.value))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: Exception) {}
        }
    }
}
