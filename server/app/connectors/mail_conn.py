"""Почта IMAP/SMTP на VPS."""

from __future__ import annotations

import email
import imaplib
import smtplib
import time
from email.header import decode_header
from email.mime.text import MIMEText

from fastapi import HTTPException

from ..config import settings
from ..credentials_store import (
    effective_mail_imap_host,
    effective_mail_imap_port,
    effective_mail_password,
    effective_mail_smtp_host,
    effective_mail_smtp_port,
    effective_mail_use_ssl,
    effective_mail_username,
    mail_is_configured,
)
from ..models import (
    ActResponse,
    ArticleStats,
    Block,
    FeedItem,
    FeedStats,
    OpenResponse,
    SaylatArticle,
    SaylatFeed,
)


def _decode_header(value: str | None) -> str:
    if not value:
        return ""
    parts = decode_header(value)
    out = []
    for chunk, enc in parts:
        if isinstance(chunk, bytes):
            out.append(chunk.decode(enc or "utf-8", errors="replace"))
        else:
            out.append(str(chunk))
    return " ".join(out).strip()


def _imap_connect() -> imaplib.IMAP4_SSL | imaplib.IMAP4:
    if not mail_is_configured():
        raise HTTPException(
            status_code=503,
            detail="Почта: введите IMAP-настройки в приложении и нажмите «Сохранить»",
        )
    host = effective_mail_imap_host()
    port = effective_mail_imap_port()
    if effective_mail_use_ssl():
        conn = imaplib.IMAP4_SSL(host, port)
    else:
        conn = imaplib.IMAP4(host, port)
    conn.login(effective_mail_username(), effective_mail_password())
    return conn


def _parse_mail_item_id(item_id: str) -> tuple[str, str]:
    parts = item_id.split("-", 2)
    if len(parts) < 3 or parts[0] != "mail":
        raise HTTPException(status_code=400, detail="Некорректный id письма")
    return parts[1], parts[2]


def _fetch_message(conn: imaplib.IMAP4, folder: str, uid: str) -> email.message.Message:
    conn.select(folder, readonly=True)
    key = uid.encode() if isinstance(uid, str) else uid
    _, msg_data = conn.fetch(key, "(RFC822)")
    if not msg_data or not msg_data[0]:
        raise HTTPException(status_code=404, detail="Письмо не найдено")
    return email.message_from_bytes(msg_data[0][1])


def _extract_plain_body(msg: email.message.Message, *, max_chars: int | None) -> str:
    chunks: list[str] = []
    if msg.is_multipart():
        for part in msg.walk():
            if part.get_content_type() != "text/plain":
                continue
            payload = part.get_payload(decode=True)
            if payload:
                chunks.append(payload.decode(errors="replace"))
    else:
        payload = msg.get_payload(decode=True)
        if payload:
            chunks.append(payload.decode(errors="replace"))
    text = "\n\n".join(c.strip() for c in chunks if c.strip())
    if not text:
        return "—"
    if max_chars is not None and len(text) > max_chars:
        return text[:max_chars] + "\n\n[… обрезано …]"
    return text


async def open_mail_message(item_id: str) -> OpenResponse:
    started = time.perf_counter()
    folder, uid = _parse_mail_item_id(item_id)
    conn = _imap_connect()
    try:
        msg = _fetch_message(conn, folder, uid)
        subject = _decode_header(msg.get("Subject")) or "(без темы)"
        from_ = _decode_header(msg.get("From"))
        date = _decode_header(msg.get("Date"))
        body = _extract_plain_body(msg, max_chars=settings.mail_body_max_chars)
        blocks: list[Block] = []
        if from_:
            blocks.append(Block(type="paragraph", text=f"От: {from_}"))
        if date:
            blocks.append(Block(type="paragraph", text=date))
        for para in body.split("\n\n"):
            line = para.strip()
            if line:
                blocks.append(Block(type="paragraph", text=line))
        if not blocks:
            blocks.append(Block(type="paragraph", text="—"))
        ms = int((time.perf_counter() - started) * 1000)
        article = SaylatArticle(
            url=f"saylat://mail/{item_id}",
            title=subject,
            byline=from_,
            blocks=blocks,
            stats=ArticleStats(fetch_ms=ms),
            site_profile="generic",
            layout_hint="article",
        )
        return OpenResponse(kind="article", article=article)
    finally:
        try:
            conn.logout()
        except Exception:
            pass


async def open_mail(resource_id: str | None) -> OpenResponse:
    rid = (resource_id or "").strip()
    if rid.startswith("mail-") and rid.count("-") >= 2:
        return await open_mail_message(rid)

    started = time.perf_counter()
    folder = rid or "INBOX"
    conn = _imap_connect()
    try:
        conn.select(folder, readonly=True)
        _, data = conn.search(None, "ALL")
        ids = data[0].split()
        ids = ids[-settings.mail_message_limit :]
        items: list[FeedItem] = []
        for num in reversed(ids):
            _, msg_data = conn.fetch(num, "(RFC822)")
            if not msg_data or not msg_data[0]:
                continue
            raw = msg_data[0][1]
            msg = email.message_from_bytes(raw)
            subject = _decode_header(msg.get("Subject"))
            from_ = _decode_header(msg.get("From"))
            uid = num.decode() if isinstance(num, bytes) else str(num)
            body = _extract_plain_body(msg, max_chars=400)
            items.append(
                FeedItem(
                    id=f"mail-{folder}-{uid}",
                    kind="message",
                    from_=from_,
                    title=subject or "(без темы)",
                    body=body,
                    unread=False,
                    actions=["open", "reply"],
                )
            )
        ms = int((time.perf_counter() - started) * 1000)
        feed = SaylatFeed(
            source="imap",
            title=f"Почта: {folder}",
            subtitle=effective_mail_username(),
            context_id=folder,
            items=items,
            stats=FeedStats(fetch_ms=ms),
        )
        return OpenResponse(kind="feed", feed=feed)
    finally:
        try:
            conn.logout()
        except Exception:
            pass


async def act_mail(item_id: str, action: str, body: str | None) -> ActResponse:
    if action != "reply":
        raise HTTPException(status_code=400, detail=f"Для почты: {action} не поддерживается")
    if not body or not body.strip():
        raise HTTPException(status_code=400, detail="Текст ответа обязателен")
    folder, uid = _parse_mail_item_id(item_id)
    conn = _imap_connect()
    try:
        msg = _fetch_message(conn, folder, uid)
        reply_to = msg.get("Reply-To") or msg.get("From")
        subject = _decode_header(msg.get("Subject"))
        re_subject = subject if subject.lower().startswith("re:") else f"Re: {subject}"
    finally:
        conn.logout()

    smtp_host = effective_mail_smtp_host()
    with smtplib.SMTP(smtp_host, effective_mail_smtp_port()) as smtp:
        if effective_mail_use_ssl():
            smtp.starttls()
        smtp.login(effective_mail_username(), effective_mail_password())
        mime = MIMEText(body.strip(), "plain", "utf-8")
        mime["Subject"] = re_subject
        mime["From"] = effective_mail_username()
        mime["To"] = _decode_header(reply_to)
        smtp.send_message(mime)

    return ActResponse(ok=True, message="Ответ отправлен")
