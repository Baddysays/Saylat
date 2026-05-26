# 🖥️ Как поднять свой сервер Saylat

Saylat — **личный прокси**, не публичный сервис. Один VPS на пользователя — свой маленький «ускоритель» для 2G.

## Быстро (скрипт)

```bash
curl -fsSL https://raw.githubusercontent.com/Baddysays/Saylat/main/scripts/install-saylat-server.sh | bash
```

Откройте порт **8787** только для своих IP (файрвол). Подробнее: [LICHNYI-SERVER.md](LICHNYI-SERVER.md).

## Docker Compose

```bash
git clone https://github.com/Baddysays/Saylat.git
cd Saylat
cp server/.env.example server/.env
# отредактируйте server/.env
docker compose up -d --build
```

Проверка: `curl http://127.0.0.1:8787/health`

## Переменные

Префикс `SAYLAT_`. Главные:

| Переменная | Назначение |
|------------|------------|
| `SAYLAT_API_KEY` | Заголовок `X-API-Key` в приложении (опционально) |
| `SAYLAT_RATE_LIMIT_PER_MINUTE` | Лимит запросов с одного IP |
| `SAYLAT_TELEGRAM_*` | Telethon на VPS |
| `SAYLAT_VK_ACCESS_TOKEN` | Лента VK |
| `SAYLAT_MAIL_*` | IMAP/SMTP |

## API для клиента

- `GET /api/extract?url=...&level=light|medium|full`
- `GET /api/feed` — Telegram + VK + почта
- `POST /api/open` — открыть URL, чат, письмо
- `POST /api/act` — ответ в Telegram / почту

Обновление APK — **только GitHub Releases**, не с вашего VPS.
