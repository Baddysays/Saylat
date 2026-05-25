# Участие в разработке Saylat

Проект: **Saylat** · **baddysays**

## Перед началом

1. Прочитайте [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md) и [docs/ROADMAP.md](docs/ROADMAP.md).
2. Установите сервер и (опционально) соберите Android — см. [README.md](README.md).

## Git-поток

```bash
git checkout -b feature/краткое-описание
# коммиты
git push -u origin feature/краткое-описание
```

Открывайте Pull Request в `main`. Один PR — одна логическая задача.

Вопросы по использованию — в [Discussions](https://github.com/Baddysays/Saylat/discussions), ошибки — в Issues.

## Стиль коммитов

```
тип: краткое описание

feat: кэш статей в Room
fix: таймаут extract на медленных сетях
docs: обновить REQUIREMENTS
android: тема Saylat
server: лимит max_images
```

Типы: `feat`, `fix`, `docs`, `android`, `server`, `chore`.

## Проверки перед PR

**Сервер:**

```powershell
cd server
.\.venv\Scripts\Activate.ps1
pip install -r requirements-dev.txt
python -c "from app.main import app; print(app.title)"
```

**Android:** сборка `assembleDebug` в Android Studio.

## Секреты

Не коммитьте `.env`, ключи, `local.properties`. Используйте `server/.env.example`.
