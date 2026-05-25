#!/usr/bin/env bash
# Saylat — установка прокси-сервера одной командой (Linux + Docker).
# Использование:
#   curl -fsSL https://raw.githubusercontent.com/Baddysays/Saylat/main/scripts/install-saylat-server.sh | bash
#   curl -fsSL .../install-saylat-server.sh | bash -s -- http://ВАШ_IP:8787
set -euo pipefail

PUBLIC_BASE="${1:-}"
INSTALL_DIR="${SAYLAT_INSTALL_DIR:-/opt/saylat}"
REPO="${SAYLAT_REPO:-https://github.com/Baddysays/Saylat.git}"
BRANCH="${SAYLAT_BRANCH:-main}"

echo "==> Saylat: проверка Docker…"
if ! command -v docker >/dev/null 2>&1; then
  echo "Установите Docker: https://docs.docker.com/engine/install/"
  exit 1
fi
if ! docker compose version >/dev/null 2>&1 && ! docker-compose version >/dev/null 2>&1; then
  echo "Нужен Docker Compose v2 (плагин docker compose)."
  exit 1
fi

COMPOSE="docker compose"
if ! docker compose version >/dev/null 2>&1; then
  COMPOSE="docker-compose"
fi

WORKDIR="$(mktemp -d)"
trap 'rm -rf "$WORKDIR"' EXIT

echo "==> Клонируем Saylat ($BRANCH)…"
git clone --depth 1 --branch "$BRANCH" "$REPO" "$WORKDIR/repo"

cd "$WORKDIR/repo"

if [ -z "$PUBLIC_BASE" ]; then
  if command -v curl >/dev/null 2>&1; then
    IP="$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || true)"
  fi
  if [ -z "${IP:-}" ]; then
    IP="$(hostname -I 2>/dev/null | awk '{print $1}' || echo "127.0.0.1")"
  fi
  PUBLIC_BASE="http://${IP}:8787"
fi
PUBLIC_BASE="${PUBLIC_BASE%/}"

echo "==> Устанавливаем в $INSTALL_DIR …"
sudo mkdir -p "$INSTALL_DIR"
sudo cp -a docker-compose.yml "$INSTALL_DIR/"
sudo cp -a server "$INSTALL_DIR/"
sudo mkdir -p "$INSTALL_DIR/server/releases" "$INSTALL_DIR/server/data"

if [ -f "$WORKDIR/repo/android/app/build/outputs/apk/release/app-release.apk" ]; then
  sudo cp "$WORKDIR/repo/android/app/build/outputs/apk/release/app-release.apk" \
    "$INSTALL_DIR/server/releases/saylat.apk" || true
fi

cd "$INSTALL_DIR"
echo "==> Сборка и запуск контейнера (первый раз 5–15 минут)…"
sudo $COMPOSE up -d --build

echo ""
echo "Готово."
echo "  Сайт и APK:  $PUBLIC_BASE/"
echo "  Скачать APK: $PUBLIC_BASE/app/download/saylat.apk"
echo "  Health:      $PUBLIC_BASE/health"
echo ""
echo "На телефоне: установите APK и введите адрес $PUBLIC_BASE при первом запуске."
echo ""
echo "Личный сервер: закройте порт 8787 для всего интернета, если не хотите публичный доступ:"
echo "  sudo ufw allow from ВАШ_IP to any port 8787 proto tcp"
echo "  sudo ufw enable"
