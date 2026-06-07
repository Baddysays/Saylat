"""Надёжное декодирование HTML/RSS с кириллицей (cp1251, koi8-r, …)."""

from __future__ import annotations

import re
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    import httpx

_META_CHARSET = re.compile(
    br'<meta[^>]+charset\s*=\s*["\']?([^"\'\s;>]+)',
    re.IGNORECASE,
)
_META_CT_CHARSET = re.compile(
    br'<meta[^>]+content\s*=\s*["\'][^"\']*charset\s*=\s*([^"\'\s;>]+)',
    re.IGNORECASE,
)

_FALLBACK_ENCODINGS = ("utf-8", "cp1251", "windows-1251", "koi8-r", "iso-8859-5", "latin-1")


def _meta_charset(raw: bytes) -> str | None:
    head = raw[:12_288]
    for pattern in (_META_CHARSET, _META_CT_CHARSET):
        match = pattern.search(head)
        if match:
            try:
                return match.group(1).decode("ascii", errors="ignore").strip().lower()
            except Exception:
                continue
    return None


def decode_response_text(resp: "httpx.Response", *, max_bytes: int | None = None) -> str:
    raw = resp.content if max_bytes is None else resp.content[:max_bytes]
    encoding = (resp.charset_encoding or "").strip().lower()
    if encoding in ("", "iso-8859-1", "latin-1", "ascii", "us-ascii"):
        encoding = _meta_charset(raw) or encoding
    if encoding:
        try:
            return raw.decode(encoding)
        except (UnicodeDecodeError, LookupError):
            pass
    for enc in _FALLBACK_ENCODINGS:
        try:
            return raw.decode(enc)
        except (UnicodeDecodeError, LookupError):
            continue
    return raw.decode("utf-8", errors="replace")
