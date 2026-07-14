"""Text-to-Speech сервис для Saylat.

Конвертирует текст статьи в аудио MP3 через Microsoft Edge TTS (edge-tts).
Полезен на 2G: аудио ~30–80 КБ/мин вместо 500+ КБ страницы, а также
для водителей и слабовидящих пользователей.
"""

import logging
import re
import time
from collections.abc import AsyncIterator
from typing import Any

import edge_tts
from fastapi import HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from .config import settings

logger = logging.getLogger(__name__)

# ──────────────────────────── Константы ────────────────────────────

# Максимум символов для одного запроса TTS (edge-tts ограничивает ~5000,
# но мы даём запас с учётом нарезки)
MAX_TEXT_LENGTH = 50_000

# Размер чанка текста для отправки в edge-tts (сервис ограничивает ~5000 символов)
CHUNK_SIZE = 4_800

# Время жизни кэша списка голосов (секунды)
VOICE_CACHE_TTL = 3600

# ──────────────────────────── Пресеты голосов ────────────────────────────

VOICE_PRESETS: dict[str, str] = {
    "ru-m": "ru-RU-DmitryNeural",       # Русский мужской
    "ru-f": "ru-RU-SvetlanaNeural",     # Русский женский
    "en-m": "en-US-GuyNeural",          # Английский мужской
    "en-f": "en-US-JennyNeural",        # Английский женский
    "de-m": "de-DE-ConradNeural",       # Немецкий мужской
    "de-f": "de-DE-KatjaNeural",        # Немецкий женский
    "fr-m": "fr-FR-HenriNeural",        # Французский мужской
    "fr-f": "fr-FR-DeniseNeural",       # Французский женский
    "es-m": "es-ES-AlvaroNeural",       # Испанский мужской
    "es-f": "es-ES-ElviraNeural",       # Испанский женский
    "zh-m": "zh-CN-YunxiNeural",        # Китайский мужской
    "zh-f": "zh-CN-XiaoxiaoNeural",     # Китайский женский
}


# ──────────────────────────── Pydantic модели ────────────────────────────

class TtsRequest(BaseModel):
    """Запрос на синтез речи: URL статьи ИЛИ прямой текст."""
    url: str = ""
    text: str = ""       # Прямой текст ИЛИ url — не оба
    voice: str = "ru-m"  # Пресет или полное имя голоса
    speed: str = "+0%"   # Напр. "-20%", "+10%", "+0%"


class TtsInfoResponse(BaseModel):
    """Информация о доступных голосах и лимитах TTS."""
    voices: list[dict[str, Any]]
    default_voice: str
    max_text_length: int


# ──────────────────────────── Разрешение голоса ────────────────────────────

def resolve_voice(voice: str) -> str:
    """Превращает пресет ('ru-m') в полное имя голоса ('ru-RU-DmitryNeural').
    Если voice не пресет — возвращаем как есть (ожидаем полное имя)."""
    return VOICE_PRESETS.get(voice.strip().lower(), voice.strip())


# ──────────────────────────── Подготовка текста ────────────────────────────

_HTML_TAG_RE = re.compile(r"<[^>]+>")
_MARKDOWN_LINK_RE = re.compile(r"\[([^\]]+)\]\([^)]+\)")
_MULTIPLE_NEWLINES_RE = re.compile(r"\n{3,}")
_MULTIPLE_SPACES_RE = re.compile(r" {2,}")


def _strip_html(text: str) -> str:
    """Убирает HTML-теги и Markdown-ссылки."""
    text = _MARKDOWN_LINK_RE.sub(r"\1", text)
    text = _HTML_TAG_RE.sub("", text)
    return text


def prepare_text(raw: str) -> str:
    """Подготавливает текст для TTS: чистка, паузы, лимит."""
    text = _strip_html(raw)

    # Убираем лишние пробелы и переносы
    text = _MULTIPLE_SPACES_RE.sub(" ", text)
    text = _MULTIPLE_NEWLINES_RE.sub("\n\n", text)

    # Абзацы → паузы: двойной перенос строки = точка + пауза
    # TTS сам делает паузу после точки
    lines = text.split("\n")
    cleaned: list[str] = []
    for line in lines:
        line = line.strip()
        if not line:
            continue
        # Добавим точку в конце абзаца, если нет знака препинания
        if line[-1] not in ".!?;:—–-":
            line += "."
        cleaned.append(line)

    text = " ".join(cleaned)

    # Ограничение длины
    if len(text) > MAX_TEXT_LENGTH:
        text = text[:MAX_TEXT_LENGTH]
        # Обрежем по последнему предложению, чтобы не разорвать
        last_punct = max(text.rfind("."), text.rfind("!"), text.rfind("?"))
        if last_punct > MAX_TEXT_LENGTH * 0.7:
            text = text[: last_punct + 1]

    return text.strip()


def _article_text(article: Any) -> str:
    """Извлекает текстовый контент из SaylatArticle."""
    # Предпочитаем plain_text, если заполнен
    if article.plain_text and len(article.plain_text.strip()) > 20:
        return article.plain_text

    # Иначе склеиваем текстовые блоки
    parts: list[str] = []
    for block in article.blocks:
        if block.type in ("heading", "paragraph", "quote"):
            text = (block.text or "").strip()
            if text:
                if block.type == "heading":
                    parts.append(text)
                else:
                    parts.append(text)
        elif block.type == "list" and block.items:
            parts.extend(block.items)

    # Добавляем заголовок статьи первым, если он есть
    title = (article.title or "").strip()
    if title and (not parts or parts[0] != title):
        parts.insert(0, title)

    return "\n\n".join(parts)


# ──────────────────────────── Нарезка длинного текста ────────────────────────────

def _split_text(text: str, chunk_size: int = CHUNK_SIZE) -> list[str]:
    """Разбивает длинный текст на чанки по предложениям.
    edge-tts имеет лимит ~5000 символов на запрос."""
    if len(text) <= chunk_size:
        return [text]

    chunks: list[str] = []
    remaining = text
    while remaining:
        if len(remaining) <= chunk_size:
            chunks.append(remaining)
            break

        # Ищем последнее предложение в пределах чанка
        cut = remaining[:chunk_size]
        best_split = -1
        for delim in (". ", "! ", "? ", ".\n", "!\n", "?\n"):
            pos = cut.rfind(delim)
            if pos > best_split:
                best_split = pos + len(delim)

        if best_split <= chunk_size * 0.4:
            # Не нашли хорошее место — режем по последнему пробелу
            best_split = cut.rfind(" ")
            if best_split <= chunk_size * 0.4:
                best_split = chunk_size

        chunks.append(remaining[:best_split])
        remaining = remaining[best_split:].lstrip()

    return chunks


# ──────────────────────────── Основной TTS ────────────────────────────

async def text_to_speech(
    text: str,
    voice: str = "ru-RU-DmitryNeural",
    speed: str = "+0%",
) -> AsyncIterator[bytes]:
    """Синтез речи через edge-tts. Возвращает MP3-чанки для стриминга.

    Автоматически нарезает длинный текст на части (edge-tts лимит ~5K символов).
    Между чанками вставляем короткую паузу (тишина в MP3).
    """
    chunks = _split_text(text)
    logger.info("TTS: %d чанков для голоса %s, скорость %s", len(chunks), voice, speed)

    for i, chunk in enumerate(chunks):
        if not chunk.strip():
            continue
        try:
            communicate = edge_tts.Communicate(chunk, voice, rate=speed)
            async for tts_chunk in communicate.stream():
                if tts_chunk["type"] == "audio":
                    yield tts_chunk["data"]
        except Exception:
            logger.exception("TTS ошибка на чанке %d/%d", i + 1, len(chunks))
            # Пропускаем проблемный чанк, продолжаем дальше
            continue


# ──────────────────────────── Статья → Аудио ────────────────────────────

async def article_to_speech(
    url: str,
    voice: str = "ru-RU-DmitryNeural",
    speed: str = "+0%",
) -> StreamingResponse:
    """Извлекает текст статьи по URL и возвращает MP3-стрим.

    Использует существующий extract_article() для получения текста.
    Аудио стримится сразу, без ожидания полной генерации.
    """
    from .extract import extract_article, extract_plain_fallback
    from .url_safety import validate_public_http_url

    # Валидация URL
    try:
        parsed_url = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    # Извлекаем статью
    article = None
    try:
        article = await extract_article(parsed_url, images_mode="off")
    except Exception:
        logger.warning("extract_article не справился, пробуем fallback: %s", url)
        try:
            article = await extract_plain_fallback(parsed_url)
        except Exception as exc:
            raise HTTPException(
                status_code=502,
                detail=f"Не удалось извлечь текст статьи: {exc}",
            ) from exc

    if article is None:
        raise HTTPException(status_code=502, detail="Не удалось извлечь текст статьи")

    # Получаем текст
    raw_text = _article_text(article)
    if not raw_text or len(raw_text.strip()) < 5:
        raise HTTPException(status_code=404, detail="Статья не содержит текста для озвучки")

    # Подготавливаем текст для TTS
    tts_text = prepare_text(raw_text)
    if not tts_text:
        raise HTTPException(status_code=404, detail="После подготовки текст пуст")

    logger.info(
        "TTS: статья '%s' → %d символов, голос %s",
        article.title[:50] if article.title else "?",
        len(tts_text),
        voice,
    )

    # Стримим аудио
    async def _audio_stream() -> AsyncIterator[bytes]:
        async for chunk in text_to_speech(tts_text, voice=voice, speed=speed):
            yield chunk

    return StreamingResponse(
        _audio_stream(),
        media_type="audio/mpeg",
        headers={
            "Content-Disposition": 'inline; filename="saylat_tts.mp3"',
            "Cache-Control": "public, max-age=600",
            "Accept-Ranges": "none",
            "X-Saylat-Audio-Chars": str(len(tts_text)),
            "X-Saylat-Audio-Voice": voice,
            "X-Saylat-Article-Title": (article.title or "")[:120],
        },
    )


# ──────────────────────────── Список голосов (с кэшем) ────────────────────────────

_voices_cache: list[dict[str, Any]] = []
_voices_cache_ts: float = 0.0


async def list_voices() -> list[dict[str, Any]]:
    """Возвращает список доступных голосов TTS. Кэшируется на VOICE_CACHE_TTL."""
    global _voices_cache, _voices_cache_ts

    now = time.time()
    if _voices_cache and (now - _voices_cache_ts) < VOICE_CACHE_TTL:
        return _voices_cache

    try:
        raw_voices = await edge_tts.list_voices()
    except Exception:
        logger.exception("Не удалось получить список голосов TTS")
        if _voices_cache:
            return _voices_cache  # Возвращаем устаревший кэш
        return []

    # Упрощаем структуру для клиента
    voices: list[dict[str, Any]] = []
    for v in raw_voices:
        voices.append({
            "name": v.get("ShortName", ""),
            "display_name": v.get("FriendlyName", v.get("ShortName", "")),
            "locale": v.get("Locale", ""),
            "gender": v.get("Gender", ""),
            "language": v.get("Languages", [{}])[0].get("DisplayName", "") if v.get("Languages") else "",
        })

    _voices_cache = voices
    _voices_cache_ts = now
    return voices


# ──────────────────────────── Информация о TTS ────────────────────────────

async def tts_info() -> TtsInfoResponse:
    """Возвращает информацию о доступных голосах и лимитах."""
    voices = await list_voices()
    return TtsInfoResponse(
        voices=voices,
        default_voice=VOICE_PRESETS.get("ru-m", "ru-RU-DmitryNeural"),
        max_text_length=MAX_TEXT_LENGTH,
    )
