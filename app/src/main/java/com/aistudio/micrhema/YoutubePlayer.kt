package com.aistudio.micrhema

import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YoutubePlayer(videoUrl: String, youtubeId: String = "", modifier: Modifier = Modifier) {
    val embedUrl = remember(videoUrl, youtubeId) {
        val url = videoUrl
        if (url.contains("youtube.com/embed/")) url
        else {
            val videoId = when {
                url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
                url.contains("shorts/") -> url.substringAfter("shorts/").substringBefore("?")
                url.isNotEmpty() && !url.contains("http") -> url
                else -> youtubeId
            }
            if (videoId.isNotEmpty()) "https://www.youtube.com/embed/$videoId?autoplay=1&fs=1&rel=0&modestbranding=1&playsinline=1" else url
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
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        return false
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
                      body { margin: 0; padding: 0; background-color: transparent; overflow: hidden; }
                      iframe { width: 100vw; height: 100vh; border: none; }
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
