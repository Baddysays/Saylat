package com.baddysays.saylat.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/** ETag + сырые байты ответа /api/extract/delta для повторных запросов. */
class ArticleWireCache(private val context: Context) {
    private val Context.store by preferencesDataStore("saylat_article_wire")

    suspend fun get(url: String, images: String = "normal", level: String = "medium"): CachedWire? {
        val prefs = context.store.data.first()
        val etag = prefs[stringPreferencesKey(etagKey(url, images, level))] ?: return null
        val b64 = prefs[stringPreferencesKey(bytesKey(url, images, level))] ?: return null
        return CachedWire(etag, android.util.Base64.decode(b64, android.util.Base64.NO_WRAP))
    }

    suspend fun put(
        url: String,
        etag: String,
        bytes: ByteArray,
        images: String = "normal",
        level: String = "medium",
    ) {
        context.store.edit { prefs ->
            prefs[stringPreferencesKey(etagKey(url, images, level))] = etag
            prefs[stringPreferencesKey(bytesKey(url, images, level))] =
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    data class CachedWire(val etag: String, val bytes: ByteArray)

    private fun cacheId(url: String, images: String, level: String): String {
        // stable key — use hash of combined string to keep preference keys short
        val raw = "$url|$images|$level"
        return raw.hashCode().toString()
    }

    private fun etagKey(url: String, images: String, level: String) =
        "etag:${cacheId(url, images, level)}"

    private fun bytesKey(url: String, images: String, level: String) =
        "bytes:${cacheId(url, images, level)}"
}
