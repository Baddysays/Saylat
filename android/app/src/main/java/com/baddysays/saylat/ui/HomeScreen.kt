package com.baddysays.saylat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.data.HistoryEntry
import com.baddysays.saylat.cache.PageCache
import com.baddysays.saylat.network.NetworkTestResult
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.prefs.PetProfile
import com.baddysays.saylat.prefs.SaylatPrefs
import com.baddysays.saylat.ui.strings.SaylatStrings
import com.baddysays.saylat.search.SearchEngine
import com.baddysays.saylat.util.formatBytes
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private sealed class HomeListEntry(val url: String, val title: String) {
    data class Pinned(val favorite: SaylatPrefs.FavoriteLink) : HomeListEntry(favorite.url, favorite.title)
    data class Cached(val entry: PageCache.CachedEntry, val pinned: Boolean) : HomeListEntry(entry.url, entry.title)
}

private fun buildHomeList(
    favorites: List<SaylatPrefs.FavoriteLink>,
    offlineCache: List<PageCache.CachedEntry>,
): List<HomeListEntry> {
    val favUrls = favorites.map { it.url }.toSet()
    val pinned = favorites.map { HomeListEntry.Pinned(it) }
    val cached = offlineCache
        .filter { it.url !in favUrls }
        .sortedByDescending { it.savedAt }
        .map { HomeListEntry.Cached(it, pinned = false) }
    return pinned + cached
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    searchEngine: SearchEngine,
    petProfile: PetProfile = PetProfile(),
    tamagotchiEnabled: Boolean = true,
    onPetCare: () -> Unit = {},
    onOpenPetShop: () -> Unit = {},
    onHatchEgg: () -> Unit = {},
    recentSearches: List<String>,
    serverUrl: String,
    serverReady: Boolean? = null,
    serverStatusMessage: String? = null,
    onRefreshServerStatus: () -> Unit = {},
    networkTesting: Boolean,
    networkTestResult: NetworkTestResult?,
    smartLayoutAvailable: Boolean,
    uiLanguage: AppLanguage = AppLanguage.RU,
    onQuickLink: (String) -> Unit,
    onRecent: (String) -> Unit,
    onRunNetworkTest: () -> Unit,
    onDismissNetworkTest: () -> Unit = {},
    connectStatus: com.baddysays.saylat.data.ConnectStatus? = null,
    onService: (String) -> Unit = {},
    onOpenServiceSettings: () -> Unit = {},
    onOpenSearchSettings: () -> Unit = {},
    slowNetworkMode: Boolean = false,
    liteImagesEnabled: Boolean = false,
    onSpeedModeChange: (QuickSpeedMode) -> Unit = {},
    favorites: List<SaylatPrefs.FavoriteLink> = emptyList(),
    visitHistory: List<SaylatPrefs.VisitEntry> = emptyList(),
    historyEntries: List<HistoryEntry> = emptyList(),
    onOpenVisit: (String) -> Unit = {},
    onOpenFavorite: (String) -> Unit = {},
    onRemoveFavorite: (String) -> Unit = {},
    onPinFavorite: (SaylatPrefs.FavoriteLink) -> Unit = {},
    offlineCache: List<PageCache.CachedEntry> = emptyList(),
    onOpenCached: (String) -> Unit = {},
    trafficSavedToday: Long = 0L,
    modifier: Modifier = Modifier,
) {
    val items = remember(favorites, offlineCache) { buildHomeList(favorites, offlineCache) }
    val homeHistory = remember(historyEntries, visitHistory) {
        if (historyEntries.isNotEmpty()) historyEntries
        else visitHistory.map { HistoryEntry(url = it.url, title = it.title, visitedAt = it.visitedAt) }
    }
    val dateFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFmtDay = remember { SimpleDateFormat("d MMM", Locale("ru")) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        item {
            HomeScreenContent(
                history = homeHistory,
                savedToday = trafficSavedToday,
                smartLayoutAvailable = smartLayoutAvailable,
                onNavigate = onQuickLink,
            )
        }
        if (items.isNotEmpty()) {
            item {
                Text(
                    SaylatStrings.homePinsAndCache(uiLanguage),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column {
                        items.forEachIndexed { index, entry ->
                            when (entry) {
                                is HomeListEntry.Pinned -> TgPageRow(
                                    title = entry.title.ifBlank { entry.url },
                                    subtitle = shortenUrl(entry.url),
                                    letter = avatarLetter(entry.title, entry.url),
                                    tint = MaterialTheme.colorScheme.primary,
                                    timeLabel = null,
                                        pinned = true,
                                        uiLanguage = uiLanguage,
                                        onClick = { onOpenFavorite(entry.url) },
                                    )
                                is HomeListEntry.Cached -> {
                                    val e = entry.entry
                                    val kinds = buildList {
                                        if (e.hasArticle) add(SaylatStrings.cacheKindText(uiLanguage))
                                        if (e.hasStrips) add(SaylatStrings.cacheKindStrips(uiLanguage))
                                    }.joinToString(" · ")
                                    val whenText = if (System.currentTimeMillis() - e.savedAt < 86_400_000) {
                                        dateFmt.format(Date(e.savedAt))
                                    } else {
                                        dateFmtDay.format(Date(e.savedAt))
                                    }
                                    TgPageRow(
                                        title = e.title.ifBlank { e.url },
                                        subtitle = buildString {
                                            append(shortenUrl(e.url))
                                            if (kinds.isNotBlank()) append(" · $kinds")
                                            append(" · ${formatBytes(e.bytesOnDisk)}")
                                        },
                                        letter = avatarLetter(e.title, e.url),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        timeLabel = whenText,
                                        pinned = false,
                                        cached = true,
                                        uiLanguage = uiLanguage,
                                        onClick = { onOpenCached(e.url) },
                                    )
                                }
                            }
                            if (index < items.lastIndex) {
                                TgRowDivider()
                            }
                        }
                    }
                }
            }
        }

        if (!smartLayoutAvailable) {
            item {
                Text(
                    SaylatStrings.homeSmartLayoutNote(uiLanguage),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun TgEmptyHome(uiLanguage: AppLanguage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            SaylatStrings.homeEmptyTitle(uiLanguage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            SaylatStrings.homeEmptyBody(uiLanguage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Text(
            SaylatStrings.homeSearchHint(uiLanguage),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TgPageRow(
    title: String,
    subtitle: String,
    letter: String,
    tint: Color,
    timeLabel: String?,
    pinned: Boolean,
    cached: Boolean = false,
    uiLanguage: AppLanguage = AppLanguage.RU,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tint,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                timeLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
                when {
                    pinned -> Icon(
                        Icons.Default.PushPin,
                        contentDescription = SaylatStrings.pinContentDescription(uiLanguage),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    )
                    cached -> Icon(
                        Icons.Default.OfflinePin,
                        contentDescription = SaylatStrings.cacheContentDescription(uiLanguage),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TgRowDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 74.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    )
}

private fun avatarLetter(title: String, url: String): String {
    val fromTitle = title.trim().firstOrNull()?.uppercaseChar()?.toString()
    if (!fromTitle.isNullOrBlank() && fromTitle.first().isLetter()) return fromTitle
    return runCatching {
        URI(url).host?.removePrefix("www.")?.first()?.uppercaseChar()?.toString()
    }.getOrNull() ?: "?"
}

private fun shortenUrl(url: String): String =
    url.removePrefix("https://").removePrefix("http://").removePrefix("www.").take(42)
