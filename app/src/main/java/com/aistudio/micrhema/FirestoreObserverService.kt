package com.aistudio.micrhema

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class FirestoreObserverService : Service() {
    private val listeners = mutableListOf<ListenerRegistration>()
    
    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        observeFirestore()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    private fun startForegroundServiceNotification() {
        val channelId = "micrhema_observer_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Serviço de Observação do App",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sincronizando Conteúdo")
            .setContentText("Aguardando atualizações em tempo real...")
            .setSmallIcon(R.drawable.ic_notification)
            .build()
            
        startForeground(1001, notification)
    }
    
    private fun observeFirestore() {
        val db = FirebaseFirestore.getInstance()
        
        val collections = listOf(
            "content_books" to Pair("Livros", "📚"),
            "content_audios" to Pair("Áudios", "🎧"),
            "content_videos" to Pair("Vídeos", "🎬"),
            "content_albums" to Pair("Álbum de Fotos", "📸"),
            "devotionals" to Pair("Devocionais", "📖"),
            "events" to Pair("Eventos", "📅"),
            "weekly_services" to Pair("Programação da Igreja", "⛪"),
            "prayer_requests" to Pair("Pedidos de Oração", "🙏"),
            "carousel_items" to Pair("Destaques", "✨")
        )

        // Listen for new members (only alert if current user is admin)
        var isFirstMember = true
        val memberReg = db.collection("members").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            if (!isFirstMember) {
                val added = snapshot.documentChanges.count { it.type == DocumentChange.Type.ADDED }
                if (added > 0) {
                    val isAdmin = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
                    
                    if (isAdmin) {
                        val unapproved = snapshot.documentChanges.filter { it.type == DocumentChange.Type.ADDED && it.document.getBoolean("isApproved") == false }
                        if (unapproved.isNotEmpty()) {
                            NotificationHelper.showNotification(
                                this,
                                "👤 Novo Pedido de Acesso",
                                "Existem ${unapproved.size} nova(s) solicitação(ões) de acesso aguardando aprovação."
                            )
                        }
                    }
                }
            }
            isFirstMember = false
        }
        listeners.add(memberReg)
        
        for ((collection, pair) in collections) {
            val name = pair.first
            val emoji = pair.second
            var isFirst = true
            val reg = db.collection(collection).addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                if (!isFirst) {
                    val added = snapshot.documentChanges.count { it.type == DocumentChange.Type.ADDED }
                    val modified = snapshot.documentChanges.count { it.type == DocumentChange.Type.MODIFIED }
                    val removed = snapshot.documentChanges.count { it.type == DocumentChange.Type.REMOVED }
                    
                    if (added > 0 || modified > 0 || removed > 0) {
                        NotificationHelper.showNotification(
                            this,
                            "$emoji $name Atualizado",
                            "O conteúdo foi modificado (Novos: $added, Editados: $modified, Removidos: $removed)"
                        )
                    }
                }
                isFirst = false
            }
            listeners.add(reg)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        listeners.forEach { it.remove() }
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
