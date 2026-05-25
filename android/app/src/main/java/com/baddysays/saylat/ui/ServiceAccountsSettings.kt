package com.baddysays.saylat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.baddysays.saylat.data.ConnectStatus
import com.baddysays.saylat.data.ServiceCredentialsPublic

data class ServiceCredentialsDraft(
    val telegramApiId: String = "",
    val telegramApiHash: String = "",
    val mailImapHost: String = "",
    val mailImapPort: String = "993",
    val mailSmtpHost: String = "",
    val mailSmtpPort: String = "587",
    val mailUsername: String = "",
    val mailPassword: String = "",
    val mailUseSsl: Boolean = true,
    val vkToken: String = "",
    val dzenCookie: String = "",
    val telegramPhone: String = "+7",
    val telegramCode: String = "",
)

fun draftFromPublic(public: ServiceCredentialsPublic?): ServiceCredentialsDraft {
    if (public == null) return ServiceCredentialsDraft()
    return ServiceCredentialsDraft(
        telegramApiId = if (public.telegram_api_id > 0) public.telegram_api_id.toString() else "",
        telegramApiHash = public.telegram_api_hash,
        mailImapHost = public.mail_imap_host,
        mailImapPort = public.mail_imap_port.toString(),
        mailSmtpHost = public.mail_smtp_host,
        mailSmtpPort = public.mail_smtp_port.toString(),
        mailUsername = public.mail_username,
        mailUseSsl = public.mail_use_ssl,
    )
}

@Composable
fun ServiceAccountsSettingsSection(
    draft: ServiceCredentialsDraft,
    onDraftChange: (ServiceCredentialsDraft) -> Unit,
    connectStatus: ConnectStatus?,
    saving: Boolean,
    saveMessage: String?,
    telegramCodeSent: Boolean,
    onTelegramRequestCode: () -> Unit,
    onTelegramSignIn: () -> Unit,
    onSave: () -> Unit,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SettingsTipCard(
            title = "Сервисы на 2G",
            body = "Вход и ключи — только на Wi‑Fi. После сохранения на сервере на медленной сети открывайте ленты с главного экрана: Telegram, почта, ВК, Дзен, Пикабу.",
        )

        SettingsStepList(
            steps = listOf(
                "Подключитесь к Wi‑Fi и откройте эту вкладку.",
                "Заполните поля нужного сервиса и нажмите «Сохранить на сервере».",
                "Для Telegram: получите API на my.telegram.org, затем «Код» и «Войти».",
                "На 2G зайдите на главную → блок «Сервисы» → нужная лента.",
            ),
        )

        ServiceBlock(
            title = "Telegram",
            hint = connectStatus?.telegram_hint,
            instruction = "API ID и Hash — с my.telegram.org. Телефон в формате +7… Код приходит в приложение Telegram.",
        ) {
            ServiceTextField(draft.telegramApiId, { onDraftChange(draft.copy(telegramApiId = it)) }, "API ID", KeyboardType.Number, fieldColors)
            ServiceTextField(draft.telegramApiHash, { onDraftChange(draft.copy(telegramApiHash = it)) }, "API Hash", KeyboardType.Text, fieldColors)
            ServiceTextField(draft.telegramPhone, { onDraftChange(draft.copy(telegramPhone = it)) }, "Телефон", KeyboardType.Phone, fieldColors)
            if (telegramCodeSent) {
                ServiceTextField(draft.telegramCode, { onDraftChange(draft.copy(telegramCode = it)) }, "Код из Telegram", KeyboardType.Number, fieldColors)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onTelegramRequestCode,
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Запросить код") }
                Button(
                    onClick = onTelegramSignIn,
                    enabled = !saving && telegramCodeSent,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Войти") }
            }
        }

        ServiceBlock(
            title = "Почта",
            hint = connectStatus?.mail_hint,
            instruction = "Данные IMAP от вашего провайдера (Yandex, Mail.ru, Gmail и т.д.). Пароль оставьте пустым, если не меняете.",
        ) {
            ServiceTextField(draft.mailImapHost, { onDraftChange(draft.copy(mailImapHost = it)) }, "IMAP сервер", KeyboardType.Text, fieldColors)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceTextField(draft.mailImapPort, { onDraftChange(draft.copy(mailImapPort = it)) }, "IMAP порт", KeyboardType.Number, fieldColors, Modifier.weight(1f))
                ServiceTextField(draft.mailSmtpPort, { onDraftChange(draft.copy(mailSmtpPort = it)) }, "SMTP порт", KeyboardType.Number, fieldColors, Modifier.weight(1f))
            }
            ServiceTextField(draft.mailSmtpHost, { onDraftChange(draft.copy(mailSmtpHost = it)) }, "SMTP (если другой)", KeyboardType.Text, fieldColors)
            ServiceTextField(draft.mailUsername, { onDraftChange(draft.copy(mailUsername = it)) }, "Логин", KeyboardType.Email, fieldColors)
            OutlinedTextField(
                value = draft.mailPassword,
                onValueChange = { onDraftChange(draft.copy(mailPassword = it)) },
                label = { Text("Пароль") },
                placeholder = { Text("Пусто — не менять") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
            SettingsSwitchRow(
                title = "SSL/TLS",
                subtitle = "Обычно включено для IMAP 993",
                checked = draft.mailUseSsl,
                onCheckedChange = { onDraftChange(draft.copy(mailUseSsl = it)) },
            )
        }

        ServiceBlock(
            title = "ВКонтакте",
            instruction = "Access token с vk.com/dev. Нужен для личной ленты; без токена откроется публичный контент.",
        ) {
            OutlinedTextField(
                value = draft.vkToken,
                onValueChange = { onDraftChange(draft.copy(vkToken = it)) },
                label = { Text("Access token") },
                placeholder = { Text("Пусто — не менять") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
        }

        ServiceBlock(
            title = "Дзен",
            instruction = "Скопируйте cookie сессии из браузера на dzen.ru (F12 → Application → Cookies), если лента новостей не открывается.",
        ) {
            OutlinedTextField(
                value = draft.dzenCookie,
                onValueChange = { onDraftChange(draft.copy(dzenCookie = it)) },
                label = { Text("Cookie") },
                placeholder = { Text("Пусто — не менять") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
            )
        }

        Button(
            onClick = onSave,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            if (saving) {
                CircularProgressIndicator(Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
            }
            Text(if (saving) "Сохраняем…" else "Сохранить на сервере")
        }
        saveMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ServiceBlock(
    title: String,
    instruction: String,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    SettingsSectionCard(title = title, subtitle = instruction) {
        hint?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        content()
    }
}

@Composable
private fun ServiceTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    colors: androidx.compose.material3.TextFieldColors,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = colors,
    )
}
