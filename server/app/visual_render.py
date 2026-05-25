"""Визуальная копия страницы: плитки текста + JPEG 1–12 КБ (Opera Mini–подобно)."""

from __future__ import annotations

import time

from .extract import extract_article
from .models import VisualPageResponse, VisualStats, VisualTile


async def build_visual_page(url: str, *, images_mode: str = "tiny") -> VisualPageResponse:
    started = time.perf_counter()
    mode = (images_mode or "tiny").strip().lower()
    if mode not in {"normal", "tiny", "off", "layout"}:
        mode = "tiny"

    article = await extract_article(url, images_mode=mode)
    tiles: list[VisualTile] = []
    image_bytes = 0

    for block in article.blocks:
        if block.type == "heading":
            tiles.append(
                VisualTile(
                    kind="heading",
                    text=block.text or "",
                    level=block.level or 2,
                )
            )
        elif block.type == "paragraph":
            text = (block.text or "").strip()
            if text:
                tiles.append(VisualTile(kind="paragraph", text=text))
        elif block.type == "quote":
            text = (block.text or "").strip()
            if text:
                tiles.append(VisualTile(kind="quote", text=text))
        elif block.type == "list" and block.items:
            tiles.append(VisualTile(kind="list", items=block.items))
        elif block.type == "divider":
            tiles.append(VisualTile(kind="divider"))
        elif block.type == "link" and block.href:
            tiles.append(
                VisualTile(
                    kind="link",
                    text=block.text or block.href,
                    href=block.href,
                )
            )
        elif block.type == "image":
            if block.src:
                approx = _approx_data_url_bytes(block.src)
                image_bytes += approx
                tiles.append(
                    VisualTile(
                        kind="image",
                        src=block.src,
                        alt=block.alt or "",
                        width=block.width,
                        height=block.height,
                        bytes_approx=approx,
                    )
                )
            else:
                tiles.append(
                    VisualTile(
                        kind="image",
                        alt=block.alt or "Изображение",
                        width=block.width,
                        height=block.height,
                        bytes_approx=0,
                    )
                )

    if not tiles:
        tiles.append(
            VisualTile(
                kind="paragraph",
                text="Страница пуста после сжатия. Попробуйте режим WebView в настройках.",
            )
        )

    payload = VisualPageResponse(
        url=article.url,
        title=article.title,
        excerpt=article.excerpt,
        lang=article.lang,
        tiles=tiles,
        structure_hint=article.layout_hint,
        stats=VisualStats(
            original_bytes=article.stats.original_bytes,
            fetch_ms=article.stats.fetch_ms,
            images_inlined=article.stats.images_inlined,
            image_bytes_approx=image_bytes,
        ),
    )
    raw = payload.model_dump_json()
    payload.stats.payload_bytes = len(raw.encode("utf-8"))
    payload.stats.build_ms = int((time.perf_counter() - started) * 1000)
    return payload


def _approx_data_url_bytes(data_url: str) -> int:
    if not data_url.startswith("data:"):
        return 0
    try:
        _, b64 = data_url.split(",", 1)
        return (len(b64) * 3) // 4
    except Exception:
        return 0
