package com.maro.feature.chat.domain

import androidx.paging.PagingData
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Error
import kotlinx.coroutines.flow.Flow

enum class SendError : Error {
    EMPTY,
    TOO_LONG,
    NOT_SIGNED_IN,
}

enum class OutboxResult {
    /** Nothing left to send (everything went out or was marked FAILED). */
    DONE,

    /** Something is still pending: try again later. */
    RETRY,
}

/** Where the open chat is with the server, see [MessageRepository.syncMessages]. */
sealed interface ChatSyncStatus {
    /** Fetching what arrived since the last sync. */
    data object CatchingUp : ChatSyncStatus

    /** Room is current and new messages stream in. */
    data object Live : ChatSyncStatus

    /** No network: catching up resumes on its own once it is back. */
    data object WaitingForNetwork : ChatSyncStatus

    /** Gave up (for example no access to the chat); the flow completes after this. */
    data class Failed(val error: DataError.Network) : ChatSyncStatus
}

/** What a notification about a new message shows; built from Room (the push itself carries ids only). */
data class IncomingMessage(
    val chatId: String,
    val messageId: String,
    val senderName: String,
    val text: String,
    /** Set in a group: the notification is the group's ("Hiking: Anna: …"). */
    val groupTitle: String? = null,
)

/** Paging needs a Throwable to report a failed page; this one carries the typed error for the UI. */
class LoadMessagesException(val error: DataError.Network) : Exception("Loading older messages failed: $error")

interface MessageRepository {
    /**
     * Backed by Room, newest first. Scrolling towards the end loads older history from the server page by page
     * (reported through the paging load state, see [LoadMessagesException]); the screen never reads the network.
     */
    fun messages(chatId: String): Flow<PagingData<Message>>

    /** `null` while the chat is not in the local cache. */
    fun chatHeader(chatId: String): Flow<ChatHeader?>

    /**
     * Optimistic send: the message is in Room as SENDING before this returns, and goes out in the background
     * (right away, then through [OutboxScheduler] while the network or the server is not cooperating).
     */
    suspend fun sendMessage(chatId: String, text: String): EmptyResult<SendError>

    /** Puts a FAILED message back into the queue. */
    suspend fun retryMessage(messageId: String)

    /**
     * For the open chat: catches Room up with everything after the last synced message (waiting for the network
     * if needed), then keeps it current until cancelled. Emits where it is; completes only after [ChatSyncStatus.Failed].
     */
    fun syncMessages(chatId: String): Flow<ChatSyncStatus>

    /**
     * For every chat, not just the open one: whenever the chat list shows a message newer than what Room has
     * (and whenever the network comes back), catches that chat up. Runs until cancelled.
     */
    suspend fun keepAllChatsInSync()

    /** One catch-up of [chatId] with the server (see [syncMessages]); used when a push says there is something new. */
    suspend fun catchUp(chatId: String): EmptyResult<DataError.Network>

    /** A message someone else sent, as cached in Room; `null` when it is not there (yet) or is the user's own. */
    suspend fun incomingMessage(chatId: String, messageId: String): IncomingMessage?

    /** The chat is open on screen right now (its [syncMessages] is being collected). */
    fun isChatOpen(chatId: String): Boolean

    /** Id of the newest message from someone else in the chat (Room), `null` while there is none. */
    fun newestIncomingMessageId(chatId: String): Flow<String?>

    /**
     * The user has seen the chat up to its newest message: moves their read mark (locally at once, on the server in
     * the background), which clears the unread counter and shows the senders their messages as READ.
     */
    suspend fun markRead(chatId: String)

    /** Sends everything that is SENDING, oldest first. [attempt] counts the previous background runs. */
    suspend fun flushOutbox(attempt: Int): OutboxResult
}

/** Asks the platform to call [MessageRepository.flushOutbox] later (Android: WorkManager, once the network is up). */
interface OutboxScheduler {
    fun schedule()
}
