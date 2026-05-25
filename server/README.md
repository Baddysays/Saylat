# Saylat — Proxy Server

FastAPI-сервис:

- `GET /api/extract?url=` → JSON `SaylatArticle`
- `GET /api/search?q=` → JSON `SearchResponse` (прокси SearXNG и fallback-движки)
- `GET /health` — статус и URL инстанса SearX

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
```

## Docker (production)

```bash
# из корня репозитория
docker compose up -d --build
```

- Веб-морда (тест API): http://&lt;host&gt;:8787/
- Swagger: http://&lt;host&gt;:8787/docs
- Health: http://&lt;host&gt;:8787/health

Деплой на VPS: `.\scripts\deploy-saylat-vps.ps1` (по умолчанию `157.22.202.235`, каталог `/opt/saylat`).

## Требования

См. [docs/REQUIREMENTS.md](../docs/REQUIREMENTS.md) § 2 и § 3.2.
