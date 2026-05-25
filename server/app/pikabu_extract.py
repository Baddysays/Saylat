"""Извлечение постов Пикабу без readability (полный текст и картинки)."""

from __future__ import annotations

import json
import re
from urllib.parse import urlparse

from bs4 import BeautifulSoup, Tag

from .models import Block

_STORY_PATH_RE = re.compile(r"/story/[^/]+_\d{5,}$", re.IGNORECASE)


def is_pikabu_url(url: str) -> bool:
    host = (urlparse(url).hostname or "").lower().removeprefix("www.")
    return host == "pikabu.ru"


def is_pikabu_story_url(url: str) -> bool:
    if not is_pikabu_url(url):
        return False
    path = urlparse(url).path.rstrip("/")
    if "/video/" in path:
        return False
    return bool(_STORY_PATH_RE.search(path))


def normalize_pikabu_story_href(href: str, base_url: str) -> str | None:
    full = href if href.startswith("http") else f"https://pikabu.ru{href}"
    if not is_pikabu_story_url(full):
        return None
    return full.split("#")[0].split("?")[0]


def _clean(text: str, limit: int = 4000) -> str:
    text = re.sub(r"[\u200b-\u200f\u2060-\u206f\ufeff]", "", text or "")
    text = re.sub(r"\s+", " ", text).strip()
    if len(text) > limit:
        return text[: limit - 1] + "…"
    return text


def _img_src(img: Tag | None) -> str:
    if not img:
        return ""
    found: list[str] = []
    for key in ("src", "data-src", "data-large-image"):
        val = (img.get(key) or "").strip()
        if val and not val.startswith("data:"):
            found.append(val)
    if not found:
        return ""
    small = [v for v in found if "_lg" not in v]
    return small[0] if small else found[-1]


def _append_image(blocks: list[Block], src: str, alt: str = "", *, index: int | None = None) -> None:
    if not src:
        return
    if any(b.type == "image" and b.src == src for b in blocks):
        return
    label = _clean(alt, 200)
    if len(label) < 2 and index is not None:
        label = f"Фото {index}"
    blocks.append(Block(type="image", src=src, alt=label))


def _append_paragraph(blocks: list[Block], text: str) -> None:
    text = _clean(text)
    if len(text) < 2:
        return
    if any(b.type == "paragraph" and b.text == text for b in blocks):
        return
    blocks.append(Block(type="paragraph", text=text))


def _parse_json_ld(soup: BeautifulSoup, blocks: list[Block]) -> None:
    for script in soup.find_all("script", type="application/ld+json"):
        raw = (script.string or "").strip()
        if not raw:
            continue
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            continue
        items = data if isinstance(data, list) else [data]
        for item in items:
            if not isinstance(item, dict):
                continue
            if item.get("@type") != "Article":
                continue
            body = item.get("articleBody") or item.get("description") or ""
            if isinstance(body, str) and len(body.strip()) > 10:
                _append_paragraph(blocks, body)
            images = item.get("image") or []
            if isinstance(images, dict):
                images = [images]
            for img in images:
                if not isinstance(img, dict):
                    continue
                src = (img.get("thumbnail") or img.get("contentUrl") or "").strip()
                name = (img.get("name") or "").strip()
                if src and "cache.php" not in src:
                    _append_image(blocks, src, name)


def _fallback_text_from_images(root: Tag, blocks: list[Block], title: str) -> None:
    if any(b.type == "paragraph" for b in blocks):
        return
    for img in root.find_all("img"):
        alt = _clean(img.get("alt") or "")
        if len(alt) >= 4 and alt != _clean(title, 200):
            _append_paragraph(blocks, alt)
            return
    tags = root.find_parent(class_=lambda c: c and "story" in " ".join(c).lower())
    if tags:
        tag_line = tags.select_one(".story__tags")
        if tag_line:
            _append_paragraph(blocks, f"Теги: {_clean(tag_line.get_text())}")


def pikabu_byline_from_soup(soup: BeautifulSoup) -> str:
    """Автор, рейтинг, комментарии — чтобы экономичный режим отличался от «пустой ленты»."""
    parts: list[str] = []
    for meta in soup.find_all("meta", property="og:description"):
        desc = _clean(meta.get("content") or "", 160)
        if desc and desc not in parts:
            parts.append(desc)
        break
    nick = soup.select_one(".story__user-info .user__nick") or soup.select_one(".story__username a")
    if nick:
        name = _clean(nick.get_text(), 80)
        if name and name not in parts:
            parts.append(name)
    rating = soup.select_one(".story__rating-count") or soup.select_one(".story__rating")
    if rating:
        r = _clean(rating.get_text(), 40)
        if r:
            parts.append(r if r.startswith(("+", "-")) else f"рейтинг {r}")
    comments = soup.select_one(".story__comments-link-count") or soup.select_one(".comments__count")
    if comments:
        c = _clean(comments.get_text(), 24)
        if c:
            parts.append(c if "комм" in c.lower() else f"{c} комм.")
    tag_line = soup.select_one(".story__tags")
    if tag_line:
        tags = _clean(tag_line.get_text(), 90)
        if tags:
            parts.append(tags)
    return " · ".join(parts[:6])


def blocks_from_pikabu_html(html: str, page_url: str) -> tuple[str, str, str, list[Block]]:
    soup = BeautifulSoup(html, "lxml")
    byline = pikabu_byline_from_soup(soup)
    title_el = soup.select_one(".story__title") or soup.select_one("h1.story__title") or soup.select_one("h1")
    title = _clean(title_el.get_text() if title_el else "", 200)

    root = (
        soup.select_one(".story__content-inner")
        or soup.select_one(".story__content")
        or soup.select_one(".story")
    )
    blocks: list[Block] = []

    _parse_json_ld(soup, blocks)

    if root:
        for node in root.select(".story-block"):
            classes = " ".join(node.get("class") or [])
            if "story-block_type_image" in classes:
                img = node.find("img")
                _append_image(blocks, _img_src(img), (img.get("alt") if img else "") or "")
                cap = node.select_one(".story-block__title, .story-image__title, figcaption")
                if cap:
                    _append_paragraph(blocks, cap.get_text())
                continue
            if "story-block_type_text" in classes or "story-block_type_html" in classes:
                for p in node.find_all("p"):
                    _append_paragraph(blocks, p.get_text())
                if not node.find_all("p"):
                    _append_paragraph(blocks, node.get_text())
                continue
            if "story-block_type_video" in classes:
                _append_paragraph(blocks, "[Видео в посте — откройте оригинал на Пикабу]")
                continue
            if "story-block_type_gallery" in classes or "story-block_type_carousel" in classes:
                for idx, img in enumerate(node.find_all("img"), start=1):
                    _append_image(
                        blocks,
                        _img_src(img),
                        (img.get("alt") if img else "") or "",
                        index=idx,
                    )
                cap = node.select_one(".story-block__title, figcaption")
                if cap:
                    _append_paragraph(blocks, cap.get_text())
                continue
            _append_paragraph(blocks, node.get_text())

        if not blocks:
            for p in root.find_all("p"):
                _append_paragraph(blocks, p.get_text())
            for img in root.find_all("img"):
                _append_image(blocks, _img_src(img), (img.get("alt") if img else "") or "")

        _fallback_text_from_images(root, blocks, title)

    if not title:
        for meta in soup.find_all("meta", property="og:title"):
            title = _clean(meta.get("content") or "", 200)
            break

    if not blocks and title:
        _append_paragraph(blocks, title)

    excerpt = next((b.text for b in blocks if b.type == "paragraph" and b.text), "")[:220]
    if not excerpt and byline:
        excerpt = byline[:220]
    return title, excerpt, byline, blocks


def blocks_after_images_off(blocks: list[Block]) -> list[Block]:
    """При images=off сохраняем смысл картинок как текст."""
    kept: list[Block] = []
    for block in blocks:
        if block.type != "image":
            kept.append(block)
            continue
        if block.alt and len(block.alt.strip()) >= 3:
            _append_paragraph(kept, block.alt)
        else:
            _append_paragraph(kept, "[Изображение в посте — включите картинки в настройках]")
    return kept
