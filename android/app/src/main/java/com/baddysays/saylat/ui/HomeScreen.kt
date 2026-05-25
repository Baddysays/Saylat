package com.baddysays.saylat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.cache.PageCache
import com.baddysays.saylat.network.NetworkFormat
import com.baddysays.saylat.network.NetworkTestResult
import com.baddysays.saylat.search.SearchEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class QuickLink(val label: String, val url: String)

private val quickLinks = listOf(
    QuickLink("Википедия", "https://ru.wikipedia.org/wiki/Интернет"),
    QuickLink("Пример", "https://example.com"),
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    searchEngine: SearchEngine,
    recentSearches: List<String>,
    serverUrl: String,
    networkTesting: Boolean,
    networkTestResult: NetworkTestResult?,
    smartLayoutAvailable: Boolean,
    layoutLabLoading: Boolean,
    onQuickLink: (String) -> Unit,
    onRecent: (String) -> Unit,
    onRunNetworkTest: () -> Unit,
    onOpenLayoutLab: () -> Unit,
    connectStatus: com.baddysays.saylat.data.ConnectStatus? = null,
    onService: (String) -> Unit = {},
    onOpenServiceSettings: () -> Unit = {},
    slowNetworkMode: Boolean = false,
    offlineCache: List<PageCache.CachedEntry> = emptyList(),
    onOpenCached: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pad = Modifier.padding(horizontal = 16.dp)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { HomeHero(networkTestResult) }
        item {
            Row(
                pad.then(Modifier.fillMaxWidth()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeActionTile(
                    title = "Читалка",
                    subtitle = "Страницы через прокси",
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(22.dp)) },
                    onClick = { onQuickLink("https://ru.wikipedia.org/wiki/Интернет") },
                    modifier = Modifier.weight(1f),
                )
                HomeActionTile(
                    title = "Умная вёрстка",
                    subtitle = "Тест структуры",
                    icon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(22.dp)) },
                    onClick = onOpenLayoutLab,
                    loading = layoutLabLoading,
                    modifier = Modifier.weight(1f),
                    emphasized = true,
                )
            }
        }
        item {
            NetworkTestCard(
                modifier = pad.fillMaxWidth(),
                serverUrl = serverUrl,
                testing = networkTesting,
                result = networkTestResult,
                onRunTest = onRunNetworkTest,
                slowNetworkMode = slowNetworkMode,
            )
        }
        item {
            ServiceQuickAccessBlock(
                modifier = pad.fillMaxWidth(),
                status = connectStatus,
                onService = onService,
                onOpenServiceSettings = onOpenServiceSettings,
            )
        }
        item {
            Surface(
                modifier = pad.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Default.Speed, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text("Поиск", fontWeight = FontWeight.SemiBold)
                        Text(searchEngine.label, color = MaterialTheme.colorScheme.primary)
                        Text(
                            searchEngine.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }
        item {
            Column(pad.then(Modifier.fillMaxWidth()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Быстрые ссылки", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickLinks.forEach { link ->
                        Surface(
                            onClick = { onQuickLink(link.url) },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        ) {
                            Text(link.label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }
        if (offlineCache.isNotEmpty()) {
            item {
                Text("Офлайн-кэш", modifier = pad, fontWeight = FontWeight.SemiBold)
            }
            items(offlineCache.take(8), key = { it.url }) { entry ->
                OfflineCacheRow(
                    entry = entry,
                    modifier = pad.fillMaxWidth(),
                    onClick = { onOpenCached(entry.url) },
                )
            }
        }
        if (recentSearches.isNotEmpty()) {
            item {
                Text("Недавние", modifier = pad, fontWeight = FontWeight.SemiBold)
            }
            items(recentSearches.take(5), key = { it }) { q ->
                Surface(
                    onClick = { onRecent(q) },
                    modifier = pad.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                        Text(q, Modifier.padding(start = 10.dp).weight(1f), maxLines = 1)
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                    }
                }
            }
        }
        if (!smartLayoutAvailable) {
            item {
                Text(
                    "Умная вёрстка недоступна на этом устройстве (мало RAM). Базовая лента работает.",
                    modifier = pad,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHero(networkResult: NetworkTestResult?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("S", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text("Saylat", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "легче салата",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                "Сервер сжимает страницу; на телефоне — карточки, визуальная копия или WebView через прокси.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            networkResult?.takeIf { it.ok }?.profile?.let { profile ->
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip(profile.title)
                    networkResult.latencyMs?.let { HeroChip("отклик ${NetworkFormat.latency(it)}") }
                    networkResult.downloadKbps?.let { HeroChip(NetworkFormat.speedKbps(it)) }
                }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfflineCacheRow(
    entry: PageCache.CachedEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kinds = buildList {
        if (entry.hasArticle) add("текст")
        if (entry.hasStrips) add("полосы")
    }.joinToString(" · ")
    val whenText = remember(entry.savedAt) {
        SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(entry.savedAt))
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.OfflinePin, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title.ifBlank { entry.url },
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    "$whenText · $kinds",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeActionTile(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    emphasized: Boolean = false,
) {
    Card(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.height(108.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            icon()
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
            }
        }
    }
}
