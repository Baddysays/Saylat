package com.baddysays.saylat.util

object UserFacingErrors {
    fun from(throwable: Throwable): String {
        val raw = throwable.message.orEmpty()
        return when {
            raw.contains("timeout", ignoreCase = true) ||
                raw.contains("12000", ignoreCase = true) ||
                raw.contains("failed to connect", ignoreCase = true) ||
                raw.contains("Unable to resolve host", ignoreCase = true) ->
                "Сервер не отвечает. На 2G подождите дольше или проверьте URL прокси."
            raw.contains("401", ignoreCase = true) ||
                raw.contains("X-API-Key", ignoreCase = true) ||
                raw.contains("API-Key", ignoreCase = true) ->
                "Нужен API-ключ прокси. Задайте saylat.api.key в local.properties или отключите SAYLAT_API_KEY на VPS."
            raw.contains("429", ignoreCase = true) ||
                raw.contains("Слишком много запросов", ignoreCase = true) ->
                "Слишком много запросов к серверу. Подождите минуту."
            raw.length > 140 -> raw.take(140) + "…"
            else -> raw.ifBlank { "Ошибка соединения" }
        }
    }
}
