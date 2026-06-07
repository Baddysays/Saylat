package com.baddysays.saylat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baddysays.saylat.BuildConfig
import com.baddysays.saylat.cache.PageCache
import com.baddysays.saylat.data.ConnectStatus
import com.baddysays.saylat.device.DeviceProfile
import com.baddysays.saylat.network.NetworkTestResult
import com.baddysays.saylat.prefs.PetGrowth
import com.baddysays.saylat.prefs.PetProfile
import com.baddysays.saylat.prefs.PetSaladEconomy
import com.baddysays.saylat.prefs.PetWallet
import com.baddysays.saylat.prefs.ReaderMode
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.prefs.SaylatPrefs
import com.baddysays.saylat.ui.strings.SaylatStrings
import com.baddysays.saylat.search.SearchEngine
import com.baddysays.saylat.ui.theme.AppThemeId
import com.baddysays.saylat.util.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    visible: Boolean,
    serverUrl: String,
    searchEngine: SearchEngine,
    searxInstanceUrl: String,
    smartLayoutEnabled: Boolean,
    smartLayoutAvailable: Boolean,
    smartLayoutHint: String?,
    appTheme: AppThemeId,
    uiLanguage: AppLanguage = AppLanguage.RU,
    onUiLanguage: (AppLanguage) -> Unit = {},
    networkTesting: Boolean,
    networkTestResult: NetworkTestResult?,
    slowNetworkMode: Boolean = true,
    liteImagesEnabled: Boolean = false,
    readerMode: ReaderMode = ReaderMode.STRIPS,
    onReaderModeChange: (ReaderMode) -> Unit = {},
    deviceProfile: DeviceProfile? = null,
    onDismiss: () -> Unit,
    tamagotchiEnabled: Boolean = true,
    onTamagotchiChange: (Boolean) -> Unit = {},
    petSkipReadyGate: Boolean = false,
    onPetSkipReadyGateChange: (Boolean) -> Unit = {},
    petProfile: PetProfile = PetProfile(),
    onPetNameChange: (String) -> Unit = {},
    onOpenPetShop: () -> Unit = {},
    onSaveServer: (String) -> Unit,
    onSearchEngine: (SearchEngine) -> Unit,
    onSearxInstance: (String) -> Unit,
    onSmartLayoutChange: (Boolean) -> Unit,
    onClearRecent: () -> Unit,
    onAppTheme: (AppThemeId) -> Unit,
    onRunNetworkTest: () -> Unit,
    onSlowNetworkChange: (Boolean) -> Unit = {},
    onLiteImagesChange: (Boolean) -> Unit = {},
    showPageLoadStats: Boolean = true,
    onPageLoadStatsChange: (Boolean) -> Unit = {},
    speedMode: QuickSpeedMode = QuickSpeedMode.BALANCED,
    onSpeedModeChange: (QuickSpeedMode) -> Unit = {},
    updateChecking: Boolean = false,
    updateDownloading: Boolean = false,
    updateStatus: String? = null,
    onCheckUpdate: () -> Unit = {},
    translateTargetLang: String = SaylatPrefs.DEFAULT_TRANSLATE_TARGET,
    onTranslateTarget: (String) -> Unit = {},
    connectStatus: ConnectStatus? = null,
    credentialsDraft: ServiceCredentialsDraft = ServiceCredentialsDraft(),
    onCredentialsDraftChange: (ServiceCredentialsDraft) -> Unit = {},
    credentialsLoading: Boolean = false,
    credentialsSaving: Boolean = false,
    credentialsMessage: String? = null,
    telegramCodeSent: Boolean = false,
    onSaveCredentials: () -> Unit = {},
    onTelegramRequestCode: () -> Unit = {},
    onTelegramSignIn: () -> Unit = {},
    connectLoading: Boolean = false,
    settingsTab: SettingsTab = SettingsTab.GENERAL,
    onSettingsTabChange: (SettingsTab) -> Unit = {},
    cacheStats: PageCache.CacheStats = PageCache.CacheStats(),
    onClearAppCache: () -> Unit = {},
    customServerEnabled: Boolean = false,
    serverReady: Boolean? = null,
    onCustomServerChange: (Boolean) -> Unit = {},
    readerTheme: ReaderTheme = ReaderTheme.AUTO,
    onReaderThemeChange: (ReaderTheme) -> Unit = {},
    readerFontSettings: ReaderFontSettings = ReaderFontSettings(),
    onReaderFontSettingsChange: (ReaderFontSettings) -> Unit = {},
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        SettingsBody(
            serverUrl = serverUrl,
            searchEngine = searchEngine,
            searxInstanceUrl = searxInstanceUrl,
            smartLayoutEnabled = smartLayoutEnabled,
            smartLayoutAvailable = smartLayoutAvailable,
            smartLayoutHint = smartLayoutHint,
            appTheme = appTheme,
            uiLanguage = uiLanguage,
            onUiLanguage = onUiLanguage,
            networkTesting = networkTesting,
            networkTestResult = networkTestResult,
            slowNetworkMode = slowNetworkMode,
            liteImagesEnabled = liteImagesEnabled,
            readerMode = readerMode,
            onReaderModeChange = onReaderModeChange,
            deviceProfile = deviceProfile,
            onSaveServer = onSaveServer,
            onSearchEngine = onSearchEngine,
            onSearxInstance = onSearxInstance,
            onSmartLayoutChange = onSmartLayoutChange,
            onClearRecent = onClearRecent,
            onAppTheme = onAppTheme,
            onRunNetworkTest = onRunNetworkTest,
            onSlowNetworkChange = onSlowNetworkChange,
            onLiteImagesChange = onLiteImagesChange,
            showPageLoadStats = showPageLoadStats,
            onPageLoadStatsChange = onPageLoadStatsChange,
            speedMode = speedMode,
            onSpeedModeChange = onSpeedModeChange,
            updateChecking = updateChecking,
            updateDownloading = updateDownloading,
            updateStatus = updateStatus,
            onCheckUpdate = onCheckUpdate,
            translateTargetLang = translateTargetLang,
            onTranslateTarget = onTranslateTarget,
            connectStatus = connectStatus,
            credentialsDraft = credentialsDraft,
            onCredentialsDraftChange = onCredentialsDraftChange,
            credentialsLoading = credentialsLoading,
            credentialsSaving = credentialsSaving,
            credentialsMessage = credentialsMessage,
            telegramCodeSent = telegramCodeSent,
            onSaveCredentials = onSaveCredentials,
            onTelegramRequestCode = onTelegramRequestCode,
            onTelegramSignIn = onTelegramSignIn,
            connectLoading = connectLoading,
            cacheStats = cacheStats,
            onClearAppCache = onClearAppCache,
            customServerEnabled = customServerEnabled,
            serverReady = serverReady,
            onCustomServerChange = onCustomServerChange,
            tamagotchiEnabled = tamagotchiEnabled,
            onTamagotchiChange = onTamagotchiChange,
            petSkipReadyGate = petSkipReadyGate,
            onPetSkipReadyGateChange = onPetSkipReadyGateChange,
            petProfile = petProfile,
            onPetNameChange = onPetNameChange,
            onOpenPetShop = onOpenPetShop,
            settingsTab = settingsTab,
            onSettingsTabChange = onSettingsTabChange,
            readerTheme = readerTheme,
            onReaderThemeChange = onReaderThemeChange,
            readerFontSettings = readerFontSettings,
            onReaderFontSettingsChange = onReaderFontSettingsChange,
        )
    }
}

// в”Ђв”Ђ Body в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsBody(
    serverUrl: String,
    searchEngine: SearchEngine,
    searxInstanceUrl: String,
    smartLayoutEnabled: Boolean,
    smartLayoutAvailable: Boolean,
    smartLayoutHint: String?,
    appTheme: AppThemeId,
    uiLanguage: AppLanguage,
    onUiLanguage: (AppLanguage) -> Unit,
    networkTesting: Boolean,
    networkTestResult: NetworkTestResult?,
    slowNetworkMode: Boolean,
    liteImagesEnabled: Boolean,
    readerMode: ReaderMode,
    onReaderModeChange: (ReaderMode) -> Unit,
    deviceProfile: DeviceProfile?,
    onSaveServer: (String) -> Unit,
    onSearchEngine: (SearchEngine) -> Unit,
    onSearxInstance: (String) -> Unit,
    onSmartLayoutChange: (Boolean) -> Unit,
    onClearRecent: () -> Unit,
    onAppTheme: (AppThemeId) -> Unit,
    onRunNetworkTest: () -> Unit,
    onSlowNetworkChange: (Boolean) -> Unit,
    onLiteImagesChange: (Boolean) -> Unit,
    showPageLoadStats: Boolean,
    onPageLoadStatsChange: (Boolean) -> Unit,
    speedMode: QuickSpeedMode,
    onSpeedModeChange: (QuickSpeedMode) -> Unit,
    updateChecking: Boolean,
    updateDownloading: Boolean,
    updateStatus: String?,
    onCheckUpdate: () -> Unit,
    translateTargetLang: String,
    onTranslateTarget: (String) -> Unit,
    connectStatus: ConnectStatus?,
    credentialsDraft: ServiceCredentialsDraft,
    onCredentialsDraftChange: (ServiceCredentialsDraft) -> Unit,
    credentialsLoading: Boolean,
    credentialsSaving: Boolean,
    credentialsMessage: String?,
    telegramCodeSent: Boolean,
    onSaveCredentials: () -> Unit,
    onTelegramRequestCode: () -> Unit,
    onTelegramSignIn: () -> Unit,
    connectLoading: Boolean,
    cacheStats: PageCache.CacheStats,
    onClearAppCache: () -> Unit,
    customServerEnabled: Boolean,
    serverReady: Boolean?,
    onCustomServerChange: (Boolean) -> Unit,
    tamagotchiEnabled: Boolean,
    onTamagotchiChange: (Boolean) -> Unit,
    petSkipReadyGate: Boolean,
    onPetSkipReadyGateChange: (Boolean) -> Unit,
    petProfile: PetProfile,
    onPetNameChange: (String) -> Unit,
    onOpenPetShop: () -> Unit,
    settingsTab: SettingsTab,
    onSettingsTabChange: (SettingsTab) -> Unit,
    readerTheme: ReaderTheme = ReaderTheme.AUTO,
    onReaderThemeChange: (ReaderTheme) -> Unit = {},
    readerFontSettings: ReaderFontSettings = ReaderFontSettings(),
    onReaderFontSettingsChange: (ReaderFontSettings) -> Unit = {},
) {
    var serverDraft by remember(serverUrl) { mutableStateOf(serverUrl) }
    var petNameDraft by remember(petProfile.name) { mutableStateOf(petProfile.name) }
    var showCustomServer by remember { mutableStateOf(customServerEnabled) }
    var searxDraft by remember(searxInstanceUrl) { mutableStateOf(searxInstanceUrl) }
    var detailTab by remember { mutableStateOf<SettingsTab?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
    ) {
        if (detailTab == null) {
            Text(
                SaylatStrings.settingsTitle(uiLanguage),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            SettingsHub(uiLanguage = uiLanguage, onSelect = { detailTab = it })
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { detailTab = null }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = SaylatStrings.settingsBack(uiLanguage),
                    )
                }
                Text(
                    SaylatStrings.settingsTab(detailTab!!, uiLanguage),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            when (detailTab) {
                SettingsTab.GENERAL -> SettingsGeneralTab(
                uiLanguage = uiLanguage,
                onUiLanguage = onUiLanguage,
                appTheme = appTheme,
                onAppTheme = onAppTheme,
                cacheStats = cacheStats,
                onClearAppCache = onClearAppCache,
                updateChecking = updateChecking,
                updateDownloading = updateDownloading,
                updateStatus = updateStatus,
                onCheckUpdate = onCheckUpdate,
            )
            SettingsTab.PET -> SettingsPetTab(
                uiLanguage = uiLanguage,
                tamagotchiEnabled = tamagotchiEnabled,
                onTamagotchiChange = onTamagotchiChange,
                petSkipReadyGate = petSkipReadyGate,
                onPetSkipReadyGateChange = onPetSkipReadyGateChange,
                petProfile = petProfile,
                petNameDraft = petNameDraft,
                onPetNameDraftChange = { petNameDraft = it },
                onPetNameChange = onPetNameChange,
                onOpenPetShop = onOpenPetShop,
                fieldColors = fieldColors,
            )
            SettingsTab.NETWORK -> SettingsNetworkTab(
                uiLanguage = uiLanguage,
                slowNetworkMode = slowNetworkMode,
                onSlowNetworkChange = onSlowNetworkChange,
                liteImagesEnabled = liteImagesEnabled,
                onLiteImagesChange = onLiteImagesChange,
                speedMode = speedMode,
                onSpeedModeChange = onSpeedModeChange,
                serverReady = serverReady,
                serverUrl = serverUrl,
                networkTesting = networkTesting,
                networkTestResult = networkTestResult,
                onRunNetworkTest = onRunNetworkTest,
                showCustomServer = showCustomServer,
                onShowCustomServerChange = {
                    showCustomServer = it
                    onCustomServerChange(it)
                },
                serverDraft = serverDraft,
                onServerDraftChange = { serverDraft = it },
                onSaveServer = onSaveServer,
                fieldColors = fieldColors,
            )
            SettingsTab.READER -> SettingsReaderTab(
                uiLanguage = uiLanguage,
                readerMode = readerMode,
                onReaderModeChange = onReaderModeChange,
                smartLayoutEnabled = smartLayoutEnabled,
                smartLayoutAvailable = smartLayoutAvailable,
                smartLayoutHint = smartLayoutHint,
                onSmartLayoutChange = onSmartLayoutChange,
                showPageLoadStats = showPageLoadStats,
                onPageLoadStatsChange = onPageLoadStatsChange,
                readerTheme = readerTheme,
                onReaderThemeChange = onReaderThemeChange,
                readerFontSettings = readerFontSettings,
                onReaderFontSettingsChange = onReaderFontSettingsChange,
            )
            SettingsTab.SERVICES -> SettingsServicesTab(
                uiLanguage = uiLanguage,
                searchEngine = searchEngine,
                onSearchEngine = onSearchEngine,
                searxDraft = searxDraft,
                onSearxDraftChange = { searxDraft = it },
                onSearxInstance = onSearxInstance,
                onClearRecent = onClearRecent,
                translateTargetLang = translateTargetLang,
                onTranslateTarget = onTranslateTarget,
                credentialsLoading = credentialsLoading,
                credentialsDraft = credentialsDraft,
                onCredentialsDraftChange = onCredentialsDraftChange,
                connectStatus = connectStatus,
                credentialsSaving = credentialsSaving,
                connectLoading = connectLoading,
                credentialsMessage = credentialsMessage,
                telegramCodeSent = telegramCodeSent,
                onTelegramRequestCode = onTelegramRequestCode,
                onTelegramSignIn = onTelegramSignIn,
                onSaveCredentials = onSaveCredentials,
                fieldColors = fieldColors,
            )
            else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHub(uiLanguage: AppLanguage, onSelect: (SettingsTab) -> Unit) {
    TgSettingsGroup {
        SettingsHubRow(
            icon = Icons.Default.Tune,
            title = SaylatStrings.settingsTab(SettingsTab.GENERAL, uiLanguage),
            subtitle = SaylatStrings.settingsHubGeneralSubtitle(uiLanguage),
            onClick = { onSelect(SettingsTab.GENERAL) },
        )
        TgDivider()
        SettingsHubRow(
            icon = Icons.Default.Pets,
            title = SaylatStrings.settingsTab(SettingsTab.PET, uiLanguage),
            subtitle = SaylatStrings.settingsHubPetSubtitle(uiLanguage),
            onClick = { onSelect(SettingsTab.PET) },
        )
        TgDivider()
        SettingsHubRow(
            icon = Icons.Default.Wifi,
            title = SaylatStrings.settingsTab(SettingsTab.NETWORK, uiLanguage),
            subtitle = SaylatStrings.settingsHubNetworkSubtitle(uiLanguage),
            onClick = { onSelect(SettingsTab.NETWORK) },
        )
        TgDivider()
        SettingsHubRow(
            icon = Icons.Default.MenuBook,
            title = SaylatStrings.settingsTab(SettingsTab.READER, uiLanguage),
            subtitle = SaylatStrings.settingsHubReaderSubtitle(uiLanguage),
            onClick = { onSelect(SettingsTab.READER) },
        )
        TgDivider()
        SettingsHubRow(
            icon = Icons.Default.Cloud,
            title = SaylatStrings.settingsTab(SettingsTab.SERVICES, uiLanguage),
            subtitle = SaylatStrings.settingsHubServicesSubtitle(uiLanguage),
            onClick = { onSelect(SettingsTab.SERVICES) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsHubRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
        }
    }
}

@Composable
private fun SettingsGeneralTab(
    uiLanguage: AppLanguage,
    onUiLanguage: (AppLanguage) -> Unit,
    appTheme: AppThemeId,
    onAppTheme: (AppThemeId) -> Unit,
    cacheStats: PageCache.CacheStats,
    onClearAppCache: () -> Unit,
    updateChecking: Boolean,
    updateDownloading: Boolean,
    updateStatus: String?,
    onCheckUpdate: () -> Unit,
) {
        SettingsGroupHeader(SaylatStrings.settingsLanguageTitle(uiLanguage))
        SettingsGroupContent {
            Text(
                SaylatStrings.settingsLanguageHint(uiLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LanguagePickerRow(
                selected = uiLanguage,
                onSelect = onUiLanguage,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        SettingsGroupHeader(SaylatStrings.settingsAppearance(uiLanguage))
        SettingsGroupContent {
            ThemePickerRow(
                selected = appTheme,
                onSelect = onAppTheme,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        SettingsGroupHeader(SaylatStrings.settingsAppSection(uiLanguage))
        SettingsGroupContent {
            Text(
                SaylatStrings.settingsVersion(uiLanguage, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Button(
                onClick = onCheckUpdate,
                enabled = !updateChecking && !updateDownloading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (updateChecking || updateDownloading) {
                    CircularProgressIndicator(Modifier.size(16.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                }
                Text(
                    when {
                        updateDownloading -> SaylatStrings.settingsUpdating(uiLanguage)
                        updateChecking -> SaylatStrings.settingsCheckingUpdate(uiLanguage)
                        else -> SaylatStrings.settingsCheckUpdate(uiLanguage)
                    },
                )
            }
            updateStatus?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
        }

        SettingsGroupHeader(SaylatStrings.settingsCacheSection(uiLanguage))
        SettingsGroupContent {
            Column(
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    SaylatStrings.settingsCacheEntries(uiLanguage, cacheStats.entryCount),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    SaylatStrings.settingsCacheStrips(uiLanguage, cacheStats.stripPageCount),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    SaylatStrings.settingsCacheSize(uiLanguage, formatBytes(cacheStats.cacheBytes)),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Button(
                onClick = onClearAppCache,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(SaylatStrings.settingsClearCache(uiLanguage))
            }
        }
}

@Composable
private fun SettingsPetTab(
    uiLanguage: AppLanguage,
    tamagotchiEnabled: Boolean,
    onTamagotchiChange: (Boolean) -> Unit,
    petSkipReadyGate: Boolean,
    onPetSkipReadyGateChange: (Boolean) -> Unit,
    petProfile: PetProfile,
    petNameDraft: String,
    onPetNameDraftChange: (String) -> Unit,
    onPetNameChange: (String) -> Unit,
    onOpenPetShop: () -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors,
) {
    TgSettingsGroup {
        TgSwitchRowInline(
            icon = Icons.Default.Pets,
            title = SaylatStrings.settingsPetHeader(uiLanguage),
            subtitle = SaylatStrings.settingsPetHeaderSub(uiLanguage),
            checked = tamagotchiEnabled,
            onCheckedChange = onTamagotchiChange,
        )
    }
    if (tamagotchiEnabled) {
        TgSettingsGroup {
            TgSwitchRowInline(
                icon = Icons.Default.Pets,
                title = SaylatStrings.settingsPetSkipReady(uiLanguage),
                subtitle = SaylatStrings.settingsPetSkipReadySub(uiLanguage),
                checked = petSkipReadyGate,
                onCheckedChange = onPetSkipReadyGateChange,
            )
        }
        SettingsGroupContent {
            Button(
                onClick = onOpenPetShop,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(SaylatStrings.settingsPetShop(uiLanguage))
            }
            OutlinedTextField(
                value = petNameDraft,
                onValueChange = { onPetNameDraftChange(it.take(24)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                label = { Text(SaylatStrings.settingsPetName(uiLanguage)) },
                placeholder = { Text(PetSaladEconomy.DEFAULT_PET_NAME) },
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(12.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { onPetNameChange(petNameDraft) },
                    enabled = petNameDraft.trim().isNotEmpty() &&
                        petNameDraft.trim() != petProfile.name,
                ) {
                    Text(SaylatStrings.save(uiLanguage))
                }
            }
            Text(
                "${petProfile.stageTitle} · ${PetGrowth.formatEatenMb(petProfile.saladsEatenBytes)} / 100 МБ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Text(
                SaylatStrings.settingsPetWallet(uiLanguage, PetWallet.formatWallet(petProfile), petProfile.salads),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (petProfile.isEgg) {
                Text(
                    SaylatStrings.settingsPetHatchHint(uiLanguage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsNetworkTab(
    uiLanguage: AppLanguage,
    slowNetworkMode: Boolean,
    onSlowNetworkChange: (Boolean) -> Unit,
    liteImagesEnabled: Boolean,
    onLiteImagesChange: (Boolean) -> Unit,
    speedMode: QuickSpeedMode,
    onSpeedModeChange: (QuickSpeedMode) -> Unit,
    serverReady: Boolean?,
    serverUrl: String,
    networkTesting: Boolean,
    networkTestResult: NetworkTestResult?,
    onRunNetworkTest: () -> Unit,
    showCustomServer: Boolean,
    onShowCustomServerChange: (Boolean) -> Unit,
    serverDraft: String,
    onServerDraftChange: (String) -> Unit,
    onSaveServer: (String) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors,
) {
        SettingsGroupHeader(SaylatStrings.settingsNetworkSection(uiLanguage))
        TgSettingsGroup {
            TgSwitchRowInline(
                icon = Icons.Default.Speed,
                title = SaylatStrings.settingsSlowNetwork(uiLanguage),
                subtitle = SaylatStrings.settingsSlowNetworkSub(uiLanguage),
                checked = slowNetworkMode,
                onCheckedChange = onSlowNetworkChange,
            )
            TgDivider()
            TgSwitchRowInline(
                icon = Icons.Default.Speed,
                title = SaylatStrings.settingsLiteImages(uiLanguage),
                subtitle = SaylatStrings.settingsLiteImagesSub(uiLanguage),
                checked = liteImagesEnabled,
                onCheckedChange = onLiteImagesChange,
            )
        }
        SettingsGroupContent {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QuickSpeedMode.entries.forEach { mode ->
                    FilterChip(
                        selected = speedMode == mode,
                        onClick = { onSpeedModeChange(mode) },
                        label = { Text(SaylatStrings.quickSpeedTitle(mode, uiLanguage)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        ),
                    )
                }
            }
            Text(
                SaylatStrings.quickSpeedSummary(speedMode, uiLanguage),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        val statusText = when (serverReady) {
            true -> SaylatStrings.settingsServerOk(uiLanguage)
            false -> SaylatStrings.settingsServerDown(uiLanguage)
            null -> SaylatStrings.settingsServerChecking(uiLanguage)
        }
        TgInfoRow(
            icon = Icons.Default.Wifi,
            title = statusText,
            subtitle = serverUrl.removePrefix("https://").removePrefix("http://").take(40),
        )
        SettingsGroupContent {
            NetworkTestCard(
                serverUrl = serverUrl,
                testing = networkTesting,
                result = networkTestResult,
                onRunTest = onRunNetworkTest,
                compact = true,
                slowNetworkMode = slowNetworkMode,
            )
        }
        TgSettingsGroup {
            TgSwitchRowInline(
                icon = Icons.Default.Cloud,
                title = SaylatStrings.settingsCustomServer(uiLanguage),
                subtitle = SaylatStrings.settingsCustomServerSub(uiLanguage),
                checked = showCustomServer,
                onCheckedChange = onShowCustomServerChange,
            )
        }
        if (showCustomServer) {
            SettingsGroupContent {
                OutlinedTextField(
                    value = serverDraft,
                    onValueChange = onServerDraftChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    singleLine = true,
                    label = { Text(SaylatStrings.settingsServerAddress(uiLanguage)) },
                    placeholder = { Text("http://ip:8787") },
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                )
                TextButton(onClick = { onSaveServer(serverDraft) }, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(SaylatStrings.save(uiLanguage))
                }
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsReaderTab(
    uiLanguage: AppLanguage,
    readerMode: ReaderMode,
    onReaderModeChange: (ReaderMode) -> Unit,
    smartLayoutEnabled: Boolean,
    smartLayoutAvailable: Boolean,
    smartLayoutHint: String?,
    onSmartLayoutChange: (Boolean) -> Unit,
    showPageLoadStats: Boolean,
    onPageLoadStatsChange: (Boolean) -> Unit,
    readerTheme: ReaderTheme,
    onReaderThemeChange: (ReaderTheme) -> Unit,
    readerFontSettings: ReaderFontSettings,
    onReaderFontSettingsChange: (ReaderFontSettings) -> Unit,
) {
        SettingsGroupHeader(SaylatStrings.settingsReaderSection(uiLanguage))
        SettingsGroupContent {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ReaderMode.settingsChoices.forEach { mode ->
                    FilterChip(
                        selected = readerMode == mode,
                        onClick = { onReaderModeChange(mode) },
                        label = { Text(SaylatStrings.readerModeLabel(mode, uiLanguage)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        ),
                    )
                }
            }
            Text(
                SaylatStrings.readerModeHint(readerMode, uiLanguage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        TgSettingsGroup {
            ReaderThemeSelector(
                currentTheme = readerTheme,
                onSelect = onReaderThemeChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            TgDivider()
            FontSizeControl(
                settings = readerFontSettings,
                onUpdate = onReaderFontSettingsChange,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            TgDivider()
            TgSwitchRowInline(
                icon = Icons.Default.Tune,
                title = SaylatStrings.settingsSmartLayout(uiLanguage),
                subtitle = smartLayoutHint ?: SaylatStrings.smartLayoutDefaultSubtitle(uiLanguage),
                checked = smartLayoutEnabled,
                onCheckedChange = onSmartLayoutChange,
                enabled = smartLayoutAvailable,
            )
            TgDivider()
            TgSwitchRowInline(
                icon = Icons.Default.Tune,
                title = SaylatStrings.settingsLoadStats(uiLanguage),
                subtitle = SaylatStrings.settingsLoadStatsSub(uiLanguage),
                checked = showPageLoadStats,
                onCheckedChange = onPageLoadStatsChange,
            )
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsServicesTab(
    uiLanguage: AppLanguage,
    searchEngine: SearchEngine,
    onSearchEngine: (SearchEngine) -> Unit,
    searxDraft: String,
    onSearxDraftChange: (String) -> Unit,
    onSearxInstance: (String) -> Unit,
    onClearRecent: () -> Unit,
    translateTargetLang: String,
    onTranslateTarget: (String) -> Unit,
    credentialsLoading: Boolean,
    credentialsDraft: ServiceCredentialsDraft,
    onCredentialsDraftChange: (ServiceCredentialsDraft) -> Unit,
    connectStatus: ConnectStatus?,
    credentialsSaving: Boolean,
    connectLoading: Boolean,
    credentialsMessage: String?,
    telegramCodeSent: Boolean,
    onTelegramRequestCode: () -> Unit,
    onTelegramSignIn: () -> Unit,
    onSaveCredentials: () -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors,
) {
        SettingsGroupHeader(SaylatStrings.settingsSearchSection(uiLanguage))
        SettingsGroupContent {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SearchEngine.entries.forEach { engine ->
                    FilterChip(
                        selected = searchEngine == engine,
                        onClick = { onSearchEngine(engine) },
                        label = { Text(engine.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        ),
                    )
                }
            }
            OutlinedTextField(
                value = searxDraft,
                onValueChange = onSearxDraftChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                label = { Text("SearXNG") },
                shape = RoundedCornerShape(14.dp),
                colors = fieldColors,
            )
            Row(Modifier.padding(horizontal = 12.dp)) {
                TextButton(onClick = { onSearxInstance(searxDraft) }) { Text(SaylatStrings.save(uiLanguage)) }
                TextButton(onClick = onClearRecent) { Text(SaylatStrings.settingsClearHistory(uiLanguage)) }
            }
        }
        SettingsGroupHeader(SaylatStrings.settingsTranslateSection(uiLanguage))
        SettingsGroupContent {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TRANSLATE_TARGETS.forEach { (code, label) ->
                    FilterChip(
                        selected = translateTargetLang == code,
                        onClick = { onTranslateTarget(code) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        ),
                    )
                }
            }
        }
        SettingsGroupHeader(SaylatStrings.settingsAccountsSection(uiLanguage))
        SettingsGroupContent {
            if (credentialsLoading) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            } else {
                ServiceAccountsSettingsSection(
                    draft = credentialsDraft,
                    onDraftChange = onCredentialsDraftChange,
                    connectStatus = connectStatus,
                    saving = credentialsSaving || connectLoading,
                    saveMessage = credentialsMessage,
                    telegramCodeSent = telegramCodeSent,
                    onTelegramRequestCode = onTelegramRequestCode,
                    onTelegramSignIn = onTelegramSignIn,
                    onSave = onSaveCredentials,
                )
            }
        }
}


// в”Ђв”Ђ Telegram-style primitives в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

@Composable
private fun TgSettingsGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun TgDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 66.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
    )
}

@Composable
private fun TgSwitchRowInline(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        title.uppercase(),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.7.sp,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SettingsGroupContent(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun TgSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
    Spacer(Modifier.height(2.dp))
}

@Composable
private fun TgInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
        }
    }
    Spacer(Modifier.height(2.dp))
}

private val TRANSLATE_TARGETS = listOf(
    "ru" to "Русский",
    "en" to "English",
    "uk" to "Українська",
    "de" to "Deutsch",
    "es" to "Español",
    "fr" to "Français",
    "tr" to "Türkçe",
    "zh" to "中文",
)
