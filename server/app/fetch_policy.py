"""Таймауты исходящих запросов с учётом медленной сети клиента."""

from __future__ import annotations

from fastapi import Request

from .config import settings


def is_slow_client(request: Request | None) -> bool:
    if request is None:
        return False
    hdr = request.headers.get("x-saylat-slow-network", "").strip().lower()
    if hdr in ("1", "true", "yes", "on"):
        return True
    level = request.headers.get("x-saylat-level", "").strip().lower()
    return level == "light"


def outbound_timeout_sec(request: Request | None = None) -> float:
    base = float(settings.request_timeout_sec)
    if is_slow_client(request):
        return max(base, 90.0)
    return base
