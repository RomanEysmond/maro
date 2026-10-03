package com.maro.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.push.PushRegistrar
import com.maro.core.domain.util.Result
import com.maro.feature.chat.domain.MessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.android.ext.android.inject

/**
 * Receives the data-only pushes of the push server (`type=message`, `chatId`, `messageId`: never the text). The app
 * fetches the message itself (Room first, as always) and shows the notification from what it has cached.
 */
class MaroMessagingService : FirebaseMessagingService() {

    private val repository: MessageRepository by inject()
    private val registrar: PushRegistrar by inject()
    private val currentUser: CurrentUserProvider by inject()
    private val notifications: MessageNotifications by inject()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (currentUser.userId != null) scope.launch { registrar.register() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data[KEY_TYPE] != TYPE_MESSAGE) return
        val chatId = data[KEY_CHAT_ID] ?: return
        val messageId = data[KEY_MESSAGE_ID] ?: return
        if (currentUser.userId == null) return

        // Already on a background thread, and the system gives this call only a few seconds: block, but bounded.
        runBlocking {
            val caughtUp = withTimeoutOrNull(FETCH_TIMEOUT_MS) { repository.catchUp(chatId) } is Result.Success
            // The user is looking at this very chat: the message is already on screen.
            if (AppVisibility.isVisible && repository.isChatOpen(chatId)) return@runBlocking

            val incoming = repository.incomingMessage(chatId, messageId)
            when {
                incoming != null -> notifications.show(chatId, incoming)
                // Not fetched in time: still say that something arrived.
                !caughtUp -> notifications.show(chatId, null)
                // Fetched, but not someone else's message (for example the user's own from another device).
                else -> Unit
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_CHAT_ID = "chatId"
        const val KEY_MESSAGE_ID = "messageId"
        const val TYPE_MESSAGE = "message"
        const val FETCH_TIMEOUT_MS = 8_000L
    }
}
