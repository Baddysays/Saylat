package com.baddysays.saylat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.SignalCellular0Bar
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.SignalCellularAlt1Bar
import androidx.compose.material.icons.filled.SignalCellularAlt2Bar
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.network.NetworkFormat
import com.baddysays.saylat.network.NetworkTestResult
import com.baddysays.saylat.network.SpeedTier

@Composable
fun NetworkTestCard(
    serverUrl: String,
    testing: Boolean,
    result: NetworkTestResult?,
    onRunTest: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    slowNetworkMode: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
    ) {
        Column(
            Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    Icons.Default.NetworkCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text("Сеть и прокси", fontWeight = FontWeight.SemiBold)
                    Text(
                        serverUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        maxLines = 1,
                    )
                    if (slowNetworkMode) {
                        Text(
                            "Режим 2G: таймаут до 3 мин, лёгкий тест 8 КБ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (testing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    if (slowNetworkMode) {
                        "На 2G это может занять 1–3 минуты…"
                    } else {
                        "Проверяем соединение и скорость…"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            result?.let { r ->
                if (!testing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            tierIcon(r.profile?.tier),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                r.profile?.title ?: if (r.ok) "Готово" else "Ошибка",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                r.profile?.description ?: r.error.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            )
                        }
                    }
                    if (r.ok) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            r.latencyMs?.let {
                                MetricChip("Отклик", NetworkFormat.latency(it))
                            }
                            r.downloadKbps?.let { kbps ->
                                MetricChip("Скорость", NetworkFormat.speedKbps(kbps))
                            }
                        }
                    } else {
                        r.error?.let { err ->
                            Text(
                                err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRunTest,
                    enabled = !testing,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    Text(if (result == null) "Тестировать" else "Повторить")
                }
                if (!compact && result != null) {
                    OutlinedButton(
                        onClick = onRunTest,
                        enabled = !testing,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Снова")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun tierIcon(tier: SpeedTier?): ImageVector = when (tier) {
    SpeedTier.OFFLINE -> Icons.Default.SignalCellularConnectedNoInternet0Bar
    SpeedTier.EDGE_2G -> Icons.Default.SignalCellular0Bar
    SpeedTier.SLOW_3G -> Icons.Default.SignalCellularAlt1Bar
    SpeedTier.FAST_3G -> Icons.Default.SignalCellularAlt2Bar
    SpeedTier.LTE -> Icons.Default.SignalCellular4Bar
    SpeedTier.BROADBAND -> Icons.Default.Wifi
    null -> Icons.Default.SignalCellular4Bar
}
