# Saylat reference logo -> ComfyUI img2img + transparent PNG + 512 icon
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$out = Join-Path $root "docs\assets\logo-kit\reference"
New-Item -ItemType Directory -Force -Path $out | Out-Null

$ref = Join-Path $root "docs\assets\saylat-reference-icon.png"
Copy-Item $ref "D:\AI\ComfyUI\input\saylat-reference.png" -Force

Write-Host "ComfyUI img2img..."
python (Join-Path $PSScriptRoot "saylat-logo-img2img.py") 0.52 "43001,43002,43003,43004,43005"
python (Join-Path $PSScriptRoot "saylat-logo-img2img.py") 0.68 "43011,43012,43013"

$comfy = "D:\AI\ComfyUI\output"
Get-ChildItem $comfy -Filter "saylat_ref_*.png" | Sort-Object Name | ForEach-Object {
    $base = $_.BaseName
    $raw = Join-Path $out ($base + "-raw.png")
    Copy-Item $_.FullName $raw -Force
    $transparent = Join-Path $out ($base + ".png")
    python (Join-Path $PSScriptRoot "logo-remove-bg.py") --mode saylat -o $transparent $raw
}

Write-Host "Reference -> transparent..."
python (Join-Path $PSScriptRoot "logo-remove-bg.py") --mode saylat `
    -o (Join-Path $out "saylat-reference-transparent.png") $ref

Write-Host "Resize to 512..."
python -c @"
from pathlib import Path
from PIL import Image
out = Path(r'$out')
for p in sorted(out.glob('*.png')):
    if '-raw' in p.name or '-512' in p.name:
        continue
    img = Image.open(p).convert('RGBA')
    img.resize((512, 512), Image.Resampling.LANCZOS).save(
        out / (p.stem + '-512.png'), 'PNG')
"@

Write-Host "Done: $out"
