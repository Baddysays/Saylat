package com.baddysays.saylat.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.readLaterStore: DataStore<Preferences>
    by preferencesDataStore(name = "saylat_read_later")

private val Context.historyStore: DataStore<Preferences>
    by preferencesDataStore(name = "saylat_history")

// ─────────────────────────────────────────────────────────────
// Модели
// ─────────────────────────────────────────────────────────────

@Serializable
data class ReadLaterItem(
    val url: String,
    val title: String,
    val excerpt: String = "",
    val savedAt: Long = System.currentTimeMillis(),
    val estimatedMinutes: Int? = null,
)

@Serializable
data class HistoryEntry(
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis(),
)

private val json = Json { ignoreUnknownKeys = true }

// ─────────────────────────────────────────────────────────────
// Read Later
// ─────────────────────────────────────────────────────────────

class ReadLaterRepository(private val context: Context) {

    private val KEY = stringPreferencesKey("items")
    private val MAX_ITEMS = 200

    val items: Flow<List<ReadLaterItem>> = context.readLaterStore.data.map { prefs ->
        prefs[KEY]?.let { raw ->
            runCatching { json.decodeFromString<List<ReadLaterItem>>(raw) }.getOrElse { emptyList() }
        } ?: emptyList()
    }

    suspend fun add(item: ReadLaterItem) {
        context.readLaterStore.edit { prefs ->
            val current = prefs[KEY]
                ?.let { runCatching { json.decodeFromString<List<ReadLaterItem>>(it) }.getOrElse { emptyList() } }
                ?: emptyList()
            // Избегаем дублей по URL
            val filtered = current.filter { it.url != item.url }
            val updated = listOf(item) + filtered
            prefs[KEY] = json.encodeToString(updated.take(MAX_ITEMS))
        }
    }

    suspend fun remove(url: String) {
        context.readLaterStore.edit { prefs ->
            val current = prefs[KEY]
                ?.let { runCatching { json.decodeFromString<List<ReadLaterItem>>(it) }.getOrElse { emptyList() } }
                ?: emptyList()
            prefs[KEY] = json.encodeToString(current.filter { it.url != url })
        }
    }

    suspend fun contains(url: String): Boolean {
        var found = false
        context.readLaterStore.edit { prefs ->
            val current = prefs[KEY]
                ?.let { runCatching { json.decodeFromString<List<ReadLaterItem>>(it) }.getOrElse { emptyList() } }
                ?: emptyList()
            found = current.any { it.url == url }
        }
        return found
    }

    suspend fun clear() {
        context.readLaterStore.edit { it.remove(KEY) }
    }
}

// ─────────────────────────────────────────────────────────────
// История браузера
// ─────────────────────────────────────────────────────────────

class BrowsingHistory(private val context: Context) {

    private val KEY = stringPreferencesKey("entries")
    private val MAX_ENTRIES = 50

    val entries: Flow<List<HistoryEntry>> = context.historyStore.data.map { prefs ->
        prefs[KEY]?.let { raw ->
            runCatching { json.decodeFromString<List<HistoryEntry>>(raw) }.getOrElse { emptyList() }
        } ?: emptyList()
    }

    /** Добавить запись. Дубли по URL обновляют время и поднимаются наверх. */
    suspend fun record(url: String, title: String) {
        if (url.isBlank() || url == "about:blank") return
        context.historyStore.edit { prefs ->
            val current = prefs[KEY]
                ?.let { runCatching { json.decodeFromString<List<HistoryEntry>>(it) }.getOrElse { emptyList() } }
                ?: emptyList()
            val deduped = current.filter { it.url != url }
            val updated = listOf(HistoryEntry(url = url, title = title.take(100))) + deduped
            prefs[KEY] = json.encodeToString(updated.take(MAX_ENTRIES))
        }
    }

    suspend fun remove(url: String) {
        context.historyStore.edit { prefs ->
            val current = prefs[KEY]
                ?.let { runCatching { json.decodeFromString<List<HistoryEntry>>(it) }.getOrElse { emptyList() } }
                ?: emptyList()
            prefs[KEY] = json.encodeToString(current.filter { it.url != url })
        }
    }

    suspend fun clear() {
        context.historyStore.edit { it.remove(KEY) }
    }

    /** Поиск по title и url — для autocomplete строки URL. */
    fun search(query: String, all: List<HistoryEntry>): List<HistoryEntry> {
        if (query.isBlank()) return all.take(8)
        val q = query.lowercase()
        return all.filter {
            it.url.lowercase().contains(q) || it.title.lowercase().contains(q)
        }.take(8)
    }
}

fun filterHistorySuggestions(query: String, all: List<HistoryEntry>): List<HistoryEntry> {
    if (query.isBlank()) return emptyList()
    val q = query.lowercase()
    return all.filter {
        it.url.lowercase().contains(q) || it.title.lowercase().contains(q)
    }.take(8)
}
