# Требуется: gh auth login (права admin на репозитории)
$ErrorActionPreference = "Stop"
gh api -X PATCH repos/Baddysays/Saylat -f has_discussions=true
Write-Host "https://github.com/Baddysays/Saylat/discussions"
