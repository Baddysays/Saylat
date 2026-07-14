package com.baddysays.saylat.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baddysays.saylat.BuildConfig
import com.baddysays.saylat.data.ApiFactory
import com.baddysays.saylat.data.ReadLaterItem
import com.baddysays.saylat.data.ReadLaterRepository
import com.baddysays.saylat.data.BrowsingHistory
import com.baddysays.saylat.data.HistoryEntry
import com.baddysays.saylat.data.CompressionLevel
import com.baddysays.saylat.data.ArticleStats
import com.baddysays.saylat.data.ConnectStatus
import com.baddysays.saylat.data.OpenRequest
import com.baddysays.saylat.data.PayloadCodec
import com.baddysays.saylat.data.fetchArticle
import com.baddysays.saylat.data.fetchArticleLegacy
import com.baddysays.saylat.data.fetchOpen
import com.baddysays.saylat.network.TrafficSavingsBridge
import com.baddysays.saylat.tts.ServerTtsPlayer
import com.baddysays.saylat.data.SaylatArticle
import com.baddysays.saylat.data.SaylatFeed
import com.baddysays.saylat.data.ServiceCredentialsUpdate
import com.baddysays.saylat.data.TelegramCodeRequest
import com.baddysays.saylat.data.TelegramSignInRequest
import com.baddysays.saylat.device.DeviceCapabilities
import com.baddysays.saylat.device.DeviceProfile
import com.baddysays.saylat.engine.LayoutLabComparison
import com.baddysays.saylat.engine.LayoutLabMetrics
import com.baddysays.saylat.engine.LayoutLabResult
import com.baddysays.saylat.engine.RenderPlan
import com.baddysays.saylat.data.ActRequest
import com.baddysays.saylat.data.FeedItem
import com.baddysays.saylat.engine.ArticleDisplayEnricher
import com.baddysays.saylat.engine.TelegramFeedMapper
import com.baddysays.saylat.engine.SmartLayoutCoordinator
import com.baddysays.saylat.data.StripPage
import com.baddysays.saylat.data.VisualMapper
import com.baddysays.saylat.data.VisualPage
import com.baddysays.saylat.cache.PageCache
import com.baddysays.saylat.util.GallerySaver
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.prefs.ImagesMode
import com.baddysays.saylat.prefs.ReaderMode
import com.baddysays.saylat.prefs.PetSaladEconomy
import com.baddysays.saylat.prefs.PetShopCatalog
import com.baddysays.saylat.prefs.PetShopResult
import com.baddysays.saylat.prefs.PetWallet
import com.baddysays.saylat.prefs.SaylatPrefs
import com.baddysays.saylat.util.SaylatUserAgents
import com.baddysays.saylat.search.SearchEngine
import com.baddysays.saylat.search.SearchHit
import com.baddysays.saylat.search.SearchRepository
import com.baddysays.saylat.translate.ArticleTranslator
import com.baddysays.saylat.update.AppUpdateManager
import com.baddysays.saylat.update.UpdateCheckResult
import com.baddysays.saylat.network.ConnectivityMonitor
import com.baddysays.saylat.network.NetworkLinkSpeed
import com.baddysays.saylat.network.NetworkDiagnostics
import com.baddysays.saylat.network.NetworkTestResult
import com.baddysays.saylat.network.SpeedProfile
import com.baddysays.saylat.network.SpeedTier
import com.baddysays.saylat.tts.ArticleTtsEngine
import com.baddysays.saylat.tts.TtsState
import com.baddysays.saylat.tts.TtsStatus
import com.baddysays.saylat.ui.pet.PetBrowserAction
import com.baddysays.saylat.ui.pet.PetBrowserBridge
import com.baddysays.saylat.ui.pet.PetBrowserCue
import com.baddysays.saylat.ui.theme.AppThemeId
import com.baddysays.saylat.tamagotchi.PetSiteReactions
import com.baddysays.saylat.util.HomeShortcutHelper
import com.baddysays.saylat.util.UrlResolver
import com.baddysays.saylat.util.UserFacingErrors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppScreen {
    HOME,
    SEARCH_RESULTS,
    READER,
    FEED,
}

data class BrowserUiState(
    val screen: AppScreen = AppScreen.HOME,
    val serverUrl: String = SaylatPrefs.DEFAULT_EMULATOR,
    val searchEngine: SearchEngine = SearchEngine.SEARXNG,
    val searxInstanceUrl: String = SaylatPrefs.DEFAULT_SEARX_INSTANCE,
    val recentSearches: List<String> = emptyList(),
    val searchResults: List<SearchHit> = emptyList(),
    val activeSearchQuery: String? = null,
    val smartLayoutEnabled: Boolean = false,
    val smartLayoutAvailable: Boolean = true,
    val smartLayoutHint: String? = null,
    val loading: Boolean = false,
    val searching: Boolean = false,
    val layoutEnhancing: Boolean = false,
    val error: String? = null,
    val article: SaylatArticle? = null,
    val plan: RenderPlan? = null,
    val showSettings: Boolean = false,
    val updateChecking: Boolean = false,
    val updateDownloading: Boolean = false,
    val updateStatus: String? = null,
    val translateTargetLang: String = SaylatPrefs.DEFAULT_TRANSLATE_TARGET,
    val translating: Boolean = false,
    val translationActive: Boolean = false,
    val appTheme: AppThemeId = AppThemeId.TEAL,
    val uiLanguage: com.baddysays.saylat.prefs.AppLanguage = com.baddysays.saylat.prefs.AppLanguage.RU,
    val networkTesting: Boolean = false,
    val networkTestResult: NetworkTestResult? = null,
    val slowNetworkMode: Boolean = true,
    val cellularSlowLink: Boolean = false,
    val liteImagesEnabled: Boolean = false,
    val deviceProfile: DeviceProfile? = null,
    val showLayoutLab: Boolean = false,
    val layoutLabLoading: Boolean = false,
    val layoutLabResult: LayoutLabResult? = null,
    val readerUseSmartLayout: Boolean = false,
    val showPageLoadStats: Boolean = true,
    val tamagotchiEnabled: Boolean = true,
    val petSkipReadyGate: Boolean = false,
    val petProfile: com.baddysays.saylat.prefs.PetProfile = com.baddysays.saylat.prefs.PetProfile(),
    val showPetShop: Boolean = false,
    val petShopMessage: String? = null,
    val pageLoadStats: ArticleStats? = null,
    val showWhatsNew: Boolean = false,
    val whatsNewVersion: String = "",
    val whatsNewNotes: String = "",
    val favoriteLinks: List<SaylatPrefs.FavoriteLink> = emptyList(),
    val feed: SaylatFeed? = null,
    val feedLoadingMore: Boolean = false,
    val visitHistory: List<SaylatPrefs.VisitEntry> = emptyList(),
    val networkOnline: Boolean = true,
    val readerMode: ReaderMode = ReaderMode.LAYOUT,
    val cachedNotice: String? = null,
    val offlineCacheEntries: List<PageCache.CachedEntry> = emptyList(),
    val cacheStats: PageCache.CacheStats = PageCache.CacheStats(),
    val showWelcome: Boolean = false,
    val welcomeServerDraft: String = "",
    val needsServerUrlSetup: Boolean = false,
    val serverReady: Boolean? = null,
    val serverStatusMessage: String? = null,
    val customServerEnabled: Boolean = false,
    val replySource: String? = null,
    val replyItemId: String? = null,
    val replyContextId: String? = null,
    val showFeedReply: Boolean = false,
    val feedReplySending: Boolean = false,
    val feedReplyError: String? = null,
    val webViewUrl: String? = null,
    val webViewLoading: Boolean = false,
    val visualPage: VisualPage? = null,
    val stripPage: StripPage? = null,
    val dismissedReaderBanners: Set<String> = emptySet(),
    val readerToast: String? = null,
    val savingGallery: Boolean = false,
    val gallerySaveMessage: String? = null,
    val connectStatus: ConnectStatus? = null,
    val credentialsDraft: ServiceCredentialsDraft = ServiceCredentialsDraft(),
    val credentialsLoading: Boolean = false,
    val credentialsSaving: Boolean = false,
    val credentialsMessage: String? = null,
    val telegramCodeSent: Boolean = false,
    val connectLoading: Boolean = false,
    val settingsTab: SettingsTab = SettingsTab.GENERAL,
    val showReadLater: Boolean = false,
    val readerTheme: ReaderTheme = ReaderTheme.AUTO,
    val readerFontSettings: ReaderFontSettings = ReaderFontSettings(),
    val petBrowserCue: PetBrowserCue? = null,
    val suppressPetReadySpeech: Boolean = false,
)

class BrowserViewModel(
    private val prefs: SaylatPrefs,
    appContext: Context,
    private val searchRepository: SearchRepository = SearchRepository(),
) : ViewModel() {

    private val appContext = appContext.applicationContext
    private val appUpdateManager = AppUpdateManager(appContext)
    private val connectivityMonitor = ConnectivityMonitor(appContext)
    private val trafficSavings = TrafficSavingsRepository(appContext)
    private val readLaterRepo = ReadLaterRepository(appContext)
    private val browsingHistory = BrowsingHistory(appContext)
    private val ttsEngine = ArticleTtsEngine(appContext)
    private val serverTts = ServerTtsPlayer(appContext)
    private var layoutLabCache: LayoutLabComparison? = null
    private var petLoadStartedAtMs = 0L
    private var lastNetworkOnline: Boolean? = null

    companion object {
        private const val LAYOUT_LAB_SAMPLE_URL = "https://ru.wikipedia.org/wiki/Интернет"
    }

    private val deviceProfile = DeviceCapabilities.profile(appContext)

    private val _state = MutableStateFlow(
        BrowserUiState(
            smartLayoutAvailable = DeviceCapabilities.canRunSmartLayout(appContext),
            smartLayoutHint = DeviceCapabilities.smartLayoutHint(appContext, AppLanguage.RU),
            deviceProfile = deviceProfile,
            slowNetworkMode = DeviceCapabilities.shouldDefaultSlowNetwork(appContext),
        ),
    )
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    init {
        TrafficSavingsBridge.listener = { original, compressed ->
            viewModelScope.launch {
                trafficSavings.record(original, compressed)
            }
        }
    }

    val screen: StateFlow<AppScreen> = state
        .map { it.screen }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppScreen.HOME)

    val readerUi: StateFlow<ReaderUiState> = state
        .map { it.toReaderUi() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    private val _searchInput = MutableStateFlow("")
    val searchInput: StateFlow<String> = _searchInput.asStateFlow()

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    val trafficSavedToday: StateFlow<Long> = trafficSavings.savedToday
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val readLaterItems = readLaterRepo.items
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyEntries = browsingHistory.entries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ttsState: StateFlow<TtsState> = combine(ttsEngine.state, serverTts.state) { local, server ->
        when {
            server.status != TtsStatus.IDLE -> server
            else -> local
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TtsState())

    init {
        viewModelScope.launch {
            prefs.ensureConsumerReady()
            prefs.ensureSlowNetworkDefault(appContext)
            val onboarded = prefs.onboardingDone.first()
            val stored = prefs.baseUrl.first()
            val needsUrl = SaylatPrefs.needsServerSetup(stored)
            val draft = when {
                stored.isNotBlank() && !needsUrl -> stored
                SaylatPrefs.publicServerUrl().isNotBlank() -> SaylatPrefs.publicServerUrl()
                else -> stored
            }
            if (!onboarded) {
                _state.value = _state.value.copy(
                    showWelcome = true,
                    needsServerUrlSetup = needsUrl,
                    welcomeServerDraft = draft,
                )
            }
            if (!needsUrl) refreshServerStatus()
            checkForUpdateSilently()
        }
        viewModelScope.launch {
            prefs.settingsBundle.collect { bundle ->
                val slow = bundle.slowNetworkMode
                    ?: DeviceCapabilities.shouldDefaultSlowNetwork(appContext)
                val prev = _state.value
                if (slow != prev.slowNetworkMode || bundle.liteImagesEnabled != prev.liteImagesEnabled) {
                    ApiFactory.invalidateCache()
                }
                _state.value = _state.value.copy(
                    slowNetworkMode = slow,
                    liteImagesEnabled = bundle.liteImagesEnabled,
                    serverUrl = bundle.serverUrl,
                    favoriteLinks = bundle.favoriteLinks,
                    smartLayoutEnabled = bundle.smartLayoutEnabled,
                    searchEngine = bundle.searchEngine,
                    searxInstanceUrl = bundle.searxInstanceUrl,
                    recentSearches = bundle.recentSearches,
                    translateTargetLang = bundle.translateTargetLang,
                    appTheme = bundle.appTheme,
                    uiLanguage = bundle.uiLanguage,
                    smartLayoutHint = DeviceCapabilities.smartLayoutHint(appContext, bundle.uiLanguage),
                    showPageLoadStats = bundle.pageLoadStatsEnabled,
                    tamagotchiEnabled = bundle.tamagotchiEnabled,
                    readerMode = bundle.readerMode,
                    dismissedReaderBanners = bundle.dismissedReaderBanners,
                    customServerEnabled = bundle.customServerEnabled,
                    visitHistory = bundle.visitHistory,
                    readerTheme = ReaderTheme.entries.find { it.name == bundle.readerTheme } ?: ReaderTheme.AUTO,
                    readerFontSettings = ReaderFontSettings(sizeSp = bundle.readerFontSize),
                )
            }
        }
        viewModelScope.launch {
            connectivityMonitor.isOnline.collect { online ->
                val was = lastNetworkOnline
                lastNetworkOnline = online
                val slowLink = NetworkLinkSpeed.isSlowCellular(appContext)
                val prevSlowLink = _state.value.cellularSlowLink
                if (slowLink != prevSlowLink) {
                    ApiFactory.invalidateCache()
                }
                _state.value = _state.value.copy(
                    networkOnline = online,
                    cellularSlowLink = slowLink,
                )
                if (was != null && was != online) {
                    petNotify(if (online) PetBrowserAction.Online else PetBrowserAction.Offline)
                }
            }
        }
        refreshConnectStatus()
        checkWhatsNewOnLaunch()
        refreshOfflineCache()
        viewModelScope.launch { prefs.recordPetAppOpen() }
        viewModelScope.launch {
            prefs.petProfile.collect { profile ->
                _state.value = _state.value.copy(petProfile = profile)
            }
        }
        viewModelScope.launch {
            prefs.petSkipReadyGate.collect { skip ->
                _state.value = _state.value.copy(petSkipReadyGate = skip)
            }
        }
    }

    private var savedPetLoadKey: String? = null
    private var savedPetPhase: PetPhase? = null

    fun restorePetPhase(loadKey: String): PetPhase? =
        savedPetPhase?.takeIf { savedPetLoadKey == loadKey }

    fun persistPetSession(loadKey: String, phase: PetPhase) {
        savedPetLoadKey = loadKey
        savedPetPhase = phase
    }

    fun setPetSkipReadyGate(skip: Boolean) {
        _state.value = _state.value.copy(petSkipReadyGate = skip)
        viewModelScope.launch { prefs.setPetSkipReadyGate(skip) }
    }

    fun awardPetXp(amount: Int) {
        viewModelScope.launch { prefs.addPetXp(amount) }
    }

    fun consumePetBrowserCue() {
        _state.value = _state.value.copy(
            petBrowserCue = null,
            suppressPetReadySpeech = false,
        )
    }

    private fun petNotify(action: PetBrowserAction) {
        if (!_state.value.tamagotchiEnabled) return
        val cue = PetBrowserBridge.cueFor(action, _state.value.uiLanguage)
        if (cue.xp > 0) awardPetXp(cue.xp)
        _state.value = _state.value.copy(petBrowserCue = cue)
    }

    private fun petLoadStart(url: String) {
        petLoadStartedAtMs = System.currentTimeMillis()
    }

    private fun petLoadSuccess(url: String, stats: ArticleStats) {
        val saved = (stats.original_bytes - stats.payload_bytes).toLong().coerceAtLeast(0)
        val duration = (System.currentTimeMillis() - petLoadStartedAtMs).coerceAtLeast(0)
        val snap = _state.value
        val eco = snap.slowNetworkMode || snap.liteImagesEnabled
        val xp = com.baddysays.saylat.tamagotchi.PetXpRewards.forLoad(saved, duration, eco)
        if (xp > 0) awardPetXp(xp)
        val ratio = if (stats.original_bytes > 0) {
            ((1 - stats.payload_bytes.toFloat() / stats.original_bytes) * 100).toInt()
        } else {
            0
        }
        val bigCompression = ratio >= 40 && stats.original_bytes > 64 * 1024
        if (bigCompression || saved > 10_000) {
            val cue = PetBrowserBridge.cueFor(
                PetBrowserAction.LoadSuccess(
                    url = url,
                    savedBytes = saved,
                    durationMs = duration,
                    stats = stats,
                    host = PetSiteReactions.hostFromUrl(url),
                    ecoMode = eco,
                ),
                snap.uiLanguage,
            )
            _state.value = _state.value.copy(
                petBrowserCue = cue.copy(xp = 0),
                suppressPetReadySpeech = true,
            )
        }
    }

    fun petHomeCare() {
        viewModelScope.launch { prefs.petHomeCare() }
    }

    fun setPetName(name: String) {
        viewModelScope.launch { prefs.setPetName(name) }
    }

    fun openPetShop() {
        _state.value = _state.value.copy(showPetShop = true, petShopMessage = null)
    }

    fun closePetShop() {
        _state.value = _state.value.copy(showPetShop = false, petShopMessage = null)
    }

    fun buyPetShopItem(itemId: String) {
        viewModelScope.launch {
            when (prefs.buyPetShopItem(itemId)) {
                PetShopResult.Success -> {
                    prefs.equipPetShopItem(itemId)
                    _state.value = _state.value.copy(
                        petShopMessage = "Куплено! ${PetShopCatalog.find(itemId)?.title ?: ""}",
                    )
                }
                PetShopResult.AlreadyOwned -> equipPetShopItem(itemId)
                PetShopResult.InsufficientFunds -> _state.value = _state.value.copy(
                    petShopMessage = "Не хватает КБ в кошельке",
                )
                PetShopResult.NotFound -> _state.value = _state.value.copy(petShopMessage = "Нет такого товара")
            }
        }
    }

    fun equipPetShopItem(itemId: String) {
        viewModelScope.launch {
            if (prefs.equipPetShopItem(itemId)) {
                val title = PetShopCatalog.find(itemId)?.title ?: ""
                _state.value = _state.value.copy(petShopMessage = "Надето: $title")
            }
        }
    }

    fun unequipPetToys() {
        viewModelScope.launch {
            prefs.unequipPetToy()
            _state.value = _state.value.copy(petShopMessage = "Игрушка убрана")
        }
    }

    /** Списать один салатик (оптимистично обновляет UI, затем DataStore). */
    fun spendPetSalad(): Boolean {
        val profile = _state.value.petProfile
        val next = PetSaladEconomy.spendSalad(profile) ?: return false
        _state.value = _state.value.copy(petProfile = next)
        viewModelScope.launch {
            if (!prefs.spendPetSalad()) {
                prefs.petProfile.first().let { restored ->
                    _state.value = _state.value.copy(petProfile = restored)
                }
            }
        }
        return true
    }

    fun hatchPetEgg() {
        val profile = _state.value.petProfile
        if (profile.petHatched) return
        _state.value = _state.value.copy(petProfile = profile.copy(petHatched = true))
        viewModelScope.launch { prefs.hatchPetEgg() }
    }

    fun setWebViewLoading(loading: Boolean) {
        if (_state.value.webViewLoading == loading) return
        _state.value = _state.value.copy(webViewLoading = loading)
    }

    private var lastSaladCreditKey: String? = null

    private fun creditPetTrafficSavings(url: String, stats: ArticleStats) {
        if (!_state.value.tamagotchiEnabled) return
        val saved = (stats.original_bytes - stats.payload_bytes).toLong()
        if (saved <= 0) return
        val key = "$url|${stats.original_bytes}|${stats.payload_bytes}"
        if (key == lastSaladCreditKey) return
        lastSaladCreditKey = key
        viewModelScope.launch { prefs.creditPetBytesSaved(saved) }
    }

    fun setWelcomeServerDraft(url: String) {
        _state.value = _state.value.copy(welcomeServerDraft = url)
    }

    fun dismissWelcome() {
        viewModelScope.launch {
            val draft = _state.value.welcomeServerDraft.trim()

            val needsUrl = _state.value.needsServerUrlSetup
            val hasValidUrl = draft.startsWith("http")

            // If user didn't provide a valid server URL yet, don't mark onboarding as completed.
            // This avoids trapping them into the same blocked state on next launch.
            if (!needsUrl) {
                prefs.setOnboardingDone()
            } else if (hasValidUrl) {
                prefs.setBaseUrl(draft)
                prefs.setCustomServerEnabled(true)
                prefs.setOnboardingDone()
            }

            _state.value = _state.value.copy(showWelcome = false)
            refreshServerStatus()
        }
    }

    fun refreshServerStatus() {
        viewModelScope.launch {
            val base = prefs.baseUrl.first()
            val slow = effectiveSlowNetwork()
            _state.value = _state.value.copy(serverReady = null, serverStatusMessage = "Проверяем сервер…")
            val result = withContext(Dispatchers.IO) {
                NetworkDiagnostics.runFullTest(base, slow)
            }
            _state.value = _state.value.copy(
                serverReady = result.ok,
                serverStatusMessage = if (result.ok) {
                    "Интернет через Saylat готов"
                } else {
                    result.error ?: "Нет связи с сервером — проверьте сеть"
                },
                networkTestResult = result,
            )
        }
    }

    private fun checkForUpdateSilently() {
        viewModelScope.launch {
            try {
                when (val check = AppUpdateManager(appContext).checkForUpdate(_state.value.serverUrl)) {
                    is UpdateCheckResult.Available -> {
                        _state.value = _state.value.copy(
                            updateStatus = "Доступно ${check.info.version_name} — Настройки → Обновить",
                        )
                    }
                    else -> Unit
                }
            } catch (_: Exception) {
                /* офлайн */
            }
        }
    }

    fun setCustomServerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setCustomServerEnabled(enabled)
        }
    }

    fun refreshOfflineCache() {
        viewModelScope.launch {
            val entries = withContext(Dispatchers.IO) { PageCache.listRecent(appContext) }
            val stats = withContext(Dispatchers.IO) { PageCache.stats(appContext) }
            _state.value = _state.value.copy(offlineCacheEntries = entries, cacheStats = stats)
        }
    }

    fun clearAppCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { PageCache.clearAll(appContext) }
            refreshOfflineCache()
            _state.value = _state.value.copy(
                gallerySaveMessage = "Кэш очищен",
                cachedNotice = null,
            )
        }
    }

    private fun checkWhatsNewOnLaunch() {
        viewModelScope.launch {
            try {
                val lastSeen = prefs.lastSeenVersionCode.first()
                if (BuildConfig.VERSION_CODE <= lastSeen) return@launch
                when (val check = appUpdateManager.checkForUpdate(_state.value.serverUrl)) {
                    is UpdateCheckResult.UpToDate -> {
                        _state.value = _state.value.copy(
                            showWhatsNew = true,
                            whatsNewVersion = check.serverVersionName.ifBlank { BuildConfig.VERSION_NAME },
                            whatsNewNotes = "Спасибо за обновление Saylat.",
                        )
                    }
                    is UpdateCheckResult.Available -> Unit
                }
            } catch (_: Exception) {
                /* GitHub недоступен */
            }
        }
    }

    fun dismissWhatsNew() {
        viewModelScope.launch {
            prefs.setLastSeenVersionCode(BuildConfig.VERSION_CODE)
            _state.value = _state.value.copy(showWhatsNew = false)
        }
    }

    fun refreshConnectStatus() {
        viewModelScope.launch {
            try {
                val status = api().connectStatus()
                _state.value = _state.value.copy(connectStatus = status)
            } catch (_: Exception) {
                /* VPS без нового API — игнор */
            }
        }
    }

    fun openServiceSettings() {
        openSettings(SettingsTab.SERVICES)
    }

    fun openNetworkSettings() {
        openSettings(SettingsTab.NETWORK)
    }

    fun setSettingsTab(tab: SettingsTab) {
        _state.value = _state.value.copy(settingsTab = tab)
    }

    fun setCredentialsDraft(draft: ServiceCredentialsDraft) {
        _state.value = _state.value.copy(credentialsDraft = draft)
    }

    fun loadServiceCredentials() {
        viewModelScope.launch {
            _state.value = _state.value.copy(credentialsLoading = true, credentialsMessage = null)
            try {
                val public = api().getServiceCredentials()
                _state.value = _state.value.copy(
                    credentialsLoading = false,
                    credentialsDraft = draftFromPublic(public),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    credentialsLoading = false,
                    credentialsMessage = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun saveServiceCredentials() {
        val draft = _state.value.credentialsDraft
        viewModelScope.launch {
            _state.value = _state.value.copy(credentialsSaving = true, credentialsMessage = null)
            try {
                val update = ServiceCredentialsUpdate(
                    telegram_api_id = draft.telegramApiId.trim().toIntOrNull(),
                    telegram_api_hash = draft.telegramApiHash.trim().ifBlank { null },
                    mail_imap_host = draft.mailImapHost.trim().ifBlank { null },
                    mail_imap_port = draft.mailImapPort.trim().toIntOrNull(),
                    mail_smtp_host = draft.mailSmtpHost.trim().ifBlank { null },
                    mail_smtp_port = draft.mailSmtpPort.trim().toIntOrNull(),
                    mail_username = draft.mailUsername.trim().ifBlank { null },
                    mail_password = draft.mailPassword.ifBlank { null },
                    mail_use_ssl = draft.mailUseSsl,
                    vk_access_token = draft.vkToken.trim().ifBlank { null },
                    dzen_session_cookie = draft.dzenCookie.trim().ifBlank { null },
                )
                val public = api().putServiceCredentials(update)
                _state.value = _state.value.copy(
                    credentialsSaving = false,
                    credentialsDraft = draftFromPublic(public).copy(
                        mailPassword = "",
                        vkToken = "",
                        dzenCookie = "",
                        telegramPhone = draft.telegramPhone,
                        telegramCode = draft.telegramCode,
                    ),
                    credentialsMessage = "Сохранено",
                )
                refreshConnectStatus()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    credentialsSaving = false,
                    credentialsMessage = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun requestTelegramCode() {
        val phone = _state.value.credentialsDraft.telegramPhone
        viewModelScope.launch {
            _state.value = _state.value.copy(connectLoading = true, credentialsMessage = null)
            try {
                val res = api().telegramCode(TelegramCodeRequest(phone.trim()))
                _state.value = _state.value.copy(
                    connectLoading = false,
                    telegramCodeSent = true,
                    credentialsMessage = res.message,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    connectLoading = false,
                    credentialsMessage = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun signInTelegram() {
        val draft = _state.value.credentialsDraft
        viewModelScope.launch {
            _state.value = _state.value.copy(connectLoading = true)
            try {
                val res = api().telegramSignIn(
                    TelegramSignInRequest(draft.telegramPhone.trim(), draft.telegramCode.trim()),
                )
                _state.value = _state.value.copy(
                    connectLoading = false,
                    credentialsMessage = res.message,
                    telegramCodeSent = false,
                )
                refreshConnectStatus()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    connectLoading = false,
                    credentialsMessage = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun openService(serviceId: String) {
        when (serviceId) {
            "inbox" -> openUnifiedInbox()
            "pikabu" -> loadUrl("https://pikabu.ru/")
            "vk" -> openTarget("vk")
            "dzen" -> openTarget("dzen", url = "https://dzen.ru/news")
            "telegram" -> openTarget("telegram")
            "mail" -> openTarget("mail")
        }
    }

    fun loadMoreFeed() {
        val current = _state.value.feed ?: return
        if (!current.has_more || _state.value.feedLoadingMore) return
        viewModelScope.launch {
            _state.value = _state.value.copy(feedLoadingMore = true, error = null)
            try {
                val offset = current.items.size
                val page = api().unifiedFeed(limit = 12, offset = offset, pageSize = 24)
                _state.value = _state.value.copy(
                    feedLoadingMore = false,
                    feed = current.copy(
                        items = current.items + page.items,
                        has_more = page.has_more,
                        total_items = page.total_items,
                    ),
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    feedLoadingMore = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun shareCurrentPage() {
        val url = currentPageUrl() ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(
            Intent.createChooser(intent, "Поделиться ссылкой").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun currentPageUrl(): String? {
        val s = _state.value
        return when {
            !s.article?.url.isNullOrBlank() -> s.article?.url
            !s.stripPage?.url.isNullOrBlank() -> s.stripPage?.url
            !s.webViewUrl.isNullOrBlank() -> s.webViewUrl
            _urlInput.value.startsWith("http://") || _urlInput.value.startsWith("https://") -> _urlInput.value
            else -> null
        }
    }

    private fun recordVisit(url: String, title: String) {
        viewModelScope.launch {
            prefs.recordVisit(url, title)
            browsingHistory.record(url, title)
        }
    }

    fun openRssFeed(feedUrl: String) {
        val normalized = normalizeUrl(feedUrl.trim())
        if (!normalized.startsWith("http")) return
        viewModelScope.launch {
            petNotify(PetBrowserAction.FeedOpen)
            _urlInput.value = normalized
            _state.value = _state.value.copy(
                screen = AppScreen.FEED,
                loading = true,
                error = null,
                feed = null,
                replySource = null,
                replyItemId = null,
                replyContextId = null,
                showFeedReply = false,
            )
            try {
                val feed = api().rssFeed(normalized)
                _state.value = _state.value.copy(
                    loading = false,
                    feed = feed,
                    feedLoadingMore = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun reloadFeed() {
        val feed = _state.value.feed ?: return
        if (feed.source == "rss" && feed.context_id.startsWith("http")) {
            openRssFeed(feed.context_id)
            return
        }
        openUnifiedInbox()
    }

    fun openUnifiedInbox() {
        viewModelScope.launch {
            petNotify(PetBrowserAction.FeedOpen)
            _state.value = _state.value.copy(
                screen = AppScreen.FEED,
                loading = true,
                error = null,
                feed = null,
            )
            try {
                val feed = api().unifiedFeed(limit = 12, offset = 0, pageSize = 24)
                _state.value = _state.value.copy(
                    loading = false,
                    feed = feed,
                    feedLoadingMore = false,
                    screen = AppScreen.FEED,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = UserFacingErrors.from(e),
                    screen = AppScreen.HOME,
                )
            }
        }
    }

    fun setPageLoadStatsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setPageLoadStatsEnabled(enabled) }
    }

    fun setTamagotchiEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setTamagotchiEnabled(enabled) }
    }

    fun setAppTheme(theme: AppThemeId) {
        viewModelScope.launch { prefs.setAppTheme(theme) }
    }

    fun setUiLanguage(language: com.baddysays.saylat.prefs.AppLanguage) {
        viewModelScope.launch { prefs.setUiLanguage(language) }
    }

    fun setSlowNetworkMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setSlowNetworkMode(enabled) }
    }

    fun setLiteImagesEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setLiteImagesEnabled(enabled) }
    }

    fun setQuickSpeedMode(mode: QuickSpeedMode) {
        val wasEco = QuickSpeedMode.fromFlags(
            _state.value.slowNetworkMode,
            _state.value.liteImagesEnabled,
        ) == QuickSpeedMode.ECO
        viewModelScope.launch {
            when (mode) {
                QuickSpeedMode.ECO -> {
                    prefs.setSlowNetworkMode(true)
                    prefs.setLiteImagesEnabled(true)
                    if (!wasEco) petNotify(PetBrowserAction.EcoModeEnabled)
                }
                QuickSpeedMode.BALANCED -> {
                    prefs.setSlowNetworkMode(true)
                    prefs.setLiteImagesEnabled(false)
                }
                QuickSpeedMode.FAST -> {
                    prefs.setSlowNetworkMode(false)
                    prefs.setLiteImagesEnabled(false)
                }
            }
        }
    }

    fun setReaderMode(mode: ReaderMode) {
        viewModelScope.launch { prefs.setReaderMode(mode) }
    }

    fun closeLayoutLab() {
        _state.value = _state.value.copy(showLayoutLab = false)
    }

    fun runLayoutLab() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                showLayoutLab = true,
                layoutLabLoading = true,
                layoutLabResult = null,
                error = null,
            )
            try {
                val article = api().fetchArticleLegacy(
                    LAYOUT_LAB_SAMPLE_URL,
                    images = extractImagesMode(),
                    level = compressionLevel(),
                )
                val comparison = withContext(Dispatchers.Default) {
                    LayoutLabMetrics.compare(article)
                }
                layoutLabCache = comparison
                _state.value = _state.value.copy(
                    layoutLabLoading = false,
                    layoutLabResult = comparison.metrics,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    layoutLabLoading = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun openLayoutLabReader(useSmart: Boolean) {
        val cached = layoutLabCache ?: return
        if (useSmart && !DeviceCapabilities.canRunSmartLayout(appContext)) return
        _urlInput.value = cached.article.url
        _state.value = _state.value.copy(
            showLayoutLab = false,
            screen = AppScreen.READER,
            article = cached.article,
            plan = if (useSmart) cached.smart else cached.baseline,
            readerUseSmartLayout = useSmart,
            layoutEnhancing = false,
            error = null,
        )
    }

    fun setReaderLayoutMode(useSmart: Boolean) {
        val article = _state.value.article ?: return
        val lang = _state.value.uiLanguage
        if (useSmart && !DeviceCapabilities.canRunSmartLayout(appContext)) {
            _state.value = _state.value.copy(
                readerToast = com.baddysays.saylat.ui.strings.SaylatStrings.smartLayoutUnavailableToast(lang),
            )
            return
        }
        viewModelScope.launch {
            applyArticle(article, useSmartLayout = useSmart)
        }
    }

    fun clearReaderToast() {
        if (_state.value.readerToast != null) {
            _state.value = _state.value.copy(readerToast = null)
        }
    }

    fun showReaderToast(message: String) {
        _state.value = _state.value.copy(readerToast = message)
    }

    fun dismissNetworkTest() {
        _state.value = _state.value.copy(networkTestResult = null)
    }

    fun runNetworkTest() {
        viewModelScope.launch {
            _state.value = _state.value.copy(networkTesting = true, error = null)
            try {
                val base = prefs.baseUrl.first()
                val slow = effectiveSlowNetwork()
                val result = NetworkDiagnostics.runFullTest(base, slowNetwork = slow)
                _state.value = _state.value.copy(
                    networkTesting = false,
                    networkTestResult = result,
                    error = null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    networkTesting = false,
                    networkTestResult = NetworkTestResult(
                        ok = false,
                        error = UserFacingErrors.from(e),
                        profile = SpeedProfile(
                            tier = SpeedTier.OFFLINE,
                            title = "Нет связи",
                            description = "Проверьте URL прокси и интернет",
                        ),
                    ),
                    error = null,
                )
            }
        }
    }

    private fun compressionLevel(): String {
        val snap = _state.value
        val smartAvail = DeviceCapabilities.canRunSmartLayout(appContext)
        return CompressionLevel.resolve(snap.slowNetworkMode, snap.smartLayoutEnabled, smartAvail)
    }

    private fun effectiveSlowNetwork(): Boolean {
        val snap = _state.value
        return snap.slowNetworkMode || snap.cellularSlowLink
    }

    private suspend fun api(): com.baddysays.saylat.data.SaylatApi {
        val base = prefs.baseUrl.first()
        val snap = _state.value
        return ApiFactory.create(
            base,
            slowNetwork = effectiveSlowNetwork(),
            compressionLevel = compressionLevel(),
            apiKey = com.baddysays.saylat.BuildConfig.PROXY_API_KEY,
        )
    }

    fun dismissReaderBanner(bannerId: String) {
        viewModelScope.launch { prefs.dismissReaderBanner(bannerId) }
    }

    fun saveStripPageToGallery() {
        val page = _state.value.stripPage ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(savingGallery = true, gallerySaveMessage = null)
            val name = page.title.take(40).ifBlank { "saylat" }.replace(Regex("[^a-zA-Zа-яА-Я0-9._-]"), "_")
            val uri = withContext(Dispatchers.IO) {
                GallerySaver.saveStripsMerged(
                    appContext,
                    page.strips.map { it.src },
                    name,
                )
            }
            _state.value = _state.value.copy(
                savingGallery = false,
                gallerySaveMessage = if (uri != null) {
                    "Сохранено в «Картинки/Saylat»"
                } else {
                    "Не удалось сохранить. Проверьте разрешения хранилища."
                },
            )
        }
    }

    /** Сохранить текущую страницу в галерею (полосы с VPS или уже загруженные). */
    fun saveCurrentPageToGallery() {
        if (_state.value.stripPage != null) {
            saveStripPageToGallery()
            return
        }
        val url = _urlInput.value.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        viewModelScope.launch {
            _state.value = _state.value.copy(savingGallery = true, gallerySaveMessage = null)
            try {
                val page = api().renderStrips(url, images = extractImagesMode(), engine = "browser")
                val name = page.title.take(40).ifBlank { "saylat" }
                    .replace(Regex("[^a-zA-Zа-яА-Я0-9._-]"), "_")
                val uri = withContext(Dispatchers.IO) {
                    GallerySaver.saveStripsMerged(appContext, page.strips.map { it.src }, name)
                }
                withContext(Dispatchers.IO) { PageCache.putStripPage(appContext, page) }
                refreshOfflineCache()
                _state.value = _state.value.copy(
                    stripPage = page,
                    savingGallery = false,
                    gallerySaveMessage = if (uri != null) {
                        "Сохранено в «Картинки/Saylat» (${page.strips.size} полос), кэш обновлён"
                    } else {
                        "Не удалось сохранить. Проверьте разрешения хранилища."
                    },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    savingGallery = false,
                    gallerySaveMessage = UserFacingErrors.from(e),
                )
            }
        }
    }

    private suspend fun extractImagesMode(): String {
        val snap = _state.value
        val ecoImages = ImagesMode.resolve(snap.slowNetworkMode, snap.liteImagesEnabled)
        val ecoActive = snap.slowNetworkMode || snap.liteImagesEnabled
        return when (effectiveReaderMode()) {
            ReaderMode.LAYOUT, ReaderMode.NATIVE ->
                if (ecoActive) ecoImages else ImagesMode.LAYOUT
            ReaderMode.STRIPS, ReaderMode.VISUAL -> ImagesMode.TINY
            ReaderMode.WEBVIEW -> ecoImages
            ReaderMode.AUTO ->
                if (ecoActive) ecoImages else ImagesMode.LAYOUT
        }
    }

    fun setTranslateTargetLang(code: String) {
        viewModelScope.launch { prefs.setTranslateTargetLang(code) }
    }

    fun loadFromUrlBar(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return
        _urlInput.value = trimmed
        loadUrl(normalizeUrl(trimmed))
    }

    fun openSettings(tab: SettingsTab = SettingsTab.GENERAL) {
        _state.value = _state.value.copy(
            showSettings = true,
            settingsTab = tab,
            credentialsMessage = null,
            telegramCodeSent = false,
        )
        refreshOfflineCache()
        loadServiceCredentials()
        refreshConnectStatus()
    }

    fun closeSettings() {
        _state.value = _state.value.copy(showSettings = false, settingsTab = SettingsTab.GENERAL)
    }

    private var originalArticle: SaylatArticle? = null

    fun goHome() {
        originalArticle = null
        _searchInput.value = ""
        _urlInput.value = ""
        _state.value = _state.value.copy(
            screen = AppScreen.HOME,
            plan = null,
            article = null,
            webViewUrl = null,
            webViewLoading = false,
            visualPage = null,
            stripPage = null,
            gallerySaveMessage = null,
            cachedNotice = null,
            replySource = null,
            replyItemId = null,
            replyContextId = null,
            showFeedReply = false,
            feedReplySending = false,
            feedReplyError = null,
            pageLoadStats = null,
            feed = null,
            translationActive = false,
            translating = false,
            searchResults = emptyList(),
            activeSearchQuery = null,
            loading = false,
            searching = false,
            layoutEnhancing = false,
            error = null,
        )
        refreshOfflineCache()
    }

    fun openCachedPage(url: String) {
        viewModelScope.launch {
            _urlInput.value = url
            _state.value = _state.value.copy(
                screen = AppScreen.READER,
                loading = true,
                error = null,
                cachedNotice = null,
                replySource = null,
                replyItemId = null,
                replyContextId = null,
                webViewUrl = null,
            webViewLoading = false,
                stripPage = null,
                article = null,
                plan = null,
            )
            val strips = withContext(Dispatchers.IO) { PageCache.loadStripPage(appContext, url) }
            if (strips != null) {
                _urlInput.value = strips.url
                _state.value = _state.value.copy(
                    screen = AppScreen.READER,
                    loading = false,
                    stripPage = strips,
                    article = null,
                    plan = null,
                    webViewUrl = null,
            webViewLoading = false,
                    cachedNotice = "Полосы из офлайн-кэша",
                    pageLoadStats = ArticleStats(
                        original_bytes = strips.stats.original_bytes,
                        payload_bytes = strips.stats.payload_bytes,
                        fetch_ms = strips.stats.fetch_ms,
                    ),
                )
                creditPetTrafficSavings(
                    strips.url,
                    ArticleStats(
                        original_bytes = strips.stats.original_bytes,
                        payload_bytes = strips.stats.payload_bytes,
                        fetch_ms = strips.stats.fetch_ms,
                    ),
                )
                return@launch
            }
            val article = withContext(Dispatchers.IO) { PageCache.loadArticle(appContext, url) }
            if (article != null) {
                _state.value = _state.value.copy(loading = false, cachedNotice = "Статья из офлайн-кэша")
                applyArticle(article, smartEnhanceFromPrefs = false)
                return@launch
            }
            loadUrl(url)
        }
    }

    fun backFromReader() {
        stopTts()
        originalArticle = null
        if (_state.value.feed != null) {
            _state.value = _state.value.copy(
                screen = AppScreen.FEED,
                plan = null,
                article = null,
                webViewUrl = null,
            webViewLoading = false,
                visualPage = null,
                stripPage = null,
                layoutEnhancing = false,
                translationActive = false,
                translating = false,
            )
            return
        }
        val prev = _state.value.activeSearchQuery
        if (prev != null) {
            _state.value = _state.value.copy(
                screen = AppScreen.SEARCH_RESULTS,
                plan = null,
                article = null,
                webViewUrl = null,
            webViewLoading = false,
                visualPage = null,
                stripPage = null,
                layoutEnhancing = false,
                translationActive = false,
                translating = false,
            )
        } else {
            goHome()
        }
    }

    fun backFromFeed() {
        val prev = _state.value.activeSearchQuery
        if (prev != null) {
            _state.value = _state.value.copy(screen = AppScreen.SEARCH_RESULTS, feed = null)
        } else {
            goHome()
        }
    }

    fun openFeedItem(item: FeedItem) {
        if (item.id.startsWith("mail-")) {
            openMailItem(item)
            return
        }
        if (item.id.startsWith("tgmsg-") && item.actions.contains("reply")) {
            openTelegramMessage(item)
            return
        }
        val href = item.href?.trim().orEmpty()
        when {
            item.id.startsWith("rss-") && href.startsWith("http") -> {
                loadUrl(href, keepFeedParent = true)
            }
            href.startsWith("saylat://telegram/") -> {
                val chatId = href.removePrefix("saylat://telegram/")
                openTarget("telegram", resourceId = chatId, keepFeedParent = true)
            }
            href.startsWith("http://") || href.startsWith("https://") -> {
                viewModelScope.launch {
                    loadStrips(href, keepFeedParent = true)
                }
            }
        }
    }

    fun reloadWithMode(mode: ReaderMode) {
        val currentUrl = _urlInput.value.trim()
        if (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://")) return
        viewModelScope.launch {
            prefs.setReaderMode(mode)
            loadUrl(currentUrl)
        }
    }

    fun reloadCurrentUrl(invalidateCache: Boolean = false) {
        val currentUrl = _urlInput.value.trim()
        if (!currentUrl.startsWith("http://") && !currentUrl.startsWith("https://")) return
        viewModelScope.launch {
            if (invalidateCache) {
                petNotify(PetBrowserAction.Refresh)
                try {
                    api().invalidateCache(currentUrl)
                } catch (_: Exception) {
                }
            }
            loadUrl(currentUrl)
        }
    }

    fun toggleReadLater() {
        val article = _state.value.article ?: return
        val url = _urlInput.value.trim().takeIf { it.startsWith("http") } ?: article.url
        viewModelScope.launch {
            if (readLaterRepo.contains(url)) {
                readLaterRepo.remove(url)
                showReaderToast("Убрано из списка чтения")
            } else {
                val plain = article.plain_text.ifBlank {
                    article.blocks.mapNotNull { it.text }.joinToString(" ")
                }
                readLaterRepo.add(
                    ReadLaterItem(
                        url = url,
                        title = article.title,
                        excerpt = article.excerpt.take(200),
                        estimatedMinutes = readingTimeFromText(plain)
                            ?.filter { it.isDigit() }
                            ?.toIntOrNull(),
                    ),
                )
                petNotify(PetBrowserAction.SaveReadLater)
                showReaderToast("Добавлено в список чтения")
            }
        }
    }

    fun removeReadLater(url: String) {
        viewModelScope.launch { readLaterRepo.remove(url) }
    }

    fun openReadLater() {
        _state.value = _state.value.copy(showReadLater = true)
    }

    fun closeReadLater() {
        _state.value = _state.value.copy(showReadLater = false)
    }

    fun setReaderTheme(theme: ReaderTheme) {
        viewModelScope.launch { prefs.setReaderTheme(theme.name) }
    }

    fun setReaderFontSettings(settings: ReaderFontSettings) {
        viewModelScope.launch { prefs.setReaderFontSize(settings.sizeSp) }
    }

    fun startTts(startFromParagraph: Int = 0) {
        val article = _state.value.article ?: return
        if (effectiveSlowNetwork()) {
            viewModelScope.launch {
                val base = prefs.baseUrl.first()
                val client = ApiFactory.httpClient(
                    base,
                    slowNetwork = true,
                    compressionLevel = compressionLevel(),
                    apiKey = BuildConfig.PROXY_API_KEY,
                )
                serverTts.play(client, base, article.url, BuildConfig.PROXY_API_KEY)
            }
            return
        }
        val paragraphs = article.blocks
            .filter { it.type == "paragraph" || it.type == "heading" }
            .mapNotNull { it.text }
            .filter { it.length > 10 }
        if (paragraphs.isEmpty() && article.plain_text.isNotBlank()) {
            ttsEngine.play(listOf(article.plain_text), startFrom = startFromParagraph)
        } else {
            ttsEngine.play(paragraphs, startFrom = startFromParagraph)
        }
    }

    fun pauseTts() {
        if (effectiveSlowNetwork()) serverTts.pause() else ttsEngine.pause()
    }

    fun resumeTts() {
        if (effectiveSlowNetwork()) serverTts.resume() else ttsEngine.resume()
    }

    fun stopTts() {
        serverTts.stop()
        ttsEngine.stop()
    }

    fun nextTts() {
        if (effectiveSlowNetwork() && serverTts.state.value.status != TtsStatus.IDLE) {
            // single MP3 — restart from beginning is best-effort no-op for skip
            return
        }
        ttsEngine.skipNext()
    }

    fun prevTts() {
        if (effectiveSlowNetwork() && serverTts.state.value.status != TtsStatus.IDLE) {
            return
        }
        ttsEngine.skipPrev()
    }

    override fun onCleared() {
        TrafficSavingsBridge.listener = null
        serverTts.destroy()
        ttsEngine.destroy()
        super.onCleared()
    }

    private fun openMailItem(item: FeedItem) {
        val parentFeed = _state.value.feed
        viewModelScope.launch {
            _state.value = _state.value.copy(
                screen = AppScreen.READER,
                loading = true,
                error = null,
                webViewUrl = null,
            webViewLoading = false,
                stripPage = null,
                visualPage = null,
                translationActive = false,
                translating = false,
            )
            try {
                val response = api().open(
                    OpenRequest(target = "mail", resource_id = item.id, images = extractImagesMode()),
                )
                applyOpenResponse(response, urlInput = "saylat://mail/${item.id}")
                _state.value = _state.value.copy(
                    feed = parentFeed,
                    replySource = "imap",
                    replyItemId = item.id,
                    replyContextId = parentFeed?.context_id?.takeIf { it.isNotBlank() },
                    showFeedReply = false,
                    feedReplyError = null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    private fun openTelegramMessage(item: FeedItem) {
        val feed = _state.value.feed ?: return
        val article = TelegramFeedMapper.toArticle(item, feed)
        originalArticle = null
        _urlInput.value = article.url
        viewModelScope.launch {
            _state.value = _state.value.copy(
                screen = AppScreen.READER,
                loading = false,
                error = null,
                webViewUrl = null,
            webViewLoading = false,
                stripPage = null,
                visualPage = null,
                feed = feed,
                replySource = "telegram",
                replyItemId = item.id,
                replyContextId = feed.context_id.takeIf { it.isNotBlank() },
                showFeedReply = false,
                feedReplyError = null,
                translationActive = false,
                translating = false,
            )
            applyArticle(article, smartEnhanceFromPrefs = false)
        }
    }

    fun prepareFeedReply(item: FeedItem) {
        val feed = _state.value.feed ?: return
        val source = when {
            item.id.startsWith("mail-") -> "imap"
            item.id.startsWith("tgmsg-") -> "telegram"
            else -> return
        }
        _state.value = _state.value.copy(
            replySource = source,
            replyItemId = item.id,
            replyContextId = feed.context_id.takeIf { it.isNotBlank() },
            showFeedReply = true,
            feedReplyError = null,
        )
    }

    fun openFeedReply() {
        _state.value = _state.value.copy(showFeedReply = true, feedReplyError = null)
    }

    fun closeFeedReply() {
        _state.value = _state.value.copy(showFeedReply = false, feedReplyError = null)
    }

    fun sendFeedReply(text: String) {
        val source = _state.value.replySource ?: return
        val itemId = _state.value.replyItemId ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(feedReplySending = true, feedReplyError = null)
            try {
                val res = api().act(
                    ActRequest(
                        source = source,
                        action = "reply",
                        item_id = itemId,
                        body = text,
                        context_id = _state.value.replyContextId,
                    ),
                )
                _state.value = _state.value.copy(
                    feedReplySending = false,
                    showFeedReply = false,
                    gallerySaveMessage = res.message.ifBlank { "Отправлено" },
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    feedReplySending = false,
                    feedReplyError = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun openLink(href: String) {
        val base = _state.value.article?.url ?: _urlInput.value
        val resolved = UrlResolver.resolve(href, base)
        if (resolved.startsWith("mailto:") || resolved.startsWith("tel:")) {
            _state.value = _state.value.copy(error = "Ссылка открывается во внешнем приложении")
            return
        }
        loadUrl(resolved)
    }

    fun openLinkFromWebView(href: String) {
        if (href.startsWith("mailto:") || href.startsWith("tel:")) {
            _state.value = _state.value.copy(error = "Ссылка открывается во внешнем приложении")
            return
        }
        loadUrl(href)
    }

    fun toggleTranslation() {
        val article = _state.value.article ?: return
        if (_state.value.translationActive) {
            val original = originalArticle ?: return
            viewModelScope.launch {
                applyArticle(original, smartEnhanceFromPrefs = false)
                _state.value = _state.value.copy(translationActive = false)
            }
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(translating = true, error = null)
            try {
                if (originalArticle == null) originalArticle = article
                val base = prefs.baseUrl.first()
                val target = prefs.translateTargetLang.first()
                val translated = ArticleTranslator.translateArticle(api(), article, target)
                applyArticle(translated, smartEnhanceFromPrefs = false)
                _state.value = _state.value.copy(
                    translating = false,
                    translationActive = true,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    translating = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    private suspend fun applyArticle(
        article: SaylatArticle,
        useSmartLayout: Boolean = false,
        smartEnhanceFromPrefs: Boolean = false,
    ) {
        if (article.compression_level == CompressionLevel.LIGHT) {
            _state.value = _state.value.copy(
                article = article,
                plan = null,
                pageLoadStats = article.stats,
                layoutEnhancing = false,
                readerUseSmartLayout = false,
            )
            creditPetTrafficSavings(article.url, article.stats)
            petLoadSuccess(article.url, article.stats)
            withContext(Dispatchers.IO) { PageCache.putArticle(appContext, article) }
            refreshOfflineCache()
            recordVisit(article.url, article.title)
            return
        }

        val reader = prefs.readerMode.first()
        val isFull = article.compression_level == CompressionLevel.FULL
        val wantSmart = isFull && (
            useSmartLayout ||
                (smartEnhanceFromPrefs &&
                    prefs.smartLayoutEnabled.first() &&
                    DeviceCapabilities.canRunSmartLayout(appContext))
            )
        val quickPlan = withContext(Dispatchers.Default) {
            val base = SmartLayoutCoordinator.quickRender(article)
            ArticleDisplayEnricher.enrich(base, article, reader)
        }
        _state.value = _state.value.copy(
            article = article,
            plan = quickPlan,
            pageLoadStats = article.stats,
            layoutEnhancing = wantSmart,
            readerUseSmartLayout = wantSmart,
        )
        creditPetTrafficSavings(article.url, article.stats)
        petLoadSuccess(article.url, article.stats)
        if (wantSmart) {
            val enhanced = withContext(Dispatchers.Default) {
                val base = SmartLayoutCoordinator.renderWithSmartLayout(article, useAi = true)
                ArticleDisplayEnricher.enrich(base, article, reader)
            }
            if (_state.value.article?.url == article.url) {
                _state.value = _state.value.copy(plan = enhanced, layoutEnhancing = false)
            }
        }
        withContext(Dispatchers.IO) {
            PageCache.putArticle(appContext, article)
        }
        refreshOfflineCache()
        recordVisit(article.url, article.title)
    }

    private suspend fun effectiveReaderMode(): ReaderMode =
        when (val stored = prefs.readerMode.first()) {
            ReaderMode.AUTO -> {
                val slow = prefs.slowNetworkMode.first() ?: _state.value.slowNetworkMode
                if (slow) ReaderMode.LAYOUT else ReaderMode.STRIPS
            }
            ReaderMode.NATIVE -> ReaderMode.LAYOUT
            ReaderMode.VISUAL -> ReaderMode.STRIPS
            else -> stored
        }

    fun backFromSearch() {
        goHome()
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            prefs.setBaseUrl(url)
            ApiFactory.invalidateCache()
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { prefs.setSearchEngine(engine.id) }
    }

    fun setSearxInstance(url: String) {
        viewModelScope.launch { prefs.setSearxInstance(url) }
    }

    fun setSmartLayoutEnabled(enabled: Boolean) {
        if (enabled && !DeviceCapabilities.canRunSmartLayout(appContext)) return
        viewModelScope.launch { prefs.setSmartLayoutEnabled(enabled) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { prefs.clearRecentSearches() }
    }

    fun checkAndInstallUpdate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                updateChecking = true,
                updateDownloading = false,
                updateStatus = null,
                error = null,
            )
            try {
                when (val check = appUpdateManager.checkForUpdate(_state.value.serverUrl)) {
                    is UpdateCheckResult.UpToDate -> {
                        _state.value = _state.value.copy(
                            updateChecking = false,
                            updateStatus = "У вас ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}). " +
                                "На сервере ${check.serverVersionName} (${check.serverVersionCode}) — обновление не требуется",
                        )
                    }
                    is UpdateCheckResult.Available -> {
                        _state.value = _state.value.copy(
                            updateChecking = false,
                            updateDownloading = true,
                            updateStatus = "Скачиваем ${check.info.version_name}…",
                        )
                        val msg = appUpdateManager.downloadAndInstall(check.info.apk_url)
                        _state.value = _state.value.copy(
                            updateDownloading = false,
                            updateStatus = msg,
                            whatsNewVersion = check.info.version_name,
                            whatsNewNotes = check.info.release_notes,
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    updateChecking = false,
                    updateDownloading = false,
                    updateStatus = null,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun useRecentQuery(query: String) {
        _searchInput.value = query
        search()
    }

    fun openExternalUrl(url: String) {
        val normalized = normalizeUrl(url.trim())
        if (normalized.isBlank()) return
        _urlInput.value = normalized
        loadUrl(normalized)
    }

    fun openQuickLink(url: String) {
        _urlInput.value = url
        loadUrl(url)
    }

    fun openFavorite(url: String) {
        openExternalUrl(url)
    }

    fun removeFavorite(url: String) {
        viewModelScope.launch { prefs.removeFavorite(url) }
    }

    fun toggleFavoriteForCurrentPage() {
        val current = currentFavoriteCandidate() ?: return
        viewModelScope.launch {
            val exists = _state.value.favoriteLinks.any { it.url == current.url }
            if (exists) {
                prefs.removeFavorite(current.url)
            } else {
                prefs.upsertFavorite(current)
            }
        }
    }

    fun pinCurrentPageShortcut() {
        val current = currentFavoriteCandidate() ?: return
        viewModelScope.launch {
            prefs.upsertFavorite(current)
            val ok = HomeShortcutHelper.requestPin(appContext, current.url, current.title)
            _state.value = _state.value.copy(
                gallerySaveMessage = if (ok) {
                    "Ярлык добавлен на рабочий стол"
                } else {
                    "Android не дал закрепить ярлык на рабочем столе"
                },
            )
        }
    }

    fun pinFavoriteShortcut(link: SaylatPrefs.FavoriteLink) {
        viewModelScope.launch {
            val ok = HomeShortcutHelper.requestPin(appContext, link.url, link.title)
            _state.value = _state.value.copy(
                gallerySaveMessage = if (ok) {
                    "Ярлык «${link.title}» добавлен"
                } else {
                    "Не удалось добавить ярлык «${link.title}»"
                },
            )
        }
    }

    fun search(query: String? = null) {
        val raw = (query ?: _searchInput.value).trim()
        _searchInput.value = raw
        if (_state.value.error != null) {
            _state.value = _state.value.copy(error = null)
        }
        if (raw.isBlank()) {
            _state.value = _state.value.copy(error = "Введите запрос")
            return
        }
        if (looksLikeUrl(raw)) {
            loadUrl(normalizeUrl(raw))
            return
        }

        petNotify(PetBrowserAction.SearchStart(raw))
        viewModelScope.launch {
            _state.value = _state.value.copy(
                searching = true,
                error = null,
                searchResults = emptyList(),
                activeSearchQuery = raw,
                screen = AppScreen.SEARCH_RESULTS,
            )
            try {
                val engineResolved = _state.value.searchEngine
                val searx = prefs.searxInstanceUrl.first()
                val proxy = prefs.baseUrl.first()
                val slow = effectiveSlowNetwork()
                val hits = searchRepository.search(
                    raw,
                    engineResolved,
                    searx,
                    proxyBaseUrl = proxy,
                    slowNetwork = slow,
                )
                prefs.pushRecentSearch(raw)
                _searchInput.value = raw
                _state.value = _state.value.copy(
                    searching = false,
                    searchResults = hits,
                )
                petNotify(PetBrowserAction.SearchDone(hits.size))
                if (hits.isEmpty()) {
                    _state.value = _state.value.copy(error = "Ничего не найдено. Смените движок или инстанс SearXNG.")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    searching = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    fun openSearchResult(hit: SearchHit) {
        loadUrl(hit.url)
    }

    private fun currentFavoriteCandidate(): SaylatPrefs.FavoriteLink? {
        val article = _state.value.article
        val stripPage = _state.value.stripPage
        val webViewUrl = _state.value.webViewUrl?.trim().orEmpty()
        val rawUrl = when {
            !article?.url.isNullOrBlank() -> article?.url.orEmpty()
            !stripPage?.url.isNullOrBlank() -> stripPage?.url.orEmpty()
            webViewUrl.startsWith("http://") || webViewUrl.startsWith("https://") -> webViewUrl
            else -> _urlInput.value.trim()
        }
        val normalized = normalizeUrl(rawUrl)
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) return null
        val title = article?.title
            ?: stripPage?.title
            ?: _state.value.activeSearchQuery
            ?: normalized
        return SaylatPrefs.FavoriteLink(
            title = title.ifBlank { normalized },
            url = normalized,
        )
    }


    private fun openTarget(
        target: String,
        url: String? = null,
        resourceId: String? = null,
        keepFeedParent: Boolean = false,
    ) {
        viewModelScope.launch {
            openTargetSuspend(target, url, resourceId, keepFeedParent)
        }
    }

    private suspend fun openTargetSuspend(
        target: String,
        url: String? = null,
        resourceId: String? = null,
        keepFeedParent: Boolean = false,
    ) {
        originalArticle = null
        val parentFeed = if (keepFeedParent) _state.value.feed else null
        val nextScreen = if (target == "url") AppScreen.READER else AppScreen.FEED
        url?.let { petLoadStart(it) }
        _state.value = _state.value.copy(
            screen = nextScreen,
            loading = true,
            layoutEnhancing = false,
            error = null,
            cachedNotice = null,
            article = null,
            plan = null,
            feed = parentFeed,
            translationActive = false,
            translating = false,
        )
        try {
            val base = prefs.baseUrl.first()
            val slow = effectiveSlowNetwork()
            val level = compressionLevel()
            val apiKey = BuildConfig.PROXY_API_KEY
            val client = ApiFactory.httpClient(base, slow, level, apiKey)
            val response = api().fetchOpen(
                context = appContext,
                client = client,
                baseUrl = base,
                request = OpenRequest(
                    target = target,
                    url = url,
                    resource_id = resourceId,
                    images = extractImagesMode(),
                    level = level,
                ),
                slowNetwork = slow,
                apiKey = apiKey,
                onPartial = { partial ->
                    _urlInput.value = partial.url.ifBlank { url ?: _urlInput.value }
                    _state.value = _state.value.copy(
                        screen = AppScreen.READER,
                        loading = true,
                        article = partial,
                        pageLoadStats = partial.stats,
                    )
                },
            )
            applyOpenResponse(response, urlInput = url ?: _urlInput.value)
        } catch (e: Exception) {
            val pageUrl = url?.trim().orEmpty()
            val cached = if (target == "url" && pageUrl.startsWith("http")) {
                withContext(Dispatchers.IO) { PageCache.loadArticle(appContext, pageUrl) }
            } else {
                null
            }
            if (cached != null) {
                _urlInput.value = cached.url
                _state.value = _state.value.copy(
                    screen = AppScreen.READER,
                    loading = false,
                    cachedNotice = "Статья из офлайн-кэша",
                    error = null,
                )
                applyArticle(cached, smartEnhanceFromPrefs = false)
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    layoutEnhancing = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    private fun loadUrl(url: String, keepFeedParent: Boolean = false) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _state.value = _state.value.copy(error = "Некорректный URL")
            return
        }
        val normalized = SaylatUserAgents.normalizeFetchUrl(url)
        if (looksLikeRssFeedUrl(normalized)) {
            openRssFeed(normalized)
            return
        }
        _urlInput.value = normalized
        _state.value = _state.value.copy(
            screen = AppScreen.READER,
            error = null,
            replySource = null,
            replyItemId = null,
            replyContextId = null,
            showFeedReply = false,
        )
        viewModelScope.launch {
            when (effectiveReaderMode()) {
                ReaderMode.WEBVIEW -> loadWebView(normalized, keepFeedParent)
                ReaderMode.STRIPS -> loadStrips(normalized, keepFeedParent)
                ReaderMode.LAYOUT, ReaderMode.NATIVE -> openTarget(
                    "url",
                    url = normalized,
                    keepFeedParent = keepFeedParent,
                )
                ReaderMode.AUTO -> loadAuto(normalized, keepFeedParent)
                ReaderMode.VISUAL -> loadStrips(normalized, keepFeedParent)
            }
        }
    }

    private suspend fun loadAuto(url: String, keepFeedParent: Boolean) {
        try {
            openTargetSuspend("url", url = url, keepFeedParent = keepFeedParent)
            if (isWeakArticle(_state.value.article)) {
                loadWebView(url, keepFeedParent)
            }
        } catch (_: Exception) {
            loadWebView(url, keepFeedParent)
        }
    }

    private suspend fun loadStrips(url: String, keepFeedParent: Boolean) {
        originalArticle = null
        val parentFeed = if (keepFeedParent) _state.value.feed else null
        lastSaladCreditKey = null
        petLoadStart(url)
        _state.value = _state.value.copy(
            screen = AppScreen.READER,
            loading = true,
            layoutEnhancing = false,
            error = null,
            cachedNotice = null,
            article = null,
            plan = null,
            visualPage = null,
            stripPage = null,
            webViewUrl = null,
            webViewLoading = false,
            feed = parentFeed,
            pageLoadStats = null,
            gallerySaveMessage = null,
        )
        try {
            val page = api().renderStrips(url, images = extractImagesMode())
            withContext(Dispatchers.IO) { PageCache.putStripPage(appContext, page) }
            refreshOfflineCache()
            val loadStats = ArticleStats(
                original_bytes = page.stats.original_bytes,
                payload_bytes = page.stats.payload_bytes,
                fetch_ms = page.stats.fetch_ms,
            )
            _state.value = _state.value.copy(
                loading = false,
                stripPage = page,
                pageLoadStats = loadStats,
            )
            creditPetTrafficSavings(url, loadStats)
            petLoadSuccess(url, loadStats)
            recordVisit(url, page.title.ifBlank { url })
        } catch (e: Exception) {
            val cached = withContext(Dispatchers.IO) { PageCache.loadStripPage(appContext, url) }
            if (cached != null) {
                val loadStats = ArticleStats(
                    original_bytes = cached.stats.original_bytes,
                    payload_bytes = cached.stats.payload_bytes,
                    fetch_ms = cached.stats.fetch_ms,
                )
                _state.value = _state.value.copy(
                    loading = false,
                    stripPage = cached,
                    cachedNotice = "Полосы из офлайн-кэша",
                    pageLoadStats = loadStats,
                )
                creditPetTrafficSavings(url, loadStats)
                petLoadSuccess(url, loadStats)
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    private suspend fun loadWebView(url: String, keepFeedParent: Boolean) {
        originalArticle = null
        val parentFeed = if (keepFeedParent) _state.value.feed else null
        recordVisit(url, url)
        _state.value = _state.value.copy(
            screen = AppScreen.READER,
            loading = false,
            layoutEnhancing = false,
            article = null,
            plan = null,
            visualPage = null,
            stripPage = null,
            webViewUrl = url,
            webViewLoading = true,
            pageLoadStats = null,
            feed = parentFeed,
            translationActive = false,
            translating = false,
        )
    }

    private suspend fun loadVisual(
        url: String,
        keepFeedParent: Boolean,
        allowWebViewFallback: Boolean,
    ) {
        originalArticle = null
        val parentFeed = if (keepFeedParent) _state.value.feed else null
        _state.value = _state.value.copy(
            screen = AppScreen.READER,
            loading = true,
            layoutEnhancing = false,
            error = null,
            article = null,
            plan = null,
            webViewUrl = null,
            webViewLoading = false,
            visualPage = null,
            feed = parentFeed,
            translationActive = false,
            translating = false,
        )
        try {
            val page = api().renderVisual(url, images = extractImagesMode())
            if (allowWebViewFallback && VisualMapper.looksEmpty(page)) {
                fallbackNativeThenWebView(url, keepFeedParent)
                return
            }
            val article = VisualMapper.toArticle(page)
            _state.value = _state.value.copy(loading = false, visualPage = page)
            applyArticle(article, smartEnhanceFromPrefs = true)
        } catch (e: Exception) {
            if (allowWebViewFallback) {
                fallbackNativeThenWebView(url, keepFeedParent)
            } else {
                _state.value = _state.value.copy(
                    loading = false,
                    error = UserFacingErrors.from(e),
                )
            }
        }
    }

    private suspend fun fallbackNativeThenWebView(url: String, keepFeedParent: Boolean) {
        try {
            openTarget("url", url = url, keepFeedParent = keepFeedParent)
            if (isWeakArticle(_state.value.article)) {
                loadWebView(url, keepFeedParent)
            }
        } catch (_: Exception) {
            loadWebView(url, keepFeedParent)
        }
    }

    private fun isWeakArticle(article: SaylatArticle?): Boolean {
        if (article == null) return true
        val paras = article.blocks.filter { it.type == "paragraph" && !it.text.isNullOrBlank() }
        if (paras.isEmpty() && article.blocks.none { it.type == "image" }) return true
        val text = paras.joinToString(" ") { it.text.orEmpty() }.lowercase()
        return text.contains("не удалось извлечь") || text.contains("страница пуста")
    }

    private suspend fun applyOpenResponse(response: com.baddysays.saylat.data.OpenResponse, urlInput: String) {
        val expanded = if (response.wire != null) {
            PayloadCodec.expandOpenResponse(response)
        } else {
            response
        }
        _urlInput.value = urlInput
        _state.value = _state.value.copy(loading = false)
        when (expanded.kind) {
            "feed" -> {
                val feed = expanded.feed
                if (feed != null) {
                    _state.value = _state.value.copy(
                        screen = AppScreen.FEED,
                        feed = feed,
                        article = null,
                        plan = null,
                        layoutEnhancing = false,
                    )
                } else {
                    _state.value = _state.value.copy(error = "Пустой ответ ленты")
                }
            }
            else -> {
                val article = expanded.article
                if (article != null) {
                    _state.value = _state.value.copy(screen = AppScreen.READER)
                    applyArticle(article, smartEnhanceFromPrefs = true)
                } else {
                    _state.value = _state.value.copy(error = "Пустой ответ")
                }
            }
        }
    }

    private fun looksLikeUrl(raw: String): Boolean {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return true
        return raw.contains(".") && !raw.contains(" ")
    }

    private fun normalizeUrl(raw: String): String = when {
        raw.startsWith("http://") || raw.startsWith("https://") -> raw
        raw.contains(".") -> "https://$raw"
        else -> raw
    }

    private fun looksLikeRssFeedUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".xml") ||
            lower.endsWith(".rss") ||
            lower.endsWith(".atom") ||
            "/rss" in lower ||
            "/feed" in lower ||
            "/atom" in lower ||
            "format=rss" in lower ||
            "feeds/posts" in lower
    }
}
