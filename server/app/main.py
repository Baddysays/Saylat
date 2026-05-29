from contextlib import asynccontextmanager
from pathlib import Path

import httpx
from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, Response
from fastapi.staticfiles import StaticFiles

from .config import settings
from .extract import extract_article, extract_plain_fallback
from .security import ApiKeyMiddleware, RateLimitMiddleware
from .unified_feed import build_unified_feed
from .site_feeds import feed_to_article, try_open_site
from .connect_api import (
    get_connect_status,
    get_service_credentials,
    save_service_credentials,
    telegram_request_code,
    telegram_sign_in,
)
from .models import (
    ActRequest,
    ActResponse,
    AppUpdateInfo,
    ConnectStatusResponse,
    ServiceCredentialsPublic,
    ServiceCredentialsUpdate,
    ExtractRequest,
    HealthResponse,
    PlaywrightStatus,
    OpenRequest,
    OpenResponse,
    QueryRequest,
    QueryResponse,
    SaylatArticle,
    SaylatFeed,
    SearchResponse,
    TelegramCodeRequest,
    TelegramSignInRequest,
    TranslateRequest,
    TranslateResponse,
)
from .search import search_web
from .thin_client import act_on_item, open_resource, query_feed
from .translate import translate_texts
from .update import apk_file_response, get_update_info
from .url_safety import validate_public_http_url
from .response_cache import response_cache
from .compression_levels import (
    apply_compression_level,
    images_mode_for_level,
    parse_compression_level,
)
from .proxy_page import fetch_proxy_html
from .visual_render import build_visual_page
from .browser_strips import BrowserStripsError, playwright_render_status
from .screenshot_strips import build_strip_page
from .images import TINY_PROFILE, fetch_image_data_url
from .models import StripPageResponse, VisualPageResponse

@asynccontextmanager
async def _app_lifespan(_app: FastAPI):
    yield
    from .browser_strips import shutdown_browser

    await shutdown_browser()


app = FastAPI(
    title="Saylat Proxy",
    description="Saylat — сжатие и вырезка страниц для медленных сетей",
    version="0.1.0",
    lifespan=_app_lifespan,
)

_cors_origins = settings.cors_origin_list()
if _cors_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=_cors_origins,
        allow_methods=["GET", "POST", "PUT"],
        allow_headers=["*"],
    )
app.add_middleware(RateLimitMiddleware, limit_per_minute=settings.rate_limit_per_minute)
app.add_middleware(ApiKeyMiddleware)

_STATIC_DIR = Path(__file__).resolve().parent.parent / "static"
app.mount("/static", StaticFiles(directory=_STATIC_DIR), name="static")


@app.get("/", include_in_schema=False)
async def web_ui() -> FileResponse:
    return FileResponse(_STATIC_DIR / "index.html")


_BENCH_BYTES = b"x" * 65_536
_BENCH_LITE_BYTES = b"x" * 8_192


@app.get("/api/bench", include_in_schema=False)
async def bench_download() -> Response:
    """Fixed-size payload for in-app download speed tests."""
    return Response(content=_BENCH_BYTES, media_type="application/octet-stream")


@app.get("/api/bench/lite", include_in_schema=False)
async def bench_download_lite() -> Response:
    """Small payload for 2G / high-latency speed tests."""
    return Response(content=_BENCH_LITE_BYTES, media_type="application/octet-stream")


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    pw = playwright_render_status()
    cache = response_cache.stats()
    return HealthResponse(
        searx_instance=settings.searx_instance,
        app_version_code=settings.app_version_code,
        app_version_name=settings.app_version_name,
        cache_entries=cache["entries"],
        cache_hits=cache["hits"],
        playwright=PlaywrightStatus(
            enabled=bool(pw["enabled"]),
            available=bool(pw["available"]),
            active_renders=int(pw["active_renders"]),
            max_concurrent=int(pw["max_concurrent"]),
            total_renders=int(pw["total_renders"]),
        ),
    )


@app.get("/api/app/update", response_model=AppUpdateInfo)
async def app_update(request: Request) -> AppUpdateInfo:
    return get_update_info(str(request.base_url))


@app.get("/app/download/saylat.apk", include_in_schema=False)
async def app_download_apk() -> FileResponse:
    return apk_file_response()


@app.get("/api/search", response_model=SearchResponse)
async def search_get(
    q: str = Query(..., min_length=1, description="Поисковый запрос"),
    engine: str = Query("searxng", description="Зарезервировано; выдача: DuckDuckGo + Wikipedia"),
) -> SearchResponse:
    try:
        return await search_web(q, engine=engine.strip().lower())
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Search upstream failed: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/open", response_model=OpenResponse)
async def thin_open(body: OpenRequest) -> OpenResponse:
    try:
        return await open_resource(body)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/feed", response_model=SaylatFeed)
async def unified_feed_get(
    limit: int = Query(12, ge=1, le=40, description="Элементов с каждого источника"),
    offset: int = Query(0, ge=0, le=500, description="Смещение в объединённой ленте"),
    page_size: int = Query(24, ge=1, le=80, description="Размер страницы ленты"),
) -> SaylatFeed:
    try:
        return await build_unified_feed(per_source=limit, offset=offset, limit=page_size)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/query", response_model=QueryResponse)
async def thin_query(body: QueryRequest) -> QueryResponse:
    try:
        return await query_feed(body)
    except HTTPException:
        raise
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Query upstream failed: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/connect/status", response_model=ConnectStatusResponse)
async def connect_status() -> ConnectStatusResponse:
    return await get_connect_status()


@app.get("/api/connect/credentials", response_model=ServiceCredentialsPublic)
async def connect_credentials_get() -> ServiceCredentialsPublic:
    return get_service_credentials()


@app.put("/api/connect/credentials", response_model=ServiceCredentialsPublic)
async def connect_credentials_put(body: ServiceCredentialsUpdate) -> ServiceCredentialsPublic:
    return save_service_credentials(body)


@app.post("/api/connect/telegram/code")
async def connect_telegram_code(body: TelegramCodeRequest) -> dict[str, str]:
    try:
        return await telegram_request_code(body)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/connect/telegram/signin")
async def connect_telegram_signin(body: TelegramSignInRequest) -> dict[str, str]:
    try:
        return await telegram_sign_in(body)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/act", response_model=ActResponse)
async def thin_act(body: ActRequest) -> ActResponse:
    try:
        return await act_on_item(body)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/translate", response_model=TranslateResponse)
async def translate_post(body: TranslateRequest) -> TranslateResponse:
    try:
        translations, ms = await translate_texts(
            body.texts,
            source=body.source.strip().lower() or "auto",
            target=body.target.strip().lower() or "ru",
        )
        return TranslateResponse(
            translations=translations,
            source=body.source,
            target=body.target,
            fetch_ms=ms,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Translate upstream failed: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/proxy/page", include_in_schema=True)
async def proxy_page(
    request: Request,
    url: str = Query(..., description="Целевой URL"),
) -> Response:
    try:
        html = await fetch_proxy_html(url.strip(), request_base=str(request.base_url))
        return Response(content=html, media_type="text/html; charset=utf-8")
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Upstream fetch failed: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/proxy/asset", include_in_schema=False)
async def proxy_asset(
    url: str = Query(..., description="URL картинки или ресурса"),
) -> Response:
    import base64

    target = url.strip()
    if not target.startswith(("http://", "https://")):
        raise HTTPException(status_code=400, detail="Only http/https URLs supported")
    page_url = target
    try:
        async with httpx.AsyncClient(follow_redirects=True) as client:
            data_url, _, _ = await fetch_image_data_url(
                client, target, page_url, profile=TINY_PROFILE
            )
            if data_url and "," in data_url:
                raw = base64.b64decode(data_url.split(",", 1)[1])
                return Response(content=raw, media_type="image/jpeg")
            resp = await client.get(target, timeout=settings.request_timeout_sec)
            resp.raise_for_status()
            content = resp.content[: settings.max_image_bytes]
            ctype = resp.headers.get("content-type", "application/octet-stream")
            return Response(content=content, media_type=ctype)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Asset fetch failed: {exc}") from exc


@app.get("/api/render/visual", response_model=VisualPageResponse)
async def render_visual(
    url: str = Query(..., description="Целевой URL"),
    images: str = Query(
        "tiny",
        description="normal | tiny | off | layout (макет без JPEG)",
        pattern="^(normal|tiny|off|layout)$",
    ),
) -> VisualPageResponse:
    try:
        parsed = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    cache_key = f"visual:{parsed}:{images}"
    try:
        return await response_cache.get_or_set(
            cache_key,
            lambda: build_visual_page(parsed, images_mode=images),
        )
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Upstream fetch failed: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/render/strips", response_model=StripPageResponse)
async def render_strips(
    url: str = Query(..., description="Целевой URL"),
    images: str = Query(
        "tiny",
        description="normal | tiny | off | layout",
        pattern="^(normal|tiny|off|layout)$",
    ),
    engine: str = Query(
        "auto",
        description="auto | browser (Playwright скриншот) | pillow (текст из extract)",
        pattern="^(auto|browser|pillow)$",
    ),
) -> StripPageResponse:
    try:
        parsed = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    cache_key = f"strips:{parsed}:{images}:{engine}"
    try:
        return await response_cache.get_or_set(
            cache_key,
            lambda: build_strip_page(parsed, images_mode=images, engine=engine),
        )
    except BrowserStripsError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail=f"Upstream fetch failed: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.get("/api/extract", response_model=SaylatArticle)
async def extract_get(
    request: Request,
    url: str = Query(..., description="Целевой URL"),
    images: str = Query(
        "normal",
        description="normal | tiny (~1–2 KB JPEG) | off | layout",
        pattern="^(normal|tiny|off|layout)$",
    ),
    level: str = Query(
        "medium",
        description="light | medium | full",
        pattern="^(light|medium|full)$",
    ),
) -> SaylatArticle:
    header = request.headers.get("x-saylat-level") or request.headers.get("x-saylat-compression-level")
    return await _extract_safe(url, images=images, level=level, level_header=header)


@app.post("/api/extract", response_model=SaylatArticle)
async def extract_post(body: ExtractRequest) -> SaylatArticle:
    return await _extract_safe(str(body.url), images=body.images, level=body.level)


async def _extract_safe(
    url: str,
    *,
    images: str = "normal",
    level: str = "medium",
    level_header: str | None = None,
) -> SaylatArticle:
    try:
        parsed = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    compression = parse_compression_level(level, level_header)
    images_mode = images_mode_for_level(compression, images)
    cache_key = f"extract:{parsed}:{images_mode}:{compression}"

    async def _load() -> SaylatArticle:
        opened = await try_open_site(parsed, images_mode=images_mode)
        if opened is not None:
            if opened.kind == "article" and opened.article is not None:
                article = opened.article
            elif opened.kind == "feed" and opened.feed is not None:
                article = feed_to_article(parsed, opened.feed)
            else:
                article = await extract_article(parsed, images_mode=images_mode)
        else:
            article = await extract_article(parsed, images_mode=images_mode)
        return apply_compression_level(article, compression)

    try:
        return await response_cache.get_or_set(cache_key, _load)
    except httpx.HTTPError as exc:
        try:
            plain = await extract_plain_fallback(parsed)
            return apply_compression_level(plain, compression)
        except Exception:
            raise HTTPException(status_code=502, detail=f"Upstream fetch failed: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc
