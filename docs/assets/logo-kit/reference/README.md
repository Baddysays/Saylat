# Saylat — варианты от вашего эталона

**Эталон:** `../../saylat-reference-icon.png` (белая миска + «Saylat» на teal-градиенте).

## Главные файлы

| Файл | Назначение |
|------|------------|
| `saylat-reference-transparent.png` | Эталон без фона (1024) |
| `saylat-reference-transparent-512.png` | Эталон 512×512 |
| `saylat-icon-512.png` | Лучший img2img (seed 43001, denoise 0.52) |
| `saylat_ref_00006_.png` … `00010_.png` | 5 прогонов, близко к исходнику |
| `saylat_ref_00011_.png` … `00013_.png` | 3 прогона denoise 0.68 (чуть другой стиль) |
| `*-raw.png` | До вырезки фона |

Прозрачность: `python scripts/logo-remove-bg.py --mode saylat`.

## Android Studio

1. `saylat-icon-512.png` → **New → Image Asset** → Launcher Icons.
2. Foreground = PNG, Background = cream `#F2EAD8` или прозрачный.
3. Текст «Saylat» в приложении — в `SaylatBrandMark.kt`, не в иконке.

## Повторить прогон

```powershell
.\scripts\run-saylat-ref-kit.ps1
```
