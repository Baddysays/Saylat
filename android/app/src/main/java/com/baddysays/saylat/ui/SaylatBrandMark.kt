package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (expanded) 12.dp else 8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.saylat_mark),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = Color.Unspecified,
        )
        if (showWordmark) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    "Saylat",
                    style = if (expanded) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (expanded) {
                    Text(
                        "легче салата",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}
