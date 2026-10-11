package com.aistudio.micrhema

import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class NotificationTimingTest {
    @Test fun devotionalAndNewsHaveSeparateSlots() {
        assertEquals(8, NotificationTiming.slot("DAILY_DEVOTIONAL"))
        assertEquals(12, NotificationTiming.slot("DAILY_NEWS"))
        assertEquals(14, NotificationTiming.slot("CONTENT_UPDATES", "discipulado_pdfs"))
        assertEquals(18, NotificationTiming.slot("CONTENT_UPDATES", destination = "about"))
    }
    @Test fun midnightQueuesContentUntilItsOwnSlot() {
        val midnight = ZonedDateTime.parse("2026-10-11T00:00:00-03:00[America/Fortaleza]")
        assertEquals(15 * 3600000L, NotificationTiming.delayMillis(15, midnight))
    }
    @Test fun lateWorkTargetsNextDayInsteadOfDeliveringImmediately() {
        val noon = ZonedDateTime.parse("2026-10-11T12:00:00-03:00[America/Fortaleza]")
        assertEquals(20 * 3600000L, NotificationTiming.delayMillis(8, noon))
        assertEquals(0L, NotificationTiming.delayMillis(12, noon))
    }
}
