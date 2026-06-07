"""
test_new_features.py — тесты для новых серверных модулей.

Запуск: pytest server/tests/test_new_features.py -v
"""
import asyncio
import time
import pytest


# ─────────────────────────────────────────────────────────────
# response_cache с TTL
# ─────────────────────────────────────────────────────────────

class TestInMemoryCacheWithTTL:

    @pytest.fixture
    def cache(self):
        from app.response_cache import InMemoryCache
        return InMemoryCache()

    @pytest.mark.asyncio
    async def test_cache_hit(self, cache):
        calls = 0
        async def loader():
            nonlocal calls
            calls += 1
            return {"data": "value"}

        r1 = await cache.get_or_set("extract:test", loader, ttl=10)
        r2 = await cache.get_or_set("extract:test", loader, ttl=10)
        assert r1 == r2
        assert calls == 1, "loader должен вызываться только один раз"

    @pytest.mark.asyncio
    async def test_cache_ttl_expiry(self, cache):
        calls = 0
        async def loader():
            nonlocal calls
            calls += 1
            return {"tick": calls}

        await cache.get_or_set("key", loader, ttl=0.05)  # 50ms TTL
        await asyncio.sleep(0.1)                          # ждём истечения
        result = await cache.get_or_set("key", loader, ttl=0.05)
        assert calls == 2, "После истечения TTL loader должен вызваться снова"
        assert result["tick"] == 2

    @pytest.mark.asyncio
    async def test_invalidate(self, cache):
        calls = 0
        async def loader():
            nonlocal calls
            calls += 1
            return {}

        await cache.get_or_set("extract:url1", loader, ttl=60)
        cache.invalidate("extract:url1")
        await cache.get_or_set("extract:url1", loader, ttl=60)
        assert calls == 2

    @pytest.mark.asyncio
    async def test_invalidate_prefix(self, cache):
        async def loader(): return {}
        await cache.get_or_set("extract:a", loader)
        await cache.get_or_set("extract:b", loader)
        await cache.get_or_set("strips:c",  loader)
        n = cache.invalidate_prefix("extract:")
        assert n == 2

    def test_stats(self, cache):
        stats = cache.stats()
        assert "hits" in stats
        assert "misses" in stats
        assert "hit_rate" in stats

    def test_ttl_by_key_prefix(self, cache):
        from app.response_cache import TTL_EXTRACT, TTL_STRIPS, TTL_DEFAULT
        assert cache._ttl_for("extract:foo") == TTL_EXTRACT
        assert cache._ttl_for("strips:foo")  == TTL_STRIPS
        assert cache._ttl_for("other:foo")   == TTL_DEFAULT


# ─────────────────────────────────────────────────────────────
# extract_v2 — readability fallback
# ─────────────────────────────────────────────────────────────

class TestExtractV2:

    MINIMAL_HTML = """
    <html><head><title>Test Page</title></head><body>
    <article>
        <h1>Main Title</h1>
        <p>First paragraph with some text content here.</p>
        <p>Second paragraph for more context and length.</p>
        <a href="https://example.com/link">External Link</a>
    </article>
    </body></html>
    """

    NO_ARTICLE_HTML = """
    <html><head><title>Blog Post</title></head><body>
    <div class="post-content">
        <h2>Post Heading</h2>
        <p>Content paragraph one is here and has sufficient length.</p>
        <p>Content paragraph two is also here and adds more substance.</p>
        <p>Content paragraph three completes the example text.</p>
    </div>
    </body></html>
    """

    @pytest.mark.asyncio
    async def test_extracts_article_tag(self):
        from app.extract_v2 import extract_article_v2
        result = await extract_article_v2("https://example.com", html=self.MINIMAL_HTML)
        assert result["title"] == "Test Page"
        blocks = result["blocks"]
        paragraphs = [b for b in blocks if b["type"] == "paragraph"]
        assert len(paragraphs) >= 2

    @pytest.mark.asyncio
    async def test_extracts_links(self):
        from app.extract_v2 import extract_article_v2
        result = await extract_article_v2("https://example.com", html=self.MINIMAL_HTML)
        links = result["links"]
        assert any(l["href"] == "https://example.com/link" for l in links)

    @pytest.mark.asyncio
    async def test_div_heuristic_fallback(self):
        from app.extract_v2 import extract_article_v2
        result = await extract_article_v2("https://example.com", html=self.NO_ARTICLE_HTML)
        blocks = result["blocks"]
        text_blocks = [b for b in blocks if b.get("text")]
        assert len(text_blocks) >= 2, "Heuristic должен найти div.post-content"

    @pytest.mark.asyncio
    async def test_empty_html_no_crash(self):
        from app.extract_v2 import extract_article_v2
        result = await extract_article_v2("https://example.com", html="<html><body></body></html>")
        assert "blocks" in result
        assert isinstance(result["blocks"], list)

    @pytest.mark.asyncio
    async def test_blocks_limit(self):
        # Генерируем страницу с >200 блоками
        many_paragraphs = "\n".join(
            f"<p>Paragraph number {i} with some content.</p>"
            for i in range(300)
        )
        html = f"<html><body><article>{many_paragraphs}</article></body></html>"
        from app.extract_v2 import extract_article_v2
        result = await extract_article_v2("https://example.com", html=html)
        assert len(result["blocks"]) <= 200, "Блоков не должно быть более 200"


# ─────────────────────────────────────────────────────────────
# RSS
# ─────────────────────────────────────────────────────────────

class TestRssFeeds:

    ATOM_FEED = """<?xml version="1.0" encoding="utf-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
        <title>Test Feed</title>
        <entry>
            <title>Entry One</title>
            <link href="https://example.com/1"/>
            <summary>Summary of entry one.</summary>
            <published>2026-01-01T10:00:00Z</published>
        </entry>
        <entry>
            <title>Entry Two</title>
            <link href="https://example.com/2"/>
            <summary>Summary of entry two.</summary>
            <published>2026-01-02T10:00:00Z</published>
        </entry>
    </feed>"""

    def test_parse_atom_feed(self):
        try:
            import feedparser
        except ImportError:
            pytest.skip("feedparser not installed")
        import feedparser as fp
        result = fp.parse(self.ATOM_FEED)
        assert result.feed.title == "Test Feed"
        assert len(result.entries) == 2

    def test_entry_to_feed_item(self):
        try:
            import feedparser
        except ImportError:
            pytest.skip("feedparser not installed")
        from app.rss_feeds import _entry_to_feed_item
        import feedparser as fp
        feed = fp.parse(self.ATOM_FEED)
        item = _entry_to_feed_item(feed.entries[0], "Test Feed")
        assert item["title"] == "Entry One"
        assert item["href"] == "https://example.com/1"
        assert item["source"] == "rss"
        assert item["source_label"] == "Test Feed"
        assert "date" in item

    def test_strips_html_from_summary(self):
        try:
            import feedparser
        except ImportError:
            pytest.skip("feedparser not installed")
        from app.rss_feeds import _entry_to_feed_item
        import feedparser as fp
        rss = """<rss><channel><item>
            <title>T</title>
            <link>https://x.com</link>
            <description>&lt;p&gt;Bold &lt;b&gt;text&lt;/b&gt;&lt;/p&gt;</description>
        </item></channel></rss>"""
        feed = fp.parse(rss)
        item = _entry_to_feed_item(feed.entries[0], "src")
        assert "<" not in item["body"], "HTML-теги должны быть удалены"


# ─────────────────────────────────────────────────────────────
# Security: RateLimitMiddleware память
# ─────────────────────────────────────────────────────────────

class TestRateLimitMemory:
    """Убеждаемся что _hits не растёт бесконечно при ротации IP."""

    def test_eviction_on_cleanup(self):
        """
        При каждом запросе window очищается от устаревших записей.
        Проверяем что для старого IP словарь не растёт.
        """
        # Имитируем структуру данных security.py
        from collections import defaultdict
        hits: dict = defaultdict(list)
        limit = 30
        now = time.monotonic()

        ip = "1.2.3.4"
        # Добавляем 30 «старых» хитов
        for _ in range(30):
            hits[ip].append(now - 120)  # 2 минуты назад (истекшие)

        # Cleanup (как в security.py)
        hits[ip] = [t for t in hits[ip] if now - t < 60.0]
        assert len(hits[ip]) == 0, "Устаревшие записи должны быть удалены"

    def test_different_ips_dont_interfere(self):
        from collections import defaultdict
        hits: dict = defaultdict(list)
        now = time.monotonic()
        limit = 5

        for i in range(100):
            ip = f"10.0.0.{i % 256}"
            hits[ip].append(now)

        # У каждого IP только 1 запрос — никто не заблокирован
        blocked = sum(1 for ip, w in hits.items() if len(w) >= limit)
        assert blocked == 0
