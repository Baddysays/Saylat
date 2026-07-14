# История изменений Saylat

Формат: `versionName` (`versionCode`). OTA: [releases/update.json](../releases/update.json).

## 0.5.58 (67) — текущая

- **saylat-binary** + `/api/extract/delta` (ETag, бинарная дельта, стабильный JSON wire)
- **Progressive SSE** на 2G без буферизации traffic-middleware
- Серверный **TTS** (`/api/tts`) и **подкаст** (лимит 20 URL)
- Счётчик трафика через `X-Saylat-Savings` (без двойного учёта)
- SSRF/лимиты: sprite/ascii image fetch, Redis invalidate по префиксам
- Клиент: wire-cache по url+images+level, UI TTS для server player

## 0.5.57 (66)

- **2G / EDGE:** длинные таймауты OkHttp, 4 повтора, автоопределение медленного cellular
- Заголовок `X-Saylat-Slow-Network` — сервер увеличивает таймаут загрузки страниц до 90 с
- **Кодировки:** декодирование HTML cp1251 / koi8-r / meta charset; JSON API с `charset=utf-8`
- Питомец **Saylat** (пиксельный ёжик), речь на FAB при загрузке, без дублей реплик
- Wire-сжатие статей gzip/zstd, `ArticleWireEnvelope`, прокси картинок в ECO
- Тесты: `http_text`, payload codec, pet bridge, 67 pytest на сервере

## 0.5.56 (65)

- Единое имя питомца Saylat, пауза между репликами короче
- Удалён legacy-спрайт салатика, рост ёжика по стадии

## 0.5.55 (64)

- Пиксельный ёжик 48×48, 45 эмоций, 300+ реплик, реакции на сайты
- Интеграция tamagotchi v2: browser bridge, XP, site reactions

## 0.5.40–0.5.54

- Тамагочи при долгой загрузке, магазин, салатики за экономию трафика
- Умная вёрстка, STRIPS/Playwright, ленты TG/VK/почта, офлайн-кэш
- Уровни сжатия Light/Medium/Full, rate limit, CORS, пагинация feed

## 0.5.40 (49)

- Тамагочи при загрузке >10 с: уход, салатики, чип «Готово»
- Переключатель в настройках

## Ранее

См. [GitHub Releases](https://github.com/Baddysays/Saylat/releases).
