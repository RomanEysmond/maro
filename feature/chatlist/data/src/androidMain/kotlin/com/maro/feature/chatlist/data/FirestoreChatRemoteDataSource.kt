package com.maro.feature.chatlist.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.maro.core.data.await
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.Result
import com.maro.feature.chatlist.domain.Chat
import com.maro.feature.chatlist.domain.ChatParticipant
import com.maro.feature.chatlist.domain.ChatType
import com.maro.feature.chatlist.domain.LastMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Firestore `chats/{chatId}`; the shape is enforced by `firestore.rules`. Not ordered server-side on
 * purpose (avoids a composite index): Room orders the cached list for the UI.
 */
internal class FirestoreChatRemoteDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ChatRemoteDataSource {

    override suspend fun fetchChats(): Result<List<Chat>, DataError.Network> {
        val uid = auth.currentUser?.uid ?: return Result.Error(DataError.Network.UNAUTHORIZED)
        return try {
            // Source.SERVER: a one-shot fetch has to report a real failure when offline, not quietly fall
            // back to the (empty, memory-only) cache and report success with nothing — the UI tells the two
            // apart (see ChatListViewModel), but only if a genuine failure reaches it.
            val documents = query(uid).get(Source.SERVER).await().documents
            Result.Success(documents.mapNotNull { it.toChat(uid) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
    }

    override fun observeChats(): Flow<Result<List<Chat>, DataError.Network>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(Result.Error(DataError.Network.UNAUTHORIZED))
            close()
            return@callbackFlow
        }

        val registration = query(uid).addSnapshotListener { snapshot, error ->
            when {
                error != null -> trySend(Result.Error(error.toNetworkError()))
                snapshot != null -> trySend(Result.Success(snapshot.documents.mapNotNull { it.toChat(uid) }))
            }
        }
        awaitClose { registration.remove() }
    }

    private fun query(uid: String) = firestore.collection(CHATS).whereArrayContains(FIELD_PARTICIPANTS, uid)

    private fun DocumentSnapshot.toChat(currentUid: String): Chat? {
        val type = getString(FIELD_TYPE) ?: return null
        val participants = get(FIELD_PARTICIPANTS) as? List<*> ?: return null
        val otherUid = participants.filterIsInstance<String>().firstOrNull { it != currentUid } ?: return null
        @Suppress("UNCHECKED_CAST")
        val otherInfo = (get(FIELD_PARTICIPANT_INFO) as? Map<String, Map<String, Any?>>)?.get(otherUid) ?: return null

        val lastMessageText = getString(FIELD_LAST_MESSAGE_TEXT)
        return Chat(
            id = id,
            type = if (type == "group") ChatType.GROUP else ChatType.DIRECT,
            otherParticipant = ChatParticipant(
                id = otherUid,
                firstName = otherInfo["firstName"] as? String ?: return null,
                lastName = otherInfo["lastName"] as? String ?: "",
                username = otherInfo["username"] as? String,
            ),
            lastMessage = lastMessageText?.let { text ->
                LastMessage(
                    text = text,
                    senderId = getString(FIELD_LAST_MESSAGE_SENDER_ID).orEmpty(),
                    sentAt = getTimestamp(FIELD_LAST_MESSAGE_AT)?.toDate()?.time ?: 0L,
                )
            },
            updatedAt = getTimestamp(FIELD_UPDATED_AT)?.toDate()?.time ?: 0L,
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
            else -> DataError.Network.UNKNOWN
        }
    }

    private companion object {
        const val CHATS = "chats"
        const val FIELD_TYPE = "type"
        const val FIELD_PARTICIPANTS = "participants"
        const val FIELD_PARTICIPANT_INFO = "participantInfo"
        const val FIELD_LAST_MESSAGE_TEXT = "lastMessageText"
        const val FIELD_LAST_MESSAGE_SENDER_ID = "lastMessageSenderId"
        const val FIELD_LAST_MESSAGE_AT = "lastMessageAt"
        const val FIELD_UPDATED_AT = "updatedAt"
    }
}
