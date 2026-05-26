package com.baddysays.saylat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.cache.PageCache
import com.baddysays.saylat.util.formatBytes
import com.baddysays.saylat.network.NetworkFormat
import com.baddysays.saylat.network.NetworkTestResult
import com.baddysays.saylat.prefs.SaylatPrefs
import com.baddysays.saylat.search.SearchEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class QuickLink(val label: String, val url: String)

private val quickLinks = listOf(
    QuickLink("Википедия", "https://ru.wikipedia.org/wiki/Интернет"),
    QuickLink("Пикабу", "https://pikabu.ru/"),
    QuickLink("Хабр", "https://habr.com/ru/articles/"),
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    searchEngine: SearchEngine,
    recentSearches: List<String>,
    serverUrl: String,
    serverReady: Boolean? = null,
    serverStatusMessage: String? = null,
    onRefreshServerStatus: () -> Unit = {},
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
    onOpenSearchSettings: () -> Unit = {},
    slowNetworkMode: Boolean = false,
    liteImagesEnabled: Boolean = false,
    onSpeedModeChange: (QuickSpeedMode) -> Unit = {},
    favorites: List<SaylatPrefs.FavoriteLink> = emptyList(),
    onOpenFavorite: (String) -> Unit = {},
    onRemoveFavorite: (String) -> Unit = {},
    onPinFavorite: (SaylatPrefs.FavoriteLink) -> Unit = {},
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
            SpeedModeStrip(
                modifier = pad.fillMaxWidth(),
                selected = when {
                    slowNetworkMode && liteImagesEnabled -> QuickSpeedMode.ECO
                    slowNetworkMode -> QuickSpeedMode.BALANCED
                    else -> QuickSpeedMode.FAST
                },
                onSelect = onSpeedModeChange,
            )
        }
        item {
            HomeServerStatusCard(
                ready = serverReady,
                message = serverStatusMessage,
                checking = serverReady == null,
                onRetry = onRefreshServerStatus,
                modifier = pad,
            )
        }
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
        if (favorites.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title = "Избранное",
                    subtitle = "Сохранённые страницы и ярлыки",
                    modifier = pad,
                )
            }
            items(favorites, key = { it.url }) { favorite ->
                FavoriteRow(
                    favorite = favorite,
                    modifier = pad.fillMaxWidth(),
                    onOpen = { onOpenFavorite(favorite.url) },
                    onRemove = { onRemoveFavorite(favorite.url) },
                    onPin = { onPinFavorite(favorite) },
                )
            }
        } else {
            item {
                EmptyFavoritesCard(
                    modifier = pad.fillMaxWidth(),
                    onOpenSample = { onQuickLink("https://ru.wikipedia.org/wiki/Интернет") },
                )
            }
        }
        item {
            HomeSectionHeader(
                title = "Поиск и прокси",
                subtitle = "Движок, инстанс и сетевые параметры",
                modifier = pad,
            )
        }
        item {
            Surface(
                onClick = onOpenSearchSettings,
                modifier = pad.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    ) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            modifier = Modifier.padding(10.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Поиск и прокси", fontWeight = FontWeight.SemiBold)
                        Text(searchEngine.label, color = MaterialTheme.colorScheme.primary)
                        Text(
                            searchEngine.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                }
            }
        }
        item {
            HomeSectionHeader(
                title = "Быстрые ссылки",
                subtitle = "Готовые точки входа для чтения",
                modifier = pad,
            )
        }
        item {
            Column(pad.then(Modifier.fillMaxWidth()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickLinks.forEach { link ->
                        Surface(
                            onClick = { onQuickLink(link.url) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                        ) {
                            Text(
                                link.label,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
        if (offlineCache.isNotEmpty()) {
            item {
                HomeSectionHeader(
                    title = "Офлайн-кэш",
                    subtitle = "Недавно сохранённые страницы",
                    modifier = pad,
                )
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
                HomeSectionHeader(
                    title = "Недавние",
                    subtitle = "Последние поисковые запросы",
                    modifier = pad,
                )
            }
            items(recentSearches.take(5), key = { it }) { q ->
                Surface(
                    onClick = { onRecent(q) },
                    modifier = pad.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
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

@Composable
private fun HomeSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyFavoritesCard(
    onOpenSample: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpenSample,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.PushPin, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Избранное появится здесь", fontWeight = FontWeight.SemiBold)
                Text(
                    "Откройте страницу и нажмите звезду вверху, чтобы сохранить её и закрепить на рабочем столе.",
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
private fun FavoriteRow(
    favorite: SaylatPrefs.FavoriteLink,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpen,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) {
                Icon(
                    Icons.Default.PushPin,
                    null,
                    Modifier.padding(9.dp).size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(favorite.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    favorite.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onPin) {
                Icon(Icons.Default.PushPin, contentDescription = "Закрепить")
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedModeStrip(
    selected: QuickSpeedMode,
    onSelect: (QuickSpeedMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text("Скорость сети", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(
                "Это влияет на сеть и трафик, но не меняет способ показа страницы",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickSpeedMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        label = { Text("${mode.title} · ${mode.subtitle}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        ),
                    )
                }
            }
            Text(
                selected.summary,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
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
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HeroChip("тонкий браузер для плохой сети")
            Text(
                "Saylat делает чтение сайтов, лент и сервисов легче на медленном интернете.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Сервер сжимает страницу, а на телефоне вы выбираете: лёгкий текст, полосы или страницу как на сайте.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeServerStatusCard(
    ready: Boolean?,
    message: String?,
    checking: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when (ready) {
        true -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        false -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    if (ready == false) {
        Card(
            onClick = onRetry,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = bg),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        ) {
            HomeServerStatusRow(ready, message, checking)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = bg,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        ) {
            HomeServerStatusRow(ready, message, checking)
        }
    }
}

@Composable
private fun HomeServerStatusRow(
    ready: Boolean?,
    message: String?,
    checking: Boolean,
) {
    Row(
        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            when (ready) {
                true -> Icons.Default.Speed
                false -> Icons.Default.OfflinePin
                null -> Icons.Default.Speed
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f)) {
            Text(
                message ?: when (ready) {
                    true -> "Интернет через Saylat готов"
                    false -> "Нет связи с сервером"
                    null -> "Проверяем сервер…"
                },
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (ready == false) {
                Text(
                    "Нажмите, чтобы повторить",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        if (checking) {
            androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
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
                    "$whenText · $kinds · ${formatBytes(entry.bytesOnDisk)}",
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
        modifier = modifier.height(116.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (emphasized) {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    },
                ) {
                    Box(Modifier.padding(10.dp)) { icon() }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}
