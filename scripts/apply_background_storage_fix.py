from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    (ROOT / rel).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        if new in text:
            return text
        raise RuntimeError(f"Bloco não encontrado: {label}")
    return text.replace(old, new, 1)


# 1) NotificationHelper: a UI deixa de recriar workers legados.
rel = "app/src/main/java/com/aistudio/micrhema/NotificationHelper.kt"
text = read(rel)
pattern = re.compile(
    r"    fun applyAdminNotificationPolicy\(context: Context, enabled: Boolean\) \{.*?\n    \}\n\n    fun createNotificationChannel",
    re.S,
)
replacement = '''    fun applyAdminNotificationPolicy(context: Context, enabled: Boolean) {
        context.getSharedPreferences("micrhema_admin_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("notificationsEnabled", enabled)
            .apply()
        BackgroundNotificationCoordinator.reconcile(context.applicationContext, enabled)
    }

    fun createNotificationChannel'''
if "BackgroundNotificationCoordinator.reconcile(context.applicationContext, enabled)" not in text:
    text, count = pattern.subn(replacement, text, count=1)
    if count != 1:
        raise RuntimeError("applyAdminNotificationPolicy não encontrado")
write(rel, text)


# 2) StorageManager: usa a operação delete que já existe no storage-gateway.
rel = "app/src/main/java/com/aistudio/micrhema/StorageManager.kt"
text = read(rel)
if "suspend fun deleteMediaAssetIfSupabase" not in text:
    anchor = "    /** Compatibilidade para telas antigas: todos os uploads gerais agora usam o Supabase. */\n"
    addition = r'''    private fun mediaStorageTarget(reference: String): Pair<String, String>? {
        val raw = reference.trim()
        if (raw.isBlank()) return null

        val objectPath = if (raw.startsWith("$MEDIA_BUCKET/")) {
            raw.removePrefix("$MEDIA_BUCKET/").substringBefore('?')
        } else {
            val configuredHost = runCatching { Uri.parse(BuildConfig.SUPABASE_URL).host }.getOrNull()
            val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
            if (uri.scheme !in setOf("http", "https") || configuredHost.isNullOrBlank() || uri.host != configuredHost) return null
            val path = uri.path.orEmpty()
            val markers = listOf(
                "/storage/v1/object/public/$MEDIA_BUCKET/",
                "/storage/v1/object/sign/$MEDIA_BUCKET/",
                "/storage/v1/object/authenticated/$MEDIA_BUCKET/"
            )
            val marker = markers.firstOrNull { path.contains(it) } ?: return null
            path.substringAfter(marker).substringBefore('?')
        }
        if (objectPath.isBlank() || objectPath.contains("..") || objectPath.startsWith('/')) return null
        val targetUid = objectPath.substringBefore('/', "")
        if (targetUid.isBlank() || '/' !in objectPath) return null
        return targetUid to "$MEDIA_BUCKET/$objectPath"
    }

    /**
     * Remove apenas arquivos pertencentes ao bucket media-assets deste projeto.
     * URLs externas (YouTube, Drive, Unsplash etc.) são ignoradas.
     */
    suspend fun deleteMediaAssetIfSupabase(context: Context, reference: String): Boolean = withContext(Dispatchers.IO) {
        val (targetUid, storagePath) = mediaStorageTarget(reference) ?: return@withContext false
        val payload = JSONObject()
            .put("operation", "delete")
            .put("bucket", MEDIA_BUCKET)
            .put("targetUid", targetUid)
            .put("storagePath", storagePath)
        executeGatewayRequest(context) { token ->
            Request.Builder()
                .url(gatewayUrl())
                .header("Authorization", "Bearer $token")
                .header("apikey", publishableKey())
                .header("X-Rhema-Bucket", MEDIA_BUCKET)
                .header("X-Rhema-Operation", "delete")
                .withAdminAuthorization()
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
        }.use { response -> responseJson(response) }
        true
    }

    suspend fun deleteMediaAssetsFromReferences(context: Context, references: Collection<String>): Int {
        var removed = 0
        references.map { it.trim() }.filter { it.isNotBlank() }.distinct().forEach { reference ->
            if (deleteMediaAssetIfSupabase(context, reference)) removed++
        }
        return removed
    }

'''
    if anchor not in text:
        raise RuntimeError("Âncora do StorageManager não encontrada")
    text = text.replace(anchor, addition + anchor, 1)
write(rel, text)


# 3) Banners: confirmação + exclusão física no Supabase antes do Firestore.
rel = "app/src/main/java/com/aistudio/micrhema/AdminMoreSections.kt"
text = read(rel)
text = replace_once(
    text,
    '''    var showDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<CarouselItem?>(null) }
''',
    '''    var showDialog by remember { mutableStateOf(false) }
    var editingBanner by remember { mutableStateOf<CarouselItem?>(null) }
    var bannerToDelete by remember { mutableStateOf<CarouselItem?>(null) }
    var isDeletingBanner by remember { mutableStateOf(false) }
''',
    "estado de exclusão de banner",
)
text = replace_once(
    text,
    '''                        IconButton(onClick = {
                            removeCarouselItem(banner)
                        }) {
''',
    '''                        IconButton(onClick = { bannerToDelete = banner }) {
''',
    "botão de exclusão de banner",
)
if 'title = { Text("Excluir banner?") }' not in text:
    anchor = "    if (showDialog && editingBanner != null) {\n"
    dialog = '''    if (bannerToDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingBanner) bannerToDelete = null },
            title = { Text("Excluir banner?") },
            text = { Text("O banner será removido do aplicativo e, se a imagem estiver no Supabase do MIC Rhema, o arquivo também será apagado para liberar espaço.") },
            confirmButton = {
                TextButton(
                    enabled = !isDeletingBanner,
                    onClick = {
                        val target = bannerToDelete ?: return@TextButton
                        isDeletingBanner = true
                        scope.launch {
                            try {
                                target.imageUrl?.let { StorageManager.deleteMediaAssetIfSupabase(context, it) }
                                removeCarouselItem(target)
                                android.widget.Toast.makeText(context, "Banner excluído com sucesso.", android.widget.Toast.LENGTH_SHORT).show()
                                bannerToDelete = null
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Não foi possível excluir: ${error.message ?: "erro no armazenamento"}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isDeletingBanner = false
                            }
                        }
                    }
                ) { Text(if (isDeletingBanner) "Excluindo..." else "Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(enabled = !isDeletingBanner, onClick = { bannerToDelete = null }) { Text("Cancelar") }
            }
        )
    }

'''
    if anchor not in text:
        raise RuntimeError("Âncora do diálogo de banner não encontrada")
    text = text.replace(anchor, dialog + anchor, 1)
write(rel, text)


# 4) Mídia comum: confirmação e limpeza real de áudio, vídeo, álbum e foto.
rel = "app/src/main/java/com/aistudio/micrhema/ContentAdmin.kt"
text = read(rel)
text = replace_once(
    text,
    '''                    onEdit = { editingAudio = audio },
                    onDelete = { removeContentAudio(audio) }
''',
    '''                    onEdit = { editingAudio = audio },
                    onDelete = {
                        coroutineScope.launch {
                            try {
                                StorageManager.deleteMediaAssetsFromReferences(context, listOf(audio.audioUrl, audio.coverUrl))
                                removeContentAudio(audio)
                                android.widget.Toast.makeText(context, "Áudio excluído.", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Áudio não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
''',
    "exclusão de áudio comum",
)
text = replace_once(
    text,
    '''                    onEdit = { editingVideo = video },
                    onDelete = { removeContentVideo(video) }
''',
    '''                    onEdit = { editingVideo = video },
                    onDelete = {
                        coroutineScope.launch {
                            try {
                                StorageManager.deleteMediaAssetsFromReferences(context, listOf(video.videoUrl, video.thumbnailUrl))
                                removeContentVideo(video)
                                android.widget.Toast.makeText(context, "Vídeo excluído.", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Vídeo não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
''',
    "exclusão de vídeo comum",
)
text = replace_once(
    text,
    '''                    type = "Álbum",
                    onEdit = { editingAlbum = album },
                    onDelete = { albumToDelete = album }
''',
    '''                    type = "Álbum",
                    onEdit = { editingAlbum = album },
                    onDelete = { albumToDelete = album },
                    confirmDelete = false
''',
    "confirmação única de álbum",
)
old_cleanup = '''                            // Simulate network delay and explicit storage cleanup logic
                            android.util.Log.i("StorageCleanup", "Deleting cover image: ${albumToDelete!!.coverUrl}")
                            albumToDelete!!.photos.forEach { photo ->
                                android.util.Log.i("StorageCleanup", "Deleting photo from storage: ${photo.url}")
                            }
                            kotlinx.coroutines.delay(1500)
                            
                            val idx = contentAlbumsState.indexOfFirst { it.id == albumToDelete!!.id }
                            if (idx != -1) {
                                removeContentPhotoAlbum(albumToDelete!!)
                            }
                            isDeleting = false
                            albumToDelete = null
'''
new_cleanup = '''                            val target = albumToDelete!!
                            try {
                                val refs = buildList {
                                    target.coverUrl?.let { add(it) }
                                    addAll(target.photos.map { it.url })
                                }
                                StorageManager.deleteMediaAssetsFromReferences(context, refs)
                                removeContentPhotoAlbum(target)
                                albumToDelete = null
                                android.widget.Toast.makeText(context, "Álbum e arquivos excluídos.", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Álbum não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isDeleting = false
                            }
'''
text = replace_once(text, old_cleanup, new_cleanup, "limpeza de álbum comum")
text = replace_once(
    text,
    '''        var newPhotoDriveUrl by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
''',
    '''        var newPhotoDriveUrl by remember { mutableStateOf("") }
        var photoToDelete by remember(editingAlbum) { mutableStateOf<AlbumPhoto?>(null) }
        var isDeletingPhoto by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
''',
    "estado de exclusão de foto comum",
)
old_photo_button = '''                                IconButton(onClick = {
                                    val updatedPhotos = editingAlbum!!.photos.filter { it.url != photo.url }
                                    val updatedCover = if (editingAlbum!!.coverUrl == photo.url) updatedPhotos.firstOrNull()?.url else editingAlbum!!.coverUrl
                                    val updatedAlbum = editingAlbum!!.copy(photos = updatedPhotos, coverUrl = updatedCover)
                                    val index = contentAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                    if (index != -1) {
                                        contentAlbumsState[index] = updatedAlbum
                                            addContentPhotoAlbum(updatedAlbum)
                                        editingAlbum = updatedAlbum
                                    }
                                }) {
'''
text = replace_once(text, old_photo_button, '''                                IconButton(onClick = { photoToDelete = photo }) {
''', "botão de foto comum")
if 'title = { Text("Excluir foto?") }' not in text:
    tail = '''        )
    }

}


@Composable
private fun MediaAdminHeader'''
    dialog_tail = '''        )

        if (photoToDelete != null) {
            AlertDialog(
                onDismissRequest = { if (!isDeletingPhoto) photoToDelete = null },
                title = { Text("Excluir foto?") },
                text = { Text("A foto será removida do álbum e do Supabase quando pertencer ao armazenamento do MIC Rhema.") },
                confirmButton = {
                    TextButton(
                        enabled = !isDeletingPhoto,
                        onClick = {
                            val targetPhoto = photoToDelete ?: return@TextButton
                            isDeletingPhoto = true
                            scope.launch {
                                try {
                                    StorageManager.deleteMediaAssetIfSupabase(context, targetPhoto.url)
                                    val updatedPhotos = editingAlbum!!.photos.filter { it.url != targetPhoto.url }
                                    val updatedCover = if (editingAlbum!!.coverUrl == targetPhoto.url) updatedPhotos.firstOrNull()?.url else editingAlbum!!.coverUrl
                                    val updatedAlbum = editingAlbum!!.copy(photos = updatedPhotos, coverUrl = updatedCover)
                                    val index = contentAlbumsState.indexOfFirst { it.id == updatedAlbum.id }
                                    if (index >= 0) contentAlbumsState[index] = updatedAlbum
                                    addContentPhotoAlbum(updatedAlbum)
                                    editingAlbum = updatedAlbum
                                    photoToDelete = null
                                } catch (error: Exception) {
                                    android.widget.Toast.makeText(context, "Foto não excluída: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                                } finally {
                                    isDeletingPhoto = false
                                }
                            }
                        }
                    ) { Text(if (isDeletingPhoto) "Excluindo..." else "Excluir", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(enabled = !isDeletingPhoto, onClick = { photoToDelete = null }) { Text("Cancelar") } }
            )
        }
    }

}


@Composable
private fun MediaAdminHeader'''
    if tail not in text:
        raise RuntimeError("Final do diálogo de álbum comum não encontrado")
    text = text.replace(tail, dialog_tail, 1)

old_row_signature = '''@Composable
private fun MediaContentRow(title: String, subtitle: String, type: String, onEdit: () -> Unit, onDelete: () -> Unit) {
'''
new_row_signature = '''@Composable
private fun MediaContentRow(title: String, subtitle: String, type: String, onEdit: () -> Unit, onDelete: () -> Unit, confirmDelete: Boolean = true) {
    var showDeleteConfirmation by remember(title, type) { mutableStateOf(false) }
'''
text = replace_once(text, old_row_signature, new_row_signature, "assinatura MediaContentRow")
text = replace_once(
    text,
    '''            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir $title", tint = MaterialTheme.colorScheme.error) }
        }
    }
}
''',
    '''            IconButton(onClick = { if (confirmDelete) showDeleteConfirmation = true else onDelete() }) { Icon(Icons.Default.Delete, contentDescription = "Excluir $title", tint = MaterialTheme.colorScheme.error) }
        }
    }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirmar exclusão") },
            text = { Text("Deseja excluir '$title'? Se o arquivo estiver no Supabase do MIC Rhema, ele também será apagado para liberar espaço.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; onDelete() }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancelar") } }
        )
    }
}
''',
    "diálogo MediaContentRow",
)
write(rel, text)


# 5) Área IBR/VIP: mesma política para áudio, vídeo, álbum e fotos.
rel = "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt"
text = read(rel)
text = replace_once(
    text,
    '''    var editingAlbum by remember { mutableStateOf<ContentPhotoAlbum?>(null) }
    var albumToDelete by remember { mutableStateOf<ContentPhotoAlbum?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
''',
    '''    var editingAlbum by remember { mutableStateOf<ContentPhotoAlbum?>(null) }
    var albumToDelete by remember { mutableStateOf<ContentPhotoAlbum?>(null) }
    var audioToDelete by remember { mutableStateOf<ContentAudio?>(null) }
    var videoToDelete by remember { mutableStateOf<ContentVideo?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
''',
    "estados de exclusão VIP",
)
text = replace_once(text, 'IconButton(onClick = { removeVipAudio(audio) })', 'IconButton(onClick = { audioToDelete = audio })', "botão áudio VIP")
text = replace_once(text, 'IconButton(onClick = { removeVipVideo(video) })', 'IconButton(onClick = { videoToDelete = video })', "botão vídeo VIP")
if 'title = { Text("Excluir áudio?") }' not in text:
    anchor = "    if (albumToDelete != null) {\n"
    dialogs = '''    if (audioToDelete != null) {
        AlertDialog(
            onDismissRequest = { audioToDelete = null },
            title = { Text("Excluir áudio?") },
            text = { Text("O áudio será removido do IBR e do Supabase quando estiver armazenado nele.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = audioToDelete ?: return@TextButton
                    coroutineScope.launch {
                        try {
                            StorageManager.deleteMediaAssetsFromReferences(context, listOf(target.audioUrl, target.coverUrl))
                            removeVipAudio(target)
                            audioToDelete = null
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Áudio não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { audioToDelete = null }) { Text("Cancelar") } }
        )
    }

    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Excluir vídeo?") },
            text = { Text("O vídeo será removido do IBR e do Supabase quando estiver armazenado nele.") },
            confirmButton = {
                TextButton(onClick = {
                    val target = videoToDelete ?: return@TextButton
                    coroutineScope.launch {
                        try {
                            StorageManager.deleteMediaAssetsFromReferences(context, listOf(target.videoUrl, target.thumbnailUrl))
                            removeVipVideo(target)
                            videoToDelete = null
                        } catch (error: Exception) {
                            android.widget.Toast.makeText(context, "Vídeo não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { videoToDelete = null }) { Text("Cancelar") } }
        )
    }

'''
    if anchor not in text:
        raise RuntimeError("Âncora de exclusão VIP não encontrada")
    text = text.replace(anchor, dialogs + anchor, 1)
old_vip_cleanup = '''                            // Simulate network delay and explicit storage cleanup logic
                            android.util.Log.i("StorageCleanup", "Deleting cover image: ${albumToDelete!!.coverUrl}")
                            albumToDelete!!.photos.forEach { photo ->
                                android.util.Log.i("StorageCleanup", "Deleting photo from storage: ${photo.url}")
                            }
                            kotlinx.coroutines.delay(1500)
                            
                            val idx = vipAlbumsState.indexOfFirst { it.id == albumToDelete!!.id }
                            if (idx != -1) {
                                removeVipAlbum(albumToDelete!!)
                            }
                            isDeleting = false
                            albumToDelete = null
'''
new_vip_cleanup = '''                            val target = albumToDelete!!
                            try {
                                val refs = buildList {
                                    target.coverUrl?.let { add(it) }
                                    addAll(target.photos.map { it.url })
                                }
                                StorageManager.deleteMediaAssetsFromReferences(context, refs)
                                removeVipAlbum(target)
                                albumToDelete = null
                                android.widget.Toast.makeText(context, "Álbum IBR e arquivos excluídos.", android.widget.Toast.LENGTH_SHORT).show()
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Álbum não excluído: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isDeleting = false
                            }
'''
text = replace_once(text, old_vip_cleanup, new_vip_cleanup, "limpeza álbum VIP")
text = replace_once(
    text,
    '''        var newPhotoDriveUrl by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
''',
    '''        var newPhotoDriveUrl by remember { mutableStateOf("") }
        var photoToDelete by remember(editingAlbum) { mutableStateOf<AlbumPhoto?>(null) }
        var isDeletingPhoto by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
''',
    "estado foto VIP",
)
old_vip_photo = '''                                IconButton(onClick = {
                                    val updatedPhotos = editingAlbum!!.photos.filter { it.url != photo.url }
                                    val updatedCover = if (editingAlbum!!.coverUrl == photo.url) updatedPhotos.firstOrNull()?.url else editingAlbum!!.coverUrl
                                    val updatedAlbum = editingAlbum!!.copy(photos = updatedPhotos, coverUrl = updatedCover)
                                    val index = vipAlbumsState.indexOfFirst { it.id == editingAlbum!!.id }
                                    if (index != -1) {
                                        vipAlbumsState[index] = updatedAlbum
                                            addVipAlbum(updatedAlbum)
                                            addVipAlbum(updatedAlbum)
                                        editingAlbum = updatedAlbum
                                    }
                                }) {
'''
text = replace_once(text, old_vip_photo, '''                                IconButton(onClick = { photoToDelete = photo }) {
''', "botão foto VIP")
if text.count('title = { Text("Excluir foto?") }') < 1:
    raise RuntimeError("Diálogo de foto comum deveria existir apenas em outro arquivo")
# Insere um diálogo de foto específico do VIP no fim do bloco editingAlbum.
if 'title = { Text("Excluir foto do IBR?") }' not in text:
    tail = '''            dismissButton = {
                TextButton(onClick = { editingAlbum = null }) { Text("Cancelar") }
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVipIbrSection'''
    replacement_tail = '''            dismissButton = {
                TextButton(onClick = { editingAlbum = null }) { Text("Cancelar") }
            }
        )

        if (photoToDelete != null) {
            AlertDialog(
                onDismissRequest = { if (!isDeletingPhoto) photoToDelete = null },
                title = { Text("Excluir foto do IBR?") },
                text = { Text("A foto será removida do álbum e do Supabase quando pertencer ao armazenamento do MIC Rhema.") },
                confirmButton = {
                    TextButton(enabled = !isDeletingPhoto, onClick = {
                        val targetPhoto = photoToDelete ?: return@TextButton
                        isDeletingPhoto = true
                        scope.launch {
                            try {
                                StorageManager.deleteMediaAssetIfSupabase(context, targetPhoto.url)
                                val updatedPhotos = editingAlbum!!.photos.filter { it.url != targetPhoto.url }
                                val updatedCover = if (editingAlbum!!.coverUrl == targetPhoto.url) updatedPhotos.firstOrNull()?.url else editingAlbum!!.coverUrl
                                val updatedAlbum = editingAlbum!!.copy(photos = updatedPhotos, coverUrl = updatedCover)
                                val index = vipAlbumsState.indexOfFirst { it.id == updatedAlbum.id }
                                if (index >= 0) vipAlbumsState[index] = updatedAlbum
                                addVipAlbum(updatedAlbum)
                                editingAlbum = updatedAlbum
                                photoToDelete = null
                            } catch (error: Exception) {
                                android.widget.Toast.makeText(context, "Foto não excluída: ${error.message ?: "erro no Supabase"}", android.widget.Toast.LENGTH_LONG).show()
                            } finally {
                                isDeletingPhoto = false
                            }
                        }
                    }) { Text(if (isDeletingPhoto) "Excluindo..." else "Excluir", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(enabled = !isDeletingPhoto, onClick = { photoToDelete = null }) { Text("Cancelar") } }
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVipIbrSection'''
    if tail not in text:
        raise RuntimeError("Final do diálogo de álbum VIP não encontrado")
    text = text.replace(tail, replacement_tail, 1)
write(rel, text)

print("Patch de notificações/armazenamento aplicado com sucesso.")
