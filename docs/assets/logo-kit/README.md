# Saylat — комплект логотипов (ComfyUI)

Сгенерировано через ComfyUI (Juggernaut-XL v9), 1024×1024.  
**PNG с прозрачным фоном:** папка [`transparent/`](transparent/) (вырезка белого фона скриптом `scripts/logo-remove-bg.py`).  
**Эталон для всего проекта:** `../saylat-logo.png` (лаунчер, UI, README).

## Файлы

| Файл | Назначение | Seed | Комментарий |
|------|------------|------|-------------|
| `saylat-symbol-1024.png` | Знак (символ) | 42001 | Листья в круге, без текста — черновик |
| `saylat-badge-1024.png` | Бейдж (README, соцсети) | 42002 | **Лучший из набора** — миска в круге на cream |
| `saylat-app-icon-1024.png` | Иконка приложения | 42003 | Круг + лист — для референса, не вставлять как есть |
| `saylat-lockup-1024.png` | Логотип с текстом | 42004 | Текст испорчен («FSA LAAT») — **не использовать** |
| `saylat-badge-v2-1024.png` | Бейдж v2 | 42016 | Повторная генерация (если есть файл) |

## Палитра (эталон)

- Cream: `#F3EAD7` / `#F2EAD8`
- Teal: `#2B8F84`
- Gold: `#E7B64A`

## Как повторить в ComfyUI

Модель: `Juggernaut-XL_v9.safetensors`, sampler `euler`, 32–36 steps, CFG 7.5–8.

Промпт (бейдж):

```
minimal flat vector logo badge, large cream beige circle #F3EAD7 background,
salad bowl with lettuce leaves outline centered inside circle, teal green #2B8F84 strokes,
gold seed dots, generous padding, no text, app store badge style, 2D clean
```

Негатив: `photorealistic, 3d, gradient, shadow, text, watermark, cluttered, blurry`

## Android Studio

1. Лучший PNG → **правый клик `res/drawable` → New → Image Asset** (только как референс).
2. Синхронизация в Android: скопировать `saylat-logo.png` в `drawable-nodpi/saylat_logo.png` или запустить скрипт из `scripts/sync-saylat-logo.ps1`.

Исходники ComfyUI: `D:\AI\ComfyUI\output\ComfyUI_00015_.png` … `00018_.png`.
