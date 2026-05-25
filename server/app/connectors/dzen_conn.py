"""Дзен — лента по cookie сессии (настроена на VPS при Wi‑Fi)."""

from __future__ import annotations

import time

import httpx
from bs4 import BeautifulSoup
from fastapi import HTTPException

from ..config import settings
from ..credentials_store import dzen_is_configured, effective_dzen_cookie
from ..models import FeedItem, FeedStats, OpenResponse, SaylatFeed
from ..site_feeds import _dzen_parse_news_html

DESKTOP_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
)


async def open_dzen(resource_id: str | None) -> OpenResponse:
    started = time.perf_counter()
    url = resource_id or "https://dzen.ru/news"
    if not url.startswith("http"):
        url = f"https://dzen.ru/{resource_id.lstrip('/')}"

    headers = {"User-Agent": DESKTOP_UA}
    if dzen_is_configured():
        headers["Cookie"] = effective_dzen_cookie()

    async with httpx.AsyncClient(follow_redirects=True, timeout=settings.request_timeout_sec) as client:
        resp = await client.get(url, headers=headers)
        html = resp.text[: settings.max_html_bytes]
        original_bytes = len(resp.content)

    items = _dzen_parse_news_html(html, "https://dzen.ru")
    if not items:
        if not dzen_is_configured():
            items = [
                FeedItem(
                    id="dzen-auth",
                    kind="notice",
                    title="Дзен: нужна сессия",
                    body=(
                        "Войдите на dzen.ru в браузере, скопируйте Cookie и вставьте "
                        "в настройках приложения → Сохранить. Потом лента на 2G."
                    ),
                    href="https://dzen.ru/news",
                    actions=["open"],
                ),
            ]
        else:
            items = [
                FeedItem(
                    id="dzen-empty",
                    kind="notice",
                    title="Дзен",
                    body="Не удалось разобрать ленту. Откройте статью dzen.ru/a/…",
                    href=url,
                    actions=["open"],
                ),
            ]

    ms = int((time.perf_counter() - started) * 1000)
    feed = SaylatFeed(
        source="dzen",
        title="Дзен",
        subtitle="Подписки и новости" if dzen_is_configured() else "Без cookie — только подсказка",
        items=items,
        stats=FeedStats(fetch_ms=ms),
    )
    return OpenResponse(kind="feed", feed=feed)
