"""Прокси WebView и визуальный рендер."""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_proxy_page_returns_html():
    r = client.get(
        "/api/proxy/page",
        params={"url": "https://example.com"},
    )
    assert r.status_code == 200
    assert "text/html" in r.headers.get("content-type", "")
    assert "saylat-banner" in r.text
    assert "Example Domain" in r.text or "example" in r.text.lower()


def test_proxy_vk_no_bad_browser():
    r = client.get(
        "/api/proxy/page",
        params={"url": "https://m.vk.com/durov"},
    )
    assert r.status_code == 200
    assert "BadBrowser" not in r.text
    assert "браузер устарел" not in r.text.lower()


def test_render_visual_has_tiles():
    r = client.get(
        "/api/render/visual",
        params={"url": "https://example.com", "images": "off"},
    )
    assert r.status_code == 200
    data = r.json()
    assert data["title"]
    assert len(data["tiles"]) >= 1
    kinds = {t["kind"] for t in data["tiles"]}
    assert "paragraph" in kinds or "heading" in kinds
