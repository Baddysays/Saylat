package com.baddysays.saylat.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ConnectStatus(
    val telegram: Boolean = false,
    val mail: Boolean = false,
    val vk: Boolean = false,
    val dzen: Boolean = false,
    val telegram_hint: String = "",
    val mail_hint: String = "",
)

@JsonClass(generateAdapter = true)
data class TelegramCodeRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class TelegramSignInRequest(val phone: String, val code: String)

@JsonClass(generateAdapter = true)
data class MessageResponse(val message: String = "")

@JsonClass(generateAdapter = true)
data class ServiceCredentialsPublic(
    val telegram_api_id: Int = 0,
    val telegram_api_hash_set: Boolean = false,
    val mail_imap_host: String = "",
    val mail_imap_port: Int = 993,
    val mail_smtp_host: String = "",
    val mail_smtp_port: Int = 587,
    val mail_username: String = "",
    val mail_password_set: Boolean = false,
    val mail_use_ssl: Boolean = true,
    val vk_access_token_set: Boolean = false,
    val dzen_session_cookie_set: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ServiceCredentialsUpdate(
    val telegram_api_id: Int? = null,
    val telegram_api_hash: String? = null,
    val mail_imap_host: String? = null,
    val mail_imap_port: Int? = null,
    val mail_smtp_host: String? = null,
    val mail_smtp_port: Int? = null,
    val mail_username: String? = null,
    val mail_password: String? = null,
    val mail_use_ssl: Boolean? = null,
    val vk_access_token: String? = null,
    val dzen_session_cookie: String? = null,
)
