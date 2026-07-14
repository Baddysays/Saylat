"""Модуль статистики трафика — сколько данных экономит Saylat для пользователей на медленных сетях.

Отслеживает каждый запрос через прокси (extract/strips/visual/open/search),
считает оригинальный и сжатый размер, агрегирует по:
  - эндпоинтам (extract, strips, visual, open, search)
  - часам (последние 24 часа)
  - доменам (топ-10 по экономии)

Потокобезопасно через asyncio.Lock. Память ограничена: храним только
последние N записей (default 10000) и часовые агрегаты за 24 часа.
"""

from __future__ import annotations

import asyncio
import hashlib
import logging
import time
from collections import defaultdict
from datetime import datetime, timedelta, timezone

from pydantic import BaseModel, Field

log = logging.getLogger(__name__)

# ─── Pydantic модели ───────────────────────────────────────────────────────────


class TrafficRecord(BaseModel):
    """Запись об одном запросе через прокси."""

    endpoint: str
    original_bytes: int
    compressed_bytes: int
    url: str = ""  # хэш URL для приватности
    timestamp: float = 0.0


class TrafficStatsResponse(BaseModel):
    """Агрегированная статистика экономии трафика."""

    total_requests: int = 0
    total_original_bytes: int = 0
    total_compressed_bytes: int = 0
    savings_percent: float = 0.0
    savings_human: str = ""  # например "97.3%"
    by_endpoint: dict[str, dict] = Field(default_factory=dict)
    # endpoint → {requests, original, compressed, savings_percent}
    by_hour: list[dict] = Field(default_factory=list)
    # последние 24 часа: [{hour, requests, original, compressed, savings_percent}]
    top_domains: list[dict] = Field(default_factory=list)
    # топ-10: [{domain, original, compressed, saved, savings_percent}]


# ─── Внутреннее состояние ──────────────────────────────────────────────────────

_MAX_RECORDS: int = 10_000
_LOCK: asyncio.Lock = asyncio.Lock()

# Кольцевой буфер последних записей
_records: list[TrafficRecord] = []

# Агрегаты по эндпоинтам: endpoint → {requests, original, compressed}
_endpoint_stats: dict[str, dict[str, int]] = defaultdict(
    lambda: {"requests": 0, "original": 0, "compressed": 0}
)

# Почасовые агрегаты: "YYYY-MM-DD HH" → {requests, original, compressed}
_hourly_stats: dict[str, dict[str, int]] = defaultdict(
    lambda: {"requests": 0, "original": 0, "compressed": 0}
)

# Агрегаты по доменам: domain → {original, compressed}
_domain_stats: dict[str, dict[str, int]] = defaultdict(
    lambda: {"original": 0, "compressed": 0}
)

# Глобальные счётчики (не сбрасываются при очистке буфера)
_total_requests: int = 0
_total_original: int = 0
_total_compressed: int = 0


# ─── Утилиты ───────────────────────────────────────────────────────────────────


def _hash_url(url: str) -> str:
    """Необратимый хэш URL для приватности — первые 12 символов SHA-256."""
    if not url:
        return ""
    return hashlib.sha256(url.encode("utf-8")).hexdigest()[:12]


def _domain_from_url(url: str) -> str:
    """Извлечь домен второго уровня из URL для группировки."""
    if not url:
        return ""
    try:
        stripped = url
        if "://" in stripped:
            stripped = stripped.split("://", 1)[1]
        host = stripped.split("/", 1)[0]
        # Убираем порт
        if ":" in host:
            host = host.split(":", 1)[0]
        # Берём последние два сегмента (example.com)
        parts = host.split(".")
        if len(parts) > 2:
            host = ".".join(parts[-2:])
        return host.lower()
    except Exception:
        return ""


def _hour_key(ts: float | None = None) -> str:
    """Ключ часа для агрегата: '2025-01-15 14'."""
    t = datetime.fromtimestamp(ts or time.time(), tz=timezone.utc)
    return t.strftime("%Y-%m-%d %H")


def _prune_old_hours() -> None:
    """Удалить часовые агрегаты старше 25 часов. Вызывать под блокировкой."""
    cutoff = datetime.now(timezone.utc) - timedelta(hours=25)
    cutoff_key = cutoff.strftime("%Y-%m-%d %H")
    stale = [k for k in _hourly_stats if k < cutoff_key]
    for k in stale:
        del _hourly_stats[k]


def _format_bytes(n: int) -> str:
    """Человекочитаемый размер: 1234567 → '1.2 MB'."""
    if n < 1024:
        return f"{n} B"
    if n < 1024 * 1024:
        return f"{n / 1024:.1f} KB"
    return f"{n / (1024 * 1024):.1f} MB"


# ─── Публичный API ─────────────────────────────────────────────────────────────


def configure(max_records: int = 10_000) -> None:
    """Настроить лимит хранимых записей. Вызывать до старта приложения."""
    global _MAX_RECORDS
    _MAX_RECORDS = max(100, max_records)


async def record_traffic(
    endpoint: str,
    original_bytes: int,
    compressed_bytes: int,
    url: str | None = None,
) -> None:
    """Записать один запрос в статистику трафика.

    Потокобезопасно — использует asyncio.Lock.
    Автоматически извлекает домен и хэширует URL.
    """
    global _total_requests, _total_original, _total_compressed

    if original_bytes <= 0 or compressed_bytes <= 0:
        return

    now = time.time()
    domain = _domain_from_url(url) if url else ""
    hashed = _hash_url(url) if url else ""

    record = TrafficRecord(
        endpoint=endpoint,
        original_bytes=original_bytes,
        compressed_bytes=compressed_bytes,
        url=hashed,
        timestamp=now,
    )

    async with _LOCK:
        # Обновляем глобальные счётчики
        _total_requests += 1
        _total_original += original_bytes
        _total_compressed += compressed_bytes

        # Кольцевой буфер — удаляем батчами для эффективности
        _records.append(record)
        if len(_records) > _MAX_RECORDS:
            excess = len(_records) - _MAX_RECORDS
            del _records[:excess]

        # Агрегат по эндпоинту
        ep = _endpoint_stats[endpoint]
        ep["requests"] += 1
        ep["original"] += original_bytes
        ep["compressed"] += compressed_bytes

        # Агрегат по часу
        hk = _hour_key(now)
        hour = _hourly_stats[hk]
        hour["requests"] += 1
        hour["original"] += original_bytes
        hour["compressed"] += compressed_bytes

        # Агрегат по домену
        if domain:
            ds = _domain_stats[domain]
            ds["original"] += original_bytes
            ds["compressed"] += compressed_bytes

        # Периодическая очистка старых агрегатов
        _prune_old_hours()

        # Ограничиваем память доменов (топ-200 достаточно)
        if len(_domain_stats) > 200:
            _prune_small_domains()

    log.debug(
        "traffic: %s original=%s compressed=%s savings=%.1f%% domain=%s",
        endpoint,
        _format_bytes(original_bytes),
        _format_bytes(compressed_bytes),
        (1 - compressed_bytes / original_bytes) * 100 if original_bytes > 0 else 0,
        domain or "—",
    )


def _prune_small_domains() -> None:
    """Оставить топ-100 доменов по экономии. Вызывать под блокировкой."""
    if len(_domain_stats) <= 200:
        return
    scored = [
        (dom, stats["original"] - stats["compressed"])
        for dom, stats in _domain_stats.items()
    ]
    scored.sort(key=lambda x: x[1], reverse=True)
    keep = {dom for dom, _ in scored[:100]}
    for dom in list(_domain_stats.keys()):
        if dom not in keep:
            del _domain_stats[dom]


async def get_traffic_stats() -> TrafficStatsResponse:
    """Получить агрегированную статистику трафика. Потокобезопасно."""
    async with _LOCK:
        # Глобальные метрики
        savings_pct = 0.0
        if _total_original > 0:
            savings_pct = round(
                (1 - _total_compressed / _total_original) * 100, 1
            )

        # По эндпоинтам
        by_endpoint: dict[str, dict] = {}
        for ep, stats in _endpoint_stats.items():
            ep_savings = 0.0
            if stats["original"] > 0:
                ep_savings = round(
                    (1 - stats["compressed"] / stats["original"]) * 100, 1
                )
            by_endpoint[ep] = {
                "requests": stats["requests"],
                "original": stats["original"],
                "compressed": stats["compressed"],
                "savings_percent": ep_savings,
            }

        # По часам — последние 24 часа, от старых к новым
        sorted_hours = sorted(_hourly_stats.keys(), reverse=True)[:24]
        by_hour: list[dict] = []
        for hk in reversed(sorted_hours):
            hstats = _hourly_stats[hk]
            h_savings = 0.0
            if hstats["original"] > 0:
                h_savings = round(
                    (1 - hstats["compressed"] / hstats["original"]) * 100, 1
                )
            by_hour.append(
                {
                    "hour": hk,
                    "requests": hstats["requests"],
                    "original": hstats["original"],
                    "compressed": hstats["compressed"],
                    "savings_percent": h_savings,
                }
            )

        # Топ-10 доменов по экономии (original - compressed)
        domain_savings: list[tuple[str, int, int, int]] = []
        for domain, ds in _domain_stats.items():
            saved = ds["original"] - ds["compressed"]
            domain_savings.append((domain, saved, ds["original"], ds["compressed"]))
        domain_savings.sort(key=lambda x: x[1], reverse=True)

        top_domains: list[dict] = []
        for domain, saved, orig, comp in domain_savings[:10]:
            d_savings = 0.0
            if orig > 0:
                d_savings = round((1 - comp / orig) * 100, 1)
            top_domains.append(
                {
                    "domain": domain,
                    "original": orig,
                    "compressed": comp,
                    "saved": saved,
                    "savings_percent": d_savings,
                }
            )

        return TrafficStatsResponse(
            total_requests=_total_requests,
            total_original_bytes=_total_original,
            total_compressed_bytes=_total_compressed,
            savings_percent=savings_pct,
            savings_human=f"{savings_pct}%",
            by_endpoint=by_endpoint,
            by_hour=by_hour,
            top_domains=top_domains,
        )
