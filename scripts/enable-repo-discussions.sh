#!/usr/bin/env bash
# Включить Discussions для Baddysays/Saylat (нужен gh auth login с правами admin).
set -euo pipefail
gh api -X PATCH repos/Baddysays/Saylat -f has_discussions=true
echo "https://github.com/Baddysays/Saylat/discussions"
