package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun SaylatRemoteImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillWidth,
    placeholderHeight: Int = 120,
    serverBaseUrl: String = "",
    pageUrl: String? = null,
    proxyRemoteImages: Boolean = false,
) {
    val resolvedModel = androidx.compose.runtime.remember(model, serverBaseUrl, pageUrl, proxyRemoteImages) {
        if (model is String && proxyRemoteImages) {
            com.baddysays.saylat.data.ImageProxyUrl.resolve(serverBaseUrl, model, pageUrl, useProxy = true)
        } else {
            model
        }
    }
    SubcomposeAsyncImage(
        model = resolvedModel,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(placeholderHeight.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(placeholderHeight.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Не удалось загрузить",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        },
    )
}
