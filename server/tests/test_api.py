"""Smoke-тесты API Saylat (запуск: pytest из каталога server)."""

from fastapi.testclient import TestClient

from app.main import app

from tests.extract_helpers import unwrap_extract_article

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


def test_health_matches_app_update_version():
    health = client.get("/health").json()
    update = client.get("/api/app/update").json()
    assert health["app_version_code"] == update["version_code"]
    assert health["app_version_name"] == update["version_name"]


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
    article = data.get("article")
    if article is None and data.get("wire"):
        from app.payload_codec import decompress_article
        from app.models import WireCompressedPayload

        article = decompress_article(WireCompressedPayload.model_validate(data["wire"])).model_dump()
    assert article["url"]
    assert article["title"]
    assert isinstance(article["blocks"], list)
    assert article["stats"]["payload_bytes"] > 0


def test_extract_binary_endpoint():
    r = client.get(
        "/api/extract/binary",
        params={"url": "https://example.com", "images": "off", "level": "medium"},
        headers={"X-Saylat-Payload-Codec": "gzip-binary,zstd-binary"},
    )
    assert r.status_code == 200
    codec = r.headers.get("x-saylat-payload-codec", "")
    assert codec or "json" in r.headers.get("content-type", "")
    if codec and codec != "identity":
        from app.payload_codec import decompress_payload_bytes

        wire = int(r.headers.get("x-saylat-wire-bytes", "0"))
        raw = int(r.headers.get("x-saylat-uncompressed-bytes", "0"))
        article = decompress_payload_bytes(r.content, codec, wire, raw)
        assert article.title
    else:
        data = r.json()
        article = data.get("article") or data
        assert article.get("title") or article["title"]


def test_extract_wire_codec():
    r = client.get(
        "/api/extract",
        params={"url": "https://example.com", "images": "off", "level": "medium"},
        headers={"X-Saylat-Payload-Codec": "gzip-b64"},
    )
    assert r.status_code == 200
    data = r.json()
    if data.get("wire"):
        assert data["wire"]["wire_bytes"] > 0
        assert data["wire"]["wire_bytes"] <= data["wire"]["uncompressed_bytes"]
    else:
        assert data.get("article")


def test_search_requires_query():
    r = client.get("/api/search")
    assert r.status_code == 422


def test_extract_layout_keeps_image_placeholders():
    r = client.get(
        "/api/extract",
        params={"url": "https://example.com", "images": "layout"},
    )
    assert r.status_code == 200
    article = unwrap_extract_article(r.json())
    assert article["stats"]["images_inlined"] == 0


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
