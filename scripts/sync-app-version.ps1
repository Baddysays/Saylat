# Синхронизация версии приложения: Gradle -> update.json (поля версии), docker-compose, config.py
# Текст release_notes в update.json правьте вручную перед релизом.
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$GradleFile = Join-Path $Root "android\app\build.gradle.kts"
$UpdateJson = Join-Path $Root "releases\update.json"
$ComposeFile = Join-Path $Root "docker-compose.yml"
$ConfigFile = Join-Path $Root "server\app\config.py"

if (-not (Test-Path $GradleFile)) { throw "Not found: $GradleFile" }
$gradleText = Get-Content $GradleFile -Raw
if ($gradleText -notmatch 'versionCode\s*=\s*(\d+)') { throw "versionCode not found in build.gradle.kts" }
if ($gradleText -notmatch 'versionName\s*=\s*"([^"]+)"') { throw "versionName not found in build.gradle.kts" }
$versionCode = [int]$Matches[1]
$gradleText -match 'versionName\s*=\s*"([^"]+)"' | Out-Null
$versionName = $Matches[1]

$releaseNotes = "Saylat $versionName"
$apkUrl = "https://github.com/Baddysays/Saylat/releases/download/v$versionName/saylat.apk"
if (Test-Path $UpdateJson) {
    $data = Get-Content $UpdateJson -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($data.release_notes) { $releaseNotes = [string]$data.release_notes }
    $data.version_code = $versionCode
    $data.version_name = $versionName
    $data.apk_url = $apkUrl
    if ($null -eq $data.mandatory) { $data | Add-Member -NotePropertyName mandatory -NotePropertyValue $false }
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($UpdateJson, ($data | ConvertTo-Json -Depth 5) + "`n", $utf8NoBom)
}

$composeNotes = ($releaseNotes -split "`n")[0]
if ($composeNotes.Length -gt 200) { $composeNotes = $composeNotes.Substring(0, 197) + "..." }
$compose = Get-Content $ComposeFile -Raw
$compose = $compose -replace 'SAYLAT_APP_VERSION_CODE:\s*"[^"]*"', "SAYLAT_APP_VERSION_CODE: `"$versionCode`""
$compose = $compose -replace 'SAYLAT_APP_VERSION_NAME:\s*"[^"]*"', "SAYLAT_APP_VERSION_NAME: `"$versionName`""
$compose = $compose -replace 'SAYLAT_APP_RELEASE_NOTES:\s*"[^"]*"', "SAYLAT_APP_RELEASE_NOTES: `"$($composeNotes -replace '"','\"')`""
[System.IO.File]::WriteAllText($ComposeFile, $compose, (New-Object System.Text.UTF8Encoding($false)))

$config = Get-Content $ConfigFile -Raw -Encoding UTF8
$config = $config -replace 'app_version_code:\s*int\s*=\s*\d+', "app_version_code: int = $versionCode"
$config = $config -replace 'app_version_name:\s*str\s*=\s*"[^"]*"', "app_version_name: str = `"$versionName`""
[System.IO.File]::WriteAllText($ConfigFile, $config, (New-Object System.Text.UTF8Encoding($false)))
Write-Host "  (app_release_notes в config.py — вручную, как в update.json)"

Write-Host "Synced $versionName ($versionCode)"
Write-Host "  releases/update.json"
Write-Host "  docker-compose.yml"
Write-Host "  server/app/config.py"
