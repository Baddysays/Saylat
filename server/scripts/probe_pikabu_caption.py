import asyncio
import json
import re
from pathlib import Path

import httpx
from bs4 import BeautifulSoup

from app.http_ua import MOBILE_UA

URL = "https://pikabu.ru/story/kto_rano_vstayot_14002427"
OUT = Path("data/pikabu_caption_probe.txt")


async def main() -> None:
    async with httpx.AsyncClient(headers={"User-Agent": MOBILE_UA}, follow_redirects=True) as client:
        html = (await client.get(URL, timeout=25)).text
    soup = BeautifulSoup(html, "lxml")
    lines: list[str] = []
    for sel in (
        ".story__title",
        ".story__header",
        ".story-image__title",
        ".story-block__title",
        ".story__caption",
        ".story__tags",
        "figcaption",
        ".story__content img[alt]",
        ".story__footer",
    ):
        for el in soup.select(sel):
            lines.append(f"{sel}: {el.get_text(' ', strip=True)[:200]} alt={el.get('alt','')[:120]}")
    for sc in soup.find_all("script", type="application/ld+json"):
        try:
            data = json.loads(sc.string or "")
        except json.JSONDecodeError:
            continue
        items = data if isinstance(data, list) else [data]
        for item in items:
            if item.get("@type") == "Article":
                lines.append("articleBody: " + str(item.get("articleBody", ""))[:300])
                lines.append("description: " + str(item.get("description", ""))[:300])
                comments = item.get("comment") or []
                if isinstance(comments, dict):
                    comments = [comments]
                for c in comments[:3]:
                    if isinstance(c, dict) and c.get("text"):
                        lines.append("comment: " + c["text"][:200])
    # desktop UA
    async with httpx.AsyncClient(
        headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0"},
        follow_redirects=True,
    ) as client:
        html2 = (await client.get(URL, timeout=25)).text
    soup2 = BeautifulSoup(html2, "lxml")
    blocks2 = soup2.select(".story-block")
    lines.append(f"desktop blocks={len(blocks2)}")
    for b in blocks2[:5]:
        lines.append("  " + " ".join(b.get("class") or []) + " | " + b.get_text(" ", strip=True)[:200])
    OUT.write_text("\n".join(lines), encoding="utf-8")


if __name__ == "__main__":
    asyncio.run(main())
