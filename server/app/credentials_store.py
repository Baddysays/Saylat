"""Учётные данные с телефона → файл на VPS (data/credentials.json)."""

from __future__ import annotations

import json
import logging
import threading
from pathlib import Path

from .config import settings
from .models import ServiceCredentials, ServiceCredentialsPublic, ServiceCredentialsUpdate

log = logging.getLogger(__name__)

_CREDENTIALS_PATH = Path(settings.telegram_session_path).parent / "credentials.json"
_lock = threading.Lock()
_cached: ServiceCredentials | None = None
_warned_plaintext = False

_SECRET_FIELDS = ("telegram_api_hash", "mail_password", "vk_access_token", "dzen_session_cookie")
_ENC_PREFIX = "fernet:"


def _fernet():
    key = settings.credentials_key.strip()
    if not key:
        return None
    from cryptography.fernet import Fernet

    return Fernet(key.encode())


def _encrypt_field(value: str) -> str:
    if not value or value.startswith(_ENC_PREFIX):
        return value
    f = _fernet()
    if f is None:
        global _warned_plaintext
        if not _warned_plaintext:
            log.warning(
                "SAYLAT_CREDENTIALS_KEY не задан — секреты в credentials.json хранятся в открытом виде"
            )
            _warned_plaintext = True
        return value
    return _ENC_PREFIX + f.encrypt(value.encode()).decode()


def _decrypt_field(value: str) -> str:
    if not value.startswith(_ENC_PREFIX):
        return value
    f = _fernet()
    if f is None:
        return value
    return f.decrypt(value[len(_ENC_PREFIX) :].encode()).decode()


def _encrypt_dict(data: dict) -> dict:
    out = dict(data)
    for field in _SECRET_FIELDS:
        raw = out.get(field)
        if raw:
            out[field] = _encrypt_field(str(raw))
    return out


def _decrypt_dict(data: dict) -> dict:
    out = dict(data)
    for field in _SECRET_FIELDS:
        raw = out.get(field)
        if raw:
            out[field] = _decrypt_field(str(raw))
    return out


def _load_unlocked() -> ServiceCredentials:
    if not _CREDENTIALS_PATH.is_file():
        return ServiceCredentials()
    try:
        raw = json.loads(_CREDENTIALS_PATH.read_text(encoding="utf-8"))
        return ServiceCredentials.model_validate(_decrypt_dict(raw))
    except Exception:
        return ServiceCredentials()


def load_credentials() -> ServiceCredentials:
    global _cached
    with _lock:
        if _cached is None:
            _cached = _load_unlocked()
        return _cached


def save_credentials(creds: ServiceCredentials) -> None:
    global _cached
    _CREDENTIALS_PATH.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(_encrypt_dict(creds.model_dump()), indent=2, ensure_ascii=False)
    with _lock:
        _CREDENTIALS_PATH.write_text(payload, encoding="utf-8")
        _cached = creds


def update_credentials(patch: ServiceCredentialsUpdate) -> ServiceCredentials:
    current = load_credentials()
    data = current.model_dump()
    for key, value in patch.model_dump(exclude_unset=True).items():
        if key in {"mail_password", "vk_access_token", "dzen_session_cookie", "telegram_api_hash"}:
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
        telegram_api_hash_set=bool(creds.telegram_api_hash.strip()),
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


class _EffectiveCredentials:
    """Один load_credentials() на цепочку effective_* вызовов."""

    __slots__ = ("_creds",)

    def __init__(self, creds: ServiceCredentials) -> None:
        self._creds = creds

    @classmethod
    def current(cls) -> _EffectiveCredentials:
        return cls(load_credentials())

    def telegram_api_id(self) -> int:
        return self._creds.telegram_api_id or settings.telegram_api_id

    def telegram_api_hash(self) -> str:
        return self._creds.telegram_api_hash.strip() or settings.telegram_api_hash.strip()

    def mail_imap_host(self) -> str:
        return self._creds.mail_imap_host.strip() or settings.mail_imap_host.strip()

    def mail_username(self) -> str:
        return self._creds.mail_username.strip() or settings.mail_username.strip()

    def mail_password(self) -> str:
        return self._creds.mail_password or settings.mail_password

    def mail_imap_port(self) -> int:
        return self._creds.mail_imap_port if self._creds.mail_imap_host else settings.mail_imap_port

    def mail_smtp_host(self) -> str:
        host = self._creds.mail_smtp_host.strip() or settings.mail_smtp_host.strip()
        return host or self.mail_imap_host()

    def mail_smtp_port(self) -> int:
        if self._creds.mail_smtp_host or self._creds.mail_imap_host:
            return self._creds.mail_smtp_port
        return settings.mail_smtp_port

    def mail_use_ssl(self) -> bool:
        return self._creds.mail_use_ssl

    def vk_token(self) -> str:
        return self._creds.vk_access_token.strip() or settings.vk_access_token.strip()

    def dzen_cookie(self) -> str:
        return self._creds.dzen_session_cookie.strip() or settings.dzen_session_cookie.strip()


def effective_telegram_api_id() -> int:
    return _EffectiveCredentials.current().telegram_api_id()


def effective_telegram_api_hash() -> str:
    return _EffectiveCredentials.current().telegram_api_hash()


def effective_mail_imap_host() -> str:
    return _EffectiveCredentials.current().mail_imap_host()


def effective_mail_username() -> str:
    return _EffectiveCredentials.current().mail_username()


def effective_mail_password() -> str:
    return _EffectiveCredentials.current().mail_password()


def effective_mail_imap_port() -> int:
    return _EffectiveCredentials.current().mail_imap_port()


def effective_mail_smtp_host() -> str:
    return _EffectiveCredentials.current().mail_smtp_host()


def effective_mail_smtp_port() -> int:
    return _EffectiveCredentials.current().mail_smtp_port()


def effective_mail_use_ssl() -> bool:
    return _EffectiveCredentials.current().mail_use_ssl()


def effective_vk_token() -> str:
    return _EffectiveCredentials.current().vk_token()


def effective_dzen_cookie() -> str:
    return _EffectiveCredentials.current().dzen_cookie()


def telegram_session_file() -> Path:
    return Path(f"{settings.telegram_session_path}.session")


def telegram_has_api_keys() -> bool:
    eff = _EffectiveCredentials.current()
    return bool(eff.telegram_api_id() and eff.telegram_api_hash())


def telegram_has_session() -> bool:
    return telegram_session_file().is_file()


def mail_is_configured() -> bool:
    eff = _EffectiveCredentials.current()
    return bool(eff.mail_imap_host() and eff.mail_username() and eff.mail_password())


def vk_is_configured() -> bool:
    return bool(_EffectiveCredentials.current().vk_token())


def dzen_is_configured() -> bool:
    return bool(_EffectiveCredentials.current().dzen_cookie())
