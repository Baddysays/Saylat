import asyncio

from app.screenshot_strips import build_strip_page


def test_render_strips_returns_jpeg_strips():
    page = asyncio.run(
        build_strip_page(
            "https://example.com",
            images_mode="tiny",
            engine="pillow",
        )
    )
    assert page.strips
    assert page.strips[0].src.startswith("data:image/jpeg;base64,")
    assert page.stats.strip_count >= 1
    assert page.stats.payload_bytes > 100
