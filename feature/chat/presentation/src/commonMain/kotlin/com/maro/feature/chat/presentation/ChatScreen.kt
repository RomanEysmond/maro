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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maro.core.designsystem.component.InitialsAvatar
import com.maro.core.designsystem.theme.MaroTheme
import com.maro.core.presentation.util.ObserveAsEvents
import com.maro.feature.chat.domain.ChatHeader
import com.maro.feature.chat.domain.Message
import com.maro.feature.chat.domain.MessageStatus
import com.maro.feature.chat.presentation.generated.resources.Res
import com.maro.feature.chat.presentation.generated.resources.chat_back
import com.maro.feature.chat.presentation.generated.resources.chat_empty
import com.maro.feature.chat.presentation.generated.resources.chat_input_hint
import com.maro.feature.chat.presentation.generated.resources.chat_send
import com.maro.feature.chat.presentation.generated.resources.chat_status_failed
import com.maro.feature.chat.presentation.generated.resources.chat_status_sending
import com.maro.feature.chat.presentation.generated.resources.chat_status_sent
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ChatRoot(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ChatEvent.NavigateBack -> onNavigateBack()
        }
    }

    ChatScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        state.header?.let { header ->
                            InitialsAvatar(initials = header.initials, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = header.participantName, maxLines = 1)
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
                if (state.messages.isEmpty()) {
                    Text(
                        text = state.error?.asString() ?: stringResource(Res.string.chat_empty),
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    // Newest at the bottom, and the list stays glued to it as new messages arrive.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(state.messages.asReversed(), key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                onRetryClick = { onAction(ChatAction.OnRetryClick(message.id)) },
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
    MaroTheme {
        ChatScreen(
            state = ChatState(
                header = ChatHeader("Иван Иванов", "ИИ"),
                messages = listOf(
                    Message("1", "c", "them", "Привет!", 1L, MessageStatus.SENT, isOutgoing = false),
                    Message("2", "c", "me", "Привет, как дела?", 2L, MessageStatus.SENDING, isOutgoing = true),
                    Message("3", "c", "me", "Не ушло", 3L, MessageStatus.FAILED, isOutgoing = true),
                ),
            ),
            onAction = {},
        )
    }
}
