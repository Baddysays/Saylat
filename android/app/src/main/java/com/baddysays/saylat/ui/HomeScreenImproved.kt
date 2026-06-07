package com.baddysays.saylat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baddysays.saylat.data.HistoryEntry

// ─────────────────────────────────────────────────────────────
// Быстрые ссылки по умолчанию
// ─────────────────────────────────────────────────────────────

data class QuickLink(
    val label: String,
    val url: String,
    val emoji: String,
)

val DEFAULT_QUICK_LINKS = listOf(
    QuickLink("Wikipedia", "https://ru.wikipedia.org", "📚"),
    QuickLink("Habr",      "https://habr.com",         "💻"),
    QuickLink("Lenta.ru",  "https://lenta.ru",         "📰"),
    QuickLink("RBC",       "https://rbc.ru",            "📊"),
    QuickLink("Meduza",    "https://meduza.io",         "🗞"),
    QuickLink("Habr RSS",  "https://habr.com/ru/rss/all/", "📡"),
    QuickLink("Pikabu",    "https://pikabu.ru",         "😄"),
    QuickLink("YouTube",   "https://m.youtube.com",    "▶️"),
    QuickLink("GitHub",    "https://github.com",        "🐙"),
)

// ─────────────────────────────────────────────────────────────
// Улучшенный HomeScreen
// ─────────────────────────────────────────────────────────────

/**
 * Заменяет экран «Пока пусто» на полезную стартовую страницу.
 *
 * Параметры:
 *   history           — последние посещённые URL из BrowsingHistory
 *   savedToday        — байт сэкономлено сегодня (из TrafficSavingsRepository)
 *   smartLayoutNotice — показывать ли уведомление об умной вёрстке
 *   onNavigate        — коллбек открытия URL
 */
@Composable
fun HomeScreenContent(
    history: List<HistoryEntry>,
    savedToday: Long,
    smartLayoutAvailable: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (savedToday > 1024) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                TrafficSavingsBadge(savedToday = savedToday)
            }
        }

        if (!smartLayoutAvailable) {
            Text(
                text = "Улучшенная вёрстка недоступна (RAM < 3.5 ГБ)",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }

        SectionHeader(title = "Быстрый доступ")
        QuickLinksRow(
            links = DEFAULT_QUICK_LINKS,
            onSelect = onNavigate,
        )

        if (history.isNotEmpty()) {
            SectionHeader(title = "Недавние")
            history.take(8).forEach { entry ->
                HistoryRow(
                    entry = entry,
                    onClick = { onNavigate(entry.url) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Компоненты
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
    )
}

@Composable
private fun QuickLinksRow(
    links: List<QuickLink>,
    onSelect: (String) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(links, key = { it.url }) { link ->
            QuickLinkChip(link = link, onClick = { onSelect(link.url) })
        }
    }
}

@Composable
private fun QuickLinkChip(
    link: QuickLink,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = link.emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = link.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title.ifBlank { entry.url },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Default.Language,
            contentDescription = "Открыть",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
    }
}
