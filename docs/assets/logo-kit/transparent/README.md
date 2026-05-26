# Saylat — PNG с прозрачным фоном

ComfyUI рисует на **белом** `#FFFFFF`, альфа-канал добавлен скриптом `scripts/logo-remove-bg.py`.

## Файлы (готовые, прозрачный фон)

| Файл | Seed | Режим вырезки |
|------|------|----------------|
| `saylat-symbol-a-42101.png` | 42101 | весь белый |
| `saylat-symbol-b-42102.png` | 42102 | весь белый |
| `saylat-symbol-c-42103.png` | 42103 | весь белый |
| `saylat-symbol-d-42105.png` | 42105 | весь белый |
| `saylat-badge-42104.png` | 42104 | только углы (круг cream сохраняется) |
| `saylat-badge-1024-transparent.png` | 42002 | углы (из прошлого прогона) |
| `saylat-symbol-1024-transparent.png` | 42001 | весь белый |

`*-raw.png` — исходник с белым фоном (до вырезки).

## Повторить вырезку

```bash
# знак — убрать весь белый
python scripts/logo-remove-bg.py --mode white -o docs/assets/logo-kit/transparent/out.png input-raw.png

# бейдж — убрать только фон снаружи круга
python scripts/logo-remove-bg.py --mode flood -o docs/assets/logo-kit/transparent/badge.png input-raw.png
```

Для Android и README по-прежнему лучше **SVG**: `../saylat-mark.svg`, `../saylat-logo.svg`.
