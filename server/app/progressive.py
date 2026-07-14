"""Прогрессивная загрузка статьи через Server-Sent Events (SSE).

Для пользователей 2G/EDGE: вместо ожидания полной статьи (5–15 с)
клиент получает контент по приоритету:
  0. meta   — заголовок, выдержка, язык (мгновенно)
  1. blocks — первые 5 текстовых блоков (0.5–2 с)
  2. blocks — оставшиеся текстовые блоки (2–5 с)
  3. images — картинки по одной (5–10 с)
  4. links  — ссылки, css_hints, статистика (финал)
"""

from __future__ import annotations

import asyncio
import json
import time
from collections.abc import AsyncIterator
from typing import Any

from fastapi.responses import StreamingResponse

from .compression_levels import apply_compression_level, images_mode_for_level, parse_compression_level
from .extract import extract_article, extract_plain_fallback
from .images import NORMAL_PROFILE, TINY_PROFILE, fetch_image_data_url
from .models import Block, SaylatArticle

# ── Типы блоков, считающиеся «текстовыми» ──────────────────────────
_TEXT_BLOCK_TYPES = frozenset({"heading", "paragraph", "quote", "list"})

# ── Сколько текстовых блоков отправить в первом пакете ──────────────
_FIRST_TEXT_BATCH = 5

# ── Задержки между приоритетными группами (секунды) ────────────────
# На 2G каждый килобайт ≈ 0.5–1 с, поэтому паузы небольшие —
# клиент всё равно занят приёмом предыдущего чанка.
_DELAY_P0_TO_P1 = 0.05   # meta → первый текст
_DELAY_P1_TO_P2 = 0.25   # первый текст → остаток
_DELAY_P2_TO_P3 = 0.40   # текст → картинки
_DELAY_IMG_BATCH = 0.15  # между картинками
_DELAY_P3_TO_P4 = 0.25   # картинки → метаданные


# ── Утилиты SSE ────────────────────────────────────────────────────

def _sse_event(event: str, data: Any) -> str:
    """Формирует одно SSE-событие по спецификации HTML5.

    Формат:
        event: <name>\\n
        data: <json>\\n
        \\n
    """
    payload = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    return f"event: {event}\ndata: {payload}\n\n"


def _block_to_dict(block: Block) -> dict[str, Any]:
    """Сериализует блок в минимальный dict для SSE-payload."""
    d: dict[str, Any] = {"type": block.type}
    if block.text is not None:
        d["text"] = block.text
    if block.level is not None:
        d["level"] = block.level
    if block.src is not None:
        d["src"] = block.src
    if block.alt is not None:
        d["alt"] = block.alt
    if block.width is not None:
        d["width"] = block.width
    if block.height is not None:
        d["height"] = block.height
    if block.items is not None:
        d["items"] = block.items
    if block.href is not None:
        d["href"] = block.href
    return d


# ── Классификация блоков по приоритетам ────────────────────────────

def _classify_blocks(
    blocks: list[Block],
) -> tuple[list[Block], list[Block], list[Block]]:
    """Разбивает блоки на три группы: первые текстовые, остальные текстовые, картинки."""
    text_blocks: list[Block] = []
    image_blocks: list[Block] = []

    for block in blocks:
        if block.type == "image":
            image_blocks.append(block)
        elif block.type in _TEXT_BLOCK_TYPES:
            text_blocks.append(block)
        # Прочие типы (divider, link) — добавляем к «остатку текста»
        elif block.type == "link":
            text_blocks.append(block)

    first_text = text_blocks[:_FIRST_TEXT_BATCH]
    rest_text = text_blocks[_FIRST_TEXT_BATCH:]
    return first_text, rest_text, image_blocks


# ── Основной генератор SSE-событий из готовой статьи ───────────────

async def build_progressive_events(article: SaylatArticle) -> AsyncIterator[str]:
    """Принимает извлечённую SaylatArticle и отдаёт SSE-строки по приоритету.

    Порядок:
      event: meta     — заголовок, выдержка, язык, layout_hint, site_profile
      event: blocks   — первые 5 текстовых блоков
      event: blocks   — оставшиеся текстовые блоки
      event: images   — картинки (по одной / мелкими пакетами)
      event: links    — ссылки и css_hints
      event: stats    — статистика загрузки
      event: done     — конец потока
    """
    first_text, rest_text, image_blocks = _classify_blocks(article.blocks)

    # ── Priority 0: meta (мгновенно) ───────────────────────────
    meta_data: dict[str, Any] = {
        "url": article.url,
        "title": article.title,
        "excerpt": article.excerpt,
        "lang": article.lang,
        "layout_hint": article.layout_hint,
    }
    if article.byline:
        meta_data["byline"] = article.byline
    if article.site_profile != "generic":
        meta_data["site_profile"] = article.site_profile
    yield _sse_event("meta", meta_data)

    await asyncio.sleep(_DELAY_P0_TO_P1)

    # ── Priority 1: первые текстовые блоки ─────────────────────
    if first_text:
        yield _sse_event(
            "blocks",
            {"blocks": [_block_to_dict(b) for b in first_text], "priority": 1},
        )

    await asyncio.sleep(_DELAY_P1_TO_P2)

    # ── Priority 2: оставшиеся текстовые блоки ─────────────────
    if rest_text:
        yield _sse_event(
            "blocks",
            {"blocks": [_block_to_dict(b) for b in rest_text], "priority": 2},
        )

    await asyncio.sleep(_DELAY_P2_TO_P3)

    # ── Priority 3: картинки (по одной) ────────────────────────
    if image_blocks:
        for img_block in image_blocks:
            yield _sse_event(
                "images",
                {"blocks": [_block_to_dict(img_block)]},
            )
            await asyncio.sleep(_DELAY_IMG_BATCH)

    await asyncio.sleep(_DELAY_P3_TO_P4)

    # ── Priority 4: ссылки, css_hints ──────────────────────────
    if article.links:
        yield _sse_event(
            "links",
            {
                "links": [
                    {"text": lnk.text, "href": lnk.href}
                    for lnk in article.links
                ],
            },
        )

    if article.css_hints:
        yield _sse_event(
            "links",
            {"css_hints": article.css_hints.model_dump(mode="json")},
        )

    # ── Статистика ─────────────────────────────────────────────
    yield _sse_event(
        "stats",
        {
            "original_bytes": article.stats.original_bytes,
            "payload_bytes": article.stats.payload_bytes,
            "fetch_ms": article.stats.fetch_ms,
            "images_inlined": article.stats.images_inlined,
            "images_omitted": article.stats.images_omitted,
        },
    )

    # ── Конец потока ───────────────────────────────────────────
    yield _sse_event("done", {})


# ── Генератор с полным циклом извлечения ───────────────────────────

async def progressive_extract(
    url: str,
    images: str = "normal",
    level: str = "medium",
) -> AsyncIterator[str]:
    """Извлекает статью и отдаёт SSE-события прогрессивно.

    1. Сначала извлекает статью целиком (используя кэш, если есть).
    2. Затем стримит контент по приоритету через build_progressive_events.

    Для изображений используется «ленивый» подход: если images != off/layout,
    изображения инлайнятся в data URL (как в обычном extract).
    """
    # NOTE: Removed `from .main import shared_http_client` to avoid circular import.
    # Removed `from .fetch_policy import outbound_timeout_sec` (unused).
    from .site_feeds import feed_to_article, try_open_site
    from .url_safety import validate_public_http_url

    started = time.perf_counter()

    # Валидация URL
    try:
        parsed_url = validate_public_http_url(url)
    except ValueError:
        yield _sse_event("meta", {
            "url": url,
            "title": "Ошибка",
            "excerpt": "Некорректный URL",
            "lang": "",
            "layout_hint": "minimal",
            "error": "invalid_url",
        })
        yield _sse_event("done", {})
        return

    # Определяем режим изображений и уровень сжатия
    compression = parse_compression_level(level)
    images_mode = images_mode_for_level(compression, images)

    # Извлекаем статью (используем существующую логику с кэшем)
    try:
        from .response_cache import response_cache

        cache_key = f"extract:{parsed_url}:{images_mode}:{compression}"

        async def _load() -> SaylatArticle:
            opened = await try_open_site(parsed_url, images_mode=images_mode)
            if opened is not None:
                if opened.kind == "article" and opened.article is not None:
                    article = opened.article
                elif opened.kind == "feed" and opened.feed is not None:
                    article = feed_to_article(parsed_url, opened.feed)
                else:
                    article = await extract_article(parsed_url, images_mode=images_mode)
            else:
                article = await extract_article(parsed_url, images_mode=images_mode)
            return apply_compression_level(article, compression)

        try:
            article = await response_cache.get_or_set(cache_key, _load)
        except Exception:
            # Фолбэк на plain-режим при ошибке readability
            try:
                article = await extract_plain_fallback(parsed_url)
                article = apply_compression_level(article, compression)
            except Exception as exc:
                yield _sse_event("meta", {
                    "url": parsed_url,
                    "title": "Ошибка загрузки",
                    "excerpt": str(exc)[:200],
                    "lang": "",
                    "layout_hint": "minimal",
                    "error": "fetch_failed",
                })
                yield _sse_event("done", {})
                return

    except Exception as exc:
        yield _sse_event("meta", {
            "url": url,
            "title": "Ошибка",
            "excerpt": str(exc)[:200],
            "lang": "",
            "layout_hint": "minimal",
            "error": "internal",
        })
        yield _sse_event("done", {})
        return

    # Отдаём прогрессивно
    async for sse_str in build_progressive_events(article):
        yield sse_str


# ── Хелпер для FastAPI StreamingResponse ───────────────────────────

async def _sse_stream_wrapper(
    generator: AsyncIterator[str],
) -> AsyncIterator[bytes]:
    """Оборачивает SSE-строки в bytes для StreamingResponse."""
    try:
        async for chunk in generator:
            yield chunk.encode("utf-8")
    except asyncio.CancelledError:
        # Клиент закрыл соединение — это нормально для SSE
        return
    except Exception:
        # При ошибке пытаемся отправить событие error
        try:
            error_event = _sse_event("error", {"message": "stream_interrupted"})
            yield error_event.encode("utf-8")
        except Exception:
            pass


def progressive_streaming_response(
    generator: AsyncIterator[str],
) -> StreamingResponse:
    """Создаёт FastAPI StreamingResponse из SSE-генератора.

    Использование:
        @app.get("/api/extract/progressive")
        async def extract_progressive(url: str = Query(...)):
            return progressive_streaming_response(
                progressive_extract(url, images="tiny", level="medium")
            )
    """
    return StreamingResponse(
        _sse_stream_wrapper(generator),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # nginx не буферизует
            "Content-Type": "text/event-stream; charset=utf-8",
        },
    )
