package com.aistudio.micrhema

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
fun EditMembersSection() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Gerenciar Membros (VIP/IBR)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(memberRequestsState) { member ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(member.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(member.email, style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = member.isApproved,
                                onCheckedChange = { 
                                    member.isApproved = it 
                                    MemberManager.saveToFirestore(member,
                                        onSuccess = { Toast.makeText(context, if(it) "Acesso aprovado para ${member.name}" else "Acesso removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                        onFailure = { Toast.makeText(context, "Erro ao atualizar acesso", Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            )
                            Text("Aprovado")
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Checkbox(
                                checked = member.isVip,
                                onCheckedChange = { 
                                    member.isVip = it 
                                    MemberManager.saveToFirestore(member,
                                        onSuccess = { Toast.makeText(context, if(it) "Acesso VIP aprovado para ${member.name}" else "Acesso VIP removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                        onFailure = { Toast.makeText(context, "Erro ao atualizar acesso", Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            )
                            Text("VIP")
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Checkbox(
                                checked = member.isIbr,
                                onCheckedChange = { 
                                    member.isIbr = it 
                                    MemberManager.saveToFirestore(member,
                                        onSuccess = { Toast.makeText(context, if(it) "Acesso IBR aprovado para ${member.name}" else "Acesso IBR removido para ${member.name}", Toast.LENGTH_SHORT).show() },
                                        onFailure = { Toast.makeText(context, "Erro ao atualizar acesso", Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            )
                            Text("IBR")
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { 
                                MemberManager.deleteFromFirestore(member)
                                memberRequestsState.remove(member)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
