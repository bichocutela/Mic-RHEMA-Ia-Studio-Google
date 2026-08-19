package com.aistudio.micrhema

import android.content.Context
import android.net.ConnectivityManager
import coil.Coil
import coil.request.CachePolicy
import coil.request.ImageRequest

/** Centraliza os comportamentos de conteúdo que dependem das preferências do usuário. */
object ContentPreferenceManager {
    fun shouldAcceptAutomaticUpdate(): Boolean = currentSettingsState.value.autoUpdateContent

    fun preloadImages(context: Context, urls: Collection<String>) {
        val settings = currentSettingsState.value
        if (!settings.preloadImages || (settings.saveMobileData && isUsingMeteredNetwork(context))) return

        val imageLoader = Coil.imageLoader(context.applicationContext)
        urls.asSequence()
            .filter { it.isNotBlank() }
            .distinct()
            .take(16)
            .forEach { url ->
                imageLoader.enqueue(
                    ImageRequest.Builder(context.applicationContext)
                        .data(url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                )
            }
    }

    fun backupIfEnabled(context: Context) {
        if (currentSettingsState.value.autoBackup) {
            LocalDataManager.saveAll(context.applicationContext)
        }
    }

    private fun isUsingMeteredNetwork(context: Context): Boolean {
        val manager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return manager?.isActiveNetworkMetered == true
    }
}
