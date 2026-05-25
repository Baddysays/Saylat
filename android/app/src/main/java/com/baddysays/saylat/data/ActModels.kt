package com.baddysays.saylat.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ActRequest(
    val source: String,
    val action: String,
    val item_id: String,
    val body: String? = null,
    val context_id: String? = null,
)

@JsonClass(generateAdapter = true)
data class ActResponse(
    val ok: Boolean = true,
    val message: String = "",
)
