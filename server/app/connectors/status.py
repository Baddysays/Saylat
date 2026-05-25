from ..credentials_store import (
    dzen_is_configured,
    effective_mail_username,
    mail_is_configured,
    telegram_has_api_keys,
    telegram_has_session,
    vk_is_configured,
)


def connection_status() -> dict[str, bool | str]:
    tg_api = telegram_has_api_keys()
    tg_session = telegram_has_session()
    return {
        "telegram": tg_api and tg_session,
        "mail": mail_is_configured(),
        "vk": vk_is_configured(),
        "dzen": dzen_is_configured(),
        "telegram_hint": _telegram_hint(tg_api, tg_session),
        "mail_hint": _mail_hint(),
    }


def _telegram_hint(has_api: bool, has_session: bool) -> str:
    if not has_api:
        return "Введите API ID и hash в настройках приложения"
    if not has_session:
        return "Сохраните ключи и войдите по коду (Wi‑Fi)"
    return "Подключено"


def _mail_hint() -> str:
    if mail_is_configured():
        return effective_mail_username()
    return "Введите IMAP в настройках приложения"
