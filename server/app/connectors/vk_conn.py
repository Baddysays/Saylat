"""ВКонтакте — VK API (токен на VPS)."""

from __future__ import annotations

import time

import httpx
from fastapi import HTTPException

from ..config import settings
from ..credentials_store import effective_vk_token, vk_is_configured
from ..models import FeedItem, FeedStats, OpenResponse, SaylatFeed

VK_API = "https://api.vk.com/method"
VK_VERSION = "5.199"


async def open_vk(resource_id: str | None) -> OpenResponse:
    if not vk_is_configured():
        raise HTTPException(
            status_code=503,
            detail="ВК: вставьте access token в настройках приложения (права wall, offline)",
        )
    started = time.perf_counter()
    token = effective_vk_token()
    params = {
        "access_token": token,
        "v": VK_VERSION,
        "count": settings.vk_feed_limit,
        "filters": "post",
    }
    if resource_id and resource_id.isdigit():
        params["owner_id"] = resource_id
        method = "wall.get"
    else:
        method = "newsfeed.get"

    async with httpx.AsyncClient(timeout=settings.request_timeout_sec) as client:
        r = await client.get(f"{VK_API}/{method}", params=params)
        data = r.json()

    if "error" in data:
        err = data["error"]
        raise HTTPException(
            status_code=502,
            detail=f"VK API: {err.get('error_msg', err)}",
        )

    items: list[FeedItem] = []
    if method == "newsfeed.get":
        posts = data.get("response", {}).get("items", [])
        profiles = {p["id"]: p for p in data.get("response", {}).get("profiles", [])}
        groups = {abs(g["id"]): g for g in data.get("response", {}).get("groups", [])}
        for post in posts:
            text = (post.get("text") or "").strip()
            if not text:
                continue
            src = post.get("source_id", 0)
            if src > 0:
                name = profiles.get(src, {}).get("first_name", "Профиль")
            else:
                name = groups.get(abs(src), {}).get("name", "Сообщество")
            owner = post.get("source_id", src)
            pid = post.get("id", 0)
            items.append(
                FeedItem(
                    id=f"vk-{owner}_{pid}",
                    kind="thread",
                    title=name,
                    body=text[:400],
                    href=f"https://vk.com/wall{owner}_{pid}",
                    actions=["open"],
                )
            )
    else:
        posts = data.get("response", {}).get("items", [])
        for post in posts:
            text = (post.get("text") or "").strip()
            if text:
                oid = post.get("owner_id", resource_id)
                pid = post.get("id", 0)
                items.append(
                    FeedItem(
                        id=f"vk-{oid}_{pid}",
                        kind="thread",
                        title=f"Запись {pid}",
                        body=text[:400],
                        href=f"https://vk.com/wall{oid}_{pid}",
                        actions=["open"],
                    )
                )

    if not items:
        items.append(
            FeedItem(
                id="vk-empty",
                kind="notice",
                title="Лента пуста",
                body="Нет постов или нет прав токена (wall, newsfeed).",
            )
        )

    ms = int((time.perf_counter() - started) * 1000)
    feed = SaylatFeed(
        source="vk",
        title="ВКонтакте",
        subtitle="Лента по токену (без WebView)",
        items=items,
        stats=FeedStats(fetch_ms=ms),
    )
    return OpenResponse(kind="feed", feed=feed)
