"""Прокси JPEG для режима images=refs — обход hotlink и единый Referer."""

from __future__ import annotations

import base64
import logging

import httpx
from fastapi import HTTPException
from fastapi.responses import Response

from .config import settings
from .images import NORMAL_PROFILE, TINY_PROFILE, fetch_image_data_url
from .response_cache import response_cache
from .url_safety import validate_public_http_url

log = logging.getLogger(__name__)

IMAGE_CACHE_TTL = 600.0


async def fetch_proxied_jpeg(
    image_url: str,
    page_url: str | None,
    *,
    tiny: bool = True,
) -> tuple[bytes, int, int]:
    """
    Скачать картинку через сервер (как при extract), вернуть сырые JPEG-байты.
    Кэшируем — повторные показы в ленте не бьют по origin.
    """
    validated = validate_public_http_url(image_url)
    referer = validate_public_http_url(page_url) if page_url else validated
    profile = TINY_PROFILE if tiny else NORMAL_PROFILE
    cache_key = f"image:{validated}:{referer}:{profile.target_bytes}"

    async def _load() -> dict:
        async with httpx.AsyncClient(
            follow_redirects=True,
            timeout=settings.request_timeout_sec,
        ) as client:
            data_url, w, h = await fetch_image_data_url(
                client,
                validated,
                referer,
                profile=profile,
            )
        if not data_url or not data_url.startswith("data:image"):
            raise HTTPException(status_code=404, detail="Image not available")
        b64 = data_url.split(",", 1)[-1]
        jpeg = base64.b64decode(b64)
        if not jpeg:
            raise HTTPException(status_code=404, detail="Empty image")
        return {"jpeg_b64": base64.b64encode(jpeg).decode("ascii"), "w": w, "h": h}

    try:
        cached = await response_cache.get_or_set(cache_key, _load, ttl=IMAGE_CACHE_TTL)
    except HTTPException:
        raise
    except Exception as exc:
        log.warning("image proxy failed %s: %s", validated, exc)
        raise HTTPException(status_code=502, detail="Image fetch failed") from exc

    jpeg = base64.b64decode(cached["jpeg_b64"])
    return jpeg, int(cached.get("w") or 0), int(cached.get("h") or 0)


async def image_proxy_response(
    image_url: str,
    page_url: str | None,
    *,
    tiny: bool = True,
) -> Response:
    jpeg, _, _ = await fetch_proxied_jpeg(image_url, page_url, tiny=tiny)
    return Response(
        content=jpeg,
        media_type="image/jpeg",
        headers={
            "Cache-Control": "public, max-age=3600",
            "X-Saylat-Image-Proxy": "1",
        },
    )
