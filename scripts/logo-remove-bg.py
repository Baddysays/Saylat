#!/usr/bin/env python3
"""Make logo PNGs transparent by removing near-white / outer background."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image


def color_dist(a: tuple[int, ...], b: tuple[int, ...]) -> float:
    return sum((int(a[i]) - int(b[i])) ** 2 for i in range(3)) ** 0.5


def remove_corners_flood(
    img: Image.Image,
    tolerance: int = 32,
    corner_sample: int = 16,
) -> Image.Image:
    """Flood-fill from corners; removes outer background only (keeps inner cream circle)."""
    rgba = img.convert("RGBA")
    w, h = rgba.size
    px = rgba.load()
    ref_colors: list[tuple[int, int, int]] = []
    seen: set[tuple[int, int, int]] = set()
    for x in list(range(min(corner_sample, w))) + list(
        range(max(0, w - corner_sample), w)
    ):
        for y in list(range(min(corner_sample, h))) + list(
            range(max(0, h - corner_sample), h)
        ):
            c = px[x, y][:3]
            if c not in seen:
                seen.add(c)
                ref_colors.append(c)

    def matches_bg(rgb: tuple[int, int, int]) -> bool:
        return any(color_dist(rgb, ref) <= tolerance for ref in ref_colors)

    visited: set[tuple[int, int]] = set()
    stack = [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]
    while stack:
        x, y = stack.pop()
        if (x, y) in visited or x < 0 or y < 0 or x >= w or y >= h:
            continue
        visited.add((x, y))
        cur = px[x, y]
        if not matches_bg(cur[:3]):
            continue
        px[x, y] = (cur[0], cur[1], cur[2], 0)
        stack.extend([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)])

    return rgba


def remove_near_white(
    img: Image.Image,
    threshold: int = 248,
    soften: int = 12,
) -> Image.Image:
    """Turn near-white pixels transparent (full canvas)."""
    rgba = img.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if r >= threshold and g >= threshold and b >= threshold:
                px[x, y] = (r, g, b, 0)
            elif r >= threshold - soften and g >= threshold - soften and b >= threshold - soften:
                # soft edge
                t = min(r, g, b)
                alpha = int(255 * (threshold - t) / max(soften, 1))
                px[x, y] = (r, g, b, max(0, min(255, alpha)))
    return rgba


def remove_teal_gradient(img: Image.Image, tolerance: int = 42) -> Image.Image:
    """Remove teal/cyan gradient app-icon background; keeps white line art."""
    return remove_corners_flood(img, tolerance=tolerance, corner_sample=24)


def _is_saylat_foreground(r: int, g: int, b: int) -> bool:
    """Keep white line art, text, cream/yellow dots (incl. anti-aliased edges)."""
    lum = (r + g + b) / 3
    if lum >= 185:
        return True
    if r >= 165 and g >= 130 and b <= 150 and (r + g) > b + 180:
        return True
    return False


def _is_saylat_background(r: int, g: int, b: int) -> bool:
    """Teal/cyan gradient, black letterbox — not foreground."""
    if _is_saylat_foreground(r, g, b):
        return False
    if max(r, g, b) < 40:
        return True
    lum = (r + g + b) / 3
    if lum < 175 and g >= 50 and b >= 50 and (g + b) > r + 30:
        return True
    return False


def remove_saylat_logo_bg(img: Image.Image) -> Image.Image:
    """Remove Saylat app-icon teal gradient; keep white + yellow elements."""
    rgba = img.convert("RGBA")
    px = rgba.load()
    w, h = rgba.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if _is_saylat_background(r, g, b):
                px[x, y] = (r, g, b, 0)
    return rgba


def process(path: Path, mode: str, out: Path | None) -> Path:
    img = Image.open(path)
    if mode == "flood":
        out_img = remove_corners_flood(img)
    elif mode == "teal":
        out_img = remove_teal_gradient(img)
    elif mode == "saylat":
        out_img = remove_saylat_logo_bg(img)
    else:
        out_img = remove_near_white(img)
    if out:
        dest = out
    else:
        stem = path.stem.removesuffix("-raw")
        dest = path.with_name(stem + ".png")
        if dest == path:
            dest = path.with_name(path.stem + "-transparent.png")
    out_img.save(dest, "PNG")
    return dest


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("inputs", nargs="+", type=Path)
    p.add_argument(
        "--mode",
        choices=("white", "flood", "teal", "saylat"),
        default="white",
        help="white | flood | teal | saylat (Saylat gradient icon)",
    )
    p.add_argument("-o", "--output", type=Path, default=None)
    args = p.parse_args()
    for inp in args.inputs:
        out = process(inp, args.mode, args.output)
        print(out)
    return 0


if __name__ == "__main__":
    sys.exit(main())
