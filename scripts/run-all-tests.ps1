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
$vps = $env:SAYLAT_TEST_URL
if (-not $vps -and (Test-Path (Join-Path $Root "saylat.deploy.env"))) {
    $line = Get-Content (Join-Path $Root "saylat.deploy.env") | Where-Object { $_ -match 'SAYLAT_DEPLOY_HOST' } | Select-Object -First 1
    if ($line -match '=\s*(.+)') { $vps = "http://$($Matches[1].Trim()):8787" }
}
if ($vps) {
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
} else {
    Write-Host "SKIP: задайте SAYLAT_TEST_URL или saylat.deploy.env" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "All local checks finished." -ForegroundColor Green
