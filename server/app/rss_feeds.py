"""
rss_feeds.py — RSS/Atom подписки для Saylat.

Endpoint: GET /api/rss/feed?url=...
Формат ответа: SaylatFeed (совместимый с Android FeedItem)
"""
from __future__ import annotations

import json
import logging
import re
import time
from datetime import datetime, timezone
from typing import Any

import httpx

from .models import FeedItem, FeedStats, SaylatFeed

log = logging.getLogger(__name__)

_TIMEOUT = 15
_MAX_ITEMS = 30
_UA = "Mozilla/5.0 (compatible; Saylat-RSS/1.0)"


def _parse_date(entry: Any) -> str:
    """Вернуть ISO-дату из feedparser entry."""
    for field in ("published_parsed", "updated_parsed"):
        t = getattr(entry, field, None)
        if t:
            try:
                dt = datetime(*t[:6], tzinfo=timezone.utc)
                return dt.isoformat()
            except Exception:
                pass
    return datetime.now(timezone.utc).isoformat()


def _entry_to_feed_item(entry: Any, source_title: str) -> FeedItem:
    title = getattr(entry, "title", "") or ""
    href = getattr(entry, "link", "") or ""
    summary = getattr(entry, "summary", "") or ""
    entry_id = getattr(entry, "id", href) or href
    summary = re.sub(r"<[^>]+>", "", summary).strip()[:500]
    author = getattr(getattr(entry, "author_detail", None), "name", None) or ""

    return FeedItem(
        id=f"rss-{hash(entry_id) & 0xFFFFFF:06x}",
        kind="link",
        title=title.strip() or href,
        body=summary,
        href=href or None,
        time=_parse_date(entry),
        from_=author or source_title,
        actions=["open"],
    )


async def fetch_rss_feed(url: str) -> SaylatFeed:
    """Загрузить и распарсить RSS/Atom ленту."""
    try:
        import feedparser  # type: ignore
    except ImportError:
        raise RuntimeError("feedparser not installed: pip install feedparser")

    t0 = time.monotonic()
    async with httpx.AsyncClient(
        follow_redirects=True,
        timeout=_TIMEOUT,
        headers={"User-Agent": _UA},
    ) as client:
        resp = await client.get(url)
        resp.raise_for_status()
        from .http_text import decode_response_text

        content = decode_response_text(resp)

    feed = feedparser.parse(content)

    if feed.bozo and not feed.entries:
        raise ValueError(f"Не удалось разобрать RSS: {url}")

    feed_title = getattr(feed.feed, "title", None) or url
    items = [
        _entry_to_feed_item(e, feed_title)
        for e in feed.entries[:_MAX_ITEMS]
    ]

    ms = int((time.monotonic() - t0) * 1000)
    payload = json.dumps(
        {"title": feed_title, "items": [i.model_dump(by_alias=True) for i in items]},
        ensure_ascii=False,
    ).encode("utf-8")
    return SaylatFeed(
        source="rss",
        title=feed_title,
        subtitle=f"{len(items)} записей",
        context_id=url,
        items=items,
        stats=FeedStats(fetch_ms=ms, payload_bytes=len(payload)),
        has_more=False,
        total_items=len(items),
    )


async def discover_rss_url(page_url: str) -> str | None:
    """Найти RSS-ссылку на HTML-странице (autodiscovery)."""
    try:
        async with httpx.AsyncClient(
            follow_redirects=True, timeout=10, headers={"User-Agent": _UA}
        ) as client:
            resp = await client.get(page_url)
            from .http_text import decode_response_text

            html = decode_response_text(resp)
        from bs4 import BeautifulSoup
        from urllib.parse import urljoin

        soup = BeautifulSoup(html, "lxml")
        for link in soup.find_all(
            "link",
            type=["application/rss+xml", "application/atom+xml"],
        ):
            href = link.get("href", "")
            if href.startswith("http"):
                return href
            if href.startswith("/"):
                return urljoin(page_url, href)
    except Exception as exc:
        log.debug("RSS autodiscovery failed: %s", exc)
    return None
