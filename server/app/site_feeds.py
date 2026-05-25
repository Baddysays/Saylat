"""Адаптеры ленточных сайтов: Пикабу, ВК, Дзен (без WebView)."""

from __future__ import annotations

import json
import re
import time
from urllib.parse import urljoin, urlparse

import httpx
from bs4 import BeautifulSoup

from .config import settings
from .extract import extract_article
from .pikabu_extract import _img_src, normalize_pikabu_story_href
from .models import ArticleStats, Block, FeedItem, FeedStats, OpenResponse, SaylatArticle, SaylatFeed

from .http_ua import CHROME_DESKTOP_UA, CHROME_MOBILE_UA, is_vk_url, normalize_fetch_url, ua_for_url

PIKABU_HOSTS = frozenset({"pikabu.ru", "www.pikabu.ru"})
VK_HOSTS = frozenset({"vk.com", "www.vk.com", "m.vk.com", "vk.ru", "www.vk.ru"})
DZEN_HOSTS = frozenset({"dzen.ru", "www.dzen.ru", "zen.yandex.ru"})

VK_PUBLIC_PAGES = [
    ("Павел Дуров", "https://vk.com/durov"),
    ("ВКонтакте", "https://vk.com/vk"),
    ("Хабр", "https://vk.com/habr"),
    ("ТАСС", "https://vk.com/tass_agency"),
]


def _host(url: str) -> str:
    return (urlparse(url).hostname or "").lower().removeprefix("www.")


def _normalize_url(url: str) -> str:
    p = urlparse(url.strip())
    if not p.scheme:
        return f"https://{url.strip()}"
    return url.strip()


def _slug_title(story_url: str) -> str:
    m = re.search(r"/story/([^/?#]+)", story_url)
    if not m:
        return "Запись"
    slug = re.sub(r"_\d+$", "", m.group(1))
    words = slug.replace("_", " ").strip()
    if not words:
        return "Запись"
    return words[:1].upper() + words[1:][:140]


def _clean_line(text: str, limit: int = 280) -> str:
    text = re.sub(r"\s+", " ", text or "").strip()
    if len(text) > limit:
        return text[: limit - 1] + "…"
    return text


async def _fetch_html(url: str, *, mobile: bool = True) -> tuple[str, int]:
    started = time.perf_counter()
    fetch_url = normalize_fetch_url(url)
    ua = ua_for_url(fetch_url) if is_vk_url(fetch_url) else (
        CHROME_MOBILE_UA if mobile else CHROME_DESKTOP_UA
    )
    headers = {"User-Agent": ua}
    async with httpx.AsyncClient(follow_redirects=True, headers=headers) as client:
        resp = await client.get(fetch_url, timeout=settings.request_timeout_sec)
        resp.raise_for_status()
        html = resp.text[: settings.max_html_bytes]
        ms = int((time.perf_counter() - started) * 1000)
        return html, len(resp.content), ms


def _feed_response(
    *,
    source: str,
    title: str,
    subtitle: str,
    items: list[FeedItem],
    original_bytes: int,
    fetch_ms: int,
) -> OpenResponse:
    payload = SaylatFeed(
        source=source,
        title=title,
        subtitle=subtitle,
        items=items[:48],
        stats=FeedStats(fetch_ms=fetch_ms),
    )
    raw = json.dumps(payload.model_dump(mode="json", by_alias=True), ensure_ascii=False)
    payload.stats.payload_bytes = len(raw.encode("utf-8"))
    return OpenResponse(kind="feed", feed=payload)


def feed_to_article(url: str, feed: SaylatFeed, *, original_bytes: int = 0, fetch_ms: int = 0) -> SaylatArticle:
    """Совместимость с GET /api/extract — лента как блоки."""
    blocks: list[Block] = []
    for item in feed.items:
        if item.title:
            blocks.append(Block(type="heading", text=item.title, level=3))
        if item.body:
            blocks.append(Block(type="paragraph", text=item.body))
        if item.href:
            blocks.append(Block(type="link", text="Открыть", href=item.href))
    if not blocks:
        blocks.append(Block(type="paragraph", text="Пустая лента"))
    payload = SaylatArticle(
        url=url,
        title=feed.title or "Лента",
        excerpt=feed.subtitle,
        blocks=blocks,
        layout_hint="feed",
        stats=ArticleStats(
            original_bytes=original_bytes,
            fetch_ms=fetch_ms or feed.stats.fetch_ms,
            payload_bytes=feed.stats.payload_bytes,
        ),
    )
    return payload


async def try_open_site(url: str, *, images_mode: str = "normal") -> OpenResponse | None:
    url = _normalize_url(url)
    host = _host(url)
    if host in PIKABU_HOSTS:
        return await _open_pikabu(url, images_mode=images_mode)
    if host in VK_HOSTS:
        return await _open_vk(url, images_mode=images_mode)
    if host in DZEN_HOSTS:
        return await _open_dzen(url, images_mode=images_mode)
    return None


async def _open_pikabu(url: str, *, images_mode: str) -> OpenResponse:
    path = urlparse(url).path.rstrip("/") or "/"
    if path.startswith("/story/"):
        article = await extract_article(url, images_mode=images_mode)
        return OpenResponse(kind="article", article=article)

    feed_url = url if path in {"/", "/new", "/hot", "/best"} else "https://pikabu.ru/"
    html, original_bytes, fetch_ms = await _fetch_html(feed_url)
    soup = BeautifulSoup(html, "lxml")
    seen: set[str] = set()
    items: list[FeedItem] = []

    for a in soup.find_all("a", href=True):
        full = normalize_pikabu_story_href(a["href"], feed_url)
        if not full or full in seen:
            continue
        seen.add(full)
        title = _clean_line(a.get("title") or a.get_text(), 120) or _slug_title(full)
        if len(title) < 4:
            title = _slug_title(full)
        thumb = ""
        if images_mode not in ("layout", "off"):
            card = a.find_parent(class_=lambda c: c and "story" in " ".join(c).lower())
            if card:
                img = card.find("img")
                thumb = _img_src(img)
        body = "Пост с картинкой" if thumb else "Текстовый пост · нажмите, чтобы открыть"
        items.append(
            FeedItem(
                id=f"pikabu-{len(items)}",
                kind="thread",
                title=title,
                body=body,
                href=full,
                thumb=thumb or None,
                actions=["open"],
            )
        )
        if len(items) >= 32:
            break

    if not items:
        items.append(
            FeedItem(
                id="pikabu-notice",
                kind="notice",
                title="Пикабу",
                body="Не удалось собрать ленту. Откройте прямую ссылку на пост /story/…",
                href=feed_url,
                actions=["open"],
            )
        )

    return _feed_response(
        source="pikabu",
        title="Пикабу",
        subtitle="Главная лента (текстовые карточки)",
        items=items,
        original_bytes=original_bytes,
        fetch_ms=fetch_ms,
    )


async def _open_vk(url: str, *, images_mode: str) -> OpenResponse:
    parsed = urlparse(url)
    path = parsed.path.rstrip("/") or "/"
    host = parsed.hostname or "vk.com"
    base = f"https://{host}"

    wall_match = re.search(r"/wall(-?\d+)_(\d+)", path)
    if wall_match:
        owner, post = wall_match.groups()
        wall_url = f"https://vk.com/wall{owner}_{post}"
        try:
            article = await extract_article(wall_url, images_mode=images_mode)
            if article.blocks and not article.blocks[0].text.startswith("Не удалось"):
                return OpenResponse(kind="article", article=article)
        except Exception:
            pass

    if path.startswith("/wall") or re.match(r"^/wall-", path):
        try:
            article = await extract_article(
                url if host in ("vk.com", "vk.ru") else f"https://vk.com{path}",
                images_mode=images_mode,
            )
            return OpenResponse(kind="article", article=article)
        except Exception:
            pass

    profile_match = re.match(r"^/(id\d+|[\w.]+|public\d+)$", path)
    if profile_match and path not in {"/feed", "/login", "/join"}:
        profile_url = f"https://vk.com{path}"
        items = await _vk_profile_items(profile_url)
        if items:
            return _feed_response(
                source="vk",
                title=f"ВК: {path.strip('/')}",
                subtitle="Публичная страница",
                items=items,
                original_bytes=0,
                fetch_ms=0,
            )

    if path in {"/feed", "/", "/news", "/discover"} or "feed" in path:
        items: list[FeedItem] = [
            FeedItem(
                id="vk-notice",
                kind="notice",
                title="Лента ВКонтакте",
                body=(
                    "Персональная лента требует входа — Saylat не использует WebView. "
                    "Ниже — публичные страницы и посты по прямой ссылке (wall…)."
                ),
                actions=[],
            ),
        ]
        for i, (title, href) in enumerate(VK_PUBLIC_PAGES):
            items.append(
                FeedItem(
                    id=f"vk-pub-{i}",
                    kind="link",
                    title=title,
                    body=href,
                    href=href,
                    actions=["open"],
                )
            )
        return _feed_response(
            source="vk",
            title="ВКонтакте",
            subtitle="Публичные страницы без входа",
            items=items,
            original_bytes=0,
            fetch_ms=0,
        )

    try:
        article = await extract_article(
            f"https://vk.com{path}" if host in ("vk.com", "vk.ru") else url,
            images_mode=images_mode,
        )
        return OpenResponse(kind="article", article=article)
    except Exception:
        return _feed_response(
            source="vk",
            title="ВКонтакте",
            subtitle=url,
            items=[
                FeedItem(
                    id="vk-fallback",
                    kind="notice",
                    title="Не удалось открыть",
                    body="Попробуйте vk.com/имя или ссылку wall-1_123",
                    href=base,
                    actions=["open"],
                )
            ],
            original_bytes=0,
            fetch_ms=0,
        )


async def _vk_profile_items(profile_url: str) -> list[FeedItem]:
    try:
        html, _, fetch_ms = await _fetch_html(profile_url)
    except Exception:
        return []
    items: list[FeedItem] = []
    for m in re.finditer(r'href="(/wall-\d+_\d+)"', html):
        href = urljoin(profile_url, m.group(1))
        items.append(
            FeedItem(
                id=f"vk-wall-{len(items)}",
                kind="thread",
                title=f"Пост {m.group(1).replace('/wall', '')}",
                body="Открыть запись",
                href=href,
                actions=["open"],
            )
        )
        if len(items) >= 24:
            break
    if items:
        items.insert(
            0,
            FeedItem(
                id="vk-profile-hint",
                kind="notice",
                title="Публичные посты",
                body=f"Найдено постов: {len(items)}. Без входа — только открытые wall-ссылки.",
                actions=[],
            ),
        )
    return items


async def _open_dzen(url: str, *, images_mode: str) -> OpenResponse:
    path = urlparse(url).path.rstrip("/") or "/"

    if path.startswith("/a/") or path.startswith("/video/"):
        article = await extract_article(url, images_mode=images_mode)
        return OpenResponse(kind="article", article=article)

    if path.startswith("/news/story/"):
        article = await extract_article(url, images_mode=images_mode)
        return OpenResponse(kind="article", article=article)

    news_url = "https://dzen.ru/news"
    if path.startswith("/news") or path == "/":
        html, original_bytes, fetch_ms = await _fetch_html(
            news_url if path.startswith("/news") else url,
            mobile=False,
        )
        items = _dzen_parse_news_html(html, news_url)
        if not items:
            items = [
                FeedItem(
                    id="dzen-notice",
                    kind="notice",
                    title="Дзен",
                    body=(
                        "Раздел новостей часто требует авторизации Яндекса. "
                        "Откройте статью по ссылке dzen.ru/a/… — она обычно читается в Saylat."
                    ),
                    href=news_url,
                    actions=["open"],
                ),
                FeedItem(
                    id="dzen-help",
                    kind="link",
                    title="Пример статьи Дзен",
                    body="Поиск «site:dzen.ru/a» в Saylat",
                    href="https://dzen.ru/?tab=articles",
                    actions=["open"],
                ),
            ]
        return _feed_response(
            source="dzen",
            title="Дзен — новости",
            subtitle="Карточки без WebView",
            items=items,
            original_bytes=original_bytes,
            fetch_ms=fetch_ms,
        )

    article = await extract_article(url, images_mode=images_mode)
    return OpenResponse(kind="article", article=article)


def _dzen_parse_news_html(html: str, base_url: str) -> list[FeedItem]:
    soup = BeautifulSoup(html, "lxml")
    items: list[FeedItem] = []
    seen: set[str] = set()

    for a in soup.find_all("a", href=True):
        href = a["href"]
        if "/news/story/" not in href and "/a/" not in href:
            continue
        full = urljoin(base_url, href.split("?")[0])
        if full in seen:
            continue
        seen.add(full)
        title = _clean_line(a.get_text(), 120)
        if len(title) < 8:
            continue
        items.append(
            FeedItem(
                id=f"dzen-{len(items)}",
                kind="thread",
                title=title,
                body="Открыть в Saylat",
                href=full,
                actions=["open"],
            )
        )
        if len(items) >= 28:
            break
    return items
