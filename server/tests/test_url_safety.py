import pytest

from app.url_safety import validate_public_http_url


def test_accepts_https():
    assert validate_public_http_url("https://example.com/path") == "https://example.com/path"


def test_rejects_localhost():
    with pytest.raises(ValueError):
        validate_public_http_url("http://localhost/test")


def test_rejects_private_ip():
    with pytest.raises(ValueError):
        validate_public_http_url("http://192.168.1.1/")
