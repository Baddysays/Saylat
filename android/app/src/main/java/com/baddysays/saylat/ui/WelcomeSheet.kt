package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSheet(
    visible: Boolean,
    needsServerUrl: Boolean,
    serverUrlDraft: String,
    onServerUrlChange: (String) -> Unit,
    serverReady: Boolean?,
    onStart: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canStart = !needsServerUrl || serverUrlDraft.trim().startsWith("http")

    ModalBottomSheet(
        onDismissRequest = { if (canStart) onStart() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SaylatBrandMark(expanded = true, iconSize = 56.dp)
            Text(
                "Добро пожаловать",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (needsServerUrl) {
                    "Saylat работает с вашим личным сервером. " +
                        "Скопируйте адрес со страницы после установки (install-saylat-server.sh) — " +
                        "обычно http://IP-вашего-VPS:8787"
                } else {
                    "Сайты сжимаются на вашем сервере; на телефоне остаётся лёгкая лента."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )
            if (needsServerUrl) {
                OutlinedTextField(
                    value = serverUrlDraft,
                    onValueChange = onServerUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Адрес вашего сервера") },
                    placeholder = { Text("http://192.168.0.10:8787") },
                    shape = RoundedCornerShape(14.dp),
                )
            }
            when (serverReady) {
                true -> Text(
                    "Сервер отвечает — можно открывать сайты.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                false -> Text(
                    "Сервер не отвечает. Проверьте адрес, интернет и что порт 8787 открыт только для вас.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
                null -> if (!needsServerUrl) {
                    Text(
                        "Проверяем связь…",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Начать")
            }
        }
    }
}
