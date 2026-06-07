from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_connect_status():
    r = client.get("/api/connect/status")
    assert r.status_code == 200
    data = r.json()
    assert "telegram" in data
    assert "mail" in data
    assert "vk" in data
    assert "dzen" in data


def test_open_vk_without_token():
    r = client.post(
        "/api/open",
        json={"target": "vk", "images": "off"},
    )
    assert r.status_code == 503


def test_credentials_get_put():
    r = client.get("/api/connect/credentials")
    assert r.status_code == 200
    data = r.json()
    assert "mail_password_set" in data
    assert "telegram_api_hash_set" in data
    assert "telegram_api_hash" not in data

    r2 = client.put(
        "/api/connect/credentials",
        json={
            "mail_imap_host": "imap.test.local",
            "mail_username": "user@test",
            "mail_password": "secret",
        },
    )
    assert r2.status_code == 200
    data = r2.json()
    assert data["mail_imap_host"] == "imap.test.local"
    assert data["mail_username"] == "user@test"
    assert data["mail_password_set"] is True
