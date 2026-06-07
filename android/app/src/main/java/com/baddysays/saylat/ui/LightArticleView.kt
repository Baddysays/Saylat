package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baddysays.saylat.data.SaylatArticle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightArticleView(
    article: SaylatArticle,
    onLinkClick: (String) -> Unit,
    onLoadFull: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    readerTheme: ReaderTheme = ReaderTheme.AUTO,
    fontSettings: ReaderFontSettings = ReaderFontSettings(),
    articleSearch: ArticleSearchState = ArticleSearchState(),
) {
    val bodyColor = readerTextColor(readerTheme)
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSettings.sizeSp.sp,
        lineHeight = (fontSettings.sizeSp * fontSettings.lineHeightMultiplier).sp,
    )
    LazyColumn(
        modifier = modifier.fillMaxSize().readerBackground(readerTheme),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                article.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (article.byline.isNotBlank()) {
                Text(
                    article.byline,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (article.excerpt.isNotBlank()) {
                Text(
                    article.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
        }
        if (article.plain_text.isNotBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                        LinkableText(
                            text = article.plain_text,
                            spans = null,
                            onLinkClick = onLinkClick,
                            modifier = Modifier.padding(16.dp),
                            style = bodyStyle,
                            color = bodyColor,
                            linkColor = MaterialTheme.colorScheme.primary,
                            searchQuery = articleSearch.query,
                            searchCurrentMatch = articleSearch.currentMatch,
                        )
                }
            }
        } else {
            article.blocks
                .filter { it.type == "paragraph" || it.type == "heading" || it.type == "link" }
                .forEach { block ->
                    item {
                        when (block.type) {
                            "link" -> {
                                val href = block.href.orEmpty()
                                if (href.isNotBlank()) {
                                    FilledTonalButton(
                                        onClick = { onLinkClick(href) },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(block.text.orEmpty().ifBlank { href })
                                    }
                                }
                            }
                            else -> {
                                val text = block.text.orEmpty()
                                if (text.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 1.dp,
                                    ) {
                        LinkableText(
                            text = text,
                            spans = block.spans,
                            onLinkClick = onLinkClick,
                            modifier = Modifier.padding(16.dp),
                            style = if (block.type == "heading") {
                                MaterialTheme.typography.titleMedium.copy(
                                    fontSize = (fontSettings.sizeSp + 2f).sp,
                                )
                            } else {
                                bodyStyle
                            },
                            color = bodyColor,
                            linkColor = MaterialTheme.colorScheme.primary,
                            searchQuery = articleSearch.query,
                            searchCurrentMatch = articleSearch.currentMatch,
                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
        article.links.forEach { link ->
            item {
                Surface(
                    onClick = { onLinkClick(link.href) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                ) {
                    Text(
                        link.text.ifBlank { link.href },
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        article.blocks.filter { it.type == "image" && !it.src.isNullOrBlank() }.forEach { img ->
            item {
                SaylatRemoteImage(
                    model = img.src,
                    contentDescription = img.alt,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
        item {
            FilledTonalButton(
                onClick = onLoadFull,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Открыть полную версию")
            }
        }
        item {
            Text(
                "Режим Light — минимум данных для медленной сети",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
    }
}
