package com.aistudio.micrhema

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

fun extractYoutubeId(url: String): String? {
    if (url.isBlank()) return null
    val cleanUrl = url.trim()
    return when {
        cleanUrl.contains("v=") -> cleanUrl.substringAfter("v=").substringBefore("&").substringBefore("?").substringBefore("/").substringBefore("#")
        cleanUrl.contains("youtu.be/") -> cleanUrl.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#")
        cleanUrl.contains("youtube.com/shorts/") -> cleanUrl.substringAfter("youtube.com/shorts/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#")
        cleanUrl.contains("youtube.com/live/") -> cleanUrl.substringAfter("youtube.com/live/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#")
        cleanUrl.contains("youtube.com/embed/") -> cleanUrl.substringAfter("youtube.com/embed/").substringBefore("?").substringBefore("&").substringBefore("/").substringBefore("#")
        !cleanUrl.contains("http") && !cleanUrl.contains("/") && cleanUrl.length >= 8 -> cleanUrl
        else -> null
    }
}

fun isYoutubeUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val cleanUrl = url.trim()
    return cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be") || extractYoutubeId(cleanUrl) != null
}

@Composable
fun YoutubePlayer(
    videoUrl: String,
    youtubeId: String = "",
    modifier: Modifier = Modifier,
    onError: ((String) -> Unit)? = null
) {
    val embedUrl = remember(videoUrl, youtubeId) {
        val url = videoUrl.trim()
        val extractedId = extractYoutubeId(url) ?: youtubeId.ifEmpty { null }
        if (extractedId != null) {
            "https://www.youtube.com/embed/$extractedId?autoplay=1&fs=1&rel=0&modestbranding=1&playsinline=1"
        } else if (url.contains("youtube.com/embed/")) {
            url
        } else {
            url
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36"
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val errMessage = "Erro ao carregar vídeo do YouTube: ${error?.description ?: "Sem conexão"}"
                            Log.e("YoutubePlayer", errMessage)
                            onError?.invoke(errMessage)
                        }
                    }
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { webView ->
            val html = """
                <!DOCTYPE html>
                <html>
                  <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                      * { margin: 0; padding: 0; box-sizing: border-box; }
                      body, html { width: 100%; height: 100%; background-color: #000000; overflow: hidden; }
                      iframe { width: 100%; height: 100%; border: none; }
                    </style>
                  </head>
                  <body>
                    <iframe 
                      id="ytplayer"
                      type="text/html"
                      src="$embedUrl" 
                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                      allowfullscreen>
                    </iframe>
                  </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null)
        },
        modifier = modifier.fillMaxSize()
    )
}


