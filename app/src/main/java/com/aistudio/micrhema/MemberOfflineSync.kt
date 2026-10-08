package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Delayed, silent and network-constrained reconciliation of a trusted device session. */
object MemberOfflineSync {
    private const val IMMEDIATE = "micrhema_member_sync_delayed_v1"
    private const val PERIODIC = "micrhema_member_sync_periodic_v1"
    private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule(context: Context, immediate: Boolean = true) {
        val app = context.applicationContext
        val manager = WorkManager.getInstance(app)
        manager.enqueueUniquePeriodicWork(
            PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<MemberOfflineSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
        )
        // After an online login there is nothing to recheck immediately.
        if (!immediate) return
        // Let a cached member enter immediately, then reconcile quietly.
        manager.enqueueUniqueWork(
            IMMEDIATE,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<MemberOfflineSyncWorker>()
                .setInitialDelay(45, TimeUnit.SECONDS)
                .setConstraints(online)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
        )
    }
}

class MemberOfflineSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val local = withContext(Dispatchers.IO) { MemberOfflineCache.restoreTrusted(context) }
            ?: return Result.success()
        val prefs = context.getSharedPreferences("micrhema_members_prefs", Context.MODE_PRIVATE)
        if (prefs.getString("logged_in_member_id", null) != local.id) return Result.success()

        return try {
            val result = MemberSessionClient.recover(context, local.phone)
            // User may have logged out while the request was in flight.
            if (prefs.getString("logged_in_member_id", null) != local.id) return Result.success()

            if (!result.found || result.member == null) {
                // This is an authoritative successful lookup, not a 429/offline response.
                MemberOfflineCache.clear(context)
                withContext(Dispatchers.Main) {
                    if (loggedInMemberState.value?.id == local.id) {
                        MemberManager.setLoggedInMember(context, null, preserveAdminFirebaseOnLogout = true)
                    } else {
                        prefs.edit().remove("logged_in_member_id").apply()
                    }
                }
                return Result.success()
            }

            val remote = result.member
            MemberOfflineCache.save(context, remote)
            withContext(Dispatchers.Main) {
                if (loggedInMemberState.value?.id == local.id) {
                    MemberManager.setLoggedInMember(context, remote, bindFirebaseIdentity = false)
                } else if (prefs.getString("logged_in_member_id", null) == local.id) {
                    prefs.edit().putString("logged_in_member_id", remote.id).apply()
                }
            }
            Result.success()
        } catch (error: Exception) {
            // A 429, missing connectivity or failed sync must never log out the
            // verified member, erase progress or expose a UI error.
            Log.w("MemberOfflineSync", "Sincronização do perfil adiada", error)
            Result.retry()
        }
    }
}
