package com.aistudio.micrhema

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf

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
    val content: String
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
    val tag: String // "EVENTO" ou "NOTÍCIA"
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



object ThemeManager {
    private const val PREFS_NAME = "micrhema_theme_prefs"
    private const val KEY_IS_DARK_THEME = "is_dark_theme"

    fun isDarkTheme(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_DARK_THEME, false)
    }

    fun setDarkTheme(context: android.content.Context, isDark: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DARK_THEME, isDark).apply()
    }
}

val isAppDarkTheme = androidx.compose.runtime.mutableStateOf(false)

data class ContentBook(
    val id: String,
    val title: String,
    val author: String,
    val coverUrl: String,
    val contentText: String, // Mocking the epub/pdf with plain text for this prototype
    val isCached: Boolean = false,
    val progress: Float = 0f
)

data class ContentAudio(
    val id: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String,
    val isCached: Boolean = false,
    val progress: Float = 0f
)

data class ContentVideo(
    val id: String,
    val title: String,
    val description: String,
    val videoUrl: String, // Can be youtube link or direct mp4
    val thumbnailUrl: String,
    val isCached: Boolean = false,
    val progress: Float = 0f
)

val contentBooksState = androidx.compose.runtime.mutableStateListOf<ContentBook>()
val contentAudiosState = androidx.compose.runtime.mutableStateListOf<ContentAudio>()
val contentVideosState = androidx.compose.runtime.mutableStateListOf<ContentVideo>()

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
    val progress: Float = 0f
)

val recentlyViewedState = androidx.compose.runtime.mutableStateListOf<RecentlyViewedItem>()

fun addRecentlyViewed(item: RecentlyViewedItem) {
    recentlyViewedState.removeAll { it.id == item.id && it.type == item.type }
    recentlyViewedState.add(0, item)
    if (recentlyViewedState.size > 10) {
        recentlyViewedState.removeAt(recentlyViewedState.size - 1)
    }
}
