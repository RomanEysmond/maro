package com.maro.feature.chat.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.maro.core.data.await
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore `chats/{chatId}/messages/{clientId}`; the shape is enforced by `firestore.rules`. The message and
 * the chat's `lastMessage*` are written in one transaction, so the chat list can never disagree with the chat.
 */
internal class FirestoreMessageRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : MessageRemoteDataSource {

    override suspend fun send(message: RemoteMessage): EmptyResult<DataError.Network> {
        val chatRef = firestore.collection(CHATS).document(message.chatId)
        val messageRef = chatRef.collection(MESSAGES).document(message.id)
        return try {
            firestore.runTransaction { transaction ->
                // Already delivered (a retry after a lost acknowledgement): nothing to do, and the timestamp
                // of the original stays untouched.
                if (transaction.get(messageRef).exists()) return@runTransaction true

                val now = FieldValue.serverTimestamp()
                transaction.update(
                    chatRef,
                    mapOf(
                        "lastMessageText" to message.text,
                        "lastMessageSenderId" to message.senderId,
                        "lastMessageAt" to now,
                        "updatedAt" to now,
                    ),
                )
                transaction.set(
                    messageRef,
                    mapOf(
                        "senderId" to message.senderId,
                        "text" to message.text,
                        "createdAt" to now,
                        "clientId" to message.id,
                    ),
                )
                true
            }.await()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
    }

    override suspend fun fetchLatest(chatId: String, limit: Int): Result<List<RemoteMessage>, DataError.Network> =
        fetch(chatId, messages(chatId).newestFirst().limit(limit.toLong()))

    override suspend fun fetchNewer(
        chatId: String,
        after: MessageCursor?,
        limit: Int,
    ): Result<List<RemoteMessage>, DataError.Network> =
        fetch(chatId, messages(chatId).oldestFirst().startAfterOrAll(after).limit(limit.toLong()))

    override suspend fun fetchOlder(
        chatId: String,
        before: MessageCursor,
        limit: Int,
    ): Result<List<RemoteMessage>, DataError.Network> =
        fetch(chatId, messages(chatId).newestFirst().startAfter(*before.toFieldValues()).limit(limit.toLong()))

    override fun observeNewer(
        chatId: String,
        after: MessageCursor?,
    ): Flow<Result<List<RemoteMessage>, DataError.Network>> = callbackFlow {
        val registration = messages(chatId).oldestFirst().startAfterOrAll(after)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.Error(error.toNetworkError()))
                    // Server snapshots only, like every other read here: the cache is not the source of truth.
                    snapshot != null && !snapshot.metadata.isFromCache ->
                        trySend(Result.Success(snapshot.documents.mapNotNull { it.toMessage(chatId) }))
                }
            }
        awaitClose { registration.remove() }
    }

    private suspend fun fetch(chatId: String, query: Query): Result<List<RemoteMessage>, DataError.Network> =
        try {
            // Source.SERVER: offline has to be a failure, not an empty page from the (memory-only) cache —
            // an empty page would be taken for "no more messages" and end the paging for good.
            val documents = query.get(Source.SERVER).await().documents
            Result.Success(documents.mapNotNull { it.toMessage(chatId) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }

    private fun messages(chatId: String): Query =
        firestore.collection(CHATS).document(chatId).collection(MESSAGES)

    // The id breaks ties between messages with the same timestamp, so a cursor is an exact position.
    private fun Query.oldestFirst(): Query =
        orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING).orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)

    private fun Query.newestFirst(): Query =
        orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING).orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)

    private fun Query.startAfterOrAll(cursor: MessageCursor?): Query =
        if (cursor == null) this else startAfter(*cursor.toFieldValues())

    private fun MessageCursor.toFieldValues(): Array<Any> = arrayOf(
        Timestamp(createdAtMicros / MICROS_PER_SECOND, ((createdAtMicros % MICROS_PER_SECOND) * NANOS_PER_MICRO).toInt()),
        id,
    )

    private fun DocumentSnapshot.toMessage(chatId: String): RemoteMessage? {
        // Only real server timestamps: the messages are written in transactions, so there are no local
        // estimates to see here, and a guessed time must never become a cursor.
        val createdAt = getTimestamp(FIELD_CREATED_AT, ServerTimestampBehavior.NONE) ?: return null
        return RemoteMessage(
            id = id,
            chatId = chatId,
            senderId = getString("senderId") ?: return null,
            text = getString("text") ?: return null,
            createdAtMicros = createdAt.seconds * MICROS_PER_SECOND + createdAt.nanoseconds / NANOS_PER_MICRO,
        )
    }

    private fun Exception.toNetworkError(): DataError.Network {
        if (this !is FirebaseFirestoreException) return DataError.Network.UNKNOWN
        return when (code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            -> DataError.Network.NO_INTERNET
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> DataError.Network.FORBIDDEN
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> DataError.Network.UNAUTHORIZED
            FirebaseFirestoreException.Code.NOT_FOUND -> DataError.Network.NOT_FOUND
            else -> DataError.Network.UNKNOWN
        }
    }

    private companion object {
        const val CHATS = "chats"
        const val MESSAGES = "messages"
        const val FIELD_CREATED_AT = "createdAt"
        const val MICROS_PER_SECOND = 1_000_000L
        const val NANOS_PER_MICRO = 1_000
    }
}
