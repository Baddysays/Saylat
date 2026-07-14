# Saylat — Proxy Server

FastAPI-прокси на порту **8787**: вытягивает страницы и ленты, сжимает payload для Android-клиента.

## Эндпоинты

| Путь | Описание |
|------|----------|
| `GET /api/extract` | Статья → JSON / gzip-b64 wire |
| `GET /api/extract/binary` | saylat-binary / zstd / gzip |
| `GET /api/extract/delta` | ETag + бинарная дельта (JSON envelope) |
| `GET /api/extract/progressive` | SSE progressive (2G) |
| `GET /api/extract/sprite` | Статья со спрайт-листом картинок |
| `GET /api/tts` · `POST /api/tts` | Edge TTS |
| `GET|POST /api/podcast` | Подкаст (до 20 статей/URL) |
| `GET /api/feed` · `POST /api/open` · `POST /api/act` | Ленты и действия |
| `GET /api/search` | Поиск (SearXNG + fallback) |
| `GET /api/traffic/stats` | Экономия трафика |
| `GET /health` | Статус |

## Установка

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
python run.py
```

Linux/macOS: `source .venv/bin/activate`, `cp .env.example .env`.

## Linux: системные пакеты для lxml

```bash
# Debian/Ubuntu
sudo apt install libxml2-dev libxslt1-dev zlib1g-dev

# Fedora
sudo dnf install libxml2-devel libxslt-devel zlib-devel
```

## Разработка

```powershell
pip install -r requirements-dev.txt
python -m pytest tests -q
```

## Docker (production)

```bash
# из корня репозитория
docker compose up -d --build
```

- Веб-морда: http://&lt;host&gt;:8787/
- Swagger: http://&lt;host&gt;:8787/docs
- Health: http://&lt;host&gt;:8787/health

Опционально: `REDIS_URL` для персистентного кэша ответов.

Деплой на **ваш** VPS:

```powershell
.\scripts\deploy-saylat-vps.ps1 -ServerHost "ваш.ip"
```

или `saylat.deploy.env` из `saylat.deploy.env.example` (не коммитить). Скрипт собирает APK, заливает `server/` + compose и пересобирает контейнер. APK также доступен с VPS: `http://HOST:8787/app/download/saylat.apk`.

Однострочник: [`scripts/install-saylat-server.sh`](../scripts/install-saylat-server.sh).

## Безопасность

- `SAYLAT_API_KEY` + rate limit
- `validate_public_http_url` против SSRF (страницы и image sub-fetch)
- Порт 8787 — только для своих IP ([docs/LICHNYI-SERVER.md](../docs/LICHNYI-SERVER.md))

## Требования

См. [docs/REQUIREMENTS.md](../docs/REQUIREMENTS.md) § 2 и § 3.2.
