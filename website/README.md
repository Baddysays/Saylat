# Website Deployment (`saylat.baddysays.ru`)

В репозитории есть две версии лендинга:

- `website/v1/index.html` — темный hero-лендинг
- `website/v2/index.html` — светлый продуктовый лендинг

## Быстрый деплой на поддомен

1. Создайте DNS-запись:
   - `A  saylat.baddysays.ru  -> <IP вашего VPS>`
2. Скопируйте выбранный `index.html` на сервер (в `/var/www/saylat-site`).
3. Настройте Nginx:

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
