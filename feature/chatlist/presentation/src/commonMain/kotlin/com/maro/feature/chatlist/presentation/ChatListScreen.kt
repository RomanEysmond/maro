package com.maro.feature.chatlist.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.feature.chatlist.presentation.generated.resources.Res
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_menu
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_placeholder
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_title
import com.maro.feature.chatlist.presentation.generated.resources.menu_help
import com.maro.feature.chatlist.presentation.generated.resources.menu_profile
import com.maro.feature.chatlist.presentation.generated.resources.menu_settings
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ChatListRoot(
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    // The list itself (Room + Paging) arrives in stage 3; a ViewModel is added with it.
    ChatListScreen(
        onProfileClick = onOpenProfile,
        onSettingsClick = onOpenSettings,
        onHelpClick = onOpenHelp,
    )
}

private data class DrawerMenuItem(
    val icon: ImageVector,
    val title: StringResource,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val menuItems = listOf(
        DrawerMenuItem(Icons.Default.Person, Res.string.menu_profile, onProfileClick),
        DrawerMenuItem(Icons.Default.Settings, Res.string.menu_settings, onSettingsClick),
        DrawerMenuItem(Icons.Default.Info, Res.string.menu_help, onHelpClick),
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                items = menuItems,
                onItemClick = { item ->
                    scope.launch { drawerState.close() }
                    item.onClick()
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.chat_list_title)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(Res.string.chat_list_menu),
                            )
                        }
                    },
                )
            },
        ) { paddingValues ->
            // Основное содержимое экрана
            Box(modifier = Modifier.padding(paddingValues)) {
                Text(stringResource(Res.string.chat_list_placeholder))
            }
        }
    }
}

@Composable
private fun DrawerContent(
    items: List<DrawerMenuItem>,
    onItemClick: (DrawerMenuItem) -> Unit,
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(Res.string.chat_list_menu),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp),
            )

            HorizontalDivider()

            items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(stringResource(item.title)) },
                    icon = { Icon(imageVector = item.icon, contentDescription = null) },
                    selected = false,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ChatListScreenPreview() {
    MaroTheme {
        ChatListScreen(onProfileClick = {}, onSettingsClick = {}, onHelpClick = {})
    }
}
