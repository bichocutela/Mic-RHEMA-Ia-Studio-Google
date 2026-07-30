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










@Composable fun AdminScreen() { 
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Início", "Devocionais", "Serviços", "Conteúdo", "Cursos IBR", "VIP", "Membros", "Orações", "Avisos", "Abas")
    
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title) })
            }
        }
        when (selectedTabIndex) {
            0 -> EditHomeSection()
            3 -> EditContentSection()
            5 -> EditVipSection()
            9 -> AdminTabsScreen()
            else -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Seção em desenvolvimento") }
        }
    }
}

@Composable
fun EditHomeSection() {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("EVENTO") }
    var imageUrl by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<CarouselItem?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Destaques Ativos no Carrossel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (editingItem == null) "Adicionar Destaque" else "Editar Destaque", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Data (Ex: 2026-07-20)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Adicionar Imagem do Evento (Recomendado: 16:9)") }, modifier = Modifier.fillMaxWidth())
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Tipo: ")
                    RadioButton(selected = tag == "EVENTO", onClick = { tag = "EVENTO" })
                    Text("EVENTO")
                    Spacer(modifier = Modifier.width(8.dp))
                    RadioButton(selected = tag == "NOTÍCIA", onClick = { tag = "NOTÍCIA" })
                    Text("NOTÍCIA")
                }

                Button(
                    onClick = {
                        if (editingItem != null) {
                            val idx = carouselItemsState.indexOfFirst { it.id == editingItem!!.id }
                            if (idx != -1) {
                                carouselItemsState[idx] = CarouselItem(editingItem!!.id, title, description, date, tag, imageUrl.takeIf { it.isNotBlank() })
                            }
                            editingItem = null
                        } else {
                            addCarouselItem(CarouselItem(UUID.randomUUID().toString(), title, description, date, tag, imageUrl.takeIf { it.isNotBlank() }))
                        }
                        title = ""; description = ""; date = ""; imageUrl = ""; tag = "EVENTO"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editingItem == null) "Adicionar" else "Salvar Alterações")
                }
                
                if (editingItem != null) {
                    TextButton(onClick = { 
                        editingItem = null
                        title = ""; description = ""; date = ""; imageUrl = ""; tag = "EVENTO"
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancelar")
                    }
                }
            }
        }

        LazyColumn {
            items(carouselItemsState) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text("Tipo: ${item.tag}")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Botão de Editar
                            IconButton(onClick = { 
                                editingItem = item
                                title = item.title
                                description = item.description
                                date = item.date
                                tag = item.tag
                                imageUrl = item.imageUrl ?: ""
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // Botão de Excluir existente
                            IconButton(onClick = { removeCarouselItem(item) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
