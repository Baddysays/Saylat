# Дорожная карта Saylat

Статусы: ✅ готово · 🟡 частично · ⬜ запланировано

## Клиент (Android)

| Задача | Статус |
|--------|--------|
| Рендер JSON (LazyColumn, карточки, light/medium/full) | ✅ |
| Переход по ссылкам без WebView (прокси `/api/open`, extract) | ✅ |
| Telegram / VK / почта через сервер | 🟡 (VK ответы — позже) |
| Офлайн-кэш (`PageCache`, DataStore) | ✅ (не Room) |
| Ошибки и таймауты на 2G | ✅ |
| FCM push на 2G | ⬜ |
| Режим SOS (&lt;10 кбит/с) | ⬜ |
| Bluetooth / Wi‑Fi Direct P2P | ⬜ |

## Сервер (FastAPI)

| Задача | Статус |
|--------|--------|
| Extract + visual + strips + сжатие light/medium/full | ✅ |
| In-memory кэш ответов | ✅ |
| SSRF-защита URL | ✅ |
| API-ключ + rate limit | ✅ |
| Fallback plain-текст при сбое upstream | ✅ |
| `/api/feed` — объединённая лента | ✅ |
| `/api/act` — отправка (TG, почта) | 🟡 |
| Redis-кэш | ⬜ (переменная зарезервирована) |
| Docker / docker-compose | ✅ |

## GitHub и продукт

| Задача | Статус |
|--------|--------|
| README с бейджами и быстрым стартом | ✅ |
| Автосборка APK (`release-apk.yml`, `saylat.apk`) | ✅ |
| OTA только с GitHub | ✅ |
| CONTRIBUTING | ✅ |
| Wiki-статьи в `docs/` | ✅ |
| Скриншоты в README | ⬜ (добавьте в `docs/assets/`) |
| GitHub Discussions / Projects | ⬜ (включите в настройках репо) |

## Идеи «вау»

- SOS-режим, P2P, голос↔текст, видео→GIF — в бэклоге, см. Issues.
