package com.example.example.screens.menu_screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Help
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.example.R
import java.lang.reflect.Modifier

@Composable
fun SettingsScreen2() {
    var darkThemeEnabled by remember { mutableStateOf(false) }
    var notificationEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("Русский") }

    Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(modifier = androidx.compose.ui.Modifier.verticalScroll(rememberScrollState())) {
            // Заголовок профиля
            ProfileHeader(
                name = "Иван Иванов",
                phone = "+7 (123) 456-78-90",
                avatarRes = R.drawable.logo
            )

            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))

            // Основные настройки
            SettingsCategory(title = "Основные") {
                SettingsItem(

                    icon = Icons.Default.Person,
                    title = "Учетная запись",
                    subtitle = "Приватность, номер телефона"
                )
                SettingsItem(
                    icon = Icons.Default.Chat,
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
                    icon = Icons.Default.Help,
                    title = "Помощь",
                    showChevron = true
                )
            }

            Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))

            // Выход
            TextButton(
                onClick = { /* Выход из аккаунта */ },
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Выйти",
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}

@Composable
private fun ProfileHeader(name: String, phone: String, avatarRes: Int) {
    Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = avatarRes),
            contentDescription = "Аватар",
            modifier = androidx.compose.ui.Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.h6
            )
            Text(
                text = phone,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SettingsCategory(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = androidx.compose.ui.Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )

        Card(
            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
            elevation = 0.dp,
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
            tint = MaterialTheme.colors.primary,
            modifier = androidx.compose.ui.Modifier.size(24.dp)
        )

        Spacer(modifier = androidx.compose.ui.Modifier.width(24.dp))

        Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Перейти",
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
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
            tint = MaterialTheme.colors.primary,
            modifier = androidx.compose.ui.Modifier.size(24.dp)
        )

        Spacer(modifier = androidx.compose.ui.Modifier.width(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            modifier = androidx.compose.ui.Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colors.primary,
                checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.4f)
            )
        )
    }
}