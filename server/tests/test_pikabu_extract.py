import asyncio

from app.extract import extract_article
from app.pikabu_extract import blocks_after_images_off, blocks_from_pikabu_html
from app.site_feeds import try_open_site


def test_pikabu_image_post_has_text_when_images_off():
    article = asyncio.run(
        extract_article(
            "https://pikabu.ru/story/kto_rano_vstayot_14002427",
            images_mode="off",
        )
    )
    paras = [b for b in article.blocks if b.type == "paragraph" and b.text]
    assert paras, "image-only post should get alt/title text"
    assert article.title


def test_pikabu_story_has_image_when_tiny():
    article = asyncio.run(
        extract_article(
            "https://pikabu.ru/story/elena_troyanskaya_zdorovogo_cheloveka_14002976",
            images_mode="tiny",
        )
    )
    image_blocks = [b for b in article.blocks if b.type == "image"]
    assert image_blocks
    assert article.stats.images_inlined >= 1
    assert image_blocks[0].src.startswith("data:image/jpeg;base64,")


def test_pikabu_text_post():
    article = asyncio.run(
        extract_article(
            "https://pikabu.ru/story/ded_i_vnuchka_14003602",
            images_mode="off",
        )
    )
    text = " ".join(b.text or "" for b in article.blocks if b.type == "paragraph")
    assert "дедом" in text.lower() or "дочь" in text.lower() or "василиса" in text.lower()


def test_pikabu_story_has_site_profile_and_byline():
    article = asyncio.run(
        extract_article(
            "https://pikabu.ru/story/kto_rano_vstayot_14002427",
            images_mode="layout",
        )
    )
    assert article.site_profile == "pikabu"
    assert len(article.byline) >= 4


def test_pikabu_home_feed_skips_video_links():
    opened = asyncio.run(try_open_site("https://pikabu.ru/", images_mode="off"))
    assert opened is not None
    for item in opened.feed.items:
        assert item.href is None or "/video/" not in item.href
