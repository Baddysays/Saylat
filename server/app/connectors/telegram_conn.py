"""Telegram через Telethon (сессия на VPS)."""

from __future__ import annotations

import time
from typing import Any

from fastapi import HTTPException

from ..config import settings
from ..credentials_store import (
    effective_telegram_api_hash,
    effective_telegram_api_id,
    telegram_has_api_keys,
)
from ..models import ActResponse, FeedItem, FeedStats, OpenResponse, SaylatFeed

_pending: dict[str, dict[str, Any]] = {}


def _client():
    try:
        from telethon import TelegramClient
    except ImportError as exc:
        raise HTTPException(
            status_code=501,
            detail="Установите telethon на сервере: pip install telethon",
        ) from exc
    if not telegram_has_api_keys():
        raise HTTPException(
            status_code=503,
            detail="Telegram: введите API ID и API Hash в настройках приложения и сохраните",
        )
    return TelegramClient(
        settings.telegram_session_path,
        effective_telegram_api_id(),
        effective_telegram_api_hash(),
    )


async def is_authorized() -> bool:
    client = _client()
    async with client:
        return await client.is_user_authorized()


async def send_login_code(phone: str) -> dict[str, str]:
    phone = phone.strip()
    if not phone.startswith("+"):
        phone = f"+{phone}"
    client = _client()
    async with client:
        await client.connect()
        sent = await client.send_code_request(phone)
        _pending[phone] = {"phone_code_hash": sent.phone_code_hash}
        return {
            "phone": phone,
            "message": "Код отправлен в Telegram. Введите его в приложении (при Wi‑Fi).",
        }


async def confirm_login(phone: str, code: str) -> dict[str, str]:
    phone = phone.strip()
    if not phone.startswith("+"):
        phone = f"+{phone}"
    meta = _pending.get(phone)
    if not meta:
        raise HTTPException(status_code=400, detail="Сначала запросите код для этого номера")
    client = _client()
    async with client:
        await client.connect()
        await client.sign_in(phone, code, phone_code_hash=meta["phone_code_hash"])
        _pending.pop(phone, None)
        me = await client.get_me()
        name = me.first_name or "Telegram"
        return {"message": f"Вход выполнен: {name}. Лента доступна на 2G."}


async def open_telegram(resource_id: str | None) -> OpenResponse:
    client = _client()
    started = time.perf_counter()
    async with client:
        if not await client.is_user_authorized():
            raise HTTPException(
                status_code=401,
                detail="Telegram не подключён. Войдите при нормальном интернете (код из SMS/Telegram).",
            )
        if resource_id:
            return await _open_chat(client, resource_id, started)
        return await _open_dialogs(client, started)


async def _open_dialogs(client, started: float) -> OpenResponse:
    items: list[FeedItem] = []
    async for dialog in client.iter_dialogs(limit=40):
        ent = dialog.entity
        title = dialog.name or "Чат"
        last = ""
        if dialog.message and dialog.message.message:
            last = dialog.message.message[:200]
        chat_id = str(dialog.id)
        items.append(
            FeedItem(
                id=f"tg-{chat_id}",
                kind="chat",
                title=title,
                body=last or "Открыть переписку",
                unread=bool(dialog.unread_count),
                href=f"saylat://telegram/{chat_id}",
                actions=["open"],
            )
        )
    ms = int((time.perf_counter() - started) * 1000)
    feed = SaylatFeed(
        source="telegram",
        title="Telegram",
        subtitle="Диалоги (только текст)",
        items=items,
        stats=FeedStats(fetch_ms=ms),
    )
    return OpenResponse(kind="feed", feed=feed)


async def _open_chat(client, chat_id: str, started: float) -> OpenResponse:
    try:
        peer = int(chat_id)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="Некорректный chat_id") from exc
    items: list[FeedItem] = []
    entity = await client.get_entity(peer)
    title = getattr(entity, "title", None) or getattr(entity, "first_name", "Чат")
    async for msg in client.iter_messages(peer, limit=settings.telegram_message_limit):
        if not msg.message:
            continue
        who = "Вы" if msg.out else "Собеседник"
        items.append(
            FeedItem(
                id=f"tgmsg-{msg.id}",
                kind="message",
                from_=who,
                title=who,
                body=msg.message[:500],
                time=msg.date.isoformat() if msg.date else "",
                actions=["reply"] if not msg.out else [],
            )
        )
    items.reverse()
    if not items:
        items.append(
            FeedItem(
                id="tg-empty",
                kind="notice",
                title="Нет текстовых сообщений",
                body="В этом чате нет текстовых сообщений в последних N.",
            )
        )
    ms = int((time.perf_counter() - started) * 1000)
    feed = SaylatFeed(
        source="telegram",
        title=str(title),
        subtitle=f"Чат {chat_id}",
        context_id=str(chat_id),
        items=items,
        stats=FeedStats(fetch_ms=ms),
    )
    return OpenResponse(kind="feed", feed=feed)


async def act_telegram(
    item_id: str,
    action: str,
    body: str | None,
    context_id: str | None = None,
) -> ActResponse:
    if action != "reply":
        raise HTTPException(status_code=400, detail=f"Для Telegram не поддерживается: {action}")
    if not body or not body.strip():
        raise HTTPException(status_code=400, detail="Текст ответа обязателен")
    chat_id = (context_id or "").strip()
    if not chat_id and item_id.startswith("tgmsg-"):
        raise HTTPException(
            status_code=400,
            detail="Укажите context_id (id чата) — откройте переписку из списка диалогов",
        )
    if not chat_id and item_id.startswith("tg-"):
        chat_id = item_id.removeprefix("tg-").split("-")[0]
    if not chat_id:
        raise HTTPException(status_code=400, detail="Откройте чат и укажите context_id")
    client = _client()
    async with client:
        await client.send_message(int(chat_id), body.strip())
    return ActResponse(ok=True, message="Сообщение отправлено")
