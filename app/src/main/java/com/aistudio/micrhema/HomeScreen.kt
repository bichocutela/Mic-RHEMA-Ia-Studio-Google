package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit = {}) {
    val scrollState = rememberScrollState()

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                coroutineScope.launch {
                    isRefreshing = true
                    forceRefreshData()
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Service Alert
            val calendar = java.util.Calendar.getInstance()
            val todayDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            val tomorrowCalendar = java.util.Calendar.getInstance()
            tomorrowCalendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            val tomorrowDayOfWeek = tomorrowCalendar.get(java.util.Calendar.DAY_OF_WEEK)
            val dayMap = mapOf(
                java.util.Calendar.SUNDAY to "Domingo",
                java.util.Calendar.MONDAY to "Segunda",
                java.util.Calendar.TUESDAY to "Terça",
                java.util.Calendar.WEDNESDAY to "Quarta",
                java.util.Calendar.THURSDAY to "Quinta",
                java.util.Calendar.FRIDAY to "Sexta",
                java.util.Calendar.SATURDAY to "Sábado"
            )
            val todayStr = dayMap[todayDayOfWeek] ?: ""
            val tomorrowStr = dayMap[tomorrowDayOfWeek] ?: ""
            
            val todayService = weeklyServicesState.find { it.day.equals(todayStr, ignoreCase = true) || it.day.contains(todayStr, ignoreCase = true) }
            val tomorrowService = weeklyServicesState.find { it.day.equals(tomorrowStr, ignoreCase = true) || it.day.contains(tomorrowStr, ignoreCase = true) }
            
            var alertMessage = ""
            var alertTime = ""
            if (todayService != null) {
                var isPast = false
                try {
                    val currentTime = java.util.Calendar.getInstance()
                    val currentHour = currentTime.get(java.util.Calendar.HOUR_OF_DAY)
                    val currentMinute = currentTime.get(java.util.Calendar.MINUTE)
                    val timeParts = todayService.time.split(":")
                    if (timeParts.size >= 2) {
                        val serviceHour = timeParts[0].trim().toInt()
                        val serviceMinuteStr = timeParts[1].trim().take(2)
                        val serviceMinute = serviceMinuteStr.toInt()
                        if (currentHour > serviceHour || (currentHour == serviceHour && currentMinute >= serviceMinute)) {
                            isPast = true
                        }
                    }
                } catch (e: Exception) {}
                
                if (isPast) {
                    alertMessage = "Hoje teve ${todayService.title}"
                } else {
                    alertMessage = "Hoje tem ${todayService.title}"
                }
                alertTime = "Às ${todayService.time}"
            } else if (tomorrowService != null) {
                alertMessage = "Amanhã é dia de ${tomorrowService.title}"
                alertTime = "Às ${tomorrowService.time}"
            }
            
            if (alertMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Outlined.DateRange, contentDescription = "Alerta", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(alertMessage, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(alertTime, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            // Featured Carousel
            val bannerState = rememberLazyListState()
            
            val validBanners = carouselItemsState.filter { banner ->
                if (banner.eventDate.isEmpty()) true
                else {
                    try {
                        val date = java.time.LocalDate.parse(banner.eventDate)
                        val today = java.time.LocalDate.now()
                        !date.isBefore(today)
                    } catch (e: Exception) {
                        true
                    }
                }
            }

            LaunchedEffect(validBanners.size) {
                if (validBanners.size > 1) {
                    while (true) {
                        kotlinx.coroutines.delay(4000)
                        val nextItem = (bannerState.firstVisibleItemIndex + 1) % validBanners.size
                        bannerState.animateScrollToItem(nextItem)
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (validBanners.isNotEmpty()) {
                    LazyRow(
                        state = bannerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true
                    ) {
                        items(validBanners) { banner ->
                            AsyncImage(
                                model = banner.imageUrl ?: "",
                                contentDescription = banner.title.ifEmpty { "Destaque" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }

            val carouselNews = if (bibleNewsState.isEmpty()) BibleNewsData.newsList else bibleNewsState
            val listState = rememberLazyListState()
            
            LaunchedEffect(carouselNews) {
                while (true) {
                    kotlinx.coroutines.delay(4000)
                    if (carouselNews.isNotEmpty()) {
                        val currentItem = listState.firstVisibleItemIndex
                        val nextItem = (currentItem + 1) % carouselNews.size
                        listState.animateScrollToItem(nextItem)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notícias Bíblicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Ver todos", 
                        style = MaterialTheme.typography.labelLarge, 
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigate("news_list") }
                    )
                }
                
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(carouselNews) { news ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .clickable { onNavigate("news_detail/${news.id}") },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column {
                                AsyncImage(
                                    model = news.imageUrl,
                                    contentDescription = news.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = news.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${news.book} ${news.chapter}:${news.verse}",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
                Spacer(modifier = Modifier.height(40.dp))
        }
        } // end PullToRefreshBox
    }
}

@Composable
fun QuickActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(72.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)),
            modifier = Modifier.size(56.dp).clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun NewsCard(title: String, subtitle: String, imageUrl: String) {
    Column(
        modifier = Modifier.width(140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            maxLines = 3,
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
