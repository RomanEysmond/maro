package com.maro.feature.profile.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.maro.core.designsystem.component.InitialsAvatar
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.feature.profile.presentation.generated.resources.Res
import com.maro.feature.profile.presentation.generated.resources.profile_bio_empty
import com.maro.feature.profile.presentation.generated.resources.profile_birth_date
import com.maro.feature.profile.presentation.generated.resources.profile_loading_error_retry
import com.maro.feature.profile.presentation.generated.resources.profile_username_empty
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileRoot(
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ProfileEvent.NavigateToEdit -> onEditClick()
        }
    }

    ProfileScreen(
        state = state,
        onAction = viewModel::onAction,
        onLogoutClick = onLogoutClick,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
    onLogoutClick: () -> Unit,
) {

    var darkThemeEnabled by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("Русский") }

    Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
            // Заголовок профиля
            ProfileHeader(state = state, onAction = onAction)

            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

            // Основные настройки
            SettingsCategory(title = "Основные") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Учетная запись",
                    subtitle = "Имя, @имя, о себе, дата рождения",
                    showChevron = true,
                    onClick = { onAction(ProfileAction.OnEditClick) },
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Чаты",
                    subtitle = "Тема, фон, история"
                )
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления",
                    subtitle = "Звуки, вибрация"
                )
                SwitchSettingsItem(
                    icon = Icons.Default.DarkMode,
                    title = "Темная тема",
                    checked = darkThemeEnabled,
                    onCheckedChange = { darkThemeEnabled = it }
                )
                SettingsItem(
                    icon = Icons.Default.Translate,
                    title = "Язык",
                    subtitle = selectedLanguage,
                    showChevron = true,
                    onClick = { /* Открыть выбор языка */ }
                )
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

            // Дополнительные настройки
            SettingsCategory(title = "Дополнительно") {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Хранилище и данные",
                    subtitle = "Использование сети, память"
                )
                SwitchSettingsItem(
                    icon = Icons.Default.Security,
                    title = "Конфиденциальность",
                    checked = notificationEnabled,
                    onCheckedChange = { notificationEnabled = it }
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Помощь",
                    showChevron = true
                )
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))

            // Выход
            TextButton(
                onClick = onLogoutClick,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Выйти",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
) {
    val profile = state.profile

    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(initials = profile?.initials.orEmpty(), size = 72.dp)

            Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))

            Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                if (profile != null) {
                    Text(text = profile.fullName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = formatPhone(profile.phone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = profile.username?.let { "@$it" } ?: stringResource(Res.string.profile_username_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else if (state.isLoading) {
                    CircularProgressIndicator(modifier = androidx.compose.ui.Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    state.error?.let { error ->
                        Text(text = error.asString(), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { onAction(ProfileAction.OnRetryClick) }) {
                        Text(stringResource(Res.string.profile_loading_error_retry))
                    }
                }
            }
        }

        if (profile != null) {
            Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
            Text(
                text = profile.bio.ifBlank { stringResource(Res.string.profile_bio_empty) },
                style = MaterialTheme.typography.bodyMedium,
            )
            profile.birthDate?.let { birthDate ->
                Text(
                    text = stringResource(Res.string.profile_birth_date, birthDate.toDisplayString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** "+79001234567" -> "+7 900 123-45-67"; anything unexpected is shown as is. */
private fun formatPhone(phone: String): String {
    if (phone.length != 12 || !phone.startsWith("+7")) return phone
    return "+7 ${phone.substring(2, 5)} ${phone.substring(5, 8)}-${phone.substring(8, 10)}-${phone.substring(10)}"
}

@Composable
private fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = androidx.compose.ui.Modifier.padding(
                start = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
        )

        Card(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RectangleShape
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = androidx.compose.ui.Modifier.size(24.dp)
        )

        Spacer(modifier = androidx.compose.ui.Modifier.width(24.dp))

        Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Перейти",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SwitchSettingsItem(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = androidx.compose.ui.Modifier.size(24.dp)
        )

        Spacer(modifier = androidx.compose.ui.Modifier.width(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = androidx.compose.ui.Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
        )
    }
}