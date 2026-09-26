package com.maro.feature.chat.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.maro.core.presentation.util.UiText
import com.maro.core.presentation.util.toUiText
import com.maro.core.designsystem.component.InitialsAvatar
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.LoadMessagesException
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageStatus
import com.maro.feature.chat.presentation.generated.resources.Res
import com.maro.feature.chat.presentation.generated.resources.chat_back
import com.maro.feature.chat.presentation.generated.resources.chat_empty
import com.maro.feature.chat.presentation.generated.resources.chat_history_error
import com.maro.feature.chat.presentation.generated.resources.chat_history_retry
import com.maro.feature.chat.presentation.generated.resources.chat_input_hint
import com.maro.feature.chat.presentation.generated.resources.chat_send
import com.maro.feature.chat.presentation.generated.resources.chat_status_failed
import com.maro.feature.chat.presentation.generated.resources.chat_status_sending
import com.maro.feature.chat.presentation.generated.resources.chat_status_sent
import com.maro.feature.chat.presentation.generated.resources.chat_updating
import com.maro.feature.chat.presentation.generated.resources.chat_waiting_for_network
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ChatRoot(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val messages = viewModel.messages.collectAsLazyPagingItems()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChatEvent.NavigateBack -> onNavigateBack()
        }
    }

    ChatScreen(
        state = state,
        messages = messages,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    messages: LazyPagingItems<Message>,
    onAction: (ChatAction) -> Unit,
) {
    val listState = rememberLazyListState()
    FollowNewestMessage(listState = listState, messages = messages)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        state.header?.let { header ->
                            InitialsAvatar(initials = header.initials, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = header.participantName, maxLines = 1)
                                state.subtitle()?.let { subtitle ->
                                    Text(
                                        text = subtitle,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(ChatAction.OnBackClick) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.chat_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (messages.itemCount == 0) {
                    EmptyChat(state = state, modifier = Modifier.align(Alignment.Center))
                } else {
                    // Newest at the bottom (see FollowNewestMessage for new arrivals). Scrolling up reaches the end
                    // of what Room has, and paging fetches the next older page.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        reverseLayout = true,
                        // Bottom, as reverseLayout's own default: a short chat sits next to the input, not at the top.
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
                    ) {
                        items(count = messages.itemCount, key = messages.itemKey { it.id }) { index ->
                            messages[index]?.let { message ->
                                MessageBubble(
                                    message = message,
                                    onRetryClick = { onAction(ChatAction.OnRetryClick(message.id)) },
                                )
                            }
                        }
                        // Last in a reversed list = on top of the screen, above the oldest message.
                        item(key = "history") {
                            HistoryLoadState(
                                loadState = messages.loadState.append,
                                onRetryClick = messages::retry,
                            )
                        }
                    }
                }
            }

            MessageInput(
                value = state.input,
                canSend = state.canSend,
                onValueChange = { onAction(ChatAction.OnInputChange(it)) },
                onSendClick = { onAction(ChatAction.OnSendClick) },
            )
        }
    }
}

/**
 * The list keeps its position by item key, so messages inserted below the one on screen (new arrivals, a
 * catch-up after being offline) would stay out of view. If the previous newest message was on screen, the
 * user was at the bottom: follow to the new one. If they were reading history further up, leave them there.
 */
@Composable
private fun FollowNewestMessage(
    listState: LazyListState,
    messages: LazyPagingItems<Message>,
) {
    val newestId = if (messages.itemCount > 0) messages.peek(0)?.id else null
    var shownNewestId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(newestId) {
        val previous = shownNewestId
        shownNewestId = newestId
        if (previous == null || newestId == null) return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.any { it.key == previous }) {
            listState.animateScrollToItem(0)
        }
    }
}

@Composable
private fun ChatState.subtitle(): String? = when {
    error != null -> error.asString()
    connection == ChatConnection.WAITING_FOR_NETWORK -> stringResource(Res.string.chat_waiting_for_network)
    connection == ChatConnection.UPDATING -> stringResource(Res.string.chat_updating)
    else -> null
}

@Composable
private fun EmptyChat(
    state: ChatState,
    modifier: Modifier = Modifier,
) {
    // Before the first catch-up an empty list only means "nothing cached yet", not "no messages".
    if (state.error == null && !state.isCaughtUp) {
        CircularProgressIndicator(modifier = modifier)
        return
    }
    Text(
        text = state.error?.asString() ?: stringResource(Res.string.chat_empty),
        modifier = modifier.padding(32.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun HistoryLoadState(
    loadState: LoadState,
    onRetryClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (loadState) {
            is LoadState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            is LoadState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val reason = (loadState.error as? LoadMessagesException)?.error?.toUiText()
                Text(
                    text = (reason ?: UiText.Resource(Res.string.chat_history_error)).asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onRetryClick) {
                    Text(stringResource(Res.string.chat_history_retry))
                }
            }
            is LoadState.NotLoading -> Unit
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    onRetryClick: () -> Unit,
) {
    val isOutgoing = message.isOutgoing
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isOutgoing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(text = message.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false))
                if (isOutgoing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusIcon(status = message.status, onRetryClick = onRetryClick)
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(
    status: MessageStatus,
    onRetryClick: () -> Unit,
) {
    when (status) {
        MessageStatus.SENDING -> Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = stringResource(Res.string.chat_status_sending),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        MessageStatus.SENT -> Icon(
            imageVector = Icons.Default.Done,
            contentDescription = stringResource(Res.string.chat_status_sent),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        MessageStatus.FAILED -> Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = stringResource(Res.string.chat_status_failed),
            modifier = Modifier.size(20.dp).clickable(onClick = onRetryClick),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun MessageInput(
    value: String,
    canSend: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(Res.string.chat_input_hint)) },
            maxLines = 5,
            shape = RoundedCornerShape(24.dp),
        )
        IconButton(onClick = onSendClick, enabled = canSend) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(Res.string.chat_send),
                tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}

@Preview
@Composable
private fun ChatScreenPreview() {
    val messages = listOf(
        Message("3", "c", "me", "Не ушло", 3L, MessageStatus.FAILED, isOutgoing = true),
        Message("2", "c", "me", "Привет, как дела?", 2L, MessageStatus.SENDING, isOutgoing = true),
        Message("1", "c", "them", "Привет!", 1L, MessageStatus.SENT, isOutgoing = false),
    )
    MaroTheme {
        ChatScreen(
            state = ChatState(header = ChatHeader("Иван Иванов", "ИИ"), connection = null, isCaughtUp = true),
            messages = flowOf(PagingData.from(messages)).collectAsLazyPagingItems(),
            onAction = {},
        )
    }
}
