package com.baddysays.saylat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.R

@Composable
fun SaylatBrandMark(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    iconSize: Dp = if (expanded) 52.dp else 36.dp,
    showWordmark: Boolean = true,
) {
    val logoSize = when {
        expanded && iconSize >= 56.dp -> 152.dp
        else -> iconSize
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (expanded) 8.dp else 0.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.saylat_logo),
            contentDescription = "Saylat",
            modifier = Modifier.size(logoSize),
            contentScale = ContentScale.Fit,
        )
        if (showWordmark && expanded) {
            Text(
                "легче салата",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
    }
}
