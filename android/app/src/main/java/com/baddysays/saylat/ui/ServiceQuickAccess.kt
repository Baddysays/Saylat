package com.baddysays.saylat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.automirrored.filled.Send
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
    val setupHint: String = "Подключить",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceQuickAccessBlock(
    status: ConnectStatus?,
    onService: (String) -> Unit,
    onOpenServiceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anyMessenger = status?.telegram == true || status?.vk == true || status?.mail == true
    val cards = listOf(
        ServiceCard("inbox", "Все ленты", "TG · VK · почта", Icons.Default.Dashboard, anyMessenger, "Подключить ленты"),
        ServiceCard("pikabu", "Пикабу", "Лента постов", Icons.AutoMirrored.Filled.Article, true),
        ServiceCard("vk", "ВКонтакте", "Лента / wall", Icons.Default.People, status?.vk == true, "Подключить VK"),
        ServiceCard("dzen", "Дзен", "Новости", Icons.Default.Newspaper, status?.dzen == true, "Добавить cookie"),
        ServiceCard("telegram", "Telegram", "Диалоги", Icons.AutoMirrored.Filled.Send, status?.telegram == true, "Войти в Telegram"),
        ServiceCard("mail", "Почта", "Входящие", Icons.Default.MailOutline, status?.mail == true, "Настроить почту"),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ) {
                    Icon(
                        Icons.Default.Forum,
                        null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Сервисы", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Быстрые входы в ленты и аккаунты без лишней навигации.",
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (card.connected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            },
        ),
        modifier = Modifier.size(width = 116.dp, height = 104.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (card.connected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    },
                ) {
                    Box(Modifier.padding(9.dp)) {
                        Icon(card.icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (card.connected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (card.connected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            },
                            modifier = Modifier.size(6.dp),
                        ) {}
                        Text(
                            if (card.connected) "готово" else "вход",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (card.connected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            },
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(card.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                Text(
                    if (card.connected) card.subtitle else card.setupHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (card.connected) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 2,
                )
            }
        }
    }
}
