<p align="center">
  <img src="docs/assets/saylat-mark.svg" width="120" alt="Saylat" />
</p>

# Saylat

**Легче салата** — браузер и хаб для медленных сетей (2G/EDGE). Страницы сжимаются на **вашем прокси**, на телефоне — нативный интерфейс без тяжёлого WebView.

[![Release](https://img.shields.io/github/v/release/Baddysays/Saylat)](https://github.com/Baddysays/Saylat/releases)
[![CI](https://github.com/Baddysays/Saylat/actions/workflows/ci.yml/badge.svg)](https://github.com/Baddysays/Saylat/actions/workflows/ci.yml)
[![APK build](https://github.com/Baddysays/Saylat/actions/workflows/release-apk.yml/badge.svg)](https://github.com/Baddysays/Saylat/actions/workflows/release-apk.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

`com.baddysays.saylat` · [baddysays](https://github.com/Baddysays)

## Возможности

- Три уровня сжатия (Light / Medium / Full) под скорость сети
- Статьи и полосы страниц через прокси (JSON, Playwright)
- Telegram, VK и IMAP — ленты и ответы через ваш VPS
- Офлайн-кэш просмотренного на устройстве
- Обновления APK с [GitHub Releases](https://github.com/Baddysays/Saylat/releases) (`saylat.apk`)

## Личный сервер

Saylat не является публичным хостингом: каждый поднимает свой прокси на VPS.

| Шаг | Действие |
|-----|----------|
| Сервер | `curl -fsSL https://raw.githubusercontent.com/Baddysays/Saylat/main/scripts/install-saylat-server.sh \| bash` |
| Телефон | [Скачать APK](https://github.com/Baddysays/Saylat/releases/latest) → указать `http://ВАШ_IP:8787` при первом запуске |

Документация: [Для пользователя](docs/DLYA-POLZOVATELYA.md) · [Сервер](docs/SERVER-SETUP.md) · [Файрвол](docs/LICHNYI-SERVER.md) · [Мессенджеры](docs/MESSENGERS.md)

Публичный IP в репозиторий не коммитится — только в локальных `local.properties` / `.env`.

## Быстрый старт (разработка)

```bash
git clone https://github.com/Baddysays/Saylat.git
cd Saylat/server
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
python run.py
```

Docker: `docker compose up -d --build` из корня репозитория.

Android: каталог `android/`, пример настроек — `android/local.properties.example`.

## Документация

| Файл | Тема |
|------|------|
| [docs/ROADMAP.md](docs/ROADMAP.md) | Планы и статус |
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | Требования |
| [docs/COMPRESSION_LEVELS.md](docs/COMPRESSION_LEVELS.md) | Уровни сжатия |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Участие в разработке |
| [shared/article.schema.json](shared/article.schema.json) | Контракт API |

## Обратная связь

- [Discussions](https://github.com/Baddysays/Saylat/discussions) — вопросы и идеи
- [Issues](https://github.com/Baddysays/Saylat/issues) — ошибки
- Pull request — по [CONTRIBUTING.md](CONTRIBUTING.md)

## Лицензия

[MIT](LICENSE) · Copyright (c) 2026 baddysays
