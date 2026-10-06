package com.maro.feature.chat.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.maro.core.designsystem.component.InitialsAvatar
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.presentation.time.dayHeaderText
import com.maro.core.presentation.time.messageTimeText
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.core.presentation.util.UiText
import com.maro.core.presentation.util.rememberTextFieldValue
import com.maro.core.presentation.util.toUiText
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.ChatMember
import com.maro.feature.chat.domain.LoadMessagesException
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageStatus
import com.maro.feature.chat.domain.SystemEventKind
import com.maro.feature.chat.presentation.generated.resources.Res
import com.maro.feature.chat.presentation.generated.resources.chat_back
import com.maro.feature.chat.presentation.generated.resources.chat_empty
import com.maro.feature.chat.presentation.generated.resources.chat_event_added
import com.maro.feature.chat.presentation.generated.resources.chat_event_created
import com.maro.feature.chat.presentation.generated.resources.chat_event_left
import com.maro.feature.chat.presentation.generated.resources.chat_event_renamed
import com.maro.feature.chat.presentation.generated.resources.chat_history_error
import com.maro.feature.chat.presentation.generated.resources.chat_history_retry
import com.maro.feature.chat.presentation.generated.resources.chat_input_hint
import com.maro.feature.chat.presentation.generated.resources.chat_members
import com.maro.feature.chat.presentation.generated.resources.chat_retry_action
import com.maro.feature.chat.presentation.generated.resources.chat_send
import com.maro.feature.chat.presentation.generated.resources.chat_status_delivered
import com.maro.feature.chat.presentation.generated.resources.chat_status_failed
import com.maro.feature.chat.presentation.generated.resources.chat_status_read
import com.maro.feature.chat.presentation.generated.resources.chat_status_sending
import com.maro.feature.chat.presentation.generated.resources.chat_status_sent
import com.maro.feature.chat.presentation.generated.resources.chat_typing
import com.maro.feature.chat.presentation.generated.resources.chat_typing_named
import com.maro.feature.chat.presentation.generated.resources.chat_typing_several
import com.maro.feature.chat.presentation.generated.resources.chat_updating
import com.maro.feature.chat.presentation.generated.resources.chat_waiting_for_network
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ChatRoot(viewModel: ChatViewModel, onNavigateBack: () -> Unit, onOpenGroupInfo: (chatId: String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()

    // Messages count as read only while the chat is actually in front of the user.
    LifecycleResumeEffect(viewModel) {
        viewModel.onAction(ChatAction.OnVisibilityChange(isVisible = true))
        onPauseOrDispose { viewModel.onAction(ChatAction.OnVisibilityChange(isVisible = false)) }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChatEvent.NavigateBack -> onNavigateBack()
            is ChatEvent.NavigateToGroupInfo -> onOpenGroupInfo(event.chatId)
        }
    }

    ChatScreen(
        state = state,
        items = items,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(state: ChatState, items: LazyPagingItems<ChatItem>, onAction: (ChatAction) -> Unit) {
    val listState = rememberLazyListState()
    FollowNewestMessage(listState = listState, items = items)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable(enabled = state.header?.isGroup == true) {
                            onAction(ChatAction.OnHeaderClick)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        state.header?.let { header ->
                            InitialsAvatar(initials = header.initials, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = header.title, maxLines = 1)
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
                if (items.itemCount == 0) {
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
                        items(
                            count = items.itemCount,
                            key = items.itemKey { it.key },
                            contentType = items.itemContentType { it::class.simpleName },
                        ) { index ->
                            when (val item = items[index]) {
                                is ChatItem.MessageItem -> if (item.message.systemEvent != null) {
                                    SystemMessage(message = item.message, members = state.header?.members.orEmpty())
                                } else {
                                    MessageBubble(
                                        message = item.message,
                                        // In a group, the name goes above the first of someone's consecutive
                                        // messages (the list is newest first: the older neighbour is index + 1).
                                        senderName = senderNameFor(state, item.message, items.olderMessage(index)),
                                        onRetryClick = { onAction(ChatAction.OnRetryClick(item.message.id)) },
                                    )
                                }

                                is ChatItem.DaySeparator -> DayHeader(date = item.date)

                                null -> Unit
                            }
                        }
                        // Last in a reversed list = on top of the screen, above the oldest message.
                        item(key = "history") {
                            HistoryLoadState(
                                loadState = items.loadState.append,
                                onRetryClick = items::retry,
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
private fun FollowNewestMessage(listState: LazyListState, items: LazyPagingItems<ChatItem>) {
    // Index 0 is always the newest message: a day's heading sits above (after) that day's oldest message.
    val newestId = if (items.itemCount > 0) items.peek(0)?.key else null
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

    isPeerTyping && header?.isGroup == true -> when (typingNames.size) {
        1 -> stringResource(Res.string.chat_typing_named, typingNames.single())
        else -> stringResource(Res.string.chat_typing_several)
    }

    isPeerTyping -> stringResource(Res.string.chat_typing)

    connection == ChatConnection.UPDATING -> stringResource(Res.string.chat_updating)

    header?.isGroup == true -> pluralStringResource(Res.plurals.chat_members, header.memberCount, header.memberCount)

    else -> null
}

private fun LazyPagingItems<ChatItem>.olderMessage(index: Int): Message? =
    if (index + 1 < itemCount) (peek(index + 1) as? ChatItem.MessageItem)?.message else null

private fun senderNameFor(state: ChatState, message: Message, older: Message?): String? {
    val header = state.header ?: return null
    if (!header.isGroup || message.isOutgoing) return null
    if (older != null && older.systemEvent == null && older.senderId == message.senderId) return null
    return header.members.firstOrNull { it.id == message.senderId }?.fullName
}

@Composable
private fun SystemMessage(message: Message, members: List<ChatMember>) {
    val event = message.systemEvent ?: return
    fun nameOf(id: String): String = members.firstOrNull { it.id == id }?.firstName ?: "?"
    val actor = nameOf(message.senderId)
    val text = when (event.kind) {
        SystemEventKind.CREATED -> stringResource(Res.string.chat_event_created, actor, event.title.orEmpty())

        SystemEventKind.ADDED ->
            stringResource(Res.string.chat_event_added, actor, event.targetIds.joinToString(", ") { nameOf(it) })

        SystemEventKind.LEFT -> stringResource(Res.string.chat_event_left, actor)

        SystemEventKind.RENAMED -> stringResource(Res.string.chat_event_renamed, actor, event.title.orEmpty())
    }
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        ) {
            Text(
                text = dayHeaderText(date),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun EmptyChat(state: ChatState, modifier: Modifier = Modifier) {
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
private fun HistoryLoadState(loadState: LoadState, onRetryClick: () -> Unit) {
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
private fun MessageBubble(message: Message, senderName: String?, onRetryClick: () -> Unit) {
    val isOutgoing = message.isOutgoing
    // One node for TalkBack: "Anna, hey, 09:16, Read". A failed message retries on a tap anywhere on the bubble,
    // not just on its 20 dp icon.
    val bubbleModifier = if (message.status == MessageStatus.FAILED) {
        Modifier.clickable(onClickLabel = stringResource(Res.string.chat_retry_action), onClick = onRetryClick)
    } else {
        Modifier.semantics(mergeDescendants = true) {}
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isOutgoing) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp)).then(bubbleModifier),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                senderName?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = messageTimeText(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusIcon(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(status: MessageStatus) {
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
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        MessageStatus.DELIVERED -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = stringResource(Res.string.chat_status_delivered),
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        MessageStatus.READ -> Icon(
            imageVector = Icons.Default.DoneAll,
            contentDescription = stringResource(Res.string.chat_status_read),
            modifier = Modifier.size(16.dp),
            tint = MaroTheme.colors.readReceipt,
        )

        MessageStatus.FAILED -> Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = stringResource(Res.string.chat_status_failed),
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun MessageInput(value: String, canSend: Boolean, onValueChange: (String) -> Unit, onSendClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val input = rememberTextFieldValue(value)
        OutlinedTextField(
            value = input.value,
            onValueChange = {
                input.value = it
                onValueChange(it.text)
            },
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(Res.string.chat_input_hint)) },
            maxLines = 5,
            shape = RoundedCornerShape(24.dp),
        )
        IconButton(onClick = onSendClick, enabled = canSend) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(Res.string.chat_send),
                tint = if (canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.38f,
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun ChatScreenPreview() {
    val items = listOf(
        Message("3", "c", "me", "Не ушло", 3L, MessageStatus.FAILED, isOutgoing = true),
        Message("2", "c", "me", "Привет, как дела?", 2L, MessageStatus.READ, isOutgoing = true),
        Message("1", "c", "them", "Привет!", 1L, MessageStatus.SENT, isOutgoing = false),
    ).map<Message, ChatItem> { ChatItem.MessageItem(it) } + ChatItem.DaySeparator(LocalDate(2026, 9, 30))
    MaroTheme {
        ChatScreen(
            state = ChatState(
                header = ChatHeader("Иван Иванов", "ИИ"),
                connection = null,
                isCaughtUp = true,
                typingUserIds = setOf("them"),
            ),
            items = flowOf(PagingData.from(items)).collectAsLazyPagingItems(),
            onAction = {},
        )
    }
}
