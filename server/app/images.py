import base64
import io
from dataclasses import dataclass
from urllib.parse import urljoin, urlparse

import httpx
from PIL import Image

from .config import settings


@dataclass(frozen=True)
class ImageInlineProfile:
    max_images: int
    max_width: int
    max_source_bytes: int
    jpeg_quality: int
    target_bytes: int


NORMAL_PROFILE = ImageInlineProfile(
    max_images=settings.max_images,
    max_width=settings.image_max_width,
    max_source_bytes=settings.max_image_bytes,
    jpeg_quality=settings.image_jpeg_quality,
    target_bytes=50_000,
)

TINY_PROFILE = ImageInlineProfile(
    max_images=4,
    max_width=128,
    max_source_bytes=200_000,
    jpeg_quality=32,
    target_bytes=8_192,
)


def _is_safe_image_url(url: str, page_url: str) -> bool:
    try:
        parsed = urlparse(url)
        if parsed.scheme not in settings.allowed_schemes:
            return False
        page_host = urlparse(page_url).hostname
        if not page_host:
            return False
        return bool(parsed.hostname)
    except Exception:
        return False


def _encode_jpeg(img: Image.Image, quality: int) -> bytes:
    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=quality, optimize=True)
    return buf.getvalue()


def _compress_to_target(img: Image.Image, profile: ImageInlineProfile) -> bytes:
    working = img
    width = profile.max_width
    quality = profile.jpeg_quality
    best = _encode_jpeg(working, quality)

    for _ in range(8):
        if len(best) <= profile.target_bytes:
            return best
        if quality > 18:
            quality -= 6
        elif width > 64:
            width = max(64, int(width * 0.75))
            ratio = width / working.size[0]
            working = working.resize(
                (width, max(1, int(working.size[1] * ratio))),
                Image.Resampling.BILINEAR,
            )
            quality = profile.jpeg_quality
        else:
            break
        best = _encode_jpeg(working, quality)

    if len(best) > profile.target_bytes:
        return b""
    return best


async def fetch_image_data_url(
    client: httpx.AsyncClient,
    image_url: str,
    page_url: str,
    profile: ImageInlineProfile = NORMAL_PROFILE,
) -> tuple[str | None, int, int]:
    absolute = urljoin(page_url, image_url)
    if not _is_safe_image_url(absolute, page_url):
        return None, 0, 0

    headers: dict[str, str] = {}
    page_host = urlparse(page_url).hostname or ""
    if page_host.endswith("pikabu.ru"):
        headers["Referer"] = "https://pikabu.ru/"
    try:
        resp = await client.get(
            absolute,
            timeout=settings.request_timeout_sec,
            headers=headers or None,
        )
        resp.raise_for_status()
        raw = resp.content[: profile.max_source_bytes]
        if not raw:
            return None, 0, 0

        img = Image.open(io.BytesIO(raw))
        if profile.target_bytes <= 2_500:
            img = img.convert("L")
        elif img.mode not in ("RGB", "L"):
            img = img.convert("RGB")

        w, h = img.size
        if w > profile.max_width:
            ratio = profile.max_width / w
            resample = (
                Image.Resampling.BILINEAR
                if profile.target_bytes <= 2_500
                else Image.Resampling.LANCZOS
            )
            img = img.resize(
                (profile.max_width, max(1, int(h * ratio))),
                resample,
            )
            w, h = img.size

        jpeg = _compress_to_target(img, profile)
        if not jpeg:
            return None, 0, 0
        if len(jpeg) > profile.target_bytes and profile.target_bytes <= 2_500:
            return None, 0, 0

        encoded = base64.b64encode(jpeg).decode("ascii")
        return f"data:image/jpeg;base64,{encoded}", w, h
    except Exception:
        return None, 0, 0
