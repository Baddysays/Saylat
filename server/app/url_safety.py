"""Проверка URL: только публичный http/https, без SSRF на localhost и LAN."""

from __future__ import annotations

import ipaddress
import socket
from urllib.parse import urlparse

_BLOCKED_HOST_SUFFIXES = (".local", ".localhost", ".internal")
_BLOCKED_NAMES = frozenset(
    {
        "localhost",
        "127.0.0.1",
        "0.0.0.0",
        "::1",
        "metadata.google.internal",
    }
)


def validate_public_http_url(url: str) -> str:
    raw = (url or "").strip()
    if not raw:
        raise ValueError("Укажите адрес страницы (http или https)")
    parsed = urlparse(raw)
    if parsed.scheme not in ("http", "https"):
        raise ValueError("Поддерживаются только ссылки http:// и https://")
    if not parsed.hostname:
        raise ValueError("Некорректный адрес")
    host = parsed.hostname.lower().strip(".")
    if host in _BLOCKED_NAMES:
        raise ValueError("Этот адрес недоступен через прокси")
    for suffix in _BLOCKED_HOST_SUFFIXES:
        if host.endswith(suffix):
            raise ValueError("Этот адрес недоступен через прокси")
    _reject_ip_literal(host)
    return raw


def _reject_ip_literal(host: str) -> None:
    try:
        addr = ipaddress.ip_address(host)
    except ValueError:
        return
    if (
        addr.is_private
        or addr.is_loopback
        or addr.is_link_local
        or addr.is_multicast
        or addr.is_reserved
    ):
        raise ValueError("Локальные и служебные адреса недоступны")


def hostname_resolves_to_private(host: str) -> bool:
    """Доп. проверка после DNS (опционально, не блокирует при ошибке DNS)."""
    try:
        infos = socket.getaddrinfo(host, None, type=socket.SOCK_STREAM)
    except OSError:
        return False
    for info in infos:
        sockaddr = info[4]
        if not sockaddr:
            continue
        ip_str = sockaddr[0]
        try:
            addr = ipaddress.ip_address(ip_str)
        except ValueError:
            continue
        if addr.is_private or addr.is_loopback or addr.is_link_local:
            return True
    return False
