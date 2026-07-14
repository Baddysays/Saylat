"""ASGI-прослойка для автоматического учёта экономии трафика Saylat.

Перехватывает ответы на ключевых эндпоинтах (extract, strips, visual, open),
измеряет реальный размер ответа (compressed_bytes), извлекает оригинальный
размер из JSON-тела или заголовков и добавляет заголовок:

    X-Saylat-Savings: original=N compressed=M savings=P%

Чистый ASGI middleware — без BaseHTTPMiddleware (нет проблем со streaming).
"""

from __future__ import annotations

import json
import logging
from urllib.parse import parse_qs

from starlette.types import ASGIApp, Message, Receive, Scope, Send

from .traffic_stats import record_traffic

log = logging.getLogger(__name__)

# Пути, которые отслеживаем
_TRACKED_PREFIXES = (
    "/api/extract",
    "/api/render/strips",
    "/api/render/visual",
    "/api/open",
)

# Маппинг путь → тип эндпоинта для статистики
_ENDPOINT_MAP = {
    "/api/extract": "extract",
    "/api/render/strips": "strips",
    "/api/render/visual": "visual",
    "/api/open": "open",
}


def _endpoint_type(path: str) -> str:
    """Определить тип эндпоинта по пути."""
    for prefix, name in _ENDPOINT_MAP.items():
        if path.startswith(prefix):
            return name
    return "unknown"


def should_buffer_traffic(path: str) -> bool:
    """Нужно ли буферизовать ответ для учёта трафика.

    Progressive SSE (/progressive) стримится — буферизация запрещена.
    """
    if path.rstrip("/").endswith("/progressive"):
        return False
    return any(path.startswith(p) for p in _TRACKED_PREFIXES)


def _url_from_scope(scope: Scope) -> str:
    """Извлечь параметр url из query string (GET-запросы)."""
    qs = scope.get("query_string", b"")
    if not qs:
        return ""
    try:
        parsed = parse_qs(qs.decode("utf-8", errors="replace"))
        values = parsed.get("url", [])
        return values[0] if values else ""
    except Exception:
        return ""


def _extract_original_bytes(
    headers: list[tuple[bytes, bytes]], body: bytes, endpoint: str
) -> int:
    """Попытаться извлечь original_bytes из заголовков или тела ответа.

    Приоритет:
      1. Заголовок X-Saylat-Original-Bytes (явно задан обработчиком)
      2. Заголовок X-Saylat-Uncompressed-Bytes (бинарные эндпоинты)
      3. Парсинг JSON-тела: stats.original_bytes, wire.uncompressed_bytes
    """
    # 1. Проверяем явный заголовок
    for name, value in headers:
        if name == b"x-saylat-original-bytes":
            try:
                return int(value)
            except (ValueError, TypeError):
                pass

    # 2. Заголовок бинарных эндпоинтов
    for name, value in headers:
        if name == b"x-saylat-uncompressed-bytes":
            try:
                return int(value)
            except (ValueError, TypeError):
                pass

    # 3. Парсинг JSON-тела
    if not body:
        return 0

    try:
        data = json.loads(body)
    except (json.JSONDecodeError, UnicodeDecodeError):
        return 0

    return _find_original_bytes_in_json(data, endpoint)


def _find_original_bytes_in_json(data: dict, endpoint: str) -> int:
    """Извлечь original_bytes из распарсенного JSON ответа.

    Структура ответов:
      extract/open:  { article: { stats: { original_bytes: N } }, wire: { uncompressed_bytes: M } }
      strips/visual: { stats: { original_bytes: N } }
    """
    # Вариант: wire.uncompressed_bytes (сжатая передача)
    wire = data.get("wire")
    if isinstance(wire, dict):
        ub = wire.get("uncompressed_bytes", 0)
        if isinstance(ub, int) and ub > 0:
            return ub

    # Вариант: article.stats.original_bytes
    article = data.get("article")
    if isinstance(article, dict):
        stats = article.get("stats")
        if isinstance(stats, dict):
            ob = stats.get("original_bytes", 0)
            if isinstance(ob, int) and ob > 0:
                return ob

    # Вариант: stats.original_bytes (strips, visual)
    stats = data.get("stats")
    if isinstance(stats, dict):
        ob = stats.get("original_bytes", 0)
        if isinstance(ob, int) and ob > 0:
            return ob

    return 0


class SaylatTrafficMiddleware:
    """Чистый ASGI middleware — автоматический учёт экономии трафика.

    Перехватывает ответы на отслеживаемых эндпоинтах, измеряет размер
    тела ответа, извлекает оригинальный размер и записывает в статистику.
    Добавляет заголовок X-Saylat-Savings с информацией об экономии.
    """

    def __init__(self, app: ASGIApp) -> None:
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        path = scope.get("path", "")
        if not should_buffer_traffic(path):
            await self.app(scope, receive, send)
            return

        endpoint = _endpoint_type(path)
        url_param = _url_from_scope(scope)

        # Состояние для перехвата ответа
        body_chunks: list[bytes] = []
        response_headers: list[tuple[bytes, bytes]] = []
        status_code: int = 200
        started = False

        async def _send(message: Message) -> None:
            nonlocal started, status_code, response_headers

            if message["type"] == "http.response.start":
                status_code = message.get("status", 200)
                response_headers = list(message.get("headers", []))
                started = True
                # Не отправляем сразу — добавим заголовок экономии позже
                return

            if message["type"] == "http.response.body":
                body = message.get("body", b"")
                if body:
                    body_chunks.append(body)

                more_body = message.get("more_body", False)

                if not more_body:
                    # Последний чанк — формируем полный ответ
                    full_body = b"".join(body_chunks)
                    compressed_bytes = len(full_body)

                    # Извлечь original_bytes
                    original_bytes = 0
                    if 200 <= status_code < 300:
                        original_bytes = _extract_original_bytes(
                            response_headers, full_body, endpoint
                        )

                    # Добавить заголовок экономии
                    savings_pct = 0.0
                    if original_bytes > 0:
                        savings_pct = round(
                            (1 - compressed_bytes / original_bytes) * 100, 1
                        )
                    savings_header = (
                        f"original={original_bytes} "
                        f"compressed={compressed_bytes} "
                        f"savings={savings_pct}%"
                    )
                    response_headers.append(
                        (b"X-Saylat-Savings", savings_header.encode("utf-8"))
                    )

                    # Отправляем http.response.start с доп. заголовком
                    await send(
                        {
                            "type": "http.response.start",
                            "status": status_code,
                            "headers": response_headers,
                        }
                    )

                    # Отправляем полное тело одним чанком
                    await send(
                        {
                            "type": "http.response.body",
                            "body": full_body,
                            "more_body": False,
                        }
                    )

                    # Записываем статистику (асинхронно, не блокируя ответ)
                    if original_bytes > 0 and compressed_bytes > 0:
                        try:
                            await record_traffic(
                                endpoint, original_bytes, compressed_bytes, url_param
                            )
                        except Exception:
                            log.debug("traffic recording failed", exc_info=True)
                    return

                # Промежуточный чанк — просто накапливаем
                return

            # Другие типы сообщений — пропускаем как есть
            await send(message)

        await self.app(scope, receive, _send)
