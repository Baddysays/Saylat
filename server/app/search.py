"""Поиск через прокси: DuckDuckGo HTML + Wikipedia; SearXNG — если инстанс отвечает."""

from __future__ import annotations

import re
import time
from urllib.parse import parse_qs, unquote, urljoin, urlparse

import httpx
from bs4 import BeautifulSoup

from .config import settings
from .http_ua import MOBILE_UA
from .models import SearchHit, SearchResponse

_SEARX_PATH = "search"
_DDG_HTML = "https://html.duckduckgo.com/html/"


async def search_web(query: str, engine: str = "searxng") -> SearchResponse:
    started = time.perf_counter()
    trimmed = query.strip()
    if not trimmed:
        return SearchResponse(query=query, engine=engine, results=[])

    merged: list[SearchHit] = []
    seen: set[str] = set()
    sources: list[str] = []

    for name, fetcher in (
        ("duckduckgo", _duckduckgo_html_search),
        ("wikipedia", _wikipedia_opensearch),
        ("duckduckgo_instant", _duckduckgo_instant),
    ):
        try:
            batch = await fetcher(trimmed)
            added = _merge_hits(merged, seen, batch)
            if added:
                sources.append(name)
        except httpx.HTTPError:
            continue

    for base in _searx_bases():
        try:
            batch = await _searx_search(trimmed, base)
            added = _merge_hits(merged, seen, batch)
            if added:
                sources.append("searxng")
                break
        except httpx.HTTPError:
            continue

    engine_label = "+".join(sources) if sources else "none"
    return SearchResponse(
        query=trimmed,
        engine=engine_label,
        results=merged[: settings.search_max_results],
        fetch_ms=int((time.perf_counter() - started) * 1000),
    )


def _merge_hits(
    merged: list[SearchHit],
    seen: set[str],
    batch: list[SearchHit],
) -> int:
    added = 0
    for hit in batch:
        url = hit.url.strip()
        if not url.startswith(("http://", "https://")) or url in seen:
            continue
        seen.add(url)
        merged.append(hit)
        added += 1
        if len(merged) >= settings.search_max_results:
            break
    return added


def _searx_bases() -> list[str]:
    raw = [settings.searx_instance, *settings.searx_fallbacks.split(",")]
    seen: set[str] = set()
    bases: list[str] = []
    for item in raw:
        base = item.strip().rstrip("/")
        if base and base not in seen:
            seen.add(base)
            bases.append(base)
    return bases


async def _searx_search(query: str, base: str) -> list[SearchHit]:
    url = urljoin(base + "/", _SEARX_PATH)
    params = {
        "q": query,
        "format": "json",
        "language": settings.search_language,
        "categories": "general",
    }
    async with httpx.AsyncClient(
        follow_redirects=True,
        headers={"User-Agent": settings.user_agent},
        timeout=settings.request_timeout_sec,
    ) as client:
        resp = await client.get(url, params=params)
        if resp.status_code == 429:
            raise httpx.HTTPError("rate limited")
        resp.raise_for_status()
        data = resp.json()

    hits: list[SearchHit] = []
    for item in data.get("results") or []:
        link = (item.get("url") or "").strip()
        if not link.startswith(("http://", "https://")):
            continue
        title = (item.get("title") or "").strip() or link
        snippet = (item.get("content") or "").strip()
        hits.append(
            SearchHit(
                title=title,
                url=link,
                snippet=snippet,
                source=item.get("engine") or "searxng",
            )
        )
    return hits


async def _duckduckgo_html_search(query: str) -> list[SearchHit]:
    """Основная веб-выдача — html.duckduckgo.com (без API-ключа)."""
    async with httpx.AsyncClient(
        follow_redirects=True,
        headers={"User-Agent": MOBILE_UA},
        timeout=settings.request_timeout_sec,
    ) as client:
        resp = await client.post(_DDG_HTML, data={"q": query})
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "lxml")

    hits: list[SearchHit] = []
    for block in soup.select(".result"):
        anchor = block.select_one("a.result__a")
        if not anchor:
            continue
        raw_href = (anchor.get("href") or "").strip()
        resolved = _resolve_ddg_href(raw_href)
        if not resolved:
            continue
        snippet_el = block.select_one(".result__snippet")
        snippet = snippet_el.get_text(" ", strip=True) if snippet_el else ""
        title = anchor.get_text(" ", strip=True) or resolved
        hits.append(
            SearchHit(
                title=title,
                url=resolved,
                snippet=snippet,
                source="duckduckgo",
            )
        )
    return hits


def _resolve_ddg_href(href: str) -> str:
    if not href:
        return ""
    if href.startswith("//"):
        href = "https:" + href
    if href.startswith("/l/") or "duckduckgo.com/l/" in href:
        try:
            parsed = urlparse(href if href.startswith("http") else f"https://duckduckgo.com{href}")
            uddg = parse_qs(parsed.query).get("uddg", [""])[0]
            return unquote(uddg)
        except Exception:
            return ""
    if href.startswith(("http://", "https://")):
        return href
    return ""


async def _wikipedia_opensearch(query: str) -> list[SearchHit]:
    async with httpx.AsyncClient(
        follow_redirects=True,
        headers={"User-Agent": settings.user_agent},
        timeout=settings.request_timeout_sec,
    ) as client:
        resp = await client.get(
            "https://ru.wikipedia.org/w/api.php",
            params={
                "action": "opensearch",
                "search": query,
                "limit": settings.search_max_results,
                "namespace": 0,
                "format": "json",
            },
        )
        resp.raise_for_status()
        data = resp.json()

    if not isinstance(data, list) or len(data) < 4:
        return []
    titles: list[str] = data[1]
    snippets: list[str] = data[2]
    urls: list[str] = data[3]
    hits: list[SearchHit] = []
    for title, snippet, url in zip(titles, snippets, urls):
        if url.startswith("http"):
            hits.append(
                SearchHit(
                    title=title,
                    url=url,
                    snippet=snippet,
                    source="wikipedia",
                )
            )
    return hits


async def _duckduckgo_instant(query: str) -> list[SearchHit]:
    async with httpx.AsyncClient(
        follow_redirects=True,
        headers={"User-Agent": settings.user_agent},
        timeout=settings.request_timeout_sec,
    ) as client:
        resp = await client.get(
            "https://api.duckduckgo.com/",
            params={
                "q": query,
                "format": "json",
                "no_html": 1,
                "skip_disambig": 1,
            },
        )
        resp.raise_for_status()
        data = resp.json()

    hits: list[SearchHit] = []
    abstract_url = (data.get("AbstractURL") or "").strip()
    if abstract_url.startswith("http"):
        hits.append(
            SearchHit(
                title=(data.get("Heading") or query).strip() or query,
                url=abstract_url,
                snippet=(data.get("AbstractText") or "").strip(),
                source="duckduckgo",
            )
        )
    for topic in data.get("RelatedTopics") or []:
        if isinstance(topic, dict) and topic.get("FirstURL"):
            hits.append(
                SearchHit(
                    title=(topic.get("Text") or "").strip() or topic["FirstURL"],
                    url=topic["FirstURL"],
                    snippet="",
                    source="duckduckgo",
                )
            )
        elif isinstance(topic, dict):
            for sub in topic.get("Topics") or []:
                if sub.get("FirstURL"):
                    hits.append(
                        SearchHit(
                            title=(sub.get("Text") or "").strip() or sub["FirstURL"],
                            url=sub["FirstURL"],
                            snippet="",
                            source="duckduckgo",
                        )
                    )
    return hits[: settings.search_max_results]
