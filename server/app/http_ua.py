"""User-Agent и нормализация URL для HTTP-запросов."""

from __future__ import annotations

from urllib.parse import urlparse, urlunparse

# Современный Chrome — VK и многие сайты режут старые / нестандартные UA.
CHROME_DESKTOP_UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
)

CHROME_MOBILE_UA = (
    "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/131.0.6778.135 Mobile Safari/537.36"
)

# Обратная совместимость
MOBILE_UA = CHROME_MOBILE_UA

_VK_MOBILE_HOSTS = frozenset({"m.vk.com", "m.vk.ru"})
_VK_HOSTS = frozenset(
    {"vk.com", "www.vk.com", "m.vk.com", "vk.ru", "www.vk.ru", "m.vk.ru"}
)
_PIKABU_HOSTS = frozenset({"pikabu.ru", "www.pikabu.ru"})


def _host(url: str) -> str:
    return (urlparse(url).hostname or "").lower().removeprefix("www.")


def ua_for_url(url: str) -> str:
    """ВК: только desktop Chrome (m.vk.com иначе отдаёт «браузер устарел»)."""
    host = _host(url)
    if host in _VK_HOSTS or host.endswith(".vk.com") or host.endswith(".vk.ru"):
        return CHROME_DESKTOP_UA
    if host in _PIKABU_HOSTS:
        return CHROME_MOBILE_UA
    return CHROME_DESKTOP_UA


def is_vk_url(url: str) -> bool:
    host = _host(url)
    return host in _VK_HOSTS or host.endswith(".vk.com") or host.endswith(".vk.ru")


def normalize_fetch_url(url: str) -> str:
    """m.vk.com → vk.com для обхода BadBrowser на мобильной вёрстке."""
    parsed = urlparse(url.strip())
    host = (parsed.hostname or "").lower()
    if host in _VK_MOBILE_HOSTS:
        new_host = "vk.com" if "vk.com" in host else "vk.ru"
        parsed = parsed._replace(netloc=new_host)
        return urlunparse(parsed)
    return url.strip()
