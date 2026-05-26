"""Скриншот страницы через Playwright и нарезка на JPEG-полосы (Opera Mini)."""

from __future__ import annotations

import asyncio
import base64
import io
import time
from typing import Literal

from PIL import Image

from .config import settings
from .http_ua import normalize_fetch_url, ua_for_url
from .models import ArticleLink, StripPageResponse, StripSegment, StripStats
from .strip_adblock import inject_adblock

RenderEngine = Literal["browser", "pillow", "browser_fallback_pillow"]

_playwright = None
_browser = None
_browser_lock = asyncio.Lock()
_render_semaphore: asyncio.Semaphore | None = None
_active_renders: int = 0
_total_renders: int = 0


class BrowserStripsError(RuntimeError):
    pass


def playwright_render_status() -> dict[str, int | bool]:
    return {
        "enabled": settings.playwright_enabled,
        "available": playwright_available(),
        "active_renders": _active_renders,
        "max_concurrent": max(1, settings.playwright_max_concurrent),
        "total_renders": _total_renders,
    }


def playwright_available() -> bool:
    if not settings.playwright_enabled:
        return False
    try:
        import playwright  # noqa: F401

        return True
    except ImportError:
        return False


def _semaphore() -> asyncio.Semaphore:
    global _render_semaphore
    if _render_semaphore is None:
        _render_semaphore = asyncio.Semaphore(max(1, settings.playwright_max_concurrent))
    return _render_semaphore


async def _ensure_browser():
    global _playwright, _browser
    async with _browser_lock:
        if _browser is not None:
            return _browser
        from playwright.async_api import async_playwright

        _playwright = await async_playwright().start()
        _browser = await _playwright.chromium.launch(
            headless=True,
            args=[
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--font-render-hinting=none",
            ],
        )
        return _browser


async def shutdown_browser() -> None:
    global _playwright, _browser
    async with _browser_lock:
        if _browser is not None:
            await _browser.close()
            _browser = None
        if _playwright is not None:
            await _playwright.stop()
            _playwright = None


def _jpeg_data_url(img: Image.Image, quality: int) -> tuple[str, int]:
    buf = io.BytesIO()
    rgb = img.convert("RGB")
    rgb.save(buf, format="JPEG", quality=quality, optimize=True)
    raw = buf.getvalue()
    b64 = base64.b64encode(raw).decode("ascii")
    return f"data:image/jpeg;base64,{b64}", len(raw)


def _slice_full_page(
    img: Image.Image,
    *,
    strip_width: int,
    slice_height: int,
    max_strips: int,
) -> list[Image.Image]:
    w, h = img.size
    if w > strip_width:
        ratio = strip_width / w
        img = img.resize((strip_width, max(1, int(h * ratio))), Image.Resampling.BILINEAR)
        w, h = img.size
    if h <= slice_height:
        return [img]
    strips: list[Image.Image] = []
    y = 0
    while y < h and len(strips) < max_strips:
        bottom = min(y + slice_height, h)
        crop = img.crop((0, y, w, bottom))
        if crop.size[1] < 24:
            break
        strips.append(crop)
        y = bottom
    return strips or [img]


async def build_browser_strip_page(
    url: str,
    *,
    original_bytes_hint: int = 0,
    site_profile: str = "generic",
    links: list[ArticleLink] | None = None,
) -> StripPageResponse:
    if not playwright_available():
        raise BrowserStripsError("Playwright не установлен или отключён (SAYLAT_PLAYWRIGHT_ENABLED)")

    started = time.perf_counter()
    fetch_url = normalize_fetch_url(url)
    ua = ua_for_url(fetch_url)
    width = settings.strip_viewport_width
    timeout_ms = int(settings.playwright_timeout_sec * 1000)

    global _active_renders, _total_renders
    _total_renders += 1
    _active_renders += 1
    try:
        return await _build_browser_strip_page_inner(
            fetch_url,
            ua,
            width,
            timeout_ms,
            original_bytes_hint,
            site_profile,
            links or [],
            started,
        )
    finally:
        _active_renders = max(0, _active_renders - 1)


async def _build_browser_strip_page_inner(
    fetch_url: str,
    ua: str,
    width: int,
    timeout_ms: int,
    original_bytes_hint: int,
    site_profile: str,
    links: list[ArticleLink],
    started: float,
) -> StripPageResponse:
    from playwright.async_api import TimeoutError as PlaywrightTimeout

    async with _semaphore():
        browser = await _ensure_browser()
        context = await browser.new_context(
            viewport={"width": width, "height": 900},
            user_agent=ua,
            device_scale_factor=1,
            locale="ru-RU",
        )
        page = await context.new_page()
        try:
            response = await page.goto(
                fetch_url,
                wait_until=settings.playwright_wait_until,
                timeout=timeout_ms,
            )
            original_bytes = original_bytes_hint
            if response:
                body = await response.body()
                original_bytes = max(original_bytes, len(body))
            await page.wait_for_timeout(settings.playwright_settle_ms)
            await inject_adblock(page)
            title = (await page.title()).strip() or fetch_url
            png_bytes = await page.screenshot(full_page=True, type="png")
        except PlaywrightTimeout as exc:
            raise BrowserStripsError(f"Таймаут загрузки страницы ({settings.playwright_timeout_sec}s)") from exc
        except Exception as exc:
            raise BrowserStripsError(str(exc)) from exc
        finally:
            await context.close()

    img = Image.open(io.BytesIO(png_bytes))
    tiles = _slice_full_page(
        img,
        strip_width=width,
        slice_height=settings.strip_slice_height,
        max_strips=settings.strip_max_count,
    )
    quality = settings.strip_jpeg_quality
    strips_out: list[StripSegment] = []
    total_bytes = 0
    for idx, tile in enumerate(tiles):
        src, nbytes = _jpeg_data_url(tile, quality=quality)
        total_bytes += nbytes
        strips_out.append(
            StripSegment(
                index=idx,
                src=src,
                width=tile.size[0],
                height=tile.size[1],
                bytes_approx=nbytes,
            )
        )

    elapsed = int((time.perf_counter() - started) * 1000)
    return StripPageResponse(
        url=fetch_url,
        title=title[:200],
        site_profile=site_profile,  # type: ignore[arg-type]
        strips=strips_out,
        links=links,
        strip_width=width,
        render_engine="browser",
        stats=StripStats(
            original_bytes=original_bytes,
            payload_bytes=total_bytes,
            strip_count=len(strips_out),
            fetch_ms=elapsed,
            build_ms=elapsed,
        ),
    )
