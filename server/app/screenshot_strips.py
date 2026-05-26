"""Opera Mini–style: страница как вертикальные JPEG-полосы (без Playwright)."""

from __future__ import annotations

import base64
import io
import os
import textwrap
import time
from dataclasses import dataclass

import httpx
from PIL import Image, ImageDraw, ImageFont

from .extract import extract_article
from .images import TINY_PROFILE, fetch_image_data_url
from .models import SaylatArticle, StripPageResponse, StripSegment, StripStats

STRIP_WIDTH = 360
STRIP_SLICE_HEIGHT = 840
PAD = 12
BG = (250, 250, 248)
TEXT = (22, 22, 26)
MUTED = (110, 110, 118)
ACCENT = (15, 118, 110)


@dataclass
class _Segment:
    kind: str  # title | text | image | gap
    text: str = ""
    image: Image.Image | None = None
    height: int = 0


def _font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for path in candidates:
        if os.path.isfile(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def _wrap_lines(text: str, font: ImageFont.ImageFont, max_width: int) -> list[str]:
    dummy = Image.new("RGB", (max_width, 10))
    draw = ImageDraw.Draw(dummy)
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        trial = f"{current} {word}".strip()
        bbox = draw.textbbox((0, 0), trial, font=font)
        if bbox[2] - bbox[0] <= max_width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    if not lines:
        lines = textwrap.wrap(text, width=42) or [text[:200]]
    return lines


def _text_block_height(lines: list[str], font: ImageFont.ImageFont, line_gap: int = 4) -> int:
    dummy = Image.new("RGB", (STRIP_WIDTH, 10))
    draw = ImageDraw.Draw(dummy)
    total = 0
    for line in lines:
        bbox = draw.textbbox((0, 0), line, font=font)
        total += (bbox[3] - bbox[1]) + line_gap
    return max(total, 20)


def _draw_text_block(
    draw: ImageDraw.ImageDraw,
    y: int,
    lines: list[str],
    font: ImageFont.ImageFont,
    fill: tuple[int, int, int],
    line_gap: int = 4,
) -> int:
    cy = y
    for line in lines:
        draw.text((PAD, cy), line, font=font, fill=fill)
        bbox = draw.textbbox((PAD, cy), line, font=font)
        cy += (bbox[3] - bbox[1]) + line_gap
    return cy


def _decode_data_url(data_url: str) -> Image.Image | None:
    if not data_url or "," not in data_url:
        return None
    try:
        raw = base64.b64decode(data_url.split(",", 1)[1])
        return Image.open(io.BytesIO(raw)).convert("RGB")
    except Exception:
        return None


def _fit_image(img: Image.Image, max_w: int) -> Image.Image:
    w, h = img.size
    if w <= max_w:
        return img
    ratio = max_w / w
    return img.resize((max_w, max(1, int(h * ratio))), Image.Resampling.BILINEAR)


async def _article_to_segments(article: SaylatArticle, client: httpx.AsyncClient) -> list[_Segment]:
    segments: list[_Segment] = []
    title_font = _font(19, bold=True)
    body_font = _font(15)
    meta_font = _font(13)

    title_lines = _wrap_lines(article.title or "Страница", title_font, STRIP_WIDTH - PAD * 2)
    segments.append(
        _Segment(
            kind="title",
            text=article.title,
            height=_text_block_height(title_lines, title_font) + PAD,
        )
    )
    if article.byline:
        meta_lines = _wrap_lines(article.byline, meta_font, STRIP_WIDTH - PAD * 2)
        segments.append(
            _Segment(
                kind="text",
                text=article.byline,
                height=_text_block_height(meta_lines, meta_font) + 6,
            )
        )

    for block in article.blocks:
        if block.type == "heading" and block.text:
            font = _font(17, bold=True)
            lines = _wrap_lines(block.text, font, STRIP_WIDTH - PAD * 2)
            segments.append(_Segment(kind="text", text=block.text, height=_text_block_height(lines, font) + 8))
        elif block.type == "paragraph" and block.text:
            lines = _wrap_lines(block.text, body_font, STRIP_WIDTH - PAD * 2)
            segments.append(_Segment(kind="text", text=block.text, height=_text_block_height(lines, body_font) + 8))
        elif block.type == "image":
            img: Image.Image | None = None
            if block.src and block.src.startswith("data:image"):
                img = _decode_data_url(block.src)
            elif block.src:
                data_url, _, _ = await fetch_image_data_url(
                    client, block.src, article.url, profile=TINY_PROFILE
                )
                if data_url:
                    img = _decode_data_url(data_url)
            if img:
                fitted = _fit_image(img, STRIP_WIDTH - PAD * 2)
                segments.append(_Segment(kind="image", image=fitted, height=fitted.size[1] + 10))
            elif block.alt:
                lines = _wrap_lines(f"🖼 {block.alt}", body_font, STRIP_WIDTH - PAD * 2)
                segments.append(
                    _Segment(kind="text", text=block.alt, height=_text_block_height(lines, body_font) + 40)
                )
        elif block.type == "quote" and block.text:
            lines = _wrap_lines(block.text, body_font, STRIP_WIDTH - PAD * 2)
            segments.append(_Segment(kind="text", text=block.text, height=_text_block_height(lines, body_font) + 16))

    if len(segments) <= 1:
        lines = _wrap_lines(
            "Мало контента для полос. Попробуйте «Как на сайте» или другой URL.",
            body_font,
            STRIP_WIDTH - PAD * 2,
        )
        segments.append(_Segment(kind="text", text="hint", height=_text_block_height(lines, body_font) + 20))
    return segments


def _pack_strips(segments: list[_Segment]) -> list[list[_Segment]]:
    packs: list[list[_Segment]] = []
    current: list[_Segment] = []
    used = 0
    for seg in segments:
        h = seg.height
        if current and used + h > STRIP_SLICE_HEIGHT:
            packs.append(current)
            current = []
            used = 0
        current.append(seg)
        used += h
    if current:
        packs.append(current)
    return packs or [segments[:1]]


def _render_strip(pack: list[_Segment], article: SaylatArticle) -> Image.Image:
    total_h = sum(s.height for s in pack) + PAD * 2
    canvas = Image.new("RGB", (STRIP_WIDTH, max(total_h, 120)), BG)
    draw = ImageDraw.Draw(canvas)
    title_font = _font(19, bold=True)
    body_font = _font(15)
    meta_font = _font(13)
    y = PAD
    title_drawn = False

    for seg in pack:
        if seg.kind == "title" and not title_drawn:
            lines = _wrap_lines(article.title or "Страница", title_font, STRIP_WIDTH - PAD * 2)
            y = _draw_text_block(draw, y, lines, title_font, TEXT) + 6
            title_drawn = True
        elif seg.kind == "text":
            font = meta_font if seg.text == article.byline else body_font
            lines = _wrap_lines(seg.text, font, STRIP_WIDTH - PAD * 2)
            y = _draw_text_block(draw, y, lines, font, TEXT if font != meta_font else MUTED) + 4
        elif seg.kind == "image" and seg.image:
            canvas.paste(seg.image, (PAD, y))
            y += seg.image.size[1] + 10
    return canvas


def _jpeg_data_url(img: Image.Image, quality: int = 48) -> tuple[str, int]:
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=quality, optimize=True)
    raw = buf.getvalue()
    b64 = base64.b64encode(raw).decode("ascii")
    return f"data:image/jpeg;base64,{b64}", len(raw)


async def build_pillow_strip_page(url: str, *, images_mode: str = "tiny") -> StripPageResponse:
    started = time.perf_counter()
    mode = images_mode if images_mode in ("tiny", "layout", "off", "normal") else "tiny"
    article = await extract_article(url, images_mode=mode)
    strips_out: list[StripSegment] = []
    total_bytes = 0

    async with httpx.AsyncClient(follow_redirects=True) as client:
        segments = await _article_to_segments(article, client)
        for idx, pack in enumerate(_pack_strips(segments)):
            img = _render_strip(pack, article)
            src, nbytes = _jpeg_data_url(img)
            total_bytes += nbytes
            strips_out.append(
                StripSegment(
                    index=idx,
                    src=src,
                    width=img.size[0],
                    height=img.size[1],
                    bytes_approx=nbytes,
                )
            )

    fetch_ms = int((time.perf_counter() - started) * 1000)
    return StripPageResponse(
        url=article.url,
        title=article.title,
        site_profile=article.site_profile,
        strips=strips_out,
        links=article.links,
        strip_width=STRIP_WIDTH,
        render_engine="pillow",
        stats=StripStats(
            original_bytes=article.stats.original_bytes,
            payload_bytes=total_bytes,
            strip_count=len(strips_out),
            fetch_ms=fetch_ms,
            build_ms=fetch_ms,
        ),
    )


async def build_strip_page(
    url: str,
    *,
    images_mode: str = "tiny",
    engine: str = "auto",
) -> StripPageResponse:
    """
    engine: auto | browser | pillow
    auto/browser — Playwright-скриншот; при сбое — Pillow (extract).
    """
    mode = (engine or "auto").strip().lower()
    if mode == "pillow":
        return await build_pillow_strip_page(url, images_mode=images_mode)

    from .browser_strips import BrowserStripsError, build_browser_strip_page, playwright_available

    if playwright_available() and mode in ("browser", "auto"):
        try:
            article = await extract_article(url, images_mode=images_mode)
            return await build_browser_strip_page(
                url,
                original_bytes_hint=article.stats.original_bytes,
                site_profile=article.site_profile,
                links=article.links,
            )
        except BrowserStripsError:
            if mode == "browser":
                pillow = await build_pillow_strip_page(url, images_mode=images_mode)
                return pillow.model_copy(update={"render_engine": "browser_fallback_pillow"})
    elif mode == "browser":
        raise BrowserStripsError("Playwright недоступен на сервере")

    return await build_pillow_strip_page(url, images_mode=images_mode)
