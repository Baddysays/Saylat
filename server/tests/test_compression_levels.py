from app.compression_levels import apply_compression_level, parse_compression_level
from app.models import Block, SaylatArticle, TextSpan


def _sample_article() -> SaylatArticle:
    return SaylatArticle(
        url="https://example.com/a",
        title="Заголовок",
        excerpt="Кратко",
        blocks=[
            Block(type="heading", text="Раздел", level=2),
            Block(type="paragraph", text="Абзац с [ссылкой](https://example.com/x)."),
            Block(
                type="paragraph",
                text="Текст",
                spans=[TextSpan(text="клик", href="https://example.com/go")],
            ),
            Block(type="image", src="data:image/jpeg;base64,abc", alt="photo"),
            Block(type="divider"),
            Block(type="list", items=["один", "два"]),
        ],
    )


def test_parse_level_header():
    assert parse_compression_level(None, "full") == "full"
    assert parse_compression_level("light", None) == "light"


def test_light_strips_blocks_and_images():
    article = apply_compression_level(_sample_article(), "light")
    assert article.compression_level == "light"
    assert "Абзац" in article.plain_text
    assert len(article.links) >= 1
    assert all(b.type != "divider" for b in article.blocks)


def test_medium_limits_images():
    blocks = [Block(type="image", src=f"http://x/{i}.jpg", alt="x") for i in range(8)]
    blocks.append(Block(type="paragraph", text="текст"))
    article = apply_compression_level(
        SaylatArticle(url="https://example.com", title="T", blocks=blocks),
        "medium",
    )
    assert article.compression_level == "medium"
    assert sum(1 for b in article.blocks if b.type == "image") <= 5


def test_full_has_css_hints():
    article = apply_compression_level(_sample_article(), "full")
    assert article.compression_level == "full"
    assert article.css_hints is not None
    assert article.css_hints.primary_color
