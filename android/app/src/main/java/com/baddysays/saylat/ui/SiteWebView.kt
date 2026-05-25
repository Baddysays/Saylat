package com.baddysays.saylat.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.baddysays.saylat.util.SaylatUserAgents

/**
 * Полная страница сайта: прямой URL + современный Chrome UA.
 * (Прокси /api/proxy/page урезает CSS/JS — только для облегчённого HTML, не для WebView.)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SiteWebView(
    pageUrl: String,
    modifier: Modifier = Modifier,
    onExternalUrl: (String) -> Unit = {},
) {
    val startUrl = resolveWebViewUrl(pageUrl)
    val userAgent = SaylatUserAgents.forUrl(startUrl)
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = true
                }
                settings.userAgentString = userAgent
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { applyUserAgent(view, it) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val target = request?.url?.toString().orEmpty()
                        if (target.isBlank()) return false
                        if (target.startsWith("mailto:") || target.startsWith("tel:")) {
                            onExternalUrl(target)
                            return true
                        }
                        if (target.startsWith("http://") || target.startsWith("https://")) {
                            val next = resolveWebViewUrl(target)
                            applyUserAgent(view, next)
                            view?.loadUrl(next)
                            return true
                        }
                        return false
                    }
                }
                loadUrl(startUrl)
            }
        },
        update = { view ->
            val current = resolveWebViewUrl(pageUrl)
            applyUserAgent(view, current)
            val loaded = view.url?.let { resolveWebViewUrl(it) }
            if (loaded != current && view.url != current) {
                view.loadUrl(current)
            }
        },
    )
}

private fun applyUserAgent(view: WebView?, url: String) {
    view ?: return
    val ua = SaylatUserAgents.forUrl(url)
    if (view.settings.userAgentString != ua) {
        view.settings.userAgentString = ua
    }
}

/** Старые сессии могли хранить /api/proxy/page?url=… — разворачиваем в прямой URL. */
private fun resolveWebViewUrl(raw: String): String {
    val marker = "url="
    val idx = raw.indexOf(marker)
    if (raw.contains("/api/proxy/page") && idx >= 0) {
        val extracted = try {
            java.net.URLDecoder.decode(
                raw.substring(idx + marker.length).substringBefore("&"),
                Charsets.UTF_8.name(),
            )
        } catch (_: Exception) {
            null
        }
        if (!extracted.isNullOrBlank()) {
            return SaylatUserAgents.normalizeFetchUrl(extracted)
        }
    }
    return SaylatUserAgents.normalizeFetchUrl(raw)
}
