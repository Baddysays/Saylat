package com.baddysays.saylat.data

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Декодер saylat-binary (сервер protobuf_codec.py).
 */
object SaylatBinaryCodec {
    private const val MAGIC: Int = 0x5A
    private const val VERSION: Int = 0x01

    private const val WIRE_VARINT = 0
    private const val WIRE_LENGTH_DELIMITED = 2
    private const val WIRE_FIXED32 = 5

    private val INT_TO_BLOCK_TYPE = mapOf(
        1 to "heading", 2 to "paragraph", 3 to "image", 4 to "list",
        5 to "quote", 6 to "divider", 7 to "link",
    )
    private val INT_TO_LAYOUT_HINT = mapOf(1 to "article", 2 to "feed", 3 to "minimal", 4 to "gallery")
    private val INT_TO_SITE_PROFILE = mapOf(1 to "generic", 2 to "pikabu")
    private val INT_TO_COMPRESSION_LEVEL = mapOf(1 to "light", 2 to "medium", 3 to "full")

    fun decode(data: ByteArray): SaylatArticle {
        val r = Reader(data)
        require(r.readByte() == MAGIC) { "Invalid magic" }
        require(r.readByte() == VERSION) { "Unsupported version" }

        var url = ""
        var title = ""
        var excerpt = ""
        var byline = ""
        var lang = ""
        val blocks = mutableListOf<Block>()
        var stats = ArticleStats()
        var layoutHint = "article"
        var siteProfile = "generic"
        var compressionLevel = "medium"
        var plainText = ""
        val links = mutableListOf<ArticleLink>()
        var cssHints: CssHints? = null

        while (r.hasMore()) {
            val tag = r.readVarint()
            val field = tag shr 3
            val wire = tag and 0x07
            when {
                field == 1 && wire == WIRE_LENGTH_DELIMITED -> url = r.readString()
                field == 2 && wire == WIRE_LENGTH_DELIMITED -> title = r.readString()
                field == 3 && wire == WIRE_LENGTH_DELIMITED -> excerpt = r.readString()
                field == 4 && wire == WIRE_LENGTH_DELIMITED -> byline = r.readString()
                field == 5 && wire == WIRE_LENGTH_DELIMITED -> lang = r.readString()
                field == 6 && wire == WIRE_LENGTH_DELIMITED -> {
                    val end = r.pos + r.readVarint()
                    while (r.pos < end) {
                        val blockLen = r.readVarint()
                        blocks += decodeBlock(r, blockLen)
                    }
                }
                field == 7 && wire == WIRE_LENGTH_DELIMITED -> stats = decodeArticleStats(r, r.readVarint())
                field == 8 && wire == WIRE_VARINT -> layoutHint = INT_TO_LAYOUT_HINT[r.readVarint()] ?: "article"
                field == 9 && wire == WIRE_VARINT -> siteProfile = INT_TO_SITE_PROFILE[r.readVarint()] ?: "generic"
                field == 10 && wire == WIRE_VARINT -> compressionLevel = INT_TO_COMPRESSION_LEVEL[r.readVarint()] ?: "medium"
                field == 11 && wire == WIRE_LENGTH_DELIMITED -> plainText = r.readString()
                field == 12 && wire == WIRE_LENGTH_DELIMITED -> {
                    val end = r.pos + r.readVarint()
                    while (r.pos < end) {
                        links += decodeArticleLink(r, r.readVarint())
                    }
                }
                field == 13 && wire == WIRE_LENGTH_DELIMITED -> cssHints = decodeCssHints(r, r.readVarint())
                else -> r.skipField(wire)
            }
        }

        return SaylatArticle(
            url = url,
            title = title,
            excerpt = excerpt,
            byline = byline,
            lang = lang,
            blocks = blocks,
            stats = stats,
            layout_hint = layoutHint,
            site_profile = siteProfile,
            compression_level = compressionLevel,
            plain_text = plainText,
            links = links,
            css_hints = cssHints,
        )
    }

    private fun decodeBlock(r: Reader, length: Int): Block {
        val end = r.pos + length
        var blockType = "paragraph"
        var text: String? = null
        var level: Int? = null
        var src: String? = null
        var alt: String? = null
        var width: Int? = null
        var height: Int? = null
        var items: List<String>? = null
        var spans: List<TextSpan>? = null
        var href: String? = null
        while (r.pos < end) {
            val tag = r.readVarint()
            val field = tag shr 3
            val wire = tag and 0x07
            when {
                field == 1 && wire == WIRE_VARINT -> blockType = INT_TO_BLOCK_TYPE[r.readVarint()] ?: "paragraph"
                field == 2 && wire == WIRE_LENGTH_DELIMITED -> text = r.readString()
                field == 3 && wire == WIRE_VARINT -> level = r.readVarint()
                field == 4 && wire == WIRE_LENGTH_DELIMITED -> src = r.readString()
                field == 5 && wire == WIRE_LENGTH_DELIMITED -> alt = r.readString()
                field == 6 && wire == WIRE_VARINT -> width = r.readVarint()
                field == 7 && wire == WIRE_VARINT -> height = r.readVarint()
                field == 8 && wire == WIRE_LENGTH_DELIMITED -> items = decodePackedStrings(r)
                field == 9 && wire == WIRE_LENGTH_DELIMITED -> spans = decodePackedTextSpans(r)
                field == 10 && wire == WIRE_LENGTH_DELIMITED -> href = r.readString()
                else -> r.skipField(wire)
            }
        }
        return Block(blockType, text, level, src, alt, width, height, items, spans, href)
    }

    private fun decodePackedStrings(r: Reader): List<String> {
        val end = r.pos + r.readVarint()
        val result = mutableListOf<String>()
        while (r.pos < end) result += r.readString()
        return result
    }

    private fun decodePackedTextSpans(r: Reader): List<TextSpan> {
        val end = r.pos + r.readVarint()
        val result = mutableListOf<TextSpan>()
        while (r.pos < end) result += decodeTextSpan(r, r.readVarint())
        return result
    }

    private fun decodeTextSpan(r: Reader, length: Int): TextSpan {
        val end = r.pos + length
        var text = ""
        var href: String? = null
        while (r.pos < end) {
            val tag = r.readVarint()
            val field = tag shr 3
            val wire = tag and 0x07
            when {
                field == 1 && wire == WIRE_LENGTH_DELIMITED -> text = r.readString()
                field == 2 && wire == WIRE_LENGTH_DELIMITED -> href = r.readString()
                else -> r.skipField(wire)
            }
        }
        return TextSpan(text, href)
    }

    private fun decodeArticleLink(r: Reader, length: Int): ArticleLink {
        val end = r.pos + length
        var text = ""
        var href = ""
        while (r.pos < end) {
            val tag = r.readVarint()
            val field = tag shr 3
            val wire = tag and 0x07
            when {
                field == 1 && wire == WIRE_LENGTH_DELIMITED -> text = r.readString()
                field == 2 && wire == WIRE_LENGTH_DELIMITED -> href = r.readString()
                else -> r.skipField(wire)
            }
        }
        return ArticleLink(text, href)
    }

    private fun decodeArticleStats(r: Reader, length: Int): ArticleStats {
        val end = r.pos + length
        var originalBytes = 0
        var payloadBytes = 0
        var wireBytes = 0
        var imagesInlined = 0
        var imagesOmitted = 0
        var fetchMs = 0
        while (r.pos < end) {
            val tag = r.readVarint()
            val field = tag shr 3
            val wire = tag and 0x07
            if (wire == WIRE_VARINT) {
                val value = r.readVarint()
                when (field) {
                    1 -> originalBytes = value
                    2 -> payloadBytes = value
                    3 -> wireBytes = value
                    4 -> imagesInlined = value
                    5 -> imagesOmitted = value
                    6 -> fetchMs = value
                }
            } else {
                r.skipField(wire)
            }
        }
        return ArticleStats(originalBytes, payloadBytes, wireBytes, imagesInlined, imagesOmitted, fetchMs)
    }

    private fun decodeCssHints(r: Reader, length: Int): CssHints {
        val end = r.pos + length
        var primaryColor: String? = null
        var backgroundColor: String? = null
        var bodyFontSizeSp: Float? = null
        var headingColor: String? = null
        while (r.pos < end) {
            val tag = r.readVarint()
            val field = tag shr 3
            val wire = tag and 0x07
            when {
                field == 1 && wire == WIRE_LENGTH_DELIMITED -> primaryColor = r.readString()
                field == 2 && wire == WIRE_LENGTH_DELIMITED -> backgroundColor = r.readString()
                field == 3 && wire == WIRE_FIXED32 -> bodyFontSizeSp = r.readFixed32()
                field == 4 && wire == WIRE_LENGTH_DELIMITED -> headingColor = r.readString()
                else -> r.skipField(wire)
            }
        }
        return CssHints(primaryColor, backgroundColor, bodyFontSizeSp, headingColor)
    }

    private class Reader(private val data: ByteArray) {
        var pos = 0

        fun hasMore() = pos < data.size

        fun readByte(): Int {
            if (pos >= data.size) error("EOF")
            return data[pos++].toInt() and 0xFF
        }

        fun readVarint(): Int {
            var result = 0
            var shift = 0
            while (true) {
                val b = readByte()
                result = result or (b and 0x7F shl shift)
                if (b and 0x80 == 0) return result
                shift += 7
            }
        }

        fun readString(): String {
            val length = readVarint()
            val raw = data.copyOfRange(pos, pos + length)
            pos += length
            return raw.decodeToString()
        }

        fun readFixed32(): Float {
            val bytes = data.copyOfRange(pos, pos + 4)
            pos += 4
            return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
        }

        fun skipField(wire: Int) {
            when (wire) {
                WIRE_VARINT -> readVarint()
                WIRE_LENGTH_DELIMITED -> pos += readVarint()
                WIRE_FIXED32 -> pos += 4
                else -> error("Unknown wire type: $wire")
            }
        }
    }
}
