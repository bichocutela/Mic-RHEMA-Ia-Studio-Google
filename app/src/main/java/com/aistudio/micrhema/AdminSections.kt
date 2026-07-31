package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
fun EditDevotionalsSection() {
    var title by remember { mutableStateOf("") }
    var verse by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<Devotional?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Devocionais", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (editingItem == null) "Adicionar Devocional" else "Editar Devocional", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = verse, onValueChange = { verse = it }, label = { Text("Versículo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (Ex: 2026-07-20)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Conteúdo") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                
                Button(
                    onClick = {
                        if (editingItem != null) {
                            val idx = devotionalsState.indexOfFirst { it.id == editingItem!!.id }
                            if (idx != -1) {
                                devotionalsState[idx] = Devotional(editingItem!!.id, title, content, date, verse)
                                addDevotional(devotionalsState[idx])
                            }
                            editingItem = null
                        } else {
                            val newItem = Devotional(UUID.randomUUID().toString(), title, content, date, verse)
                            devotionalsState.add(0, newItem)
                            addDevotional(newItem)
                        }
                        title = ""; verse = ""; date = ""; content = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingItem == null) "Adicionar" else "Salvar Alterações")
                }
                
                if (editingItem != null) {
                    TextButton(onClick = { 
                         editingItem = null
                         title = ""; verse = ""; date = ""; content = ""
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar")
                    }
                }
            }
        }
        LazyColumn {
            items(devotionalsState) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text(item.date)
                        }
                        Row {
                            IconButton(onClick = { 
                                editingItem = item
                                title = item.title
                                verse = item.verse
                                date = item.date
                                content = item.content
                            }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { 
                                devotionalsState.remove(item)
                                removeDevotional(item)
                            }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditServicesSection() {
    var day by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<ChurchService?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Serviços", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (editingItem == null) "Adicionar Serviço" else "Editar Serviço", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Dia (Ex: Domingo)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Horário (Ex: 18:00)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = {
                        if (editingItem != null) {
                            val idx = weeklyServicesState.indexOfFirst { it.id == editingItem!!.id }
                            if (idx != -1) {
                                weeklyServicesState[idx] = ChurchService(editingItem!!.id, day, time, description)
                                addChurchService(weeklyServicesState[idx])
                            }
                            editingItem = null
                        } else {
                            val newItem = ChurchService(UUID.randomUUID().toString(), day, time, description)
                            weeklyServicesState.add(newItem)
                            addChurchService(newItem)
                        }
                        day = ""; time = ""; description = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingItem == null) "Adicionar" else "Salvar Alterações")
                }
                if (editingItem != null) {
                    TextButton(onClick = { 
                         editingItem = null
                         day = ""; time = ""; description = ""
                    }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                }
            }
        }
        LazyColumn {
            items(weeklyServicesState) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.day, fontWeight = FontWeight.Bold)
                            Text(item.time)
                        }
                        Row {
                            IconButton(onClick = { 
                                editingItem = item
                                day = item.day
                                time = item.time
                                description = item.description
                            }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { 
                                weeklyServicesState.remove(item)
                                removeChurchService(item)
                            }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditIbrSection() {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<IbrCourse?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Cursos IBR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (editingItem == null) "Adicionar Curso" else "Editar Curso", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = theme, onValueChange = { theme = it }, label = { Text("Tema") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = {
                        if (editingItem != null) {
                            val idx = ibrCoursesState.indexOfFirst { it.id == editingItem!!.id }
                            if (idx != -1) {
                                ibrCoursesState[idx] = editingItem!!.copy(title = title, description = description, theme = theme)
                                addIbrCourse(ibrCoursesState[idx])
                            }
                            editingItem = null
                        } else {
                            val newItem = IbrCourse(id = UUID.randomUUID().toString(), title = title, theme = theme, description = description, imageUrl = "", chapters = emptyList())
                            ibrCoursesState.add(newItem)
                            addIbrCourse(newItem)
                        }
                        title = ""; description = ""; theme = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingItem == null) "Adicionar" else "Salvar Alterações")
                }
                if (editingItem != null) {
                    TextButton(onClick = { 
                         editingItem = null
                         title = ""; description = ""; theme = ""
                    }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                }
            }
        }
        LazyColumn {
            items(ibrCoursesState) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text(item.theme)
                        }
                        Row {
                            IconButton(onClick = { 
                                editingItem = item
                                title = item.title
                                description = item.description
                                theme = item.theme
                            }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { 
                                ibrCoursesState.remove(item)
                                removeIbrCourse(item)
                            }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditMembersSection() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Membros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(memberRequestsState) { member ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(member.name, fontWeight = FontWeight.Bold)
                        Text(member.email, style = MaterialTheme.typography.bodySmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Aprovado")
                            Switch(checked = member.isApproved, onCheckedChange = { 
                                val idx = memberRequestsState.indexOfFirst { it.id == member.id }
                                if (idx != -1) {
                                    val updated = memberRequestsState[idx].copy(isApproved = it)
                                    memberRequestsState[idx] = updated
                                    MemberManager.saveToFirestore(updated)
                                }
 })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("VIP")
                            Switch(checked = member.isVip, onCheckedChange = { 
                                val idx = memberRequestsState.indexOfFirst { it.id == member.id }
                                if (idx != -1) {
                                    val updated = memberRequestsState[idx].copy(isVip = it)
                                    memberRequestsState[idx] = updated
                                    MemberManager.saveToFirestore(updated)
                                }
 })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("IBR")
                            Switch(checked = member.isIbr, onCheckedChange = { 
                                val idx = memberRequestsState.indexOfFirst { it.id == member.id }
                                if (idx != -1) {
                                    val updated = memberRequestsState[idx].copy(isIbr = it)
                                    memberRequestsState[idx] = updated
                                    MemberManager.saveToFirestore(updated)
                                }
 })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditPrayersSection() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Pedidos de Oração", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(prayerRequestsState) { req ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(req.name, fontWeight = FontWeight.Bold)
                            Text(req.request)
                            Text(req.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { 
                            prayerRequestsState.remove(req)
                            removePrayerRequest(req)
                        }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
fun EditNoticesSection() {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<ChurchEvent?>(null) }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Eventos / Avisos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (editingItem == null) "Adicionar Evento" else "Editar Evento", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                
                Button(
                    onClick = {
                        if (editingItem != null) {
                            val idx = eventsState.indexOfFirst { it.id == editingItem!!.id }
                            if (idx != -1) {
                                eventsState[idx] = ChurchEvent(editingItem!!.id, title, date, description)
                                addChurchEvent(eventsState[idx])
                            }
                            editingItem = null
                        } else {
                            val newItem = ChurchEvent(UUID.randomUUID().toString(), title, date, description)
                            eventsState.add(newItem)
                            addChurchEvent(newItem)
                        }
                        title = ""; date = ""; description = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingItem == null) "Adicionar" else "Salvar Alterações")
                }
                if (editingItem != null) {
                    TextButton(onClick = { 
                         editingItem = null
                         title = ""; date = ""; description = ""
                    }, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
                }
            }
        }
        LazyColumn {
            items(eventsState) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text(item.date)
                        }
                        Row {
                            IconButton(onClick = { 
                                editingItem = item
                                title = item.title
                                date = item.date
                                description = item.description
                            }) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary) }
                            IconButton(onClick = { 
                                eventsState.remove(item)
                                removeChurchEvent(item)
                            }) { Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }
}
