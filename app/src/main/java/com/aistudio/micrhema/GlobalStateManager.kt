package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global State Manager
 * 
 * Uses Kotlin Flows and Firestore snapshot listeners to immediately propagate
 * data changes made in the admin panel to all active app instances, ensuring
 * real-time UI updates for all users without requiring a restart.
 */
object GlobalStateManager {

    private val db = Firebase.firestore

    // Centralized Flow definitions for real-time reactivity across instances
    private val _devotionals = MutableStateFlow<List<Devotional>>(emptyList())
    val devotionals: StateFlow<List<Devotional>> = _devotionals.asStateFlow()

    private val _churchServices = MutableStateFlow<List<ChurchService>>(emptyList())
    val churchServices: StateFlow<List<ChurchService>> = _churchServices.asStateFlow()

    private val _vipVideos = MutableStateFlow<List<ContentVideo>>(emptyList())
    val vipVideos: StateFlow<List<ContentVideo>> = _vipVideos.asStateFlow()
    
    private val _contentVideos = MutableStateFlow<List<ContentVideo>>(emptyList())
    val contentVideos: StateFlow<List<ContentVideo>> = _contentVideos.asStateFlow()
    
    private val _serviceVideos = MutableStateFlow<List<ServiceVideoModel>>(emptyList())
    val serviceVideos: StateFlow<List<ServiceVideoModel>> = _serviceVideos.asStateFlow()

    private val _events = MutableStateFlow<List<ChurchEvent>>(emptyList())
    val events: StateFlow<List<ChurchEvent>> = _events.asStateFlow()
    
    private val _carouselItems = MutableStateFlow<List<CarouselItem>>(emptyList())
    val carouselItems: StateFlow<List<CarouselItem>> = _carouselItems.asStateFlow()

    fun initializeRealtimeUpdates(context: Context) {
        if (BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
            Log.d("GlobalStateManager", "Firebase not configured, skipping real-time listeners.")
            return
        }
        
        Log.d("GlobalStateManager", "Initializing real-time Firestore listeners to propagate changes across instances.")

        // devocionais are managed by DevotionalManager.syncDevotionals()


        db.collection("cultos_agenda").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(ChurchService::class.java) } catch(ex: Exception) { null } }
            _churchServices.value = list
            weeklyServicesState.clear()
            weeklyServicesState.addAll(list)
        }

        var videosInitialized = false
        db.collection("conteudos_videos").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(ContentVideo::class.java) } catch(ex: Exception) { null } }
            if (!videosInitialized) {
                NotificationHelper.rememberMediaIds(context, list.map { it.id })
                videosInitialized = true
            } else {
                val knownIds = context.getSharedPreferences("micrhema_prefs", android.content.Context.MODE_PRIVATE)
                    .getStringSet("notified_media_ids", emptySet()) ?: emptySet()
                snapshot.documentChanges
                    .filter { it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED && it.document.id !in knownIds }
                    .forEach { change ->
                        NotificationHelper.showNotification(
                            context = context,
                            title = "Novo vídeo em Mídia",
                            message = change.document.getString("title") ?: "Novo vídeo disponível",
                            category = NotificationHelper.Category.MEDIA,
                            respectPreferences = true
                        )
                        NotificationHelper.rememberMediaIds(context, listOf(change.document.id))
                    }
            }
            _contentVideos.value = list
            contentVideosState.clear()
            contentVideosState.addAll(list)
        }
        
        db.collection("vip_videos").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(ContentVideo::class.java) } catch(ex: Exception) { null } }
            _vipVideos.value = list
            vipVideosState.clear()
            vipVideosState.addAll(list)
        }

        db.collection("cultos").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(ServiceVideoModel::class.java) } catch(ex: Exception) { null } }
            _serviceVideos.value = list
            serviceVideosState.clear()
            serviceVideosState.addAll(list)
        }

        db.collection("events").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(ChurchEvent::class.java) } catch(ex: Exception) { null } }
            _events.value = list
            eventsState.clear()
            eventsState.addAll(list)
        }

        db.collection("carousel_items").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(CarouselItem::class.java) } catch(ex: Exception) { null } }
            _carouselItems.value = list
            carouselItemsState.clear()
            carouselItemsState.addAll(list)
        }
        
        Log.d("GlobalStateManager", "Real-time listeners attached successfully.")
    }
}
