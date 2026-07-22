package com.aistudio.micrhema

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import android.util.Log

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

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
    val id: String,
    val title: String,
    val date: String,
    val verse: String,
    val verseReference: String,
    val content: String,
    var likes: Int = 0
)

data class ChurchService(
    val id: String,
    val day: String,
    val dayShort: String,
    val time: String,
    val title: String,
    val description: String
)

data class ChurchEvent(
    val id: String,
    val title: String,
    val date: String,
    val description: String,
    val location: String
)

data class PrayerRequest(
    val id: String,
    val name: String,
    val request: String,
    val date: String
)

data class CarouselItem(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val tag: String, // "EVENTO" ou "NOTÍCIA"
    val imageUrl: String? = null
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
    val id: String,
    val name: String,
    val email: String,
    val isApproved: Boolean = false,
    val isVip: Boolean = false,
    val isIbr: Boolean = false
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
            db.collection("members").document(member.id).delete()
        } catch (e: Exception) {
            Log.e("MemberManager", "Firestore not initialized or error", e)
        }
    }

    fun saveToFirestore(member: MemberRequest) {
        try {
            val db = Firebase.firestore
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
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (member == null) {
            prefs.edit().remove(KEY_LOGGED_IN_ID).apply()
        } else {
            prefs.edit().putString(KEY_LOGGED_IN_ID, member.id).apply()
        }
    }
}


data class IbrChapter(
    val id: String,
    val title: String,
    val description: String = "",
    val durationMinutes: Int,
    val videoUrl: String = "", // URL to video stream or YouTube
    val audioUrl: String = "", // URL to audio stream
    val isYoutube: Boolean = false,
    val youtubeId: String = "" // if Youtube link
)

data class IbrCourse(
    val id: String,
    val title: String,
    val theme: String, // e.g., "Teologia", "História Bíblica", "Vida Cristã"
    val description: String,
    val imageUrl: String = "",
    val chapters: List<IbrChapter> = emptyList()
)

data class IbrProgress(
    val courseId: String,
    val chapterId: String,
    val lastPositionSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val isCompleted: Boolean = false
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
                videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
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
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val contentText: String, // Mocking the epub/pdf with plain text for this prototype
    val isCached: Boolean = false,
    val progress: Float = 0f,
    var lastPosition: Long = 0L
)

data class ContentAudio(
    val id: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String,
    val isCached: Boolean = false,
    val progress: Float = 0f,
    var lastPosition: Long = 0L
)

data class ContentVideo(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String, // Can be youtube link or direct mp4
    val thumbnailUrl: String,
    val isCached: Boolean = false,
    val progress: Float = 0f,
    var lastPosition: Long = 0L
)

data class AlbumPhoto(
    val url: String,
    val caption: String = ""
)

data class ContentPhotoAlbum(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String? = null,
    val photos: List<AlbumPhoto> = emptyList()
)

val contentBooksState = androidx.compose.runtime.mutableStateListOf<ContentBook>()
val contentAudiosState = androidx.compose.runtime.mutableStateListOf<ContentAudio>()
val contentVideosState = androidx.compose.runtime.mutableStateListOf<ContentVideo>()
val contentAlbumsState = androidx.compose.runtime.mutableStateListOf<ContentPhotoAlbum>()

fun initializeMockContent() {
    if (contentBooksState.isEmpty()) {
        contentBooksState.add(
            ContentBook("1", "O Poder da Oração", "E.M. Bounds", "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=500&q=80", "A oração é a força mais poderosa da terra...", isCached = true, progress = 0.1f)
        )
    }
    if (contentAudiosState.isEmpty()) {
        contentAudiosState.add(
            ContentAudio("1", "Mensagem de Fé", "Pr. Presidente", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80", isCached = true, progress = 0.4f)
        )
    }
    if (contentVideosState.isEmpty()) {
        contentVideosState.add(
            ContentVideo("1", "Culto Especial de Domingo", "Mensagem sobre a graça de Deus", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "https://images.unsplash.com/photo-1505764761634-1d77b57e1966?w=500&q=80", isCached = true, progress = 0.8f)
        )
    }
}

enum class ContentType { BOOK, AUDIO, VIDEO }

data class RecentlyViewedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val type: ContentType,
    val isCached: Boolean = false,
    val progress: Float = 0f,
    var lastPosition: Long = 0L
)

val recentlyViewedState = androidx.compose.runtime.mutableStateListOf<RecentlyViewedItem>()

fun addRecentlyViewed(item: RecentlyViewedItem) {
    recentlyViewedState.removeAll { it.id == item.id && it.type == item.type }
    recentlyViewedState.add(0, item)
    if (recentlyViewedState.size > 10) {
        recentlyViewedState.removeAt(recentlyViewedState.size - 1)
    }
}


enum class TabContentType {
    SYSTEM, PHOTOS, VIDEOS, LINKS, MIXED
}

data class AppTab(
    val id: String,
    val title: String,
    val iconName: String,
    val isPrivate: Boolean,
    val isVisible: Boolean,
    val showInBottomBar: Boolean,
    val order: Int,
    val type: TabContentType,
    val systemRoute: String? = null
)

data class CustomTabItem(
    val id: String,
    val tabId: String,
    val type: TabContentType,
    val url: String,
    val title: String = "",
    val description: String = ""
)

val appTabsState = androidx.compose.runtime.mutableStateListOf<AppTab>()
val customTabItemsState = androidx.compose.runtime.mutableStateListOf<CustomTabItem>()

fun initializeTabs() {
    if (appTabsState.isEmpty()) {
        appTabsState.addAll(listOf(
            AppTab("1", "Início", "Home", false, true, true, 0, TabContentType.SYSTEM, "home"),
            AppTab("2", "Devocionais", "Book", false, true, true, 1, TabContentType.SYSTEM, "devotionals"),
            AppTab("3", "Cultos", "Church", false, true, true, 2, TabContentType.SYSTEM, "services"),
            AppTab("4", "Conteúdo", "LibraryBooks", false, true, true, 3, TabContentType.SYSTEM, "content"),
            AppTab("5", "Oração", "Favorite", false, true, false, 4, TabContentType.SYSTEM, "prayer"),
            AppTab("6", "Membro (VIP)", "People", true, true, false, 5, TabContentType.SYSTEM, "members"),
            AppTab("7", "IBR", "Group", false, true, false, 6, TabContentType.SYSTEM, "ibr"),
            AppTab("bible_tab", "Bíblia", "MenuBook", false, true, false, 7, TabContentType.SYSTEM, "bible"),
            AppTab("team_tab", "Equipe", "Groups", false, true, false, 8, TabContentType.SYSTEM, "team"),
            AppTab("8", "Sobre", "Info", false, true, false, 9, TabContentType.SYSTEM, "about"),
            AppTab("9", "Configurações", "Settings", false, true, false, 9, TabContentType.SYSTEM, "settings"),
            AppTab("10", "Área ADM", "Lock", true, true, false, 10, TabContentType.SYSTEM, "admin")
        ))
    }
}


data class TeamMember(
    val id: String = "",
    val name: String = "",
    val role: String = "",
    val imageUrl: String = "",
    val order: Int = 0,
    val category: String = "Pastoral"
)

val teamMembersState = androidx.compose.runtime.mutableStateListOf<TeamMember>(
    TeamMember("1", "Evaldo e Denilza", "Pastor e Fundador da Igreja", "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000506-55fe455fe5/IMG_2580.jpeg?ph=bf16b0ed3a", 0, "Pastoral"),
    TeamMember("2", "Pb Alessandro e Silvana", "Departamento Missões", "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000508-0cca70cca9/IMG_2582.jpeg?ph=bf16b0ed3a", 1, "Missões"),
    TeamMember("3", "Dac. Rosemeiry", "Tesoureira", "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000494-ee984ee985/1000030995.png?ph=bf16b0ed3a", 2, "Secretaria"),
    TeamMember("4", "Pr Alexsandro e Pra Antônia", "Pastores Auxiliar", "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000510-454a6454a9/IMG_2584.jpeg?ph=bf16b0ed3a", 3, "Pastoral"),
    TeamMember("5", "Júlia", "Departamento Crianças", "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000498-5503255034/1000031100.png?ph=bf16b0ed3a", 4, "Infantil"),
    TeamMember("6", "Dac. Priscila e Josineide", "", "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000530-d79abd79ad/IMG-20260215-WA0022.jpeg?ph=bf16b0ed3a", 5, "Secretaria"),
    TeamMember("7", "Edimara de Andrade", "Pastora Cong. Mãe Luiza", "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000514-e45b9e45bb/IMG_2586.jpeg?ph=bf16b0ed3a", 6, "Pastoral"),
    TeamMember("8", "Josineide e Lucineide", "Dirigentes do Matutino", "https://bf16b0ed3a.cbaul-cdnwnd.com/33d00cab3ecde9380e3cf364b55ce6c5/200000528-69b4a69b4c/IMG-20260210-WA0080.jpeg?ph=bf16b0ed3a", 7, "Pastoral")
)

fun loadTeamMembersFromFirebase() {
    if (com.aistudio.micrhema.BuildConfig.FIREBASE_PROJECT_ID.isNotEmpty()) {
        try {
            val db = Firebase.firestore
            db.collection("team").orderBy("order").addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.w("Data", "Listen team failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val newList = mutableListOf<TeamMember>()
                    for (document in snapshot.documents) {
                        val id = document.id
                        val name = document.getString("name") ?: ""
                        val role = document.getString("role") ?: ""
                        val imageUrl = document.getString("imageUrl") ?: ""
                        val order = document.getLong("order")?.toInt() ?: 0
                        val category = document.getString("category") ?: "Pastoral"
                        newList.add(TeamMember(id, name, role, imageUrl, order, category))
                    }
                    teamMembersState.clear()
                    teamMembersState.addAll(newList)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Data", "Error loading team", e)
        }
    }
}
