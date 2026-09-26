package com.maro.feature.chatlist.presentation.newchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maro.core.designsystem.component.InitialsAvatar
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.feature.chatlist.domain.FoundUser
import com.maro.feature.chatlist.presentation.generated.resources.Res
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_back
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_hint
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_search
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_title
import com.maro.feature.chatlist.presentation.generated.resources.new_chat_username
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NewChatRoot(
    onNavigateBack: () -> Unit,
    onOpenChat: (chatId: String) -> Unit,
    viewModel: NewChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            NewChatEvent.NavigateBack -> onNavigateBack()
            is NewChatEvent.NavigateToChat -> onOpenChat(event.chatId)
        }
    }

    NewChatScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    state: NewChatState,
    onAction: (NewChatAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.new_chat_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(NewChatAction.OnBackClick) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.new_chat_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onAction(NewChatAction.OnQueryChange(it)) },
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                label = { Text(stringResource(Res.string.new_chat_username)) },
                prefix = { Text("@") },
                singleLine = true,
                supportingText = { Text(stringResource(Res.string.new_chat_hint)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onAction(NewChatAction.OnSearchClick) }),
                trailingIcon = {
                    if (state.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { onAction(NewChatAction.OnSearchClick) }, enabled = state.canSearch) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.new_chat_search))
                        }
                    }
                },
            )

            state.error?.let { error ->
                Text(
                    text = error.asString(),
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            state.foundUser?.let { user ->
                Spacer(modifier = Modifier.padding(top = 8.dp))
                HorizontalDivider()
                UserRow(user = user, enabled = !state.isStarting, onClick = { onAction(NewChatAction.OnUserClick) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun UserRow(
    user: FoundUser,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialsAvatar(initials = user.initials)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = user.fullName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "@${user.username}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun NewChatScreenPreview() {
    MaroTheme {
        NewChatScreen(
            state = NewChatState(query = "anna_p", foundUser = FoundUser("uid", "Анна", "Петрова", "anna_p")),
            onAction = {},
        )
    }
}
