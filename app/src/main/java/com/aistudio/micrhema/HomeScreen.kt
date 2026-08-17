package com.aistudio.micrhema
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay


import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit = {}) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    val currentMember = loggedInMemberState.value
    
    // Data filtering for Banners
    val today = LocalDate.now()
    val validBanners = carouselItemsState.filter { banner ->
        if (banner.eventDate.isBlank()) return@filter true
        try {
            val date = LocalDate.parse(banner.eventDate) // format yyyy-MM-dd
            !date.isBefore(today)
        } catch (e: DateTimeParseException) {
            true // If format is invalid, keep it to not break the app
        }
    }
    
    // Sort logic for Services
    val dayMap = mapOf(
        "Domingo" to DayOfWeek.SUNDAY,
        "Segunda" to DayOfWeek.MONDAY,
        "Segunda-feira" to DayOfWeek.MONDAY,
        "Terça" to DayOfWeek.TUESDAY,
        "Terça-feira" to DayOfWeek.TUESDAY,
        "Quarta" to DayOfWeek.WEDNESDAY,
        "Quarta-feira" to DayOfWeek.WEDNESDAY,
        "Quinta" to DayOfWeek.THURSDAY,
        "Quinta-feira" to DayOfWeek.THURSDAY,
        "Sexta" to DayOfWeek.FRIDAY,
        "Sexta-feira" to DayOfWeek.FRIDAY,
        "Sábado" to DayOfWeek.SATURDAY
    )
    val currentTime = java.time.LocalTime.now()
    val currentDayOfWeek = today.dayOfWeek
    val validServices = weeklyServicesState.sortedBy { service ->
        val serviceDay = dayMap[service.day] ?: DayOfWeek.SUNDAY
        var diff = serviceDay.value - currentDayOfWeek.value
        
        var parsedTime = LocalTime.of(23, 59)
        try {
            val cleanTime = service.time.replace("h", ":", ignoreCase = true).filter { it.isDigit() || it == ':' }
            val timeParts = cleanTime.split(":")
            if (timeParts.size >= 2) {
                parsedTime = LocalTime.of(timeParts[0].toInt(), timeParts[1].take(2).toInt())
            }
        } catch (e: Exception) {
            // Treat invalid times safely without crashing
        }

        if (diff < 0) {
            diff += 7
        } else if (diff == 0) {
            if (parsedTime.isBefore(currentTime)) {
                diff += 7
            }
        }
        
        today.plusDays(diff.toLong()).atTime(parsedTime)
    }.take(3)
    
    // Devotional Logic
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val todayStr = today.format(formatter)
    val todayDevotional = devotionalsState.find { it.date == todayStr } ?: devotionalsState.firstOrNull()
    
    val editorialNews = BibleNewsEditorial.withEditorialCatalog(
        if (bibleNewsState.isEmpty()) BibleNewsData.newsList else bibleNewsState.toList()
    )
    val latestNews = editorialNews
        .filter { it.featured }
        .ifEmpty { editorialNews }
        .sortedByDescending { it.publishedAt }
        .take(5)
    
    // Mood State
    var showMoodSelector by remember { mutableStateOf(false) }
    val prefs = context.getSharedPreferences("mic_rhema_prefs", Context.MODE_PRIVATE)
    
    val todayDateStr = today.toString()
    
    var savedMoodKey by remember { mutableStateOf(prefs.getString("moodKey", null)) }
    var savedMoodDate by remember { mutableStateOf(prefs.getString("moodDate", null)) }
    
    if (savedMoodDate != todayDateStr) {
        savedMoodKey = null
        savedMoodDate = null
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                try {
                    refreshHomeData()
                } catch (e: Exception) {
                    Toast.makeText(context, "Não foi possível atualizar a página inicial.", Toast.LENGTH_SHORT).show()
                } finally {
                    isRefreshing = false
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 100.dp), // Padding for Bottom Navigation
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // 1. Saudação
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            if (currentMember != null && currentMember.name.isNotBlank()) {
                val firstName = currentMember.name.split(" ").firstOrNull() ?: ""
                Text(
                    text = "Olá, $firstName 👋",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            } else {
                Text(
                    text = "Seja bem-vindo à Rhema",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Que a paz do Senhor esteja com você",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        
        // 1.5. Culto Hoje Aviso Contextual
        val todayServices = weeklyServicesState.filter { service -> 
            val serviceDay = dayMap[service.day] ?: DayOfWeek.SUNDAY
            serviceDay == currentDayOfWeek
        }.map { service ->
            var parsedTime = LocalTime.of(23, 59)
            try {
                val cleanTime = service.time.replace("h", ":", ignoreCase = true).filter { it.isDigit() || it == ':' }
                val timeParts = cleanTime.split(":")
                if (timeParts.size >= 2) {
                    parsedTime = LocalTime.of(timeParts[0].toInt(), timeParts[1].take(2).toInt())
                }
            } catch (e: Exception) {}
            Pair(service, parsedTime)
        }.sortedBy { it.second }
        
        if (todayServices.isNotEmpty()) {
            var targetServicePair = todayServices.find { it.second.isAfter(currentTime) || it.second == currentTime }
            var isFuture = true
            
            if (targetServicePair == null) {
                targetServicePair = todayServices.last()
                isFuture = false
            }
            
            val targetService = targetServicePair.first
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable { onNavigate(Screen.Services.route) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = "Culto de Hoje",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (isFuture) "Hoje tem ${targetService.title}" else "Hoje teve ${targetService.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Às ${targetService.time}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // 2. Destaques / Banners
        if (validBanners.isNotEmpty()) {
            val bannerListState = rememberLazyListState()
            Column {
                LazyRow(
                    state = bannerListState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(validBanners) { banner ->
                        val hasEventInfo = banner.eventInfo.isNotBlank()
                        Card(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clickable(
                                    enabled = hasEventInfo,
                                    onClick = { onNavigate(Screen.Services.route) }
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                AsyncImage(
                                    model = banner.imageUrl?.takeIf { it.isNotBlank() },
                                    contentDescription = banner.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                                if (banner.tag.isNotBlank() || banner.title.isNotBlank() || banner.description.isNotBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.58f))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            if (banner.tag.isNotBlank()) {
                                                Text(
                                                    text = banner.tag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White.copy(alpha = 0.8f),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            if (banner.title.isNotBlank()) {
                                                Text(
                                                    text = banner.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (banner.description.isNotBlank()) {
                                                Text(
                                                    text = banner.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (validBanners.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val currentItem = bannerListState.firstVisibleItemIndex
                        validBanners.indices.forEach { index ->
                            val color = if (index == currentItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(8.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    }
                }
            }
        }
        
        // 3. Como você está se sentindo hoje?
        MoodCard(
            savedMoodKey = savedMoodKey,
            onSelectMood = { showMoodSelector = true },
            onNavigate = onNavigate
        )
        
        // 4. Atalhos rápidos
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionItem(icon = Icons.Outlined.Book, label = "Bíblia", onClick = { onNavigate("bible") })
            QuickActionItem(icon = PrayingHandsIcon, label = "Pedidos", onClick = { onNavigate(Screen.Prayer.route) })
            QuickActionItem(icon = Icons.Outlined.DateRange, label = "Planos", onClick = { onNavigate(Screen.Plans.route) })
            QuickActionItem(icon = Icons.Filled.People, label = "Membros", onClick = { onNavigate(Screen.Members.route) })
        }
        
        // 5. Devocional Diário
        if (todayDevotional != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onNavigate(Screen.Devotionals.route + "?id=${todayDevotional.id}") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Devocional Diário", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ler", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Ler", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(todayDevotional.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(todayDevotional.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(todayDevotional.content, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        
        // 6. Notícias Bíblicas
        if (latestNews.isNotEmpty()) {
            HomeSectionHeader(title = "Notícias Bíblicas", action = "Ver todas", onAction = { onNavigate("news_list") })
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(latestNews) { news ->
                    Card(
                        modifier = Modifier.width(240.dp).clickable { onNavigate("news_detail/${news.id}") },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            BibleNewsImage(
                                news = news,
                                contentDescription = news.title,
                                modifier = Modifier.fillMaxWidth().height(120.dp)
                            )
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(news.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(news.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text("${news.book} ${news.chapter}:${news.verse} • ${BibleNewsEditorial.intensityLabel(news.intensity)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        
        // 7. Próximos Cultos
        if (validServices.isNotEmpty()) {
            HomeSectionHeader(title = "Próximos Cultos", action = "Ver", onAction = { onNavigate(Screen.Services.route) })
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(validServices) { service ->
                    Card(
                        modifier = Modifier.width(200.dp).clickable { onNavigate(Screen.Services.route) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                                Text(service.dayShort, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                val dayNum = "🗓"
                                Text(dayNum, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(service.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(service.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        
        // 8. Mídia
        val recentVideos = contentVideosState
            .sortedByDescending { it.id.toLongOrNull() ?: Long.MIN_VALUE }
            .take(3)
        val recentAudios = contentAudiosState
            .sortedByDescending { it.id.toLongOrNull() ?: Long.MIN_VALUE }
            .take(3)
        val recentBooks = contentBooksState
            .sortedByDescending { it.id.toLongOrNull() ?: Long.MIN_VALUE }
            .take(3)
        val hasMedia = recentVideos.isNotEmpty() || recentAudios.isNotEmpty() || recentBooks.isNotEmpty()
        if (hasMedia) {
            HomeSectionHeader(title = "Mídia", action = "Ver todas", onAction = { onNavigate(Screen.Content.route) })
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(recentVideos) { video ->
                    MediaCard(
                        title = video.title,
                        type = "Vídeo",
                        icon = Icons.Filled.OndemandVideo,
                        cover = video.thumbnailUrl,
                        videoUrl = video.videoUrl,
                        onClick = { onNavigate("${Screen.Content.route}?type=video&id=${video.id}") }
                    )
                }
                items(recentAudios) { audio ->
                    MediaCard(title = audio.title, type = "Áudio", icon = Icons.Filled.MusicNote, cover = audio.coverUrl, onClick = { onNavigate("${Screen.Content.route}?type=audio&id=${audio.id}") })
                }
                items(recentBooks) { book ->
                    MediaCard(title = book.title, type = "Livro", icon = Icons.Outlined.Book, cover = book.coverUrl, onClick = { onNavigate("${Screen.Content.route}?type=book&id=${book.id}") })
                }
            }
        }
    }
    }
    
    // Mood Bottom Sheet
    if (showMoodSelector) {
        ModalBottomSheet(
            onDismissRequest = { showMoodSelector = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Como está seu coração hoje?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                val moods = listOf(
                    MoodItem("Feliz", "😊", "Alegria"),
                    MoodItem("Ansioso", "😟", "Ansiedade"),
                    MoodItem("Triste", "😔", "Esperança"),
                    MoodItem("Com medo", "😨", "Medo"),
                    MoodItem("Irritado", "😤", "Raiva"),
                    MoodItem("Desanimado", "😞", "Esperança"),
                    MoodItem("Em paz", "😌", "Paz"),
                    MoodItem("Preciso de esperança", "🙏", "Esperança")
                )
                
                // Chunk to rows of 2
                moods.chunked(2).forEach { rowMoods ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowMoods.forEach { mood ->
                            Card(
                                modifier = Modifier.weight(1f).clickable {
                                    prefs.edit().putString("moodKey", mood.title).putString("moodDate", todayDateStr).apply()
                                    savedMoodKey = mood.title
                                    showMoodSelector = false
                                    onNavigate(Screen.Plans.route + "?theme=" + Uri.encode(getMappedPlan(mood.title)))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(mood.emoji, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(mood.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

data class MoodItem(val title: String, val emoji: String, val planCategory: String)

@Composable
fun MoodCard(savedMoodKey: String?, onSelectMood: () -> Unit, onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectMood() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    if (savedMoodKey == null) {
                        Text("Como você está se sentindo hoje?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Toque para escolher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    } else {
                        Text("Hoje você marcou: $savedMoodKey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Toque para alterar o sentimento", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
            if (savedMoodKey != null) {
                val mappedPlan = getMappedPlan(savedMoodKey)
                Row(
                    modifier = Modifier
                        .clickable { onNavigate(Screen.Plans.route + "?theme=" + Uri.encode(mappedPlan)) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Ver plano:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(mappedPlan, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Ver Plano", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

fun getMappedPlan(mood: String): String {
    return when(mood) {
        "Feliz", "Grato" -> "Alegria"
        "Ansioso" -> "Ansiedade"
        "Triste", "Desanimado", "Preciso de esperança" -> "Esperança"
        "Com medo" -> "Medo"
        "Irritado" -> "Raiva"
        "Sem paciência" -> "Paciência"
        "Em paz" -> "Paz"
        "Sofrendo", "Ferido" -> "Cura"
        "Saudade / Perda" -> "Perda"
        "Em dúvida" -> "Dúvida"
        "Tentado" -> "Tentação"
        "Estressado" -> "Estresse"
        "Com inveja" -> "Inveja"
        else -> "Esperança"
    }
}

@Composable
fun HomeSectionHeader(title: String, action: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            action, 
            style = MaterialTheme.typography.labelLarge, 
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onAction() }
        )
    }
}

@Composable
fun MediaCard(title: String, type: String, icon: ImageVector, cover: String, videoUrl: String = "", onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                if (videoUrl.isNotBlank() && (type == "Vídeo" || isYoutubeUrl(videoUrl))) {
                    YoutubeThumbnailImage(
                        videoUrl = videoUrl,
                        explicitThumbnailUrl = cover,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = cover,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(type, color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                title, 
                style = MaterialTheme.typography.titleSmall, 
                fontWeight = FontWeight.Bold, 
                maxLines = 2, 
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun QuickActionItem(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(72.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp).clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

val PrayingHandsIcon: ImageVector
    get() = ImageVector.Builder(
        name = "PrayingHands",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.43f, 9.67f)
            curveTo(11.47f, 9.78f, 11.5f, 9.88f, 11.5f, 10f)
            verticalLineTo(15.22f)
            curveTo(11.5f, 15.72f, 11.31f, 16.2f, 10.97f, 16.57f)
            lineTo(8.18f, 19.62f)
            lineTo(4.78f, 16.22f)
            lineTo(6f, 15f)
            lineTo(8.8f, 2.86f)
            curveTo(8.92f, 2.36f, 9.37f, 2f, 9.89f, 2f)
            curveTo(10.5f, 2f, 11f, 2.5f, 11f, 3.11f)
            verticalLineTo(8.07f)
            curveTo(10.84f, 8.03f, 10.67f, 8f, 10.5f, 8f)
            curveTo(9.4f, 8f, 8.5f, 8.9f, 8.5f, 10f)
            verticalLineTo(13f)
            curveTo(8.5f, 13.28f, 8.72f, 13.5f, 9f, 13.5f)
            reflectiveCurveTo(9.5f, 13.28f, 9.5f, 13f)
            verticalLineTo(10f)
            curveTo(9.5f, 9.45f, 9.95f, 9f, 10.5f, 9f)
            curveTo(10.69f, 9f, 10.85f, 9.07f, 11f, 9.16f)
            curveTo(11.12f, 9.23f, 11.21f, 9.32f, 11.3f, 9.42f)
            curveTo(11.33f, 9.46f, 11.36f, 9.5f, 11.38f, 9.55f)
            curveTo(11.4f, 9.59f, 11.42f, 9.63f, 11.43f, 9.67f)
            moveTo(2f, 19f)
            lineTo(6f, 22f)
            lineTo(7.17f, 20.73f)
            lineTo(3.72f, 17.28f)
            lineTo(2f, 19f)
            moveTo(18f, 15f)
            lineTo(15.2f, 2.86f)
            curveTo(15.08f, 2.36f, 14.63f, 2f, 14.11f, 2f)
            curveTo(13.5f, 2f, 13f, 2.5f, 13f, 3.11f)
            verticalLineTo(8.07f)
            curveTo(13.16f, 8.03f, 13.33f, 8f, 13.5f, 8f)
            curveTo(14.6f, 8f, 15.5f, 8.9f, 15.5f, 10f)
            verticalLineTo(13f)
            curveTo(15.5f, 13.28f, 15.28f, 13.5f, 15f, 13.5f)
            reflectiveCurveTo(14.5f, 13.28f, 14.5f, 13f)
            verticalLineTo(10f)
            curveTo(14.5f, 9.45f, 14.05f, 9f, 13.5f, 9f)
            curveTo(13.31f, 9f, 13.15f, 9.07f, 13f, 9.16f)
            curveTo(12.88f, 9.23f, 12.79f, 9.32f, 12.71f, 9.42f)
            curveTo(12.68f, 9.46f, 12.64f, 9.5f, 12.62f, 9.55f)
            curveTo(12.6f, 9.59f, 12.58f, 9.63f, 12.57f, 9.67f)
            curveTo(12.53f, 9.78f, 12.5f, 9.88f, 12.5f, 10f)
            verticalLineTo(15.22f)
            curveTo(12.5f, 15.72f, 12.69f, 16.2f, 13.03f, 16.57f)
            lineTo(15.82f, 19.62f)
            lineTo(19.22f, 16.22f)
            lineTo(18f, 15f)
            moveTo(20.28f, 17.28f)
            lineTo(16.83f, 20.73f)
            lineTo(18f, 22f)
            lineTo(22f, 19f)
            lineTo(20.28f, 17.28f)
            close()
        }
    }.build()
