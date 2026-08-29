package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen() {
    var selectedService by remember { mutableStateOf<ChurchService?>(null) }
    var selectedEvent by remember { mutableStateOf<ChurchEventModel?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val today = remember { LocalDate.now() }
    val publicEvents = churchEventsState
        .filter { event ->
            if (!event.isPublished) return@filter false
            val end = parseChurchEventDate(event.endDate.ifBlank { event.startDate })
            end == null || !end.isBefore(today)
        }
        .sortedWith(compareBy<ChurchEventModel> { parseChurchEventDate(it.startDate) ?: LocalDate.MAX }.thenBy { it.time })

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DateRange, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Cultos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Programação da igreja e eventos especiais", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { SectionTitle("Cultos Fixos", "Nossa programação semanal") }
                if (weeklyServicesState.isEmpty()) {
                    item { EmptyServicesCard("A programação fixa ainda não foi cadastrada.") }
                } else {
                    items(weeklyServicesState, key = { it.id.ifBlank { "${it.day}-${it.time}-${it.title}" } }) { service ->
                        FixedServiceCard(service) { selectedService = service }
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionTitle("Eventos Especiais", "Próximos eventos e programações temporárias")
                }
                if (publicEvents.isEmpty()) {
                    item { EmptyServicesCard("Nenhum evento especial publicado no momento.") }
                } else {
                    items(publicEvents, key = { it.id.ifBlank { "${it.startDate}-${it.title}" } }) { event ->
                        EventCard(event, if (event.description.isNotBlank()) ({ selectedEvent = event }) else null)
                    }
                }
            }
        }
    }

    selectedService?.let { service ->
        AlertDialog(
            onDismissRequest = { selectedService = null },
            title = { Text(service.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${service.day} às ${service.time}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(service.description.ifBlank { "A descrição deste culto ainda não foi cadastrada." }, style = MaterialTheme.typography.bodyLarge)
                }
            },
            confirmButton = { TextButton(onClick = { selectedService = null }) { Text("Fechar") } }
        )
    }

    selectedEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { selectedEvent = null },
            title = { Text(event.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(formatChurchEventPeriod(event), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (event.time.isNotBlank()) Text("Horário: ${event.time}")
                    if (event.location.isNotBlank()) Text("Local: ${event.location}")
                    if (event.preacher.isNotBlank()) Text("Preletor: ${event.preacher}")
                    if (event.description.isNotBlank()) {
                        HorizontalDivider()
                        Text(event.description, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { selectedEvent = null }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FixedServiceCard(service: ChurchService, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(20.dp)) {
            Text(service.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("${service.day} às ${service.time}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            if (service.description.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(service.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

@Composable
private fun EventCard(event: ChurchEventModel, onClick: (() -> Unit)?) {
    val modifier = if (onClick != null) Modifier.fillMaxWidth().clickable(onClick = onClick) else Modifier.fillMaxWidth()
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column {
            if (event.bannerUrl.isNotBlank()) {
                coil.compose.AsyncImage(model = event.bannerUrl, contentDescription = "Banner de ${event.title}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)))
            }
            Column(Modifier.padding(18.dp)) {
                Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(formatChurchEventPeriod(event), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (event.time.isNotBlank()) Text(event.time, style = MaterialTheme.typography.bodyMedium)
                if (event.location.isNotBlank()) Text(event.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (event.preacher.isNotBlank()) Text("Preletor: ${event.preacher}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (event.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Toque para ver os detalhes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun EmptyServicesCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
        Text(message, modifier = Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun parseChurchEventDate(value: String): LocalDate? = runCatching { LocalDate.parse(value.trim()) }.getOrNull()

private fun formatChurchEventPeriod(event: ChurchEventModel): String {
    val start = formatChurchEventDate(event.startDate)
    val endRaw = event.endDate.ifBlank { event.startDate }
    val end = formatChurchEventDate(endRaw)
    return when {
        start.isBlank() -> "Data a definir"
        end.isBlank() || endRaw == event.startDate -> start
        else -> "$start até $end"
    }
}

private fun formatChurchEventDate(value: String): String {
    val parsed = parseChurchEventDate(value) ?: return value
    return "${parsed.dayOfMonth.toString().padStart(2, '0')}/${parsed.monthValue.toString().padStart(2, '0')}/${parsed.year}"
}
