"""Конвертация изображений в AVIF/WebP — экономия 30-50% трафика на 2G.

Модуль расширяет существующий пайплайн images.py (только JPEG):
- AVIF: лучшее сжатие, иногда не поддерживается старыми Pillow
- WebP: хорошая совместимость, заметно лучше JPEG
- JPEG: фолбэк, если ничего другого нет

Приоритет форматов: avif > webp > jpeg.
Грациозная деградация: ошибка AVIF → пробуем WebP → пробуем JPEG.
"""

from __future__ import annotations

import base64
import io
import logging
from dataclasses import dataclass
from functools import lru_cache
from urllib.parse import urljoin, urlparse

import httpx
from PIL import Image, features

from .config import settings

log = logging.getLogger(__name__)

# ──────────────────────────── Определение поддержки форматов ────────────────────────────

_avif_available: bool | None = None
_webp_available: bool | None = None


def check_avif_support() -> bool:
    """Проверить, умеет ли Pillow кодировать AVIF. Результат кэшируется."""
    global _avif_available
    if _avif_available is None:
        try:
            _avif_available = features.check("avif")
            # Дополнительно проверяем, что именно encode работает
            if _avif_available:
                _probe = Image.new("RGB", (1, 1))
                buf = io.BytesIO()
                _probe.save(buf, format="AVIF", quality=10)
                _avif_available = True
        except Exception:
            _avif_available = False
        log.info("AVIF encode support: %s", _avif_available)
    return _avif_available


def check_webp_support() -> bool:
    """Проверить, умеет ли Pillow кодировать WebP. Результат кэшируется."""
    global _webp_available
    if _webp_available is None:
        try:
            _webp_available = features.check("webp")
            if _webp_available:
                _probe = Image.new("RGB", (1, 1))
                buf = io.BytesIO()
                _probe.save(buf, format="WEBP", quality=10)
                _webp_available = True
        except Exception:
            _webp_available = False
        log.info("WebP encode support: %s", _webp_available)
    return _webp_available


# ──────────────────────────── Профили форматов ────────────────────────────


@dataclass(frozen=True)
class FormatProfile:
    """Профиль конвертации — аналог ImageInlineProfile из images.py."""

    format: str       # "avif", "webp", "jpeg"
    quality: int
    max_width: int
    max_images: int


# Крошечные картинки для самого медленного 2G
AVIF_TINY = FormatProfile(format="avif", quality=30, max_width=128, max_images=4)
WEBP_TINY = FormatProfile(format="webp", quality=35, max_width=128, max_images=4)

# Нормальные картинки — читаемые, но компактные
AVIF_NORMAL = FormatProfile(format="avif", quality=40, max_width=480, max_images=6)
WEBP_NORMAL = FormatProfile(format="webp", quality=50, max_width=480, max_images=6)


# ──────────────────────────── Определение поддерживаемых форматов ────────────────────────────


def detect_supported_formats(accept_header: str) -> list[str]:
    """Разобрать Accept-заголовок клиента и определить предпочтительные форматы.

    Учитываем:
    - Стандартный Accept: image/avif, image/webp
    - Кастомный заголовок Saylat: X-Saylat-Image-Format (передаётся в accept_header)

    Приоритет: avif > webp > jpeg.
    Возвращаем список в порядке предпочтения.
    """
    if not accept_header:
        return ["jpeg"]

    header = accept_header.strip().lower()

    # Кастомный заголовок X-Saylat-Image-Format — точное указание формата
    if header in ("avif", "image/avif"):
        result = ["avif"]
    elif header in ("webp", "image/webp"):
        result = ["webp"]
    elif header in ("jpeg", "image/jpeg"):
        result = ["jpeg"]
    else:
        # Парсим стандартный Accept
        result: list[str] = []
        parts = [p.strip() for p in header.split(",")]
        for part in parts:
            mime = part.split(";")[0].strip()  # убираем q= и прочие параметры
            if mime == "image/avif" and "avif" not in result:
                result.append("avif")
            elif mime == "image/webp" and "webp" not in result:
                result.append("webp")
            elif mime == "image/jpeg" and "jpeg" not in result:
                result.append("jpeg")
            elif mime == "*/*" or mime == "image/*":
                # Принимает любой формат — добавим те, что умеем
                for fmt in ("avif", "webp", "jpeg"):
                    if fmt not in result:
                        result.append(fmt)

    # JPEG всегда в конце как фолбэк
    if "jpeg" not in result:
        result.append("jpeg")

    # Убираем форматы, которые Pillow не умеет кодировать
    if not check_avif_support() and "avif" in result:
        result.remove("avif")
    if not check_webp_support() and "webp" in result:
        result.remove("webp")

    return result if result else ["jpeg"]


# ──────────────────────────── Конвертация ────────────────────────────


def _prepare_image(image_bytes: bytes, max_width: int) -> tuple[Image.Image, int, int]:
    """Открыть и масштабировать изображение. Возвращает (PIL Image, width, height)."""
    img = Image.open(io.BytesIO(image_bytes))

    # Конвертируем в RGB (AVIF/WebP не поддерживают палитру/RGBA без потерь)
    if img.mode == "RGBA":
        # Склеиваем альфу с белым фоном — для 2G прозрачность не критична
        background = Image.new("RGB", img.size, (255, 255, 255))
        background.paste(img, mask=img.split()[3])
        img = background
    elif img.mode == "P":
        # Палитровое изображение — конвертируем в RGB
        if "transparency" in img.info:
            img = img.convert("RGBA")
            background = Image.new("RGB", img.size, (255, 255, 255))
            background.paste(img, mask=img.split()[3])
            img = background
        else:
            img = img.convert("RGB")
    elif img.mode == "LA":
        img = img.convert("RGBA")
        background = Image.new("RGB", img.size, (255, 255, 255))
        background.paste(img, mask=img.split()[3])
        img = background
    elif img.mode not in ("RGB", "L"):
        img = img.convert("RGB")

    w, h = img.size
    if w > max_width:
        ratio = max_width / w
        new_h = max(1, int(h * ratio))
        img = img.resize((max_width, new_h), Image.Resampling.LANCZOS)
        w, h = img.size

    return img, w, h


def convert_to_webp(
    image_bytes: bytes,
    quality: int = 50,
    max_width: int = 480,
) -> tuple[bytes, int, int]:
    """Конвертировать изображение в WebP.

    Возвращает (webp_bytes, width, height).
    Выбрасывает RuntimeError, если WebP не поддерживается.
    """
    if not check_webp_support():
        raise RuntimeError("WebP encoding is not available in Pillow")

    img, w, h = _prepare_image(image_bytes, max_width)
    buf = io.BytesIO()
    img.save(buf, format="WEBP", quality=quality, method=4)
    return buf.getvalue(), w, h


def convert_to_avif(
    image_bytes: bytes,
    quality: int = 40,
    max_width: int = 480,
) -> tuple[bytes, int, int]:
    """Конвертировать изображение в AVIF.

    Возвращает (avif_bytes, width, height).
    При недоступности AVIF — фолбэк на WebP, затем JPEG.
    """
    img, w, h = _prepare_image(image_bytes, max_width)

    # Пробуем AVIF
    if check_avif_support():
        try:
            buf = io.BytesIO()
            img.save(buf, format="AVIF", quality=quality, speed=6)
            return buf.getvalue(), w, h
        except Exception as exc:
            log.warning("AVIF encode failed, fallback to WebP: %s", exc)

    # Фолбэк: WebP
    if check_webp_support():
        try:
            return convert_to_webp(image_bytes, quality=quality, max_width=max_width)
        except Exception as exc:
            log.warning("WebP fallback failed, fallback to JPEG: %s", exc)

    # Последний фолбэк: JPEG
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue(), w, h


def _convert_to_jpeg(
    image_bytes: bytes,
    quality: int = 50,
    max_width: int = 480,
) -> tuple[bytes, int, int]:
    """Конвертировать изображение в JPEG (фолбэк)."""
    img, w, h = _prepare_image(image_bytes, max_width)
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue(), w, h


# Маппинг формат → MIME-тип
_FORMAT_MIME: dict[str, str] = {
    "avif": "image/avif",
    "webp": "image/webp",
    "jpeg": "image/jpeg",
}

# Маппинг формат → функция конвертации
_FORMAT_CONVERTERS: dict[str, callable] = {
    "avif": convert_to_avif,
    "webp": convert_to_webp,
    "jpeg": _convert_to_jpeg,
}


def optimize_image(
    image_bytes: bytes,
    fmt: str = "auto",
    quality: int = 50,
    max_width: int = 480,
) -> tuple[bytes, str, int, int]:
    """Главная точка входа: конвертировать изображение в оптимальный формат.

    Args:
        image_bytes: Сырые байты исходного изображения.
        fmt: "auto" (определить по поддержке клиента), "avif", "webp", "jpeg".
        quality: Качество кодирования (1-100).
        max_width: Максимальная ширина в пикселях.

    Returns:
        (image_bytes, actual_format, width, height)
        actual_format может отличаться от запрошенного при фолбэке.
    """
    if fmt == "auto":
        fmt = "avif"  # По умолчанию пытаемся в лучший формат

    # Определяем порядок попыток: запрошенный → альтернативы → JPEG
    if fmt == "avif":
        attempt_order = ["avif", "webp", "jpeg"]
    elif fmt == "webp":
        attempt_order = ["webp", "avif", "jpeg"]
    else:
        attempt_order = ["jpeg"]

    last_exc: Exception | None = None
    for attempt_fmt in attempt_order:
        converter = _FORMAT_CONVERTERS.get(attempt_fmt)
        if converter is None:
            continue
        try:
            result_bytes, w, h = converter(image_bytes, quality=quality, max_width=max_width)
            if result_bytes:
                return result_bytes, attempt_fmt, w, h
        except Exception as exc:
            last_exc = exc
            log.warning(
                "Format %s conversion failed, trying next: %s",
                attempt_fmt,
                exc,
            )

    # Все попытки провалились — возвращаем исходные байты как JPEG-совпатимые
    log.error("All format conversions failed: %s", last_exc)
    try:
        img, w, h = _prepare_image(image_bytes, max_width)
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=max(quality, 20), optimize=True)
        return buf.getvalue(), "jpeg", w, h
    except Exception:
        # Совсем всё плохо — возвращаем как есть
        return image_bytes, "jpeg", 0, 0


# ──────────────────────────── Data URL ────────────────────────────


async def image_data_url_optimized(
    src: str,
    page_url: str,
    fmt: str = "auto",
    quality: int = 50,
    max_width: int = 480,
) -> str:
    """Скачать картинку и вернуть data URL в оптимальном формате.

    Формат результата: data:image/avif;base64,... или data:image/webp;base64,...
    или data:image/jpeg;base64,... — зависит от поддержки и фолбэка.
    """
    absolute = urljoin(page_url, src)

    # Проверяем безопасность URL (inline validation — no dependency on images._is_safe_image_url)
    try:
        parsed = urlparse(absolute)
        if parsed.scheme not in settings.allowed_schemes:
            return ""
        page_host = urlparse(page_url).hostname
        if not page_host or not parsed.hostname:
            return ""
    except Exception:
        return ""

    try:
        async with httpx.AsyncClient(
            follow_redirects=True,
            timeout=settings.request_timeout_sec,
        ) as client:
            headers: dict[str, str] = {}
            # Pikabu требует Referer
            page_host = urlparse(page_url).hostname or ""
            if page_host.endswith("pikabu.ru"):
                headers["Referer"] = "https://pikabu.ru/"

            resp = await client.get(
                absolute,
                timeout=settings.request_timeout_sec,
                headers=headers or None,
            )
            resp.raise_for_status()
            raw = resp.content[: settings.max_image_bytes]
            if not raw:
                return ""

            img_bytes, actual_fmt, w, h = optimize_image(
                raw, fmt=fmt, quality=quality, max_width=max_width
            )

            if not img_bytes:
                return ""

            mime = _FORMAT_MIME.get(actual_fmt, "image/jpeg")
            encoded = base64.b64encode(img_bytes).decode("ascii")
            return f"data:{mime};base64,{encoded}"

    except Exception as exc:
        log.warning("image_data_url_optimized failed for %s: %s", absolute, exc)
        return ""


# ──────────────────────────── Утилита: выбрать лучший профиль ────────────────────────────


def best_profile_for_accept(
    accept_header: str,
    tiny: bool = False,
) -> FormatProfile:
    """Выбрать профиль формата на основе Accept-заголовка клиента.

    Если клиент поддерживает AVIF — даём AVIF, иначе WebP, иначе JPEG.
    """
    formats = detect_supported_formats(accept_header)
    preferred = formats[0] if formats else "jpeg"

    if tiny:
        if preferred == "avif":
            return AVIF_TINY
        if preferred == "webp":
            return WEBP_TINY
        # Для JPEG формируем профиль на лету
        return FormatProfile(format="jpeg", quality=32, max_width=128, max_images=4)
    else:
        if preferred == "avif":
            return AVIF_NORMAL
        if preferred == "webp":
            return WEBP_NORMAL
        return FormatProfile(
            format="jpeg",
            quality=settings.image_jpeg_quality,
            max_width=settings.image_max_width,
            max_images=settings.max_images,
        )
