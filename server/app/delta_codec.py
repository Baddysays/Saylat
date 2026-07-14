"""Дельта/diff-обновления для экономии трафика на 2G/EDGE.

Когда клиент повторно запрашивает ту же статью с заголовком If-None-Match,
сервер отправляет только бинарную дельту вместо полного содержимого.

Алгоритм (аналог rsync):
  1. Сервер хранит последнюю версию статьи в DeltaCache (ключ — URL)
  2. Клиент шлёт If-None-Match: <etag> → совпадает → 304 Not Modified (0 байт!)
  3. ETag не совпадает, но есть старая версия → вычисляем бинарную дельту → шлём diff
  4. Нет старой версии → сохраняем и отдаём полный контент
  5. Если дельта больше полного контента — fallback на полный контент

Формат дельты (самодостаточный, содержит base ETag):
  Заголовок (35 байт):
    Magic:     b"DL"           (2 байта)
    Version:   0x01            (1 байт)
    Base ETag: raw MD5 старой  (16 байт)
    New ETag:  raw MD5 новой   (16 байт)
  Поток команд:
    COPY:   0x01 + varint(offset) + varint(length) — копировать из старой версии
    INSERT: 0x02 + varint(length) + data[length]   — вставить новые байты
"""

from __future__ import annotations

import asyncio
import hashlib
import logging
import time
from collections import OrderedDict
from difflib import SequenceMatcher
from typing import Any

from fastapi import Request, Response

from .models import SaylatArticle

log = logging.getLogger(__name__)

# ─── Константы формата ────────────────────────────────────────────────

DELTA_MAGIC = b"DL"
DELTA_VERSION = 0x01
CMD_COPY = 0x01
CMD_INSERT = 0x02
DELTA_HEADER_SIZE = 2 + 1 + 16 + 16  # 35 байт

# Порог: дельта должна быть хотя бы на 5% меньше полного контента
DELTA_MIN_SAVINGS_RATIO = 0.05

# Максимальный размер данных для difflib (больше — разбиваем на блоки)
_DIFF_MAX_SIZE = 512 * 1024  # 512 KB

# Размер блока для блочного алгоритма (если данные больше _DIFF_MAX_SIZE)
_BLOCK_SIZE = 256

# ─── Varint кодирование (protobuf-style, LE, 7 бит на байт) ──────────


def _encode_varint(value: int) -> bytes:
    """Кодирование целого числа в varint."""
    if value < 0:
        raise ValueError("varint must be non-negative")
    result = bytearray()
    while value > 0x7F:
        result.append((value & 0x7F) | 0x80)
        value >>= 7
    result.append(value & 0x7F)
    return bytes(result)


def _decode_varint(data: bytes, offset: int) -> tuple[int, int]:
    """Декодирование varint из data[offset:]. Возвращает (value, new_offset)."""
    value = 0
    shift = 0
    while offset < len(data):
        byte = data[offset]
        value |= (byte & 0x7F) << shift
        offset += 1
        if not (byte & 0x80):
            return value, offset
        shift += 7
        if shift > 63:
            raise ValueError("varint too large")
    raise ValueError("truncated varint")


# ─── ETag ─────────────────────────────────────────────────────────────


def compute_etag(data: bytes) -> str:
    """Генерация ETag на основе MD5 хеша содержимого.

    Быстрый и детерминированный — подходит для проверки изменений
    при повторных запросах от 2G-клиентов.
    """
    return hashlib.md5(data).hexdigest()


def _etag_to_raw(etag_hex: str) -> bytes:
    """Преобразование hex ETag (32 символа) в 16 байт raw MD5."""
    return bytes.fromhex(etag_hex)


def _raw_to_etag(raw: bytes) -> str:
    """Преобразование 16 байт raw MD5 в hex ETag (32 символа)."""
    return raw.hex()


# ─── Дельта: вычисление (difflib) ────────────────────────────────────


def _compute_delta_difflib(old_data: bytes, new_data: bytes) -> bytearray:
    """Вычисление дельты через difflib.SequenceMatcher (для данных < _DIFF_MAX_SIZE).

    Оптимально для статей с небольшими изменениями — типичный сценарий 2G:
    обновился счётчик комментариев, добавился абзац, изменилась дата.
    """
    buf = bytearray()

    sm = SequenceMatcher(None, old_data, new_data, autojunk=False)
    for tag, i1, i2, j1, j2 in sm.get_opcodes():
        if tag == "equal":
            # COPY — копируем из старой версии
            length = i2 - i1
            if length > 0:
                buf.append(CMD_COPY)
                buf.extend(_encode_varint(i1))
                buf.extend(_encode_varint(length))
        elif tag == "replace":
            # Замена: старые данные пропускаем, новые — INSERT
            new_chunk = new_data[j1:j2]
            if new_chunk:
                buf.append(CMD_INSERT)
                buf.extend(_encode_varint(len(new_chunk)))
                buf.extend(new_chunk)
        elif tag == "insert":
            # INSERT — новые данные, которых не было в старой версии
            new_chunk = new_data[j1:j2]
            if new_chunk:
                buf.append(CMD_INSERT)
                buf.extend(_encode_varint(len(new_chunk)))
                buf.extend(new_chunk)
        # tag == "delete" — пропускаем, ничего не добавляем

    return buf


# ─── Дельта: вычисление (блочный алгоритм) ───────────────────────────


def _compute_delta_block(old_data: bytes, new_data: bytes) -> bytearray:
    """Вычисление дельты блочным алгоритмом (для больших данных >= _DIFF_MAX_SIZE).

    Аналог rsync: разбиваем старые данные на блоки, строим хеш-таблицу,
    сканируем новые данные и ищем совпадения блоков.
    Не совпавшие байты — INSERT, совпавшие — COPY.
    """
    block_size = _BLOCK_SIZE

    # Хеш-таблица: prefix (4 байта) → список (offset, block_end)
    # Используем первые 4 байта MD5 блока как ключ
    block_index: dict[bytes, list[tuple[int, int]]] = {}
    for i in range(0, len(old_data), block_size):
        chunk = old_data[i : i + block_size]
        if not chunk:
            continue
        prefix = hashlib.md5(chunk).digest()[:4]
        if prefix not in block_index:
            block_index[prefix] = []
        block_index[prefix].append((i, i + len(chunk)))

    buf = bytearray()
    pos = 0
    insert_buf = bytearray()  # накапливаем несопоставленные байты

    while pos < len(new_data):
        chunk = new_data[pos : pos + block_size]
        if not chunk:
            break

        # Ищем совпадение в старой версии
        prefix = hashlib.md5(chunk).digest()[:4]
        candidates = block_index.get(prefix, [])

        matched = False
        for old_start, old_end in candidates:
            if old_data[old_start:old_end] == chunk:
                # Нашли совпадение — сначала flush INSERT
                if insert_buf:
                    buf.append(CMD_INSERT)
                    buf.extend(_encode_varint(len(insert_buf)))
                    buf.extend(insert_buf)
                    insert_buf.clear()
                # Затем COPY
                buf.append(CMD_COPY)
                buf.extend(_encode_varint(old_start))
                buf.extend(_encode_varint(old_end - old_start))
                pos += len(chunk)
                matched = True
                break

        if not matched:
            # Нет совпадения — накапливаем в INSERT
            insert_buf.append(new_data[pos])
            pos += 1

    # Flush оставшийся INSERT-буфер
    if insert_buf:
        buf.append(CMD_INSERT)
        buf.extend(_encode_varint(len(insert_buf)))
        buf.extend(insert_buf)

    return buf


def compute_delta(old_data: bytes, new_data: bytes) -> bytes:
    """Вычисление бинарной дельты между двумя версиями.

    Выбирает алгоритм в зависимости от размера данных:
    - Меньше _DIFF_MAX_SIZE → difflib (точнее, медленнее)
    - Больше → блочный алгоритм (быстрее, чуть менее эффективен)

    Формат дельты самодостаточный: содержит base ETag, чтобы клиент
    знал, к какой версии она применима.
    """
    new_etag = compute_etag(new_data)

    if not old_data:
        # Нет базовой версии — INSERT всего нового контента
        old_etag_raw = b"\x00" * 16
        commands = bytearray()
        if new_data:
            commands.append(CMD_INSERT)
            commands.extend(_encode_varint(len(new_data)))
            commands.extend(new_data)
    else:
        old_etag_raw = _etag_to_raw(compute_etag(old_data))
        if old_data == new_data:
            # Данные идентичны — дельта = только COPY всей старой версии
            commands = bytearray()
            commands.append(CMD_COPY)
            commands.extend(_encode_varint(0))
            commands.extend(_encode_varint(len(old_data)))
        elif len(old_data) < _DIFF_MAX_SIZE and len(new_data) < _DIFF_MAX_SIZE:
            commands = _compute_delta_difflib(old_data, new_data)
        else:
            commands = _compute_delta_block(old_data, new_data)

    # Собираем полный пакет: заголовок + команды
    buf = bytearray()
    buf.extend(DELTA_MAGIC)               # 2 байта
    buf.append(DELTA_VERSION)              # 1 байт
    buf.extend(old_etag_raw)               # 16 байт
    buf.extend(_etag_to_raw(new_etag))     # 16 байт
    buf.extend(commands)

    return bytes(buf)


# ─── Дельта: применение ──────────────────────────────────────────────


def apply_delta(old_data: bytes, delta: bytes) -> bytes:
    """Применение дельты к старой версии для восстановления новой.

    Формат дельты самодостаточный: содержит base ETag для проверки.
    Если ETag старой версии не совпадает — выбрасывает ValueError.
    В конце проверяет, что восстановленный контент имеет ожидаемый ETag.
    """
    if len(delta) < DELTA_HEADER_SIZE:
        raise ValueError(
            f"Delta too short: {len(delta)} bytes, need at least {DELTA_HEADER_SIZE}"
        )

    # Парсинг заголовка
    if delta[:2] != DELTA_MAGIC:
        raise ValueError(f"Invalid delta magic: {delta[:2]!r}, expected b'DL'")

    version = delta[2]
    if version != DELTA_VERSION:
        raise ValueError(f"Unsupported delta version: {version}, expected {DELTA_VERSION}")

    base_etag_raw = delta[3:19]     # 16 байт raw MD5
    new_etag_raw = delta[19:35]     # 16 байт raw MD5

    # Проверка, что дельта применима к нашей базе
    if old_data:
        old_etag = compute_etag(old_data)
        if _etag_to_raw(old_etag) != base_etag_raw:
            raise ValueError(
                f"Base ETag mismatch: delta expects {_raw_to_etag(base_etag_raw)}, "
                f"but current data has {old_etag}"
            )

    # Выполнение команд
    result = bytearray()
    offset = DELTA_HEADER_SIZE

    while offset < len(delta):
        cmd = delta[offset]
        offset += 1

        if cmd == CMD_COPY:
            copy_offset, offset = _decode_varint(delta, offset)
            copy_length, offset = _decode_varint(delta, offset)
            end = copy_offset + copy_length
            if end > len(old_data):
                raise ValueError(
                    f"COPY out of bounds: offset={copy_offset}, length={copy_length}, "
                    f"old_data_len={len(old_data)}"
                )
            result.extend(old_data[copy_offset:end])
        elif cmd == CMD_INSERT:
            insert_length, offset = _decode_varint(delta, offset)
            if offset + insert_length > len(delta):
                raise ValueError(
                    f"INSERT out of bounds: length={insert_length}, "
                    f"remaining={len(delta) - offset}"
                )
            result.extend(delta[offset : offset + insert_length])
            offset += insert_length
        else:
            raise ValueError(f"Unknown delta command: 0x{cmd:02x}")

    # Проверка корректности результата
    reconstructed = bytes(result)
    reconstructed_etag = compute_etag(reconstructed)
    expected_etag = _raw_to_etag(new_etag_raw)
    if reconstructed_etag != expected_etag:
        raise ValueError(
            f"Reconstruction ETag mismatch: got {reconstructed_etag}, "
            f"expected {expected_etag}. Delta may be corrupted."
        )

    return reconstructed


# ─── Кэш дельт ───────────────────────────────────────────────────────


class DeltaCache:
    """In-memory LRU кэш последних версий статей для вычисления дельт.

    Ключ: строка (URL + параметры сжатия)
    Значение: (etag: str, data: bytes, timestamp: float)

    Потокобезопасный через asyncio.Lock.
    Авто-вытеснение: LRU + записи старше TTL.
    """

    def __init__(
        self,
        max_entries: int = 1000,
        ttl_seconds: float = 3600.0,
    ) -> None:
        self._max_entries = max_entries
        self._ttl = ttl_seconds
        self._store: OrderedDict[str, tuple[str, bytes, float]] = OrderedDict()
        self._lock = asyncio.Lock()
        self._hits = 0
        self._misses = 0

    async def get(self, key: str) -> tuple[str, bytes] | None:
        """Получить (etag, data) из кэша. None если нет или устарело."""
        async with self._lock:
            entry = self._store.get(key)
            if entry is None:
                self._misses += 1
                return None
            etag, data, ts = entry
            if time.monotonic() - ts > self._ttl:
                # Запись устарела
                del self._store[key]
                self._misses += 1
                return None
            # LRU: переместить в конец (недавно использованный)
            self._store.move_to_end(key)
            self._hits += 1
            return etag, data

    async def put(self, key: str, etag: str, data: bytes) -> None:
        """Сохранить версию статьи в кэш."""
        async with self._lock:
            self._store[key] = (etag, data, time.monotonic())
            self._store.move_to_end(key)
            # LRU-вытеснение при переполнении
            while len(self._store) > self._max_entries:
                self._store.popitem(last=False)

    async def remove(self, key: str) -> None:
        """Удалить запись из кэша."""
        async with self._lock:
            self._store.pop(key, None)

    async def evict_expired(self) -> int:
        """Удалить все устаревшие записи. Возвращает количество удалённых."""
        async with self._lock:
            now = time.monotonic()
            expired = [
                k
                for k, (_, _, ts) in self._store.items()
                if now - ts > self._ttl
            ]
            for k in expired:
                del self._store[k]
            if expired:
                log.debug("delta_cache: evicted %d expired entries", len(expired))
            return len(expired)

    def stats(self) -> dict[str, int | float]:
        """Статистика кэша дельт."""
        total = self._hits + self._misses
        return {
            "entries": len(self._store),
            "max_entries": self._max_entries,
            "ttl_seconds": int(self._ttl),
            "hits": self._hits,
            "misses": self._misses,
            "hit_rate": round(self._hits / max(1, total) * 100, 1),
        }


# Глобальный экземпляр кэша дельт
delta_cache = DeltaCache()

# Периодическая очистка устаревших записей (раз в 10 минут)
_EVICIT_INTERVAL = 600.0
_eviction_task: asyncio.Task | None = None


async def _eviction_loop() -> None:
    """Фоновая задача для периодической очистки DeltaCache."""
    while True:
        await asyncio.sleep(_EVICIT_INTERVAL)
        try:
            await delta_cache.evict_expired()
        except Exception:
            log.warning("delta_cache eviction error", exc_info=True)


def start_delta_eviction() -> None:
    """Запуск фоновой очистки кэша дельт (вызывать при старте приложения)."""
    global _eviction_task
    if _eviction_task is None or _eviction_task.done():
        _eviction_task = asyncio.create_task(_eviction_loop())


# ─── HTTP-ответ с дельтой ────────────────────────────────────────────


def _full_response(serialized: bytes, etag: str, media_type: str = "application/json") -> Response:
    """Формирование ответа с полным контентом и ETag."""
    return Response(
        content=serialized,
        media_type=media_type,
        headers={
            "ETag": f'"{etag}"',
            "X-Saylat-Delta": "false",
            "X-Saylat-Uncompressed-Bytes": str(len(serialized)),
            "Cache-Control": "no-cache",
        },
    )


async def maybe_delta_response(
    article: SaylatArticle,
    request: Request,
    serialized: bytes,
    cache_key: str,
    media_type: str = "application/json",
) -> Response:
    """Проверка If-None-Match и формирование ответа с дельтой.

    Основная функция интеграции — вызывается из extract-эндпоинтов.

    Возвращает:
      - Response(304) — контент не изменился (0 байт тела!)
      - Response(delta_bytes) — дельта-обновление (только изменения)
      - Response(full_bytes) — полный контент (первый запрос или дельта неэффективна)

    Заголовки ответа:
      - ETag — хеш текущего контента
      - X-Saylat-Delta: true/false — дельта или полный контент
      - X-Saylat-Delta-Base-Etag — ETag базовой версии (для дельты)
      - X-Saylat-Payload-Codec: delta — маркер дельта-формата
    """
    # Вычисляем ETag для нового контента
    new_etag = compute_etag(serialized)

    # Проверяем If-None-Match от клиента
    if_none_match = _parse_if_none_match(request.headers.get("if-none-match", ""))

    # ── 304 Not Modified — контент не изменился ──
    if if_none_match and if_none_match == new_etag:
        log.debug("delta: 304 Not Modified for %s", cache_key)
        return Response(
            status_code=304,
            headers={
                "ETag": f'"{new_etag}"',
                "X-Saylat-Delta": "false",
                "Cache-Control": "no-cache",
            },
        )

    # Получаем старую версию из кэша дельт
    cached = await delta_cache.get(cache_key)

    if cached is not None:
        old_etag, old_data = cached

        # Клиент прислал ETag, совпадающий с нашей кэшированной версией
        # → можно отправить дельту (контент изменился с версии клиента)
        if if_none_match and if_none_match == old_etag and old_data != serialized:
            # Вычисляем дельту в worker-потоке (не блокирует asyncio)
            delta_bytes = await asyncio.to_thread(compute_delta, old_data, serialized)

            # Fallback: если дельта не даёт экономии — отправляем полный контент
            if len(delta_bytes) >= len(serialized) * (1.0 - DELTA_MIN_SAVINGS_RATIO):
                log.debug(
                    "delta: no savings for %s (delta=%d, full=%d, ratio=%.1f%%)",
                    cache_key,
                    len(delta_bytes),
                    len(serialized),
                    len(delta_bytes) / max(1, len(serialized)) * 100,
                )
                await delta_cache.put(cache_key, new_etag, serialized)
                return _full_response(serialized, new_etag, media_type)

            savings_pct = (1.0 - len(delta_bytes) / max(1, len(serialized))) * 100
            log.info(
                "delta: sending diff for %s (delta=%d, full=%d, savings=%.1f%%)",
                cache_key,
                len(delta_bytes),
                len(serialized),
                savings_pct,
            )
            # Обновляем кэш новой версией
            await delta_cache.put(cache_key, new_etag, serialized)
            return Response(
                content=delta_bytes,
                media_type="application/vnd.saylat.delta",
                headers={
                    "ETag": f'"{new_etag}"',
                    "X-Saylat-Delta": "true",
                    "X-Saylat-Delta-Base-Etag": f'"{old_etag}"',
                    "X-Saylat-Payload-Codec": "delta",
                    "X-Saylat-Delta-Bytes": str(len(delta_bytes)),
                    "X-Saylat-Uncompressed-Bytes": str(len(serialized)),
                    "Cache-Control": "no-cache",
                },
            )

        # ETag клиента не совпадает с нашей базой — отправляем полный контент
        # (клиент не имеет базовой версии, к которой применима дельта)

    # Сохраняем текущую версию в кэш для будущих запросов
    await delta_cache.put(cache_key, new_etag, serialized)

    return _full_response(serialized, new_etag, media_type)


def _parse_if_none_match(header: str) -> str | None:
    """Разбор заголовка If-None-Match — убирает кавычки и W/ prefix."""
    value = header.strip()
    if not value:
        return None
    # Убираем W/ prefix (weak ETag)
    if value.startswith("W/"):
        value = value[2:]
    # Убираем кавычки
    value = value.strip('"')
    if not value or value == "*":
        return None
    return value


# ─── Утилита: проверить, поддерживает ли клиент дельты ───────────────


def client_supports_delta(request: Request) -> bool:
    """Проверка, что клиент поддерживает дельта-обновления."""
    codec_header = request.headers.get("x-saylat-payload-codec", "").lower()
    return "delta" in codec_header
