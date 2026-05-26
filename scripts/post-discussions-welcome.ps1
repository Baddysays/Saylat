# Создать приветственное обсуждение (нужен: gh auth login с правами repo).
# Закрепление: GitHub → Discussions → ⋯ → Pin discussion.
$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$BodyFile = Join-Path $Root "docs\github\DISCUSSIONS-WELCOME.md"
if (-not (Test-Path $BodyFile)) { Write-Error "Нет файла $BodyFile" }

$raw = Get-Content $BodyFile -Raw -Encoding UTF8
$title = "Добро пожаловать в Saylat — с чего начать"
if ($raw -match '<!-- TITLE: (.+?) -->') { $title = $Matches[1].Trim() }
$categorySlug = "announcements"
if ($raw -match '<!-- CATEGORY: (.+?) -->') { $categorySlug = $Matches[1].Trim() }
$body = ($raw -replace '(?s)<!--.*?-->\s*', '').Trim()

$gh = Get-Command gh -ErrorAction SilentlyContinue
if (-not $gh) {
    Write-Host "gh не установлен. Создайте вручную:"
    Write-Host "https://github.com/Baddysays/Saylat/discussions/new?category=$categorySlug"
    Write-Host "Заголовок: $title"
    exit 0
}

$catId = gh api repos/Baddysays/Saylat/discussions/categories `
    --jq ".[] | select(.slug==`"$categorySlug`") | .id" 2>$null
if (-not $catId) {
    $catId = gh api repos/Baddysays/Saylat/discussions/categories `
        --jq '.[] | select(.slug=="general") | .id'
}
if (-not $catId) {
    Write-Host "Категории не найдены. Создайте вручную: https://github.com/Baddysays/Saylat/discussions/new"
    exit 1
}

$resp = gh api repos/Baddysays/Saylat/discussions `
    -f title="$title" -f body="$body" -f category_id="$catId"
$url = ($resp | ConvertFrom-Json).html_url
Write-Host "Создано: $url"
Write-Host "Закрепите: Discussions → ⋯ → Pin discussion"
