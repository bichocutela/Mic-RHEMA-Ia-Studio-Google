package com.aistudio.micrhema

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                Icon(Icons.Default.Add, contentDescription = "Adicionar Plano")
                Spacer(Modifier.width(4.dp))
                Text("Novo Plano")
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        val newPlan = PlanCategory(name, androidx.compose.ui.graphics.Color.Gray, currentThemes.toList())
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
                        val newTheme = PlanTheme(tTitle, tContent, listOf(tVerse), tImage)
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
                        IconButton(onClick = { removeChurchService(service); weeklyServicesState.remove(service) }) {
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
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editingService == null) "Novo Culto" else "Editar Culto") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título (ex: Culto da Família)") })
                    OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Dia (ex: Domingo)") })
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Horário (ex: 18:30)") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotEmpty()) {
                        val newService = ChurchService(editingService?.id ?: java.util.UUID.randomUUID().toString(), title, time, day)
                        if (editingService != null) {
                            val idx = weeklyServicesState.indexOf(editingService)
                            if (idx >= 0) weeklyServicesState[idx] = newService
                        } else {
                            weeklyServicesState.add(newService)
                        }
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
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotEmpty()) {
                        val newDev = Devotional(editingDevotional?.id ?: java.util.UUID.randomUUID().toString(), title, date, verse, ref, content, editingDevotional?.likes ?: 0)
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
    }
}

