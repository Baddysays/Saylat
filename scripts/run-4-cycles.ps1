# 4 цикла: pytest + E2E + сборка APK
param([string]$ServerDir = "$PSScriptRoot\..\server")
param([string]$AndroidDir = "$PSScriptRoot\..\android")

$ErrorActionPreference = "Stop"
$venv = Join-Path $ServerDir ".venv\Scripts\python.exe"

for ($i = 1; $i -le 4; $i++) {
    Write-Host "`n========== CYCLE $i ==========" -ForegroundColor Cyan
    Push-Location $ServerDir
    try {
        & $venv -m pytest tests\ -q --tb=line
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        & $venv scripts\e2e_page_modes.py | Out-File -FilePath "data\e2e_cycle_$i.txt" -Encoding utf8
    } finally {
        Pop-Location
    }
}

Push-Location $AndroidDir
try {
    .\gradlew.bat assembleDebug --quiet
} finally {
    Pop-Location
}
Write-Host "`nAll 4 cycles OK" -ForegroundColor Green
