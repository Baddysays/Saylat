# Telegram и VK в Saylat

Всё идёт через **ваш VPS**: токены не зашиты в APK.

## Telegram

1. [my.telegram.org](https://my.telegram.org) → API development tools → `api_id` и `api_hash`.
2. На VPS в `server/.env`: `SAYLAT_TELEGRAM_API_ID`, `SAYLAT_TELEGRAM_API_HASH`.
3. В приложении (Wi‑Fi): Настройки → подключения → номер → код → вход.
4. Сессия сохраняется в `server/data/telegram.session`.

Лента: `POST /api/open` с `"target": "telegram"` или общая `GET /api/feed`.

## VK

1. [dev.vk.com](https://dev.vk.com) → приложение → access token (offline, wall, messages по необходимости).
2. `SAYLAT_VK_ACCESS_TOKEN` в `.env` или токен в настройках приложения (сохраняется на VPS).

Лента: `"target": "vk"` или `/api/feed`.

## Почта (IMAP)

`SAYLAT_MAIL_IMAP_HOST`, порт, логин, пароль — см. [server/.env.example](../server/.env.example).

Ответы: `POST /api/act` с `"source": "imap"` или `"telegram"`.

## Объединённая лента

`GET /api/feed?limit=12` — до 12 элементов с каждого настроенного источника.
