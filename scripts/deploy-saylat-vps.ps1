# Deploy Saylat proxy to VPS via Docker Compose
param(
    [string]$ServerHost = "157.22.202.235",
    [string]$User = "root",
    [string]$RemoteDir = "/opt/saylat"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$Archive = Join-Path $env:TEMP "saylat-deploy.tgz"

Push-Location $Root
try {
    if (Test-Path $Archive) { Remove-Item $Archive -Force }
    $releaseApk = Join-Path $Root "android\app\build\outputs\apk\release\app-release.apk"
    $debugApk = Join-Path $Root "android\app\build\outputs\apk\debug\app-debug.apk"
    $apk = if (Test-Path $releaseApk) { $releaseApk } elseif (Test-Path $debugApk) { $debugApk } else { $null }
    if ($apk) {
        Copy-Item $apk (Join-Path $Root "server\releases\saylat.apk") -Force
        Write-Host "APK copied to server/releases/saylat.apk ($([IO.Path]::GetFileName($apk)))"
    } else {
        Write-Host "WARN: APK not found - run: cd android; .\gradlew.bat assembleRelease"
    }
    tar -czf $Archive --exclude="server/.venv" --exclude="**/__pycache__" docker-compose.yml server
    ssh "${User}@${ServerHost}" "mkdir -p $RemoteDir"
    scp $Archive "${User}@${ServerHost}:${RemoteDir}/saylat-deploy.tgz"
    $remoteCmd = "set -e; cd $RemoteDir; tar xzf saylat-deploy.tgz; docker compose up -d --build; docker compose ps; curl -fsS http://127.0.0.1:8787/health"
    ssh "${User}@${ServerHost}" $remoteCmd
    Write-Host ""
    Write-Host "Saylat: http://${ServerHost}:8787/"
    Write-Host "Swagger: http://${ServerHost}:8787/docs"
} finally {
    Pop-Location
}
