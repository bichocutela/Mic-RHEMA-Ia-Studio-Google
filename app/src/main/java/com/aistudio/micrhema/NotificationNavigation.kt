package com.aistudio.micrhema

import java.net.URLEncoder

/** One contract for remote pushes and local reminders. No Android dependency. */
internal enum class NotificationKind {
    GENERAL, DEVOTIONAL, DISCIPULADO, PLAN, COURSE, LESSON, SERMON,
    MEDIA, VIDEO, AUDIO, BOOK, ALBUM, EVENT, SERVICE, NEWS, PRAYER, UPDATE, BIBLE, LIVE, MEMBER
}
internal data class NotificationDestination(val kind: NotificationKind, val route: String)

internal object NotificationNavigation {
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    fun safeRoute(value: String?): String? {
        val route = value?.trim()?.takeIf { it.isNotBlank() && it.length <= 2048 } ?: return null
        if (route.any { it == '\n' || it == '\r' || it == '#' } || route.contains("://")) return null
        val base = route.substringBefore('?')
        val tabs = setOf("home", "devotionals", "services", "prayer", "members", "ibr", "discipulado", "about", "donations", "settings", "content", "admin", "profile", "plans", "news_list", "bible", "equipe", "team")
        if (base in tabs) return route
        if (Regex("^(discipulado_pdf|ibr_course|admin_prayer)/[^/?]+$").matches(base)) return route
        if (Regex("^ibr_lesson/[^/?]+/[^/?]+$").matches(base)) return route
        if (Regex("^news_detail/[1-9][0-9]*$").matches(base)) return route
        return null
    }

    fun resolve(data: Map<String, String>): NotificationDestination {
        fun field(vararg keys: String) = keys.firstNotNullOfOrNull { data[it]?.trim()?.takeIf(String::isNotBlank) }.orEmpty()
        val category = field("category").lowercase()
        val collection = field("collection").lowercase()
        val destination = field("destination", "route")
        val id = field("documentId", "document_id", "id")
        val course = field("courseId", "course_id").ifBlank { if (collection == "ibr_courses") id else "" }
        val chapter = field("chapterId", "chapter_id", "lessonId", "lesson_id")
        val theme = field("theme", "themeName", "theme_name")
        val plan = field("planId", "plan_id").ifBlank { if (collection == "bible_plans") id else "" }
        val kind = when {
            category == "app_update" || destination == "about" -> NotificationKind.UPDATE
            collection == "prayer_requests" || collection == "prayer_response" || category in setOf("prayer", "prayer_response", "oracao", "oração") || destination.startsWith("admin_prayer/") || destination.startsWith("prayer") -> NotificationKind.PRAYER
            collection == "discipulado_pdfs" || category in setOf("discipulado", "study", "estudo") || destination.startsWith("discipulado") -> NotificationKind.DISCIPULADO
            collection == "bible_plans" || category in setOf("plan", "plans", "theme", "tema", "plano") || destination.startsWith("plans") -> NotificationKind.PLAN
            category in setOf("devotional", "devocional", "daily_devotional") || collection == "devocionais" || destination.startsWith("devotionals") -> NotificationKind.DEVOTIONAL
            category in setOf("news", "noticia", "notícia", "daily_news") || collection == "bible_news" || destination.startsWith("news_") -> NotificationKind.NEWS
            chapter.isNotBlank() || category in setOf("ibr", "ibr_content", "aula", "lesson") || destination.startsWith("ibr_lesson/") -> NotificationKind.LESSON
            collection == "ibr_courses" || category in setOf("course", "courses", "curso", "course_ibr") || destination.startsWith("ibr_course/") -> NotificationKind.COURSE
            category in setOf("sermon", "sermons", "pregacao", "pregação") -> NotificationKind.SERMON
            collection == "conteudos_audios" || category == "audio" -> NotificationKind.AUDIO
            collection == "conteudos_books" || category == "book" -> NotificationKind.BOOK
            collection == "conteudos_albums" || category in setOf("album", "photo") -> NotificationKind.ALBUM
            collection == "conteudos_videos" || category == "video" -> NotificationKind.VIDEO
            category in setOf("service", "culto", "next_service") || collection == "cultos_agenda" || destination.startsWith("services") -> NotificationKind.SERVICE
            category in setOf("event", "events", "evento") || collection == "events" -> NotificationKind.EVENT
            category in setOf("media", "midia", "mídia") || destination == "content" -> NotificationKind.MEDIA
            category == "bible" || destination.startsWith("bible") -> NotificationKind.BIBLE
            category == "live" -> NotificationKind.LIVE
            collection == "acessos_pendentes" || category in setOf("member", "members") -> NotificationKind.MEMBER
            else -> NotificationKind.GENERAL
        }
        val explicit = safeRoute(destination)
        // A detailed route already provided by a local producer stays authoritative.
        if (explicit != null && (explicit.contains('/') || explicit.contains('?'))) return NotificationDestination(kind, explicit)
        val route = when (kind) {
            NotificationKind.UPDATE -> "about"
            NotificationKind.DISCIPULADO -> if (id.isNotBlank()) "discipulado_pdf/${encode(id)}" else "discipulado"
            NotificationKind.DEVOTIONAL -> if (id.isNotBlank()) "devotionals?id=${encode(id)}" else "devotionals"
            NotificationKind.NEWS -> if (id.toIntOrNull()?.let { it > 0 } == true) "news_detail/$id" else "news_list"
            NotificationKind.PLAN -> "plans" + listOfNotNull(theme.takeIf(String::isNotBlank)?.let { "theme=${encode(it)}" }, plan.takeIf(String::isNotBlank)?.let { "planId=${encode(it)}" }).joinToString("&").let { if (it.isBlank()) "" else "?$it" }
            NotificationKind.COURSE, NotificationKind.LESSON -> when {
                course.isNotBlank() && chapter.isNotBlank() -> "ibr_lesson/${encode(course)}/${encode(chapter)}"
                course.isNotBlank() -> "ibr_course/${encode(course)}"
                id.isNotBlank() && kind == NotificationKind.COURSE -> "ibr_course/${encode(id)}"
                else -> "ibr"
            }
            NotificationKind.PRAYER -> if (collection == "prayer_requests" || destination == "admin") {
                if (id.isBlank()) "admin" else "admin_prayer/${encode(id)}"
            } else if (id.isNotBlank()) "prayer?request=${encode(id)}" else "prayer"
            NotificationKind.SERMON, NotificationKind.VIDEO, NotificationKind.AUDIO, NotificationKind.BOOK, NotificationKind.ALBUM -> {
                val type = when (kind) { NotificationKind.AUDIO -> "audio"; NotificationKind.BOOK -> "book"; NotificationKind.ALBUM -> "album"; else -> "video" }
                if (id.isBlank()) "content?type=$type" else "content?type=$type&id=${encode(id)}"
            }
            NotificationKind.MEDIA -> "content"
            NotificationKind.EVENT, NotificationKind.SERVICE -> if (id.isBlank()) "services" else "services?id=${encode(id)}"
            NotificationKind.BIBLE -> "bible"
            NotificationKind.LIVE -> "home"
            NotificationKind.MEMBER -> if (collection == "acessos_pendentes") "admin?section=members" else "members"
            NotificationKind.GENERAL -> explicit ?: when (category) { "media", "midia", "mídia" -> "content"; "ibr_content" -> "ibr"; else -> "home" }
        }
        return NotificationDestination(kind, route)
    }
}
