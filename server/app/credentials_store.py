"""Учётные данные с телефона → файл на VPS (data/credentials.json)."""

from __future__ import annotations

import json
import threading
from pathlib import Path

from .config import settings
from .models import ServiceCredentials, ServiceCredentialsPublic, ServiceCredentialsUpdate

_CREDENTIALS_PATH = Path(settings.telegram_session_path).parent / "credentials.json"
_lock = threading.Lock()


def _load_unlocked() -> ServiceCredentials:
    if not _CREDENTIALS_PATH.is_file():
        return ServiceCredentials()
    try:
        raw = json.loads(_CREDENTIALS_PATH.read_text(encoding="utf-8"))
        return ServiceCredentials.model_validate(raw)
    except Exception:
        return ServiceCredentials()


def load_credentials() -> ServiceCredentials:
    with _lock:
        return _load_unlocked()


def save_credentials(creds: ServiceCredentials) -> None:
    _CREDENTIALS_PATH.parent.mkdir(parents=True, exist_ok=True)
    with _lock:
        _CREDENTIALS_PATH.write_text(
            creds.model_dump_json(indent=2),
            encoding="utf-8",
        )


def update_credentials(patch: ServiceCredentialsUpdate) -> ServiceCredentials:
    current = load_credentials()
    data = current.model_dump()
    for key, value in patch.model_dump(exclude_unset=True).items():
        if key in {"mail_password", "vk_access_token", "dzen_session_cookie"}:
            if value is None or (isinstance(value, str) and not value.strip()):
                continue
        if value is not None:
            data[key] = value
    merged = ServiceCredentials.model_validate(data)
    save_credentials(merged)
    return merged


def to_public(creds: ServiceCredentials) -> ServiceCredentialsPublic:
    return ServiceCredentialsPublic(
        telegram_api_id=creds.telegram_api_id,
        telegram_api_hash=creds.telegram_api_hash,
        mail_imap_host=creds.mail_imap_host,
        mail_imap_port=creds.mail_imap_port,
        mail_smtp_host=creds.mail_smtp_host,
        mail_smtp_port=creds.mail_smtp_port,
        mail_username=creds.mail_username,
        mail_password_set=bool(creds.mail_password),
        mail_use_ssl=creds.mail_use_ssl,
        vk_access_token_set=bool(creds.vk_access_token.strip()),
        dzen_session_cookie_set=bool(creds.dzen_session_cookie.strip()),
    )


def effective_telegram_api_id() -> int:
    stored = load_credentials().telegram_api_id
    return stored or settings.telegram_api_id


def effective_telegram_api_hash() -> str:
    stored = load_credentials().telegram_api_hash.strip()
    return stored or settings.telegram_api_hash.strip()


def effective_mail_imap_host() -> str:
    return load_credentials().mail_imap_host.strip() or settings.mail_imap_host.strip()


def effective_mail_username() -> str:
    return load_credentials().mail_username.strip() or settings.mail_username.strip()


def effective_mail_password() -> str:
    return load_credentials().mail_password or settings.mail_password


def effective_mail_imap_port() -> int:
    c = load_credentials()
    return c.mail_imap_port if c.mail_imap_host else settings.mail_imap_port


def effective_mail_smtp_host() -> str:
    c = load_credentials()
    return c.mail_smtp_host.strip() or settings.mail_smtp_host.strip() or effective_mail_imap_host()


def effective_mail_smtp_port() -> int:
    c = load_credentials()
    return c.mail_smtp_port if c.mail_smtp_host or c.mail_imap_host else settings.mail_smtp_port


def effective_mail_use_ssl() -> bool:
    return load_credentials().mail_use_ssl


def effective_vk_token() -> str:
    return load_credentials().vk_access_token.strip() or settings.vk_access_token.strip()


def effective_dzen_cookie() -> str:
    return load_credentials().dzen_session_cookie.strip() or settings.dzen_session_cookie.strip()


def telegram_session_file() -> Path:
    return Path(f"{settings.telegram_session_path}.session")


def telegram_has_api_keys() -> bool:
    return bool(effective_telegram_api_id() and effective_telegram_api_hash())


def telegram_has_session() -> bool:
    return telegram_session_file().is_file()


def mail_is_configured() -> bool:
    return bool(
        effective_mail_imap_host()
        and effective_mail_username()
        and effective_mail_password()
    )


def vk_is_configured() -> bool:
    return bool(effective_vk_token())


def dzen_is_configured() -> bool:
    return bool(effective_dzen_cookie())
