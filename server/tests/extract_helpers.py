"""Разбор ответа /api/extract после введения ArticleWireEnvelope."""

from app.models import WireCompressedPayload
from app.payload_codec import decompress_article


def unwrap_extract_article(data: dict) -> dict:
    article = data.get("article")
    if article is not None:
        return article
    wire = data.get("wire")
    if wire is not None:
        return decompress_article(WireCompressedPayload.model_validate(wire)).model_dump()
    raise AssertionError("extract response missing article envelope")
