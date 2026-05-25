package com.baddysays.saylat.engine

import com.baddysays.saylat.data.Block
import com.baddysays.saylat.data.FeedItem
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.data.SaylatFeed

object MailFeedMapper {
    fun toArticle(item: FeedItem, feed: SaylatFeed): SaylatArticle {
        val blocks = buildList {
            item.from?.takeIf { it.isNotBlank() }?.let { from ->
                add(Block(type = "paragraph", text = from))
            }
            if (item.body.isNotBlank()) {
                add(Block(type = "paragraph", text = item.body))
            }
            item.time.takeIf { it.isNotBlank() }?.let { t ->
                add(Block(type = "paragraph", text = t))
            }
        }
        return SaylatArticle(
            url = "saylat://mail/${item.id}",
            title = item.title.ifBlank { "(без темы)" },
            byline = item.from.orEmpty(),
            blocks = blocks.ifEmpty { listOf(Block(type = "paragraph", text = "—")) },
            site_profile = "mail",
            layout_hint = "mail_message",
        )
    }
}
