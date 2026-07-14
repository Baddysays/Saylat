"""Podcast-режим Saylat: склейка TTS-аудио из нескольких статей в один MP3-стрим.

Для пользователей на 2G: один запрос — один поток, несколько статей
озвучиваются подряд как подкаст. MP3-кадры можно склеивать без
перекодирования, поэтому стримим каждый чанк сразу, без буферизации.
"""

from __future__ import annotations

import asyncio
import logging
import shutil
import tempfile
from collections.abc import AsyncIterator
from pathlib import Path
from typing import Any

import edge_tts
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from .tts_service import (
    VOICE_PRESETS,
    _article_text,
    prepare_text,
    resolve_voice,
    text_to_speech,
)

logger = logging.getLogger(__name__)

# ──────────────────────────── Кэш тишины ────────────────────────────

# Кэш MP3-тишины по ключу "duration_ms".
# Параметры тишины (частота, битрейт) не зависят от голоса —
# MP3-декодеры корректно обрабатывают смешанные битрейты в одном потоке.
_silence_cache: dict[str, bytes] = {}


# ──────────────────────────── Генерация тишины ────────────────────────────


async def generate_silence(
    duration_ms: int = 1000,
    voice: str = "ru-RU-DmitryNeural",
    speed: str = "+0%",
) -> bytes:
    """Генерирует MP3-тишину для пауз между статьями.

    Стратегия (по приоритету):
    1. ffmpeg — генерирует настоящую тишину, если доступен
    2. edge-tts — синтезирует короткое «мм» как естественную паузу
    3. Пустой результат — если ничего не сработало

    Результат кэшируется, повторных запросов нет.
    """
    cache_key = f"{duration_ms}"
    if cache_key in _silence_cache:
        return _silence_cache[cache_key]

    silence_bytes = b""

    # ── Способ 1: ffmpeg → настоящая тишина ──
    ffmpeg_path = shutil.which("ffmpeg")
    if ffmpeg_path:
        try:
            silence_bytes = await _silence_via_ffmpeg(ffmpeg_path, duration_ms)
        except Exception:
            logger.warning("ffmpeg не смог сгенерировать тишину, пробуем edge-tts")

    # ── Способ 2: edge-tts → «мм» как естественная пауза ──
    if not silence_bytes:
        try:
            silence_bytes = await _silence_via_tts(voice, speed)
        except Exception:
            logger.warning("edge-tts не смог сгенерировать паузу")

    if silence_bytes:
        _silence_cache[cache_key] = silence_bytes
        logger.debug(
            "Тишина сгенерирована: %d байт (%d мс)",
            len(silence_bytes),
            duration_ms,
        )

    return silence_bytes


async def _silence_via_ffmpeg(ffmpeg_path: str, duration_ms: int) -> bytes:
    """Генерирует MP3-тишину через ffmpeg.

    Параметры: 24 кГц, моно, 48 кбит/с — совпадают с edge-tts (MPEG2 Layer3).
    ffmpeg может использовать ближайший поддерживаемый битрейт (64 кбит/с),
    это нормально — MP3-декодеры обрабатывают смешанные битрейты корректно.
    """
    duration_sec = max(0.1, duration_ms / 1000.0)
    with tempfile.TemporaryDirectory(prefix="saylat_silence_") as tmp_dir:
        out_path = Path(tmp_dir) / "silence.mp3"
        proc = await asyncio.create_subprocess_exec(
            ffmpeg_path,
            "-y",
            "-f", "lavfi",
            "-i", f"anullsrc=r=24000:cl=mono",
            "-t", f"{duration_sec:.2f}",
            "-c:a", "libmp3lame",
            "-b:a", "48k",
            "-map_metadata", "-1",
            "-id3v2_version", "0",
            str(out_path),
            stdout=asyncio.subprocess.DEVNULL,
            stderr=asyncio.subprocess.DEVNULL,
        )
        await proc.wait()
        if out_path.exists() and out_path.stat().st_size > 0:
            return out_path.read_bytes()

    return b""


async def _silence_via_tts(voice: str, speed: str) -> bytes:
    """Генерирует короткую паузу через edge-tts: «мм» ≈ 0.5 сек."""
    communicate = edge_tts.Communicate("мм", voice, rate=speed)
    chunks: list[bytes] = []
    async for chunk in communicate.stream():
        if chunk["type"] == "audio":
            chunks.append(chunk["data"])
    return b"".join(chunks)


# ──────────────────────────── Подкаст из ленты ────────────────────────────


async def generate_podcast(
    voice: str = "ru-RU-DmitryNeural",
    speed: str = "+0%",
    max_articles: int = 5,
    sources: list[str] | None = None,
) -> AsyncIterator[bytes]:
    """Генерирует подкаст из объединённой ленты Saylat.

    Для каждой статьи (до max_articles):
    1. Анонс: «Следующая статья. Заголовок: {title}»
    2. TTS текста статьи
    3. Пауза-тишина между статьями

    Стримит MP3-чанки по мере генерации — без буферизации всего подкаста.
    Если одна статья не удалась — пропускает и продолжает дальше.
    """
    from .unified_feed import build_unified_feed

    # Берём с запасом — потом отфильтруем и обрежем
    feed = await build_unified_feed(limit=max_articles * 3)

    # Фильтрация по источникам (по префиксу заголовка)
    items = feed.items
    if sources:
        source_map: dict[str, str] = {
            "telegram": "TG",
            "tg": "TG",
            "vk": "VK",
            "вк": "VK",
            "mail": "✉",
            "почта": "✉",
            "email": "✉",
        }
        allowed_prefixes: list[str] = []
        for s in sources:
            mapped = source_map.get(s.lower().strip())
            if mapped:
                allowed_prefixes.append(mapped)
            else:
                allowed_prefixes.append(s.strip())
        items = [
            item
            for item in items
            if any(item.title.startswith(p) for p in allowed_prefixes)
        ]

    # Пропускаем служебные уведомления
    items = [item for item in items if item.kind != "notice"]
    items = items[:max_articles]

    if not items:
        logger.warning("Podcast: нет статей для озвучки")
        return

    logger.info("Podcast: начинаем озвучку %d статей, голос %s", len(items), voice)

    article_idx = 0  # Счётчик успешно озвученных

    for item in items:
        try:
            # ── Пауза между статьями (не перед первой) ──
            if article_idx > 0:
                silence = await generate_silence(voice=voice, speed=speed)
                yield silence

            # ── Анонс статьи ──
            if article_idx == 0:
                intro = f"Первая статья. Заголовок: {item.title}"
            else:
                intro = f"Следующая статья. Заголовок: {item.title}"

            async for chunk in text_to_speech(
                prepare_text(intro), voice=voice, speed=speed
            ):
                yield chunk

            # Короткая пауза после анонса
            silence = await generate_silence(voice=voice, speed=speed)
            yield silence

            # ── TTS основного текста статьи ──
            article_text = await _extract_item_text(item)

            if article_text and len(article_text.strip()) > 5:
                tts_text = prepare_text(article_text)
                async for chunk in text_to_speech(
                    tts_text, voice=voice, speed=speed
                ):
                    yield chunk
                article_idx += 1
            else:
                logger.warning(
                    "Podcast: статья «%s» без текста, пропускаем",
                    item.title[:50],
                )

        except Exception:
            logger.exception(
                "Podcast: ошибка при обработке статьи «%s»",
                item.title[:50],
            )
            continue

    logger.info("Podcast: готово, озвучено %d статей", article_idx)


# ──────────────────────────── Подкаст из URL-списка ────────────────────────────


async def podcast_from_urls(
    urls: list[str],
    voice: str = "ru-RU-DmitryNeural",
    speed: str = "+0%",
) -> AsyncIterator[bytes]:
    """Генерирует подкаст из списка URL-адресов статей.

    Для каждого URL: извлечение статьи → анонс → TTS → пауза.
    """
    from .extract import extract_article, extract_plain_fallback
    from .url_safety import validate_public_http_url

    urls = list(urls)[:20]
    article_idx = 0

    for url in urls:
        try:
            # ── Извлечение статьи ──
            parsed_url = validate_public_http_url(url)
            article = None
            try:
                article = await extract_article(parsed_url, images_mode="off")
            except Exception:
                logger.warning(
                    "Podcast: extract_article не справился, пробуем fallback: %s",
                    url,
                )
                try:
                    article = await extract_plain_fallback(parsed_url)
                except Exception as exc:
                    logger.error("Podcast: fallback не удался для %s: %s", url, exc)
                    continue

            if article is None:
                continue

            # ── Пауза между статьями ──
            if article_idx > 0:
                silence = await generate_silence(voice=voice, speed=speed)
                yield silence

            # ── Анонс ──
            title = article.title or "Без заголовка"
            if article_idx == 0:
                intro = f"Первая статья. Заголовок: {title}"
            else:
                intro = f"Следующая статья. Заголовок: {title}"

            async for chunk in text_to_speech(
                prepare_text(intro), voice=voice, speed=speed
            ):
                yield chunk

            # Пауза после анонса
            silence = await generate_silence(voice=voice, speed=speed)
            yield silence

            # ── TTS статьи ──
            article_text = _article_text(article)
            if article_text and len(article_text.strip()) > 5:
                tts_text = prepare_text(article_text)
                async for chunk in text_to_speech(
                    tts_text, voice=voice, speed=speed
                ):
                    yield chunk
                article_idx += 1
            else:
                logger.warning(
                    "Podcast: статья «%s» без текста, пропускаем",
                    title[:50],
                )

        except Exception:
            logger.exception("Podcast: ошибка при обработке URL %s", url)
            continue

    logger.info("Podcast (URLs): готово, озвучено %d статей", article_idx)


# ──────────────────────────── Подкаст из текстов ────────────────────────────


async def podcast_from_texts(
    items: list[dict],
    voice: str = "ru-RU-DmitryNeural",
    speed: str = "+0%",
) -> AsyncIterator[bytes]:
    """Генерирует подкаст из списка словарей ``{"title": "...", "text": "..."}``.

    Самый быстрый путь — без извлечения статей, текст уже готов.
    """
    article_idx = 0

    for item in items:
        title = item.get("title", "")
        text = item.get("text", "")

        if not text or len(text.strip()) < 5:
            logger.warning(
                "Podcast: элемент «%s» без текста, пропускаем",
                (title or "?")[:50],
            )
            continue

        try:
            # ── Пауза между статьями ──
            if article_idx > 0:
                silence = await generate_silence(voice=voice, speed=speed)
                yield silence

            # ── Анонс ──
            if title:
                if article_idx == 0:
                    intro = f"Первая статья. Заголовок: {title}"
                else:
                    intro = f"Следующая статья. Заголовок: {title}"

                async for chunk in text_to_speech(
                    prepare_text(intro), voice=voice, speed=speed
                ):
                    yield chunk

                # Пауза после анонса
                silence = await generate_silence(voice=voice, speed=speed)
                yield silence

            # ── TTS текста ──
            tts_text = prepare_text(text)
            async for chunk in text_to_speech(
                tts_text, voice=voice, speed=speed
            ):
                yield chunk
            article_idx += 1

        except Exception:
            logger.exception(
                "Podcast: ошибка при обработке текста «%s»",
                (title or "?")[:50],
            )
            continue

    logger.info("Podcast (texts): готово, озвучено %d элементов", article_idx)


# ──────────────────────────── Вспомогательные функции ────────────────────────────


async def _extract_item_text(item: Any) -> str:
    """Извлекает текст из FeedItem: по URL или из body."""
    if item.href:
        from .extract import extract_article, extract_plain_fallback
        from .url_safety import validate_public_http_url

        try:
            parsed_url = validate_public_http_url(item.href)
        except ValueError:
            logger.warning("Podcast: некорректный URL %s", item.href)
            return item.body or ""

        article = None
        try:
            article = await extract_article(parsed_url, images_mode="off")
        except Exception:
            try:
                article = await extract_plain_fallback(parsed_url)
            except Exception as exc:
                logger.error("Podcast: fallback не удался для %s: %s", item.href, exc)

        if article is not None:
            return _article_text(article)

    # Fallback: текст из body FeedItem
    return item.body or ""


# ──────────────────────────── Pydantic-модели ────────────────────────────


class PodcastRequest(BaseModel):
    """Запрос на генерацию подкаста."""

    urls: list[str] = Field(
        default_factory=list,
        max_length=20,
        description="URLs статей для озвучки (макс. 20)",
    )
    voice: str = "ru-m"
    speed: str = "+0%"
    max_articles: int = Field(default=5, ge=1, le=20)
    sources: list[str] | None = Field(
        default=None,
        description="Фильтр источников ленты (telegram, vk, mail)",
    )


class PodcastInfoResponse(BaseModel):
    """Информация о podcast-режиме."""

    available_voices: list[str]
    max_articles: int
    description: str


# ──────────────────────────── FastAPI-интеграция ────────────────────────────


def podcast_streaming_response(
    generator: AsyncIterator[bytes],
    *,
    article_count: int = 0,
    voice: str = "",
) -> StreamingResponse:
    """Оборачивает асинхронный генератор подкаста в FastAPI StreamingResponse.

    Использование::

        gen = generate_podcast(voice=resolved_voice, speed=speed)
        return podcast_streaming_response(gen, article_count=5, voice=voice)
    """
    headers: dict[str, str] = {
        "Content-Disposition": 'inline; filename="saylat_podcast.mp3"',
        "Cache-Control": "no-cache",  # Подкаст генерируется динамически
        "Accept-Ranges": "none",
        "X-Saylat-Podcast": "true",
    }
    if voice:
        headers["X-Saylat-Audio-Voice"] = voice
    if article_count:
        headers["X-Saylat-Podcast-Articles"] = str(article_count)

    return StreamingResponse(
        generator,
        media_type="audio/mpeg",
        headers=headers,
    )


async def podcast_info() -> PodcastInfoResponse:
    """Возвращает информацию о podcast-режиме."""
    return PodcastInfoResponse(
        available_voices=list(VOICE_PRESETS.keys()),
        max_articles=20,
        description=(
            "Podcast-режим: склейка TTS-аудио из нескольких статей "
            "в один непрерывный MP3-стрим. Удобно для прослушивания "
            "ленты как подкаста — один запрос, несколько статей подряд."
        ),
    )
