package com.maro.feature.profile.presentation.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maro.core.design_system.generated.resources.Res as DesignRes
import com.maro.core.design_system.generated.resources.logo
import com.maro.feature.profile.presentation.generated.resources.Res
import com.maro.feature.profile.presentation.generated.resources.settings_about
import com.maro.feature.profile.presentation.generated.resources.settings_account
import com.maro.feature.profile.presentation.generated.resources.settings_chats
import com.maro.feature.profile.presentation.generated.resources.settings_help
import com.maro.feature.profile.presentation.generated.resources.settings_invite
import com.maro.feature.profile.presentation.generated.resources.settings_logout
import com.maro.feature.profile.presentation.generated.resources.settings_notifications
import com.maro.feature.profile.presentation.generated.resources.settings_privacy
import com.maro.feature.profile.presentation.generated.resources.settings_search
import com.maro.feature.profile.presentation.generated.resources.settings_storage
import com.maro.feature.profile.presentation.generated.resources.settings_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLogoutClick: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                actions = {
                    IconButton(onClick = { /* Действие поиска */ }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.settings_search))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Профиль пользователя
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { /* Открыть профиль */ },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(DesignRes.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Иван Иванов",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "+7 (123) 456-78-90",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            // Раздел настроек 1
            item {
                SettingsSection(title = stringResource(Res.string.settings_account)) {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        text = stringResource(Res.string.settings_privacy),
                        showDivider = true,
                    )
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        text = stringResource(Res.string.settings_chats),
                        showDivider = true,
                    )
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        text = stringResource(Res.string.settings_notifications),
                        showDivider = true,
                    )
                    SettingsItem(
                        icon = Icons.Default.Storage,
                        text = stringResource(Res.string.settings_storage),
                    )
                }
            }

            // Раздел настроек 2
            item {
                SettingsSection(title = stringResource(Res.string.settings_about)) {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        text = stringResource(Res.string.settings_help),
                        showDivider = true,
                    )
                    SettingsItem(
                        icon = Icons.Default.People,
                        text = stringResource(Res.string.settings_invite),
                    )
                }
            }

            // Выход
            item {
                Text(
                    text = stringResource(Res.string.settings_logout),
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable(onClick = onLogoutClick),
                    fontSize = 16.sp,
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingsItem(icon: ImageVector, text: String, showDivider: Boolean = false, onClick: () -> Unit = {}) {
    Column(modifier = Modifier.clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 56.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
            )
        }
    }
}
