package com.aistudio.micrhema

import org.junit.Assert.*
import org.junit.Test

class NotificationNavigationTest {
    private fun target(vararg entries: Pair<String, String>) = NotificationNavigation.resolve(mapOf(*entries))

    @Test fun opensEachStudyInsteadOfOnlyTheTab() {
        val result = target("category" to "content_updates", "collection" to "discipulado_pdfs", "documentId" to "drive-abc", "destination" to "discipulado")
        assertEquals(NotificationKind.DISCIPULADO, result.kind)
        assertEquals("discipulado_pdf/drive-abc", result.route)
    }
    @Test fun mediaGetsItsOwnIconAndExactContent() {
        for ((collection, type, kind) in listOf(
            Triple("conteudos_videos", "video", NotificationKind.VIDEO), Triple("conteudos_audios", "audio", NotificationKind.AUDIO),
            Triple("conteudos_books", "book", NotificationKind.BOOK), Triple("conteudos_albums", "album", NotificationKind.ALBUM))) {
            val result = target("collection" to collection, "documentId" to "item", "category" to "content_updates")
            assertEquals(kind, result.kind); assertEquals("content?type=$type&id=item", result.route)
        }
    }
    @Test fun honorsExplicitDetailRoutesAndEncodesIds() {
        assertEquals("devotionals?id=abc", target("destination" to "devotionals?id=abc").route)
        assertEquals("discipulado_pdf/a%2Fb", target("collection" to "discipulado_pdfs", "id" to "a/b").route)
        assertEquals("ibr_lesson/course/lesson", target("category" to "ibr", "courseId" to "course", "chapterId" to "lesson").route)
    }
    @Test fun planThemeIsNotLostAndSupportsPortugueseNames() {
        assertEquals("plans?theme=F%C3%A9%20e%20ora%C3%A7%C3%A3o&planId=plan", target("category" to "plan", "theme" to "Fé e oração", "planId" to "plan").route)
    }
    @Test fun newsPrayerServicesAndUpdatesGoToTheirTargets() {
        assertEquals("news_detail/42", target("collection" to "bible_news", "id" to "42").route)
        assertEquals("prayer?request=abc", target("category" to "prayer_response", "id" to "abc").route)
        assertEquals("admin_prayer/abc", target("collection" to "prayer_requests", "id" to "abc").route)
        assertEquals("services?id=abc", target("category" to "service", "id" to "abc").route)
        assertEquals(NotificationKind.UPDATE, target("category" to "app_update").kind)
        assertEquals("about", target("category" to "app_update").route)
    }
    @Test fun everyGeneralReminderHasAUsefulFallback() {
        assertEquals("devotionals", target("category" to "daily_devotional").route)
        assertEquals("ibr", target("category" to "course").route)
        assertEquals("content", target("category" to "media").route)
        assertEquals("news_list", target("category" to "news").route)
    }
    @Test fun refusesExternalAndUnregisteredRoutes() {
        assertNull(NotificationNavigation.safeRoute("https://example.com"))
        assertNull(NotificationNavigation.safeRoute("unknown/123"))
        assertNull(NotificationNavigation.safeRoute("news_detail/broken"))
        assertNotNull(NotificationNavigation.safeRoute("discipulado_pdf/drive-abc"))
    }
}
