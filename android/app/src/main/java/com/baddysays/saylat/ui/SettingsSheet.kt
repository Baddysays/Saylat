package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.BuildConfig
import com.baddysays.saylat.data.ConnectStatus
import com.baddysays.saylat.device.DeviceProfile
import com.baddysays.saylat.network.NetworkTestResult
import com.baddysays.saylat.prefs.ReaderMode
import com.baddysays.saylat.prefs.SaylatPrefs
import com.baddysays.saylat.search.SearchEngine
import com.baddysays.saylat.cache.PageCache
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
    networkTesting: Boolean,
    networkTestResult: NetworkTestResult?,
    slowNetworkMode: Boolean = true,
    liteImagesEnabled: Boolean = false,
    readerMode: ReaderMode = ReaderMode.STRIPS,
    onReaderModeChange: (ReaderMode) -> Unit = {},
    deviceProfile: DeviceProfile? = null,
    onDismiss: () -> Unit,
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
    tamagotchiEnabled: Boolean = true,
    onTamagotchiChange: (Boolean) -> Unit = {},
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
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        SettingsSheetContent(
            serverUrl = serverUrl,
            searchEngine = searchEngine,
            searxInstanceUrl = searxInstanceUrl,
            smartLayoutEnabled = smartLayoutEnabled,
            smartLayoutAvailable = smartLayoutAvailable,
            smartLayoutHint = smartLayoutHint,
            appTheme = appTheme,
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
            settingsTab = settingsTab,
            onSettingsTabChange = onSettingsTabChange,
            cacheStats = cacheStats,
            onClearAppCache = onClearAppCache,
            customServerEnabled = customServerEnabled,
            serverReady = serverReady,
            onCustomServerChange = onCustomServerChange,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSheetContent(
    serverUrl: String,
    searchEngine: SearchEngine,
    searxInstanceUrl: String,
    smartLayoutEnabled: Boolean,
    smartLayoutAvailable: Boolean,
    smartLayoutHint: String?,
    appTheme: AppThemeId,
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
    settingsTab: SettingsTab,
    onSettingsTabChange: (SettingsTab) -> Unit,
    cacheStats: PageCache.CacheStats,
    onClearAppCache: () -> Unit,
    customServerEnabled: Boolean,
    serverReady: Boolean?,
    onCustomServerChange: (Boolean) -> Unit,
) {
    var serverDraft by remember(serverUrl) { mutableStateOf(serverUrl) }
    var showAdvancedServer by remember { mutableStateOf(customServerEnabled) }
    var searxDraft by remember(searxInstanceUrl) { mutableStateOf(searxInstanceUrl) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(bottom = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            SaylatBrandMark(iconSize = 32.dp, showWordmark = false)
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        SettingsTabBar(selected = settingsTab, onSelect = onSettingsTabChange)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (settingsTab) {
                SettingsTab.GENERAL -> {
                    SettingsSectionCard(title = "Кэш приложения", subtitle = "Полосы и статьи на устройстве") {
                        Text(
                            "Записей: ${cacheStats.entryCount} · статей: ${cacheStats.articleCount} · " +
                                "страниц с полосами: ${cacheStats.stripPageCount}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "JPEG-полос в кэше: ${cacheStats.stripImageCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Text(
                            "Кэш Saylat: ${formatBytes(cacheStats.cacheBytes)}",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Всего в data приложения: ${formatBytes(cacheStats.appFilesBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        )
                        Text(
                            "Полосы сохраняются при просмотре и открываются офлайн, как страница в браузере.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                        Button(
                            onClick = onClearAppCache,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                        ) {
                            Text("Очистить кэш")
                        }
                    }
                    SettingsTipCard(
                        title = "Как пользоваться",
                        body = "На Wi‑Fi открывайте сайты и настраивайте входы. На 2G — поиск, читалка и ленты сервисов: трафик в разы меньше, чем в обычном браузере.",
                    )
                    SettingsSectionCard(title = "Оформление", subtitle = "Тема интерфейса") {
                        ThemePickerRow(selected = appTheme, onSelect = onAppTheme)
                    }
                    SettingsSectionCard(title = "Устройство", subtitle = "Профиль и экономия трафика") {
                        deviceProfile?.let { profile ->
                            Text(profile.label, fontWeight = FontWeight.Medium)
                            Text(
                                profile.hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                        Text(
                            "Параметры скорости и картинки вынесены в раздел «Подключение», чтобы сеть и прокси были в одном месте.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    SettingsSectionCard(title = "Перевод", subtitle = "Язык в читалке") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TRANSLATE_TARGETS.forEach { (code, label) ->
                                FilterChip(
                                    selected = translateTargetLang == code,
                                    onClick = { onTranslateTarget(code) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    ),
                                )
                            }
                        }
                    }
                    SettingsSectionCard(title = "Приложение") {
                        Text(
                            "Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Button(
                            onClick = onCheckUpdate,
                            enabled = !updateChecking && !updateDownloading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            if (updateChecking || updateDownloading) {
                                CircularProgressIndicator(
                                    Modifier.padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                )
                            }
                            Text(
                                when {
                                    updateDownloading -> "Скачиваем…"
                                    updateChecking -> "Проверяем…"
                                    else -> "Проверить обновление"
                                },
                            )
                        }
                        updateStatus?.let {
                            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                SettingsTab.NETWORK -> {
                    SettingsTipCard(
                        title = "Интернет через Saylat",
                        body = when (serverReady) {
                            true -> "Сервер отвечает — сайты и поиск работают. Настраивать ничего не нужно."
                            false -> "Сервер не доступен. Проверьте мобильный интернет или Wi‑Fi."
                            null -> "Проверяем связь…"
                        },
                    )
                    SettingsSectionCard(title = "Скорость сети", subtitle = "Только про интернет и трафик") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            QuickSpeedMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = speedMode == mode,
                                    onClick = { onSpeedModeChange(mode) },
                                    label = { Text("${mode.title} · ${mode.subtitle}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    ),
                                )
                            }
                        }
                        Text(
                            speedMode.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Этот пресет меняет сеть и картинки, но не способ показа страницы. Вид страницы выбирается отдельно во вкладке «Чтение».",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        )
                    }
                    SettingsSectionCard(title = "Тонкая настройка", subtitle = "Обычно достаточно пресетов выше") {
                        SettingsSwitchRow(
                            title = "Режим 2G / EDGE",
                            subtitle = "Включает длинные таймауты и мягкие сетевые проверки",
                            checked = slowNetworkMode,
                            onCheckedChange = onSlowNetworkChange,
                        )
                        SettingsSwitchRow(
                            title = "Облегчённые картинки",
                            subtitle = "Нужно только если хотите сильнее ужать картинки в WebView и похожих режимах",
                            checked = liteImagesEnabled,
                            onCheckedChange = onLiteImagesChange,
                        )
                        Text(
                            "Эти два переключателя дублируют пресеты скорости и нужны только для ручной подстройки.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    SettingsSectionCard(title = "Связь", subtitle = "Автоматически") {
                        NetworkTestCard(
                            serverUrl = serverUrl,
                            testing = networkTesting,
                            result = networkTestResult,
                            onRunTest = onRunNetworkTest,
                            compact = true,
                            slowNetworkMode = slowNetworkMode,
                        )
                    }
                    SettingsSectionCard(title = "Свой сервер", subtitle = "Только если вы подняли VPS сами") {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Указать свой адрес", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = showAdvancedServer,
                                onCheckedChange = {
                                    showAdvancedServer = it
                                    onCustomServerChange(it)
                                },
                            )
                        }
                        if (showAdvancedServer) {
                            OutlinedTextField(
                                value = serverDraft,
                                onValueChange = { serverDraft = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                singleLine = true,
                                label = { Text("Адрес сервера") },
                                placeholder = { Text("http://ваш-ip:8787") },
                                shape = RoundedCornerShape(14.dp),
                                colors = fieldColors,
                            )
                            Button(
                                onClick = { onSaveServer(serverDraft) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Сохранить")
                            }
                        }
                    }
                    SettingsSectionCard(title = "Поиск", subtitle = "Через прокси Saylat") {
                        Text(
                            "DuckDuckGo + Wikipedia на VPS. SearXNG — запасной, если инстанс отвечает.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = searxDraft,
                            onValueChange = { searxDraft = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            singleLine = true,
                            label = { Text("SearXNG (опционально)") },
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                        )
                        TextButton(onClick = { onSearxInstance(searxDraft) }) {
                            Text("Сохранить инстанс SearXNG")
                        }
                        TextButton(onClick = onClearRecent) {
                            Text("Очистить историю поиска")
                        }
                    }
                }

                SettingsTab.READER -> {
                    SettingsTipCard(
                        title = "Как понимать режимы",
                        body = "Во вкладке «Подключение» выбирается поведение сети. Здесь выбирается только то, как показать саму страницу: текстом, полосами или как обычный сайт.",
                    )
                    SettingsSectionCard(title = "Как открывать сайты", subtitle = "Это про вид страницы, а не про сеть") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReaderMode.settingsChoices.forEach { mode ->
                                FilterChip(
                                    selected = readerMode == mode,
                                    onClick = { onReaderModeChange(mode) },
                                    label = { Text(mode.label) },
                                )
                            }
                        }
                        Text(
                            readerMode.hint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            readerModeResultHint(readerMode, speedMode),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    SettingsSectionCard(title = "Вёрстка и статистика") {
                        SettingsSwitchRow(
                            title = "Умная вёрстка",
                            subtitle = "Второй проход по блокам на устройстве",
                            checked = smartLayoutEnabled,
                            onCheckedChange = onSmartLayoutChange,
                            enabled = smartLayoutAvailable,
                        )
                        smartLayoutHint?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                        SettingsSwitchRow(
                            title = "Статистика загрузки",
                            subtitle = "Вес ленты и сравнение с оригиналом",
                            checked = showPageLoadStats,
                            onCheckedChange = onPageLoadStatsChange,
                        )
                        SettingsSwitchRow(
                            title = "Тамагочи при загрузке",
                            subtitle = "Питомец, если страница грузится дольше 10 секунд",
                            checked = tamagotchiEnabled,
                            onCheckedChange = onTamagotchiChange,
                        )
                    }
                }

                SettingsTab.SERVICES -> {
                    if (credentialsLoading) {
                        CircularProgressIndicator(Modifier.padding(24.dp))
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
        }
    }
}

private fun readerModeResultHint(readerMode: ReaderMode, speedMode: QuickSpeedMode): String = when (readerMode) {
    ReaderMode.LAYOUT, ReaderMode.NATIVE ->
        "Итог: страница откроется как лёгкие карточки и текст. Пресет скорости влияет только на сеть, а не на внешний вид."
    ReaderMode.STRIPS, ReaderMode.VISUAL ->
        "Итог: страница откроется длинными JPEG-полосами с VPS. Это ближе всего к Opera Mini и подходит для сохранения в галерею."
    ReaderMode.WEBVIEW -> when (speedMode) {
        QuickSpeedMode.ECO ->
            "Итог: откроется обычный сайт, но сеть будет максимально экономной и терпеливой к плохому каналу."
        QuickSpeedMode.BALANCED ->
            "Итог: откроется обычный сайт с умеренной экономией и длинными таймаутами для нестабильной сети."
        QuickSpeedMode.FAST ->
            "Итог: откроется обычный сайт без дополнительной экономии. Лучше для Wi-Fi и хорошего 4G."
    }
    ReaderMode.AUTO -> when (speedMode) {
        QuickSpeedMode.ECO, QuickSpeedMode.BALANCED ->
            "Итог: в авто при медленной сети Saylat сначала выберет экономичный текстовый вид, а если не выйдет — подберёт запасной вариант."
        QuickSpeedMode.FAST ->
            "Итог: в авто при быстрой сети Saylat сначала выберет полосы, а если страница не читается — откроет её как обычный сайт."
    }
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
