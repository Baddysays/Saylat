import asyncio
import sys

from app.extract import extract_article
from app.site_feeds import try_open_site

OUT = "data/pikabu_probe.txt"


async def main() -> None:
    lines: list[str] = []
    opened = await try_open_site("https://pikabu.ru/", images_mode="off")
    for item in opened.feed.items[:8]:
        if not item.href:
            continue
        article = await extract_article(item.href, images_mode="off")
        paras = [b for b in article.blocks if b.type == "paragraph" and b.text]
        imgs = [b for b in article.blocks if b.type == "image"]
        lines.append(f"URL: {item.href}")
        lines.append(f"title: {article.title}")
        lines.append(f"paragraphs: {len(paras)} images: {len(imgs)}")
        for i, p in enumerate(paras[:3]):
            lines.append(f"  p{i}: {p.text[:200]}")
        if not paras:
            for b in article.blocks[:8]:
                lines.append(f"  block {b.type}: {(b.text or b.src or '')[:80]}")
        lines.append("")
    Path = __import__("pathlib").Path
    Path(OUT).write_text("\n".join(lines), encoding="utf-8")
    lines.append(f"written {OUT}")


if __name__ == "__main__":
    asyncio.run(main())
    print("ok", file=sys.stderr)
