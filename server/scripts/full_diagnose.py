"""Полная диагностика Saylat: API, Пикабу, версии."""
from __future__ import annotations

import asyncio
import json
import sys
from pathlib import Path

import httpx

from app.extract import extract_article
from app.site_feeds import try_open_site

VPS = "http://157.22.202.235:8787"
OUT = Path(__file__).resolve().parent.parent / "data" / "diagnose_report.txt"


async def local_checks() -> list[str]:
    lines = ["=== LOCAL ==="]
    opened = await try_open_site("https://pikabu.ru/", images_mode="off")
    lines.append(f"pikabu feed items: {len(opened.feed.items)}")
    bad = [i.href for i in opened.feed.items if i.href and "/video/" in i.href]
    lines.append(f"bad video links: {len(bad)}")

    for url, mode in (
        ("https://pikabu.ru/story/kto_rano_vstayot_14002427", "off"),
        ("https://pikabu.ru/story/ded_i_vnuchka_14003602", "off"),
        ("https://pikabu.ru/story/kto_rano_vstayot_14002427", "tiny"),
    ):
        a = await extract_article(url, images_mode=mode)
        paras = [b for b in a.blocks if b.type == "paragraph"]
        imgs = [b for b in a.blocks if b.type == "image"]
        lines.append(f"{url[-30:]} mode={mode} paras={len(paras)} imgs={len(imgs)} inl={a.stats.images_inlined}")
        if paras:
            lines.append(f"  text: {paras[0].text[:100]}")
    return lines


async def remote_checks() -> list[str]:
    lines = ["", "=== VPS ==="]
    async with httpx.AsyncClient(timeout=30.0) as client:
        h = await client.get(f"{VPS}/health")
        lines.append(f"health {h.status_code}: {h.text}")
        for path in (
            "/api/extract?url=https://pikabu.ru/story/kto_rano_vstayot_14002427&images=off",
            "/api/extract?url=https://pikabu.ru/story/ded_i_vnuchka_14003602&images=off",
        ):
            r = await client.get(f"{VPS}{path}")
            lines.append(f"GET {path[-40:]} -> {r.status_code}")
            if r.status_code == 200:
                data = r.json()
                paras = [b for b in data.get("blocks", []) if b.get("type") == "paragraph"]
                lines.append(f"  blocks={len(data.get('blocks',[]))} paras={len(paras)}")
                if paras:
                    lines.append(f"  p0: {paras[0].get('text','')[:100]}")
            else:
                lines.append(f"  err: {r.text[:200]}")
        r = await client.post(
            f"{VPS}/api/open",
            json={"target": "url", "url": "https://pikabu.ru/", "images": "off"},
        )
        lines.append(f"POST open pikabu -> {r.status_code}")
        if r.status_code == 200:
            feed = r.json().get("feed", {})
            lines.append(f"  feed items: {len(feed.get('items', []))}")
    return lines


async def main() -> None:
    lines = await local_checks()
    lines.extend(await remote_checks())
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(OUT.read_text(encoding="utf-8"))


if __name__ == "__main__":
    asyncio.run(main())
