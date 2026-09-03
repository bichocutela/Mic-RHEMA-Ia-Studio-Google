from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Trecho esperado não encontrado em {path}: {old[:160]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# 1) Player compartilhado: mantém compatibilidade com Mídia e passa a ser oficialmente reutilizável.
player = "app/src/main/java/com/aistudio/micrhema/CleanVideoPlayer.kt"
replace_once(
    player,
    """ * Player único da aba Mídia.\n *\n * YouTube usa exclusivamente o player oficial da biblioteca, sem overlays de toque\n * personalizados sobre a WebView. Isso evita que os controles nativos sejam bloqueados.\n * MP4/links diretos continuam usando ExoPlayer com os controles nativos do PlayerView.\n */""",
    """ * Player de vídeo reutilizável oficial do aplicativo.\n *\n * Pode ser usado por Mídia, IBR e futuras telas sem duplicar implementação.\n * YouTube usa a biblioteca Android YouTube Player; MP4/links diretos usam ExoPlayer.\n * Links completos, Shorts, Live, youtu.be e IDs puros do YouTube são aceitos.\n */""",
)
replace_once(
    player,
    """    onClose: (() -> Unit)? = null,\n    canDownload: Boolean = false,\n    onDownload: (() -> Unit)? = null\n) {""",
    """    onClose: (() -> Unit)? = null,\n    canDownload: Boolean = false,\n    onDownload: (() -> Unit)? = null,\n    showTitleBar: Boolean = true,\n    showExternalButton: Boolean = true\n) {""",
)
replace_once(
    player,
    """    val youtubeId = remember(videoUrl) { extractYouTubeVideoId(videoUrl) }\n\n    var errorMessage""",
    """    val youtubeId = remember(videoUrl) { extractYouTubeVideoId(videoUrl) }\n    val externalVideoUrl = remember(videoUrl, youtubeId, isYouTube) {\n        if (isYouTube && youtubeId != null) \"https://www.youtube.com/watch?v=$youtubeId\" else videoUrl.trim()\n    }\n\n    var errorMessage""",
)
replace_once(
    player,
    """        Row(\n            modifier = Modifier\n                .fillMaxWidth()\n                .padding(horizontal = 8.dp, vertical = 6.dp),\n            verticalAlignment = Alignment.CenterVertically,\n            horizontalArrangement = Arrangement.spacedBy(8.dp)\n        ) {\n            Text(\n                text = title,\n                modifier = Modifier.weight(1f),\n                style = MaterialTheme.typography.titleSmall,\n                fontWeight = FontWeight.SemiBold,\n                maxLines = 1,\n                overflow = TextOverflow.Ellipsis\n            )\n\n            if (!isYouTube && canDownload && onDownload != null) {\n                IconButton(onClick = onDownload) {\n                    Icon(Icons.Default.Download, contentDescription = \"Baixar vídeo\")\n                }\n            }\n\n            if (onClose != null) {\n                IconButton(onClick = onClose) {\n                    Icon(Icons.Default.Close, contentDescription = \"Fechar player\")\n                }\n            }\n        }""",
    """        if (showTitleBar || (!isYouTube && canDownload && onDownload != null) || onClose != null) {\n            Row(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .padding(horizontal = 8.dp, vertical = 6.dp),\n                verticalAlignment = Alignment.CenterVertically,\n                horizontalArrangement = Arrangement.spacedBy(8.dp)\n            ) {\n                if (showTitleBar) {\n                    Text(\n                        text = title,\n                        modifier = Modifier.weight(1f),\n                        style = MaterialTheme.typography.titleSmall,\n                        fontWeight = FontWeight.SemiBold,\n                        maxLines = 1,\n                        overflow = TextOverflow.Ellipsis\n                    )\n                } else {\n                    Spacer(modifier = Modifier.weight(1f))\n                }\n\n                if (!isYouTube && canDownload && onDownload != null) {\n                    IconButton(onClick = onDownload) {\n                        Icon(Icons.Default.Download, contentDescription = \"Baixar vídeo\")\n                    }\n                }\n\n                if (onClose != null) {\n                    IconButton(onClick = onClose) {\n                        Icon(Icons.Default.Close, contentDescription = \"Fechar player\")\n                    }\n                }\n            }\n        }""",
)
replace_once(
    player,
    """                        onOpenExternal = if (videoUrl.startsWith(\"http\", ignoreCase = true)) {\n                            {\n                                runCatching {\n                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))\n                                }\n                            }\n                        } else null""",
    """                        onOpenExternal = if (externalVideoUrl.startsWith(\"http\", ignoreCase = true)) {\n                            {\n                                runCatching {\n                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalVideoUrl)))\n                                }\n                            }\n                        } else null""",
)
replace_once(
    player,
    """        if (isYouTube) {\n            Spacer(Modifier.height(8.dp))\n            Button(\n                onClick = {\n                    runCatching {\n                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))\n                    }\n                },""",
    """        if (isYouTube && showExternalButton) {\n            Spacer(Modifier.height(8.dp))\n            Button(\n                onClick = {\n                    runCatching {\n                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(externalVideoUrl)))\n                    }\n                },""",
)

# 2) IBR: troca o quadro fictício pelo player real e remove o Intent como ação principal.
ibr = "app/src/main/java/com/aistudio/micrhema/IbrScreen.kt"
replace_once(
    ibr,
    """            Box(\n                modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer),\n                contentAlignment = Alignment.Center\n            ) {\n                Icon(typeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(74.dp))\n            }""",
    """            if (chapter.type == \"VIDEO\") {\n                val videoSource = chapter.videoUrl.ifBlank { chapter.youtubeId }\n                CleanVideoPlayer(\n                    videoUrl = videoSource,\n                    title = chapter.title,\n                    modifier = Modifier.fillMaxWidth(),\n                    showTitleBar = false,\n                    showExternalButton = true\n                )\n            } else {\n                Box(\n                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primaryContainer),\n                    contentAlignment = Alignment.Center\n                ) {\n                    Icon(typeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(74.dp))\n                }\n            }""",
)
replace_once(
    ibr,
    """            if (chapter.type == \"VIDEO\") {\n                Button(\n                    onClick = {\n                        val url = if (chapter.isYoutube && chapter.videoUrl.isNotEmpty() && !chapter.videoUrl.startsWith(\"http\")) \"https://www.youtube.com/watch?v=${chapter.videoUrl}\" else chapter.videoUrl\n                        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }\n                        catch (_: Exception) { android.widget.Toast.makeText(context, \"Não foi possível abrir o vídeo.\", android.widget.Toast.LENGTH_SHORT).show() }\n                    },\n                    modifier = Modifier.fillMaxWidth(),\n                    shape = RoundedCornerShape(12.dp)\n                ) {\n                    Icon(Icons.Default.PlayArrow, contentDescription = null)\n                    Spacer(Modifier.width(8.dp))\n                    Text(\"Abrir vídeo\")\n                }\n            } else {\n                OutlinedButton(""",
    """            if (chapter.type != \"VIDEO\") {\n                OutlinedButton(""",
)

# 3) ADM IBR: detecta YouTube automaticamente e guarda também o ID normalizado.
admin = "app/src/main/java/com/aistudio/micrhema/VipAdmin.kt"
replace_once(
    admin,
    """                                    val duration = chapterDuration.toIntOrNull() ?: 30\n                                    val newChapter = IbrChapter(""",
    """                                    val duration = chapterDuration.toIntOrNull() ?: 30\n                                    val detectedYoutubeId = extractYouTubeVideoId(videoUrl).orEmpty()\n                                    val detectedYoutube = isYoutube || detectedYoutubeId.isNotBlank() || isYoutubeUrl(videoUrl)\n                                    val newChapter = IbrChapter(""",
)
replace_once(
    admin,
    """                                        textContent = textContent,\n                                        isYoutube = isYoutube\n                                    )""",
    """                                        textContent = textContent,\n                                        isYoutube = detectedYoutube,\n                                        youtubeId = detectedYoutubeId\n                                    )""",
)

print("Player reutilizável e integração IBR aplicados com sucesso.")
