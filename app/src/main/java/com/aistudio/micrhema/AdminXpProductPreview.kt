package com.aistudio.micrhema

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * Prévia administrativa da Loja XP. A intenção é renderizar o mesmo ativo que será
 * entregue ao membro, sem criar resgate, gastar XP ou alterar estoque.
 */
@Composable
fun AdminXpProductPreviewDialog(
    item: AdminXpShopItem,
    testMode: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var resolvedUrl by remember(item.imageUrl) { mutableStateOf("") }
    var resolving by remember(item.imageUrl) { mutableStateOf(false) }
    var error by remember(item.imageUrl) { mutableStateOf("") }
    val assetType = parseXpShopAssetRef(item.imageUrl)?.type
    val member = loggedInMemberState.value

    LaunchedEffect(item.imageUrl) {
        if (item.imageUrl.isBlank()) return@LaunchedEffect
        resolving = true
        error = ""
        runCatching { resolveXpShopAssetUrl(context, item.imageUrl) }
            .onSuccess { resolvedUrl = it }
            .onFailure { error = it.message ?: "Não foi possível carregar o arquivo real da recompensa." }
        resolving = false
    }

    fun openRealAsset() {
        val target = resolvedUrl.ifBlank { item.imageUrl }
        if (target.isBlank()) return
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure { error = "Não foi encontrado um aplicativo compatível para abrir este conteúdo." }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(if (testMode) Icons.Default.Science else Icons.Default.Visibility, contentDescription = null) },
        title = { Text(if (testMode) "Testar produto" else "Prévia do produto") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (testMode)
                        "Modo ADM: usa o conteúdo real da recompensa, sem consumir XP, estoque ou criar compra."
                    else
                        "Esta é a aparência/conteúdo real que o cliente receberá depois da compra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when (item.id) {
                    XpRewardManager.PROMISE_FRAME -> {
                        Text("Moldura Luz da Promessa aplicada ao avatar", fontWeight = FontWeight.SemiBold)
                        if (member != null) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                BiblicalAvatarWithBadge(
                                    avatar = biblicalAvatarForId(member.avatarId),
                                    badge = biblicalBadgeForId(member.equippedBadgeId),
                                    modifier = Modifier.size(190.dp),
                                    previewPromiseFrame = true,
                                    previewReaderBadge = XpRewardManager.isActive(context, XpRewardManager.READER_BADGE)
                                )
                            }
                        } else Text("Entre com um perfil de membro para visualizar a moldura sobre um avatar real.")
                    }
                    XpRewardManager.READER_BADGE -> {
                        Text("Distintivo Leitor da Palavra aplicado ao avatar", fontWeight = FontWeight.SemiBold)
                        if (member != null) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                BiblicalAvatarWithBadge(
                                    avatar = biblicalAvatarForId(member.avatarId),
                                    badge = biblicalBadgeForId(member.equippedBadgeId),
                                    modifier = Modifier.size(190.dp),
                                    previewPromiseFrame = XpRewardManager.isActive(context, XpRewardManager.PROMISE_FRAME),
                                    previewReaderBadge = true
                                )
                            }
                        } else Text("Entre com um perfil de membro para visualizar o distintivo sobre um avatar real.")
                    }
                    XpRewardManager.GOLD_PLUS_THEME -> {
                        Text("Dourado Plus", fontWeight = FontWeight.SemiBold)
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Tema Dourado Plus", fontWeight = FontWeight.Bold)
                                }
                                Text("A prévia usa os mesmos componentes Material do aplicativo. No teste, nenhuma preferência do usuário é salva.")
                                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Botão no tema") }
                            }
                        }
                    }
                    else -> {
                        if (resolving) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Carregando o arquivo real…", style = MaterialTheme.typography.bodySmall)
                        }
                        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                        val isImage = assetType == "image" || assetType == "emblem" ||
                            resolvedUrl.lowercase().let { it.endsWith(".png") || it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".webp") }
                        if (isImage && resolvedUrl.isNotBlank()) {
                            AsyncImage(
                                model = resolvedUrl,
                                contentDescription = item.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp).clip(RoundedCornerShape(16.dp))
                            )
                        } else if (item.imageUrl.isNotBlank()) {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when (assetType) {
                                            "video" -> Icons.Default.VideoFile
                                            "audio" -> Icons.Default.AudioFile
                                            "pdf" -> Icons.Default.PictureAsPdf
                                            "emblem" -> Icons.Default.MilitaryTech
                                            else -> Icons.Default.AttachFile
                                        },
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(9.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(xpShopAssetLabel(item.imageUrl), fontWeight = FontWeight.SemiBold)
                                        Text("Mesmo arquivo entregue ao cliente", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            Button(onClick = { openRealAsset() }, enabled = resolvedUrl.isNotBlank() || !item.imageUrl.startsWith("micrhema-xp://"), modifier = Modifier.fillMaxWidth()) {
                                Icon(if (testMode) Icons.Default.PlayArrow else Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(Modifier.width(7.dp))
                                Text(if (testMode) "Executar conteúdo" else "Abrir conteúdo real")
                            }
                        }

                        if (item.kind == "physical") {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("Produto físico", fontWeight = FontWeight.Bold)
                                    Text("A prévia mostra a foto/arquivo cadastrado e os dados exatos da recompensa. A entrega física continua sendo feita pelo administrador.")
                                    Text(if (item.stock == null) "Estoque: ilimitado" else "Estoque atual: ${item.stock}")
                                }
                            }
                        }
                    }
                }

                if (item.description.isNotBlank()) {
                    HorizontalDivider()
                    Text("Descrição entregue na Loja XP", fontWeight = FontWeight.SemiBold)
                    Text(item.description)
                }
                Text("Preço exibido ao cliente: ${item.cost} XP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}
