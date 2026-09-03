package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Planejador dos avisos de culto. Ele não notifica ao ser executado: cria trabalhos
 * pontuais para 10h da véspera e 10h do próprio dia, permitindo funcionamento com
 * o app fechado e evitando o antigo disparo "assim que abrir".
 */
class ServiceAlertWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) return Result.retry()

            val services = FirebaseFirestore.getInstance()
                .collection("cultos_agenda")
                .get()
                .await()
                .documents
                .mapNotNull { document ->
                    runCatching { document.toObject(ChurchService::class.java)?.also { if (it.id.isBlank()) it.id = document.id } }.getOrNull()
                }
                .filter { it.isApproved }

            val workManager = WorkManager.getInstance(context)
            val desiredNames = linkedSetOf<String>()
            val today = LocalDate.now()

            services.forEach { service ->
                val serviceDate = resolveNextServiceDate(service, today) ?: return@forEach
                if (serviceDate.isBefore(today) || serviceDate.isAfter(today.plusDays(8))) return@forEach

                scheduleMoment(workManager, service, serviceDate.minusDays(1), ServiceMomentNotificationWorker.KIND_DAY_BEFORE)
                    ?.let(desiredNames::add)
                scheduleMoment(workManager, service, serviceDate, ServiceMomentNotificationWorker.KIND_TODAY)
                    ?.let(desiredNames::add)
            }

            reconcileStaleMoments(context, workManager, desiredNames)
            Result.success()
        } catch (e: Exception) {
            Log.e("ServiceAlertWorker", "Falha ao planejar notificações de culto", e)
            Result.retry()
        }
    }

    private fun scheduleMoment(
        workManager: WorkManager,
        service: ChurchService,
        expectedDate: LocalDate,
        kind: String
    ): String? {
        val target = expectedDate.atTime(10, 0)
        val now = LocalDateTime.now()
        if (target.isBefore(now.minusMinutes(15))) return null

        val stableServiceId = service.id.ifBlank { service.title.lowercase(Locale.getDefault()).replace(Regex("[^a-z0-9]+"), "-") }
        val workName = "ServiceMomentV3:${stableServiceId}:${expectedDate}:$kind"
        val delayMs = Duration.between(now, target).toMillis().coerceAtLeast(0L)
        val data = Data.Builder()
            .putString(ServiceMomentNotificationWorker.KEY_SERVICE_ID, stableServiceId)
            .putString(ServiceMomentNotificationWorker.KEY_TITLE, service.title)
            .putString(ServiceMomentNotificationWorker.KEY_TIME, service.time)
            .putString(ServiceMomentNotificationWorker.KEY_EXPECTED_DATE, expectedDate.toString())
            .putString(ServiceMomentNotificationWorker.KEY_KIND, kind)
            .build()
        val request = OneTimeWorkRequestBuilder<ServiceMomentNotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
        return workName
    }

    private fun resolveNextServiceDate(service: ChurchService, from: LocalDate): LocalDate? {
        service.date.trim().takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { LocalDate.parse(raw) }.getOrNull()?.let { return it }
        }
        val wantedDay = parseDay(service.day) ?: return null
        return (0..7)
            .map { from.plusDays(it.toLong()) }
            .firstOrNull { it.dayOfWeek == wantedDay }
    }

    private fun parseDay(raw: String): DayOfWeek? {
        val normalized = java.text.Normalizer.normalize(raw.lowercase(Locale.getDefault()), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return when {
            "domingo" in normalized -> DayOfWeek.SUNDAY
            "segunda" in normalized -> DayOfWeek.MONDAY
            "terca" in normalized -> DayOfWeek.TUESDAY
            "quarta" in normalized -> DayOfWeek.WEDNESDAY
            "quinta" in normalized -> DayOfWeek.THURSDAY
            "sexta" in normalized -> DayOfWeek.FRIDAY
            "sabado" in normalized -> DayOfWeek.SATURDAY
            else -> null
        }
    }

    companion object {
        private const val PREFS = "micrhema_service_moment_schedule"
        private const val KEY_NAMES = "scheduled_names"

        private fun reconcileStaleMoments(context: Context, wm: WorkManager, desired: Set<String>) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val previous = prefs.getStringSet(KEY_NAMES, emptySet()).orEmpty()
            (previous - desired).forEach(wm::cancelUniqueWork)
            prefs.edit().putStringSet(KEY_NAMES, desired).apply()
        }

        fun cancelScheduledServiceMoments(wm: WorkManager) {
            val context = runCatching {
                val field = WorkManager::class.java.getDeclaredField("mContext")
                field.isAccessible = true
                field.get(wm) as? Context
            }.getOrNull()
            if (context != null) {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                prefs.getStringSet(KEY_NAMES, emptySet()).orEmpty().forEach(wm::cancelUniqueWork)
                prefs.edit().remove(KEY_NAMES).apply()
            }
        }
    }
}
