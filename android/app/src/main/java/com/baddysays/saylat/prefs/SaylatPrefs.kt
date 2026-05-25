package com.baddysays.saylat.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baddysays.saylat.search.SearchEngine
import com.baddysays.saylat.ui.theme.AppThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("saylat_prefs")

class SaylatPrefs(private val context: Context) {
    private val serverKey = stringPreferencesKey("server_base_url")
    private val smartLayoutKey = booleanPreferencesKey("smart_layout_enabled")
    private val searchEngineKey = stringPreferencesKey("search_engine_id")
    private val searxInstanceKey = stringPreferencesKey("searx_instance_url")
    private val recentSearchesKey = stringPreferencesKey("recent_searches")
    private val translateTargetKey = stringPreferencesKey("translate_target_lang")
    private val appThemeKey = stringPreferencesKey("app_theme_id")
    private val slowNetworkKey = booleanPreferencesKey("slow_network_mode")
    private val liteImagesKey = booleanPreferencesKey("lite_images_enabled")
    private val pageLoadStatsKey = booleanPreferencesKey("page_load_stats_enabled")
    private val readerModeKey = stringPreferencesKey("reader_mode")
    private val dismissedBannersKey = stringPreferencesKey("dismissed_reader_banners")
    private val lastSeenVersionKey = androidx.datastore.preferences.core.intPreferencesKey("last_seen_version_code")

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[serverKey] ?: DEFAULT_EMULATOR
    }

    val smartLayoutEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[smartLayoutKey] ?: false
    }

    val searchEngineId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[searchEngineKey] ?: SearchEngine.SEARXNG.id
    }

    val searxInstanceUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[searxInstanceKey] ?: DEFAULT_SEARX_INSTANCE
    }

    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        decodeRecent(prefs[recentSearchesKey])
    }

    val translateTargetLang: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[translateTargetKey] ?: DEFAULT_TRANSLATE_TARGET
    }

    val appThemeId: Flow<AppThemeId> = context.dataStore.data.map { prefs ->
        AppThemeId.fromId(prefs[appThemeKey])
    }

    val slowNetworkMode: Flow<Boolean?> = context.dataStore.data.map { prefs ->
        prefs[slowNetworkKey]
    }

    val liteImagesEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[liteImagesKey] ?: false
    }

    val pageLoadStatsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[pageLoadStatsKey] ?: true
    }

    val readerMode: Flow<ReaderMode> = context.dataStore.data.map { prefs ->
        ReaderMode.fromId(prefs[readerModeKey])
    }

    val dismissedReaderBanners: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        decodeSet(prefs[dismissedBannersKey])
    }

    val lastSeenVersionCode: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[lastSeenVersionKey] ?: 0
    }

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { it[serverKey] = url.trim() }
    }

    suspend fun setSmartLayoutEnabled(enabled: Boolean) {
        context.dataStore.edit { it[smartLayoutKey] = enabled }
    }

    suspend fun setSearchEngine(engineId: String) {
        context.dataStore.edit { it[searchEngineKey] = engineId }
    }

    suspend fun setSearxInstance(url: String) {
        context.dataStore.edit { it[searxInstanceKey] = url.trim() }
    }

    suspend fun pushRecentSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = decodeRecent(prefs[recentSearchesKey])
            val updated = listOf(q) + current.filter { it != q }
            prefs[recentSearchesKey] = encodeRecent(updated.take(MAX_RECENT))
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(recentSearchesKey) }
    }

    suspend fun setTranslateTargetLang(code: String) {
        context.dataStore.edit { it[translateTargetKey] = code.trim().lowercase() }
    }

    suspend fun setAppTheme(theme: AppThemeId) {
        context.dataStore.edit { it[appThemeKey] = theme.id }
    }

    suspend fun setSlowNetworkMode(enabled: Boolean) {
        context.dataStore.edit { it[slowNetworkKey] = enabled }
    }

    suspend fun setLiteImagesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[liteImagesKey] = enabled }
    }

    suspend fun setPageLoadStatsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[pageLoadStatsKey] = enabled }
    }

    suspend fun setReaderMode(mode: ReaderMode) {
        context.dataStore.edit { it[readerModeKey] = mode.id }
    }

    suspend fun dismissReaderBanner(bannerId: String) {
        val id = bannerId.trim()
        if (id.isBlank()) return
        context.dataStore.edit { prefs ->
            val next = decodeSet(prefs[dismissedBannersKey]) + id
            prefs[dismissedBannersKey] = encodeSet(next)
        }
    }

    suspend fun setLastSeenVersionCode(code: Int) {
        context.dataStore.edit { it[lastSeenVersionKey] = code }
    }

    suspend fun ensureSlowNetworkDefault(context: android.content.Context) {
        context.dataStore.edit { prefs ->
            if (prefs[slowNetworkKey] == null) {
                prefs[slowNetworkKey] = com.baddysays.saylat.device.DeviceCapabilities
                    .shouldDefaultSlowNetwork(context)
            }
        }
    }

    companion object {
        const val DEFAULT_TRANSLATE_TARGET = "ru"
        const val DEFAULT_EMULATOR = "http://10.0.2.2:8787"
        const val DEFAULT_PRODUCTION = "http://157.22.202.235:8787"
        const val DEFAULT_SEARX_INSTANCE = "https://searx.tiekoetter.com"
        private const val MAX_RECENT = 8
        private const val RECENT_SEP = "\u001E"

        private fun decodeRecent(raw: String?): List<String> =
            raw?.split(RECENT_SEP)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

        private fun encodeRecent(items: List<String>): String =
            items.joinToString(RECENT_SEP)

        private fun decodeSet(raw: String?): Set<String> =
            raw?.split(RECENT_SEP)?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

        private fun encodeSet(items: Set<String>): String =
            items.joinToString(RECENT_SEP)
    }
}
