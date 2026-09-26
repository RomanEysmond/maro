package com.maro.feature.chat.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
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

    override fun observeMessages(chatId: String): Flow<Result<List<RemoteMessage>, DataError.Network>> = callbackFlow {
        val registration = firestore.collection(CHATS).document(chatId).collection(MESSAGES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> trySend(Result.Error(error.toNetworkError()))
                    snapshot != null -> trySend(Result.Success(snapshot.documents.mapNotNull { it.toMessage(chatId) }))
                }
            }
        awaitClose { registration.remove() }
    }

    private fun DocumentSnapshot.toMessage(chatId: String): RemoteMessage? = RemoteMessage(
        id = id,
        chatId = chatId,
        senderId = getString("senderId") ?: return null,
        text = getString("text") ?: return null,
        // ESTIMATE: a timestamp the server has not filled in yet reads as "now" instead of null.
        createdAt = getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate()?.time ?: return null,
    )

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

        // Paging arrives with stage 5; until then the screen shows the latest page.
        const val PAGE_SIZE = 50L
    }
}
