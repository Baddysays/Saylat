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
            if ($metaJson.version_code) { $versionCode = [int]$metaJson.version_code }
            if ($metaJson.version_name) { $versionName = [string]$metaJson.version_name }
            if ($metaJson.release_notes) { $releaseNotes = [string]$metaJson.release_notes }
        } catch {
            Write-Host "WARN: не удалось прочитать releases/update.json, оставляем notes по умолчанию"
        }
    }

    $releaseApk = Join-Path $Root "android\app\build\outputs\apk\release\app-release.apk"
    $debugApk = Join-Path $Root "android\app\build\outputs\apk\debug\app-debug.apk"
    $apk = if (Test-Path $releaseApk) { $releaseApk } elseif (Test-Path $debugApk) { $debugApk } else { $null }
    if ($apk) {
        $releasesDir = Join-Path $Root "server\releases"
        New-Item -ItemType Directory -Force -Path $releasesDir | Out-Null
        Copy-Item $apk (Join-Path $releasesDir "saylat.apk") -Force
        $meta = @{
            version_code = $versionCode
            version_name = $versionName
            release_notes = $releaseNotes
        } | ConvertTo-Json -Compress
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText((Join-Path $releasesDir "apk-meta.json"), $meta, $utf8NoBom)
        Write-Host "APK + apk-meta.json ($versionName / $versionCode)"
    } else {
        Write-Host "WARN: APK not found - run: cd android; .\gradlew.bat assembleRelease"
    }

    tar -czf $Archive --exclude="server/.venv" --exclude="**/__pycache__" docker-compose.yml server
    ssh "${User}@${ServerHost}" "mkdir -p $RemoteDir"
    scp $Archive "${User}@${ServerHost}:${RemoteDir}/saylat-deploy.tgz"
    $remoteCmd = "set -e; cd $RemoteDir; tar xzf saylat-deploy.tgz; docker compose up -d --build; sleep 3; curl -fsS http://127.0.0.1:8787/health"
    ssh "${User}@${ServerHost}" $remoteCmd
    Write-Host ""
    Write-Host "Личный Saylat: http://${ServerHost}:8787/"
    Write-Host "APK (только для вас): http://${ServerHost}:8787/app/download/saylat.apk"
    Write-Host "Закройте порт 8787 в файрволе для чужих IP — см. docs/LICHNYI-SERVER.md"
} finally {
    Pop-Location
}
