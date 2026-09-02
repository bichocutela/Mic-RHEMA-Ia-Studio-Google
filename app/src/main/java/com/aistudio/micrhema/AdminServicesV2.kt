package com.aistudio.micrhema

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/** Novo gerenciamento administrativo de Cultos: programação recorrente e eventos temporários. */
@Composable
fun EditServicesSectionV2() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingService by remember { mutableStateOf<ChurchService?>(null) }
    var editingEvent by remember { mutableStateOf<ChurchEventModel?>(null) }
    var showServiceDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Cultos e Eventos", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("A programação fixa se repete semanalmente. Eventos possuem período próprio.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Programação Fixa") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Eventos") })
        }
        Spacer(Modifier.height(12.dp))

        if (selectedTab == 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Cultos Fixos", fontWeight = FontWeight.Bold); Text("Sem data de início ou término", style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { editingService = null; showServiceDialog = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Novo") }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(weeklyServicesState, key = { it.id.ifBlank { "${it.day}-${it.time}-${it.title}" } }) { service ->
                    Card(Modifier.fillMaxWidth().clickable { editingService = service; showServiceDialog = true }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(service.title, fontWeight = FontWeight.Bold); Text("${service.day} • ${service.time}", style = MaterialTheme.typography.bodySmall); if (service.description.isNotBlank()) Text(service.description, style = MaterialTheme.typography.bodySmall, maxLines = 2) }
                            IconButton(onClick = { editingService = service; showServiceDialog = true }) { Icon(Icons.Default.Edit, "Editar") }
                            IconButton(onClick = { removeChurchService(service) }) { Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Eventos", fontWeight = FontWeight.Bold); Text("Com início, término, banner e publicação", style = MaterialTheme.typography.bodySmall) }
                Button(onClick = { editingEvent = null; showEventDialog = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Novo") }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(churchEventsState, key = { it.id.ifBlank { "${it.startDate}-${it.title}" } }) { event ->
                    Card(Modifier.fillMaxWidth().clickable { editingEvent = event; showEventDialog = true }) {
                        Column {
                            if (event.bannerUrl.isNotBlank()) AsyncImage(model = event.bannerUrl, contentDescription = "Banner de ${event.title}", modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Crop)
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(event.title, fontWeight = FontWeight.Bold); Text(eventPeriodLabel(event), style = MaterialTheme.typography.bodySmall); Text(if (event.isPublished) "Publicado" else "Oculto", style = MaterialTheme.typography.labelSmall) }
                                Switch(checked = event.isPublished, onCheckedChange = { saveChurchEvent(event.copy(isPublished = it)) })
                                IconButton(onClick = { editingEvent = event; showEventDialog = true }) { Icon(Icons.Default.Edit, "Editar") }
                                IconButton(onClick = { removeChurchEvent(event) }) { Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showServiceDialog) FixedServiceDialog(item = editingService, onDismiss = { showServiceDialog = false }, onSave = { addChurchService(it); showServiceDialog = false })
    if (showEventDialog) ChurchEventAdminDialog(item = editingEvent, onDismiss = { showEventDialog = false }, onSave = { saveChurchEvent(it); showEventDialog = false })
}

@Composable
private fun FixedServiceDialog(item: ChurchService?, onDismiss: () -> Unit, onSave: (ChurchService) -> Unit) {
    var title by remember(item) { mutableStateOf(item?.title.orEmpty()) }; var day by remember(item) { mutableStateOf(item?.day.orEmpty()) }; var time by remember(item) { mutableStateOf(item?.time.orEmpty()) }; var description by remember(item) { mutableStateOf(item?.description.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (item == null) "Novo culto fixo" else "Editar culto fixo") }, text = { Column(modifier = Modifier.imePadding().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(day, { day = it }, label = { Text("Dia da semana") }, placeholder = { Text("Ex.: Terça-feira") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(time, { time = it }, label = { Text("Horário") }, placeholder = { Text("19:00") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, minLines = 3, modifier = Modifier.fillMaxWidth()); Text("Cultos fixos não usam data de início ou término.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } }, confirmButton = { TextButton(enabled = title.isNotBlank() && day.isNotBlank() && time.isNotBlank(), onClick = { val cleanDay = day.trim(); onSave(ChurchService(id = item?.id ?: java.util.UUID.randomUUID().toString(), date = "", day = cleanDay, dayShort = cleanDay.take(3).uppercase(), time = time.trim(), title = title.trim(), description = description.trim(), type = item?.type ?: "culto", content = item?.content.orEmpty(), mediaUrl = item?.mediaUrl.orEmpty(), isApproved = item?.isApproved ?: true)) }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
private fun ChurchEventAdminDialog(item: ChurchEventModel?, onDismiss: () -> Unit, onSave: (ChurchEventModel) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember(item) { mutableStateOf(item?.title.orEmpty()) }; var description by remember(item) { mutableStateOf(item?.description.orEmpty()) }; var preacher by remember(item) { mutableStateOf(item?.preacher.orEmpty()) }; var startDate by remember(item) { mutableStateOf(item?.startDate.orEmpty()) }; var endDate by remember(item) { mutableStateOf(item?.endDate.orEmpty()) }; var time by remember(item) { mutableStateOf(item?.time.orEmpty()) }; var location by remember(item) { mutableStateOf(item?.location.orEmpty()) }; var bannerUrl by remember(item) { mutableStateOf(item?.bannerUrl.orEmpty()) }; var published by remember(item) { mutableStateOf(item?.isPublished ?: true) }
    var uploadingBanner by remember(item) { mutableStateOf(false) }; var uploadProgress by remember(item) { mutableFloatStateOf(0f) }; var uploadError by remember(item) { mutableStateOf<String?>(null) }

    fun uploadBanner(uri: Uri) {
        uploadingBanner = true; uploadProgress = 0f; uploadError = null
        scope.launch {
            runCatching {
                StorageManager.uploadMediaAsset(context = context, uri = uri, uid = "admin", onProgress = { uploadProgress = it }, mimeTypeHint = context.contentResolver.getType(uri))
            }.onSuccess { result -> bannerUrl = result.signedUrl; uploadingBanner = false }
             .onFailure { error -> uploadError = error.message ?: "Não foi possível enviar o banner."; uploadingBanner = false }
        }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) uploadBanner(uri) }

    AlertDialog(onDismissRequest = { if (!uploadingBanner) onDismiss() }, title = { Text(if (item == null) "Novo evento" else "Editar evento") }, text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 560.dp).imePadding()) {
            item { OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(preacher, { preacher = it }, label = { Text("Preletor") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(startDate, { startDate = it }, label = { Text("Data inicial (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(endDate, { endDate = it }, label = { Text("Data final (AAAA-MM-DD)") }, supportingText = { Text("Deixe vazio para evento de um dia") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(time, { time = it }, label = { Text("Horário") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(location, { location = it }, label = { Text("Local") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Banner do evento", fontWeight = FontWeight.SemiBold)
                    if (bannerUrl.isNotBlank()) AsyncImage(model = bannerUrl, contentDescription = "Prévia do banner", modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentScale = ContentScale.Crop)
                    Button(onClick = { bannerPicker.launch("image/*") }, enabled = !uploadingBanner, modifier = Modifier.fillMaxWidth()) { Text(if (bannerUrl.isBlank()) "Selecionar banner 16:9" else "Trocar banner") }
                    if (uploadingBanner) { LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth()); Text("Enviando banner...", style = MaterialTheme.typography.bodySmall) }
                    uploadError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Text("JPEG, PNG ou WebP. A prévia usa proporção 16:9.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(checked = published, onCheckedChange = { published = it }); Spacer(Modifier.width(8.dp)); Text(if (published) "Publicado" else "Oculto") } }
        }
    }, confirmButton = { TextButton(enabled = title.isNotBlank() && startDate.isNotBlank() && !uploadingBanner, onClick = { onSave(ChurchEventModel(id = item?.id.orEmpty(), title = title.trim(), description = description.trim(), preacher = preacher.trim(), startDate = startDate.trim(), endDate = endDate.trim().ifBlank { startDate.trim() }, time = time.trim(), location = location.trim(), bannerUrl = bannerUrl.trim(), isPublished = published, createdAt = item?.createdAt ?: 0L, updatedAt = item?.updatedAt ?: 0L)) }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !uploadingBanner) { Text("Cancelar") } })
}

private fun eventPeriodLabel(event: ChurchEventModel): String { val period = if (event.endDate.isBlank() || event.endDate == event.startDate) event.startDate else "${event.startDate} até ${event.endDate}"; return listOf(period, event.time, event.location).filter { it.isNotBlank() }.joinToString(" • ") }
