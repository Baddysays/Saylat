"""Тесты открытия полного письма по item_id."""

from unittest.mock import MagicMock, patch

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)

SAMPLE_MSG = b"""From: Ivan <ivan@test.ru>
Subject: Test subject
Date: Mon, 25 May 2026 10:00:00 +0000
Content-Type: text/plain; charset=utf-8

Hello from mail body line one.

Second paragraph here.
"""


@patch("app.connectors.mail_conn.mail_is_configured", return_value=True)
@patch("app.connectors.mail_conn.effective_mail_imap_host", return_value="imap.test")
@patch("app.connectors.mail_conn.effective_mail_imap_port", return_value=993)
@patch("app.connectors.mail_conn.effective_mail_username", return_value="u@test")
@patch("app.connectors.mail_conn.effective_mail_password", return_value="secret")
@patch("app.connectors.mail_conn.effective_mail_use_ssl", return_value=True)
def test_open_mail_message_returns_article(*_mocks):
    conn = MagicMock()
    conn.fetch.return_value = ("OK", [(b"1", SAMPLE_MSG)])

    with patch("app.connectors.mail_conn._imap_connect", return_value=conn):
        r = client.post(
            "/api/open",
            json={"target": "mail", "resource_id": "mail-INBOX-1", "images": "off"},
        )

    assert r.status_code == 200
    data = r.json()
    assert data["kind"] == "article"
    article = data["article"]
    assert article["title"] == "Test subject"
    assert "Hello from mail body" in " ".join(
        b.get("text", "") or "" for b in article["blocks"] if b["type"] == "paragraph"
    )


def test_health_includes_playwright_block():
    r = client.get("/health")
    assert r.status_code == 200
    assert "playwright" in r.json()
