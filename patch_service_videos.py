import re

with open('app/src/main/java/com/aistudio/micrhema/ServiceVideos.kt', 'r') as f:
    content = f.read()

target = """            val exoPlayer = remember {
                ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(selectedVideo!!.videoUrl))
                    prepare()
                    playWhenReady = true
                }
            }
            
            DisposableEffect(selectedVideo) {
                onDispose {
                    exoPlayer.release()
                }
            }
            
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .clip(RoundedCornerShape(16.dp))
            )"""

replacement = """            val isYouTube = selectedVideo!!.videoUrl.contains("youtube.com") || selectedVideo!!.videoUrl.contains("youtu.be")
            
            if (isYouTube) {
                val embedUrl = remember(selectedVideo!!.videoUrl) {
                    val url = selectedVideo!!.videoUrl
                    if (url.contains("youtube.com/embed/")) url
                    else {
                        val videoId = when {
                            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
                            url.contains("shorts/") -> url.substringAfter("shorts/").substringBefore("?")
                            else -> ""
                        }
                        if (videoId.isNotEmpty()) "https://www.youtube.com/embed/$videoId?autoplay=1" else url
                    }
                }
                
                AndroidView(
                    factory = { ctx ->
                        android.webkit.WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            webChromeClient = android.webkit.WebChromeClient()
                            webViewClient = android.webkit.WebViewClient()
                            loadUrl(embedUrl)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f/9f)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                val exoPlayer = remember(selectedVideo!!.videoUrl) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(selectedVideo!!.videoUrl))
                        prepare()
                        playWhenReady = true
                    }
                }
                
                DisposableEffect(selectedVideo!!.videoUrl) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
                
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f/9f)
                        .clip(RoundedCornerShape(16.dp))
                )
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/aistudio/micrhema/ServiceVideos.kt', 'w') as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
