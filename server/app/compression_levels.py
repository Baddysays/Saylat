"""Три уровня сжатия: light / medium / full."""

from __future__ import annotations

import re
from typing import Literal

from .models import ArticleLink, Block, CssHints, SaylatArticle

CompressionLevel = Literal["light", "medium", "full"]

_MEDIUM_TYPES = frozenset({"heading", "paragraph", "list", "link", "image", "quote"})
_IMPORTANT_ALT = re.compile(r"important|главн|hero|облож", re.I)


def parse_compression_level(
    query_level: str | None = None,
    header_level: str | None = None,
) -> CompressionLevel:
    raw = (query_level or header_level or "medium").strip().lower()
    if raw in ("light", "lite"):
        return "light"
    if raw == "full":
        return "full"
    return "medium"


def images_mode_for_level(level: CompressionLevel, images_param: str) -> str:
    mode = (images_param or "normal").strip().lower()
    if level == "light":
        if mode == "layout":
            return "layout"
        return "tiny"
    if level == "medium":
        if mode in ("off", "layout", "refs"):
            return mode
        return "tiny"
    if level == "full" and mode == "refs":
        return "refs"
    return mode


def apply_compression_level(article: SaylatArticle, level: CompressionLevel) -> SaylatArticle:
    if level == "light":
        return _to_light(article)
    if level == "medium":
        return _to_medium(article)
    return _to_full(article)


def collect_links_from_blocks(blocks: list[Block]) -> list[ArticleLink]:
    links: list[ArticleLink] = []
    seen: set[str] = set()

    def add(text: str, href: str | None) -> None:
        h = (href or "").strip()
        t = (text or "").strip()
        if not h.startswith(("http://", "https://")):
            return
        key = f"{h}|{t}"
        if key in seen:
            return
        seen.add(key)
        links.append(ArticleLink(text=t or h, href=h))

    for block in blocks:
        if block.type == "link" and block.href:
            add(block.text or "", block.href)
        for span in block.spans or []:
            if span.href:
                add(span.text, span.href)
    return links


def _block_plain_line(block: Block) -> str:
    if block.type == "heading" and block.text:
        return block.text.strip()
    if block.type == "quote" and block.text:
        return block.text.strip()
    if block.type == "paragraph":
        text = (block.text or "").strip()
        if not text and block.spans:
            text = " ".join((s.text or "").strip() for s in block.spans if (s.text or "").strip())
        return text
    if block.type == "list" and block.items:
        return "\n".join(item.strip() for item in block.items if item.strip())
    return ""


def _plain_text_from_blocks(blocks: list[Block]) -> str:
    parts: list[str] = []
    for block in blocks:
        line = _block_plain_line(block)
        if line:
            parts.append(line)
    return "\n\n".join(parts)


def _to_light(article: SaylatArticle) -> SaylatArticle:
    plain = _plain_text_from_blocks(article.blocks)
    if not plain.strip():
        plain = (article.excerpt or article.title or "").strip()
    links = collect_links_from_blocks(article.blocks)
    important_images: list[Block] = []
    for block in article.blocks:
        if block.type != "image" or not block.src:
            continue
        alt = (block.alt or "").strip()
        if _IMPORTANT_ALT.search(alt):
            important_images.append(
                Block(type="image", src=block.src, alt=alt, width=block.width, height=block.height)
            )
    data = article.model_dump()
    data.update(
        {
            "compression_level": "light",
            "plain_text": plain,
            "links": [link.model_dump() for link in links],
            "blocks": important_images[:2],
            "css_hints": None,
        }
    )
    out = SaylatArticle.model_validate(data)
    out.stats.payload_bytes = len(out.model_dump_json().encode("utf-8"))
    return out


def _simplify_block(block: Block) -> Block | None:
    if block.type not in _MEDIUM_TYPES:
        return None
    if block.type == "image":
        return Block(
            type="image",
            src=block.src,
            alt=block.alt,
            width=block.width,
            height=block.height,
        )
    if block.type == "link":
        return Block(type="link", text=block.text, href=block.href)
    if block.type == "list":
        return Block(type="list", items=block.items)
    if block.type == "heading":
        return Block(type="heading", text=block.text, level=block.level)
    return Block(
        type=block.type,
        text=block.text,
        href=block.href,
        items=block.items,
    )


def _to_medium(article: SaylatArticle) -> SaylatArticle:
    blocks: list[Block] = []
    image_count = 0
    for block in article.blocks:
        simple = _simplify_block(block)
        if simple is None:
            continue
        if simple.type == "image":
            if not simple.src or image_count >= 5:
                continue
            image_count += 1
        blocks.append(simple)
    data = article.model_dump()
    links = collect_links_from_blocks(blocks)
    data.update(
        {
            "compression_level": "medium",
            "blocks": [b.model_dump() for b in blocks],
            "plain_text": _plain_text_from_blocks(blocks),
            "links": [link.model_dump() for link in links[:48]],
            "css_hints": None,
        }
    )
    out = SaylatArticle.model_validate(data)
    out.stats.payload_bytes = len(out.model_dump_json().encode("utf-8"))
    return out


def _default_css_hints(article: SaylatArticle) -> CssHints:
    if article.site_profile == "pikabu":
        return CssHints(
            primary_color="#2d7a4f",
            background_color="#f4f4f4",
            body_font_size_sp=16.0,
            heading_color="#1a1a1a",
        )
    return CssHints(
        primary_color="#0F766E",
        background_color="#FFFFFF",
        body_font_size_sp=16.0,
        heading_color="#134E4A",
    )


def _to_full(article: SaylatArticle) -> SaylatArticle:
    data = article.model_dump()
    data.update(
        {
            "compression_level": "full",
            "plain_text": _plain_text_from_blocks(article.blocks),
            "links": [link.model_dump() for link in collect_links_from_blocks(article.blocks)],
            "css_hints": _default_css_hints(article).model_dump(),
        }
    )
    out = SaylatArticle.model_validate(data)
    out.stats.payload_bytes = len(out.model_dump_json().encode("utf-8"))
    return out
