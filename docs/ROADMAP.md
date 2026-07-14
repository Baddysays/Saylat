# 🗺️ Дорожная карта Saylat

Статусы: ✅ готово · 🟡 частично · ⬜ в планах

## 📱 Android

| Задача | Статус |
|--------|--------|
| Рендер статей (LazyColumn, light/medium/full) | ✅ |
| Навигация по ссылкам через прокси | ✅ |
| Telegram / VK / почта | 🟡 (ответы VK — позже) |
| Офлайн-кэш (`PageCache`) | ✅ |
| Ошибки и таймауты на 2G | ✅ |
| Push (FCM) | ⬜ |
| Режим очень слабой сети (progressive + server TTS) | ✅ |

## 🖥️ Сервер

| Задача | Статус |
|--------|--------|
| Extract, visual, strips, сжатие | ✅ |
| Progressive / delta / saylat-binary | ✅ |
| TTS / podcast | ✅ |
| Кэш ответов | ✅ |
| Защита URL (SSRF) | 🟡 (страницы + sprite/ascii; redirect-hardening — дальше) |
| API-ключ и rate limit | ✅ |
| Fallback plain-текст | ✅ |
| `/api/feed` — общая лента | ✅ |
| `/api/act` (Telegram, почта) | 🟡 |
| Redis (`REDIS_URL`) | ✅ |
| Docker | ✅ |

## 📦 Репозиторий

| Задача | Статус |
|--------|--------|
| README, CI, релиз APK | ✅ |
| OTA с GitHub | ✅ |
| Документация | ✅ |
| Discussions | ✅ (шаблоны в `.github/`) |
| Скриншоты в README | 🟡 |

## 🔮 Дальше

SOS-режим, P2P, голос↔текст, видео→GIF — предложения в [Issues](https://github.com/Baddysays/Saylat/issues) и [Discussions](https://github.com/Baddysays/Saylat/discussions).
