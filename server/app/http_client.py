"""Общий httpx.AsyncClient с connection pooling для исходящих запросов."""

from __future__ import annotations

import httpx

_client: httpx.AsyncClient | None = None


async def init_shared_http_client() -> None:
    global _client
    if _client is None:
        _client = httpx.AsyncClient(
            follow_redirects=True,
            limits=httpx.Limits(max_connections=20, max_keepalive_connections=10),
        )


async def close_shared_http_client() -> None:
    global _client
    if _client is not None:
        await _client.aclose()
        _client = None


def shared_http_client() -> httpx.AsyncClient:
    if _client is None:
        return httpx.AsyncClient(follow_redirects=True)
    return _client
