package com.baddysays.saylat.translate

import com.baddysays.saylat.data.Block
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.data.SaylatApi
import com.baddysays.saylat.data.TranslateRequest

object ArticleTranslator {

    private sealed class Slot {
        data object Title : Slot()
        data object Excerpt : Slot()
        data class BlockText(val blockIndex: Int) : Slot()
        data class SpanText(val blockIndex: Int, val spanIndex: Int) : Slot()
        data class ListItem(val blockIndex: Int, val itemIndex: Int) : Slot()
    }

    private fun collectTexts(article: SaylatArticle): Pair<List<String>, List<Slot>> {
        val texts = mutableListOf<String>()
        val slots = mutableListOf<Slot>()

        fun add(text: String, slot: Slot) {
            if (text.isBlank()) return
            texts += text
            slots += slot
        }

        add(article.title, Slot.Title)
        add(article.excerpt, Slot.Excerpt)

        article.blocks.forEachIndexed { bi, block ->
            when (block.type) {
                "heading", "paragraph", "quote", "link" -> {
                    val spans = block.spans
                    if (!spans.isNullOrEmpty()) {
                        spans.forEachIndexed { si, span ->
                            add(span.text, Slot.SpanText(bi, si))
                        }
                    } else {
                        add(block.text.orEmpty(), Slot.BlockText(bi))
                    }
                }
                "list" -> block.items?.forEachIndexed { ii, item ->
                    add(item, Slot.ListItem(bi, ii))
                }
            }
        }
        return texts to slots
    }

    suspend fun translateArticle(
        api: SaylatApi,
        article: SaylatArticle,
        targetLang: String,
    ): SaylatArticle {
        val (texts, slots) = collectTexts(article)
        if (texts.isEmpty()) return article

        val source = article.lang.ifBlank { "auto" }
        val response = api.translate(
            TranslateRequest(texts = texts, source = source, target = targetLang),
        )
        if (response.translations.size != texts.size) {
            throw IllegalStateException("Неверный ответ перевода")
        }

        var title = article.title
        var excerpt = article.excerpt
        val blocks = article.blocks.toMutableList()

        slots.forEachIndexed { i, slot ->
            val translated = response.translations[i]
            when (slot) {
                Slot.Title -> title = translated
                Slot.Excerpt -> excerpt = translated
                is Slot.BlockText -> {
                    val b = blocks[slot.blockIndex]
                    blocks[slot.blockIndex] = b.copy(text = translated)
                }
                is Slot.SpanText -> {
                    val b = blocks[slot.blockIndex]
                    val spans = b.spans.orEmpty().toMutableList()
                    if (slot.spanIndex in spans.indices) {
                        val old = spans[slot.spanIndex]
                        spans[slot.spanIndex] = old.copy(text = translated)
                        blocks[slot.blockIndex] = b.copy(
                            spans = spans,
                            text = spans.joinToString("") { it.text },
                        )
                    }
                }
                is Slot.ListItem -> {
                    val b = blocks[slot.blockIndex]
                    val items = b.items.orEmpty().toMutableList()
                    if (slot.itemIndex in items.indices) {
                        items[slot.itemIndex] = translated
                        blocks[slot.blockIndex] = b.copy(items = items)
                    }
                }
            }
        }

        return article.copy(title = title, excerpt = excerpt, blocks = blocks)
    }
}
