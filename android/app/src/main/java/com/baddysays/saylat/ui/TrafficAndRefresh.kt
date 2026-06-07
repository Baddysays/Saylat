package com.baddysays.saylat.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SignalCellularOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ─────────────────────────────────────────────────────────────
// Счётчик сэкономленного трафика
// ─────────────────────────────────────────────────────────────

private val Context.trafficStore by preferencesDataStore("saylat_traffic")

private val KEY_SAVED_TODAY = longPreferencesKey("saved_today_bytes")
private val KEY_SAVED_TOTAL = longPreferencesKey("saved_total_bytes")
private val KEY_LAST_DAY    = longPreferencesKey("last_day_epoch")

class TrafficSavingsRepository(private val context: Context) {

    val savedToday: Flow<Long> = context.trafficStore.data.map { it[KEY_SAVED_TODAY] ?: 0L }
    val savedTotal: Flow<Long> = context.trafficStore.data.map { it[KEY_SAVED_TOTAL] ?: 0L }

    /**
     * Вызвать после каждого успешного запроса к серверу.
     * originalBytes — оригинальный размер страницы (из stats.original_bytes)
     * payloadBytes  — размер ответа сервера (из stats.payload_bytes)
     */
    suspend fun record(originalBytes: Long, payloadBytes: Long) {
        val saved = (originalBytes - payloadBytes).coerceAtLeast(0L)
        if (saved == 0L) return
        val todayEpoch = System.currentTimeMillis() / 86_400_000L
        context.trafficStore.edit { prefs ->
            val lastDay = prefs[KEY_LAST_DAY] ?: 0L
            if (lastDay != todayEpoch) {
                // Новый день — сброс счётчика «сегодня»
                prefs[KEY_SAVED_TODAY] = saved
                prefs[KEY_LAST_DAY] = todayEpoch
            } else {
                prefs[KEY_SAVED_TODAY] = (prefs[KEY_SAVED_TODAY] ?: 0L) + saved
            }
            prefs[KEY_SAVED_TOTAL] = (prefs[KEY_SAVED_TOTAL] ?: 0L) + saved
        }
    }

    suspend fun resetTotal() {
        context.trafficStore.edit { prefs ->
            prefs[KEY_SAVED_TODAY] = 0L
            prefs[KEY_SAVED_TOTAL] = 0L
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Форматирование байт
// ─────────────────────────────────────────────────────────────

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes Б"
    bytes < 1_048_576 -> "${bytes / 1024} КБ"
    bytes < 1_073_741_824 -> "%.1f МБ".format(bytes / 1_048_576.0)
    else -> "%.2f ГБ".format(bytes / 1_073_741_824.0)
}

// ─────────────────────────────────────────────────────────────
// UI: плашка «Сэкономлено»
// ─────────────────────────────────────────────────────────────

@Composable
fun TrafficSavingsBadge(
    savedToday: Long,
    modifier: Modifier = Modifier,
) {
    if (savedToday < 1024) return  // Не показываем если меньше 1 КБ

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.SaveAlt,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Сэкономлено ${formatBytes(savedToday)} сегодня",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Баннер «Нет сети»
// ─────────────────────────────────────────────────────────────

/**
 * Отслеживает подключение к интернету через ConnectivityManager.
 * Возвращает true если есть активное сетевое соединение.
 */
@Composable
fun rememberNetworkState(): Boolean {
    val context = LocalContext.current
    var isOnline by remember {
        mutableStateOf(
            (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
                .activeNetwork != null
        )
    }

    DisposableEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline = true }
            override fun onLost(network: Network) {
                // Проверяем реально ли нет сети (мог переключиться на другую)
                isOnline = cm.activeNetwork != null
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }

    return isOnline
}

@Composable
fun OfflineBanner(
    isOnline: Boolean,
    hasOfflineCache: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        Surface(
            color = if (hasOfflineCache)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.errorContainer,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (hasOfflineCache) Icons.Default.CloudOff else Icons.Default.SignalCellularOff,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (hasOfflineCache)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (hasOfflineCache)
                        "Нет интернета — доступен офлайн-кэш"
                    else
                        "Нет подключения к интернету",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasOfflineCache)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Pull-to-refresh обёртка
// ─────────────────────────────────────────────────────────────

/**
 * Добавляет pull-to-refresh к любому контенту.
 *
 * Использование в FeedScreen:
 *   PullToRefreshWrapper(
 *       isRefreshing = state.loading,
 *       onRefresh = viewModel::reloadFeed,
 *   ) {
 *       FeedList(items = state.feedItems, ...)
 *   }
 *
 * Использование в читалке — с инвалидацией кэша:
 *   PullToRefreshWrapper(
 *       isRefreshing = state.loading,
 *       onRefresh = { viewModel.reloadCurrentUrl(invalidateCache = true) },
 *   ) {
 *       ArticleContent(...)
 *   }
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshWrapper(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        content()
    }
}
