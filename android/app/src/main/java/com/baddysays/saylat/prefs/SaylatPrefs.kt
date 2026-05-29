package com.baddysays.saylat.prefs

import android.content.Context
import android.os.Build
import com.baddysays.saylat.BuildConfig
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
    data class FavoriteLink(
        val title: String,
        val url: String,
    )

    data class VisitEntry(
        val url: String,
        val title: String,
        val visitedAt: Long,
    )

    private val serverKey = stringPreferencesKey("server_base_url")
    private val smartLayoutKey = booleanPreferencesKey("smart_layout_enabled")
    private val searchEngineKey = stringPreferencesKey("search_engine_id")
    private val searxInstanceKey = stringPreferencesKey("searx_instance_url")
    private val recentSearchesKey = stringPreferencesKey("recent_searches")
    private val favoritesKey = stringPreferencesKey("favorite_links")
    private val translateTargetKey = stringPreferencesKey("translate_target_lang")
    private val appThemeKey = stringPreferencesKey("app_theme_id")
    private val slowNetworkKey = booleanPreferencesKey("slow_network_mode")
    private val liteImagesKey = booleanPreferencesKey("lite_images_enabled")
    private val pageLoadStatsKey = booleanPreferencesKey("page_load_stats_enabled")
    private val readerModeKey = stringPreferencesKey("reader_mode")
    private val dismissedBannersKey = stringPreferencesKey("dismissed_reader_banners")
    private val lastSeenVersionKey = androidx.datastore.preferences.core.intPreferencesKey("last_seen_version_code")
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_done")
    private val customServerKey = booleanPreferencesKey("custom_server_enabled")
    private val visitHistoryKey = stringPreferencesKey("visit_history")

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[serverKey] ?: defaultProxyUrl()
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

    val favoriteLinks: Flow<List<FavoriteLink>> = context.dataStore.data.map { prefs ->
        decodeFavorites(prefs[favoritesKey])
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

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingDoneKey] ?: false
    }

    val customServerEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[customServerKey] ?: false
    }

    val visitHistory: Flow<List<VisitEntry>> = context.dataStore.data.map { prefs ->
        decodeVisits(prefs[visitHistoryKey])
    }

    /** Атомарный снимок настроек — без гонок между отдельными collect. */
    data class SettingsBundle(
        val slowNetworkMode: Boolean?,
        val liteImagesEnabled: Boolean,
        val serverUrl: String,
        val favoriteLinks: List<FavoriteLink>,
        val smartLayoutEnabled: Boolean,
        val searchEngine: SearchEngine,
        val searxInstanceUrl: String,
        val recentSearches: List<String>,
        val translateTargetLang: String,
        val appTheme: AppThemeId,
        val pageLoadStatsEnabled: Boolean,
        val readerMode: ReaderMode,
        val dismissedReaderBanners: Set<String>,
        val customServerEnabled: Boolean,
        val visitHistory: List<VisitEntry>,
    )

    val settingsBundle: Flow<SettingsBundle> = context.dataStore.data.map { prefs ->
        SettingsBundle(
            slowNetworkMode = prefs[slowNetworkKey],
            liteImagesEnabled = prefs[liteImagesKey] ?: false,
            serverUrl = prefs[serverKey] ?: defaultProxyUrl(),
            favoriteLinks = decodeFavorites(prefs[favoritesKey]),
            smartLayoutEnabled = prefs[smartLayoutKey] ?: false,
            searchEngine = SearchEngine.fromId(prefs[searchEngineKey] ?: SearchEngine.SEARXNG.id),
            searxInstanceUrl = prefs[searxInstanceKey] ?: DEFAULT_SEARX_INSTANCE,
            recentSearches = decodeRecent(prefs[recentSearchesKey]),
            translateTargetLang = prefs[translateTargetKey] ?: DEFAULT_TRANSLATE_TARGET,
            appTheme = AppThemeId.fromId(prefs[appThemeKey]),
            pageLoadStatsEnabled = prefs[pageLoadStatsKey] ?: true,
            readerMode = ReaderMode.fromId(prefs[readerModeKey]),
            dismissedReaderBanners = decodeSet(prefs[dismissedBannersKey]),
            customServerEnabled = prefs[customServerKey] ?: false,
            visitHistory = decodeVisits(prefs[visitHistoryKey]),
        )
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

    suspend fun recordVisit(url: String, title: String) {
        val normalized = url.trim()
        if (!normalized.startsWith("http")) return
        val label = title.trim().ifBlank { normalized }
        context.dataStore.edit { prefs ->
            val current = decodeVisits(prefs[visitHistoryKey])
            val updated = listOf(VisitEntry(normalized, label, System.currentTimeMillis())) +
                current.filter { it.url != normalized }
            prefs[visitHistoryKey] = encodeVisits(updated.take(MAX_VISITS))
        }
    }

    suspend fun clearVisitHistory() {
        context.dataStore.edit { it.remove(visitHistoryKey) }
    }

    suspend fun upsertFavorite(link: FavoriteLink) {
        val normalized = link.url.trim()
        if (normalized.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = decodeFavorites(prefs[favoritesKey])
            val next = listOf(link.copy(url = normalized)) + current.filter { it.url != normalized }
            prefs[favoritesKey] = encodeFavorites(next.take(MAX_FAVORITES))
        }
    }

    suspend fun removeFavorite(url: String) {
        val normalized = url.trim()
        if (normalized.isBlank()) return
        context.dataStore.edit { prefs ->
            val next = decodeFavorites(prefs[favoritesKey]).filterNot { it.url == normalized }
            if (next.isEmpty()) prefs.remove(favoritesKey)
            else prefs[favoritesKey] = encodeFavorites(next)
        }
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

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[onboardingDoneKey] = true }
    }

    suspend fun setCustomServerEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[customServerKey] = enabled
            if (!enabled && !isEmulatorDevice()) {
                val baked = publicServerUrl()
                if (baked.isNotEmpty()) prefs[serverKey] = baked
            }
        }
    }

    suspend fun ensureConsumerReady() {
        migrateProxyUrlFromEmulator()
        val baked = publicServerUrl()
        if (baked.isNotEmpty() && !isEmulatorDevice()) {
            context.dataStore.edit { prefs ->
                val custom = prefs[customServerKey] ?: false
                if (!custom) prefs[serverKey] = baked
            }
        }
    }

    /** Убираем адрес эмулятора на реальном телефоне — пользователь введёт свой. */
    suspend fun migrateProxyUrlFromEmulator() {
        if (Companion.isEmulatorDeviceStatic()) return
        context.dataStore.edit { prefs ->
            val current = prefs[serverKey]?.trim().orEmpty()
            if (current == DEFAULT_EMULATOR) {
                prefs.remove(serverKey)
            }
        }
    }

    private fun isEmulatorDevice(): Boolean = Companion.isEmulatorDeviceStatic()

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
        /** Пусто в открытой сборке — адрес задаётся при установке сервера или в local.properties. */
        fun publicServerUrl(): String = BuildConfig.PUBLIC_SERVER_URL.trim()

        fun defaultProxyUrl(): String =
            if (isEmulatorDeviceStatic()) DEFAULT_EMULATOR
            else publicServerUrl()

        fun needsServerSetup(storedUrl: String?): Boolean {
            val u = storedUrl?.trim().orEmpty()
            if (u.isEmpty()) return true
            if (!isEmulatorDeviceStatic() && u == DEFAULT_EMULATOR) return true
            return false
        }

        private fun isEmulatorDeviceStatic(): Boolean =
            Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                Build.MODEL.contains("sdk_gphone", ignoreCase = true) ||
                Build.MODEL.contains("Emulator", ignoreCase = true)
        const val DEFAULT_SEARX_INSTANCE = "https://searx.tiekoetter.com"
        private const val MAX_RECENT = 8
        private const val MAX_FAVORITES = 10
        private const val MAX_VISITS = 50
        private const val RECENT_SEP = "\u001E"
        private const val FIELD_SEP = "\u001F"

        private fun decodeRecent(raw: String?): List<String> =
            raw?.split(RECENT_SEP)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

        private fun encodeRecent(items: List<String>): String =
            items.joinToString(RECENT_SEP)

        private fun decodeFavorites(raw: String?): List<FavoriteLink> =
            raw?.split(RECENT_SEP)
                ?.mapNotNull { row ->
                    val parts = row.split(FIELD_SEP)
                    val title = parts.getOrNull(0)?.trim().orEmpty()
                    val url = parts.getOrNull(1)?.trim().orEmpty()
                    if (url.isBlank()) null else FavoriteLink(title = title.ifBlank { url }, url = url)
                }
                ?: emptyList()

        private fun encodeFavorites(items: List<FavoriteLink>): String =
            items.joinToString(RECENT_SEP) { item ->
                "${item.title.trim().replace(FIELD_SEP, " ")}$FIELD_SEP${item.url.trim()}"
            }

        private fun decodeSet(raw: String?): Set<String> =
            raw?.split(RECENT_SEP)?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

        private fun encodeSet(items: Set<String>): String =
            items.joinToString(RECENT_SEP)

        private fun decodeVisits(raw: String?): List<VisitEntry> =
            raw?.split(RECENT_SEP)
                ?.mapNotNull { row ->
                    val parts = row.split(FIELD_SEP)
                    val url = parts.getOrNull(0)?.trim().orEmpty()
                    if (url.isBlank()) return@mapNotNull null
                    val title = parts.getOrNull(1)?.trim().orEmpty().ifBlank { url }
                    val at = parts.getOrNull(2)?.toLongOrNull() ?: 0L
                    VisitEntry(url = url, title = title, visitedAt = at)
                }
                ?: emptyList()

        private fun encodeVisits(items: List<VisitEntry>): String =
            items.joinToString(RECENT_SEP) { entry ->
                "${entry.url}$FIELD_SEP${entry.title.replace(FIELD_SEP, " ")}$FIELD_SEP${entry.visitedAt}"
            }
    }
}
