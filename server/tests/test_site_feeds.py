import pytest
from fastapi.testclient import TestClient

from app.main import app

from tests.extract_helpers import unwrap_extract_article

client = TestClient(app)


def test_open_pikabu_home_feed():
    r = client.post(
        "/api/open",
        json={"target": "url", "url": "https://pikabu.ru/", "images": "off"},
    )
    assert r.status_code == 200
    data = r.json()
    assert data["kind"] == "feed"
    feed = data["feed"]
    assert feed["source"] == "pikabu"
    assert len(feed["items"]) >= 1


def test_open_vk_feed_notice():
    r = client.post(
        "/api/open",
        json={"target": "url", "url": "https://vk.com/feed", "images": "off"},
    )
    assert r.status_code == 200
    data = r.json()
    assert data["kind"] == "feed"
    assert data["feed"]["source"] == "vk"


def test_extract_pikabu_via_compat():
    r = client.get(
        "/api/extract",
        params={"url": "https://pikabu.ru/", "images": "off"},
        headers={"X-Saylat-Payload-Codec": "identity"},
    )
    assert r.status_code == 200
    article = unwrap_extract_article(r.json())
    assert article["layout_hint"] == "feed"
    assert len(article["blocks"]) >= 1
