"""E2E: режимы страниц и экономия трафика (для 4 циклов оптимизации)."""
from __future__ import annotations
import asyncio
import json
import sys
from pathlib import Path

# run from server/
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.extract import extract_article
from app.site_feeds import try_open_site

CASES = [
    ("pikabu_story", "https://pikabu.ru/story/kto_rano_vstayot_14002427"),
    ("pikabu_story2", "https://pikabu.ru/story/ded_i_vnuchka_14003602"),
    ("example", "https://example.com"),
    ("wiki", "https://ru.wikipedia.org/wiki/Интернет"),
]

MODES = ("layout", "off", "tiny")


async def probe() -> list[dict]:
    rows: list[dict] = []
    for name, url in CASES:
        for mode in MODES:
            try:
                a = await extract_article(url, images_mode=mode)
                imgs = [b for b in a.blocks if b.type == "image"]
                placeholders = sum(1 for b in imgs if not b.src)
                paras = sum(1 for b in a.blocks if b.type == "paragraph" and b.text)
                heads = sum(1 for b in a.blocks if b.type == "heading")
                saved = a.stats.original_bytes - a.stats.payload_bytes
                pct = (saved * 100 // a.stats.original_bytes) if a.stats.original_bytes else 0
                rows.append(
                    {
                        "case": name,
                        "mode": mode,
                        "ok": True,
                        "blocks": len(a.blocks),
                        "paras": paras,
                        "heads": heads,
                        "images": len(imgs),
                        "placeholders": placeholders,
                        "inlined": a.stats.images_inlined,
                        "original_kb": a.stats.original_bytes // 1024,
                        "payload_kb": a.stats.payload_bytes // 1024,
                        "saved_pct": pct,
                        "title": (a.title or "")[:40],
                    }
                )
            except Exception as e:
                rows.append({"case": name, "mode": mode, "ok": False, "error": str(e)[:120]})
    opened = await try_open_site("https://pikabu.ru/", images_mode="layout")
    if opened and opened.feed:
        rows.append({"case": "pikabu_feed", "mode": "layout", "feed_items": len(opened.feed.items)})
    return rows


def main() -> None:
    rows = asyncio.run(probe())
    out = Path(__file__).resolve().parent.parent / "data" / "e2e_page_modes.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(rows, ensure_ascii=False, indent=2), encoding="utf-8")
    print(out.read_text(encoding="utf-8"))


if __name__ == "__main__":
    main()
