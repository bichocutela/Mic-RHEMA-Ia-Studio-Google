package com.aistudio.micrhema

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ServiceMomentNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val serviceId = inputData.getString(KEY_SERVICE_ID).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "Culto" }
        val time = inputData.getString(KEY_TIME).orEmpty()
        val expectedDate = inputData.getString(KEY_EXPECTED_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return Result.success()
        val kind = inputData.getString(KEY_KIND).orEmpty()

        if (LocalDate.now() != expectedDate) return Result.success()
        if (!NotificationHelper.hasNotificationPermission(context)) return Result.success()

        if (kind == KIND_TODAY) {
            val serviceTime = parseTime(time)
            if (serviceTime != null && LocalDateTime.now().isAfter(expectedDate.atTime(serviceTime).plusMinutes(15))) {
                return Result.success()
            }
        }

        val prefs = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
        val dedupe = "service:$kind:$expectedDate:$serviceId:$title"
        if (prefs.getString("last_service_moment_$kind", null) == dedupe) return Result.success()

        val timeText = time.takeIf { it.isNotBlank() }?.let { " às $it" }.orEmpty()
        when (kind) {
            KIND_DAY_BEFORE -> NotificationHelper.showNotification(
                context = context,
                title = "Amanhã tem culto: $title",
                message = "Prepare-se: amanhã teremos $title$timeText.",
                category = NotificationHelper.Category.NEXT_SERVICE,
                respectPreferences = true,
                destinationRoute = "services"
            )
            KIND_TODAY -> NotificationHelper.showNotification(
                context = context,
                title = "Hoje tem culto: $title",
                message = "Hoje é dia de $title$timeText. Esperamos você!",
                category = NotificationHelper.Category.NEXT_SERVICE,
                respectPreferences = true,
                destinationRoute = "services"
            )
            else -> return Result.success()
        }
        prefs.edit().putString("last_service_moment_$kind", dedupe).apply()
        return Result.success()
    }

    private fun parseTime(raw: String): LocalTime? {
        val normalized = raw.trim().lowercase().replace("h", ":").filter { it.isDigit() || it == ':' }
        return runCatching {
            val parts = normalized.split(':')
            when {
                parts.size >= 2 -> LocalTime.of(parts[0].toInt(), parts[1].take(2).toInt())
                normalized.length in 3..4 -> LocalTime.of(normalized.dropLast(2).toInt(), normalized.takeLast(2).toInt())
                else -> null
            }
        }.getOrNull()
    }

    companion object {
        const val KEY_SERVICE_ID = "service_id"
        const val KEY_TITLE = "title"
        const val KEY_TIME = "time"
        const val KEY_EXPECTED_DATE = "expected_date"
        const val KEY_KIND = "kind"
        const val KIND_DAY_BEFORE = "day_before"
        const val KIND_TODAY = "today"
    }
}
