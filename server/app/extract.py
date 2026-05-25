import re
import time
from urllib.parse import urljoin, urlparse

import bleach
import httpx
from bs4 import BeautifulSoup, NavigableString, Tag
from readability import Document

from .config import settings
from .images import NORMAL_PROFILE, TINY_PROFILE, fetch_image_data_url
from .models import ArticleStats, Block, LayoutHint, SaylatArticle, TextSpan
from .pikabu_extract import (
    blocks_after_images_off,
    blocks_from_pikabu_html,
    is_pikabu_story_url,
    is_pikabu_url,
)
from .http_ua import normalize_fetch_url, ua_for_url

ALLOWED_TAGS = {
    "p",
    "br",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "ul",
    "ol",
    "li",
    "blockquote",
    "img",
    "strong",
    "em",
    "a",
    "div",
    "span",
}
ALLOWED_ATTRS = {
    "img": ["src", "alt", "width", "height", "data-src", "data-large-image"],
    "a": ["href"],
}


def _clean_text(text: str) -> str:
    text = bleach.clean(text, tags=[], strip=True)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def _resolve_href(href: str, base_url: str) -> str:
    href = (href or "").strip()
    if not href or href.startswith("#"):
        return ""
    if href.startswith(("http://", "https://", "mailto:", "tel:")):
        return href
    return urljoin(base_url, href)


def _node_to_spans(node: Tag, base_url: str) -> list[TextSpan]:
    spans: list[TextSpan] = []
    for child in node.children:
        if isinstance(child, NavigableString):
            t = _clean_text(str(child))
            if t:
                spans.append(TextSpan(text=t))
        elif isinstance(child, Tag):
            if child.name == "a":
                href = _resolve_href(child.get("href") or "", base_url)
                label = _clean_text(child.get_text())
                if label and href:
                    spans.append(TextSpan(text=label, href=href))
                elif label:
                    spans.append(TextSpan(text=label))
            elif child.name in {"strong", "em", "span", "b", "i"}:
                spans.extend(_node_to_spans(child, base_url))
            else:
                t = _clean_text(child.get_text())
                if t:
                    spans.append(TextSpan(text=t))
    return spans


def _spans_plain(spans: list[TextSpan]) -> str:
    return "".join(s.text for s in spans)


def _meta_title(html: str) -> str:
    soup = BeautifulSoup(html, "lxml")
    for selector in (
        ("meta", {"property": "og:title"}),
        ("meta", {"name": "twitter:title"}),
        ("meta", {"name": "title"}),
    ):
        tag = soup.find(selector[0], attrs=selector[1])
        if tag and tag.get("content"):
            title = _clean_text(tag["content"])
            if title:
                return title
    if soup.title and soup.title.string:
        return _clean_text(soup.title.string)
    return ""


def _guess_layout(blocks: list[Block]) -> LayoutHint:
    images = sum(1 for b in blocks if b.type == "image")
    headings = sum(1 for b in blocks if b.type == "heading")
    paragraphs = sum(1 for b in blocks if b.type == "paragraph")
    if images >= 4 and paragraphs <= images:
        return "gallery"
    if headings >= 6 and paragraphs <= headings * 2:
        return "feed"
    if paragraphs <= 2 and images == 0:
        return "minimal"
    return "article"


def _page_lang(html: str) -> str:
    soup = BeautifulSoup(html, "lxml")
    html_tag = soup.find("html")
    if html_tag and html_tag.get("lang"):
        return _clean_text(html_tag["lang"]).split("-")[0].lower()[:8]
    meta = soup.find("meta", attrs={"http-equiv": "content-language"})
    if meta and meta.get("content"):
        return _clean_text(meta["content"]).split("-")[0].lower()[:8]
    return ""


def _html_to_blocks(soup: BeautifulSoup, page_url: str) -> list[Block]:
    blocks: list[Block] = []
    for node in soup.find_all(
        ["h1", "h2", "h3", "h4", "h5", "h6", "p", "ul", "ol", "blockquote", "img", "a"],
        recursive=True,
    ):
        if not isinstance(node, Tag):
            continue
        name = node.name
        if name in {"h1", "h2", "h3", "h4", "h5", "h6"}:
            spans = _node_to_spans(node, page_url)
            text = _spans_plain(spans) if spans else _clean_text(node.get_text())
            if text:
                blocks.append(
                    Block(
                        type="heading",
                        text=text,
                        level=int(name[1]),
                        spans=spans if any(s.href for s in spans) else None,
                    )
                )
        elif name == "p":
            spans = _node_to_spans(node, page_url)
            text = _spans_plain(spans) if spans else _clean_text(node.get_text())
            if len(text) >= 2:
                blocks.append(
                    Block(
                        type="paragraph",
                        text=text,
                        spans=spans if any(s.href for s in spans) else None,
                    )
                )
        elif name == "a" and node.parent and node.parent.name not in {"p", "h1", "h2", "h3", "h4", "h5", "h6"}:
            href = _resolve_href(node.get("href") or "", page_url)
            label = _clean_text(node.get_text())
            if href and label:
                blocks.append(Block(type="link", text=label, href=href))
        elif name in {"ul", "ol"}:
            items = [
                _clean_text(li.get_text())
                for li in node.find_all("li", recursive=False)
            ]
            items = [i for i in items if i]
            if items:
                blocks.append(Block(type="list", items=items[:24]))
        elif name == "blockquote":
            text = _clean_text(node.get_text())
            if text:
                blocks.append(Block(type="quote", text=text))
        elif name == "img":
            src = (
                node.get("data-large-image")
                or node.get("data-src")
                or node.get("src")
                or ""
            ).strip()
            alt = _clean_text(node.get("alt") or "")
            if src and not src.startswith("data:"):
                blocks.append(Block(type="image", src=src, alt=alt))
    return blocks


def _strip_image_blocks(blocks: list[Block]) -> list[Block]:
    return [b for b in blocks if b.type != "image"]


def _dedupe_blocks(blocks: list[Block]) -> list[Block]:
    """Убираем подряд одинаковый текст (Пикабу: alt картинки = абзац)."""
    out: list[Block] = []
    prev_key = ""
    for block in blocks:
        key = ""
        if block.type == "paragraph":
            key = (block.text or "").strip()
        elif block.type == "image":
            key = (block.alt or "").strip()
        if block.type == "paragraph" and key and key == prev_key:
            continue
        out.append(block)
        if key:
            prev_key = key
    return out


def blocks_layout_no_images(blocks: list[Block]) -> list[Block]:
    """Макет сохранён: блоки image остаются как плейсхолдеры без загрузки JPEG."""
    result: list[Block] = []
    for block in blocks:
        if block.type != "image":
            result.append(block)
            continue
        alt = (block.alt or "").strip()
        label = alt if len(alt) >= 2 else "Изображение"
        result.append(
            Block(
                type="image",
                src=None,
                alt=label,
                width=block.width,
                height=block.height,
            )
        )
    return result


async def extract_article(url: str, *, images_mode: str = "normal") -> SaylatArticle:
    started = time.perf_counter()
    fetch_url = normalize_fetch_url(url)
    ua = ua_for_url(fetch_url)
    async with httpx.AsyncClient(
        follow_redirects=True,
        headers={"User-Agent": ua},
    ) as client:
        resp = await client.get(fetch_url, timeout=settings.request_timeout_sec)
        resp.raise_for_status()
        html = resp.text[: settings.max_html_bytes]
        original_bytes = len(resp.content)
        page_lang = _page_lang(html)

        excerpt = ""
        byline = ""
        site_profile = "generic"
        if is_pikabu_story_url(url):
            title, excerpt, byline, blocks = blocks_from_pikabu_html(html, url)
            site_profile = "pikabu"
        else:
            doc = Document(html)
            title = _clean_text(doc.title() or "") or _meta_title(html)
            summary_html = doc.summary()
            cleaned = bleach.clean(
                summary_html,
                tags=ALLOWED_TAGS,
                attributes=ALLOWED_ATTRS,
                strip=True,
            )
            soup = BeautifulSoup(cleaned, "lxml")
            blocks = _html_to_blocks(soup, url)
            excerpt = _clean_text(doc.short_title() or "")
        if not blocks:
            host = urlparse(url).hostname or url
            blocks.append(
                Block(
                    type="paragraph",
                    text=(
                        f"Не удалось извлечь текст с {host}. "
                        "Сайт может отдавать пустую оболочку (SPA) или блокировать прокси."
                    ),
                )
            )

        mode = (images_mode or "normal").strip().lower()
        image_count = 0
        images_omitted = 0
        if mode == "layout":
            images_omitted = sum(1 for b in blocks if b.type == "image" and b.src)
            blocks = _dedupe_blocks(blocks_layout_no_images(blocks))
        elif mode == "off":
            blocks = blocks_after_images_off(blocks) if is_pikabu_url(url) else _strip_image_blocks(blocks)
        else:
            profile = TINY_PROFILE if mode == "tiny" else NORMAL_PROFILE
            for block in blocks:
                if block.type != "image" or not block.src or image_count >= profile.max_images:
                    continue
                data_url, w, h = await fetch_image_data_url(
                    client, block.src, url, profile=profile
                )
                if data_url:
                    block.src = data_url
                    block.width = w
                    block.height = h
                    image_count += 1
        if not excerpt and blocks:
            first_p = next((b.text for b in blocks if b.type == "paragraph" and b.text), "")
            excerpt = first_p[:220]

        layout_hint = _guess_layout(blocks)
        if mode == "layout" and sum(1 for b in blocks if b.type == "image") >= 2:
            layout_hint = "gallery"

        article = SaylatArticle(
            url=fetch_url,
            title=title or urlparse(url).hostname or "Saylat",
            excerpt=excerpt,
            byline=byline,
            lang=page_lang,
            blocks=blocks,
            layout_hint=layout_hint,
            site_profile=site_profile,
            stats=ArticleStats(
                original_bytes=original_bytes,
                fetch_ms=int((time.perf_counter() - started) * 1000),
                images_omitted=images_omitted,
            ),
        )
        payload = article.model_dump_json()
        article.stats.payload_bytes = max(1, len(payload.encode("utf-8")))
        article.stats.images_inlined = image_count
        return article
