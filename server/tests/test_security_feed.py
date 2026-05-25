import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.main import app


@pytest.fixture
def client(monkeypatch):
    monkeypatch.setattr(settings, "api_key", "")
    monkeypatch.setattr(settings, "rate_limit_per_minute", 1000)
    return TestClient(app)


def test_unified_feed_returns_structure(client):
    r = client.get("/api/feed?limit=3")
    assert r.status_code == 200
    data = r.json()
    assert data["source"] == "unified"
    assert "items" in data
    assert isinstance(data["items"], list)


def test_api_key_when_configured(monkeypatch):
    monkeypatch.setattr(settings, "api_key", "secret-test-key")
    monkeypatch.setattr(settings, "rate_limit_per_minute", 1000)
    c = TestClient(app)
    r = c.get("/api/extract?url=https://example.com")
    assert r.status_code == 401
    r2 = c.get(
        "/api/extract?url=https://example.com",
        headers={"X-API-Key": "secret-test-key"},
    )
    assert r2.status_code in (200, 502)
