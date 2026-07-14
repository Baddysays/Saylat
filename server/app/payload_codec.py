"""Сжатие JSON-статей: zstd/gzip binary, b64-envelope, кэш, быстрые уровни."""

from __future__ import annotations

import asyncio
import base64
import gzip
import json
import logging
from typing import Any

from .models import ArticleWireEnvelope, OpenResponse, SaylatArticle, WireCompressedPayload
from .response_cache import response_cache

log = logging.getLogger(__name__)

CODEC_ZSTD_BINARY = "zstd-binary"
CODEC_GZIP_BINARY = "gzip-binary"
CODEC_GZIP_B64 = "gzip-b64"
CODEC_IDENTITY = "identity"
CODEC_SAYLAT_BINARY = "saylat-binary"

MEDIA_TYPE_SAYLAT_ZSTD = "application/vnd.saylat.v1+zstd"
MEDIA_TYPE_SAYLAT_GZIP = "application/vnd.saylat.v1+gzip"
MEDIA_TYPE_SAYLAT_BINARY = "application/vnd.saylat.v1+protobuf"

HDR_WIRE_BYTES = "X-Saylat-Wire-Bytes"
HDR_UNCOMPRESSED_BYTES = "X-Saylat-Uncompressed-Bytes"
HDR_PAYLOAD_CODEC = "X-Saylat-Payload-Codec"

MIN_COMPRESS_BYTES = 1_800
MIN_SAVINGS_RATIO = 0.12
GZIP_LEVEL_FAST = 1
GZIP_LEVEL_LARGE = 3
ZSTD_LEVEL_FAST = 3
LARGE_PAYLOAD_BYTES = 96_000

_BINARY_CODECS = frozenset({CODEC_SAYLAT_BINARY, CODEC_ZSTD_BINARY, CODEC_GZIP_BINARY})
_CODEC_PREFERENCE = (CODEC_SAYLAT_BINARY, CODEC_ZSTD_BINARY, CODEC_GZIP_BINARY, CODEC_GZIP_B64)

_zstd_module: Any = False


def zstd_available() -> bool:
    global _zstd_module
    if _zstd_module is False:
        try:
            import zstandard as zstd

            _zstd_module = zstd
        except ImportError:
            _zstd_module = None
            log.info("zstandard not installed — only gzip wire codecs")
    return _zstd_module is not None


def parse_payload_codec(header: str | None) -> str | None:
    """Первый поддерживаемый кодек из заголовка клиента."""
    if not header:
        return None
    tokens = [p.strip().lower() for p in header.replace(" ", "").split(",") if p.strip()]
    if tokens == [CODEC_IDENTITY]:
        return None
    for preferred in _CODEC_PREFERENCE:
        if preferred in tokens and _codec_usable(preferred):
            return preferred
    for token in tokens:
        if token == CODEC_IDENTITY:
            continue
        if _codec_usable(token):
            return token
    return None


def _codec_usable(codec: str) -> bool:
    if codec == CODEC_ZSTD_BINARY:
        return zstd_available()
    if codec == CODEC_SAYLAT_BINARY:
        try:
            from .protobuf_codec import encode_article  # noqa: F401
            return True
        except ImportError:
            return False
    return codec in (CODEC_GZIP_BINARY, CODEC_GZIP_B64)


def default_binary_codec() -> str:
    return CODEC_ZSTD_BINARY if zstd_available() else CODEC_GZIP_BINARY


def article_to_json_bytes(article: SaylatArticle) -> bytes:
    return json.dumps(
        article.model_dump(mode="json"),
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")


def article_from_json_bytes(raw: bytes) -> SaylatArticle:
    data: dict[str, Any] = json.loads(raw.decode("utf-8"))
    return SaylatArticle.model_validate(data)


def _gzip_level_for_size(raw_len: int) -> int:
    return GZIP_LEVEL_LARGE if raw_len >= LARGE_PAYLOAD_BYTES else GZIP_LEVEL_FAST


def compress_raw(raw: bytes, codec: str) -> bytes:
    if codec == CODEC_SAYLAT_BINARY:
        from .protobuf_codec import encode_article

        article = article_from_json_bytes(raw)
        return encode_article(article)
    if codec == CODEC_ZSTD_BINARY:
        zstd = _zstd_module
        if not zstd:
            raise ValueError("zstd not available")
        return zstd.ZstdCompressor(level=ZSTD_LEVEL_FAST).compress(raw)
    if codec in (CODEC_GZIP_BINARY, CODEC_GZIP_B64):
        return gzip.compress(raw, compresslevel=_gzip_level_for_size(len(raw)))
    raise ValueError(f"Unsupported codec: {codec}")


def decompress_raw(data: bytes, codec: str) -> bytes:
    if codec == CODEC_SAYLAT_BINARY:
        from .protobuf_codec import decode_article

        article = decode_article(data)
        return article_to_json_bytes(article)
    if codec == CODEC_ZSTD_BINARY:
        zstd = _zstd_module
        if not zstd:
            raise ValueError("zstd not available")
        return zstd.ZstdDecompressor().decompress(data)
    if codec in (CODEC_GZIP_BINARY, CODEC_GZIP_B64):
        return gzip.decompress(data)
    raise ValueError(f"Unsupported codec: {codec}")


def media_type_for_codec(codec: str) -> str:
    if codec == CODEC_SAYLAT_BINARY:
        return MEDIA_TYPE_SAYLAT_BINARY
    if codec == CODEC_ZSTD_BINARY:
        return MEDIA_TYPE_SAYLAT_ZSTD
    return MEDIA_TYPE_SAYLAT_GZIP


def _ensure_payload_stats(article: SaylatArticle, raw_len: int) -> SaylatArticle:
    if article.stats.payload_bytes <= 0:
        article.stats.payload_bytes = raw_len
    return article


def _should_skip_wire(wire_bytes: int, uncompressed_bytes: int) -> bool:
    return wire_bytes >= int(uncompressed_bytes * (1.0 - MIN_SAVINGS_RATIO))


def compress_article_sync(article: SaylatArticle, codec: str) -> dict[str, Any]:
    """Один проход сжатия в worker-thread (не блокирует asyncio)."""
    if codec not in _BINARY_CODECS | {CODEC_GZIP_B64}:
        raise ValueError(f"Unsupported codec: {codec}")

    article = _ensure_payload_stats(article, len(article_to_json_bytes(article)))
    raw = article_to_json_bytes(article)
    raw_len = len(raw)

    if raw_len < MIN_COMPRESS_BYTES:
        return {"skip": True, "reason": "too_small", "raw_len": raw_len}

    compressed = compress_raw(raw, codec if codec != CODEC_GZIP_B64 else CODEC_GZIP_BINARY)
    wire_bytes = len(compressed)
    if _should_skip_wire(wire_bytes, raw_len):
        return {"skip": True, "reason": "no_gain", "raw_len": raw_len, "wire_bytes": wire_bytes}

    if codec in _BINARY_CODECS:
        return {
            "skip": False,
            "codec": codec,
            "blob_b64": base64.b64encode(compressed).decode("ascii"),
            "wire_bytes": wire_bytes,
            "uncompressed_bytes": raw_len,
        }

    data = base64.b64encode(compressed).decode("ascii")
    wire = WireCompressedPayload(
        codec=CODEC_GZIP_B64,
        wire_bytes=len(data.encode("ascii")),
        uncompressed_bytes=raw_len,
        data=data,
    )
    return {"skip": False, "wire": wire.model_dump(mode="json")}


def decompress_payload_bytes(
    payload: bytes,
    codec: str,
    wire_bytes: int,
    uncompressed_bytes: int,
) -> SaylatArticle:
    raw = decompress_raw(payload, codec)
    article = article_from_json_bytes(raw)
    article.stats.wire_bytes = wire_bytes
    if article.stats.payload_bytes <= 0:
        article.stats.payload_bytes = uncompressed_bytes
    return article


def decompress_gzip_bytes(gzip_bytes: bytes, wire_bytes: int, uncompressed_bytes: int) -> SaylatArticle:
    return decompress_payload_bytes(gzip_bytes, CODEC_GZIP_BINARY, wire_bytes, uncompressed_bytes)


def decompress_article(wire: WireCompressedPayload) -> SaylatArticle:
    compressed = base64.b64decode(wire.data.encode("ascii"))
    codec = wire.codec if wire.codec in (CODEC_ZSTD_BINARY, CODEC_GZIP_BINARY, CODEC_GZIP_B64) else CODEC_GZIP_B64
    return decompress_payload_bytes(compressed, codec, wire.wire_bytes, wire.uncompressed_bytes)


def _wire_cache_key(cache_key: str, codec: str) -> str:
    if codec in _BINARY_CODECS:
        return f"wirebin:{cache_key}:{codec}"
    return f"wire:{cache_key}:{codec}"


async def prepare_article_envelope(
    article: SaylatArticle,
    codec: str | None,
    cache_key: str,
) -> ArticleWireEnvelope:
    if not codec or codec in _BINARY_CODECS:
        return ArticleWireEnvelope(article=article)
    if codec != CODEC_GZIP_B64:
        return ArticleWireEnvelope(article=article)

    wire_cache_key = _wire_cache_key(cache_key, codec)

    async def _load() -> dict[str, Any]:
        return await asyncio.to_thread(compress_article_sync, article, codec)

    cached = await response_cache.get_or_set(wire_cache_key, _load)
    if cached.get("skip"):
        return ArticleWireEnvelope(article=article)

    wire = WireCompressedPayload.model_validate(cached["wire"])
    article.stats.wire_bytes = wire.wire_bytes
    return ArticleWireEnvelope(article=None, wire=wire)


async def prepare_binary_body(
    article: SaylatArticle,
    cache_key: str,
    codec: str | None = None,
) -> tuple[bytes | None, dict[str, str], str]:
    """
    Сырые сжатые байты для /api/extract/binary и /api/open/binary.
    codec — из заголовка клиента или лучший доступный (zstd > gzip).
    """
    chosen = codec if codec in _BINARY_CODECS else default_binary_codec()
    wire_cache_key = _wire_cache_key(cache_key, chosen)

    async def _load() -> dict[str, Any]:
        return await asyncio.to_thread(compress_article_sync, article, chosen)

    cached = await response_cache.get_or_set(wire_cache_key, _load)
    if cached.get("skip"):
        return None, {}, "application/json"

    blob = base64.b64decode(cached["blob_b64"].encode("ascii"))
    wire_bytes = int(cached["wire_bytes"])
    uncompressed = int(cached["uncompressed_bytes"])
    article.stats.wire_bytes = wire_bytes
    headers = {
        HDR_PAYLOAD_CODEC: chosen,
        HDR_WIRE_BYTES: str(wire_bytes),
        HDR_UNCOMPRESSED_BYTES: str(uncompressed),
    }
    return blob, headers, media_type_for_codec(chosen)


async def maybe_wire_compress_open(
    response: OpenResponse,
    codec: str | None,
    cache_key: str | None = None,
) -> OpenResponse:
    if not codec or response.kind != "article" or response.article is None:
        return response
    if codec in _BINARY_CODECS:
        return response
    key = cache_key or f"open:{response.article.url}"
    envelope = await prepare_article_envelope(response.article, codec, key)
    return OpenResponse(
        kind="article",
        article=envelope.article,
        feed=None,
        wire=envelope.wire,
    )
