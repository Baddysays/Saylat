# Saylat — сборка debug APK из командной строки
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$Android = Join-Path $Root "android"
$Sdk = "$env:LOCALAPPDATA\Android\Sdk"

if (-not (Test-Path $Sdk)) {
    Write-Host "SDK не найден: $Sdk"
    Write-Host "Запустите Android Studio один раз (Setup Wizard) или установите commandline-tools."
    exit 1
}

$sdkDir = $Sdk -replace '\\', '/'
"sdk.dir=$sdkDir" | Out-File -Encoding ascii (Join-Path $Android "local.properties")

$env:ANDROID_HOME = $Sdk
$env:ANDROID_SDK_ROOT = $Sdk

Push-Location $Android
try {
    if (-not (Test-Path ".\gradlew.bat")) {
        Write-Host "gradlew.bat отсутствует. Откройте android/ в Android Studio для генерации wrapper."
        exit 1
    }
    Write-Host "Сборка assembleDebug..."
    .\gradlew.bat assembleDebug --no-daemon
    $apk = Get-ChildItem -Recurse "app\build\outputs\apk\debug\*.apk" | Select-Object -First 1
    if ($apk) {
        Write-Host "OK: $($apk.FullName)"
        $releases = Join-Path (Split-Path $PSScriptRoot -Parent) "server\releases"
        New-Item -ItemType Directory -Force -Path $releases | Out-Null
        Copy-Item $apk.FullName (Join-Path $releases "saylat.apk") -Force
        Write-Host "Published: server/releases/saylat.apk (for OTA updates)"
    }
} finally {
    Pop-Location
}
