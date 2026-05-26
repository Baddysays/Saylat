# Сборка статического лендинга v3 для деплоя (saylat.baddysays.ru)
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$SrcHtml = Join-Path $Root "website\v3\index.html"
$Dist = Join-Path $Root "website\dist"
$AssetsSrc = Join-Path $Root "docs\assets"
$AssetsDist = Join-Path $Dist "assets"

if (-not (Test-Path $SrcHtml)) {
    Write-Error "Не найден $SrcHtml"
}

Remove-Item $Dist -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $AssetsDist | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $AssetsDist "screenshots") | Out-Null

Copy-Item $SrcHtml (Join-Path $Dist "index.html") -Force
Copy-Item (Join-Path $AssetsSrc "saylat-logo.png") $AssetsDist -Force
Copy-Item (Join-Path $AssetsSrc "logo-kit\reference\saylat-icon-512.png") (Join-Path $AssetsDist "favicon.png") -Force

$shots = @(
    "v3-hero-home.png",
    "v3-reader-mode.png",
    "v3-strips-mode.png",
    "v3-search-mode.png",
    "v3-settings-light.png"
)
foreach ($name in $shots) {
    $src = Join-Path $AssetsSrc "screenshots\$name"
    if (-not (Test-Path $src)) {
        Write-Error "Нет скриншота: $src"
    }
    Copy-Item $src (Join-Path $AssetsDist "screenshots\$name") -Force
}

Write-Host "OK: $Dist"
Write-Host "  index.html + assets/ ($($shots.Count) screenshots)"
Write-Host "Локально: cd website\dist; python -m http.server 8790"
