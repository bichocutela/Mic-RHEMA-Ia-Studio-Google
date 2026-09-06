package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Manifesto compatível com versões antigas: se o documento não existir, o app
 * mantém os listeners e o cache atuais sem bloquear a inicialização.
 *
 * Firestore: settings/content_manifest
 */
data class ContentSyncManifest(
    var contentVersion: Long = 0L,
    var minimumAppVersion: String = "",
    var recommendedAppVersion: String = "",
    var updatedSections: List<String> = emptyList(),
    var forceApkUpdate: Boolean = false,
    var updatedAt: Long = 0L
)

object SilentContentSyncManager {
    private const val TAG = "SilentContentSync"
    private const val PREFS = "micrhema_content_sync"
    private const val KEY_APPLIED_VERSION = "applied_content_version"
    private const val PERIODIC_WORK = "SilentContentSyncV1"
    private const val IMMEDIATE_WORK = "SilentContentSyncImmediateV1"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun initialize(context: Context) {
        if (BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) return
        val appContext = context.applicationContext
        schedulePeriodic(appContext)
        enqueueImmediate(appContext)
    }

    fun enqueueImmediate(context: Context, force: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<SilentContentSyncWorker>()
            .setConstraints(networkConstraint)
            .setInputData(androidx.work.workDataOf(SilentContentSyncWorker.KEY_FORCE to force))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<SilentContentSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    internal suspend fun synchronize(context: Context, force: Boolean): Boolean {
        val appContext = context.applicationContext
        val snapshot = FirebaseFirestore.getInstance()
            .collection("settings")
            .document("content_manifest")
            .get(Source.SERVER)
            .await()

        // Implantação progressiva: antes de o painel publicar o manifesto, mantemos a
        // atualização completa já existente. Assim a funcionalidade entra em produção
        // sem depender de uma alteração simultânea no servidor.
        if (!snapshot.exists()) {
            forceRefreshData()
            LocalDataManager.saveAll(appContext)
            return true
        }

        val manifest = snapshot.toObject(ContentSyncManifest::class.java) ?: return true
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val appliedVersion = prefs.getLong(KEY_APPLIED_VERSION, 0L)

        notifyIfApkIsRequired(appContext, manifest)
        if (!force && manifest.contentVersion <= appliedVersion) return true

        // As rotinas atuais continuam sendo a autoridade de leitura. Só confirmamos a
        // versão após a atualização terminar; falhas preservam a versão/cache anterior.
        forceRefreshData()
        LocalDataManager.saveAll(appContext)
        prefs.edit()
            .putLong(KEY_APPLIED_VERSION, manifest.contentVersion)
            .putLong("last_success_at", System.currentTimeMillis())
            .putStringSet("last_sections", manifest.updatedSections.toSet())
            .apply()
        Log.i(TAG, "Conteúdo ${manifest.contentVersion} sincronizado")
        return true
    }

    private fun notifyIfApkIsRequired(context: Context, manifest: ContentSyncManifest) {
        if (!manifest.forceApkUpdate || manifest.minimumAppVersion.isBlank()) return
        if (!isNewerVersion(BuildConfig.VERSION_NAME, manifest.minimumAppVersion)) return

        val eventKey = "minimum_app_version:${manifest.minimumAppVersion}"
        if (!NotificationHelper.claimNotificationEvent(context, eventKey)) return
        NotificationHelper.showNotification(
            context = context,
            title = "Atualização necessária",
            message = "Atualize o MIC Rhema para continuar recebendo todos os recursos.",
            category = NotificationHelper.Category.CONTENT_UPDATES,
            respectPreferences = false,
            destinationRoute = Screen.About.route
        )
    }

    private fun isNewerVersion(current: String, required: String): Boolean {
        val currentParts = current.split('.').map { it.toIntOrNull() ?: 0 }
        val requiredParts = required.split('.').map { it.toIntOrNull() ?: 0 }
        repeat(maxOf(currentParts.size, requiredParts.size)) { index ->
            val installed = currentParts.getOrElse(index) { 0 }
            val minimum = requiredParts.getOrElse(index) { 0 }
            if (minimum > installed) return true
            if (minimum < installed) return false
        }
        return false
    }
}

class SilentContentSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) return Result.success()
        return try {
            val force = inputData.getBoolean(KEY_FORCE, false)
            if (SilentContentSyncManager.synchronize(applicationContext, force)) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (error: Exception) {
            Log.w("SilentContentSync", "Sincronização adiada; cache preservado", error)
            Result.retry()
        }
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
