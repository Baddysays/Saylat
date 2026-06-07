package com.baddysays.saylat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.request.ImageRequest
import com.baddysays.saylat.data.StripPage
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.ui.strings.SaylatStrings

@Composable
fun StripReaderScreen(
    loading: Boolean,
    stripPage: StripPage?,
    pageUrl: String,
    fromCache: Boolean,
    tamagotchiEnabled: Boolean = false,
    uiLanguage: AppLanguage = AppLanguage.RU,
    saveInProgress: Boolean,
    saveMessage: String?,
    onSaveStrips: () -> Unit,
    onSwitchToReader: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> {
            if (tamagotchiEnabled) {
                Box(modifier.fillMaxSize())
            } else {
                Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            SaylatStrings.readerLoadingStrips(uiLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        stripPage != null -> {
            val context = LocalContext.current
            Column(modifier = modifier.fillMaxSize()) {
                StripBrowserChrome(
                    url = pageUrl.ifBlank { stripPage.url },
                    title = stripPage.title,
                    fromCache = fromCache,
                    stripCount = stripPage.strips.size,
                    payloadKb = stripPage.stats.payload_bytes / 1024,
                    engine = stripPage.render_engine,
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = onSaveStrips,
                                enabled = !saveInProgress,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null)
                                Text(
                                    if (saveInProgress) "Сохраняем…" else "Копия в галерею",
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            saveMessage?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            FilledTonalButton(
                                onClick = onSwitchToReader,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Открыть как текст (с ссылками)")
                            }
                            stripPage.links.forEach { link ->
                                Surface(
                                    onClick = { onOpenLink(link.href) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                ) {
                                    Text(
                                        link.text.ifBlank { link.href },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                    itemsIndexed(stripPage.strips, key = { idx, _ -> "strip-$idx" }) { idx, strip ->
                        val model = remember(strip.src) { strip.src }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 800.dp),
                        ) {
                            ZoomableBox(modifier = Modifier.fillMaxSize()) {
                                SaylatRemoteImage(
                                    model = ImageRequest.Builder(context).data(model).crossfade(false).build(),
                                    contentDescription = "Полоса ${idx + 1}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    contentScale = ContentScale.FillWidth,
                                    placeholderHeight = 200,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StripBrowserChrome(
    url: String,
    title: String,
    fromCache: Boolean,
    stripCount: Int,
    payloadKb: Int,
    engine: String,
) {
    val engineLabel = when (engine) {
        "browser" -> "скриншот"
        "browser_fallback_pillow" -> "упрощённо"
        else -> "текст"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.ifBlank { "Страница" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (fromCache) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.OfflinePin,
                            contentDescription = null,
                            modifier = Modifier.padding(0.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text("кэш", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(
                url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$stripCount полос · $payloadKb КБ · $engineLabel · прокрутка как в браузере",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}
