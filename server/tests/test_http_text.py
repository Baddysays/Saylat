from types import SimpleNamespace

from app.http_text import decode_response_text


def _resp(content: bytes, charset: str | None = None):
    headers = {}
    if charset:
        headers["content-type"] = f"text/html; charset={charset}"
    return SimpleNamespace(content=content, charset_encoding=charset, headers=headers)


def test_decode_utf8_html():
    raw = "<html><head><meta charset=utf-8></head><body>Привет</body></html>".encode()
    text = decode_response_text(_resp(raw, "utf-8"))
    assert "Привет" in text


def test_decode_cp1251_when_header_wrong():
    raw = (
        b"<html><head><meta charset=windows-1251></head><body>"
        + "Привет".encode("cp1251")
        + b"</body></html>"
    )
    text = decode_response_text(_resp(raw, "iso-8859-1"))
    assert "Привет" in text
