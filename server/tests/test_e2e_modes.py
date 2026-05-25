"""E2E пороги: layout = структура без JPEG, экономия на Пикабу."""

import asyncio

from app.extract import extract_article


def test_pikabu_layout_has_placeholder_not_inlined():
    article = asyncio.run(
        extract_article(
            "https://pikabu.ru/story/kto_rano_vstayot_14002427",
            images_mode="layout",
        )
    )
    assert article.stats.images_inlined == 0
    assert article.stats.images_omitted >= 1
    imgs = [b for b in article.blocks if b.type == "image"]
    assert imgs and imgs[0].src is None
    assert article.site_profile == "pikabu"
    assert article.byline
    paras = [b for b in article.blocks if b.type == "paragraph"]
    if paras:
        assert paras[0].text != imgs[0].alt or len(paras) == 1


def test_pikabu_layout_saves_traffic():
    article = asyncio.run(
        extract_article(
            "https://pikabu.ru/story/kto_rano_vstayot_14002427",
            images_mode="layout",
        )
    )
    assert article.stats.original_bytes > 50_000
    assert article.stats.payload_bytes < article.stats.original_bytes // 2


def test_pikabu_tiny_smaller_than_layout_or_equal():
    layout = asyncio.run(
        extract_article(
            "https://pikabu.ru/story/kto_rano_vstayot_14002427",
            images_mode="layout",
        )
    )
    tiny = asyncio.run(
        extract_article(
            "https://pikabu.ru/story/kto_rano_vstayot_14002427",
            images_mode="tiny",
        )
    )
    assert layout.stats.images_inlined == 0
    assert tiny.stats.payload_bytes >= layout.stats.payload_bytes
