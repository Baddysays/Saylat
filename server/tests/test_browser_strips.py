import asyncio

import pytest

from app.browser_strips import BrowserStripsError, build_browser_strip_page, playwright_available


@pytest.mark.integration
@pytest.mark.skipif(not playwright_available(), reason="Playwright/Chromium not installed")
def test_browser_strips_example_com():
    page = asyncio.run(build_browser_strip_page("https://example.com"))
    assert page.render_engine == "browser"
    assert len(page.strips) >= 1
    assert page.strips[0].src.startswith("data:image/jpeg;base64,")
    assert page.strips[0].height >= 40


@pytest.mark.integration
def test_build_strip_page_browser_engine_integration():
    from app.screenshot_strips import build_strip_page

    if not playwright_available():
        pytest.skip("Playwright not installed")
    page = asyncio.run(
        build_strip_page("https://example.com", images_mode="tiny", engine="browser")
    )
    assert page.strips
    assert page.render_engine in ("browser", "browser_fallback_pillow")


def test_build_strip_page_pillow_engine():
    from app.screenshot_strips import build_strip_page

    page = asyncio.run(
        build_strip_page("https://example.com", images_mode="tiny", engine="pillow")
    )
    assert page.render_engine == "pillow"
