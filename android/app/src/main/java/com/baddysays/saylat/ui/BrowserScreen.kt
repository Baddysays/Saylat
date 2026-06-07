package com.baddysays.saylat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.baddysays.saylat.engine.CardKind
import com.baddysays.saylat.engine.RenderCard
import com.baddysays.saylat.data.filterHistorySuggestions
import com.baddysays.saylat.tts.TtsStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val screen by viewModel.screen.collectAsState()
    val state by viewModel.state.collectAsState()
    val trafficSavedToday by viewModel.trafficSavedToday.collectAsState()
    val readLaterItems by viewModel.readLaterItems.collectAsState()
    val historyEntries by viewModel.historyEntries.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val readerUi by viewModel.readerUi.collectAsState()
    val searchSeed by viewModel.searchInput.collectAsState()
    val urlSeed by viewModel.urlInput.collectAsState()
    val currentFavoriteUrl = remember(state.article, state.stripPage, state.webViewUrl, urlSeed) {
        when {
            !state.article?.url.isNullOrBlank() -> state.article?.url
            !state.stripPage?.url.isNullOrBlank() -> state.stripPage?.url
            !state.webViewUrl.isNullOrBlank() -> state.webViewUrl
            urlSeed.startsWith("http://") || urlSeed.startsWith("https://") -> urlSeed
            else -> null
        }
    }
    val isFavorite = currentFavoriteUrl != null && state.favoriteLinks.any { it.url == currentFavoriteUrl }
    val isReadLater = currentFavoriteUrl != null && readLaterItems.any { it.url == currentFavoriteUrl }
    val readerListState = rememberLazyListState()
    val readerScope = rememberCoroutineScope()
    var showToc by remember { mutableStateOf(false) }
    var articleSearch by remember { mutableStateOf(ArticleSearchState()) }
    val tocEntries = remember(state.article) {
        state.article?.blocks?.let { buildToc(it as List<*>, startOffset = 1) } ?: emptyList()
    }
    val articleSearchTexts = remember(state.article, state.plan) {
        state.article?.blocks?.mapNotNull { it.text?.takeIf { t -> t.isNotBlank() } }
            ?: state.plan?.cards?.map { card ->
                listOfNotNull(
                    card.title.takeIf { it.isNotBlank() },
                    card.body.takeIf { it.isNotBlank() },
                ).joinToString(" ")
            }?.filter { it.isNotBlank() }
            ?: emptyList()
    }
    val readerListTotalItems = remember(state.article, state.plan, articleSearchTexts) {
        when {
            state.plan != null -> state.plan?.cards?.size ?: 0
            state.article != null -> articleSearchTexts.size.coerceAtLeast(1)
            else -> 0
        }
    }
    LaunchedEffect(articleSearch.currentMatch, articleSearch.query, articleSearch.totalMatches) {
        if (articleSearch.query.isBlank() || articleSearch.totalMatches <= 0) return@LaunchedEffect
        val positions = findMatchPositions(articleSearchTexts, articleSearch.query)
        positions.getOrNull(articleSearch.currentMatch)?.first?.let { idx ->
            readerListState.animateScrollToItem(idx.coerceAtLeast(0))
        }
    }
    val petController = rememberTamagotchiController(
        onAwardXp = viewModel::awardPetXp,
        onSpendSalad = viewModel::spendPetSalad,
        onHatchEgg = viewModel::hatchPetEgg,
        restorePhase = viewModel.restorePetPhase(urlSeed),
    )
    val readerBusy = state.loading ||
        state.layoutEnhancing ||
        state.translating ||
        (state.webViewUrl != null && state.webViewLoading)
    val readerLoadFailed = !readerBusy && !state.error.isNullOrBlank()
    val showTamagotchiOverlay = screen == AppScreen.READER && state.tamagotchiEnabled &&
        (
            readerBusy ||
                petController.phase == PetPhase.Waiting ||
                petController.phase == PetPhase.Active ||
                petController.phase == PetPhase.PageReady ||
                petController.phase == PetPhase.LoadFailed
            )

    val interceptBack = state.showLayoutLab ||
        state.showFeedReply ||
        state.showPetShop ||
        state.showSettings ||
        state.showWhatsNew ||
        state.showWelcome ||
        state.showReadLater ||
        (showTamagotchiOverlay && petController.interactionExpanded) ||
        screen != AppScreen.HOME

    val searchFocusRequester = remember { FocusRequester() }
    val searchFocusTick = remember { mutableIntStateOf(0) }
    LaunchedEffect(searchFocusTick.intValue) {
        if (searchFocusTick.intValue > 0) {
            searchFocusRequester.requestFocus()
        }
    }

    BackHandler(enabled = interceptBack) {
        when {
            state.showLayoutLab -> viewModel.closeLayoutLab()
            state.showFeedReply -> viewModel.closeFeedReply()
            state.showPetShop -> viewModel.closePetShop()
            state.showSettings -> viewModel.closeSettings()
            state.showReadLater -> viewModel.closeReadLater()
            state.showWhatsNew -> viewModel.dismissWhatsNew()
            state.showWelcome -> viewModel.dismissWelcome()
            showTamagotchiOverlay && petController.interactionExpanded -> petController.collapseInteraction()
            screen == AppScreen.READER -> viewModel.backFromReader()
            screen == AppScreen.SEARCH_RESULTS -> viewModel.backFromSearch()
            screen == AppScreen.FEED -> viewModel.backFromFeed()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.readerToast) {
        val msg = state.readerToast ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearReaderToast()
    }

    LaunchedEffect(state.petBrowserCue) {
        val cue = state.petBrowserCue ?: return@LaunchedEffect
        if (state.tamagotchiEnabled) {
            petController.sayBrowserLine(cue.line)
        }
        viewModel.consumePetBrowserCue()
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SaylatTopBar(
                screen = screen,
                activeQuery = state.activeSearchQuery,
                networkOnline = state.networkOnline,
                onBack = when (screen) {
                    AppScreen.SEARCH_RESULTS -> viewModel::backFromSearch
                    AppScreen.READER -> viewModel::backFromReader
                    AppScreen.FEED -> viewModel::backFromFeed
                    AppScreen.HOME -> viewModel::goHome
                },
                onHome = viewModel::goHome,
                onSettings = viewModel::openSettings,
                onSearchFocus = if (screen == AppScreen.HOME) {
                    { searchFocusTick.intValue += 1 }
                } else {
                    null
                },
                translationActive = state.translationActive,
                translating = state.translating,
                showTranslate = screen == AppScreen.READER && state.article != null,
                onToggleTranslate = viewModel::toggleTranslation,
                showGallerySave = screen == AppScreen.READER &&
                    !state.loading &&
                    state.webViewUrl == null &&
                    (state.article != null || state.stripPage != null),
                gallerySaveInProgress = state.savingGallery,
                onSaveGallery = viewModel::saveCurrentPageToGallery,
                showFavoriteActions = screen == AppScreen.READER && currentFavoriteUrl != null,
                isFavorite = isFavorite,
                onToggleFavorite = viewModel::toggleFavoriteForCurrentPage,
                onPinShortcut = viewModel::pinCurrentPageShortcut,
                showShare = screen == AppScreen.READER && currentFavoriteUrl != null,
                onShare = viewModel::shareCurrentPage,
                readLaterCount = readLaterItems.size,
                onOpenReadLater = if (screen == AppScreen.HOME) viewModel::openReadLater else null,
                showReadLaterAction = screen == AppScreen.READER && state.article != null,
                isReadLater = isReadLater,
                onToggleReadLater = viewModel::toggleReadLater,
                showToc = screen == AppScreen.READER && tocEntries.isNotEmpty(),
                onTocClick = { showToc = true },
                showArticleSearch = screen == AppScreen.READER &&
                    state.article != null &&
                    state.webViewUrl == null &&
                    state.stripPage == null,
                articleSearchActive = articleSearch.visible,
                onArticleSearchClick = {
                    articleSearch = if (articleSearch.visible) {
                        ArticleSearchState()
                    } else {
                        articleSearch.copy(visible = true)
                    }
                },
                showTts = screen == AppScreen.READER && state.article != null && state.webViewUrl == null,
                onTtsClick = {
                    if (ttsState.status == TtsStatus.PAUSED) viewModel.resumeTts() else viewModel.startTts()
                },
                tamagotchiEnabled = state.tamagotchiEnabled,
                petProfile = state.petProfile,
                uiLanguage = state.uiLanguage,
            )
        },
        bottomBar = {
            Column {
                if (screen == AppScreen.READER && state.webViewUrl == null) {
                    InArticleSearchBar(
                        state = articleSearch,
                        onQueryChange = { query ->
                            val total = countMatches(articleSearchTexts, query)
                            articleSearch = articleSearch.copy(
                                query = query,
                                totalMatches = total,
                                currentMatch = 0,
                            )
                        },
                        onNext = {
                            if (articleSearch.totalMatches > 0) {
                                articleSearch = articleSearch.copy(
                                    currentMatch = (articleSearch.currentMatch + 1) % articleSearch.totalMatches,
                                )
                            }
                        },
                        onPrev = {
                            if (articleSearch.totalMatches > 0) {
                                articleSearch = articleSearch.copy(
                                    currentMatch = (articleSearch.currentMatch - 1 + articleSearch.totalMatches) %
                                        articleSearch.totalMatches,
                                )
                            }
                        },
                        onClose = { articleSearch = ArticleSearchState() },
                    )
                    TtsControlBar(
                        state = ttsState,
                        onPlay = viewModel::startTts,
                        onPause = viewModel::pauseTts,
                        onStop = viewModel::stopTts,
                        onNext = viewModel::nextTts,
                        onPrev = viewModel::prevTts,
                    )
                }
            val bottomValue = if (screen == AppScreen.READER || screen == AppScreen.FEED) {
                urlSeed
            } else {
                searchSeed
            }
            val historySuggestions = remember(bottomValue, historyEntries, screen) {
                if (screen == AppScreen.READER || screen == AppScreen.FEED) emptyList()
                else filterHistorySuggestions(bottomValue, historyEntries)
            }
            val speedMode = QuickSpeedMode.fromFlags(state.slowNetworkMode, state.liteImagesEnabled)
            BottomSearchBar(
                externalValue = bottomValue,
                searchEngine = state.searchEngine,
                enabled = !state.searching && !state.loading,
                speedMode = speedMode,
                onSpeedModeChange = viewModel::setQuickSpeedMode,
                onSearch = viewModel::search,
                focusRequester = searchFocusRequester,
                uiLanguage = state.uiLanguage,
                historySuggestions = historySuggestions,
                onHistorySelect = viewModel::openExternalUrl,
            )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OfflineBanner(
                isOnline = state.networkOnline,
                hasOfflineCache = state.offlineCacheEntries.isNotEmpty(),
            )

            if (
                screen == AppScreen.READER &&
                state.article != null &&
                !state.loading &&
                state.webViewUrl == null
            ) {
                calculateReadingTime(state.article!!.blocks as List<*>)?.let { readingTime ->
                    Text(
                        readingTime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }

            if (screen == AppScreen.READER && state.replyItemId != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    FilledTonalButton(onClick = viewModel::openFeedReply) {
                        Text("Ответить")
                    }
                }
            }

            if (
                screen == AppScreen.READER &&
                state.replyItemId == null &&
                state.webViewUrl == null &&
                state.stripPage == null &&
                state.article != null &&
                state.readerMode == com.baddysays.saylat.prefs.ReaderMode.LAYOUT
            ) {
                ReaderLayoutBar(
                    useSmart = state.readerUseSmartLayout,
                    smartAvailable = state.smartLayoutAvailable,
                    enhancing = state.layoutEnhancing,
                    uiLanguage = state.uiLanguage,
                    onSelectBaseline = { viewModel.setReaderLayoutMode(useSmart = false) },
                    onSelectSmart = { viewModel.setReaderLayoutMode(useSmart = true) },
                    onSmartUnavailable = {
                        viewModel.showReaderToast(
                            com.baddysays.saylat.ui.strings.SaylatStrings.smartLayoutUnavailableToast(
                                state.uiLanguage,
                            ),
                        )
                    },
                )
            }

            if (
                screen == AppScreen.READER &&
                state.readerMode == com.baddysays.saylat.prefs.ReaderMode.STRIPS &&
                state.smartLayoutEnabled
            ) {
                Text(
                    com.baddysays.saylat.ui.strings.SaylatStrings.smartLayoutNeedsTextMode(state.uiLanguage),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }

            if (screen == AppScreen.READER || screen == AppScreen.FEED) {
                state.cachedNotice?.let { notice ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ) {
                        Text(
                            notice,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                if (state.showPageLoadStats && screen == AppScreen.READER) {
                    val stats = state.pageLoadStats ?: state.article?.stats
                    val dismissed = state.dismissedReaderBanners
                    when {
                        state.webViewUrl != null -> ReaderWebViewModeBanner(
                            dismissedIds = dismissed,
                            onDismiss = viewModel::dismissReaderBanner,
                        )
                        stats != null && state.stripPage == null && !state.loading -> ReaderPageLoadSummary(
                            stats = stats,
                            modeDetail = com.baddysays.saylat.engine.ArticleDisplayEnricher.statsDetailForMode(
                                stats,
                                state.readerMode,
                            ),
                            dismissedIds = dismissed,
                            onDismiss = viewModel::dismissReaderBanner,
                        )
                    }
                }
            }

            if (state.layoutEnhancing) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        "Улучшаем вёрстку локально…",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            state.error?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            state.gallerySaveMessage
                ?.takeIf { it.isNotBlank() && (screen != AppScreen.READER || state.stripPage == null) }
                ?.let { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                androidx.compose.runtime.key(screen) {
                when (screen) {
                    AppScreen.HOME -> HomeContent(
                        searchEngine = state.searchEngine,
                        petProfile = state.petProfile,
                        tamagotchiEnabled = state.tamagotchiEnabled,
                        onPetCare = viewModel::petHomeCare,
                        onOpenPetShop = viewModel::openPetShop,
                        onHatchEgg = viewModel::hatchPetEgg,
                        recentSearches = state.recentSearches,
                        serverUrl = state.serverUrl,
                        serverReady = state.serverReady,
                        serverStatusMessage = state.serverStatusMessage,
                        networkTesting = state.networkTesting,
                        networkTestResult = state.networkTestResult,
                        onRefreshServerStatus = viewModel::refreshServerStatus,
                        smartLayoutAvailable = state.smartLayoutAvailable,
                        uiLanguage = state.uiLanguage,
                        onQuickLink = viewModel::openQuickLink,
                        onRecent = viewModel::useRecentQuery,
                        onRunNetworkTest = viewModel::runNetworkTest,
                        onDismissNetworkTest = viewModel::dismissNetworkTest,
                        connectStatus = state.connectStatus,
                        onService = viewModel::openService,
                        onOpenServiceSettings = viewModel::openServiceSettings,
                        onOpenSearchSettings = viewModel::openNetworkSettings,
                        slowNetworkMode = state.slowNetworkMode,
                        liteImagesEnabled = state.liteImagesEnabled,
                        onSpeedModeChange = viewModel::setQuickSpeedMode,
                        favorites = state.favoriteLinks,
                        visitHistory = state.visitHistory,
                        historyEntries = historyEntries,
                        onOpenVisit = viewModel::openFavorite,
                        onOpenFavorite = viewModel::openFavorite,
                        onRemoveFavorite = viewModel::removeFavorite,
                        onPinFavorite = viewModel::pinFavoriteShortcut,
                        offlineCache = state.offlineCacheEntries,
                        onOpenCached = viewModel::openCachedPage,
                        trafficSavedToday = trafficSavedToday,
                    )
                    AppScreen.SEARCH_RESULTS -> SearchResultsList(
                        query = state.activeSearchQuery.orEmpty(),
                        results = state.searchResults,
                        searching = state.searching,
                        onOpen = viewModel::openSearchResult,
                    )
                    AppScreen.READER -> {
                        Box(Modifier.fillMaxSize()) {
                            SwipeBackContainer(onBack = viewModel::backFromReader) {
                                PullToRefreshWrapper(
                                    isRefreshing = state.loading &&
                                        (readerUi.article != null || readerUi.stripPage != null),
                                    onRefresh = { viewModel.reloadCurrentUrl(invalidateCache = true) },
                                    modifier = Modifier.fillMaxSize(),
                                ) {
                                    val webUrl = readerUi.webViewUrl
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .readerBackground(state.readerTheme),
                                    ) {
                                        when {
                                            webUrl != null -> SiteWebView(
                                                pageUrl = webUrl,
                                                onExternalUrl = viewModel::openLinkFromWebView,
                                                onLoadingChange = viewModel::setWebViewLoading,
                                            )
                                            readerUi.stripPage != null ||
                                                readerUi.readerMode == com.baddysays.saylat.prefs.ReaderMode.STRIPS -> {
                                                StripReaderScreen(
                                                    loading = readerUi.loading,
                                                    stripPage = readerUi.stripPage,
                                                    pageUrl = urlSeed,
                                                    fromCache = readerUi.cachedNotice != null,
                                                    tamagotchiEnabled = state.tamagotchiEnabled,
                                                    uiLanguage = state.uiLanguage,
                                                    saveInProgress = state.savingGallery,
                                                    saveMessage = state.gallerySaveMessage,
                                                    onSaveStrips = viewModel::saveCurrentPageToGallery,
                                                    onSwitchToReader = {
                                                        viewModel.reloadWithMode(
                                                            com.baddysays.saylat.prefs.ReaderMode.LAYOUT,
                                                        )
                                                    },
                                                    onOpenLink = viewModel::openLink,
                                                )
                                            }
                                            else -> ReaderBody(
                                                loading = readerUi.loading,
                                                translating = readerUi.translating,
                                                article = readerUi.article,
                                                plan = readerUi.plan,
                                                showStatsCards = true,
                                                showLoadingSpinner = !state.tamagotchiEnabled,
                                                listState = readerListState,
                                                readerTheme = state.readerTheme,
                                                fontSettings = state.readerFontSettings,
                                                articleSearch = articleSearch,
                                                serverBaseUrl = state.serverUrl,
                                                pageUrl = urlSeed,
                                                proxyRemoteImages = readerUi.article?.blocks?.any { b ->
                                                    b.type == "image" &&
                                                        com.baddysays.saylat.data.ImageProxyUrl
                                                            .shouldProxy(b.src)
                                                } == true,
                                                onLinkClick = viewModel::openLink,
                                                onLoadFull = {
                                                    viewModel.reloadWithMode(
                                                        com.baddysays.saylat.prefs.ReaderMode.LAYOUT,
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            if (
                                !state.loading &&
                                readerListTotalItems > 0 &&
                                state.webViewUrl == null &&
                                state.stripPage == null
                            ) {
                                ReadingProgressBar(
                                    listState = readerListState,
                                    totalItems = readerListTotalItems,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .zIndex(2f),
                                )
                            }
                        }
                    }
                    AppScreen.FEED -> {
                        PullToRefreshWrapper(
                            isRefreshing = state.loading || state.feedLoadingMore,
                            onRefresh = viewModel::reloadFeed,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                        val feed = state.feed
                        if (state.loading) {
                            FeedLoadingSkeleton()
                        } else if (feed != null) {
                            FeedScreen(
                                feed = feed,
                                onOpenItem = viewModel::openFeedItem,
                                onOpenLink = viewModel::openLink,
                                onReplyItem = viewModel::prepareFeedReply,
                                onOpenServiceSettings = viewModel::openServiceSettings,
                                hasMore = feed.has_more,
                                loadingMore = state.feedLoadingMore,
                                onLoadMore = viewModel::loadMoreFeed,
                            )
                        }
                        }
                    }
                }
                }

            }
        }
    }

    if (showTamagotchiOverlay) {
        TamagotchiReaderLayer(
            controller = petController,
            loading = state.loading,
            readerBusy = readerBusy,
            loadFailed = readerLoadFailed,
            skipReadyGate = state.petSkipReadyGate,
            onHatchEgg = viewModel::hatchPetEgg,
            onSessionChange = viewModel::persistPetSession,
            onSkipReadyGateChange = viewModel::setPetSkipReadyGate,
            enabled = state.tamagotchiEnabled,
            loadKey = urlSeed,
            url = urlSeed,
            profile = state.petProfile,
            uiLanguage = state.uiLanguage,
            suppressReadySpeech = state.suppressPetReadySpeech,
            overlayGate = false,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(12f),
        )
    }

    WelcomeSheet(
        visible = state.showWelcome,
        needsServerUrl = state.needsServerUrlSetup,
        serverUrlDraft = state.welcomeServerDraft,
        onServerUrlChange = viewModel::setWelcomeServerDraft,
        serverReady = state.serverReady,
        onStart = viewModel::dismissWelcome,
    )

    if (state.showWhatsNew) {
        WhatsNewSheet(
            visible = true,
            versionName = state.whatsNewVersion.ifBlank { com.baddysays.saylat.BuildConfig.VERSION_NAME },
            releaseNotes = state.whatsNewNotes,
            onDismiss = viewModel::dismissWhatsNew,
        )
    }

    com.baddysays.saylat.ui.pet.PetShopSheet(
        visible = state.showPetShop,
        profile = state.petProfile,
        shopMessage = state.petShopMessage,
        onDismiss = viewModel::closePetShop,
        onBuy = viewModel::buyPetShopItem,
        onEquip = viewModel::equipPetShopItem,
        onUnequipToys = viewModel::unequipPetToys,
    )

    if (state.showSettings) {
        SettingsSheet(
            visible = true,
            serverUrl = state.serverUrl,
            searchEngine = state.searchEngine,
            searxInstanceUrl = state.searxInstanceUrl,
            smartLayoutEnabled = state.smartLayoutEnabled,
            smartLayoutAvailable = state.smartLayoutAvailable,
            smartLayoutHint = state.smartLayoutHint,
            onDismiss = viewModel::closeSettings,
            onSaveServer = viewModel::updateServerUrl,
            onSearchEngine = viewModel::setSearchEngine,
            onSearxInstance = viewModel::setSearxInstance,
            onSmartLayoutChange = viewModel::setSmartLayoutEnabled,
            onClearRecent = viewModel::clearRecentSearches,
            updateChecking = state.updateChecking,
            updateDownloading = state.updateDownloading,
            updateStatus = state.updateStatus,
            onCheckUpdate = viewModel::checkAndInstallUpdate,
            translateTargetLang = state.translateTargetLang,
            onTranslateTarget = viewModel::setTranslateTargetLang,
            appTheme = state.appTheme,
            uiLanguage = state.uiLanguage,
            onUiLanguage = viewModel::setUiLanguage,
            onAppTheme = viewModel::setAppTheme,
            networkTesting = state.networkTesting,
            networkTestResult = state.networkTestResult,
            onRunNetworkTest = viewModel::runNetworkTest,
            slowNetworkMode = state.slowNetworkMode,
            deviceProfile = state.deviceProfile,
            onSlowNetworkChange = viewModel::setSlowNetworkMode,
            liteImagesEnabled = state.liteImagesEnabled,
            onLiteImagesChange = viewModel::setLiteImagesEnabled,
            readerMode = state.readerMode,
            onReaderModeChange = viewModel::setReaderMode,
            showPageLoadStats = state.showPageLoadStats,
            onPageLoadStatsChange = viewModel::setPageLoadStatsEnabled,
            tamagotchiEnabled = state.tamagotchiEnabled,
            onTamagotchiChange = viewModel::setTamagotchiEnabled,
            petSkipReadyGate = state.petSkipReadyGate,
            onPetSkipReadyGateChange = viewModel::setPetSkipReadyGate,
            petProfile = state.petProfile,
            onPetNameChange = viewModel::setPetName,
            onOpenPetShop = {
                viewModel.closeSettings()
                viewModel.openPetShop()
            },
            speedMode = QuickSpeedMode.fromFlags(state.slowNetworkMode, state.liteImagesEnabled),
            onSpeedModeChange = viewModel::setQuickSpeedMode,
            connectStatus = state.connectStatus,
            credentialsDraft = state.credentialsDraft,
            onCredentialsDraftChange = viewModel::setCredentialsDraft,
            credentialsLoading = state.credentialsLoading,
            credentialsSaving = state.credentialsSaving,
            credentialsMessage = state.credentialsMessage,
            telegramCodeSent = state.telegramCodeSent,
            onSaveCredentials = viewModel::saveServiceCredentials,
            onTelegramRequestCode = viewModel::requestTelegramCode,
            onTelegramSignIn = viewModel::signInTelegram,
            connectLoading = state.connectLoading,
            settingsTab = state.settingsTab,
            onSettingsTabChange = viewModel::setSettingsTab,
            cacheStats = state.cacheStats,
            onClearAppCache = viewModel::clearAppCache,
            customServerEnabled = state.customServerEnabled,
            serverReady = state.serverReady,
            onCustomServerChange = viewModel::setCustomServerEnabled,
            readerTheme = state.readerTheme,
            onReaderThemeChange = viewModel::setReaderTheme,
            readerFontSettings = state.readerFontSettings,
            onReaderFontSettingsChange = viewModel::setReaderFontSettings,
        )
    }

    if (state.showReadLater) {
        val readLaterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = viewModel::closeReadLater,
            sheetState = readLaterSheetState,
        ) {
            ReadLaterScreen(
                items = readLaterItems,
                onOpen = { url ->
                    viewModel.closeReadLater()
                    viewModel.openFavorite(url)
                },
                onRemove = viewModel::removeReadLater,
            )
        }
    }

    if (showToc) {
        TableOfContentsSheet(
            entries = tocEntries,
            listState = readerListState,
            scope = readerScope,
            onDismiss = { showToc = false },
        )
    }

    MailReplySheet(
        visible = state.showFeedReply,
        title = when (state.replySource) {
            "telegram" -> "Ответ в Telegram"
            "imap", "mail" -> "Ответ на письмо"
            else -> "Ответ"
        },
        sending = state.feedReplySending,
        error = state.feedReplyError,
        onDismiss = viewModel::closeFeedReply,
        onSend = viewModel::sendFeedReply,
    )

    if (state.showLayoutLab) {
        LayoutLabSheet(
            visible = true,
            loading = state.layoutLabLoading,
            result = state.layoutLabResult,
            smartAvailable = state.smartLayoutAvailable,
            smartHint = state.smartLayoutHint,
            onDismiss = viewModel::closeLayoutLab,
            onOpenBaseline = { viewModel.openLayoutLabReader(useSmart = false) },
            onOpenSmart = { viewModel.openLayoutLabReader(useSmart = true) },
        )
    }
    }
}

@Composable
private fun ReaderBody(
    loading: Boolean,
    translating: Boolean,
    article: com.baddysays.saylat.data.SaylatArticle?,
    plan: com.baddysays.saylat.engine.RenderPlan?,
    showStatsCards: Boolean = false,
    showLoadingSpinner: Boolean = true,
    listState: androidx.compose.foundation.lazy.LazyListState,
    readerTheme: ReaderTheme = ReaderTheme.AUTO,
    fontSettings: ReaderFontSettings = ReaderFontSettings(),
    articleSearch: ArticleSearchState = ArticleSearchState(),
    serverBaseUrl: String = "",
    pageUrl: String = "",
    proxyRemoteImages: Boolean = false,
    onLinkClick: (String) -> Unit,
    onLoadFull: () -> Unit,
) {
    when {
        loading || translating -> {
            if (showLoadingSpinner) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(if (translating) "Переводим…" else "Сжимаем страницу…")
                }
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
        article?.compression_level == "light" -> LightArticleView(
            article = article,
            onLinkClick = onLinkClick,
            onLoadFull = onLoadFull,
            listState = listState,
            readerTheme = readerTheme,
            fontSettings = fontSettings,
            articleSearch = articleSearch,
        )
        plan != null -> ArticleList(
            cards = plan.cards.filter { showStatsCards || it.kind != CardKind.STATS },
            listState = listState,
            readerTheme = readerTheme,
            fontSettings = fontSettings,
            articleSearch = articleSearch,
            serverBaseUrl = serverBaseUrl,
            pageUrl = pageUrl,
            proxyRemoteImages = proxyRemoteImages,
            onLinkClick = onLinkClick,
        )
        else -> Unit
    }
}

@Composable
private fun ArticleList(
    cards: List<RenderCard>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    readerTheme: ReaderTheme,
    fontSettings: ReaderFontSettings,
    articleSearch: ArticleSearchState,
    serverBaseUrl: String,
    pageUrl: String,
    proxyRemoteImages: Boolean,
    onLinkClick: (String) -> Unit,
) {
    val stableCards = remember(cards) { cards }
    val searchMatchOffsets = remember(stableCards, articleSearch.query) {
        var offset = 0
        stableCards.map { card ->
            val current = offset
            val cardText = listOfNotNull(
                card.title.takeIf { it.isNotBlank() },
                card.body.takeIf { it.isNotBlank() },
            ).joinToString(" ")
            if (articleSearch.query.isNotBlank()) {
                offset += countMatches(listOf(cardText), articleSearch.query)
            }
            current
        }
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            items = stableCards,
            key = { index, card -> "${card.kind.name}-$index-${card.imageSrc.orEmpty()}" },
        ) { index, card ->
            RenderCardView(
                card = card,
                readerTheme = readerTheme,
                fontSettings = fontSettings,
                articleSearch = articleSearch,
                searchMatchOffset = searchMatchOffsets.getOrElse(index) { 0 },
                serverBaseUrl = serverBaseUrl,
                pageUrl = pageUrl,
                proxyRemoteImages = proxyRemoteImages,
                onLinkClick = onLinkClick,
            )
        }
    }
}

@Composable
private fun RenderCardView(
    card: RenderCard,
    readerTheme: ReaderTheme,
    fontSettings: ReaderFontSettings,
    articleSearch: ArticleSearchState,
    searchMatchOffset: Int = 0,
    serverBaseUrl: String = "",
    pageUrl: String = "",
    proxyRemoteImages: Boolean = false,
    onLinkClick: (String) -> Unit,
) {
    val bodyColor = readerTextColor(readerTheme)
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSettings.sizeSp.sp,
        lineHeight = (fontSettings.sizeSp * fontSettings.lineHeightMultiplier).sp,
    )
    val linkColor = when (card.kind) {
        CardKind.QUOTE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
    val searchQuery = articleSearch.query
    val searchCurrent = articleSearch.currentMatch
    val searchOffset = searchMatchOffset
    val containerColor = when (card.kind) {
        CardKind.QUOTE -> MaterialTheme.colorScheme.secondaryContainer
        CardKind.STATS -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        tonalElevation = if (card.emphasis) 1.dp else 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (card.kind) {
                CardKind.HERO -> {
                    Text(
                        card.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = bodyColor,
                    )
                    if (card.meta.isNotBlank()) {
                        Text(card.meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (card.body.isNotBlank()) {
                        LinkableText(
                            text = card.body,
                            spans = card.spans,
                            onLinkClick = onLinkClick,
                            style = bodyStyle,
                            color = bodyColor,
                            linkColor = linkColor,
                            searchQuery = searchQuery,
                            searchCurrentMatch = searchCurrent,
                            searchMatchOffset = searchOffset,
                        )
                    }
                }
                CardKind.HEADING -> {
                    val spans = card.spans
                    if (!spans.isNullOrEmpty() && spans.any { !it.href.isNullOrBlank() }) {
                        LinkableText(
                            text = card.title,
                            spans = spans,
                            onLinkClick = onLinkClick,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = bodyColor,
                            linkColor = linkColor,
                        )
                    } else {
                        Text(
                            card.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = bodyColor,
                        )
                    }
                }
                CardKind.BODY -> LinkableText(
                    text = card.body,
                    spans = card.spans,
                    onLinkClick = onLinkClick,
                    style = bodyStyle,
                    color = bodyColor,
                    linkColor = linkColor,
                    searchQuery = searchQuery,
                    searchCurrentMatch = searchCurrent,
                    searchMatchOffset = searchOffset,
                )
                CardKind.QUOTE -> LinkableText(
                    text = "«${card.body}»",
                    spans = card.spans,
                    onLinkClick = onLinkClick,
                    style = bodyStyle,
                    color = bodyColor,
                    linkColor = linkColor,
                    searchQuery = searchQuery,
                    searchCurrentMatch = searchCurrent,
                    searchMatchOffset = searchOffset,
                )
                CardKind.LINK -> {
                    FilledTonalButton(
                        onClick = { card.linkHref?.let(onLinkClick) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(card.title.ifBlank { card.linkHref.orEmpty() }, maxLines = 2)
                    }
                }
                CardKind.LIST -> Text(
                    card.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor,
                )
                CardKind.IMAGE -> {
                    if (card.imagePlaceholder || card.imageSrc.isNullOrBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            ),
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "Картинка не загружена",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (card.meta.isNotBlank()) {
                                    Text(
                                        card.meta,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    } else {
                        SaylatRemoteImage(
                            model = card.imageSrc,
                            contentDescription = card.meta,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth,
                            placeholderHeight = 120,
                            serverBaseUrl = serverBaseUrl,
                            pageUrl = pageUrl,
                            proxyRemoteImages = proxyRemoteImages,
                        )
                        if (card.meta.isNotBlank()) {
                            Text(
                                card.meta,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                CardKind.STATS -> {
                    Text(card.body, style = MaterialTheme.typography.labelLarge, color = bodyColor)
                    Text(card.meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
