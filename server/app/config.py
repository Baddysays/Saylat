from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

_DATA_DIR = Path(__file__).resolve().parent.parent / "data"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_prefix="SAYLAT_")

    host: str = "0.0.0.0"
    port: int = 8787
    request_timeout_sec: float = 25.0
    max_html_bytes: int = 2_000_000
    max_image_bytes: int = 400_000
    image_max_width: int = 480
    image_jpeg_quality: int = 52
    max_images: int = 6
    searx_instance: str = "https://searx.tiekoetter.com"
    searx_fallbacks: str = "https://search.sapti.me,https://search.mdosch.de"
    search_language: str = "ru-RU"
    search_max_results: int = 24
    app_version_code: int = 43
    app_version_name: str = "0.5.33"
    app_release_notes: str = (
        "0.5.33: единый логотип Saylat — иконка приложения, интерфейс и README; обновлён брендинг."
    )
    translate_timeout_sec: float = 20.0
    translate_default_target: str = "ru"
    app_update_mandatory: bool = False
    user_agent: str = (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
    )
    allowed_schemes: tuple[str, ...] = ("http", "https")
    cache_ttl_sec: int = 900
    cache_max_entries: int = 400
    redis_url: str = ""

    # Защита личного VPS (пусто = без ключа)
    api_key: str = ""
    rate_limit_per_minute: int = 120

    # Playwright — скриншот-полосы (Opera Mini)
    playwright_enabled: bool = True
    playwright_timeout_sec: float = 35.0
    playwright_wait_until: str = "domcontentloaded"
    playwright_settle_ms: int = 800
    playwright_max_concurrent: int = 2
    strip_viewport_width: int = 360
    strip_slice_height: int = 840
    strip_max_count: int = 14
    strip_jpeg_quality: int = 46

    # Telegram (Telethon, сессия на VPS)
    telegram_api_id: int = 0
    telegram_api_hash: str = ""
    telegram_session_path: str = str(_DATA_DIR / "telegram")

    # Почта IMAP/SMTP
    mail_imap_host: str = ""
    mail_imap_port: int = 993
    mail_smtp_host: str = ""
    mail_smtp_port: int = 587
    mail_username: str = ""
    mail_password: str = ""
    mail_use_ssl: bool = True

    # VK API
    vk_access_token: str = ""

    # Дзен (cookie сессии, опционально)
    dzen_session_cookie: str = ""

    telegram_message_limit: int = 40
    mail_message_limit: int = 30
    mail_body_max_chars: int = 8000
    vk_feed_limit: int = 25

    def telegram_configured(self) -> bool:
        return bool(self.telegram_api_id and self.telegram_api_hash and self.telegram_session_path)

    def mail_configured(self) -> bool:
        return bool(self.mail_imap_host and self.mail_username and self.mail_password)

    def vk_configured(self) -> bool:
        return bool(self.vk_access_token.strip())

    def dzen_configured(self) -> bool:
        return bool(self.dzen_session_cookie.strip())

    def telegram_status_hint(self) -> str:
        if not self.telegram_api_id or not self.telegram_api_hash:
            return "Задайте API ID и hash на сервере (.env)"
        session_file = Path(f"{self.telegram_session_path}.session")
        if session_file.is_file():
            return "Сессия найдена"
        return "Войдите при Wi‑Fi: код из Telegram"

    def mail_status_hint(self) -> str:
        if self.mail_configured():
            return self.mail_username
        return "Настройте IMAP в .env на VPS"


settings = Settings()
