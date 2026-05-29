from typing import Literal

from pydantic import BaseModel, Field, HttpUrl


BlockType = Literal["heading", "paragraph", "image", "list", "quote", "divider", "link"]
LayoutHint = Literal["article", "feed", "minimal", "gallery"]
SiteProfile = Literal["generic", "pikabu"]
StripRenderEngine = Literal["pillow", "browser", "browser_fallback_pillow"]
CompressionLevel = Literal["light", "medium", "full"]


class TextSpan(BaseModel):
    text: str
    href: str | None = None


class Block(BaseModel):
    type: BlockType
    text: str | None = None
    level: int | None = Field(default=None, ge=1, le=6)
    src: str | None = None
    alt: str | None = None
    width: int | None = None
    height: int | None = None
    items: list[str] | None = None
    spans: list[TextSpan] | None = None
    href: str | None = None


class ArticleStats(BaseModel):
    original_bytes: int = 0
    payload_bytes: int = 0
    images_inlined: int = 0
    images_omitted: int = 0
    fetch_ms: int = 0


class ArticleLink(BaseModel):
    text: str
    href: str


class CssHints(BaseModel):
    primary_color: str | None = None
    background_color: str | None = None
    body_font_size_sp: float | None = None
    heading_color: str | None = None


class SaylatArticle(BaseModel):
    url: str
    title: str
    excerpt: str = ""
    byline: str = ""
    lang: str = ""
    blocks: list[Block] = Field(default_factory=list)
    stats: ArticleStats = Field(default_factory=ArticleStats)
    layout_hint: LayoutHint = "article"
    site_profile: SiteProfile = "generic"
    compression_level: CompressionLevel = "medium"
    plain_text: str = ""
    links: list[ArticleLink] = Field(default_factory=list)
    css_hints: CssHints | None = None


ImagesMode = Literal["normal", "tiny", "off", "layout"]


class ExtractRequest(BaseModel):
    url: HttpUrl
    images: ImagesMode = "normal"
    level: CompressionLevel = "medium"


class SearchHit(BaseModel):
    title: str
    url: str
    snippet: str = ""
    source: str | None = None


class SearchResponse(BaseModel):
    query: str
    engine: str = "searxng"
    results: list[SearchHit] = Field(default_factory=list)
    fetch_ms: int = 0


class AppUpdateInfo(BaseModel):
    version_code: int
    version_name: str
    apk_url: str
    release_notes: str = ""
    mandatory: bool = False


class TranslateRequest(BaseModel):
    texts: list[str] = Field(..., min_length=1, max_length=36)
    source: str = "auto"
    target: str = "ru"


class TranslateResponse(BaseModel):
    translations: list[str] = Field(default_factory=list)
    source: str = "auto"
    target: str = "ru"
    provider: str = "mymemory"
    fetch_ms: int = 0


class PlaywrightStatus(BaseModel):
    enabled: bool = False
    available: bool = False
    active_renders: int = 0
    max_concurrent: int = 1
    total_renders: int = 0


class HealthResponse(BaseModel):
    status: str = "ok"
    version: str = "0.1.0"
    search: bool = True
    translate: bool = True
    thin_client: bool = True
    searx_instance: str = ""
    app_version_code: int = 0
    app_version_name: str = ""
    playwright: PlaywrightStatus | None = None
    cache_entries: int = 0
    cache_hits: int = 0


FeedItemKind = Literal["message", "chat", "thread", "link", "notice"]
FeedAction = Literal["open", "reply", "archive", "delete", "mark_read"]


class FeedItem(BaseModel):
    id: str
    kind: FeedItemKind = "message"
    title: str
    from_: str | None = Field(default=None, alias="from")
    time: str = ""
    body: str = ""
    unread: bool = False
    href: str | None = None
    thumb: str | None = None
    actions: list[FeedAction] = Field(default_factory=lambda: ["open"])

    model_config = {"populate_by_name": True}


class FeedStats(BaseModel):
    payload_bytes: int = 0
    fetch_ms: int = 0


class SaylatFeed(BaseModel):
    source: str = "article"
    title: str = ""
    subtitle: str = ""
    context_id: str = ""
    items: list[FeedItem] = Field(default_factory=list)
    stats: FeedStats = Field(default_factory=FeedStats)
    has_more: bool = False
    total_items: int = 0


OpenTarget = Literal["url", "mail", "telegram", "vk", "dzen"]


class OpenRequest(BaseModel):
    """POST /api/open — открыть страницу или сервис (тонкий клиент)."""
    target: OpenTarget = "url"
    url: str | None = None
    resource_id: str | None = None
    images: ImagesMode = "normal"
    level: CompressionLevel = "medium"


class OpenResponse(BaseModel):
    kind: Literal["article", "feed"] = "article"
    article: SaylatArticle | None = None
    feed: SaylatFeed | None = None


class QueryRequest(BaseModel):
    """POST /api/query — поиск / список."""
    q: str = Field(..., min_length=1)
    engine: str = "searxng"
    limit: int = Field(default=24, ge=1, le=48)


class QueryResponse(BaseModel):
    feed: SaylatFeed


class ActRequest(BaseModel):
    """POST /api/act — действие в сервисе (ответ, архив…)."""
    source: str = Field(..., description="imap | telegram | …")
    action: FeedAction
    item_id: str
    body: str | None = None
    context_id: str | None = None


class ConnectStatusResponse(BaseModel):
    telegram: bool = False
    mail: bool = False
    vk: bool = False
    dzen: bool = False
    telegram_hint: str = ""
    mail_hint: str = ""


class ServiceCredentialsPublic(BaseModel):
    telegram_api_id: int = 0
    telegram_api_hash: str = ""
    mail_imap_host: str = ""
    mail_imap_port: int = 993
    mail_smtp_host: str = ""
    mail_smtp_port: int = 587
    mail_username: str = ""
    mail_password_set: bool = False
    mail_use_ssl: bool = True
    vk_access_token_set: bool = False
    dzen_session_cookie_set: bool = False


class ServiceCredentialsUpdate(BaseModel):
    telegram_api_id: int | None = None
    telegram_api_hash: str | None = None
    mail_imap_host: str | None = None
    mail_imap_port: int | None = None
    mail_smtp_host: str | None = None
    mail_smtp_port: int | None = None
    mail_username: str | None = None
    mail_password: str | None = None
    mail_use_ssl: bool | None = None
    vk_access_token: str | None = None
    dzen_session_cookie: str | None = None


class ServiceCredentials(BaseModel):
    """Полная запись на диске (секреты)."""
    telegram_api_id: int = 0
    telegram_api_hash: str = ""
    mail_imap_host: str = ""
    mail_imap_port: int = 993
    mail_smtp_host: str = ""
    mail_smtp_port: int = 587
    mail_username: str = ""
    mail_password: str = ""
    mail_use_ssl: bool = True
    vk_access_token: str = ""
    dzen_session_cookie: str = ""


class TelegramCodeRequest(BaseModel):
    phone: str = Field(..., min_length=8)


class TelegramSignInRequest(BaseModel):
    phone: str = Field(..., min_length=8)
    code: str = Field(..., min_length=4, max_length=10)


class ActResponse(BaseModel):
    ok: bool = True
    message: str = ""


VisualTileKind = Literal[
    "heading",
    "paragraph",
    "image",
    "list",
    "quote",
    "divider",
    "link",
]


class VisualTile(BaseModel):
    kind: VisualTileKind
    text: str = ""
    level: int | None = Field(default=None, ge=1, le=6)
    src: str | None = None
    alt: str = ""
    width: int | None = None
    height: int | None = None
    items: list[str] | None = None
    href: str | None = None
    bytes_approx: int = 0


class VisualStats(BaseModel):
    original_bytes: int = 0
    payload_bytes: int = 0
    fetch_ms: int = 0
    build_ms: int = 0
    images_inlined: int = 0
    image_bytes_approx: int = 0


class StripSegment(BaseModel):
    index: int = 0
    src: str
    width: int = 360
    height: int = 0
    bytes_approx: int = 0


class StripStats(BaseModel):
    original_bytes: int = 0
    payload_bytes: int = 0
    strip_count: int = 0
    fetch_ms: int = 0
    build_ms: int = 0


class StripPageResponse(BaseModel):
    """Opera Mini–лайт: вертикальные JPEG-полосы."""

    url: str
    title: str = ""
    site_profile: SiteProfile = "generic"
    strips: list[StripSegment] = Field(default_factory=list)
    links: list[ArticleLink] = Field(default_factory=list)
    strip_width: int = 360
    render_engine: StripRenderEngine = "pillow"
    stats: StripStats = Field(default_factory=StripStats)


class VisualPageResponse(BaseModel):
    """Визуальная копия: плитки + JPEG data URL (режим Opera Mini–лайт)."""

    url: str
    title: str
    excerpt: str = ""
    lang: str = ""
    tiles: list[VisualTile] = Field(default_factory=list)
    structure_hint: LayoutHint = "article"
    stats: VisualStats = Field(default_factory=VisualStats)
