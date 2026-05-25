import asyncio
import time

import httpx

from .config import settings

MYMEMORY_URL = "https://api.mymemory.translated.net/get"
_MAX_CHUNK = 480
_MAX_TEXTS = 36


def _langpair(source: str, target: str) -> str:
    src = (source or "auto").strip().lower()
    tgt = (target or "ru").strip().lower()
    if src in {"", "auto"}:
        return f"en|{tgt}"
    return f"{src}|{tgt}"


async def _translate_one(
    client: httpx.AsyncClient,
    text: str,
    langpair: str,
) -> str:
    chunk = text.strip()
    if not chunk:
        return text
    if len(chunk) > _MAX_CHUNK:
        chunk = chunk[:_MAX_CHUNK]

    resp = await client.get(
        MYMEMORY_URL,
        params={"q": chunk, "langpair": langpair},
        timeout=settings.translate_timeout_sec,
    )
    resp.raise_for_status()
    data = resp.json()
    translated = (
        (data.get("responseData") or {}).get("translatedText") or ""
    ).strip()
    if not translated or translated.upper() == chunk.upper():
        return text
    return translated


async def translate_texts(
    texts: list[str],
    *,
    source: str = "auto",
    target: str = "ru",
) -> tuple[list[str], int]:
    if not texts:
        return [], 0
    if len(texts) > _MAX_TEXTS:
        raise ValueError(f"At most {_MAX_TEXTS} text segments per request")

    langpair = _langpair(source, target)
    started = time.perf_counter()

    async with httpx.AsyncClient(
        headers={"User-Agent": settings.user_agent},
    ) as client:
        tasks = [_translate_one(client, t, langpair) for t in texts]
        out = await asyncio.gather(*tasks)

    ms = int((time.perf_counter() - started) * 1000)
    return list(out), ms
