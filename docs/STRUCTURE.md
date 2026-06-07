# Структура репозитория Saylat

Актуальная версия приложения: **0.5.57** (`versionCode` 66) — см. [CHANGELOG.md](CHANGELOG.md).

```
thin-browser/                    # монорепозиторий Saylat
├── .github/
│   ├── workflows/
│   │   ├── ci.yml               # тесты сервера
│   │   └── release-apk.yml      # сборка APK по тегу v*, release notes из update.json
│   └── DISCUSSION_TEMPLATE/
├── android/                     # клиент Android (Kotlin + Jetpack Compose)
│   └── app/src/main/java/com/baddysays/saylat/
│       ├── ui/                  # BrowserScreen, Home, Feed, TamagotchiPet, Settings…
│       ├── data/                # Retrofit Api, модели
│       ├── prefs/               # DataStore (SaylatPrefs, settingsBundle)
│       ├── network/             # SaylatHttpClient, RetryInterceptor, 2G-диагностика
│       ├── tamagotchi/          # ёжик Saylat: эмоции, реплики, site reactions
│       ├── ui/pet/              # рендер, диалоги, browser bridge
│       ├── engine/              # умная вёрстка, мапперы лент
│       ├── cache/               # офлайн PageCache
│       └── update/              # OTA через GitHub / сервер
├── server/                      # прокси FastAPI (Docker)
│   ├── app/
│   │   ├── main.py              # HTTP API
│   │   ├── extract.py           # статьи, уровни сжатия
│   │   ├── unified_feed.py      # лента TG/VK/почта + пагинация
│   │   ├── security.py          # API-key, rate limit
│   │   ├── browser_strips.py    # Playwright STRIPS
│   │   ├── connectors/          # telegram, vk, mail, dzen
│   │   ├── config.py            # SAYLAT_* env, версия по умолчанию
│   │   └── update.py            # /api/app/update, /app/download/saylat.apk
│   ├── releases/                # saylat.apk + apk-meta.json (в git только meta; APK в .gitignore)
│   └── tests/
├── website/
│   ├── v3/                      # прод-лендинг → saylat.baddysays.ru
│   ├── apache/                  # vhost для aaPanel
│   └── dist/                    # сборка (gitignore)
├── releases/
│   └── update.json              # OTA для приложения (версия + release_notes + apk_url)
├── shared/                      # JSON-схемы API
├── scripts/                     # сборка, деплой VPS/сайта, GitHub release
├── docs/                        # документация
├── docker-compose.yml           # сервер на VPS :8787
└── README.md
```

## Версии: что должно совпадать

| Файл | Назначение |
|------|------------|
| `android/app/build.gradle.kts` | **Источник** `versionName` / `versionCode` для APK |
| `releases/update.json` | GitHub OTA: те же цифры + текст релиза + `apk_url` |
| `server/app/config.py` | Дефолты health/API, если нет `apk-meta.json` |
| `docker-compose.yml` | `SAYLAT_APP_VERSION_*` на VPS (должны совпадать с релизом) |
| `server/.env.example` | Пример для ручного `.env` |

После смены версии в Gradle: обновить `update.json` и `config.py`, тег `v0.5.xx` → CI соберёт APK и допишет `apk_url` в `update.json`.

## Релизы

| Канал | URL |
|-------|-----|
| GitHub | https://github.com/Baddysays/Saylat/releases/latest |
| Личный VPS | `http://ВАШ_IP:8787/app/download/saylat.apk` |

## Что не коммитить

См. [`.gitignore`](../.gitignore): `.venv/`, `build/`, `.env`, `local.properties`, `*.apk`, `saylat.deploy.env`, `website/dist/`, `dist/`.
