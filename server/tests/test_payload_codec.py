import asyncio

import pytest

from app.compression_levels import apply_compression_level
from app.models import ArticleStats, Block, OpenResponse, SaylatArticle, WireCompressedPayload
from app.payload_codec import (
    CODEC_GZIP_B64,
    CODEC_GZIP_BINARY,
    CODEC_ZSTD_BINARY,
    article_to_json_bytes,
    compress_article_sync,
    decompress_article,
    decompress_payload_bytes,
    maybe_wire_compress_open,
    prepare_binary_body,
    zstd_available,
)


def _sample_article(*, big: bool = False) -> SaylatArticle:
    text = "Hello world. " * (200 if big else 40)
    return SaylatArticle(
        url="https://example.com/post",
        title="Test",
        blocks=[Block(type="paragraph", text=text)],
        stats=ArticleStats(original_bytes=50_000, payload_bytes=0, fetch_ms=120),
    )


def test_gzip_binary_roundtrip():
    article = apply_compression_level(_sample_article(big=True), "medium")
    cached = compress_article_sync(article, CODEC_GZIP_BINARY)
    assert not cached["skip"]
    blob = __import__("base64").b64decode(cached["blob_b64"])
    restored = decompress_payload_bytes(
        blob, CODEC_GZIP_BINARY, cached["wire_bytes"], cached["uncompressed_bytes"]
    )
    assert restored.title == article.title


@pytest.mark.skipif(not zstd_available(), reason="zstandard not installed")
def test_zstd_binary_roundtrip():
    article = apply_compression_level(_sample_article(big=True), "full")
    cached = compress_article_sync(article, CODEC_ZSTD_BINARY)
    assert not cached["skip"]
    blob = __import__("base64").b64decode(cached["blob_b64"])
    restored = decompress_payload_bytes(
        blob, CODEC_ZSTD_BINARY, cached["wire_bytes"], cached["uncompressed_bytes"]
    )
    assert restored.title == article.title


def test_gzip_b64_roundtrip():
    article = apply_compression_level(_sample_article(big=True), "medium")
    cached = compress_article_sync(article, CODEC_GZIP_B64)
    wire = WireCompressedPayload.model_validate(cached["wire"])
    restored = decompress_article(wire)
    assert restored.title == article.title


@pytest.mark.skipif(not zstd_available(), reason="zstandard not installed")
def test_prepare_binary_prefers_zstd():
    article = apply_compression_level(_sample_article(big=True), "full")

    async def _run():
        return await prepare_binary_body(article, "test:zstd:1", codec=CODEC_ZSTD_BINARY)

    body, headers, media_type = asyncio.run(_run())
    assert body is not None
    assert headers["X-Saylat-Payload-Codec"] == CODEC_ZSTD_BINARY
    assert "zstd" in media_type


def test_maybe_wire_compress_open_b64():
    article = apply_compression_level(_sample_article(big=True), "medium")
    raw = OpenResponse(kind="article", article=article)

    async def _run():
        return await maybe_wire_compress_open(raw, CODEC_GZIP_B64, cache_key="test:open")

    wired = asyncio.run(_run())
    assert wired.wire is not None
