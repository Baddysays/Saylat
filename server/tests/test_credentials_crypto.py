from cryptography.fernet import Fernet

from app import credentials_store
from app.config import settings
from app.models import ServiceCredentials


def test_credentials_encrypt_roundtrip(tmp_path, monkeypatch):
    key = Fernet.generate_key().decode()
    creds_path = tmp_path / "credentials.json"
    monkeypatch.setattr(credentials_store, "_CREDENTIALS_PATH", creds_path)
    monkeypatch.setattr(credentials_store, "_cached", None)
    monkeypatch.setattr(settings, "credentials_key", key)

    creds = ServiceCredentials(
        telegram_api_hash="secret-hash",
        mail_password="mail-pass",
        vk_access_token="vk-token",
        dzen_session_cookie="dzen-cookie",
    )
    credentials_store.save_credentials(creds)

    raw = creds_path.read_text(encoding="utf-8")
    assert "secret-hash" not in raw
    assert "mail-pass" not in raw

    loaded = credentials_store.load_credentials()
    assert loaded.telegram_api_hash == "secret-hash"
    assert loaded.mail_password == "mail-pass"
    assert loaded.vk_access_token == "vk-token"
    assert loaded.dzen_session_cookie == "dzen-cookie"
