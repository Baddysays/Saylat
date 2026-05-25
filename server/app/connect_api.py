"""API подключения сервисов (вход при Wi‑Fi)."""

from .connectors import connection_status
from .connectors import telegram_conn
from .credentials_store import load_credentials, to_public, update_credentials
from .models import (
    ConnectStatusResponse,
    ServiceCredentialsPublic,
    ServiceCredentialsUpdate,
    TelegramCodeRequest,
    TelegramSignInRequest,
)


async def get_connect_status() -> ConnectStatusResponse:
    raw = connection_status()
    return ConnectStatusResponse(**raw)


async def telegram_request_code(body: TelegramCodeRequest) -> dict[str, str]:
    return await telegram_conn.send_login_code(body.phone)


async def telegram_sign_in(body: TelegramSignInRequest) -> dict[str, str]:
    return await telegram_conn.confirm_login(body.phone, body.code)


def get_service_credentials() -> ServiceCredentialsPublic:
    return to_public(load_credentials())


def save_service_credentials(body: ServiceCredentialsUpdate) -> ServiceCredentialsPublic:
    merged = update_credentials(body)
    return to_public(merged)
