from app.strip_adblock import ADBLOCK_CSS, inject_adblock


def test_adblock_css_nonempty():
    assert "adsbygoogle" in ADBLOCK_CSS
    assert "display: none" in ADBLOCK_CSS


def test_inject_adblock_noop_without_page():
    import asyncio

    class Page:
        async def add_style_tag(self, content: str):
            self.css = content

        async def evaluate(self, script: str):
            self.js = script

    page = Page()
    asyncio.run(inject_adblock(page))
    assert hasattr(page, "css")
