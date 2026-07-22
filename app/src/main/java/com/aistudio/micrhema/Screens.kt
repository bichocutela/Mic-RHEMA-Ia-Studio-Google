package com.aistudio.micrhema

import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase

import android.content.Intent
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun shimmerBrush(
    showShimmer: Boolean = true,
    targetValue: Float = 1000f
): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color(0xFFE5E5E5),
            Color(0xFFF2F2F2),
            Color(0xFFE5E5E5)
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset.Zero
        )
    }
}

@Composable
fun SkeletonItem(
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 100.dp,
    height: androidx.compose.ui.unit.Dp = 20.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(shimmerBrush())
    )
}

@Composable
fun SkeletonCircle(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(shimmerBrush())
    )
}

@Composable
fun BreathingBadge(text: String, tag: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "opacity"
    )
    
    val backgroundColor = if (tag == "EVENTO") Color(0xFFD4AF37) else Color(0xFF3B82F6)
    
    Row(
        modifier = Modifier
            .scale(scale)
            .alpha(opacity)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor.copy(alpha = 0.15f))
            .border(BorderStroke(1.dp, backgroundColor.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(backgroundColor)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = backgroundColor
        )
    }
}

@Composable
fun InteractiveCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    colors: CardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )
    
    val cardModifier = modifier.scale(scale)
    
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            border = border,
            colors = colors,
            shape = RoundedCornerShape(24.dp),
            content = {
                Column {
                    content()
                }
            }
        )
    } else {
        Card(
            modifier = cardModifier,
            border = border,
            colors = colors,
            shape = RoundedCornerShape(24.dp),
            content = {
                Column {
                    content()
                }
            }
        )
    }
}

@Composable
fun HomeCarousel() {
    if (carouselItemsState.isEmpty()) return
    
    var activeIndex by remember { mutableStateOf(0) }
    var selectedCarouselItem by remember { mutableStateOf<CarouselItem?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Destaques & Eventos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "${carouselItemsState.size} itens",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(carouselItemsState) { index, item ->
                val isSelected = activeIndex == index
                
                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.02f else 0.98f,
                    animationSpec = tween(300),
                    label = "scale"
                )
                
                val gradient = if (item.tag == "EVENTO") {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFB8860B), Color(0xFFD4AF37))
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                    )
                }
                
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .height(150.dp)
                        .scale(animatedScale)
                        .clickable { 
                            activeIndex = index 
                            selectedCarouselItem = item
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (item.imageUrl != null) {
                            coil.compose.AsyncImage(
                                model = item.imageUrl,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Glass overlay
                            Box(modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.1f),
                                        Color.Black.copy(alpha = 0.8f)
                                    )
                                )
                            ))
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(gradient))
                        }
                        
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BreathingBadge(text = item.tag, tag = item.tag)
                                
                                Text(
                                    text = item.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    maxLines = 2,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            carouselItemsState.forEachIndexed { index, _ ->
                val isSelected = activeIndex == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "indicator_width"
                )
                val color by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    label = "indicator_color"
                )
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(height = 8.dp, width = width)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { activeIndex = index }
                )
            }
        }
    }

    if (selectedCarouselItem != null) {
        AlertDialog(
            onDismissRequest = { selectedCarouselItem = null },
            title = {
                Text(
                    text = selectedCarouselItem?.title ?: "",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    if (selectedCarouselItem?.imageUrl != null) {
                        coil.compose.AsyncImage(
                            model = selectedCarouselItem?.imageUrl,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    Text(
                        text = selectedCarouselItem?.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Data",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedCarouselItem?.date ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCarouselItem = null }) {
                    Text("Fechar")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var activeDevotionalForReading by remember { mutableStateOf<Devotional?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var devFontSize by remember { mutableStateOf(16f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        isLoading = false
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(1500)
                
                val versesList = listOf(
                    Pair("\"Porque Deus amou o mundo de tal maneira que deu o seu Filho unigênito, para que todo aquele que nele crê não pereça, mas tenha a vida eterna.\"", "João 3:16"),
                    Pair("\"O Senhor é o meu pastor, nada me faltará.\"", "Salmo 23:1"),
                    Pair("\"Posso todas as coisas naquele que me fortalece.\"", "Filipenses 4:13"),
                    Pair("\"Eu sou o caminho, e a verdade e a vida; ninguém vem ao Pai, senão por mim.\"", "João 14:6"),
                    Pair("\"Tudo tem o seu tempo determinado, e há tempo para todo o propósito debaixo do céu.\"", "Eclesiastes 3:1"),
                    Pair("\"Lâmpada para os meus pés é tua palavra, e luz para o meu caminho.\"", "Salmo 119:105"),
                    Pair("\"Não fui eu que lhe ordenei? Seja forte e corajoso! Não se apavore, nem desanime, pois o Senhor, o seu Deus, estará com você por onde você andar.\"", "Josué 1:9")
                )
                
                val servicesList = listOf(
                    Triple("DOM", "18:30", "Culto de Celebração"),
                    Triple("QUA", "19:30", "Culto de Doutrina"),
                    Triple("SÁB", "20:00", "Culto de Jovens"),
                    Triple("SEX", "19:30", "Noite de Intercessão")
                )
                
                val currentVerse = palavraDoDiaVerse.value
                val currentService = proximoCultoTitle.value
                
                var nextVerse = versesList.random()
                while (nextVerse.first == currentVerse && versesList.size > 1) {
                    nextVerse = versesList.random()
                }
                
                var nextService = servicesList.random()
                while (nextService.third == currentService && servicesList.size > 1) {
                    nextService = servicesList.random()
                }

                palavraDoDiaVerse.value = nextVerse.first
                palavraDoDiaRef.value = nextVerse.second

                proximoCultoDayShort.value = nextService.first
                proximoCultoTime.value = nextService.second
                proximoCultoTitle.value = nextService.third
                proximoCultoDayFull.value = when (nextService.first) {
                    "DOM" -> "Domingo"
                    "QUA" -> "Quarta-feira"
                    "SÁB" -> "Sábado"
                    "SEX" -> "Sexta-feira"
                    else -> "Domingo"
                }

                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SkeletonItem(width = 240.dp, height = 32.dp, shape = RoundedCornerShape(16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    SkeletonItem(width = 280.dp, height = 18.dp, shape = RoundedCornerShape(4.dp))
                }
                
                item {
                    InteractiveCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(shimmerBrush())
                            )
                        }
                    }
                }
                
                item {
                    InteractiveCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SkeletonItem(width = 120.dp, height = 16.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            SkeletonItem(width = 300.dp, height = 20.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonItem(width = 260.dp, height = 20.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            SkeletonItem(width = 80.dp, height = 14.dp)
                        }
                    }
                }
                
                item {
                    SkeletonItem(width = 140.dp, height = 24.dp)
                }
                
                item {
                    InteractiveCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SkeletonCircle(size = 60.dp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                SkeletonItem(width = 180.dp, height = 20.dp)
                                Spacer(modifier = Modifier.height(8.dp))
                                SkeletonItem(width = 120.dp, height = 14.dp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo_rhema),
                            contentDescription = "Logo MIC Rhema",
                            modifier = Modifier
                                .size(120.dp)
                                .padding(bottom = 16.dp)
                        )
                    }
                    Text(
                        text = "Bem-vindo à MIC Rhema",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Que a paz do Senhor Jesus esteja com você.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                item {
                    HomeCarousel()
                }
                
                item {
                    InteractiveCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Palavra do Dia", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                palavraDoDiaVerse.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(palavraDoDiaRef.value, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                item {
                    Text(
                        text = "Devocional de Hoje",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    FirestoreDailyDevotional(
                        onReadFull = { activeDevotionalForReading = it },
                        onShare = { dev ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val shareText = "${dev.title}\n${dev.date}\n\n\"${dev.verse}\" (${dev.verseReference})\n\n${dev.content}"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Devocional"))
                        }
                    )
                }
                
                item {
                    Text(
                        text = "Próximo Culto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                item {
                    InteractiveCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(proximoCultoDayShort.value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(proximoCultoTime.value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(proximoCultoTitle.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${proximoCultoDayFull.value} • ${proximoCultoTime.value}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    activeDevotionalForReading?.let { dev ->
        Dialog(onDismissRequest = { activeDevotionalForReading = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DEVOCIONAL DIÁRIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dev.date,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { activeDevotionalForReading = null },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(
                        text = dev.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "\"${dev.verse}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = (devFontSize * 0.9f).sp,
                                lineHeight = (devFontSize * 1.35f).sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = dev.verseReference,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("A", fontSize = 12.sp, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = devFontSize,
                            onValueChange = { devFontSize = it },
                            valueRange = 12f..32f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("A", fontSize = 24.sp, style = MaterialTheme.typography.labelSmall)
                    }

                    val scrollState = rememberScrollState()
                    val progress = if (scrollState.maxValue > 0) {
                        scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                    } else 0f
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                    ) {
                        Column(modifier = Modifier.verticalScroll(scrollState)) {
                            Text(
                                text = dev.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                fontSize = devFontSize.sp,
                                lineHeight = (devFontSize * 1.5f).sp
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GlassButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    val shareText = "${dev.title}\n${dev.date}\n\n\"${dev.verse}\" (${dev.verseReference})\n\n${dev.content}"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Devocional"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Compartilhar", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { activeDevotionalForReading = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("Fechar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevotionalsScreen() {
    val context = LocalContext.current
    val dbHelper = remember { IbrDatabaseHelper(context) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Todos, 1 = Favoritos
    var bookmarkedList by remember { mutableStateOf(emptyList<Devotional>()) }
    var bookmarkedIds by remember { mutableStateOf(setOf<String>()) }
    var showBibleReader by remember { mutableStateOf(false) }
    var bibleReference by remember { mutableStateOf("") }

    fun refreshBookmarks() {
        val bookmarks = dbHelper.getAllBookmarks()
        bookmarkedList = bookmarks
        bookmarkedIds = bookmarks.map { it.id }.toSet()
    }

    LaunchedEffect(Unit) {
        refreshBookmarks()
        kotlinx.coroutines.delay(1000)
        isLoading = false
    }

    if (isLoading) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Devocionais",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(3) {
                InteractiveCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SkeletonItem(width = 80.dp, height = 12.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonItem(width = 200.dp, height = 20.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        SkeletonItem(width = 300.dp, height = 16.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonItem(width = 240.dp, height = 16.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonItem(width = 100.dp, height = 12.dp)
                    }
                }
            }
        }
    } else {
        val displayedDevotionals = if (selectedTab == 0) {
            devotionalsState
        } else {
            bookmarkedList
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Devocionais",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            label = { Text("Todos") },
                            leadingIcon = if (selectedTab == 0) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            label = { Text("Favoritos") },
                            leadingIcon = if (selectedTab == 1) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                    }
                }
            }

            if (selectedTab == 1 && displayedDevotionals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Nenhum favorito ainda",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Toque no coração de um devocional para salvá-lo aqui.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(displayedDevotionals) { dev ->
                    val isBookmarked = bookmarkedIds.contains(dev.id)
                    InteractiveCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dev.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isBookmarked) {
                                                dbHelper.removeBookmark(dev.id)
                                            } else {
                                                dbHelper.saveBookmark(dev)
                                            }
                                            refreshBookmarks()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isBookmarked) "Remover dos favoritos" else "Favoritar devocional",
                                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                val shareText = "${dev.title}\n${dev.date}\n\n\"${dev.verse}\" (${dev.verseReference})\n\n${dev.content}"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Devocional"))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Compartilhar devocional",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(dev.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("\"${dev.verse}\"", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    bibleReference = dev.verseReference
                                    showBibleReader = true
                                }.padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = "Ler Bíblia", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(dev.verseReference, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            if (dev.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(dev.content, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showBibleReader) {
        BibleReaderModal(
            onDismiss = { showBibleReader = false },
            initialReference = bibleReference
        )
    }
}

@Composable
fun ServicesScreen() {
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        isLoading = false
    }

    if (isLoading) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Cultos e Eventos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                ServiceVideosGallery()
            }
            item {
                Text("Cultos Semanais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(2) {
                InteractiveCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonItem(width = 50.dp, height = 50.dp, shape = RoundedCornerShape(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                SkeletonItem(width = 160.dp, height = 20.dp)
                                Spacer(modifier = Modifier.height(6.dp))
                                SkeletonItem(width = 120.dp, height = 14.dp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SkeletonItem(width = 280.dp, height = 16.dp)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Próximos Eventos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(2) {
                InteractiveCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SkeletonItem(width = 80.dp, height = 12.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonItem(width = 180.dp, height = 20.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonItem(width = 260.dp, height = 16.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        SkeletonItem(width = 120.dp, height = 12.dp)
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Cultos e Eventos",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                ServiceVideosGallery()
            }
            item {
                Text("Cultos Semanais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(weeklyServicesState) { service ->
                InteractiveCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(service.dayShort, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(service.time, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(service.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${service.day} • ${service.time}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(service.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Próximos Eventos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(eventsState) { event ->
                InteractiveCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(event.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(event.description, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("📍 ${event.location}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerScreen() {
    var name by remember { mutableStateOf("") }
    var request by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800)
        isLoading = false
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Pedido enviado! 🙏") },
            text = { Text("Seu pedido de oração foi registrado. Estaremos orando por você!") },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (isLoading) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Pedidos de Oração",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonItem(width = 200.dp, height = 16.dp)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🙏", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonItem(width = 260.dp, height = 16.dp)
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonItem(width = 200.dp, height = 16.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonItem(width = 80.dp, height = 12.dp)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SkeletonItem(width = 120.dp, height = 20.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        SkeletonItem(width = 280.dp, height = 48.dp, shape = RoundedCornerShape(24.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        SkeletonItem(width = 280.dp, height = 80.dp, shape = RoundedCornerShape(24.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        SkeletonItem(width = 120.dp, height = 44.dp, shape = RoundedCornerShape(24.dp))
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Pedidos de Oração",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Compartilhe seu pedido conosco",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🙏", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "\"Orai uns pelos outros, para que sejais curados. A oração do justo tem grande poder.\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("— Tiago 5:16", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Enviar Pedido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Seu nome") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassTextField(
                            value = request,
                            onValueChange = { request = it },
                            label = { Text("Pedido de oração") },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 5
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        GlassButton(
                            onClick = {
                                if (name.isNotBlank() && request.isNotBlank()) {
                                    val newReq = PrayerRequest(
                                        id = (prayerRequestsState.size + 1).toString(),
                                        name = name.trim(),
                                        request = request.trim(),
                                        date = "2026-07-13"
                                    )
                                    prayerRequestsState.add(0, newReq)
                                    name = ""
                                    request = ""
                                    showDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("🙏 Enviar Pedido")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleLogo(modifier: Modifier = Modifier) {
    val bluePath = remember { PathParser().parsePathString("M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z").toPath() }
    val greenPath = remember { PathParser().parsePathString("M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z").toPath() }
    val yellowPath = remember { PathParser().parsePathString("M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z").toPath() }
    val redPath = remember { PathParser().parsePathString("M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z").toPath() }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val scaleX = this.size.width / 24f
        val scaleY = this.size.height / 24f
        
        drawContext.canvas.save()
        drawContext.canvas.scale(scaleX, scaleY)
        
        drawPath(bluePath, color = Color(0xFF4285F4))
        drawPath(greenPath, color = Color(0xFF34A853))
        drawPath(yellowPath, color = Color(0xFFFBBC05))
        drawPath(redPath, color = Color(0xFFEA4335))
        
        drawContext.canvas.restore()
    }
}

@Composable
fun GoogleLoginPlaceholder(
    roleRequired: String,
    onLoginSuccess: (MemberRequest) -> Unit
) {
    val context = LocalContext.current
    var isLoggingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var showGoogleChooser by remember { mutableStateOf(false) }
    var googleChooserMode by remember { mutableStateOf("select") } // "select" or "input"
    var customName by remember { mutableStateOf("") }
    var customEmail by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⛪", fontSize = 40.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Área de Membros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Para acessar os conteúdos exclusivos da aba $roleRequired, conecte-se de forma rápida e segura com sua Conta do Google.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Conectando ao Google...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable {
                                    showGoogleChooser = true
                                },
                            shape = RoundedCornerShape(100.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                GoogleLogo(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Continuar com o Google",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F1F1F)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGoogleChooser) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showGoogleChooser = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Brand Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GoogleLogo(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5F6368)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (googleChooserMode == "select") {
                        Text(
                            text = "Escolha uma conta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "para continuar no MIC Rhema",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5F6368),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val userEmail = "haydendanex@gmail.com"
                        val userName = "Hayden Danex"

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // User account
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showGoogleChooser = false
                                        isLoggingIn = true
                                        scope.launch {
                                            kotlinx.coroutines.delay(1200)
                                            val existing = memberRequestsState.find { it.email.equals(userEmail, ignoreCase = true) }
                                            val target = if (existing != null) {
                                                existing
                                            } else {
                                                val newReq = MemberRequest(
                                                    id = System.currentTimeMillis().toString(),
                                                    name = userName,
                                                    email = userEmail,
                                                    isApproved = false,
                                                    isVip = false,
                                                    isIbr = false
                                                )
                                                memberRequestsState.add(newReq)
                                                MemberManager.saveToFirestore(newReq)
                                                newReq
                                            }
                                            MemberManager.saveMembers(context)
                                            MemberManager.setLoggedInMember(context, target)
                                            onLoginSuccess(target)
                                            isLoggingIn = false
                                            NotificationHelper.showNotification(
                                                context,
                                                "Solicitação de acesso enviada!",
                                                "Aguarde a aprovação do administrador para $userEmail."
                                            )
                                        }
                                    },
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3F51B5)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "H",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = userName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1F1F1F)
                                        )
                                        Text(
                                            text = userEmail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5F6368)
                                        )
                                    }
                                }
                            }

                            Divider(color = Color(0xFFE0E0E0))

                            // Use another account
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        googleChooserMode = "input"
                                    },
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F3F4)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF5F6368),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "Usar outra conta",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1F1F1F)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        TextButton(
                            onClick = { showGoogleChooser = false },
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text("Cancelar", color = Color(0xFF1A73E8))
                        }
                    } else {
                        // "input" mode
                        Text(
                            text = "Fazer login",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1F1F)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Use sua Conta do Google",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF5F6368)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        GlassTextField(
                            value = customName,
                            onValueChange = { customName = it; showError = "" },
                            label = { Text("Nome completo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        GlassTextField(
                            value = customEmail,
                            onValueChange = { customEmail = it; showError = "" },
                            label = { Text("E-mail do Google") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )

                        if (showError.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = showError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    googleChooserMode = "select"
                                    showError = ""
                                }
                            ) {
                                Text("Voltar", color = Color(0xFF1A73E8))
                            }

                            GlassButton(
                                onClick = {
                                    if (customName.isBlank() || customEmail.isBlank()) {
                                        showError = "Preencha todos os campos!"
                                        return@GlassButton
                                    }
                                    if (!customEmail.contains("@") || !customEmail.contains(".")) {
                                        showError = "Digite um e-mail do Google válido!"
                                        return@GlassButton
                                    }
                                    showGoogleChooser = false
                                    isLoggingIn = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(1200)
                                        val existing = memberRequestsState.find { it.email.equals(customEmail, ignoreCase = true) }
                                        val target = if (existing != null) {
                                            existing
                                        } else {
                                            val newReq = MemberRequest(
                                                id = System.currentTimeMillis().toString(),
                                                name = customName,
                                                email = customEmail,
                                                isApproved = false,
                                                isVip = false,
                                                isIbr = false
                                            )
                                            memberRequestsState.add(newReq)
                                            MemberManager.saveToFirestore(newReq)
                                            newReq
                                        }
                                        MemberManager.saveMembers(context)
                                        MemberManager.setLoggedInMember(context, target)
                                        onLoginSuccess(target)
                                        isLoggingIn = false
                                        NotificationHelper.showNotification(
                                            context,
                                            "Solicitação de acesso enviada!",
                                            "Aguarde a aprovação do administrador para $customEmail."
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text("Próxima", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingApprovalScreen(member: MemberRequest, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⏳", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Solicitação Pendente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Olá, ${member.name}!\n\nSua solicitação de acesso com o e-mail (${member.email}) está aguardando aprovação dos administradores da igreja.\n\nPor favor, aguarde a ativação dos seus privilégios (VIP ou IBR).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                GlassButton(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Sair da Conta", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RestrictedAccessScreen(member: MemberRequest, requiredRole: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔒", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Acesso Restrito", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Olá, ${member.name}!\n\nSua conta (${member.email}) está aprovada, mas não possui a permissão ativa para a área: $requiredRole.\n\nFale com o administrador no painel para ativar seu acesso VIP ou IBR.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                GlassButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Desconectar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MembersScreen() {
    val member = loggedInMemberState.value
    val context = LocalContext.current
    
    if (member == null) {
        GoogleLoginPlaceholder(roleRequired = "Membro (VIP)") { req ->
            MemberManager.setLoggedInMember(context, req)
        }
    } else {
        val currentMember = memberRequestsState.find { it.email.equals(member.email, ignoreCase = true) } ?: member
        
        if (!currentMember.isApproved) {
            PendingApprovalScreen(member = currentMember, onLogout = { MemberManager.setLoggedInMember(context, null) })
        } else if (!currentMember.isVip) {
            RestrictedAccessScreen(member = currentMember, requiredRole = "Membro (VIP)", onLogout = { MemberManager.setLoggedInMember(context, null) })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Área VIP ✨",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Olá, ${currentMember.name}!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { 
                                MemberManager.setLoggedInMember(context, null) 
                                NotificationHelper.showNotification(context, "Log Out", "Você saiu da área de membros.")
                            }
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                item {
                    InteractiveCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Exclusivo para Você", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Estudos Bíblicos Profundos & Comunhão", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Como membro VIP do MIC Rhema, você tem acesso a materiais pastorais premium e prioridade em solicitações de atendimento pastoral.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text("Mensagens Exclusivas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    InteractiveCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFD4AF37)))
                                Text("VÍDEO PASTORAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Como blindar sua mente nestes tempos difíceis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Uma palavra especial do Pastor Presidente sobre perseverança espiritual e fé inabalável em meio às tempestades da vida moderna.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    InteractiveCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                                Text("DOWNLOAD PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Guia de Discipulado Avançado 2026", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Baixe o material de acompanhamento para o crescimento ministerial e capacitação espiritual da igreja.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item {
                    Text("Apoio e Aconselhamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    InteractiveCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Gostaria de agendar uma conversa pastoral?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Como membro VIP, você tem acesso direto à agenda pastoral para aconselhamento espiritual e oração.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            GlassButton(
                                onClick = {
                                    NotificationHelper.showNotification(context, "Solicitação Recebida", "Em breve a equipe pastoral entrará em contato.")
                                },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Solicitar Agendamento", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IbrScreen() {
    val member = loggedInMemberState.value
    val context = LocalContext.current
    
    if (member == null) {
        GoogleLoginPlaceholder(roleRequired = "IBR") { req ->
            MemberManager.setLoggedInMember(context, req)
        }
    } else {
        val currentMember = memberRequestsState.find { it.email.equals(member.email, ignoreCase = true) } ?: member
        
        if (!currentMember.isApproved) {
            PendingApprovalScreen(member = currentMember, onLogout = { MemberManager.setLoggedInMember(context, null) })
        } else if (!currentMember.isIbr) {
            RestrictedAccessScreen(member = currentMember, requiredRole = "IBR", onLogout = { MemberManager.setLoggedInMember(context, null) })
        } else {
            var activeView by remember { mutableStateOf("catalog") } // "catalog", "details", "player", "pip"
            var selectedCourse by remember { mutableStateOf<IbrCourse?>(null) }
            var selectedChapter by remember { mutableStateOf<IbrChapter?>(null) }
            var currentPlaybackType by remember { mutableStateOf("video") } // "video" or "audio"
            var resumeSeconds by remember { mutableStateOf(0) }
            
            // Shared PiP State
            var pipCourse by remember { mutableStateOf<IbrCourse?>(null) }
            var pipChapter by remember { mutableStateOf<IbrChapter?>(null) }
            var pipType by remember { mutableStateOf("video") }
            var pipSeconds by remember { mutableStateOf(0f) }
            var pipIsPlaying by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize()) {
                when (activeView) {
                    "catalog" -> {
                        IbrCatalogView(
                            currentMemberName = currentMember.name,
                            onLogout = { MemberManager.setLoggedInMember(context, null) },
                            onCourseSelected = { course ->
                                selectedCourse = course
                                activeView = "details"
                            },
                            onChapterResume = { course, chapter, secs, type ->
                                selectedCourse = course
                                selectedChapter = chapter
                                resumeSeconds = secs
                                currentPlaybackType = type
                                activeView = "player"
                            }
                        )
                    }
                    "details" -> {
                        selectedCourse?.let { course ->
                            IbrCourseDetailView(
                                course = course,
                                onBack = { activeView = "catalog" },
                                onPlayChapter = { chapter, type ->
                                    selectedChapter = chapter
                                    currentPlaybackType = type
                                    val prog = ibrProgressState.find { it.courseId == course.id && it.chapterId == chapter.id }
                                    resumeSeconds = prog?.lastPositionSeconds ?: 0
                                    activeView = "player"
                                }
                            )
                        } ?: run { activeView = "catalog" }
                    }
                    "player" -> {
                        if (selectedCourse != null && selectedChapter != null) {
                            IbrMediaPlayerView(
                                course = selectedCourse!!,
                                chapter = selectedChapter!!,
                                playbackType = currentPlaybackType,
                                initialSeconds = resumeSeconds,
                                onBack = { activeView = "details" },
                                onEnterPip = { currentSecs, playing ->
                                    pipCourse = selectedCourse
                                    pipChapter = selectedChapter
                                    pipType = currentPlaybackType
                                    pipSeconds = currentSecs
                                    pipIsPlaying = playing
                                    activeView = "catalog"
                                }
                            )
                        } else {
                            activeView = "catalog"
                        }
                    }
                }

                // Mini PiP Player Overlay at the bottom right
                if (pipCourse != null && pipChapter != null && activeView != "player") {
                    // Tick PiP player position when playing
                    LaunchedEffect(pipIsPlaying) {
                        if (pipIsPlaying) {
                            while (pipSeconds < (pipChapter!!.durationMinutes * 60)) {
                                kotlinx.coroutines.delay(1000)
                                pipSeconds = (pipSeconds + 1f).coerceAtMost((pipChapter!!.durationMinutes * 60).toFloat())
                            }
                            pipIsPlaying = false
                        }
                    }

                    // Auto-save PiP progress periodically
                    LaunchedEffect(pipSeconds) {
                        val secs = pipSeconds.toInt()
                        if (secs > 0 && secs % 5 == 0) {
                            val newProg = IbrProgress(
                                courseId = pipCourse!!.id,
                                chapterId = pipChapter!!.id,
                                lastPositionSeconds = secs,
                                totalDurationSeconds = pipChapter!!.durationMinutes * 60,
                                isCompleted = secs >= (pipChapter!!.durationMinutes * 60 - 60)
                            )
                            val existingIdx = ibrProgressState.indexOfFirst {
                                it.courseId == pipCourse!!.id && it.chapterId == pipChapter!!.id
                            }
                            if (existingIdx != -1) {
                                ibrProgressState[existingIdx] = newProg
                            } else {
                                ibrProgressState.add(newProg)
                            }
                            IbrDatabaseHelper(context).saveProgress(newProg)
                        }
                    }

                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .width(200.dp)
                            .height(120.dp)
                            
                            .clickable {
                                // Restore player state
                                selectedCourse = pipCourse
                                selectedChapter = pipChapter
                                currentPlaybackType = pipType
                                resumeSeconds = pipSeconds.toInt()
                                // Clear PiP
                                pipCourse = null
                                pipChapter = null
                                activeView = "player"
                            },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Mini pulsing waveform / rotating cover
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF1E1E2C), Color(0xFF0F0F16))
                                        )
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Em Miniatura (PiP)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = {
                                                // Save progress and close PiP
                                                val existingIdx = ibrProgressState.indexOfFirst {
                                                    it.courseId == pipCourse!!.id && it.chapterId == pipChapter!!.id
                                                }
                                                val newProg = IbrProgress(
                                                    courseId = pipCourse!!.id,
                                                    chapterId = pipChapter!!.id,
                                                    lastPositionSeconds = pipSeconds.toInt(),
                                                    totalDurationSeconds = pipChapter!!.durationMinutes * 60,
                                                    isCompleted = pipSeconds >= (pipChapter!!.durationMinutes * 58)
                                                )
                                                if (existingIdx != -1) {
                                                    ibrProgressState[existingIdx] = newProg
                                                } else {
                                                    ibrProgressState.add(newProg)
                                                }
                                                IbrDatabaseHelper(context).saveProgress(newProg)
                                                pipCourse = null
                                                pipChapter = null
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Fechar PiP", tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = pipChapter!!.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = pipCourse!!.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { pipIsPlaying = !pipIsPlaying },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (pipIsPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play/Pause",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        val progressPercent = pipSeconds / (pipChapter!!.durationMinutes * 60f)
                                        LinearProgressIndicator(
                                            progress = { progressPercent },
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                                .height(4.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = Color.DarkGray
                                        )

                                        Text(
                                            text = "${(pipSeconds / 60).toInt()}:${String.format("%02d", (pipSeconds % 60).toInt())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IbrCatalogView(
    currentMemberName: String,
    onLogout: () -> Unit,
    onCourseSelected: (IbrCourse) -> Unit,
    onChapterResume: (IbrCourse, IbrChapter, Int, String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedThemeFilter by remember { mutableStateOf("Todos") }
    var isLocalLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val dbHelper = IbrDatabaseHelper(context)
        val list = dbHelper.getAllProgress()
        ibrProgressState.clear()
        ibrProgressState.addAll(list)
        kotlinx.coroutines.delay(1200)
        isLocalLoading = false
    }

    // Find and map active progress
    val activeProgressList = remember(ibrProgressState.size) {
        ibrProgressState.filter { it.lastPositionSeconds > 0 && !it.isCompleted }.mapNotNull { progress ->
            val course = ibrCoursesState.find { it.id == progress.courseId }
            val chapter = course?.chapters?.find { it.id == progress.chapterId }
            if (course != null && chapter != null) {
                Triple(course, chapter, progress)
            } else {
                null
            }
        }
    }

    // Filter courses
    val filteredCourses = ibrCoursesState.filter { course ->
        val matchesSearch = course.title.contains(searchQuery, ignoreCase = true) ||
                course.description.contains(searchQuery, ignoreCase = true) ||
                course.theme.contains(searchQuery, ignoreCase = true)
        val matchesTheme = selectedThemeFilter == "Todos" || course.theme.equals(selectedThemeFilter, ignoreCase = true)
        matchesSearch && matchesTheme
    }

    val themes = listOf("Todos") + ibrCoursesState.map { it.theme }.distinct()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Academy Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Faculdade IBR 🎓",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Instituto Bíblico Rhema",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Introduction banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Olá, $currentMemberName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Bem-vindo ao seu portal de estudos teológicos. Assista aulas em vídeo, ouça os áudios e retome de onde parou a qualquer momento.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar
        item {
            GlassTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar cursos, temas ou lições...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(100.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // Horizontal Category Row (Netflix-Style Carousel Filters)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { theme ->
                    val isSelected = selectedThemeFilter == theme
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedThemeFilter = theme },
                        label = { Text(theme) },
                        shape = RoundedCornerShape(100.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // 1. "CONTINUAR DE ONDE PAROU" (Netflix-style Resume Study)
        if (isLocalLoading) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonItem(width = 220.dp, height = 24.dp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(2) {
                            Card(
                                modifier = Modifier.width(260.dp),
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        SkeletonItem(width = 60.dp, height = 12.dp)
                                        SkeletonItem(width = 40.dp, height = 12.dp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SkeletonItem(width = 180.dp, height = 16.dp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    SkeletonItem(width = 120.dp, height = 12.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    SkeletonItem(width = 140.dp, height = 12.dp)
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeProgressList.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Continuar de onde parou ⏳",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeProgressList) { (course, chapter, progress) ->
                            Card(
                                modifier = Modifier
                                    .width(260.dp)
                                    .clickable {
                                        val mediaType = if (chapter.videoUrl.isNotEmpty()) "video" else "audio"
                                        onChapterResume(course, chapter, progress.lastPositionSeconds, mediaType)
                                    },
                                shape = RoundedCornerShape(32.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = course.theme.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (chapter.isYoutube) "📺 YouTube" else if (chapter.videoUrl.isNotEmpty()) "🎥 Vídeo" else "🎵 Áudio",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = course.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    val currentMin = (progress.lastPositionSeconds / 60)
                                    val totalMin = (progress.totalDurationSeconds / 60)
                                    val progressPercent = progress.lastPositionSeconds.toFloat() / progress.totalDurationSeconds.toFloat()
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Parou em: $currentMin min",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "$totalMin min total",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progressPercent },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(100.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. COURSES CATALOG LIST
        item {
            Text(
                text = "Cursos Disponíveis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (isLocalLoading) {
            items(2) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(shimmerBrush())
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            SkeletonItem(width = 180.dp, height = 20.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            SkeletonItem(width = 280.dp, height = 14.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            SkeletonItem(width = 240.dp, height = 14.dp)
                        }
                    }
                }
            }
        } else if (filteredCourses.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nenhum curso corresponde aos filtros.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredCourses) { course ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCourseSelected(course) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // Custom Gradient Course Poster representation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(
                                            text = course.theme.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "${course.chapters.size} aulas",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Text(
                                    text = course.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 2
                                )
                            }
                        }

                        // Info Content
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = course.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val supportsVideo = course.chapters.any { it.videoUrl.isNotEmpty() || it.isYoutube }
                                    val supportsAudio = course.chapters.any { it.audioUrl.isNotEmpty() }
                                    if (supportsVideo) Text("🎥 Vídeo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    if (supportsAudio) Text("🎵 Áudio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                                Text(
                                    text = "Explorar aulas →",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IbrCourseDetailView(
    course: IbrCourse,
    onBack: () -> Unit,
    onPlayChapter: (IbrChapter, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Back bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detalhes do Curso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        // Hero Course Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(
                            course.theme.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(course.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "A grade possui um total de ${course.chapters.size} aulas dedicadas ao seu aprendizado.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Lectures title
        item {
            Text(
                text = "Cronograma de Aulas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (course.chapters.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma aula cadastrada para este curso ainda.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        } else {
            itemsIndexed(course.chapters) { index, chapter ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        text = chapter.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${chapter.durationMinutes} minutos",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        if (chapter.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = chapter.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls container - Spotify & Netflix-like action options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (chapter.isYoutube) {
                                GlassButton(
                                    onClick = { onPlayChapter(chapter, "video") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)), // YouTube Red / Netflix style
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("📺", fontSize = 16.sp)
                                        Text("Assistir YouTube", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                if (chapter.videoUrl.isNotEmpty()) {
                                    GlassButton(
                                        onClick = { onPlayChapter(chapter, "video") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("🎥", fontSize = 16.sp)
                                            Text("Assistir Vídeo", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (chapter.audioUrl.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { onPlayChapter(chapter, "audio") },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("🎵", fontSize = 16.sp)
                                            Text("Ouvir Áudio", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IbrMediaPlayerView(
    course: IbrCourse,
    chapter: IbrChapter,
    playbackType: String, // "video" or "audio"
    initialSeconds: Int,
    onBack: () -> Unit,
    onEnterPip: (Float, Boolean) -> Unit
) {
    val context = LocalContext.current
    
    // Playback States
    var isPlaying by remember { mutableStateOf(true) }
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var currentSeconds by remember { mutableStateOf(initialSeconds.toFloat()) }
    val totalSeconds = chapter.durationMinutes * 60
    
    // Simulation Sliders
    var simulatedBrightness by remember { mutableStateOf(85f) } // 0f to 100f
    var simulatedVolume by remember { mutableStateOf(70f) } // 0f to 100f
    var showVolumeHud by remember { mutableStateOf(false) }

    // Background play & locks simulation
    var isBackgroundAudioActive by remember { mutableStateOf(false) }

    // Live Tick Loop
    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (currentSeconds < totalSeconds) {
                kotlinx.coroutines.delay(1000)
                currentSeconds = (currentSeconds + playbackSpeed).coerceAtMost(totalSeconds.toFloat())
            }
            isPlaying = false
        }
    }

    // Volume HUD show timer
    LaunchedEffect(simulatedVolume) {
        showVolumeHud = true
        kotlinx.coroutines.delay(1800)
        showVolumeHud = false
    }

    // Vinyl spinning angle for audio
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Auto-save logic helper
    var lastSavedSeconds by remember { mutableStateOf(initialSeconds) }
    
    val autoSaveProgress = {
        val currentSecInt = currentSeconds.toInt()
        val newProgress = IbrProgress(
            courseId = course.id,
            chapterId = chapter.id,
            lastPositionSeconds = currentSecInt,
            totalDurationSeconds = totalSeconds,
            isCompleted = currentSecInt >= (totalSeconds - 60)
        )
        val existingIdx = ibrProgressState.indexOfFirst { it.courseId == course.id && it.chapterId == chapter.id }
        if (existingIdx != -1) {
            ibrProgressState[existingIdx] = newProgress
        } else {
            ibrProgressState.add(newProgress)
        }
        val dbHelper = IbrDatabaseHelper(context)
        dbHelper.saveProgress(newProgress)
        lastSavedSeconds = currentSecInt
    }

    // Auto-save progress every 5 seconds during active playback
    LaunchedEffect(currentSeconds) {
        if (Math.abs(currentSeconds.toInt() - lastSavedSeconds) >= 5) {
            autoSaveProgress()
        }
    }

    // Save on exit / dispose
    DisposableEffect(Unit) {
        onDispose {
            autoSaveProgress()
        }
    }

    // Save Progress logic helper (called manually with notification)
    val saveProgressAction = {
        autoSaveProgress()
        val displayMin = (currentSeconds / 60).toInt()
        val displaySec = (currentSeconds % 60).toInt()
        NotificationHelper.showNotification(
            context,
            "Progresso Salvo!",
            "Você parou em ${displayMin}:${String.format("%02d", displaySec)} de '${chapter.title}'"
        )
    }

    // Actual Dark Cinema Canvas Root Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)) // Pure deep black cinema background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. TOP HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        saveProgressAction()
                        onBack()
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar e Salvar", tint = Color.White)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = course.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }

                // PiP trigger
                IconButton(
                    onClick = {
                        onEnterPip(currentSeconds, isPlaying)
                    }
                ) {
                    Icon(Icons.Default.PictureInPicture, contentDescription = "Modo PiP", tint = Color.White)
                }
            }

            // 2. MAIN MEDIA WINDOW CONTAINER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF111116))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (playbackType == "video" || chapter.isYoutube) {
                    if (chapter.isYoutube) {
                        YoutubePlayer(
                            videoUrl = chapter.videoUrl,
                            youtubeId = chapter.youtubeId,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (chapter.videoUrl.isNotEmpty()) {
                        val exoPlayer = remember(chapter.videoUrl) {
                            androidx.media3.exoplayer.ExoPlayer.Builder(context).setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(com.aistudio.micrhema.ExoPlayerCache.getCacheDataSourceFactory(context))).build().apply {
                                setMediaItem(androidx.media3.common.MediaItem.fromUri(chapter.videoUrl))
                                prepare()
                                playWhenReady = true
                            }
                        }
                        
                        var isVideoBuffering by remember { mutableStateOf(true) }
                        DisposableEffect(chapter.videoUrl) {
                            val listener = object : androidx.media3.common.Player.Listener {
                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    isVideoBuffering = playbackState == androidx.media3.common.Player.STATE_BUFFERING || playbackState == androidx.media3.common.Player.STATE_IDLE
                                }
                            }
                            exoPlayer.addListener(listener)
                            onDispose {
                                exoPlayer.removeListener(listener)
                                exoPlayer.release()
                            }
                        }
                        
                        Box(modifier = Modifier.fillMaxSize()) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    androidx.media3.ui.PlayerView(ctx).apply {
                                        player = exoPlayer
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isVideoBuffering) {
                                Box(modifier = Modifier.fillMaxSize().background(shimmerBrush()))
                            }

                            
                            // Custom Video Overlay
                            val authorizedUser = loggedInMemberState.value?.let { it.isApproved || it.isIbr || it.isVip } ?: false
                            if (authorizedUser) {
                                val context = LocalContext.current
                                IconButton(
                                    onClick = {
                                        DownloadHelper.downloadFile(
                                            context = context,
                                            url = chapter.videoUrl,
                                            title = chapter.title,
                                            fileName = "micrhema_aula_${chapter.id}.mp4"
                                        )
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Baixar Aula (Offline)",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    } else {
                        // Fallback text if no URL provided
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("URL do vídeo não configurada.", color = Color.White)
                        }
                    }
                } else {
                    // Spotify Vinyl / Podcast Waveforms Simulation Window
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.radialGradient(listOf(Color(0xFF131B2E), Color(0xFF090E17))))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Rotating vinyl record cover
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .graphicsLayer {
                                        if (isPlaying) rotationZ = angle
                                    }
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(4.dp, Color.DarkGray, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                // Inner color ring
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("IBR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Pulsing waveform status text
                            Text(
                                text = "ÁUDIO DE ESTUDOS REPRODUZINDO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Simple audio wave lines decoration
                            Row(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bars = if (isPlaying) listOf(14.dp, 24.dp, 10.dp, 28.dp, 18.dp, 24.dp, 8.dp) else listOf(8.dp, 8.dp, 8.dp, 8.dp, 8.dp, 8.dp, 8.dp)
                                bars.forEach { height ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(height)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(100.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. CONTROLS AREA (Sliders, Speed, Timers, Buttons)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // Volume & Brightness HUD sliders combined row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Brightness slider (Netflix interface style)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("☀️", fontSize = 14.sp)
                            Text("Brilho de Tela", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                        Slider(
                            value = simulatedBrightness,
                            onValueChange = { simulatedBrightness = it },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.DarkGray
                            )
                        )
                    }

                    // Volume slider
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔊", fontSize = 14.sp)
                            Text("Volume Áudio", style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
                        }
                        Slider(
                            value = simulatedVolume,
                            onValueChange = { simulatedVolume = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.DarkGray
                            )
                        )
                    }
                }

                // Background Play Toggle Row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("📻", fontSize = 18.sp)
                            Column {
                                Text("Ouvir com Tela Desligada", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Mantém o áudio tocando ao sair do app", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                        Switch(
                            checked = isBackgroundAudioActive,
                            onCheckedChange = { active ->
                                isBackgroundAudioActive = active
                                if (active) {
                                    NotificationHelper.showNotification(
                                        context,
                                        "IBR Áudio Ativo 🎧",
                                        "A aula '${chapter.title}' continuará tocando mesmo com smartphone bloqueado."
                                    )
                                }
                            }
                        )
                    }
                }

                // Progress Timer slider (Temporizador)
                Column {
                    val currentMin = (currentSeconds / 60).toInt()
                    val currentSec = (currentSeconds % 60).toInt()
                    val totalMin = (totalSeconds / 60)
                    val totalSec = (totalSeconds % 60)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${currentMin}:${String.format("%02d", currentSec)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${totalMin}:${String.format("%02d", totalSec)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray
                        )
                    }

                    Slider(
                        value = currentSeconds,
                        onValueChange = { currentSeconds = it.coerceIn(0f, totalSeconds.toFloat()) },
                        valueRange = 0f..totalSeconds.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.DarkGray
                        )
                    )
                }

                // Control Bar Buttons (Skip -10, Play, Skip +10, Speed)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Playback Speed chip selector
                    TextButton(
                        onClick = {
                            playbackSpeed = when (playbackSpeed) {
                                1.0f -> 1.5f
                                1.5f -> 2.0f
                                else -> 1.0f
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Rewind 10s
                    IconButton(
                        onClick = { currentSeconds = (currentSeconds - 10f).coerceAtLeast(0f) }
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Voltar 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Main Center Play Pause
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = { currentSeconds = (currentSeconds + 10f).coerceAtMost(totalSeconds.toFloat()) }
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Avançar 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Bookmark Save
                    IconButton(
                        onClick = {
                            saveProgressAction()
                        }
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = "Salvar Progresso", tint = Color.White)
                    }
                }
            }
        }

        // Floating Volume level HUD simulation Overlay
        if (showVolumeHud) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp),
                shape = RoundedCornerShape(100.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔊", color = Color.White, fontSize = 14.sp)
                    Text(
                        text = "Volume: ${simulatedVolume.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Semi-transparent Overlay Dimming layer to simulate screen brightness physically!
        val dimOpacity = ((100f - simulatedBrightness) / 100f) * 0.75f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimOpacity))
                .clickable(enabled = false) {} // block click propagation
        )
    }
}

@Composable
fun AboutScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Sobre",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Conheça a MIC Rhema",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👨‍💼", fontSize = 40.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(pastorNameState.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(pastorTitleState.value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NOSSA MISSÃO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("MIC Rhema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(missionTaglineState.value, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("O QUE É RHEMA?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(rhemaMeaningState.value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}


val isAdminLogged = mutableStateOf(false)

@Composable
fun AdminScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("micrhema_admin_prefs", android.content.Context.MODE_PRIVATE)
    
    DisposableEffect(Unit) {
        onDispose {
            LocalDataManager.saveAll(context)
        }
    }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        isAdminLogged.value = prefs.getBoolean("admin_logged", false)
    }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf("") }

    if (!isAdminLogged.value) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Área Administrativa", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Faça login para gerenciar o conteúdo", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Usuário") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
            
            if (loginError.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(loginError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            GlassButton(
                onClick = {
                    if (username.trim() == "Admin" && password == "igreja10") {
                        isAdminLogged.value = true
                        prefs.edit().putBoolean("admin_logged", true).apply()
                    } else {
                        loginError = "Usuário ou senha incorretos!"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Entrar", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        AdminPanel {
            isAdminLogged.value = false
            prefs.edit().putBoolean("admin_logged", false).apply()
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanel(onLogout: () -> Unit) {
    var selectedSection by remember { mutableStateOf("home") }
    var globalSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OutlinedTextField(
            value = globalSearchQuery,
            onValueChange = { globalSearchQuery = it },
            placeholder = { Text("Buscar aulas, capítulos ou conteúdos VIP...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = { 
                if (globalSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { globalSearchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            shape = RoundedCornerShape(24.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Sair", tint = MaterialTheme.colorScheme.error)
            }
            val sections = listOf(
                "home" to "Início",
                "devotionals" to "Devocionais",
                "services" to "Cultos",
                "prayers" to "Orações",
                "members" to "Membros",
                "ibr" to "Seminário IBR 🎓",
                "content" to "Conteúdo",
                "team" to "Equipe",
                "about" to "Sobre",
                "tabs" to "Gerenciar Abas"
            )
            sections.forEach { (key, label) ->
                FilterChip(
                    selected = selectedSection == key,
                    onClick = { selectedSection = key },
                    label = { Text(label) }
                )
            }
        }
        
        Divider()
        
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (globalSearchQuery.isNotBlank()) {
                AdminGlobalSearchSection(globalSearchQuery)
            } else {
                when (selectedSection) {
                    "home" -> EditHomeSection()
                    "devotionals" -> EditDevotionalsSection()
                    "services" -> EditServicesSection()
                    "prayers" -> EditPrayersSection()
                    "members" -> EditMembersSection()
                    "ibr" -> EditIbrSection()
                    "content" -> EditContentSection()
                    "team" -> EditTeamSection()
                    "about" -> EditAboutSection()
                    "tabs" -> AdminTabsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIbrSection() {
    val context = LocalContext.current
    
    // Add Course Form States
    var courseTheme by remember { mutableStateOf("Teologia") }
    var courseTitle by remember { mutableStateOf("") }
    var courseDescription by remember { mutableStateOf("") }
    

    // Add Chapter Form States
    var editingCourse by remember { mutableStateOf<IbrCourse?>(null) }
    var editingChapter by remember { mutableStateOf<IbrChapter?>(null) }

    var selectedCourseForChapter by remember { mutableStateOf<IbrCourse?>(null) }
    var chapterTitle by remember { mutableStateOf("") }
    var chapterDescription by remember { mutableStateOf("") }
    var chapterDuration by remember { mutableStateOf("30") }
    var isYoutube by remember { mutableStateOf(false) }
    var videoUrl by remember { mutableStateOf("") }
    var audioUrl by remember { mutableStateOf("") }
    
    // Bulk Upload States
    class BulkUploadTask(
        val id: String,
        val filename: String,
        val title: String,
        var progress: Float = 0f,
        var status: String = "Pendente"
    )
    val bulkUploadQueue = remember { mutableStateListOf<BulkUploadTask>() }
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Section Title
        item {
            Column {
                Text(
                    text = "Gestão de Conteúdo - Faculdade IBR",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Cadastre novos cursos teológicos, capítulos, links do YouTube, vídeos e áudios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. CREATE NEW COURSE CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🆕 Criar Novo Curso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    GlassTextField(
                        value = courseTitle,
                        onValueChange = { courseTitle = it },
                        label = { Text("Título do Curso") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    )

                    GlassTextField(
                        value = courseDescription,
                        onValueChange = { courseDescription = it },
                        label = { Text("Descrição Curta") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    )

                    // Theme selector chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tema / Categoria:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val themes = listOf("Teologia", "História Bíblica", "Vida Cristã")
                            themes.forEach { theme ->
                                FilterChip(
                                    selected = courseTheme == theme,
                                    onClick = { courseTheme = theme },
                                    label = { Text(theme) }
                                )
                            }
                        }
                    }

                    GlassButton(
                        onClick = {
                            if (courseTitle.isNotBlank()) {
                                val newCourse = IbrCourse(
                                    id = "course_${System.currentTimeMillis()}",
                                    title = courseTitle,
                                    description = courseDescription,
                                    theme = courseTheme,
                                    imageUrl = "",
                                    chapters = mutableStateListOf()
                                )
                                ibrCoursesState.add(newCourse)
                                NotificationHelper.showNotification(
                                    context,
                                    "Curso Criado! 🎓",
                                    "O curso '$courseTitle' foi adicionado com sucesso."
                                )
                                courseTitle = ""
                                courseDescription = ""
                            } else {
                                NotificationHelper.showNotification(context, "Erro", "Preencha o título do curso.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Adicionar Curso", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Initialize selected course if empty
        if (selectedCourseForChapter == null && ibrCoursesState.isNotEmpty()) {
            selectedCourseForChapter = ibrCoursesState.first()
        }

        // 2. ADD CHAPTER CARD (Only shown if courses exist)
        if (ibrCoursesState.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("➕ Adicionar Aula/Capítulo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        
                        // Selected Course picker
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Selecionar Curso Destino:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ibrCoursesState.forEach { c ->
                                    FilterChip(
                                        selected = selectedCourseForChapter?.id == c.id,
                                        onClick = { selectedCourseForChapter = c },
                                        label = { Text(c.title, maxLines = 1) }
                                    )
                                }
                            }
                        }

                        GlassTextField(
                            value = chapterTitle,
                            onValueChange = { chapterTitle = it },
                            label = { Text("Título da Aula") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        GlassTextField(
                            value = chapterDescription,
                            onValueChange = { chapterDescription = it },
                            label = { Text("Descrição / Conteúdo") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextField(
                                value = chapterDuration,
                                onValueChange = { chapterDuration = it },
                                label = { Text("Duração (Minutos)") },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Text("É YouTube?", style = MaterialTheme.typography.labelMedium)
                                Switch(checked = isYoutube, onCheckedChange = { isYoutube = it })
                            }
                        }

                        if (isYoutube) {
                            GlassTextField(
                                value = videoUrl,
                                onValueChange = { videoUrl = it },
                                label = { Text("Link do YouTube (ID ou URL)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                placeholder = { Text("https://youtube.com/watch?v=...") }
                            )
                        } else {
                            GlassTextField(
                                value = videoUrl,
                                onValueChange = { videoUrl = it },
                                label = { Text("URL do Upload do Vídeo") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                            GlassTextField(
                                value = audioUrl,
                                onValueChange = { audioUrl = it },
                                label = { Text("URL do Upload do Áudio (Opcional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp)
                            )
                        }

                        GlassButton(
                            onClick = {
                                if (chapterTitle.isNotBlank() && selectedCourseForChapter != null) {
                                    val duration = chapterDuration.toIntOrNull() ?: 30
                                    val newChapter = IbrChapter(
                                        id = "chap_${System.currentTimeMillis()}",
                                        title = chapterTitle,
                                        description = chapterDescription,
                                        durationMinutes = duration,
                                        videoUrl = videoUrl,
                                        audioUrl = audioUrl,
                                        isYoutube = isYoutube
                                    )
                                    // Add to the selected course chapters list
                                    val targetCourse = ibrCoursesState.find { it.id == selectedCourseForChapter!!.id }
                                    if (targetCourse != null) {
                                        val updatedChapters = targetCourse.chapters.toMutableList().apply { add(newChapter) }
                                        val updatedCourse = targetCourse.copy(chapters = updatedChapters)
                                        val index = ibrCoursesState.indexOf(targetCourse)
                                        if (index != -1) {
                                            ibrCoursesState[index] = updatedCourse
                                        }
                                        selectedCourseForChapter = updatedCourse
                                    }
                                    
                                    NotificationHelper.showNotification(
                                        context,
                                        "Aula Adicionada! 🎓",
                                        "A aula '$chapterTitle' foi adicionada ao curso '${targetCourse?.title}'"
                                    )
                                    // Clear form
                                    chapterTitle = ""
                                    chapterDescription = ""
                                    videoUrl = ""
                                    audioUrl = ""
                                    isYoutube = false
                                } else {
                                    NotificationHelper.showNotification(context, "Erro", "Preencha o título da aula.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Adicionar Aula", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
        
        // 2.5 BULK UPLOAD CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🚀 Upload em Lote (Múltiplas Aulas)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Selecionar Curso Destino:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ibrCoursesState.forEach { c ->
                                FilterChip(
                                    selected = selectedCourseForChapter?.id == c.id,
                                    onClick = { selectedCourseForChapter = c },
                                    label = { Text(c.title, maxLines = 1) }
                                )
                            }
                        }
                    }
                    
                    if (bulkUploadQueue.isEmpty()) {
                        OutlinedButton(
                            onClick = {
                                if (selectedCourseForChapter == null) {
                                    NotificationHelper.showNotification(context, "Erro", "Selecione o curso destino antes de adicionar arquivos.")
                                    return@OutlinedButton
                                }
                                bulkUploadQueue.add(BulkUploadTask("t1", "aula1_introducao.mp4", "Aula 1: Introdução"))
                                bulkUploadQueue.add(BulkUploadTask("t2", "aula2_fundamentos.mp4", "Aula 2: Fundamentos"))
                                bulkUploadQueue.add(BulkUploadTask("t3", "aula3_avancado.mp4", "Aula 3: Avançado"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Selecionar Arquivos Locais (Simulado)")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bulkUploadQueue.forEach { task ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(task.title, fontWeight = FontWeight.Bold)
                                            Text(task.status, style = MaterialTheme.typography.labelSmall, color = if (task.status == "Concluído") Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
                                        }
                                        Text(task.filename, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        LinearProgressIndicator(
                                            progress = task.progress,
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = if (task.status == "Concluído") Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            val allDone = bulkUploadQueue.all { it.status == "Concluído" }
                            if (!allDone) {
                                GlassButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            for (task in bulkUploadQueue) {
                                                if (task.status == "Concluído") continue
                                                
                                                val index = bulkUploadQueue.indexOf(task)
                                                
                                                val updatedTask = BulkUploadTask(task.id, task.filename, task.title, 0f, "Enviando...")
                                                bulkUploadQueue[index] = updatedTask
                                                
                                                // Simulate upload progress
                                                for (p in 1..10) {
                                                    kotlinx.coroutines.delay(300)
                                                    bulkUploadQueue[index] = BulkUploadTask(task.id, task.filename, task.title, p * 0.1f, "Enviando... ${p*10}%")
                                                }
                                                
                                                bulkUploadQueue[index] = BulkUploadTask(task.id, task.filename, task.title, 1f, "Concluído")
                                                
                                                // Add chapter to course
                                                val targetCourse = ibrCoursesState.find { it.id == selectedCourseForChapter?.id }
                                                if (targetCourse != null) {
                                                    val newChap = IbrChapter(
                                                        id = "chap_${System.currentTimeMillis()}_${task.id}",
                                                        title = task.title,
                                                        description = "Upload em lote: ${task.filename}",
                                                        durationMinutes = 45,
                                                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                                        audioUrl = "",
                                                        isYoutube = false
                                                    )
                                                    val updatedChapters = targetCourse.chapters.toMutableList().apply { add(newChap) }
                                                    val updatedCourse = targetCourse.copy(chapters = updatedChapters)
                                                    val cIndex = ibrCoursesState.indexOf(targetCourse)
                                                    if (cIndex != -1) {
                                                        ibrCoursesState[cIndex] = updatedCourse
                                                    }
                                                    selectedCourseForChapter = updatedCourse
                                                }
                                            }
                                            NotificationHelper.showNotification(context, "Sucesso", "Todos os uploads foram concluídos e adicionados ao curso.")
                                            kotlinx.coroutines.delay(2000)
                                            bulkUploadQueue.clear()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("Iniciar Upload em Lote", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                GlassButton(
                                    onClick = { bulkUploadQueue.clear() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("Limpar Fila", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. LIST OF EXISTING COURSES AND CHAPTERS
        item {
            Text("📚 Cursos e Aulas Ativas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        if (ibrCoursesState.isEmpty()) {
            item {
                Text("Nenhum curso ou aula cadastrado ainda.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            items(ibrCoursesState) { course ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text(course.theme.uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary)
                                }
                                Text(course.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Row {
                                IconButton(onClick = { editingCourse = course }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar Curso", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { ibrCoursesState.remove(course) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Deletar Curso", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Text(course.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text("Aulas (${course.chapters.size}):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        
                        if (course.chapters.isEmpty()) {
                            Text("Sem aulas cadastradas neste curso.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        } else {
                            course.chapters.forEachIndexed { idx, ch ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("${idx + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Column {
                                            Text(ch.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = "${ch.durationMinutes} min • ${if (ch.isYoutube) "YouTube 📺" else if (ch.videoUrl.isNotEmpty()) "Vídeo 🎥" else "Somente Áudio 🎵"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingCourse = course
                                                editingChapter = ch
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar Aula", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                val updatedChapters = course.chapters.toMutableList().apply { remove(ch) }
                                                val updatedCourse = course.copy(chapters = updatedChapters)
                                                val index = ibrCoursesState.indexOf(course)
                                                if (index != -1) {
                                                    ibrCoursesState[index] = updatedCourse
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Deletar Aula", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    if (editingCourse != null && editingChapter == null) {
        var editTitle by remember(editingCourse) { mutableStateOf(editingCourse!!.title) }
        var editDescription by remember(editingCourse) { mutableStateOf(editingCourse!!.description) }
        var editTheme by remember(editingCourse) { mutableStateOf(editingCourse!!.theme) }
        
        AlertDialog(
            onDismissRequest = { editingCourse = null },
            title = { Text("Editar Curso") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("Descrição") })
                    GlassTextField(value = editTheme, onValueChange = { editTheme = it }, label = { Text("Tema") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val idx = ibrCoursesState.indexOfFirst { it.id == editingCourse!!.id }
                    if (idx != -1) {
                        ibrCoursesState[idx] = editingCourse!!.copy(title = editTitle, description = editDescription, theme = editTheme)
                    }
                    editingCourse = null
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editingCourse = null }) { Text("Cancelar") } }
        )
    }

    if (editingCourse != null && editingChapter != null) {
        var editTitle by remember(editingChapter) { mutableStateOf(editingChapter!!.title) }
        var editDescription by remember(editingChapter) { mutableStateOf(editingChapter!!.description) }
        var editDuration by remember(editingChapter) { mutableStateOf(editingChapter!!.durationMinutes.toString()) }
        var editVideoUrl by remember(editingChapter) { mutableStateOf(editingChapter!!.videoUrl) }
        
        AlertDialog(
            onDismissRequest = {
                editingChapter = null
                editingCourse = null
            },
            title = { Text("Editar Aula") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") })
                    GlassTextField(value = editDescription, onValueChange = { editDescription = it }, label = { Text("Descrição") })
                    GlassTextField(value = editDuration, onValueChange = { editDuration = it }, label = { Text("Duração (Min)") })
                    GlassTextField(value = editVideoUrl, onValueChange = { editVideoUrl = it }, label = { Text("URL Vídeo") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val courseIdx = ibrCoursesState.indexOfFirst { it.id == editingCourse!!.id }
                    if (courseIdx != -1) {
                        val course = ibrCoursesState[courseIdx]
                        val chapterIdx = course.chapters.indexOfFirst { it.id == editingChapter!!.id }
                        if (chapterIdx != -1) {
                            val updatedChapters = course.chapters.toMutableList()
                            updatedChapters[chapterIdx] = editingChapter!!.copy(
                                title = editTitle,
                                description = editDescription,
                                durationMinutes = editDuration.toIntOrNull() ?: editingChapter!!.durationMinutes,
                                videoUrl = editVideoUrl
                            )
                            ibrCoursesState[courseIdx] = course.copy(chapters = updatedChapters)
                        }
                    }
                    editingChapter = null
                    editingCourse = null
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingChapter = null
                    editingCourse = null
                }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun StatusBadge(text: String, containerColor: Color, contentColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditMembersSection() {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    
    val filteredMembers = remember(memberRequestsState.toList(), searchQuery, selectedFilter) {
        memberRequestsState.filter { member ->
            val matchesSearch = member.name.contains(searchQuery, ignoreCase = true) || member.email.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Aprovados" -> member.isApproved
                "Pendentes" -> !member.isApproved
                "VIP" -> member.isVip
                "IBR" -> member.isIbr
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aprovações e Níveis de Membros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gerencie quem tem acesso às abas VIP e IBR",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        GlassTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar membro por nome ou email") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Todos", "Aprovados", "Pendentes", "VIP", "IBR")
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredMembers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("👥", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Nenhum membro registrado", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredMembers) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initials = req.name.split(" ").filter { it.isNotEmpty() }.take(2).map { it[0].uppercaseChar() }.joinToString("")
                                    Text(
                                        text = initials.ifEmpty { "?" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = req.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = req.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        memberRequestsState.remove(req)
                                        MemberManager.saveMembers(context)
                                        MemberManager.deleteFromFirestore(req)
                                        NotificationHelper.showNotification(
                                            context,
                                            "Membro removido",
                                            "A conta de ${req.name} foi apagada."
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir solicitação",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (req.isApproved) {
                                    StatusBadge(text = "APROVADO", containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32))
                                } else {
                                    StatusBadge(text = "PENDENTE", containerColor = Color(0xFFFFF3E0), contentColor = Color(0xFFE65100))
                                }
                                
                                if (req.isVip) {
                                    StatusBadge(text = "VIP", containerColor = Color(0xFFF3E5F5), contentColor = Color(0xFF7B1FA2))
                                }
                                
                                if (req.isIbr) {
                                    StatusBadge(text = "IBR", containerColor = Color(0xFFE1F5FE), contentColor = Color(0xFF0288D1))
                                }
                            }
                            
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassButton(
                                    onClick = {
                                        val idx = memberRequestsState.indexOf(req)
                                        if (idx != -1) {
                                            val updated = req.copy(isApproved = !req.isApproved)
                                            memberRequestsState[idx] = updated
                                            MemberManager.saveMembers(context)
                                            MemberManager.saveToFirestore(updated)
                                            NotificationHelper.showNotification(
                                                context,
                                                if (updated.isApproved) "Membro Aprovado! 🎉" else "Membro Desaprovado!",
                                                "${updated.name} foi ${if (updated.isApproved) "aprovado" else "desaprovado"}."
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f).height(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (req.isApproved) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (req.isApproved) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (req.isApproved) Icons.Default.Close else Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (req.isApproved) "Desaprovar" else "Aprovar",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        val idx = memberRequestsState.indexOf(req)
                                        if (idx != -1) {
                                            val updated = req.copy(isVip = !req.isVip)
                                            memberRequestsState[idx] = updated
                                            MemberManager.saveMembers(context)
                                            MemberManager.saveToFirestore(updated)
                                            NotificationHelper.showNotification(
                                                context,
                                                if (updated.isVip) "VIP Ativado! ✨" else "VIP Desativado!",
                                                "Status VIP de ${updated.name} atualizado."
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, if (req.isVip) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.outline),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (req.isVip) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (req.isVip) Color(0xFF7B1FA2) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (req.isVip) "VIP Ativo" else "Ativar VIP",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        val idx = memberRequestsState.indexOf(req)
                                        if (idx != -1) {
                                            val updated = req.copy(isIbr = !req.isIbr)
                                            memberRequestsState[idx] = updated
                                            MemberManager.saveMembers(context)
                                            MemberManager.saveToFirestore(updated)
                                            NotificationHelper.showNotification(
                                                context,
                                                if (updated.isIbr) "IBR Ativado! ⛪" else "IBR Desativado!",
                                                "Status IBR de ${updated.name} atualizado."
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, if (req.isIbr) Color(0xFF0288D1) else MaterialTheme.colorScheme.outline),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (req.isIbr) Color(0xFF0288D1) else MaterialTheme.colorScheme.onSurface
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (req.isIbr) Color(0xFF0288D1) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (req.isIbr) "IBR Ativo" else "Ativar IBR",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
}
}

@Composable
fun EditHomeSection() {
    val context = LocalContext.current
    var verseInput by remember { mutableStateOf(palavraDoDiaVerse.value) }
    var refInput by remember { mutableStateOf(palavraDoDiaRef.value) }
    
    var cultoTitleInput by remember { mutableStateOf(proximoCultoTitle.value) }
    var cultoTimeInput by remember { mutableStateOf(proximoCultoTime.value) }
    var cultoDayShortInput by remember { mutableStateOf(proximoCultoDayShort.value) }
    var cultoDayFullInput by remember { mutableStateOf(proximoCultoDayFull.value) }

    var carouselTitleInput by remember { mutableStateOf("") }
    var carouselDescriptionInput by remember { mutableStateOf("") }
    var carouselDateInput by remember { mutableStateOf("") }
    var carouselTagInput by remember { mutableStateOf("EVENTO") } // "EVENTO" ou "NOTÍCIA"
    var carouselImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val carouselImagePicker = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        carouselImageUri = uri
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Palavra do Dia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            GlassTextField(
                value = verseInput,
                onValueChange = { verseInput = it },
                label = { Text("Versículo") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = refInput,
                onValueChange = { refInput = it },
                label = { Text("Referência") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        
        item {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Configurar Próximo Culto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            GlassTextField(
                value = cultoTitleInput,
                onValueChange = { cultoTitleInput = it },
                label = { Text("Título do Culto") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassTextField(
                    value = cultoDayFullInput,
                    onValueChange = { cultoDayFullInput = it },
                    label = { Text("Dia (Completo)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                GlassTextField(
                    value = cultoDayShortInput,
                    onValueChange = { cultoDayShortInput = it },
                    label = { Text("Dia (Curto)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
        item {
            GlassTextField(
                value = cultoTimeInput,
                onValueChange = { cultoTimeInput = it },
                label = { Text("Horário") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        
        item {
            GlassButton(
                onClick = {
                    palavraDoDiaVerse.value = verseInput
                    palavraDoDiaRef.value = refInput
                    proximoCultoTitle.value = cultoTitleInput
                    proximoCultoTime.value = cultoTimeInput
                    proximoCultoDayFull.value = cultoDayFullInput
                    proximoCultoDayShort.value = cultoDayShortInput
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Salvar Alterações", fontWeight = FontWeight.Bold)
            }
        }

        item {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Adicionar ao Carrossel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            GlassTextField(
                value = carouselTitleInput,
                onValueChange = { carouselTitleInput = it },
                label = { Text("Título da Novidade/Evento") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = carouselDescriptionInput,
                onValueChange = { carouselDescriptionInput = it },
                label = { Text("Descrição Breve") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = carouselDateInput,
                onValueChange = { carouselDateInput = it },
                label = { Text("Data (Ex: 2026-07-20)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassButton(
                onClick = { carouselImagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (carouselImageUri != null) "Imagem Selecionada" else "Adicionar Imagem do Evento (Recomendado: 16:9)")
            }
        }
        @OptIn(ExperimentalMaterial3Api::class)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Tipo:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                FilterChip(
                    selected = carouselTagInput == "EVENTO",
                    onClick = { carouselTagInput = "EVENTO" },
                    label = { Text("EVENTO") }
                )
                FilterChip(
                    selected = carouselTagInput == "NOTÍCIA",
                    onClick = { carouselTagInput = "NOTÍCIA" },
                    label = { Text("NOTÍCIA") }
                )
            }
        }
        item {
            GlassButton(
                onClick = {
                    if (carouselTitleInput.isNotBlank() && carouselDescriptionInput.isNotBlank()) {
                        val newItem = CarouselItem(
                            id = System.currentTimeMillis().toString(),
                            title = carouselTitleInput,
                            description = carouselDescriptionInput,
                            date = if (carouselDateInput.isBlank()) "Hoje" else carouselDateInput,
                            tag = carouselTagInput,
                            imageUrl = carouselImageUri?.toString()
                        )
                        carouselItemsState.add(newItem)
                        NotificationHelper.showNotification(
                            context = context,
                            title = if (carouselTagInput == "EVENTO") "Novo Evento! 🗓️" else "Nova Notícia! 📰",
                            message = carouselTitleInput
                        )
                        carouselTitleInput = ""
                        carouselDescriptionInput = ""
                        carouselDateInput = ""
                        carouselImageUri = null
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Adicionar Destaque", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Destaques Ativos no Carrossel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        if (carouselItemsState.isEmpty()) {
            item {
                Text("Nenhum destaque cadastrado.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(carouselItemsState) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = item.tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.tag == "EVENTO") Color(0xFFD81B60) else Color(0xFF00ACC1)
                                )
                                Text(item.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text(item.description, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { carouselItemsState.remove(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditDevotionalsSection() {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var verse by remember { mutableStateOf("") }
    var verseRef by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Novo Devocional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            GlassTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Data (Ex: YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = verse,
                onValueChange = { verse = it },
                label = { Text("Versículo Base") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = verseRef,
                onValueChange = { verseRef = it },
                label = { Text("Referência Bíblica") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Conteúdo / Mensagem") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassButton(
                onClick = {
                    if (title.isNotBlank() && date.isNotBlank() && verse.isNotBlank()) {
                        val newId = java.util.UUID.randomUUID().toString()
                        val newDevotional = hashMapOf(
                            "title" to title,
                            "date" to date,
                            "verse" to verse,
                            "verseReference" to verseRef,
                            "content" to content,
                            "likes" to 0
                        )
                        
                        try {
                            val db = com.google.firebase.Firebase.firestore
                            db.collection("devotionals").document(newId).set(newDevotional)
                                .addOnSuccessListener {
                                    NotificationHelper.showNotification(
                                        context = context,
                                        title = "Novo Devocional Publicado! 📖",
                                        message = title
                                    )
                                    title = ""
                                    date = ""
                                    verse = ""
                                    verseRef = ""
                                    content = ""
                                }
                        } catch (e: Exception) {
                            NotificationHelper.showNotification(
                                context = context,
                                title = "Erro",
                                message = "Falha ao salvar no Firestore"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Adicionar Devocional", fontWeight = FontWeight.Bold)
            }
        }
        
        item {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Lista de Devocionais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        items(devotionalsState) { dev ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dev.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Text(dev.date, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { devotionalsState.remove(dev) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun EditServicesSection() {
    val context = LocalContext.current
    var serviceTitle by remember { mutableStateOf("") }
    var serviceDay by remember { mutableStateOf("") }
    var serviceDayShort by remember { mutableStateOf("") }
    var serviceTime by remember { mutableStateOf("") }
    var serviceDesc by remember { mutableStateOf("") }
    
    var eventTitle by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventDesc by remember { mutableStateOf("") }
    var eventLoc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Novo Culto Semanal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            GlassTextField(
                value = serviceTitle,
                onValueChange = { serviceTitle = it },
                label = { Text("Nome do Culto") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GlassTextField(
                    value = serviceDay,
                    onValueChange = { serviceDay = it },
                    label = { Text("Dia (Completo)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
                GlassTextField(
                    value = serviceDayShort,
                    onValueChange = { serviceDayShort = it },
                    label = { Text("Dia (Curto)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }
        item {
            GlassTextField(
                value = serviceTime,
                onValueChange = { serviceTime = it },
                label = { Text("Horário (Ex: 19:00)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = serviceDesc,
                onValueChange = { serviceDesc = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassButton(
                onClick = {
                    if (serviceTitle.isNotBlank() && serviceDay.isNotBlank() && serviceTime.isNotBlank()) {
                        weeklyServicesState.add(
                            ChurchService(
                                id = (weeklyServicesState.size + 1).toString(),
                                day = serviceDay,
                                dayShort = serviceDayShort,
                                time = serviceTime,
                                title = serviceTitle,
                                description = serviceDesc
                            )
                        )
                        NotificationHelper.showNotification(
                            context = context,
                            title = "Novo Culto Semanal! ⛪",
                            message = "$serviceTitle - $serviceDay às $serviceTime"
                        )
                        serviceTitle = ""
                        serviceDay = ""
                        serviceDayShort = ""
                        serviceTime = ""
                        serviceDesc = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Adicionar Culto", fontWeight = FontWeight.Bold)
            }
        }
        
        item {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Lista de Cultos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        items(weeklyServicesState) { service ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(service.title, fontWeight = FontWeight.Bold)
                        Text("${service.day} • ${service.time}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { weeklyServicesState.remove(service) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        item {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Novo Evento Especial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            GlassTextField(
                value = eventTitle,
                onValueChange = { eventTitle = it },
                label = { Text("Nome do Evento") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = eventDate,
                onValueChange = { eventDate = it },
                label = { Text("Data (Ex: YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = eventLoc,
                onValueChange = { eventLoc = it },
                label = { Text("Localização") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = eventDesc,
                onValueChange = { eventDesc = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassButton(
                onClick = {
                    if (eventTitle.isNotBlank() && eventDate.isNotBlank()) {
                        eventsState.add(
                            ChurchEvent(
                                id = (eventsState.size + 1).toString(),
                                title = eventTitle,
                                date = eventDate,
                                description = eventDesc,
                                location = eventLoc
                            )
                        )
                        NotificationHelper.showNotification(
                            context = context,
                            title = "Novo Evento Especial! 🎉",
                            message = "$eventTitle - $eventDate"
                        )
                        eventTitle = ""
                        eventDate = ""
                        eventDesc = ""
                        eventLoc = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Adicionar Evento", fontWeight = FontWeight.Bold)
            }
        }
        
        item {
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Lista de Eventos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        items(eventsState) { event ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(event.title, fontWeight = FontWeight.Bold)
                        Text(event.date, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { eventsState.remove(event) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun EditPrayersSection() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Pedidos de Oração Enviados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        if (prayerRequestsState.isEmpty()) {
            item {
                Text("Nenhum pedido de oração enviado ainda.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(prayerRequestsState) { req ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(req.date, style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(req.request, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { prayerRequestsState.remove(req) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditAboutSection() {
    var pastorNameInput by remember { mutableStateOf(pastorNameState.value) }
    var pastorTitleInput by remember { mutableStateOf(pastorTitleState.value) }
    var taglineInput by remember { mutableStateOf(missionTaglineState.value) }
    var meaningInput by remember { mutableStateOf(rhemaMeaningState.value) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Editar Informações da Igreja", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            GlassTextField(
                value = pastorNameInput,
                onValueChange = { pastorNameInput = it },
                label = { Text("Nome do Pastor") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = pastorTitleInput,
                onValueChange = { pastorTitleInput = it },
                label = { Text("Título do Pastor") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = taglineInput,
                onValueChange = { taglineInput = it },
                label = { Text("Slogan / Missão") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassTextField(
                value = meaningInput,
                onValueChange = { meaningInput = it },
                label = { Text("Significado de Rhema") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }
        item {
            GlassButton(
                onClick = {
                    pastorNameState.value = pastorNameInput
                    pastorTitleState.value = pastorTitleInput
                    missionTaglineState.value = taglineInput
                    rhemaMeaningState.value = meaningInput
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Salvar Informações", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            NotificationHelper.showNotification(
                context,
                "Notificações Ativadas!",
                "Você agora receberá alertas sobre novos devocionais diários."
            )
        }
    }

    LaunchedEffect(Unit) {
        hasPermission = NotificationHelper.hasNotificationPermission(context)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Configurações",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Personalize sua experiência no aplicativo",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        item {
            InteractiveCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Aparência",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Escolha o tema do aplicativo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeMode.values().forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> "Sistema"
                                ThemeMode.LIGHT -> "Claro"
                                ThemeMode.DARK -> "Escuro"
                            }
                            FilterChip(
                                selected = currentThemeMode.value == mode,
                                onClick = {
                                    currentThemeMode.value = mode
                                    SettingsManager.setThemeMode(context, mode)
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }
        
        item {
            InteractiveCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modo Cachê Offline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Priorizar conteúdo salvo localmente para economizar dados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isOfflineModeState.value,
                            onCheckedChange = { isOffline ->
                                isOfflineModeState.value = isOffline
                                SettingsManager.setOfflineMode(context, isOffline)
                            }
                        )
                    }
                }
            }
        }

        item {
            InteractiveCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notificações de Devocionais",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Lembretes diários para ler a Palavra de Deus",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(
                        text = "Fique por dentro de cada mensagem diária! Ativando as notificações, o MIC Rhema enviará um alerta inspirador sempre que um novo devocional for publicado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (hasPermission) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Notificações Ativas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            GlassButton(
                                onClick = {
                                    NotificationHelper.showNotification(
                                        context,
                                        "MIC Rhema Devocionais",
                                        "\"O Senhor te abençoe e te guarde...\" Novo devocional diário disponível!"
                                    )
                                },
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Testar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        GlassButton(
                            onClick = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    hasPermission = true
                                    NotificationHelper.showNotification(
                                        context,
                                        "Notificações Ativadas!",
                                        "Você agora receberá alertas sobre novos devocionais diários."
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text("Ativar Notificações", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            InteractiveCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sobre as Notificações",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Alertas diários automáticos pela manhã.\n• Conteúdo focado puramente em reflexão bíblica e avisos de cultos.\n• Sem anúncios ou spam.\n• Você pode gerenciar ou desativar os alertas a qualquer momento nas configurações do sistema do seu aparelho.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}



@Composable
fun DailyDevotionalCard(
    devotional: Devotional?,
    onReadFull: (Devotional) -> Unit,
    onShare: (Devotional) -> Unit
) {
    if (devotional != null) {
        InteractiveCard(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = devotional.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = devotional.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${devotional.verse}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = devotional.verseReference,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = devotional.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassButton(
                        onClick = { onReadFull(devotional) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Ler Completo", fontWeight = FontWeight.Bold)
                    }
                    
                    IconButton(
                        onClick = { onShare(devotional) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    } else {
        InteractiveCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Nenhum devocional disponível", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Puxe a tela para atualizar ou verifique seu arquivo local.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


@Composable
fun FirestoreDailyDevotional(
    onReadFull: (Devotional) -> Unit,
    onShare: (Devotional) -> Unit
) {
    var devotional by remember { mutableStateOf<Devotional?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val db = com.google.firebase.Firebase.firestore
            db.collection("devotionals")
                .get()
                .addOnSuccessListener { result ->
                    val list = mutableListOf<Devotional>()
                    for (document in result) {
                        val id = document.id
                        val title = document.getString("title") ?: ""
                        val date = document.getString("date") ?: ""
                        val verse = document.getString("verse") ?: ""
                        val verseReference = document.getString("verseReference") ?: ""
                        val textContent = document.getString("content") ?: ""
                        val likes = document.getLong("likes")?.toInt() ?: 0
                        list.add(Devotional(id, title, date, verse, verseReference, textContent, likes))
                    }
                    if (list.isNotEmpty()) {
                        list.sortByDescending { it.date }
                        devotional = list.first()
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    hasError = true
                    isLoading = false
                }
        } catch (e: Exception) {
            hasError = true
            isLoading = false
        }
    }

    if (isLoading) {
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonItem(width = 24.dp, height = 24.dp, shape = RoundedCornerShape(12.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    SkeletonItem(width = 120.dp, height = 16.dp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                SkeletonItem(width = 220.dp, height = 24.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonItem(width = 280.dp, height = 16.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonItem(width = 180.dp, height = 16.dp)
            }
        }
    } else if (hasError || devotional == null) {
        // Fallback to offline or standard daily devotional card if error or empty
        DailyDevotionalCard(
            devotional = devotionalsState.firstOrNull(),
            onReadFull = onReadFull,
            onShare = onShare
        )
    } else {
        DailyDevotionalCard(
            devotional = devotional,
            onReadFull = onReadFull,
            onShare = onShare
        )
    }
}


@Composable
fun DevotionalFeed() {
    var devotionals by remember { mutableStateOf<List<Devotional>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var lastDocument by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    val PAGE_SIZE = 10L

    val loadMore = {
        if (!isLoadingMore && hasMore) {
            if (devotionals.isNotEmpty()) isLoadingMore = true
            try {
                val db = com.google.firebase.Firebase.firestore
                var query = db.collection("devotionals")
                    .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE)
                
                lastDocument?.let { 
                    query = query.startAfter(it)
                }

                query.get()
                    .addOnSuccessListener { result ->
                        if (result.isEmpty) {
                            hasMore = false
                        } else {
                            lastDocument = result.documents.lastOrNull()
                            val list = mutableListOf<Devotional>()
                            for (document in result) {
                                val id = document.id
                                val title = document.getString("title") ?: ""
                                val date = document.getString("date") ?: ""
                                val verse = document.getString("verse") ?: ""
                                val verseReference = document.getString("verseReference") ?: ""
                                val textContent = document.getString("content") ?: ""
                                val likes = document.getLong("likes")?.toInt() ?: 0
                                list.add(Devotional(id, title, date, verse, verseReference, textContent, likes))
                            }
                            devotionals = devotionals + list
                            if (result.size() < PAGE_SIZE) {
                                hasMore = false
                            }
                        }
                        isLoading = false
                        isLoadingMore = false
                    }
                    .addOnFailureListener {
                        hasError = true
                        isLoading = false
                        isLoadingMore = false
                    }
            } catch (e: Exception) {
                hasError = true
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMore()
    }

    if (isLoading && devotionals.isEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(3) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SkeletonItem(width = 80.dp, height = 12.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        SkeletonItem(width = 200.dp, height = 24.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        SkeletonItem(width = 280.dp, height = 16.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        SkeletonItem(width = 240.dp, height = 16.dp)
                    }
                }
            }
        }
    } else if (hasError && devotionals.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Nenhum devocional encontrado.")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(devotionals.size) { index ->
                val dev = devotionals[index]
                DevotionalFeedItem(devotional = dev)
                
                if (index == devotionals.size - 1 && !isLoadingMore && hasMore) {
                    LaunchedEffect(index) {
                        loadMore()
                    }
                }
            }
            if (isLoadingMore) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SkeletonItem(width = 80.dp, height = 12.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            SkeletonItem(width = 200.dp, height = 20.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            SkeletonItem(width = 280.dp, height = 16.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevotionalFeedItem(devotional: Devotional) {
    var likes by remember { mutableStateOf(devotional.likes) }
    var isLiked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = devotional.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = devotional.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = devotional.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        if (!isLiked) {
                            likes += 1
                            isLiked = true
                            try {
                                val db = com.google.firebase.Firebase.firestore
                                db.collection("devotionals").document(devotional.id)
                                    .update("likes", com.google.firebase.firestore.FieldValue.increment(1))
                            } catch (e: Exception) {
                                // Ignore or handle
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$likes likes",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdminGlobalSearchSection(query: String) {
    val q = query.lowercase()
    
    val matchedIbrCourses = ibrCoursesState.filter { 
        it.title.lowercase().contains(q) || it.theme.lowercase().contains(q) || it.description.lowercase().contains(q) ||
        it.chapters.any { ch -> ch.title.lowercase().contains(q) || ch.description.lowercase().contains(q) }
    }
    
    val matchedBooks = contentBooksState.filter {
        it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
    }
    
    val matchedAudios = contentAudiosState.filter {
        it.title.lowercase().contains(q) || it.artist.lowercase().contains(q)
    }
    
    val matchedVideos = contentVideosState.filter {
        it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Resultados da Busca", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        
        if (matchedIbrCourses.isEmpty() && matchedBooks.isEmpty() && matchedAudios.isEmpty() && matchedVideos.isEmpty()) {
            item {
                Text("Nenhum resultado encontrado para '$query'.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        if (matchedIbrCourses.isNotEmpty()) {
            item { Text("Seminário IBR (${matchedIbrCourses.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(matchedIbrCourses) { course ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(course.title, fontWeight = FontWeight.Bold)
                            Text(course.theme, style = MaterialTheme.typography.labelSmall)
                            if (course.chapters.any { ch -> ch.title.lowercase().contains(q) }) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Aulas correspondentes:", style = MaterialTheme.typography.labelSmall)
                                course.chapters.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) }.forEach { ch ->
                                    Text("• ${ch.title}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (matchedBooks.isNotEmpty()) {
            item { Text("Livros VIP (${matchedBooks.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(matchedBooks) { book ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(book.title, fontWeight = FontWeight.Bold)
                            Text(book.author, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        
        if (matchedAudios.isNotEmpty()) {
            item { Text("Áudios VIP (${matchedAudios.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(matchedAudios) { audio ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(audio.title, fontWeight = FontWeight.Bold)
                            Text(audio.artist, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        
        if (matchedVideos.isNotEmpty()) {
            item { Text("Vídeos VIP (${matchedVideos.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(matchedVideos) { video ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(video.title, fontWeight = FontWeight.Bold)
                            Text(video.description, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
