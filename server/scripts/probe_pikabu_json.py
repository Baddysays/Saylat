import asyncio
import json
import re
from pathlib import Path

import httpx
from bs4 import BeautifulSoup

from app.http_ua import MOBILE_UA

URL = "https://pikabu.ru/story/kto_rano_vstayot_14002427"
OUT = Path("data/pikabu_json_probe.txt")


async def main() -> None:
    async with httpx.AsyncClient(headers={"User-Agent": MOBILE_UA}, follow_redirects=True) as client:
        html = (await client.get(URL, timeout=25)).text
    soup = BeautifulSoup(html, "lxml")
    lines: list[str] = []
    for script in soup.find_all("script"):
        body = (script.string or "").strip()
        if len(body) < 100:
            continue
        if "storyId" in body or "story_id" in body or "Story" in body:
            lines.append(f"--- script len={len(body)} ---")
            lines.append(body[:3000])
        # JSON-LD
    for sc in soup.find_all("script", type="application/ld+json"):
        lines.append("ld+json:")
        lines.append((sc.string or "")[:2000])
    # api hints
    for m in re.finditer(r'https://[^"\']+api[^"\']+', html):
        lines.append("api_url: " + m.group(0)[:120])
    OUT.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    asyncio.run(main())
