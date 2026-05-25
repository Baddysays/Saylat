"""Smoke-тесты API Saylat (запуск: pytest из каталога server)."""

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    data = r.json()
    assert data["status"] == "ok"
    assert data["app_version_code"] >= 1
    assert data.get("search") is True


def test_app_update():
    r = client.get("/api/app/update")
    assert r.status_code == 200
    data = r.json()
    assert data["version_code"] >= 1
    assert "saylat.apk" in data["apk_url"]


def test_bench_lite():
    r = client.get("/api/bench/lite")
    assert r.status_code == 200
    assert len(r.content) >= 1024


def test_extract_example():
    r = client.get(
        "/api/extract",
        params={"url": "https://example.com", "images": "off"},
    )
    assert r.status_code == 200
    data = r.json()
    assert data["url"]
    assert data["title"]
    assert isinstance(data["blocks"], list)
    assert data["stats"]["payload_bytes"] > 0


def test_search_requires_query():
    r = client.get("/api/search")
    assert r.status_code == 422


def test_extract_layout_keeps_image_placeholders():
    r = client.get(
        "/api/extract",
        params={"url": "https://example.com", "images": "layout"},
    )
    assert r.status_code == 200
    data = r.json()
    assert data["stats"]["images_inlined"] == 0


def test_search_returns_web_hits():
    r = client.get("/api/search", params={"q": "python"})
    assert r.status_code == 200
    data = r.json()
    assert data["results"]
    assert data["engine"] != "none"
    urls = [hit["url"] for hit in data["results"]]
    assert any("wikipedia.org" in u or "python.org" in u for u in urls)


def test_translate_single():
    r = client.post(
        "/api/translate",
        json={"texts": ["Hello"], "source": "en", "target": "ru"},
    )
    assert r.status_code == 200
    data = r.json()
    assert len(data["translations"]) == 1
    assert data["translations"][0]
