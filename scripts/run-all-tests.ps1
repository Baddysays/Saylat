# Full thin-browser checks: Android build + server pytest + optional VPS smoke
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent

Write-Host "=== Android assembleDebug ===" -ForegroundColor Cyan
Push-Location (Join-Path $Root "android")
try {
    .\gradlew.bat assembleDebug --quiet
    if ($LASTEXITCODE -ne 0) { throw "Gradle failed" }
    Write-Host "OK: APK built" -ForegroundColor Green
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== Server pytest ===" -ForegroundColor Cyan
Push-Location (Join-Path $Root "server")
try {
    $py = ".venv\Scripts\python.exe"
    if (-not (Test-Path $py)) {
        Write-Host "WARN: server .venv missing, skip pytest" -ForegroundColor Yellow
    } else {
        & $py -m pytest tests\ -q --tb=short
        if ($LASTEXITCODE -ne 0) { throw "pytest failed" }
        Write-Host "OK: pytest passed" -ForegroundColor Green
    }
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "=== VPS smoke ===" -ForegroundColor Cyan
$vps = "http://157.22.202.235:8787"
try {
    $h = curl.exe -fsS --connect-timeout 8 "$vps/health" 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "health: $h"
        curl.exe -fsS --connect-timeout 8 "$vps/api/bench/lite" -o NUL 2>$null
        Write-Host "OK: VPS reachable" -ForegroundColor Green
    }
} catch {
    Write-Host "WARN: VPS check skipped" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "All local checks finished." -ForegroundColor Green
