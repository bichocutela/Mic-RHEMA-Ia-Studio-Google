package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen() {
    var selectedService by remember { mutableStateOf<ChurchService?>(null) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
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
            modifier = Modifier.padding(paddingValues).fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Horários",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(weeklyServicesState) { service ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedService = service },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = service.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${service.day} às ${service.time}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (service.date.isNotBlank()) {
                                Text(
                                    text = formatChurchServiceDate(service.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (service.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Toque para ver a descrição",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (service.mediaUrl.isNotBlank() && isYoutubeUrl(service.mediaUrl)) {
                                val thumb = getYoutubeThumbnailUrl(service.mediaUrl)
                                if (thumb != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    coil.compose.AsyncImage(
                                        model = thumb,
                                        contentDescription = "Capa do Vídeo",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(service.mediaUrl))
                                                context.startActivity(intent)
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    } // PullToRefreshBox

    selectedService?.let { service ->
        AlertDialog(
            onDismissRequest = { selectedService = null },
            title = { Text(service.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "${service.day} às ${service.time}",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (service.date.isNotBlank()) {
                        Text(formatChurchServiceDate(service.date), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        service.description.ifBlank { "A descrição deste culto ainda não foi cadastrada." },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedService = null }) { Text("Fechar") }
            }
        )
    }
}

private fun formatChurchServiceDate(date: String): String = runCatching {
    val parsed = java.time.LocalDate.parse(date)
    "Data: ${parsed.dayOfMonth.toString().padStart(2, '0')}/${parsed.monthValue.toString().padStart(2, '0')}/${parsed.year}"
}.getOrDefault(date)
