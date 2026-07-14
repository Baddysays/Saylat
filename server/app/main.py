from contextlib import asynccontextmanager
from pathlib import Path

import httpx
from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, Response, StreamingResponse
from fastapi.staticfiles import StaticFiles

from .config import settings
from .extract import extract_article, extract_plain_fallback
from .security import ApiKeyMiddleware, RateLimitMiddleware
from .traffic_middleware import SaylatTrafficMiddleware
from .traffic_stats import TrafficStatsResponse, configure as configure_traffic_stats, get_traffic_stats
from .progressive import progressive_extract, progressive_streaming_response
from .tts_service import (
    TtsInfoResponse,
    TtsRequest,
    article_to_speech,
    prepare_text,
    resolve_voice,
    text_to_speech,
    tts_info,
)
from .delta_codec import maybe_delta_response
from .sprite_sheet import apply_sprite_to_article, extract_with_sprites
from .podcast import (
    PodcastInfoResponse,
    PodcastRequest,
    generate_podcast,
    podcast_from_urls,
    podcast_info,
    podcast_streaming_response,
)
from .ascii_art import (
    AsciiBlockData,
    apply_ascii_to_article,
    image_to_block_data,
)
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
    ArticleWireEnvelope,
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
from .update import apk_file_response, get_update_info, resolve_app_version
from .fetch_policy import outbound_timeout_sec
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
    from .http_client import close_shared_http_client, init_shared_http_client

    await init_shared_http_client()
    try:
        yield
    finally:
        from .browser_strips import shutdown_browser

        await shutdown_browser()
        await close_shared_http_client()


app = FastAPI(
    title="Saylat Proxy",
    description="Saylat — сжатие и вырезка страниц для медленных сетей",
    version="0.1.0",
    lifespan=_app_lifespan,
)

configure_traffic_stats(max_records=settings.traffic_max_records)

_cors_origins = settings.cors_origin_list()
if _cors_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=_cors_origins,
        allow_methods=["GET", "POST", "PUT"],
        allow_headers=["*"],
    )
app.add_middleware(SaylatTrafficMiddleware)
app.add_middleware(RateLimitMiddleware, limit_per_minute=settings.rate_limit_per_minute)
app.add_middleware(ApiKeyMiddleware)


@app.middleware("http")
async def _utf8_json_charset(request: Request, call_next):
    response = await call_next(request)
    content_type = response.headers.get("content-type", "")
    if content_type.startswith("application/json") and "charset=" not in content_type.lower():
        response.headers["content-type"] = "application/json; charset=utf-8"
    return response

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
    version_code, version_name, _ = resolve_app_version()
    return HealthResponse(
        searx_instance=settings.searx_instance,
        app_version_code=version_code,
        app_version_name=version_name,
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
async def thin_open(request: Request, body: OpenRequest) -> OpenResponse:
    from .payload_codec import CODEC_GZIP_B64, maybe_wire_compress_open, parse_payload_codec

    try:
        response = await open_resource(body, timeout_sec=outbound_timeout_sec(request))
        from .payload_codec import CODEC_GZIP_BINARY, CODEC_ZSTD_BINARY

        codec = parse_payload_codec(request.headers.get("x-saylat-payload-codec"))
        if codec in (CODEC_GZIP_BINARY, CODEC_ZSTD_BINARY):
            codec = CODEC_GZIP_B64
        cache_key = None
        if body.target == "url" and body.url:
            cache_key = f"open:{body.url.strip()}:{body.level}:{body.images}"
        return await maybe_wire_compress_open(response, codec, cache_key=cache_key)
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/api/open/binary")
async def thin_open_binary(request: Request, body: OpenRequest) -> Response:
    from .payload_codec import article_to_json_bytes, parse_payload_codec, prepare_binary_body

    if body.target != "url":
        raise HTTPException(
            status_code=400,
            detail="open/binary only supports target=url; use POST /api/open for feeds",
        )
    try:
        response = await open_resource(body, timeout_sec=outbound_timeout_sec(request))
        if response.kind != "article" or response.article is None:
            raise HTTPException(status_code=400, detail="Not an article response")
        cache_key = f"open:{(body.url or '').strip()}:{body.level}:{body.images}"
        codec = parse_payload_codec(request.headers.get("x-saylat-payload-codec"))
        body_bytes, extra_headers, media_type = await prepare_binary_body(
            response.article,
            cache_key,
            codec=codec,
        )
        if body_bytes is None:
            raw = article_to_json_bytes(response.article)
            return Response(
                content=raw,
                media_type="application/json",
                headers={
                    "X-Saylat-Payload-Codec": "identity",
                    "X-Saylat-Uncompressed-Bytes": str(len(raw)),
                },
            )
        return Response(
            content=body_bytes,
            media_type=media_type,
            headers=extra_headers,
        )
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

    try:
        target = validate_public_http_url(url.strip())
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    page_url = target
    try:
        from .http_client import shared_http_client

        client = shared_http_client()
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


@app.get("/api/extract", response_model=ArticleWireEnvelope)
async def extract_get(
    request: Request,
    url: str = Query(..., description="Целевой URL"),
    images: str = Query(
        "normal",
        description="normal | tiny (~1–2 KB JPEG) | off | layout",
        pattern="^(normal|tiny|off|layout|refs)$",
    ),
    level: str = Query(
        "medium",
        description="light | medium | full",
        pattern="^(light|medium|full)$",
    ),
) -> ArticleWireEnvelope:
    from .payload_codec import CODEC_GZIP_B64, parse_payload_codec, prepare_article_envelope

    header = request.headers.get("x-saylat-level") or request.headers.get("x-saylat-compression-level")
    codec = parse_payload_codec(request.headers.get("x-saylat-payload-codec")) or CODEC_GZIP_B64
    article, cache_key = await _extract_safe(
        url, images=images, level=level, level_header=header, request=request,
    )
    return await prepare_article_envelope(article, codec, cache_key)


@app.get("/api/image")
async def api_image_proxy(
    url: str = Query(..., description="URL картинки"),
    page_url: str | None = Query(None, description="URL статьи для Referer"),
    tiny: bool = Query(True, description="Мини-JPEG (~8 KB)"),
) -> Response:
    from .image_proxy import image_proxy_response

    return await image_proxy_response(url, page_url, tiny=tiny)


@app.get("/api/extract/binary")
async def extract_binary_get(
    request: Request,
    url: str = Query(...),
    images: str = Query("normal", pattern="^(normal|tiny|off|layout|refs)$"),
    level: str = Query("medium", pattern="^(light|medium|full)$"),
) -> Response:
    from .payload_codec import article_to_json_bytes, parse_payload_codec, prepare_binary_body

    header = request.headers.get("x-saylat-level") or request.headers.get("x-saylat-compression-level")
    codec = parse_payload_codec(request.headers.get("x-saylat-payload-codec"))
    article, cache_key = await _extract_safe(
        url, images=images, level=level, level_header=header, request=request,
    )
    body, extra_headers, media_type = await prepare_binary_body(article, cache_key, codec=codec)
    if body is None:
        raw = article_to_json_bytes(article)
        return Response(
            content=raw,
            media_type="application/json",
            headers={
                "X-Saylat-Payload-Codec": "identity",
                "X-Saylat-Uncompressed-Bytes": str(len(raw)),
            },
        )
    return Response(content=body, media_type=media_type, headers=extra_headers)


@app.post("/api/extract", response_model=ArticleWireEnvelope)
async def extract_post(request: Request, body: ExtractRequest) -> ArticleWireEnvelope:
    from .payload_codec import parse_payload_codec, prepare_article_envelope

    codec = parse_payload_codec(request.headers.get("x-saylat-payload-codec"))
    article, cache_key = await _extract_safe(
        str(body.url),
        images=body.images,
        level=body.level,
        request=request,
    )
    return await prepare_article_envelope(article, codec, cache_key)


async def _extract_safe(
    url: str,
    *,
    images: str = "normal",
    level: str = "medium",
    level_header: str | None = None,
    request: Request | None = None,
) -> tuple[SaylatArticle, str]:
    try:
        parsed = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    compression = parse_compression_level(level, level_header)
    images_mode = images_mode_for_level(compression, images)
    cache_key = f"extract:{parsed}:{images_mode}:{compression}"

    timeout = outbound_timeout_sec(request)

    async def _load() -> SaylatArticle:
        opened = await try_open_site(parsed, images_mode=images_mode, timeout_sec=timeout)
        if opened is not None:
            if opened.kind == "article" and opened.article is not None:
                article = opened.article
            elif opened.kind == "feed" and opened.feed is not None:
                article = feed_to_article(parsed, opened.feed)
            else:
                article = await extract_article(parsed, images_mode=images_mode, timeout_sec=timeout)
        else:
            article = await extract_article(parsed, images_mode=images_mode, timeout_sec=timeout)
        return apply_compression_level(article, compression)

    try:
        article = await response_cache.get_or_set(cache_key, _load)
        return article, cache_key
    except httpx.HTTPError as exc:
        try:
            plain = await extract_plain_fallback(parsed, timeout_sec=timeout)
            article = apply_compression_level(plain, compression)
            return article, cache_key
        except Exception:
            raise HTTPException(status_code=502, detail=f"Upstream fetch failed: {exc}") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail=str(exc)) from exc


from .main_additions import cache_invalidate, rss_discover, rss_feed_get


@app.get("/api/rss/feed")
async def api_rss_feed(url: str = Query(...)) -> dict:
    return await rss_feed_get(url=url)


@app.get("/api/rss/discover")
async def api_rss_discover(url: str = Query(...)) -> dict:
    return await rss_discover(url=url)


@app.post("/api/cache/invalidate")
async def api_cache_invalidate(url: str = Query(...)) -> dict:
    return await cache_invalidate(url=url)


@app.get("/api/tts", response_class=StreamingResponse)
async def tts_get(
    url: str = Query("", description="URL статьи для озвучки"),
    text: str = Query("", description="Прямой текст (вместо URL)"),
    voice: str = Query("ru-m", description="Пресет голоса (ru-m, en-f, …) или полное имя"),
    speed: str = Query("+0%", description="Скорость: -20%, +0%, +10%, …"),
) -> StreamingResponse:
    resolved_voice = resolve_voice(voice)

    if url.strip():
        try:
            return await article_to_speech(url.strip(), voice=resolved_voice, speed=speed)
        except HTTPException:
            raise
        except Exception as exc:
            raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера") from exc

    if text.strip():
        tts_text = prepare_text(text.strip())
        if not tts_text:
            raise HTTPException(status_code=400, detail="Текст пуст после подготовки")
        if len(tts_text) > 50_000:
            raise HTTPException(status_code=400, detail="Текст слишком длинный")

        async def _stream():
            async for chunk in text_to_speech(tts_text, voice=resolved_voice, speed=speed):
                yield chunk

        return StreamingResponse(
            _stream(),
            media_type="audio/mpeg",
            headers={
                "Content-Disposition": 'inline; filename="saylat_tts.mp3"',
                "Cache-Control": "public, max-age=600",
                "X-Saylat-Audio-Chars": str(len(tts_text)),
                "X-Saylat-Audio-Voice": resolved_voice,
            },
        )

    raise HTTPException(status_code=400, detail="Укажите url или text")


@app.post("/api/tts", response_class=StreamingResponse)
async def tts_post(body: TtsRequest) -> StreamingResponse:
    resolved_voice = resolve_voice(body.voice)

    if body.url.strip():
        try:
            return await article_to_speech(body.url.strip(), voice=resolved_voice, speed=body.speed)
        except HTTPException:
            raise
        except Exception as exc:
            raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера") from exc

    if body.text.strip():
        tts_text = prepare_text(body.text.strip())
        if not tts_text:
            raise HTTPException(status_code=400, detail="Текст пуст после подготовки")
        if len(tts_text) > 50_000:
            raise HTTPException(status_code=400, detail="Текст слишком длинный")

        async def _stream():
            async for chunk in text_to_speech(tts_text, voice=resolved_voice, speed=body.speed):
                yield chunk

        return StreamingResponse(
            _stream(),
            media_type="audio/mpeg",
            headers={
                "Content-Disposition": 'inline; filename="saylat_tts.mp3"',
                "Cache-Control": "public, max-age=600",
                "X-Saylat-Audio-Chars": str(len(tts_text)),
                "X-Saylat-Audio-Voice": resolved_voice,
            },
        )

    raise HTTPException(status_code=400, detail="Укажите url или text")


@app.get("/api/tts/info", response_model=TtsInfoResponse)
async def tts_info_endpoint() -> TtsInfoResponse:
    try:
        return await tts_info()
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера") from exc


@app.get("/api/extract/delta")
async def extract_delta(
    request: Request,
    url: str = Query(..., description="Целевой URL"),
    images: str = Query("normal", pattern="^(normal|tiny|off|layout|refs)$"),
    level: str = Query("medium", pattern="^(light|medium|full)$"),
) -> Response:
    # Delta всегда JSON envelope (gzip-b64) — тот же формат, что кэширует Android ArticleWireCache.
    from .payload_codec import CODEC_GZIP_B64, prepare_article_envelope

    header = request.headers.get("x-saylat-level") or request.headers.get("x-saylat-compression-level")
    article, cache_key = await _extract_safe(
        url, images=images, level=level, level_header=header, request=request,
    )
    envelope = await prepare_article_envelope(article, CODEC_GZIP_B64, cache_key)
    serialized = envelope.model_dump_json().encode("utf-8")
    return await maybe_delta_response(
        article, request, serialized, cache_key,
        media_type="application/json; charset=utf-8",
    )


@app.get("/api/extract/sprite", response_model=ArticleWireEnvelope)
async def extract_sprite(
    request: Request,
    url: str = Query(..., description="Целевой URL"),
    level: str = Query("medium", pattern="^(light|medium|full)$"),
    sprite_width: int = Query(360, ge=120, le=800, description="Ширина спрайта в px"),
) -> ArticleWireEnvelope:
    from .payload_codec import CODEC_GZIP_B64, parse_payload_codec, prepare_article_envelope

    try:
        parsed = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    compression = parse_compression_level(level)
    codec = parse_payload_codec(request.headers.get("x-saylat-payload-codec")) or CODEC_GZIP_B64

    try:
        article = await extract_with_sprites(parsed, images_mode="sprite", level=level)
        article = apply_compression_level(article, compression)
        cache_key = f"sprite:{parsed}:{compression}:{sprite_width}"
        return await prepare_article_envelope(article, codec, cache_key)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="Upstream fetch failed") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера") from exc


@app.get("/api/extract/ascii", response_model=ArticleWireEnvelope)
async def extract_ascii(
    request: Request,
    url: str = Query(..., description="Целевой URL"),
    level: str = Query("light", pattern="^(light|medium|full)$"),
    ascii_width: int = Query(60, ge=30, le=120, description="Ширина ASCII в символах"),
    ascii_style: str = Query("standard", pattern="^(standard|blocks|braille)$"),
) -> ArticleWireEnvelope:
    from .payload_codec import CODEC_GZIP_B64, parse_payload_codec, prepare_article_envelope

    try:
        parsed = validate_public_http_url(url)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    compression = parse_compression_level(level)
    codec = parse_payload_codec(request.headers.get("x-saylat-payload-codec")) or CODEC_GZIP_B64
    timeout = outbound_timeout_sec(request)

    try:
        article = await extract_article(parsed, images_mode="tiny", timeout_sec=timeout)
        article = apply_ascii_to_article(article, width=ascii_width, style=ascii_style)
        article = apply_compression_level(article, compression)
        cache_key = f"ascii:{parsed}:{compression}:{ascii_width}:{ascii_style}"
        return await prepare_article_envelope(article, codec, cache_key)
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="Upstream fetch failed") from exc
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера") from exc


@app.post("/api/image/ascii", response_model=AsciiBlockData)
async def image_to_ascii_endpoint(
    url: str = Query(..., description="URL картинки"),
    width: int = Query(60, ge=20, le=200),
    style: str = Query("standard", pattern="^(standard|blocks|braille)$"),
) -> AsciiBlockData:
    try:
        target = validate_public_http_url(url.strip())
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    try:
        async with httpx.AsyncClient(
            follow_redirects=False,
            timeout=settings.request_timeout_sec,
        ) as client:
            resp = await client.get(target)
            resp.raise_for_status()
            raw = resp.content[: settings.max_image_bytes]
            return image_to_block_data(raw, width=width, style=style)
    except Exception as exc:
        raise HTTPException(
            status_code=500, detail="Не удалось преобразовать изображение"
        ) from exc


@app.get("/api/extract/progressive")
async def extract_progressive(
    url: str = Query(..., description="Целевой URL"),
    images: str = Query("normal", pattern="^(normal|tiny|off|layout|refs)$"),
    level: str = Query("medium", pattern="^(light|medium|full)$"),
) -> StreamingResponse:
    return progressive_streaming_response(
        progressive_extract(url.strip(), images=images, level=level)
    )


@app.get("/api/podcast", response_class=StreamingResponse)
async def podcast_get(
    voice: str = Query("ru-m", description="Пресет голоса"),
    speed: str = Query("+0%", description="Скорость речи"),
    max_articles: int = Query(5, ge=1, le=20, description="Максимум статей"),
) -> StreamingResponse:
    resolved_voice = resolve_voice(voice)
    try:
        gen = generate_podcast(voice=resolved_voice, speed=speed, max_articles=max_articles)
        return podcast_streaming_response(gen)
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера") from exc


@app.post("/api/podcast", response_class=StreamingResponse)
async def podcast_post(body: PodcastRequest) -> StreamingResponse:
    resolved_voice = resolve_voice(body.voice)
    try:
        if body.urls:
            gen = podcast_from_urls(body.urls, voice=resolved_voice, speed=body.speed)
        else:
            gen = generate_podcast(
                voice=resolved_voice,
                speed=body.speed,
                max_articles=body.max_articles,
                sources=body.sources,
            )
        return podcast_streaming_response(gen)
    except Exception as exc:
        raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера") from exc


@app.get("/api/podcast/info", response_model=PodcastInfoResponse)
async def podcast_info_endpoint() -> PodcastInfoResponse:
    return await podcast_info()


@app.get("/api/traffic/stats", response_model=TrafficStatsResponse)
async def traffic_stats_endpoint() -> TrafficStatsResponse:
    return await get_traffic_stats()
