package com.aistudio.micrhema

import java.time.ZonedDateTime
import java.time.ZoneId

/** Public reminder times follow the church's timezone, independent of the phone. */
object NotificationTiming {
    val zone: ZoneId = ZoneId.of("America/Fortaleza")
    fun now(): ZonedDateTime = ZonedDateTime.now(zone)
    fun slot(category: String, collection: String = "", destination: String = ""): Int? = when {
        collection == "devocionais" || category == "DAILY_DEVOTIONAL" -> 8
        category == "NEXT_SERVICE" -> 10
        category == "DAILY_NEWS" -> 12
        collection == "discipulado_pdfs" || destination.startsWith("discipulado") -> 14
        category == "APP_UPDATE" || destination == "about" -> 18
        category == "COURSES" || category == "IBR_CONTENT" -> 19
        collection.startsWith("conteudos_") || category == "MEDIA" || category == "SERMONS" -> 15
        category == "EVENTS" || category == "CONTENT_UPDATES" -> 17
        else -> null
    }
    fun isReminder(slot: Int?) = slot == 8 || slot == 10 || slot == 12
    fun delayMillis(slot: Int, now: ZonedDateTime = now()): Long {
        if (now.hour == slot) return 0
        var target = now.withHour(slot).withMinute(0).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return java.time.Duration.between(now, target).toMillis()
    }
}
