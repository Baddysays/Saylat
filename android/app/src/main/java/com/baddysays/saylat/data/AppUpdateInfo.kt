package com.baddysays.saylat.data

data class AppUpdateInfo(
    val version_code: Int = 0,
    val version_name: String = "",
    val apk_url: String = "",
    val release_notes: String = "",
    val mandatory: Boolean = false,
)
