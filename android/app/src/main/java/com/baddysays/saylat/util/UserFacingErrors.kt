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
            raw.length > 140 -> raw.take(140) + "…"
            else -> raw.ifBlank { "Ошибка соединения" }
        }
    }
}
