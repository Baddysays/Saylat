"""Простой кэш ответов прокси — быстрее повторные страницы, меньше нагрузка на VPS."""

from __future__ import annotations

import asyncio
import time
from collections import OrderedDict
from typing import Any, Awaitable, Callable, TypeVar

T = TypeVar("T")


class ResponseCache:
    def __init__(self, *, ttl_sec: int = 900, max_entries: int = 400) -> None:
        self._ttl = max(60, ttl_sec)
        self._max = max(50, max_entries)
        self._data: OrderedDict[str, tuple[float, Any]] = OrderedDict()
        self._lock = asyncio.Lock()
        self.hits = 0
        self.misses = 0

    async def get_or_set(self, key: str, factory: Callable[[], Awaitable[T]]) -> T:
        now = time.monotonic()
        async with self._lock:
            entry = self._data.get(key)
            if entry is not None:
                expires_at, value = entry
                if expires_at > now:
                    self._data.move_to_end(key)
                    self.hits += 1
                    return value  # type: ignore[return-value]
                del self._data[key]

        self.misses += 1
        value = await factory()
        async with self._lock:
            self._data[key] = (now + self._ttl, value)
            self._data.move_to_end(key)
            while len(self._data) > self._max:
                self._data.popitem(last=False)
        return value

    def stats(self) -> dict[str, int]:
        return {
            "entries": len(self._data),
            "hits": self.hits,
            "misses": self.misses,
        }


def _make_cache() -> ResponseCache:
    from .config import settings

    return ResponseCache(
        ttl_sec=settings.cache_ttl_sec,
        max_entries=settings.cache_max_entries,
    )


response_cache = _make_cache()
