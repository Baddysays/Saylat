# Copy canonical Saylat logo into Android res and ComfyUI input
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$src = Join-Path $root "docs\assets\saylat-logo.png"
if (-not (Test-Path $src)) { throw "Missing $src" }

python -c @"
from pathlib import Path
from PIL import Image
root = Path(r'$root')
img = Image.open(root / 'docs/assets/saylat-logo.png').convert('RGBA')
for name, size in [('saylat_logo', 512), ('saylat_launcher_fg', 1024)]:
    p = root / 'android/app/src/main/res/drawable-nodpi' / f'{name}.png'
    p.parent.mkdir(parents=True, exist_ok=True)
    img.resize((size, size), Image.Resampling.LANCZOS).save(p, 'PNG')
for folder, sz in {'mipmap-mdpi':48,'mipmap-hdpi':72,'mipmap-xhdpi':96,'mipmap-xxhdpi':144,'mipmap-xxxhdpi':192}.items():
    p = root / 'android/app/src/main/res' / folder / 'ic_launcher.png'
    p.parent.mkdir(parents=True, exist_ok=True)
    img.resize((sz, sz), Image.Resampling.LANCZOS).save(p, 'PNG')
print('OK')
"@

Copy-Item $src "D:\AI\ComfyUI\input\saylat-reference.png" -Force -ErrorAction SilentlyContinue
Write-Host "Synced from $src"
