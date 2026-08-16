package com.aistudio.micrhema
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob


import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import android.util.Log

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

private val dataSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

object DevotionalManager {
    fun syncDevotionals(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
        try {
            val cachedDevotionals = IbrDatabaseHelper(context).getCachedDevotionals()
            if (cachedDevotionals.isNotEmpty()) {
                devotionalsState.clear()
                devotionalsState.addAll(cachedDevotionals)
            }
            if (isOfflineModeState.value) return
            
            scope.launch {
                DevotionalRepository.getDevotionalsFlow().collect { newList ->
                    if (newList.isNotEmpty()) {
                        val sorted = newList.sortedByDescending { it.timestamp }
                        devotionalsState.clear()
                        devotionalsState.addAll(sorted)
                        
                        IbrDatabaseHelper(context).saveCachedDevotionals(sorted)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DevotionalManager", "Firestore error", e)
        }
    }
}

fun loadDevotionalsFromJson(context: Context) {
    val dbHelper = IbrDatabaseHelper(context)
    try {
        // Simulating a network fetch by reading from assets
        val inputStream: java.io.InputStream = context.assets.open("devotionals.json")
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        val jsonString = String(buffer, Charsets.UTF_8)
        val jsonArray = org.json.JSONArray(jsonString)
        
        val fetchedList = mutableListOf<Devotional>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val dev = Devotional(
                id = jsonObject.getString("id"),
                title = jsonObject.getString("title"),
                date = jsonObject.getString("date"),
                verse = jsonObject.getString("verse"),
                verseReference = jsonObject.getString("verseReference"),
                content = jsonObject.getString("content")
            )
            fetchedList.add(dev)
        }
        
        // Cache the newly fetched devotionals
        if (fetchedList.isNotEmpty()) {
            dbHelper.saveCachedDevotionals(fetchedList)
        }
        
        devotionalsState.clear()
        devotionalsState.addAll(fetchedList)
    } catch (e: Exception) {
        e.printStackTrace()
        // If "network" fails, fallback to local cache
        val cachedDevotionals = dbHelper.getCachedDevotionals()
        if (cachedDevotionals.isNotEmpty()) {
            devotionalsState.clear()
            devotionalsState.addAll(cachedDevotionals)
        }
    }
}

data class Devotional(
    var id: String = "",
    var title: String = "",
    var date: String = "",
    var verse: String = "",
    var verseReference: String = "",
    var content: String = "",
    var likes: Int = 0,
    var type: String = "devocional",
    var mediaUrl: String = "",
    var isApproved: Boolean = true,
    var timestamp: Long = System.currentTimeMillis()
)

data class ChurchService(
    var id: String = "",
    var day: String = "",
    var dayShort: String = "",
    var time: String = "",
    var title: String = "",
    var description: String = "",
    var type: String = "culto",
    var content: String = "",
    var mediaUrl: String = "",
    var isApproved: Boolean = true
)

data class ChurchEvent(
    var id: String = "",
    var title: String = "",
    var date: String = "",
    var description: String = "",
    var location: String = ""
)

data class PrayerRequest(
    var id: String = "",
    var name: String = "",
    var request: String = "",
    var date: String = ""
)

data class CarouselItem(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var date: String = "",
    var tag: String = "", // "EVENTO" ou "NOTÍCIA"
    var imageUrl: String? = null,
    var eventDate: String = "", // Formato: yyyy-MM-dd
    var eventInfo: String = "" // Informação explícita que libera o clique do evento
)

// Global mutable states
val carouselItemsState = mutableStateListOf<CarouselItem>(
    CarouselItem(
        id = "1",
        title = "Inauguração do Novo Templo",
        description = "Venha celebrar conosco a abertura do nosso novo espaço de adoração e comunhão com toda a igreja.",
        date = "2026-07-20",
        tag = "EVENTO"
    ),
    CarouselItem(
        id = "2",
        title = "Campanha do Agasalho",
        description = "Estamos arrecadando cobertores e roupas de frio para doar às famílias em situação de vulnerabilidade de nossa cidade.",
        date = "2026-07-25",
        tag = "NOTÍCIA"
    ),
    CarouselItem(
        id = "3",
        title = "Escola de Líderes 2026",
        description = "Inscrições abertas para o novo curso de formação ministerial e capacitação espiritual.",
        date = "2026-08-01",
        tag = "NOTÍCIA"
    )
)

val prayerRequestsState = mutableStateListOf<PrayerRequest>(
    PrayerRequest(
        id = "1",
        name = "Maria Souza",
        request = "Pela saúde da minha família e restauração do meu casamento.",
        date = "2026-07-13"
    ),
    PrayerRequest(
        id = "2",
        name = "João Silva",
        request = "Agradecimento pela porta de emprego aberta e oração para que tudo corra bem no novo trabalho.",
        date = "2026-07-12"
    )
)

val devotionalsState = mutableStateListOf<Devotional>(
    Devotional(
        id = "1",
        title = "Co-Herdeiros",
        date = "2026-05-21",
        verse = "O mesmo Espírito testifica com o nosso espírito que somos filhos de Deus. E, se nós somos filhos, somos, logo, herdeiros também, herdeiros de Deus e co-herdeiros de Cristo.",
        verseReference = "Romanos 8:16-17",
        content = "Você acha que as pessoas que viviam segundo a Antiga Aliança podiam ser mais abençoadas do que aquelas que estão na Igreja do Senhor Jesus Cristo?\n\nVocê acha que a Igreja, o Corpo de Cristo, o Corpo do Filho de Deus, precisa debater-se na vida, empobrecida, emaciada, desgastada pela fome, doença e aflição?\n\nFora com tais ideias!\n\nA Bíblia declara que somos co-herdeiros com Cristo! Filhos de Deus! Estamos no Reino de Deus!\n\nNão somos mendigos! Somos novas criaturas. Somos bem-aventurados acima de todas as pessoas.\n\nConfissão: \"O próprio Espírito Santo testifica com o meu espírito que sou um filho de Deus. Deus é meu Pai. Eu sou Seu filho. Posto que sou o Seu filho, logo, sou o Seu herdeiro. Sou um herdeiro de Deus, o Criador do Universo e co-herdeiro com Jesus Cristo!\""
    ),
    Devotional(
        id = "2",
        title = "Para Meu Benefício",
        date = "2026-05-20",
        verse = "E amar-te-á, e abençoar-te-á, e te fará multiplicar... E o SENHOR de ti desviará toda enfermidade; sobre ti não porá nenhuma das más doenças dos egípcios.",
        verseReference = "Deuteronômio 7:13-15",
        content = "E amar-te-á... Amar-te-á! Amar-te-á!\n\nEle porá enfermidade sobre você? Fará você morrer ainda criança? Não! Não! Não! As Sagradas Escrituras não ensinam dessa forma!\n\nA Primeira Carta de Paulo aos Coríntios está no Novo Testamento. Examinemos 1 Coríntios 10:11: \"Ora, tudo isso lhes sobreveio como figuras, e estão escritas para aviso nosso, para quem já são chegados os fins dos séculos.\"\n\nGlória! Deuteronômio 7:13-15 foi escrito para meu benefício. Foi escrito para minha advertência!\n\nConfissão: \"O Senhor me ama. Ele me abençoa. Ele abençoa os meus filhos. O Senhor desvia de mim toda enfermidade. Sou abençoado mais do que todos os povos!\""
    ),
    Devotional(
        id = "3",
        title = "A Força da Comunhão",
        date = "2026-05-18",
        verse = "Não deixemos de congregar-nos, como é costume de alguns; antes, façamos admoestações e tanto mais quanto vedes que o Dia se aproxima.",
        verseReference = "Hebreus 10:25",
        content = "A vida cristã não foi projetada para ser vivida em isolamento. Deus nos colocou em comunidade porque sabe que precisamos uns dos outros. Na comunhão, somos fortalecidos, corrigidos, encorajados e edificados.\n\nA igreja não é um prédio, é um corpo vivo onde cada membro tem função e valor. Quando nos afastamos da comunhão, ficamos vulneráveis. Quando nos aproximamos, somos fortalecidos.\n\nValoize sua igreja, seus irmãos e o tempo que passam juntos adorando ao Senhor. Cada culto é uma oportunidade de receber uma palavra rhema — específica, pessoal e transformadora — diretamente de Deus para a sua vida."
    ),
    Devotional(
        id = "4",
        title = "Palavra Viva",
        date = "2026-05-17",
        verse = "Porque a palavra de Deus é viva, e eficaz, e mais cortante do que qualquer espada de dois gumes.",
        verseReference = "Hebreus 4:12",
        content = "A Palavra de Deus não é um livro comum. Ela é viva e ativa, capaz de transformar realidades, curar feridas e direcionar caminhos. Quando lemos a Bíblia com fé, algo sobrenatural acontece: o Espírito Santo fala diretamente ao nosso coração.\n\nÉ o rhema — a palavra específica de Deus para o seu momento. Não deixe a Bíblia fechada. Abra-a com expectativa, leia com fé e permita que ela penetre nas profundezas do seu ser.\n\nA Palavra tem poder para mudar sua história hoje. Cada versículo que você lê com fé é uma semente plantada no solo do seu coração, pronta para germinar e produzir frutos em abundância."
    )
)

val weeklyServicesState = mutableStateListOf<ChurchService>(
    ChurchService(
        id = "1",
        day = "Terça-feira",
        dayShort = "TER",
        time = "19:00",
        title = "Culto de Ensino",
        description = "Estudo aprofundado da Palavra de Deus para edificação e crescimento espiritual."
    ),
    ChurchService(
        id = "2",
        day = "Quinta-feira",
        dayShort = "QUI",
        time = "19:00",
        title = "Culto de Oração",
        description = "Momento de intercessão, louvor e busca pela presença de Deus."
    ),
    ChurchService(
        id = "3",
        day = "Domingo",
        dayShort = "DOM",
        time = "18:30",
        title = "Culto de Celebração",
        description = "Celebração com louvor, adoração e ministração da Palavra."
    )
)

val eventsState = mutableStateListOf<ChurchEvent>(
    ChurchEvent(
        id = "1",
        title = "Discipulado MIC Rhema",
        date = "2026-05-24",
        description = "Encontro de discipulado para novos membros e interessados em crescer na fé.",
        location = "Igreja MIC Rhema"
    ),
    ChurchEvent(
        id = "2",
        title = "Noite de Louvor",
        date = "2026-05-31",
        description = "Uma noite especial dedicada à adoração e louvor ao Senhor.",
        location = "Igreja MIC Rhema"
    ),
    ChurchEvent(
        id = "3",
        title = "Encontro de Jovens",
        date = "2026-06-07",
        description = "Momento de comunhão, palavra e atividades para a juventude da igreja.",
        location = "Igreja MIC Rhema"
    )
)

// Dynamic home states
val palavraDoDiaVerse = mutableStateOf("\"Porque Deus amou o mundo de tal maneira que deu o seu Filho unigênito, para que todo aquele que nele crê não pereça, mas tenha a vida eterna.\"")
val palavraDoDiaRef = mutableStateOf("João 3:16")

// Dynamic next service states
val proximoCultoDayShort = mutableStateOf("DOM")
val proximoCultoTime = mutableStateOf("18:30")
val proximoCultoTitle = mutableStateOf("Culto de Celebração")
val proximoCultoDayFull = mutableStateOf("Domingo")

// Dynamic about states
val pastorNameState = mutableStateOf("Pr. Nome do Pastor")
val pastorTitleState = mutableStateOf("Pastor Presidente")
val missionTaglineState = mutableStateOf("\"Alcançando Vidas, Restaurando Famílias\"")
val rhemaMeaningState = mutableStateOf("Rhema é a palavra revelada de Deus, falada diretamente ao nosso coração no tempo presente.")

data class MemberRequest(
    var id: String = "",
    var firebaseUid: String = "",
    var name: String = "",
    var ibrCertificateName: String = "",
    var phone: String = "",
    var email: String = "",
    var isApproved: Boolean = false,
    var isVip: Boolean = false,
    var isIbr: Boolean = false,
    var isAdmin: Boolean = false,
    var ibrCertificateUrl: String = "",
    var ibrCertificateStoragePath: String = "",
    var status: String = "pendente",
    var title: String = "",
    var type: String = "acesso",
    var content: String = "",
    var mediaUrl: String = "",
    var profilePhotoUrl: String = "",
    var avatarId: String = DEFAULT_BIBLICAL_AVATAR_ID,
    var unlockedBadgeIds: List<String> = listOf(DEFAULT_BIBLICAL_BADGE_ID),
    var equippedBadgeId: String = DEFAULT_BIBLICAL_BADGE_ID,
    var supabaseStoragePath: String = "",
    var address: String = "",
    var birthDate: String = "",
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L
)

val memberRequestsState = mutableStateListOf<MemberRequest>(
    MemberRequest(
        id = "1",
        name = "Carlos Oliveira",
        phone = "11999999999",
        isApproved = true,
        isVip = false,
        isIbr = false
    ),
    MemberRequest(
        id = "2",
        name = "Ana Costa",
        phone = "11888888888",
        isApproved = true,
        isVip = false,
        isIbr = true
    ),
    MemberRequest(
        id = "3",
        name = "Marcos Souza",
        phone = "11777777777",
        isApproved = false,
        isVip = false,
        isIbr = false
    )
)

val loggedInMemberState = mutableStateOf<MemberRequest?>(null)
val adminAuthenticatedState = androidx.compose.runtime.mutableStateOf(false)

object MemberManager {
    private const val PREFS_NAME = "micrhema_members_prefs"
    private const val KEY_MEMBERS = "members_list"
    private const val KEY_LOGGED_IN_ID = "logged_in_member_id"
    private var membersListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun stopSync() {
        membersListener?.remove()
        membersListener = null
    }

    fun syncFromFirestore(context: android.content.Context) {
        stopSync()
        try {
            val db = Firebase.firestore

            membersListener = db.collection("acessos_pendentes").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val newList = mutableListOf<MemberRequest>()
                for (document in snapshot.documents) {
                    val id = document.id
                    val firebaseUid = document.getString("firebaseUid") ?: ""
                    val name = document.getString("name") ?: ""
                    val ibrCertificateName = document.getString("ibrCertificateName") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val rawIsApproved = document.getBoolean("isApproved") ?: false
                    val rawIsVip = document.getBoolean("isVip") ?: false
                    val effectiveApproved = rawIsApproved || rawIsVip
                    val isVip = false
                    val isApproved = effectiveApproved
                    val isIbr = document.getBoolean("isIbr") ?: false
                    val email = document.getString("email") ?: ""
                    val isAdmin = document.getBoolean("isAdmin") ?: false
                    val ibrCertificateUrl = document.getString("ibrCertificateUrl") ?: ""
                    val ibrCertificateStoragePath = document.getString("ibrCertificateStoragePath") ?: ""
                    val status = document.getString("status") ?: if (isApproved || isIbr) "aprovado" else "pendente"
                    val title = document.getString("title") ?: ""
                    val type = document.getString("type") ?: "acesso"
                    val content = document.getString("content") ?: ""
                    val mediaUrl = document.getString("mediaUrl") ?: ""
                    val remoteProfilePhotoUrl = document.getString("profilePhotoUrl") ?: ""
                    val avatarId = document.getString("avatarId").orEmpty().ifBlank { DEFAULT_BIBLICAL_AVATAR_ID }
                    val supabaseStoragePath = document.getString("supabaseStoragePath") ?: ""
                    val profilePhotoUrl = resolveProfilePhotoUrl(context, id, remoteProfilePhotoUrl, supabaseStoragePath)
                    val address = document.getString("address") ?: ""
                    val birthDate = document.getString("birthDate") ?: ""
                    val createdAt = document.getLong("createdAt") ?: 0L
                    val updatedAt = document.getLong("updatedAt") ?: 0L
                                            newList.add(MemberRequest(
                        id = id,
                        firebaseUid = firebaseUid,
                        name = name,
                        ibrCertificateName = ibrCertificateName,
                        phone = phone,
                        email = email,
                        isApproved = isApproved,
                        isVip = isVip,
                        isIbr = isIbr,
                        isAdmin = isAdmin,
                        ibrCertificateUrl = ibrCertificateUrl,
                        ibrCertificateStoragePath = ibrCertificateStoragePath,
                        status = status,
                        title = title,
                        type = type,
                        content = content,
                        mediaUrl = mediaUrl,
                        profilePhotoUrl = profilePhotoUrl,
                        avatarId = avatarId,

                        supabaseStoragePath = supabaseStoragePath,
                        address = address,
                        birthDate = birthDate,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    ))
                }
                memberRequestsState.clear()
                memberRequestsState.addAll(newList)
                saveMembers(context)
                refreshSignedStorageUrls(context, newList)

                val loggedInId = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                        .getString(KEY_LOGGED_IN_ID, "") ?: ""
                if (loggedInId.isNotEmpty()) {
                    val member = memberRequestsState.find { it.id == loggedInId }
                    loggedInMemberState.value = member
                    if (member != null) loadIbrProgressFromFirestore()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }
    
    fun deleteFromFirestore(context: android.content.Context, member: MemberRequest) {
        if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) return
        try {
            val db = Firebase.firestore
            db.collection("acessos_pendentes").document(member.id).delete()
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }

    private fun resolveProfilePhotoUrl(
        context: android.content.Context,
        memberId: String,
        remoteUrl: String,
        storagePath: String
    ): String {
        if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) return remoteUrl
        val localUrl = StorageManager.getLocalProfilePhotoUri(context, memberId)
        return localUrl.ifBlank { if (remoteUrl.startsWith("file://") || storagePath.isNotBlank()) "" else remoteUrl }
    }

    private fun refreshSignedStorageUrls(context: android.content.Context, members: List<MemberRequest>) {
        dataSyncScope.launch {
            members.forEach { member ->
                val profileUrl = if (member.supabaseStoragePath.isNotBlank()) {
                    runCatching {
                        StorageManager.getSignedUrl("profile-photos", member.supabaseStoragePath, member.id, context)
                    }.getOrNull().orEmpty()
                } else ""
                val certificateUrl = if (member.ibrCertificateStoragePath.isNotBlank()) {
                    runCatching {
                        StorageManager.getSignedUrl("church-documents", member.ibrCertificateStoragePath, member.id, context)
                    }.getOrNull().orEmpty()
                } else ""
                if (profileUrl.isBlank() && certificateUrl.isBlank()) return@forEach
                kotlinx.coroutines.withContext(Dispatchers.Main.immediate) {
                    val index = memberRequestsState.indexOfFirst { it.id == member.id }
                    if (index >= 0) {
                        val current = memberRequestsState[index]
                        val updated = current.copy(
                            profilePhotoUrl = if (profileUrl.isNotBlank() && current.supabaseStoragePath == member.supabaseStoragePath) profileUrl else current.profilePhotoUrl,
                            ibrCertificateUrl = if (certificateUrl.isNotBlank() && current.ibrCertificateStoragePath == member.ibrCertificateStoragePath) certificateUrl else current.ibrCertificateUrl
                        )
                        memberRequestsState[index] = updated
                        if (loggedInMemberState.value?.id == member.id) loggedInMemberState.value = updated.copy()
                        saveMembers(context)
                    }
                }
            }
        }
    }

    fun saveToFirestore(context: android.content.Context, member: MemberRequest, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val remoteProfilePhotoUrl = member.profilePhotoUrl.takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        }.orEmpty()
        saveMembers(context)
        if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
            onSuccess()
            return
        }
        try {
            val db = Firebase.firestore

            val memberMap = hashMapOf<String, Any>(
                "name" to member.name,
                "firebaseUid" to member.firebaseUid,
                "ibrCertificateName" to member.ibrCertificateName,
                "phone" to member.phone,
                "email" to member.email,
                "isApproved" to member.isApproved,
                "isVip" to false,
                "isIbr" to member.isIbr,
                "isAdmin" to member.isAdmin,
                "ibrCertificateUrl" to if (member.ibrCertificateStoragePath.isBlank()) member.ibrCertificateUrl else "",
                "ibrCertificateStoragePath" to member.ibrCertificateStoragePath,
                "status" to member.status.ifBlank { if (member.isApproved || member.isIbr) "aprovado" else "pendente" },
                "title" to member.title,
                "type" to member.type,
                "content" to member.content,
                "mediaUrl" to member.mediaUrl,
                "avatarId" to member.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID },
                "unlockedBadgeIds" to member.unlockedBadgeIds.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) },
                "equippedBadgeId" to member.equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID },
                "address" to member.address,
                "birthDate" to member.birthDate,
                "createdAt" to member.createdAt,
                "updatedAt" to member.updatedAt,
                "supabaseStoragePath" to member.supabaseStoragePath
            )
            if (member.supabaseStoragePath.isNotBlank()) {
                memberMap["profilePhotoUrl"] = ""
            } else if (member.profilePhotoUrl.isBlank() || remoteProfilePhotoUrl.isNotBlank()) {
                memberMap["profilePhotoUrl"] = remoteProfilePhotoUrl
            }
            db.collection("acessos_pendentes").document(member.id).set(memberMap, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { 
                    Log.d("MemberManager", "Document successfully written!") 
                    onSuccess()
                }
                .addOnFailureListener { e -> 
                    Log.w("MemberManager", "Error writing document", e)
                    onFailure(e)
                }
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
            onFailure(e)
        }
    }

    fun loadMembers(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val serialized = prefs.getString(KEY_MEMBERS, "") ?: ""
        if (serialized.isNotEmpty()) {
            try {
                val jsonArray = org.json.JSONArray(serialized)
                val list = mutableListOf<MemberRequest>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        MemberRequest(
                            id = obj.optString("id", ""),
                            firebaseUid = obj.optString("firebaseUid", ""),
                            name = obj.optString("name", ""),
                            ibrCertificateName = obj.optString("ibrCertificateName", ""),
                            phone = obj.optString("phone", ""),
                            email = obj.optString("email", ""),
                            isApproved = obj.optBoolean("isApproved", false) || obj.optBoolean("isVip", false),
                            isVip = false,
                            isIbr = obj.optBoolean("isIbr", false),
                            isAdmin = obj.optBoolean("isAdmin", false),
                            ibrCertificateUrl = obj.optString("ibrCertificateUrl", ""),
                            ibrCertificateStoragePath = obj.optString("ibrCertificateStoragePath", ""),
                            status = obj.optString("status", "pendente"),
                            title = obj.optString("title", ""),
                            type = obj.optString("type", "acesso"),
                            content = obj.optString("content", ""),
                            mediaUrl = obj.optString("mediaUrl", ""),
                            profilePhotoUrl = obj.optString("profilePhotoUrl", ""),
                            avatarId = obj.optString("avatarId", DEFAULT_BIBLICAL_AVATAR_ID).ifBlank { DEFAULT_BIBLICAL_AVATAR_ID },
                            unlockedBadgeIds = obj.optJSONArray("unlockedBadgeIds")?.let { array ->
                                List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
                            }?.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) } ?: listOf(DEFAULT_BIBLICAL_BADGE_ID),
                            equippedBadgeId = obj.optString("equippedBadgeId", DEFAULT_BIBLICAL_BADGE_ID).ifBlank { DEFAULT_BIBLICAL_BADGE_ID },
                            supabaseStoragePath = obj.optString("supabaseStoragePath", ""),
                            address = obj.optString("address", ""),
                            birthDate = obj.optString("birthDate", ""),
                            createdAt = obj.optLong("createdAt", 0L),
                            updatedAt = obj.optLong("updatedAt", 0L)
                        )
                    )
                }
                if (list.isNotEmpty()) {
                memberRequestsState.clear()
                memberRequestsState.addAll(list)
            }
            } catch (e: Exception) {
                e.printStackTrace()
                // Se der erro no formato antigo, limpa para não mostrar lixo
                prefs.edit().remove(KEY_MEMBERS).apply()
                memberRequestsState.clear()
            }
        }
        
        val loggedInId = prefs.getString(KEY_LOGGED_IN_ID, "") ?: ""
        if (loggedInId.isNotEmpty()) {
            val member = memberRequestsState.find { it.id == loggedInId }
            if (member != null) {
                setLoggedInMember(context, member)
            }
        }
    }

    fun saveMembers(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        try {
            val jsonArray = org.json.JSONArray()
            memberRequestsState.forEach { member ->
                val obj = org.json.JSONObject()
                obj.put("id", member.id)
                obj.put("firebaseUid", member.firebaseUid)
                obj.put("name", member.name)
                obj.put("ibrCertificateName", member.ibrCertificateName)
                obj.put("phone", member.phone)
                obj.put("email", member.email)
                obj.put("isApproved", member.isApproved)
                obj.put("isVip", false)
                obj.put("isIbr", member.isIbr)
                obj.put("isAdmin", member.isAdmin)
                obj.put("ibrCertificateUrl", member.ibrCertificateUrl)
                obj.put("ibrCertificateStoragePath", member.ibrCertificateStoragePath)
                obj.put("status", member.status)
                obj.put("title", member.title)
                obj.put("type", member.type)
                obj.put("content", member.content)
                obj.put("mediaUrl", member.mediaUrl)
                obj.put("profilePhotoUrl", member.profilePhotoUrl)
                obj.put("avatarId", member.avatarId.ifBlank { DEFAULT_BIBLICAL_AVATAR_ID })
                obj.put("unlockedBadgeIds", org.json.JSONArray(member.unlockedBadgeIds.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }))
                obj.put("equippedBadgeId", member.equippedBadgeId.ifBlank { DEFAULT_BIBLICAL_BADGE_ID })
                obj.put("supabaseStoragePath", member.supabaseStoragePath)
                obj.put("address", member.address)
                obj.put("birthDate", member.birthDate)
                obj.put("createdAt", member.createdAt)
                obj.put("updatedAt", member.updatedAt)
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_MEMBERS, jsonArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun bindFirebaseUidToLoggedInMember(context: android.content.Context, firebaseUid: String) {
        val member = loggedInMemberState.value ?: return
        if (firebaseUid.isBlank() || member.firebaseUid == firebaseUid) return

        member.firebaseUid = firebaseUid
        val index = memberRequestsState.indexOfFirst { it.id == member.id }
        if (index >= 0) memberRequestsState[index] = member.copy()
        loggedInMemberState.value = member.copy()
        saveMembers(context)

        if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
            Firebase.firestore.collection("acessos_pendentes").document(member.id)
                .set(mapOf("firebaseUid" to firebaseUid), com.google.firebase.firestore.SetOptions.merge())
                .await()
        }
    }

    fun setLoggedInMember(context: android.content.Context, member: MemberRequest?) {
        loggedInMemberState.value = member
        loadIbrProgressFromFirestore()
        context.getSharedPreferences("micrhema_member_session", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isIbr", member?.isIbr == true)
            .putString("memberId", member?.id.orEmpty())
            .apply()
        runCatching {
            val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            if (member?.isIbr == true) messaging.subscribeToTopic("ibr_users")
            else messaging.unsubscribeFromTopic("ibr_users")
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (member == null) {
            prefs.edit().remove(KEY_LOGGED_IN_ID).apply()
            runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() }
        } else {
            prefs.edit().putString(KEY_LOGGED_IN_ID, member.id).apply()
            UserSettingsManager.loadSettings(context)
            dataSyncScope.launch {
                runCatching {
                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val firebaseUser = auth.currentUser
                        ?: auth.signInAnonymously().await().user
                    firebaseUser?.let { bindFirebaseUidToLoggedInMember(context, it.uid) }
                }.onFailure { error ->
                    Log.w("MemberManager", "Não foi possível preparar a sessão Firebase do perfil", error)
                }
            }
        }
    }
}


data class IbrChapter(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var durationMinutes: Int = 0,
    var type: String = "VIDEO", // VIDEO, AUDIO, TEXT
    var videoUrl: String = "", // URL to video stream or YouTube
    var audioUrl: String = "", // URL to audio stream
    var textContent: String = "", // For TEXT type
    var isYoutube: Boolean = false,
    var youtubeId: String = "" // if Youtube link
)

data class IbrCourse(
    var id: String = "",
    var title: String = "",
    var theme: String = "", // e.g., "Teologia", "História Bíblica", "Vida Cristã"
    var description: String = "",
    var imageUrl: String = "",
    var chapters: List<IbrChapter> = emptyList()
)

data class IbrProgress(
    var courseId: String = "",
    var chapterId: String = "",
    var lastPositionSeconds: Int = 0,
    var totalDurationSeconds: Int = 0,
    var isCompleted: Boolean = false
)

val ibrCoursesState = mutableStateListOf<IbrCourse>()

val ibrProgressState = mutableStateListOf<IbrProgress>()



enum class ThemeMode { SYSTEM, LIGHT, DARK }

object SettingsManager {
    private const val PREFS_NAME = "micrhema_settings_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_OFFLINE_MODE = "offline_mode"

    fun getThemeMode(context: android.content.Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try { ThemeMode.valueOf(modeStr) } catch (e: Exception) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(context: android.content.Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun isOfflineMode(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_OFFLINE_MODE, false)
    }

    fun setOfflineMode(context: android.content.Context, isOffline: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_OFFLINE_MODE, isOffline).apply()
    }
}

val currentThemeMode = androidx.compose.runtime.mutableStateOf(ThemeMode.SYSTEM)
val isOfflineModeState = androidx.compose.runtime.mutableStateOf(false)

data class AdminAppSettings(
    var notificationsEnabled: Boolean = true,
    var showDonationsTab: Boolean = true,
    var updatedAt: Long = 0L
)

val adminAppSettingsState = androidx.compose.runtime.mutableStateOf(AdminAppSettings())

fun loadAdminAppSettings() {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) return
    com.google.firebase.Firebase.firestore.collection("settings").document("app")
        .addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            val settings = AdminAppSettings(
                notificationsEnabled = snapshot.getBoolean("notificationsEnabled") ?: true,
                showDonationsTab = snapshot.getBoolean("showDonationsTab") ?: true,
                updatedAt = snapshot.getLong("updatedAt") ?: 0L
            )
            adminAppSettingsState.value = settings
            runCatching {
                com.google.firebase.FirebaseApp.getInstance().applicationContext
                    .getSharedPreferences("micrhema_admin_settings", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("notificationsEnabled", settings.notificationsEnabled)
                    .apply()
            }
        }
}

fun saveAdminAppSettings(
    settings: AdminAppSettings,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    val persistedSettings = settings.copy(updatedAt = System.currentTimeMillis())
    adminAppSettingsState.value = persistedSettings
    runCatching {
        com.google.firebase.FirebaseApp.getInstance().applicationContext
            .getSharedPreferences("micrhema_admin_settings", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("notificationsEnabled", persistedSettings.notificationsEnabled)
            .apply()
    }
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
        onFailure(IllegalStateException("Firebase não configurado"))
        return
    }
    com.google.firebase.Firebase.firestore.collection("settings").document("app").set(
        mapOf(
            "notificationsEnabled" to persistedSettings.notificationsEnabled,
            "showDonationsTab" to persistedSettings.showDonationsTab,
            "updatedAt" to persistedSettings.updatedAt
        ),
        com.google.firebase.firestore.SetOptions.merge()
    ).addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}

data class ContentBook(
    var id: String = "",
    var title: String = "",
    var author: String = "",
    var coverUrl: String = "",
    var contentText: String = "",
    var bookUrl: String = "",
    var isCached: Boolean = false,
    var progress: Float = 0f,
    var lastPosition: Long = 0L,
    var type: String = "livro",
    var content: String = "",
    var mediaUrl: String = "",
    var isApproved: Boolean = true
)

data class ContentAudio(
    var id: String = "",
    var title: String = "",
    var artist: String = "",
    var audioUrl: String = "",
    var coverUrl: String = "",
    var isCached: Boolean = false,
    var progress: Float = 0f,
    var lastPosition: Long = 0L,
    var type: String = "audio",
    var content: String = "",
    var mediaUrl: String = "",
    var isApproved: Boolean = true
)

data class ContentVideo(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var videoUrl: String = "",
    var thumbnailUrl: String = "",
    var isCached: Boolean = false,
    var progress: Float = 0f,
    var lastPosition: Long = 0L,
    var type: String = "video",
    var content: String = "",
    var mediaUrl: String = "",
    var isApproved: Boolean = true
)

data class AlbumPhoto(
    var url: String = "",
    var caption: String = ""
)

data class ContentPhotoAlbum(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var coverUrl: String? = null,
    var photos: List<AlbumPhoto> = emptyList(),
    var driveFolderUrl: String = ""
)


val vipBooksState = androidx.compose.runtime.mutableStateListOf<ContentBook>()
val vipAudiosState = androidx.compose.runtime.mutableStateListOf<ContentAudio>()
val vipVideosState = androidx.compose.runtime.mutableStateListOf<ContentVideo>()
val vipAlbumsState = androidx.compose.runtime.mutableStateListOf<ContentPhotoAlbum>()
val vipCoursesState = androidx.compose.runtime.mutableStateListOf<IbrCourse>()

val contentBooksState = androidx.compose.runtime.mutableStateListOf<ContentBook>()
val contentAudiosState = androidx.compose.runtime.mutableStateListOf<ContentAudio>()
val contentVideosState = androidx.compose.runtime.mutableStateListOf<ContentVideo>()
val contentAlbumsState = androidx.compose.runtime.mutableStateListOf<ContentPhotoAlbum>()
val serviceVideosState = androidx.compose.runtime.mutableStateListOf<ServiceVideoModel>()

fun loadContentFromFirebase(context: Context) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        try {
            val db = Firebase.firestore
            GlobalStateManager.initializeRealtimeUpdates(context)
            loadAdminAppSettings()

            // FREE CONTENT
            db.collection("conteudos_books").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentBook::class.java) } catch(ex: Exception) { null } }
                contentBooksState.clear()
                    contentBooksState.addAll(list)
            }
            db.collection("settings").document("sync_trigger").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                dataSyncScope.launch {
                    try {
                        forceRefreshData()
                    } catch (refreshError: Exception) {
                        Log.e("Data", "Falha ao atualizar dados após sync_trigger", refreshError)
                    }
                }
            }
            var audiosInitialized = false
            db.collection("conteudos_audios").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentAudio::class.java) } catch(ex: Exception) { null } }
                if (!audiosInitialized) {
                    NotificationHelper.rememberMediaIds(context, list.map { it.id })
                    audiosInitialized = true
                } else {
                    val knownIds = context.getSharedPreferences("micrhema_prefs", Context.MODE_PRIVATE)
                        .getStringSet("notified_media_ids", emptySet()) ?: emptySet()
                    snapshot.documentChanges
                        .filter { it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED && it.document.id !in knownIds }
                        .forEach { change ->
                            NotificationHelper.showNotification(
                                context = context,
                                title = "Novo áudio em Mídia",
                                message = change.document.getString("title") ?: "Novo áudio disponível",
                                category = NotificationHelper.Category.MEDIA,
                                respectPreferences = true
                            )
                            NotificationHelper.rememberMediaIds(context, listOf(change.document.id))
                        }
                }
                contentAudiosState.clear()
                contentAudiosState.addAll(list)
            }
                        db.collection("conteudos_albums").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentPhotoAlbum::class.java) } catch(ex: Exception) { null } }
                contentAlbumsState.clear()
                    contentAlbumsState.addAll(list)
            }
            
            // IBR CONTENT
            db.collection("vip_books").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentBook::class.java) } catch(ex: Exception) { null } }
                vipBooksState.clear()
                    vipBooksState.addAll(list)
            }
            db.collection("vip_audios").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentAudio::class.java) } catch(ex: Exception) { null } }
                vipAudiosState.clear()
                    vipAudiosState.addAll(list)
            }
                        db.collection("vip_albums").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentPhotoAlbum::class.java) } catch(ex: Exception) { null } }
                vipAlbumsState.clear()
                    vipAlbumsState.addAll(list)
            }
            db.collection("vip_courses").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(IbrCourse::class.java) } catch(e: Exception) { null } }
            vipCoursesState.clear()
                    vipCoursesState.addAll(list)
        }
        
        db.collection("settings").document("about").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            pastorNameState.value = snapshot.getString("pastorName") ?: pastorNameState.value
            pastorTitleState.value = snapshot.getString("pastorTitle") ?: pastorTitleState.value
            missionTaglineState.value = snapshot.getString("missionTagline") ?: missionTaglineState.value
            rhemaMeaningState.value = snapshot.getString("rhemaMeaning") ?: rhemaMeaningState.value
        }
        
        db.collection("app_tabs").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(AppTab::class.java) } catch(ex: Exception) { null } }
            val normalizedTabs = ensureDiscipuladoTab(list)
            appTabsState.clear()
                appTabsState.addAll(normalizedTabs.sortedBy { it.order })
        }
        
        db.collection("equipe").orderBy("order").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(TeamMember::class.java) } catch(ex: Exception) { null } }
            teamMembersState.clear()
                    teamMembersState.addAll(list)
        }
        
                
                
                
                
        db.collection("prayer_requests").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(PrayerRequest::class.java) } catch(ex: Exception) { null } }
            prayerRequestsState.clear()
                    prayerRequestsState.addAll(list)
        }
        
        db.collection("ibr_courses").addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(IbrCourse::class.java) } catch(ex: Exception) { null } }
            ibrCoursesState.clear()
                    ibrCoursesState.addAll(list)
        }
        
                
    } catch (e: Exception) {
        android.util.Log.e("Data", "Firestore not initialized or error", e)
    }
}

}

data class TeamMember(
    var id: String = "",
    var name: String = "",
    var role: String = "",
    var imageUrl: String = "",
    var order: Int = 0,
    var category: String = "Pastores",
    var title: String = "",
    var type: String = "equipe",
    var content: String = "",
    var mediaUrl: String = "",
    var isApproved: Boolean = true
)

val teamMembersState = androidx.compose.runtime.mutableStateListOf<TeamMember>(
    TeamMember(id = "team_1", name = "Evaldo e Denilza", role = "Pastores", category = "Pastoral", order = 1, imageUrl = "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000506-55fe455fe5/IMG_2580.jpeg?ph=bf16b0ed3a"),
    TeamMember(id = "team_2", name = "Pb Alessandro e Silvana", role = "Presbíteros", category = "Pastoral", order = 2, imageUrl = "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000508-0cca70cca9/IMG_2582.jpeg?ph=bf16b0ed3a"),
    TeamMember(id = "team_3", name = "Dac. Rosemeiry", role = "Diaconisa", category = "Diaconato", order = 3, imageUrl = "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000494-ee984ee985/1000030995.png?ph=bf16b0ed3a"),
    TeamMember(id = "team_4", name = "Pr Alexsandro e Pra Antônia", role = "Pastores", category = "Pastoral", order = 4, imageUrl = "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000510-454a6454a9/IMG_2584.jpeg?ph=bf16b0ed3a"),
    TeamMember(id = "team_5", name = "Júlia", role = "Líder de Jovens", category = "Liderança", order = 5, imageUrl = "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000498-5503255034/1000031100.png?ph=bf16b0ed3a"),
    TeamMember(id = "team_6", name = "Dac. Priscila e Josineide", role = "Dirigentes do circulo de Oração", category = "Liderança", order = 6, imageUrl = "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000530-d79abd79ad/IMG-20260215-WA0022.jpeg?ph=bf16b0ed3a"),
    TeamMember(id = "team_7", name = "Edimara de Andrade", role = "Líder", category = "Liderança", order = 7, imageUrl = "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000514-e45b9e45bb/IMG_2586.jpeg?ph=bf16b0ed3a"),
    TeamMember(id = "team_8", name = "Josineide e Lucineide", role = "Líderes", category = "Liderança", order = 8, imageUrl = "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000528-69b4a69b4c/IMG-20260210-WA0080.jpeg?ph=bf16b0ed3a")
)



enum class TabContentType {
    SYSTEM, WEBVIEW, EXTERNAL, NATIVE, PHOTOS, VIDEOS, LINKS, MIXED
}

data class CustomTabContent(
    var id: String = "",
    var title: String = "",
    var subtitle: String = "",
    var fileUrl: String = "",
    var type: String = "" // "PDF", "VIDEO", "PHOTO", "MUSIC"
)

data class DiscipuladoPdf(
    var id: String = "",
    var title: String = "",
    var subtitle: String = "",
    var description: String = "",
    var category: String = "Estudos bíblicos",
    var coverUrl: String = "",
    var storagePath: String = "",
    var fileUrl: String = "",
    var pageCount: Int = 0,
    var order: Int = 0,
    var isPublished: Boolean = true,
    var createdAt: Long = System.currentTimeMillis()
)

val discipuladoPdfsState = androidx.compose.runtime.mutableStateListOf<DiscipuladoPdf>()

data class AppTab(
    var id: String = "",
    var title: String = "",
    var iconName: String = "",
    var isPrivate: Boolean = false,
    var isVisible: Boolean = true,
    var showInBottomBar: Boolean = true,
    var order: Int = 0,
    var type: TabContentType = TabContentType.SYSTEM,
    var systemRoute: String? = null,
    var webUrl: String = "",
    var customContents: List<CustomTabContent> = emptyList()
)

enum class ContentType {
    BOOK, AUDIO, VIDEO, ALBUM
}

data class RecentlyViewedItem(
    var id: String = "",
    var title: String = "",
    var subtitle: String = "",
    var imageUrl: String = "",
    var type: ContentType = ContentType.BOOK,
    var isCached: Boolean = false,
    var progress: Float = 0f
)

val recentlyViewedState = androidx.compose.runtime.mutableStateListOf<RecentlyViewedItem>()

fun addRecentlyViewed(item: RecentlyViewedItem) {
    recentlyViewedState.removeAll { it.id == item.id && it.type == item.type }
    recentlyViewedState.add(0, item)
    if (recentlyViewedState.size > 10) recentlyViewedState.removeAt(recentlyViewedState.lastIndex)
}

val appTabsState = androidx.compose.runtime.mutableStateListOf<AppTab>()

fun ensureDiscipuladoTab(tabs: List<AppTab>): List<AppTab> {
    if (tabs.any { it.id == "discipulado_tab" || it.systemRoute == Screen.Discipulado.route }) return tabs
    val insertOrder = (tabs.firstOrNull { it.systemRoute == Screen.Ibr.route }?.order ?: 4) + 1
    val shifted = tabs.map { tab ->
        if (tab.order >= insertOrder) tab.copy(order = tab.order + 1) else tab
    }.toMutableList()
    shifted.add(AppTab("discipulado_tab", "Discipulado", "MenuBook", false, true, false, insertOrder, TabContentType.SYSTEM, Screen.Discipulado.route))
    return shifted
}

fun initializeTabs() {
    if (appTabsState.isNotEmpty()) return
    val defaultTabs = listOf(
        AppTab("1", "Início", "Home", false, true, true, 0, TabContentType.SYSTEM, Screen.Home.route),
        AppTab("bible_tab", "Bíblia", "MenuBook", false, true, false, 1, TabContentType.SYSTEM, "bible"),
        AppTab("2", "Cultos", "DateRange", false, true, true, 2, TabContentType.SYSTEM, Screen.Services.route),
        AppTab("3", "Devocionais", "Book", false, true, false, 3, TabContentType.SYSTEM, Screen.Devotionals.route),
        AppTab("4", "Cursos IBR", "School", false, true, false, 4, TabContentType.SYSTEM, Screen.Ibr.route),
        AppTab("discipulado_tab", "Discipulado", "MenuBook", false, true, false, 5, TabContentType.SYSTEM, Screen.Discipulado.route),
        AppTab("5", "Mídia", "PlayArrow", false, true, false, 6, TabContentType.SYSTEM, Screen.Content.route),
        AppTab("6", "Pedidos de Oração", "Favorite", false, true, true, 7, TabContentType.SYSTEM, Screen.Prayer.route),
        AppTab("plans_tab", "Planos", "List", false, true, true, 8, TabContentType.SYSTEM, "plans"),
        AppTab("team_tab", "Equipe", "Groups", false, true, false, 9, TabContentType.SYSTEM, Screen.Team.route),
        AppTab("7", "Membros", "Person", false, true, false, 10, TabContentType.SYSTEM, Screen.Members.route),
        AppTab("8", "Sobre", "Info", false, true, false, 11, TabContentType.SYSTEM, Screen.About.route),
        AppTab("settings_tab", "Configurações", "Settings", false, true, false, 12, TabContentType.SYSTEM, Screen.Settings.route),
        AppTab("10", "Dízimos e Ofertas", "VolunteerActivism", false, true, true, 13, TabContentType.SYSTEM, Screen.Donations.route),
        AppTab("admin_tab", "Área ADM", "Lock", false, true, false, 14, TabContentType.SYSTEM, Screen.Admin.route)
    )
    appTabsState.addAll(defaultTabs)
}

fun loadTeamMembersFromFirebase() {
    //
}


fun syncIbrProgressToFirestore(progress: IbrProgress) {
    val userId = loggedInMemberState.value?.id ?: return
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("ibrProgress").document("${progress.courseId}_${progress.chapterId}")
        .set(progress)
        .addOnFailureListener { e -> e.printStackTrace() }
}

fun loadIbrProgressFromFirestore() {
    val userId = loggedInMemberState.value?.id ?: return
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("ibrProgress")
        .addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { try { it.toObject(IbrProgress::class.java) } catch(ex: Exception) { null } }
            ibrProgressState.clear()
            ibrProgressState.addAll(list)
        }
}

fun addContentBook(item: ContentBook) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("conteudos_books").document(item.id).set(item)
    }
}
fun removeContentBook(item: ContentBook) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("conteudos_books").document(item.id).delete()
    }
}

fun addContentAudio(item: ContentAudio) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("conteudos_audios").document(item.id).set(item)
    }
}
fun removeContentAudio(item: ContentAudio) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("conteudos_audios").document(item.id).delete()
    }
}

fun addContentVideo(item: ContentVideo) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("conteudos_videos").document(item.id).set(item)
    }
}
fun removeContentVideo(item: ContentVideo) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("conteudos_videos").document(item.id).delete()
    }
}

fun addContentPhotoAlbum(item: ContentPhotoAlbum) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("conteudos_albums").document(item.id).set(item)
    }
}
fun removeContentPhotoAlbum(item: ContentPhotoAlbum) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("conteudos_albums").document(item.id).delete()
    }
}

fun addIbrCourse(item: IbrCourse) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("ibr_courses").document(item.id).set(item)
    }
}
fun removeIbrCourse(item: IbrCourse) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("ibr_courses").document(item.id).delete()
    }
}

fun addServiceVideo(item: ServiceVideoModel) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("cultos").document(item.id).set(item)
    }
}
fun removeServiceVideo(item: ServiceVideoModel) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("cultos").document(item.id).delete()
    }
}

fun addDevotional(context: android.content.Context, item: Devotional) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("devocionais").document(item.id).set(item)
            .addOnSuccessListener {
                android.widget.Toast.makeText(context, "Devocional salvo com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                // Update timestamp for GlobalStateManager
                Firebase.firestore.collection("settings").document("sync_trigger").set(mapOf("timestamp" to System.currentTimeMillis()))
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(context, "Erro ao salvar: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
    }
}
fun removeDevotional(context: android.content.Context, item: Devotional) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("devocionais").document(item.id).delete()
            .addOnSuccessListener {
                android.widget.Toast.makeText(context, "Removido com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                Firebase.firestore.collection("settings").document("sync_trigger").set(mapOf("timestamp" to System.currentTimeMillis()))
            }
            .addOnFailureListener { e ->
                android.widget.Toast.makeText(context, "Erro ao remover: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
    }
}

fun addChurchService(item: ChurchService) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("cultos_agenda").document(item.id).set(item)
    }
}
fun removeChurchService(item: ChurchService) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("cultos_agenda").document(item.id).delete()
    }
}

fun removeBibleNews(item: BibleNews) {
    if (isOfflineModeState.value) return
    val db = com.google.firebase.Firebase.firestore
    db.collection("bible_news").document(item.id.toString()).delete()
}

fun addBibleNews(item: BibleNews) {
    if (isOfflineModeState.value) return
    val db = com.google.firebase.Firebase.firestore
    db.collection("bible_news").document(item.id.toString()).set(BibleNewsEditorial.decorate(item))
}

fun addChurchEvent(item: ChurchEvent) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("events").document(item.id).set(item)
    }
}
fun removeChurchEvent(item: ChurchEvent) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("events").document(item.id).delete()
    }
}

fun addCarouselItem(item: CarouselItem) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("carousel_items").document(item.id).set(item)
    }
}
fun removeCarouselItem(item: CarouselItem) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("carousel_items").document(item.id).delete()
    }
}

fun addPrayerRequest(
    item: PrayerRequest,
    onSuccess: () -> Unit = {},
    onFailure: (Exception) -> Unit = {}
) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
        onFailure(IllegalStateException("Firebase não configurado"))
        return
    }
    Firebase.firestore.collection("prayer_requests").document(item.id).set(item)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}
fun removePrayerRequest(item: PrayerRequest) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("prayer_requests").document(item.id).delete()
    }
}

fun addAppTab(item: AppTab) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("app_tabs").document(item.id).set(item)
    }
}
fun removeAppTab(item: AppTab) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("app_tabs").document(item.id).delete()
    }
}



val bibleNewsState = androidx.compose.runtime.mutableStateListOf<BibleNews>()
val biblePlansState = androidx.compose.runtime.mutableStateListOf<PlanCategory>().apply { addAll(PlansData.categories) }
val planSyncErrorState = androidx.compose.runtime.mutableStateOf("")
val dailyNewsNotificationIdState = androidx.compose.runtime.mutableStateOf<Int?>(null)

fun loadDailyNewsNotificationSelection() {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) return
    com.google.firebase.Firebase.firestore.collection("settings").document("daily_news")
        .addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            dailyNewsNotificationIdState.value = snapshot.getLong("selectedNewsId")?.toInt()
        }
}

fun selectDailyNewsNotification(news: BibleNews, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
        onFailure(IllegalStateException("Firebase não configurado"))
        return
    }
    com.google.firebase.Firebase.firestore.collection("settings").document("daily_news").set(
        mapOf(
            "selectedNewsId" to news.id,
            "title" to news.title,
            "summary" to news.summary,
            "content" to news.content,
            "updatedAt" to System.currentTimeMillis()
        ),
        com.google.firebase.firestore.SetOptions.merge()
    ).addOnSuccessListener {
        dailyNewsNotificationIdState.value = news.id
        onSuccess()
    }.addOnFailureListener { onFailure(it) }
}

fun clearDailyNewsNotification(onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isEmpty()) {
        onFailure(IllegalStateException("Firebase não configurado"))
        return
    }
    com.google.firebase.Firebase.firestore.collection("settings").document("daily_news")
        .delete()
        .addOnSuccessListener {
            dailyNewsNotificationIdState.value = null
            onSuccess()
        }
        .addOnFailureListener { onFailure(it) }
}

fun syncBibleNewsAndPlans() {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    loadDailyNewsNotificationSelection()
    
    // Sync News: a Home e a Central carregam somente a primeira página.
    BibleNewsPagination.start()
    
    // Sync Plans
    db.collection("bible_plans").addSnapshotListener { snapshot, e ->
        if (e != null || snapshot == null) {
            planSyncErrorState.value = "Listener erro: $e"
            return@addSnapshotListener
        }
        val firstDocThemes = snapshot.documents.firstOrNull()?.get("themes") as? List<*>
        val needsUpdate = snapshot.isEmpty || (firstDocThemes != null && firstDocThemes.size < 37)
        if (needsUpdate) {
            PlansData.categories.forEach { cat ->
                db.collection("bible_plans").document(cat.name).set(mapOf(
                    "name" to cat.name,
                    "color" to cat.color.value.toLong(),
                    "themes" to cat.themes.map { theme ->
                        mapOf(
                            "title" to theme.title,
                            "content" to theme.content,
                            "verses" to theme.verses,
                            "imageUrl" to theme.imageUrl
                        )
                    }
                ))
            }
            biblePlansState.clear()
            biblePlansState.addAll(PlansData.categories)
        } else {
            val list = snapshot.documents.mapNotNull { doc ->
                try {
                    val name = doc.getString("name") ?: ""
                    val colorValue = doc.getLong("color") ?: 0L
                    val themesList = doc.get("themes") as? List<Map<String, Any>> ?: emptyList()
                    val themes = themesList.map { t ->
                        PlanTheme(
                            title = t["title"] as? String ?: "",
                            content = t["content"] as? String ?: "",
                            verses = t["verses"] as? List<String> ?: emptyList(),
                            imageUrl = t["imageUrl"] as? String ?: ""
                        )
                    }
                    PlanCategory(name, androidx.compose.ui.graphics.Color(colorValue.toULong()), themes)
                } catch (ex: Exception) { android.util.Log.e("BIBLE_PLANS_SYNC", "Error parsing plan", ex); planSyncErrorState.value = ex.toString(); null }
            }
            biblePlansState.clear()
            biblePlansState.addAll(list)
        }
    }
}


data class FavoriteItem(
    val id: String = "",
    val type: String = "", // "bible" or "devotional"
    val reference: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

val favoriteItemsState = androidx.compose.runtime.mutableStateListOf<FavoriteItem>()

fun loadFavoritesFromFirestore() {
    val userId = loggedInMemberState.value?.id ?: return
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("favorites")
        .addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            val list = snapshot.documents.mapNotNull { 
                try { it.toObject(FavoriteItem::class.java) } catch(ex: Exception) { null } 
            }
            val merged = (favoriteItemsState.toList() + list)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
            favoriteItemsState.clear()
            favoriteItemsState.addAll(merged)
        }
}

fun addFavorite(item: FavoriteItem) {
    favoriteItemsState.removeAll { it.id == item.id }
    favoriteItemsState.add(0, item)
    val userId = loggedInMemberState.value?.id ?: return
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("favorites").document(item.id)
        .set(item)
}

fun removeFavorite(itemId: String) {
    favoriteItemsState.removeAll { it.id == itemId }
    val userId = loggedInMemberState.value?.id ?: return
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("users").document(userId).collection("favorites").document(itemId)
        .delete()
}


val homeBannersState = androidx.compose.runtime.mutableStateListOf<String>("https://images.unsplash.com/photo-1544427920-c49ccfb85579?auto=format&fit=crop&q=80&w=800")

val pixKeyState = androidx.compose.runtime.mutableStateOf("")
val pixQrCodeUrlState = androidx.compose.runtime.mutableStateOf("")

fun loadDonationsFromFirestore() {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("settings").document("donations").addSnapshotListener { snapshot, e ->
        if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
        pixKeyState.value = snapshot.getString("pixKey") ?: ""
        pixQrCodeUrlState.value = snapshot.getString("qrCodeUrl") ?: ""
    }
}

fun saveDonationsToFirestore(pixKey: String, qrCodeUrl: String) {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("settings").document("donations").set(mapOf(
        "pixKey" to pixKey,
        "qrCodeUrl" to qrCodeUrl
    ))
}


fun loadBannersFromFirestore() {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("settings").document("home_banners").addSnapshotListener { snapshot, e ->
        if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
        val list = snapshot.get("urls") as? List<String>
        if (list != null && list.isNotEmpty()) {
            homeBannersState.clear()
            homeBannersState.addAll(list)
        }
    }
}

fun saveBannersToFirestore(urls: List<String>) {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    db.collection("settings").document("home_banners").set(mapOf("urls" to urls))
}

fun convertGoogleDriveUrl(url: String): String {
    return GoogleDriveService.getDirectDownloadLink(url)
}



suspend fun refreshHomeData() {
    try {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val source = com.google.firebase.firestore.Source.SERVER

        try {
            val bannersSnapshot = db.collection("carousel_items").get(source).await()
            val list = bannersSnapshot.documents.mapNotNull { try { it.toObject(CarouselItem::class.java) } catch(ex: Exception) { null } }
            if (list.isNotEmpty() || bannersSnapshot.isEmpty) {
                carouselItemsState.clear()
                carouselItemsState.addAll(list)
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val devotionalsSnapshot = db.collection("devocionais").get(source).await()
            val list = devotionalsSnapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val date = doc.getString("date") ?: ""
                    val verse = doc.getString("verse") ?: ""
                    val verseReference = doc.getString("verseReference") ?: ""
                    val content = doc.getString("content") ?: ""
                    val likes = doc.getLong("likes")?.toInt() ?: 0
                    val type = doc.getString("type") ?: "devocional"
                    val mediaUrl = doc.getString("mediaUrl") ?: ""
                    val isApproved = doc.getBoolean("isApproved") ?: true
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    Devotional(id, title, date, verse, verseReference, content, likes, type, mediaUrl, isApproved, timestamp)
                } catch(ex: Exception) { null }
            }
            if (list.isNotEmpty() || devotionalsSnapshot.isEmpty) {
                devotionalsState.clear()
                devotionalsState.addAll(list.sortedByDescending { it.timestamp })
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val newsSnapshot = db.collection("bible_news").get(source).await()
            val list = newsSnapshot.documents.mapNotNull { try { it.toObject(BibleNews::class.java) } catch(ex: Exception) { null } }
            if (list.isNotEmpty() || newsSnapshot.isEmpty) {
                bibleNewsState.clear()
                bibleNewsState.addAll(list)
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val servicesSnapshot = db.collection("cultos_agenda").get(source).await()
            val list = servicesSnapshot.documents.mapNotNull { try { it.toObject(ChurchService::class.java) } catch(ex: Exception) { null } }
            if (list.isNotEmpty() || servicesSnapshot.isEmpty) {
                weeklyServicesState.clear()
                weeklyServicesState.addAll(list)
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val plansSnapshot = db.collection("bible_plans").get(source).await()
            val list = plansSnapshot.documents.mapNotNull { doc ->
                try {
                    val name = doc.getString("name") ?: ""
                    val colorValue = doc.getLong("color") ?: 0L
                    val themesList = doc.get("themes") as? List<Map<String, Any>> ?: emptyList()
                    val themes = themesList.map { t ->
                        PlanTheme(
                            title = t["title"] as? String ?: "",
                            content = t["content"] as? String ?: "",
                            verses = (t["verses"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                            imageUrl = t["imageUrl"] as? String ?: ""
                        )
                    }
                    PlanCategory(name, androidx.compose.ui.graphics.Color(colorValue.toULong()), themes)
                } catch (ex: Exception) { null }
            }
            if (list.isNotEmpty() || plansSnapshot.isEmpty) {
                biblePlansState.clear()
                biblePlansState.addAll(list)
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val videosSnapshot = db.collection("conteudos_videos").get(source).await()
            val list = videosSnapshot.documents.mapNotNull { try { it.toObject(ContentVideo::class.java) } catch(ex: Exception) { null } }
            if (list.isNotEmpty() || videosSnapshot.isEmpty) {
                contentVideosState.clear()
                contentVideosState.addAll(list)
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val audiosSnapshot = db.collection("conteudos_audios").get(source).await()
            val list = audiosSnapshot.documents.mapNotNull { try { it.toObject(ContentAudio::class.java) } catch(ex: Exception) { null } }
            if (list.isNotEmpty() || audiosSnapshot.isEmpty) {
                contentAudiosState.clear()
                contentAudiosState.addAll(list)
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val booksSnapshot = db.collection("conteudos_books").get(source).await()
            val list = booksSnapshot.documents.mapNotNull { try { it.toObject(ContentBook::class.java) } catch(ex: Exception) { null } }
            if (list.isNotEmpty() || booksSnapshot.isEmpty) {
                contentBooksState.clear()
                contentBooksState.addAll(list)
            }
        } catch (e: Exception) { e.printStackTrace() }

        try {
            val albumsSnapshot = db.collection("conteudos_albums").get(source).await()
            val list = albumsSnapshot.documents.mapNotNull { try { it.toObject(ContentPhotoAlbum::class.java) } catch(ex: Exception) { null } }
            if (list.isNotEmpty() || albumsSnapshot.isEmpty) {
                contentAlbumsState.clear()
                contentAlbumsState.addAll(list)
            }
        } catch (e: Exception) { e.printStackTrace() }

        val currentMember = loggedInMemberState.value
        if (currentMember != null) {
            try {
                val memberSnapshot = db.collection("acessos_pendentes").document(currentMember.id).get(source).await()
                if (memberSnapshot.exists()) {
                    val id = memberSnapshot.id
                    val name = memberSnapshot.getString("name") ?: ""
                    val phone = memberSnapshot.getString("phone") ?: ""
                    val rawIsApproved = memberSnapshot.getBoolean("isApproved") ?: false
                    val isVip = memberSnapshot.getBoolean("isVip") ?: false
                    val effectiveApproved = rawIsApproved || isVip
                    val isIbr = memberSnapshot.getBoolean("isIbr") ?: false
                    val email = memberSnapshot.getString("email") ?: ""
                    val isAdmin = memberSnapshot.getBoolean("isAdmin") ?: false
                    val ibrCertificateUrl = memberSnapshot.getString("ibrCertificateUrl") ?: ""
                    val ibrCertificateStoragePath = memberSnapshot.getString("ibrCertificateStoragePath") ?: ""
                    val status = memberSnapshot.getString("status") ?: if (effectiveApproved || isIbr) "aprovado" else "pendente"
                    val title = memberSnapshot.getString("title") ?: ""
                    val type = memberSnapshot.getString("type") ?: "acesso"
                    val content = memberSnapshot.getString("content") ?: ""
                    val mediaUrl = memberSnapshot.getString("mediaUrl") ?: ""
                    val remoteProfilePhotoUrl = memberSnapshot.getString("profilePhotoUrl") ?: ""
                    val avatarId = memberSnapshot.getString("avatarId").orEmpty().ifBlank { DEFAULT_BIBLICAL_AVATAR_ID }
                    val unlockedBadgeIds = (memberSnapshot.get("unlockedBadgeIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.filter { it.isNotBlank() }
                        ?.ifEmpty { listOf(DEFAULT_BIBLICAL_BADGE_ID) }
                        ?: listOf(DEFAULT_BIBLICAL_BADGE_ID)
                    val equippedBadgeId = memberSnapshot.getString("equippedBadgeId").orEmpty().ifBlank { DEFAULT_BIBLICAL_BADGE_ID }
                    val supabaseStoragePath = memberSnapshot.getString("supabaseStoragePath") ?: ""
                    val profilePhotoUrl = remoteProfilePhotoUrl.takeIf { it.isNotBlank() }
                        ?: currentMember.profilePhotoUrl
                    val address = memberSnapshot.getString("address") ?: ""
                    val birthDate = memberSnapshot.getString("birthDate") ?: ""
                    val createdAt = memberSnapshot.getLong("createdAt") ?: 0L
                    val updatedAt = memberSnapshot.getLong("updatedAt") ?: 0L
                    val updatedMember = MemberRequest(
                        id = id, 
                        name = name, 
                        phone = phone, 
                        email = email,
                        isApproved = effectiveApproved, 
                        isVip = false, 
                        isIbr = isIbr,
                        isAdmin = isAdmin,
                        ibrCertificateUrl = ibrCertificateUrl,
                        ibrCertificateStoragePath = ibrCertificateStoragePath,
                        status = status,
                        title = title,
                        type = type,
                        content = content,
                        mediaUrl = mediaUrl,
                        profilePhotoUrl = profilePhotoUrl,
                        avatarId = avatarId,
                        unlockedBadgeIds = unlockedBadgeIds,
                        equippedBadgeId = equippedBadgeId,
                        supabaseStoragePath = supabaseStoragePath,
                        address = address,
                        birthDate = birthDate,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    loggedInMemberState.value = updatedMember
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
suspend fun forceRefreshData() {
    try {
        kotlinx.coroutines.delay(1000)
        com.google.firebase.firestore.FirebaseFirestore.getInstance().enableNetwork().await()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
