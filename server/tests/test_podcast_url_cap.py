"""PodcastRequest urls list is capped at 20."""

import pytest
from pydantic import ValidationError

from app.podcast import PodcastRequest


def test_podcast_request_accepts_20_urls():
    urls = [f"https://example.com/{i}" for i in range(20)]
    req = PodcastRequest(urls=urls)
    assert len(req.urls) == 20


def test_podcast_request_rejects_21_urls():
    urls = [f"https://example.com/{i}" for i in range(21)]
    with pytest.raises(ValidationError):
        PodcastRequest(urls=urls)
