<p align="center">
  <img src="docs/assets/saylat-logo.png" width="160" alt="Saylat logo" />
</p>

# Saylat

**Легче салата** — мобильный браузер для медленных сетей (2G/EDGE).  
Страница сжимается на **прокси-сервере**, на телефоне — нативный UI и локальный раскладчик.

*by **baddysays*** · `com.baddysays.saylat`

## Личный сервер (не публичный сервис)

| Шаг | Действие |
|-----|----------|
| VPS | `curl -fsSL .../install-saylat-server.sh \| bash` на **вашем** сервере |
| Телефон | `http://ВАШ_IP:8787/` → APK → при первом запуске ввести **тот же** адрес |

Подробно: **[docs/DLYA-POLZOVATELYA.md](docs/DLYA-POLZOVATELYA.md)** · файрвол: **[docs/LICHNYI-SERVER.md](docs/LICHNYI-SERVER.md)**.  
IP **не** хранится в открытом репозитории — только у вас в `local.properties` / `saylat.deploy.env`.

## Документация

| Документ | Содержание |
|----------|------------|
| [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) | **Системные требования** — минимальные и рекомендуемые |
| [docs/STRUCTURE.md](docs/STRUCTURE.md) | Структура репозитория и Git |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Как вносить изменения |
| [shared/article.schema.json](shared/article.schema.json) | JSON-контракт API |
| [docs/SMART_LAYOUT.md](docs/SMART_LAYOUT.md) | Умная вёрстка (прототип) |
| [docs/COMPRESSION_LEVELS.md](docs/COMPRESSION_LEVELS.md) | Light / Medium / Full |

## Архитектура

```
[Android Saylat]  --JSON-->  [Saylat Proxy]  --HTML-->  [Сайт]
        |                         |
        +-- SmartLayoutCoordinator +-- readability, bleach, JPEG
            (эвристика → опция ИИ)
```

В настройках: **Умная вёрстка** — второй проход по меткам блоков (прототип; позже Gemma).

## Быстрый старт

### Сервер

```powershell
cd server
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
python run.py
```

- Health: http://127.0.0.1:8787/health  
- Extract: http://127.0.0.1:8787/api/extract?url=https://example.com  
- Search: http://127.0.0.1:8787/api/search?q=saylat  
- Веб-морда: http://127.0.0.1:8787/  
- Swagger: http://127.0.0.1:8787/docs  

Переменные: префикс `SAYLAT_` — см. [server/.env.example](server/.env.example).

### Android

1. Откройте папку `android/` в Android Studio.
2. Соберите **Run** → эмулятор или устройство.
3. URL прокси: `http://10.0.2.2:8787` (эмулятор) или `http://<IP-ПК>:8787` (телефон).

## Требования (кратко)

| | Минимум | Рекомендуется |
|--|---------|---------------|
| Android | 8.0 (API 26) | 10+ |
| Python (сервер) | 3.11 | 3.12 |
| RAM сервера | 512 МБ | 2 ГБ |
| Сеть | GPRS/EDGE | Wi‑Fi / LAN к прокси |

Полная таблица: **[docs/REQUIREMENTS.md](docs/REQUIREMENTS.md)**.

## Структура репозитория

```
android/   # клиент
server/    # прокси FastAPI
shared/    # схема SaylatArticle
docs/      # требования и структура
```

## Умная вёрстка

1. Сразу показывается быстрый план (эвристика).
2. Если опция включена и RAM ≥ 3.5 ГБ — через ~0.7 с лента обновляется (прототип «ИИ»).

## Лицензия

[MIT](LICENSE) · Copyright (c) 2026 baddysays
