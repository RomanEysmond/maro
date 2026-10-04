package com.maro.feature.chat.data

import com.maro.core.database.chat.ChatDao
import com.maro.core.database.message.MessageDao
import com.maro.core.domain.auth.CurrentUserProvider
import com.maro.core.domain.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Told when messages from others have landed in Room (by a catch-up or the live listener). */
fun interface DeliveryReporter {
    fun messagesArrived(chatId: String)

    companion object {
        val None = DeliveryReporter { }
    }
}

/**
 * Keeps the user's `lastDelivered` / `lastRead` marks on the server up to date with Room: always pointing at the
 * newest message from someone else. Each mark is written once per new message, in the background (best effort:
 * a failed write is simply repeated with the next message or the next time the chat is read).
 */
internal class ReceiptReporter(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val remote: MessageRemoteDataSource,
    private val currentUser: CurrentUserProvider,
    private val scope: CoroutineScope,
) : DeliveryReporter {

    private val mutex = Mutex()

    // (kind, chatId) -> the message the mark was last written for; avoids a write per listener snapshot.
    private val reported = mutableMapOf<Pair<ReceiptKind, String>, String>()

    override fun messagesArrived(chatId: String) {
        scope.launch { report(chatId, ReceiptKind.DELIVERED) }
    }

    /** Locally at once (the unread counter drops immediately), on the server in the background. */
    suspend fun markRead(chatId: String) {
        val userId = currentUser.userId ?: return
        val newest = messageDao.getNewestIncoming(chatId, userId) ?: return
        chatDao.advanceMyReadAt(chatId, newest.createdAt)
        scope.launch { report(chatId, ReceiptKind.READ) }
    }

    private suspend fun report(chatId: String, kind: ReceiptKind) {
        val userId = currentUser.userId ?: return
        val newest = messageDao.getNewestIncoming(chatId, userId) ?: return
        val key = kind to chatId
        mutex.withLock {
            if (reported[key] == newest.id) return
            reported[key] = newest.id
        }
        // Bounded: offline, the write waits in Firestore's queue anyway; nothing here needs to wait for it.
        val result = withTimeoutOrNull(REPORT_TIMEOUT_MS) {
            remote.reportReceipt(chatId, userId, kind, newest.id, newest.createdAt)
        }
        if (result !is Result.Success) {
            mutex.withLock { if (reported[key] == newest.id) reported.remove(key) }
        }
    }

    private companion object {
        const val REPORT_TIMEOUT_MS = 10_000L
    }
}
