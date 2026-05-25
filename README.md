<p align="center">
  <img src="docs/assets/saylat-logo.png" width="160" alt="Saylat logo" />
</p>

# 🥗 Saylat — браузер и хаб для слабых сетей (2G/EDGE)

[![GitHub release](https://img.shields.io/github/v/release/Baddysays/Saylat)](https://github.com/Baddysays/Saylat/releases)
[![CI](https://github.com/Baddysays/Saylat/actions/workflows/ci.yml/badge.svg)](https://github.com/Baddysays/Saylat/actions/workflows/ci.yml)
[![Release APK](https://github.com/Baddysays/Saylat/actions/workflows/release-apk.yml/badge.svg)](https://github.com/Baddysays/Saylat/actions/workflows/release-apk.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Легче салата** — страницы сжимаются на **вашем прокси**, на телефоне нативный UI без тяжёлого WebView.

*by **baddysays*** · `com.baddysays.saylat`

## 🚀 Возможности

- 📡 **Три уровня сжатия** — Light / Medium / Full под скорость сети
- 🌐 **Тонкий браузер** — JSON-статьи, полосы Playwright, умная вёрстка
- 💬 **Telegram, VK, IMAP** — ленты и ответы через ваш VPS (не из APK)
- 📦 **Офлайн-кэш** — недавние страницы и полосы на устройстве
- 🔄 **Обновления APK** — только с [GitHub Releases](https://github.com/Baddysays/Saylat/releases) (`saylat.apk`)

## Личный сервер (не публичный сервис)

| Шаг | Действие |
|-----|----------|
| VPS | `curl -fsSL .../install-saylat-server.sh \| bash` на **вашем** сервере |
| Телефон | Скачать APK → при первом запуске ввести `http://ВАШ_IP:8787` |

Подробно: **[docs/DLYA-POLZOVATELYA.md](docs/DLYA-POLZOVATELYA.md)** · файрвол: **[docs/LICHNYI-SERVER.md](docs/LICHNYI-SERVER.md)** · wiki: **[docs/WIKI-SERVER.md](docs/WIKI-SERVER.md)**.

IP **не** хранится в открытом репозитории — только у вас в `local.properties` / `.env`.

## 📸 Скриншоты

Добавьте свои скриншоты в `docs/assets/` (главный экран, сравнение с Chrome на 2G, лента мессенджеров).

## 🔧 Быстрый старт

### Для пользователей

**[Скачать APK (последний релиз)](https://github.com/Baddysays/Saylat/releases/latest)** — файл `saylat.apk`.

### Для разработчиков

```bash
git clone https://github.com/Baddysays/Saylat.git
cd Saylat/server
python -m venv .venv && source .venv/bin/activate  # Windows: .\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
cp .env.example .env
python run.py
```

Docker:

```bash
docker compose up -d --build
```

Android: откройте `android/` в Android Studio, `local.properties` — см. `android/local.properties.example`.

## Документация

| Документ | Содержание |
|----------|------------|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Что готово и что в планах |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Системные требования |
| [docs/COMPRESSION_LEVELS.md](docs/COMPRESSION_LEVELS.md) | Light / Medium / Full |
| [docs/WIKI-TELEGRAM-VK.md](docs/WIKI-TELEGRAM-VK.md) | Подключение мессенджеров |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Pull request и стиль коммитов |
| [shared/article.schema.json](shared/article.schema.json) | JSON-контракт API |

## Архитектура

```
[Android Saylat]  --JSON-->  [Ваш Saylat Proxy]  --HTML-->  [Сайты]
        |                              |
        +-- SmartLayout + кэш          +-- extract / feed / act
```

## 🤝 Как помочь

- ⭐ Звезда репозиторию
- 🐛 [Issues](https://github.com/Baddysays/Saylat/issues) — баги и идеи
- 🌍 Переводы и PR — см. [CONTRIBUTING.md](CONTRIBUTING.md)
- 💬 Включите **Discussions** в настройках GitHub для вопросов пользователей

## 📜 Лицензия

[MIT](LICENSE) · Copyright (c) 2026 baddysays
