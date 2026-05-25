# Три уровня сжатия

Клиент запрашивает уровень через **`?level=light|medium|full`** или заголовок **`X-Saylat-Level`**.

| Уровень | Сервер отдаёт | Клиент |
|---------|---------------|--------|
| **light** | `plain_text`, `links`, без картинок (кроме important) | `LightArticleView` — текст и кликабельные ссылки |
| **medium** | Упрощённые `blocks`, до 5 JPEG tiny, без `css_hints` | Карточки по ролям (заголовок, абзац, список…) |
| **full** | Полные `blocks`, `css_hints`, ссылки | Карточки + опционально «умная вёрстка» |

## Автовыбор в Android

- **Медленная сеть** включена → `light`
- **Умная вёрстка** включена и доступна → `full`
- Иначе → `medium`

## Примеры

```http
GET /api/extract?url=https://example.com&level=light
X-Saylat-Level: medium
```

```http
POST /api/open
{"target":"url","url":"https://example.com","level":"full"}
```
