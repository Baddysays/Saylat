# Deploy Saylat proxy to YOUR VPS (личный сервер — хост не в репозитории)
param(
    [string]$ServerHost = "",
    [string]$User = "root",
    [string]$RemoteDir = "/opt/saylat"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$EnvFile = Join-Path $Root "saylat.deploy.env"
if (-not $ServerHost -and (Test-Path $EnvFile)) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*SAYLAT_DEPLOY_HOST\s*=\s*(.+)\s*$') { $ServerHost = $Matches[1].Trim() }
        if ($_ -match '^\s*SAYLAT_DEPLOY_USER\s*=\s*(.+)\s*$') { $User = $Matches[1].Trim() }
        if ($_ -match '^\s*SAYLAT_DEPLOY_DIR\s*=\s*(.+)\s*$') { $RemoteDir = $Matches[1].Trim() }
    }
}
if (-not $ServerHost) {
    Write-Host "Укажите хост: .\scripts\deploy-saylat-vps.ps1 -ServerHost 'ваш.ip'"
    Write-Host "Или создайте saylat.deploy.env из saylat.deploy.env.example (не коммитить)."
    exit 1
}

$Archive = Join-Path $env:TEMP "saylat-deploy.tgz"
$GradleFile = Join-Path $Root "android\app\build.gradle.kts"

Push-Location $Root
try {
    if (Test-Path $Archive) { Remove-Item $Archive -Force }

    $versionCode = 0
    $versionName = "0.0.0"
    $releaseNotes = "Обновление Saylat."
    if (Test-Path $GradleFile) {
        $gradleText = Get-Content $GradleFile -Raw
        if ($gradleText -match 'versionCode\s*=\s*(\d+)') { $versionCode = [int]$Matches[1] }
        if ($gradleText -match 'versionName\s*=\s*"([^"]+)"') { $versionName = $Matches[1] }
    }
    $UpdateJson = Join-Path $Root "releases\update.json"
    if (Test-Path $UpdateJson) {
        try {
            $updateRaw = [System.IO.File]::ReadAllText($UpdateJson, [System.Text.Encoding]::UTF8)
            $metaJson = $updateRaw | ConvertFrom-Json
            if ($metaJson.release_notes) { $releaseNotes = [string]$metaJson.release_notes }
        } catch {
            Write-Host "WARN: could not read releases/update.json for release_notes"
        }
    }

    Write-Host "Сборка APK (clean assembleDebug)..."
    Push-Location (Join-Path $Root "android")
    try {
        .\gradlew.bat clean assembleDebug --no-daemon
    } finally {
        Pop-Location
    }
    $debugApk = Join-Path $Root "android\app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $debugApk) {
        $releasesDir = Join-Path $Root "server\releases"
        New-Item -ItemType Directory -Force -Path $releasesDir | Out-Null
        Copy-Item $debugApk (Join-Path $releasesDir "saylat.apk") -Force
        $stamp = (Get-Date).ToUniversalTime().ToString("o")
        [System.IO.File]::WriteAllText((Join-Path $releasesDir "deploy-stamp.txt"), $stamp)
        if (-not $releaseNotes -or $releaseNotes -eq "Обновление Saylat.") {
            $releaseNotes = "0.5.41 Saylatik: koshelek KB, magazin shlyap, myach, kachalka, 24x24 animacii."
        }
        $meta = @{
            version_code = $versionCode
            version_name = $versionName
            release_notes = $releaseNotes
        } | ConvertTo-Json -Compress
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText((Join-Path $releasesDir "apk-meta.json"), $meta, $utf8NoBom)
        $apkSize = (Get-Item (Join-Path $releasesDir "saylat.apk")).Length
        Write-Host "APK + apk-meta.json ($versionName / $versionCode) - $apkSize bytes"
    } else {
        Write-Host "ERROR: APK not found after build"
        exit 1
    }

    tar -czf $Archive --exclude="server/.venv" --exclude="**/__pycache__" docker-compose.yml server
    ssh "${User}@${ServerHost}" "mkdir -p $RemoteDir"
    scp $Archive "${User}@${ServerHost}:${RemoteDir}/saylat-deploy.tgz"
    $remoteCmd = "set -e; cd $RemoteDir; tar xzf saylat-deploy.tgz; docker compose build --no-cache; docker compose up -d; sleep 3; curl -fsS http://127.0.0.1:8787/health"
    ssh "${User}@${ServerHost}" $remoteCmd
    Write-Host ""
    Write-Host "Личный Saylat: http://${ServerHost}:8787/"
    Write-Host "APK (только для вас): http://${ServerHost}:8787/app/download/saylat.apk"
    Write-Host "Firewall: close port 8787 for strangers - see docs/LICHNYI-SERVER.md"
} finally {
    Pop-Location
}
