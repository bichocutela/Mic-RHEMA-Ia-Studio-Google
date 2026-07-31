package com.aistudio.micrhema
import kotlinx.coroutines.tasks.await


import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import android.util.Log

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

val userIsAdmin = mutableStateOf(false)
object DevotionalManager {
    fun syncDevotionals(context: Context) {
        try {
            // First load from local cache instantly for offline support
            val cachedDevotionals = IbrDatabaseHelper(context).getCachedDevotionals()
            if (cachedDevotionals.isNotEmpty()) {
                devotionalsState.clear()
                devotionalsState.addAll(cachedDevotionals)
            }

            if (isOfflineModeState.value) {
                return
            }

            val db = com.google.firebase.Firebase.firestore
            db.collection("devotionals").get()
                .addOnSuccessListener { result ->
                    val newList = mutableListOf<Devotional>()
                    for (document in result) {
                        val id = document.id
                        val title = document.getString("title") ?: ""
                        val date = document.getString("date") ?: ""
                        val verse = document.getString("verse") ?: ""
                        val verseReference = document.getString("verseReference") ?: ""
                        val textContent = document.getString("content") ?: ""
                        val likes = document.getLong("likes")?.toInt() ?: 0
                        newList.add(Devotional(id, title, date, verse, verseReference, textContent, likes))
                    }
                    if (newList.isNotEmpty()) {
                        newList.sortByDescending { it.date }
                        devotionalsState.clear()
                        devotionalsState.addAll(newList)
                        
                        val dbHelper = IbrDatabaseHelper(context)
                        dbHelper.saveCachedDevotionals(newList)
                    }
                }
                .addOnFailureListener {
                    // Cache already loaded above, do nothing
                }
        } catch (e: Exception) {
            android.util.Log.e("DevotionalManager", "Firestore not initialized or error", e)
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
    var likes: Int = 0
)

data class ChurchService(
    var id: String = "",
    var day: String = "",
    var dayShort: String = "",
    var time: String = "",
    var title: String = "",
    var description: String = ""
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
    var imageUrl: String? = null
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
    var name: String = "",
    var email: String = "",
    var isApproved: Boolean = false,
    var isVip: Boolean = false,
    var isIbr: Boolean = false
)

val memberRequestsState = mutableStateListOf<MemberRequest>(
    MemberRequest(
        id = "1",
        name = "Carlos Oliveira",
        email = "carlos.oliveira@gmail.com",
        isApproved = true,
        isVip = true,
        isIbr = false
    ),
    MemberRequest(
        id = "2",
        name = "Ana Costa",
        email = "ana.costa@gmail.com",
        isApproved = true,
        isVip = false,
        isIbr = true
    ),
    MemberRequest(
        id = "3",
        name = "Marcos Souza",
        email = "marcos.souza@gmail.com",
        isApproved = false,
        isVip = false,
        isIbr = false
    )
)

val loggedInMemberState = mutableStateOf<MemberRequest?>(null)

object MemberManager {
    private const val PREFS_NAME = "micrhema_members_prefs"
    private const val KEY_MEMBERS = "members_list"
    private const val KEY_LOGGED_IN_ID = "logged_in_member_id"

    fun syncFromFirestore(context: android.content.Context) {
        try {
            val db = Firebase.firestore

            db.collection("vip_books").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentBook::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipBooksState.clear()
                    vipBooksState.addAll(list)
                } else if (vipBooksState.isNotEmpty()) {
                    vipBooksState.forEach { db.collection("vip_books").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_audios").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentAudio::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipAudiosState.clear()
                    vipAudiosState.addAll(list)
                } else if (vipAudiosState.isNotEmpty()) {
                    vipAudiosState.forEach { db.collection("vip_audios").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_videos").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentVideo::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipVideosState.clear()
                    vipVideosState.addAll(list)
                } else if (vipVideosState.isNotEmpty()) {
                    vipVideosState.forEach { db.collection("vip_videos").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_albums").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentPhotoAlbum::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipAlbumsState.clear()
                    vipAlbumsState.addAll(list)
                } else if (vipAlbumsState.isNotEmpty()) {
                    vipAlbumsState.forEach { db.collection("vip_albums").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_courses").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(IbrCourse::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipCoursesState.clear()
                    vipCoursesState.addAll(list)
                } else if (vipCoursesState.isNotEmpty()) {
                    vipCoursesState.forEach {
                        db.collection("vip_courses").document(it.id).set(it)
                    }
                }
            }

            db.collection("members").get()
                .addOnSuccessListener { result ->
                    val newList = mutableListOf<MemberRequest>()
                    for (document in result) {
                        val id = document.id
                        val name = document.getString("name") ?: ""
                        val email = document.getString("email") ?: ""
                        val isApproved = document.getBoolean("isApproved") ?: false
                        val isVip = document.getBoolean("isVip") ?: false
                        val isIbr = document.getBoolean("isIbr") ?: false
                        newList.add(MemberRequest(id, name, email, isApproved, isVip, isIbr))
                    }
                    if (newList.isNotEmpty()) {
                        memberRequestsState.clear()
                        memberRequestsState.addAll(newList)
                        saveMembers(context)
                        
                        val loggedInId = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                            .getString(KEY_LOGGED_IN_ID, "") ?: ""
                        if (loggedInId.isNotEmpty()) {
                            val member = memberRequestsState.find { it.id == loggedInId }
                            if (member != null) {
                                loggedInMemberState.value = member
        loadIbrProgressFromFirestore()
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("MemberManager", "Error getting documents: ", exception)
                }
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }

    fun deleteFromFirestore(member: MemberRequest) {
        try {
            val db = Firebase.firestore

            db.collection("vip_books").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentBook::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipBooksState.clear()
                    vipBooksState.addAll(list)
                } else if (vipBooksState.isNotEmpty()) {
                    vipBooksState.forEach { db.collection("vip_books").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_audios").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentAudio::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipAudiosState.clear()
                    vipAudiosState.addAll(list)
                } else if (vipAudiosState.isNotEmpty()) {
                    vipAudiosState.forEach { db.collection("vip_audios").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_videos").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentVideo::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipVideosState.clear()
                    vipVideosState.addAll(list)
                } else if (vipVideosState.isNotEmpty()) {
                    vipVideosState.forEach { db.collection("vip_videos").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_albums").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentPhotoAlbum::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipAlbumsState.clear()
                    vipAlbumsState.addAll(list)
                } else if (vipAlbumsState.isNotEmpty()) {
                    vipAlbumsState.forEach { db.collection("vip_albums").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_courses").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(IbrCourse::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipCoursesState.clear()
                    vipCoursesState.addAll(list)
                } else if (vipCoursesState.isNotEmpty()) {
                    vipCoursesState.forEach {
                        db.collection("vip_courses").document(it.id).set(it)
                    }
                }
            }

            db.collection("members").document(member.id).delete()
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }

    fun saveToFirestore(member: MemberRequest) {
        try {
            val db = Firebase.firestore

            db.collection("vip_books").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentBook::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipBooksState.clear()
                    vipBooksState.addAll(list)
                } else if (vipBooksState.isNotEmpty()) {
                    vipBooksState.forEach { db.collection("vip_books").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_audios").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentAudio::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipAudiosState.clear()
                    vipAudiosState.addAll(list)
                } else if (vipAudiosState.isNotEmpty()) {
                    vipAudiosState.forEach { db.collection("vip_audios").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_videos").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentVideo::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipVideosState.clear()
                    vipVideosState.addAll(list)
                } else if (vipVideosState.isNotEmpty()) {
                    vipVideosState.forEach { db.collection("vip_videos").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_albums").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentPhotoAlbum::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipAlbumsState.clear()
                    vipAlbumsState.addAll(list)
                } else if (vipAlbumsState.isNotEmpty()) {
                    vipAlbumsState.forEach { db.collection("vip_albums").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_courses").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(IbrCourse::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipCoursesState.clear()
                    vipCoursesState.addAll(list)
                } else if (vipCoursesState.isNotEmpty()) {
                    vipCoursesState.forEach {
                        db.collection("vip_courses").document(it.id).set(it)
                    }
                }
            }

            val memberMap = hashMapOf(
                "name" to member.name,
                "email" to member.email,
                "isApproved" to member.isApproved,
                "isVip" to member.isVip,
                "isIbr" to member.isIbr
            )
            db.collection("members").document(member.id).set(memberMap)
                .addOnSuccessListener { Log.d("MemberManager", "Document successfully written!") }
                .addOnFailureListener { e -> Log.w("MemberManager", "Error writing document", e) }
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }

    fun loadMembers(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val serialized = prefs.getString(KEY_MEMBERS, "") ?: ""
        if (serialized.isNotEmpty()) {
            val list = serialized.split("||").mapNotNull {
                val parts = it.split("|")
                if (parts.size >= 6) {
                    MemberRequest(
                        id = parts[0],
                        name = parts[1],
                        email = parts[2],
                        isApproved = parts[3].toBoolean(),
                        isVip = parts[4].toBoolean(),
                        isIbr = parts[5].toBoolean()
                    )
                } else null
            }
            if (list.isNotEmpty()) {
                memberRequestsState.clear()
                memberRequestsState.addAll(list)
            }
        }
        
        val loggedInId = prefs.getString(KEY_LOGGED_IN_ID, "") ?: ""
        if (loggedInId.isNotEmpty()) {
            val member = memberRequestsState.find { it.id == loggedInId }
            if (member != null) {
                loggedInMemberState.value = member
        loadIbrProgressFromFirestore()
            }
        }
    }

    fun saveMembers(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val serialized = memberRequestsState.joinToString("||") {
            "${it.id}|${it.name}|${it.email}|${it.isApproved}|${it.isVip}|${it.isIbr}"
        }
        prefs.edit().putString(KEY_MEMBERS, serialized).apply()
    }
    
    fun setLoggedInMember(context: android.content.Context, member: MemberRequest?) {
        loggedInMemberState.value = member
        loadIbrProgressFromFirestore()
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (member == null) {
            prefs.edit().remove(KEY_LOGGED_IN_ID).apply()
        } else {
            prefs.edit().putString(KEY_LOGGED_IN_ID, member.id).apply()
        }
    }
}


data class IbrChapter(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var durationMinutes: Int = 0,
    var videoUrl: String = "", // URL to video stream or YouTube
    var audioUrl: String = "", // URL to audio stream
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

val ibrCoursesState = mutableStateListOf<IbrCourse>(
    IbrCourse(
        id = "1",
        title = "Introdução à Teologia Sistemática",
        theme = "Teologia",
        description = "Explore os pilares fundamentais da fé cristã, a Doutrina de Deus, Cristo e da Revelação Divina com profundidade acadêmica e prática ministerial.",
        imageUrl = "https://images.unsplash.com/photo-1504052434569-70ad5836ab65?w=500&auto=format&fit=crop&q=60",
        chapters = listOf(
            IbrChapter(
                id = "1_1",
                title = "Aula 1: O que é Teologia?",
                description = "Conceito, importância e o método do estudo teológico sistemático na caminhada cristã.",
                durationMinutes = 45,
                videoUrl = "https://m.youtube.com/watch?v=SKsJdyi4eUE",
                isYoutube = true,
                youtubeId = "SKsJdyi4eUE",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            ),
            IbrChapter(
                id = "1_2",
                title = "Aula 2: A Doutrina da Trindade",
                description = "Uma imersão na revelação bíblica sobre a natureza triúna de Deus: Pai, Filho e Espírito Santo.",
                durationMinutes = 52,
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
            ),
            IbrChapter(
                id = "1_3",
                title = "Aula 3: Hermenêutica e Revelação Divina",
                description = "Vídeo oficial de apoio sobre como interpretar e receber a palavra revelada do Senhor.",
                durationMinutes = 38,
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                isYoutube = true,
                youtubeId = "dQw4w9WgXcQ"
            )
        )
    ),
    IbrCourse(
        id = "2",
        title = "História da Igreja Cristã",
        theme = "História Bíblica",
        description = "Caminhe através dos séculos de história cristã, desde o dia de Pentecostes até o avivamento contemporâneo.",
        imageUrl = "https://images.unsplash.com/photo-1438211331416-0be89cc621a8?w=500&auto=format&fit=crop&q=60",
        chapters = listOf(
            IbrChapter(
                id = "2_1",
                title = "Aula 1: A Igreja Primitiva",
                description = "Os primeiros séculos da fé cristã, o império romano e a propagação do Evangelho.",
                durationMinutes = 55,
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"
            ),
            IbrChapter(
                id = "2_2",
                title = "Aula 2: A Reforma Protestante",
                description = "O retorno às Escrituras Sagradas e as cinco solas que moldaram o pensamento protestante.",
                durationMinutes = 60,
                videoUrl = "",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
            )
        )
    ),
    IbrCourse(
        id = "3",
        title = "Doutrina do Rhema - Fé Prática",
        theme = "Vida Cristã",
        description = "Compreenda a diferença bíblica entre Logos e Rhema e como aplicar a palavra revelada para viver uma vida vitoriosa em Cristo.",
        imageUrl = "https://images.unsplash.com/photo-1447069387593-a5de0862481e?w=500&auto=format&fit=crop&q=60",
        chapters = listOf(
            IbrChapter(
                id = "3_1",
                title = "Aula 1: Logos vs Rhema",
                description = "A palavra escrita frente à palavra falada pelo Espírito diretamente ao nosso coração.",
                durationMinutes = 35,
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3"
            )
        )
    )
)

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

data class ContentBook(
    var id: String = "",
    var title: String = "",
    var author: String = "",
    var coverUrl: String = "",
    var contentText: String = "",
    var bookUrl: String = "",
    var isCached: Boolean = false,
    var progress: Float = 0f,
    var lastPosition: Long = 0L
)

data class ContentAudio(
    var id: String = "",
    var title: String = "",
    var artist: String = "",
    var audioUrl: String = "",
    var coverUrl: String = "",
    var isCached: Boolean = false,
    var progress: Float = 0f,
    var lastPosition: Long = 0L
)

data class ContentVideo(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var videoUrl: String = "",
    var thumbnailUrl: String = "",
    var isCached: Boolean = false,
    var progress: Float = 0f,
    var lastPosition: Long = 0L
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
    var photos: List<AlbumPhoto> = emptyList()
)


val vipBooksState = androidx.compose.runtime.mutableStateListOf<ContentBook>()
val vipAudiosState = androidx.compose.runtime.mutableStateListOf<ContentAudio>()
val vipVideosState = androidx.compose.runtime.mutableStateListOf<ContentVideo>()
val vipAlbumsState = androidx.compose.runtime.mutableStateListOf<ContentPhotoAlbum>()
val vipCoursesState = androidx.compose.runtime.mutableStateListOf<IbrCourse>()

val contentBooksState = androidx.compose.runtime.mutableStateListOf<ContentBook>(
    ContentBook("1", "O Poder da Oração", "E.M. Bounds", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80", "A oração é a força mais poderosa da terra...", isCached = true, progress = 0.1f)
)
val contentAudiosState = androidx.compose.runtime.mutableStateListOf<ContentAudio>(
    ContentAudio("1", "Mensagem de Fé", "Pr. Presidente", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80", isCached = true, progress = 0.4f)
)
val contentVideosState = androidx.compose.runtime.mutableStateListOf<ContentVideo>(
    ContentVideo("1", "Culto Especial de Domingo", "Mensagem sobre a graça de Deus", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "https://images.unsplash.com/photo-1505764761634-1d77b57e1966?w=500&q=80", isCached = true, progress = 0.8f)
)
val contentAlbumsState = androidx.compose.runtime.mutableStateListOf<ContentPhotoAlbum>()
val serviceVideosState = androidx.compose.runtime.mutableStateListOf<ServiceVideoModel>(
    ServiceVideoModel(
        id = "1",
        title = "Culto de Domingo - Família",
        date = "Domingo, 10h",
        videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        thumbnailUrl = "https://images.unsplash.com/photo-1438211331416-0be89cc621a8?w=500&q=80"
    ),
    ServiceVideoModel(
        id = "2",
        title = "Culto de Celebração e Palavra",
        date = "Domingo, 18h",
        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        thumbnailUrl = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=500&q=80"
    )
)

fun loadContentFromFirebase(context: Context) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        try {
            val db = Firebase.firestore

            db.collection("vip_books").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentBook::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipBooksState.clear()
                    vipBooksState.addAll(list)
                } else if (vipBooksState.isNotEmpty()) {
                    vipBooksState.forEach { db.collection("vip_books").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_audios").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentAudio::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipAudiosState.clear()
                    vipAudiosState.addAll(list)
                } else if (vipAudiosState.isNotEmpty()) {
                    vipAudiosState.forEach { db.collection("vip_audios").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_videos").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentVideo::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipVideosState.clear()
                    vipVideosState.addAll(list)
                } else if (vipVideosState.isNotEmpty()) {
                    vipVideosState.forEach { db.collection("vip_videos").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_albums").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentPhotoAlbum::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipAlbumsState.clear()
                    vipAlbumsState.addAll(list)
                } else if (vipAlbumsState.isNotEmpty()) {
                    vipAlbumsState.forEach { db.collection("vip_albums").document(it.id).set(it) }
                }
            }
            
            db.collection("vip_courses").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(IbrCourse::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    vipCoursesState.clear()
                    vipCoursesState.addAll(list)
                } else if (vipCoursesState.isNotEmpty()) {
                    vipCoursesState.forEach {
                        db.collection("vip_courses").document(it.id).set(it)
                    }
                }
            }

            
            db.collection("content_books").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentBook::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    contentBooksState.clear()
                    contentBooksState.addAll(list)
                } else if (contentBooksState.isNotEmpty()) {
                    contentBooksState.forEach { db.collection("content_books").document(it.id).set(it) }
                }
            }
            
            db.collection("content_audios").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentAudio::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    contentAudiosState.clear()
                    contentAudiosState.addAll(list)
                } else if (contentAudiosState.isNotEmpty()) {
                    contentAudiosState.forEach { db.collection("content_audios").document(it.id).set(it) }
                }
            }
            
            db.collection("content_videos").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentVideo::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    contentVideosState.clear()
                    contentVideosState.addAll(list)
                } else if (contentVideosState.isNotEmpty()) {
                    contentVideosState.forEach { db.collection("content_videos").document(it.id).set(it) }
                }
            }
            
            db.collection("content_albums").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ContentPhotoAlbum::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    contentAlbumsState.clear()
                    contentAlbumsState.addAll(list)
                } else if (contentAlbumsState.isNotEmpty()) {
                    contentAlbumsState.forEach { db.collection("content_albums").document(it.id).set(it) }
                }
            }
            
            db.collection("devotionals").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val date = doc.getString("date") ?: ""
                    val verse = doc.getString("verse") ?: ""
                    val verseRef = doc.getString("verseReference") ?: ""
                    val contentText = doc.getString("content") ?: ""
                    val likes = doc.getLong("likes")?.toInt() ?: 0
                    Devotional(id, title, date, verse, verseRef, contentText, likes)
                }
                if (list.isNotEmpty()) {
                    devotionalsState.clear()
                    devotionalsState.addAll(list.sortedByDescending { it.date })
                } else if (devotionalsState.isNotEmpty()) {
                    devotionalsState.forEach { 
                        db.collection("devotionals").document(it.id).set(it)
                    }
                }
            }
            
            db.collection("weekly_services").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ChurchService::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    weeklyServicesState.clear()
                    weeklyServicesState.addAll(list)
                } else if (weeklyServicesState.isNotEmpty()) {
                    weeklyServicesState.forEach { addChurchService(it) }
                }
            }
            
            db.collection("events").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ChurchEvent::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    eventsState.clear()
                    eventsState.addAll(list)
                } else if (eventsState.isNotEmpty()) {
                    eventsState.forEach { addChurchEvent(it) }
                }
            }
            
            db.collection("carousel_items").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(CarouselItem::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    carouselItemsState.clear()
                    carouselItemsState.addAll(list)
                } else if (carouselItemsState.isNotEmpty()) {
                    carouselItemsState.forEach { addCarouselItem(it) }
                }
            }
            
            db.collection("prayer_requests").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(PrayerRequest::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    prayerRequestsState.clear()
                    prayerRequestsState.addAll(list.sortedByDescending { it.date })
                } else if (prayerRequestsState.isNotEmpty()) {
                    prayerRequestsState.forEach { addPrayerRequest(it) }
                }
            }
            
            db.collection("app_tabs").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(AppTab::class.java) } catch(e: Exception) { null } }
                appTabsState.clear()
                if (list.isNotEmpty()) {
                    appTabsState.addAll(list.sortedBy { it.order })
                    // Ensure all 10 default tabs are present
                    val defaultTabIds = listOf("1", "2", "3", "4", "5", "6", "7", "bible_tab", "team_tab", "8", "9", "10")
                    if (appTabsState.size < defaultTabIds.size) {
                        initializeTabs()
                    }
                } else if (appTabsState.isEmpty()) {
                    initializeTabs()
                }
                if (appTabsState.isNotEmpty() && list.isEmpty()) {
                    appTabsState.forEach { addAppTab(it) }
                }
            }
            
            db.collection("ibr_courses").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(IbrCourse::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    ibrCoursesState.clear()
                    ibrCoursesState.addAll(list)
                } else if (ibrCoursesState.isNotEmpty()) {
                    ibrCoursesState.forEach {
                        db.collection("ibr_courses").document(it.id).set(it)
                    }
                }
            }
            
            db.collection("service_videos").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(ServiceVideoModel::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    serviceVideosState.clear()
                    serviceVideosState.addAll(list)
                } else if (serviceVideosState.isNotEmpty()) {
                    serviceVideosState.forEach {
                        db.collection("service_videos").document(it.id).set(it)
                    }
                }
            }
            
            db.collection("team").orderBy("order").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { try { it.toObject(TeamMember::class.java) } catch(e: Exception) { null } }
                if (list.isNotEmpty()) {
                    teamMembersState.clear()
                    teamMembersState.addAll(list)
                } else if (teamMembersState.isNotEmpty()) {
                    teamMembersState.forEach {
                        db.collection("team").document(it.id).set(it)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


data class TeamMember(
    var id: String = "",
    var name: String = "",
    var role: String = "",
    var imageUrl: String = "",
    var order: Int = 0,
    var category: String = "Pastores" // Pastores, Diretoria, Diáconos, Louvor, etc
)

val teamMembersState = androidx.compose.runtime.mutableStateListOf<TeamMember>()



enum class TabContentType {
    SYSTEM, WEBVIEW, EXTERNAL, NATIVE, PHOTOS, VIDEOS, LINKS, MIXED
}

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
    var webUrl: String = ""
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
    if (recentlyViewedState.size > 10) recentlyViewedState.removeAt(recentlyViewedState.size - 1)
}

val appTabsState = androidx.compose.runtime.mutableStateListOf<AppTab>()

fun initializeTabs() {
    appTabsState.clear()
    val defaultTabs = listOf(
        AppTab("1", "Início", "Home", false, true, true, 0, TabContentType.SYSTEM, Screen.Home.route),
        AppTab("bible_tab", "Bíblia", "MenuBook", false, true, true, 1, TabContentType.SYSTEM, Screen.Content.route),
        AppTab("2", "Cultos", "Church", false, true, true, 2, TabContentType.SYSTEM, Screen.Services.route),
        AppTab("3", "Devocionais", "Book", false, true, true, 3, TabContentType.SYSTEM, Screen.Devotionals.route),
        AppTab("4", "Cursos IBR", "School", false, true, true, 4, TabContentType.SYSTEM, Screen.Ibr.route),
        AppTab("5", "Mídia", "PlayArrow", false, true, true, 5, TabContentType.SYSTEM, Screen.Content.route),
        AppTab("6", "Pedidos de Oração", "Favorite", false, true, true, 6, TabContentType.SYSTEM, Screen.Prayer.route),
        AppTab("team_tab", "Equipe", "Groups", false, true, true, 7, TabContentType.SYSTEM, Screen.Team.route),
        AppTab("7", "Membros", "Person", false, true, true, 8, TabContentType.SYSTEM, Screen.Members.route),
        AppTab("8", "Eventos", "Event", false, true, true, 9, TabContentType.SYSTEM, Screen.About.route),
        AppTab("9", "Ajuda", "Help", false, true, true, 10, TabContentType.SYSTEM, Screen.About.route),
        AppTab("10", "Dízimos e Ofertas", "MonetizationOn", false, true, true, 11, TabContentType.SYSTEM, Screen.About.route)
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
        .get()
        .addOnSuccessListener { result ->
            val list = result.documents.mapNotNull { try { it.toObject(IbrProgress::class.java) } catch(e: Exception) { null } }
            ibrProgressState.clear()
            ibrProgressState.addAll(list)
        }
        .addOnFailureListener { e -> e.printStackTrace() }
}

fun addContentBook(item: ContentBook) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("content_books").document(item.id).set(item)
    }
}
fun removeContentBook(item: ContentBook) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("content_books").document(item.id).delete()
    }
}

fun addContentAudio(item: ContentAudio) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("content_audios").document(item.id).set(item)
    }
}
fun removeContentAudio(item: ContentAudio) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("content_audios").document(item.id).delete()
    }
}

fun addContentVideo(item: ContentVideo) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("content_videos").document(item.id).set(item)
    }
}
fun removeContentVideo(item: ContentVideo) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("content_videos").document(item.id).delete()
    }
}

fun addContentPhotoAlbum(item: ContentPhotoAlbum) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("content_albums").document(item.id).set(item)
    }
}
fun removeContentPhotoAlbum(item: ContentPhotoAlbum) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("content_albums").document(item.id).delete()
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
        Firebase.firestore.collection("service_videos").document(item.id).set(item)
    }
}
fun removeServiceVideo(item: ServiceVideoModel) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("service_videos").document(item.id).delete()
    }
}

fun addDevotional(item: Devotional) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("devotionals").document(item.id).set(item)
    }
}
fun removeDevotional(item: Devotional) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("devotionals").document(item.id).delete()
    }
}

fun addChurchService(item: ChurchService) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("weekly_services").document(item.id).set(item)
    }
}
fun removeChurchService(item: ChurchService) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("weekly_services").document(item.id).delete()
    }
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

fun addPrayerRequest(item: PrayerRequest) {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        Firebase.firestore.collection("prayer_requests").document(item.id).set(item)
    }
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

suspend fun forceSyncEvents() {
    try {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        val eventsSnapshot = db.collection("events").get(com.google.firebase.firestore.Source.SERVER).await()
        val eventsList = eventsSnapshot.documents.mapNotNull { try { it.toObject(ChurchEvent::class.java) } catch(e: Exception) { null } }
        if (eventsList.isNotEmpty()) {
            eventsState.clear()
            eventsState.addAll(eventsList)
        }
        
        val carouselSnapshot = db.collection("carousel_items").get(com.google.firebase.firestore.Source.SERVER).await()
        val carouselList = carouselSnapshot.documents.mapNotNull { try { it.toObject(CarouselItem::class.java) } catch(e: Exception) { null } }
        if (carouselList.isNotEmpty()) {
            carouselItemsState.clear()
            carouselItemsState.addAll(carouselList)
        }
        
        val servicesSnapshot = db.collection("weekly_services").get(com.google.firebase.firestore.Source.SERVER).await()
        val servicesList = servicesSnapshot.documents.mapNotNull { try { it.toObject(ChurchService::class.java) } catch(e: Exception) { null } }
        if (servicesList.isNotEmpty()) {
            weeklyServicesState.clear()
            weeklyServicesState.addAll(servicesList)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
