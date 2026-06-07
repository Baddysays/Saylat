import re
import time
from urllib.parse import urljoin, urlparse

import bleach
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
from .compression_levels import collect_links_from_blocks
from .http_client import shared_http_client
from .http_text import decode_response_text
from .http_ua import normalize_fetch_url, ua_for_url

MIN_WORDS_V2_FALLBACK = 80


def _blocks_word_count(blocks: list[Block]) -> int:
    parts = [
        (b.text or "")
        for b in blocks
        if b.type in {"paragraph", "heading", "quote"}
    ]
    return len(re.findall(r"\w{3,}", " ".join(parts)))

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


_CONTENT_SELECTORS = (
    "article",
    "main",
    "[role='main']",
    ".entry-content",
    ".post-content",
    ".article-body",
    ".article-content",
    ".content",
    "#content",
)


def _pick_content_root(soup: BeautifulSoup) -> Tag:
    best: Tag | None = None
    best_len = 0
    for selector in _CONTENT_SELECTORS:
        for node in soup.select(selector):
            n = len(_clean_text(node.get_text()))
            if n > best_len:
                best_len = n
                best = node
    if best is not None and best_len >= 80:
        return best
    return soup.body or soup


def _inside_chrome(node: Tag) -> bool:
    for parent in node.parents:
        if isinstance(parent, Tag) and parent.name in {"nav", "aside", "footer", "header", "script", "style"}:
            return True
    return False


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
    root = _pick_content_root(soup)
    blocks: list[Block] = []
    for node in root.find_all(
        ["h1", "h2", "h3", "h4", "h5", "h6", "p", "ul", "ol", "blockquote", "img", "a"],
        recursive=True,
    ):
        if not isinstance(node, Tag) or _inside_chrome(node):
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


async def extract_article(
    url: str,
    *,
    images_mode: str = "normal",
    timeout_sec: float | None = None,
) -> SaylatArticle:
    started = time.perf_counter()
    fetch_url = normalize_fetch_url(url)
    ua = ua_for_url(fetch_url)
    timeout = timeout_sec or settings.request_timeout_sec
    client = shared_http_client()
    resp = await client.get(fetch_url, timeout=timeout, headers={"User-Agent": ua})
    resp.raise_for_status()
    html = decode_response_text(resp, max_bytes=settings.max_html_bytes)
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
        if not is_pikabu_story_url(url):
            from .extract_v2 import try_improve_blocks

            blocks = try_improve_blocks(html, url, blocks)
        excerpt = _clean_text(doc.short_title() or "")
    mode = (images_mode or "normal").strip().lower()
    wc = _blocks_word_count(blocks)
    if wc < MIN_WORDS_V2_FALLBACK and not is_pikabu_story_url(url):
        from .extract_v2 import extract_article_v2

        v2_data = await extract_article_v2(fetch_url, html=html, images_mode=mode)
        v2_blocks = [Block.model_validate(b) for b in v2_data.get("blocks", [])]
        if _blocks_word_count(v2_blocks) > wc:
            blocks = v2_blocks
            v2_title = _clean_text(str(v2_data.get("title") or ""))
            if v2_title:
                title = v2_title
            v2_excerpt = _clean_text(str(v2_data.get("excerpt") or ""))
            if v2_excerpt:
                excerpt = v2_excerpt
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

    image_count = 0
    images_omitted = 0
    if mode == "layout":
        images_omitted = sum(1 for b in blocks if b.type == "image" and b.src)
        blocks = _dedupe_blocks(blocks_layout_no_images(blocks))
    elif mode == "off":
        blocks = blocks_after_images_off(blocks) if is_pikabu_url(url) else _strip_image_blocks(blocks)
    elif mode == "refs":
        from urllib.parse import urljoin

        for block in blocks:
            if block.type != "image" or not block.src:
                continue
            absolute = urljoin(url, block.src)
            if absolute.startswith(("http://", "https://")):
                block.src = absolute
                image_count += 1
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
        links=collect_links_from_blocks(blocks),
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


async def extract_plain_fallback(url: str, *, timeout_sec: float | None = None) -> SaylatArticle:
    """Если readability не справился — отдать сырой текст страницы без картинок."""
    started = time.perf_counter()
    fetch_url = normalize_fetch_url(url)
    ua = ua_for_url(fetch_url)
    timeout = timeout_sec or settings.request_timeout_sec
    client = shared_http_client()
    resp = await client.get(fetch_url, timeout=timeout, headers={"User-Agent": ua})
    resp.raise_for_status()
    html = decode_response_text(resp, max_bytes=settings.max_html_bytes)
    original_bytes = len(resp.content)

    soup = BeautifulSoup(html, "lxml")
    for tag in soup(["script", "style", "noscript", "svg", "nav", "footer", "header"]):
        tag.decompose()
    title = _meta_title(html) or _clean_text(soup.title.string if soup.title else "") or urlparse(url).hostname or "Saylat"
    raw = _clean_text(soup.get_text(separator="\n"))
    chunks = [ln.strip() for ln in raw.splitlines() if len(ln.strip()) > 20]
    if not chunks:
        chunks = [raw[:2000]] if raw else ["Текст страницы недоступен."]
    blocks = [Block(type="paragraph", text=c[:1200]) for c in chunks[:24]]
    excerpt = chunks[0][:220] if chunks else ""
    article = SaylatArticle(
        url=fetch_url,
        title=title,
        excerpt=excerpt,
        byline="режим plain (fallback)",
        blocks=blocks,
        layout_hint="minimal",
        site_profile="generic",
        plain_text="\n\n".join(chunks[:40]),
        stats=ArticleStats(
            original_bytes=original_bytes,
            fetch_ms=int((time.perf_counter() - started) * 1000),
        ),
    )
    payload = article.model_dump_json()
    article.stats.payload_bytes = max(1, len(payload.encode("utf-8")))
    return article
