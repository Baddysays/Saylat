package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.baddysays.saylat.data.StripPage

@Composable
fun StripReaderScreen(
    loading: Boolean,
    stripPage: StripPage?,
    saveInProgress: Boolean,
    saveMessage: String?,
    onSaveStrips: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Рисуем полосы на сервере…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        stripPage != null -> {
            val context = LocalContext.current
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stripPage.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val engineLabel = when (stripPage.render_engine) {
                            "browser" -> "Скриншот сайта (Playwright)"
                            "browser_fallback_pillow" -> "Скриншот недоступен — упрощённые полосы"
                            else -> "Упрощённые полосы (текст)"
                        }
                        Text(
                            "$engineLabel · ${stripPage.strips.size} полос · " +
                                "${stripPage.stats.payload_bytes / 1024} КБ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(
                            onClick = onSaveStrips,
                            enabled = !saveInProgress,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Text(
                                if (saveInProgress) "Сохраняем…" else "Сохранить полосы в галерею",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        saveMessage?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                itemsIndexed(stripPage.strips, key = { idx, _ -> "strip-$idx" }) { _, strip ->
                    val model = remember(strip.src) { strip.src }
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(model).crossfade(false).build(),
                        contentDescription = "Полоса ${strip.index + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillWidth,
                    )
                }
            }
        }
    }
}
