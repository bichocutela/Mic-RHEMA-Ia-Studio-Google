package com.aistudio.micrhema

import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore

/** Persistência dos eventos temporários, separada da programação fixa de cultos. */
fun saveChurchEvent(
    item: ChurchEventModel,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    if (BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
        onFailure(IllegalStateException("Firebase não configurado"))
        return
    }

    val now = System.currentTimeMillis()
    val id = item.id.ifBlank { java.util.UUID.randomUUID().toString() }
    val isNewEvent = item.id.isBlank()
    val persisted = item.copy(
        id = id,
        startDate = item.startDate.trim(),
        endDate = item.endDate.trim().ifBlank { item.startDate.trim() },
        title = item.title.trim(),
        description = item.description.trim(),
        preacher = item.preacher.trim(),
        time = item.time.trim(),
        location = item.location.trim(),
        bannerUrl = item.bannerUrl.trim(),
        createdAt = item.createdAt.takeIf { it > 0L } ?: now,
        updatedAt = now
    )

    val data = mapOf(
        "id" to persisted.id,
        "title" to persisted.title,
        "description" to persisted.description,
        "preacher" to persisted.preacher,
        "startDate" to persisted.startDate,
        "endDate" to persisted.endDate,
        "time" to persisted.time,
        "location" to persisted.location,
        "bannerUrl" to persisted.bannerUrl,
        "isPublished" to persisted.isPublished,
        "createdAt" to persisted.createdAt,
        "updatedAt" to persisted.updatedAt
    )

    Firebase.firestore.collection("events").document(id)
        .set(data, SetOptions.merge())
        .addOnSuccessListener {
            // Eventos temporários usam exclusivamente a preferência "Avisos de eventos e cultos".
            // Não dispara novamente em simples edições para evitar notificações duplicadas.
            if (isNewEvent && persisted.isPublished) {
                val period = if (persisted.endDate.isBlank() || persisted.endDate == persisted.startDate) {
                    persisted.startDate
                } else {
                    "${persisted.startDate} até ${persisted.endDate}"
                }
                val details = listOf(period, persisted.time, persisted.location)
                    .filter { it.isNotBlank() }
                    .joinToString(" • ")
                NotificationDispatcher.enqueue(
                    topic = "all_users",
                    title = "Novo evento: ${persisted.title}",
                    body = details.ifBlank { "Confira os detalhes na aba Cultos." },
                    collection = "events",
                    documentId = persisted.id
                )
            }
            onSuccess()
        }
        .addOnFailureListener { onFailure(it) }
}

fun removeChurchEvent(
    item: ChurchEventModel,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    if (BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
        onFailure(IllegalStateException("Firebase não configurado"))
        return
    }
    if (item.id.isBlank()) {
        onFailure(IllegalArgumentException("Evento sem identificador"))
        return
    }

    Firebase.firestore.collection("events").document(item.id)
        .delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}
