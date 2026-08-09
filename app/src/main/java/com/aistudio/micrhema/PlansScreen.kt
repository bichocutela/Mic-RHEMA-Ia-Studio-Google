package com.aistudio.micrhema

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(initialThemeName: String? = null, onNavigateToBible: (String, Int) -> Unit = { _, _ -> }) {
    var selectedCategory by remember { mutableStateOf<PlanCategory?>(null) }
    var selectedTheme by remember { mutableStateOf<PlanTheme?>(null) }
    
    androidx.activity.compose.BackHandler(enabled = selectedTheme != null || selectedCategory != null) {
        if (selectedTheme != null) {
            selectedTheme = null
        } else if (selectedCategory != null) {
            selectedCategory = null
        }
    }

    LaunchedEffect(initialThemeName) {
        if (initialThemeName != null) {
            val allPlans = if (biblePlansState.isEmpty()) PlansData.categories else biblePlansState
            val theme = allPlans.flatMap { it.themes }.find { it.title == initialThemeName }
            if (theme != null) {
                selectedTheme = theme
            }
        }
    }

    if (selectedTheme != null) {
        ThemeDetailScreen(
            theme = selectedTheme!!, 
            onBack = { selectedTheme = null },
            onGoToVerse = { verseRef -> 
                val parts = verseRef.split(" ")
                if (parts.size >= 2) {
                    val book = if (parts[0].first().isDigit()) parts[0] + " " + parts[1] else parts[0]
                    val chapStr = verseRef.substringAfter(book).trim().split(":").firstOrNull()
                    val chap = chapStr?.toIntOrNull() ?: 1
                    onNavigateToBible(book, chap)
                }
            }
        )
    } else if (selectedCategory != null) {
        CategoryScreen(category = selectedCategory!!, onBack = { selectedCategory = null }, onThemeClick = { selectedTheme = it })
    } else {
        MainPlansScreen(onCategoryClick = { selectedCategory = it }, onThemeClick = { selectedTheme = it })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPlansScreen(onCategoryClick: (PlanCategory) -> Unit, onThemeClick: (PlanTheme) -> Unit) {
    val randomThemes = remember { (if (biblePlansState.isEmpty()) PlansData.categories else biblePlansState).flatMap { it.themes }.shuffled() }
    var currentBannerIndex by remember { mutableStateOf(0) }

    LaunchedEffect(randomThemes) {
        while (true) {
            delay(3000)
            currentBannerIndex = (currentBannerIndex + 1) % randomThemes.size
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                forceRefreshData()
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Planos",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp, 24.dp, 20.dp, 12.dp)
        )

        // Banner Carousel
        if (randomThemes.isNotEmpty()) {
            val theme = randomThemes[currentBannerIndex]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 20.dp)
                    .clickable { onThemeClick(theme) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = theme.imageUrl,
                        contentDescription = theme.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = theme.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Plano em destaque",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Explorar por Emoções",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(if (biblePlansState.isEmpty()) PlansData.categories else biblePlansState) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onCategoryClick(category) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = category.color)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = category.name.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
    } // PullToRefreshBox
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(category: PlanCategory, onBack: () -> Unit, onThemeClick: (PlanTheme) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        val unused = paddingValues
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(category.themes) { theme ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeClick(theme) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = theme.imageUrl,
                            contentDescription = theme.title,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                            Text(
                                text = theme.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = theme.verses.firstOrNull() ?: "",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailScreen(theme: PlanTheme, onBack: () -> Unit, onGoToVerse: (String) -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        val unused = paddingValues
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = theme.imageUrl,
                contentDescription = theme.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = theme.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Versículos Base:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        theme.verses.forEach { verse ->
                            Text(
                                text = "📖 $verse",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = theme.content,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Concluir Leitura", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { onGoToVerse(theme.verses.firstOrNull() ?: "") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Ir para Versículo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
