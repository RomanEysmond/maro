package com.maro.feature.chatlist.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.maro.core.data.await
import com.maro.core.domain.util.DataError
import com.maro.core.domain.util.EmptyResult
import com.maro.core.domain.util.Result
import kotlin.coroutines.cancellation.CancellationException

/** Creates `chats/{chatId}`: direct chats and groups; the shapes are enforced by `firestore.rules`. */
internal class FirestoreNewChatRemoteDataSource(private val firestore: FirebaseFirestore) : NewChatRemoteDataSource {

    override suspend fun createChatIfAbsent(
        chatId: String,
        participants: List<ParticipantCard>,
    ): EmptyResult<DataError.Network> {
        val chatRef = firestore.collection(CHATS).document(chatId)
        return try {
            firestore.runTransaction { transaction ->
                if (transaction.get(chatRef).exists()) return@runTransaction true

                val now = FieldValue.serverTimestamp()
                transaction.set(
                    chatRef,
                    mapOf(
                        "type" to "direct",
                        "participants" to participants.map { it.id },
                        "participantInfo" to participants.associate { it.id to it.toInfo() },
                        "updatedAt" to now,
                        "createdAt" to now,
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

    override suspend fun createGroup(
        chatId: String,
        title: String,
        creatorId: String,
        participants: List<ParticipantCard>,
        systemMessageId: String,
    ): EmptyResult<DataError.Network> {
        val chatRef = firestore.collection(CHATS).document(chatId)
        val now = FieldValue.serverTimestamp()
        return try {
            // One batch: the group never exists without its "created" message, nor the message without the group.
            firestore.batch()
                .set(
                    chatRef,
                    mapOf(
                        "type" to "group",
                        "title" to title,
                        "createdBy" to creatorId,
                        "participants" to participants.map { it.id },
                        "participantInfo" to participants.associate { it.id to it.toInfo() },
                        "updatedAt" to now,
                        "createdAt" to now,
                    ),
                )
                .set(
                    chatRef.collection(MESSAGES).document(systemMessageId),
                    mapOf(
                        "senderId" to creatorId,
                        "type" to "system",
                        "event" to mapOf(
                            "kind" to "created",
                            "targets" to participants.map { it.id }.filter { it != creatorId },
                            "title" to title,
                        ),
                        "createdAt" to now,
                        "clientId" to systemMessageId,
                    ),
                )
                .commit()
                .await()
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.Error(e.toNetworkError())
        }
    }

    private fun ParticipantCard.toInfo(): Map<String, Any> = buildMap {
        put("firstName", firstName)
        put("lastName", lastName)
        username?.let { put("username", it) }
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
        const val MESSAGES = "messages"
    }
}
