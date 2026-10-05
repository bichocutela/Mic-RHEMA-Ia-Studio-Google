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
        sharedContentListeners.getOrPut(key) {
            addSnapshotListener { snapshot, error ->
                if (error != null) releaseFailedSubscription(key)
                listener.onEvent(snapshot, error)
            }
        }
    }

internal fun DocumentReference.addSharedSnapshotListener(key: String, listener: EventListener<DocumentSnapshot>): ListenerRegistration =
    synchronized(sharedContentListeners) {
        sharedContentListeners.getOrPut(key) {
            addSnapshotListener { snapshot, error ->
                if (error != null) releaseFailedSubscription(key)
                listener.onEvent(snapshot, error)
            }
        }
    }

private fun releaseFailedSubscription(key: String) {
    synchronized(sharedContentListeners) { sharedContentListeners.remove(key)?.remove() }
}

internal fun <T> SnapshotStateList<T>.replaceContentsIfChanged(items: List<T>) {
    if (toList() == items) return
    Snapshot.withMutableSnapshot {
        clear()
        addAll(items)
    }
}
