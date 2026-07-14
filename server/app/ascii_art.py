"""Конвертация изображений в ASCII-арт для экономии трафика на 2G/EDGE.

Полное изображение: 5–50 КБ. ASCII-арт: 200–500 байт — экономия ~99%.
Клиент рендерит в моноширинном шрифте, ~60 символов в ширину на телефоне.
"""

import base64
import io
from typing import Literal

from PIL import Image
from pydantic import BaseModel

from .models import Block, SaylatArticle

# ── Наборы символов ──────────────────────────────────────────────────────────

ASCII_CHARS = " .:-=+*#%@"

BLOCK_CHARS = " ░▒▓█"

# Braille: U+2800–U+28FF, каждый символ кодирует 2×4 точки
# Порядок точек (столбец, строка) → бит:
#   (0,0)=0x01  (1,0)=0x08
#   (0,1)=0x02  (1,1)=0x10
#   (0,2)=0x04  (1,2)=0x20
#   (0,3)=0x40  (1,3)=0x80
_BRAILLE_OFFSETS = [
    (0, 0, 0x01), (0, 1, 0x02), (0, 2, 0x04), (0, 3, 0x40),
    (1, 0, 0x08), (1, 1, 0x10), (1, 2, 0x20), (1, 3, 0x80),
]

Style = Literal["standard", "blocks", "braille"]

# Порог яркости для Braille (0–255): пиксель темнее → точка ставится
_BRAILLE_THRESHOLD = 128


# ── Модель данных ────────────────────────────────────────────────────────────

class AsciiBlockData(BaseModel):
    """Структурированный результат ASCII-конвертации."""
    text: str
    width: int
    height: int          # в строках
    colored: list[list[tuple[str, str]]] | None = None  # [row][col] = (char, hex_color)
    format: str          # "ascii" | "blocks" | "braille"
    original_bytes: int
    ascii_bytes: int


# ── Вспомогательные функции ──────────────────────────────────────────────────

def _open_and_prepare(image_bytes: bytes, width: int, aspect: float = 0.5) -> Image.Image:
    """Открыть изображение, изменить размер и перевести в градации серого."""
    img = Image.open(io.BytesIO(image_bytes))
    if img.mode not in ("RGB", "RGBA", "L", "LA", "P"):
        img = img.convert("RGBA")

    orig_w, orig_h = img.size
    # Масштабируем высоту с поправкой на пропорции символа (символ выше, чем шире)
    new_w = max(1, min(width, orig_w))
    new_h = max(1, int(orig_h * (new_w / orig_w) * aspect))
    img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)

    return img


def _pixel_brightness(img: Image.Image, x: int, y: int) -> int:
    """Яркость пикселя 0–255 (0 = чёрный, 255 = белый)."""
    pixel = img.getpixel((x, y))
    if isinstance(pixel, (int, float)):
        return int(pixel)
    # RGB / RGBA — взвешенная сумма
    r, g, b = pixel[:3]
    return int(0.299 * r + 0.587 * g + 0.114 * b)


def _pixel_color(img: Image.Image, x: int, y: int) -> str:
    """Hex-цвет пикселя (#rrggbb)."""
    pixel = img.getpixel((x, y))
    if isinstance(pixel, (int, float)):
        v = int(pixel) & 0xFF
        return f"#{v:02x}{v:02x}{v:02x}"
    r, g, b = pixel[:3]
    return f"#{r:02x}{g:02x}{b:02x}"


# ── Основные функции конвертации ─────────────────────────────────────────────

def image_to_ascii(image_bytes: bytes, width: int = 60, style: str = "standard") -> str:
    """Конвертировать изображение в ASCII-арт.

    Args:
        image_bytes: сырые байты изображения (JPEG/PNG/…)
        width: ширина в символах (60 ≈ экран маленького телефона)
        style: "standard" — ASCII, "blocks" — Unicode block, "braille" — Braille

    Returns:
        Многострочная строка с ASCII-артом.
    """
    if style == "braille":
        return image_to_braille(image_bytes, width=width)

    chars = ASCII_CHARS if style == "standard" else BLOCK_CHARS
    img = _open_and_prepare(image_bytes, width, aspect=0.5)
    gray = img.convert("L")

    w, h = gray.size
    n = len(chars) - 1
    lines: list[str] = []
    for y in range(h):
        row: list[str] = []
        for x in range(w):
            brightness = _pixel_brightness(gray, x, y)
            # brightness 0 = чёрный → последний символ (самый плотный)
            idx = int((1.0 - brightness / 255.0) * n)
            idx = max(0, min(n, idx))
            row.append(chars[idx])
        lines.append("".join(row))
    return "\n".join(lines)


def image_to_braille(image_bytes: bytes, width: int = 40) -> str:
    """Конвертировать изображение в Braille-арт (2× разрешение по горизонтали).

    Каждый символ Braille = 2×4 пикселя, поэтому при width=40
    реальная ширина изображения — 80 пикселей.
    """
    # Braille-символ покрывает 2 столбца и 4 строки → реальная ширина = width * 2
    pixel_w = width * 2
    img = _open_and_prepare(image_bytes, pixel_w, aspect=0.5)
    gray = img.convert("L")

    w, h = gray.size
    # Размеры в Braille-символах
    braille_cols = w // 2
    braille_rows = h // 4

    lines: list[str] = []
    for br in range(braille_rows):
        row: list[str] = []
        for bc in range(braille_cols):
            code = 0x2800
            for col_off, row_off, bit in _BRAILLE_OFFSETS:
                px = bc * 2 + col_off
                py = br * 4 + row_off
                if px < w and py < h:
                    brightness = _pixel_brightness(gray, px, py)
                    if brightness < _BRAILLE_THRESHOLD:
                        code |= bit
            row.append(chr(code))
        lines.append("".join(row))
    return "\n".join(lines)


# ── Цветной ASCII ────────────────────────────────────────────────────────────

def image_to_colored_ascii(
    image_bytes: bytes,
    width: int = 60,
    style: str = "standard",
) -> list[tuple[str, str]]:
    """Конвертировать изображение в цветной ASCII-арт.

    Returns:
        Список (символ, hex_цвет) слева направо, сверху вниз.
        Клиент может рендерить как:
            <span style="color:#ff0000">@</span>
    """
    if style == "braille":
        # Для Braille нет смысла красить каждый символ —
        # используем средний цвет блока 2×4
        return _colored_braille(image_bytes, width)

    chars = ASCII_CHARS if style == "standard" else BLOCK_CHARS
    img = _open_and_prepare(image_bytes, width, aspect=0.5)
    gray = img.convert("L")

    w, h = gray.size
    n = len(chars) - 1
    result: list[tuple[str, str]] = []
    for y in range(h):
        for x in range(w):
            brightness = _pixel_brightness(gray, x, y)
            idx = int((1.0 - brightness / 255.0) * n)
            idx = max(0, min(n, idx))
            color = _pixel_color(img, x, y)
            result.append((chars[idx], color))
    return result


def _colored_braille(image_bytes: bytes, width: int = 40) -> list[tuple[str, str]]:
    """Цветной Braille — средний цвет блока 2×4."""
    pixel_w = width * 2
    img = _open_and_prepare(image_bytes, pixel_w, aspect=0.5)
    gray = img.convert("L")

    w, h = gray.size
    braille_cols = w // 2
    braille_rows = h // 4

    result: list[tuple[str, str]] = []
    for br in range(braille_rows):
        for bc in range(braille_cols):
            code = 0x2800
            r_sum, g_sum, b_sum, count = 0, 0, 0, 0
            for col_off, row_off, bit in _BRAILLE_OFFSETS:
                px = bc * 2 + col_off
                py = br * 4 + row_off
                if px < w and py < h:
                    brightness = _pixel_brightness(gray, px, py)
                    if brightness < _BRAILLE_THRESHOLD:
                        code |= bit
                    pixel = img.getpixel((px, py))
                    if isinstance(pixel, (int, float)):
                        v = int(pixel) & 0xFF
                        r_sum += v; g_sum += v; b_sum += v
                    else:
                        r_sum += pixel[0]; g_sum += pixel[1]; b_sum += pixel[2]
                    count += 1
            if count > 0:
                r_avg = r_sum // count
                g_avg = g_sum // count
                b_avg = b_sum // count
                color = f"#{r_avg:02x}{g_avg:02x}{b_avg:02x}"
            else:
                color = "#000000"
            result.append((chr(code), color))
    return result


# ── Структурированный вывод ──────────────────────────────────────────────────

def image_to_block_data(
    image_bytes: bytes,
    width: int = 40,
    style: str = "standard",
) -> AsciiBlockData:
    """Конвертировать изображение и вернуть структурированные данные."""
    fmt = style if style in ("ascii", "blocks", "braille") else "ascii"
    actual_style = style if style != "braille" else "braille"

    text = image_to_ascii(image_bytes, width=width, style=actual_style)
    colored_raw = image_to_colored_ascii(image_bytes, width=width, style=actual_style)

    # Разбиваем colored на строки по ширине
    text_lines = text.split("\n")
    text_height = len(text_lines)
    char_width = width
    # Для Braille реальная ширина текста = width символов
    if actual_style == "braille":
        char_width = width

    colored: list[list[tuple[str, str]]] = []
    idx = 0
    for _ in range(text_height):
        row_len = len(text_lines[_]) if _ < len(text_lines) else char_width
        row: list[tuple[str, str]] = []
        for __ in range(row_len):
            if idx < len(colored_raw):
                row.append(colored_raw[idx])
                idx += 1
            else:
                row.append((" ", "#000000"))
        colored.append(row)

    ascii_bytes = len(text.encode("utf-8"))

    return AsciiBlockData(
        text=text,
        width=char_width,
        height=text_height,
        colored=colored,
        format=fmt,
        original_bytes=len(image_bytes),
        ascii_bytes=ascii_bytes,
    )


# ── Data URL ─────────────────────────────────────────────────────────────────

def ascii_data_url(
    image_bytes: bytes,
    width: int = 60,
    style: str = "standard",
) -> str:
    """Конвертировать изображение в data URL с ASCII-артом.

    Возвращает: data:text/plain;charset=utf-8;base64,...
    """
    text = image_to_ascii(image_bytes, width=width, style=style)
    encoded = base64.b64encode(text.encode("utf-8")).decode("ascii")
    return f"data:text/plain;charset=utf-8;base64,{encoded}"


# ── Интеграция со статьёй ────────────────────────────────────────────────────

def apply_ascii_to_article(
    article: SaylatArticle,
    width: int = 60,
    style: str = "standard",
) -> SaylatArticle:
    """Заменить изображения в статье на ASCII-арт.

    Для каждого image-блока:
      - Если src — data URL (уже загружен), конвертируем в ASCII
      - Если src — обычный URL, оставляем как есть
      - Заменяем block.src на ASCII data URL
      - Добавляем префикс "[ASCII] " к block.alt
      - Устанавливаем width/height в символьные размеры

    Returns:
        Модифицированная статья (новый экземпляр).
    """
    blocks: list[Block] = []
    for block in article.blocks:
        if block.type != "image" or not block.src:
            blocks.append(block.model_copy())
            continue

        src = block.src

        # Только data URL (уже загруженные изображения) конвертируем
        if not src.startswith("data:"):
            blocks.append(block.model_copy())
            continue

        # Извлекаем байты из data URL
        try:
            image_bytes = _extract_bytes_from_data_url(src)
        except ValueError:
            # Не удалось разобрать data URL — пропускаем
            blocks.append(block.model_copy())
            continue

        # Конвертируем
        try:
            ascii_url = ascii_data_url(image_bytes, width=width, style=style)
        except Exception:
            # Ошибка конвертации — оставляем оригинал
            blocks.append(block.model_copy())
            continue

        # Определяем размеры в символах
        try:
            text = image_to_ascii(image_bytes, width=width, style=style)
            text_height = text.count("\n") + 1
        except Exception:
            text_height = 0

        new_alt = block.alt or ""
        if not new_alt.startswith("[ASCII]"):
            new_alt = f"[ASCII] {new_alt}".strip()

        blocks.append(
            Block(
                type="image",
                src=ascii_url,
                alt=new_alt or "[ASCII]",
                width=width,
                height=text_height,
            )
        )

    # Обновляем статистику трафика
    new_payload = article.model_copy(update={"blocks": blocks})
    payload_json = new_payload.model_dump_json()
    new_payload.stats.payload_bytes = max(1, len(payload_json.encode("utf-8")))
    return new_payload


def _extract_bytes_from_data_url(data_url: str) -> bytes:
    """Извлечь байты из data URL (base64)."""
    # Формат: data:<mime>;base64,<payload>
    if ";base64," not in data_url:
        raise ValueError("Не base64 data URL")
    _, encoded = data_url.split(";base64,", 1)
    return base64.b64decode(encoded)
