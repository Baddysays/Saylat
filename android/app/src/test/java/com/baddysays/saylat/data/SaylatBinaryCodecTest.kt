package com.baddysays.saylat.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SaylatBinaryCodecTest {

    @Test
    fun decode_minimalArticle() {
        // Сгенерировано сервером: article_to_bytes(SaylatArticle(url=..., title=Hi, blocks=[paragraph abc]))
        val hex = "5a010a1368747470733a2f2f6578616d706c652e636f6d1202486932080708021203616263400148015002"
        val bytes = hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val article = SaylatBinaryCodec.decode(bytes)
        assertEquals("https://example.com", article.url)
        assertEquals("Hi", article.title)
        assertEquals(1, article.blocks.size)
        assertEquals("paragraph", article.blocks[0].type)
        assertEquals("abc", article.blocks[0].text)
    }
}
