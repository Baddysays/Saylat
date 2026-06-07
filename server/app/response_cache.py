"""
Кэш ответов с TTL и опциональным Redis-бэкендом.
Заменяет оригинальный response_cache.py.

In-memory: работает сразу, сбрасывается при рестарте.
Redis: добавить REDIS_URL в env → кэш переживает рестарты контейнера.
"""
from __future__ import annotations

import asyncio
import json
import logging
import time
from typing import Any, Callable, Awaitable

log = logging.getLogger(__name__)

# TTL по типу эндпоинта (секунды)
TTL_EXTRACT = 300      # 5 мин — статьи
TTL_STRIPS  = 60       # 1 мин — полосы (Playwright тяжёлый)
TTL_VISUAL  = 120      # 2 мин
TTL_SEARCH  = 60       # 1 мин
TTL_DEFAULT = 180      # 3 мин


class _Entry:
    __slots__ = ("value", "expires_at")

    def __init__(self, value: Any, ttl: float) -> None:
        self.value = value
        self.expires_at = time.monotonic() + ttl


class InMemoryCache:
    """Простой in-memory кэш с TTL и периодической очисткой."""

    def __init__(self) -> None:
        self._store: dict[str, _Entry] = {}
        self._hits = 0
        self._misses = 0
        self._store_lock = asyncio.Lock()
        self._key_locks: dict[str, asyncio.Lock] = {}

    def _ttl_for(self, key: str) -> float:
        if key.startswith("extract:"):
            return TTL_EXTRACT
        if key.startswith("strips:"):
            return TTL_STRIPS
        if key.startswith("visual:"):
            return TTL_VISUAL
        if key.startswith("search:"):
            return TTL_SEARCH
        return TTL_DEFAULT

    async def _key_lock(self, key: str) -> asyncio.Lock:
        async with self._store_lock:
            lock = self._key_locks.get(key)
            if lock is None:
                lock = asyncio.Lock()
                self._key_locks[key] = lock
            return lock

    async def get_or_set(
        self,
        key: str,
        loader: Callable[[], Awaitable[Any]],
        ttl: float | None = None,
    ) -> Any:
        async with self._store_lock:
            entry = self._store.get(key)
            if entry is not None:
                if time.monotonic() < entry.expires_at:
                    self._hits += 1
                    log.debug("cache hit: %s", key)
                    return entry.value
                del self._store[key]

        key_lock = await self._key_lock(key)
        async with key_lock:
            async with self._store_lock:
                entry = self._store.get(key)
                if entry is not None and time.monotonic() < entry.expires_at:
                    self._hits += 1
                    log.debug("cache hit: %s", key)
                    return entry.value

            self._misses += 1
            log.debug("cache miss: %s", key)
            value = await loader()
            effective_ttl = ttl if ttl is not None else self._ttl_for(key)
            async with self._store_lock:
                self._store[key] = _Entry(value, effective_ttl)
                if self._misses % 50 == 0:
                    self._evict()
            return value

    def _evict(self) -> None:
        now = time.monotonic()
        stale = [k for k, v in self._store.items() if v.expires_at <= now]
        for k in stale:
            del self._store[k]
        if stale:
            log.debug("evicted %d stale cache entries", len(stale))

    def invalidate(self, key: str) -> None:
        self._store.pop(key, None)

    def invalidate_prefix(self, prefix: str) -> int:
        keys = [k for k in self._store if k.startswith(prefix)]
        for k in keys:
            del self._store[k]
        return len(keys)

    def clear(self) -> None:
        self._store.clear()

    def stats(self) -> dict:
        now = time.monotonic()
        alive = sum(1 for v in self._store.values() if v.expires_at > now)
        return {
            "entries": alive,
            "hits": self._hits,
            "misses": self._misses,
            "hit_rate": round(self._hits / max(1, self._hits + self._misses) * 100, 1),
        }


class RedisCache:
    """Redis-кэш для персистентного хранения между рестартами."""

    def __init__(self, url: str) -> None:
        try:
            import redis.asyncio as aioredis  # type: ignore
            self._redis = aioredis.from_url(url, decode_responses=False)
            self._available = True
        except ImportError:
            log.warning("redis package not installed, falling back to in-memory cache")
            self._available = False
        self._fallback = InMemoryCache()
        self._hits = 0
        self._misses = 0

    def _ttl_for(self, key: str) -> int:
        if key.startswith("extract:"):
            return int(TTL_EXTRACT)
        if key.startswith("strips:"):
            return int(TTL_STRIPS)
        if key.startswith("visual:"):
            return int(TTL_VISUAL)
        return int(TTL_DEFAULT)

    async def get_or_set(
        self,
        key: str,
        loader: Callable[[], Awaitable[Any]],
        ttl: float | None = None,
    ) -> Any:
        if not self._available:
            return await self._fallback.get_or_set(key, loader, ttl)
        try:
            raw = await self._redis.get(key)
            if raw is not None:
                self._hits += 1
                return json.loads(raw)
        except Exception as exc:
            log.warning("redis get error: %s", exc)

        self._misses += 1
        value = await loader()
        try:
            effective_ttl = int(ttl) if ttl is not None else self._ttl_for(key)
            await self._redis.setex(key, effective_ttl, json.dumps(value, default=str))
        except Exception as exc:
            log.warning("redis set error: %s", exc)
        return value

    def invalidate(self, key: str) -> None:
        if not self._available:
            self._fallback.invalidate(key)
            return

        async def _delete() -> None:
            try:
                await self._redis.delete(key)
            except Exception as exc:
                log.warning("redis delete error: %s", exc)

        asyncio.create_task(_delete())

    def invalidate_prefix(self, prefix: str) -> int:
        return self._fallback.invalidate_prefix(prefix)

    def stats(self) -> dict:
        return {"entries": -1, "hits": self._hits, "misses": self._misses}


def build_cache() -> InMemoryCache | RedisCache:
    """Автовыбор бэкенда по REDIS_URL в окружении."""
    import os
    url = os.getenv("REDIS_URL", "").strip()
    if url:
        log.info("Using Redis cache: %s", url.split("@")[-1])
        return RedisCache(url)
    log.info("Using in-memory cache with TTL")
    return InMemoryCache()


response_cache: InMemoryCache | RedisCache = build_cache()
