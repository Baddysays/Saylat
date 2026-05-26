package com.baddysays.saylat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.baddysays.saylat.engine.CardKind
import com.baddysays.saylat.engine.RenderCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val screen by viewModel.screen.collectAsState()
    val state by viewModel.state.collectAsState()
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SaylatTopBar(
                screen = screen,
                activeQuery = state.activeSearchQuery,
                onBack = when (screen) {
                    AppScreen.SEARCH_RESULTS -> viewModel::backFromSearch
                    AppScreen.READER -> viewModel::backFromReader
                    AppScreen.FEED -> viewModel::backFromFeed
                    AppScreen.HOME -> viewModel::goHome
                },
                onHome = viewModel::goHome,
                onSettings = viewModel::openSettings,
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
            )
        },
        bottomBar = {
            val bottomValue = if (screen == AppScreen.READER || screen == AppScreen.FEED) {
                urlSeed
            } else {
                searchSeed
            }
            val speedMode = QuickSpeedMode.fromFlags(state.slowNetworkMode, state.liteImagesEnabled)
            BottomSearchBar(
                externalValue = bottomValue,
                searchEngine = state.searchEngine,
                enabled = !state.searching && !state.loading,
                speedMode = speedMode,
                onSpeedModeChange = viewModel::setQuickSpeedMode,
                onSearch = viewModel::search,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
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
                state.smartLayoutAvailable
            ) {
                ReaderLayoutBar(
                    useSmart = state.readerUseSmartLayout,
                    smartAvailable = state.smartLayoutAvailable,
                    enhancing = state.layoutEnhancing,
                    onSelectBaseline = { viewModel.setReaderLayoutMode(useSmart = false) },
                    onSelectSmart = { viewModel.setReaderLayoutMode(useSmart = true) },
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
                        state.loading && state.stripPage == null -> ReaderPageLoadProgress(url = urlSeed)
                        stats != null && state.stripPage == null -> ReaderPageLoadSummary(
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
                        recentSearches = state.recentSearches,
                        serverUrl = state.serverUrl,
                        serverReady = state.serverReady,
                        serverStatusMessage = state.serverStatusMessage,
                        networkTesting = state.networkTesting,
                        networkTestResult = state.networkTestResult,
                        onRefreshServerStatus = viewModel::refreshServerStatus,
                        smartLayoutAvailable = state.smartLayoutAvailable,
                        layoutLabLoading = state.layoutLabLoading,
                        onQuickLink = viewModel::openQuickLink,
                        onRecent = viewModel::useRecentQuery,
                        onRunNetworkTest = viewModel::runNetworkTest,
                        onOpenLayoutLab = viewModel::runLayoutLab,
                        connectStatus = state.connectStatus,
                        onService = viewModel::openService,
                        onOpenServiceSettings = viewModel::openServiceSettings,
                        onOpenSearchSettings = viewModel::openNetworkSettings,
                        slowNetworkMode = state.slowNetworkMode,
                        liteImagesEnabled = state.liteImagesEnabled,
                        onSpeedModeChange = viewModel::setQuickSpeedMode,
                        favorites = state.favoriteLinks,
                        onOpenFavorite = viewModel::openFavorite,
                        onRemoveFavorite = viewModel::removeFavorite,
                        onPinFavorite = viewModel::pinFavoriteShortcut,
                        offlineCache = state.offlineCacheEntries,
                        onOpenCached = viewModel::openCachedPage,
                    )
                    AppScreen.SEARCH_RESULTS -> SearchResultsList(
                        query = state.activeSearchQuery.orEmpty(),
                        results = state.searchResults,
                        searching = state.searching,
                        onOpen = viewModel::openSearchResult,
                    )
                    AppScreen.READER -> {
                        val webUrl = state.webViewUrl
                        when {
                            webUrl != null -> SiteWebView(
                                pageUrl = webUrl,
                                onExternalUrl = viewModel::openLinkFromWebView,
                            )
                            state.stripPage != null || state.readerMode == com.baddysays.saylat.prefs.ReaderMode.STRIPS -> {
                                StripReaderScreen(
                                    loading = state.loading,
                                    stripPage = state.stripPage,
                                    pageUrl = urlSeed,
                                    fromCache = state.cachedNotice != null,
                                    saveInProgress = state.savingGallery,
                                    saveMessage = state.gallerySaveMessage,
                                    onSaveStrips = viewModel::saveCurrentPageToGallery,
                                )
                            }
                            else -> ReaderBody(
                                loading = state.loading,
                                translating = state.translating,
                                article = state.article,
                                plan = state.plan,
                                showStatsCards = true,
                                onLinkClick = viewModel::openLink,
                            )
                        }
                    }
                    AppScreen.FEED -> {
                        val feed = state.feed
                        if (state.loading) {
                            ReaderBody(
                                loading = true,
                                translating = false,
                                article = null,
                                plan = null,
                                onLinkClick = viewModel::openLink,
                            )
                        } else if (feed != null) {
                            FeedScreen(
                                feed = feed,
                                onOpenItem = viewModel::openFeedItem,
                                onReplyItem = viewModel::prepareFeedReply,
                                onOpenServiceSettings = viewModel::openServiceSettings,
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ReaderUrlBar(
    externalUrl: String,
    loading: Boolean,
    onGo: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(externalUrl) }
    LaunchedEffect(externalUrl) {
        if (externalUrl != draft) draft = externalUrl
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text("Страница") },
            shape = RoundedCornerShape(20.dp),
        )
        FilledTonalButton(
            onClick = { onGo(draft.trim()) },
            enabled = !loading && draft.isNotBlank(),
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
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
    onLinkClick: (String) -> Unit,
) {
    when {
        loading || translating -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(if (translating) "Переводим…" else "Сжимаем страницу…")
        }
        article?.compression_level == "light" -> LightArticleView(
            article = article,
            onLinkClick = onLinkClick,
        )
        plan != null -> ArticleList(
            cards = plan.cards.filter { showStatsCards || it.kind != CardKind.STATS },
            onLinkClick = onLinkClick,
        )
        else -> Unit
    }
}

@Composable
private fun ArticleList(cards: List<RenderCard>, onLinkClick: (String) -> Unit) {
    val stableCards = remember(cards) { cards }
    LazyColumn(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            items = stableCards,
            key = { index, card -> "${card.kind.name}-$index-${card.title.hashCode()}-${card.imageSrc?.hashCode() ?: 0}" },
        ) { _, card ->
            RenderCardView(card, onLinkClick = onLinkClick)
        }
    }
}

@Composable
private fun RenderCardView(card: RenderCard, onLinkClick: (String) -> Unit) {
    val bodyColor = when (card.kind) {
        CardKind.QUOTE -> MaterialTheme.colorScheme.onSecondaryContainer
        CardKind.STATS -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val linkColor = when (card.kind) {
        CardKind.QUOTE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
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
                            style = MaterialTheme.typography.bodyLarge,
                            color = bodyColor,
                            linkColor = linkColor,
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = bodyColor,
                    linkColor = linkColor,
                )
                CardKind.QUOTE -> LinkableText(
                    text = "«${card.body}»",
                    spans = card.spans,
                    onLinkClick = onLinkClick,
                    style = MaterialTheme.typography.bodyLarge,
                    color = bodyColor,
                    linkColor = linkColor,
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
                        val imageModel = remember(card.imageSrc) { card.imageSrc }
                        AsyncImage(
                            model = imageModel,
                            contentDescription = card.meta,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.FillWidth,
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
