package com.aistudio.micrhema

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
                        IconButton(onClick = { removeDevotional(dev); devotionalsState.remove(dev) }) {
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
                        if (editingDevotional != null) {
                            val idx = devotionalsState.indexOf(editingDevotional)
                            if (idx >= 0) devotionalsState[idx] = newDev
                        } else {
                            devotionalsState.add(newDev)
                        }
                        addDevotional(newDev)
                        showDialog = false
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Configurações Adicionais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = true, onCheckedChange = {})
            Text("Habilitar Notificações")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = true, onCheckedChange = {})
            Text("Mostrar aba de Doações")
        }
        
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            // Placeholder save
        }) {
            Text("Salvar Configurações")
        }
        
        Spacer(Modifier.height(16.dp))
        val context = androidx.compose.ui.platform.LocalContext.current
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
    var banners by remember { mutableStateOf(homeBannersState.toList()) }
    var newUrl by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            isUploading = true
            scope.launch {
                uploadProgress = 0f
                val uploadedUrl = StorageManager.uploadFile(context, uri, "banners") { progress ->
                    uploadProgress = progress
                }
                if (uploadedUrl.isNotEmpty()) {
                    banners = banners + uploadedUrl
                }
                isUploading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Banners (Destaques)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Adicione até 5 imagens no formato 16:9 para a tela inicial.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(banners) { index, url ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        coil.compose.AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp, 56.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Banner ${index + 1}", modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            banners = banners.toMutableList().apply { removeAt(index) }
                        }) {
                            Icon(androidx.compose.material.icons.Icons.Default.Delete, contentDescription = "Remover", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = newUrl,
                onValueChange = { newUrl = it },
                label = { Text("URL da Imagem") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (newUrl.isNotBlank()) {
                            banners = banners + convertGoogleDriveUrl(newUrl)
                            newUrl = ""
                        }
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
        
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                saving = true
                saveBannersToFirestore(banners)
                saving = false
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving && !isUploading
        ) {
            Text("Salvar Alterações")
        }
        Spacer(Modifier.height(32.dp))
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
                uploadProgress = 0f
                val uploadedUrl = StorageManager.uploadFile(context, uri, "donations") { progress ->
                    uploadProgress = progress
                }
                if (uploadedUrl.isNotEmpty()) {
                    qrCodeUrl = uploadedUrl
                }
                isUploading = false
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
