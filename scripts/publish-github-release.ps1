# Сборка release APK, обновление releases/update.json, GitHub Release с asset saylat.apk
param(
    [string]$Tag = "",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$GradleFile = Join-Path $Root "android\app\build.gradle.kts"
$ReleaseApk = Join-Path $Root "android\app\build\outputs\apk\release\app-release.apk"
$UpdateJson = Join-Path $Root "releases\update.json"

if (-not $Tag) {
    if (Test-Path $GradleFile) {
        $t = Get-Content $GradleFile -Raw
        if ($t -match 'versionName\s*=\s*"([^"]+)"') { $Tag = "v$($Matches[1])" }
    }
    if (-not $Tag) { $Tag = "v0.0.0" }
}

Push-Location $Root
try {
    if (-not $SkipBuild) {
        Push-Location (Join-Path $Root "android")
        .\gradlew.bat assembleRelease --no-daemon
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed" }
        Pop-Location
    }
    if (-not (Test-Path $ReleaseApk)) {
        throw "APK not found: $ReleaseApk"
    }

    $versionCode = 0
    $versionName = $Tag.TrimStart("v")
    if (Test-Path $GradleFile) {
        $gradleText = Get-Content $GradleFile -Raw
        if ($gradleText -match 'versionCode\s*=\s*(\d+)') { $versionCode = [int]$Matches[1] }
        if ($gradleText -match 'versionName\s*=\s*"([^"]+)"') { $versionName = $Matches[1] }
    }

    $apkUrl = "https://github.com/Baddysays/Saylat/releases/download/$Tag/saylat.apk"
    $notes = "Saylat $versionName — см. CHANGELOG в репозитории."
    $meta = @{
        version_code = $versionCode
        version_name = $versionName
        apk_url = $apkUrl
        release_notes = "Обновление с GitHub. Прокси Saylat — на вашем сервере."
        mandatory = $false
    } | ConvertTo-Json -Compress
    New-Item -ItemType Directory -Force -Path (Split-Path $UpdateJson) | Out-Null
    Set-Content -Path $UpdateJson -Value $meta -Encoding UTF8

    Write-Host "Tag: $Tag  ($versionName / $versionCode)"
    Write-Host "update.json -> $apkUrl"

    $existing = gh release view $Tag 2>$null
    if ($LASTEXITCODE -eq 0) {
        gh release upload $Tag $ReleaseApk --clobber
        Write-Host "Uploaded saylat.apk to existing release $Tag"
    } else {
        gh release create $Tag $ReleaseApk --title "Saylat $versionName" --notes $notes
        Write-Host "Created release $Tag"
    }
    Write-Host ""
    Write-Host "APK: $apkUrl"
    Write-Host "Meta: https://raw.githubusercontent.com/Baddysays/Saylat/main/releases/update.json"
} finally {
    Pop-Location
}
