package com.maro.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.maro.core.domain.util.Result
import com.maro.core.presentation.util.UiText
import com.maro.core.presentation.util.toUiText
import com.maro.feature.chat.domain.ChatSyncStatus
import com.maro.feature.chat.domain.MessageRepository
import com.maro.feature.chat.domain.MessageRules
import com.maro.feature.chat.domain.TypingRepository
import com.maro.feature.chat.presentation.generated.resources.Res
import com.maro.feature.chat.presentation.generated.resources.chat_image_unreadable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatId: String,
    private val repository: MessageRepository,
    private val typingRepository: TypingRepository,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state = _state.asStateFlow()

    private val _events = Channel<ChatEvent>()
    val events = _events.receiveAsFlow()

    /** Newest first, straight from Room, with a heading per day; older pages are fetched as the list is scrolled up. */
    val items: Flow<PagingData<ChatItem>> = repository.messages(chatId)
        .map { page ->
            page.map { ChatItem.MessageItem(it) }
                .insertSeparators<ChatItem.MessageItem, ChatItem> { newer, older ->
                    daySeparatorBetween(newer?.message, older?.message, timeZone)
                }
        }
        .cachedIn(viewModelScope)

    // Whether the screen is in front of the user (resumed): only then does a new message count as read.
    private val isVisible = MutableStateFlow(false)

    init {
        // What was typed here last time and not sent. Unless the user has started typing in the meantime.
        viewModelScope.launch {
            val draft = repository.draft(chatId)
            if (draft.isNotEmpty()) _state.update { if (it.input.isEmpty()) it.copy(input = draft) else it }
        }
        // The screen only ever reads Room; the server feeds Room through `syncMessages` below.
        repository.chatHeader(chatId)
            .onEach { header -> _state.update { it.copy(header = header) } }
            .launchIn(viewModelScope)
        // Runs until the screen is left; it only completes early after a permanent failure.
        repository.syncMessages(chatId)
            .onEach { status -> _state.update { it.with(status) } }
            .launchIn(viewModelScope)
        typingRepository.typingUsers(chatId)
            .onEach { typing -> _state.update { it.copy(typingUserIds = typing) } }
            .launchIn(viewModelScope)
        // While the chat is on screen, every new message from the other side is read as soon as it arrives.
        isVisible
            .flatMapLatest { visible -> if (visible) repository.newestIncomingMessageId(chatId) else emptyFlow() }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { repository.markRead(chatId) }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        // Leaving the chat mid-word must not leave "typing…" behind (it would expire anyway, but later).
        typingRepository.setTyping(chatId, isTyping = false)
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.OnInputChange -> {
                val input = action.value.take(MessageRules.MAX_LENGTH)
                _state.update { it.copy(input = input) }
                repository.saveDraft(chatId, input)
                typingRepository.setTyping(chatId, isTyping = input.isNotBlank())
            }

            is ChatAction.OnVisibilityChange -> isVisible.value = action.isVisible

            ChatAction.OnSendClick -> send()

            is ChatAction.OnRetryClick -> viewModelScope.launch { repository.retryMessage(action.messageId) }

            is ChatAction.OnImagePicked -> sendImage(action.source)

            is ChatAction.OnImageClick -> viewModelScope.launch { _events.send(ChatEvent.OpenImage(action.image)) }

            ChatAction.OnBackClick -> viewModelScope.launch { _events.send(ChatEvent.NavigateBack) }

            ChatAction.OnHeaderClick -> if (_state.value.header?.isGroup == true) {
                viewModelScope.launch { _events.send(ChatEvent.NavigateToGroupInfo(chatId)) }
            }
        }
    }

    private fun send() {
        val text = _state.value.input
        if (text.isBlank()) return
        // Cleared at once: the message is in Room (and on screen) before the network is even asked.
        _state.update { it.copy(input = "") }
        repository.saveDraft(chatId, "")
        typingRepository.setTyping(chatId, isTyping = false)
        viewModelScope.launch {
            val result = repository.sendMessage(chatId, text)
            // Only reachable for input the UI already rules out; keep the text rather than lose it.
            if (result is Result.Error) {
                _state.update { it.copy(input = text) }
                repository.saveDraft(chatId, text)
            }
        }
    }

    private fun sendImage(source: String) {
        viewModelScope.launch {
            // The photo is in the list (with the user's own copy) before it is uploaded; only an unreadable pick
            // fails here.
            if (repository.sendImage(chatId, source) is Result.Error) {
                _events.send(ChatEvent.ShowError(UiText.Resource(Res.string.chat_image_unreadable)))
            }
        }
    }

    private fun ChatState.with(status: ChatSyncStatus): ChatState = when (status) {
        ChatSyncStatus.CatchingUp -> copy(connection = ChatConnection.UPDATING)
        ChatSyncStatus.WaitingForNetwork -> copy(connection = ChatConnection.WAITING_FOR_NETWORK)
        ChatSyncStatus.Live -> copy(connection = null, isCaughtUp = true)
        is ChatSyncStatus.Failed -> copy(connection = null, error = status.error.toUiText())
    }
}
