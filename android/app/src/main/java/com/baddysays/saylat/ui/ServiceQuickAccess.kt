package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.data.ConnectStatus

data class ServiceCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val connected: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceQuickAccessBlock(
    status: ConnectStatus?,
    onService: (String) -> Unit,
    onOpenServiceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cards = listOf(
        ServiceCard("pikabu", "Пикабу", "Лента постов", Icons.AutoMirrored.Filled.Article, true),
        ServiceCard("vk", "ВКонтакте", "Лента / wall", Icons.Default.People, status?.vk == true),
        ServiceCard("dzen", "Дзен", "Новости", Icons.Default.Newspaper, status?.dzen == true),
        ServiceCard("telegram", "Telegram", "Диалоги", Icons.Default.Send, status?.telegram == true),
        ServiceCard("mail", "Почта", "Входящие", Icons.Default.MailOutline, status?.mail == true),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Forum, null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Сервисы", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Вход в Настройки → Сервисы. Ленты на 2G без WebView.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                cards.forEach { card ->
                    ServiceQuickCard(
                        card = card,
                        onClick = {
                            if (!card.connected && card.id != "pikabu") {
                                onOpenServiceSettings()
                            } else {
                                onService(card.id)
                            }
                        },
                    )
                }
            }
            status?.telegram_hint?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceQuickCard(card: ServiceCard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.size(width = 108.dp, height = 96.dp),
    ) {
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(card.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Surface(
                    shape = CircleShape,
                    color = if (card.connected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    },
                    modifier = Modifier.size(8.dp),
                ) {}
            }
            Column {
                Text(card.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                Text(
                    card.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                )
            }
        }
    }
}
