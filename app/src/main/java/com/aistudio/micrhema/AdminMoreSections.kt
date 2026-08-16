package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// PLANS
@Composable
fun EditPlansSection() {
    var showDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<PlanCategory?>(null) }
    var editingTheme by remember { mutableStateOf<PlanTheme?>(null) }
    var currentThemes by remember { mutableStateOf(mutableListOf<PlanTheme>()) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gerenciar Planos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true; editingPlan = null; currentThemes = mutableListOf() }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                Spacer(Modifier.width(4.dp))
                Text("Novo")
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(biblePlansState) { plan ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        editingPlan = plan
                        currentThemes = plan.themes.toMutableList()
                        showDialog = true 
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(plan.name, fontWeight = FontWeight.Bold)
                            Text("${plan.themes.size} Temas", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { 
                            biblePlansState.remove(plan)
                            Firebase.firestore.collection("bible_plans").document(plan.name).delete()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        var name by remember { mutableStateOf(editingPlan?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingPlan == null) "Novo Plano" else "Editar Plano") },
            text = {
                Column(modifier = Modifier.height(400.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome do Plano") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Temas:", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { editingTheme = null; showThemeDialog = true }) {
                            Text("Adicionar Tema")
                        }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        items(currentThemes) { theme ->
                            Card(modifier = Modifier.fillMaxWidth().clickable { editingTheme = theme; showThemeDialog = true }) {
                                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(theme.title, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { currentThemes.remove(theme) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remover Tema", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotEmpty()) {
                        val newPlan = PlanCategory(name, editingPlan?.color ?: androidx.compose.ui.graphics.Color.Gray, currentThemes.toList())
                        if (editingPlan != null) {
                            val idx = biblePlansState.indexOf(editingPlan)
                            if (idx >= 0) biblePlansState[idx] = newPlan
                        } else {
                            biblePlansState.add(newPlan)
                        }
                        val map = mapOf("name" to name, "color" to newPlan.color.value.toLong(), "themes" to newPlan.themes.map { mapOf("title" to it.title, "content" to it.content, "verses" to it.verses, "imageUrl" to it.imageUrl) })
                        Firebase.firestore.collection("bible_plans").document(name).set(map)
                        showDialog = false
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
    
    if (showThemeDialog) {
        var tTitle by remember { mutableStateOf(editingTheme?.title ?: "") }
        var tContent by remember { mutableStateOf(editingTheme?.content ?: "") }
        var tVerse by remember { mutableStateOf(if (editingTheme?.verses?.isNotEmpty() == true) editingTheme!!.verses[0] else "") }
        var tImage by remember { mutableStateOf(editingTheme?.imageUrl ?: "") }
        
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(if (editingTheme == null) "Novo Tema" else "Editar Tema") },
            text = {
                Column {
                    OutlinedTextField(value = tTitle, onValueChange = { tTitle = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tVerse, onValueChange = { tVerse = it }, label = { Text("Versículo (Ref)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tImage, onValueChange = { tImage = it }, label = { Text("URL da Imagem") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = tContent, onValueChange = { tContent = it }, label = { Text("Conteúdo") }, modifier = Modifier.fillMaxWidth().height(120.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tTitle.isNotEmpty()) {
                        val newTheme = PlanTheme(tTitle, tContent, listOf(tVerse), convertGoogleDriveUrl(tImage))
                        if (editingTheme != null) {
                            val idx = currentThemes.indexOf(editingTheme)
                            if (idx >= 0) {
                                val newList = currentThemes.toMutableList()
                                newList[idx] = newTheme
                                currentThemes = newList
                            }
                        } else {
                            val newList = currentThemes.toMutableList()
                            newList.add(newTheme)
                            currentThemes = newList
                        }
                        showThemeDialog = false
                    }
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cancelar") } }
        )
    }
}

// SERVICES (Cultos)
@Composable
fun EditServicesSection() {
    var showDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<ChurchService?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gerenciar Cultos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true; editingService = null }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Culto")
                Spacer(Modifier.width(4.dp))
                Text("Novo Culto")
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(weeklyServicesState) { service ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { editingService = service; showDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(service.title, fontWeight = FontWeight.Bold)
                            Text("${service.day} às ${service.time}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { removeChurchService(service);  }) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        var title by remember { mutableStateOf(editingService?.title ?: "") }
        var day by remember { mutableStateOf(editingService?.day ?: "") }
        var time by remember { mutableStateOf(editingService?.time ?: "") }
        var mediaUrl by remember { mutableStateOf(editingService?.mediaUrl ?: "") }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingService == null) "Novo Culto" else "Editar Culto") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título (ex: Culto da Família)") })
                    OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Dia (ex: Domingo)") })
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Horário (ex: 18:30)") })
                    OutlinedTextField(value = mediaUrl, onValueChange = { mediaUrl = it }, label = { Text("URL do Vídeo (YouTube)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotEmpty()) {
                        val newService = ChurchService(id = editingService?.id ?: java.util.UUID.randomUUID().toString(), title = title, day = day, time = time, dayShort = day.take(3).uppercase(), mediaUrl = mediaUrl)
                        
                        addChurchService(newService)
                        showDialog = false
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}

// DEVOTIONALS
@Composable
fun EditDevotionalsSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var editingDevotional by remember { mutableStateOf<Devotional?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Gerenciar Devocionais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showDialog = true; editingDevotional = null }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                Spacer(Modifier.width(4.dp))
                Text("Novo")
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(devotionalsState) { dev ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { editingDevotional = dev; showDialog = true },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(dev.title, fontWeight = FontWeight.Bold)
                            Text(dev.date, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { 
                            scope.launch {
                                val res = DevotionalRepository.deleteDevotional(dev.id)
                                if (res.isSuccess) {
                                    android.widget.Toast.makeText(context, "Removido", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Erro ao remover", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        var title by remember { mutableStateOf(editingDevotional?.title ?: "") }
        var content by remember { mutableStateOf(editingDevotional?.content ?: "") }
        var verse by remember { mutableStateOf(editingDevotional?.verse ?: "") }
        var mediaUrl by remember { mutableStateOf(editingDevotional?.mediaUrl ?: "") }
        var ref by remember { mutableStateOf(editingDevotional?.verseReference ?: "") }
        var date by remember { mutableStateOf(editingDevotional?.date ?: "") }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingDevotional == null) "Novo Devocional" else "Editar Devocional") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") })
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (ex: 24 de Maio)") })
                    OutlinedTextField(value = ref, onValueChange = { ref = it }, label = { Text("Referência (ex: João 3:16)") })
                    OutlinedTextField(value = verse, onValueChange = { verse = it }, label = { Text("Versículo") })
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Conteúdo") }, modifier = Modifier.height(120.dp))
                    OutlinedTextField(value = mediaUrl, onValueChange = { mediaUrl = it }, label = { Text("URL de Mídia (YouTube, etc)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotEmpty()) {
                        val newDev = Devotional(
                            id = editingDevotional?.id ?: java.util.UUID.randomUUID().toString(),
                            title = title,
                            date = date,
                            verse = verse,
                            verseReference = ref,
                            content = content,
                            likes = editingDevotional?.likes ?: 0,
                            mediaUrl = mediaUrl,
                            timestamp = editingDevotional?.timestamp ?: System.currentTimeMillis()
                        )
                        scope.launch {
                            val res = DevotionalRepository.addDevotional(newDev)
                            if (res.isSuccess) {
                                android.widget.Toast.makeText(context, "Devocional salvo!", android.widget.Toast.LENGTH_SHORT).show()
                                showDialog = false
                                forceRefreshData()
                            } else {
                                android.widget.Toast.makeText(context, "Erro ao salvar. Verifique as regras do Firestore ou sua conexão.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancelar") } }
        )
    }
}

// ABOUT
@Composable
fun EditAboutSection() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Sobre", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        
        var pastorTitle by remember { mutableStateOf(pastorTitleState.value) }
        var missionTagline by remember { mutableStateOf(missionTaglineState.value) }
        var rhemaMeaning by remember { mutableStateOf(rhemaMeaningState.value) }
        
        OutlinedTextField(value = pastorNameState.value, onValueChange = { pastorNameState.value = it }, label = { Text("Nome do Pastor") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = pastorTitle, onValueChange = { pastorTitle = it; pastorTitleState.value = it }, label = { Text("Título do Pastor") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = missionTagline, onValueChange = { missionTagline = it; missionTaglineState.value = it }, label = { Text("Missão") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = rhemaMeaning, onValueChange = { rhemaMeaning = it; rhemaMeaningState.value = it }, label = { Text("Significado de Rhema") }, modifier = Modifier.fillMaxWidth().height(150.dp))
        
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val db = Firebase.firestore
            db.collection("settings").document("about").set(mapOf(
                "pastorName" to pastorNameState.value,
                "pastorTitle" to pastorTitleState.value,
                "missionTagline" to missionTaglineState.value,
                "rhemaMeaning" to rhemaMeaningState.value
            ))
        }) {
            Text("Salvar Informações")
        }
    }
}


// SETTINGS
@Composable
fun EditSettingsSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val remoteSettings = adminAppSettingsState.value
    var notificationsEnabled by remember(remoteSettings.updatedAt) { mutableStateOf(remoteSettings.notificationsEnabled) }
    var showDonationsTab by remember(remoteSettings.updatedAt) { mutableStateOf(remoteSettings.showDonationsTab) }
    var savingSettings by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Configurações Adicionais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Estas opções são sincronizadas pelo Firebase para todos os aparelhos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
            Text("Habilitar Notificações")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = showDonationsTab, onCheckedChange = { showDonationsTab = it })
            Text("Mostrar aba de Doações")
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                savingSettings = true
                saveAdminAppSettings(
                    settings = AdminAppSettings(
                        notificationsEnabled = notificationsEnabled,
                        showDonationsTab = showDonationsTab
                    ),
                    onSuccess = {
                        savingSettings = false
                        android.widget.Toast.makeText(context, "Configurações sincronizadas para todos os aparelhos", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        savingSettings = false
                        android.widget.Toast.makeText(context, "Não foi possível sincronizar: ${error.message ?: "verifique sua conexão"}", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            },
            enabled = !savingSettings
        ) {
            if (savingSettings) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            else Text("Salvar Configurações")
        }

        Spacer(Modifier.height(16.dp))
        val scope = rememberCoroutineScope()
        var syncing by remember { mutableStateOf(false) }
        
        Button(
            onClick = {
                scope.launch {
                    syncing = true
                    try {
                        Firebase.firestore.collection("settings").document("sync_trigger").set(mapOf("timestamp" to System.currentTimeMillis()))
                        forceRefreshData()
                        android.widget.Toast.makeText(context, "Sincronização forçada com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Erro ao sincronizar", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    syncing = false
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(if (syncing) Icons.Default.Refresh else Icons.Default.Refresh, contentDescription = "Sincronizar") // Replace later with appropriate icon
            Spacer(Modifier.width(8.dp))
            Text(if (syncing) "Sincronizando..." else "Sincronizar Banco de Dados")
        }
    }
}



// BANNERS
@Composable
fun EditBannersSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    var showDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<CarouselItem?>(null) }
    
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            isUploading = true
            scope.launch {
                try {
                    uploadProgress = 0f
                    val uploadedUrl = StorageHelper.uploadFile(context, uri, "banners", mimeTypeHint = "image/*") { progress ->
                        uploadProgress = progress
                    }
                    if (uploadedUrl.isBlank()) throw IllegalStateException("O Supabase não retornou a URL do banner.")
                    val newItem = CarouselItem(
                        id = java.util.UUID.randomUUID().toString(),
                        imageUrl = uploadedUrl,
                        eventDate = ""
                    )
                    addCarouselItem(newItem)
                } catch (error: Exception) {
                    android.widget.Toast.makeText(context, "Upload do banner não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Banners (Destaques)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Adicione imagens no formato 16:9 para a tela inicial.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(carouselItemsState, key = { it.id }) { banner ->
                val dateStr = banner.eventDate
                var isActive = true
                if (dateStr.isNotEmpty()) {
                    try {
                        val eventDate = java.time.LocalDate.parse(dateStr)
                        val today = java.time.LocalDate.now()
                        if (eventDate.isBefore(today)) {
                            isActive = false
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                val formattedDate = if (dateStr.isNotEmpty()) {
                    try {
                        val parts = dateStr.split("-")
                        "${parts[2]}/${parts[1]}/${parts[0]}"
                    } catch(e: Exception) { dateStr }
                } else ""
                
                Card(modifier = Modifier.fillMaxWidth().clickable { editingBanner = banner; showDialog = true }) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        coil.compose.AsyncImage(
                            model = banner.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp, 56.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (dateStr.isEmpty()) {
                                Text("🟢 Ativo - Banner permanente", style = MaterialTheme.typography.bodySmall)
                            } else if (isActive) {
                                Text("🟢 Ativo - Evento: $formattedDate", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("🔴 Expirado - Evento: $formattedDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            Text(
                                text = if (banner.eventInfo.isNotBlank()) "Clique ativo: evento configurado" else "Sem clique: nenhum evento informado",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (banner.eventInfo.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            removeCarouselItem(banner)
                        }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    editingBanner = CarouselItem(id = java.util.UUID.randomUUID().toString())
                    showDialog = true
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Adicionar URL")
            }
            
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier.weight(1f),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Imagem")
                    Spacer(Modifier.width(4.dp))
                    Text("Galeria")
                }
            }
        }
    }

    if (showDialog && editingBanner != null) {
        BannerEditDialog(
            banner = editingBanner!!,
            onDismiss = { showDialog = false; editingBanner = null },
            onSave = { updatedBanner ->
                addCarouselItem(updatedBanner)
                showDialog = false
                editingBanner = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BannerEditDialog(banner: CarouselItem, onDismiss: () -> Unit, onSave: (CarouselItem) -> Unit) {
    var imageUrl by remember { mutableStateOf(banner.imageUrl ?: "") }
    var eventInfo by remember { mutableStateOf(banner.eventInfo) }
    var eventDate by remember { mutableStateOf(banner.eventDate) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (eventDate.isNotEmpty()) {
            try {
                java.time.LocalDate.parse(eventDate).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (e: Exception) { null }
        } else null
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Banner") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL da Imagem") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = eventInfo,
                    onValueChange = { eventInfo = it },
                    label = { Text("Informações do evento (opcional)") },
                    placeholder = { Text("Ex.: Culto especial no domingo, às 18h, no templo") },
                    supportingText = { Text("Se preenchido, o banner ficará clicável e abrirá a área de cultos.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                
                val formattedDate = if (eventDate.isNotEmpty()) {
                    try {
                        val parts = eventDate.split("-")
                        "${parts[2]}/${parts[1]}/${parts[0]}"
                    } catch(e: Exception) { eventDate }
                } else "Sem data / Banner permanente"

                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = { },
                    label = { Text("Data do evento (opcional)") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.DateRange, contentDescription = "Selecionar Data")
                        }
                    }
                )
                
                if (eventDate.isNotEmpty()) {
                    TextButton(onClick = { eventDate = "" }) {
                        Text("Remover data (Tornar permanente)")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalUrl = convertGoogleDriveUrl(imageUrl)
                onSave(
                    banner.copy(
                        imageUrl = finalUrl,
                        eventDate = eventDate,
                        eventInfo = eventInfo.trim()
                    )
                )
            }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        eventDate = date.toString() // yyyy-MM-dd
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// DONATIONS
@Composable
fun EditDonationsSection() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pixKey by remember { mutableStateOf(pixKeyState.value) }
    var qrCodeUrl by remember { mutableStateOf(pixQrCodeUrlState.value) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            isUploading = true
            scope.launch {
                try {
                    uploadProgress = 0f
                    val uploadedUrl = StorageHelper.uploadFile(context, uri, "donations", mimeTypeHint = "image/*") { progress ->
                        uploadProgress = progress
                    }
                    if (uploadedUrl.isBlank()) throw IllegalStateException("O Supabase não retornou a URL do QR Code.")
                    qrCodeUrl = uploadedUrl
                } catch (error: Exception) {
                    android.widget.Toast.makeText(context, "Upload do QR Code não concluído: ${error.message ?: "verifique a conexão"}", android.widget.Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Dízimos e Ofertas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Configure a chave Pix e o QR Code que serão exibidos na tela de doações.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = pixKey,
            onValueChange = { pixKey = it },
            label = { Text("Chave Pix") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text("QR Code (Opcional)", fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (qrCodeUrl.isNotEmpty()) {
                coil.compose.AsyncImage(
                    model = qrCodeUrl,
                    contentDescription = "QR Code Pix",
                    modifier = Modifier.size(100.dp).padding(end = 16.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            
            Column {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("${(uploadProgress * 100).toInt()}%")
                    } else {
                        Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Adicionar QR Code")
                        Spacer(Modifier.width(8.dp))
                        Text(if (qrCodeUrl.isNotEmpty()) "Trocar Imagem" else "Enviar Imagem")
                    }
                }
                if (qrCodeUrl.isNotEmpty()) {
                    TextButton(onClick = { qrCodeUrl = "" }) {
                        Text("Remover Imagem", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = {
                scope.launch {
                    saving = true
                    try {
                        saveDonationsToFirestore(pixKey, qrCodeUrl)
                        android.widget.Toast.makeText(context, "Salvo com sucesso!", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Erro ao salvar", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    saving = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving && !isUploading
        ) {
            if (saving) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Salvar Configurações")
            }
        }
    }
}
