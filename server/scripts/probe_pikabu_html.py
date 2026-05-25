import asyncio
import re
from pathlib import Path

import httpx
from bs4 import BeautifulSoup

from app.http_ua import MOBILE_UA

URLS = [
    "https://pikabu.ru/story/kto_rano_vstayot_14002427",
    "https://pikabu.ru/story/elena_troyanskaya_zdorovogo_cheloveka_14002976",
    "https://pikabu.ru/story/ded_i_vnuchka_14003602",
]
OUT = Path("data/pikabu_html_probe.txt")


async def probe(url: str) -> list[str]:
    async with httpx.AsyncClient(headers={"User-Agent": MOBILE_UA}, follow_redirects=True) as client:
        resp = await client.get(url, timeout=25)
    soup = BeautifulSoup(resp.text, "lxml")
    lines = [url, f"status={resp.status_code}"]
    ogd = soup.find("meta", property="og:description")
    if ogd and ogd.get("content"):
        lines.append(f"og:description: {ogd['content'][:400]}")
    blocks = soup.select(".story-block")
    lines.append(f"story-blocks={len(blocks)}")
    for block in blocks[:15]:
        cls = " ".join(block.get("class") or [])
        text = block.get_text(" ", strip=True)[:200]
        lines.append(f"  [{cls}] {text}")
    inner = soup.select_one(".story__content-inner") or soup.select_one(".story__content")
    if inner:
        raw = inner.get_text(" ", strip=True)[:500]
        lines.append(f"inner_text: {raw}")
    for script in soup.find_all("script"):
        body = script.string or ""
        if len(body) < 200 or "story" not in body.lower():
            continue
        if "block" in body and ("text" in body or "content" in body):
            lines.append(f"script_chunk len={len(body)}")
            for pat in (r'"content"\s*:\s*"([^"]{30,})"', r'"text"\s*:\s*"([^"]{30,})"'):
                m = re.search(pat, body)
                if m:
                    lines.append(f"  json_text: {m.group(1)[:200]}")
                    break
    return lines


async def main() -> None:
    chunks: list[str] = []
    for url in URLS:
        chunks.extend(await probe(url))
        chunks.append("")
    OUT.write_text("\n".join(chunks), encoding="utf-8")


if __name__ == "__main__":
    asyncio.run(main())
