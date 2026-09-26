package com.maro.feature.chatlist.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maro.core.designsystem.component.InitialsAvatar
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatParticipant
import com.maro.feature.chatlist.domain.ChatType
import com.maro.feature.chatlist.domain.LastMessage
import com.maro.feature.chatlist.presentation.generated.resources.Res
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_empty_subtitle
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_empty_title
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_menu
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_new_chat
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_no_messages_yet
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_retry
import com.maro.feature.chatlist.presentation.generated.resources.chat_list_title
import com.maro.feature.chatlist.presentation.generated.resources.menu_help
import com.maro.feature.chatlist.presentation.generated.resources.menu_profile
import com.maro.feature.chatlist.presentation.generated.resources.menu_settings
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatListRoot(
    onNewChatClick: () -> Unit,
    onOpenChat: (chatId: String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    viewModel: ChatListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ChatListEvent.NavigateToChat -> onOpenChat(event.chatId)
        }
    }

    ChatListScreen(
        state = state,
        onAction = viewModel::onAction,
        onNewChatClick = onNewChatClick,
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
    state: ChatListState,
    onAction: (ChatListAction) -> Unit,
    onNewChatClick: () -> Unit,
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
            floatingActionButton = {
                FloatingActionButton(onClick = onNewChatClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.chat_list_new_chat),
                    )
                }
            },
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
            ChatListContent(
                state = state,
                onAction = onAction,
                modifier = Modifier.fillMaxSize().padding(paddingValues),
            )
        }
    }
}

@Composable
private fun ChatListContent(
    state: ChatListState,
    onAction: (ChatListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.error != null -> ChatListMessage(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = state.error.asString(),
            modifier = modifier,
        ) {
            TextButton(onClick = { onAction(ChatListAction.OnRetryClick) }) {
                Text(stringResource(Res.string.chat_list_retry))
            }
        }

        state.isLoading && state.chats.isEmpty() -> Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.chats.isEmpty() -> ChatListMessage(
            icon = Icons.AutoMirrored.Filled.Chat,
            title = stringResource(Res.string.chat_list_empty_title),
            subtitle = stringResource(Res.string.chat_list_empty_subtitle),
            modifier = modifier,
        )

        else -> LazyColumn(modifier = modifier) {
            items(state.chats, key = { it.id }) { chat ->
                ChatListItem(chat = chat, onClick = { onAction(ChatListAction.OnChatClick(chat.id)) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(initials = chat.otherParticipant.initials)

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = chat.otherParticipant.fullName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = chat.lastMessage?.text ?: stringResource(Res.string.chat_list_no_messages_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun ChatListMessage(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
        Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        action?.let {
            Spacer(modifier = Modifier.padding(top = 8.dp))
            it()
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
private fun ChatListScreenEmptyPreview() {
    MaroTheme {
        ChatListScreen(
            state = ChatListState(),
            onAction = {},
            onNewChatClick = {},
            onProfileClick = {},
            onSettingsClick = {},
            onHelpClick = {},
        )
    }
}

@Preview
@Composable
private fun ChatListScreenPopulatedPreview() {
    MaroTheme {
        ChatListScreen(
            state = ChatListState(
                chats = listOf(
                    Chat(
                        id = "1",
                        type = ChatType.DIRECT,
                        otherParticipant = ChatParticipant("uid", "Иван", "Иванов", "ivan"),
                        lastMessage = LastMessage("Привет!", "uid", 0L),
                        updatedAt = 0L,
                    ),
                ),
            ),
            onAction = {},
            onNewChatClick = {},
            onProfileClick = {},
            onSettingsClick = {},
            onHelpClick = {},
        )
    }
}
