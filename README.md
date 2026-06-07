<p align="center">
  <img src="docs/assets/saylat-logo.png" width="160" alt="Saylat — легче салата" />
</p>

# 🥗 Saylat — браузер для слабого интернета (2G/EDGE)

[![GitHub release](https://img.shields.io/github/v/release/Baddysays/Saylat)](https://github.com/Baddysays/Saylat/releases)
[![CI](https://github.com/Baddysays/Saylat/actions/workflows/ci.yml/badge.svg)](https://github.com/Baddysays/Saylat/actions/workflows/ci.yml)
[![Release APK](https://github.com/Baddysays/Saylat/actions/workflows/release-apk.yml/badge.svg)](https://github.com/Baddysays/Saylat/actions/workflows/release-apk.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Легче салата** — сайт обрабатывается на **вашем сервере**, телефон получает сжатый и удобный контент.

Привет! 👋 Saylat для тех, у кого интернет медленный: сайты, Telegram, VK и почта — без лишних мегабайт.

*by **baddysays*** · ✉️ [hello@baddysays.ru](mailto:hello@baddysays.ru) · 💬 [@baddysays](https://t.me/baddysays)

**Текущий релиз:** [0.5.57](https://github.com/Baddysays/Saylat/releases/tag/v0.5.57) (build 66) — [что нового](docs/CHANGELOG.md)

## Что это

Saylat — personal-first браузер и контент-хаб:

- VPS вытягивает страницу или ленту и сжимает payload (gzip/zstd wire);
- Android показывает нативный экран без тяжёлого full-WebView по умолчанию;
- режимы **Light / Medium / Full**, STRIPS и умная вёрстка под слабый телефон.

## 🚀 Возможности

- 📡 **Три уровня сжатия** — Light для 2G, Full для быстрой сети
- 🌐 **Тонкий браузер** — текстовый рендер + STRIPS (полосы скриншотов)
- 🦔 **Saylat** — пиксельный ёжик-тамагочи, пока страница грузится (можно отключить)
- 💬 **Telegram, VK, почта** — ленты через VPS (токены не в APK)
- 📦 **Офлайн-кэш** — недавно прочитанное на телефоне
- 🔄 **Обновления** — APK с [GitHub Releases](https://github.com/Baddysays/Saylat/releases/latest)

## ⚡ Быстрая установка

**Сервер (VPS или домашний ПК):**

```bash
curl -fsSL https://raw.githubusercontent.com/Baddysays/Saylat/main/scripts/install-saylat-server.sh | bash
```

**Телефон:**

1. [Скачать APK](https://github.com/Baddysays/Saylat/releases/latest) (`saylat.apk`)
2. При первом запуске указать сервер: `http://ВАШ_IP:8787`
3. На 2G/EDGE оставить **«Режим 2G»** включённым в настройках

## 🏠 Личный сервер

Saylat — **ваш** прокси, а не общий хостинг.

| Шаг | Действие |
|-----|----------|
| 🖥️ Сервер | `install-saylat-server.sh` или `docker compose up -d --build` |
| 📱 Телефон | APK → URL сервера в настройках |
| 🔒 Доступ | Закройте порт 8787 для чужих — см. [LICHNYI-SERVER.md](docs/LICHNYI-SERVER.md) |

📚 [Для пользователя](docs/DLYA-POLZOVATELYA.md) · [Сервер](docs/SERVER-SETUP.md) · [Мессенджеры](docs/MESSENGERS.md)

Публичный IP в git не кладём — только в `local.properties` / `.env` на вашей машине.

## 📸 Скриншоты

<p align="center">
  <img src="docs/assets/screenshots/v3-hero-home.png" width="220" alt="Главный экран" />
  <img src="docs/assets/screenshots/v3-reader-mode.png" width="220" alt="Читалка" />
  <img src="docs/assets/screenshots/v3-strips-mode.png" width="220" alt="STRIPS" />
</p>
<p align="center">
  <img src="docs/assets/screenshots/v3-search-mode.png" width="220" alt="Поиск" />
  <img src="docs/assets/screenshots/v3-settings-light.png" width="220" alt="Настройки" />
</p>

## 🌍 Сайт

- [saylat.baddysays.ru](https://saylat.baddysays.ru) — лендинг (`website/v3`)

## 🔧 Разработка

```bash
git clone https://github.com/Baddysays/Saylat.git
cd Saylat/server
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
python run.py
```

```bash
# из корня
docker compose up -d --build
cd server && python -m pytest tests -q
cd android && ./gradlew testDebugUnitTest
```

Android: `android/local.properties.example` → `local.properties` (URL сервера опционально).

## 📖 Документация

| Документ | О чём |
|----------|--------|
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | История версий |
| [docs/STRUCTURE.md](docs/STRUCTURE.md) | Структура репозитория |
| [docs/COMPRESSION_LEVELS.md](docs/COMPRESSION_LEVELS.md) | Light / Medium / Full |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Планы |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Как помочь |
| [shared/article.schema.json](shared/article.schema.json) | Контракт API |

## 🤝 Обратная связь

- ✉️ [hello@baddysays.ru](mailto:hello@baddysays.ru)
- 💬 [@baddysays](https://t.me/baddysays)
- 💬 [Discussions](https://github.com/Baddysays/Saylat/discussions)
- 🐛 [Issues](https://github.com/Baddysays/Saylat/issues)

## 📜 Лицензия

[MIT](LICENSE) · Copyright (c) 2026 baddysays
