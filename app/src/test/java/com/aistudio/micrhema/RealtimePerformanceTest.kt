package com.aistudio.micrhema

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.*
import org.junit.Test

class RealtimePerformanceTest {
    @Test fun unchangedContentKeepsListIdentity() {
        val state = mutableStateListOf("book", "audio")
        val before = state.toList()
        state.replaceContentsIfChanged(listOf("book", "audio"))
        assertSame(before, state.toList())
    }

    @Test fun readerSnapshotRemainsConsistentDuringCatalogReplacement() {
        val state = mutableStateListOf("old-book", "old-audio")
        val reader = Snapshot.takeSnapshot()
        try {
            state.replaceContentsIfChanged(listOf("new-audio", "new-book"))
            assertEquals(listOf("old-book", "old-audio"), reader.enter { state.toList() })
            assertEquals(listOf("new-audio", "new-book"), state.toList())
        } finally {
            reader.dispose()
        }
    }

    @Test fun deletingLastRemoteItemClearsCatalog() {
        val state = mutableStateListOf("deleted-book")
        state.replaceContentsIfChanged(emptyList())
        assertTrue(state.isEmpty())
    }
}
