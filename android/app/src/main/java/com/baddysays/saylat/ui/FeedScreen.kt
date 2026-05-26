package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.size.Scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.data.FeedItem
import com.baddysays.saylat.data.SaylatFeed

@Composable
fun FeedScreen(
    feed: SaylatFeed,
    onOpenItem: (FeedItem) -> Unit,
    onOpenLink: (String) -> Unit,
    onReplyItem: ((FeedItem) -> Unit)? = null,
    onOpenServiceSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(feed.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (feed.subtitle.isNotBlank()) {
                    Text(
                        feed.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
                if (feed.stats.fetch_ms > 0) {
                    Text(
                        "Загрузка ${feed.stats.fetch_ms} мс · ${formatFeedBytes(feed.stats.payload_bytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (feed.items.isEmpty()) {
            item {
                EmptyFeedCard(onOpenServiceSettings = onOpenServiceSettings)
            }
        }
        items(feed.items, key = { it.id }) { item ->
            FeedItemCard(
                item = item,
                onClick = { onOpenItem(item) },
                onOpenLink = onOpenLink,
                onReply = if (item.actions.contains("reply")) {
                    { onReplyItem?.invoke(item) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun EmptyFeedCard(onOpenServiceSettings: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Пока пусто", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Подключите Telegram, VK, почту или Дзен в разделе «Аккаунты», и лента появится здесь.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
            onOpenServiceSettings?.let { openSettings ->
                FilledTonalButton(onClick = openSettings) {
                    Text("Открыть аккаунты")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedItemCard(
    item: FeedItem,
    onClick: () -> Unit,
    onOpenLink: (String) -> Unit,
    onReply: (() -> Unit)? = null,
) {
    val isNotice = item.kind == "notice"
    val clickable = item.href != null || item.id.startsWith("mail-") || item.id.startsWith("tgmsg-")
    Card(
        onClick = if (clickable) onClick else {
            {}
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNotice) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item.thumb?.takeIf { it.isNotBlank() }?.let { thumb ->
                val context = LocalContext.current
                val request = remember(thumb) {
                    ImageRequest.Builder(context)
                        .data(thumb)
                        .size(480, 270)
                        .scale(Scale.FILL)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            item.from?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(item.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            if (item.body.isNotBlank()) {
                LinkableText(
                    item.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    spans = null,
                    onLinkClick = onOpenLink,
                )
            }
            item.time.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val openHref = item.href?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            openHref?.let { href ->
                FilledTonalButton(onClick = { onOpenLink(href) }) {
                    Text("Открыть ссылку")
                }
            }
            onReply?.let { reply ->
                FilledTonalButton(onClick = reply) {
                    Text("Ответить")
                }
            }
        }
    }
}

private fun formatFeedBytes(n: Int): String = when {
    n >= 1_000_000 -> String.format("%.1f МБ", n / 1_000_000f)
    n >= 1_000 -> String.format("%.1f КБ", n / 1_000f)
    else -> "$n Б"
}
