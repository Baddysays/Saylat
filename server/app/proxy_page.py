"""Прокси-страница для WebView: HTML с сервера, ссылки и картинки через Saylat."""

from __future__ import annotations

import re
from urllib.parse import quote, urljoin, urlparse

import httpx
from bs4 import BeautifulSoup, Tag

from .config import settings
from .http_ua import normalize_fetch_url, ua_for_url

_PROXY_CSS = """
html, body { margin: 0; padding: 0; background: #f5f5f5; }
body {
  font-family: system-ui, sans-serif;
  font-size: 16px;
  line-height: 1.45;
  color: #1a1a1a;
  padding: 10px 12px 24px;
  max-width: 100%;
  overflow-x: hidden;
}
img { max-width: 100% !important; height: auto !important; }
a { color: #0d6e6e; word-break: break-word; }
table { max-width: 100%; font-size: 14px; }
.saylat-banner {
  background: #e8f4f4;
  border: 1px solid #b8d8d8;
  border-radius: 8px;
  padding: 8px 10px;
  margin-bottom: 12px;
  font-size: 13px;
}
"""

_BANNER = (
    '<div class="saylat-banner">'
    "Saylat · страница через прокси VPS. Картинки и ссылки идут через сервер."
    "</div>"
)


def _is_http_url(url: str) -> bool:
    try:
        p = urlparse(url.strip())
        return p.scheme in settings.allowed_schemes and bool(p.hostname)
    except Exception:
        return False


def _same_site(a: str, b: str) -> bool:
    try:
        return urlparse(a).hostname == urlparse(b).hostname
    except Exception:
        return False


def proxy_page_url(request_base: str, target: str) -> str:
    base = request_base.rstrip("/")
    return f"{base}/api/proxy/page?url={quote(target, safe='')}"


def proxy_asset_url(request_base: str, asset_url: str) -> str:
    base = request_base.rstrip("/")
    return f"{base}/api/proxy/asset?url={quote(asset_url, safe='')}"


def _rewrite_attr(
    tag: Tag,
    attr: str,
    page_url: str,
    request_base: str,
    *,
    asset_only: bool = False,
) -> None:
    raw = (tag.get(attr) or "").strip()
    if not raw or raw.startswith(("#", "javascript:", "data:", "mailto:", "tel:")):
        return
    resolved = urljoin(page_url, raw)
    if not _is_http_url(resolved):
        return
    if asset_only and tag.name != "img" and attr != "src":
        return
    if tag.name == "a" and attr == "href":
        if _same_site(resolved, page_url) or True:
            tag[attr] = proxy_page_url(request_base, resolved)
        return
    if tag.name == "img" and attr in {"src", "data-src", "data-lazy-src"}:
        tag[attr] = proxy_asset_url(request_base, resolved)


async def fetch_proxy_html(target_url: str, *, request_base: str) -> str:
    if not _is_http_url(target_url):
        raise ValueError("Only http/https URLs supported")

    fetch_url = normalize_fetch_url(target_url)
    ua = ua_for_url(fetch_url)
    async with httpx.AsyncClient(
        follow_redirects=True,
        headers={"User-Agent": ua},
    ) as client:
        resp = await client.get(fetch_url, timeout=settings.request_timeout_sec)
        resp.raise_for_status()
        html = resp.text[: settings.max_html_bytes]

    return build_proxy_document(html, fetch_url, request_base)


def build_proxy_document(html: str, page_url: str, request_base: str) -> str:
    soup = BeautifulSoup(html, "lxml")
    for tag in soup(["script", "iframe", "noscript", "style", "link", "meta"]):
        tag.decompose()

    body = soup.body
    if body is None:
        body = soup
        inner = str(soup)
    else:
        inner = body.decode_contents() if hasattr(body, "decode_contents") else "".join(
            str(c) for c in body.children
        )

    # Re-parse fragment for rewrites
    frag = BeautifulSoup(f"<body>{inner}</body>", "lxml")
    body2 = frag.body
    if body2:
        for tag in body2.find_all(True):
            if tag.name == "a":
                _rewrite_attr(tag, "href", page_url, request_base)
            elif tag.name == "img":
                for attr in ("src", "data-src", "data-lazy-src", "data-original"):
                    if tag.get(attr):
                        _rewrite_attr(tag, attr, page_url, request_base)
                        break
            elif tag.name in {"source", "video"} and tag.get("src"):
                _rewrite_attr(tag, "src", page_url, request_base)

        inner = body2.decode_contents()

    title_raw = soup.title.get_text() if soup.title else ""
    title = _clean_title(title_raw) or urlparse(page_url).hostname or "Saylat"
    banner = _BANNER

    return f"""<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1"/>
<title>{_escape(title)}</title>
<style>{_PROXY_CSS}</style>
</head>
<body>
{banner}
{inner}
</body>
</html>"""


def _clean_title(raw: str) -> str:
    return re.sub(r"\s+", " ", (raw or "")).strip()[:200]


def _escape(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )
