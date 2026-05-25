"""Единый тонкий API: open, query, translate, act."""

import time

from fastapi import HTTPException

from .connectors import dzen_conn, mail_conn, telegram_conn, vk_conn
from .extract import extract_article
from .site_feeds import try_open_site
from .models import (
    ActRequest,
    ActResponse,
    FeedItem,
    FeedStats,
    OpenRequest,
    OpenResponse,
    QueryRequest,
    QueryResponse,
    SaylatFeed,
)
from .search import search_web


async def open_resource(req: OpenRequest) -> OpenResponse:
    target = req.target
    rid = (req.resource_id or "").strip() or None

    if target == "url":
        url = (req.url or "").strip()
        if not url.startswith(("http://", "https://")):
            raise HTTPException(status_code=400, detail="url required for target=url")
        opened = await try_open_site(url, images_mode=req.images)
        if opened is not None:
            return opened
        article = await extract_article(url, images_mode=req.images)
        return OpenResponse(kind="article", article=article)

    if target == "telegram":
        return await telegram_conn.open_telegram(rid)

    if target == "mail":
        return await mail_conn.open_mail(rid)

    if target == "vk":
        return await vk_conn.open_vk(rid)

    if target == "dzen":
        return await dzen_conn.open_dzen(rid or req.url)

    raise HTTPException(status_code=400, detail=f"Unknown target: {target}")


async def query_feed(req: QueryRequest) -> QueryResponse:
    started = time.perf_counter()
    result = await search_web(req.q, engine=req.engine.strip().lower())
    items = [
        FeedItem(
            id=f"hit-{i}",
            kind="link",
            title=hit.title,
            body=hit.snippet,
            href=hit.url,
            actions=["open"],
        )
        for i, hit in enumerate(result.results[: req.limit])
    ]
    payload = SaylatFeed(
        source="search",
        title=f"Поиск: {req.q}",
        subtitle=req.engine,
        items=items,
        stats=FeedStats(fetch_ms=result.fetch_ms),
    )
    import json

    payload.stats.payload_bytes = len(
        json.dumps(payload.model_dump(mode="json"), ensure_ascii=False).encode("utf-8")
    )
    return QueryResponse(feed=payload)


async def act_on_item(req: ActRequest) -> ActResponse:
    source = req.source.lower()
    if source in {"telegram", "tg"}:
        return await telegram_conn.act_telegram(
            req.item_id, req.action, req.body, req.context_id
        )
    if source in {"imap", "mail"}:
        return await mail_conn.act_mail(req.item_id, req.action, req.body)
    if source == "vk":
        raise HTTPException(status_code=501, detail="Ответы в ВК — следующий этап")
    if source == "dzen":
        raise HTTPException(status_code=501, detail="Комментарии Дзен — следующий этап")
    raise HTTPException(status_code=400, detail=f"Unknown source: {req.source}")
