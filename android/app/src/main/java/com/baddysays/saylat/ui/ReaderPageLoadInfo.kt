package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.data.ArticleStats
import com.baddysays.saylat.prefs.AppLanguage
import com.baddysays.saylat.ui.strings.SaylatStrings
import com.baddysays.saylat.util.PageLoadStats

@Composable
fun ReaderPageLoadProgress(
    url: String,
    uiLanguage: AppLanguage = AppLanguage.RU,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(
                SaylatStrings.readerLoadingPage(uiLanguage),
                fontWeight = FontWeight.SemiBold,
            )
            if (url.isNotBlank()) {
                Text(
                    url,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
fun ReaderWebViewModeBanner(
    dismissedIds: Set<String>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DismissibleReaderBanner(
        bannerId = "webview",
        dismissedIds = dismissedIds,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        Text("Полная страница", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        Text(
            "Сайт грузится как в обычном браузере. Для лёгкого чтения переключитесь на «Экономию текста» или «Полосы».",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
        )
    }
}

@Composable
fun ReaderPageLoadSummary(
    stats: ArticleStats,
    modeDetail: String? = null,
    dismissedIds: Set<String>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val banner = PageLoadStats.fromStats(stats)
    val detail = modeDetail ?: banner.detail
    DismissibleReaderBanner(
        bannerId = "stats",
        dismissedIds = dismissedIds,
        onDismiss = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
    ) {
        Text(banner.headline, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        Text(
            banner.comparison,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
        )
        detail?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
