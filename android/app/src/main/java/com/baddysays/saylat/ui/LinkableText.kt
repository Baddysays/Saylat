package com.baddysays.saylat.ui

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.baddysays.saylat.data.TextSpan

@Composable
fun LinkableText(
    text: String,
    spans: List<TextSpan>?,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    searchQuery: String = "",
    searchCurrentMatch: Int = -1,
    searchMatchOffset: Int = 0,
) {
    val resolvedStyle = style.merge(TextStyle(color = color))
    val linkSpans = spans?.filter { !it.href.isNullOrBlank() }.orEmpty()

    if (searchQuery.isNotBlank() && linkSpans.isEmpty()) {
        val highlighted = highlightMatches(
            text = text,
            query = searchQuery,
            currentMatchIndex = searchCurrentMatch,
            matchIndexOffset = searchMatchOffset,
            highlightColor = MaterialTheme.colorScheme.primary,
            currentHighlightColor = MaterialTheme.colorScheme.tertiary,
            textColor = color,
        )
        Text(text = highlighted, style = resolvedStyle, modifier = modifier)
        return
    }

    if (linkSpans.isEmpty()) {
        if (searchQuery.isNotBlank()) {
            val highlighted = highlightMatches(
                text = text,
                query = searchQuery,
                currentMatchIndex = searchCurrentMatch,
                matchIndexOffset = searchMatchOffset,
                highlightColor = MaterialTheme.colorScheme.primary,
                currentHighlightColor = MaterialTheme.colorScheme.tertiary,
                textColor = color,
            )
            Text(text = highlighted, style = resolvedStyle, modifier = modifier)
        } else {
            Text(text = text, style = resolvedStyle, modifier = modifier)
        }
        return
    }

    val annotated = remember(text, spans, color, linkColor, searchQuery, searchCurrentMatch) {
        buildLinkAnnotated(text, spans, color, linkColor)
    }

    ClickableText(
        text = annotated,
        style = resolvedStyle,
        modifier = modifier,
        onClick = { offset ->
            annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                onLinkClick(it.item)
            }
        },
    )
}

private fun buildLinkAnnotated(
    text: String,
    spans: List<TextSpan>?,
    color: Color,
    linkColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        val segments = spans ?: listOf(TextSpan(text = text))
        for (segment in segments) {
            val href = segment.href?.trim().orEmpty()
            if (href.isNotEmpty()) {
                val start = length
                append(segment.text)
                addStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                    start,
                    length,
                )
                addStringAnnotation(tag = "URL", annotation = href, start = start, end = length)
            } else {
                val start = length
                append(segment.text)
                addStyle(SpanStyle(color = color), start, length)
            }
        }
    }
}
