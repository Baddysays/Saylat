# Saylat emulator E2E (UIAutomator dump + adb input)
param(
    [string]$Serial = "emulator-5554",
    [string]$OutDir = "$PSScriptRoot\..\qa-emulator"
)
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { throw "adb not found at $adb" }
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Invoke-Adb([Parameter(ValueFromRemainingArguments = $true)][string[]]$A) {
    & $adb -s $Serial @A 2>&1
}
function Wait-Sec([int]$n) { Start-Sleep -Seconds $n }
function Shot([string]$name) {
    Invoke-Adb shell screencap -p "/sdcard/$name.png" | Out-Null
    Invoke-Adb pull "/sdcard/$name.png" (Join-Path $OutDir $name) | Out-Null
}
function Dump([string]$name) {
    Invoke-Adb shell uiautomator dump "/sdcard/$name.xml" | Out-Null
    Invoke-Adb pull "/sdcard/$name.xml" (Join-Path $OutDir "$name.xml") | Out-Null
    Join-Path $OutDir "$name.xml"
}
function Tap([int]$x, [int]$y) { Invoke-Adb shell input tap $x $y | Out-Null }

$pkg = "com.baddysays.saylat"
Invoke-Adb shell settings put secure show_ime_with_hardkeyboard 0 | Out-Null
Invoke-Adb shell am force-stop $pkg | Out-Null
Wait-Sec 1
Invoke-Adb shell am start -n "$pkg/.MainActivity" | Out-Null
Wait-Sec 3
$xml = Dump "qa_launch"
if ((Get-Content $xml -Raw) -match "bounds=`"\[63,2128\]") {
    Invoke-Adb shell input tap 540 2190
    Wait-Sec 2
}
Invoke-Adb shell am start -n "$pkg/.MainActivity" | Out-Null
Wait-Sec 2
Invoke-Adb shell input tap 532 2242
Wait-Sec 0.6
Invoke-Adb shell input text "example.com"
Wait-Sec 0.8
Invoke-Adb shell input keyevent 66
Wait-Sec 18
Shot "qa_reader"
$raw = Get-Content (Dump "qa_reader_ui") -Raw
$ok = $raw -match "saylat" -and ($raw -match "Example|example|Back|Loading|Ready|Collapse")
Write-Host "Reader UI ok: $ok"
if (-not $ok) { Write-Host "WARN: check qa_reader.png" }
