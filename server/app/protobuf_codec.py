"""Saylat Binary Codec — компактный бинарный формат статьи для 2G-клиентов.

Чистый Python, без protoc / protoc-генерации.
Формат похож на protobuf (varint, length-delimited, tag+wire_type),
но проще и не требует компиляции .proto-файлов.

Сэкономлено vs JSON:
  - нет накладных расходов JSON-синтаксиса (кавычки, двоеточия, скобки)
  - varint для целых чисел (0..127 = 1 байт вместо 1-10 символов)
  - enum-индексы вместо строк ("paragraph" → 1 байт вместо 9)
  - None/пустые поля пропускаются
  - строки — length-prefixed UTF-8 (без экранирования)
"""

from __future__ import annotations

import struct
from io import BytesIO

from .models import (
    ArticleLink,
    ArticleStats,
    Block,
    CssHints,
    SaylatArticle,
    TextSpan,
)

# ── Магические байты и версия ─────────────────────────────────

MAGIC = 0x5A  # «Z» — Saylat / сжатие
VERSION = 0x01

# ── Wire types (младшие 3 бита тега) ──────────────────────────

WIRE_VARINT = 0
WIRE_LENGTH_DELIMITED = 2
WIRE_FIXED32 = 5

# ── Enum-таблицы ───────────────────────────────────────────────

_BLOCK_TYPE_TO_INT: dict[str, int] = {
    "heading": 1,
    "paragraph": 2,
    "image": 3,
    "list": 4,
    "quote": 5,
    "divider": 6,
    "link": 7,
}
_INT_TO_BLOCK_TYPE: dict[int, str] = {v: k for k, v in _BLOCK_TYPE_TO_INT.items()}

_LAYOUT_HINT_TO_INT: dict[str, int] = {
    "article": 1,
    "feed": 2,
    "minimal": 3,
    "gallery": 4,
}
_INT_TO_LAYOUT_HINT: dict[int, str] = {v: k for k, v in _LAYOUT_HINT_TO_INT.items()}

_SITE_PROFILE_TO_INT: dict[str, int] = {
    "generic": 1,
    "pikabu": 2,
}
_INT_TO_SITE_PROFILE: dict[int, str] = {v: k for k, v in _SITE_PROFILE_TO_INT.items()}

_COMPRESSION_LEVEL_TO_INT: dict[str, int] = {
    "light": 1,
    "medium": 2,
    "full": 3,
}
_INT_TO_COMPRESSION_LEVEL: dict[int, str] = {v: k for k, v in _COMPRESSION_LEVEL_TO_INT.items()}


# ── Varint кодирование/декодирование (LEB128, как protobuf) ───


def _encode_varint(value: int) -> bytes:
    """Беззнаковый varint (LEB128). Отрицательные — как uint64."""
    if value < 0:
        value += 1 << 64
    buf = bytearray()
    while value > 0x7F:
        buf.append((value & 0x7F) | 0x80)
        value >>= 7
    buf.append(value & 0x7F)
    return bytes(buf)


def _decode_varint(stream: BytesIO) -> int:
    """Прочитать varint из потока."""
    result = 0
    shift = 0
    while True:
        b = stream.read(1)
        if not b:
            raise ValueError("Unexpected end of varint")
        byte = b[0]
        result |= (byte & 0x7F) << shift
        if not (byte & 0x80):
            break
        shift += 7
    return result


# ── Tag: (field_number << 3) | wire_type ──────────────────────


def _make_tag(field_number: int, wire_type: int) -> int:
    return (field_number << 3) | wire_type


def _parse_tag(tag: int) -> tuple[int, int]:
    """Вернуть (field_number, wire_type)."""
    return tag >> 3, tag & 0x07


# ── Примитивные кодировщики ───────────────────────────────────


def _encode_string(value: str) -> bytes:
    """Length-prefixed UTF-8 строка."""
    raw = value.encode("utf-8")
    return _encode_varint(len(raw)) + raw


def _decode_string(stream: BytesIO) -> str:
    length = _decode_varint(stream)
    raw = stream.read(length)
    return raw.decode("utf-8")


def _encode_field_varint(buf: bytearray, field_number: int, value: int) -> None:
    """Записать поле: tag(varint) + value(varint)."""
    if value == 0:
        return  # default — не кодируем
    tag = _make_tag(field_number, WIRE_VARINT)
    buf.extend(_encode_varint(tag))
    buf.extend(_encode_varint(value))


def _encode_field_string(buf: bytearray, field_number: int, value: str) -> None:
    """Записать поле: tag(length-delimited) + length-prefixed string."""
    if not value:
        return  # пустая строка — не кодируем
    tag = _make_tag(field_number, WIRE_LENGTH_DELIMITED)
    buf.extend(_encode_varint(tag))
    buf.extend(_encode_string(value))


def _encode_field_bytes(buf: bytearray, field_number: int, data: bytes) -> None:
    """Записать поле: tag(length-delimited) + length + data."""
    if not data:
        return
    tag = _make_tag(field_number, WIRE_LENGTH_DELIMITED)
    buf.extend(_encode_varint(tag))
    buf.extend(_encode_varint(len(data)))
    buf.extend(data)


def _encode_field_fixed32(buf: bytearray, field_number: int, value: float) -> None:
    """Записать 32-bit float."""
    if value == 0.0:
        return
    tag = _make_tag(field_number, WIRE_FIXED32)
    buf.extend(_encode_varint(tag))
    buf.extend(struct.pack("<f", value))


# ── Submessage кодировщики ────────────────────────────────────


def _encode_text_span(span: TextSpan) -> bytes:
    buf = bytearray()
    _encode_field_string(buf, 1, span.text)
    if span.href:
        _encode_field_string(buf, 2, span.href)
    return bytes(buf)


def _decode_text_span(stream: BytesIO, length: int) -> TextSpan:
    start = stream.tell()
    text = ""
    href: str | None = None
    while stream.tell() - start < length:
        tag = _decode_varint(stream)
        fn, wt = _parse_tag(tag)
        if fn == 1 and wt == WIRE_LENGTH_DELIMITED:
            text = _decode_string(stream)
        elif fn == 2 and wt == WIRE_LENGTH_DELIMITED:
            href = _decode_string(stream)
        else:
            _skip_field(stream, wt)
    return TextSpan(text=text, href=href)


def _encode_article_link(link: ArticleLink) -> bytes:
    buf = bytearray()
    _encode_field_string(buf, 1, link.text)
    _encode_field_string(buf, 2, link.href)
    return bytes(buf)


def _decode_article_link(stream: BytesIO, length: int) -> ArticleLink:
    start = stream.tell()
    text = ""
    href = ""
    while stream.tell() - start < length:
        tag = _decode_varint(stream)
        fn, wt = _parse_tag(tag)
        if fn == 1 and wt == WIRE_LENGTH_DELIMITED:
            text = _decode_string(stream)
        elif fn == 2 and wt == WIRE_LENGTH_DELIMITED:
            href = _decode_string(stream)
        else:
            _skip_field(stream, wt)
    return ArticleLink(text=text, href=href)


def _encode_article_stats(stats: ArticleStats) -> bytes:
    buf = bytearray()
    _encode_field_varint(buf, 1, stats.original_bytes)
    _encode_field_varint(buf, 2, stats.payload_bytes)
    _encode_field_varint(buf, 3, stats.wire_bytes)
    _encode_field_varint(buf, 4, stats.images_inlined)
    _encode_field_varint(buf, 5, stats.images_omitted)
    _encode_field_varint(buf, 6, stats.fetch_ms)
    return bytes(buf)


def _decode_article_stats(stream: BytesIO, length: int) -> ArticleStats:
    start = stream.tell()
    original_bytes = 0
    payload_bytes = 0
    wire_bytes = 0
    images_inlined = 0
    images_omitted = 0
    fetch_ms = 0
    while stream.tell() - start < length:
        tag = _decode_varint(stream)
        fn, wt = _parse_tag(tag)
        if wt == WIRE_VARINT:
            val = _decode_varint(stream)
            if fn == 1:
                original_bytes = val
            elif fn == 2:
                payload_bytes = val
            elif fn == 3:
                wire_bytes = val
            elif fn == 4:
                images_inlined = val
            elif fn == 5:
                images_omitted = val
            elif fn == 6:
                fetch_ms = val
        else:
            _skip_field(stream, wt)
    return ArticleStats(
        original_bytes=original_bytes,
        payload_bytes=payload_bytes,
        wire_bytes=wire_bytes,
        images_inlined=images_inlined,
        images_omitted=images_omitted,
        fetch_ms=fetch_ms,
    )


def _encode_css_hints(hints: CssHints) -> bytes:
    buf = bytearray()
    if hints.primary_color:
        _encode_field_string(buf, 1, hints.primary_color)
    if hints.background_color:
        _encode_field_string(buf, 2, hints.background_color)
    if hints.body_font_size_sp is not None and hints.body_font_size_sp != 0.0:
        _encode_field_fixed32(buf, 3, hints.body_font_size_sp)
    if hints.heading_color:
        _encode_field_string(buf, 4, hints.heading_color)
    return bytes(buf)


def _decode_css_hints(stream: BytesIO, length: int) -> CssHints:
    start = stream.tell()
    primary_color: str | None = None
    background_color: str | None = None
    body_font_size_sp: float | None = None
    heading_color: str | None = None
    while stream.tell() - start < length:
        tag = _decode_varint(stream)
        fn, wt = _parse_tag(tag)
        if fn == 1 and wt == WIRE_LENGTH_DELIMITED:
            primary_color = _decode_string(stream)
        elif fn == 2 and wt == WIRE_LENGTH_DELIMITED:
            background_color = _decode_string(stream)
        elif fn == 3 and wt == WIRE_FIXED32:
            body_font_size_sp = struct.unpack("<f", stream.read(4))[0]
        elif fn == 4 and wt == WIRE_LENGTH_DELIMITED:
            heading_color = _decode_string(stream)
        else:
            _skip_field(stream, wt)
    return CssHints(
        primary_color=primary_color,
        background_color=background_color,
        body_font_size_sp=body_font_size_sp,
        heading_color=heading_color,
    )


def _encode_block(block: Block) -> bytes:
    buf = bytearray()
    # type — enum varint
    type_int = _BLOCK_TYPE_TO_INT.get(block.type, 0)
    _encode_field_varint(buf, 1, type_int)
    # text
    if block.text:
        _encode_field_string(buf, 2, block.text)
    # level
    if block.level is not None and block.level > 0:
        _encode_field_varint(buf, 3, block.level)
    # src
    if block.src:
        _encode_field_string(buf, 4, block.src)
    # alt
    if block.alt:
        _encode_field_string(buf, 5, block.alt)
    # width
    if block.width is not None and block.width > 0:
        _encode_field_varint(buf, 6, block.width)
    # height
    if block.height is not None and block.height > 0:
        _encode_field_varint(buf, 7, block.height)
    # items — packed repeated string
    if block.items:
        items_buf = bytearray()
        for item in block.items:
            items_buf.extend(_encode_string(item))
        _encode_field_bytes(buf, 8, bytes(items_buf))
    # spans — packed repeated TextSpan
    if block.spans:
        spans_buf = bytearray()
        for span in block.spans:
            span_data = _encode_text_span(span)
            # Каждый span — length-prefixed submessage
            spans_buf.extend(_encode_varint(len(span_data)))
            spans_buf.extend(span_data)
        _encode_field_bytes(buf, 9, bytes(spans_buf))
    # href
    if block.href:
        _encode_field_string(buf, 10, block.href)
    return bytes(buf)


def _decode_block(stream: BytesIO, length: int) -> Block:
    start = stream.tell()
    block_type = "paragraph"  # default
    text: str | None = None
    level: int | None = None
    src: str | None = None
    alt: str | None = None
    width: int | None = None
    height: int | None = None
    items: list[str] | None = None
    spans: list[TextSpan] | None = None
    href: str | None = None

    while stream.tell() - start < length:
        tag = _decode_varint(stream)
        fn, wt = _parse_tag(tag)

        if fn == 1 and wt == WIRE_VARINT:
            val = _decode_varint(stream)
            block_type = _INT_TO_BLOCK_TYPE.get(val, "paragraph")
        elif fn == 2 and wt == WIRE_LENGTH_DELIMITED:
            text = _decode_string(stream)
        elif fn == 3 and wt == WIRE_VARINT:
            level = _decode_varint(stream)
        elif fn == 4 and wt == WIRE_LENGTH_DELIMITED:
            src = _decode_string(stream)
        elif fn == 5 and wt == WIRE_LENGTH_DELIMITED:
            alt = _decode_string(stream)
        elif fn == 6 and wt == WIRE_VARINT:
            width = _decode_varint(stream)
        elif fn == 7 and wt == WIRE_VARINT:
            height = _decode_varint(stream)
        elif fn == 8 and wt == WIRE_LENGTH_DELIMITED:
            # packed repeated string
            items = _decode_packed_strings(stream)
        elif fn == 9 and wt == WIRE_LENGTH_DELIMITED:
            # packed repeated TextSpan
            spans = _decode_packed_text_spans(stream)
        elif fn == 10 and wt == WIRE_LENGTH_DELIMITED:
            href = _decode_string(stream)
        else:
            _skip_field(stream, wt)

    return Block(
        type=block_type,
        text=text,
        level=level,
        src=src,
        alt=alt,
        width=width,
        height=height,
        items=items,
        spans=spans,
        href=href,
    )


def _decode_packed_strings(stream: BytesIO) -> list[str]:
    """Декодировать packed repeated string: length + data."""
    total_len = _decode_varint(stream)
    end = stream.tell() + total_len
    result: list[str] = []
    while stream.tell() < end:
        result.append(_decode_string(stream))
    return result


def _decode_packed_text_spans(stream: BytesIO) -> list[TextSpan]:
    """Декодировать packed repeated TextSpan: length + data."""
    total_len = _decode_varint(stream)
    end = stream.tell() + total_len
    result: list[TextSpan] = []
    while stream.tell() < end:
        span_len = _decode_varint(stream)
        span = _decode_text_span(stream, span_len)
        result.append(span)
    return result


# ── Пропуск неизвестного поля ─────────────────────────────────


def _skip_field(stream: BytesIO, wire_type: int) -> None:
    """Пропустить значение поля с неизвестным тегом."""
    if wire_type == WIRE_VARINT:
        _decode_varint(stream)
    elif wire_type == WIRE_LENGTH_DELIMITED:
        length = _decode_varint(stream)
        stream.read(length)
    elif wire_type == WIRE_FIXED32:
        stream.read(4)
    else:
        raise ValueError(f"Unknown wire type: {wire_type}")


# ── Корневые функции сериализации ─────────────────────────────


def article_to_bytes(article: SaylatArticle) -> bytes:
    """Сериализовать SaylatArticle в компактный бинарный формат."""
    buf = bytearray()

    # Заголовок: magic + version
    buf.append(MAGIC)
    buf.append(VERSION)

    # Поля SaylatArticle:
    # 1: url (string) — всегда есть
    _encode_field_string(buf, 1, article.url)
    # 2: title (string) — всегда есть
    _encode_field_string(buf, 2, article.title)
    # 3: excerpt
    _encode_field_string(buf, 3, article.excerpt)
    # 4: byline
    _encode_field_string(buf, 4, article.byline)
    # 5: lang
    _encode_field_string(buf, 5, article.lang)

    # 6: blocks — packed repeated Block
    if article.blocks:
        blocks_buf = bytearray()
        for block in article.blocks:
            block_data = _encode_block(block)
            blocks_buf.extend(_encode_varint(len(block_data)))
            blocks_buf.extend(block_data)
        _encode_field_bytes(buf, 6, bytes(blocks_buf))

    # 7: stats — submessage
    stats_data = _encode_article_stats(article.stats)
    if stats_data:
        _encode_field_bytes(buf, 7, stats_data)

    # 8: layout_hint — enum varint
    layout_int = _LAYOUT_HINT_TO_INT.get(article.layout_hint, 0)
    _encode_field_varint(buf, 8, layout_int)

    # 9: site_profile — enum varint
    profile_int = _SITE_PROFILE_TO_INT.get(article.site_profile, 0)
    _encode_field_varint(buf, 9, profile_int)

    # 10: compression_level — enum varint
    level_int = _COMPRESSION_LEVEL_TO_INT.get(article.compression_level, 0)
    _encode_field_varint(buf, 10, level_int)

    # 11: plain_text
    _encode_field_string(buf, 11, article.plain_text)

    # 12: links — packed repeated ArticleLink
    if article.links:
        links_buf = bytearray()
        for link in article.links:
            link_data = _encode_article_link(link)
            links_buf.extend(_encode_varint(len(link_data)))
            links_buf.extend(link_data)
        _encode_field_bytes(buf, 12, bytes(links_buf))

    # 13: css_hints — submessage
    if article.css_hints is not None:
        hints_data = _encode_css_hints(article.css_hints)
        if hints_data:
            _encode_field_bytes(buf, 13, hints_data)

    return bytes(buf)


def bytes_to_article(data: bytes) -> SaylatArticle:
    """Десериализовать SaylatArticle из бинарного формата."""
    stream = BytesIO(data)

    # Заголовок
    magic = stream.read(1)
    if not magic or magic[0] != MAGIC:
        raise ValueError(f"Invalid magic byte: {magic!r}, expected 0x{MAGIC:02X}")
    version = stream.read(1)
    if not version or version[0] != VERSION:
        raise ValueError(f"Unsupported version: {version!r}, expected 0x{VERSION:02X}")

    # Поля
    url = ""
    title = ""
    excerpt = ""
    byline = ""
    lang = ""
    blocks: list[Block] = []
    stats = ArticleStats()
    layout_hint: str = "article"
    site_profile: str = "generic"
    compression_level: str = "medium"
    plain_text = ""
    links: list[ArticleLink] = []
    css_hints: CssHints | None = None

    while True:
        tag_bytes = stream.read(1)
        if not tag_bytes:
            break
        stream.seek(-1, 1)  # откат — varint может быть >1 байта

        try:
            tag = _decode_varint(stream)
        except ValueError:
            break

        fn, wt = _parse_tag(tag)

        if fn == 1 and wt == WIRE_LENGTH_DELIMITED:
            url = _decode_string(stream)
        elif fn == 2 and wt == WIRE_LENGTH_DELIMITED:
            title = _decode_string(stream)
        elif fn == 3 and wt == WIRE_LENGTH_DELIMITED:
            excerpt = _decode_string(stream)
        elif fn == 4 and wt == WIRE_LENGTH_DELIMITED:
            byline = _decode_string(stream)
        elif fn == 5 and wt == WIRE_LENGTH_DELIMITED:
            lang = _decode_string(stream)
        elif fn == 6 and wt == WIRE_LENGTH_DELIMITED:
            # packed repeated Block
            total_len = _decode_varint(stream)
            end = stream.tell() + total_len
            while stream.tell() < end:
                block_len = _decode_varint(stream)
                block = _decode_block(stream, block_len)
                blocks.append(block)
        elif fn == 7 and wt == WIRE_LENGTH_DELIMITED:
            # ArticleStats submessage
            sub_len = _decode_varint(stream)
            stats = _decode_article_stats(stream, sub_len)
        elif fn == 8 and wt == WIRE_VARINT:
            val = _decode_varint(stream)
            layout_hint = _INT_TO_LAYOUT_HINT.get(val, "article")
        elif fn == 9 and wt == WIRE_VARINT:
            val = _decode_varint(stream)
            site_profile = _INT_TO_SITE_PROFILE.get(val, "generic")
        elif fn == 10 and wt == WIRE_VARINT:
            val = _decode_varint(stream)
            compression_level = _INT_TO_COMPRESSION_LEVEL.get(val, "medium")
        elif fn == 11 and wt == WIRE_LENGTH_DELIMITED:
            plain_text = _decode_string(stream)
        elif fn == 12 and wt == WIRE_LENGTH_DELIMITED:
            # packed repeated ArticleLink
            total_len = _decode_varint(stream)
            end = stream.tell() + total_len
            while stream.tell() < end:
                link_len = _decode_varint(stream)
                link = _decode_article_link(stream, link_len)
                links.append(link)
        elif fn == 13 and wt == WIRE_LENGTH_DELIMITED:
            # CssHints submessage
            sub_len = _decode_varint(stream)
            css_hints = _decode_css_hints(stream, sub_len)
        else:
            _skip_field(stream, wt)

    return SaylatArticle(
        url=url,
        title=title,
        excerpt=excerpt,
        byline=byline,
        lang=lang,
        blocks=blocks,
        stats=stats,
        layout_hint=layout_hint,
        site_profile=site_profile,
        compression_level=compression_level,
        plain_text=plain_text,
        links=links,
        css_hints=css_hints,
    )


def article_wire_size(article: SaylatArticle) -> int:
    """Оценить размер бинарного представления (без полной сериализации).

    Быстрая оценка для принятия решения о сжатии.
    """
    size = 2  # magic + version

    # Строки: varint(length) + utf8 bytes + tag overhead (~1-2 байта на поле)
    for s in (article.url, article.title, article.excerpt,
              article.byline, article.lang, article.plain_text):
        if s:
            utf8_len = len(s.encode("utf-8"))
            size += 2 + utf8_len + _varint_size(utf8_len)  # tag + length + data

    # Блоки
    for block in article.blocks:
        size += _estimate_block_size(block)

    # Stats — varints (6 полей, каждое ~1-5 байт + tag)
    size += 30  # консервативная оценка

    # Enum'ы (3 × ~2 байта: tag + varint)
    size += 6

    # Links
    for link in article.links:
        size += 2 + len(link.text.encode("utf-8")) + 2 + len(link.href.encode("utf-8"))

    # CssHints
    if article.css_hints:
        for s in (article.css_hints.primary_color, article.css_hints.background_color,
                  article.css_hints.heading_color):
            if s:
                size += 2 + len(s.encode("utf-8"))
        if article.css_hints.body_font_size_sp:
            size += 5  # tag + fixed32

    return size


def _varint_size(value: int) -> int:
    """Количество байт для varint-кодирования value."""
    if value < 0:
        value += 1 << 64
    if value <= 0x7F:
        return 1
    size = 0
    while value > 0:
        size += 1
        value >>= 7
    return size


def _estimate_block_size(block: Block) -> int:
    """Грубая оценка размера блока."""
    size = 2  # tag + enum varint (type)
    if block.text:
        utf8_len = len(block.text.encode("utf-8"))
        size += 2 + utf8_len + _varint_size(utf8_len)
    if block.level:
        size += 2
    if block.src:
        utf8_len = len(block.src.encode("utf-8"))
        size += 2 + utf8_len + _varint_size(utf8_len)
    if block.alt:
        utf8_len = len(block.alt.encode("utf-8"))
        size += 2 + utf8_len + _varint_size(utf8_len)
    if block.width:
        size += 2
    if block.height:
        size += 2
    if block.items:
        for item in block.items:
            utf8_len = len(item.encode("utf-8"))
            size += utf8_len + _varint_size(utf8_len)
        size += 3  # tag + outer length prefix
    if block.spans:
        for span in block.spans:
            utf8_len = len(span.text.encode("utf-8"))
            size += 2 + utf8_len + _varint_size(utf8_len)
            if span.href:
                href_len = len(span.href.encode("utf-8"))
                size += 2 + href_len + _varint_size(href_len)
        size += 3
    if block.href:
        utf8_len = len(block.href.encode("utf-8"))
        size += 2 + utf8_len + _varint_size(utf8_len)
    # length prefix для submessage
    size += _varint_size(size)
    return size


# Алиасы для payload_codec и внешних импортов
encode_article = article_to_bytes
decode_article = bytes_to_article
