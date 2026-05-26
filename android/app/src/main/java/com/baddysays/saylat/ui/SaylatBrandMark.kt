package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
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

/** Знак в шапке: иконка слева, «Saylat» справа (без слогана). */
@Composable
fun SaylatBrandMark(
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    showWordmark: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.saylat_mark),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = Color.Unspecified,
        )
        if (showWordmark) {
            Text(
                "Saylat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
