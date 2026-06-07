package com.baddysays.saylat.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPOutputStream

class PayloadCodecTest {

    @Test
    fun expandArticle_fromWire() {
        val json =
            """{"url":"https://example.com","title":"Hi","blocks":[{"type":"paragraph","text":"abc"}]}"""
        val gz = ByteArrayOutputStream()
        GZIPOutputStream(gz).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        val wire = WireCompressedPayload(
            codec = PayloadCodec.CODEC_GZIP_B64,
            wire_bytes = 80,
            uncompressed_bytes = json.length,
            data = Base64.getEncoder().encodeToString(gz.toByteArray()),
        )
        val restored = PayloadCodec.expandArticleSync(ArticleWireEnvelope(wire = wire))
        assertEquals("Hi", restored.title)
        assertEquals("https://example.com", restored.url)
    }
}
