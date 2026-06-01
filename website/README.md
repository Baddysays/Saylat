# Website Deployment (`saylat.baddysays.ru`)

В репозитории есть две версии лендинга:

- `website/v1/index.html` — темный hero-лендинг
- `website/v2/index.html` — светлый продуктовый лендинг
- `website/v3/index.html` — dark premium (контраст, метрики, сильный hero)
- `website/v4/index.html` — clean editorial (светлый, минималистичный)

## Быстрый деплой на поддомен

**Рекомендуется v3** (лендинг + APK с [GitHub Releases latest](https://github.com/Baddysays/Saylat/releases/latest)). При смене версии приложения: `.\scripts\sync-app-version.ps1`.

Сборка и деплой:

```powershell
.\scripts\build-saylat-website.ps1
.\scripts\deploy-saylat-website.ps1 -ServerHost 157.22.202.235
```

Скрипт кладёт файлы в `/www/wwwroot/saylat.baddysays.ru`, vhost Apache — `website/apache/saylat.baddysays.ru.conf`, SSL через `acme.sh` (aaPanel).

1. DNS: `A  saylat.baddysays.ru  -> <IP VPS>`
2. Ручной вариант (Nginx):

```nginx
server {
    listen 80;
    server_name saylat.baddysays.ru;

    root /var/www/saylat-site;
    index index.html;

    location / {
        try_files $uri $uri/ =404;
    }
}
```

4. Включите HTTPS (Let's Encrypt):

```bash
sudo certbot --nginx -d saylat.baddysays.ru
```

## Примечания

- Для корректного отображения скриншотов можно:
  - либо положить `docs/assets` рядом с `index.html`,
  - либо заменить относительные пути на `https://raw.githubusercontent.com/Baddysays/Saylat/main/...`.
- Лендинг и личный сервер (`:8787`) можно держать отдельно.
