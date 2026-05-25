# Структура репозитория Saylat

```
saylat/                          # корень монорепозитория
├── .github/                     # шаблоны GitHub (PR, issues)
├── android/                     # клиент Android (Kotlin + Compose)
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/baddysays/saylat/
│   │       │   ├── engine/      # LayoutPlan, SmartLayoutCoordinator
│   │       │   ├── ai/          # GemmaLayoutEnhancer (заглушка)
│   │       │   └── device/      # проверка RAM
│   │       └── res/
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── server/                      # прокси FastAPI
│   ├── app/
│   │   ├── main.py              # HTTP API
│   │   ├── extract.py           # readability + блоки
│   │   ├── images.py            # сжатие JPEG
│   │   ├── models.py            # SaylatArticle
│   │   └── config.py            # SAYLAT_* env
│   ├── requirements.txt
│   ├── requirements-dev.txt
│   ├── .env.example
│   └── run.py
├── shared/
│   ├── article.schema.json      # контракт клиент ↔ сервер
│   └── layout-plan.schema.json  # план вёрстки на телефоне
├── docs/
│   ├── REQUIREMENTS.md          # системные требования
│   ├── SMART_LAYOUT.md          # прототип умной вёрстки
│   └── STRUCTURE.md             # этот файл
├── .editorconfig
├── .gitattributes
├── .gitignore
├── LICENSE
├── README.md
└── CONTRIBUTING.md
```

## Ветки и релизы (рекомендация)

| Ветка | Назначение |
|-------|------------|
| `main` | стабильная, проходящая сборку |
| `develop` | интеграция фич (опционально) |
| `feature/*` | отдельные задачи |

Теги: `v0.1.0`, `android-v0.1.0`, `server-v0.1.0` — при раздельных релизах компонентов.

## Что не коммитить

См. [`.gitignore`](../.gitignore): `.venv/`, `build/`, `.env`, `local.properties`, APK/AAB.
