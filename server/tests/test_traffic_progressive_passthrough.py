"""Progressive SSE must not be buffered by traffic middleware."""

from app.traffic_middleware import should_buffer_traffic


def test_progressive_path_not_buffered():
    assert should_buffer_traffic("/api/extract/progressive") is False
    assert should_buffer_traffic("/api/extract/progressive/") is False


def test_extract_path_is_buffered():
    assert should_buffer_traffic("/api/extract") is True
    assert should_buffer_traffic("/api/extract/delta") is True
    assert should_buffer_traffic("/api/extract/sprite") is True


def test_untracked_path_not_buffered():
    assert should_buffer_traffic("/health") is False
    assert should_buffer_traffic("/api/tts") is False
