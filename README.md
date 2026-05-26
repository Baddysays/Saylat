<p align="center">
  <img src="docs/assets/saylat-logo.png" width="160" alt="Saylat — легче салата" />
</p>

# 🥗 Saylat — браузер для слабого интернета (2G/EDGE)

[![GitHub release](https://img.shields.io/github/v/release/Baddysays/Saylat)](https://github.com/Baddysays/Saylat/releases)
[![CI](https://github.com/Baddysays/Saylat/actions/workflows/ci.yml/badge.svg)](https://github.com/Baddysays/Saylat/actions/workflows/ci.yml)
[![Release APK](https://github.com/Baddysays/Saylat/actions/workflows/release-apk.yml/badge.svg)](https://github.com/Baddysays/Saylat/actions/workflows/release-apk.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Легче салата** — сайт обрабатывается на **вашем сервере**, а телефон получает сжатый и удобный контент.

Привет! 👋 Это проект для тех, у кого интернет медленный, а телефон хочется использовать по-настоящему: сайты, Telegram, VK и почта — без лишних мегабайт.

*by **baddysays*** · ✉️ [hello@baddysays.ru](mailto:hello@baddysays.ru) · 💬 [@baddysays](https://t.me/baddysays)

## Что это

Saylat — это personal-first браузер и контент-хаб:

- сервер на вашем VPS вытягивает страницу/ленту и сжимает payload;
- Android-клиент показывает нативный экран без тяжёлого full-WebView по умолчанию;
- для медленной сети есть режимы Light / Medium / Full и STRIPS.

## 🚀 Возможности

- 📡 **Три уровня сжатия** (Light / Medium / Full) — подстраиваются под скорость сети
- 🌐 **Тонкий браузер** — текстовый рендер + режим STRIPS (скриншот-полосы)
- 💬 **Telegram, VK, почта** — ленты и ответы через VPS (токены не в APK)
- 📦 **Офлайн-кэш** — недавно прочитанное остаётся на телефоне
- 🔄 **Обновления** — APK только с [GitHub Releases](https://github.com/Baddysays/Saylat/releases) (`saylat.apk`)

## ⚡ Быстрая установка (одной командой)

```bash
curl -fsSL https://raw.githubusercontent.com/Baddysays/Saylat/main/scripts/install-saylat-server.sh | bash
```

После установки:

1. Откройте на телефоне [последний релиз APK](https://github.com/Baddysays/Saylat/releases/latest)
2. В приложении укажите сервер: `http://ВАШ_IP:8787`

## 🏠 Личный сервер (не публичный сервис)

Saylat — это **ваш** прокси на VPS или домашнем ПК, а не общий хостинг для всех.

| Шаг | Действие |
|-----|----------|
| 🖥️ Сервер | `curl -fsSL https://raw.githubusercontent.com/Baddysays/Saylat/main/scripts/install-saylat-server.sh \| bash` |
| 📱 Телефон | [Скачать APK](https://github.com/Baddysays/Saylat/releases/latest) → при первом запуске указать `http://ВАШ_IP:8787` |

📚 Подробнее: [для пользователя](docs/DLYA-POLZOVATELYA.md) · [сервер](docs/SERVER-SETUP.md) · [файрвол](docs/LICHNYI-SERVER.md) · [мессенджеры](docs/MESSENGERS.md)

Публичный IP в открытый git не кладём — только у вас в `local.properties` / `.env`.

## 📸 Как это выглядит

<p align="center">
  <img src="docs/assets/screenshots/home-speed-modes.png" width="280" alt="Главная Saylat: режимы скорости" />
  <img src="docs/assets/screenshots/settings-reading-modes.png" width="280" alt="Настройки Saylat: режимы чтения" />
  <img src="docs/assets/screenshots/settings-themes-dark.png" width="280" alt="Настройки Saylat: темы интерфейса" />
</p>

## 🔍 Обычный браузер vs Saylat

Один и тот же сайт в условиях слабой сети:

<p align="center">
  <img src="docs/assets/screenshots/browser-vs-saylat.svg" width="860" alt="Сравнение: обычный браузер и Saylat в слабой сети" />
</p>

- Слева: тяжелая страница с баннерами, скриптами и долгой загрузкой.
- Справа: Saylat-рендер, где остаются текст, структура и нужные ссылки.

## 🌍 Сайт проекта

- Планируемый домен: `https://saylat.baddysays.ru`
- Базовый лендинг уже добавлен в `website/v1` и `website/v2` (две версии)
- Деплой-инструкция: `website/README.md`

## 🔧 Быстрый старт (для разработчиков)

```bash
git clone https://github.com/Baddysays/Saylat.git
cd Saylat/server
python -m venv .venv && source .venv/bin/activate   # Windows: .\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
cp .env.example .env
python run.py
```

🐳 Docker из корня репозитория:

```bash
docker compose up -d --build
```

🤖 Android: папка `android/`, пример настроек — `android/local.properties.example`.

## 📖 Документация

| Документ | О чём |
|----------|--------|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Планы и статус |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Требования |
| [docs/COMPRESSION_LEVELS.md](docs/COMPRESSION_LEVELS.md) | Light / Medium / Full |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Как помочь проекту |
| [shared/article.schema.json](shared/article.schema.json) | Контракт API |

## 🤝 Обратная связь

Будем рады любой помощи — от звезды ⭐ до кода.

- ✉️ Почта: [hello@baddysays.ru](mailto:hello@baddysays.ru)
- 💬 Telegram: [@baddysays](https://t.me/baddysays)
- 💬 [Discussions](https://github.com/Baddysays/Saylat/discussions) — [вопрос](https://github.com/Baddysays/Saylat/discussions/new?category=general) · [идея](https://github.com/Baddysays/Saylat/discussions/new?category=ideas) · [Q&A](https://github.com/Baddysays/Saylat/discussions/new?category=q-a)
- 🐛 [Issues](https://github.com/Baddysays/Saylat/issues) — ошибки
- 🔧 Pull request — см. [CONTRIBUTING.md](CONTRIBUTING.md)

## 🧩 Как внести вклад

1. Сделайте fork репозитория
2. Создайте ветку: `feature/your-change`
3. Добавьте изменения + тесты
4. Откройте Pull Request

Подробно: [CONTRIBUTING.md](CONTRIBUTING.md)

## 📜 Лицензия

[MIT](LICENSE) · Copyright (c) 2026 baddysays
