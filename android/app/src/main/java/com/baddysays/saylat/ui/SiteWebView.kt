package com.baddysays.saylat.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.baddysays.saylat.util.SaylatUserAgents

/**
 * Полная страница сайта: прямой URL + современный Chrome UA.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SiteWebView(
    pageUrl: String,
    modifier: Modifier = Modifier,
    onExternalUrl: (String) -> Unit = {},
    onLoadingChange: (Boolean) -> Unit = {},
) {
    val startUrl = remember(pageUrl) { resolveWebViewUrl(pageUrl) }
    val onLoading by rememberUpdatedState(onLoadingChange)
    val onExternal by rememberUpdatedState(onExternalUrl)
    DisposableEffect(startUrl) {
        onLoading(true)
        onDispose { onLoading(false) }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = true
                }
                settings.userAgentString = SaylatUserAgents.forUrl(startUrl)
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view?.post { onLoading(true) }
                        url?.let { applyUserAgent(view, it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.post { onLoading(false) }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            view?.post { onLoading(false) }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?,
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        view?.post { onLoading(false) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val target = request?.url?.toString().orEmpty()
                        if (target.isBlank()) return false
                        if (target.startsWith("mailto:") || target.startsWith("tel:")) {
                            view?.post { onExternal(target) }
                            return true
                        }
                        if (target.startsWith("http://") || target.startsWith("https://")) {
                            val next = resolveWebViewUrl(target)
                            applyUserAgent(view, next)
                            view?.post { view.loadUrl(next) }
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
        onRelease = { webView ->
            webView.stopLoading()
            webView.webViewClient = WebViewClient()
            webView.destroy()
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
