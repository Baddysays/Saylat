"""
main_additions.py — новые эндпоинты для добавления в main.py.

Вставить в конец existing main.py:
    from .rss_feeds import fetch_rss_feed, discover_rss_url
    from .extract_v2 import extract_article_v2

И добавить эти роуты.
"""
from fastapi import HTTPException, Query
from .models import SaylatFeed
from .rss_feeds import fetch_rss_feed, discover_rss_url
from .url_safety import validate_public_http_url


# ──────────────────────────────────────────────
# RSS
# ──────────────────────────────────────────────

async def rss_feed_get(
    url: str = Query(..., description="URL RSS/Atom ленты или HTML-страницы"),
) -> SaylatFeed:
    """
    GET /api/rss/feed?url=...
    Вернуть SaylatFeed из RSS/Atom ленты.
    Если url — HTML-страница, попробовать autodiscovery.
    """
    try:
        validated = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    try:
        return await fetch_rss_feed(validated)
    except Exception:
        # Попробовать autodiscovery
        rss_url = await discover_rss_url(validated)
        if not rss_url:
            raise HTTPException(
                status_code=422,
                detail="RSS-лента не найдена. Укажите прямую ссылку на .rss или .atom",
            )
        try:
            return await fetch_rss_feed(rss_url)
        except Exception as exc:
            raise HTTPException(status_code=502, detail=str(exc)) from exc


async def rss_discover(
    url: str = Query(..., description="HTML-страница для autodiscovery"),
) -> dict:
    """
    GET /api/rss/discover?url=...
    Найти URL RSS-ленты на HTML-странице.
    """
    try:
        validated = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    rss_url = await discover_rss_url(validated)
    if not rss_url:
        raise HTTPException(status_code=404, detail="RSS не найден на странице")
    return {"rss_url": rss_url}


# ──────────────────────────────────────────────
# Cache management
# ──────────────────────────────────────────────

async def cache_invalidate(
    url: str = Query(..., description="URL страницы для инвалидации кэша"),
) -> dict:
    """
    POST /api/cache/invalidate?url=...
    Сбросить кэш для конкретного URL (для pull-to-refresh на клиенте).
    """
    from .response_cache import response_cache
    from .url_safety import validate_public_http_url

    try:
        validated = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    count = response_cache.invalidate_prefix(f"extract:{validated}")
    count += response_cache.invalidate_prefix(f"wire:extract:{validated}")
    count += response_cache.invalidate_prefix(f"wirebin:extract:{validated}")
    count += response_cache.invalidate_prefix(f"image:{validated}")
    return {"invalidated": count, "url": validated}


# ──────────────────────────────────────────────
# Регистрация в main.py (добавить в конец файла)
# ──────────────────────────────────────────────
ROUTES_TO_ADD = """
# RSS
app.get("/api/rss/feed")(rss_feed_get)
app.get("/api/rss/discover")(rss_discover)

# Cache
app.post("/api/cache/invalidate")(cache_invalidate)
"""
