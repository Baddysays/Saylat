"""Rate-limit pruning, CORS config, feed pagination."""

import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app
from app.security import RateLimitMiddleware


def test_rate_limit_prunes_idle_ips():
    mw = RateLimitMiddleware(app=None, limit_per_minute=60)  # type: ignore[arg-type]
    mw._last_seen["203.0.113.1"] = 0.0
    mw._prune_stale_ips(400.0)
    assert "203.0.113.1" not in mw._last_seen
    assert "203.0.113.1" not in mw._hits


def test_rate_limit_caps_dictionary_size():
    mw = RateLimitMiddleware(app=None, limit_per_minute=60)  # type: ignore[arg-type]
    now = 1000.0
    for i in range(5100):
        ip = f"10.0.{i // 256}.{i % 256}"
        mw._last_seen[ip] = now
        mw._hits[ip] = [now]
    mw._prune_stale_ips(now)
    assert len(mw._hits) <= 5000


def test_cors_origin_list_parsing(monkeypatch):
    monkeypatch.setattr(settings, "cors_origins", " https://a.test ,https://b.test, ")
    assert settings.cors_origin_list() == ["https://a.test", "https://b.test"]
    monkeypatch.setattr(settings, "cors_origins", "")
    assert settings.cors_origin_list() == []


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setattr(settings, "api_key", "")
    monkeypatch.setattr(settings, "rate_limit_per_minute", 1000)
    return TestClient(app)


def test_unified_feed_pagination_metadata(client):
    r = client.get("/api/feed?limit=3&offset=0&page_size=2")
    assert r.status_code == 200
    data = r.json()
    assert "has_more" in data
    assert "total_items" in data
    assert isinstance(data["has_more"], bool)
    assert data["total_items"] >= len(data["items"])


def test_unified_feed_offset_slice(client):
    full = client.get("/api/feed?limit=10&offset=0&page_size=80").json()
    total = full["total_items"]
    if total < 2:
        pytest.skip("feed too small for offset test")
    page0 = client.get("/api/feed?limit=10&offset=0&page_size=1").json()
    page1 = client.get("/api/feed?limit=10&offset=1&page_size=1").json()
    assert len(page0["items"]) == 1
    assert len(page1["items"]) == 1
    assert page0["items"][0]["id"] != page1["items"][0]["id"]
