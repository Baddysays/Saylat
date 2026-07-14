"""Спрайт-лист: объединяем N картинок статьи в одну вертикальную полосу.

На 2G/EDGE каждый HTTP-запрос стоит 1–3 секунды на round-trip.
Вместо 5 отдельных запросов клиент получает 1 спрайт-лист и
кадрирует отдельные картинки через sprite_y / sprite_h.

Схема:
  1. Сервер собирает URL картинок из блоков article
  2. Загружает параллельно (asyncio.gather), ресайзит до width
  3. Склеивает в вертикальную JPEG/WebP полосу с 2px зазорами
  4. Отдаёт base64 data URL + метаданные каждого кадра
  5. Клиент использует Canvas.clip() / ImageView viewport для показа
"""

from __future__ import annotations

import asyncio
import base64
import io
import logging
from urllib.parse import urljoin, urlparse

import httpx
from PIL import Image
from pydantic import BaseModel, Field

from .config import settings
from .url_safety import validate_public_http_url

logger = logging.getLogger(__name__)

# ── Константы ─────────────────────────────────────────────────────────

SPRITE_GAP_PX = 2           # зазор между картинками в спрайте
MIN_IMAGE_WIDTH = 50         # пропускаем иконки и трекеры
MAX_SPRITE_HEIGHT = 5000     # лимит общей высоты спрайта
DEFAULT_SPRITE_WIDTH = 360
DEFAULT_JPEG_QUALITY = 50
DEFAULT_MAX_IMAGES = 8

# ── URL safety (SSRF) ─────────────────────────────────────────────────


def _is_safe_image_url(url: str, page_url: str) -> bool:
    try:
        validate_public_http_url(url, check_dns=True)
        return True
    except ValueError:
        return False


# ── Модели ────────────────────────────────────────────────────────────


class SpriteImage(BaseModel):
    """Метаданные одного кадра в спрайт-листе."""

    original_src: str   # оригинальный URL
    y: int              # смещение от верха спрайта (px)
    height: int         # высота кадра в спрайте (px)
    width: int          # ширина (одинакова для всех кадров)
    alt: str = ""       # alt текст


class SpriteSheetResult(BaseModel):
    """Результат сборки спрайт-листа."""

    sprite_data_url: str          # base64 data URL полного спрайта
    format: str                   # "jpeg" или "webp"
    width: int
    total_height: int
    images: list[SpriteImage]
    total_bytes: int


# ── Внутренние хелперы ────────────────────────────────────────────────


async def _fetch_single_image(
    client: httpx.AsyncClient,
    image_url: str,
    page_url: str,
    target_width: int,
) -> tuple[str, Image.Image, str] | None:
    """Загрузить одну картинку, ресайзить до target_width.

    Возвращает (original_url, Pillow-Image, alt) или None при ошибке.
    """
    absolute = urljoin(page_url, image_url)
    if not _is_safe_image_url(absolute, page_url):
        return None

    headers: dict[str, str] = {}
    page_host = urlparse(page_url).hostname or ""
    if page_host.endswith("pikabu.ru"):
        headers["Referer"] = "https://pikabu.ru/"

    try:
        resp = await client.get(
            absolute,
            timeout=settings.request_timeout_sec,
            headers=headers or None,
            follow_redirects=False,
        )
        resp.raise_for_status()
        raw = resp.content[: settings.max_image_bytes]
        if not raw:
            return None

        img = Image.open(io.BytesIO(raw))
        # Конвертируем в RGB для JPEG
        if img.mode not in ("RGB", "L"):
            img = img.convert("RGB")

        w, h = img.size
        # Пропускаем мелкие картинки (иконки, трекеры)
        if w < MIN_IMAGE_WIDTH:
            return None

        # Ресайз до целевой ширины с сохранением пропорций
        if w > target_width:
            ratio = target_width / w
            new_h = max(1, int(h * ratio))
            img = img.resize((target_width, new_h), Image.Resampling.LANCZOS)
        elif w < target_width:
            # Маленькие картинки увеличиваем до target_width
            ratio = target_width / w
            new_h = max(1, int(h * ratio))
            img = img.resize((target_width, new_h), Image.Resampling.BILINEAR)

        return (image_url, img, "")
    except Exception:
        logger.debug("sprite: не удалось загрузить %s", absolute, exc_info=True)
        return None


def _encode_sprite(img: Image.Image, quality: int) -> tuple[bytes, str]:
    """Кодировать спрайт в JPEG (или WebP при поддержке).

    Возвращает (image_bytes, format_name).
    """
    buf = io.BytesIO()

    # Пробуем WebP — лучше сжимает при том же качестве
    try:
        img.save(buf, format="WEBP", quality=quality, method=4)
        webp_bytes = buf.getvalue()
        # WebP стоит использовать, если он меньше эквивалентного JPEG
        jpeg_buf = io.BytesIO()
        img.save(jpeg_buf, format="JPEG", quality=quality, optimize=True)
        jpeg_bytes = jpeg_buf.getvalue()
        if len(webp_bytes) < len(jpeg_bytes):
            return webp_bytes, "webp"
        return jpeg_bytes, "jpeg"
    except Exception:
        # Fallback на JPEG если WebP не поддерживается
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=quality, optimize=True)
        return buf.getvalue(), "jpeg"


# ── Публичный API ─────────────────────────────────────────────────────


async def build_sprite_sheet(
    image_urls: list[str],
    page_url: str,
    width: int = DEFAULT_SPRITE_WIDTH,
    quality: int = DEFAULT_JPEG_QUALITY,
    max_images: int = DEFAULT_MAX_IMAGES,
) -> SpriteSheetResult | None:
    """Собрать спрайт-лист из списка URL картинок.

    Возвращает SpriteSheetResult или None, если собрать не удалось
    (нет картинок, только одна и т.д.).
    """
    if not image_urls:
        return None

    # Ограничиваем количество картинок
    urls = image_urls[:max_images]

    # Если всего одна картинка — спрайт не нужен
    if len(urls) < 2:
        return None

    # Create inline httpx.AsyncClient instead of using shared_http_client
    async with httpx.AsyncClient(follow_redirects=False) as client:
        # Параллельная загрузка картинок
        tasks = [_fetch_single_image(client, url, page_url, width) for url in urls]
        results = await asyncio.gather(*tasks, return_exceptions=True)

    # Фильтруем успешные результаты
    fetched: list[tuple[str, Image.Image, str]] = []
    for result in results:
        if isinstance(result, Exception):
            logger.debug("sprite: исключение при загрузке: %s", result)
            continue
        if result is not None:
            fetched.append(result)

    # Нужно минимум 2 картинки для спрайта
    if len(fetched) < 2:
        return None

    # Считаем общую высоту с зазорами и проверяем лимит
    total_height = 0
    for _, img, _ in fetched:
        total_height += img.size[1] + SPRITE_GAP_PX
    total_height -= SPRITE_GAP_PX  # последний зазор не нужен

    if total_height > MAX_SPRITE_HEIGHT:
        # Обрезаем картинки, чтобы уложиться в лимит
        cut: list[tuple[str, Image.Image, str]] = []
        running_h = 0
        for item in fetched:
            img_h = item[1].size[1]
            if running_h + img_h > MAX_SPRITE_HEIGHT:
                break
            cut.append(item)
            running_h += img_h + SPRITE_GAP_PX
        fetched = cut
        total_height = running_h - SPRITE_GAP_PX if cut else 0

    if len(fetched) < 2:
        return None

    # Создаём спрайт-лист — вертикальная полоса
    sprite_img = Image.new("RGB", (width, total_height), (255, 255, 255))

    sprite_images: list[SpriteImage] = []
    y_offset = 0

    for original_url, img, alt in fetched:
        img_w, img_h = img.size

        # Вставляем картинку в спрайт
        sprite_img.paste(img, (0, y_offset))

        sprite_images.append(
            SpriteImage(
                original_src=original_url,
                y=y_offset,
                height=img_h,
                width=img_w,
                alt=alt,
            )
        )

        y_offset += img_h + SPRITE_GAP_PX

    # Кодируем в JPEG/WebP
    sprite_bytes, fmt = _encode_sprite(sprite_img, quality)
    encoded = base64.b64encode(sprite_bytes).decode("ascii")
    mime = "image/webp" if fmt == "webp" else "image/jpeg"
    data_url = f"data:{mime};base64,{encoded}"

    return SpriteSheetResult(
        sprite_data_url=data_url,
        format=fmt,
        width=width,
        total_height=total_height,
        images=sprite_images,
        total_bytes=len(sprite_bytes),
    )


async def apply_sprite_to_article(
    article: "SaylatArticle",
    width: int = DEFAULT_SPRITE_WIDTH,
    quality: int = DEFAULT_JPEG_QUALITY,
) -> "SaylatArticle":
    """Применить спрайт-лист к статье: заменить N картинок на 1 спрайт.

    Блоки, которые не попали в спрайт (ошибка загрузки, слишком мелкие),
    остаются без изменений.
    """
    from .models import Block, SaylatArticle as SA

    # Собираем URL картинок из блоков
    image_blocks: list[tuple[int, Block]] = []
    seen_urls: set[str] = set()

    for idx, block in enumerate(article.blocks):
        if block.type != "image" or not block.src:
            continue
        # Пропускаем уже встроенные data URL
        if block.src.startswith("data:"):
            continue
        # Дедупликация
        if block.src in seen_urls:
            continue
        seen_urls.add(block.src)
        image_blocks.append((idx, block))

    if len(image_blocks) < 2:
        # Спрайт не нужен — слишком мало картинок
        return article

    page_url = article.url
    image_urls = [block.src for _, block in image_blocks]

    # Строим спрайт
    sprite_result = await build_sprite_sheet(
        image_urls=image_urls,
        page_url=page_url,
        width=width,
        quality=quality,
        max_images=DEFAULT_MAX_IMAGES,
    )

    if sprite_result is None:
        return article

    # Маппинг: original_src → SpriteImage
    src_to_sprite: dict[str, SpriteImage] = {
        si.original_src: si for si in sprite_result.images
    }

    # Обновляем блоки
    new_blocks: list[Block] = []
    sprite_src_used = False  # вставляем data URL спрайта только в первый блок

    for block in article.blocks:
        if block.type != "image" or not block.src or block.src.startswith("data:"):
            new_blocks.append(block)
            continue

        si = src_to_sprite.get(block.src)
        if si is None:
            # Картинка не попала в спрайт — оставляем как есть
            new_blocks.append(block)
            continue

        if not sprite_src_used:
            # Первый блок со спрайтом: src = data URL всего спрайта
            new_blocks.append(
                Block(
                    type="image",
                    src=sprite_result.sprite_data_url,
                    alt=block.alt or si.alt,
                    width=si.width,
                    height=si.height,
                    sprite_y=si.y,
                    sprite_h=si.height,
                )
            )
            sprite_src_used = True
        else:
            # Последующие блоки: тот же спрайт, но другие координаты
            new_blocks.append(
                Block(
                    type="image",
                    src=sprite_result.sprite_data_url,
                    alt=block.alt or si.alt,
                    width=si.width,
                    height=si.height,
                    sprite_y=si.y,
                    sprite_h=si.height,
                )
            )

    # Собираем обновлённую статью
    data = article.model_dump()
    data["blocks"] = [b.model_dump() for b in new_blocks]

    updated = SA.model_validate(data)
    payload = updated.model_dump_json()
    updated.stats.payload_bytes = max(1, len(payload.encode("utf-8")))
    updated.stats.images_inlined = len(sprite_result.images)
    return updated


async def extract_with_sprites(
    url: str,
    images_mode: str = "sprite",
    level: str = "medium",
) -> "SaylatArticle":
    """Извлечь статью и применить спрайт-лист к картинкам.

    Новый режим images_mode='sprite' — извлекаем статью,
    загружаем картинки как спрайт-лист вместо отдельных JPEG.
    """
    from .extract import extract_article
    from .compression_levels import apply_compression_level, parse_compression_level

    # Извлекаем статью в режиме 'refs' — получаем оригинальные URL картинок
    article = await extract_article(url, images_mode="refs")

    # Применяем спрайт-лист
    article = await apply_sprite_to_article(article)

    # Применяем уровень сжатия
    compression = parse_compression_level(level)
    article = apply_compression_level(article, compression)

    return article
