"""Опциональный API-ключ и лимит запросов по IP для личного VPS."""

from __future__ import annotations

import time
from collections import defaultdict
from typing import Callable

from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import JSONResponse

from .config import settings

_CONNECT_PREFIX = "/api/connect"
_PUBLIC_PREFIXES = ("/health", "/static", "/api/app/update", "/app/download/")


def _path_exempt(path: str) -> bool:
    if path == "/" or path.startswith(_PUBLIC_PREFIXES):
        return True
    if path.startswith(_CONNECT_PREFIX):
        return True
    return False


class ApiKeyMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        key = settings.api_key.strip()
        if not key or not request.url.path.startswith("/api/"):
            return await call_next(request)
        if _path_exempt(request.url.path):
            return await call_next(request)
        provided = request.headers.get("x-api-key", "").strip()
        if provided != key:
            return JSONResponse(
                status_code=401,
                content={"detail": "Неверный или отсутствующий заголовок X-API-Key"},
            )
        return await call_next(request)


class RateLimitMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, *, limit_per_minute: int) -> None:
        super().__init__(app)
        self._limit = max(10, limit_per_minute)
        self._hits: dict[str, list[float]] = defaultdict(list)

    def _client_ip(self, request: Request) -> str:
        forwarded = request.headers.get("x-forwarded-for", "").split(",")[0].strip()
        if forwarded:
            return forwarded
        if request.client:
            return request.client.host
        return "unknown"

    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        if not request.url.path.startswith("/api/"):
            return await call_next(request)
        now = time.monotonic()
        ip = self._client_ip(request)
        window = self._hits[ip]
        window[:] = [t for t in window if now - t < 60.0]
        if len(window) >= self._limit:
            return JSONResponse(
                status_code=429,
                content={"detail": "Слишком много запросов. Подождите минуту."},
            )
        window.append(now)
        return await call_next(request)
