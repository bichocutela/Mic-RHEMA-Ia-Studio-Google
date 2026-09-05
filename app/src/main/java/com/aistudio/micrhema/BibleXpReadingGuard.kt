package com.aistudio.micrhema

import android.content.Context
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Confirma leitura bíblica real sem depender do simples carregamento da tela.
 *
 * Capítulo: pelo menos 30 s na mesma leitura e avanço até 80% dos versículos.
 * Versículo: a mesma posição precisa permanecer estável por 5 s antes do +1 XP.
 * Os receipts do backend continuam sendo a proteção final contra duplicidade.
 */
object BibleXpReadingGuard {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val chapterJobs = ConcurrentHashMap<String, Job>()
    private val verseJobs = ConcurrentHashMap<String, Job>()

    fun scheduleChapter(context: Context, contentId: String) {
        val member = loggedInMemberState.value ?: return
        val parsed = parseChapterId(contentId) ?: return
        val appContext = context.applicationContext
        val key = "${member.id}:$contentId"
        if (chapterJobs[key]?.isActive == true) return

        chapterJobs[key] = scope.launch {
            try {
                delay(30_000L)
                val verses = runCatching {
                    BibleFetcher.getChapter(appContext, parsed.book, parsed.chapter, parsed.version)
                }.getOrDefault(emptyList())
                val lastVerse = verses.maxOfOrNull { it.verse } ?: return@launch
                val threshold = kotlin.math.ceil(lastVerse * 0.80).toInt().coerceAtLeast(1)

                repeat(420) {
                    val activeMember = loggedInMemberState.value
                    if (activeMember?.id != member.id) return@launch
                    val position = BibleReadingPreferences.getLastReading(appContext)
                    if (
                        position != null &&
                        position.book == parsed.book &&
                        position.chapter == parsed.chapter &&
                        position.version == parsed.version &&
                        position.verse >= threshold
                    ) {
                        BadgeActivityTracker.recordVerifiedBibleChapter(appContext, contentId)
                        return@launch
                    }
                    delay(2_000L)
                }
            } finally {
                chapterJobs.remove(key)
            }
        }
    }

    fun observeVerse(context: Context, position: BibleReadingPreferences.ReadingPosition) {
        val member = loggedInMemberState.value ?: return
        val appContext = context.applicationContext
        val memberKey = member.id
        verseJobs.remove(memberKey)?.cancel()
        verseJobs[memberKey] = scope.launch {
            try {
                delay(5_000L)
                if (loggedInMemberState.value?.id != member.id) return@launch
                val current = BibleReadingPreferences.getLastReading(appContext) ?: return@launch
                if (current != position) return@launch
                XpActivityBridge.bibleVerse(
                    appContext,
                    "${position.version}:${position.book}:${position.chapter}:${position.verse}"
                )
            } finally {
                verseJobs.remove(memberKey)
            }
        }
    }

    fun clearSession(memberId: String? = null) {
        if (memberId == null) {
            chapterJobs.values.forEach { it.cancel() }
            verseJobs.values.forEach { it.cancel() }
            chapterJobs.clear()
            verseJobs.clear()
            return
        }
        chapterJobs.entries.removeIf { (key, job) ->
            if (key.startsWith("$memberId:")) {
                job.cancel()
                true
            } else false
        }
        verseJobs.remove(memberId)?.cancel()
    }

    private data class ChapterId(val version: String, val book: String, val chapter: Int)

    private fun parseChapterId(value: String): ChapterId? {
        val first = value.indexOf(':')
        val last = value.lastIndexOf(':')
        if (first <= 0 || last <= first) return null
        val version = value.substring(0, first).trim()
        val book = value.substring(first + 1, last).trim()
        val chapter = value.substring(last + 1).toIntOrNull() ?: return null
        val maxChapter = chapterCounts[book] ?: return null
        if (version.isBlank() || chapter !in 1..maxChapter) return null
        return ChapterId(version, book, chapter)
    }
}
