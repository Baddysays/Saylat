"""Объединённая лента: Telegram + VK + почта (если настроены на VPS)."""

from __future__ import annotations

import time

from fastapi import HTTPException

from .connectors import mail_conn, telegram_conn, vk_conn
from .credentials_store import mail_is_configured, telegram_has_api_keys, vk_is_configured
from .models import FeedItem, FeedStats, SaylatFeed


async def build_unified_feed(*, per_source: int = 12) -> SaylatFeed:
    started = time.perf_counter()
    merged: list[FeedItem] = []
    notes: list[str] = []

    if telegram_has_api_keys():
        try:
            opened = await telegram_conn.open_telegram(None)
            if opened.feed:
                for item in opened.feed.items[:per_source]:
                    merged.append(
                        item.model_copy(
                            update={
                                "title": f"TG · {item.title}",
                                "id": f"unified-{item.id}",
                            }
                        )
                    )
        except HTTPException as exc:
            notes.append(f"Telegram: {exc.detail}")
        except Exception as exc:
            notes.append(f"Telegram: {exc}")

    if vk_is_configured():
        try:
            opened = await vk_conn.open_vk(None)
            if opened.feed:
                for item in opened.feed.items[:per_source]:
                    merged.append(
                        item.model_copy(
                            update={
                                "title": f"VK · {item.title}",
                                "id": f"unified-{item.id}",
                            }
                        )
                    )
        except HTTPException as exc:
            notes.append(f"VK: {exc.detail}")
        except Exception as exc:
            notes.append(f"VK: {exc}")

    if mail_is_configured():
        try:
            opened = await mail_conn.open_mail(None)
            if opened.feed:
                for item in opened.feed.items[:per_source]:
                    merged.append(
                        item.model_copy(
                            update={
                                "title": f"✉ · {item.title}",
                                "id": f"unified-{item.id}",
                            }
                        )
                    )
        except HTTPException as exc:
            notes.append(f"Почта: {exc.detail}")
        except Exception as exc:
            notes.append(f"Почта: {exc}")

    if notes and not merged:
        merged.append(
            FeedItem(
                id="unified-hint",
                kind="notice",
                title="Сервисы не подключены",
                body=" ".join(notes),
            )
        )
    elif notes:
        merged.insert(
            0,
            FeedItem(
                id="unified-partial",
                kind="notice",
                title="Часть лент недоступна",
                body=" · ".join(notes[:3]),
            ),
        )

    if not merged:
        merged.append(
            FeedItem(
                id="unified-empty",
                kind="notice",
                title="Настройте мессенджеры",
                body=(
                    "В приложении: Telegram API, токен VK или IMAP. "
                    "Сохраните на VPS при Wi‑Fi."
                ),
            )
        )

    ms = int((time.perf_counter() - started) * 1000)
    return SaylatFeed(
        source="unified",
        title="Все ленты",
        subtitle="Telegram · VK · почта",
        items=merged,
        stats=FeedStats(fetch_ms=ms),
    )
