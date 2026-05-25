package com.baddysays.saylat.engine

import com.baddysays.saylat.data.Block
import com.baddysays.saylat.data.FeedItem
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.data.SaylatFeed

object TelegramFeedMapper {
    fun toArticle(item: FeedItem, feed: SaylatFeed): SaylatArticle {
        val blocks = buildList {
            item.from?.takeIf { it.isNotBlank() }?.let { add(Block(type = "paragraph", text = it)) }
            if (item.body.isNotBlank()) add(Block(type = "paragraph", text = item.body))
            item.time.takeIf { it.isNotBlank() }?.let { add(Block(type = "paragraph", text = it)) }
        }
        return SaylatArticle(
            url = "saylat://telegram/${feed.context_id}/${item.id}",
            title = item.title.ifBlank { feed.title },
            byline = feed.subtitle,
            blocks = blocks.ifEmpty { listOf(Block(type = "paragraph", text = "—")) },
            site_profile = "telegram",
            layout_hint = "telegram_message",
        )
    }
}
