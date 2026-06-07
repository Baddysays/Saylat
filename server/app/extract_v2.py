"""
extract_v2.py — улучшенный экстрактор с readability-lxml fallback.

Алгоритм:
1. Попытка через BeautifulSoup (оригинальный extract.py)
2. Если результат «слабый» (< MIN_WORDS слов) — fallback на readability-lxml
3. Если и он слабый — возвращаем что есть, не пустой ответ

Установить: pip install readability-lxml
"""
from __future__ import annotations

import logging
import re
from typing import Any

import httpx
from bs4 import BeautifulSoup, Tag

log = logging.getLogger(__name__)

MIN_WORDS_PRIMARY = 80   # ниже — пробуем readability
MIN_WORDS_FALLBACK = 20  # ниже — лог предупреждения

# CSS-классы и атрибуты контентных контейнеров (порядок = приоритет)
_CONTENT_SELECTORS = [
    "article",
    "main",
    "[itemprop='articleBody']",
    "[class*='article-body']",
    "[class*='article_body']",
    "[class*='post-content']",
    "[class*='post_content']",
    "[class*='entry-content']",
    "[class*='entry_content']",
    "[class*='content-body']",
    "[class*='content_body']",
    "[class*='story-body']",
    "[class*='text-body']",
    "[class*='article-text']",
    "[class*='publication-body']",
    "div[class*='content']:not(header):not(footer):not(nav)",
    ".post",
    ".article",
    ".entry",
]

_JUNK_TAGS = {"script", "style", "noscript", "svg", "nav", "footer",
               "header", "aside", "form", "figure", "figcaption"}
_JUNK_CLASSES = re.compile(
    r"comment|footer|sidebar|nav|menu|banner|advert|social|share|related|"
    r"recommend|subscribe|popup|cookie|promo|sponsor|widget", re.I
)


def _count_words(text: str) -> int:
    return len(re.findall(r"\w{3,}", text))


def _clean_soup(container: Tag) -> list[dict[str, Any]]:
    """Преобразовать HTML-контейнер в список блоков."""
    for tag in container(list(_JUNK_TAGS)):
        tag.decompose()
    for tag in container.find_all(True):
        cls = " ".join(tag.get("class", []))
        if _JUNK_CLASSES.search(cls):
            tag.decompose()

    blocks: list[dict[str, Any]] = []
    seen: set[str] = set()

    def add_text(t: str, btype: str = "paragraph", **kw: Any) -> None:
        t = re.sub(r"\s+", " ", t).strip()
        if len(t) < 10 or t in seen:
            return
        seen.add(t)
        blocks.append({"type": btype, "text": t, **kw})

    for el in container.descendants:
        if not isinstance(el, Tag):
            continue
        name = el.name
        if name in {"h1", "h2", "h3", "h4"}:
            add_text(el.get_text(), "heading",
                     level=int(name[1]) if name[1].isdigit() else 2)
        elif name == "p":
            add_text(el.get_text(), "paragraph")
        elif name in {"ul", "ol"}:
            items = [li.get_text().strip() for li in el.find_all("li", recursive=False)]
            items = [i for i in items if len(i) > 3]
            if items:
                blocks.append({"type": "list", "items": items})
        elif name == "blockquote":
            add_text(el.get_text(), "quote")
        elif name == "a" and el.get("href", "").startswith("http"):
            href = el["href"]
            text = el.get_text().strip() or href
            if href not in seen:
                seen.add(href)
                blocks.append({"type": "link", "text": text, "href": href})
        elif name == "img":
            src = el.get("src", "") or el.get("data-src", "")
            if src and src.startswith("http"):
                blocks.append({"type": "image", "src": src,
                                "alt": el.get("alt", "")})

    return blocks


def _readability_extract(html: str, url: str) -> tuple[str, str, list[dict]]:
    """Fallback через readability-lxml."""
    try:
        from readability import Document  # type: ignore
        doc = Document(html)
        title = doc.title() or ""
        content_html = doc.summary(html_partial=True)
        soup = BeautifulSoup(content_html, "lxml")
        blocks = _clean_soup(soup)
        return title, "", blocks
    except ImportError:
        log.warning("readability-lxml not installed; pip install readability-lxml")
        return "", "", []
    except Exception as exc:
        log.warning("readability fallback failed: %s", exc)
        return "", "", []


def _div_heuristic(soup: BeautifulSoup) -> Tag | None:
    """Найти самый текстонасыщенный div если стандартные селекторы не помогли."""
    candidates: list[tuple[int, Tag]] = []
    for div in soup.find_all("div"):
        cls = " ".join(div.get("class", []))
        if _JUNK_CLASSES.search(cls):
            continue
        p_count = len(div.find_all("p", recursive=True))
        text_len = len(div.get_text())
        if p_count >= 2 and text_len > 300:
            candidates.append((text_len, div))
    if not candidates:
        return None
    candidates.sort(key=lambda x: x[0], reverse=True)
    return candidates[0][1]


async def extract_article_v2(
    url: str,
    html: str | None = None,
    images_mode: str = "normal",
) -> dict[str, Any]:
    """
    Основная точка входа — возвращает dict совместимый с SaylatArticle.
    Если html=None, скачивает сам.
    """
    if html is None:
        async with httpx.AsyncClient(
            follow_redirects=True,
            timeout=30,
            headers={"User-Agent": "Mozilla/5.0 (compatible; Saylat/1.0)"},
        ) as client:
            resp = await client.get(url)
            resp.raise_for_status()
            from .http_text import decode_response_text

            html = decode_response_text(resp)

    soup = BeautifulSoup(html, "lxml")
    title = (soup.find("title") or soup.find("h1") or Tag(name="")).get_text().strip()
    if not title:
        title = url

    # Мета-описание
    excerpt = ""
    meta = soup.find("meta", attrs={"name": "description"}) or \
           soup.find("meta", attrs={"property": "og:description"})
    if meta:
        excerpt = meta.get("content", "").strip()[:300]

    # 1. Стандартные CSS-селекторы
    container: Tag | None = None
    for sel in _CONTENT_SELECTORS:
        found = soup.select_one(sel)
        if found:
            container = found
            break

    blocks: list[dict] = []
    if container:
        blocks = _clean_soup(container)

    word_count = _count_words(" ".join(b.get("text", "") for b in blocks))

    # 2. Heuristic div fallback
    if word_count < MIN_WORDS_PRIMARY:
        log.debug("Primary extract weak (%d words), trying heuristic div", word_count)
        heuristic = _div_heuristic(soup)
        if heuristic:
            heuristic_blocks = _clean_soup(heuristic)
            heuristic_words = _count_words(
                " ".join(b.get("text", "") for b in heuristic_blocks)
            )
            if heuristic_words > word_count:
                blocks = heuristic_blocks
                word_count = heuristic_words

    # 3. readability-lxml fallback
    if word_count < MIN_WORDS_PRIMARY:
        log.debug("Still weak (%d words), trying readability", word_count)
        r_title, _, r_blocks = _readability_extract(html, url)
        r_words = _count_words(" ".join(b.get("text", "") for b in r_blocks))
        if r_words > word_count:
            blocks = r_blocks
            word_count = r_words
            if r_title and not title:
                title = r_title

    if word_count < MIN_WORDS_FALLBACK:
        log.warning("Low quality extract for %s: only %d words", url, word_count)

    # Собираем ссылки
    links: list[dict] = [
        {"text": b.get("text", b.get("href", "")), "href": b["href"]}
        for b in blocks
        if b["type"] == "link" and b.get("href")
    ]

    return {
        "url": url,
        "title": title,
        "byline": "",
        "excerpt": excerpt,
        "blocks": blocks[:200],  # лимит блоков
        "links": links,
        "plain_text": "",
        "compression_level": "medium",
        "site_profile": None,
        "css_hints": None,
        "stats": {
            "fetch_ms": 0,
            "original_bytes": len(html.encode()),
            "payload_bytes": 0,
            "images_inlined": 0,
            "images_omitted": 0,
        },
    }


def _blocks_word_count(blocks: list[Any]) -> int:
    parts: list[str] = []
    for block in blocks:
        if isinstance(block, dict):
            if block.get("type") in {"paragraph", "heading", "quote"}:
                parts.append(str(block.get("text") or ""))
        else:
            btype = getattr(block, "type", None)
            if btype in {"paragraph", "heading", "quote"}:
                parts.append(str(getattr(block, "text", "") or ""))
    return len(re.findall(r"\w{3,}", " ".join(parts)))


def try_improve_blocks(html: str, url: str, blocks: list[Any]) -> list[Any]:
    """Улучшить блоки через v2-эвристику/readability, если primary extract слабый."""
    from .models import Block

    current = _blocks_word_count(blocks)
    if current >= MIN_WORDS_PRIMARY:
        return blocks

    soup = BeautifulSoup(html, "lxml")
    container: Tag | None = None
    for sel in _CONTENT_SELECTORS:
        found = soup.select_one(sel)
        if found:
            container = found
            break

    candidate: list[dict[str, Any]] = []
    if container:
        candidate = _clean_soup(container)

    word_count = _blocks_word_count(candidate)
    if word_count < MIN_WORDS_PRIMARY:
        heuristic = _div_heuristic(soup)
        if heuristic:
            heuristic_blocks = _clean_soup(heuristic)
            heuristic_words = _blocks_word_count(heuristic_blocks)
            if heuristic_words > word_count:
                candidate = heuristic_blocks
                word_count = heuristic_words

    if word_count < MIN_WORDS_PRIMARY:
        _, _, r_blocks = _readability_extract(html, url)
        r_words = _blocks_word_count(r_blocks)
        if r_words > word_count:
            candidate = r_blocks
            word_count = r_words

    if word_count <= current:
        return blocks

    return [Block.model_validate(b) for b in candidate[:200]]
