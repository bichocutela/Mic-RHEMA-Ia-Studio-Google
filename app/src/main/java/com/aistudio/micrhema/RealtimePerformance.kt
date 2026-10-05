package com.aistudio.micrhema

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

// Inscrições do catálogo público vivem no processo, não na Activity recriada.
private val sharedContentListeners = mutableMapOf<String, ListenerRegistration>()

internal fun Query.addSharedSnapshotListener(key: String, listener: EventListener<QuerySnapshot>): ListenerRegistration =
    synchronized(sharedContentListeners) {
        sharedContentListeners.getOrPut(key) { addSnapshotListener(listener) }
    }

internal fun DocumentReference.addSharedSnapshotListener(key: String, listener: EventListener<DocumentSnapshot>): ListenerRegistration =
    synchronized(sharedContentListeners) {
        sharedContentListeners.getOrPut(key) { addSnapshotListener(listener) }
    }

internal fun <T> SnapshotStateList<T>.replaceContentsIfChanged(items: List<T>) {
    if (toList() == items) return
    Snapshot.withMutableSnapshot {
        clear()
        addAll(items)
    }
}
